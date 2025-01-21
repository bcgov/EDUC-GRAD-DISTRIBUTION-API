package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.model.dto.v2.District;
import ca.bc.gov.educ.api.distribution.model.dto.v2.School;
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

    @Test
    public void testGetCommonSchoolDetails() {
        String mincode = "123456";
        ca.bc.gov.educ.api.distribution.model.dto.v2.School commonSchool = new ca.bc.gov.educ.api.distribution.model.dto.v2.School();
        commonSchool.setMinCode(mincode);
        commonSchool.setSchoolName("Test School");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolByMincode(), mincode))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(ca.bc.gov.educ.api.distribution.model.dto.v2.School.class)).thenReturn(Mono.just(commonSchool));

        var response = this.schoolService.getSchool(mincode, new ExceptionMessage());
        Assert.assertEquals(mincode, response.getMinCode());
    }

    @Test
    public void testGetDefaultSchoolDetails() {
        String mincode = "123456";
        ca.bc.gov.educ.api.distribution.model.dto.v2.School commonSchool = new ca.bc.gov.educ.api.distribution.model.dto.v2.School();
        commonSchool.setMinCode(mincode);
        commonSchool.setSchoolName("Test School");
        commonSchool.setCity("VANCOUVER");

        StudentSearchRequest searchRequest = new StudentSearchRequest();
        searchRequest.setUser("Test User");
        Address address = new Address();
        address.setCity("VANCOUVER");
        searchRequest.setAddress(address);

        var response = this.schoolService.getDefaultSchoolDetailsForPackingSlip(searchRequest, "properName");
        Assert.assertNotNull(response);
        Assert.assertEquals("VANCOUVER", response.getCity());

        searchRequest.setUser(null);
        searchRequest.setAddress(null);

        response = this.schoolService.getDefaultSchoolDetailsForPackingSlip(searchRequest, "properName");
        Assert.assertNotNull(response);
        Assert.assertEquals("VICTORIA", response.getCity());
    }

    @Test
    public void testGetCommonSchoolDetails_Exception() {
        String mincode = "123456";
        ca.bc.gov.educ.api.distribution.model.dto.v2.School commonSchool = new School();
        commonSchool.setMinCode(mincode);
        commonSchool.setSchoolName("Test School");

        ExceptionMessage exceptionMessage = new ExceptionMessage();

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolByMincode(), mincode))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(ca.bc.gov.educ.api.distribution.model.dto.v2.School.class)).thenReturn(Mono.just(commonSchool));

        var response = this.schoolService.getSchool("234567", exceptionMessage);
        Assert.assertNull(response);
        Assert.assertEquals(EducDistributionApiConstants.TRAX_API_STATUS, exceptionMessage.getExceptionName());
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
        ca.bc.gov.educ.api.distribution.model.dto.v2.School school = new ca.bc.gov.educ.api.distribution.model.dto.v2.School();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolByMincode(), mincode))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(ca.bc.gov.educ.api.distribution.model.dto.v2.School.class)).thenReturn(Mono.just(school));

        var response = this.schoolService.getSchool(mincode, new ExceptionMessage());
        Assert.assertEquals(mincode, response.getMinCode());
    }

    @Test
    public void testGetTraxSchool_Exception() {
        String mincode = "123456";
        ca.bc.gov.educ.api.distribution.model.dto.v2.School school = new ca.bc.gov.educ.api.distribution.model.dto.v2.School();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        ExceptionMessage exceptionMessage = new ExceptionMessage();

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolByMincode(), mincode))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(ca.bc.gov.educ.api.distribution.model.dto.v2.School.class)).thenReturn(Mono.just(school));

        var response = this.schoolService.getSchool("234567", exceptionMessage);
        Assert.assertNull(response);
        Assert.assertEquals("TRAX-API IS DOWN", exceptionMessage.getExceptionName());
    }

    @Test
    public void testGetTraxDistrict() {
        String mincode = "123";
        District district = new District();
        district.setDistrictNumber(mincode);
        district.setDistrictName("Test District");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getDistrictByDistcode(), mincode))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(District.class)).thenReturn(Mono.just(district));

        var response = this.schoolService.getDistrict(mincode, new ExceptionMessage());
        Assert.assertEquals(mincode, response.getDistrictNumber());
    }

    @Test
    public void testGetTraxDistrict_Exception() {
        String mincode = "123";
        District district = new District();
        district.setDistrictNumber(mincode);
        district.setDistrictName("Test District");

        ExceptionMessage exceptionMessage = new ExceptionMessage();

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getDistrictByDistcode(), mincode))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(District.class)).thenReturn(Mono.just(district));

        var response = this.schoolService.getDistrict("234", exceptionMessage);
        Assert.assertNull(response);
        Assert.assertEquals("TRAX-API IS DOWN", exceptionMessage.getExceptionName());
    }

}
