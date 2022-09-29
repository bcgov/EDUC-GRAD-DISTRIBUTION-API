package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
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
		logger.info("************* TIME START  ************ {}",startTime);
		DistributionResponse response = new DistributionResponse();
		Map<String,DistributionPrintRequest> mapDist = processorData.getMapDistribution();
		int numberOfPdfs = 0;
		int counter=0;
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("PST"), Locale.CANADA);
		int year = cal.get(Calendar.YEAR);
		String month = String.format("%02d", cal.get(Calendar.MONTH) + 1);
		for (Map.Entry<String, DistributionPrintRequest> entry : mapDist.entrySet()) {
			counter++;
			String mincode = entry.getKey();
			DistributionPrintRequest obj = entry.getValue();
			if (obj.getSchoolReportPostRequest() != null) {
				SchoolReportPostRequest schoolRepPostReq = obj.getSchoolReportPostRequest();
				numberOfPdfs = processFile(schoolRepPostReq.getGradReport(),mincode,year,month,"GRADDIST",numberOfPdfs,processorData);
				numberOfPdfs = processFile(schoolRepPostReq.getNongradReport(),mincode,year,month,"NONGRAD",numberOfPdfs,processorData);
				numberOfPdfs = processFile(schoolRepPostReq.getNongradprjreport(),mincode,year,"00","NONGRADPRJ",numberOfPdfs,processorData);
			}
			if (counter % 50 == 0) {
				accessTokenService.fetchAccessToken(processorData);
			}
			logger.info("School {}/{} Number of Reports {}",counter,mapDist.size(),numberOfPdfs);

		}
		long endTime = System.currentTimeMillis();
		long diff = (endTime - startTime)/1000;
		logger.info("************* TIME Taken  ************ {} secs",diff);
		response.setMergeProcessResponse("Read Successful and Posting Done");
		processorData.setDistributionResponse(response);
		return processorData;
	}

	private int processFile(SchoolReportDistribution scdReport, String mincode, int year, String month, String type, int numberOfPdfs, ProcessorData processorData) {
		if(scdReport != null) {
			List<InputStream> locations = new ArrayList<>();
			try {
				InputStreamResource gradReportPdf = webClient.get().uri(String.format(educDistributionApiConstants.getSchoolReport(), scdReport.getSchoolOfRecord(), scdReport.getReportTypeCode())).headers(h -> h.setBearerAuth(processorData.getAccessToken())).retrieve().bodyToMono(InputStreamResource.class).block();
				if (gradReportPdf != null) {
					locations.add(gradReportPdf.getInputStream());
					logger.debug("*** Added PDFs Current Report Type {}", scdReport.getReportTypeCode());
					mergeDocuments(processorData, mincode, EducDistributionApiUtils.getFileNameSchoolReports(mincode, year, month, type), locations);
					numberOfPdfs++;
				} else {
					logger.debug("*** Failed to Add PDFs Current Report Type {}", scdReport.getReportTypeCode());
				}
				logger.info("*** GRADDIST Report Created");
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
			pBuilder.append(LOC).append(processorData.getBatchId()).append(DEL).append(mincode).append(DEL);
			Path path = Paths.get(pBuilder.toString());
			Files.createDirectories(path);
			pBuilder = new StringBuilder();
			pBuilder.append(LOC).append(processorData.getBatchId()).append(DEL).append(mincode).append(DEL).append(fileName).append(".pdf");
			objs.setDestinationFileName(pBuilder.toString());
			objs.addSources(locations);
			objs.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
			sftpUtils.sftpUploadTSW(processorData.getBatchId(),mincode,fileName);
		}catch (Exception e) {
			logger.debug(EXCEPTION,e.getLocalizedMessage());
		}
	}
}
