package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.service.GraduationService;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils;
import ca.bc.gov.educ.api.distribution.util.IOUtils;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;
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
import java.util.Arrays;
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
                if (processorData.getTransmissionMode().equalsIgnoreCase("FTP")) {
                    processStudentsForCSVs(scdList, psiCode, processorData);
                } else {
                    processStudentsForPDFs(scdList, locations, processorData);
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


    private void processStudentsForPDFs(List<PsiCredentialDistribution> scdList, List<InputStream> locations, ProcessorData processorData) throws IOException {
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
        String[] studentInfo = null;
        String[] schoolInfo = null;
        String[] examinableCoursesAndAssessmentsInfo = null;
        String[] nonExaminableCoursesInfo = null;
        String csv;
        Path path;
        File newFile = null;
        List<String[]> studentTranscriptdata = new ArrayList<>();
        List<String> updatedStudentTranscriptdataList = new ArrayList<>();
        CsvMapper csvMapper = new CsvMapper();
        try {
            StringBuilder filePathBuilder = IOUtils.createFolderStructureInTempDirectory(processorData, psiCode, "02");

            filePathBuilder = filePathBuilder.append("/GRAD_INT").append(psiCode).append("_RESULTS").append(".").append(EducDistributionApiUtils.getFileName()).append(".DAT");

            if (filePathBuilder != null) {
                path = Paths.get(filePathBuilder.toString());
                newFile = new File(Files.createFile(path).toUri());
            } else {
                throw new IOException("Path is not available to create DAT file to write the student data");
            }
            for (PsiCredentialDistribution scd : scdList) {

                ReportData transcriptCsv = graduationService.getReportData(scd.getPen());

                if (transcriptCsv != null) {
                    Student studentDetails = transcriptCsv.getStudent();
                    School schoolDetails = transcriptCsv.getSchool();
                    List<TranscriptResult> courseDetails = (transcriptCsv.getTranscript() != null ? transcriptCsv.getTranscript().getResults() : null);


                    //Writes the A's row's data on CSV
                    if (studentDetails != null) {
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(educDistributionApiConstants.DATE_FORMAT);
                        String studentBirthDate = simpleDateFormat.format(studentDetails.getBirthdate());
                        studentInfo = new String[]{
                                scd.getPen(),
                                "A",
                                studentDetails.getLastName(),
                                studentDetails.getFirstName(),
                                studentDetails.getMiddleName(),
                                studentDetails.getBirthdate() == null ? "" : (StringUtils.isNotBlank(studentBirthDate) ? studentBirthDate : ""),
                                studentDetails.getGender(), studentDetails.getCitizenship(),
                                studentDetails.getGrade(), studentDetails.getGraduationStatus().getSchoolOfRecord(), studentDetails.getLocalId(),
                                "", //Optional program
                                studentDetails.getConsumerEducReqt(),
                                "0000",
                                StringUtils.isNotBlank(studentDetails.getGraduationStatus().getProgramCompletionDate()) ? studentDetails.getGraduationStatus().getProgramCompletionDate() : "000000",
                                String.valueOf(studentDetails.getGraduationData().getDogwoodFlag()).equals("false") ? "N" : "Y",
                                String.valueOf(studentDetails.getGraduationData().getHonorsFlag()).equals("false") ? "N" : "Y",
                                "", //Non grad reasons
                                "", //18 Blanks
                                studentDetails.getGradProgram() == null ? "" : studentDetails.getGradProgram().substring(0,3)};

                        setColumnsWidths(
                                studentInfo,
                                IntStream.of(10, 1, 25, 25, 25, 8, 1, 1, 2, 8, 12, 2, 1, 4, 6, 1, 1, 15, 18, 4).toArray(),
                                studentTranscriptdata);
                    }

                    //Writes the B's row's data on CSV
                    if (schoolDetails != null) {
                        schoolInfo = new String[]{
                                scd.getPen(), "B",
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
                    if (courseDetails != null) {
                        for (TranscriptResult course : courseDetails) {
                            String creditsString = course.getUsedForGrad();
                            int credits = creditsString.chars()
                                    .filter(Character::isDigit)
                                    .reduce(0, (a, b) -> a * 10 + Character.getNumericValue(b));
                            //C rows writes Examinable Courses and Assessments
                            if (course.getCourse().getType().equals("1") || course.getCourse().getType().equals("3")) {
                                try {
                                    examinableCoursesAndAssessmentsInfo = new String[]{
                                            scd.getPen(), "C", course.getCourse().getCode(),
                                            course.getCourse().getLevel(),
                                            course.getCourse().getSessionDate(),
                                            course.getMark().getInterimLetterGrade(),
                                            "", //IB flag
                                            StringUtils.isBlank(course.getMark().getSchoolPercent()) ? "000" : String.format("%03d", Integer.parseInt(course.getMark().getSchoolPercent())) ,
                                            "", //Special case
                                            StringUtils.isBlank(course.getMark().getExamPercent()) ? "000" : String.format("%03d", Integer.parseInt(course.getMark().getExamPercent())),
                                            StringUtils.isBlank(course.getMark().getFinalPercent()) ? "000" : String.format("%03d", Integer.parseInt(course.getMark().getFinalPercent())),
                                            course.getMark().getFinalLetterGrade() ,
                                            StringUtils.isBlank(course.getMark().getInterimPercent()) ? "000" : String.format("%03d", Integer.parseInt(course.getMark().getInterimPercent())),
                                            StringUtils.isBlank(course.getCourse().getCredits()) ? "00" : String.format("%02d", Integer.parseInt(course.getCourse().getCredits())),
                                            "", //Course case
                                            credits > 0 ? "Y" : ""
                                    };

                                    setColumnsWidths(examinableCoursesAndAssessmentsInfo,
                                            IntStream.of(10, 1, 5, 3, 6, 2, 1, 3, 1, 3, 3, 2, 3, 2, 1, 1).toArray(),
                                            studentTranscriptdata);
                                }
                                catch (Exception e) {
                                    logger.error(EXCEPTION, e.getLocalizedMessage());
                                }
                            }
                        }
                    }

                    //Writes D's rows data on CSV
                    if (courseDetails != null) {
                        for (TranscriptResult course : courseDetails) {
                            //D rows writes only Non-Examinable Courses
                            if (course.getCourse().getType().equals("2")) {
                                String creditsString = course.getUsedForGrad();
                                int credits = creditsString.chars()
                                        .filter(Character::isDigit)
                                        .reduce(0, (a, b) -> a * 10 + Character.getNumericValue(b));

                                try {
                                nonExaminableCoursesInfo = new String[]{
                                        scd.getPen(),
                                        "D",
                                        course.getCourse().getCode(),
                                        course.getCourse().getLevel(),
                                        StringUtils.isNotBlank(course.getCourse().getSessionDate()) ? course.getCourse().getSessionDate() : "",
                                        course.getMark().getInterimLetterGrade(),
                                        course.getMark().getFinalLetterGrade(),
                                        StringUtils.isBlank(course.getMark().getInterimPercent()) ? "000" : String.format("%03d", Integer.parseInt(course.getMark().getInterimPercent())),
                                        StringUtils.isBlank(course.getMark().getFinalPercent()) ? "000" : String.format("%03d", Integer.parseInt(course.getMark().getFinalPercent())),
                                        StringUtils.isBlank(course.getCourse().getCredits()) ? "00" : String.format("%02d", Integer.parseInt(course.getCourse().getCredits())),
                                        course.getCourse().getRelatedCourse(),
                                        course.getCourse().getRelatedLevel(),
                                        course.getCourse().getName(),
                                        course.getEquivalency(),
                                        course.getRequirement(),
                                        "",// partial flag
                                        credits > 0 ? "Y" : ""
                                };

                                setColumnsWidths(nonExaminableCoursesInfo,
                                        IntStream.of(10, 1, 5, 3, 6, 2, 2, 3, 3, 2, 5, 3, 40, 1, 1, 1, 1).toArray(),
                                        studentTranscriptdata);
                            }
                                catch (Exception e) {
                                    logger.error(EXCEPTION, e.getLocalizedMessage());
                            }
                            }
                        }
                    }

                    currentTranscript++;
                    logger.debug("*** Added csv {}/{} Current student {}", currentTranscript, scdList.size(), scd.getPen());
                } else {
                    failedToAdd++;
                    logger.debug("*** Failed to Add {} Current student {}", failedToAdd, scd.getPen());
                }

            }
            for (String[] studentData : studentTranscriptdata) {
                String listAsString = String.join("", studentData);
                updatedStudentTranscriptdataList.add(listAsString);
            }

            csv = csvMapper.writeValueAsString(updatedStudentTranscriptdataList);
            csv = csv.replaceAll("\"", "").replaceAll(",", "\r\n");
            try (FileWriter fWriter = new FileWriter(newFile)) {
                fWriter.write(csv);
            } catch (IOException e) {
                logger.error(EXCEPTION, e.getLocalizedMessage());
            }

        } catch (Exception e) {
            logger.error(EXCEPTION, e.getLocalizedMessage());
        }

    }

    private void setColumnsWidths(String[] allCSVRowsInfo, int[] eachRowsColumnsWidths, List<String[]> studentTranscriptdata) {
        for (int i = 0; i < allCSVRowsInfo.length; i++) {
            String formattedString = String.format("%-" + eachRowsColumnsWidths[i] + "s", (allCSVRowsInfo[i] != null ? allCSVRowsInfo[i] : ""));
            allCSVRowsInfo[i] = formattedString;
        }
        studentTranscriptdata.add(allCSVRowsInfo);
    }

}
