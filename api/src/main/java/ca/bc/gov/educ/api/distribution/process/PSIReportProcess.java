package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.modelmapper.internal.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
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
@EqualsAndHashCode(callSuper = false)
public class PSIReportProcess extends BaseProcess{
	
	private static Logger logger = LoggerFactory.getLogger(PSIReportProcess.class);

	@Override
	public ProcessorData fire(ProcessorData processorData) {
		long startTime = System.currentTimeMillis();
		logger.info("************* TIME START  ************ {}",startTime);
		DistributionResponse response = new DistributionResponse();
		ExceptionMessage exception = new ExceptionMessage();
		Map<String,DistributionPrintRequest> mapDist = processorData.getMapDistribution();
		Long batchId = processorData.getBatchId();
		int numberOfPdfs = 0;
		int counter=0;
		for (Map.Entry<String, DistributionPrintRequest> entry : mapDist.entrySet()) {
			counter++;
			int currentSlipCount = 0;
			String psiCode = entry.getKey();
			DistributionPrintRequest obj = entry.getValue();
			Psi psiDetails = psiService.getPsiDetails(psiCode,processorData.getAccessToken(),exception);
			if(psiDetails != null) {
				logger.info("*** PSI Details Acquired {}", psiDetails.getPsiName());
				ReportRequest packSlipReq = reportService.preparePackingSlipDataPSI(psiDetails, processorData.getBatchId());
				Pair<Integer,Integer> pV = processTranscriptPrintRequest(obj,currentSlipCount,packSlipReq,processorData,psiCode,numberOfPdfs);
				numberOfPdfs = pV.getRight();
				logger.info("PDFs Merged {}", psiDetails.getPsiName());
				if (counter % 50 == 0) {
					accessTokenService.fetchAccessToken(processorData);
				}
				logger.info("PSI {}/{}",counter,mapDist.size());
			}
		}
		createZipFile(batchId);
		if(processorData.getLocalDownload() == null || !processorData.getLocalDownload().equalsIgnoreCase("Y")) {
			createControlFile(batchId, numberOfPdfs);
			sftpUtils.sftpUploadBCMail(batchId);
		}
		long endTime = System.currentTimeMillis();
		long diff = (endTime - startTime)/1000;
		logger.info("************* TIME Taken  ************ {} secs",diff);
		response.setMergeProcessResponse("Merge Successful and File Uploaded");
		processorData.setDistributionResponse(response);
		return processorData;
	}

	private Pair<Integer, Integer> processTranscriptPrintRequest(DistributionPrintRequest obj, int currentSlipCount, ReportRequest packSlipReq, ProcessorData processorData, String psiCode, int numberOfPdfs) {
		if (obj.getPsiCredentialPrintRequest() != null) {
			PsiCredentialPrintRequest psiCredentialPrintRequest = obj.getPsiCredentialPrintRequest();
			List<PsiCredentialDistribution> scdList = psiCredentialPrintRequest.getPsiList();
			List<InputStream> locations = new ArrayList<>();
			currentSlipCount++;
			setExtraDataForPackingSlip(packSlipReq, "YED4", obj.getTotal(), scdList.size(), 1, "Transcript", psiCredentialPrintRequest.getBatchId());
			try {
				locations.add(reportService.getPackingSlip(packSlipReq, processorData.getAccessToken()).getInputStream());
				logger.info("*** Packing Slip Added");
				processStudents(scdList,locations,processorData);
				mergeDocuments(processorData,psiCode,"/EDGRAD.T.","YED4",locations);
				numberOfPdfs++;
				logger.info("*** Transcript Documents Merged");
			} catch (IOException e) {
				logger.debug(EXCEPTION,e.getLocalizedMessage());
			}
		}
		return Pair.of(currentSlipCount,numberOfPdfs);
	}

	private void processStudents(List<PsiCredentialDistribution> scdList, List<InputStream> locations, ProcessorData processorData) throws IOException {
		int currentTranscript = 0;
		int failedToAdd = 0;
		for (PsiCredentialDistribution scd : scdList) {
			InputStreamResource transcriptPdf = webClient.get().uri(String.format(educDistributionApiConstants.getTranscriptUsingStudentID(), scd.getStudentID())).headers(h -> h.setBearerAuth(processorData.getAccessToken())).retrieve().bodyToMono(InputStreamResource.class).block();
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

	private void createControlFile(Long batchId,int numberOfPdfs) {
		try(FileOutputStream fos = new FileOutputStream("/tmp/EDGRAD.BATCH." + batchId + ".txt")) {
			byte[] contentInBytes = String.valueOf(numberOfPdfs).getBytes();
			fos.write(contentInBytes);
			fos.flush();
		} catch (IOException e) {
			logger.debug(EXCEPTION,e.getLocalizedMessage());
		}
		logger.info("Created Control file ");

	}

	private void mergeDocuments(ProcessorData processorData,String psiCode,String fileName,String paperType,List<InputStream> locations) {
		try {
			PDFMergerUtility objs = new PDFMergerUtility();
			StringBuilder pBuilder = new StringBuilder();
			pBuilder.append(LOC).append(processorData.getBatchId()).append(DEL).append(psiCode).append(DEL);
			Path path = Paths.get(pBuilder.toString());
			Files.createDirectories(path);
			pBuilder = new StringBuilder();
			pBuilder.append(LOC).append(processorData.getBatchId()).append(DEL).append(psiCode).append(fileName).append(paperType).append(".").append(EducDistributionApiUtils.getFileName()).append(".pdf");
			objs.setDestinationFileName(pBuilder.toString());
			objs.addSources(locations);
			objs.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
		}catch (Exception e) {
			logger.debug(EXCEPTION,e.getLocalizedMessage());
		}
	}
}