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
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils.ADDRESS_LABEL_PSI;
import static ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils.SCHOOL_LABELS_CODE;


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
        DistributionResponse response = new DistributionResponse();
        response.setTransmissionMode(processorData.getTransmissionMode());
        DistributionRequest distributionRequest = processorData.getDistributionRequest();
        Map<String, DistributionPrintRequest> mapDist = distributionRequest.getMapDist();
        StudentSearchRequest searchRequest = distributionRequest.getStudentSearchRequest();
        Long batchId = processorData.getBatchId();
        int numberOfPdfs = 0;
        int counter = 0;
        List<School> schoolsForLabels = new ArrayList<>();
        List<Integer> valueList = mapDist.values()
                .stream()
                .map(e -> e.getPsiCredentialPrintRequest().getPsiList().size())
                .collect(Collectors.toList());
        int studentsCount = valueList.stream().mapToInt(i->i).sum();
        logger.debug("Total number of students to be processed: {}", studentsCount);
        for (String psiCode : mapDist.keySet()) {
            counter++;
            int currentSlipCount = 0;
            DistributionPrintRequest obj = mapDist.get(psiCode);
            psiCode = StringUtils.trim(psiCode);
            Psi psiDetails = psiService.getPsiDetails(psiCode, restUtils.getAccessToken());
            if (psiDetails != null) {
                logger.debug("*** PSI Details Acquired {}", psiDetails.getPsiName());
                ReportRequest packSlipReq = reportService.preparePackingSlipDataPSI(psiDetails, processorData.getBatchId());
                Pair<Integer, Integer> pV = processTranscriptPrintRequest(obj, currentSlipCount, packSlipReq, processorData, psiCode, numberOfPdfs);
                numberOfPdfs = pV.getRight();
                logger.debug("PDFs Merged {}", psiDetails.getPsiName());
                processSchoolsForLabels(schoolsForLabels, psiDetails);
                if (counter % 50 == 0) {
                    restUtils.fetchAccessToken(processorData);
                }
                logger.debug("PSI {}/{}", counter, mapDist.size());
            }
        }
        restUtils.fetchAccessToken(processorData);
        logger.debug("***** Create and Store school labels reports *****");
        int numberOfCreatedSchoolLabelReports = createSchoolLabelsReport(schoolsForLabels, ADDRESS_LABEL_PSI);
        logger.debug("***** Number of created school labels reports {} *****", numberOfCreatedSchoolLabelReports);
        logger.debug("***** Distribute school labels reports *****");
        String schoolLabelCode = schoolsForLabels.size() == 1 ? schoolsForLabels.get(0).getMincode() : SCHOOL_LABELS_CODE;
        int numberOfProcessedSchoolLabelsReports = processDistrictSchoolDistribution(batchId, List.of(schoolLabelCode), ADDRESS_LABEL_PSI, null, null, processorData.getTransmissionMode());
        logger.debug("***** Number of distributed school labels reports {} *****", numberOfProcessedSchoolLabelsReports);
        numberOfPdfs += numberOfProcessedSchoolLabelsReports;
        postingProcess(batchId, processorData, numberOfPdfs, getRootPathForFilesStorage(processorData));
        long eTime = System.currentTimeMillis();
        long difference = (eTime - sTime) / 1000;
        logger.debug("************* TIME Taken  ************ {} secs", difference);
        response.setMergeProcessResponse("Merge Successful and File Uploaded");
        response.setNumberOfPdfs(numberOfPdfs);
        response.setBatchId(processorData.getBatchId());
        response.setLocalDownload(processorData.getLocalDownload());
        response.setActivityCode(distributionRequest.getActivityCode());
        response.getSchools().addAll(schoolsForLabels);
        response.setStudentSearchRequest(searchRequest);
        processorData.setDistributionResponse(response);
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
                if (EducDistributionApiConstants.TRANSMISSION_MODE_FTP.equalsIgnoreCase(processorData.getTransmissionMode())) {
                    processStudentsForCSVs(scdList, psiCode, processorData);
                } else {
                    processStudentsForPDFs(processorData.getBatchId(), scdList, locations);
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

    private void processStudentsForPDFs(Long batchId, List<PsiCredentialDistribution> scdList, List<InputStream> locations) throws IOException {
        int currentTranscript = 0;
        int failedToAdd = 0;

        for (PsiCredentialDistribution scd : scdList) {
            int result = 0;
            if(scd.getStudentID() != null) {
                result = addStudentTranscriptToLocations(scd.getStudentID().toString(), locations);
            }
            if(result == 0) {
                failedToAdd++;
                logger.debug("*** Failed to Add PDFs {} Current student {} in batch {}", failedToAdd, scd.getStudentID(), batchId);
            } else {
                currentTranscript++;
                logger.debug("*** Added PSI PDFs {}/{} Current student {}", currentTranscript, scdList.size(), scd.getStudentID());
            }
        }
    }

    //Grad2-1931 : Writes students transcripts data on CSV and formatting them - mchintha
    private void processStudentsForCSVs(List<PsiCredentialDistribution> scdList, String psiCode, ProcessorData processorData) throws IOException {
        int currentTranscript = 0;
        int failedToAdd = 0;
        String[] schoolInfo;
        String csv;
        Path path;
        File newFile;
        List<String[]> studentTranscriptdata = null;
        List<String> updatedStudentTranscriptdataList = new ArrayList<>();
        CsvMapper csvMapper = new CsvMapper();

        try {
            psiCode = StringUtils.trim(psiCode);
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
                        GradProgram gradProgram = transcriptCsv.getGradProgram();
                        List<NonGradReason> nonGR = transcriptCsv.getNonGradReasons();
                        School schoolDetails = transcriptCsv.getSchool();
                        List<TranscriptResult> courseDetails = (transcriptCsv.getTranscript() != null ? transcriptCsv.getTranscript().getResults() : null);

                        //Writes the A's row's data on CSV
                        writesCsvFileRowA(studentTranscriptdata, scd.getPen(), studentDetails, gradProgram, nonGR);

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
                assert studentTranscriptdata != null;
                for (String[] studentData : studentTranscriptdata) {
                    String stringArrayAsString = String.join("", studentData);
                    updatedStudentTranscriptdataList.add(stringArrayAsString);
                }
            }
            //Grad2-2182 sorting data by PEN - mchintha
            Collections.sort(updatedStudentTranscriptdataList);

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
        String[] nonExaminableCoursesInfo;
        if (courseDetails != null) {
            for (TranscriptResult course : courseDetails) {
                String usedForGrad = getUsedForGrad(course);
                String courseType = getCourseType(course);
                String gradReqtType = getGradReqtType(course);
                Integer courseOriginalCredits = course.getCourse().getOriginalCredits() == null ? 0 : course.getCourse().getOriginalCredits();
                Integer credits = course.getCourse().getCredit() == null ? 0 : course.getCourse().getCredit();

                //D rows writes only Non-Examinable Courses
                if (courseType.equals("2")) {

                    nonExaminableCoursesInfo = new String[]{
                            pen,
                            EducDistributionApiConstants.LETTER_D,
                            (course.getCourse().getCode() == null || course.getCourse().getCode().isBlank()) ? "" : course.getCourse().getCode(),
                            (course.getCourse().getLevel() == null || course.getCourse().getLevel().isBlank()) ? "" : course.getCourse().getLevel(),
                            (course.getCourse().getSessionDate() != null || StringUtils.isNotBlank(course.getCourse().getSessionDate())) ? course.getCourse().getSessionDate() : "",
                            //(course.getMark().getInterimLetterGrade() == null || course.getMark().getInterimLetterGrade().isBlank()) ? "" : course.getMark().getInterimLetterGrade(),
                            "",
                            (course.getMark().getFinalLetterGrade() == null || course.getMark().getFinalLetterGrade().isBlank()) ? "" : course.getMark().getFinalLetterGrade(),
                            //(course.getMark().getInterimPercent() == null || StringUtils.isBlank(course.getMark().getInterimPercent())) ? EducDistributionApiConstants.THREE_ZEROES : String.format("%03d", extractNumericValue(course.getMark().getInterimPercent())),
                            EducDistributionApiConstants.THREE_ZEROES,
                            (course.getMark().getFinalPercent() == null || StringUtils.isBlank(course.getMark().getFinalPercent())) ? EducDistributionApiConstants.THREE_ZEROES : String.format("%03d", extractNumericValue(course.getMark().getFinalPercent())),
                            (course.getCourse().getCredits() == null || StringUtils.isBlank(course.getCourse().getCredits())) ? EducDistributionApiConstants.TWO_ZEROES : String.format("%02d", extractNumericValue(course.getCourse().getCredits())),
                            (course.getCourse().getRelatedCourse() == null || course.getCourse().getRelatedCourse().isBlank()) ? "" : course.getCourse().getRelatedCourse(),
                            (course.getCourse().getRelatedLevel() == null || course.getCourse().getRelatedLevel().isBlank()) ? "" : course.getCourse().getRelatedLevel(),
                            (course.getCourse().getCustomizedCourseName() == null || course.getCourse().getCustomizedCourseName().isBlank()) ? "" : course.getCourse().getCustomizedCourseName(),
                            (course.getEquivalency() == null || course.getEquivalency().isBlank()) ? "" : course.getEquivalency(),
                            gradReqtType,
                            courseOriginalCredits > credits ? "Y" : "",// partial flag
                            extractNumericValue(usedForGrad) > 0 ? EducDistributionApiConstants.LETTER_Y : ""
                    };

                    setColumnsWidths(nonExaminableCoursesInfo,
                            IntStream.of(10, 1, 5, 3, 6, 2, 2, 3, 3, 2, 5, 3, 40, 1, 1, 1, 1).toArray(),
                            studentTranscriptdata);

                }
            }
        }
    }

    private static String getCourseType(TranscriptResult course) {
        return (course.getCourse().getType() == null || course.getCourse().getType().isBlank()) ? "" : course.getCourse().getType();
    }

    private static String getUsedForGrad(TranscriptResult course) {
        return (course.getUsedForGrad() == null || course.getUsedForGrad().isBlank()) ? "" : course.getUsedForGrad();
    }

    private static String getGradReqtType(TranscriptResult course) {
        String fineArtsAppliedSkills = course.getCourse().getFineArtsAppliedSkills();
        return (fineArtsAppliedSkills == null || StringUtils.isBlank(fineArtsAppliedSkills)) ? "" : fineArtsAppliedSkills;
    }

    //Grad2-1931 : Writes Row C's data on CSV - mchintha
    private void writesCsvFileRowC(List<String[]> studentTranscriptdata, String pen, List<TranscriptResult> courseDetails) {
        String[] examinableCoursesAndAssessmentsInfo;
        String used;
        String finalLetterGrade;
        String finalPercent;

        if (courseDetails != null) {
            for (TranscriptResult course : courseDetails) {
                String courseType = getCourseType(course);


                //C rows writes Examinable Courses and Assessments
                if (courseType.equals("1") || courseType.equals("3")) {
                    String credits;
                    Double proficiencyScore = course.getCourse().getProficiencyScore() == null || Double.isNaN(course.getCourse().getProficiencyScore()) ? 0.0 : course.getCourse().getProficiencyScore();

                    boolean assessmentsConditionTrue = isAssessmentsConditionTrue(course);
                    //Grad2-2182 setting used for grad as per coursetype is assessments - mchintha
                    //Used for Grad and final percentage
                    Pair<String, String> result = calculateUsedAndFinalPercent(course, assessmentsConditionTrue, proficiencyScore);

                    //Final letter Grade and final percent for assessements LTE10 and LTP10
                    if(assessmentsConditionTrue) {
                        finalLetterGrade = (proficiencyScore > 0.0) ? "RM" : "";
                    } else {
                        finalLetterGrade = (course.getMark().getFinalLetterGrade() == null || course.getMark().getFinalLetterGrade().isBlank()) ? "" : course.getMark().getFinalLetterGrade();
                    }


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
                            String.format("%03d", extractNumericValue(result.getRight())), //finalPercent
                            finalLetterGrade.equalsIgnoreCase("NA") ? "" : finalLetterGrade,
                            (course.getMark().getInterimPercent() == null || StringUtils.isBlank(course.getMark().getInterimPercent())) ? EducDistributionApiConstants.THREE_ZEROES : String.format("%03d", extractNumericValue(course.getMark().getInterimPercent())),
                            (course.getCourse().getCredits() == null || StringUtils.isBlank(course.getCourse().getCredits())) ? EducDistributionApiConstants.TWO_ZEROES : String.format("%02d", extractNumericValue(course.getCourse().getCredits())),
                            "", //Course case
                            result.getLeft()//used
                            //extractNumericValue(used) > 0 ? EducDistributionApiConstants.LETTER_Y : ""
                    };

                    setColumnsWidths(examinableCoursesAndAssessmentsInfo,
                            IntStream.of(10, 1, 5, 3, 6, 2, 1, 3, 1, 3, 3, 2, 3, 2, 1, 1).toArray(),
                            studentTranscriptdata);

                }
            }
        }
    }

    private static Pair<String, String> calculateUsedAndFinalPercent(TranscriptResult course, boolean assessmentsConditionTrue, Double proficiencyScore) {

        String credits;
        String used;
        String finalPercent;
        DecimalFormat decimalFormat = new DecimalFormat("#");
        String courseType = getCourseType(course);
        if (courseType.equals("3")) {
            credits = course.getCourse().getUsed() == null ? "" : String.valueOf(course.getCourse().getUsed());
            used = (credits != null && credits.equalsIgnoreCase("true")) ? EducDistributionApiConstants.LETTER_Y : "";
            finalPercent = assessmentsConditionTrue ? EducDistributionApiConstants.THREE_ZEROES : decimalFormat.format(proficiencyScore);
        } else {
            credits = (course.getUsedForGrad() == null || course.getUsedForGrad().isBlank()) ? "" : course.getUsedForGrad();
            used = extractNumericValue(credits) > 0 ? EducDistributionApiConstants.LETTER_Y : "";
            String completedCoursePercentage = course.getMark().getCompletedCoursePercentage() == null ? EducDistributionApiConstants.THREE_ZEROES : decimalFormat.format(course.getMark().getCompletedCoursePercentage());
            finalPercent = assessmentsConditionTrue ? EducDistributionApiConstants.THREE_ZEROES : completedCoursePercentage;
        }
        return Pair.of(used, finalPercent);
    }
    private static boolean isAssessmentsConditionTrue(TranscriptResult course) {
        return course.getCourse().getCode() == null || StringUtils.isBlank(course.getCourse().getCode()) ?
                false :
                (course.getCourse().getCode().equalsIgnoreCase(EducDistributionApiConstants.ASSESSMENT_LTE) || course.getCourse().getCode().equalsIgnoreCase(EducDistributionApiConstants.ASSESSMENT_LTP));
    }

    //Grad2-1931 : Writes Row A's data on CSV - mchintha
    private void writesCsvFileRowA(List<String[]> studentTranscriptdata, String pen, Student studentDetails, GradProgram gradProgram, List<NonGradReason> nonGR) {
        String[] studentInfo;

        //Writes the A's row's data on CSV
        if (studentDetails != null) {

            String dogWoodFlag = String.valueOf(studentDetails.getGraduationData().getDogwoodFlag()).isBlank() ? "" : String.valueOf(studentDetails.getGraduationData().getDogwoodFlag());

            String honorsFlag = String.valueOf(studentDetails.getGraduationData().getHonorsFlag()).isBlank() ? "" : String.valueOf(studentDetails.getGraduationData().getHonorsFlag());

            //Optional or Career program codes
            List<String> optionalOrCareerProgramCodes = studentDetails.getGraduationData().getProgramCodes();
            int programCodesListSize = optionalOrCareerProgramCodes != null ? optionalOrCareerProgramCodes.size() : 0;

            studentInfo = new String[]{
                        pen,
                        EducDistributionApiConstants.LETTER_A,
                        (studentDetails.getLastName() == null || StringUtils.isBlank(studentDetails.getLastName())) ? "" : studentDetails.getLastName(),
                        (studentDetails.getFirstName() == null || StringUtils.isBlank(studentDetails.getFirstName())) ? "" : studentDetails.getFirstName(),
                        (studentDetails.getMiddleName() == null || StringUtils.isBlank(studentDetails.getMiddleName())) ? "" : studentDetails.getMiddleName(),
                        getBirthDate(studentDetails),
                        (studentDetails.getGender() == null || StringUtils.isBlank(studentDetails.getGender())) ? "" : studentDetails.getGender(),
                        (studentDetails.getCitizenship() == null || StringUtils.isBlank(studentDetails.getCitizenship())) ? "" : studentDetails.getCitizenship(),
                        (studentDetails.getGrade() == null || StringUtils.isBlank(studentDetails.getGrade())) ? "" : studentDetails.getGrade(),
                        (studentDetails.getGraduationStatus().getSchoolOfRecord() == null || StringUtils.isBlank(studentDetails.getGraduationStatus().getSchoolOfRecord())) ? "" : studentDetails.getGraduationStatus().getSchoolOfRecord(),
                        (studentDetails.getLocalId() == null || StringUtils.isBlank(studentDetails.getLocalId())) ? "" : studentDetails.getLocalId(),
                        programCodesListSize >= EducDistributionApiConstants.NUMBER_ONE ? studentDetails.getGraduationData().getProgramCodes().get(0) : "",
                        (studentDetails.getConsumerEducReqt() == null || StringUtils.isBlank(studentDetails.getConsumerEducReqt())) ? "N" : studentDetails.getConsumerEducReqt(),
                        EducDistributionApiConstants.FOUR_ZEROES,
                        getProgramCompleteionDate(studentDetails),
                        dogWoodFlag.equals("false") ? EducDistributionApiConstants.LETTER_N : EducDistributionApiConstants.LETTER_Y,
                        honorsFlag.equals("false") ? EducDistributionApiConstants.LETTER_N : EducDistributionApiConstants.LETTER_Y,
                        getNonGradReasons(nonGR),
                        programCodesListSize >= EducDistributionApiConstants.NUMBER_TWO ? studentDetails.getGraduationData().getProgramCodes().get(1) : "",
                        programCodesListSize >= EducDistributionApiConstants.NUMBER_THREE ? studentDetails.getGraduationData().getProgramCodes().get(2) : "",
                        programCodesListSize >= EducDistributionApiConstants.NUMBER_FOUR ? studentDetails.getGraduationData().getProgramCodes().get(3) : "",
                        programCodesListSize >= EducDistributionApiConstants.NUMBER_FIVE ? studentDetails.getGraduationData().getProgramCodes().get(4) : "",
                        "", //10 Blanks
                        (gradProgram == null || StringUtils.isBlank(gradProgram.getCode().getCode()) ? "" : gradProgram.getCode().getCode().substring(0, 4))
                };

                setColumnsWidths(
                        studentInfo,
                        IntStream.of(10, 1, 25, 25, 25, 8, 1, 1, 2, 8, 12, 2, 1, 4, 6, 1, 1, 15, 2, 2, 2, 2, 10, 4).toArray(),
                        studentTranscriptdata);
            }

    }

    private static String getNonGradReasons(List<NonGradReason> nonGR) {
        return ((nonGR == null) || nonGR.isEmpty()) ? "" : nonGR.stream()
                .map(NonGradReason::getCode)
                .filter(Objects::nonNull)
                .limit(15)
                .collect(Collectors.joining(""));
    }

    private static String getProgramCompleteionDate(Student studentDetails) {
        String programCompleteionDate;
        DateTimeFormatter formatDateYYYYMM = DateTimeFormatter.ofPattern(EducDistributionApiConstants.DATE_FORMAT_YYYYMM);
        if(studentDetails.getGraduationStatus().getProgramCompletionDate() == null || StringUtils.isBlank(studentDetails.getGraduationStatus().getProgramCompletionDate().toString()))
        {
            programCompleteionDate = EducDistributionApiConstants.SIX_ZEROES;
        } else {
            programCompleteionDate = studentDetails.getGraduationStatus().getProgramCompletionDate().format(formatDateYYYYMM);
        }
        return programCompleteionDate;
    }

    private static String getBirthDate(Student studentDetails) {
        String birthDate;
        DateTimeFormatter formatDateYYYYMMDD = DateTimeFormatter.ofPattern(EducDistributionApiConstants.DATE_FORMAT_YYYYMMDD);
        if (studentDetails.getBirthdate() == null || StringUtils.isBlank(studentDetails.getBirthdate().toString())) {
            birthDate = "";
        } else {
            birthDate = studentDetails.getBirthdate().format(formatDateYYYYMMDD);
        }
        return birthDate;
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
    private static int extractNumericValue(String val) {
        return val.chars()
                .filter(Character::isDigit)
                .reduce(0, (a, b) -> a * 10 + Character.getNumericValue(b));

    }

    //Grad2-2052 - setting SFTP root folder location for PSIRUN paper where it has to pick zip folders from, to send them to BC mail - mchintha
    @Override
    protected String getRootPathForFilesStorage(ProcessorData processorData) {
        String transmissionMode = StringUtils.upperCase(processorData.getTransmissionMode());
        if (StringUtils.isBlank(transmissionMode)) {
            throw new GradBusinessRuleException(TRANSMISSION_MODE_ERROR);
        }
        String rootPath = EducDistributionApiConstants.TMP_DIR + EducDistributionApiConstants.FILES_FOLDER_STRUCTURE + transmissionMode;
        logger.debug("getZipFolderFromRootLocation {} transmission mode {}", rootPath, transmissionMode);
        return rootPath;
    }
}
