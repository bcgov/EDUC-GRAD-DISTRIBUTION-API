package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.ProcessorData;
import ca.bc.gov.educ.api.distribution.model.dto.School;
import ca.bc.gov.educ.api.distribution.model.dto.SchoolReports;
import ca.bc.gov.educ.api.distribution.model.dto.TraxDistrict;
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
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
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

    @Autowired
    public PostingDistributionService(SFTPUtils sftpUtils, RestUtils restUtils, WebClient webClient, EducDistributionApiConstants educDistributionApiConstants) {
        this.sftpUtils = sftpUtils;
        this.restUtils = restUtils;
        this.webClient = webClient;
        this.educDistributionApiConstants = educDistributionApiConstants;
    }

    public boolean postingProcess(Long batchId, String download, List<School> schools, String activityCode) {
        Integer numberOfPdfs = createSchoolLabelsReport(schools, ADDRESS_LABEL_SCHL);
        processSchoolLabelsDistribution(batchId, ADDRESS_LABEL_SCHL);
        processSchoolLabelsDistribution(batchId, ADDRESS_LABEL_YE);
        if(YEARENDDIST.equalsIgnoreCase(activityCode)) {
            createDistrictSchoolYearEndReport(null, DISTREP_YE_SD, DISTREP_YE_SC);
            processDistrictSchoolDistribution(batchId, null, DISTREP_YE_SD, DISTREP_YE_SC);
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
        StringBuilder sourceFileBuilder = new StringBuilder().append(EducDistributionApiConstants.TMP_DIR).append(batchId);
        try (FileOutputStream fos = new FileOutputStream(EducDistributionApiConstants.TMP_DIR + "EDGRAD.BATCH." + batchId + ".zip")) {
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            File fileToZip = new File(sourceFileBuilder.toString());
            EducDistributionApiUtils.zipFile(fileToZip, fileToZip.getName(), zipOut);
            zipOut.close();
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage());
        }
    }

    protected void createControlFile(Long batchId, int numberOfPdfs) {
        try (FileOutputStream fos = new FileOutputStream("/tmp/EDGRAD.BATCH." + batchId + ".txt")) {
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
        reportCount += webClient.get().uri(String.format(educDistributionApiConstants.getSchoolDistrictSupplementalReport(), schooLabelReportType, districtReportType, schoolReportType))
                .headers(h -> {
                    h.setBearerAuth(restUtils.getAccessToken());
                    h.set(EducDistributionApiConstants.CORRELATION_ID, correlationID.toString());
                })
                .retrieve().bodyToMono(Integer.class).block();
        return reportCount;
    }

    public Integer createDistrictSchoolMonthReport(String schooLabelReportType, String districtReportType, String schoolReportType) {
        Integer reportCount = 0;
        final UUID correlationID = UUID.randomUUID();
        reportCount += webClient.get().uri(String.format(educDistributionApiConstants.getSchoolDistrictMonthReport(), schooLabelReportType, districtReportType, schoolReportType))
                .headers(h -> {
                    h.setBearerAuth(restUtils.getAccessToken());
                    h.set(EducDistributionApiConstants.CORRELATION_ID, correlationID.toString());
                })
                .retrieve().bodyToMono(Integer.class).block();
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
        reportCount += webClient.post().uri(url)
                .headers(h -> {
                    h.setBearerAuth(restUtils.getAccessToken());
                    h.set(EducDistributionApiConstants.CORRELATION_ID, correlationID.toString());
                })
                .body(BodyInserters.fromValue(processSchools)).retrieve().bodyToMono(Integer.class).block();
        logger.debug("***** Number of distributed School Label Reports {} *****", reportCount);
        return reportCount;
    }

    public Integer createDistrictLabelsReport(List<TraxDistrict> schools, String districtLabelReportType) {
        Integer reportCount = 0;
        final UUID correlationID = UUID.randomUUID();
        reportCount += webClient.post().uri(String.format(educDistributionApiConstants.getSchoolLabelsReport(), districtLabelReportType))
                .headers(h -> {
                    h.setBearerAuth(restUtils.getAccessToken());
                    h.set(EducDistributionApiConstants.CORRELATION_ID, correlationID.toString());
                })
                .body(BodyInserters.fromValue(schools)).retrieve().bodyToMono(Integer.class).block();
        return reportCount;
    }

    public Integer createDistrictSchoolYearEndReport(String schooLabelReportType, String districtReportType, String schoolReportType) {
        Integer reportCount = 0;
        final UUID correlationID = UUID.randomUUID();
        reportCount += webClient.get().uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndReport(), schooLabelReportType, districtReportType, schoolReportType))
                .headers(h -> {
                    h.setBearerAuth(restUtils.getAccessToken());
                    h.set(EducDistributionApiConstants.CORRELATION_ID, correlationID.toString());
                })
                .retrieve().bodyToMono(Integer.class).block();
        return reportCount;
    }

    public int processSchoolLabelsDistribution(Long batchId, String schooLabelReportType) {
        String accessTokenSl = restUtils.getAccessToken();
        List<SchoolReports> yeSchooLabelsReports = webClient.get().uri(String.format(educDistributionApiConstants.getSchoolReportsByReportType(), schooLabelReportType, ""))
                .headers(h ->
                        h.setBearerAuth(accessTokenSl)
                ).retrieve().bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
                }).block();
        assert yeSchooLabelsReports != null;
        return processDistrictSchoolReports(yeSchooLabelsReports, batchId, schooLabelReportType);
    }

    public int processDistrictSchoolDistribution(ProcessorData processorData, String schooLabelReportType, String districtReportType, String schoolReportType) {
        int numberOfPdfs = 0;
        List<String> mincodes = new ArrayList<>();
        if (processorData.getMapDistribution() != null && !processorData.getMapDistribution().isEmpty()) {
            mincodes.addAll(processorData.getMapDistribution().keySet());
        }
        if (StringUtils.isNotBlank(schooLabelReportType)) {
            numberOfPdfs += processSchoolLabelsDistribution(processorData.getBatchId(), schooLabelReportType);
        }
        if (StringUtils.isNotBlank(districtReportType)) {
            String accessTokenSd = restUtils.getAccessToken();
            List<SchoolReports> yeDistrictReports = new ArrayList<>();
            if (mincodes.isEmpty()) {
                String districtReportUrl = String.format(educDistributionApiConstants.getSchoolReportsByReportType(), districtReportType);
                yeDistrictReports.addAll(Objects.requireNonNull(webClient.get().uri(districtReportUrl)
                        .headers(h ->
                                h.setBearerAuth(accessTokenSd)
                        ).retrieve().bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
                        }).block()));
            } else {
                for (String mincode : mincodes) {
                    String districtReportUrl = String.format(educDistributionApiConstants.getSchoolReportsByReportType(), districtReportType, mincode);
                    yeDistrictReports.addAll(Objects.requireNonNull(webClient.get().uri(districtReportUrl)
                            .headers(h ->
                                    h.setBearerAuth(accessTokenSd)
                            ).retrieve().bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
                            }).block()));
                }
            }

            assert yeDistrictReports != null;
            numberOfPdfs += processDistrictSchoolReports(yeDistrictReports, processorData.getBatchId(), schooLabelReportType);
        }
        if (StringUtils.isNotBlank(schoolReportType)) {
            String accessTokenSc = restUtils.getAccessToken();
            List<SchoolReports> yeSchoolReports = new ArrayList<>();
            if (mincodes.isEmpty()) {
                String schoolReportUrl = String.format(educDistributionApiConstants.getSchoolReportsByReportType(), schoolReportType);
                yeSchoolReports.addAll(Objects.requireNonNull(webClient.get().uri(schoolReportUrl)
                        .headers(
                                h -> h.setBearerAuth(accessTokenSc)
                        ).retrieve().bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
                        }).block()));
            } else {
                for (String mincode : mincodes) {
                    String schoolReportUrl = String.format(educDistributionApiConstants.getSchoolReportsByReportType(), schoolReportType, mincode);
                    yeSchoolReports.addAll(Objects.requireNonNull(webClient.get().uri(schoolReportUrl)
                            .headers(
                                    h -> h.setBearerAuth(accessTokenSc)
                            ).retrieve().bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
                            }).block()));
                }
            }
            assert yeSchoolReports != null;
            numberOfPdfs += processDistrictSchoolReports(yeSchoolReports, processorData.getBatchId(), schooLabelReportType);
        }
        return numberOfPdfs;
    }

    public int processDistrictSchoolDistribution(Long batchId, String schooLabelReportType, String districtReportType, String schoolReportType) {
        int numberOfPdfs = 0;
        if (StringUtils.isNotBlank(schooLabelReportType)) {
            numberOfPdfs += processSchoolLabelsDistribution(batchId, schooLabelReportType);
        }
        if (StringUtils.isNotBlank(districtReportType)) {
            String accessTokenSd = restUtils.getAccessToken();
            String districtReportUrl = String.format(educDistributionApiConstants.getSchoolReportsByReportType(), districtReportType);
            List<SchoolReports> yeDistrictReports = webClient.get().uri(districtReportUrl)
                    .headers(h ->
                            h.setBearerAuth(accessTokenSd)
                    ).retrieve().bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
                    }).block();

            assert yeDistrictReports != null;
            numberOfPdfs += processDistrictSchoolReports(yeDistrictReports, batchId, schooLabelReportType);
        }
        if (StringUtils.isNotBlank(schoolReportType)) {
            String accessTokenSc = restUtils.getAccessToken();
            String schoolReportUrl = String.format(educDistributionApiConstants.getSchoolReportsByReportType(), schoolReportType);
            List<SchoolReports> yeSchoolReports = webClient.get().uri(schoolReportUrl)
                    .headers(
                            h -> h.setBearerAuth(accessTokenSc)
                    ).retrieve().bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
                    }).block();
            assert yeSchoolReports != null;
            numberOfPdfs += processDistrictSchoolReports(yeSchoolReports, batchId, schooLabelReportType);
        }
        return numberOfPdfs;
    }

    protected int processDistrictSchoolReports(List<SchoolReports> schoolReports, Long batchId, String schooLabelReportType) {
        int numberOfPdfs = 0;
        String accessToken = restUtils.getAccessToken();
        for (SchoolReports report : schoolReports) {
            try {
                byte[] gradReportPdf = webClient.get().uri(String.format(educDistributionApiConstants.getSchoolReport(), report.getSchoolOfRecord(), report.getReportTypeCode())).headers(h -> h.setBearerAuth(accessToken)).retrieve().bodyToMono(byte[].class).block();
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
                fileLocBuilder.append(EducDistributionApiConstants.TMP_DIR).append(batchId).append(DEL).append(districtCode);
            } else if (SCHOOL_LABELS_CODE.equalsIgnoreCase(mincode) || ADDRESS_LABEL_SCHL.equalsIgnoreCase(schooLabelReportType)) {
                fileLocBuilder.append(EducDistributionApiConstants.TMP_DIR).append(batchId);
            } else if ("02".equalsIgnoreCase(schoolCategory)) {
                fileLocBuilder.append(EducDistributionApiConstants.TMP_DIR).append(batchId).append(DEL).append(mincode);
            } else {
                fileLocBuilder.append(EducDistributionApiConstants.TMP_DIR).append(batchId).append(DEL).append(districtCode).append(DEL).append(mincode);
            }
            Path path = Paths.get(fileLocBuilder.toString());
            Files.createDirectories(path);
            StringBuilder fileNameBuilder = new StringBuilder();
            if (isDistrict) {
                fileNameBuilder.append(EducDistributionApiConstants.TMP_DIR).append(batchId).append(DEL).append(districtCode);
            } else if (SCHOOL_LABELS_CODE.equalsIgnoreCase(mincode) || ADDRESS_LABEL_SCHL.equalsIgnoreCase(schooLabelReportType)) {
                fileNameBuilder.append(EducDistributionApiConstants.TMP_DIR).append(batchId);
            } else if ("02".equalsIgnoreCase(schoolCategory)) {
                fileNameBuilder.append(EducDistributionApiConstants.TMP_DIR).append(batchId).append(DEL).append(mincode);
            } else {
                fileNameBuilder.append(EducDistributionApiConstants.TMP_DIR).append(batchId).append(DEL).append(districtCode).append(DEL).append(mincode);
            }
            if (SCHOOL_LABELS_CODE.equalsIgnoreCase(mincode) || ADDRESS_LABEL_YE.equalsIgnoreCase(schooLabelReportType) || ADDRESS_LABEL_SCHL.equalsIgnoreCase(schooLabelReportType)) {
                fileNameBuilder.append("/EDGRAD.L.").append("3L14.");
            } else {
                fileNameBuilder.append("/EDGRAD.R.").append("324W.");
            }
            fileNameBuilder.append(EducDistributionApiUtils.getFileNameWithMincodeReports(mincode)).append(".pdf");
            try (OutputStream out = new FileOutputStream(fileNameBuilder.toString())) {
                out.write(gradReportPdf);
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
        }
    }

}
