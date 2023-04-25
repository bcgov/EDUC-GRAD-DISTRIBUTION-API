package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.util.Generated;
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

@Data
@Component
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class YearEndMergeProcess extends MergeProcess {
	
	private static Logger logger = LoggerFactory.getLogger(YearEndMergeProcess.class);

	@Override
	@Generated
	public ProcessorData fire(ProcessorData processorData) {
		long startTime = System.currentTimeMillis();
		logger.debug("************* TIME START  ************ {}",startTime);
		DistributionResponse response = new DistributionResponse();
		ExceptionMessage exception = new ExceptionMessage();
		Map<String,DistributionPrintRequest> mapDist = processorData.getMapDistribution();
		int numberOfPdfs = 0;
		int counter=0;
		List<School> schoolsForLabels = new ArrayList<>();
		for (Map.Entry<String, DistributionPrintRequest> entry : mapDist.entrySet()) {
			counter++;
			int currentSlipCount = 0;
			String mincode = entry.getKey();
			DistributionPrintRequest distributionPrintRequest = entry.getValue();
			CommonSchool schoolDetails = getBaseSchoolDetails(distributionPrintRequest,mincode,exception);
			if(schoolDetails != null) {
				String schoolCategoryCode = schoolDetails.getSchoolCategoryCode();
				logger.debug("*** School Details Acquired {} category {}", mincode, schoolCategoryCode);
				List<Student> studListNonGrad = new ArrayList<>();
				ReportRequest packSlipReq = reportService.preparePackingSlipData(schoolDetails, processorData.getBatchId());
				Pair<Integer,Integer> pV = processTranscriptPrintRequest(distributionPrintRequest,currentSlipCount,packSlipReq,studListNonGrad,processorData,mincode,schoolCategoryCode,numberOfPdfs);
				numberOfPdfs = pV.getRight();
				if(!studListNonGrad.isEmpty()) {
					createAndSaveNonGradReport(schoolDetails,studListNonGrad,mincode);
				}
				logger.debug("PDFs Merged {}", schoolDetails.getSchoolName());
				processSchoolsForLabels(schoolsForLabels, mincode, restUtils.getAccessToken(), exception);
				logger.debug("School {}/{}",counter,mapDist.size());

				int numberOfCreatedSchoolReports = 0;
				int numberOfProcessedSchoolReports = 0;
				if (YEARENDDIST.equalsIgnoreCase(processorData.getActivityCode())) {
					logger.debug("***** Create and Store Year End school reports *****");
					numberOfCreatedSchoolReports += createDistrictSchoolYearEndReport(restUtils.getAccessToken(), ADDRESS_LABEL_YE, DISTREP_YE_SD, DISTREP_YE_SC);
					logger.debug("***** Number of created Year End school reports {} *****", numberOfCreatedSchoolReports);
					logger.debug("***** Distribute Year End school reports *****");
					numberOfProcessedSchoolReports += processDistrictSchoolDistribution(processorData, ADDRESS_LABEL_YE, DISTREP_YE_SD, DISTREP_YE_SC);
					logger.debug("***** Number of distributed Year End school reports {} *****", numberOfProcessedSchoolReports);
				}
				if (NONGRADDIST.equalsIgnoreCase(processorData.getActivityCode())) {
					logger.debug("***** Create and Store Student NonGrad School Report *****");
					numberOfCreatedSchoolReports += createSchoolLabelsReport(schoolsForLabels, restUtils.getAccessToken(), ADDRESS_LABEL_SCHL );
					logger.debug("***** Number of created Student NonGrad School Reports {} *****", numberOfCreatedSchoolReports);
					logger.debug("***** Distribute Student NonGrad School Reports *****");
					numberOfProcessedSchoolReports += processDistrictSchoolDistribution(processorData, ADDRESS_LABEL_SCHL, null, NONGRADDISTREP_SC);
					logger.debug("***** Number of distributed Student NonGrad School Reports {} *****", numberOfProcessedSchoolReports);
				}
				numberOfPdfs += numberOfProcessedSchoolReports;
				long endTime = System.currentTimeMillis();
				long diff = (endTime - startTime)/1000;
				logger.debug("************* TIME Taken  ************ {} secs",diff);
				response.setMergeProcessResponse("Merge Successful and File Uploaded");
				processorData.setDistributionResponse(response);
			}
		}
		return processorData;
	}

}
