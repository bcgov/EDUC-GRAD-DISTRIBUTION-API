package ca.bc.gov.educ.api.distribution.controller;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.service.GradDistributionService;
import ca.bc.gov.educ.api.distribution.util.GradValidation;
import ca.bc.gov.educ.api.distribution.util.MessageHelper;
import ca.bc.gov.educ.api.distribution.util.ResponseHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;


@ExtendWith(MockitoExtension.class)
class DistributionControllerTest {

	@Mock
	private GradDistributionService gradDistributionService;
	
	@Mock
	ResponseHelper response;
	
	@InjectMocks
	private DistributionController distributionController;
	
	@Mock
	GradValidation validation;
	
	@Mock
	MessageHelper messagesHelper;
	
	@Mock
	SecurityContextHolder securityContextHolder;
	
	@Test
	void testDistributeCredentials() {
		String runType = "MER";
		String activityCode = "USERDIST";
		Long batchId= 9029L;
		Map<String, DistributionPrintRequest> mapDist= new HashMap<>();
		String localDownload = null;
		String accessToken = "123";
		String mincode = "123123133";

		CommonSchool schObj = new CommonSchool();
		schObj.setSchlNo(mincode.substring(2,mincode.length()-1));
		schObj.setDistNo(mincode.substring(0,2));
		schObj.setPhysAddressLine1("sadadad");
		schObj.setPhysAddressLine2("adad");

		List<StudentCredentialDistribution> scdList = new ArrayList<>();
		StudentCredentialDistribution scd = new StudentCredentialDistribution();
		scd.setCredentialTypeCode("BC1950-PUB");
		scd.setPen("123213133");
		scd.setProgram("1950");
		scd.setStudentID(UUID.randomUUID());
		scd.setSchoolOfRecord(mincode);
		scd.setPaperType("YED4");
		scd.setStudentGrade("AD");
		scd.setLegalFirstName("asda");
		scd.setLegalMiddleNames("sd");
		scd.setLegalLastName("322f");

		List<GradRequirement> nongradReasons = new ArrayList<>();
		GradRequirement gR= new GradRequirement();
		gR.setRule("100");
		gR.setDescription("Not Passed");
		gR.setProjected(false);
		nongradReasons.add(gR);
		scd.setNonGradReasons(nongradReasons);

		scdList.add(scd);
		TranscriptPrintRequest tPReq = new TranscriptPrintRequest();
		tPReq.setBatchId(batchId);
		tPReq.setCount(34);
		tPReq.setTranscriptList(scdList);

		SchoolDistributionRequest sdReq = new SchoolDistributionRequest();
		sdReq.setCount(34);
		sdReq.setBatchId(batchId);
		sdReq.setStudentList(scdList);

		DistributionPrintRequest printRequest = new DistributionPrintRequest();
		printRequest.setTranscriptPrintRequest(tPReq);
		printRequest.setSchoolDistributionRequest(sdReq);
		mapDist.put(mincode,printRequest);

		DistributionResponse res = new DistributionResponse();
		res.setMergeProcessResponse("MERGED");

		Mockito.when(gradDistributionService.distributeCredentials(runType,batchId,mapDist,activityCode,null,"accessToken")).thenReturn(res);
		distributionController.distributeCredentials(runType,batchId,activityCode,mapDist,null,"accessToken");
		Mockito.verify(gradDistributionService).distributeCredentials(runType,batchId,mapDist,activityCode,null,"accessToken");
	}


	@Test
	void testDownloadZipFile() {
		Long batchId= 9029L;
		byte[] bytesSAR = "Any String you want".getBytes();
		Mockito.when(gradDistributionService.getDownload(batchId)).thenReturn(bytesSAR);
		distributionController.downloadZipFile(batchId);
		Mockito.verify(gradDistributionService).getDownload(batchId);
	}
	
}
