package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.internal.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        DistributionRequest distributionRequest = processorData.getDistributionRequest();
        Long batchId = processorData.getBatchId();
        Map<String, DistributionPrintRequest> mapDist = distributionRequest.getMapDist();
        StudentSearchRequest searchRequest = distributionRequest.getStudentSearchRequest();
        int numberOfPdfs = 0;
        int schoolCounter = 0;
        int numberOfCreatedSchoolReports = 0;
        int numberOfProcessedSchoolReports = 0;
        int numberOfCreatedSchoolLabelReports = 0;
        List<School> schoolsForLabels = new ArrayList<>();
        List<School> districtsForLabels = new ArrayList<>();
        for (String mincode : mapDist.keySet()) {
            CommonSchool commonSchool = getBaseSchoolDetails(null, mincode, exception);
            if (commonSchool != null) {
                int currentSlipCount = 0;
                schoolCounter++;
                String schoolCategoryCode = commonSchool.getSchoolCategoryCode();

                logger.debug("*** School Details Acquired {} category {}", mincode, schoolCategoryCode);
                if(StringUtils.containsAnyIgnoreCase(schoolCategoryCode, "02", "03", "09")) {
                    processSchoolsForLabels(schoolsForLabels, mincode, exception);
                    logger.debug("Added Independent School {} for processing", commonSchool.getSchoolName());
                } else {
                    // GRAD2-2269: no district for 02,03,09 school category
                    String distcode = getDistrictCodeFromMincode(mincode);
                    processDistrictsForLabels(districtsForLabels, distcode, exception);
                }
                logger.debug("{} School {}/{}", mincode, schoolCounter, mapDist.size());
                List<Student> studListNonGrad = new ArrayList<>();

                DistributionPrintRequest distributionPrintRequest = mapDist.get(mincode);

                ReportRequest packSlipReq = reportService.preparePackingSlipData(getBaseSchoolDetails(distributionPrintRequest, mincode, exception), processorData.getBatchId());
                Pair<Integer, Integer> pV = processTranscriptPrintRequest(distributionPrintRequest, currentSlipCount, packSlipReq, studListNonGrad, processorData, mincode, schoolCategoryCode, numberOfPdfs);
                currentSlipCount = pV.getLeft();
                numberOfPdfs = pV.getRight();
                pV = processYed2CertificatePrintRequest(distributionPrintRequest,currentSlipCount,packSlipReq,studListNonGrad,processorData,mincode,schoolCategoryCode,numberOfPdfs);
                currentSlipCount = pV.getLeft();
                numberOfPdfs = pV.getRight();
                pV = processYedbCertificatePrintRequest(distributionPrintRequest,currentSlipCount,packSlipReq,studListNonGrad,processorData,mincode,schoolCategoryCode,numberOfPdfs);
                currentSlipCount = pV.getLeft();
                numberOfPdfs = pV.getRight();
                pV = processYedrCertificatePrintRequest(distributionPrintRequest,currentSlipCount,packSlipReq,studListNonGrad,processorData,mincode,schoolCategoryCode,numberOfPdfs);
                numberOfPdfs = pV.getRight();

                if (!studListNonGrad.isEmpty() && NONGRADYERUN.equalsIgnoreCase(processorData.getActivityCode())) {
                    logger.debug("***** Create Student NonGrad {} School Reports *****", mincode);
                    numberOfCreatedSchoolReports += createAndSaveNonGradReport(commonSchool, studListNonGrad, mincode, educDistributionApiConstants.getStudentNonGrad());
                    logger.debug("***** Number of Student NonGrad School Reports Created {} *****", numberOfCreatedSchoolReports);
                    logger.debug("***** Distribute Student NonGrad {} School Reports *****", mincode);
                    List<String> mincodes = new ArrayList<>();
                    mincodes.add(mincode);
                    numberOfProcessedSchoolReports += processDistrictSchoolDistribution(processorData.getBatchId(), mincodes, null, null, NONGRADDISTREP_SC, null);
                    logger.debug("***** Number of distributed Student NonGrad School Reports {} *****", numberOfProcessedSchoolReports);
                }

                if(!NONGRADYERUN.equalsIgnoreCase(processorData.getActivityCode())) {
                    logger.debug("***** Create {} School Report *****", mincode);
                    List<String> mincodes = new ArrayList<>();
                    mincodes.add(mincode);
                    numberOfCreatedSchoolReports += createDistrictSchoolYearEndReport(null, null, DISTREP_YE_SC, mincodes);
                    logger.debug("***** Number of School Reports Created {} *****", numberOfCreatedSchoolReports);
                    logger.debug("***** Distribute {} School Reports *****", mincode);
                    numberOfProcessedSchoolReports += processDistrictSchoolDistribution(processorData.getBatchId(), mincodes, null, null, DISTREP_YE_SC, null);
                    logger.debug("***** {} School Report Created*****", mincode);
                    logger.debug("***** Number of distributed School Reports {} *****", numberOfProcessedSchoolReports);
                }

                logger.debug("PDFs Merged for School {}", commonSchool.getSchoolName());
            }
        }
        response.setTranscriptResponse(numberOfPdfs + " transcripts have been processed in batch " + processorData.getBatchId());
        if(!districtsForLabels.isEmpty()) {
            logger.debug("***** Create and Store district labels reports *****");
            for (School sch : districtsForLabels) {
                List<School> districts = new ArrayList<>();
                districts.add(sch);
                numberOfCreatedSchoolLabelReports += createSchoolLabelsReport(districts, ADDRESS_LABEL_YE);
            }
            logger.debug("***** Number of created district labels reports {} *****", numberOfCreatedSchoolLabelReports);
            logger.debug("***** Distribute District Label reports *****");
            List<String> mincodes = districtsForLabels.stream().map(School::getMincode).toList();
            numberOfProcessedSchoolReports += processDistrictSchoolDistribution(batchId, mincodes, ADDRESS_LABEL_YE, null, null, null);
            logger.debug("***** Number of distributed District Label reports {} *****", numberOfProcessedSchoolReports);

            if(!NONGRADYERUN.equalsIgnoreCase(processorData.getActivityCode())) {
                logger.debug("***** Create and Store District Reports *****");
                numberOfCreatedSchoolLabelReports += createDistrictSchoolYearEndReport(null, DISTREP_YE_SD, null, mincodes);
                logger.debug("***** Number of created District Reports {} *****", numberOfCreatedSchoolLabelReports);
                logger.debug("***** Distribute District Reports *****");
                numberOfProcessedSchoolReports += processDistrictSchoolDistribution(batchId, mincodes, null, DISTREP_YE_SD, null, null);
            }
        }

        if(!schoolsForLabels.isEmpty()) {
            logger.debug("***** Create and Store school labels reports *****");
            numberOfCreatedSchoolLabelReports += createSchoolLabelsReport(schoolsForLabels, ADDRESS_LABEL_SCHL);
            logger.debug("***** Number of created district labels reports {} *****", numberOfCreatedSchoolLabelReports);
            logger.debug("***** Distribute School Label reports *****");
            numberOfProcessedSchoolReports += processSchoolLabelsDistribution(batchId, ADDRESS_LABEL_SCHL, null);
            logger.debug("***** Number of distributed School Label reports {} *****", numberOfProcessedSchoolReports);
        }

        numberOfPdfs += numberOfProcessedSchoolReports;
        long endTime = System.currentTimeMillis();
        long diff = (endTime - startTime) / 1000;
        logger.debug("************* TIME Taken  ************ {} secs", diff);
        response.setMergeProcessResponse("Merge Successful and File Uploaded");
        response.setNumberOfPdfs(numberOfPdfs);
        response.setBatchId(processorData.getBatchId());
        response.setLocalDownload(processorData.getLocalDownload());
        response.setActivityCode(processorData.getActivityCode());
        response.getSchools().addAll(schoolsForLabels);
        response.getDistricts().addAll(districtsForLabels);
        response.setStudentSearchRequest(searchRequest);
        response.getDistrictSchools().addAll(mapDist.keySet());
        processorData.setDistributionResponse(response);
        return processorData;
    }
}
