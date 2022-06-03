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
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

@Data
@Component
@NoArgsConstructor
public class MergeProcess implements DistributionProcess {
	
	private static Logger logger = LoggerFactory.getLogger(MergeProcess.class);

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
				List<Student> studListNonGrad = new ArrayList<>();
				ReportRequest packSlipReq = reportService.preparePackingSlipData(schoolDetails, processorData.getBatchId());
				DistributionPrintRequest obj = entry.getValue();
				if(obj.getSchoolDistributionRequest() != null) {
					ReportRequest schoolDistributionReportRequest = reportService.prepareSchoolDistributionReportData(obj.getSchoolDistributionRequest(), processorData.getBatchId(),schoolDetails);
					createAndSaveDistributionReport(schoolDistributionReportRequest,mincode,processorData);
					numberOfPdfs++;
				}
				if (obj.getTranscriptPrintRequest() != null) {
					TranscriptPrintRequest transcriptPrintRequest = obj.getTranscriptPrintRequest();
					List<StudentCredentialDistribution> scdList = transcriptPrintRequest.getTranscriptList();
					List<InputStream> locations = new ArrayList<>();
					currentSlipCount++;
					setExtraDataForPackingSlip(packSlipReq, "YED4", obj.getTotal(), scdList.size(), 1, "Transcript", transcriptPrintRequest.getBatchId());
					try {
						locations.add(reportService.getPackingSlip(packSlipReq, processorData.getAccessToken(), exception).getInputStream());
						logger.info("*** Packing Slip Added");
						int currentTranscript = 0;
						int failedToAdd = 0;
						for (StudentCredentialDistribution scd : scdList) {
							if(scd.getNonGradReasons() != null && !scd.getNonGradReasons().isEmpty()) {
								Student objStd = prepareStudentObj(scd,studListNonGrad);
								if(objStd != null)
									studListNonGrad.add(objStd);
							}
							InputStreamResource transcriptPdf = webClient.get().uri(String.format(educDistributionApiConstants.getTranscript(), scd.getStudentID(), scd.getCredentialTypeCode(), scd.getDocumentStatusCode())).headers(h -> h.setBearerAuth(processorData.getAccessToken())).retrieve().bodyToMono(InputStreamResource.class).block();
							if (transcriptPdf != null) {
								locations.add(transcriptPdf.getInputStream());
								currentTranscript++;
								logger.debug("*** Added PDFs {}/{} Current student {}", currentTranscript, scdList.size(), scd.getStudentID());
							} else {
								failedToAdd++;
								logger.debug("*** Failed to Add PDFs {} Current student {}", failedToAdd, scd.getStudentID());
							}
						}
						mergeDocuments(processorData,mincode,"/EDGRAD.T.","YED4",locations);
						numberOfPdfs++;
						logger.info("*** Transcript Documents Merged");
					} catch (IOException e) {
						logger.debug(EXCEPTION,e.getLocalizedMessage());
					}
				}
				if (obj.getYed2CertificatePrintRequest() != null) {
					currentSlipCount++;
					CertificatePrintRequest certificatePrintRequest = obj.getYed2CertificatePrintRequest();
					PackingSlipRequest request = PackingSlipRequest.builder().mincode(mincode).currentSlip(currentSlipCount).total(obj.getTotal()).paperType("YED2").build();
					mergeCertificates(packSlipReq, certificatePrintRequest, request, exception, processorData,studListNonGrad);
					numberOfPdfs++;
					logger.info("*** YED2 Documents Merged");
				}
				if (obj.getYedbCertificatePrintRequest() != null) {
					currentSlipCount++;
					CertificatePrintRequest certificatePrintRequest = obj.getYedbCertificatePrintRequest();
					PackingSlipRequest request = PackingSlipRequest.builder().mincode(mincode).currentSlip(currentSlipCount).total(obj.getTotal()).paperType("YEDB").build();
					mergeCertificates(packSlipReq, certificatePrintRequest, request, exception, processorData,studListNonGrad);
					numberOfPdfs++;
					logger.info("*** YEDB Documents Merged");
				}
				if (obj.getYedrCertificatePrintRequest() != null) {
					currentSlipCount++;
					CertificatePrintRequest certificatePrintRequest = obj.getYedrCertificatePrintRequest();
					PackingSlipRequest request = PackingSlipRequest.builder().mincode(mincode).currentSlip(currentSlipCount).total(obj.getTotal()).paperType("YEDR").build();
					mergeCertificates(packSlipReq, certificatePrintRequest, request, exception, processorData,studListNonGrad);
					numberOfPdfs++;
					logger.info("*** YEDR Documents Merged");
				}
				if(!studListNonGrad.isEmpty()) {
					createAndSaveNonGradReport(schoolDetails,studListNonGrad,mincode,processorData.getAccessToken());
				}
				logger.info("PDFs Merged {}", schoolDetails.getSchoolName());
				if (counter % 50 == 0) {
					accessTokenService.fetchAccessToken(processorData);
				}
				logger.info("School {}/{}",counter,mapDist.size());
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

