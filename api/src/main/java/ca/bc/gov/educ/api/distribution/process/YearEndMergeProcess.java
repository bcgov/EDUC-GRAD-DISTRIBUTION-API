package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils;
import ca.bc.gov.educ.api.distribution.util.Generated;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.internal.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
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
public class YearEndMergeProcess extends BaseProcess {
	
	private static Logger logger = LoggerFactory.getLogger(YearEndMergeProcess.class);

	@Override
	@Generated
	public ProcessorData fire(ProcessorData processorData) {
		long startTime = System.currentTimeMillis();
		logger.debug("************* TIME START  ************ {}",startTime);
		DistributionResponse response = new DistributionResponse();
		ExceptionMessage exception = new ExceptionMessage();
		Map<String,DistributionPrintRequest> mapDist = processorData.getMapDistribution();
		int numberOfPdfs = 0;
		int counter=0;
		List<School> schoolsForLabels = new ArrayList<>();
		for (Map.Entry<String, DistributionPrintRequest> entry : mapDist.entrySet()) {
			counter++;
			int currentSlipCount = 0;
			String mincode = entry.getKey();
			DistributionPrintRequest distributionPrintRequest = entry.getValue();
			CommonSchool schoolDetails = getBaseSchoolDetails(distributionPrintRequest,mincode,processorData,exception);
			if(schoolDetails != null) {
				String schoolCategoryCode = schoolDetails.getSchoolCategoryCode();
				logger.debug("*** School Details Acquired {} category {}", mincode, schoolCategoryCode);
				List<Student> studListNonGrad = new ArrayList<>();
				ReportRequest packSlipReq = reportService.preparePackingSlipData(schoolDetails, processorData.getBatchId());
				Pair<Integer,Integer> pV = processTranscriptPrintRequest(distributionPrintRequest,currentSlipCount,packSlipReq,studListNonGrad,processorData,mincode,schoolCategoryCode,numberOfPdfs);
				numberOfPdfs = pV.getRight();
				if(!studListNonGrad.isEmpty()) {
					createAndSaveNonGradReport(schoolDetails,studListNonGrad,mincode);
				}
				logger.debug("PDFs Merged {}", schoolDetails.getSchoolName());
				processSchoolsForLabels(schoolsForLabels, mincode, restUtils.getAccessToken(), exception);
				logger.debug("School {}/{}",counter,mapDist.size());

				int numberOfCreatedSchoolReports = 0;
				int numberOfProcessedSchoolReports = 0;
				if (YEARENDDIST.equalsIgnoreCase(processorData.getActivityCode())) {
					logger.debug("***** Create and Store Year End school reports *****");
					numberOfCreatedSchoolReports += createDistrictSchoolYearEndReport(restUtils.getAccessToken(), ADDRESS_LABEL_YE, DISTREP_YE_SD, DISTREP_YE_SC);
					logger.debug("***** Number of created Year End school reports {} *****", numberOfCreatedSchoolReports);
					logger.debug("***** Distribute Year End school reports *****");
					numberOfProcessedSchoolReports += processDistrictSchoolDistribution(processorData, ADDRESS_LABEL_YE, DISTREP_YE_SD, DISTREP_YE_SC);
					logger.debug("***** Number of distributed Year End school reports {} *****", numberOfProcessedSchoolReports);
				}
				if (NONGRADDIST.equalsIgnoreCase(processorData.getActivityCode())) {
					logger.debug("***** Create and Store Student NonGrad School Report *****");
					numberOfCreatedSchoolReports += createSchoolLabelsReport(schoolsForLabels, restUtils.getAccessToken(), ADDRESS_LABEL_SCHL );
					logger.debug("***** Number of created Student NonGrad School Reports {} *****", numberOfCreatedSchoolReports);
					logger.debug("***** Distribute Student NonGrad School Reports *****");
					numberOfProcessedSchoolReports += processDistrictSchoolDistribution(processorData, ADDRESS_LABEL_SCHL, null, NONGRADDISTREP_SC);
					logger.debug("***** Number of distributed Student NonGrad School Reports {} *****", numberOfProcessedSchoolReports);
				}
				numberOfPdfs += numberOfProcessedSchoolReports;
				long endTime = System.currentTimeMillis();
				long diff = (endTime - startTime)/1000;
				logger.debug("************* TIME Taken  ************ {} secs",diff);
				response.setMergeProcessResponse("Merge Successful and File Uploaded");
				processorData.setDistributionResponse(response);
			}
		}
		return processorData;
	}

