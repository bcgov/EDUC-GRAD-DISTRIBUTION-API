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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Component
@NoArgsConstructor
public class TranscriptDataFileProcess implements DistributionProcess {
	
	private static Logger logger = LoggerFactory.getLogger(TranscriptDataFileProcess.class);
	
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
		List<StudentCredentialDistribution> tList = processorData.getTranscriptList() != null ?processorData.getTranscriptList():new ArrayList<>();
		Map<String, DistributionPrintRequest> mapDist = new HashMap<>();
		int counter = 0;
		for(StudentCredentialDistribution tran:tList) {
			counter++;
			GradSearchStudent stuRec = gradStudentService.getStudentData(tran.getStudentID().toString(), processorData.getAccessToken());
			if(stuRec != null) {
				tran.setSchoolOfRecord(stuRec.getSchoolOfRecord());
			}
			if (counter % 300 == 0) {
				accessTokenService.fetchAccessToken(processorData);
			}
			logger.info(String.format("%s of %s",counter,tList.size()));
		}
		processorData.setTranscriptList(tList);
		List<String> uniqueSchoolList = tList.stream().map(StudentCredentialDistribution::getSchoolOfRecord).distinct().collect(Collectors.toList());
		uniqueSchoolList.forEach(usl->{
			List<StudentCredentialDistribution> reducedList = tList.stream().filter(scd->scd.getSchoolOfRecord().compareTo(usl)==0).collect(Collectors.toList());
			if(!reducedList.isEmpty()) {
				TranscriptPrintRequest tpReq = new TranscriptPrintRequest();
				tpReq.setBatchId(processorData.getBatchId());
				tpReq.setPsId(usl +" " +processorData.getBatchId());
				tpReq.setCount(reducedList.size());
				tpReq.setTranscriptList(reducedList);
				if(mapDist.get(usl) != null) {
					DistributionPrintRequest dist = mapDist.get(usl);
					dist.setTranscriptPrintRequest(tpReq);
					dist.setTotal(dist.getTotal()+1);
					mapDist.put(usl,dist);
				}else{
					DistributionPrintRequest dist = new DistributionPrintRequest();
					dist.setTranscriptPrintRequest(tpReq);
					dist.setTotal(dist.getTotal()+1);
					mapDist.put(usl,dist);
				}
			}
		});
		processorData.setMapDistribution(mapDist);
		long endTime = System.currentTimeMillis();
		long diff = (endTime - startTime)/1000;
		logger.info("************* TIME Taken  ************ "+diff+" secs");
		response.setTranscriptResponse("trasncript");
		processorData.setDistributionResponse(response);
		return processorData;
	}

	@Override
    public void setInputData(ProcessorData inputData) {
		processorData = inputData;
        logger.info("TranscriptDataFileYED4Process: ");
    }

}
