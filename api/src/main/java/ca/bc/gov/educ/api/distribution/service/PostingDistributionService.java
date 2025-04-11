package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.constants.SchoolCategoryCodes;
import ca.bc.gov.educ.api.distribution.exception.ServiceException;
import ca.bc.gov.educ.api.distribution.model.dto.DistributionResponse;
import ca.bc.gov.educ.api.distribution.model.dto.ExceptionMessage;
import ca.bc.gov.educ.api.distribution.model.dto.School;
import ca.bc.gov.educ.api.distribution.model.dto.v2.District;
import ca.bc.gov.educ.api.distribution.model.dto.v2.YearEndReportRequest;
import ca.bc.gov.educ.api.distribution.model.dto.v2.reports.DistrictReport;
import ca.bc.gov.educ.api.distribution.model.dto.v2.reports.SchoolReport;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils;
import ca.bc.gov.educ.api.distribution.util.Generated;
import ca.bc.gov.educ.api.distribution.util.SFTPUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipOutputStream;

import static ca.bc.gov.educ.api.distribution.model.dto.ActivityCode.*;
import static ca.bc.gov.educ.api.distribution.model.dto.ReportType.DISTREP_YE_SD;
import static ca.bc.gov.educ.api.distribution.model.dto.ReportType.NONGRADDISTREP_SD;
import static ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants.*;
import static ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils.*;
import static java.nio.file.Files.createDirectories;

@Slf4j
@Service
public class PostingDistributionService {

    final SFTPUtils sftpUtils;

    final EducDistributionApiConstants educDistributionApiConstants;
    final RestService restService;
    private final SchoolService schoolService;

    @Autowired
    public PostingDistributionService(SFTPUtils sftpUtils, EducDistributionApiConstants educDistributionApiConstants, RestService restService, SchoolService schoolService) {
        this.sftpUtils = sftpUtils;
        this.educDistributionApiConstants = educDistributionApiConstants;
        this.restService = restService;
        this.schoolService = schoolService;
    }

    public boolean postingProcess(DistributionResponse distributionResponse) {
        Long batchId = distributionResponse.getBatchId();
        String activityCode = distributionResponse.getActivityCode();
        String download = distributionResponse.getLocalDownload();
        String transmissionMode = distributionResponse.getTransmissionMode();
        int numberOfPdfs = distributionResponse.getNumberOfPdfs();
        boolean hasDistricts = distributionResponse.getDistricts() != null && !distributionResponse.getDistricts().isEmpty();

        if (NONGRADYERUN.getValue().equalsIgnoreCase(activityCode) && hasDistricts) {
            createDistrictSchoolYearEndNonGradReport(null, NONGRADDISTREP_SD.getValue(), null, distributionResponse.getDistrictSchools());
            numberOfPdfs += processDistrictSchoolDistribution(batchId, null, NONGRADDISTREP_SD.getValue(), null, transmissionMode);
            // GRAD2-2264: removed the redundant logic of NONGRADDISTREP_SC because schools are already processed in YearEndMergeProcess
        }
        return zipBatchDirectory(batchId, download, numberOfPdfs, TMP_DIR);
    }

    public boolean zipBatchDirectory(Long batchId, String download, int numberOfPdfs, String pathToZip) {
        try {
            createZipFile(batchId, pathToZip);
            if (StringUtils.isBlank(download) || !"Y".equalsIgnoreCase(download)) {
                createControlFile(batchId, numberOfPdfs, pathToZip);
                return sftpUtils.sftpUploadBCMail(batchId, pathToZip);
            }
        } catch (Exception ex) {
            log.error(ex.getLocalizedMessage());
            return false;
        }
        return true;
    }

    private void createZipFile(Long batchId, String pathToZip) {
        StringBuilder sourceFileBuilder = new StringBuilder().append(pathToZip).append(DEL).append(batchId);
        Path path = Paths.get(sourceFileBuilder.toString());
        try {
            if (!Files.exists(path)) { createDirectories(path); }
            FileOutputStream fos = new FileOutputStream(pathToZip + "/EDGRAD.BATCH." + batchId + ".zip");
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            File fileToZip = new File(sourceFileBuilder.toString());
            EducDistributionApiUtils.zipFile(fileToZip, fileToZip.getName(), zipOut);
            zipOut.finish();
        } catch (IOException e) {
            throw new ServiceException("Failed to create zip file for batch " + batchId);
        }
    }