	@Generated
	private Pair<Integer, Integer> processTranscriptPrintRequest(DistributionPrintRequest obj, int currentSlipCount, ReportRequest packSlipReq, List<Student> studListNonGrad, ProcessorData processorData, String mincode, String schoolCategoryCode, int numberOfPdfs) {
		if (obj.getTranscriptPrintRequest() != null) {
			TranscriptPrintRequest transcriptPrintRequest = obj.getTranscriptPrintRequest();
			List<StudentCredentialDistribution> scdList = transcriptPrintRequest.getTranscriptList();
			List<InputStream> locations = new ArrayList<>();
			currentSlipCount++;
			setExtraDataForPackingSlip(packSlipReq, "YED4", obj.getTotal(), scdList.size(), 1, "Transcript", transcriptPrintRequest.getBatchId());
			try {
				locations.add(reportService.getPackingSlip(packSlipReq, restUtils.getAccessToken()).getInputStream());
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

	@Generated
	private void processStudents(List<StudentCredentialDistribution> scdList, List<Student> studListNonGrad, List<InputStream> locations) {
		int currentTranscript = 0;
		int failedToAdd = 0;
		for (StudentCredentialDistribution scd : scdList) {
			if(scd.getNonGradReasons() != null && !scd.getNonGradReasons().isEmpty()) {
				Student objStd = prepareStudentObj(scd,studListNonGrad);
				if(objStd != null)
					studListNonGrad.add(objStd);
			}
			List<GradStudentTranscripts> studentTranscripts = webClient.get().uri(String.format(educDistributionApiConstants.getTranscriptUsingStudentID(), scd.getStudentID())).headers(h -> h.setBearerAuth(restUtils.fetchAccessToken())).retrieve().bodyToMono(new ParameterizedTypeReference<List<GradStudentTranscripts>>() {}).block();
			if(studentTranscripts != null && !studentTranscripts.isEmpty() ) {
				GradStudentTranscripts studentTranscript = studentTranscripts.get(0);
				byte[] transcriptPdf = Base64.decodeBase64(studentTranscript.getTranscript());
				locations.add(new ByteArrayInputStream(transcriptPdf));
				currentTranscript++;
				logger.debug("*** Added PDFs {}/{} Current student {}", currentTranscript, scdList.size(), scd.getStudentID());
			} else {
				failedToAdd++;
				logger.debug("*** Failed to Add PDFs {} Current student {}", failedToAdd, scd.getStudentID());
			}
		}
	}

	@Generated
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

	@Generated
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

	@Generated
	private void createAndSaveNonGradReport(CommonSchool schoolDetails, List<Student> studListNonGrad, String mincode) {
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

		byte[] bytesSAR = webClient.post().uri(educDistributionApiConstants.getStudentNonGrad())
				.headers(h -> h.setBearerAuth(restUtils.fetchAccessToken())).body(BodyInserters.fromValue(reportParams)).retrieve().bodyToMono(byte[].class).block();
		byte[] encoded = Base64.encodeBase64(bytesSAR);
		assert encoded != null;
		String encodedPdf = new String(encoded, StandardCharsets.US_ASCII);
		saveSchoolDistributionReport(encodedPdf,mincode,NONGRADDISTREP_SC);
	}

	@Generated
	private void saveSchoolDistributionReport(String encodedPdf, String mincode, String reportType) {
		SchoolReports requestObj = new SchoolReports();
		requestObj.setReport(encodedPdf);
		requestObj.setSchoolOfRecord(mincode);
		requestObj.setReportTypeCode(reportType);
		webClient.post().uri(educDistributionApiConstants.getUpdateSchoolReport()).headers(h ->h.setBearerAuth(restUtils.fetchAccessToken())).body(BodyInserters.fromValue(requestObj)).retrieve().bodyToMono(SchoolReports.class).block();
	}
}
