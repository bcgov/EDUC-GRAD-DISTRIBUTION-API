package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.Address;
import ca.bc.gov.educ.api.distribution.model.dto.StudentSearchRequest;
import ca.bc.gov.educ.api.distribution.model.dto.v2.School;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

@SpringBootTest
@RunWith(SpringRunner.class)
public class ReportServiceTest {

    @Autowired
    private ReportService reportService;

    @Test
    public void testPreparePackingSlipData() {
        String mincode = "123456";
        School commonSchool = new School();
        UUID schoolId = UUID.randomUUID();
        commonSchool.setMinCode(mincode);
        commonSchool.setSchoolName("Test School");
        commonSchool.setSchoolId(schoolId.toString());

        StudentSearchRequest searchRequest = new StudentSearchRequest();
        searchRequest.setUser("Test User");
        searchRequest.setAddress(new Address());

        var response = this.reportService.preparePackingSlipData(searchRequest, commonSchool, 0L);
        Assert.assertNotNull(response);

        searchRequest.setUser(null);
        searchRequest.setAddress(null);

        response = this.reportService.preparePackingSlipData(searchRequest, commonSchool, 0L);
        Assert.assertNotNull(response);
    }


}
