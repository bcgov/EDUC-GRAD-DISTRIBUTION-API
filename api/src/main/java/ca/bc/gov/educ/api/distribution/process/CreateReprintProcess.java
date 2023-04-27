package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
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
public class CreateReprintProcess extends BaseProcess {
	
	private static Logger logger = LoggerFactory.getLogger(CreateReprintProcess.class);

	@Override
	public ProcessorData fire(ProcessorData processorData) {
		long startTime = System.currentTimeMillis();
		logger.debug("************* TIME START  ************ {}",startTime);
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
			CommonSchool schoolDetails = getBaseSchoolDetails(obj,mincode,exception);
			if(schoolDetails != null) {
				logger.debug("*** School Details Acquired {}", schoolDetails.getSchoolName());

				ReportRequest packSlipReq = reportService.preparePackingSlipData(schoolDetails, processorData.getBatchId());

				if(obj.getSchoolDistributionRequest() != null) {
					ReportRequest schoolDistributionReportRequest = reportService.prepareSchoolDistributionReportData(obj.getSchoolDistributionRequest(), processorData.getBatchId(),schoolDetails);
					createAndSaveDistributionReport(schoolDistributionReportRequest,mincode,processorData);
					numberOfPdfs++;
				}
				numberOfPdfs = processYed2Certificate(obj,currentSlipCount,packSlipReq,mincode,processorData,numberOfPdfs);
				numberOfPdfs = processYedbCertificate(obj,currentSlipCount,packSlipReq,mincode,processorData,numberOfPdfs);
				numberOfPdfs = processYedrCertificate(obj,currentSlipCount,packSlipReq,mincode,processorData,numberOfPdfs);

				logger.debug("PDFs Merged {}", schoolDetails.getSchoolName());
				logger.debug("School {}/{}",counter,mapDist.size());
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
		processorData.setDistributionResponse(response);
		return processorData;
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
			processCertificatePrintFile(packSlipReq,obj.getYedbCertificatePrintRequest(),mincode,currentSlipCount,obj,processorData,"YEDB");
			numberOfPdfs++;
			logger.debug("*** YEDB Documents Merged");
		}
		return numberOfPdfs;
	}

	private int processYed2Certificate(DistributionPrintRequest obj, int currentSlipCount, ReportRequest packSlipReq, String mincode, ProcessorData processorData, int numberOfPdfs) {
		if (obj.getYed2CertificatePrintRequest() != null) {
			currentSlipCount++;
			processCertificatePrintFile(packSlipReq,obj.getYed2CertificatePrintRequest(),mincode,currentSlipCount,obj,processorData,"YED2");
			numberOfPdfs++;
			logger.debug("*** YED2 Documents Merged");
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
			locations.add(reportService.getPackingSlip(packSlipReq).getInputStream());
			int currentCertificate = 0;
			int failedToAdd = 0;
			for (StudentCredentialDistribution scd : scdList) {
				ReportData data = restService.executeGet(educDistributionApiConstants.getCertDataReprint(), ReportData.class, scd.getPen());
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
				byte[] bytesSAR = restService.executePost(educDistributionApiConstants.getCertificateReport(), byte[].class, reportParams);
				if (bytesSAR != null) {
					locations.add(new ByteArrayInputStream(bytesSAR));
					currentCertificate++;
					logger.debug("*** Added PDFs {}/{} Current student {}", currentCertificate, scdList.size(), scd.getStudentID());
				} else {
					failedToAdd++;
					logger.debug("*** Failed to Add PDFs {} Current student {}", failedToAdd, scd.getStudentID());
				}
			}
			mergeDocumentsPDFs(processorData,mincode,"02","/EDGRAD.C.",paperType,locations);
		} catch (IOException e) {
			logger.debug(EXCEPTION,e.getMessage());
		}
	}

	private void createAndSaveDistributionReport(ReportRequest distributionRequest,String mincode,ProcessorData processorData) {
		List<InputStream> locations=new ArrayList<>();
		try {
			byte[] bytesSAR = restService.executePost(educDistributionApiConstants.getSchoolDistributionReport(), byte[].class, distributionRequest);
			if(bytesSAR != null) {
				locations.add(new ByteArrayInputStream(bytesSAR));
			}
			mergeDocumentsPDFs(processorData,mincode,"02","/EDGRAD.R.","324W",locations);
		} catch (Exception e) {
			logger.debug(EXCEPTION,e.getMessage());
		}
	}
}
