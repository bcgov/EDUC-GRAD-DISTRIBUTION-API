package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.v2.reports.DistrictReport;
import ca.bc.gov.educ.api.distribution.model.dto.v2.reports.SchoolReport;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;
import java.util.UUID;

import static ca.bc.gov.educ.api.distribution.model.dto.ReportType.DISTREP_YE_SC;
import static ca.bc.gov.educ.api.distribution.model.dto.ReportType.NONGRADDISTREP_SD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PostingDistributionServiceTest {

  @Mock
  private RestService restService;

  @Mock
  private EducDistributionApiConstants educDistributionApiConstants;

  @InjectMocks
  private PostingDistributionService postingDistributionService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testProcessDistrictSchoolDistribution_withDistrictIds() {
    Long batchId = 1L;
    UUID districtId = UUID.randomUUID();
    List<String> districtIds = List.of(districtId.toString());
    String districtReportType =NONGRADDISTREP_SD.getValue();
    String transmissionMode = "transmissionMode";

    DistrictReport districtReport = new DistrictReport();
    districtReport.setDistrictId(districtId);
    districtReport.setReportTypeCode(districtReportType);

    when(restService.executeGet(educDistributionApiConstants.getDistrictReport(), new ParameterizedTypeReference<List<DistrictReport>>() {},  districtReportType, districtId.toString()))
        .thenReturn(List.of(districtReport));
    when(restService.executeGet(educDistributionApiConstants.getDistrictReportPDF(), byte[].class, districtReportType, districtId.toString()))
        .thenReturn(new byte[]{1, 2, 3});

    int result = postingDistributionService.processDistrictSchoolDistribution(batchId, null, districtIds, null, districtReportType, null, transmissionMode);

    assertEquals(1, result);
    verify(restService, times(1)).executeGet(educDistributionApiConstants.getDistrictReport(), new ParameterizedTypeReference<List<DistrictReport>>() {},  districtReportType, districtId.toString());
    verify(restService, times(1)).executeGet(educDistributionApiConstants.getDistrictReportPDF(), byte[].class, districtReportType, districtId.toString());
  }

  @Test
  void testProcessDistrictSchoolDistribution_withSchoolIds() {
    Long batchId = 1L;
    UUID schoolId = UUID.randomUUID();
    List<String> schoolIds = List.of(schoolId.toString());
    String reportType =DISTREP_YE_SC.getValue();
    String transmissionMode = "transmissionMode";

    SchoolReport schoolReport = new SchoolReport();
    schoolReport.setSchoolOfRecordId(schoolId);
    schoolReport.setReportTypeCode(reportType);

    when(restService.executeGet(educDistributionApiConstants.getSchoolReport(), new ParameterizedTypeReference<List<SchoolReport>>() {},  reportType, schoolId.toString()))
        .thenReturn(List.of(schoolReport));
    when(restService.executeGet(educDistributionApiConstants.getSchoolReportPDF(), byte[].class, reportType, schoolId.toString()))
        .thenReturn(new byte[]{1, 2, 3});

    int result = postingDistributionService.processDistrictSchoolDistribution(batchId, schoolIds, null, null, null, reportType, transmissionMode);

    assertEquals(1, result);
    verify(restService, times(1)).executeGet(educDistributionApiConstants.getSchoolReport(), new ParameterizedTypeReference<List<SchoolReport>>() {},  reportType, schoolId.toString());
    verify(restService, times(1)).executeGet(educDistributionApiConstants.getSchoolReportPDF(), byte[].class, reportType, schoolId.toString());
  }
}