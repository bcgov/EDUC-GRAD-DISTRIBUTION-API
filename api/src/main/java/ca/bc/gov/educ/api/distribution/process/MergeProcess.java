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
public class MergeProcess extends BaseProcess {
	
	private static Logger logger = LoggerFactory.getLogger(MergeProcess.class);

	@Override
	public ProcessorData fire(ProcessorData processorData) {
		long startTime = System.currentTimeMillis();
		logger.debug("************* TIME START  ************ {}",startTime);
		DistributionResponse response = new DistributionResponse();
		ExceptionMessage exception = new ExceptionMessage();
		DistributionRequest distributionRequest = processorData.getDistributionRequest();
		Map<String, DistributionPrintRequest> mapDist = distributionRequest.getMapDist();
		Long batchId = processorData.getBatchId();
		int numberOfPdfs = 0;
		int counter=0;
		List<School> schoolsForLabels = new ArrayList<>();
		for (String mincode : mapDist.keySet()) {
			counter++;
			int currentSlipCount = 0;
			DistributionPrintRequest distributionPrintRequest = mapDist.get(mincode);
			CommonSchool schoolDetails = getBaseSchoolDetails(distributionPrintRequest,mincode,exception);
			if(schoolDetails != null) {
				String schoolCategoryCode = schoolDetails.getSchoolCategoryCode();
				logger.debug("*** School Details Acquired {} category {}", mincode, schoolCategoryCode);
				List<Student> studListNonGrad = new ArrayList<>();
				ReportRequest packSlipReq = reportService.preparePackingSlipData(schoolDetails, processorData.getBatchId());

				if(distributionPrintRequest.getSchoolDistributionRequest() != null && MONTHLYDIST.equalsIgnoreCase(processorData.getActivityCode())) {
					ReportRequest schoolDistributionReportRequest = reportService.prepareSchoolDistributionReportData(distributionPrintRequest.getSchoolDistributionRequest(), processorData.getBatchId(),schoolDetails);
					createAndSaveDistributionReport(schoolDistributionReportRequest,mincode,schoolCategoryCode,processorData);
					numberOfPdfs++;
				}
				Pair<Integer,Integer> pV = processTranscriptPrintRequest(distributionPrintRequest,currentSlipCount,packSlipReq,studListNonGrad,processorData,mincode,schoolCategoryCode,numberOfPdfs);
				currentSlipCount = pV.getLeft();
				numberOfPdfs = pV.getRight();
				pV = processYed2CertificatePrintRequest(distributionPrintRequest,currentSlipCount,packSlipReq,studListNonGrad,processorData,mincode,schoolCategoryCode,numberOfPdfs);
				currentSlipCount = pV.getLeft();
				numberOfPdfs = pV.getRight();
				pV = processYedbCertificatePrintRequest(distributionPrintRequest,currentSlipCount,packSlipReq,studListNonGrad,processorData,mincode,schoolCategoryCode,numberOfPdfs);
				currentSlipCount = pV.getLeft();
				numberOfPdfs = pV.getRight();
				pV = processYedrCertificatePrintRequest(distributionPrintRequest,currentSlipCount,packSlipReq,studListNonGrad,processorData,mincode,schoolCategoryCode,numberOfPdfs);
				numberOfPdfs = pV.getRight();
				if(!studListNonGrad.isEmpty()) {
					createAndSaveNonGradReport(schoolDetails,studListNonGrad,mincode);
				}
				logger.debug("PDFs Merged {}", schoolDetails.getSchoolName());
				processSchoolsForLabels(schoolsForLabels, mincode, restUtils.getAccessToken(), exception);
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
			numberOfCreatedSchoolReports += createSchoolLabelsReport(schoolsForLabels, ADDRESS_LABEL_SCHL);
			logger.debug("***** Number of created Monthly school reports {} *****", numberOfCreatedSchoolReports);
			logger.debug("***** Distribute Monthly school reports *****");
			numberOfProcessedSchoolReports += processDistrictSchoolDistribution(batchId, mapDist.keySet(), ADDRESS_LABEL_SCHL, null, null, null);
			logger.debug("***** Number of distributed Monthly school reports {} *****", numberOfProcessedSchoolReports);
		}
		if (SUPPDIST.equalsIgnoreCase(processorData.getActivityCode())) {
			logger.debug("***** Create and Store Supplemental school reports *****");
			numberOfCreatedSchoolReports += createSchoolLabelsReport(schoolsForLabels, ADDRESS_LABEL_SCHL);
			numberOfCreatedSchoolReports += createDistrictSchoolSuppReport( null, null, DISTREP_SC);
			logger.debug("***** Number of created Supplemental school reports {} *****", numberOfCreatedSchoolReports);
			logger.debug("***** Distribute Supplemental school reports *****");
			numberOfProcessedSchoolReports += processDistrictSchoolDistribution(batchId, mapDist.keySet(), ADDRESS_LABEL_SCHL, null, DISTREP_SC, null);
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

	private Pair<Integer,Integer> processYedrCertificatePrintRequest(DistributionPrintRequest obj, int currentSlipCount, ReportRequest packSlipReq, List<Student> studListNonGrad, ProcessorData processorData, String mincode, String schoolCategoryCode, int numberOfPdfs) {
		if (obj.getYedrCertificatePrintRequest() != null) {
			currentSlipCount++;
			CertificatePrintRequest certificatePrintRequest = obj.getYedrCertificatePrintRequest();
			PackingSlipRequest request = PackingSlipRequest.builder().mincode(mincode).currentSlip(currentSlipCount).total(obj.getTotal()).paperType("YEDR").build();
			mergeCertificates(packSlipReq, certificatePrintRequest, request,processorData,studListNonGrad,schoolCategoryCode);
			numberOfPdfs++;
			logger.debug("*** YEDR Documents Merged ***");
		}
		return Pair.of(currentSlipCount,numberOfPdfs);
	}

	private Pair<Integer,Integer> processYedbCertificatePrintRequest(DistributionPrintRequest obj, int currentSlipCount, ReportRequest packSlipReq, List<Student> studListNonGrad, ProcessorData processorData, String mincode, String schoolCategoryCode, int numberOfPdfs) {
		if (obj.getYedbCertificatePrintRequest() != null) {
			currentSlipCount++;
			CertificatePrintRequest certificatePrintRequest = obj.getYedbCertificatePrintRequest();
			PackingSlipRequest request = PackingSlipRequest.builder().mincode(mincode).currentSlip(currentSlipCount).total(obj.getTotal()).paperType("YEDB").build();
			mergeCertificates(packSlipReq, certificatePrintRequest, request,processorData,studListNonGrad, schoolCategoryCode);
			numberOfPdfs++;
			logger.debug("*** YEDB Documents Merged ***");
		}
		return Pair.of(currentSlipCount,numberOfPdfs);
	}

	private Pair<Integer,Integer> processYed2CertificatePrintRequest(DistributionPrintRequest obj, int currentSlipCount, ReportRequest packSlipReq, List<Student> studListNonGrad, ProcessorData processorData, String mincode, String schoolCategoryCode, int numberOfPdfs) {
		if (obj.getYed2CertificatePrintRequest() != null) {
			currentSlipCount++;
			CertificatePrintRequest certificatePrintRequest = obj.getYed2CertificatePrintRequest();
			PackingSlipRequest request = PackingSlipRequest.builder().mincode(mincode).currentSlip(currentSlipCount).total(obj.getTotal()).paperType("YED2").build();
			mergeCertificates(packSlipReq, certificatePrintRequest, request,processorData,studListNonGrad, schoolCategoryCode);
			numberOfPdfs++;
			logger.debug("*** YED2 Documents Merged ***");
		}
		return Pair.of(currentSlipCount,numberOfPdfs);
	}

	protected Pair<Integer, Integer> processTranscriptPrintRequest(DistributionPrintRequest obj, int currentSlipCount, ReportRequest packSlipReq, List<Student> studListNonGrad, ProcessorData processorData, String mincode, String schoolCategoryCode, int numberOfPdfs) {
		if (obj.getTranscriptPrintRequest() != null) {
			TranscriptPrintRequest transcriptPrintRequest = obj.getTranscriptPrintRequest();
			List<StudentCredentialDistribution> scdList = transcriptPrintRequest.getTranscriptList();
			List<InputStream> locations = new ArrayList<>();
			currentSlipCount++;
			setExtraDataForPackingSlip(packSlipReq, "YED4", obj.getTotal(), scdList.size(), 1, "Transcript", transcriptPrintRequest.getBatchId());
			try {
				locations.add(reportService.getPackingSlip(packSlipReq).getInputStream());
				logger.debug("*** Packing Slip Added");
				processStudents(scdList,studListNonGrad,locations);
				mergeDocumentsPDFs(processorData,mincode,schoolCategoryCode,"/EDGRAD.T.","YED4",locations);
				numberOfPdfs++;
				logger.debug("*** Transcript Documents Merged ***");
			} catch (IOException e) {
				logger.debug(EXCEPTION,e.getLocalizedMessage());
			}
		}
		return Pair.of(currentSlipCount,numberOfPdfs);
	}

	private void processStudents(List<StudentCredentialDistribution> scdList, List<Student> studListNonGrad, List<InputStream> locations) {
		int currentTranscript = 0;
		int failedToAdd = 0;
		for (StudentCredentialDistribution scd : scdList) {
			if(scd.getNonGradReasons() != null && !scd.getNonGradReasons().isEmpty()) {
				Student objStd = prepareStudentObj(scd,studListNonGrad);
				if(objStd != null)
					studListNonGrad.add(objStd);
			}
			int result = 0;
			if(scd.getStudentID() != null) {
				result = addStudentTranscriptToLocations(scd.getStudentID().toString(), locations);
			}
			if(result == 0) {
				failedToAdd++;
				logger.debug("*** Failed to Add PDFs {} Current student {}", failedToAdd, scd.getStudentID());
			} else {
				currentTranscript++;
				logger.debug("*** Added PDFs {}/{} Current student {}", currentTranscript, scdList.size(), scd.getStudentID());
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
			std.setGradProgram(scd.getProgram());
			std.setLastUpdateDate(scd.getLastUpdateDate());
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

	private void mergeCertificates(ReportRequest packSlipReq, CertificatePrintRequest certificatePrintRequest, PackingSlipRequest request, ProcessorData processorData, List<Student> studListNonGrad, String schoolCategoryCode) {
		List<StudentCredentialDistribution> scdList = certificatePrintRequest.getCertificateList();
		String mincode = request.getMincode();
		String paperType = request.getPaperType();
		List<InputStream> locations=new ArrayList<>();
		setExtraDataForPackingSlip(packSlipReq,paperType,request.getTotal(),scdList.size(),request.getCurrentSlip(),"Certificate", certificatePrintRequest.getBatchId());
		try {
			locations.add(reportService.getPackingSlip(packSlipReq).getInputStream());
			int currentCertificate = 0;
			int failedToAdd = 0;
			for (StudentCredentialDistribution scd : scdList) {
				if(scd.getNonGradReasons() != null && !scd.getNonGradReasons().isEmpty()) {
					Student objStd = prepareStudentObj(scd,studListNonGrad);
					if(objStd != null)
						studListNonGrad.add(objStd);
				}
				InputStreamResource certificatePdf = restService.executeGet(
						educDistributionApiConstants.getCertificate(),
						InputStreamResource.class,
						scd.getStudentID().toString(),
						scd.getCredentialTypeCode(),
						scd.getDocumentStatusCode()
				);
				if(certificatePdf != null) {
					locations.add(certificatePdf.getInputStream());
					currentCertificate++;
					logger.debug("*** Added PDFs {}/{} Current student {}",currentCertificate,scdList.size(),scd.getStudentID());
				}else {
					failedToAdd++;
					logger.debug("*** Failed to Add PDFs {} Current student {} papertype : {}",failedToAdd,scd.getStudentID(),paperType);
				}
			}
			mergeDocumentsPDFs(processorData,mincode,schoolCategoryCode,"/EDGRAD.C.",paperType,locations);
		} catch (IOException e) {
			logger.debug(EXCEPTION,e.getLocalizedMessage());
		}
	}

	protected void createAndSaveDistributionReport(ReportRequest distributionRequest, String mincode, String schoolCategoryCode, ProcessorData processorData) {
		List<InputStream> locations=new ArrayList<>();
		try {
			byte[] bytesSAR = restService.executePost(
					educDistributionApiConstants.getSchoolDistributionReport(),
					byte[].class,
					distributionRequest
			);
			if(bytesSAR != null) {
				locations.add(new ByteArrayInputStream(bytesSAR));
				byte[] encoded = Base64.encodeBase64(bytesSAR);
				String encodedPdf = new String(encoded, StandardCharsets.US_ASCII);
				if(!processorData.getActivityCode().contains("USERDIST"))
					saveSchoolDistributionReport(encodedPdf,mincode,"DISTREP_SC");
			}
			mergeDocumentsPDFs(processorData,mincode,schoolCategoryCode,"/EDGRAD.R.","324W",locations);
		} catch (Exception e) {
			logger.debug(EXCEPTION,e.getLocalizedMessage());
		}
	}

	protected Integer createAndSaveNonGradReport(CommonSchool schoolDetails, List<Student> studListNonGrad, String mincode) {
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

		byte[] bytesSAR = restService.executePost(
						educDistributionApiConstants.getStudentNonGrad(),
						byte[].class,
						reportParams
				);
		byte[] encoded = Base64.encodeBase64(bytesSAR);
		assert encoded != null;
		String encodedPdf = new String(encoded, StandardCharsets.US_ASCII);
		saveSchoolDistributionReport(encodedPdf,mincode,NONGRADDISTREP_SC);
		return 1;
	}

	private void saveSchoolDistributionReport(String encodedPdf, String mincode, String reportType) {
		SchoolReports requestObj = new SchoolReports();
		requestObj.setReport(encodedPdf);
		requestObj.setSchoolOfRecord(mincode);
		requestObj.setReportTypeCode(reportType);
		restService.executePost(educDistributionApiConstants.getUpdateSchoolReport(), SchoolReports.class, requestObj);
	}
}
