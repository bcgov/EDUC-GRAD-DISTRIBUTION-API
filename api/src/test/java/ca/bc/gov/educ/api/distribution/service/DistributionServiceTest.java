package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class DistributionServiceTest {

	@Autowired
	private GradDistributionService gradDistributionService;
	
	@Autowired
	private ExceptionMessage exception;
	
	@Autowired
	private SchoolService schoolService;
	
	@Autowired
	private ReportService reportService;
	
	@MockBean
	WebClient webClient;

	@Mock
	private WebClient.RequestHeadersSpec requestHeadersMock;
	@Mock
	private WebClient.RequestHeadersUriSpec requestHeadersUriMock;
	@Mock
	private WebClient.RequestBodySpec requestBodyMock;
	@Mock
	private WebClient.RequestBodyUriSpec requestBodyUriMock;
	@Mock
	private WebClient.ResponseSpec responseMock;

	@Mock
	private Mono<InputStreamResource> inputResponse;

	@Mock
	private Mono<ReportData> inputResponseReport;

	@Mock
	private Mono<CommonSchool> inputResponseSchool;

	@Mock
	private Mono<Psi> inputResponsePsi;

	@Autowired
	private EducDistributionApiConstants constants;

	@Mock
	Path path;
	
	@Test
	public void testdistributeCredentialsMonthly() {
		DistributionResponse res = testdistributeCredentials_transcript("MER","USERDIST",false,null);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MER","USERDIST","YED2",null,true);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MER","USERDIST","YEDB",null,false);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MER","USERDIST","YEDR",null,false);
		assertNotNull(res);
	}

	@Test
	public void testdistributeCredentialsMonthlyLocalDownload() {
		DistributionResponse res = testdistributeCredentials_transcript("MER","USERDIST",false,"Y");
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MER","USERDIST","YED2",null,false);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MER","USERDIST","YEDB",null,false);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MER","USERDIST","YEDR",null,false);
		assertNotNull(res);
	}

	@Test
	public void testdistributeCredentialsMonthly_schoolNull() {
		DistributionResponse res = testdistributeCredentials_transcript("MER","USERDIST",true,null);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MER","USERDIST","YED2",null,false);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MER","USERDIST","YEDB",null,false);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MER","USERDIST","YEDR",null,false);
		assertNotNull(res);
	}

	@Test
	public void testdistributeCredentialsUserReq() {
		DistributionResponse res = testdistributeCredentials_certificate("MER","USERDISTRC","YED2","John Doe",false);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MER","USERDISTRC","YEDB","John Doe",false);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MER","USERDISTRC","YEDR","John Doe",false);
		assertNotNull(res);
	}

	@Test
	public void testdistributeCredentialsYearly() {
		DistributionResponse res = testdistributeCredentials_transcript("MERYER","USERDIST",false,null);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MERYER","USERDIST","YED2",null,false);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MERYER","USERDIST","YEDB",null,false);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MERYER","USERDIST","YEDR",null,false);
		assertNotNull(res);
	}

	@Test
	public void testdistributeCredentialsCertReprint() {
		DistributionResponse res = testdistributeCredentials_certificate_reprint("RPR","USERDIST","YED2",true);
		assertNotNull(res);
		res = testdistributeCredentials_certificate_reprint("RPR","USERDIST","YEDB",false);
		assertNotNull(res);
		res = testdistributeCredentials_certificate_reprint("RPR","USERDIST","YEDR",false);
		assertNotNull(res);
	}

	@Test
	public void testdistributeCredentialsBlankSchoolNUll() {
		DistributionResponse res = testdistributeCredentials_transcript_blank("BCPR",false,null);
		assertNotNull(res);
		res = testdistributeCredentials_certificate_blank("BCPR","YED2");
		assertNotNull(res);
		res = testdistributeCredentials_certificate_blank("BCPR","YEDB");
		assertNotNull(res);
		res = testdistributeCredentials_certificate_blank("BCPR","YEDR");
		assertNotNull(res);
	}

	@Test
	public void testdistributeCredentialsBlank() {
		DistributionResponse res = testdistributeCredentials_transcript_blank("BCPR",true,"Y");
		assertNotNull(res);
		res = testdistributeCredentials_certificate_blank("BCPR","YED2");
		assertNotNull(res);
		res = testdistributeCredentials_certificate_blank("BCPR","YEDB");
		assertNotNull(res);
		res = testdistributeCredentials_certificate_blank("BCPR","YEDR");
		assertNotNull(res);
	}

	@Test
	public void testGetDownload() {
		Long batchId= 9029L;
		byte[] arr = gradDistributionService.getDownload(batchId);
		assertNotNull(arr);
	}

	@Test
	public void testdistributeSchoolReport() {
		DistributionResponse res = testdistributeSchoolReport("PSR","GRADDIST");
		assertNotNull(res);
		res = testdistributeSchoolReport("PSR","NONGRAD");
		assertNotNull(res);
		res = testdistributeSchoolReport("PSR","NONGRADPRJ");
		assertNotNull(res);
	}

	private DistributionResponse testdistributeSchoolReport(String runType,String reportType) {
		Long batchId= 9029L;
		Map<String, DistributionPrintRequest > mapDist= new HashMap<>();
		String localDownload = null;
		String accessToken = "123";
		String mincode = "123123133";

		CommonSchool schObj = new CommonSchool();
		schObj.setSchlNo(mincode.substring(2,mincode.length()-1));
		schObj.setDistNo(mincode.substring(0,2));
		schObj.setPhysAddressLine1("sadadad");
		schObj.setPhysAddressLine2("adad");

		SchoolReportDistribution obj = new SchoolReportDistribution();
		obj.setId(UUID.randomUUID());
		obj.setReportTypeCode(reportType);
		obj.setSchoolOfRecord(mincode);

		final ResponseObj tokenObject = new ResponseObj();
		tokenObject.setAccess_token("123");
		tokenObject.setRefresh_token("456");

		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getTokenUrl())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(ResponseObj.class)).thenReturn(Mono.just(tokenObject));


		SchoolReportPostRequest tPReq = new SchoolReportPostRequest();
		tPReq.setBatchId(batchId);
		tPReq.setCount(34);
		if(reportType.equalsIgnoreCase("GRADDIST"))
			tPReq.setGradReport(obj);
		if(reportType.equalsIgnoreCase("NONGRADPRJ"))
			tPReq.setNongradprjreport(obj);
		if(reportType.equalsIgnoreCase("NONGRAD"))
			tPReq.setNongradReport(obj);

		DistributionPrintRequest printRequest = new DistributionPrintRequest();
		printRequest.setSchoolReportPostRequest(tPReq);
		mapDist.put(mincode,printRequest);

		byte[] bytesSAR = "Any String you want".getBytes();

		byte[] greBPack = "Any String you want".getBytes();
		InputStreamResource inSRPack = new InputStreamResource(new ByteArrayInputStream(greBPack));

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReport(), obj.getSchoolOfRecord(),obj.getReportTypeCode()))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(InputStreamResource.class)).thenReturn(inputResponse);
		when(this.inputResponse.block()).thenReturn(inSRPack);


		return gradDistributionService.distributeCredentials(runType,batchId,mapDist,null,null,accessToken);
	}

	private DistributionResponse testdistributeCredentials_transcript_blank(String runType,boolean schoolNull,String localDownload) {
		Long batchId= 9029L;
		Map<String, DistributionPrintRequest > mapDist= new HashMap<>();
		String accessToken = "123";
		String mincode = "123123133";

		CommonSchool schObj = null;
		if(!schoolNull) {
			schObj = new CommonSchool();
			schObj.setSchlNo(mincode.substring(2,mincode.length()-1));
			schObj.setDistNo(mincode.substring(0,2));
			schObj.setPhysAddressLine1("sadadad");
			schObj.setPhysAddressLine2("adad");
		}


		List<BlankCredentialDistribution> bcdList = new ArrayList<>();
		BlankCredentialDistribution bcd = new BlankCredentialDistribution();
		bcd.setCredentialTypeCode("BC1950-PUB");
		bcd.setPaperType("YED4");
		bcd.setQuantity(5);
		bcd.setSchoolOfRecord(mincode);

		bcdList.add(bcd);

		TranscriptPrintRequest tPReq = new TranscriptPrintRequest();
		tPReq.setBatchId(batchId);
		tPReq.setCount(34);
		tPReq.setBlankTranscriptList(bcdList);

		DistributionPrintRequest printRequest = new DistributionPrintRequest();
		printRequest.setTranscriptPrintRequest(tPReq);
		mapDist.put(mincode,printRequest);

		byte[] bytesSAR = "Any String you want".getBytes();

		byte[] greBPack = "Any String you want".getBytes();
		InputStreamResource inSRPack = new InputStreamResource(new ByteArrayInputStream(greBPack));

		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getPackingSlip())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(InputStreamResource.class)).thenReturn(Mono.just(inSRPack));

		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getTranscriptReport())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(bytesSAR));

		final ResponseObj tokenObject = new ResponseObj();
		tokenObject.setAccess_token("123");
		tokenObject.setRefresh_token("456");

		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getTokenUrl())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(ResponseObj.class)).thenReturn(Mono.just(tokenObject));

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolDetails(), mincode))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(CommonSchool.class)).thenReturn(inputResponseSchool);
		when(this.inputResponseSchool.block()).thenReturn(schObj);

		return gradDistributionService.distributeCredentials(runType,batchId,mapDist,null,localDownload,accessToken);
	}

	private DistributionResponse testdistributeCredentials_certificate_blank(String runType,String paperType) {
		Long batchId= 9029L;
		Map<String, DistributionPrintRequest > mapDist= new HashMap<>();
		String localDownload = null;
		String accessToken = "123";
		String mincode = "123123133";

		CommonSchool schObj = new CommonSchool();
		schObj.setSchlNo(mincode.substring(2,mincode.length()-1));
		schObj.setDistNo(mincode.substring(0,2));
		schObj.setPhysAddressLine1("sadadad");
		schObj.setPhysAddressLine2("adad");

		List<BlankCredentialDistribution> bcdList = new ArrayList<>();
		BlankCredentialDistribution bcd = new BlankCredentialDistribution();
		bcd.setCredentialTypeCode("BC1950-PUB");
		bcd.setPaperType("YED4");
		bcd.setQuantity(5);
		bcd.setSchoolOfRecord(mincode);

		bcdList.add(bcd);

		CertificatePrintRequest cReq = new CertificatePrintRequest();
		cReq.setBatchId(batchId);
		cReq.setCount(34);
		cReq.setBlankCertificateList(bcdList);

		DistributionPrintRequest printRequest = new DistributionPrintRequest();
		if(paperType.equalsIgnoreCase("YED2"))
			printRequest.setYed2CertificatePrintRequest(cReq);
		if(paperType.equalsIgnoreCase("YEDB"))
			printRequest.setYedbCertificatePrintRequest(cReq);
		if(paperType.equalsIgnoreCase("YEDR"))
			printRequest.setYedrCertificatePrintRequest(cReq);
		mapDist.put(mincode,printRequest);

		byte[] bytesSAR = "Any String you want".getBytes();

		byte[] greBPack = "Any String you want".getBytes();
		InputStreamResource inSRPack = new InputStreamResource(new ByteArrayInputStream(greBPack));

		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getPackingSlip())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(InputStreamResource.class)).thenReturn(Mono.just(inSRPack));

		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getCertificateReport())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(bytesSAR));

		final ResponseObj tokenObject = new ResponseObj();
		tokenObject.setAccess_token("123");
		tokenObject.setRefresh_token("456");

		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getTokenUrl())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(ResponseObj.class)).thenReturn(Mono.just(tokenObject));

		Mockito.when(schoolService.getSchoolDetails(mincode,accessToken,exception)).thenReturn(schObj);
		return gradDistributionService.distributeCredentials(runType,batchId,mapDist,null,null,accessToken);
	}

	private DistributionResponse testdistributeCredentials_transcript(String runType, String activityCode,boolean schoolNull,String localDownload) {
		Long batchId= 9029L;
		Map<String, DistributionPrintRequest > mapDist= new HashMap<>();
		String accessToken = "123";
		String mincode = "123123133";

		CommonSchool schObj = null;
		if(!schoolNull) {
			schObj = new CommonSchool();
			schObj.setSchlNo(mincode.substring(2, mincode.length() - 1));
			schObj.setDistNo(mincode.substring(0, 2));
			schObj.setPhysAddressLine1("sadadad");
			schObj.setPhysAddressLine2("adad");
		}

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

		byte[] bytesSAR = "Any String you want".getBytes();
		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getDistributionReport())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(bytesSAR));

		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getNonGrad())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(bytesSAR));

		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getUpdateSchoolReport())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(SchoolReports.class)).thenReturn(Mono.just(new SchoolReports()));

		byte[] greBPack = "Any String you want".getBytes();
		byte[] greBTran = "ASD".getBytes();
		InputStreamResource inSRPack = new InputStreamResource(new ByteArrayInputStream(greBPack));
		InputStreamResource inSRTran = new InputStreamResource(new ByteArrayInputStream(greBTran));


		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getPackingSlip())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(InputStreamResource.class)).thenReturn(Mono.just(inSRPack));

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getTranscript(), scd.getStudentID(),scd.getCredentialTypeCode(),scd.getDocumentStatusCode()))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(InputStreamResource.class)).thenReturn(inputResponse);
		when(this.inputResponse.block()).thenReturn(inSRTran);

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolDetails(), mincode))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(CommonSchool.class)).thenReturn(inputResponseSchool);
		when(this.inputResponseSchool.block()).thenReturn(schObj);

		final ResponseObj tokenObject = new ResponseObj();
		tokenObject.setAccess_token("123");
		tokenObject.setRefresh_token("456");

		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getTokenUrl())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(ResponseObj.class)).thenReturn(Mono.just(tokenObject));

		return gradDistributionService.distributeCredentials(runType,batchId,mapDist,activityCode,localDownload,accessToken);
	}


	private DistributionResponse testdistributeCredentials_certificate(String runType, String activityCode,String paperType,String properName,boolean noSchoolDis) {
		Long batchId= 9029L;
		Map<String, DistributionPrintRequest > mapDist= new HashMap<>();
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
		scd.setPaperType("YED2");
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

		List<StudentCredentialDistribution> scdCertList = new ArrayList<>();
		StudentCredentialDistribution scdCert = new StudentCredentialDistribution();
		scdCert.setCredentialTypeCode("E");
		scdCert.setPen("123213133");
		scdCert.setProgram("1950");
		scdCert.setStudentID(UUID.randomUUID());
		scdCert.setSchoolOfRecord(mincode);
		scdCert.setPaperType("YED2");
		scdCert.setStudentGrade("AD");
		scdCert.setLegalFirstName("asda");
		scdCert.setLegalMiddleNames("sd");
		scdCert.setLegalLastName("322f");
		scdCert.setNonGradReasons(nongradReasons);

		scdCertList.add(scdCert);

		CertificatePrintRequest cReq = new CertificatePrintRequest();
		cReq.setBatchId(batchId);
		cReq.setCount(34);
		cReq.setCertificateList(scdCertList);

		SchoolDistributionRequest sdReq = null;
		if(!noSchoolDis) {
			sdReq = new SchoolDistributionRequest();
			sdReq.setCount(34);
			sdReq.setBatchId(batchId);
			sdReq.setStudentList(scdList);
		}

		DistributionPrintRequest printRequest = new DistributionPrintRequest();
		printRequest.setSchoolDistributionRequest(sdReq);
		if(properName != null)
			printRequest.setProperName(properName);
		if(paperType.equalsIgnoreCase("YED2"))
			printRequest.setYed2CertificatePrintRequest(cReq);
		if(paperType.equalsIgnoreCase("YEDB"))
			printRequest.setYedbCertificatePrintRequest(cReq);
		if(paperType.equalsIgnoreCase("YEDR"))
			printRequest.setYedrCertificatePrintRequest(cReq);
		mapDist.put(mincode,printRequest);

		byte[] bytesSAR = "Any String you want".getBytes();
		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getDistributionReport())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(bytesSAR));

		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getNonGrad())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(bytesSAR));

		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getUpdateSchoolReport())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(SchoolReports.class)).thenReturn(Mono.just(new SchoolReports()));

		byte[] greBPack = "Any String you want".getBytes();
		byte[] greBCert = "DER".getBytes();
		InputStreamResource inSRPack = new InputStreamResource(new ByteArrayInputStream(greBPack));
		InputStreamResource inSRCert = new InputStreamResource(new ByteArrayInputStream(greBCert));


		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getPackingSlip())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(InputStreamResource.class)).thenReturn(Mono.just(inSRPack));

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getCertificate(), scdCert.getStudentID(),scdCert.getCredentialTypeCode(),scdCert.getDocumentStatusCode()))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(InputStreamResource.class)).thenReturn(inputResponse);
		when(this.inputResponse.block()).thenReturn(inSRCert);

		final ResponseObj tokenObject = new ResponseObj();
		tokenObject.setAccess_token("123");
		tokenObject.setRefresh_token("456");

		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getTokenUrl())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(ResponseObj.class)).thenReturn(Mono.just(tokenObject));

		if(properName == null)
			Mockito.when(schoolService.getSchoolDetails(mincode,accessToken,exception)).thenReturn(schObj);
		return gradDistributionService.distributeCredentials(runType,batchId,mapDist,activityCode,localDownload,accessToken);
	}

	private DistributionResponse testdistributeCredentials_certificate_reprint(String runType, String activityCode,String paperType,boolean schoolNull) {
		Long batchId= 9029L;
		Map<String, DistributionPrintRequest > mapDist= new HashMap<>();
		String localDownload = null;
		String accessToken = "123";
		String mincode = "123123133";

		CommonSchool schObj =null;
		if(!schoolNull) {
			schObj = new CommonSchool();
			schObj.setSchlNo(mincode.substring(2, mincode.length() - 1));
			schObj.setDistNo(mincode.substring(0, 2));
			schObj.setPhysAddressLine1("sadadad");
			schObj.setPhysAddressLine2("adad");
		}

		List<StudentCredentialDistribution> scdList = new ArrayList<>();
		StudentCredentialDistribution scd = new StudentCredentialDistribution();
		scd.setCredentialTypeCode("BC1950-PUB");
		scd.setPen("123213133");
		scd.setProgram("1950");
		scd.setStudentID(UUID.randomUUID());
		scd.setSchoolOfRecord(mincode);
		scd.setPaperType("YED2");
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

		List<StudentCredentialDistribution> scdCertList = new ArrayList<>();
		StudentCredentialDistribution scdCert = new StudentCredentialDistribution();
		scdCert.setCredentialTypeCode("E");
		scdCert.setPen("123213133");
		scdCert.setProgram("1950");
		scdCert.setStudentID(UUID.randomUUID());
		scdCert.setSchoolOfRecord(mincode);
		scdCert.setPaperType("YED2");
		scdCert.setStudentGrade("AD");
		scdCert.setLegalFirstName("asda");
		scdCert.setLegalMiddleNames("sd");
		scdCert.setLegalLastName("322f");
		scdCert.setNonGradReasons(nongradReasons);

		scdCertList.add(scdCert);

		CertificatePrintRequest cReq = new CertificatePrintRequest();
		cReq.setBatchId(batchId);
		cReq.setCount(34);
		cReq.setCertificateList(scdCertList);

		SchoolDistributionRequest sdReq = new SchoolDistributionRequest();
		sdReq.setCount(34);
		sdReq.setBatchId(batchId);
		sdReq.setStudentList(scdList);

		DistributionPrintRequest printRequest = new DistributionPrintRequest();
		printRequest.setSchoolDistributionRequest(sdReq);
		if(paperType.equalsIgnoreCase("YED2"))
			printRequest.setYed2CertificatePrintRequest(cReq);
		if(paperType.equalsIgnoreCase("YEDB"))
			printRequest.setYedbCertificatePrintRequest(cReq);
		if(paperType.equalsIgnoreCase("YEDR"))
			printRequest.setYedrCertificatePrintRequest(cReq);
		mapDist.put(mincode,printRequest);

		byte[] bytesSAR = "Any String you want".getBytes();
		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getDistributionReport())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(bytesSAR));

		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getCertificateReport())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(bytesSAR));

		byte[] greBPack = "Any String you want".getBytes();
		byte[] greBCert = "DER".getBytes();
		InputStreamResource inSRPack = new InputStreamResource(new ByteArrayInputStream(greBPack));
		InputStreamResource inSRCert = new InputStreamResource(new ByteArrayInputStream(greBCert));


		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getPackingSlip())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(InputStreamResource.class)).thenReturn(Mono.just(inSRPack));

		ReportData data = new ReportData();
		data.setStudent(new Student());
		Certificate cert = new Certificate();
		cert.setIssued(new Date());
		cert.setCertStyle("Reprint");

		OrderType oType = new OrderType();
		oType.setName("Certificate");
		CertificateType cType = new CertificateType();
		cType.setReportName("Certificate");
		PaperType pType = new PaperType();
		pType.setCode("E");
		cType.setPaperType(pType);
		oType.setCertificateType(cType);
		cert.setOrderType(oType);
		data.setCertificate(cert);

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getCertDataReprint(), scdCert.getPen()))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(ReportData.class)).thenReturn(inputResponseReport);
		when(this.inputResponseReport.block()).thenReturn(data);

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolDetails(), mincode))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(CommonSchool.class)).thenReturn(inputResponseSchool);
		when(this.inputResponseSchool.block()).thenReturn(schObj);

		final ResponseObj tokenObject = new ResponseObj();
		tokenObject.setAccess_token("123");
		tokenObject.setRefresh_token("456");

		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getTokenUrl())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(ResponseObj.class)).thenReturn(Mono.just(tokenObject));

		return gradDistributionService.distributeCredentials(runType,batchId,mapDist,activityCode,localDownload,accessToken);
	}

	@Test
	public void testdistributeCredentialsPSI() {
		DistributionResponse res = testpsidistributeCredential("PSPR",null);
		assertNotNull(res);
	}

	private DistributionResponse testpsidistributeCredential(String runType, String localDownload) {
		String activityCode = null;
		Long batchId= 9029L;
		Map<String, DistributionPrintRequest > mapDist= new HashMap<>();
		String accessToken = "123";
		String psiCode = "001";

		Psi psiObj = new Psi();
		psiObj.setPsiCode("001");
		psiObj.setAddress2("sadasd");
		psiObj.setAddress1("sadaasdadad");
		psiObj.setCity("adad");


		List<PsiCredentialDistribution> scdList = new ArrayList<>();
		PsiCredentialDistribution scd = new PsiCredentialDistribution();
		scd.setPen("123213133");
		scd.setStudentID(UUID.randomUUID());
		scd.setPsiCode("001");
		scdList.add(scd);


		PsiCredentialPrintRequest cReq = new PsiCredentialPrintRequest();
		cReq.setBatchId(batchId);
		cReq.setCount(34);
		cReq.setPsiList(scdList);


		DistributionPrintRequest printRequest = new DistributionPrintRequest();
		printRequest.setPsiCredentialPrintRequest(cReq);
		mapDist.put(psiCode,printRequest);

		byte[] bytesSAR = "Any String you want".getBytes();
		InputStreamResource inSRCert = new InputStreamResource(new ByteArrayInputStream(bytesSAR));
		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getTranscriptUsingStudentID(), scd.getStudentID()))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(InputStreamResource.class)).thenReturn(inputResponse);
		when(this.inputResponse.block()).thenReturn(inSRCert);

		byte[] greBPack = "Any String you want".getBytes();
		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getPackingSlip())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(greBPack));


		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getPsiDetails(), psiCode))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(Psi.class)).thenReturn(inputResponsePsi);
		when(this.inputResponsePsi.block()).thenReturn(psiObj);

		final ResponseObj tokenObject = new ResponseObj();
		tokenObject.setAccess_token("123");
		tokenObject.setRefresh_token("456");

		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getTokenUrl())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(ResponseObj.class)).thenReturn(Mono.just(tokenObject));

		return gradDistributionService.distributeCredentials(runType,batchId,mapDist,activityCode,localDownload,accessToken);
	}

}