	private Student prepareStudentObj(StudentCredentialDistribution scd, List<Student> studListNonGrad) {
		if(scd.getStudentGrade().equalsIgnoreCase("AD") || scd.getStudentGrade().equalsIgnoreCase("12")) {
			Student std = new Student();
			std.setFirstName(scd.getLegalFirstName());
			std.setLastName(scd.getLegalLastName());
			std.setMiddleName(scd.getLegalMiddleNames());
			Pen pen = new Pen();
			pen.setPen(scd.getPen());
			std.setPen(pen);
			std.setGrade(scd.getStudentGrade());
			std.setGraduationData(new GraduationData());
			std.setNonGradReasons(getNonGradReasons(scd.getNonGradReasons()));

			Student scObj = studListNonGrad.stream().filter(pr -> pr.getPen().getPen().compareTo(std.getPen().getPen()) == 0)
					.findAny()
					.orElse(null);
			if (scObj == null)
				return std;
		}
		return null;
	}
	private void createControlFile(Long batchId,int numberOfPdfs) {
		try(FileOutputStream fos = new FileOutputStream("/tmp/EDGRAD.BATCH." + batchId + ".txt")) {
			byte[] contentInBytes = String.valueOf(numberOfPdfs).getBytes();
			fos.write(contentInBytes);
			fos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void createZipFile(Long batchId) {
		StringBuilder sourceFileBuilder = new StringBuilder().append(LOC).append(batchId);
		try(FileOutputStream fos = new FileOutputStream(LOC+"EDGRAD.BATCH." + batchId + ".zip")) {
			ZipOutputStream zipOut = new ZipOutputStream(fos);
			File fileToZip = new File(sourceFileBuilder.toString());
			EducDistributionApiUtils.zipFile(fileToZip, fileToZip.getName(), zipOut);
			zipOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private List<NonGradReason> getNonGradReasons(List<GradRequirement> nonGradReasons) {
		List<NonGradReason> nList = new ArrayList<>();
		if (nonGradReasons != null) {
			for (GradRequirement gR : nonGradReasons) {
				NonGradReason obj = new NonGradReason();
				obj.setCode(gR.getRule());
				obj.setDescription(gR.getDescription());
				nList.add(obj);
			}
		}
		return nList;
	}

	private void setExtraDataForPackingSlip(ReportRequest packSlipReq, String paperType, int total, int quantity, int currentSlip, String orderType, Long batchId) {
		packSlipReq.getData().getPackingSlip().setTotal(total);
		packSlipReq.getData().getPackingSlip().setCurrent(currentSlip);
		packSlipReq.getData().getPackingSlip().setQuantity(quantity);
		packSlipReq.getData().getPackingSlip().getOrderType().getPackingSlipType().getPaperType().setCode(paperType);
		packSlipReq.getData().getPackingSlip().getOrderType().setName(orderType);
		packSlipReq.getData().getPackingSlip().setOrderNumber(batchId);
	}

	private void mergeCertificates(ReportRequest packSlipReq, CertificatePrintRequest certificatePrintRequest, PackingSlipRequest request, ExceptionMessage exception, ProcessorData processorData, List<Student> studListNonGrad) {
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
				if(scd.getNonGradReasons() != null && !scd.getNonGradReasons().isEmpty()) {
					Student objStd = prepareStudentObj(scd,studListNonGrad);
					if(objStd != null)
						studListNonGrad.add(objStd);
				}
				InputStreamResource certificatePdf = webClient.get().uri(String.format(educDistributionApiConstants.getCertificate(),scd.getStudentID(),scd.getCredentialTypeCode(),scd.getDocumentStatusCode())).headers(h -> h.setBearerAuth(processorData.getAccessToken())).retrieve().bodyToMono(InputStreamResource.class).block();
				if(certificatePdf != null) {
					locations.add(certificatePdf.getInputStream());
					currentCertificate++;
					logger.debug("*** Added PDFs {}/{} Current student {}",currentCertificate,scdList.size(),scd.getStudentID());
				}else {
					failedToAdd++;
					logger.debug("*** Failed to Add PDFs {} Current student {} papertype : {}",failedToAdd,scd.getStudentID(),paperType);
				}
			}
			mergeDocuments(processorData,mincode,"/EDGRAD.C.",paperType,locations);
		} catch (IOException e) {
			logger.debug(EXCEPTION,e.getLocalizedMessage());
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
				byte[] encoded = Base64.encodeBase64(bytesSAR);
				String encodedPdf= new String(encoded, StandardCharsets.US_ASCII);
				saveSchoolDistributionReport(encodedPdf,mincode,processorData.getAccessToken(),"GRAD");
			}
			mergeDocuments(processorData,mincode,"/EDGRAD.R.","324W",locations);
		} catch (Exception e) {
			logger.debug(EXCEPTION,e.getLocalizedMessage());
		}
	}

	private void createAndSaveNonGradReport(SchoolTrax schoolDetails, List<Student> studListNonGrad, String mincode, String accessToken) {
		ReportData nongradProjected = new ReportData();
		School schObj = new School();
		schObj.setMincode(schoolDetails.getMinCode());
		schObj.setName(schoolDetails.getSchoolName());
		schObj.setStudents(studListNonGrad);
		nongradProjected.setSchool(schObj);
		nongradProjected.setOrgCode(StringUtils.startsWith(nongradProjected.getSchool().getMincode(), "098") ? "YU" : "BC");
		nongradProjected.setIssueDate(EducDistributionApiUtils.formatIssueDateForReportJasper(new java.sql.Date(System.currentTimeMillis()).toString()));
		nongradProjected.setReportNumber("TRAX241B");
		nongradProjected.setReportTitle("Grade 12 Examinations and Transcripts");
		nongradProjected.setReportSubTitle("Grade 12 and Adult Students Not Able to Graduate on Grad Requirements");
		ReportOptions options = new ReportOptions();
		options.setReportFile(String.format("%s_%s00_NONGRAD",mincode, LocalDate.now().getYear()));
		options.setReportName(String.format("%s_%s00_NONGRAD.pdf",mincode, LocalDate.now().getYear()));
		ReportRequest reportParams = new ReportRequest();
		reportParams.setOptions(options);
		reportParams.setData(nongradProjected);

		byte[] bytesSAR = webClient.post().uri(educDistributionApiConstants.getNonGrad())
				.headers(h -> h.setBearerAuth(accessToken)).body(BodyInserters.fromValue(reportParams)).retrieve().bodyToMono(byte[].class).block();
		byte[] encoded = Base64.encodeBase64(bytesSAR);
		assert encoded != null;
		String encodedPdf = new String(encoded, StandardCharsets.US_ASCII);
		saveSchoolDistributionReport(encodedPdf,mincode,accessToken, "NONGRAD");
	}

	private void saveSchoolDistributionReport(String encodedPdf, String mincode, String accessToken, String reportType) {
		SchoolReports requestObj = new SchoolReports();
		requestObj.setReport(encodedPdf);
		requestObj.setSchoolOfRecord(mincode);
		requestObj.setReportTypeCode(reportType);
		webClient.post().uri(educDistributionApiConstants.getUpdateSchoolReport()).headers(h ->h.setBearerAuth(accessToken)).body(BodyInserters.fromValue(requestObj)).retrieve().bodyToMono(SchoolReports.class).block();
	}
}
