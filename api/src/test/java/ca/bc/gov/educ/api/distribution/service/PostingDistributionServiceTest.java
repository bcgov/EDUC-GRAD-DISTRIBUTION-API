package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.v2.District;
import ca.bc.gov.educ.api.distribution.model.dto.v2.School;
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
import static ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants.ADDRESS_LABEL_SCHL;
import static ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants.ADDRESS_LABEL_YE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PostingDistributionServiceTest {

  @Mock
  private RestService restService;
  @Mock
  private SchoolService schoolService;
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
    District district = District.builder().districtId(districtId.toString()).districtNumber("005").build();

    DistrictReport districtReport = new DistrictReport();
    districtReport.setDistrictId(districtId);
    districtReport.setReportTypeCode(districtReportType);

    when(restService.executeGet(educDistributionApiConstants.getLightDistrictReport(), new ParameterizedTypeReference<List<DistrictReport>>() {},  districtReportType, districtId.toString()))
        .thenReturn(List.of(districtReport));
    when(restService.executeGet(educDistributionApiConstants.getDistrictReportPDF(), byte[].class, districtReportType, districtId.toString()))
        .thenReturn(new byte[]{1, 2, 3});
    when(schoolService.getDistrict(eq(districtId), any())).thenReturn(district);

    int result = postingDistributionService.processDistrictSchoolDistribution(batchId, null, districtIds, null, districtReportType, null, transmissionMode);

    assertEquals(1, result);
    verify(restService, times(1)).executeGet(educDistributionApiConstants.getLightDistrictReport(), new ParameterizedTypeReference<List<DistrictReport>>() {},  districtReportType, districtId.toString());
    verify(restService, times(1)).executeGet(educDistributionApiConstants.getDistrictReportPDF(), byte[].class, districtReportType, districtId.toString());
    verify(schoolService, times(1)).getDistrict(eq(districtId), any());
  }

  @Test
  void testProcessDistrictSchoolDistribution_withDistrictLabel() {
    Long batchId = 1L;
    UUID districtId = UUID.randomUUID();
    List<String> districtIds = List.of(districtId.toString());
    String districtReportType =ADDRESS_LABEL_YE;
    String transmissionMode = "transmissionMode";

    DistrictReport districtReport = new DistrictReport();
    districtReport.setDistrictId(districtId);
    districtReport.setReportTypeCode(districtReportType);

    when(restService.executeGet(educDistributionApiConstants.getLightDistrictReport(), new ParameterizedTypeReference<List<DistrictReport>>() {},  districtReportType, districtId.toString()))
        .thenReturn(List.of(districtReport));
    when(restService.executeGet(educDistributionApiConstants.getDistrictReportPDF(), byte[].class, districtReportType, districtId.toString()))
        .thenReturn(null);

    int result = postingDistributionService.processDistrictSchoolDistribution(batchId, null, districtIds, districtReportType, null, null, transmissionMode);

    assertEquals(0, result);
    verify(restService, times(1)).executeGet(educDistributionApiConstants.getLightDistrictReport(), new ParameterizedTypeReference<List<DistrictReport>>() {},  districtReportType, districtId.toString());
    verify(restService, times(1)).executeGet(educDistributionApiConstants.getDistrictReportPDF(), byte[].class, districtReportType, districtId.toString());
  }

  @Test
  void testProcessDistrictSchoolDistribution_withSchoolLabelAndNoIds() {
    Long batchId = 1L;
    UUID schoolId = UUID.randomUUID();
    String reportType =ADDRESS_LABEL_SCHL;
    String transmissionMode = "transmissionMode";
    School school = new School();
    school.setSchoolId(schoolId.toString());
    school.setMinCode("12345678");

    SchoolReport schoolReport = new SchoolReport();
    schoolReport.setSchoolOfRecordId(schoolId);
    schoolReport.setReportTypeCode(reportType);

    when(restService.executeGet(educDistributionApiConstants.getLightSchoolReport(), new ParameterizedTypeReference<List<SchoolReport>>() {},  reportType, ""))
        .thenReturn(List.of(schoolReport));
    when(restService.executeGet(educDistributionApiConstants.getSchoolReportPDF(), byte[].class, reportType, schoolId.toString()))
        .thenReturn(new byte[]{1, 2, 3});
    when(schoolService.getSchool(eq(schoolId), any())).thenReturn(school);

    int result = postingDistributionService.processDistrictSchoolDistribution(batchId, null, null, reportType, null, null, transmissionMode);

    assertEquals(1, result);
    verify(restService, times(1)).executeGet(educDistributionApiConstants.getLightSchoolReport(), new ParameterizedTypeReference<List<SchoolReport>>() {},  reportType, "");
    verify(restService, times(1)).executeGet(educDistributionApiConstants.getSchoolReportPDF(), byte[].class, reportType, schoolId.toString());
    verify(schoolService, times(1)).getSchool(eq(schoolId), any());
  }

  @Test
  void testProcessDistrictSchoolDistribution_withSchoolIds() {
    Long batchId = 1L;
    UUID schoolId = UUID.randomUUID();
    List<String> schoolIds = List.of(schoolId.toString());
    String reportType =DISTREP_YE_SC.getValue();
    String transmissionMode = "transmissionMode";
    School school = new School();
    school.setSchoolId(schoolId.toString());
    school.setMinCode("12345678");

    SchoolReport schoolReport = new SchoolReport();
    schoolReport.setSchoolOfRecordId(schoolId);
    schoolReport.setReportTypeCode(reportType);

    when(restService.executeGet(educDistributionApiConstants.getLightSchoolReport(), new ParameterizedTypeReference<List<SchoolReport>>() {},  reportType, schoolId.toString()))
        .thenReturn(List.of(schoolReport));
    when(restService.executeGet(educDistributionApiConstants.getSchoolReportPDF(), byte[].class, reportType, schoolId.toString()))
        .thenReturn(new byte[]{1, 2, 3});
    when(schoolService.getSchool(eq(schoolId), any())).thenReturn(school);

    int result = postingDistributionService.processDistrictSchoolDistribution(batchId, schoolIds, null, null, null, reportType, transmissionMode);

    assertEquals(1, result);
    verify(restService, times(1)).executeGet(educDistributionApiConstants.getLightSchoolReport(), new ParameterizedTypeReference<List<SchoolReport>>() {},  reportType, schoolId.toString());
    verify(restService, times(1)).executeGet(educDistributionApiConstants.getSchoolReportPDF(), byte[].class, reportType, schoolId.toString());
    verify(schoolService, times(1)).getSchool(eq(schoolId), any());
  }
}