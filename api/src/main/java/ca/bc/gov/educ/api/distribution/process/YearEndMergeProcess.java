package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.constants.SchoolCategoryCodes;
import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.model.dto.v2.District;
import ca.bc.gov.educ.api.distribution.model.dto.v2.YearEndReportRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.internal.Pair;
import org.springframework.stereotype.Component;

import java.util.*;

import static ca.bc.gov.educ.api.distribution.model.dto.ActivityCode.*;
import static ca.bc.gov.educ.api.distribution.model.dto.ReportType.*;
import static ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants.*;

@Slf4j
@Data
@Component
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class YearEndMergeProcess extends MergeProcess {

    @Override
    public ProcessorData fire(ProcessorData processorData) {
        long startTime = System.currentTimeMillis();
        log.debug("************* TIME START  ************ {}", startTime);
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
        List<ca.bc.gov.educ.api.distribution.model.dto.School> schoolsForLabels = new ArrayList<>();
        Map<District, List<ca.bc.gov.educ.api.distribution.model.dto.School>> districtSchoolsForLabels = new HashMap<>();
        List<String> processedSchools = new ArrayList<>();
        for (Map.Entry<String, DistributionPrintRequest> entry : mapDist.entrySet()) {
            UUID schoolId = UUID.fromString(entry.getKey());
            DistributionPrintRequest distributionPrintRequest = entry.getValue();
            ca.bc.gov.educ.api.distribution.model.dto.v2.School schoolDetails =
                    getBaseSchoolDetails(distributionPrintRequest, searchRequest, schoolId, exception);
            if (schoolDetails != null) {
                int currentSlipCount = 0;
                schoolCounter++;
                String schoolCategoryCode = schoolDetails.getSchoolCategoryCode();

                log.debug("*** School Details Acquired {} category {}", schoolDetails.getMinCode(), schoolCategoryCode);
                if(SchoolCategoryCodes.getSchoolTypesWithoutDistricts().contains(schoolCategoryCode)) {
                    processSchoolsForLabels(searchRequest.getUser(), schoolsForLabels, schoolDetails);
                    log.debug("Added Independent School {} for processing", schoolDetails.getSchoolName());
                } else {
                    // No district for 02,03,09 school category
                    processDistrictsForLabels(districtSchoolsForLabels, schoolDetails, exception);
                    processedSchools.add(schoolDetails.getSchoolId());
                }
                log.debug("{} School {}/{}", schoolDetails.getMinCode(), schoolCounter, mapDist.size());
                List<Student> studListNonGrad = new ArrayList<>();

                ReportRequest packSlipReq = reportService.preparePackingSlipData(searchRequest, schoolDetails,
                        processorData.getBatchId());
                Pair<Integer, Integer> pV = processTranscriptPrintRequest(distributionPrintRequest, currentSlipCount,
                        packSlipReq, studListNonGrad, processorData, schoolDetails.getMinCode(), schoolCategoryCode, numberOfPdfs);
                currentSlipCount = pV.getLeft();
                numberOfPdfs = pV.getRight();
                pV = processYed2CertificatePrintRequest(distributionPrintRequest, currentSlipCount, packSlipReq,
                        studListNonGrad, processorData, schoolDetails.getMinCode(), schoolCategoryCode, numberOfPdfs);
                currentSlipCount = pV.getLeft();
                numberOfPdfs = pV.getRight();
                pV = processYedbCertificatePrintRequest(distributionPrintRequest, currentSlipCount, packSlipReq,
                        studListNonGrad, processorData, schoolDetails.getMinCode(), schoolCategoryCode, numberOfPdfs);
                currentSlipCount = pV.getLeft();
                numberOfPdfs = pV.getRight();
                pV = processYedrCertificatePrintRequest(distributionPrintRequest, currentSlipCount, packSlipReq,
                        studListNonGrad, processorData, schoolDetails.getMinCode(), schoolCategoryCode, numberOfPdfs);
                numberOfPdfs = pV.getRight();

                if (!studListNonGrad.isEmpty() && NONGRADYERUN.getValue().equalsIgnoreCase(processorData.getActivityCode())) {
                    log.debug("***** Create Student NonGrad {} School Reports *****", schoolDetails.getMinCode());
                    numberOfCreatedSchoolReports += createAndSaveNonGradReport(schoolDetails, studListNonGrad, schoolId,
                            educDistributionApiConstants.getStudentNonGrad());
                    log.debug("***** Number of Student NonGrad School Reports Created {} *****", numberOfCreatedSchoolReports);
                    log.debug("***** Distribute Student NonGrad {} School Reports *****", schoolDetails.getMinCode());
                    YearEndReportRequest yearEndReportRequest = new YearEndReportRequest();
                    yearEndReportRequest.setSchoolIds(List.of(UUID.fromString(schoolDetails.getSchoolId())));
                    numberOfProcessedSchoolReports += processDistrictSchoolDistribution(processorData.getBatchId(),
                        List.of(schoolDetails.getSchoolId()), null, null, null, NONGRADDISTREP_SC.getValue(),
                            null);
                    log.debug("***** Number of distributed Student NonGrad School Reports {} *****", numberOfProcessedSchoolReports);
                }

                if(!NONGRADYERUN.getValue().equalsIgnoreCase(processorData.getActivityCode())) {
                    log.debug("***** Create {} School Report *****", schoolDetails.getMinCode());
                    YearEndReportRequest yearEndReportRequest = new YearEndReportRequest();
                    yearEndReportRequest.setSchoolIds(List.of(UUID.fromString(schoolDetails.getSchoolId())));
                    numberOfCreatedSchoolReports += createDistrictSchoolYearEndReport(null,
                            null, DISTREP_YE_SC.getValue(), yearEndReportRequest);
                    log.debug("***** Number of School Reports Created {} *****", numberOfCreatedSchoolReports);
                    log.debug("***** Distribute {} School Reports *****", schoolDetails.getMinCode());
                    numberOfProcessedSchoolReports += processDistrictSchoolDistribution(processorData.getBatchId(),
                        List.of(schoolDetails.getSchoolId()), null, null, null, DISTREP_YE_SC.getValue(), null);
                    log.debug("***** {} School Report Created*****", schoolDetails.getMinCode());
                    log.debug("***** Number of distributed School Reports {} *****", numberOfProcessedSchoolReports);
                }

                log.debug("PDFs Merged for School {}", schoolDetails.getSchoolName());
            }
        }
        response.setTranscriptResponse(numberOfPdfs + " transcripts have been processed in batch " + processorData.getBatchId());
        if(!districtSchoolsForLabels.isEmpty()) {
            log.debug("***** Create and Store district labels reports *****");
            for(Map.Entry<District, List<ca.bc.gov.educ.api.distribution.model.dto.School>> entry: districtSchoolsForLabels.entrySet()) {
                log.debug("Create district labels report {} ", entry.getKey().getDistrictId());
                numberOfCreatedSchoolLabelReports += postingDistributionService.createDistrictLabelsReport(List.of(entry.getKey()), ADDRESS_LABEL_YE);
                log.debug("Create district school labels report {} ", entry.getKey().getDistrictId());
                numberOfCreatedSchoolLabelReports += postingDistributionService.createDistrictLabelsReport(entry.getKey().getDistrictId(), entry.getValue(), ADDRESS_LABEL_SCH_YE);
                log.debug("***** Number of distributed School Label reports {} *****", numberOfProcessedSchoolReports);
            }


            log.debug("***** Number of created district labels reports {} *****", numberOfCreatedSchoolLabelReports);
            log.debug("***** Distribute District Label reports *****");

            List<String> districtIds = districtSchoolsForLabels.keySet().stream().map(District::getDistrictId).toList();
            YearEndReportRequest yearEndReportRequest = new YearEndReportRequest();
            yearEndReportRequest.setDistrictIds(districtIds.stream().map(UUID::fromString).toList());
            numberOfProcessedSchoolReports += processDistrictSchoolDistribution(batchId, null, districtIds, ADDRESS_LABEL_YE,
                    null, null, null);
            numberOfProcessedSchoolReports += processDistrictSchoolDistribution(batchId, null, districtIds, ADDRESS_LABEL_SCH_YE,
                    null, null, null);
            log.debug("***** Number of distributed District Label reports {} *****", numberOfProcessedSchoolReports);

            if(!NONGRADYERUN.getValue().equalsIgnoreCase(processorData.getActivityCode())) {
                log.debug("***** Create and Store District Reports *****");
                numberOfCreatedSchoolLabelReports += createDistrictSchoolYearEndReport(null,
                        DISTREP_YE_SD.getValue(), null, yearEndReportRequest);
                log.debug("***** Number of created District Reports {} *****", numberOfCreatedSchoolLabelReports);
                log.debug("***** Distribute District Reports *****");
                numberOfProcessedSchoolReports += processDistrictSchoolDistribution(batchId, null, districtIds,
                        null, DISTREP_YE_SD.getValue(), null, null);
            }
        }

        if(!schoolsForLabels.isEmpty()) {
            log.debug("***** Create and Store school labels reports *****");
            numberOfCreatedSchoolLabelReports += createSchoolLabelsReport(schoolsForLabels, ADDRESS_LABEL_SCHL);
            log.debug("***** Number of created district labels reports {} *****", numberOfCreatedSchoolLabelReports);
            log.debug("***** Distribute School Label reports *****");
            numberOfProcessedSchoolReports += processSchoolLabelsDistribution(batchId, ADDRESS_LABEL_SCHL, null);
            log.debug("***** Number of distributed School Label reports {} *****", numberOfProcessedSchoolReports);
        }

        numberOfPdfs += numberOfProcessedSchoolReports;
        long endTime = System.currentTimeMillis();
        long diff = (endTime - startTime) / 1000;
        log.debug("************* TIME Taken  ************ {} secs", diff);
        response.setMergeProcessResponse("Merge Successful and File Uploaded");
        response.setNumberOfPdfs(numberOfPdfs);
        response.setBatchId(processorData.getBatchId());
        response.setLocalDownload(processorData.getLocalDownload());
        response.setActivityCode(processorData.getActivityCode());
        response.getSchools().addAll(schoolsForLabels);
        response.getDistricts().addAll(districtSchoolsForLabels.keySet());
        response.setStudentSearchRequest(searchRequest);
        response.getDistrictSchools().addAll(processedSchools);
        processorData.setDistributionResponse(response);
        return processorData;
    }
}
