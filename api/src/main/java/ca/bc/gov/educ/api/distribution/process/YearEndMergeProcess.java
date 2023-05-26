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

import static ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils.getDistrictCodeFromMincode;

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
        DistributionRequest distributionRequest = processorData.getDistributionRequest();
        response.setBatchId(processorData.getBatchId());
        response.setLocalDownload(processorData.getLocalDownload());
        response.setTotalCyclesCount(distributionRequest.getTotalCyclesCount());
        response.setActivityCode(processorData.getActivityCode());
        Long batchId = processorData.getBatchId();
        Map<String, DistributionPrintRequest> mapDist = distributionRequest.getMapDist();
        int numberOfPdfs = 0;
        int schoolCounter = 0;
        int numberOfCreatedSchoolReports = 0;
        int numberOfProcessedSchoolReports = 0;
        int numberOfCreatedSchoolLabelReports = 0;
        List<School> schoolsForLabels = distributionRequest.getSchools();
        List<School> districtsForLabels = new ArrayList<>();
        for (String mincode : mapDist.keySet()) {

            String distcode = getDistrictCodeFromMincode(mincode);
            processDistrictsForLabels(districtsForLabels, distcode, exception);

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

                ReportRequest packSlipReq = reportService.preparePackingSlipData(getBaseSchoolDetails(distributionPrintRequest, mincode, exception), processorData.getBatchId());
                Pair<Integer, Integer> pV = processTranscriptPrintRequest(distributionPrintRequest, currentSlipCount, packSlipReq, studListNonGrad, processorData, mincode, schoolCategoryCode, numberOfPdfs);
                numberOfPdfs += pV.getRight();

                if (!studListNonGrad.isEmpty() && NONGRADDIST.equalsIgnoreCase(processorData.getActivityCode())) {
                    logger.debug("***** Create Student NonGrad School Reports *****");
                    numberOfCreatedSchoolReports += createAndSaveNonGradReport(commonSchool, studListNonGrad, mincode);
                    logger.debug("***** Number of Student NonGrad School Reports Created {} *****", numberOfCreatedSchoolReports);
                    logger.debug("***** Distribute Student NonGrad School Reports *****");
                    List<String> mincodes = new ArrayList<>();
                    mincodes.add(mincode);
                    numberOfProcessedSchoolReports += processDistrictSchoolDistribution(processorData.getBatchId(), mincodes, null, null, NONGRADDISTREP_SC);
                    logger.debug("***** Number of distributed Student NonGrad School Reports {} *****", numberOfProcessedSchoolReports);
                }
                logger.debug("PDFs Merged {}", commonSchool.getSchoolName());
            }
        }
        response.setTranscriptResponse(numberOfPdfs + " transcripts have been processed in batch " + processorData.getBatchId());
        logger.debug("***** Create and Store district labels reports *****");
        for(School sch: districtsForLabels) {
            List<School> districts = new ArrayList<>();
            districts.add(sch);
            numberOfCreatedSchoolLabelReports += createSchoolLabelsReport(districts, ADDRESS_LABEL_YE);
        }
        logger.debug("***** Number of created district labels reports {} *****", numberOfCreatedSchoolLabelReports);
        logger.debug("***** Distribute District Label reports *****");
        List<String> mincodes = districtsForLabels.stream().map(s->s.getMincode()).toList();
        numberOfProcessedSchoolReports += processDistrictSchoolDistribution(batchId, mincodes, ADDRESS_LABEL_YE, null, null);
        logger.debug("***** Number of distributed District Label reports {} *****", numberOfProcessedSchoolReports);

        if(!schoolsForLabels.isEmpty()) {
            logger.debug("***** Create and Store school labels reports *****");
            numberOfCreatedSchoolLabelReports += createSchoolLabelsReport(schoolsForLabels, ADDRESS_LABEL_SCHL);
            logger.debug("***** Number of created district labels reports {} *****", numberOfCreatedSchoolLabelReports);
            logger.debug("***** Distribute School Label reports *****");
            numberOfProcessedSchoolReports += processSchoolLabelsDistribution(batchId, ADDRESS_LABEL_SCHL);
            logger.debug("***** Number of distributed School Label reports {} *****", numberOfProcessedSchoolReports);
        }

        numberOfPdfs += numberOfProcessedSchoolReports;
        postingDistributionService.postingProcess(processorData.getBatchId(), processorData.getLocalDownload(), processorData.getActivityCode(), numberOfPdfs);
        long endTime = System.currentTimeMillis();
        long diff = (endTime - startTime) / 1000;
        logger.debug("************* TIME Taken  ************ {} secs", diff);
        response.setMergeProcessResponse("Merge Successful and File Uploaded");
        response.setNumberOfPdfs(numberOfPdfs);
        response.setProcessedCyclesCount(distributionRequest.getProcessedCyclesCount());
        response.getSchools().addAll(schoolsForLabels);
        processorData.setDistributionResponse(response);
        return processorData;
    }
}
