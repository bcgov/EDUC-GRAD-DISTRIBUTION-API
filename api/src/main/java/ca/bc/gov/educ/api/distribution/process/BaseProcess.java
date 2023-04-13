package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.service.PostingDistributionService;
import ca.bc.gov.educ.api.distribution.service.PsiService;
import ca.bc.gov.educ.api.distribution.service.ReportService;
import ca.bc.gov.educ.api.distribution.service.SchoolService;
import ca.bc.gov.educ.api.distribution.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public abstract class BaseProcess implements DistributionProcess {

    private static final Logger logger = LoggerFactory.getLogger(BaseProcess.class);

    protected static final String DEL = "/";
    protected static final String EXCEPTION = "Error {} ";
    protected static final String SCHOOL_LABELS_CODE = "000000000";

    protected static final String YEARENDDIST = "YEARENDDIST";
    protected static final String MONTHLYDIST = "MONTHLYDIST";
    protected static final String NONGRADDIST = "NONGRADDIST";
    protected static final String SUPPDIST = "SUPPDIST";
    protected static final String DISTREP_YE_SD = "DISTREP_YE_SD";
    protected static final String DISTREP_YE_SC = "DISTREP_YE_SC";
    protected static final String ADDRESS_LABEL_SCHL = "ADDRESS_LABEL_SCHL";
    protected static final String ADDRESS_LABEL_YE = "ADDRESS_LABEL_YE";
    protected static final String DISTREP_SD = "DISTREP_SD";
    protected static final String DISTREP_SC = "DISTREP_SC";
    protected static final String NONGRADDISTREP_SC = "NONGRADDISTREP_SC";

    @Autowired
    GradValidation validation;

    @Autowired
    WebClient webClient;

    @Autowired
    EducDistributionApiConstants educDistributionApiConstants;

    @Autowired
    RestUtils restUtils;

    @Autowired
    SchoolService schoolService;

    @Autowired
    ReportService reportService;

    @Autowired
    PostingDistributionService postingDistributionService;

    @Autowired
    SFTPUtils sftpUtils;

    @Autowired
    PsiService psiService;

    protected CommonSchool getBaseSchoolDetails(DistributionPrintRequest obj, String mincode, ExceptionMessage exception) {
        if (obj != null && obj.getProperName() != null)
            return schoolService.getCommonSchoolDetailsForPackingSlip(obj.getProperName());
        else
            return schoolService.getCommonSchoolDetails(mincode, exception);
    }

    protected void setExtraDataForPackingSlip(ReportRequest packSlipReq, String paperType, int total, int quantity, int currentSlip, Long batchId) {
        packSlipReq.getData().getPackingSlip().setTotal(total);
        packSlipReq.getData().getPackingSlip().setCurrent(currentSlip);
        packSlipReq.getData().getPackingSlip().setQuantity(quantity);
        packSlipReq.getData().getPackingSlip().getOrderType().getPackingSlipType().getPaperType().setCode(paperType);
        packSlipReq.getData().getPackingSlip().getOrderType().setName("Certificate");
        packSlipReq.getData().getPackingSlip().setOrderNumber(batchId);
    }

    protected void setExtraDataForPackingSlip(ReportRequest packSlipReq, String paperType, int total, int quantity, int currentSlip, String orderType, Long batchId) {
        packSlipReq.getData().getPackingSlip().setTotal(total);
        packSlipReq.getData().getPackingSlip().setCurrent(currentSlip);
        packSlipReq.getData().getPackingSlip().setQuantity(quantity);
        packSlipReq.getData().getPackingSlip().getOrderType().getPackingSlipType().getPaperType().setCode(paperType);
        packSlipReq.getData().getPackingSlip().getOrderType().setName(orderType);
        packSlipReq.getData().getPackingSlip().setOrderNumber(batchId);
    }

    protected void postingProcess(Long batchId, ProcessorData processorData, Integer numberOfPdfs) {
        postingProcess(batchId, processorData.getLocalDownload(), numberOfPdfs);
    }

    protected void postingProcess(Long batchId, String download, Integer numberOfPdfs) {
        postingDistributionService.zipBatchDirectory(batchId, download, numberOfPdfs);
    }

    protected Integer createDistrictSchoolYearEndReport(String schooLabelReportType, String districtReportType, String schoolReportType) {
        return postingDistributionService.createDistrictSchoolYearEndReport(schooLabelReportType, districtReportType, schoolReportType);
    }

    protected Integer createSchoolLabelsReport(List<School> schools, String schooLabelReportType) {
        return postingDistributionService.createSchoolLabelsReport(schools, schooLabelReportType);
    }

    protected Integer createDistrictSchoolMonthReport(String schooLabelReportType, String districtReportType, String schoolReportType) {
        return postingDistributionService.createDistrictSchoolMonthReport(schooLabelReportType, districtReportType, schoolReportType);
    }

    protected Integer createDistrictSchoolSuppReport(String schooLabelReportType, String districtReportType, String schoolReportType) {
        return postingDistributionService.createDistrictSchoolSuppReport(schooLabelReportType, districtReportType, schoolReportType);
    }

    protected int processDistrictSchoolDistribution(ProcessorData processorData, String schooLabelReportType, String districtReportType, String schoolReportType) {
        return postingDistributionService.processDistrictSchoolDistribution(processorData,schooLabelReportType, districtReportType, schoolReportType);
    }

    protected void mergeDocuments(ProcessorData processorData, String mincode, String schoolCategoryCode, String fileName, String paperType, List<InputStream> locations) {
        logger.debug("*** Start Transcript Documents Merge ***");
        String districtCode = StringUtils.substring(mincode, 0, 3);
        String activityCode = processorData.getActivityCode();
        File bufferDirectory = null;
        try {
            bufferDirectory = IOUtils.createTempDirectory(EducDistributionApiConstants.TMP_DIR, "buffer");
            PDFMergerUtility pdfMergerUtility = new PDFMergerUtility();
            StringBuilder directoryPathBuilder = new StringBuilder();
            if (MONTHLYDIST.equalsIgnoreCase(activityCode) || "02".equalsIgnoreCase(schoolCategoryCode)) {
                directoryPathBuilder.append(EducDistributionApiConstants.TMP_DIR).append(processorData.getBatchId()).append(DEL).append(mincode).append(DEL);
            } else {
                directoryPathBuilder.append(EducDistributionApiConstants.TMP_DIR).append(processorData.getBatchId()).append(DEL).append(districtCode).append(DEL).append(mincode);
            }
            Path path = Paths.get(directoryPathBuilder.toString());
            Files.createDirectories(path);
            StringBuilder filePathBuilder = new StringBuilder();
            if (MONTHLYDIST.equalsIgnoreCase(activityCode) || "02".equalsIgnoreCase(schoolCategoryCode)) {
                filePathBuilder.append(EducDistributionApiConstants.TMP_DIR).append(processorData.getBatchId()).append(DEL).append(mincode);
            } else {
                filePathBuilder.append(EducDistributionApiConstants.TMP_DIR).append(processorData.getBatchId()).append(DEL).append(districtCode).append(DEL).append(mincode);
            }
            filePathBuilder.append(fileName).append(paperType).append(".").append(EducDistributionApiUtils.getFileNameWithMincodeReports(mincode)).append(".pdf");
            pdfMergerUtility.setDestinationFileName(filePathBuilder.toString());
            pdfMergerUtility.addSources(locations);
            MemoryUsageSetting memoryUsageSetting = MemoryUsageSetting.setupMixed(50000000)
                    .setTempDir(bufferDirectory);
            pdfMergerUtility.mergeDocuments(memoryUsageSetting);
        } catch (Exception e) {
            logger.error(EXCEPTION, e.getLocalizedMessage());
        } finally {
            if (bufferDirectory != null) {
                IOUtils.removeFileOrDirectory(bufferDirectory);
            }
        }
    }

    protected void processSchoolsForLabels(List<School> schools, Psi psi) {
        School school = new School();
        school.setMincode(psi.getPsiCode());
        school.setName(psi.getPsiName());
        school.setTypeBanner(psi.getAttentionName());
        Address address = new Address();
        address.setStreetLine1(psi.getAddress1());
        address.setStreetLine2(psi.getAddress2());
        address.setStreetLine3(psi.getAddress3());
        address.setCity(psi.getCity());
        address.setRegion(psi.getProvinceCode());
        address.setCountry(psi.getCountryCode());
        address.setCode(psi.getPostal());
        school.setAddress(address);
        schools.add(school);
    }

    protected void processSchoolsForLabels(List<School> schools, String mincode, String accessToken, ExceptionMessage exception) {
        TraxSchool traxSchool = schoolService.getTraxSchool(mincode, accessToken, exception);
        if (traxSchool != null) {
            School school = new School();
            school.setMincode(traxSchool.getMinCode());
            school.setName(traxSchool.getSchoolName());
            Address address = new Address();
            address.setStreetLine1(traxSchool.getAddress1());
            address.setStreetLine2(traxSchool.getAddress2());
            address.setCity(traxSchool.getCity());
            address.setRegion(traxSchool.getProvCode());
            address.setCountry(traxSchool.getCountryCode());
            address.setCode(traxSchool.getPostal());
            school.setAddress(address);
            schools.add(school);
        }
    }

}