    protected void createControlFile(Long batchId, int numberOfPdfs, String pathToZip) {
        try (FileOutputStream fos = new FileOutputStream(pathToZip + "/EDGRAD.BATCH." + batchId + ".txt")) {
            byte[] contentInBytes = String.valueOf(numberOfPdfs).getBytes();
            fos.write(contentInBytes);
            fos.flush();
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
        }
        log.debug("Created Control file for {} PDFs", numberOfPdfs);
    }

    public Integer createDistrictSchoolSuppReport(String schooLabelReportType, String districtReportType, String schoolReportType) {
        return NumberUtils.toInt(restService.executeGet(educDistributionApiConstants.getSchoolDistrictSupplementalReport(), String.class, schooLabelReportType, districtReportType, schoolReportType));
    }

    public Integer createDistrictSchoolMonthReport(String schooLabelReportType, String districtReportType, String schoolReportType) {
        return NumberUtils.toInt(restService.executeGet(educDistributionApiConstants.getSchoolDistrictMonthReport(), String.class, schooLabelReportType, districtReportType, schoolReportType));
    }

    public Integer createSchoolLabelsReport(List<School> schools, String schoolLabelReportType) {
        return NumberUtils.toInt(restService.executePost(educDistributionApiConstants.getSchoolLabelsReport(), String.class, schools, schoolLabelReportType));
    }

    public Integer createDistrictLabelsReport(List<District> districts, String districtLabelReportType) {
        return NumberUtils.toInt(restService.executePost(educDistributionApiConstants.getDistrictLabelsReport(), String.class, districts, districtLabelReportType));
    }

    public Integer createDistrictSchoolYearEndReport(String schooLabelReportType, String districtReportType, String schoolReportType) {
        return NumberUtils.toInt(restService.executeGet(educDistributionApiConstants.getSchoolDistrictYearEndReport(), String.class, schooLabelReportType, districtReportType, schoolReportType));
    }

    public Integer createDistrictSchoolYearEndNonGradReport(String schooLabelReportType, String districtReportType, String schoolReportType) {
        return NumberUtils.toInt(restService.executeGet(educDistributionApiConstants.getSchoolDistrictYearEndNonGradReport(), String.class, schooLabelReportType, districtReportType, schoolReportType));
    }

    public Integer createDistrictSchoolYearEndReport(String schooLabelReportType, String districtReportType, String schoolReportType, YearEndReportRequest yearEndReportRequest) {
        return NumberUtils.toInt(restService.executePost(educDistributionApiConstants.getSchoolDistrictYearEndReport(), String.class, yearEndReportRequest, schooLabelReportType, districtReportType, schoolReportType));
    }

    public Integer createDistrictSchoolYearEndNonGradReport(String schooLabelReportType, String districtReportType, String schoolReportType, List<String> schools) {
        return NumberUtils.toInt(restService.executePost(educDistributionApiConstants.getSchoolDistrictYearEndNonGradReport(), String.class, schools, schooLabelReportType, districtReportType, schoolReportType));
    }

    private int processDistrictLabelDistribution(Long batchId, String districtLabelReportType, List<String> districtIds, String transmissionMode) {
        List<DistrictReport> yeDistrictLabelReports = new ArrayList<>();
        if (districtIds == null || districtIds.isEmpty()) {
            yeDistrictLabelReports.addAll(Objects.requireNonNull(
                restService.executeGet(educDistributionApiConstants.getLightDistrictReport(),
                    new ParameterizedTypeReference<List<DistrictReport>>() {
                    }, districtLabelReportType, "")
            ));
        } else {
            for (String districtId : districtIds) {
                yeDistrictLabelReports.addAll(Objects.requireNonNull(
                    restService.executeGet(educDistributionApiConstants.getLightDistrictReport(),
                        new ParameterizedTypeReference<List<DistrictReport>>() {
                        }, districtLabelReportType, districtId)
                ));
            }
        }
        return processDistrictReports(yeDistrictLabelReports, batchId, districtLabelReportType, transmissionMode);
    }

