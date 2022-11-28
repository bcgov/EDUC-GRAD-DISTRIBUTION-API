package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils;
import ca.bc.gov.educ.api.distribution.util.JsonTransformer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.*;
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
public class CreateBlankCredentialProcess extends BaseProcess {
	
	private static Logger logger = LoggerFactory.getLogger(CreateBlankCredentialProcess.class);

	@Autowired
	JsonTransformer jsonTransformer;

	@Override
	public ProcessorData fire(ProcessorData processorData) {
		long startTime = System.currentTimeMillis();
		logger.info("************* TIME START  ************ {}",startTime);
		DistributionResponse response = new DistributionResponse();
		ExceptionMessage exception = new ExceptionMessage();
		Map<String, DistributionPrintRequest> mapDist = processorData.getMapDistribution();
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
				numberOfPdfs = processYed4Transcript(obj,currentSlipCount,packSlipReq,mincode,processorData,numberOfPdfs);
				numberOfPdfs = processYed2Certificate(obj,currentSlipCount,packSlipReq,mincode,processorData,numberOfPdfs);
				numberOfPdfs = processYedbCertificate(obj,currentSlipCount,packSlipReq,mincode,processorData,numberOfPdfs);
				numberOfPdfs = processYedrCertificate(obj,currentSlipCount,packSlipReq,mincode,processorData,numberOfPdfs);

				logger.info("PDFs Merged {}", schoolDetails.getSchoolName());
				logger.info("School {}/{}",counter,mapDist.size());
				if (counter % 50 == 0) {
					accessTokenService.fetchAccessToken(processorData);
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

	private int processYed4Transcript(DistributionPrintRequest obj, int currentSlipCount, ReportRequest packSlipReq, String mincode,ProcessorData processorData, int numberOfPdfs) {
		if (obj.getTranscriptPrintRequest() != null) {
			TranscriptPrintRequest transcriptPrintRequest = obj.getTranscriptPrintRequest();
			List<BlankCredentialDistribution> bcdList = transcriptPrintRequest.getBlankTranscriptList();
			List<InputStream> locations = new ArrayList<>();
			currentSlipCount++;
			int totalQuantity = transcriptPrintRequest.getBlankTranscriptList().get(0).getQuantity() * bcdList.size();
			setExtraDataForPackingSlip(packSlipReq, "YED4", obj.getTotal(), totalQuantity, currentSlipCount, transcriptPrintRequest.getBatchId());
			try {
				locations.add(reportService.getPackingSlip(packSlipReq, processorData.getAccessToken()).getInputStream());
				logger.info("*** Packing Slip Added");
				int currentTranscript = 0;
				int failedToAdd = 0;
				for (BlankCredentialDistribution bcd : bcdList) {
					ReportData data = prepareBlankTranscriptData(bcd,mincode);
					ReportOptions options = new ReportOptions();
					options.setReportFile("Transcript");
					options.setReportName("Transcript.pdf");
					ReportRequest reportParams = new ReportRequest();
					reportParams.setOptions(options);
					reportParams.setData(data);
					byte[] bytesSAR = webClient.post().uri(educDistributionApiConstants.getTranscriptReport()).headers(h -> h.setBearerAuth(processorData.getAccessToken())).body(BodyInserters.fromValue(reportParams)).retrieve().bodyToMono(byte[].class).block();
					if (bytesSAR != null) {
						for(int i=1;i<=bcd.getQuantity();i++) {
							locations.add(new ByteArrayInputStream(bytesSAR));
						}
						currentTranscript++;
						logger.debug("*** Added PDFs {}/{} Current Credential {}", currentTranscript, bcdList.size(), bcd.getCredentialTypeCode());
					} else {
						failedToAdd++;
						logger.debug("*** Failed to Add PDFs {} Current Credential {}", failedToAdd, bcd.getCredentialTypeCode());
					}
				}
				mergeDocuments(processorData,mincode,"/EDGRAD.T.","YED4",locations);
				numberOfPdfs++;
				logger.info("*** Transcript Documents Merged");
			} catch (IOException e) {
				logger.debug(EXCEPTION,e.getLocalizedMessage());
			}
		}
		return numberOfPdfs;
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
			processCertificatePrintFile(packSlipReq,obj.getYedbCertificatePrintRequest(),mincode,currentSlipCount,obj,processorData, "YEDB");
			numberOfPdfs++;
			logger.info("*** YEDB Documents Merged");
		}
		return numberOfPdfs;
	}

	private int processYed2Certificate(DistributionPrintRequest obj, int currentSlipCount, ReportRequest packSlipReq, String mincode,ProcessorData processorData, int numberOfPdfs) {
		if (obj.getYed2CertificatePrintRequest() != null) {
			currentSlipCount++;
			processCertificatePrintFile(packSlipReq,obj.getYed2CertificatePrintRequest(),mincode,currentSlipCount,obj,processorData, "YED2");
			numberOfPdfs++;
			logger.info("*** YED2 Documents Merged");
		}
		return numberOfPdfs;
	}
	private void processCertificatePrintFile(ReportRequest packSlipReq, CertificatePrintRequest certificatePrintRequest, String mincode, int currentSlipCount, DistributionPrintRequest obj, ProcessorData processorData,String paperType) {
		PackingSlipRequest request = PackingSlipRequest.builder().mincode(mincode).currentSlip(currentSlipCount).total(obj.getTotal()).paperType(paperType).build();
		mergeCertificates(packSlipReq, certificatePrintRequest, request,processorData);
	}

	@SneakyThrows
	private void mergeCertificates(ReportRequest packSlipReq, CertificatePrintRequest certificatePrintRequest,PackingSlipRequest request,ProcessorData processorData) {
		List<BlankCredentialDistribution> bcdList = certificatePrintRequest.getBlankCertificateList();
		String mincode = request.getMincode();
		String paperType = request.getPaperType();
		List<InputStream> locations=new ArrayList<>();
		int totalQuantity = certificatePrintRequest.getBlankCertificateList().get(0).getQuantity() * bcdList.size();
		setExtraDataForPackingSlip(packSlipReq,paperType,request.getTotal(),totalQuantity,request.getCurrentSlip(),certificatePrintRequest.getBatchId());
		try {
			locations.add(reportService.getPackingSlip(packSlipReq,processorData.getAccessToken()).getInputStream());
			int currentCertificate = 0;
			int failedToAdd = 0;
			for (BlankCredentialDistribution bcd : bcdList) {
				ReportData data = prepareBlankCertData(bcd);
				ReportOptions options = new ReportOptions();
				options.setReportFile("certificate");
				options.setReportName("Certificate.pdf");
				ReportRequest reportParams = new ReportRequest();
				reportParams.setOptions(options);
				reportParams.setData(data);
				String json = jsonTransformer.marshall(reportParams);
				System.out.println(educDistributionApiConstants.getCertificateReport());
				System.out.println();
				System.out.println(json);
				byte[] bytesSAR = webClient.post().uri(educDistributionApiConstants.getCertificateReport()).headers(h -> h.setBearerAuth(processorData.getAccessToken())).body(BodyInserters.fromValue(reportParams)).retrieve().bodyToMono(byte[].class).block();
				if (bytesSAR != null) {
					try (OutputStream out = new FileOutputStream("target/" + reportParams.getOptions().getReportFile() + "_" + bcd.getCredentialTypeCode() + ".pdf")) {
						out.write(bytesSAR);
					}
					for(int i=1;i<=bcd.getQuantity();i++) {
						locations.add(new ByteArrayInputStream(bytesSAR));
					}
					currentCertificate++;
					logger.debug("*** Added PDFs {}/{} Current Credential {}", currentCertificate, bcdList.size(), bcd.getCredentialTypeCode());
				} else {
					failedToAdd++;
					logger.debug("*** Failed to Add PDFs {} Current Credential {}", failedToAdd, bcd.getCredentialTypeCode());
				}
			}
			mergeDocuments(processorData,mincode,"/EDGRAD.C.",paperType,locations);
		} catch (IOException e) {
			logger.debug(EXCEPTION,e.getMessage());
		}
	}

	private ReportData prepareBlankCertData(BlankCredentialDistribution bcd) {
		ReportData data = new ReportData();
		Student std = new Student();
		if(bcd.getCredentialTypeCode().equalsIgnoreCase("E") || bcd.getCredentialTypeCode().equalsIgnoreCase("A") || bcd.getCredentialTypeCode().equalsIgnoreCase("EI") || bcd.getCredentialTypeCode().equalsIgnoreCase("AI")) {
			std.setEnglishCert(bcd.getCredentialTypeCode());
		} else if(bcd.getCredentialTypeCode().equalsIgnoreCase("F") || bcd.getCredentialTypeCode().equalsIgnoreCase("S") || bcd.getCredentialTypeCode().equalsIgnoreCase("SCF")) {
			std.setFrenchCert(bcd.getCredentialTypeCode());
		}
		Certificate cert = new Certificate();
		cert.setIssued(EducDistributionApiUtils.formatIssueDateForReportJasper(new java.sql.Date(System.currentTimeMillis()).toString()));
		cert.setCertStyle("Blank");
		OrderType orderType = new OrderType();
		orderType.setName("Certificate");
		CertificateType cType = new CertificateType();
		cType.setReportName(bcd.getCredentialTypeCode());
		PaperType pType = new PaperType();
		pType.setCode(bcd.getPaperType());
		cType.setPaperType(pType);
		orderType.setCertificateType(cType);

		cert.setOrderType(orderType);
		data.setStudent(std);
		data.setCertificate(cert);
		data.setIssueDate(EducDistributionApiUtils.formatIssueDateForReportJasper(new java.sql.Date(System.currentTimeMillis()).toString()));
		return data;
	}

	private ReportData prepareBlankTranscriptData(BlankCredentialDistribution bcd, String mincode) {
		ReportData data = new ReportData();
		Transcript tran = new Transcript();
		tran.setInterim("true");
		Code code = new Code();
		code.setCode(bcd.getCredentialTypeCode());
		tran.setTranscriptTypeCode(code);
		tran.setIssueDate(EducDistributionApiUtils.formatIssueDateForReportJasper(new java.sql.Date(System.currentTimeMillis()).toString()));
		code = new Code();
		code.setCode("BLANK");
		GradProgram gP = new GradProgram();
		gP.setCode(code);
		data.setGradProgram(gP);
		data.setLogo(StringUtils.startsWith(mincode, "098") ? "YU" : "BC");
		data.setTranscript(tran);
		data.setIssueDate(EducDistributionApiUtils.formatIssueDateForReportJasper(new java.sql.Date(System.currentTimeMillis()).toString()));
		return data;
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
}
