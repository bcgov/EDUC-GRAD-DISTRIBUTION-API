package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.service.GraduationService;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.internal.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Data
@Component
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PSIReportProcess extends BaseProcess {

    private static Logger logger = LoggerFactory.getLogger(PSIReportProcess.class);
    private static final String ADDRESS_LABEL_PSI = "ADDRESS_LABEL_PSI";

    GraduationService graduationService;

    @Autowired
    public PSIReportProcess(GraduationService graduationService) {
        this.graduationService = graduationService;
    }

    @Override
    public ProcessorData fire(ProcessorData processorData) {
        long sTime = System.currentTimeMillis();
        logger.debug("************* TIME START   ************ {}", sTime);
        DistributionResponse disRes = new DistributionResponse();
        Map<String, DistributionPrintRequest> mDist = processorData.getMapDistribution();
        Long bId = processorData.getBatchId();
        int numOfPdfs = 0;
        int cnter = 0;
        List<School> schoolsForLabels = new ArrayList<>();
        for (Map.Entry<String, DistributionPrintRequest> entry : mDist.entrySet()) {
            cnter++;
            int currentSlipCount = 0;
            String psiCode = entry.getKey();
            DistributionPrintRequest obj = entry.getValue();
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
                logger.debug("PSI {}/{}", cnter, mDist.size());
            }
        }
        restUtils.fetchAccessToken(processorData);
        logger.debug("***** Create and Store school labels reports *****");
        int numberOfCreatedSchoolLabelReports = createSchoolLabelsReport(schoolsForLabels, processorData.getAccessToken(), ADDRESS_LABEL_PSI);
        logger.debug("***** Number of created school labels reports {} *****", numberOfCreatedSchoolLabelReports);
        logger.debug("***** Distribute school labels reports *****");
        int numberOfProcessedSchoolLabelsReports = processDistrictSchoolDistribution(processorData, ADDRESS_LABEL_PSI, null, null);
        logger.debug("***** Number of distributed school labels reports {} *****", numberOfProcessedSchoolLabelsReports);
        numOfPdfs += numberOfProcessedSchoolLabelsReports;
        postingProcess(bId, processorData, numOfPdfs);
        long eTime = System.currentTimeMillis();
        long difference = (eTime - sTime) / 1000;
        logger.debug("************* TIME Taken  ************ {} secs", difference);
        disRes.setMergeProcessResponse("Merge Successful and File Uploaded");
        processorData.setDistributionResponse(disRes);
        return processorData;
    }

    private Pair<Integer, Integer> processTranscriptPrintRequest(DistributionPrintRequest obj, int currentSlipCount, ReportRequest packSlipReq, ProcessorData processorData, String psiCode, int numOfPdfs) {
        if (obj.getPsiCredentialPrintRequest() != null) {
            PsiCredentialPrintRequest psiCredentialPrintRequest = obj.getPsiCredentialPrintRequest();
            List<PsiCredentialDistribution> scdList = psiCredentialPrintRequest.getPsiList();
            List<InputStream> locations = new ArrayList<>();
            currentSlipCount++;
            setExtraDataForPackingSlip(packSlipReq, "YED4", obj.getTotal(), scdList.size(), 1, "Transcript", psiCredentialPrintRequest.getBatchId());
            try {
                locations.add(reportService.getPackingSlip(packSlipReq, restUtils.getAccessToken()).getInputStream());
                logger.debug("*** Packing Slip Added");
                //Grad2-1931 : processing students transcripts, merging them and placing in tmp location for transmission mode FTP to generate CSV files- mchintha
                if (processorData.getTransmissionMode().equalsIgnoreCase(EducDistributionApiConstants.TRANSMISSION_MODE_FTP)) {
                    processStudentsForCSVs(scdList, psiCode, processorData);
                } else {
                    processStudentsForPDFs(scdList, locations);
                    mergeDocumentsPDFs(processorData, psiCode, "02", "/EDGRAD.T.", "YED4", locations);
                }
                numOfPdfs++;
                logger.debug("*** Transcript Documents Merged");
            } catch (IOException e) {
                logger.debug(EXCEPTION, e.getLocalizedMessage());
            }
        }
        return Pair.of(currentSlipCount, numOfPdfs);
    }


    private void processStudentsForPDFs(List<PsiCredentialDistribution> scdList, List<InputStream> locations) throws IOException {
        int currentTranscript = 0;
        int failedToAdd = 0;

        for (PsiCredentialDistribution scd : scdList) {
            InputStreamResource transcriptPdf = webClient.get().uri(String.format(educDistributionApiConstants.getTranscriptUsingStudentID(), scd.getStudentID())).headers(h -> h.setBearerAuth(restUtils.fetchAccessToken())).retrieve().bodyToMono(InputStreamResource.class).block();
            if (transcriptPdf != null) {
                locations.add(transcriptPdf.getInputStream());
                currentTranscript++;
                logger.debug("*** Added PDFs {}/{} Current student {}", currentTranscript, scdList.size(), scd.getStudentID());
            } else {
                failedToAdd++;
                logger.debug("*** Failed to Add PDFs {} Current student {}", failedToAdd, scd.getStudentID());
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
        List<String[]> studentTranscriptdata = new ArrayList<>();
        List<String> updatedStudentTranscriptdataList = new ArrayList<>();
        CsvMapper csvMapper = new CsvMapper();

        try {
            StringBuilder filePathBuilder = createFolderStructureInTempDirectory(processorData, psiCode, "02");

            filePathBuilder.append(EducDistributionApiConstants.FTP_FILENAME_PREFIX).append(psiCode).append(EducDistributionApiConstants.FTP_FILENAME_SUFFIX).append(".").append(EducDistributionApiUtils.getFileNameSchoolReports(psiCode)).append(".DAT");

            if (filePathBuilder != null) {
                path = Paths.get(filePathBuilder.toString());
                newFile = new File(Files.createFile(path).toUri());
            } else {
                throw new IOException(EducDistributionApiConstants.EXCEPTION_MSG_FILE_NOT_CREATED_AT_PATH);
            }
            for (PsiCredentialDistribution scd : scdList) {

                ReportData transcriptCsv = graduationService.getReportData(scd.getPen());

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

            csv = csvMapper.writeValueAsString(updatedStudentTranscriptdataList);
            csv = csv.replace("\"", "").replace(",", "\r\n");
            writesFormattedAllRowsDataOnCSV(csv, newFile);
        } catch (Exception e) {
            logger.error(EXCEPTION, e.getLocalizedMessage());
        }

    }

    private static void writesFormattedAllRowsDataOnCSV(String csv, File newFile) {
        try(FileWriter fWriter = new FileWriter(newFile)) {
            fWriter.write(csv);
        }
        catch (IOException e){
            logger.error(EXCEPTION, e.getLocalizedMessage());
        }
    }

    //Grad2-1931 : Writes Row D's data on CSV - mchintha
    private void writesCsvFileRowD(List<String[]> studentTranscriptdata, String pen, List<TranscriptResult> courseDetails) {
        String[] nonExaminableCoursesInfo = null;
        if (courseDetails != null) {
            for (TranscriptResult course : courseDetails) {
                //D rows writes only Non-Examinable Courses
                if (course.getCourse().getType().equals("2")) {
                    String credits = (course.getUsedForGrad() == null || course.getUsedForGrad().isBlank()) ? "" : course.getUsedForGrad();

                    nonExaminableCoursesInfo = new String[]{
                            pen,
                            EducDistributionApiConstants.LETTER_D,
                            course.getCourse().getCode(),
                            course.getCourse().getLevel(),
                            StringUtils.isNotBlank(course.getCourse().getSessionDate()) ? course.getCourse().getSessionDate() : "",
                            course.getMark().getInterimLetterGrade(),
                            course.getMark().getFinalLetterGrade(),
                            StringUtils.isBlank(course.getMark().getInterimPercent()) ? EducDistributionApiConstants.THREE_ZEROES : String.format("%03d", extractNumericValue(course.getMark().getInterimPercent())),
                            StringUtils.isBlank(course.getMark().getFinalPercent()) ? EducDistributionApiConstants.THREE_ZEROES : String.format("%03d", extractNumericValue(course.getMark().getFinalPercent())),
                            StringUtils.isBlank(course.getCourse().getCredits()) ? EducDistributionApiConstants.TWO_ZEROES : String.format("%02d", extractNumericValue(course.getCourse().getCredits())),
                            course.getCourse().getRelatedCourse(),
                            course.getCourse().getRelatedLevel(),
                            course.getCourse().getName(),
                            course.getEquivalency(),
                            course.getCourse().getType(),
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

                //C rows writes Examinable Courses and Assessments
                if (course.getCourse().getType().equals("1") || course.getCourse().getType().equals("3")) {

                    examinableCoursesAndAssessmentsInfo = new String[]{
                            pen,
                            EducDistributionApiConstants.LETTER_C,
                            course.getCourse().getCode(),
                            course.getCourse().getLevel(),
                            course.getCourse().getSessionDate(),
                            course.getMark().getInterimLetterGrade(),
                            "", //IB flag
                            StringUtils.isBlank(course.getMark().getSchoolPercent()) ? EducDistributionApiConstants.THREE_ZEROES : String.format("%03d", extractNumericValue(course.getMark().getSchoolPercent())),
                            course.getCourse().getSpecialCase(),
                            StringUtils.isBlank(course.getMark().getExamPercent()) ? EducDistributionApiConstants.THREE_ZEROES : String.format("%03d", extractNumericValue(course.getMark().getExamPercent())),
                            StringUtils.isBlank(course.getMark().getFinalPercent()) ? EducDistributionApiConstants.THREE_ZEROES : String.format("%03d", extractNumericValue(course.getMark().getFinalPercent())),
                            course.getMark().getFinalLetterGrade(),
                            StringUtils.isBlank(course.getMark().getInterimPercent()) ? EducDistributionApiConstants.THREE_ZEROES : String.format("%03d", extractNumericValue(course.getMark().getInterimPercent())),
                            StringUtils.isBlank(course.getCourse().getCredits()) ? EducDistributionApiConstants.TWO_ZEROES : String.format("%02d", extractNumericValue(course.getCourse().getCredits())),
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
        //Writes the A's row's data on CSV
        if (studentDetails != null) {

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(EducDistributionApiConstants.DATE_FORMAT);
            String studentBirthDate = simpleDateFormat.format(studentDetails.getBirthdate());
            studentInfo = new String[]{
                    pen,
                    EducDistributionApiConstants.LETTER_A,
                    studentDetails.getLastName(),
                    studentDetails.getFirstName(),
                    studentDetails.getMiddleName(),
                    (studentDetails.getBirthdate() == null || StringUtils.isBlank(studentBirthDate)) ? "" : studentBirthDate,
                    studentDetails.getGender(),
                    studentDetails.getCitizenship(),
                    studentDetails.getGrade(),
                    studentDetails.getGraduationStatus().getSchoolOfRecord(), studentDetails.getLocalId(),
                    "", //Optional program blank
                    studentDetails.getConsumerEducReqt(),
                    EducDistributionApiConstants.FOUR_ZEROES,
                    StringUtils.isNotBlank(studentDetails.getGraduationStatus().getProgramCompletionDate()) ? studentDetails.getGraduationStatus().getProgramCompletionDate() : EducDistributionApiConstants.SIX_ZEROES,
                    String.valueOf(studentDetails.getGraduationData().getDogwoodFlag()).equals("false") ? EducDistributionApiConstants.LETTER_N : EducDistributionApiConstants.LETTER_Y,
                    String.valueOf(studentDetails.getGraduationData().getHonorsFlag()).equals("false") ? EducDistributionApiConstants.LETTER_N : EducDistributionApiConstants.LETTER_Y,
                    studentDetails.getNonGradReasons().stream()
                            .map(NonGradReason::getCode)
                            .collect(Collectors.joining(",")),//Non grad reasons
                    "", //18 Blanks
                    StringUtils.isBlank(studentDetails.getGradProgram()) ? "" : studentDetails.getGradProgram().substring(1, 4)};

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

    //Grad2-1931 : Uploads school reports for only PSIRUNs- mchintha
    @Override
    protected void uploadSchoolReportDocuments(Long batchId, String mincode, String schoolCategory, String transmissionMode, byte[] gradReportPdf) {
        boolean isDistrict = StringUtils.isNotBlank(mincode) && StringUtils.length(mincode) == 3;
        String districtCode = StringUtils.substring(mincode, 0, 3);
        try {
            StringBuilder fileLocBuilder = new StringBuilder();
            if (SCHOOL_LABELS_CODE.equalsIgnoreCase(mincode)) {
                fileLocBuilder.append(EducDistributionApiConstants.TMP_DIR).append(EducDistributionApiConstants.FILES_FOLDER_STRUCTURE).append(transmissionMode.toUpperCase()).append(EducDistributionApiConstants.DEL).append(batchId);
            } else if (isDistrict) {
                fileLocBuilder.append(EducDistributionApiConstants.TMP_DIR).append(EducDistributionApiConstants.FILES_FOLDER_STRUCTURE).append(transmissionMode.toUpperCase()).append(EducDistributionApiConstants.DEL).append(batchId).append(EducDistributionApiConstants.DEL).append(districtCode);
            } else if ("02".equalsIgnoreCase(schoolCategory)) {
                fileLocBuilder.append(EducDistributionApiConstants.TMP_DIR).append(EducDistributionApiConstants.FILES_FOLDER_STRUCTURE).append(transmissionMode.toUpperCase()).append(EducDistributionApiConstants.DEL).append(batchId).append(EducDistributionApiConstants.DEL).append(mincode);
            } else {
                fileLocBuilder.append(EducDistributionApiConstants.TMP_DIR).append(EducDistributionApiConstants.FILES_FOLDER_STRUCTURE).append(transmissionMode.toUpperCase()).append(EducDistributionApiConstants.DEL).append(batchId).append(EducDistributionApiConstants.DEL).append(districtCode).append(EducDistributionApiConstants.DEL).append(mincode);
            }
            Path path = Paths.get(fileLocBuilder.toString());
            Files.createDirectories(path);
            StringBuilder fileNameBuilder = new StringBuilder();
            if (SCHOOL_LABELS_CODE.equalsIgnoreCase(mincode)) {
                fileNameBuilder.append(EducDistributionApiConstants.TMP_DIR).append(EducDistributionApiConstants.FILES_FOLDER_STRUCTURE).append(transmissionMode.toUpperCase()).append(EducDistributionApiConstants.DEL).append(batchId);
            } else if (isDistrict) {
                fileNameBuilder.append(EducDistributionApiConstants.TMP_DIR).append(EducDistributionApiConstants.FILES_FOLDER_STRUCTURE).append(transmissionMode.toUpperCase()).append(EducDistributionApiConstants.DEL).append(batchId).append(EducDistributionApiConstants.DEL).append(districtCode);
            } else if ("02".equalsIgnoreCase(schoolCategory)) {
                fileNameBuilder.append(EducDistributionApiConstants.TMP_DIR).append(EducDistributionApiConstants.FILES_FOLDER_STRUCTURE).append(transmissionMode.toUpperCase()).append(EducDistributionApiConstants.DEL).append(batchId).append(EducDistributionApiConstants.DEL).append(mincode);
            } else {
                fileNameBuilder.append(EducDistributionApiConstants.TMP_DIR).append(EducDistributionApiConstants.FILES_FOLDER_STRUCTURE).append(transmissionMode.toUpperCase()).append(EducDistributionApiConstants.DEL).append(batchId).append(EducDistributionApiConstants.DEL).append(districtCode).append(EducDistributionApiConstants.DEL).append(mincode);
            }
            if (SCHOOL_LABELS_CODE.equalsIgnoreCase(mincode)) {
                fileNameBuilder.append("/EDGRAD.L.").append("3L14.");
            } else {
                fileNameBuilder.append("/EDGRAD.R.").append("324W.");
            }
            fileNameBuilder.append(EducDistributionApiUtils.getFileNameSchoolReports(mincode)).append(".pdf");
            if (transmissionMode.equalsIgnoreCase(EducDistributionApiConstants.TRANSMISSION_MODE_PAPER)) {
                try (OutputStream out = new FileOutputStream(fileNameBuilder.toString())) {
                    out.write(gradReportPdf);
                }
            }


        } catch (Exception e) {
            logger.debug(EXCEPTION, e.getLocalizedMessage());
        }
    }
    //Grad2-1931 : Creates folder structure in temp directory only for PSIRUNs - mchintha
    public static StringBuilder createFolderStructureInTempDirectory(ProcessorData processorData, String minCode, String schoolCategoryCode) {
        String districtCode = StringUtils.substring(minCode, 0, 3);
        String activityCode = processorData.getActivityCode();
        String transmissionMode = processorData.getTransmissionMode();
        StringBuilder directoryPathBuilder = new StringBuilder();
        StringBuilder filePathBuilder = new StringBuilder();
        Path path;
        try {
            Boolean conditionResult = (EducDistributionApiConstants.MONTHLYDIST.equalsIgnoreCase(activityCode) || "02".equalsIgnoreCase(schoolCategoryCode));
                if (Boolean.TRUE.equals(conditionResult)) {
                    directoryPathBuilder.append(EducDistributionApiConstants.TMP_DIR).append(EducDistributionApiConstants.FILES_FOLDER_STRUCTURE).append(transmissionMode.toUpperCase()).append(EducDistributionApiConstants.DEL).append(processorData.getBatchId()).append(EducDistributionApiConstants.DEL).append(minCode).append(EducDistributionApiConstants.DEL);

                } else {
                    directoryPathBuilder.append(EducDistributionApiConstants.TMP_DIR).append(EducDistributionApiConstants.FILES_FOLDER_STRUCTURE).append(transmissionMode.toUpperCase()).append(EducDistributionApiConstants.DEL).append(processorData.getBatchId()).append(EducDistributionApiConstants.DEL).append(districtCode).append(EducDistributionApiConstants.DEL).append(minCode);
                }
                path = Paths.get(directoryPathBuilder.toString());
                Files.createDirectories(path);

                if (Boolean.TRUE.equals(conditionResult)) {
                    filePathBuilder.append(EducDistributionApiConstants.TMP_DIR).append(EducDistributionApiConstants.FILES_FOLDER_STRUCTURE).append(transmissionMode.toUpperCase()).append(EducDistributionApiConstants.DEL).append(processorData.getBatchId()).append(EducDistributionApiConstants.DEL).append(minCode);
                } else {
                    filePathBuilder.append(EducDistributionApiConstants.TMP_DIR).append(EducDistributionApiConstants.FILES_FOLDER_STRUCTURE).append(transmissionMode.toUpperCase()).append(EducDistributionApiConstants.DEL).append(processorData.getBatchId()).append(EducDistributionApiConstants.DEL).append(districtCode).append(EducDistributionApiConstants.DEL).append(minCode);
                }

        } catch (Exception e) {
            logger.error(EXCEPTION, e.getLocalizedMessage());
        }

        return filePathBuilder;
    }
}
