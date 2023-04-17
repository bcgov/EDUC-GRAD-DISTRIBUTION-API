package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.internal.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils.*;

@Data
@Component
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class YearEndMergeProcess extends MergeProcess {

    private static Logger logger = LoggerFactory.getLogger(YearEndMergeProcess.class);

    @Override
    public ProcessorData fire(ProcessorData processorData) {
        long startTime = System.currentTimeMillis();
        logger.debug("************* TIME START  ************ {}", startTime);
        DistributionResponse response = new DistributionResponse();
        ExceptionMessage exception = new ExceptionMessage();
        Map<String, DistributionPrintRequest> mapDist = processorData.getMapDistribution();
        int numberOfPdfs = 0;
        int schoolCounter = 0;
        List<School> schoolsForLabels = new ArrayList<>();
        List<School> districtsForLabels = new ArrayList<>();
        for (String mincode : mapDist.keySet()) {

            if(districtsForLabels.isEmpty()) {
                String distcode = getDistrictCodeFromMincode(mincode);
                processDistrictsForLabels(districtsForLabels, distcode, restUtils.getAccessToken(), exception);
            }

            //TODO: TEST CODE - REMOVE
            /*********** TEST CODE - REMOVE **********/
            if(schoolCounter >= 5) break;
            /*********** TEST CODE - REMOVE **********/

            CommonSchool commonSchool = getBaseSchoolDetails(null, mincode, exception);
            if (commonSchool != null) {
                int currentSlipCount = 0;
                schoolCounter++;
                String schoolCategoryCode = commonSchool.getSchoolCategoryCode();
                logger.debug("*** School Details Acquired {} category {}", mincode, schoolCategoryCode);
                if("02".equals(schoolCategoryCode)) {
                    processSchoolsForLabels(schoolsForLabels, mincode, restUtils.getAccessToken(), exception);
                }
                logger.debug("School {}/{}", schoolCounter, mapDist.size());
                List<Student> studListNonGrad = new ArrayList<>();

                DistributionPrintRequest distributionPrintRequest = mapDist.get(mincode);

                //TODO: TEST CODE - REMOVE
                /*********** TEST CODE - REMOVE **********/
                List<StudentCredentialDistribution> studentList = distributionPrintRequest.getTranscriptPrintRequest().getTranscriptList();
                distributionPrintRequest.getTranscriptPrintRequest().setTranscriptList(studentList.subList(0,
                        new Random().ints(0, Math.min(5, studentList.size())).findFirst().getAsInt()));
                /*********** TEST CODE - REMOVE **********/

                ReportRequest packSlipReq = reportService.preparePackingSlipData(getBaseSchoolDetails(distributionPrintRequest, mincode, exception), processorData.getBatchId());
                Pair<Integer, Integer> pV = processTranscriptPrintRequest(distributionPrintRequest, currentSlipCount, packSlipReq, studListNonGrad, processorData, mincode, schoolCategoryCode, numberOfPdfs);
                numberOfPdfs += pV.getRight();

                if (!studListNonGrad.isEmpty()) {
                    createAndSaveNonGradReport(commonSchool, studListNonGrad, mincode);
                }
                logger.debug("PDFs Merged {}", commonSchool.getSchoolName());
            }
        }
        response.setTranscriptResponse(numberOfPdfs + " transcripts have been processed in batch " + processorData.getBatchId());
        int numberOfCreatedSchoolReports = 0;
        int numberOfProcessedSchoolReports = 0;
        int numberOfCreatedSchoolLabelReports = 0;
        if (YEARENDDIST.equalsIgnoreCase(processorData.getActivityCode())) {
            logger.debug("***** Create and Store Year End school reports *****");
            numberOfCreatedSchoolReports += createDistrictSchoolYearEndReport(ADDRESS_LABEL_YE, DISTREP_YE_SD, DISTREP_YE_SC);
            logger.debug("***** Number of created Year End school reports {} *****", numberOfCreatedSchoolReports);
            logger.debug("***** Distribute Year End school reports *****");
            numberOfProcessedSchoolReports += processDistrictSchoolDistribution(processorData, ADDRESS_LABEL_YE, DISTREP_YE_SD, DISTREP_YE_SC);
            logger.debug("***** Number of distributed Year End school reports {} *****", numberOfProcessedSchoolReports);
        }
        if (NONGRADDIST.equalsIgnoreCase(processorData.getActivityCode())) {
//            logger.debug("***** Create and Store school labels reports *****");
//            numberOfCreatedSchoolLabelReports += createSchoolLabelsReport(schoolsForLabels, ADDRESS_LABEL_SCHL);
//            logger.debug("***** Number of created school labels reports {} *****", numberOfCreatedSchoolLabelReports);
            logger.debug("***** Create and Store district labels reports *****");
             numberOfCreatedSchoolLabelReports += createSchoolLabelsReport(districtsForLabels, ADDRESS_LABEL_YE);
            logger.debug("***** Number of created district labels reports {} *****", numberOfCreatedSchoolLabelReports);
            logger.debug("***** Distribute Student NonGrad School Reports *****");
            numberOfProcessedSchoolReports += processDistrictSchoolDistribution(processorData, null, null, NONGRADDISTREP_SC);
            logger.debug("***** Number of distributed Student NonGrad School Reports {} *****", numberOfProcessedSchoolReports);
            logger.debug("***** Distribute District Labels Reports *****");
            numberOfProcessedSchoolReports += processDistrictSchoolDistribution(processorData, ADDRESS_LABEL_YE, null, null);
            logger.debug("***** Number of distributed District Labels Reports {} *****", numberOfProcessedSchoolReports);
        }
        numberOfPdfs += numberOfProcessedSchoolReports;
        long endTime = System.currentTimeMillis();
        long diff = (endTime - startTime) / 1000;
        logger.debug("************* TIME Taken  ************ {} secs", diff);
        response.setMergeProcessResponse("Merge Successful and File Uploaded");
        response.setNumberOfPdfs(numberOfPdfs);
        response.getSchools().addAll(schoolsForLabels);
        processorData.setDistributionResponse(response);
        return processorData;
    }
}
