package ca.bc.gov.educ.api.distribution.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class EducDistributionApiConstants {

    //API end-point Mapping constants
	public static final String API_ROOT_MAPPING = "";
    public static final String API_VERSION = "v1";
    public static final String DISTRIBUTION_API_ROOT_MAPPING = "/api/" + API_VERSION + "/distribute";

    public static final String CORRELATION_ID = "correlationID";

    public static final String DISTRIBUTION_RUN = "/run/{runType}";
    public static final String LOCAL_DOWNLOAD = "/download/{batchId}";
    //Default Date format constants
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String SECONDARY_DATE_FORMAT = "yyyy/MM/dd";
    //Grad2-1931
    public static final String DATE_FORMAT = "yyyyMMdd";

    public static final String TMP_DIR = "/tmp/";

    @Value("${endpoint.grad-graduation-report-api.get-transcript-list.url}")
    private String transcriptDistributionList;

    @Value("${endpoint.grad-graduation-report-api.get-certificate-list.url}")
    private String certificateDistributionList;

    @Value("${endpoint.grad-graduation-report-api.get-transcript.url}")
    private String transcript;

    @Value("${endpoint.grad-graduation-report-api.get-transcript-by-student-id.url}")
    private String transcriptUsingStudentID;

    @Value("${endpoint.grad-graduation-report-api.get-achievement-report.url}")
    private String achievementReport;

    @Value("${endpoint.grad-graduation-report-api.get-certificate.url}")
    private String certificate;

    @Value("${endpoint.grad-student-api.get-student-record.url}")
    private String studentInfo;

    @Value("${endpoint.grad-report-api.get-packing-slip.url}")
    private String packingSlip;

    @Value("${authorization.user}")
    private String userName;

    @Value("${authorization.password}")
    private String password;

    @Value("${endpoint.keycloak.getToken}")
    private String tokenUrl;

    @Value("${endpoint.educ-school-api.school-by-min-code.url}")
    private String schoolDetails;

    @Value("${endpoint.grad-trax-api.psi-by-psi-code.url}")
    private String psiDetails;

    @Value("${endpoint.grad-report-api.get-school-distribution-report.url}")
    private String schoolDistributionReport;

    @Value("${endpoint.grad-graduation-api.report-by-pen.url}")
    private String certDataReprint;

    @Value("${endpoint.grad-graduation-api.student-report-by-pen.url}")
    private String transcriptCSVData;

    @Value("${endpoint.grad-report-api.certificate_report.url}")
    private String certificateReport;

    @Value("${endpoint.grad-report-api.transcript_report.url}")
    private String transcriptReport;

    @Value("${endpoint.grad-graduation-report-api.update-grad-school-report.url}")
    private String updateSchoolReport;

    @Value("${endpoint.grad-report-api.student_non_grad}")
    private String nonGrad;

    @Value("${endpoint.grad-graduation-report-api.school-report.url}")
    private String schoolReport;

    @Value("${endpoint.grad-graduation-report-api.school-report-by-report-type.url}")
    private String schoolReportsByReportType;

    @Value("${endpoint.grad-graduation-api.school_district_year_end_report.url}")
    private String schoolDistrictYearEndReport;

    @Value("${endpoint.grad-graduation-api.school_district_month_report.url}")
    private String schoolDistrictMonthReport;

    @Value("${endpoint.grad-graduation-api.school_district_supplemental_report.url}")
    private String schoolDistrictSupplementalReport;

    @Value("${endpoint.grad-graduation-api.school_labels_report.url}")
    private String schoolLabelsReport;

    // Splunk LogHelper Enabled
    @Value("${splunk.log-helper.enabled}")
    private boolean splunkLogHelperEnabled;

    @Value("${authorization.token-expiry-offset}")
    private int tokenExpiryOffset;

}
