package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils;
import ca.bc.gov.educ.api.distribution.util.GradBusinessRuleException;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.internal.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants.TMP_DIR;


@Data
@Component
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PSIReportProcess extends BaseProcess {

    private static Logger logger = LoggerFactory.getLogger(PSIReportProcess.class);
    private static final String TRANSMISSION_MODE_ERROR = "Transmission mode can't be blank for PSI distribution";

    @Override
    public ProcessorData fire(ProcessorData processorData) {
        long sTime = System.currentTimeMillis();
        logger.debug("************* TIME START   ************ {}", sTime);
        DistributionResponse disRes = new DistributionResponse();
        disRes.setTransmissionMode(processorData.getTransmissionMode());
        DistributionRequest distributionRequest = processorData.getDistributionRequest();
        Map<String, DistributionPrintRequest> mapDist = distributionRequest.getMapDist();
        Long batchId = processorData.getBatchId();
        int numOfPdfs = 0;
        int cnter = 0;
        List<School> schoolsForLabels = new ArrayList<>();
        for (String psiCode : mapDist.keySet()) {
            cnter++;
            int currentSlipCount = 0;
            DistributionPrintRequest obj = mapDist.get(psiCode);
            Psi psiDetails = psiService.getPsiDetails(psiCode, restUtils.getAccessToken());
            if (psiDetails != null) {
                logger.debug("*** PSI Details Acquired {}", psiDetails.getPsiName());
                ReportRequest packSlipReq = reportService.preparePackingSlipDataPSI(psiDetails, processorData.getBatchId());
                Pair<Integer, Integer> pV = processTranscriptPrintRequest(obj, currentSlipCount, packSlipReq, processorData, psiCode, numOfPdfs);
                numOfPdfs = pV.getRight();
                logger.debug("PDFs Merged {}", psiDetails.getPsiName());
                processSchoolsForLabels(schoolsForLabels, psiDetails);
                if (cnter % 50 == 0) {
                    restUtils.fetchAccessToken(processorData);
                }
                logger.debug("PSI {}/{}", cnter, mapDist.size());
            }
        }
        restUtils.fetchAccessToken(processorData);
        logger.debug("***** Create and Store school labels reports *****");
        int numberOfCreatedSchoolLabelReports = createSchoolLabelsReport(schoolsForLabels, ADDRESS_LABEL_PSI);
        logger.debug("***** Number of created school labels reports {} *****", numberOfCreatedSchoolLabelReports);
        logger.debug("***** Distribute school labels reports *****");
        int numberOfProcessedSchoolLabelsReports = processDistrictSchoolDistribution(batchId, new ArrayList<>(), ADDRESS_LABEL_PSI, null, null, processorData.getTransmissionMode());
        logger.debug("***** Number of distributed school labels reports {} *****", numberOfProcessedSchoolLabelsReports);
        numOfPdfs += numberOfProcessedSchoolLabelsReports;
        postingProcess(batchId, processorData, numOfPdfs, getZipFolderFromRootLocation(processorData));
        long eTime = System.currentTimeMillis();
        long difference = (eTime - sTime) / 1000;
        logger.debug("************* TIME Taken  ************ {} secs", difference);
        disRes.setMergeProcessResponse("Merge Successful and File Uploaded");
        processorData.setDistributionResponse(disRes);
        return processorData;
    }

    protected Pair<Integer, Integer> processTranscriptPrintRequest(DistributionPrintRequest obj, int currentSlipCount, ReportRequest packSlipReq, ProcessorData processorData, String psiCode, int numOfPdfs) {
        if (obj.getPsiCredentialPrintRequest() != null) {
            PsiCredentialPrintRequest psiCredentialPrintRequest = obj.getPsiCredentialPrintRequest();
            List<PsiCredentialDistribution> scdList = psiCredentialPrintRequest.getPsiList();
            List<InputStream> locations = new ArrayList<>();
            currentSlipCount++;
            setExtraDataForPackingSlip(packSlipReq, "YED4", obj.getTotal(), scdList.size(), 1, "Transcript", psiCredentialPrintRequest.getBatchId());
            try {
                locations.add(reportService.getPackingSlip(packSlipReq).getInputStream());
                logger.debug("*** Packing Slip Added");
                //Grad2-1931 : processing students transcripts, merging them and placing in tmp location for transmission mode FTP to generate CSV files- mchintha
                if (EducDistributionApiConstants.TRANSMISSION_MODE_FTP.equalsIgnoreCase(processorData.getTransmissionMode())) {
                    processStudentsForCSVs(scdList, psiCode, processorData);
                } else {
                    processStudentsForPDFs(scdList, locations);
                    mergeDocumentsPDFs(processorData, psiCode, "02", "/EDGRAD.T.", "YED4", locations);
                }
                numOfPdfs++;
                logger.debug("*** Transcript Documents Merged");
            } catch (IOException e) {
                logger.error(EXCEPTION, e.getLocalizedMessage());
            }
        }
        return Pair.of(currentSlipCount, numOfPdfs);
    }

    private void processStudentsForPDFs(List<PsiCredentialDistribution> scdList, List<InputStream> locations) throws IOException {
        int currentTranscript = 0;
        int failedToAdd = 0;

        for (PsiCredentialDistribution scd : scdList) {
            int result = addStudentTranscriptToLocations(scd.getStudentID().toString(), locations);
            if(result == 0) {
                failedToAdd++;
                logger.debug("*** Failed to Add PDFs {} Current student {}", failedToAdd, scd.getStudentID());
            } else {
                currentTranscript++;
                logger.debug("*** Added PDFs {}/{} Current student {}", currentTranscript, scdList.size(), scd.getStudentID());
            }
        }
    }

    //Grad2-1931 : Writes students transcripts data on CSV and formatting them - mchintha
    private void processStudentsForCSVs(List<PsiCredentialDistribution> scdList, String psiCode, ProcessorData processorData) throws IOException {
        int currentTranscript = 0;
        int failedToAdd = 0;
        String[] schoolInfo = null;
        String csv;
        Path path;
        File newFile = null;
        List<String[]> studentTranscriptdata = null;
        List<String> updatedStudentTranscriptdataList = new ArrayList<>();
        CsvMapper csvMapper = new CsvMapper();

        try {
            String transmissionMode = processorData.getTransmissionMode();
            if (StringUtils.isBlank(transmissionMode)) {
                throw new GradBusinessRuleException(TRANSMISSION_MODE_ERROR);
            }
            String rootDirectory = EducDistributionApiConstants.TMP_DIR + EducDistributionApiConstants.FILES_FOLDER_STRUCTURE + transmissionMode.toUpperCase();
            StringBuilder filePathBuilder = createFolderStructureInTempDirectory(rootDirectory, processorData, psiCode, "02");
            filePathBuilder.append(EducDistributionApiConstants.FTP_FILENAME_PREFIX).append(psiCode).append(EducDistributionApiConstants.FTP_FILENAME_SUFFIX).append(".").append(EducDistributionApiUtils.getFileNameSchoolReports(psiCode)).append(".DAT");
            if (filePathBuilder != null) {
                path = Paths.get(filePathBuilder.toString());
                newFile = new File(Files.createFile(path).toUri());
            } else {
                throw new IOException(EducDistributionApiConstants.EXCEPTION_MSG_FILE_NOT_CREATED_AT_PATH);
            }
            for (PsiCredentialDistribution scd : scdList) {
                if (scd.getPen() != null) {

                    studentTranscriptdata = new ArrayList<>();
                    ReportData transcriptCsv = psiService.getReportData(scd.getPen());

                    if (transcriptCsv != null) {
                        Student studentDetails = transcriptCsv.getStudent();
                        School schoolDetails = transcriptCsv.getSchool();
                        List<TranscriptResult> courseDetails = (transcriptCsv.getTranscript() != null ? transcriptCsv.getTranscript().getResults() : null);

                        //Writes the A's row's data on CSV
                        writesCsvFileRowA(studentTranscriptdata, scd.getPen(), studentDetails);

                        //Writes the B's row's data on CSV
                        if (schoolDetails != null) {
                            schoolInfo = new String[]{
                                    scd.getPen(),
                                    EducDistributionApiConstants.LETTER_B,
                                    "", //Address blank
                                    "", //Address blank
                                    "", //City blank
                                    "", //Prov code blank
                                    "", //country code blank
                                    "" //postal blank
                            };

                            setColumnsWidths(schoolInfo,
                                    IntStream.of(10, 1, 40, 40, 30, 2, 2, 7).toArray(),
                                    studentTranscriptdata);
                        }
                        //Writes the C's data on CSV
                        writesCsvFileRowC(studentTranscriptdata, scd.getPen(), courseDetails);

                        //Writes D's rows data on CSV
                        writesCsvFileRowD(studentTranscriptdata, scd.getPen(), courseDetails);
                        currentTranscript++;
                        logger.debug("*** Added csv {}/{} Current student {}", currentTranscript, scdList.size(), scd.getPen());
                    } else {
                        failedToAdd++;
                        logger.debug("*** Failed to Add {} Current student {}", failedToAdd, scd.getPen());
                    }

                }
                // Converts list of string array to list of string which removes double quotes surrounded by each string and commas in between.
                for (String[] studentData : studentTranscriptdata) {
                    String stringArrayAsString = String.join("", studentData);
                    updatedStudentTranscriptdataList.add(stringArrayAsString);
                }
            }

            csv = csvMapper.writeValueAsString(updatedStudentTranscriptdataList);
            csv = csv.replace("\"", "").replace(",", "\r\n");
            writesFormattedAllRowsDataOnCSV(csv, newFile);
        } catch (Exception e) {
            logger.error(EXCEPTION, e.getLocalizedMessage());
        }

    }

    //Grad2-1931 : Writes all rows data together on CSV - mchintha
    private void writesFormattedAllRowsDataOnCSV(String csv, File newFile) {
        try (FileWriter fWriter = new FileWriter(newFile)) {
            fWriter.write(csv);
            fWriter.flush();
        } catch (IOException e) {
            logger.error(EXCEPTION, e.getLocalizedMessage());
        }
    }

    //Grad2-1931 : Writes Row D's data on CSV - mchintha
    private void writesCsvFileRowD(List<String[]> studentTranscriptdata, String pen, List<TranscriptResult> courseDetails) {
        String[] nonExaminableCoursesInfo = null;
        if (courseDetails != null) {
            for (TranscriptResult course : courseDetails) {
                String credits = (course.getUsedForGrad() == null || course.getUsedForGrad().isBlank()) ? "" : course.getUsedForGrad();
                String courseType = (course.getCourse().getType() == null || course.getCourse().getType().isBlank()) ? "" : course.getCourse().getType();
                //D rows writes only Non-Examinable Courses
                if (courseType.equals("2")) {

                    nonExaminableCoursesInfo = new String[]{
                            pen,
                            EducDistributionApiConstants.LETTER_D,
                            (course.getCourse().getCode() == null || course.getCourse().getCode().isBlank()) ? "" : course.getCourse().getCode(),
                            (course.getCourse().getLevel() == null || course.getCourse().getLevel().isBlank()) ? "" : course.getCourse().getLevel(),
                            (course.getCourse().getSessionDate() != null || StringUtils.isNotBlank(course.getCourse().getSessionDate())) ? course.getCourse().getSessionDate() : "",
                            (course.getMark().getInterimLetterGrade() == null || course.getMark().getInterimLetterGrade().isBlank()) ? "" : course.getMark().getInterimLetterGrade(),
                            (course.getMark().getFinalLetterGrade() == null || course.getMark().getFinalLetterGrade().isBlank()) ? "" : course.getMark().getFinalLetterGrade(),
                            (course.getMark().getInterimPercent() == null || StringUtils.isBlank(course.getMark().getInterimPercent())) ? EducDistributionApiConstants.THREE_ZEROES : String.format("%03d", extractNumericValue(course.getMark().getInterimPercent())),
                            (course.getMark().getFinalPercent() == null || StringUtils.isBlank(course.getMark().getFinalPercent())) ? EducDistributionApiConstants.THREE_ZEROES : String.format("%03d", extractNumericValue(course.getMark().getFinalPercent())),
                            (course.getCourse().getCredits() == null || StringUtils.isBlank(course.getCourse().getCredits())) ? EducDistributionApiConstants.TWO_ZEROES : String.format("%02d", extractNumericValue(course.getCourse().getCredits())),
                            (course.getCourse().getRelatedCourse() == null || course.getCourse().getRelatedCourse().isBlank()) ? "" : course.getCourse().getRelatedCourse(),
                            (course.getCourse().getRelatedLevel() == null || course.getCourse().getRelatedLevel().isBlank()) ? "" : course.getCourse().getRelatedLevel(),
                            (course.getCourse().getName() == null || course.getCourse().getName().isBlank()) ? "" : course.getCourse().getName(),
                            (course.getEquivalency() == null || course.getEquivalency().isBlank()) ? "" : course.getEquivalency(),
                            courseType,
                            "",// partial flag
                            extractNumericValue(credits) > 0 ? EducDistributionApiConstants.LETTER_Y : ""
                    };

                    setColumnsWidths(nonExaminableCoursesInfo,
                            IntStream.of(10, 1, 5, 3, 6, 2, 2, 3, 3, 2, 5, 3, 40, 1, 1, 1, 1).toArray(),
                            studentTranscriptdata);

                }
            }
        }
    }

    //Grad2-1931 : Writes Row C's data on CSV - mchintha
    private void writesCsvFileRowC(List<String[]> studentTranscriptdata, String pen, List<TranscriptResult> courseDetails) {
        String[] examinableCoursesAndAssessmentsInfo = null;
        if (courseDetails != null) {
            for (TranscriptResult course : courseDetails) {
                String credits = (course.getUsedForGrad() == null || course.getUsedForGrad().isBlank()) ? "" : course.getUsedForGrad();
                String courseType = (course.getCourse().getType() == null || course.getCourse().getType().isBlank()) ? "" : course.getCourse().getType();

                //C rows writes Examinable Courses and Assessments
                if (courseType.equals("1") || courseType.equals("3")) {

                    examinableCoursesAndAssessmentsInfo = new String[]{
                            pen,
                            EducDistributionApiConstants.LETTER_C,
                            (course.getCourse().getCode() == null || course.getCourse().getCode().isBlank()) ? "" : course.getCourse().getCode(),
                            (course.getCourse().getLevel() == null || course.getCourse().getLevel().isBlank()) ? "" : course.getCourse().getLevel(),
                            (course.getCourse().getSessionDate() == null || StringUtils.isBlank(course.getCourse().getSessionDate())) ? "" : course.getCourse().getSessionDate(),
                            (course.getMark().getInterimLetterGrade() == null || course.getMark().getInterimLetterGrade().isBlank()) ? "" : course.getMark().getInterimLetterGrade(),
                            "", //IB flag
                            (course.getMark().getSchoolPercent() == null || StringUtils.isBlank(course.getMark().getSchoolPercent())) ? EducDistributionApiConstants.THREE_ZEROES : String.format("%03d", extractNumericValue(course.getMark().getSchoolPercent())),
                            (course.getCourse().getSpecialCase() == null || course.getCourse().getSpecialCase().isBlank()) ? "" : course.getCourse().getSpecialCase(),
                            (course.getMark().getExamPercent() == null || StringUtils.isBlank(course.getMark().getExamPercent())) ? EducDistributionApiConstants.THREE_ZEROES : String.format("%03d", extractNumericValue(course.getMark().getExamPercent())),
                            (course.getMark().getFinalPercent() == null || StringUtils.isBlank(course.getMark().getFinalPercent())) ? EducDistributionApiConstants.THREE_ZEROES : String.format("%03d", extractNumericValue(course.getMark().getFinalPercent())),
                            (course.getMark().getFinalLetterGrade() == null || course.getMark().getFinalLetterGrade().isBlank()) ? "" : course.getMark().getFinalLetterGrade(),
                            (course.getMark().getInterimPercent() == null || StringUtils.isBlank(course.getMark().getInterimPercent())) ? EducDistributionApiConstants.THREE_ZEROES : String.format("%03d", extractNumericValue(course.getMark().getInterimPercent())),
                            (course.getCourse().getCredits() == null || StringUtils.isBlank(course.getCourse().getCredits())) ? EducDistributionApiConstants.TWO_ZEROES : String.format("%02d", extractNumericValue(course.getCourse().getCredits())),
                            "", //Course case
                            extractNumericValue(credits) > 0 ? EducDistributionApiConstants.LETTER_Y : ""
                    };

                    setColumnsWidths(examinableCoursesAndAssessmentsInfo,
                            IntStream.of(10, 1, 5, 3, 6, 2, 1, 3, 1, 3, 3, 2, 3, 2, 1, 1).toArray(),
                            studentTranscriptdata);
                }
            }
        }
    }

    //Grad2-1931 : Writes Row A's data on CSV - mchintha
    private void writesCsvFileRowA(List<String[]> studentTranscriptdata, String pen, Student studentDetails) {
        String[] studentInfo;
        String birthDate = null;
        //Writes the A's row's data on CSV
        if (studentDetails != null) {

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(EducDistributionApiConstants.DATE_FORMAT);

            if (studentDetails.getBirthdate() == null || StringUtils.isBlank(studentDetails.getBirthdate().toString())) {
                birthDate = "";
            } else {
                birthDate = simpleDateFormat.format(studentDetails.getBirthdate());
            }
            String dogWoodFlag = String.valueOf(studentDetails.getGraduationData().getDogwoodFlag()).isBlank() ? "" : String.valueOf(studentDetails.getGraduationData().getDogwoodFlag());
            String honorsFlag = String.valueOf(studentDetails.getGraduationData().getHonorsFlag()).isBlank() ? "" : String.valueOf(studentDetails.getGraduationData().getHonorsFlag());

            studentInfo = new String[]{
                    pen,
                    EducDistributionApiConstants.LETTER_A,
                    (studentDetails.getLastName() == null || StringUtils.isBlank(studentDetails.getLastName())) ? "" : studentDetails.getLastName(),
                    (studentDetails.getFirstName() == null || StringUtils.isBlank(studentDetails.getFirstName())) ? "" : studentDetails.getFirstName(),
                    (studentDetails.getMiddleName() == null || StringUtils.isBlank(studentDetails.getMiddleName())) ? "" : studentDetails.getMiddleName(),
                    birthDate,
                    (studentDetails.getGender() == null || StringUtils.isBlank(studentDetails.getGender())) ? "" : studentDetails.getGender(),
                    (studentDetails.getCitizenship() == null || StringUtils.isBlank(studentDetails.getCitizenship())) ? "" : studentDetails.getCitizenship(),
                    (studentDetails.getGrade() == null || StringUtils.isBlank(studentDetails.getGrade())) ? "" : studentDetails.getGrade(),
                    (studentDetails.getGraduationStatus().getSchoolOfRecord() == null || StringUtils.isBlank(studentDetails.getGraduationStatus().getSchoolOfRecord())) ? "" : studentDetails.getGraduationStatus().getSchoolOfRecord(),
                    (studentDetails.getLocalId() == null || StringUtils.isBlank(studentDetails.getLocalId())) ? "" : studentDetails.getLocalId(),
                    "", //Optional program blank
                    (studentDetails.getConsumerEducReqt() == null || StringUtils.isBlank(studentDetails.getConsumerEducReqt())) ? "" : studentDetails.getConsumerEducReqt(),
                    EducDistributionApiConstants.FOUR_ZEROES,
                    StringUtils.isNotBlank(studentDetails.getGraduationStatus().getProgramCompletionDate()) ? studentDetails.getGraduationStatus().getProgramCompletionDate() : EducDistributionApiConstants.SIX_ZEROES,
                    dogWoodFlag.equals("false") ? EducDistributionApiConstants.LETTER_N : EducDistributionApiConstants.LETTER_Y,
                    honorsFlag.equals("false") ? EducDistributionApiConstants.LETTER_N : EducDistributionApiConstants.LETTER_Y,
                    studentDetails.getNonGradReasons().stream()
                            .map(NonGradReason::getCode)
                            .collect(Collectors.joining(",")),//Non grad reasons
                    "", //18 Blanks
                    (studentDetails.getGradProgram() == null || StringUtils.isBlank(studentDetails.getGradProgram())) ? "" : studentDetails.getGradProgram().substring(1, 4)};

            setColumnsWidths(
                    studentInfo,
                    IntStream.of(10, 1, 25, 25, 25, 8, 1, 1, 2, 8, 12, 2, 1, 4, 6, 1, 1, 15, 18, 4).toArray(),
                    studentTranscriptdata);
        }
    }

    //Grad2-1931 sets columns widths of each row on csv - mchintha
    private void setColumnsWidths(String[] allCSVRowsInfo, int[] eachRowsColumnsWidths, List<String[]> studentTranscriptdata) {
        for (int i = 0; i < allCSVRowsInfo.length; i++) {
            String columnWidth = allCSVRowsInfo[i] != null ? allCSVRowsInfo[i] : "";
            String value = "%-" + eachRowsColumnsWidths[i] + "s";
            String formattedString = String.format(value, columnWidth);
            allCSVRowsInfo[i] = formattedString;
        }
        studentTranscriptdata.add(allCSVRowsInfo);
    }

    //Grad2-1931 considers only numeric values - mchintha
    private int extractNumericValue(String val) {
        return val.chars()
                .filter(Character::isDigit)
                .reduce(0, (a, b) -> a * 10 + Character.getNumericValue(b));

    }

    //Grad2-2052 - setting SFTP root folder location for PSIRUN paper where it has to pick zip folders from, to send them to BC mail - mchintha
    @Override
    protected String getZipFolderFromRootLocation(ProcessorData processorData) {
        String transmissionMode = StringUtils.upperCase(processorData.getTransmissionMode());
        if (StringUtils.isBlank(transmissionMode)) {
            throw new GradBusinessRuleException(TRANSMISSION_MODE_ERROR);
        }
        logger.debug("getZipFolderFromRootLocation {} transmission mode {}", TMP_DIR, transmissionMode);
        return EducDistributionApiConstants.TMP_DIR + EducDistributionApiConstants.FILES_FOLDER_STRUCTURE + transmissionMode;
    }
}
