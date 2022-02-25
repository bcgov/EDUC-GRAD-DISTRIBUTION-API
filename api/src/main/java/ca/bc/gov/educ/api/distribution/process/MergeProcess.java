package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.service.AccessTokenService;
import ca.bc.gov.educ.api.distribution.service.GradStudentService;
import ca.bc.gov.educ.api.distribution.service.ReportService;
import ca.bc.gov.educ.api.distribution.service.SchoolService;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils;
import ca.bc.gov.educ.api.distribution.util.GradValidation;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Component
@NoArgsConstructor
public class MergeProcess implements DistributionProcess {
	
	private static Logger logger = LoggerFactory.getLogger(MergeProcess.class);
	
	@Autowired
    private ProcessorData processorData;

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
	
	@Override
	public ProcessorData fire() {				
		long startTime = System.currentTimeMillis();
		logger.info("************* TIME START  ************ "+startTime);
		DistributionResponse response = new DistributionResponse();
		ExceptionMessage exception = new ExceptionMessage();
		Map<String,DistributionPrintRequest> mapDist = processorData.getMapDistribution();
		processorData.setMapDistribution(mapDist);
		int counter=0;
		for (Map.Entry<String, DistributionPrintRequest> entry : mapDist.entrySet()) {
			counter++;
			String mincode = entry.getKey();
			SchoolTrax schoolDetails = schoolService.getSchoolDetails(mincode,processorData.getAccessToken(),exception);
			logger.info("School Details Acquired {}",schoolDetails.getSchoolName());
			ReportRequest packSlipReq = reportService.preparePackingSlipData(schoolDetails,processorData.getBatchId());
			DistributionPrintRequest obj = entry.getValue();
			if(obj.getTranscriptPrintRequest() != null) {
				TranscriptPrintRequest transcriptPrintRequest = obj.getTranscriptPrintRequest();
				List<StudentCredentialDistribution> scdList = transcriptPrintRequest.getTranscriptList();
				List<InputStream> locations=new ArrayList<InputStream>();
				setExtraDataForPackingSlip(packSlipReq,"YED4",obj.getTotal(),scdList.size(),1,"Transcript",transcriptPrintRequest.getBatchId());
				try {
					locations.add(reportService.getPackingSlip(packSlipReq,processorData.getAccessToken(),exception).getInputStream());
					logger.info("Packing Slip Added");
					for (StudentCredentialDistribution scd : scdList) {
						InputStreamResource transcriptPdf = webClient.get().uri(String.format(educDistributionApiConstants.getTranscript(),scd.getStudentID(),scd.getCredentialTypeCode(),"COMPL")).headers(h -> h.setBearerAuth(processorData.getAccessToken())).retrieve().bodyToMono(InputStreamResource.class).block();
						locations.add(transcriptPdf.getInputStream());
					}
					PDFMergerUtility objs = new PDFMergerUtility();
					Path path = Paths.get("/tmp/"+mincode+"/");
					Files.createDirectories(path);
					objs.setDestinationFileName("/tmp/"+mincode+"/GRAD.T.YED4."+ EducDistributionApiUtils.getFileName()+".pdf");
					objs.addSources(locations);
					objs.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			if(obj.getYed2CertificatePrintRequest() != null) {
				CertificatePrintRequest certificatePrintRequest = obj.getYed2CertificatePrintRequest();
				PackingSlipRequest request = PackingSlipRequest.builder().mincode(mincode).currentSlip(2).total(obj.getTotal()).paperType("YED2").build();
				mergeCertificates(packSlipReq,certificatePrintRequest,request,exception);
			}
			if(obj.getYedbCertificatePrintRequest() != null) {
				CertificatePrintRequest certificatePrintRequest = obj.getYedbCertificatePrintRequest();
				PackingSlipRequest request = PackingSlipRequest.builder().mincode(mincode).currentSlip(2).total(obj.getTotal()).paperType("YEDB").build();
				mergeCertificates(packSlipReq,certificatePrintRequest,request,exception);
			}
			if(obj.getYedrCertificatePrintRequest() != null) {
				CertificatePrintRequest certificatePrintRequest = obj.getYedrCertificatePrintRequest();
				PackingSlipRequest request = PackingSlipRequest.builder().mincode(mincode).currentSlip(2).total(obj.getTotal()).paperType("YEDR").build();
				mergeCertificates(packSlipReq,certificatePrintRequest,request,exception);
			}
			logger.info("PDFs Merged {}",schoolDetails.getSchoolName());
			if (counter % 50 == 0) {
				accessTokenService.fetchAccessToken(processorData);
			}
		}
		long endTime = System.currentTimeMillis();
		long diff = (endTime - startTime)/1000;
		logger.info("************* TIME Taken  ************ "+diff+" secs");
		return processorData;
	}

	private void setExtraDataForPackingSlip(ReportRequest packSlipReq, String paperType, int total, int quantity, int currentSlip, String orderType, Long batchId) {
		packSlipReq.getData().getPackingSlip().setTotal(total);
		packSlipReq.getData().getPackingSlip().setCurrent(currentSlip);
		packSlipReq.getData().getPackingSlip().setQuantity(quantity);
		packSlipReq.getData().getPackingSlip().getOrderType().getPackingSlipType().getPaperType().setCode(paperType);
		packSlipReq.getData().getPackingSlip().getOrderType().setName(orderType);
		packSlipReq.getData().getPackingSlip().setOrderNumber(batchId);
	}

	private void mergeCertificates(ReportRequest packSlipReq, CertificatePrintRequest certificatePrintRequest,PackingSlipRequest request, ExceptionMessage exception) {
		List<StudentCredentialDistribution> scdList = certificatePrintRequest.getCertificateList();
		String mincode = request.getMincode();
		String paperType = request.getPaperType();
		List<InputStream> locations=new ArrayList<InputStream>();
		setExtraDataForPackingSlip(packSlipReq,paperType,request.getTotal(),scdList.size(),request.getCurrentSlip(),"Certificate", certificatePrintRequest.getBatchId());
		try {
			locations.add(reportService.getPackingSlip(packSlipReq,processorData.getAccessToken(),exception).getInputStream());
			for (StudentCredentialDistribution scd : scdList) {
				InputStreamResource certificatePdf = webClient.get().uri(String.format(educDistributionApiConstants.getCertificate(),scd.getStudentID(),scd.getCredentialTypeCode(),"COMPL")).headers(h -> h.setBearerAuth(processorData.getAccessToken())).retrieve().bodyToMono(InputStreamResource.class).block();
				locations.add(certificatePdf.getInputStream());
			}
			PDFMergerUtility objs = new PDFMergerUtility();
			Path path = Paths.get("C:/Users/s.karekkattumanasree/Downloads/"+mincode+"/");
			Files.createDirectories(path);
			objs.setDestinationFileName("C:/Users/s.karekkattumanasree/Downloads/"+mincode+"/GRAD.C."+paperType+"."+ EducDistributionApiUtils.getFileName()+".pdf");
			objs.addSources(locations);
			objs.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
    public void setInputData(ProcessorData inputData) {
		processorData = inputData;
        logger.info("MergeProcess: ");
    }

}
