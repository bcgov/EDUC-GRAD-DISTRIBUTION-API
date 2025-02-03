package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Data
@Component
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PostingSchoolReportProcess extends BaseProcess {
	
	@Override
	public ProcessorData fire(ProcessorData processorData) {
		long startTime = System.currentTimeMillis();
		log.debug("************* TIME START  ************ {}",startTime);
		DistributionResponse response = new DistributionResponse();
		ExceptionMessage exception = new ExceptionMessage();
		DistributionRequest distributionRequest = processorData.getDistributionRequest();
		Map<UUID, DistributionPrintRequest> mapDist = distributionRequest.getMapDist();
		StudentSearchRequest searchRequest = distributionRequest.getStudentSearchRequest();
		int numberOfPdfs = 0;
		int counter=0;
		for (Map.Entry<UUID, DistributionPrintRequest> entry : mapDist.entrySet()) {
			UUID schoolId = entry.getKey();
			counter++;
			DistributionPrintRequest distributionPrintRequest = entry.getValue();
			ca.bc.gov.educ.api.distribution.model.dto.v2.School schoolDetails =
					getBaseSchoolDetails(distributionPrintRequest, searchRequest, schoolId, exception);
			if (distributionPrintRequest.getSchoolReportPostRequest() != null) {
				SchoolReportPostRequest schoolRepPostReq = distributionPrintRequest.getSchoolReportPostRequest();
				numberOfPdfs = processFile(schoolRepPostReq.getGradReport(), schoolDetails.getMinCode(), numberOfPdfs, processorData);
				numberOfPdfs = processFile(schoolRepPostReq.getNongradReport(), schoolDetails.getMinCode(), numberOfPdfs, processorData);
				numberOfPdfs = processFile(schoolRepPostReq.getNongradprjreport(), schoolDetails.getMinCode(), numberOfPdfs, processorData);
			}
			if (counter % 50 == 0) {
				restUtils.fetchAccessToken(processorData);
			}
			log.debug("School {}/{} Number of Reports {}", counter, mapDist.size(), numberOfPdfs);

		}
		long endTime = System.currentTimeMillis();
		long diff = (endTime - startTime)/1000;
		log.debug("************* TIME Taken  ************ {} secs",diff);
		response.setMergeProcessResponse("Read Successful and Posting Done");
		response.setNumberOfPdfs(numberOfPdfs);
		response.setBatchId(processorData.getBatchId());
		response.setLocalDownload(processorData.getLocalDownload());
		response.setActivityCode(distributionRequest.getActivityCode());
		response.setStudentSearchRequest(searchRequest);
		processorData.setDistributionResponse(response);
		return processorData;
	}

	private int processFile(SchoolReportDistribution scdReport, String mincode, int numberOfPdfs, ProcessorData processorData) {
		if(scdReport != null) {
			List<InputStream> locations = new ArrayList<>();
			try {
				InputStreamResource gradReportPdf = restService.executeGet(
						educDistributionApiConstants.getSchoolReport(),
						InputStreamResource.class,
						mincode, scdReport.getReportTypeCode()
				);
				if (gradReportPdf != null) {
					locations.add(gradReportPdf.getInputStream());
					log.debug("*** Added School Report PDFs Report Type {}", scdReport.getReportTypeCode());
					mergeDocuments(processorData, mincode, EducDistributionApiUtils.getFileNameSchoolReports(mincode), locations);
					numberOfPdfs++;
				} else {
					log.info("*** Failed to Add School Report PDFs Report Type {} in batch {}", scdReport.getReportTypeCode(), processorData.getBatchId());
				}
				log.debug("*** GRADDIST Report Created");
			} catch (IOException e) {
				log.debug(EXCEPTION, e.getLocalizedMessage());
			}
		}
		return numberOfPdfs;
	}

	private void mergeDocuments(ProcessorData processorData,String mincode,String fileName,List<InputStream> locations) {
		try {
			PDFMergerUtility objs = new PDFMergerUtility();
			StringBuilder pBuilder = new StringBuilder();
			pBuilder.append(EducDistributionApiConstants.TMP_DIR).append(EducDistributionApiConstants.DEL).append(processorData.getBatchId()).append(EducDistributionApiConstants.DEL).append(mincode).append(EducDistributionApiConstants.DEL);
			Path path = Paths.get(pBuilder.toString());
			Files.createDirectories(path);
			pBuilder = new StringBuilder();
			pBuilder.append(EducDistributionApiConstants.TMP_DIR).append(EducDistributionApiConstants.DEL).append(processorData.getBatchId()).append(EducDistributionApiConstants.DEL).append(mincode).append(EducDistributionApiConstants.DEL).append(fileName).append(".pdf");
			objs.setDestinationFileName(pBuilder.toString());
			objs.addSources(locations);
			objs.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
		}catch (Exception e) {
			log.debug(EXCEPTION,e.getLocalizedMessage());
		}
	}
}
