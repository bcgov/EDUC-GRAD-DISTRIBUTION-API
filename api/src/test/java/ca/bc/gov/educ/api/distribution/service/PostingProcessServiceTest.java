package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.DistributionResponse;
import ca.bc.gov.educ.api.distribution.model.dto.School;
import ca.bc.gov.educ.api.distribution.model.dto.SchoolReports;
import ca.bc.gov.educ.api.distribution.model.dto.TraxDistrict;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.WebClient;
import org.wildfly.common.Assert;
import reactor.core.publisher.Mono;

import java.io.IOException;
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
        response.setLocalDownload("N");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndReport(), "", DISTREP_YE_SD, ""))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndReport(), "", "", DISTREP_YE_SC))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndReport(), "", NONGRADDISTREP_SD, ""))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

        SchoolReports distrepYeSd = new SchoolReports();
        String distrepYeSdMincode = RandomStringUtils.randomNumeric(3);
        distrepYeSd.setId(UUID.randomUUID());
        distrepYeSd.setReportTypeCode(DISTREP_YE_SD);
        distrepYeSd.setSchoolOfRecord(distrepYeSdMincode);
        distrepYeSd.setSchoolOfRecordName(RandomStringUtils.randomAlphabetic(15));
        distrepYeSd.setSchoolCategory(RandomStringUtils.randomNumeric(2));
        distrepYeSd.setReport(Base64.getEncoder().encodeToString(RandomStringUtils.randomAlphanumeric(25).getBytes()));

        SchoolReports distrepYeNgSd = new SchoolReports();
        String distrepYeNgSdMincode = RandomStringUtils.randomNumeric(3);
        distrepYeNgSd.setId(UUID.randomUUID());
        distrepYeNgSd.setReportTypeCode(NONGRADDISTREP_SD);
        distrepYeNgSd.setSchoolOfRecord(distrepYeNgSdMincode);
        distrepYeNgSd.setSchoolOfRecordName(RandomStringUtils.randomAlphabetic(15));
        distrepYeNgSd.setSchoolCategory(RandomStringUtils.randomNumeric(2));
        distrepYeNgSd.setReport(Base64.getEncoder().encodeToString(RandomStringUtils.randomAlphanumeric(25).getBytes()));

        SchoolReports distrepYeSc = new SchoolReports();
        String distrepYeScMincode = RandomStringUtils.randomNumeric(6);
        distrepYeSc.setId(UUID.randomUUID());
        distrepYeSc.setReportTypeCode(DISTREP_YE_SC);
        distrepYeSc.setSchoolOfRecord(distrepYeScMincode);
        distrepYeSc.setSchoolOfRecordName(RandomStringUtils.randomAlphabetic(15));
        distrepYeSc.setSchoolCategory(RandomStringUtils.randomNumeric(2));
        distrepYeSc.setReport(Base64.getEncoder().encodeToString(RandomStringUtils.randomAlphanumeric(25).getBytes()));

        School district = new School();
        district.setMincode(distrepYeSd.getSchoolOfRecord());
        district.setName(distrepYeSd.getSchoolOfRecordName());
        district.setSchoolCategoryCode(distrepYeSd.getSchoolCategory());

        response.getDistricts().add(district);

        School districtNg = new School();
        districtNg.setMincode(distrepYeNgSd.getSchoolOfRecord());
        districtNg.setName(distrepYeNgSd.getSchoolOfRecordName());
        districtNg.setSchoolCategoryCode(distrepYeNgSd.getSchoolCategory());

        response.getDistricts().add(districtNg);

        School school = new School();
        school.setMincode(distrepYeSc.getSchoolOfRecord());
        school.setName(distrepYeSc.getSchoolOfRecordName());
        school.setSchoolCategoryCode(distrepYeSc.getSchoolCategory());

        response.getSchools().add(school);

        SchoolReports addressLabelSchl = new SchoolReports();
        String addressLabelSchlMincode = RandomStringUtils.randomNumeric(6);
        addressLabelSchl.setId(UUID.randomUUID());
        addressLabelSchl.setReportTypeCode(ADDRESS_LABEL_SCHL);
        addressLabelSchl.setSchoolOfRecord(addressLabelSchlMincode);
        addressLabelSchl.setSchoolOfRecordName(RandomStringUtils.randomAlphabetic(15));
        addressLabelSchl.setSchoolCategory(RandomStringUtils.randomNumeric(2));
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
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolReportsByReportType(), distrepYeNgSd.getReportTypeCode(), ""))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
        })).thenReturn(Mono.just(List.of(distrepYeNgSd)));

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

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndReport(), "", distrepYeSd.getReportTypeCode(), ""))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndNonGradReport(), "", distrepYeNgSd.getReportTypeCode(), ""))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndReport(), "", "", distrepYeSc.getReportTypeCode()))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

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
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolReport(), distrepYeNgSd.getSchoolOfRecord(), ""))).thenReturn(this.requestHeadersMock);
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

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndNonGradReport(), "", "", "DISTREP_YE_SC"))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

        var result = this.postingDistributionService.postingProcess(response);
        Assert.assertTrue(result);

        response.setActivityCode(NONGRADDIST);
        result = this.postingDistributionService.postingProcess(response);
        Assert.assertTrue(result);
    }

    @Test
    public void testCreateSchoolLabelsReport() {
        School district = new School();
        district.setMincode(RandomStringUtils.randomNumeric(3));
        district.setName(RandomStringUtils.randomAlphabetic(15));
        district.setSchoolCategoryCode(RandomStringUtils.randomNumeric(2));

        School school = new School();
        school.setMincode(RandomStringUtils.randomNumeric(6));
        school.setName(RandomStringUtils.randomAlphabetic(15));
        school.setSchoolCategoryCode(RandomStringUtils.randomNumeric(2));

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(educDistributionApiConstants.getSchoolLabelsReport(), DISTREP_YE_SD))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

        var result = this.postingDistributionService.createSchoolLabelsReport(List.of(district), DISTREP_YE_SD);
        Assert.assertTrue(1 == result);

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(educDistributionApiConstants.getSchoolLabelsReport(), DISTREP_YE_SC))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

        result = this.postingDistributionService.createSchoolLabelsReport(List.of(school), DISTREP_YE_SC);
        Assert.assertTrue(1 == result);

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(educDistributionApiConstants.getSchoolLabelsReport(), ADDRESS_LABEL_SCHL))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

        result = this.postingDistributionService.createSchoolLabelsReport(List.of(school), ADDRESS_LABEL_SCHL);
        Assert.assertTrue(1 == result);

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(educDistributionApiConstants.getSchoolLabelsReport(), ADDRESS_LABEL_YE))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

        result = this.postingDistributionService.createSchoolLabelsReport(List.of(district), ADDRESS_LABEL_YE);
        Assert.assertTrue(1 == result);
    }

    @Test
    public void testCreateDistrictLabelsReport() {
        TraxDistrict district = new TraxDistrict();
        district.setDistrictNumber(RandomStringUtils.randomNumeric(3));
        district.setDistrictName(RandomStringUtils.randomAlphabetic(15));

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(educDistributionApiConstants.getSchoolLabelsReport(), ADDRESS_LABEL_YE))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

        var result = this.postingDistributionService.createDistrictLabelsReport(List.of(district), ADDRESS_LABEL_YE);
        Assert.assertTrue(1 == result);
    }

    @Test
    public void testCreateDistrictSchoolMonthReport() {
        TraxDistrict district = new TraxDistrict();
        district.setDistrictNumber(RandomStringUtils.randomNumeric(3));
        district.setDistrictName(RandomStringUtils.randomAlphabetic(15));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictMonthReport(), ADDRESS_LABEL_SCHL, DISTREP_SD, DISTREP_SC))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

        var result = this.postingDistributionService.createDistrictSchoolMonthReport(ADDRESS_LABEL_SCHL, DISTREP_SD, DISTREP_SC);
        Assert.assertTrue(1 == result);
    }

    @Test
    public void testCreateDistrictSchoolSuppReport() {
        TraxDistrict district = new TraxDistrict();
        district.setDistrictNumber(RandomStringUtils.randomNumeric(3));
        district.setDistrictName(RandomStringUtils.randomAlphabetic(15));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictSupplementalReport(), ADDRESS_LABEL_SCHL, DISTREP_SD, DISTREP_SC))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

        var result = this.postingDistributionService.createDistrictSchoolSuppReport(ADDRESS_LABEL_SCHL, DISTREP_SD, DISTREP_SC);
        Assert.assertTrue(1 == result);
    }

    @Test
    public void testCreateDistrictSchoolYearEndReport() {
        TraxDistrict district = new TraxDistrict();
        district.setDistrictNumber(RandomStringUtils.randomNumeric(3));
        district.setDistrictName(RandomStringUtils.randomAlphabetic(15));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndReport(), ADDRESS_LABEL_YE, DISTREP_YE_SD, DISTREP_YE_SC))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

        var result = this.postingDistributionService.createDistrictSchoolYearEndReport(ADDRESS_LABEL_YE, DISTREP_YE_SD, DISTREP_YE_SC);
        Assert.assertTrue(1 == result);
    }

    @Test
    public void testCreateDistrictSchoolYearEndNonGradReport() {
        TraxDistrict district = new TraxDistrict();
        district.setDistrictNumber(RandomStringUtils.randomNumeric(3));
        district.setDistrictName(RandomStringUtils.randomAlphabetic(15));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndNonGradReport(), ADDRESS_LABEL_YE, DISTREP_YE_SD, DISTREP_YE_SC))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

        var result = this.postingDistributionService.createDistrictSchoolYearEndNonGradReport(ADDRESS_LABEL_YE, DISTREP_YE_SD, DISTREP_YE_SC);
        Assert.assertTrue(1 == result);
    }

    @Test
    public void testCreateDistrictSchoolYearEndReportFromListOfSchools() {
        School school = new School();
        school.setMincode(RandomStringUtils.randomNumeric(6));
        school.setName(RandomStringUtils.randomAlphabetic(15));
        school.setSchoolCategoryCode(RandomStringUtils.randomNumeric(2));

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndReport(), ADDRESS_LABEL_YE, DISTREP_YE_SD, DISTREP_YE_SC))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

        var result = this.postingDistributionService.createDistrictSchoolYearEndReport(ADDRESS_LABEL_YE, DISTREP_YE_SD, DISTREP_YE_SC, List.of(school.getMincode()));
        Assert.assertTrue(1 == result);
    }

    @Test
    public void testCreateDistrictSchoolYearEndNonGradReportFromListOfSchools() {
        School school = new School();
        school.setMincode(RandomStringUtils.randomNumeric(6));
        school.setName(RandomStringUtils.randomAlphabetic(15));
        school.setSchoolCategoryCode(RandomStringUtils.randomNumeric(2));

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndNonGradReport(), ADDRESS_LABEL_YE, DISTREP_YE_SD, DISTREP_YE_SC))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

        var result = this.postingDistributionService.createDistrictSchoolYearEndNonGradReport(ADDRESS_LABEL_YE, DISTREP_YE_SD, DISTREP_YE_SC, List.of(school.getMincode()));
        Assert.assertTrue(1 == result);
    }

    private synchronized byte[] readBinaryFile(String path) {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(path);
        try {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            return new byte[] {};
        }
    }
}
