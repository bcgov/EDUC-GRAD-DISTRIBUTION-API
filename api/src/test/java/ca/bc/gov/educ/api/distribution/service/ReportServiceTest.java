package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.Address;
import ca.bc.gov.educ.api.distribution.model.dto.CommonSchool;
import ca.bc.gov.educ.api.distribution.model.dto.StudentSearchRequest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class ReportServiceTest {

    @Autowired
    private ReportService reportService;

    @Test
    public void testPreparePackingSlipData() {
        String mincode = "123456";
        CommonSchool commonSchool = new CommonSchool();
        commonSchool.setSchlNo(mincode);
        commonSchool.setSchoolName("Test School");

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