    private int processSchoolLabelDistribution(Long batchId, List<String> schoolIds, String schoolLabelReportType, String transmissionMode) {
        List<SchoolReport> yeSchoolLabelReports = new ArrayList<>();
        if (schoolIds == null || schoolIds.isEmpty()) {
            yeSchoolLabelReports.addAll(Objects.requireNonNull(
                restService.executeGet(educDistributionApiConstants.getLightSchoolReport(),
                    new ParameterizedTypeReference<List<SchoolReport>>() {
                    }, schoolLabelReportType, "")
            ));
        } else {
            for (String schoolId : schoolIds) {
                yeSchoolLabelReports.addAll(Objects.requireNonNull(
                    restService.executeGet(educDistributionApiConstants.getLightSchoolReport(),
                        new ParameterizedTypeReference<List<SchoolReport>>() {
                        }, schoolLabelReportType, schoolId)
                ));
            }
        }
        return  processSchoolReports(yeSchoolLabelReports, batchId, schoolLabelReportType, transmissionMode);
    }

    @Generated
    public int processDistrictSchoolDistribution(Long batchId, List<String> schoolIds, List<String> districtIds, String schooLabelReportType, String districtReportType, String schoolReportType, String transmissionMode) {
        int numberOfPdfs = 0;
        if (StringUtils.isNotBlank(schooLabelReportType)) {
            if(StringUtils.equalsIgnoreCase(schooLabelReportType, ADDRESS_LABEL_YE)) {//ADDRESS_LABEL_YE are district reports
                numberOfPdfs += processDistrictLabelDistribution(batchId, schooLabelReportType, districtIds, transmissionMode);
            } else {
                numberOfPdfs += processSchoolLabelDistribution(batchId, schoolIds, schooLabelReportType, transmissionMode);
            }
        }
        if (StringUtils.isNotBlank(districtReportType)) {
            List<DistrictReport> yeDistrictReports = new ArrayList<>();
            if (districtIds == null || districtIds.isEmpty()) {
                yeDistrictReports.addAll(Objects.requireNonNull(
                        restService.executeGet(educDistributionApiConstants.getLightDistrictReport(),
                                new ParameterizedTypeReference<List<DistrictReport>>() {
                                }, districtReportType, "")
                ));
            } else {
                for (String districtId : districtIds) {
                    yeDistrictReports.addAll(Objects.requireNonNull(
                            restService.executeGet(educDistributionApiConstants.getLightDistrictReport(),
                                    new ParameterizedTypeReference<List<DistrictReport>>() {
                                    }, districtReportType, districtId)
                    ));
                }
            }
            numberOfPdfs += processDistrictReports(yeDistrictReports, batchId, districtReportType, transmissionMode);
        }
        if (StringUtils.isNotBlank(schoolReportType)) {
            List<SchoolReport> yeSchoolReports = new ArrayList<>();
            if (schoolIds == null || schoolIds.isEmpty()) {
                yeSchoolReports.addAll(Objects.requireNonNull(
                        restService.executeGet(educDistributionApiConstants.getLightSchoolReport(),
                                new ParameterizedTypeReference<List<SchoolReport>>() {
                                }, schoolReportType, "")
                ));
            } else {
                for (String schoolId : schoolIds) {
                    yeSchoolReports.addAll(Objects.requireNonNull(
                            restService.executeGet(educDistributionApiConstants.getLightSchoolReport(),
                                    new ParameterizedTypeReference<List<SchoolReport>>() {
                                    }, schoolReportType, schoolId)
                    ));
                }
            }
            numberOfPdfs += processSchoolReports(yeSchoolReports, batchId, schoolReportType, transmissionMode);
        }
        return numberOfPdfs;
    }

    @Generated
    public int processSchoolLabelsDistribution(Long batchId, String schooLabelReportType, String transmissionMode) {
        List<SchoolReport> yeSchooLabelsReports = restService.executeGet(educDistributionApiConstants.getLightSchoolReport(), new ParameterizedTypeReference<>() {
        }, schooLabelReportType, DEFAULT_SCHOOL_ID);
        assert yeSchooLabelsReports != null;
        return processSchoolReports(yeSchooLabelsReports, batchId, schooLabelReportType, transmissionMode);
    }

