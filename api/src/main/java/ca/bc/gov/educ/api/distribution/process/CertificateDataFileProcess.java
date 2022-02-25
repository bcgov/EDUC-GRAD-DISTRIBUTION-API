package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.service.AccessTokenService;
import ca.bc.gov.educ.api.distribution.service.GradStudentService;
import ca.bc.gov.educ.api.distribution.util.GradValidation;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Component
@NoArgsConstructor
public class CertificateDataFileProcess implements DistributionProcess {
	
	private static Logger logger = LoggerFactory.getLogger(CertificateDataFileProcess.class);
	
	@Autowired
    private ProcessorData processorData;

	@Autowired
	private GradStudentService gradStudentService;

	@Autowired
	GradValidation validation;

	@Autowired
	AccessTokenService accessTokenService;
	
	
	@Override
	public ProcessorData fire() {				
		long startTime = System.currentTimeMillis();
		logger.info("************* TIME START  ************ "+startTime);
		DistributionResponse response = new DistributionResponse();
		List<StudentCredentialDistribution> cList = new ArrayList<>();
		if(processorData.getCertificateProcessType().equalsIgnoreCase("YED2")) {
			cList = processorData.getYed2List() != null ? processorData.getYed2List() : new ArrayList<>();
		}else if(processorData.getCertificateProcessType().equalsIgnoreCase("YEDB")) {
			cList = processorData.getYed2List() != null ? processorData.getYedbList() : new ArrayList<>();
		}else if(processorData.getCertificateProcessType().equalsIgnoreCase("YEDR")) {
			cList = processorData.getYed2List() != null ? processorData.getYedrList() : new ArrayList<>();
		}

		List<StudentCredentialDistribution> tList = processorData.getTranscriptList();
		Map<String, DistributionPrintRequest> mapDist = processorData.getMapDistribution();
		int counter = 0;
		for(StudentCredentialDistribution cert:cList) {
			counter++;
			StudentCredentialDistribution scObj = tList.stream().filter(pr -> pr.getStudentID().compareTo(cert.getStudentID()) == 0)
					.findAny()
					.orElse(null);
			if(scObj != null) {
				cert.setSchoolOfRecord(scObj.getSchoolOfRecord());
			}else {
				GradSearchStudent stuRec = gradStudentService.getStudentData(cert.getStudentID().toString(), processorData.getAccessToken());
				if (stuRec != null) {
					cert.setSchoolOfRecord(stuRec.getSchoolOfRecord());
				}
			}
			if (counter % 300 == 0) {
				accessTokenService.fetchAccessToken(processorData);
			}
			logger.info(String.format("%s of %s",counter,cList.size()));
		}

		List<String> uniqueSchoolList = cList.stream().map(StudentCredentialDistribution::getSchoolOfRecord).distinct().collect(Collectors.toList());
		List<StudentCredentialDistribution> finalCList = cList;
		uniqueSchoolList.forEach(usl->{
			List<StudentCredentialDistribution> reducedList = finalCList.stream().filter(scd->scd.getSchoolOfRecord().compareTo(usl)==0).collect(Collectors.toList());
			if(!reducedList.isEmpty()) {
				CertificatePrintRequest cpReq = new CertificatePrintRequest();
				cpReq.setBatchId(processorData.getBatchId());
				cpReq.setPsId(usl +" " +processorData.getBatchId());
				cpReq.setCount(reducedList.size());
				cpReq.setCertificateList(reducedList);
				if(mapDist.get(usl) != null) {
					DistributionPrintRequest dist = mapDist.get(usl);
					dist.setYed2CertificatePrintRequest(cpReq);
					dist.setTotal(dist.getTotal()+1);
					mapDist.put(usl,dist);
				}else{
					DistributionPrintRequest dist = new DistributionPrintRequest();
					dist.setYed2CertificatePrintRequest(cpReq);
					dist.setTotal(dist.getTotal()+1);
					mapDist.put(usl,dist);
				}
			}
		});
		processorData.setMapDistribution(mapDist);

		if(processorData.getCertificateProcessType().equalsIgnoreCase("YED2")) {
			response.setYed2Response("yed2 certificate");
		}else if(processorData.getCertificateProcessType().equalsIgnoreCase("YEDB")) {
			response.setYed2Response("yedb certificate");
		}else if(processorData.getCertificateProcessType().equalsIgnoreCase("YEDR")) {
			response.setYed2Response("yedr certificate");
		}
		processorData.setDistributionResponse(response);

		long endTime = System.currentTimeMillis();
		long diff = (endTime - startTime)/1000;
		logger.info("************* TIME Taken  ************ "+diff+" secs");
		return processorData;
	}

	@Override
    public void setInputData(ProcessorData inputData) {
		processorData = inputData;
        logger.info("CertificateDataFileYED2Process: ");
    }

}
