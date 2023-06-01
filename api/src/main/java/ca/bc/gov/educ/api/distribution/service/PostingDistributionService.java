package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils;
import ca.bc.gov.educ.api.distribution.util.RestUtils;
import ca.bc.gov.educ.api.distribution.util.SFTPUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipOutputStream;

import static ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils.*;

@Service
public class PostingDistributionService {

    private static final Logger logger = LoggerFactory.getLogger(PostingDistributionService.class);

    public static final String DEL = "/";

    final SFTPUtils sftpUtils;
    final RestUtils restUtils;
    final WebClient webClient;
    final EducDistributionApiConstants educDistributionApiConstants;
    final RestService restService;

    @Autowired
    public PostingDistributionService(SFTPUtils sftpUtils, RestUtils restUtils, WebClient webClient, EducDistributionApiConstants educDistributionApiConstants, RestService restService) {
        this.sftpUtils = sftpUtils;
        this.restUtils = restUtils;
        this.webClient = webClient;
        this.educDistributionApiConstants = educDistributionApiConstants;
        this.restService = restService;
    }

    public boolean postingProcess(DistributionResponse distributionResponse) {
        Long batchId = distributionResponse.getBatchId();
        String activityCode = distributionResponse.getActivityCode();
        String download = distributionResponse.getLocalDownload();
        int numberOfPdfs = distributionResponse.getNumberOfPdfs();
        StudentSearchRequest searchRequest = distributionResponse.getStudentSearchRequest();
        if(YEARENDDIST.equalsIgnoreCase(activityCode)) {
            if(searchRequest != null && (searchRequest.getDistricts() != null && !searchRequest.getDistricts().isEmpty())) {
                createDistrictSchoolYearEndReport(null, DISTREP_YE_SD, DISTREP_YE_SC, searchRequest.getDistricts());
            } else if(searchRequest != null && (searchRequest.getSchoolOfRecords() != null && !searchRequest.getSchoolOfRecords().isEmpty())) {
                createDistrictSchoolYearEndReport(null, DISTREP_YE_SD, DISTREP_YE_SC, searchRequest.getSchoolOfRecords());
            } else {
                createDistrictSchoolYearEndReport(null, DISTREP_YE_SD, DISTREP_YE_SC);
            }
            numberOfPdfs += processDistrictSchoolDistribution(batchId, null, DISTREP_YE_SD, DISTREP_YE_SC);
        }
        return zipBatchDirectory(batchId, download, numberOfPdfs);
    }

    public boolean zipBatchDirectory(Long batchId, String download, int numberOfPdfs) {
        createZipFile(batchId);
        if (StringUtils.isBlank(download) || !"Y".equalsIgnoreCase(download)) {
            createControlFile(batchId, numberOfPdfs);
            sftpUtils.sftpUploadBCMail(batchId);
        }
        return true;
    }