    @Generated
    protected int processDistrictSchoolDistribution(Long batchId, String schooLabelReportType, String districtReportType, String schoolReportType, String transmissionMode) {
        int numberOfPdfs = 0;
        if (StringUtils.isNotBlank(schooLabelReportType)) {
            numberOfPdfs += processSchoolLabelsDistribution(batchId, schooLabelReportType, transmissionMode);
        }
        if (StringUtils.isNotBlank(districtReportType)) {
            List<DistrictReport> yeDistrictReports = restService.executeGet(educDistributionApiConstants.getLightDistrictReport(),
                    new ParameterizedTypeReference<>() {
                    }, districtReportType, "");

            assert yeDistrictReports != null;
            numberOfPdfs += processDistrictReports(yeDistrictReports, batchId, districtReportType, transmissionMode);
        }
        if (StringUtils.isNotBlank(schoolReportType)) {
            List<SchoolReport> yeSchoolReports = restService.executeGet(educDistributionApiConstants.getLightSchoolReport(),
                    new ParameterizedTypeReference<>() {
                    }, schoolReportType, "");
            assert yeSchoolReports != null;
            numberOfPdfs += processSchoolReports(yeSchoolReports, batchId, schoolReportType, transmissionMode);
        }
        return numberOfPdfs;
    }

    protected int processDistrictReports(List<DistrictReport> districtReports, Long batchId, String reportType, String transmissionMode) {
        int numberOfPdfs = 0;
        for (DistrictReport report : districtReports) {
            try {
                byte[] gradReportPdf = restService.executeGet(educDistributionApiConstants.getDistrictReportPDF(),
                    byte[].class, report.getReportTypeCode(), report.getDistrictId().toString());
                if (gradReportPdf != null) {
                    log.debug("*** Added District Report PDFs Report Type {} for district {}",
                        report.getReportTypeCode(), report.getDistrictId().toString());

                    ExceptionMessage exception = new ExceptionMessage();
                    District district = schoolService.getDistrict(report.getDistrictId(), exception);

                    if(district != null) {
                        uploadSchoolReportDocuments(
                            batchId,
                            reportType,
                            DEFAULT_SCHOOL_ID.compareTo(report.getDistrictId().toString()) == 0 ?
                                DEFAULT_MINCODE : district.getDistrictNumber(),
                            null,
                            transmissionMode,
                            gradReportPdf);
                        numberOfPdfs++;
                    } else {
                        log.error("Failed to get district information for district {}. The report with id {} has not been processed.", report.getDistrictId().toString(), report.getId());
                        log.error("Exception: {}", exception);
                    }
                } else {
                    log.debug("*** Failed to Add District Report PDFs Report Type {} for district {} in batch {}",
                        report.getReportTypeCode(), report.getDistrictId().toString(), batchId);
                }
            } catch (Exception e) {
                log.error(e.getLocalizedMessage());
            }
        }
        return numberOfPdfs;
    }

    @Generated
    protected int processSchoolReports(List<SchoolReport> schoolReports, Long batchId, String reportType, String transmissionMode) {
        int numberOfPdfs = 0;
        for (SchoolReport report : schoolReports) {
            try {
                byte[] gradReportPdf = restService.executeGet(educDistributionApiConstants.getSchoolReportPDF(),
                        byte[].class, report.getReportTypeCode(), report.getSchoolOfRecordId().toString());
                if (gradReportPdf != null) {
                    log.debug("*** Added School Report PDFs Report Type {} for school {} category {}",
                            report.getReportTypeCode(), report.getSchoolOfRecordId().toString(), report.getSchoolCategory());

                    String mincode = DEFAULT_MINCODE;
                    if (!DEFAULT_SCHOOL_ID.equals(report.getSchoolOfRecordId().toString())) {
                        ExceptionMessage exception = new ExceptionMessage();
                        ca.bc.gov.educ.api.distribution.model.dto.v2.School school = schoolService.getSchool(report.getSchoolOfRecordId(), exception);

                        if (school == null) {
                            log.error("Failed to retrieve school information for School {}. Report ID {} not processed.",
                                report.getSchoolOfRecordId(), report.getId());
                            log.error("Exception: {}", exception);
                        } else {
                            mincode = school.getMinCode();
                        }
                    }
                    uploadSchoolReportDocuments(
                        batchId,
                        reportType,
                        mincode,
                        report.getSchoolCategory(),
                        transmissionMode,
                        gradReportPdf);
                    numberOfPdfs++;
                } else {
                    log.debug("*** Failed to Add School Report PDFs Report Type {} for school {} category {} in batch {}",
                            report.getReportTypeCode(), report.getSchoolOfRecordId().toString(), report.getSchoolCategory(), batchId);
                }
            } catch (Exception e) {
                log.error(e.getLocalizedMessage());
            }
        }
        return numberOfPdfs;
    }

