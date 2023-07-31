package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Component
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CreateBlankCredentialProcess extends BaseProcess {
	
	private static Logger logger = LoggerFactory.getLogger(CreateBlankCredentialProcess.class);

	@Override
	public ProcessorData fire(ProcessorData processorData) {
		long startTime = System.currentTimeMillis();
		logger.debug("************* TIME START  ************ {}",startTime);
		DistributionResponse response = new DistributionResponse();
		ExceptionMessage exception = new ExceptionMessage();
		DistributionRequest distributionRequest = processorData.getDistributionRequest();
		Map<String, DistributionPrintRequest> mapDist = distributionRequest.getMapDist();
		StudentSearchRequest searchRequest = distributionRequest.getStudentSearchRequest();
		Long batchId = processorData.getBatchId();
		int numberOfPdfs = 0;
		int counter=0;
		for (String mincode : mapDist.keySet()) {
			counter++;
			int currentSlipCount = 0;
			DistributionPrintRequest obj = mapDist.get(mincode);
			CommonSchool schoolDetails = getBaseSchoolDetails(obj,mincode,exception);
			if(schoolDetails != null) {
				logger.debug("*** School Details Acquired {}", schoolDetails.getSchoolName());

				ReportRequest packSlipReq = reportService.preparePackingSlipData(schoolDetails, processorData.getBatchId());
				numberOfPdfs = processYed4Transcript(obj,currentSlipCount,packSlipReq,mincode,processorData,numberOfPdfs);
				numberOfPdfs = processYed2Certificate(obj,currentSlipCount,packSlipReq,mincode,processorData,numberOfPdfs);
				numberOfPdfs = processYedbCertificate(obj,currentSlipCount,packSlipReq,mincode,processorData,numberOfPdfs);
				numberOfPdfs = processYedrCertificate(obj,currentSlipCount,packSlipReq,mincode,processorData,numberOfPdfs);

				logger.debug("PDFs Merged {}", schoolDetails.getSchoolName());
				logger.debug("{} School {}/{}",mincode,counter,mapDist.size());
				if (counter % 50 == 0) {
					restUtils.fetchAccessToken(processorData);
				}
			}
		}
		postingProcess(batchId,processorData,numberOfPdfs);
		long endTime = System.currentTimeMillis();
		long diff = (endTime - startTime)/1000;
		logger.debug("************* TIME Taken  ************ {} secs",diff);
		response.setMergeProcessResponse("Merge Successful and File Uploaded");
		response.setNumberOfPdfs(numberOfPdfs);
		response.setBatchId(processorData.getBatchId());
		response.setLocalDownload(processorData.getLocalDownload());
		response.setActivityCode(distributionRequest.getActivityCode());
		response.setStudentSearchRequest(searchRequest);
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
			setExtraDataForPackingSlip(packSlipReq, "YED4", obj.getTotal(), totalQuantity, currentSlipCount, "Transcript", transcriptPrintRequest.getBatchId());
			try {
				locations.add(reportService.getPackingSlip(packSlipReq).getInputStream());
				logger.debug("*** Packing Slip Added");
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
					byte[] bytesSAR = restService.executePost(educDistributionApiConstants.getTranscriptReport(), byte[].class, reportParams);
					if (bytesSAR != null) {
						for(int i=1;i<=bcd.getQuantity();i++) {
							locations.add(new ByteArrayInputStream(bytesSAR));
						}
						currentTranscript++;
						logger.debug("*** Added transcript PDFs {}/{} Current Credential {}", currentTranscript, bcdList.size(), bcd.getCredentialTypeCode());
					} else {
						failedToAdd++;
						logger.info("*** Failed to Add transcript PDFs {} Current Credential {} in batch {}", failedToAdd, bcd.getCredentialTypeCode(), processorData.getBatchId());
					}
				}
				mergeDocumentsPDFs(processorData,mincode,"02","/EDGRAD.T.","YED4",locations);
				numberOfPdfs++;
				logger.debug("*** Transcript Documents Merged");
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
			logger.debug("*** YEDR Documents Merged");
		}
		return numberOfPdfs;
	}

	private int processYedbCertificate(DistributionPrintRequest obj, int currentSlipCount, ReportRequest packSlipReq, String mincode,ProcessorData processorData, int numberOfPdfs) {
		if (obj.getYedbCertificatePrintRequest() != null) {
			currentSlipCount++;
			processCertificatePrintFile(packSlipReq,obj.getYedbCertificatePrintRequest(),mincode,currentSlipCount,obj,processorData, "YEDB");
			numberOfPdfs++;
			logger.debug("*** YEDB Documents Merged");
		}
		return numberOfPdfs;
	}

	private int processYed2Certificate(DistributionPrintRequest obj, int currentSlipCount, ReportRequest packSlipReq, String mincode, ProcessorData processorData, int numberOfPdfs) {
		if (obj.getYed2CertificatePrintRequest() != null) {
			currentSlipCount++;
			processCertificatePrintFile(packSlipReq,obj.getYed2CertificatePrintRequest(),mincode,currentSlipCount,obj,processorData, "YED2");
			numberOfPdfs++;
			logger.debug("*** YED2 Documents Merged");
		}
		return numberOfPdfs;
	}
	private void processCertificatePrintFile(ReportRequest packSlipReq, CertificatePrintRequest certificatePrintRequest, String mincode, int currentSlipCount, DistributionPrintRequest obj, ProcessorData processorData,String paperType) {
		PackingSlipRequest request = PackingSlipRequest.builder().mincode(mincode).currentSlip(currentSlipCount).total(obj.getTotal()).paperType(paperType).build();
		mergeCertificates(packSlipReq, certificatePrintRequest, request,processorData);
	}

	private void mergeCertificates(ReportRequest packSlipReq, CertificatePrintRequest certificatePrintRequest,PackingSlipRequest request, ProcessorData processorData) {
		List<BlankCredentialDistribution> bcdList = certificatePrintRequest.getBlankCertificateList();
		String mincode = request.getMincode();
		String paperType = request.getPaperType();
		List<InputStream> locations=new ArrayList<>();
		int totalQuantity = certificatePrintRequest.getBlankCertificateList().get(0).getQuantity() * bcdList.size();
		setExtraDataForPackingSlip(packSlipReq,paperType,request.getTotal(),totalQuantity,request.getCurrentSlip(),"Certificate",certificatePrintRequest.getBatchId());
		try {
			locations.add(reportService.getPackingSlip(packSlipReq).getInputStream());
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
				byte[] bytesSAR = restService.executePost(educDistributionApiConstants.getCertificateReport(), byte[].class, reportParams);
				if (bytesSAR != null) {
					for(int i=1;i<=bcd.getQuantity();i++) {
						locations.add(new ByteArrayInputStream(bytesSAR));
					}
					currentCertificate++;
					logger.debug("*** Added {} Certificate PDFs {}/{} Current Credential {}", bcd.getCredentialTypeCode(), currentCertificate, bcdList.size(), bcd.getCredentialTypeCode());
				} else {
					failedToAdd++;
					logger.info("*** Failed to Add {} Certificate PDFs {} Current Credential {} in batch {}", bcd.getCredentialTypeCode(),failedToAdd, bcd.getCredentialTypeCode(), processorData.getBatchId());
				}
			}
			mergeDocumentsPDFs(processorData,mincode,"02","/EDGRAD.C.",paperType,locations);
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
		tran.setInterim("false");
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

}
