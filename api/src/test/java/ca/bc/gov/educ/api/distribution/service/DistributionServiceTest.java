package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.JsonTransformer;
import ca.bc.gov.educ.api.distribution.util.RestUtils;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
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
	
	@MockBean
	private SchoolService schoolService;

	@Autowired
	JsonTransformer jsonTransformer;
	
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

	@MockBean
	private RestUtils restUtils;
	
	private static final String MOCK_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE2NzcxMDM0NzYsImlhdCI6MTY3NzEwMzE3NiwiYXV0aF90aW1lIjoxNjc3MTAyMjk0LCJqdGkiOiJkNWE5MTQ1Ny1mYzVjLTQ4YmItODNiZC1hYjMyYmEwMzQ1MzIiLCJpc3MiOiJodHRwczovL3NvYW0tZGV2LmFwcHMuc2lsdmVyLmRldm9wcy5nb3YuYmMuY2EvYXV0aC9yZWFsbXMvbWFzdGVyIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6IjIzZGYxMzJlLTE3NTQtNDYzYi05MGI1LWIyN2E4ODIxMjM0NSIsInR5cCI6IkJlYXJlciIsImF6cCI6ImZha2VfY2xpZW50Iiwic2Vzc2lvbl9zdGF0ZSI6IjUzY2UxNDBiLTMzMTctNDA3NC04YmEzLWIwYWE3MTIzMjQ1NCIsImFjciI6IjAiLCJhbGxvd2VkLW9yaWdpbnMiOlsiaHR0cHM6Ly9kZXYuZ3JhZC5nb3YuYmMuY2EiLCJodHRwczovL2dyYWQuZ292LmJjLmNhIl0sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJyb2xlXzEiLCJyb2xlXzIiXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbInJvbGVfMSJdfX0sInNjb3BlIjoiTVlfU0NPUEUifQ.D57DWJJuLPFIj84A14EmRlKSKcLVOG9HLvc-OCWTTeM";

	@Mock
	Path path;

	@Test
	public void testdistributeCredentialsMonthly() {
		DistributionResponse res = testdistributeCredentials_transcript("MER","MONTHLYDIST",false,"Y", true);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MER","MONTHLYDIST","YED2",null,true, true);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MER","MONTHLYDIST","YEDB",null,false, true);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MER","MONTHLYDIST","YEDR",null,false, true);
		assertNotNull(res);
	}

	@Test
	public void testdistributeCredentialsUserRequest() {
		DistributionResponse res = testdistributeCredentials_transcript("MER","USERDIST",false,"Y", false);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MER","USERDIST","YED2",null,true, false);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MER","USERDIST","YEDB",null,false, false);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MER","USERDIST","YEDR",null,false, false);
		assertNotNull(res);
	}

	@Test
	public void testdistributeCredentialsMonthlyLocalDownload() {
		DistributionResponse res = testdistributeCredentials_transcript("MER","USERDIST",false,"Y", false);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MER","USERDIST","YED2",null,false, false);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MER","USERDIST","YEDB",null,false, false);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MER","USERDIST","YEDR",null,false, false);
		assertNotNull(res);
	}

	@Test
	public void testdistributeCredentialsMonthly_schoolNull() {
		DistributionResponse res = testdistributeCredentials_transcript("MER","USERDIST",true,"Y", false);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MER","USERDIST","YED2",null,false, false);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MER","USERDIST","YEDB",null,false, false);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MER","USERDIST","YEDR",null,false, false);
		assertNotNull(res);
	}

	@Test
	public void testdistributeCredentialsUserReq() {
		DistributionResponse res = testdistributeCredentials_certificate("MER","USERDISTRC","YED2","John Doe",false, false);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MER","USERDISTRC","YEDB","John Doe",false, false);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MER","USERDISTRC","YEDR","John Doe",false, false);
		assertNotNull(res);
	}

	@Test
	public void testdistributeCredentialsYearly() {
		DistributionResponse res = testdistributeCredentials_transcript("MERYER","DISTRUN_YE",false,"Y", true);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MERYER","DISTRUN_YE","YED2",null,false, true);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MERYER","DISTRUN_YE","YEDB",null,false, true);
		assertNotNull(res);
		res = testdistributeCredentials_certificate("MERYER","DISTRUN_YE","YEDR",null,false, true);
		assertNotNull(res);
	}

	@Test
	public void testdistributeSchoolReports() {
		DistributionResponse res = testdistributeSchoolReport("MERYER","DISTRUN_YE", "YEARENDDIST");
		assertNotNull(res);
		res = testdistributeSchoolReport("MER","DISTRUN", "MONTHLYDIST");
		assertNotNull(res);
		res = testdistributeSchoolReport("MERSUPP","DISTRUN_SUPP", "SUPPDIST");
		assertNotNull(res);
		res = testdistributeSchoolReport("MERYER","DISTRUN_YE", "NONGRADDIST");
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
		DistributionResponse res = testdistributeCredentials_transcript_blank("BCPR",false,"Y");
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
		String transmissionMode = "ftp";
		byte[] arr = gradDistributionService.getDownload(batchId, transmissionMode.toUpperCase());
		assertNotNull(arr);
	}

	@Test
	public void testdistributeSchoolReport() {
		DistributionResponse res = testdistributeSchoolReport("PSR","DISTREP_SC", null);
		assertNotNull(res);
//		res = testdistributeSchoolReport("PSR","NONGRADDISTREP_SC", null);
//		assertNotNull(res);
//		res = testdistributeSchoolReport("PSR","NONGRADPRJ", null);
//		assertNotNull(res);
	}

	@SneakyThrows
	private DistributionResponse testdistributeSchoolReport(String runType, String reportType, String activityCode) {
		Long batchId= 9029L;
		Map<String, DistributionPrintRequest > mapDist = new HashMap<>();
		String localDownload = "Y";
		String transmissionMode = "ftp";
		String accessToken = MOCK_TOKEN;
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

		Mockito.doReturn(getMockResponseObject()).when(this.restUtils).getTokenResponseObject();

		SchoolReportPostRequest tPReq = new SchoolReportPostRequest();
		tPReq.setBatchId(batchId);
		tPReq.setCount(34);
		if(reportType.equalsIgnoreCase("DISTREP_SC"))
			tPReq.setGradReport(obj);
		if(reportType.equalsIgnoreCase("NONGRADPRJ"))
			tPReq.setNongradprjreport(obj);
		if(reportType.equalsIgnoreCase("NONGRADDISTREP_SC"))
			tPReq.setNongradReport(obj);

		DistributionPrintRequest printRequest = new DistributionPrintRequest();
		printRequest.setSchoolReportPostRequest(tPReq);
		mapDist.put(mincode,printRequest);

		mockTraxSchool(mincode);

		byte[] bytesSAR = "Any String you want".getBytes();

		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getStudentNonGrad())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(bytesSAR));

		byte[] greBPack = "Any String you want".getBytes();
		InputStreamResource inSRPack = new InputStreamResource(new ByteArrayInputStream(greBPack));

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReport(), obj.getSchoolOfRecord(), obj.getReportTypeCode()))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(InputStreamResource.class)).thenReturn(inputResponse);
		when(this.inputResponse.block()).thenReturn(inSRPack);

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReport(), null, null))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(InputStreamResource.class)).thenReturn(inputResponse);
		when(this.inputResponse.block()).thenReturn(inSRPack);

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getCommonSchoolByMincode(), mincode))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(CommonSchool.class)).thenReturn(inputResponseSchool);
		when(this.inputResponseSchool.block()).thenReturn(schObj);

		System.out.println(jsonTransformer.marshall(printRequest));

			SchoolReports schoolLabelsReports = new SchoolReports();
			schoolLabelsReports.setSchoolOfRecord("000000000");
			schoolLabelsReports.setReportTypeCode("ADDRESS_LABEL_YE");
			schoolLabelsReports.setId(UUID.randomUUID());

			when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
			when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReportsByReportType(), "ADDRESS_LABEL_YE", schoolLabelsReports.getSchoolOfRecord()))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
			when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
			})).thenReturn(Mono.just(List.of(schoolLabelsReports)));

			when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
			when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReportsByReportType(), "ADDRESS_LABEL_SCHL", schoolLabelsReports.getSchoolOfRecord()))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
			when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
			})).thenReturn(Mono.just(List.of(schoolLabelsReports)));

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReportsByReportType(), "ADDRESS_LABEL_SCHL", ""))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReportsByReportType(), "ADDRESS_LABEL_SCHL", mincode))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

		when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
		})).thenReturn(Mono.just(List.of(schoolLabelsReports)));

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReportsByReportType(), "ADDRESS_LABEL_YE", ""))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReportsByReportType(), "ADDRESS_LABEL_YE", mincode))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
		})).thenReturn(Mono.just(List.of(schoolLabelsReports)));

			when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
			when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReport(), "000000000", "ADDRESS_LABEL_YE", schoolLabelsReports.getSchoolOfRecord()))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
			when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(UUID.randomUUID().toString().getBytes()));

			when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
			when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReport(), "000000000", "ADDRESS_LABEL_SCHL"))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
			when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(UUID.randomUUID().toString().getBytes()));

			SchoolReports districtReports = new SchoolReports();
			districtReports.setSchoolOfRecord("005");
			districtReports.setReportTypeCode("DISTREP_YE_SD");
			districtReports.setSchoolOfRecordName("Sooke");
			districtReports.setId(UUID.randomUUID());

			when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
			when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReportsByReportType(), "DISTREP_YE_SD", obj.getSchoolOfRecord()))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
			when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
			})).thenReturn(Mono.just(List.of(districtReports)));

		SchoolReports schoolReports = new SchoolReports();
		districtReports.setSchoolOfRecord(mincode);
		schoolReports.setSchoolCategory("02");
		districtReports.setReportTypeCode("DISTREP_YE_SC");
		districtReports.setSchoolOfRecordName("Langford");
		districtReports.setId(UUID.randomUUID());

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReportsByReportType(), "DISTREP_YE_SC", obj.getSchoolOfRecord()))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
		})).thenReturn(Mono.just(List.of(schoolReports)));

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReportsByReportType(), "DISTREP_YE_SC", ""))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
		})).thenReturn(Mono.just(List.of(schoolReports)));

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReportsByReportType(), "NONGRADDISTREP_SC", obj.getSchoolOfRecord()))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
		})).thenReturn(Mono.just(List.of(schoolReports)));

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReportsByReportType(), "DISTREP_SC", obj.getSchoolOfRecord()))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
		})).thenReturn(Mono.just(List.of(schoolReports)));

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReportsByReportType(), "DISTREP_YE_SD", districtReports.getSchoolOfRecord()))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
		})).thenReturn(Mono.just(List.of(districtReports)));


		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReportsByReportType(), "DISTREP_YE_SD", ""))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
		})).thenReturn(Mono.just(List.of(districtReports)));

			when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
			when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReportsByReportType(), "DISTREP_SD", districtReports.getSchoolOfRecord()))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
			when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
			})).thenReturn(Mono.just(List.of(districtReports)));

			when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
			when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReport(), "005", "DISTREP_YE_SD"))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
			when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(UUID.randomUUID().toString().getBytes()));

			when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
			when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReport(), "005", "DISTREP_SD"))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
			when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(UUID.randomUUID().toString().getBytes()));

			when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
			when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReportsByReportType(), "DISTREP_YE_SC", schoolReports.getSchoolOfRecord()))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
			when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
			})).thenReturn(Mono.just(List.of(schoolReports)));

			when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
			when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReportsByReportType(), "DISTREP_SC", schoolReports.getSchoolOfRecord()))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
			when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
			})).thenReturn(Mono.just(List.of(schoolReports)));

			when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
			when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReport(), "00500201", "DISTREP_YE_SC"))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
			when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(UUID.randomUUID().toString().getBytes()));

			when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
			when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReport(), "00500201", "DISTREP_SC"))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
			when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(UUID.randomUUID().toString().getBytes()));

			when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
			when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolDistrictYearEndReport(), "null", "DISTREP_YE_SD", "DISTREP_YE_SC"))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
			when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(4));

			when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
			when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolDistrictMonthReport(), "ADDRESS_LABEL_SCHL", null, null))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
			when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
			when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(2));

		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(String.format(constants.getSchoolLabelsReport(), "ADDRESS_LABEL_SCHL"))).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolDistrictSupplementalReport(), "ADDRESS_LABEL_SCHL", null, "DISTREP_SC"))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(2));

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolDistrictSupplementalReport(), "ADDRESS_LABEL_SCHL", null, null))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(2));

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolDistrictSupplementalReport(), null, null, "DISTREP_SC"))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(2));

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolDistrictStudentNonGradReport(), "ADDRESS_LABEL_YE", null, null))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(String.format(constants.getSchoolLabelsReport(), "ADDRESS_LABEL_YE"))).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		//when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolDistrictYearEndReport(), "ADDRESS_LABEL_YE", "DISTREP_YE_SD", "DISTREP_YE_SC"))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(2));

		Psi psi = new Psi();
		psi.setPsiCode("001");
		psi.setPsiName("Test PSI");

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getPsiByPsiCode(), "001"))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(Psi.class)).thenReturn(Mono.just(psi));

		Mockito.when(schoolService.getCommonSchoolDetails(mincode,exception)).thenReturn(schObj);

		DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(mapDist).build();
		return gradDistributionService.distributeCredentials(runType,batchId,distributionRequest,activityCode,transmissionMode.toUpperCase(),"Y",accessToken);
	}

	private ResponseObj getMockResponseObject(){
		ResponseObj obj = new ResponseObj();
		obj.setAccess_token(MOCK_TOKEN);
		obj.setRefresh_token("456");
		return obj;
	}

	private DistributionResponse testdistributeCredentials_transcript_blank(String runType,boolean schoolNull,String localDownload) {
		Long batchId= 9029L;
		Map<String, DistributionPrintRequest > mapDist= new HashMap<>();
		String accessToken = MOCK_TOKEN;
		String transmissionMode = "paper";
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

		mockTraxSchool(mincode);

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

		Mockito.doReturn(getMockResponseObject()).when(this.restUtils).getTokenResponseObject();

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getCommonSchoolByMincode(), mincode))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(CommonSchool.class)).thenReturn(inputResponseSchool);
		when(this.inputResponseSchool.block()).thenReturn(schObj);

		DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(mapDist).build();
		return gradDistributionService.distributeCredentials(runType,batchId,distributionRequest,null,transmissionMode,localDownload,accessToken);
	}

	private DistributionResponse testdistributeCredentials_certificate_blank(String runType,String paperType) {
		Long batchId= 9029L;
		Map<String, DistributionPrintRequest > mapDist= new HashMap<>();
		String localDownload = "Y";
		String transmissionMode = "ftp";
		String accessToken = MOCK_TOKEN;
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

		mockTraxSchool(mincode);

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

		Mockito.doReturn(getMockResponseObject()).when(this.restUtils).getTokenResponseObject();

		Mockito.when(schoolService.getCommonSchoolDetails(mincode,exception)).thenReturn(schObj);
		DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(mapDist).build();
		return gradDistributionService.distributeCredentials(runType,batchId,distributionRequest,null, transmissionMode,"Y",accessToken);
	}

	private DistributionResponse testdistributeCredentials_transcript(String runType, String activityCode,boolean schoolNull,String localDownload, boolean isAsyncProcess) {
		Long batchId= 9029L;
		Map<String, DistributionPrintRequest > mapDist= new HashMap<>();
		String accessToken = MOCK_TOKEN;
		String transmissionMode = "ftp";
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

		mockTraxSchool(mincode);

		byte[] bytesSAR = "Any String you want".getBytes();
		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getSchoolDistributionReport())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(bytesSAR));

		SchoolReports schoolLabelsReports = new SchoolReports();
		schoolLabelsReports.setSchoolOfRecord("");
		schoolLabelsReports.setReportTypeCode("ADDRESS_LABEL_SCHL");
		schoolLabelsReports.setId(UUID.randomUUID());

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReportsByReportType(), "ADDRESS_LABEL_SCHL", schoolLabelsReports.getSchoolOfRecord()))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getStudentNonGrad())).thenReturn(this.requestBodyUriMock);
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

		GradStudentTranscripts studentTranscripts = new GradStudentTranscripts();
		studentTranscripts.setStudentID(scd.getStudentID());
		studentTranscripts.setTranscript(Base64.encodeBase64String(greBPack));

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getTranscriptUsingStudentID(), scd.getStudentID()))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<GradStudentTranscripts>>() {
		})).thenReturn(Mono.just(List.of(studentTranscripts)));

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getCommonSchoolByMincode(), mincode))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(CommonSchool.class)).thenReturn(inputResponseSchool);
		when(this.inputResponseSchool.block()).thenReturn(schObj);

		Mockito.doReturn(getMockResponseObject()).when(this.restUtils).getTokenResponseObject();
