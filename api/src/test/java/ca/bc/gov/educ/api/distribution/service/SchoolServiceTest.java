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

import java.util.UUID;

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
    @MockBean
    RestService restServiceMock;

    @Test
    public void testGetCommonSchoolDetails() {
        String mincode = "123456";
        UUID schoolId = UUID.randomUUID();
        School commonSchool = new School();
        commonSchool.setMinCode(mincode);
        commonSchool.setSchoolName("Test School");
        commonSchool.setSchoolId(schoolId.toString());

        when(restService.executeGet(
                educDistributionApiConstants.getSchoolById(),
                School.class,
                schoolId.toString()
        )).thenReturn(commonSchool);

        var response = this.schoolService.getSchool(schoolId, new ExceptionMessage());
        Assert.assertEquals(schoolId.toString(), response.getSchoolId());
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
        UUID schoolId = UUID.randomUUID();
        ca.bc.gov.educ.api.distribution.model.dto.v2.School commonSchool = new School();
        commonSchool.setMinCode(mincode);
        commonSchool.setSchoolName("Test School");
        commonSchool.setSchoolId(schoolId.toString());

        ExceptionMessage exceptionMessage = new ExceptionMessage();
        when(restService.executeGet(
                educDistributionApiConstants.getSchoolById(),
                School.class,
                schoolId.toString()
        )).thenThrow(NullPointerException.class);
        var response = this.schoolService.getSchool(schoolId, exceptionMessage);
        Assert.assertNull(response);
        Assert.assertEquals(String.format("TRAX-API IS DOWN: %s", educDistributionApiConstants.getSchoolById()),
                exceptionMessage.getExceptionName());
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
        UUID schoolId = UUID.randomUUID();
        ca.bc.gov.educ.api.distribution.model.dto.v2.School school = new ca.bc.gov.educ.api.distribution.model.dto.v2.School();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");
        school.setSchoolId(schoolId.toString());

        when(this.restService.executeGet(educDistributionApiConstants.getSchoolById(),
                School.class,
                schoolId.toString())).thenReturn(school);

        var response = this.schoolService.getSchool(schoolId, new ExceptionMessage());
        Assert.assertEquals(mincode, response.getMinCode());
    }

    @Test(expected = Exception.class)
    public void testGetTraxSchool_Exception() {
        String mincode = "123456";
        UUID schoolId = UUID.randomUUID();
        ca.bc.gov.educ.api.distribution.model.dto.v2.School school = new ca.bc.gov.educ.api.distribution.model.dto.v2.School();
        school.setMinCode(mincode);
        school.setSchoolName("Test School");

        when(restService.executeGet(educDistributionApiConstants.getSchoolById(), District.class,
                schoolId.toString())).thenThrow(Exception.class);

        var response = this.schoolService.getSchool(schoolId, new ExceptionMessage());
        Assert.assertNull(response);
    }

    @Test
    public void testGetTraxDistrict() {
        String districtNumber = "022";
        UUID districtId = UUID.randomUUID();
        District district = new District();
        district.setDistrictNumber(districtNumber);
        district.setDisplayName("Test District");

        when(restService.executeGet(educDistributionApiConstants.getDistrictById(), District.class,
                districtId.toString())).thenReturn(district);
        var response = this.schoolService.getDistrict(districtId, new ExceptionMessage());
        Assert.assertEquals(districtNumber, response.getDistrictNumber());
    }

    @Test(expected = Exception.class)
    public void testGetTraxDistrict_Exception() {
        String districtNumber = "022";
        UUID districtId = UUID.randomUUID();
        District district = new District();
        district.setDistrictNumber(districtNumber);
        district.setDisplayName("Test District");

        ExceptionMessage exceptionMessage = new ExceptionMessage();
        when(restService.executeGet(educDistributionApiConstants.getDistrictById(), District.class,
                districtNumber)).thenThrow(Exception.class);

        var response = this.schoolService.getDistrict(districtId, exceptionMessage);
        Assert.assertNull(response);
    }

    @Test
    public void testGetDistrictByDistrictNumber() {
        String districtNumber = "022";
        District district = new District();
        district.setDistrictNumber(districtNumber);
        district.setDisplayName("Test District");

        when(restService.executeGet(educDistributionApiConstants.getDistrictByDistrictNumber(), District.class,
                districtNumber)).thenReturn(district);
        var response = this.schoolService.getDistrictByDistrictNumber(districtNumber, new ExceptionMessage());
        Assert.assertEquals(districtNumber, response.getDistrictNumber());
    }

    @Test(expected = Exception.class)
    public void testGetDistrictByDistrictNumber_Exception() {
        String districtNumber = "022";
        UUID districtId = UUID.randomUUID();
        District district = new District();
        district.setDistrictNumber(districtNumber);
        district.setDisplayName("Test District");

        when(restService.executeGet(educDistributionApiConstants.getDistrictByDistrictNumber(), District.class,
                districtId.toString())).thenThrow(Exception.class);

        var response = this.schoolService.getDistrictByDistrictNumber(districtNumber, new ExceptionMessage());
        Assert.assertNull(response);
    }

}
