package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.DistributionResponse;
import ca.bc.gov.educ.api.distribution.model.dto.School;
import ca.bc.gov.educ.api.distribution.model.dto.SchoolReports;
import ca.bc.gov.educ.api.distribution.model.dto.TraxDistrict;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils;
import ca.bc.gov.educ.api.distribution.util.Generated;
import ca.bc.gov.educ.api.distribution.util.SFTPUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipOutputStream;

import static ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants.*;
import static ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils.*;
import static java.nio.file.Files.createDirectories;

@Service
public class PostingDistributionService {

    private static final Logger logger = LoggerFactory.getLogger(PostingDistributionService.class);

    final SFTPUtils sftpUtils;

    final EducDistributionApiConstants educDistributionApiConstants;
    final RestService restService;

    @Autowired
    public PostingDistributionService(SFTPUtils sftpUtils, EducDistributionApiConstants educDistributionApiConstants, RestService restService) {
        this.sftpUtils = sftpUtils;
        this.educDistributionApiConstants = educDistributionApiConstants;
        this.restService = restService;
    }

    public boolean postingProcess(DistributionResponse distributionResponse) {
        Long batchId = distributionResponse.getBatchId();
        String activityCode = distributionResponse.getActivityCode();
        String download = distributionResponse.getLocalDownload();
        String transmissionMode = distributionResponse.getTransmissionMode();
        int numberOfPdfs = distributionResponse.getNumberOfPdfs();
        List<String> districtCodes = extractDistrictCodes(distributionResponse);
        if (NONGRADYERUN.equalsIgnoreCase(activityCode) && !districtCodes.isEmpty()) {
            createDistrictSchoolYearEndNonGradReport(null, NONGRADDISTREP_SD, null,
                distributionResponse.getDistrictSchools().isEmpty()? districtCodes : distributionResponse.getDistrictSchools());
            numberOfPdfs += processDistrictSchoolDistribution(batchId, null, NONGRADDISTREP_SD, null, transmissionMode);
            // GRAD2-2264: removed the redundant logic of NONGRADDISTREP_SC because schools are already processed in YearEndMergeProcess
        }
        return zipBatchDirectory(batchId, download, numberOfPdfs, TMP_DIR);
    }

    public boolean zipBatchDirectory(Long batchId, String download, int numberOfPdfs, String pathToZip) {
        createZipFile(batchId, pathToZip);
        if (StringUtils.isBlank(download) || !"Y".equalsIgnoreCase(download)) {
            createControlFile(batchId, numberOfPdfs, pathToZip);
            sftpUtils.sftpUploadBCMail(batchId, pathToZip);
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
            logger.error(e.getLocalizedMessage());
        }
    }

