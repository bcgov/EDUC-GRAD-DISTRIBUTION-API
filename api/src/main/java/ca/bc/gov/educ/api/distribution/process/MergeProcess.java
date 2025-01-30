package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils;
import ca.bc.gov.educ.api.distribution.util.Generated;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.internal.Pair;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static ca.bc.gov.educ.api.distribution.model.dto.ActivityCode.*;
import static ca.bc.gov.educ.api.distribution.model.dto.ReportType.*;
import static ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants.*;

@Slf4j
@Data
@Component
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class MergeProcess extends BaseProcess {
	
	@Override
	@Generated
	public ProcessorData fire(ProcessorData processorData) {
		long startTime = System.currentTimeMillis();
		log.debug("************* TIME START  ************ {}",startTime);
		DistributionResponse response = new DistributionResponse();
		ExceptionMessage exception = new ExceptionMessage();
		DistributionRequest distributionRequest = processorData.getDistributionRequest();
		Map<UUID, DistributionPrintRequest> mapDist = distributionRequest.getMapDist();
		StudentSearchRequest searchRequest = distributionRequest.getStudentSearchRequest();
		Long batchId = processorData.getBatchId();
		int numberOfPdfs = 0;
		int counter = 0;
		List<ca.bc.gov.educ.api.distribution.model.dto.School> schoolsForLabels = new ArrayList<>();
		for (Map.Entry<UUID, DistributionPrintRequest> entry : mapDist.entrySet()) {
			UUID schoolId = entry.getKey();
			counter++;
			int currentSlipCount = 0;
			DistributionPrintRequest distributionPrintRequest = entry.getValue();
			ca.bc.gov.educ.api.distribution.model.dto.v2.School schoolDetails =
					getBaseSchoolDetails(distributionPrintRequest, searchRequest, schoolId, exception);
			if(schoolDetails != null) {
				String schoolCategoryCode = schoolDetails.getSchoolCategoryLegacyCode();
				log.debug("*** School Details Acquired {} category {}", schoolDetails.getMinCode(), schoolCategoryCode);
				List<Student> studListNonGrad = new ArrayList<>();
				ReportRequest packSlipReq = reportService
						.preparePackingSlipData(searchRequest, schoolDetails, processorData.getBatchId());

				if(distributionPrintRequest.getSchoolDistributionRequest() != null
						&& StringUtils.equalsAnyIgnoreCase(processorData.getActivityCode(), MONTHLYDIST.getValue(), SUPPDIST.getValue())) {
					ReportRequest schoolDistributionReportRequest = reportService.prepareSchoolDistributionReportData(
							distributionPrintRequest.getSchoolDistributionRequest(), processorData.getBatchId(), schoolDetails);
					createAndSaveDistributionReport(schoolDistributionReportRequest, schoolId, schoolDetails.getMinCode(),
							schoolCategoryCode,processorData);
					numberOfPdfs++;
				}
				Pair<Integer,Integer> pV = processTranscriptPrintRequest(distributionPrintRequest, currentSlipCount,
						packSlipReq, studListNonGrad, processorData, schoolDetails.getMinCode(), schoolCategoryCode, numberOfPdfs);
				currentSlipCount = pV.getLeft();
				numberOfPdfs = pV.getRight();
				pV = processYed2CertificatePrintRequest(distributionPrintRequest, currentSlipCount, packSlipReq,
						studListNonGrad, processorData, schoolDetails.getMinCode(), schoolCategoryCode, numberOfPdfs);
				currentSlipCount = pV.getLeft();
				numberOfPdfs = pV.getRight();
				pV = processYedbCertificatePrintRequest(distributionPrintRequest, currentSlipCount, packSlipReq,
						studListNonGrad, processorData, schoolDetails.getMinCode(), schoolCategoryCode, numberOfPdfs);
				currentSlipCount = pV.getLeft();
				numberOfPdfs = pV.getRight();
				pV = processYedrCertificatePrintRequest(distributionPrintRequest, currentSlipCount, packSlipReq,
						studListNonGrad, processorData, schoolDetails.getMinCode(), schoolCategoryCode, numberOfPdfs);
				numberOfPdfs = pV.getRight();
				if(!studListNonGrad.isEmpty()
						&& StringUtils.equalsAnyIgnoreCase(processorData.getActivityCode(), MONTHLYDIST.getValue(), SUPPDIST.getValue())) {
					createAndSaveNonGradReport(schoolDetails, studListNonGrad, schoolId, educDistributionApiConstants.getStudentNonGradProjected());
				}
				log.debug("PDFs Merged {}", schoolDetails.getSchoolName());
				processSchoolsForLabels(searchRequest.getUser(), schoolsForLabels, schoolId, exception);
				if (counter % 50 == 0) {
					restUtils.fetchAccessToken(processorData);
				}
				log.debug("{} School {}/{}", schoolDetails.getMinCode(), counter, mapDist.size());
			}
		}
		int numberOfCreatedSchoolReports = 0;
		int numberOfProcessedSchoolReports = 0;
		List<String> schoolsForLabelsCodes = List.of(DEFAULT_SCHOOL_ID);
		if(schoolsForLabels.size() == 1) {
			schoolsForLabelsCodes = List.of(schoolsForLabels.get(0).getMincode());
		}
		if(MONTHLYDIST.getValue().equalsIgnoreCase(processorData.getActivityCode())) {
			log.debug("***** Create and Store Monthly school reports *****");
			numberOfCreatedSchoolReports += createSchoolLabelsReport(schoolsForLabels, ADDRESS_LABEL_SCHL);
			log.debug("***** Number of created Monthly school reports {} *****", numberOfCreatedSchoolReports);
			log.debug("***** Distribute Monthly school reports *****");
			numberOfProcessedSchoolReports += processDistrictSchoolDistribution(batchId, schoolsForLabelsCodes, ADDRESS_LABEL_SCHL,
					null, null, processorData.getActivityCode());
			log.debug("***** Number of distributed Monthly school reports {} *****", numberOfProcessedSchoolReports);
		}
		if (SUPPDIST.getValue().equalsIgnoreCase(processorData.getActivityCode())) {
			log.debug("***** Create and Store Supplemental school reports *****");
			numberOfCreatedSchoolReports += createSchoolLabelsReport(schoolsForLabels, ADDRESS_LABEL_SCHL);
			log.debug("***** Number of created Supplemental school reports {} *****", numberOfCreatedSchoolReports);
			log.debug("***** Distribute Supplemental school label reports *****");
			numberOfProcessedSchoolReports += processDistrictSchoolDistribution(batchId, schoolsForLabelsCodes, ADDRESS_LABEL_SCHL,
					null, null, processorData.getActivityCode());
			log.debug("***** Number of distributed Supplemental school label reports {} *****", numberOfProcessedSchoolReports);
		}
		numberOfPdfs += numberOfProcessedSchoolReports;
		postingProcess(batchId,processorData,numberOfPdfs);
		long endTime = System.currentTimeMillis();
		long diff = (endTime - startTime)/1000;
		log.debug("************* TIME Taken  ************ {} secs",diff);
		response.setMergeProcessResponse("Merge Successful and File Uploaded");
		response.setNumberOfPdfs(numberOfPdfs);
		response.setBatchId(processorData.getBatchId());
		response.setLocalDownload(processorData.getLocalDownload());
		response.setActivityCode(distributionRequest.getActivityCode());
		response.setStudentSearchRequest(searchRequest);
		processorData.setDistributionResponse(response);
		return processorData;
	}

	protected Pair<Integer,Integer> processYedrCertificatePrintRequest(DistributionPrintRequest obj, int currentSlipCount,
																	   ReportRequest packSlipReq, List<Student> studListNonGrad,
																	   ProcessorData processorData, String mincode,
																	   String schoolCategoryCode, int numberOfPdfs) {
		if (obj.getYedrCertificatePrintRequest() != null) {
			currentSlipCount++;
			CertificatePrintRequest certificatePrintRequest = obj.getYedrCertificatePrintRequest();
			PackingSlipRequest request = PackingSlipRequest.builder()
					.mincode(mincode)
					.currentSlip(currentSlipCount)
					.total(obj.getTotal())
					.paperType("YEDR")
					.build();
			mergeCertificates(packSlipReq, certificatePrintRequest, request, processorData, studListNonGrad, schoolCategoryCode);
			numberOfPdfs++;
			log.debug("*** YEDR Documents Merged ***");
		}
		return Pair.of(currentSlipCount,numberOfPdfs);
	}

	protected Pair<Integer,Integer> processYedbCertificatePrintRequest(DistributionPrintRequest obj, int currentSlipCount,
																	   ReportRequest packSlipReq, List<Student> studListNonGrad,
																	   ProcessorData processorData, String mincode,
																	   String schoolCategoryCode, int numberOfPdfs) {
		if (obj.getYedbCertificatePrintRequest() != null) {
			currentSlipCount++;
			CertificatePrintRequest certificatePrintRequest = obj.getYedbCertificatePrintRequest();
			PackingSlipRequest request = PackingSlipRequest.builder()
					.mincode(mincode)
					.currentSlip(currentSlipCount)
					.total(obj.getTotal())
					.paperType("YEDB")
					.build();
			mergeCertificates(packSlipReq, certificatePrintRequest, request, processorData, studListNonGrad, schoolCategoryCode);
			numberOfPdfs++;
			log.debug("*** YEDB Documents Merged ***");
		}
		return Pair.of(currentSlipCount,numberOfPdfs);
	}

	protected Pair<Integer,Integer> processYed2CertificatePrintRequest(DistributionPrintRequest obj, int currentSlipCount,
																	   ReportRequest packSlipReq, List<Student> studListNonGrad,
																	   ProcessorData processorData, String mincode,
																	   String schoolCategoryCode, int numberOfPdfs) {
		if (obj.getYed2CertificatePrintRequest() != null) {
			currentSlipCount++;
			CertificatePrintRequest certificatePrintRequest = obj.getYed2CertificatePrintRequest();
			PackingSlipRequest request = PackingSlipRequest.builder()
					.mincode(mincode)
					.currentSlip(currentSlipCount)
					.total(obj.getTotal())
					.paperType("YED2")
					.build();
			mergeCertificates(packSlipReq, certificatePrintRequest, request, processorData, studListNonGrad, schoolCategoryCode);
			numberOfPdfs++;
			log.debug("*** YED2 Documents Merged ***");
		}
		return Pair.of(currentSlipCount,numberOfPdfs);
	}

	/**
	 * Processes the transcript print request and manages the generation of packing slips
	 * and merging of PDF documents. It updates the current slip count and number of PDFs processed.
	 *
	 * @param distributionPrintRequest The distribution print request containing transcript details.
	 * @param currentSlipCount         The current count of packing slips generated.
	 * @param packSlipReq              The request object for generating packing slips.
	 * @param studListNonGrad          The list of non-graduated students for whom transcripts are to be processed.
	 * @param processorData            The processor data containing batch and activity information.
	 * @param mincode                  The ministry code identifying the school.
	 * @param schoolCategoryCode       The school category code (e.g., "02", "03", "09").
	 * @param numberOfPdfs             The current count of PDFs processed.
	 * @return A {@link Pair} containing:
	 *         <ul>
	 *           <li>The updated slip count as the first element.</li>
	 *           <li>The updated number of PDFs processed as the second element.</li>
	 *         </ul>
	 *
	 * @implNote
	 * <p>The method performs the following steps:</p>
	 * <ul>
	 *   <li>Increments the slip count if a transcript print request exists.</li>
	 *   <li>Processes the list of transcripts using {@code processTranscripts} and updates the packing slip data.</li>
	 *   <li>Adds a packing slip PDF to the beginning of the PDF list for merging.</li>
	 *   <li>Merges the PDFs into a single document using {@code mergeDocumentsPDFs}.</li>
	 *   <li>Handles any {@link IOException} during processing and logs debug messages.</li>
	 * </ul>
	 *
	 * @throws NullPointerException if any required parameter is null.
	 * @throws IOException          if an error occurs during PDF merging or file operations.
	 */
	protected Pair<Integer, Integer> processTranscriptPrintRequest(DistributionPrintRequest distributionPrintRequest, int currentSlipCount,
																   ReportRequest packSlipReq, List<Student> studListNonGrad,
																   ProcessorData processorData, String mincode,
																   String schoolCategoryCode, int numberOfPdfs) {
		if (distributionPrintRequest.getTranscriptPrintRequest() != null) {
			List<InputStream> locations = new ArrayList<>();
			currentSlipCount++;
			try {
				int currentTranscript = processTranscripts(processorData,
						distributionPrintRequest.getTranscriptPrintRequest().getTranscriptList(), studListNonGrad, locations);
				setExtraDataForPackingSlip(packSlipReq, "YED4", distributionPrintRequest.getTotal(), currentTranscript, 1,
						"Transcript", distributionPrintRequest.getTranscriptPrintRequest().getBatchId());
				locations.add(0, reportService.getPackingSlip(packSlipReq).getInputStream());
				log.debug("*** Packing Slip Added");
				mergeDocumentsPDFs(processorData, mincode, schoolCategoryCode,"/EDGRAD.T.","YED4", locations);
				numberOfPdfs++;
				log.debug("*** Transcript Documents Merged ***");
			} catch (IOException e) {
				log.debug(EXCEPTION,e.getLocalizedMessage());
			}
		}
		return Pair.of(currentSlipCount, numberOfPdfs);
	}

	private int processTranscripts(ProcessorData processorData, List<StudentCredentialDistribution> scdList,
								   List<Student> studListNonGrad, List<InputStream> locations) {
		int currentTranscript = 0;
		int failedToAdd = 0;
		scdList.sort(Comparator.comparing(StudentCredentialDistribution::getLegalLastName, Comparator.nullsLast(String::compareTo))
				.thenComparing(StudentCredentialDistribution::getLegalFirstName, Comparator.nullsLast(String::compareTo)));
		for (StudentCredentialDistribution scd : scdList) {
			if(scd.getNonGradReasons() != null && !scd.getNonGradReasons().isEmpty()) {
				Student objStd = prepareStudentObj(scd, studListNonGrad);
				if(objStd != null)
					studListNonGrad.add(objStd);
			}
			int result = 0;
			if(scd.getStudentID() != null) {
				result = addStudentTranscriptToLocations(scd.getStudentID().toString(), locations);
			}
			if(result == 0) {
				failedToAdd++;
				log.info("*** Failed to Add PDFs {} Current student {} school {} in batch {}", failedToAdd,
						scd.getStudentID(), scd.getSchoolOfRecord(), processorData.getBatchId());
			} else {
				currentTranscript++;
				log.debug("*** Added Transcript PDFs {}/{} Current student {} - {}, {}", currentTranscript,
						scdList.size(), scd.getStudentID(), scd.getLegalLastName(), scd.getLegalFirstName());
			}
		}
		return currentTranscript;
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
			std.setLastUpdateDate(Date.from(scd.getLastUpdateDate().atZone(ZoneId.systemDefault()).toInstant()));
			std.setGraduationData(new GraduationData());
			std.setNonGradReasons(getNonGradReasons(scd.getProgram(), scd.getNonGradReasons()));

			Student scObj = studListNonGrad.stream().filter(pr -> pr.getPen().getPen().compareTo(std.getPen().getPen()) == 0)
					.findAny()
					.orElse(null);
			if (scObj == null)
				return std;
		}
		return null;
	}

	private List<NonGradReason> getNonGradReasons(String gradProgramCode, List<GradRequirement> nonGradReasons) {
		List<NonGradReason> nList = new ArrayList<>();
		if (nonGradReasons != null) {
			nonGradReasons.removeIf(a -> "506".equalsIgnoreCase(a.getRule())
					&& (StringUtils.isNotBlank(gradProgramCode) && gradProgramCode.contains("1950")));
			for (GradRequirement gR : nonGradReasons) {
				NonGradReason obj = new NonGradReason();
				obj.setCode(gR.getRule());
				obj.setDescription(gR.getDescription());
				nList.add(obj);
			}
		}
		return nList;
	}

	private void mergeCertificates(ReportRequest packSlipReq, CertificatePrintRequest certificatePrintRequest,
								   PackingSlipRequest request, ProcessorData processorData, List<Student> studListNonGrad,
								   String schoolCategoryCode) {
		List<StudentCredentialDistribution> scdList = certificatePrintRequest.getCertificateList();
		String mincode = request.getMincode();
		String paperType = request.getPaperType();
		List<InputStream> locations = new ArrayList<>();
		try {
			int currentCertificate = 0;
			int failedToAdd = 0;
			scdList.sort(Comparator.comparing(StudentCredentialDistribution::getLegalLastName, Comparator.nullsLast(String::compareTo))
					.thenComparing(StudentCredentialDistribution::getLegalFirstName, Comparator.nullsLast(String::compareTo)));
			for (StudentCredentialDistribution scd : scdList) {
				if(scd.getNonGradReasons() != null && !scd.getNonGradReasons().isEmpty()) {
					Student objStd = prepareStudentObj(scd, studListNonGrad);
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
					log.debug("*** Added {} Certificate PDFs {}/{} Current student {} - {}, {}", scd.getCredentialTypeCode(),
							currentCertificate, scdList.size(), scd.getStudentID(), scd.getLegalLastName(), scd.getLegalFirstName());
				} else {
					failedToAdd++;
					log.info("*** Failed to Add {} Certificate PDFs {} Current student {} credentials {} document status {} in batch {}",
							scd.getCredentialTypeCode(), failedToAdd, scd.getStudentID(), scd.getCredentialTypeCode(),
							scd.getDocumentStatusCode(), processorData.getBatchId());
				}
			}
			setExtraDataForPackingSlip(packSlipReq,paperType,request.getTotal(),currentCertificate,request.getCurrentSlip(),
					"Certificate", certificatePrintRequest.getBatchId());
			locations.add(0, reportService.getPackingSlip(packSlipReq).getInputStream());
			mergeDocumentsPDFs(processorData, mincode, schoolCategoryCode,"/EDGRAD.C.", paperType, locations);
		} catch (IOException e) {
			log.debug(EXCEPTION,e.getLocalizedMessage());
		}
	}

	protected void createAndSaveDistributionReport(ReportRequest distributionRequest, UUID schoolId, String mincode,
												   String schoolCategoryCode, ProcessorData processorData) {
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
				if(!processorData.getActivityCode().contains(USERDIST.getValue()))
					saveSchoolDistributionReport(encodedPdf, schoolId, DISTREP_SC.getValue());
			}
			mergeDocumentsPDFs(processorData, mincode, schoolCategoryCode,"/EDGRAD.R.","324W", locations);
		} catch (Exception e) {
			log.debug(EXCEPTION, e.getLocalizedMessage());
		}
	}

	protected Integer createAndSaveNonGradReport(ca.bc.gov.educ.api.distribution.model.dto.v2.School schoolDetails,
												 List<Student> studListNonGrad, UUID schoolId, String url) {
		ReportData nongradProjected = new ReportData();
		ca.bc.gov.educ.api.distribution.model.dto.School schObj = new ca.bc.gov.educ.api.distribution.model.dto.School();
		schObj.setMincode(schoolDetails.getMinCode());
		schObj.setName(schoolDetails.getSchoolName());
		schObj.setStudents(studListNonGrad);
		nongradProjected.setSchool(schObj);
		nongradProjected.setOrgCode(StringUtils.startsWith(nongradProjected.getSchool().getMincode(), "098") ? "YU" : "BC");
		nongradProjected.setIssueDate(EducDistributionApiUtils.formatIssueDateForReportJasper(
				new java.sql.Date(System.currentTimeMillis()).toString()));
		nongradProjected.setReportNumber("TRAX241B");
		nongradProjected.setReportTitle("Graduation Records and Achievement Data");
		nongradProjected.setReportSubTitle("Grade 12 and Adult Students Not Able to Graduate on Grad Requirements");
		ReportOptions options = new ReportOptions();
		options.setReportFile(String.format("%s_%s00_NONGRAD", schObj.getMincode(), LocalDate.now().getYear()));
		options.setReportName(String.format("%s_%s00_NONGRAD.pdf", schObj.getMincode(), LocalDate.now().getYear()));
		ReportRequest reportParams = new ReportRequest();
		reportParams.setOptions(options);
		reportParams.setData(nongradProjected);

		byte[] bytesSAR = restService.executePost(
						url,
						byte[].class,
						reportParams
				);
		byte[] encoded = Base64.encodeBase64(bytesSAR);
		assert encoded != null;
		String encodedPdf = new String(encoded, StandardCharsets.US_ASCII);
		saveSchoolDistributionReport(encodedPdf, schoolId, NONGRADDISTREP_SC.getValue());
		return 1;
	}

	private void saveSchoolDistributionReport(String encodedPdf, UUID schoolId, String reportType) {
		SchoolReports requestObj = new SchoolReports();
		requestObj.setReport(encodedPdf);
		requestObj.setSchoolOfRecordId(schoolId);
		requestObj.setReportTypeCode(reportType);
		restService.executePost(educDistributionApiConstants.getUpdateSchoolReport(), SchoolReports.class, requestObj);
	}
}
