package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
public class SchoolServiceTest {

    @Autowired
    private RestService restService;

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private EducDistributionApiConstants educDistributionApiConstants;

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

    private static final byte[] TEST_BYTES = "The rain in Spain stays mainly on the plain.".getBytes();

    @Test
    public void testGetCommonSchoolDetails() {
        String mincode = "123456";
        CommonSchool commonSchool = new CommonSchool();
        commonSchool.setSchlNo(mincode);
        commonSchool.setSchoolName("Test School");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getCommonSchoolByMincode(), mincode))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(CommonSchool.class)).thenReturn(Mono.just(commonSchool));

        var response = this.schoolService.getCommonSchoolDetails(mincode, new ExceptionMessage());
        Assert.assertEquals(mincode, response.getSchlNo());
    }

    @Test
    public void testGetDefaultSchoolDetails() {
        String mincode = "123456";
        CommonSchool commonSchool = new CommonSchool();
        commonSchool.setSchlNo(mincode);
        commonSchool.setSchoolName("Test School");
        commonSchool.setScCity("VANCOUVER");

        StudentSearchRequest searchRequest = new StudentSearchRequest();
        searchRequest.setUser("Test User");
        Address address = new Address();
        address.setCity("VANCOUVER");
        searchRequest.setAddress(address);

        var response = this.schoolService.getDefaultSchoolDetailsForPackingSlip(searchRequest, "properName");
        Assert.assertNotNull(response);
        Assert.assertEquals("VANCOUVER", response.getScCity());

        searchRequest.setUser(null);
        searchRequest.setAddress(null);

        response = this.schoolService.getDefaultSchoolDetailsForPackingSlip(searchRequest, "properName");
        Assert.assertNotNull(response);
        Assert.assertEquals("VICTORIA", response.getScCity());
    }

    @Test
    public void testGetCommonSchoolDetails_Exception() {
        String mincode = "123456";
        CommonSchool commonSchool = new CommonSchool();
        commonSchool.setSchlNo(mincode);
        commonSchool.setSchoolName("Test School");

        ExceptionMessage exceptionMessage = new ExceptionMessage();

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getCommonSchoolByMincode(), mincode))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(CommonSchool.class)).thenReturn(Mono.just(commonSchool));

        var response = this.schoolService.getCommonSchoolDetails("234567", exceptionMessage);
        Assert.assertNull(response);
        Assert.assertEquals("SCHOOL-API IS DOWN", exceptionMessage.getExceptionName());
    }

    @Test
    public void testGetCommonSchoolDetailsForPackingSlip() {
        String properName = "properName";
        var response = this.schoolService.getDefaultSchoolDetailsForPackingSlip(new StudentSearchRequest(), properName);
        Assert.assertNotNull(response);
    }

    @Test
    public void testGetTraxSchool() {
        String mincode = "123456";
        TraxSchool traxSchool = new TraxSchool();
        traxSchool.setMinCode(mincode);
        traxSchool.setSchoolName("Test School");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getTraxSchoolByMincode(), mincode))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(TraxSchool.class)).thenReturn(Mono.just(traxSchool));

        var response = this.schoolService.getTraxSchool(mincode, new ExceptionMessage());
        Assert.assertEquals(mincode, response.getMinCode());
    }

    @Test
    public void testGetTraxSchool_Exception() {
        String mincode = "123456";
        TraxSchool traxSchool = new TraxSchool();
        traxSchool.setMinCode(mincode);
        traxSchool.setSchoolName("Test School");

        ExceptionMessage exceptionMessage = new ExceptionMessage();

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getTraxSchoolByMincode(), mincode))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(TraxSchool.class)).thenReturn(Mono.just(traxSchool));

        var response = this.schoolService.getTraxSchool("234567", exceptionMessage);
        Assert.assertNull(response);
        Assert.assertEquals("TRAX-API IS DOWN", exceptionMessage.getExceptionName());
    }

    @Test
    public void testGetTraxDistrict() {
        String mincode = "123";
        TraxDistrict traxDistrict = new TraxDistrict();
        traxDistrict.setDistrictNumber(mincode);
        traxDistrict.setDistrictName("Test District");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getTraxDistrictByDistcode(), mincode))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(TraxDistrict.class)).thenReturn(Mono.just(traxDistrict));

        var response = this.schoolService.getTraxDistrict(mincode, new ExceptionMessage());
        Assert.assertEquals(mincode, response.getDistrictNumber());
    }

    @Test
    public void testGetTraxDistrict_Exception() {
        String mincode = "123";
        TraxDistrict traxDistrict = new TraxDistrict();
        traxDistrict.setDistrictNumber(mincode);
        traxDistrict.setDistrictName("Test District");

        ExceptionMessage exceptionMessage = new ExceptionMessage();

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getTraxDistrictByDistcode(), mincode))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(TraxDistrict.class)).thenReturn(Mono.just(traxDistrict));

        var response = this.schoolService.getTraxDistrict("234", exceptionMessage);
        Assert.assertNull(response);
        Assert.assertEquals("TRAX-API IS DOWN", exceptionMessage.getExceptionName());
    }

}