    protected void createControlFile(Long batchId, int numberOfPdfs, String pathToZip) {
        try (FileOutputStream fos = new FileOutputStream(pathToZip + "/EDGRAD.BATCH." + batchId + ".txt")) {
            byte[] contentInBytes = String.valueOf(numberOfPdfs).getBytes();
            fos.write(contentInBytes);
            fos.flush();
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage());
        }
        logger.debug("Created Control file for {} PDFs", numberOfPdfs);
    }

    public Integer createDistrictSchoolSuppReport(String schooLabelReportType, String districtReportType, String schoolReportType) {
        return restService.executeGet(educDistributionApiConstants.getSchoolDistrictSupplementalReport(), Integer.class, schooLabelReportType, districtReportType, schoolReportType);
    }

    public Integer createDistrictSchoolMonthReport(String schooLabelReportType, String districtReportType, String schoolReportType) {
        return restService.executeGet(educDistributionApiConstants.getSchoolDistrictMonthReport(), Integer.class, schooLabelReportType, districtReportType, schoolReportType);
    }

    public Integer createSchoolLabelsReport(List<School> schools, String schooLabelReportType) {
        logger.debug("***** Create School Label Reports {} *****", schooLabelReportType);
        Integer reportCount = 0;
        String url = String.format(educDistributionApiConstants.getSchoolLabelsReport(), schooLabelReportType);
        List<School> processSchools = new ArrayList<>();
        if (StringUtils.equalsIgnoreCase(ADDRESS_LABEL_SCHL, schooLabelReportType)) {
            for (School s : schools) {
                if (s.getMincode().length() > 3) {
                    processSchools.add(s);
                }
            }
        }
        if (StringUtils.equalsIgnoreCase(ADDRESS_LABEL_YE, schooLabelReportType)) {
            for (School s : schools) {
                if (s.getMincode().length() == 3) {
                    processSchools.add(s);
                }
            }
        }
        if (StringUtils.equalsIgnoreCase(ADDRESS_LABEL_PSI, schooLabelReportType)) {
            for (School s : schools) {
                if (s.getMincode().length() <= 3) {
                    processSchools.add(s);
                }
            }
        }
        reportCount += restService.executePost(url, Integer.class, processSchools);
        logger.debug("***** Number of created School Label Reports {} *****", reportCount);
        return reportCount;
    }

    public Integer createDistrictLabelsReport(List<TraxDistrict> schools, String districtLabelReportType) {
        return restService.executePost(educDistributionApiConstants.getSchoolLabelsReport(), Integer.class, schools, districtLabelReportType);
    }

    public Integer createDistrictSchoolYearEndReport(String schooLabelReportType, String districtReportType, String schoolReportType) {
        return restService.executeGet(educDistributionApiConstants.getSchoolDistrictYearEndReport(), Integer.class, schooLabelReportType, districtReportType, schoolReportType);
    }

    public Integer createDistrictSchoolYearEndNonGradReport(String schooLabelReportType, String districtReportType, String schoolReportType) {
        return restService.executeGet(educDistributionApiConstants.getSchoolDistrictYearEndNonGradReport(), Integer.class, schooLabelReportType, districtReportType, schoolReportType);
    }

    public Integer createDistrictSchoolYearEndReport(String schooLabelReportType, String districtReportType, String schoolReportType, List<String> schools) {
        return restService.executePost(educDistributionApiConstants.getSchoolDistrictYearEndReport(), Integer.class, schools, schooLabelReportType, districtReportType, schoolReportType);
    }

    public Integer createDistrictSchoolYearEndNonGradReport(String schooLabelReportType, String districtReportType, String schoolReportType, List<String> schools) {
        return restService.executePost(educDistributionApiConstants.getSchoolDistrictYearEndNonGradReport(), Integer.class, schools, schooLabelReportType, districtReportType, schoolReportType);
    }

    public int processSchoolLabelsDistribution(Long batchId, String schooLabelReportType, String transmissionMode) {
        return processSchoolLabelsDistribution(batchId, "", schooLabelReportType, transmissionMode);
    }

    @Generated
    public int processDistrictSchoolDistribution(Long batchId, Collection<String> mincodes, String schooLabelReportType, String districtReportType, String schoolReportType, String transmissionMode) {
        int numberOfPdfs = 0;
        boolean processMincodes = !(mincodes != null && !mincodes.isEmpty());
        if (StringUtils.isNotBlank(schooLabelReportType)) {
            List<SchoolReports> yeSchoolLabelReports = new ArrayList<>();
            if (processMincodes) {
                yeSchoolLabelReports.addAll(Objects.requireNonNull(
                        restService.executeGet(educDistributionApiConstants.getSchoolReportsByReportType(),
                                new ParameterizedTypeReference<List<SchoolReports>>() {
                                }, schooLabelReportType, "")
                ));
            } else {
                for (String mincode : mincodes) {
                    yeSchoolLabelReports.addAll(Objects.requireNonNull(
                            restService.executeGet(educDistributionApiConstants.getSchoolReportsByReportType(),
                                    new ParameterizedTypeReference<List<SchoolReports>>() {
                                    }, schooLabelReportType, mincode)
                    ));
                }
            }
            assert yeSchoolLabelReports != null;
            numberOfPdfs += processDistrictSchoolReports(yeSchoolLabelReports, batchId, schooLabelReportType, transmissionMode);
        }
        if (StringUtils.isNotBlank(districtReportType)) {
            List<SchoolReports> yeDistrictReports = new ArrayList<>();
            if (processMincodes) {
                yeDistrictReports.addAll(Objects.requireNonNull(
                        restService.executeGet(educDistributionApiConstants.getSchoolReportsByReportType(),
                                new ParameterizedTypeReference<List<SchoolReports>>() {
                                }, districtReportType, "")
                ));
            } else {
                for (String mincode : mincodes) {
                    yeDistrictReports.addAll(Objects.requireNonNull(
                            restService.executeGet(educDistributionApiConstants.getSchoolReportsByReportType(),
                                    new ParameterizedTypeReference<List<SchoolReports>>() {
                                    }, districtReportType, mincode)
                    ));
                }
            }

            assert yeDistrictReports != null;
            numberOfPdfs += processDistrictSchoolReports(yeDistrictReports, batchId, districtReportType, transmissionMode);
        }
        if (StringUtils.isNotBlank(schoolReportType)) {
            List<SchoolReports> yeSchoolReports = new ArrayList<>();
            if (processMincodes) {
                yeSchoolReports.addAll(Objects.requireNonNull(
                        restService.executeGet(educDistributionApiConstants.getSchoolReportsByReportType(),
                                new ParameterizedTypeReference<List<SchoolReports>>() {
                                }, schoolReportType, "")
                ));
            } else {
                for (String mincode : mincodes) {
                    yeSchoolReports.addAll(Objects.requireNonNull(
                            restService.executeGet(educDistributionApiConstants.getSchoolReportsByReportType(),
                                    new ParameterizedTypeReference<List<SchoolReports>>() {
                                    }, schoolReportType, mincode)
                    ));
                }
            }
            assert yeSchoolReports != null;

            numberOfPdfs += processDistrictSchoolReports(yeSchoolReports, batchId, schoolReportType, transmissionMode);
        }
        return numberOfPdfs;
    }

    @Generated
    protected int processSchoolLabelsDistribution(Long batchId, String mincode, String schooLabelReportType, String transmissionMode) {
        List<SchoolReports> yeSchooLabelsReports = restService.executeGet(educDistributionApiConstants.getSchoolReportsByReportType(), new ParameterizedTypeReference<List<SchoolReports>>() {
        }, schooLabelReportType, mincode);
        assert yeSchooLabelsReports != null;
        return processDistrictSchoolReports(yeSchooLabelsReports, batchId, schooLabelReportType, transmissionMode);
    }

    @Generated
    protected int processDistrictSchoolDistribution(Long batchId, String schooLabelReportType, String districtReportType, String schoolReportType, String transmissionMode) {
        int numberOfPdfs = 0;
        if (StringUtils.isNotBlank(schooLabelReportType)) {
            numberOfPdfs += processSchoolLabelsDistribution(batchId, schooLabelReportType, transmissionMode);
        }
        if (StringUtils.isNotBlank(districtReportType)) {
            List<SchoolReports> yeDistrictReports = restService.executeGet(educDistributionApiConstants.getSchoolReportsByReportType(),
                    new ParameterizedTypeReference<List<SchoolReports>>() {
                    }, districtReportType, "");

            assert yeDistrictReports != null;
            numberOfPdfs += processDistrictSchoolReports(yeDistrictReports, batchId, districtReportType, transmissionMode);
        }
        if (StringUtils.isNotBlank(schoolReportType)) {
            List<SchoolReports> yeSchoolReports = restService.executeGet(educDistributionApiConstants.getSchoolReportsByReportType(),
                    new ParameterizedTypeReference<List<SchoolReports>>() {
                    }, schoolReportType, "");
            assert yeSchoolReports != null;
            numberOfPdfs += processDistrictSchoolReports(yeSchoolReports, batchId, schoolReportType, transmissionMode);
        }
        return numberOfPdfs;
    }

    @Generated
    protected int processDistrictSchoolReports(List<SchoolReports> schoolReports, Long batchId, String reportType, String transmissionMode) {
        int numberOfPdfs = 0;
        for (SchoolReports report : schoolReports) {
            try {
                byte[] gradReportPdf = restService.executeGet(educDistributionApiConstants.getSchoolReport(), byte[].class, report.getSchoolOfRecord(), report.getReportTypeCode());
                if (gradReportPdf != null) {
                    logger.debug("*** Added School Report PDFs Report Type {} for school {} category {}", report.getReportTypeCode(), report.getSchoolOfRecord(), report.getSchoolCategory());
                    uploadSchoolReportDocuments(
                            batchId,
                            reportType,
                            report.getSchoolOfRecord(),
                            report.getSchoolCategory(),
                            transmissionMode,
                            gradReportPdf);
                    numberOfPdfs++;
                } else {
                    logger.debug("*** Failed to Add School Report PDFs Report Type {} for school {} category {} in batch {}", report.getReportTypeCode(), report.getSchoolOfRecord(), report.getSchoolCategory(), batchId);
                }
            } catch (Exception e) {
                logger.error(e.getLocalizedMessage());
            }
        }
        return numberOfPdfs;
    }

    @Generated
    protected void uploadSchoolReportDocuments(Long batchId, String reportType, String mincode, String schoolCategory, String transmissionMode, byte[] gradReportPdf) {
        boolean isDistrict = ADDRESS_LABEL_YE.equalsIgnoreCase(reportType) || DISTREP_YE_SD.equalsIgnoreCase(reportType) || NONGRADDISTREP_SD.equalsIgnoreCase(reportType);
        String districtCode = getDistrictCodeFromMincode(mincode);
        if (StringUtils.isNotBlank(transmissionMode) && TRANSMISSION_MODE_FTP.equalsIgnoreCase(transmissionMode))
            return;
        String rootDirectory = StringUtils.containsAnyIgnoreCase(transmissionMode, TRANSMISSION_MODE_PAPER, TRANSMISSION_MODE_FTP) ? TMP_DIR + EducDistributionApiConstants.FILES_FOLDER_STRUCTURE + StringUtils.upperCase(transmissionMode) : TMP_DIR;
        boolean schoolLevelFolders = StringUtils.containsAnyIgnoreCase(schoolCategory, "02", "03", "09") || MONTHLYDIST.equalsIgnoreCase(transmissionMode) || SUPPDIST.equalsIgnoreCase(transmissionMode);
        try {
            StringBuilder fileLocBuilder = new StringBuilder();
            if (isDistrict) {
                fileLocBuilder.append(rootDirectory).append(DEL).append(batchId).append(DEL).append(districtCode);
            } else if (SCHOOL_LABELS_CODE.equalsIgnoreCase(mincode) || ADDRESS_LABEL_SCHL.equalsIgnoreCase(reportType) || ADDRESS_LABEL_PSI.equalsIgnoreCase(reportType)) {
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
            } else if (SCHOOL_LABELS_CODE.equalsIgnoreCase(mincode) || ADDRESS_LABEL_SCHL.equalsIgnoreCase(reportType) || ADDRESS_LABEL_PSI.equalsIgnoreCase(reportType)) {
                fileNameBuilder.append(rootDirectory).append(DEL).append(batchId);
            } else if (schoolLevelFolders) {
                fileNameBuilder.append(rootDirectory).append(DEL).append(batchId).append(DEL).append(mincode);
            } else {
                fileNameBuilder.append(rootDirectory).append(DEL).append(batchId).append(DEL).append(districtCode).append(DEL).append(mincode);
            }
            if (SCHOOL_LABELS_CODE.equalsIgnoreCase(mincode) || ADDRESS_LABEL_YE.equalsIgnoreCase(reportType) || ADDRESS_LABEL_SCHL.equalsIgnoreCase(reportType) || ADDRESS_LABEL_PSI.equalsIgnoreCase(reportType)) {
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
            logger.error(e.getLocalizedMessage());
        }
    }

    private List<String> extractDistrictCodes(DistributionResponse distributionResponse) {
        return distributionResponse.getDistricts().stream().map(School::getMincode).toList();
    }

}
