package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.service.AccessTokenService;
import ca.bc.gov.educ.api.distribution.service.GradStudentService;
import ca.bc.gov.educ.api.distribution.service.ReportService;
import ca.bc.gov.educ.api.distribution.service.SchoolService;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.GradValidation;
import ca.bc.gov.educ.api.distribution.util.SFTPUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Component
@NoArgsConstructor
public class PostingStudentReportProcess implements DistributionProcess {
	
	private static Logger logger = LoggerFactory.getLogger(PostingStudentReportProcess.class);

	private static final String LOC = "/tmp/";
	private static final String DEL = "/";
	private static final String EXCEPTION = "Error {} ";

	@Autowired
	private GradStudentService gradStudentService;

	@Autowired
	GradValidation validation;

	@Autowired
	WebClient webClient;

	@Autowired
	EducDistributionApiConstants educDistributionApiConstants;

	@Autowired
	AccessTokenService accessTokenService;

	@Autowired
	SchoolService schoolService;

	@Autowired
	ReportService reportService;

	@Autowired
	SFTPUtils sftpUtils;

	@Override
	public ProcessorData fire(ProcessorData processorData) {
		long startTime = System.currentTimeMillis();
		logger.info("************* TIME START  ************ {}",startTime);
		DistributionResponse response = new DistributionResponse();
		Map<String,DistributionPrintRequest> mapDist = processorData.getMapDistribution();
		int numberOfPdfs = 0;
		int counter=0;
		for (Map.Entry<String, DistributionPrintRequest> entry : mapDist.entrySet()) {
			counter++;
			String mincode = entry.getKey();
			DistributionPrintRequest obj = entry.getValue();
			numberOfPdfs = processTranscriptPrintPostingRequest(obj,processorData,mincode,numberOfPdfs);
			numberOfPdfs = processAchievementPrintPostingRequest(obj,processorData,mincode,numberOfPdfs);
			logger.info("PDFs Merged {}", mincode);
			if (counter % 50 == 0) {
				accessTokenService.fetchAccessToken(processorData);
			}
			logger.info("School {}/{}",counter,mapDist.size());

		}
		long endTime = System.currentTimeMillis();
		long diff = (endTime - startTime)/1000;
		logger.info("************* TIME Taken  ************ {} secs",diff);
		response.setMergeProcessResponse("Merge Successful and File Uploaded "+numberOfPdfs);
		processorData.setDistributionResponse(response);
		return processorData;
	}

	private Integer processTranscriptPrintPostingRequest(DistributionPrintRequest obj,ProcessorData processorData, String mincode, int numberOfPdfs) {
		if (obj.getTranscriptPrintRequest() != null) {
			TranscriptPrintPostingRequest transcriptPrintRequest = obj.getTranscriptPrintPostingRequest();
			List<SchoolStudentCredentialDistribution> scdList = transcriptPrintRequest.getTranscriptList();
			List<InputStream> locations = new ArrayList<>();
			try {
				logger.info("*** Packing Slip Added");
				for (SchoolStudentCredentialDistribution scd : scdList) {
					InputStreamResource transcriptPdf = webClient.get().uri(String.format(educDistributionApiConstants.getTranscript(), scd.getStudentID(), scd.getCredentialTypeCode(),scd.getDocumentStatusCode())).headers(h -> h.setBearerAuth(processorData.getAccessToken())).retrieve().bodyToMono(InputStreamResource.class).block();
					if (transcriptPdf != null) {
						locations.add(transcriptPdf.getInputStream());
						mergeDocuments(processorData,mincode,mincode+"_"+ scd.getPen()+"_TRAN",locations);
						logger.info("*** Transcript Documents Merged and Posted");
						numberOfPdfs++;
						logger.debug("*** Added PDFs {} Current student {}", scdList.size(), scd.getStudentID());
					} else {
						logger.debug("*** Failed to Add PDFs Current student {}", scd.getStudentID());
					}
				}

			} catch (IOException e) {
				logger.debug(EXCEPTION,e.getLocalizedMessage());
			}
		}
		return numberOfPdfs;
	}

	private Integer processAchievementPrintPostingRequest(DistributionPrintRequest obj,ProcessorData processorData, String mincode, int numberOfPdfs) {
		if (obj.getTranscriptPrintRequest() != null) {
			TVRReportPrintPostingRequest transcriptPrintRequest = obj.getTvrReportPrintPostingRequest();
			List<SchoolStudentCredentialDistribution> scdList = transcriptPrintRequest.getTvrList();
			List<InputStream> locations = new ArrayList<>();
			try {
				for (SchoolStudentCredentialDistribution scd : scdList) {
					InputStreamResource tvrPdf = webClient.get().uri(String.format(educDistributionApiConstants.getTranscript(), scd.getStudentID(), scd.getCredentialTypeCode(),scd.getDocumentStatusCode())).headers(h -> h.setBearerAuth(processorData.getAccessToken())).retrieve().bodyToMono(InputStreamResource.class).block();
					if (tvrPdf != null) {
						locations.add(tvrPdf.getInputStream());
						mergeDocuments(processorData,mincode,mincode+"_"+ scd.getPen()+"_TVR",locations);
						logger.info("*** Achievement Documents Merged and Posted");
						numberOfPdfs++;
						logger.debug("*** Added PDFs {} Current student {}", scdList.size(), scd.getStudentID());
					} else {
						logger.debug("*** Failed to Add PDFs Current student {}", scd.getStudentID());
					}
				}

			} catch (IOException e) {
				logger.debug(EXCEPTION,e.getLocalizedMessage());
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
			pBuilder.append(LOC).append(processorData.getBatchId()).append(DEL).append(mincode).append(DEL).append(fileName).append(".").append(".pdf");
			objs.setDestinationFileName(pBuilder.toString());
			objs.addSources(locations);
			objs.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
			sftpUtils.sftpUploadTSW(processorData.getBatchId(),mincode,fileName);
		}catch (Exception e) {
			logger.debug(EXCEPTION,e.getLocalizedMessage());
		}
	}

}