//		Mockito.doReturn(schObj).when(schoolService).getCommonSchoolDetails(mincode,exception);

		ProcessorData data = new ProcessorData();
		data.setAccessToken(MOCK_TOKEN);
		DistributionResponse response = new DistributionResponse();
		response.setBatchId(batchId);
		data.setDistributionResponse(response);

		DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(mapDist).build();
		if (isAsyncProcess) {
			response.setJobStatus("success");
			Mockito.doNothing().when(this.restUtils).notifyDistributionJobIsCompleted(batchId, data);
			response.setJobStatus("error");
			Mockito.doNothing().when(this.restUtils).notifyDistributionJobIsCompleted(batchId, data);

			gradDistributionService.asyncDistributeCredentials(runType, batchId, distributionRequest, activityCode, transmissionMode, localDownload, accessToken);
			DistributionResponse disRes = new DistributionResponse();
			disRes.setBatchId(batchId);
			disRes.setLocalDownload(localDownload);
			disRes.setMergeProcessResponse("Merge Successful and File Uploaded");
			return disRes;
		} else {
			return gradDistributionService.distributeCredentials(runType, batchId, distributionRequest, activityCode, transmissionMode, localDownload, accessToken);
		}
	}


	private DistributionResponse testdistributeCredentials_certificate(String runType, String activityCode,String paperType,String properName,boolean noSchoolDis, boolean isAsyncProcess) {
		Long batchId= 9029L;
		Map<String, DistributionPrintRequest > mapDist= new HashMap<>();
		String localDownload = "Y";
		String transmissionMode = "ftp";
		String accessToken = MOCK_TOKEN;
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

		mockTraxSchool(mincode);

		byte[] bytesSAR = "Any String you want".getBytes();
		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getSchoolDistributionReport())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(bytesSAR));

		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getStudentNonGrad())).thenReturn(this.requestBodyUriMock);
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

		SchoolReports schoolLabelsReports = new SchoolReports();
		schoolLabelsReports.setSchoolOfRecord("");
		schoolLabelsReports.setReportTypeCode("ADDRESS_LABEL_SCHL");
		schoolLabelsReports.setId(UUID.randomUUID());

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReportsByReportType(), "ADDRESS_LABEL_SCHL", schoolLabelsReports.getSchoolOfRecord()))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReportsByReportType(), "ADDRESS_LABEL_SCHL", ""))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

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

		Mockito.doReturn(getMockResponseObject()).when(this.restUtils).getTokenResponseObject();

		if(properName == null)
			Mockito.doReturn(schObj).when(schoolService).getCommonSchoolDetails(mincode,exception);
		else
			Mockito.doReturn(schObj).when(schoolService).getCommonSchoolDetailsForPackingSlip(properName);

		ProcessorData data = new ProcessorData();
		data.setAccessToken(MOCK_TOKEN);
		DistributionResponse response = new DistributionResponse();
		response.setBatchId(batchId);
		data.setDistributionResponse(response);

		DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(mapDist).build();
		if (isAsyncProcess) {
			response.setJobStatus("success");
			Mockito.doNothing().when(this.restUtils).notifyDistributionJobIsCompleted(batchId, data);
			response.setJobStatus("error");
			Mockito.doNothing().when(this.restUtils).notifyDistributionJobIsCompleted(batchId, data);

			gradDistributionService.asyncDistributeCredentials(runType, batchId, distributionRequest, activityCode, transmissionMode, localDownload, accessToken);
			DistributionResponse disRes = new DistributionResponse();
			disRes.setBatchId(batchId);
			disRes.setLocalDownload(localDownload);
			disRes.setMergeProcessResponse("Merge Successful and File Uploaded");
			return disRes;
		} else {
			return gradDistributionService.distributeCredentials(runType, batchId, distributionRequest, activityCode, transmissionMode, localDownload, accessToken);
		}
	}

	private DistributionResponse testdistributeCredentials_certificate_reprint(String runType, String activityCode,String paperType,boolean schoolNull) {
		Long batchId= 9029L;
		Map<String, DistributionPrintRequest > mapDist= new HashMap<>();
		String localDownload = "Y";
		String accessToken = MOCK_TOKEN;
		String transmissionMode = "ftp";
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

		mockTraxSchool(mincode);

		byte[] bytesSAR = "Any String you want".getBytes();
		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getSchoolDistributionReport())).thenReturn(this.requestBodyUriMock);
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
		when(this.requestHeadersUriMock.uri(String.format(constants.getCommonSchoolByMincode(), mincode))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(CommonSchool.class)).thenReturn(inputResponseSchool);
		when(this.inputResponseSchool.block()).thenReturn(schObj);

		Mockito.doReturn(getMockResponseObject()).when(this.restUtils).getTokenResponseObject();

		DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(mapDist).build();
		return gradDistributionService.distributeCredentials(runType,batchId,distributionRequest,activityCode,transmissionMode,localDownload,accessToken);
	}

	@Test
	public void testdistributeCredentialsPSI() {
		DistributionResponse res = testpsidistributeCredential("PSPR","Y", "ftp");
		assertNotNull(res);
		res = testpsidistributeCredential("PSPR","Y", "paper");
		assertNotNull(res);
	}

    // testcase for PSIRUNs for both ftp and paper.
	private DistributionResponse testpsidistributeCredential(String runType, String localDownload, String transmissionMode) {
		String activityCode = null;
		Long batchId= 9029L;
		Map<String, DistributionPrintRequest > mapDist= new HashMap<>();
		String accessToken = MOCK_TOKEN;
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

		//Grad2-1931 Setting properties for report data to test PSI FTP transmission mode.
		ReportData data = new ReportData();
		Student student = new Student();
		student.setFirstName("aaa");
		student.setLastName("bbbb");
		student.setBirthdate(new Date(270597));
		GraduationData gradData = new GraduationData();
		gradData.setDogwoodFlag(true);
		gradData.setHonorsFlag(false);
		gradData.setTotalCreditsUsedForGrad("");
		GraduationStatus gradStatus = new GraduationStatus();
		gradStatus.setSchoolOfRecord("cccc");
		gradStatus.setGraduationMessage("xxxx");
		student.setGraduationData(gradData);
		student.setGraduationStatus(gradStatus);
		data.setStudent(student);
		data.setSchool(new School());
		Transcript transcript = new Transcript();
		data.setTranscript(transcript);
		TranscriptResult result1 = new TranscriptResult();
		TranscriptResult result2 = new TranscriptResult();
		TranscriptResult result3 = new TranscriptResult();
		List<TranscriptResult> listOfResults = new ArrayList<TranscriptResult>();
		listOfResults.add(result1);
		listOfResults.add(result2);
		listOfResults.add(result3);
		transcript.setResults(listOfResults);
		TranscriptCourse course1 = new TranscriptCourse();
		TranscriptCourse course2 = new TranscriptCourse();
		TranscriptCourse course3 = new TranscriptCourse();
		result1.setEquivalency("Result1Course1Type1");
		result2.setEquivalency("Result2Course2Type2");
		result3.setEquivalency("Result3Course3type3");
		result1.setCourse(course1);
		result2.setCourse(course2);
		result3.setCourse(course3);
		course1.setType("1");
		course2.setType("2");
		course3.setType("3");
		Mark mark = new Mark();
		mark.setInterimLetterGrade("A");
		result1.setMark(mark);
		result2.setMark(mark);
		result3.setMark(mark);
		result1.setUsedForGrad("04");


		byte[] bytesSAR = "Any String you want".getBytes();
		InputStreamResource inSRCert = new InputStreamResource(new ByteArrayInputStream(bytesSAR));
		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getTranscriptPsiUsingStudentID(), scd.getStudentID()))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(InputStreamResource.class)).thenReturn(inputResponse);
		when(this.inputResponse.block()).thenReturn(inSRCert);

		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getStudentNonGrad())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(bytesSAR));

		byte[] greBPack = "Any String you want".getBytes();
		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(constants.getPackingSlip())).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(greBPack));

		GradStudentTranscripts studentTranscripts = new GradStudentTranscripts();
		studentTranscripts.setStudentID(scd.getStudentID());
		studentTranscripts.setTranscript(Base64.encodeBase64String(greBPack));

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getTranscriptUsingStudentID(), scd.getStudentID()))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<GradStudentTranscripts>>() {
		})).thenReturn(Mono.just(List.of(studentTranscripts)));

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReportsByReportType(), "ADDRESS_LABEL_PSI", "000000000"))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
		})).thenReturn(Mono.just(List.of(new SchoolReports())));

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReportsByReportType(), "ADDRESS_LABEL_PSI", ""))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getSchoolReportsByReportType(), "ADDRESS_LABEL_PSI", psiCode))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
		})).thenReturn(Mono.just(List.of(new SchoolReports())));

		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(String.format(constants.getSchoolLabelsReport(), "ADDRESS_LABEL_PSI", null, null))).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

		when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(String.format(constants.getSchoolLabelsReport(), "ADDRESS_LABEL_SCHL", null, null))).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
		when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getPsiByPsiCode(), psiCode))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(Psi.class)).thenReturn(inputResponsePsi);
		when(this.inputResponsePsi.block()).thenReturn(psiObj);

		//Grad2-1931 checking for CSV transcripts - mchintha
		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getTranscriptCSVData(), scd.getPen()))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(ReportData.class)).thenReturn(inputResponseReport);
		when(this.inputResponseReport.block()).thenReturn(data);

		Mockito.doReturn(getMockResponseObject()).when(this.restUtils).getTokenResponseObject();

		DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(mapDist).build();
        return gradDistributionService.distributeCredentials(runType,batchId,distributionRequest,activityCode,transmissionMode.toUpperCase(),localDownload,accessToken);
	}

	protected void mockTraxSchool(String mincode) {
		TraxSchool traxSchool = new TraxSchool();
		traxSchool.setMinCode(mincode);
		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(String.format(constants.getTraxSchoolByMincode(),traxSchool.getMinCode()))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(TraxSchool.class)).thenReturn(Mono.just(traxSchool));
	}
}
