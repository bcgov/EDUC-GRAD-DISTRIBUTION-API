package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.internal.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Component
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class MergeProcess extends BaseProcess{
	
	private static Logger logger = LoggerFactory.getLogger(MergeProcess.class);
	private static final String YEARENDDIST = "YEARENDDIST";
	private static final String MONTHLYDIST = "MONTHLYDIST";
	private static final String SUPPDIST = "SUPPDIST";
	private static final String DISTREP_YE_SD = "DISTREP_YE_SD";
	private static final String DISTREP_YE_SC = "DISTREP_YE_SC";
	private static final String ADDRESS_LABEL_SCHL = "ADDRESS_LABEL_SCHL";
	private static final String ADDRESS_LABEL_YE = "ADDRESS_LABEL_YE";
	private static final String DISTREP_SD = "DISTREP_SD";
	private static final String DISTREP_SC = "DISTREP_SC";

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
			CommonSchool schoolDetails = getBaseSchoolDetails(obj,mincode,processorData,exception);
			if(schoolDetails != null) {
				logger.debug("*** School Details Acquired {}", schoolDetails.getSchoolName());
				List<Student> studListNonGrad = new ArrayList<>();
				ReportRequest packSlipReq = reportService.preparePackingSlipData(schoolDetails, processorData.getBatchId());

				if(obj.getSchoolDistributionRequest() != null && MONTHLYDIST.equalsIgnoreCase(processorData.getActivityCode())) {
					ReportRequest schoolDistributionReportRequest = reportService.prepareSchoolDistributionReportData(obj.getSchoolDistributionRequest(), processorData.getBatchId(),schoolDetails);
					createAndSaveDistributionReport(schoolDistributionReportRequest,mincode,processorData);
					numberOfPdfs++;
				}

				Pair<Integer,Integer> pV = processTranscriptPrintRequest(obj,currentSlipCount,packSlipReq,studListNonGrad,processorData,mincode,numberOfPdfs);
				currentSlipCount = pV.getLeft();
				numberOfPdfs = pV.getRight();
				pV = processYed2CertificatePrintRequest(obj,currentSlipCount,packSlipReq,studListNonGrad,processorData,mincode,numberOfPdfs);
				currentSlipCount = pV.getLeft();
				numberOfPdfs = pV.getRight();
				pV = processYedbCertificatePrintRequest(obj,currentSlipCount,packSlipReq,studListNonGrad,processorData,mincode,numberOfPdfs);
				currentSlipCount = pV.getLeft();
				numberOfPdfs = pV.getRight();
				pV = processYedrCertificatePrintRequest(obj,currentSlipCount,packSlipReq,studListNonGrad,processorData,mincode,numberOfPdfs);
				numberOfPdfs = pV.getRight();
				if(!studListNonGrad.isEmpty()) {
					createAndSaveNonGradReport(schoolDetails,studListNonGrad,mincode,restUtils.getAccessToken());
				}
				logger.debug("PDFs Merged {}", schoolDetails.getSchoolName());
				if (counter % 50 == 0) {
					restUtils.fetchAccessToken(processorData);
				}
				logger.debug("School {}/{}",counter,mapDist.size());
			}
		}
		int numberOfCreatedSchoolReports = 0;
		int numberOfProcessedSchoolReports = 0;
		if(MONTHLYDIST.equalsIgnoreCase(processorData.getActivityCode())) {
			logger.debug("***** Create and Store Monthly school reports *****");
			numberOfCreatedSchoolReports += createDistrictSchoolMonthReport(restUtils.getAccessToken(), ADDRESS_LABEL_SCHL, null, null);
			logger.debug("***** Number of created Monthly school reports {} *****", numberOfCreatedSchoolReports);
			logger.debug("***** Distribute Monthly school reports *****");
			numberOfProcessedSchoolReports += processDistrictSchoolDistribution(processorData, ADDRESS_LABEL_SCHL, null, null);
			logger.debug("***** Number of distributed Monthly school reports {} *****", numberOfProcessedSchoolReports);
		}
		if (YEARENDDIST.equalsIgnoreCase(processorData.getActivityCode())) {
			logger.debug("***** Create and Store Year End school reports *****");
			numberOfCreatedSchoolReports += createDistrictSchoolYearEndReport(restUtils.getAccessToken(), ADDRESS_LABEL_YE, DISTREP_YE_SD, DISTREP_YE_SC);
			logger.debug("***** Number of created Year End school reports {} *****", numberOfCreatedSchoolReports);
			logger.debug("***** Distribute Year End school reports *****");
			numberOfProcessedSchoolReports += processDistrictSchoolDistribution(processorData, ADDRESS_LABEL_YE, DISTREP_YE_SD, DISTREP_YE_SC);
			logger.debug("***** Number of distributed Year End school reports {} *****", numberOfProcessedSchoolReports);
		}
		if (SUPPDIST.equalsIgnoreCase(processorData.getActivityCode())) {
			logger.debug("***** Create and Store Supplemental school reports *****");
			numberOfCreatedSchoolReports += createDistrictSchoolSuppReport(restUtils.getAccessToken(), ADDRESS_LABEL_SCHL, null, DISTREP_SC);
			logger.debug("***** Number of created Supplemental school reports {} *****", numberOfCreatedSchoolReports);
			logger.debug("***** Distribute Supplemental school reports *****");
			numberOfProcessedSchoolReports += processDistrictSchoolDistribution(processorData, ADDRESS_LABEL_SCHL, null, DISTREP_SC);
			logger.debug("***** Number of distributed Supplemental school reports {} *****", numberOfProcessedSchoolReports);
		}
		numberOfPdfs += numberOfProcessedSchoolReports;
		postingProcess(batchId,processorData,numberOfPdfs);
		long endTime = System.currentTimeMillis();
		long diff = (endTime - startTime)/1000;
		logger.debug("************* TIME Taken  ************ {} secs",diff);
		response.setMergeProcessResponse("Merge Successful and File Uploaded");
		processorData.setDistributionResponse(response);
		return processorData;
	}

	private Pair<Integer,Integer> processYedrCertificatePrintRequest(DistributionPrintRequest obj, int currentSlipCount, ReportRequest packSlipReq, List<Student> studListNonGrad, ProcessorData processorData, String mincode, int numberOfPdfs) {
		if (obj.getYedrCertificatePrintRequest() != null) {
			currentSlipCount++;
			CertificatePrintRequest certificatePrintRequest = obj.getYedrCertificatePrintRequest();
			PackingSlipRequest request = PackingSlipRequest.builder().mincode(mincode).currentSlip(currentSlipCount).total(obj.getTotal()).paperType("YEDR").build();
			mergeCertificates(packSlipReq, certificatePrintRequest, request,processorData,studListNonGrad);
			numberOfPdfs++;
			logger.debug("*** YEDR Documents Merged");
		}
		return Pair.of(currentSlipCount,numberOfPdfs);
	}

	private Pair<Integer,Integer> processYedbCertificatePrintRequest(DistributionPrintRequest obj, int currentSlipCount, ReportRequest packSlipReq, List<Student> studListNonGrad, ProcessorData processorData, String mincode, int numberOfPdfs) {
		if (obj.getYedbCertificatePrintRequest() != null) {
			currentSlipCount++;
			CertificatePrintRequest certificatePrintRequest = obj.getYedbCertificatePrintRequest();
			PackingSlipRequest request = PackingSlipRequest.builder().mincode(mincode).currentSlip(currentSlipCount).total(obj.getTotal()).paperType("YEDB").build();
			mergeCertificates(packSlipReq, certificatePrintRequest, request,processorData,studListNonGrad);
			numberOfPdfs++;
			logger.debug("*** YEDB Documents Merged");
		}
		return Pair.of(currentSlipCount,numberOfPdfs);
	}

	private Pair<Integer,Integer> processYed2CertificatePrintRequest(DistributionPrintRequest obj, int currentSlipCount, ReportRequest packSlipReq, List<Student> studListNonGrad, ProcessorData processorData, String mincode, int numberOfPdfs) {
		if (obj.getYed2CertificatePrintRequest() != null) {
			currentSlipCount++;
			CertificatePrintRequest certificatePrintRequest = obj.getYed2CertificatePrintRequest();
			PackingSlipRequest request = PackingSlipRequest.builder().mincode(mincode).currentSlip(currentSlipCount).total(obj.getTotal()).paperType("YED2").build();
			mergeCertificates(packSlipReq, certificatePrintRequest, request,processorData,studListNonGrad);
			numberOfPdfs++;
			logger.debug("*** YED2 Documents Merged");
		}
		return Pair.of(currentSlipCount,numberOfPdfs);
	}
	private Pair<Integer, Integer> processTranscriptPrintRequest(DistributionPrintRequest obj, int currentSlipCount, ReportRequest packSlipReq, List<Student> studListNonGrad, ProcessorData processorData, String mincode, int numberOfPdfs) {
		if (obj.getTranscriptPrintRequest() != null) {
			TranscriptPrintRequest transcriptPrintRequest = obj.getTranscriptPrintRequest();
			List<StudentCredentialDistribution> scdList = transcriptPrintRequest.getTranscriptList();
			List<InputStream> locations = new ArrayList<>();
			currentSlipCount++;
			setExtraDataForPackingSlip(packSlipReq, "YED4", obj.getTotal(), scdList.size(), 1, "Transcript", transcriptPrintRequest.getBatchId());
			try {
				locations.add(reportService.getPackingSlip(packSlipReq, restUtils.getAccessToken()).getInputStream());
				logger.debug("*** Packing Slip Added");
				processStudents(scdList,studListNonGrad,locations,processorData);
				mergeDocuments(processorData,mincode,"/EDGRAD.T.","YED4",locations);
				numberOfPdfs++;
				logger.debug("*** Transcript Documents Merged");
			} catch (IOException e) {
				logger.debug(EXCEPTION,e.getLocalizedMessage());
			}
		}
		return Pair.of(currentSlipCount,numberOfPdfs);
	}

	private void processStudents(List<StudentCredentialDistribution> scdList, List<Student> studListNonGrad, List<InputStream> locations, ProcessorData processorData) throws IOException {
		int currentTranscript = 0;
		int failedToAdd = 0;
		for (StudentCredentialDistribution scd : scdList) {
			if(scd.getNonGradReasons() != null && !scd.getNonGradReasons().isEmpty()) {
				Student objStd = prepareStudentObj(scd,studListNonGrad);
				if(objStd != null)
					studListNonGrad.add(objStd);
			}
			InputStreamResource transcriptPdf = webClient.get().uri(String.format(educDistributionApiConstants.getTranscript(), scd.getStudentID(), scd.getCredentialTypeCode(), scd.getDocumentStatusCode())).headers(h -> h.setBearerAuth(restUtils.fetchAccessToken())).retrieve().bodyToMono(InputStreamResource.class).block();
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

	private Student prepareStudentObj(StudentCredentialDistribution scd, List<Student> studListNonGrad) {
		if(scd.getStudentGrade().equalsIgnoreCase("AD") || scd.getStudentGrade().equalsIgnoreCase("12")) {
			Student std = new Student();
			std.setFirstName(scd.getLegalFirstName());
			std.setLastName(scd.getLegalLastName());
			std.setMiddleName(scd.getLegalMiddleNames());
			std.setCitizenship(scd.getStudentCitizenship());
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

	private void mergeCertificates(ReportRequest packSlipReq, CertificatePrintRequest certificatePrintRequest, PackingSlipRequest request,ProcessorData processorData, List<Student> studListNonGrad) {
		List<StudentCredentialDistribution> scdList = certificatePrintRequest.getCertificateList();
		String mincode = request.getMincode();
		String paperType = request.getPaperType();
		List<InputStream> locations=new ArrayList<>();
		setExtraDataForPackingSlip(packSlipReq,paperType,request.getTotal(),scdList.size(),request.getCurrentSlip(),"Certificate", certificatePrintRequest.getBatchId());
		try {
			locations.add(reportService.getPackingSlip(packSlipReq,restUtils.getAccessToken()).getInputStream());
			int currentCertificate = 0;
			int failedToAdd = 0;
			for (StudentCredentialDistribution scd : scdList) {
				if(scd.getNonGradReasons() != null && !scd.getNonGradReasons().isEmpty()) {
					Student objStd = prepareStudentObj(scd,studListNonGrad);
					if(objStd != null)
						studListNonGrad.add(objStd);
				}
				InputStreamResource certificatePdf = webClient.get().uri(String.format(educDistributionApiConstants.getCertificate(),scd.getStudentID(),scd.getCredentialTypeCode(),scd.getDocumentStatusCode())).headers(h -> h.setBearerAuth(restUtils.fetchAccessToken())).retrieve().bodyToMono(InputStreamResource.class).block();
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

	private void createAndSaveDistributionReport(ReportRequest distributionRequest,String mincode,ProcessorData processorData) {
		List<InputStream> locations=new ArrayList<>();
		try {
			byte[] bytesSAR = webClient.post().uri(educDistributionApiConstants.getSchoolDistributionReport()).headers(h -> h.setBearerAuth(restUtils.fetchAccessToken())).body(BodyInserters.fromValue(distributionRequest)).retrieve().bodyToMono(byte[].class).block();
			if(bytesSAR != null) {
				locations.add(new ByteArrayInputStream(bytesSAR));
				byte[] encoded = Base64.encodeBase64(bytesSAR);
				String encodedPdf= new String(encoded, StandardCharsets.US_ASCII);
				if(!processorData.getActivityCode().contains("USERDIST"))
					saveSchoolDistributionReport(encodedPdf,mincode,restUtils.getAccessToken(),"DISTREP_SC");
			}
			mergeDocuments(processorData,mincode,"/EDGRAD.R.","324W",locations);
		} catch (Exception e) {
			logger.debug(EXCEPTION,e.getLocalizedMessage());
		}
	}

	private void createAndSaveNonGradReport(CommonSchool schoolDetails, List<Student> studListNonGrad, String mincode, String accessToken) {
		ReportData nongradProjected = new ReportData();
		School schObj = new School();
		schObj.setMincode(schoolDetails.getDistNo()+schoolDetails.getSchlNo());
		schObj.setName(schoolDetails.getSchoolName());
		schObj.setStudents(studListNonGrad);
		nongradProjected.setSchool(schObj);
		nongradProjected.setOrgCode(StringUtils.startsWith(nongradProjected.getSchool().getMincode(), "098") ? "YU" : "BC");
		nongradProjected.setIssueDate(EducDistributionApiUtils.formatIssueDateForReportJasper(new java.sql.Date(System.currentTimeMillis()).toString()));
		nongradProjected.setReportNumber("TRAX241B");
		nongradProjected.setReportTitle("Graduation Records and Achievement Data");
		nongradProjected.setReportSubTitle("Grade 12 and Adult Students Not Able to Graduate on Grad Requirements");
		ReportOptions options = new ReportOptions();
		options.setReportFile(String.format("%s_%s00_NONGRAD",mincode, LocalDate.now().getYear()));
		options.setReportName(String.format("%s_%s00_NONGRAD.pdf",mincode, LocalDate.now().getYear()));
		ReportRequest reportParams = new ReportRequest();
		reportParams.setOptions(options);
		reportParams.setData(nongradProjected);

		byte[] bytesSAR = webClient.post().uri(educDistributionApiConstants.getNonGrad())
				.headers(h -> h.setBearerAuth(restUtils.fetchAccessToken())).body(BodyInserters.fromValue(reportParams)).retrieve().bodyToMono(byte[].class).block();
		byte[] encoded = Base64.encodeBase64(bytesSAR);
		assert encoded != null;
		String encodedPdf = new String(encoded, StandardCharsets.US_ASCII);
		saveSchoolDistributionReport(encodedPdf,mincode,accessToken, "NONGRADDISTREP_SC");
	}

	private void saveSchoolDistributionReport(String encodedPdf, String mincode, String accessToken, String reportType) {
		SchoolReports requestObj = new SchoolReports();
		requestObj.setReport(encodedPdf);
		requestObj.setSchoolOfRecord(mincode);
		requestObj.setReportTypeCode(reportType);
		webClient.post().uri(educDistributionApiConstants.getUpdateSchoolReport()).headers(h ->h.setBearerAuth(restUtils.fetchAccessToken())).body(BodyInserters.fromValue(requestObj)).retrieve().bodyToMono(SchoolReports.class).block();
	}
}
