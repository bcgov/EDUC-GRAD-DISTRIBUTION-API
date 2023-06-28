package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Data
@Component
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PostingSchoolReportProcess extends BaseProcess {
	
	private static Logger logger = LoggerFactory.getLogger(PostingSchoolReportProcess.class);

	@Override
	public ProcessorData fire(ProcessorData processorData) {
		long startTime = System.currentTimeMillis();
		logger.debug("************* TIME START  ************ {}",startTime);
		DistributionResponse response = new DistributionResponse();
		DistributionRequest distributionRequest = processorData.getDistributionRequest();
		Map<String, DistributionPrintRequest> mapDist = distributionRequest.getMapDist();
		StudentSearchRequest searchRequest = distributionRequest.getStudentSearchRequest();
		int numberOfPdfs = 0;
		int counter=0;
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("PST"), Locale.CANADA);
		for (String mincode : mapDist.keySet()) {
			counter++;
			DistributionPrintRequest obj = mapDist.get(mincode);
			if (obj.getSchoolReportPostRequest() != null) {
				SchoolReportPostRequest schoolRepPostReq = obj.getSchoolReportPostRequest();
				numberOfPdfs = processFile(schoolRepPostReq.getGradReport(),mincode,numberOfPdfs,processorData);
				numberOfPdfs = processFile(schoolRepPostReq.getNongradReport(),mincode,numberOfPdfs,processorData);
				numberOfPdfs = processFile(schoolRepPostReq.getNongradprjreport(),mincode,numberOfPdfs,processorData);
			}
			if (counter % 50 == 0) {
				restUtils.fetchAccessToken(processorData);
			}
			logger.debug("School {}/{} Number of Reports {}",counter,mapDist.size(),numberOfPdfs);

		}
		long endTime = System.currentTimeMillis();
		long diff = (endTime - startTime)/1000;
		logger.debug("************* TIME Taken  ************ {} secs",diff);
		response.setMergeProcessResponse("Read Successful and Posting Done");
		response.setNumberOfPdfs(numberOfPdfs);
		response.setProcessedCyclesCount(distributionRequest.getProcessedCyclesCount());
		response.setBatchId(processorData.getBatchId());
		response.setLocalDownload(processorData.getLocalDownload());
		response.setTotalCyclesCount(distributionRequest.getTotalCyclesCount());
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
						scdReport.getSchoolOfRecord(), scdReport.getReportTypeCode()
				);
				if (gradReportPdf != null) {
					locations.add(gradReportPdf.getInputStream());
					logger.debug("*** Added PDFs Current Report Type {}", scdReport.getReportTypeCode());
					mergeDocuments(processorData, mincode, EducDistributionApiUtils.getFileNameSchoolReports(mincode), locations);
					numberOfPdfs++;
				} else {
					logger.info("*** Failed to Add PDFs Current Report Type {} in batch {}", scdReport.getReportTypeCode(), processorData.getBatchId());
				}
				logger.debug("*** GRADDIST Report Created");
			} catch (IOException e) {
				logger.debug(EXCEPTION, e.getLocalizedMessage());
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
			sftpUtils.sftpUploadTSW(processorData.getBatchId(),mincode,fileName);
		}catch (Exception e) {
			logger.debug(EXCEPTION,e.getLocalizedMessage());
		}
	}
}
