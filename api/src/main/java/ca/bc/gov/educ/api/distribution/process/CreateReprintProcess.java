package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.service.AccessTokenService;
import ca.bc.gov.educ.api.distribution.service.GradStudentService;
import ca.bc.gov.educ.api.distribution.service.ReportService;
import ca.bc.gov.educ.api.distribution.service.SchoolService;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils;
import ca.bc.gov.educ.api.distribution.util.GradValidation;
import ca.bc.gov.educ.api.distribution.util.SFTPUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

@Data
@Component
@NoArgsConstructor
public class CreateReprintProcess implements DistributionProcess {
	
	private static Logger logger = LoggerFactory.getLogger(CreateReprintProcess.class);
	private static final String LOC = "/tmp/";
	private static final String DEL = "/";
	private static final String EXCEPTION = "IO Exp {} ";

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
		ExceptionMessage exception = new ExceptionMessage();
		Map<String,DistributionPrintRequest> mapDist = processorData.getMapDistribution();
		Long batchId = processorData.getBatchId();
		int numberOfPdfs = 0;
		int counter=0;
		for (Map.Entry<String, DistributionPrintRequest> entry : mapDist.entrySet()) {
			counter++;
			int currentSlipCount = 0;
			String mincode = entry.getKey();
			SchoolTrax schoolDetails = schoolService.getSchoolDetails(mincode,processorData.getAccessToken(),exception);
			if(schoolDetails != null) {
				logger.info("*** School Details Acquired {}", schoolDetails.getSchoolName());

				ReportRequest packSlipReq = reportService.preparePackingSlipData(schoolDetails, processorData.getBatchId());
				DistributionPrintRequest obj = entry.getValue();
				if(obj.getSchoolDistributionRequest() != null) {
					ReportRequest schoolDistributionReportRequest = reportService.prepareSchoolDistributionReportData(obj.getSchoolDistributionRequest(), processorData.getBatchId(),schoolDetails);
					createAndSaveDistributionReport(schoolDistributionReportRequest,mincode,exception,processorData);
					numberOfPdfs++;
				}
				if (obj.getYed2CertificatePrintRequest() != null) {
					currentSlipCount++;
					CertificatePrintRequest certificatePrintRequest = obj.getYed2CertificatePrintRequest();
					PackingSlipRequest request = PackingSlipRequest.builder().mincode(mincode).currentSlip(currentSlipCount).total(obj.getTotal()).paperType("YED2").build();
					mergeCertificates(packSlipReq, certificatePrintRequest, request, exception, processorData);
					numberOfPdfs++;
					logger.info("*** YED2 Documents Merged");
				}
				if (obj.getYedbCertificatePrintRequest() != null) {
					currentSlipCount++;
					CertificatePrintRequest certificatePrintRequest = obj.getYedbCertificatePrintRequest();
					PackingSlipRequest request = PackingSlipRequest.builder().mincode(mincode).currentSlip(currentSlipCount).total(obj.getTotal()).paperType("YEDB").build();
					mergeCertificates(packSlipReq, certificatePrintRequest, request, exception, processorData);
					numberOfPdfs++;
					logger.info("*** YEDB Documents Merged");
				}
				if (obj.getYedrCertificatePrintRequest() != null) {
					currentSlipCount++;
					CertificatePrintRequest certificatePrintRequest = obj.getYedrCertificatePrintRequest();
					PackingSlipRequest request = PackingSlipRequest.builder().mincode(mincode).currentSlip(currentSlipCount).total(obj.getTotal()).paperType("YEDR").build();
					mergeCertificates(packSlipReq, certificatePrintRequest, request, exception, processorData);
					numberOfPdfs++;
					logger.info("*** YEDR Documents Merged");
				}
				logger.info("PDFs Merged {}", schoolDetails.getSchoolName());
				if (counter % 50 == 0) {
					accessTokenService.fetchAccessToken(processorData);
				}
			}
		}
		createZipFile(batchId);
		createControlFile(batchId,numberOfPdfs);
		sftpUtils.sftpUpload(batchId);
		long endTime = System.currentTimeMillis();
		long diff = (endTime - startTime)/1000;
		logger.info("************* TIME Taken  ************ {} secs",diff);
		response.setMergeProcessResponse("Merge Successful and File Uploaded");
		processorData.setDistributionResponse(response);
		return processorData;
	}

	@SneakyThrows
	private void createControlFile(Long batchId, int numberOfPdfs) {
		try (FileOutputStream fos = new FileOutputStream(LOC + "EDGRAD.BATCH." + batchId + ".txt")) {
			byte[] contentInBytes = String.valueOf(numberOfPdfs).getBytes();
			fos.write(contentInBytes);
			fos.flush();
		} catch (IOException e) {
			logger.debug("IO Exp {}", e.getMessage());
		}

	}

	@SneakyThrows
	private void createZipFile(Long batchId) {
		String sourceFile = LOC+batchId;
		try (FileOutputStream fos = new FileOutputStream(LOC + "EDGRAD.BATCH." + batchId + ".zip")) {
			ZipOutputStream zipOut = new ZipOutputStream(fos);
			File fileToZip = new File(sourceFile);
			EducDistributionApiUtils.zipFile(fileToZip, fileToZip.getName(), zipOut);
			zipOut.close();

		} catch (IOException e) {
			logger.debug(EXCEPTION,e.getMessage());
		}

	}

	private void setExtraDataForPackingSlip(ReportRequest packSlipReq, String paperType, int total, int quantity, int currentSlip, String orderType, Long batchId) {
		packSlipReq.getData().getPackingSlip().setTotal(total);
		packSlipReq.getData().getPackingSlip().setCurrent(currentSlip);
		packSlipReq.getData().getPackingSlip().setQuantity(quantity);
		packSlipReq.getData().getPackingSlip().getOrderType().getPackingSlipType().getPaperType().setCode(paperType);
		packSlipReq.getData().getPackingSlip().getOrderType().setName(orderType);
		packSlipReq.getData().getPackingSlip().setOrderNumber(batchId);
	}

	private void mergeCertificates(ReportRequest packSlipReq, CertificatePrintRequest certificatePrintRequest,PackingSlipRequest request, ExceptionMessage exception,ProcessorData processorData) {
		List<StudentCredentialDistribution> scdList = certificatePrintRequest.getCertificateList();
		String mincode = request.getMincode();
		String paperType = request.getPaperType();
		List<InputStream> locations=new ArrayList<>();
		setExtraDataForPackingSlip(packSlipReq,paperType,request.getTotal(),scdList.size(),request.getCurrentSlip(),"Certificate", certificatePrintRequest.getBatchId());
		try {
			locations.add(reportService.getPackingSlip(packSlipReq,processorData.getAccessToken(),exception).getInputStream());
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
			PDFMergerUtility objs = new PDFMergerUtility();
			Path path = Paths.get(LOC+processorData.getBatchId()+DEL+mincode+DEL);
			Files.createDirectories(path);
			objs.setDestinationFileName(LOC+processorData.getBatchId()+DEL+mincode+"/EDGRAD.C."+paperType+"."+ EducDistributionApiUtils.getFileName()+".pdf");
			objs.addSources(locations);
			objs.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
		} catch (IOException e) {
			logger.debug(EXCEPTION,e.getMessage());
		}
	}

	private void createAndSaveDistributionReport(ReportRequest distributionRequest,String mincode,ExceptionMessage exception,ProcessorData processorData) {
		List<InputStream> locations=new ArrayList<>();
		try {

			byte[] bytesSAR = webClient.post().uri(educDistributionApiConstants.getDistributionReport()).headers(h -> h.setBearerAuth(processorData.getAccessToken())).body(BodyInserters.fromValue(distributionRequest)).retrieve().bodyToMono(byte[].class).block();
			if(bytesSAR != null) {
				locations.add(new ByteArrayInputStream(bytesSAR));
			}
			PDFMergerUtility objs = new PDFMergerUtility();
			Path path = Paths.get(LOC+processorData.getBatchId()+DEL+mincode+DEL);
			Files.createDirectories(path);
			objs.setDestinationFileName(LOC+processorData.getBatchId()+DEL+mincode+"/EDGRAD.R.324W."+ EducDistributionApiUtils.getFileName()+".pdf");
			objs.addSources(locations);
			objs.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
		} catch (IOException e) {
			logger.debug(EXCEPTION,e.getMessage());
			exception.setExceptionName("Error building Distribution Report");
		}
	}
}