    private void createZipFile(Long batchId) {
        StringBuilder sourceFileBuilder = new StringBuilder().append(EducDistributionApiConstants.TMP_DIR).append(DEL).append(batchId);
        try (FileOutputStream fos = new FileOutputStream(EducDistributionApiConstants.TMP_DIR + "/EDGRAD.BATCH." + batchId + ".zip")) {
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            File fileToZip = new File(sourceFileBuilder.toString());
            EducDistributionApiUtils.zipFile(fileToZip, fileToZip.getName(), zipOut);
            zipOut.close();
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage());
        }
    }

    protected void createControlFile(Long batchId, int numberOfPdfs) {
        try (FileOutputStream fos = new FileOutputStream(EducDistributionApiConstants.TMP_DIR + "/EDGRAD.BATCH." + batchId + ".txt")) {
            byte[] contentInBytes = String.valueOf(numberOfPdfs).getBytes();
            fos.write(contentInBytes);
            fos.flush();
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage());
        }
        logger.debug("Created Control file for {} PDFs", numberOfPdfs);
    }

    public Integer createDistrictSchoolSuppReport(String schooLabelReportType, String districtReportType, String schoolReportType) {
        Integer reportCount = 0;
        final UUID correlationID = UUID.randomUUID();
        reportCount += restService.executeGet(educDistributionApiConstants.getSchoolDistrictSupplementalReport(), Integer.class, schooLabelReportType, districtReportType, schoolReportType );
        return reportCount;
    }

    public Integer createDistrictSchoolMonthReport(String schooLabelReportType, String districtReportType, String schoolReportType) {
        Integer reportCount = 0;
        final UUID correlationID = UUID.randomUUID();
        reportCount += restService.executeGet(educDistributionApiConstants.getSchoolDistrictMonthReport(), Integer.class, schooLabelReportType, districtReportType, schoolReportType);
        return reportCount;
    }

    public Integer createSchoolLabelsReport(List<School> schools, String schooLabelReportType) {
        logger.debug("***** Distribute School Label Reports {} *****", schooLabelReportType);
        Integer reportCount = 0;
        final UUID correlationID = UUID.randomUUID();
        String url = String.format(educDistributionApiConstants.getSchoolLabelsReport(), schooLabelReportType);
        List<School> processSchools = new ArrayList<>();
        if(StringUtils.equalsIgnoreCase(ADDRESS_LABEL_SCHL, schooLabelReportType)) {
           for(School s: schools) {
                if(s.getMincode().length() > 3) {
                    processSchools.add(s);
                }
            }
        }
        if(StringUtils.equalsIgnoreCase(ADDRESS_LABEL_YE, schooLabelReportType)) {
            for(School s: schools) {
                if(s.getMincode().length() == 3) {
                    processSchools.add(s);
                }
            }
        }
        reportCount += restService.executePost(url, Integer.class, processSchools);
        logger.debug("***** Number of distributed School Label Reports {} *****", reportCount);
        return reportCount;
    }

    public Integer createDistrictLabelsReport(List<TraxDistrict> schools, String districtLabelReportType) {
        return restService.executePost(educDistributionApiConstants.getSchoolLabelsReport(), Integer.class, schools, districtLabelReportType);
    }

    public Integer createDistrictSchoolYearEndReport(String schooLabelReportType, String districtReportType, String schoolReportType) {
        return restService.executeGet(educDistributionApiConstants.getSchoolDistrictYearEndReport(), Integer.class, schooLabelReportType, districtReportType, schoolReportType);
    }

    public Integer createDistrictSchoolYearEndReport(String schooLabelReportType, String districtReportType, String schoolReportType, List<String> schools) {
        return restService.executePost(educDistributionApiConstants.getSchoolDistrictYearEndReport(), Integer.class, schools, schooLabelReportType, districtReportType, schoolReportType);
    }

    public int processSchoolLabelsDistribution(Long batchId, String schooLabelReportType) {
        return processSchoolLabelsDistribution(batchId, "", schooLabelReportType);
    }

    public int processSchoolLabelsDistribution(Long batchId, String mincode, String schooLabelReportType) {
        List<SchoolReports> yeSchooLabelsReports = restService.executeGet(educDistributionApiConstants.getSchoolReportsByReportType(), new ParameterizedTypeReference<List<SchoolReports>>() {
        }, schooLabelReportType, mincode);
        assert yeSchooLabelsReports != null;
        return processDistrictSchoolReports(yeSchooLabelsReports, batchId, schooLabelReportType);
    }

    public int processDistrictSchoolDistribution(Long batchId, Collection<String> mincodes, String schooLabelReportType, String districtReportType, String schoolReportType) {
        int numberOfPdfs = 0;
        if (StringUtils.isNotBlank(schooLabelReportType)) {
            List<SchoolReports> yeSchoolLabelReports = new ArrayList<>();
            if (mincodes.isEmpty()) {
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
            numberOfPdfs += processDistrictSchoolReports(yeSchoolLabelReports, batchId, schooLabelReportType);
        }
        if (StringUtils.isNotBlank(districtReportType)) {
            List<SchoolReports> yeDistrictReports = new ArrayList<>();
            if (mincodes.isEmpty()) {
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
            numberOfPdfs += processDistrictSchoolReports(yeDistrictReports, batchId, schooLabelReportType);
        }
        if (StringUtils.isNotBlank(schoolReportType)) {
            List<SchoolReports> yeSchoolReports = new ArrayList<>();
            if (mincodes.isEmpty()) {
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
            numberOfPdfs += processDistrictSchoolReports(yeSchoolReports, batchId, schooLabelReportType);
        }
        return numberOfPdfs;
    }

    public int processDistrictSchoolDistribution(Long batchId, String schooLabelReportType, String districtReportType, String schoolReportType) {
        int numberOfPdfs = 0;
        if (StringUtils.isNotBlank(schooLabelReportType)) {
            numberOfPdfs += processSchoolLabelsDistribution(batchId, schooLabelReportType);
        }
        if (StringUtils.isNotBlank(districtReportType)) {
            List<SchoolReports> yeDistrictReports = restService.executeGet(educDistributionApiConstants.getSchoolReportsByReportType(),
                    new ParameterizedTypeReference<List<SchoolReports>>() {
                    }, districtReportType, "");

            assert yeDistrictReports != null;
            numberOfPdfs += processDistrictSchoolReports(yeDistrictReports, batchId, schooLabelReportType);
        }
        if (StringUtils.isNotBlank(schoolReportType)) {
            List<SchoolReports> yeSchoolReports = restService.executeGet(educDistributionApiConstants.getSchoolReportsByReportType(),
                    new ParameterizedTypeReference<List<SchoolReports>>() {
                    }, schoolReportType, "");
            assert yeSchoolReports != null;
            numberOfPdfs += processDistrictSchoolReports(yeSchoolReports, batchId, schooLabelReportType);
        }
        return numberOfPdfs;
    }

    protected int processDistrictSchoolReports(List<SchoolReports> schoolReports, Long batchId, String schooLabelReportType) {
        int numberOfPdfs = 0;
        for (SchoolReports report : schoolReports) {
            try {
                byte[] gradReportPdf = restService.executeGet(educDistributionApiConstants.getSchoolReport(), byte[].class, report.getSchoolOfRecord(), report.getReportTypeCode());
                if (gradReportPdf != null) {
                    logger.debug("*** Added PDFs Current Report Type {} for school {} category {}", report.getReportTypeCode(), report.getSchoolOfRecord(), report.getSchoolCategory());
                    uploadSchoolReportDocuments(
                            batchId,
                            schooLabelReportType,
                            report.getSchoolOfRecord(),
                            report.getSchoolCategory(),
                            gradReportPdf);
                    numberOfPdfs++;
                } else {
                    logger.debug("*** Failed to Add PDFs Current Report Type {} for school {} category {}", report.getReportTypeCode(), report.getSchoolOfRecord(), report.getSchoolCategory());
                }
            } catch (Exception e) {
                logger.error(e.getLocalizedMessage());
            }
        }
        return numberOfPdfs;
    }

    protected void uploadSchoolReportDocuments(Long batchId, String schooLabelReportType, String mincode, String schoolCategory, byte[] gradReportPdf) {
        boolean isDistrict = (StringUtils.isNotBlank(mincode) && StringUtils.length(mincode) == 3)  || ADDRESS_LABEL_YE.equalsIgnoreCase(schooLabelReportType);
        String districtCode = getDistrictCodeFromMincode(mincode);
        try {
            StringBuilder fileLocBuilder = new StringBuilder();
            if (isDistrict) {
                fileLocBuilder.append(EducDistributionApiConstants.TMP_DIR).append(DEL).append(batchId).append(DEL).append(districtCode);
            } else if (SCHOOL_LABELS_CODE.equalsIgnoreCase(mincode) || ADDRESS_LABEL_SCHL.equalsIgnoreCase(schooLabelReportType)) {
                fileLocBuilder.append(EducDistributionApiConstants.TMP_DIR).append(DEL).append(batchId);
            } else if ("02".equalsIgnoreCase(schoolCategory)) {
                fileLocBuilder.append(EducDistributionApiConstants.TMP_DIR).append(DEL).append(batchId).append(DEL).append(mincode);
            } else {
                fileLocBuilder.append(EducDistributionApiConstants.TMP_DIR).append(DEL).append(batchId).append(DEL).append(districtCode).append(DEL).append(mincode);
            }
            Path path = Paths.get(fileLocBuilder.toString());
            Files.createDirectories(path);
            StringBuilder fileNameBuilder = new StringBuilder();
            if (isDistrict) {
                fileNameBuilder.append(EducDistributionApiConstants.TMP_DIR).append(DEL).append(batchId).append(DEL).append(districtCode);
            } else if (SCHOOL_LABELS_CODE.equalsIgnoreCase(mincode) || ADDRESS_LABEL_SCHL.equalsIgnoreCase(schooLabelReportType)) {
                fileNameBuilder.append(EducDistributionApiConstants.TMP_DIR).append(DEL).append(batchId);
            } else if ("02".equalsIgnoreCase(schoolCategory)) {
                fileNameBuilder.append(EducDistributionApiConstants.TMP_DIR).append(DEL).append(batchId).append(DEL).append(mincode);
            } else {
                fileNameBuilder.append(EducDistributionApiConstants.TMP_DIR).append(DEL).append(batchId).append(DEL).append(districtCode).append(DEL).append(mincode);
            }
            if (SCHOOL_LABELS_CODE.equalsIgnoreCase(mincode) || ADDRESS_LABEL_YE.equalsIgnoreCase(schooLabelReportType) || ADDRESS_LABEL_SCHL.equalsIgnoreCase(schooLabelReportType)) {
                fileNameBuilder.append("/EDGRAD.L.").append("3L14.");
            } else {
                fileNameBuilder.append("/EDGRAD.R.").append("324W.");
            }
            fileNameBuilder.append(EducDistributionApiUtils.getFileNameSchoolReports(mincode)).append(".pdf");
            try (OutputStream out = new FileOutputStream(fileNameBuilder.toString())) {
                out.write(gradReportPdf);
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
        }
    }

}
