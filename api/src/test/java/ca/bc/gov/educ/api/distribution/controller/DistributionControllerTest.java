package ca.bc.gov.educ.api.distribution.controller;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.model.dto.v2.School;
import ca.bc.gov.educ.api.distribution.service.GradDistributionService;
import ca.bc.gov.educ.api.distribution.service.PostingDistributionService;
import ca.bc.gov.educ.api.distribution.util.GradValidation;
import ca.bc.gov.educ.api.distribution.util.MessageHelper;
import ca.bc.gov.educ.api.distribution.util.RestUtils;
import io.undertow.util.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
	private PostingDistributionService postingDistributionService;
	
	@Mock
    RestUtils response;
	
	@InjectMocks
	private DistributionController distributionController;
	
	@Mock
	GradValidation validation;
	
	@Mock
	MessageHelper messagesHelper;
	
	@Mock
	SecurityContextHolder securityContextHolder;

	@ParameterizedTest
	@ValueSource(strings = {"USERDIST", "PSPR"})
	void testDistributeCredentials(String activityCode) throws BadRequestException {
		String runType = "MER";
		Long batchId= 9029L;
		Map<String, DistributionPrintRequest> mapDist= new HashMap<>();
		String transmissionMode = "paper";
		String mincode = "123123133";
		UUID schoolId = UUID.randomUUID();

		ca.bc.gov.educ.api.distribution.model.dto.v2.School schObj = new ca.bc.gov.educ.api.distribution.model.dto.v2.School();
		schObj.setSchoolId(UUID.randomUUID().toString());
		schObj.setMinCode(mincode);
		schObj.setAddress1("sadadad");
		schObj.setAddress2("adad");

		List<StudentCredentialDistribution> scdList = new ArrayList<>();
		StudentCredentialDistribution scd = new StudentCredentialDistribution();
		scd.setCredentialTypeCode("BC1950-PUB");
		scd.setPen("123213133");
		scd.setProgram("1950");
		scd.setStudentID(UUID.randomUUID());
		scd.setSchoolId(schoolId);
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
		mapDist.put(schoolId.toString(), printRequest);

		DistributionResponse res = new DistributionResponse();
		res.setMergeProcessResponse("MERGED");

		DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(mapDist).build();
		Mockito.when(gradDistributionService.distributeCredentials(runType,batchId,distributionRequest,activityCode, transmissionMode.toUpperCase(),null,"accessToken")).thenReturn(res);
		distributionController.distributeCredentials(runType,batchId,activityCode,transmissionMode.toUpperCase(),distributionRequest,null,"accessToken");
		Mockito.verify(gradDistributionService).distributeCredentials(runType,batchId,distributionRequest,activityCode,transmissionMode.toUpperCase(),null,"accessToken");
	}

	@Test
	void testDistributeCredentialsAsynchronously() throws BadRequestException {
		String runType = "MER";
		String activityCode = "MONTHLYDIST";
		Long batchId= 9029L;
		Map<String, DistributionPrintRequest> mapDist= new HashMap<>();
		String transmissionMode = "paper";
		String mincode = "123123133";
		UUID schoolId = UUID.randomUUID();

		ca.bc.gov.educ.api.distribution.model.dto.v2.School schObj = new School();
		schObj.setSchoolId(schoolId.toString());
		schObj.setMinCode(mincode);
		schObj.setAddress1("sadadad");
		schObj.setAddress2("adad");

		List<StudentCredentialDistribution> scdList = new ArrayList<>();
		StudentCredentialDistribution scd = new StudentCredentialDistribution();
		scd.setCredentialTypeCode("BC1950-PUB");
		scd.setPen("123213133");
		scd.setProgram("1950");
		scd.setStudentID(UUID.randomUUID());
		scd.setSchoolId(schoolId);
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
		mapDist.put(schoolId.toString(),printRequest);

		DistributionResponse res = new DistributionResponse();
		res.setMergeProcessResponse("MERGED");

		DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(mapDist).build();
		Mockito.doNothing().when(gradDistributionService).asyncDistributeCredentials(runType,batchId,distributionRequest,activityCode, transmissionMode.toUpperCase(),null,"accessToken");
		distributionController.distributeCredentials(runType,batchId,activityCode,transmissionMode.toUpperCase(),distributionRequest,null,"accessToken");
		Mockito.verify(gradDistributionService).asyncDistributeCredentials(runType,batchId,distributionRequest,activityCode,transmissionMode.toUpperCase(),null,"accessToken");
	}


	@Test
	void testDownloadZipFile() {
		Long batchId= 9029L;
		String transmissionMode = "ftp";
		byte[] bytesSAR = "Any String you want".getBytes();
		Mockito.when(gradDistributionService.getDownload(batchId, transmissionMode.toUpperCase())).thenReturn(bytesSAR);
		distributionController.downloadZipFile(batchId, transmissionMode.toUpperCase());
		Mockito.verify(gradDistributionService).getDownload(batchId, transmissionMode.toUpperCase());
	}

	@Test
	void testPostingDistribution() {
		DistributionResponse res = new DistributionResponse();
		Mockito.when(postingDistributionService.postingProcess(res)).thenReturn(Boolean.TRUE);
		distributionController.postingDistribution(res);
		Mockito.verify(postingDistributionService).postingProcess(res);
	}
	
}
