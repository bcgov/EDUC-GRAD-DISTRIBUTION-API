package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.DistributionResponse;
import ca.bc.gov.educ.api.distribution.model.dto.SchoolReports;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import org.wildfly.common.Assert;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
public class PostingProcessServiceTest {

    @Autowired
    private PostingDistributionService postingDistributionService;
    @Autowired
    private EducDistributionApiConstants educDistributionApiConstants;

    @Mock
    RestService restService;
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
    @MockBean
    WebClient webClient;


    @Test
    public void testPostingProcess() {
        DistributionResponse response = new DistributionResponse();
        response.setBatchId(12345L);
        response.setActivityCode(YEARENDDIST);
        response.setLocalDownload("Y");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndReport(), null, DISTREP_YE_SD, DISTREP_YE_SC))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

        SchoolReports distrepYeSd = new SchoolReports();
        distrepYeSd.setId(UUID.randomUUID());
        distrepYeSd.setReportTypeCode(DISTREP_YE_SD);
        distrepYeSd.setSchoolOfRecord(RandomStringUtils.randomNumeric(3));
        distrepYeSd.setSchoolOfRecordName(RandomStringUtils.randomAlphabetic(15));
        distrepYeSd.setSchoolCategory(RandomStringUtils.randomNumeric(3));
        distrepYeSd.setReport(Base64.getEncoder().encodeToString(RandomStringUtils.randomAlphanumeric(25).getBytes()));

        SchoolReports distrepYeSc = new SchoolReports();
        distrepYeSc.setId(UUID.randomUUID());
        distrepYeSc.setReportTypeCode(DISTREP_YE_SC);
        distrepYeSc.setSchoolOfRecord(RandomStringUtils.randomNumeric(6));
        distrepYeSc.setSchoolOfRecordName(RandomStringUtils.randomAlphabetic(15));
        distrepYeSc.setSchoolCategory(RandomStringUtils.randomNumeric(3));
        distrepYeSc.setReport(Base64.getEncoder().encodeToString(RandomStringUtils.randomAlphanumeric(25).getBytes()));

        SchoolReports addressLabelSchl = new SchoolReports();
        addressLabelSchl.setId(UUID.randomUUID());
        addressLabelSchl.setReportTypeCode(ADDRESS_LABEL_SCHL);
        addressLabelSchl.setSchoolOfRecord(RandomStringUtils.randomNumeric(6));
        addressLabelSchl.setSchoolOfRecordName(RandomStringUtils.randomAlphabetic(15));
        addressLabelSchl.setSchoolCategory(RandomStringUtils.randomNumeric(3));
        addressLabelSchl.setReport(Base64.getEncoder().encodeToString(RandomStringUtils.randomAlphanumeric(25).getBytes()));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolReportsByReportType(), addressLabelSchl.getReportTypeCode(), ""))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
        })).thenReturn(Mono.just(List.of(addressLabelSchl)));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolReportsByReportType(), addressLabelSchl.getReportTypeCode(), addressLabelSchl.getSchoolOfRecord()))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
        })).thenReturn(Mono.just(List.of(addressLabelSchl)));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolReportsByReportType(), distrepYeSd.getReportTypeCode(), ""))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
        })).thenReturn(Mono.just(List.of(distrepYeSd)));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolReportsByReportType(), distrepYeSd.getReportTypeCode(), distrepYeSd.getSchoolOfRecord()))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
        })).thenReturn(Mono.just(List.of(distrepYeSd)));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolReportsByReportType(), distrepYeSc.getReportTypeCode(), ""))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
        })).thenReturn(Mono.just(List.of(distrepYeSc)));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolReportsByReportType(), distrepYeSc.getReportTypeCode(), distrepYeSc.getSchoolOfRecord()))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
        })).thenReturn(Mono.just(List.of(distrepYeSc)));

        byte[] bytesPdf = readBinaryFile("data/sample.pdf");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolReport(), distrepYeSc.getSchoolOfRecord(), distrepYeSc.getReportTypeCode()))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(bytesPdf));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolReport(), distrepYeSd.getSchoolOfRecord(), distrepYeSd.getReportTypeCode()))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(bytesPdf));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolReport(), addressLabelSchl.getSchoolOfRecord(), addressLabelSchl.getReportTypeCode()))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(bytesPdf));

        var result = this.postingDistributionService.postingProcess(response);
        Assert.assertTrue(result);
    }

    @SneakyThrows
    private byte[] readBinaryFile(String path) {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(path);
        return inputStream.readAllBytes();
    }
}
