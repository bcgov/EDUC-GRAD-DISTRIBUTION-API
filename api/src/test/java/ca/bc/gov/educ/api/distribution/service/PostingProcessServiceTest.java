package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.DistributionResponse;
import ca.bc.gov.educ.api.distribution.model.dto.School;
import ca.bc.gov.educ.api.distribution.model.dto.v2.District;
import ca.bc.gov.educ.api.distribution.model.dto.v2.YearEndReportRequest;
import ca.bc.gov.educ.api.distribution.model.dto.v2.reports.DistrictReport;
import ca.bc.gov.educ.api.distribution.model.dto.v2.reports.SchoolReport;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.SFTPUtils;
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

import static ca.bc.gov.educ.api.distribution.model.dto.ActivityCode.*;
import static ca.bc.gov.educ.api.distribution.model.dto.ReportType.*;
import static ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants.*;
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
    @MockBean
    SFTPUtils stpUtils;


    @Test
    public void testPostingProcess() {
        DistributionResponse response = new DistributionResponse();
        response.setBatchId(12345L);
        response.setActivityCode(YEARENDDIST.getValue());
        response.setLocalDownload("N");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndReport(), "", DISTREP_YE_SD.getValue(), ""))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(String.class)).thenReturn(Mono.just("1"));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndReport(), "", "", DISTREP_YE_SC.getValue()))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(String.class)).thenReturn(Mono.just("1"));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndReport(), "", NONGRADDISTREP_SD.getValue(), ""))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(String.class)).thenReturn(Mono.just("1"));

        DistrictReport distrepYeSd = new DistrictReport();
        distrepYeSd.setId(UUID.randomUUID());
        distrepYeSd.setReportTypeCode(DISTREP_YE_SD.getValue());
        distrepYeSd.setDistrictName(RandomStringUtils.randomAlphabetic(15));
        distrepYeSd.setReport(Base64.getEncoder().encodeToString(RandomStringUtils.randomAlphanumeric(25).getBytes()));

        DistrictReport distrepYeNgSd = new DistrictReport();
        distrepYeNgSd.setId(UUID.randomUUID());
        distrepYeNgSd.setReportTypeCode(NONGRADDISTREP_SD.getValue());
        distrepYeNgSd.setDistrictName(RandomStringUtils.randomAlphabetic(15));
        distrepYeNgSd.setReport(Base64.getEncoder().encodeToString(RandomStringUtils.randomAlphanumeric(25).getBytes()));

        SchoolReport distrepYeSc = new SchoolReport();
        distrepYeSc.setId(UUID.randomUUID());
        distrepYeSc.setReportTypeCode(DISTREP_YE_SC.getValue());
        distrepYeSc.setSchoolName(RandomStringUtils.randomAlphabetic(15));
        distrepYeSc.setSchoolCategory(RandomStringUtils.randomNumeric(2));
        distrepYeSc.setReport(Base64.getEncoder().encodeToString(RandomStringUtils.randomAlphanumeric(25).getBytes()));

        District district = new District();
        district.setDistrictId(UUID.randomUUID().toString());
        district.setDisplayName(distrepYeSd.getDistrictName());

        response.getDistricts().add(district);

        District districtNg = new District();
        districtNg.setDistrictId(UUID.randomUUID().toString());
        districtNg.setDisplayName(distrepYeNgSd.getDistrictName());

        response.getDistricts().add(districtNg);

        School school = new School();
        school.setSchoolId(UUID.randomUUID().toString());
        school.setName(distrepYeSc.getSchoolName());
        school.setSchoolCategoryCode(distrepYeSc.getSchoolCategory());

        response.getSchools().add(school);

        SchoolReport addressLabelSchl = new SchoolReport();
        addressLabelSchl.setId(UUID.randomUUID());
        addressLabelSchl.setReportTypeCode(ADDRESS_LABEL_SCHL);
        addressLabelSchl.setSchoolOfRecordId(UUID.randomUUID());
        addressLabelSchl.setSchoolName(RandomStringUtils.randomAlphabetic(15));
        addressLabelSchl.setSchoolCategory(RandomStringUtils.randomNumeric(2));
        addressLabelSchl.setReport(Base64.getEncoder().encodeToString(RandomStringUtils.randomAlphanumeric(25).getBytes()));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolReport(), addressLabelSchl.getReportTypeCode(), ""))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReport>>() {
        })).thenReturn(Mono.just(List.of(addressLabelSchl)));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolReport(), addressLabelSchl.getReportTypeCode(), addressLabelSchl.getSchoolOfRecordId()))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReport>>() {
        })).thenReturn(Mono.just(List.of(addressLabelSchl)));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getDistrictReport(), distrepYeSd.getReportTypeCode(), ""))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<DistrictReport>>() {
        })).thenReturn(Mono.just(List.of(distrepYeSd)));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getLightDistrictReport(), distrepYeNgSd.getReportTypeCode(), ""))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<DistrictReport>>() {
        })).thenReturn(Mono.just(List.of(distrepYeNgSd)));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getDistrictReport(), distrepYeSd.getReportTypeCode(), distrepYeSd.getDistrictId()))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<DistrictReport>>() {
        })).thenReturn(Mono.just(List.of(distrepYeSd)));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolReport(), distrepYeSc.getReportTypeCode(), ""))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReport>>() {
        })).thenReturn(Mono.just(List.of(distrepYeSc)));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolReport(), distrepYeSc.getReportTypeCode(), distrepYeSc.getSchoolOfRecordId()))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<SchoolReport>>() {
        })).thenReturn(Mono.just(List.of(distrepYeSc)));

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndReport(), "", distrepYeSd.getReportTypeCode(), ""))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(String.class)).thenReturn(Mono.just("1"));

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndNonGradReport(), "", distrepYeNgSd.getReportTypeCode(), ""))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(String.class)).thenReturn(Mono.just("1"));

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndReport(), "", "", distrepYeSc.getReportTypeCode()))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(String.class)).thenReturn(Mono.just("1"));

        byte[] bytesPdf = readBinaryFile("data/sample.pdf");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolReport(), distrepYeSc.getSchoolOfRecordId(), distrepYeSc.getReportTypeCode()))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(bytesPdf));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getDistrictReport(), distrepYeSd.getDistrictId(), distrepYeSd.getReportTypeCode()))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(bytesPdf));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getLightDistrictReport(), distrepYeNgSd.getDistrictId(), ""))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(bytesPdf));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolReport(), addressLabelSchl.getSchoolOfRecordId(), addressLabelSchl.getReportTypeCode()))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(bytesPdf));

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndNonGradReport(), "", "NONGRADDISTREP_SD", ""))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(String.class)).thenReturn(Mono.just("1"));

        when(this.stpUtils.sftpUploadBCMail(any(), any(), any())).thenReturn(true);

        var result = this.postingDistributionService.postingProcess(response);
        Assert.assertFalse(result);

        response.setActivityCode(NONGRADYERUN.getValue());
        result = this.postingDistributionService.postingProcess(response);
        Assert.assertFalse(result);
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
        when(this.requestBodyUriMock.uri(String.format(educDistributionApiConstants.getSchoolLabelsReport(), DISTREP_YE_SD.getValue()))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(String.class)).thenReturn(Mono.just("1"));

        var result = this.postingDistributionService.createSchoolLabelsReport(List.of(district), DISTREP_YE_SD.getValue());
        Assert.assertTrue(1 == result);

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(educDistributionApiConstants.getSchoolLabelsReport(), DISTREP_YE_SC.getValue()))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(String.class)).thenReturn(Mono.just("1"));

        result = this.postingDistributionService.createSchoolLabelsReport(List.of(school), DISTREP_YE_SC.getValue());
        Assert.assertTrue(1 == result);

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(educDistributionApiConstants.getSchoolLabelsReport(), ADDRESS_LABEL_SCHL))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(String.class)).thenReturn(Mono.just("1"));

        result = this.postingDistributionService.createSchoolLabelsReport(List.of(school), ADDRESS_LABEL_SCHL);
        Assert.assertTrue(1 == result);

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(educDistributionApiConstants.getSchoolLabelsReport(), ADDRESS_LABEL_YE))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(String.class)).thenReturn(Mono.just("1"));

        result = this.postingDistributionService.createSchoolLabelsReport(List.of(district), ADDRESS_LABEL_YE);
        Assert.assertTrue(1 == result);
    }

    @Test
    public void testCreateDistrictLabelsReport() {
        District district = new District();
        district.setDistrictNumber(RandomStringUtils.randomNumeric(3));
        district.setDisplayName(RandomStringUtils.randomAlphabetic(15));

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(educDistributionApiConstants.getDistrictLabelsReport(), ADDRESS_LABEL_YE))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(String.class)).thenReturn(Mono.just("1"));

        var result = this.postingDistributionService.createDistrictLabelsReport(List.of(district), ADDRESS_LABEL_YE);
        Assert.assertTrue(1 == result);
    }

    @Test
    public void testCreateDistrictSchoolLabelsReport() {
        UUID districtId = UUID.randomUUID();

        School school = new School();
        school.setMincode(RandomStringUtils.randomNumeric(6));
        school.setName(RandomStringUtils.randomAlphabetic(15));
        school.setSchoolCategoryCode(RandomStringUtils.randomNumeric(2));
        school.setSchoolId(UUID.randomUUID().toString());

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(educDistributionApiConstants.getDistrictSchoolLabelsReport(), districtId, ADDRESS_LABEL_SCH_YE))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(String.class)).thenReturn(Mono.just("1"));
        var result = this.postingDistributionService.createDistrictLabelsReport(districtId.toString(), List.of(school), ADDRESS_LABEL_SCH_YE);
        Assert.assertTrue(1 == result);
    }

    @Test
    public void testCreateDistrictSchoolMonthReport() {
        District district = new District();
        district.setDistrictNumber(RandomStringUtils.randomNumeric(3));
        district.setDisplayName(RandomStringUtils.randomAlphabetic(15));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictMonthReport(), ADDRESS_LABEL_SCHL, DISTREP_SD.getValue(), DISTREP_SC.getValue()))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(String.class)).thenReturn(Mono.just("1"));

        var result = this.postingDistributionService.createDistrictSchoolMonthReport(ADDRESS_LABEL_SCHL, DISTREP_SD.getValue(), DISTREP_SC.getValue());
        Assert.assertTrue(1 == result);
    }

    @Test
    public void testCreateDistrictSchoolSuppReport() {
        District district = new District();
        district.setDistrictNumber(RandomStringUtils.randomNumeric(3));
        district.setDisplayName(RandomStringUtils.randomAlphabetic(15));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictSupplementalReport(), ADDRESS_LABEL_SCHL, DISTREP_SD.getValue(), DISTREP_SC.getValue()))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(String.class)).thenReturn(Mono.just("1"));

        var result = this.postingDistributionService.createDistrictSchoolSuppReport(ADDRESS_LABEL_SCHL, DISTREP_SD.getValue(), DISTREP_SC.getValue());
        Assert.assertTrue(1 == result);
    }

    @Test
    public void testCreateDistrictSchoolYearEndReport() {
        District district = new District();
        district.setDistrictNumber(RandomStringUtils.randomNumeric(3));
        district.setDisplayName(RandomStringUtils.randomAlphabetic(15));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndReport(), ADDRESS_LABEL_YE, DISTREP_YE_SD.getValue(), DISTREP_YE_SC.getValue()))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(String.class)).thenReturn(Mono.just("1"));

        var result = this.postingDistributionService.createDistrictSchoolYearEndReport(ADDRESS_LABEL_YE, DISTREP_YE_SD.getValue(), DISTREP_YE_SC.getValue());
        Assert.assertTrue(1 == result);
    }

    @Test
    public void testCreateDistrictSchoolYearEndNonGradReport() {
        District district = new District();
        district.setDistrictNumber(RandomStringUtils.randomNumeric(3));
        district.setDisplayName(RandomStringUtils.randomAlphabetic(15));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndNonGradReport(), ADDRESS_LABEL_YE, DISTREP_YE_SD.getValue(), DISTREP_YE_SC.getValue()))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(String.class)).thenReturn(Mono.just("1"));

        var result = this.postingDistributionService.createDistrictSchoolYearEndNonGradReport(ADDRESS_LABEL_YE, DISTREP_YE_SD.getValue(), DISTREP_YE_SC.getValue());
        Assert.assertTrue(1 == result);
    }

    @Test
    public void testCreateDistrictSchoolYearEndReportFromListOfSchools() {
        School school = new School();
        school.setMincode(RandomStringUtils.randomNumeric(6));
        school.setName(RandomStringUtils.randomAlphabetic(15));
        school.setSchoolCategoryCode(RandomStringUtils.randomNumeric(2));
        school.setSchoolId(UUID.randomUUID().toString());

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndReport(), ADDRESS_LABEL_YE, DISTREP_YE_SD.getValue(), DISTREP_YE_SC.getValue()))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(String.class)).thenReturn(Mono.just("1"));

        YearEndReportRequest yearEndReportRequest = new YearEndReportRequest();
        yearEndReportRequest.setSchoolIds(List.of(UUID.fromString(school.getSchoolId())));
        var result = this.postingDistributionService.createDistrictSchoolYearEndReport(ADDRESS_LABEL_YE, DISTREP_YE_SD.getValue(), DISTREP_YE_SC.getValue(), yearEndReportRequest);
        Assert.assertTrue(1 == result);
    }

    @Test
    public void testCreateDistrictSchoolYearEndNonGradReportFromListOfSchools() {
        School school = new School();
        school.setMincode(RandomStringUtils.randomNumeric(6));
        school.setName(RandomStringUtils.randomAlphabetic(15));
        school.setSchoolCategoryCode(RandomStringUtils.randomNumeric(2));

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndNonGradReport(), ADDRESS_LABEL_YE, DISTREP_YE_SD.getValue(), DISTREP_YE_SC.getValue()))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(String.class)).thenReturn(Mono.just("1"));

        var result = this.postingDistributionService.createDistrictSchoolYearEndNonGradReport(ADDRESS_LABEL_YE, DISTREP_YE_SD.getValue(), DISTREP_YE_SC.getValue(), List.of(school.getMincode()));
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
