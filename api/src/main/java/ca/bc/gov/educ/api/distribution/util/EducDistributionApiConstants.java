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
    public static final String POST_DISTRIBUTION = "/zipandupload";
    //Default Date format constants
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String SECOND_DEFAULT_DATE_FORMAT = "yyyy/MM/dd";
    public static final String SECOND_DEFAULT_DATE_TIME_FORMAT = "yyyy/MM/dd HH:mm:ss";
    //Grad2-1931 - mchintha
    public static final String DATE_FORMAT_YYYYMMDD = "yyyyMMdd";
    public static final String TMP_DIR = "/tmp";
    public static final String DEL = "/";
    public static final String FILES_FOLDER_STRUCTURE = "/Batch/PSI/";
    public static final String TRANSMISSION_MODE_FTP = "FTP";
    public static final String TRANSMISSION_MODE_PAPER = "PAPER";
    public static final String FTP_FILENAME_PREFIX = "/GRAD_FIN";
    public static final String FTP_FILENAME_SUFFIX = "_RESULTS";
    public static final String EXCEPTION_MSG_FILE_NOT_CREATED_AT_PATH = "Path is not available to create DAT file to write the student data";
    public static final String FOUR_ZEROES = "0000";
    public static final String THREE_ZEROES = "000";
    public static final String SIX_ZEROES = "000000";
    public static final String TWO_ZEROES = "00";
    public static final String LETTER_A = "A";
    public static final String LETTER_B = "B";
    public static final String LETTER_C = "C";
    public static final String LETTER_D = "D";
    public static final String LETTER_Y = "Y";
    public static final String LETTER_N = "N";

    //Grad2-2182 - mchintha
    public static final int NUMBER_ONE = 1;
    public static final int NUMBER_TWO = 2;
    public static final int NUMBER_THREE = 3;
    public static final int NUMBER_FOUR = 4;
    public static final int NUMBER_FIVE = 5;
    public static final String DATE_FORMAT_YYYYMM = "yyyyMM";
    public static final String ASSESSMENT_LTE = "LTE10";
    public static final String ASSESSMENT_LTP = "LTP10";

    public static final String TRAX_API_STATUS = "TRAX-API IS DOWN";

    @Value("${endpoint.grad-graduation-report-api.get-transcript-list.url}")
    private String transcriptDistributionList;

    @Value("${endpoint.grad-graduation-report-api.get-certificate-list.url}")
    private String certificateDistributionList;

    @Value("${endpoint.grad-graduation-report-api.get-transcript.url}")
    private String transcript;

    @Value("${endpoint.grad-graduation-report-api.get-transcript-psi-by-student-id.url}")
    private String transcriptPsiUsingStudentID;

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

    @Value("${endpoint.grad-trax-api.psi-by-psi-code.url}")
    private String psiByPsiCode;

    @Value("${endpoint.grad-trax-api.school-by-min-code.url}")
    private String schoolByMincode;

    @Value("${endpoint.grad-trax-api.district-by-dist-code.url}")
    private String districtByDistcode;

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

    @Value("${endpoint.grad-report-api.student_non_grad_projected}")
    private String studentNonGradProjected;

    @Value("${endpoint.grad-report-api.student_non_grad}")
    private String studentNonGrad;

    @Value("${endpoint.grad-graduation-report-api.school-report.url}")
    private String schoolReport;

    @Value("${endpoint.grad-graduation-report-api.district-report.url}")
    private String districtReport;

    /*@Value("${endpoint.grad-graduation-report-api.school-report-by-report-type.url}")
    private String schoolReportsByReportType;*/

    @Value("${endpoint.grad-graduation-api.school_district_year_end_report.url}")
    private String schoolDistrictYearEndReport;

    @Value("${endpoint.grad-graduation-api.school_district_year_end_nongrad_report.url}")
    private String schoolDistrictYearEndNonGradReport;

    @Value("${endpoint.grad-graduation-api.school_district_month_report.url}")
    private String schoolDistrictMonthReport;

    @Value("${endpoint.grad-graduation-api.school_district_supplemental_report.url}")
    private String schoolDistrictSupplementalReport;

    @Value("${endpoint.grad-graduation-api.school_labels_report.url}")
    private String schoolLabelsReport;

    @Value("${endpoint.grad-batch-graduation-api.distribution.notify-completion.url}")
    private String distributionJobCompleteNotification;

    // Splunk LogHelper Enabled
    @Value("${splunk.log-helper.enabled}")
    private boolean splunkLogHelperEnabled;

    @Value("${authorization.token-expiry-offset}")
    private int tokenExpiryOffset;

    private int threadPoolCoreSize = 5;

    private int threadPoolMaxSize = 15;

}
