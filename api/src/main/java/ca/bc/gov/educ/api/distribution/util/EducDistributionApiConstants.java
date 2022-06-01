package ca.bc.gov.educ.api.distribution.util;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
public class EducDistributionApiConstants {

    //API end-point Mapping constants
	public static final String API_ROOT_MAPPING = "";
    public static final String API_VERSION = "v1";
    public static final String DISTRIBUTION_API_ROOT_MAPPING = "/api/" + API_VERSION + "/distribute";


    public static final String DISTRIBUTION_RUN = "/run/{runType}";
    //Default Date format constants
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

    @Value("${endpoint.grad-graduation-report-api.get-transcript-list.url}")
    private String transcriptDistributionList;

    @Value("${endpoint.grad-graduation-report-api.get-certificate-list.url}")
    private String certificateDistributionList;

    @Value("${endpoint.grad-graduation-report-api.get-transcript.url}")
    private String transcript;

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

    @Value("${endpoint.grad-trax-api.school-by-min-code.url}")
    private String schoolDetails;

    @Value("${endpoint.grad-report-api.get-school-distribution-report.url}")
    private String distributionReport;

    @Value("${endpoint.grad-graduation-api.report-by-pen.url}")
    private String certDataReprint;

    @Value("${endpoint.grad-report-api.certificate_report.url}")
    private String certificateReport;

    @Value("${endpoint.grad-graduation-report-api.update-grad-school-report.url}")
    private String updateSchoolReport;

    @Value("${endpoint.report-api.student_non_grad}")
    private String nonGrad;
}
