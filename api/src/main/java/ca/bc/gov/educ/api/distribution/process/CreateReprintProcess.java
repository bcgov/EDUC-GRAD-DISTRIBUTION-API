package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.constants.SchoolCategoryCodes;
import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.model.dto.v2.School;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Data
@Component
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CreateReprintProcess extends BaseProcess {
	
	@Override
	public ProcessorData fire(ProcessorData processorData) {
		long startTime = System.currentTimeMillis();
		log.debug("************* TIME START  ************ {}",startTime);
		DistributionResponse response = new DistributionResponse();
		ExceptionMessage exception = new ExceptionMessage();
		DistributionRequest distributionRequest = processorData.getDistributionRequest();
		Map<String, DistributionPrintRequest> mapDist = distributionRequest.getMapDist();
		StudentSearchRequest searchRequest = distributionRequest.getStudentSearchRequest();
		Long batchId = processorData.getBatchId();
		int numberOfPdfs = 0;
		int counter=0;
		for (Map.Entry<String, DistributionPrintRequest> entry : mapDist.entrySet()) {
			UUID schoolId = UUID.fromString(entry.getKey());
			counter++;
			int currentSlipCount = 0;
			DistributionPrintRequest distributionPrintRequest = entry.getValue();
			School schoolDetails = getBaseSchoolDetails(distributionPrintRequest, searchRequest, schoolId, exception);
			if(schoolDetails != null) {
				log.debug("*** School Details Acquired {}", schoolDetails.getSchoolName());

				ReportRequest packSlipReq = reportService
						.preparePackingSlipData(searchRequest, schoolDetails, processorData.getBatchId());

				if(distributionPrintRequest.getSchoolDistributionRequest() != null) {
					ReportRequest schoolDistributionReportRequest = reportService.prepareSchoolDistributionReportData(
							distributionPrintRequest.getSchoolDistributionRequest(), processorData.getBatchId(), schoolDetails);
					createAndSaveDistributionReport(schoolDistributionReportRequest, schoolDetails.getMinCode(), processorData);
					numberOfPdfs++;
				}
				numberOfPdfs = processYed2Certificate(distributionPrintRequest, currentSlipCount, packSlipReq,
						schoolDetails.getMinCode(), processorData, numberOfPdfs);
				numberOfPdfs = processYedbCertificate(distributionPrintRequest, currentSlipCount, packSlipReq,
						schoolDetails.getMinCode(), processorData, numberOfPdfs);
				numberOfPdfs = processYedrCertificate(distributionPrintRequest, currentSlipCount, packSlipReq,
						schoolDetails.getMinCode(), processorData, numberOfPdfs);

				log.debug("PDFs Merged {}", schoolDetails.getSchoolName());
				log.debug("{} School {}/{}", schoolDetails.getMinCode(), counter, mapDist.size());
				if (counter % 50 == 0) {
					restUtils.fetchAccessToken(processorData);
				}
			}
		}
		boolean postingStatus = postingProcess(batchId,processorData,numberOfPdfs);
		long endTime = System.currentTimeMillis();
		long diff = (endTime - startTime)/1000;
		log.debug("************* TIME Taken  ************ {} secs",diff);
		response.setMergeProcessResponse(postingStatus ? "COMPLETED": "FAILED");
		response.setNumberOfPdfs(numberOfPdfs);
		response.setLocalDownload(processorData.getLocalDownload());
		response.setBatchId(processorData.getBatchId());
		response.setActivityCode(distributionRequest.getActivityCode());
		response.setStudentSearchRequest(searchRequest);
		processorData.setDistributionResponse(response);
		return processorData;
	}

	private int processYedrCertificate(DistributionPrintRequest obj, int currentSlipCount, ReportRequest packSlipReq,
									   String mincode,ProcessorData processorData, int numberOfPdfs) {
		if (obj.getYedrCertificatePrintRequest() != null) {
			currentSlipCount++;
			numberOfPdfs = numberOfPdfs + processCertificatePrintFile(packSlipReq, obj.getYedrCertificatePrintRequest(),
					mincode, currentSlipCount, obj, processorData,"YEDR");
			log.debug("*** YEDR Documents Merged");
		}
		return numberOfPdfs;
	}

	private int processYedbCertificate(DistributionPrintRequest obj, int currentSlipCount, ReportRequest packSlipReq,
									   String mincode,ProcessorData processorData, int numberOfPdfs) {
		if (obj.getYedbCertificatePrintRequest() != null) {
			currentSlipCount++;
			numberOfPdfs = numberOfPdfs + processCertificatePrintFile(packSlipReq, obj.getYedbCertificatePrintRequest(),
					mincode, currentSlipCount, obj, processorData,"YEDB");
			log.debug("*** YEDB Documents Merged");
		}
		return numberOfPdfs;
	}

	private int processYed2Certificate(DistributionPrintRequest obj, int currentSlipCount, ReportRequest packSlipReq,
									   String mincode, ProcessorData processorData, int numberOfPdfs) {
		if (obj.getYed2CertificatePrintRequest() != null) {
			currentSlipCount++;
			numberOfPdfs = numberOfPdfs + processCertificatePrintFile(packSlipReq, obj.getYed2CertificatePrintRequest(),
					mincode, currentSlipCount, obj, processorData,"YED2");
			log.debug("*** YED2 Documents Merged");
		}
		return numberOfPdfs;
	}

	private int processCertificatePrintFile(ReportRequest packSlipReq, CertificatePrintRequest certificatePrintRequest,
											String mincode, int currentSlipCount, DistributionPrintRequest obj,
											ProcessorData processorData, String paperType) {
		PackingSlipRequest request = PackingSlipRequest.builder()
				.mincode(mincode)
				.currentSlip(currentSlipCount)
				.total(obj.getTotal())
				.paperType(paperType)
				.build();
		return mergeCertificates(packSlipReq, certificatePrintRequest, request, processorData);
	}

	private int mergeCertificates(ReportRequest packSlipReq, CertificatePrintRequest certificatePrintRequest,
								  PackingSlipRequest request, ProcessorData processorData) {
		List<StudentCredentialDistribution> scdList = certificatePrintRequest.getCertificateList();
		String mincode = request.getMincode();
		String paperType = request.getPaperType();
		List<InputStream> locations=new ArrayList<>();
		int currentCertificate = 0;
		int failedToAdd = 0;
		try {
			setExtraDataForPackingSlip(packSlipReq, paperType, request.getTotal(), scdList.size(), request.getCurrentSlip(),
					"Certificate", certificatePrintRequest.getBatchId());
			locations.add(reportService.getPackingSlip(packSlipReq).getInputStream());
			for (StudentCredentialDistribution scd : scdList) {
				ReportData data = restService.executeGet(educDistributionApiConstants.getCertDataReprint(),
						ReportData.class, scd.getPen());
				if(data != null) {
					if(data.getCertificate() == null) {
						log.info("Certificate doesn't exists for student {}", scd.getPen());
						continue;
					}
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
				byte[] bytesSAR = restService.executePost(educDistributionApiConstants.getCertificateReport(),
						byte[].class, reportParams);
				if (bytesSAR != null) {
					locations.add(new ByteArrayInputStream(bytesSAR));
					currentCertificate++;
					log.debug("*** Added {} Certificate PDFs {}/{} Current student {}", scd.getCredentialTypeCode(),
							currentCertificate, scdList.size(), scd.getStudentID());
				} else {
					failedToAdd++;
					log.info("*** Failed to Add {} Certificate PDFs {} Current student {} in batch {}",
							scd.getCredentialTypeCode(), failedToAdd, scd.getStudentID(), processorData.getBatchId());
				}
			}
			mergeDocumentsPDFs(processorData, mincode, SchoolCategoryCodes.INDEPEND.getCode(), "/EDGRAD.C.", paperType, locations);
		} catch (IOException e) {
			log.debug(EXCEPTION,e.getMessage());
		}
		return currentCertificate;
	}

	private void createAndSaveDistributionReport(ReportRequest distributionRequest, String mincode, ProcessorData processorData) {
		List<InputStream> locations=new ArrayList<>();
		try {
			byte[] bytesSAR = restService.executePost(educDistributionApiConstants.getSchoolDistributionReport(),
					byte[].class, distributionRequest);
			if(bytesSAR != null) {
				locations.add(new ByteArrayInputStream(bytesSAR));
			}
			mergeDocumentsPDFs(processorData,mincode,SchoolCategoryCodes.INDEPEND.getCode(),"/EDGRAD.R.","324W",locations);
		} catch (Exception e) {
			log.debug(EXCEPTION,e.getMessage());
		}
	}
}