    @Generated
    protected void uploadSchoolReportDocuments(Long batchId, String reportType, String mincode, String schoolCategory, String transmissionMode, byte[] gradReportPdf) {
        boolean isDistrict = ADDRESS_LABEL_YE.equalsIgnoreCase(reportType) || DISTREP_YE_SD.getValue().equalsIgnoreCase(reportType) || NONGRADDISTREP_SD.getValue().equalsIgnoreCase(reportType);
        String districtCode = getDistrictCodeFromMincode(mincode);
        if (StringUtils.isNotBlank(transmissionMode) && TRANSMISSION_MODE_FTP.equalsIgnoreCase(transmissionMode))
            return;
        String rootDirectory = StringUtils.containsAnyIgnoreCase(transmissionMode, TRANSMISSION_MODE_PAPER, TRANSMISSION_MODE_FTP) ? TMP_DIR + EducDistributionApiConstants.FILES_FOLDER_STRUCTURE + StringUtils.upperCase(transmissionMode) : TMP_DIR;
        boolean schoolLevelFolders =
            SchoolCategoryCodes.getSchoolTypesWithoutDistricts().contains(schoolCategory)
            || MONTHLYDIST.getValue().equalsIgnoreCase(transmissionMode)
            || SUPPDIST.getValue().equalsIgnoreCase(transmissionMode) ;
        try {
            StringBuilder fileLocBuilder = new StringBuilder();
            if (isDistrict) {
                fileLocBuilder.append(rootDirectory).append(DEL).append(batchId).append(DEL).append(districtCode);
            } else if (DEFAULT_MINCODE.equalsIgnoreCase(mincode) || ADDRESS_LABEL_SCHL.equalsIgnoreCase(reportType) || ADDRESS_LABEL_PSI.equalsIgnoreCase(reportType)) {
                fileLocBuilder.append(rootDirectory).append(DEL).append(batchId);
            } else if (schoolLevelFolders) {
                fileLocBuilder.append(rootDirectory).append(DEL).append(batchId).append(DEL).append(mincode);
            } else {
                fileLocBuilder.append(rootDirectory).append(DEL).append(batchId).append(DEL).append(districtCode).append(DEL).append(mincode);
            }
            Path path = Paths.get(fileLocBuilder.toString());
            createDirectories(path);
            StringBuilder fileNameBuilder = new StringBuilder();
            if (isDistrict) {
                fileNameBuilder.append(rootDirectory).append(DEL).append(batchId).append(DEL).append(districtCode);
            } else if (DEFAULT_MINCODE.equalsIgnoreCase(mincode) || ADDRESS_LABEL_SCHL.equalsIgnoreCase(reportType) || ADDRESS_LABEL_PSI.equalsIgnoreCase(reportType)) {
                fileNameBuilder.append(rootDirectory).append(DEL).append(batchId);
            } else if (schoolLevelFolders) {
                fileNameBuilder.append(rootDirectory).append(DEL).append(batchId).append(DEL).append(mincode);
            } else {
                fileNameBuilder.append(rootDirectory).append(DEL).append(batchId).append(DEL).append(districtCode).append(DEL).append(mincode);
            }
            if (DEFAULT_MINCODE.equalsIgnoreCase(mincode) || ADDRESS_LABEL_YE.equalsIgnoreCase(reportType) || ADDRESS_LABEL_SCHL.equalsIgnoreCase(reportType) || ADDRESS_LABEL_PSI.equalsIgnoreCase(reportType)) {
                fileNameBuilder.append("/EDGRAD.L.").append("3L14.");
            } else {
                fileNameBuilder.append("/EDGRAD.R.").append("324W.");
            }
            fileNameBuilder.append(EducDistributionApiUtils.getFileNameSchoolReports(mincode)).append(".pdf");
            try (OutputStream out = new FileOutputStream(fileNameBuilder.toString())) {
                out.write(gradReportPdf);
                out.flush();
            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
        }
    }
}
