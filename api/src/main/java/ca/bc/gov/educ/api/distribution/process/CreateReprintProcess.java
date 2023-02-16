package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.ByteArrayInputStream;
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
public class CreateReprintProcess extends BaseProcess {
	
	private static Logger logger = LoggerFactory.getLogger(CreateReprintProcess.class);

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
			String mincode = entry.getKey();
			DistributionPrintRequest obj = entry.getValue();
			CommonSchool schoolDetails = getBaseSchoolDetails(obj,mincode,processorData,exception);
			if(schoolDetails != null) {
				logger.info("*** School Details Acquired {}", schoolDetails.getSchoolName());

				ReportRequest packSlipReq = reportService.preparePackingSlipData(schoolDetails, processorData.getBatchId());

				if(obj.getSchoolDistributionRequest() != null) {
					ReportRequest schoolDistributionReportRequest = reportService.prepareSchoolDistributionReportData(obj.getSchoolDistributionRequest(), processorData.getBatchId(),schoolDetails);
					createAndSaveDistributionReport(schoolDistributionReportRequest,mincode,processorData);
					numberOfPdfs++;
				}
				numberOfPdfs = processYed2Certificate(obj,currentSlipCount,packSlipReq,mincode,processorData,numberOfPdfs);
				numberOfPdfs = processYedbCertificate(obj,currentSlipCount,packSlipReq,mincode,processorData,numberOfPdfs);
				numberOfPdfs = processYedrCertificate(obj,currentSlipCount,packSlipReq,mincode,processorData,numberOfPdfs);

				logger.info("PDFs Merged {}", schoolDetails.getSchoolName());
				logger.info("School {}/{}",counter,mapDist.size());
				if (counter % 50 == 0) {
					restUtils.fetchAccessToken(processorData);
				}
			}
		}
		postingProcess(batchId,processorData,numberOfPdfs);
		long endTime = System.currentTimeMillis();
		long diff = (endTime - startTime)/1000;
		logger.info("************* TIME Taken  ************ {} secs",diff);
		response.setMergeProcessResponse("Merge Successful and File Uploaded");
		processorData.setDistributionResponse(response);
		return processorData;
	}

	private int processYedrCertificate(DistributionPrintRequest obj, int currentSlipCount, ReportRequest packSlipReq, String mincode,ProcessorData processorData, int numberOfPdfs) {
		if (obj.getYedrCertificatePrintRequest() != null) {
			currentSlipCount++;
			processCertificatePrintFile(packSlipReq,obj.getYedrCertificatePrintRequest(),mincode,currentSlipCount,obj,processorData,"YEDR");
			numberOfPdfs++;
			logger.info("*** YEDR Documents Merged");
		}
		return numberOfPdfs;
	}

	private int processYedbCertificate(DistributionPrintRequest obj, int currentSlipCount, ReportRequest packSlipReq, String mincode,ProcessorData processorData, int numberOfPdfs) {
		if (obj.getYedbCertificatePrintRequest() != null) {
			currentSlipCount++;
			processCertificatePrintFile(packSlipReq,obj.getYedbCertificatePrintRequest(),mincode,currentSlipCount,obj,processorData,"YEDB");
			numberOfPdfs++;
			logger.info("*** YEDB Documents Merged");
		}
		return numberOfPdfs;
	}

	private int processYed2Certificate(DistributionPrintRequest obj, int currentSlipCount, ReportRequest packSlipReq, String mincode, ProcessorData processorData, int numberOfPdfs) {
		if (obj.getYed2CertificatePrintRequest() != null) {
			currentSlipCount++;
			processCertificatePrintFile(packSlipReq,obj.getYed2CertificatePrintRequest(),mincode,currentSlipCount,obj,processorData,"YED2");
			numberOfPdfs++;
			logger.info("*** YED2 Documents Merged");
		}
		return numberOfPdfs;
	}
	private void processCertificatePrintFile(ReportRequest packSlipReq, CertificatePrintRequest certificatePrintRequest, String mincode, int currentSlipCount, DistributionPrintRequest obj, ProcessorData processorData, String paperType) {
		PackingSlipRequest request = PackingSlipRequest.builder().mincode(mincode).currentSlip(currentSlipCount).total(obj.getTotal()).paperType(paperType).build();
		mergeCertificates(packSlipReq, certificatePrintRequest,request,processorData);
	}

	@SneakyThrows
	private void mergeCertificates(ReportRequest packSlipReq, CertificatePrintRequest certificatePrintRequest, PackingSlipRequest request, ProcessorData processorData) {
		List<StudentCredentialDistribution> scdList = certificatePrintRequest.getCertificateList();
		String mincode = request.getMincode();
		String paperType = request.getPaperType();
		List<InputStream> locations=new ArrayList<>();
		setExtraDataForPackingSlip(packSlipReq,paperType,request.getTotal(),scdList.size(),request.getCurrentSlip(), certificatePrintRequest.getBatchId());
		try {
			locations.add(reportService.getPackingSlip(packSlipReq,processorData.getAccessToken()).getInputStream());
			int currentCertificate = 0;
			int failedToAdd = 0;
			for (StudentCredentialDistribution scd : scdList) {
				ReportData data = webClient.get().uri(String.format(educDistributionApiConstants.getCertDataReprint(), scd.getPen())).headers(h -> h.setBearerAuth(processorData.getAccessToken())).retrieve().bodyToMono(ReportData.class).block();
				if(data != null) {
					data.getCertificate().setCertStyle("Reprint");
					data.getCertificate().getOrderType().getCertificateType().setReportName(scd.getCredentialTypeCode());
					data.getCertificate().getOrderType().getCertificateType().getPaperType().setCode(scd.getPaperType());
				}
				ReportOptions options = new ReportOptions();
				options.setReportFile("certificate");
				options.setReportName("Certificate.pdf");
				ReportRequest reportParams = new ReportRequest();
				reportParams.setOptions(options);
				reportParams.setData(data);
				byte[] bytesSAR = webClient.post().uri(educDistributionApiConstants.getCertificateReport()).headers(h -> h.setBearerAuth(processorData.getAccessToken())).body(BodyInserters.fromValue(reportParams)).retrieve().bodyToMono(byte[].class).block();
				if (bytesSAR != null) {
					locations.add(new ByteArrayInputStream(bytesSAR));
					currentCertificate++;
					logger.debug("*** Added PDFs {}/{} Current student {}", currentCertificate, scdList.size(), scd.getStudentID());
				} else {
					failedToAdd++;
					logger.debug("*** Failed to Add PDFs {} Current student {}", failedToAdd, scd.getStudentID());
				}
			}
			mergeDocuments(processorData,mincode,"/EDGRAD.C.",paperType,locations);
		} catch (IOException e) {
			logger.debug(EXCEPTION,e.getMessage());
		}
	}

	private void mergeDocuments(ProcessorData processorData,String mincode,String fileName,String paperType,List<InputStream> locations) {
		try {
			PDFMergerUtility objs = new PDFMergerUtility();
			StringBuilder pBuilder = new StringBuilder();
			pBuilder.append(LOC).append(processorData.getBatchId()).append(DEL).append(mincode).append(DEL);
			Path path = Paths.get(pBuilder.toString());
			Files.createDirectories(path);
			pBuilder = new StringBuilder();
			pBuilder.append(LOC).append(processorData.getBatchId()).append(DEL).append(mincode).append(fileName).append(paperType).append(".").append(EducDistributionApiUtils.getFileName()).append(".pdf");
			objs.setDestinationFileName(pBuilder.toString());
			objs.addSources(locations);
			objs.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
		}catch (Exception e) {
			logger.debug(EXCEPTION,e.getLocalizedMessage());
		}
	}

	private void createAndSaveDistributionReport(ReportRequest distributionRequest,String mincode,ProcessorData processorData) {
		List<InputStream> locations=new ArrayList<>();
		try {
			byte[] bytesSAR = webClient.post().uri(educDistributionApiConstants.getDistributionReport()).headers(h -> h.setBearerAuth(processorData.getAccessToken())).body(BodyInserters.fromValue(distributionRequest)).retrieve().bodyToMono(byte[].class).block();
			if(bytesSAR != null) {
				locations.add(new ByteArrayInputStream(bytesSAR));
			}
			mergeDocuments(processorData,mincode,"/EDGRAD.R.","324W",locations);
		} catch (Exception e) {
			logger.debug(EXCEPTION,e.getMessage());
		}
	}
}
