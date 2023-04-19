package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.ZipOutputStream;

public abstract class BaseProcess implements DistributionProcess {

    private static final Logger logger = LoggerFactory.getLogger(BaseProcess.class);

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
    SFTPUtils sftpUtils;

    @Autowired
    PsiService psiService;

    protected CommonSchool getBaseSchoolDetails(DistributionPrintRequest obj, String mincode, ExceptionMessage exception) {
        if (obj.getProperName() != null)
            return schoolService.getCommonSchoolDetailsForPackingSlip(obj.getProperName());
        else
            return schoolService.getCommonSchoolDetails(mincode, exception);
    }

    //Grad2-1931 Changed the folder structure of created files to be placed - mchintha
    protected void createZipFile(Long batchId, String transmissionMode) {
        StringBuilder sourceFileBuilder = null;
        File file ;
        if(!transmissionMode.isBlank() && (transmissionMode.equalsIgnoreCase(EducDistributionApiConstants.TRANSMISSION_MODE_FTP) || (transmissionMode.equalsIgnoreCase(EducDistributionApiConstants.TRANSMISSION_MODE_PAPER)))) {
            sourceFileBuilder = new StringBuilder().append(EducDistributionApiConstants.TMP_DIR).append(EducDistributionApiConstants.FILES_FOLDER_STRUCTURE).append(transmissionMode.toUpperCase()).append(EducDistributionApiConstants.DEL).append(batchId);
            file = new File(EducDistributionApiConstants.TMP_DIR + EducDistributionApiConstants.FILES_FOLDER_STRUCTURE + transmissionMode.toUpperCase() + "/EDGRAD.BATCH." + batchId + ".zip");
        }
        else {
            sourceFileBuilder = new StringBuilder().append(EducDistributionApiConstants.TMP_DIR).append(batchId);
            file = new File(EducDistributionApiConstants.TMP_DIR + "EDGRAD.BATCH." + batchId + ".zip");
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            File fileToZip = new File(sourceFileBuilder.toString());
            EducDistributionApiUtils.zipFile(fileToZip, fileToZip.getName(), zipOut);
        } catch (IOException e) {
            logger.debug(EXCEPTION, e.getLocalizedMessage());
        }
    }

    //Grad2-1931 Changed the folder structure of PSIRUN files to be placed - mchintha
    protected void createControlFile(Long batchId, String transmissionMode, int numberOfPdfs) {
        File file;
        if(transmissionMode.equalsIgnoreCase(EducDistributionApiConstants.TRANSMISSION_MODE_FTP) || transmissionMode.equalsIgnoreCase(EducDistributionApiConstants.TRANSMISSION_MODE_PAPER)) {
            file = new File("/tmp/Batch/PSI/" + transmissionMode.toUpperCase() + "/EDGRAD.BATCH." + batchId + ".txt");
        }
        else {
            file = new File("/tmp/EDGRAD.BATCH." + batchId + ".txt");
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] contentInBytes = String.valueOf(numberOfPdfs).getBytes();
            fos.write(contentInBytes);
            fos.flush();
        } catch (IOException e) {
            logger.debug(EXCEPTION, e.getLocalizedMessage());
        }
        logger.debug("Created Control file ");

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
        createZipFile(batchId, processorData.getTransmissionMode());
        if (processorData.getLocalDownload() == null || !processorData.getLocalDownload().equalsIgnoreCase("Y")) {
            createControlFile(batchId, processorData.getTransmissionMode(), numberOfPdfs);
            sftpUtils.sftpUploadBCMail(batchId);
        }
    }

    protected Integer createDistrictSchoolYearEndReport(String accessToken, String schooLabelReportType, String districtReportType, String schoolReportType) {
        Integer reportCount = 0;
        final UUID correlationID = UUID.randomUUID();
        reportCount += webClient.get().uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndReport(), schooLabelReportType, districtReportType, schoolReportType))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducDistributionApiConstants.CORRELATION_ID, correlationID.toString());
                })
                .retrieve().bodyToMono(Integer.class).block();
        return reportCount;
    }

    protected Integer createSchoolLabelsReport(List<School> schools, String accessToken, String schooLabelReportType) {
        Integer reportCount = 0;
        final UUID correlationID = UUID.randomUUID();
        reportCount += webClient.post().uri(String.format(educDistributionApiConstants.getSchoolLabelsReport(), schooLabelReportType))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducDistributionApiConstants.CORRELATION_ID, correlationID.toString());
                })
                .body(BodyInserters.fromValue(schools)).retrieve().bodyToMono(Integer.class).block();
        return reportCount;
    }

    protected Integer createDistrictSchoolMonthReport(String accessToken, String schooLabelReportType, String districtReportType, String schoolReportType) {
        Integer reportCount = 0;
        final UUID correlationID = UUID.randomUUID();
        reportCount += webClient.get().uri(String.format(educDistributionApiConstants.getSchoolDistrictMonthReport(), schooLabelReportType, districtReportType, schoolReportType))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducDistributionApiConstants.CORRELATION_ID, correlationID.toString());
                })
                .retrieve().bodyToMono(Integer.class).block();
        return reportCount;
    }

    protected Integer createDistrictSchoolSuppReport(String schooLabelReportType, String districtReportType, String schoolReportType) {
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

    protected int processDistrictSchoolDistribution(ProcessorData processorData, String schooLabelReportType, String districtReportType, String schoolReportType) {
        int numberOfPdfs = 0;
        List<String> mincodes = new ArrayList<>();
        if (processorData.getMapDistribution() != null && !processorData.getMapDistribution().isEmpty()) {
            mincodes.addAll(processorData.getMapDistribution().keySet());
        }
        if (StringUtils.isNotBlank(schooLabelReportType)) {
            String accessTokenSl = restUtils.getAccessToken();
            List<SchoolReports> yeSchooLabelsReports = webClient.get().uri(String.format(educDistributionApiConstants.getSchoolReportsByReportType(), schooLabelReportType, SCHOOL_LABELS_CODE))
                    .headers(h ->
                            h.setBearerAuth(accessTokenSl)
                    ).retrieve().bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
                    }).block();
            assert yeSchooLabelsReports != null;
            numberOfPdfs += processDistrictSchoolReports(yeSchooLabelsReports, processorData);
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
            numberOfPdfs += processDistrictSchoolReports(yeDistrictReports, processorData);
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
            numberOfPdfs += processDistrictSchoolReports(yeSchoolReports, processorData);
        }
        return numberOfPdfs;
    }

    protected int processDistrictSchoolReports(List<SchoolReports> schoolReports, ProcessorData processorData) {
        int numberOfPdfs = 0;
        String accessToken = restUtils.getAccessToken();
        for (SchoolReports report : schoolReports) {
            try {
                byte[] gradReportPdf = webClient.get().uri(String.format(educDistributionApiConstants.getSchoolReport(), report.getSchoolOfRecord(), report.getReportTypeCode())).headers(h -> h.setBearerAuth(accessToken)).retrieve().bodyToMono(byte[].class).block();
                if (gradReportPdf != null) {
                    logger.debug("*** Added PDFs Current Report Type {} for school {} category {}", report.getReportTypeCode(), report.getSchoolOfRecord(), report.getSchoolCategory());
                    uploadSchoolReportDocuments(
                            processorData.getBatchId(),
                            report.getSchoolOfRecord(),
                            report.getSchoolCategory(),
                            processorData.getTransmissionMode(),
                            gradReportPdf);
                    numberOfPdfs++;
                } else {
                    logger.debug("*** Failed to Add PDFs Current Report Type {} for school {} category {}", report.getReportTypeCode(), report.getSchoolOfRecord(), report.getSchoolCategory());
                }
            } catch (Exception e) {
                logger.debug(EXCEPTION, e.getLocalizedMessage());
            }
        }
        return numberOfPdfs;
    }

    //Uploads school report labels for all the batch runs
    protected void uploadSchoolReportDocuments(Long batchId, String mincode, String schoolCategory, String transmissionMode, byte[] gradReportPdf) {
        boolean isDistrict = StringUtils.isNotBlank(mincode) && StringUtils.length(mincode) == 3;
        String districtCode = StringUtils.substring(mincode, 0, 3);
        try {
            StringBuilder fileLocBuilder = new StringBuilder();
            if (SCHOOL_LABELS_CODE.equalsIgnoreCase(mincode)) {
                fileLocBuilder.append(EducDistributionApiConstants.TMP_DIR).append(batchId);
            } else if (isDistrict) {
                fileLocBuilder.append(EducDistributionApiConstants.TMP_DIR).append(batchId).append(EducDistributionApiConstants.DEL).append(districtCode);
            } else if ("02".equalsIgnoreCase(schoolCategory)) {
                fileLocBuilder.append(EducDistributionApiConstants.TMP_DIR).append(batchId).append(EducDistributionApiConstants.DEL).append(mincode);
            } else {
                fileLocBuilder.append(EducDistributionApiConstants.TMP_DIR).append(batchId).append(EducDistributionApiConstants.DEL).append(districtCode).append(EducDistributionApiConstants.DEL).append(mincode);
            }
            Path path = Paths.get(fileLocBuilder.toString());
            Files.createDirectories(path);
            StringBuilder fileNameBuilder = new StringBuilder();
            if (SCHOOL_LABELS_CODE.equalsIgnoreCase(mincode)) {
                fileNameBuilder.append(EducDistributionApiConstants.TMP_DIR).append(batchId);
            } else if (isDistrict) {
                fileNameBuilder.append(EducDistributionApiConstants.TMP_DIR).append(batchId).append(EducDistributionApiConstants.DEL).append(districtCode);
            } else if ("02".equalsIgnoreCase(schoolCategory)) {
                fileNameBuilder.append(EducDistributionApiConstants.TMP_DIR).append(batchId).append(EducDistributionApiConstants.DEL).append(mincode);
            } else {
                fileNameBuilder.append(EducDistributionApiConstants.TMP_DIR).append(batchId).append(EducDistributionApiConstants.DEL).append(districtCode).append(EducDistributionApiConstants.DEL).append(mincode);
            }
            if (SCHOOL_LABELS_CODE.equalsIgnoreCase(mincode)) {
                fileNameBuilder.append("/EDGRAD.L.").append("3L14.");
            } else {
                fileNameBuilder.append("/EDGRAD.R.").append("324W.");
            }
            fileNameBuilder.append(EducDistributionApiUtils.getFileNameSchoolReports(mincode)).append(".pdf");
                try (OutputStream out = new FileOutputStream(fileNameBuilder.toString())) {
                    out.write(gradReportPdf);
                }
        } catch (Exception e) {
            logger.debug(EXCEPTION, e.getLocalizedMessage());
        }
    }

    protected void mergeDocumentsPDFs(ProcessorData processorData, String mincode, String schoolCategoryCode, String fileName, String paperType, List<InputStream> locations) {
        StringBuilder filePathBuilder;
        try {
            File bufferDirectory = IOUtils.createTempDirectory(EducDistributionApiConstants.TMP_DIR, "buffer");
            if(!processorData.getTransmissionMode().isBlank() || processorData.getTransmissionMode() != null) {
                filePathBuilder = PSIReportProcess.createFolderStructureInTempDirectory(processorData, mincode, schoolCategoryCode);
            }
            else {
                filePathBuilder = createFolderStructureInTempDirectory(processorData, mincode, schoolCategoryCode);
            }
            PDFMergerUtility pdfMergerUtility = new PDFMergerUtility();
            //Naming the file with extension
            filePathBuilder.append(fileName).append(paperType).append(".").append(EducDistributionApiUtils.getFileNameSchoolReports(mincode)).append(".pdf");
            pdfMergerUtility.setDestinationFileName(filePathBuilder.toString());
            pdfMergerUtility.addSources(locations);
            MemoryUsageSetting memoryUsageSetting = MemoryUsageSetting.setupMixed(50000000)
                    .setTempDir(bufferDirectory);
            pdfMergerUtility.mergeDocuments(memoryUsageSetting);
            IOUtils.removeFileOrDirectory(bufferDirectory);
        } catch (Exception e) {
            logger.error(EXCEPTION, e.getLocalizedMessage());
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

    //Grad2-1931 : Creates folder structure in temp directory for all the batch runs - mchintha/
    public static StringBuilder createFolderStructureInTempDirectory(ProcessorData processorData, String minCode, String schoolCategoryCode) {
        String districtCode = StringUtils.substring(minCode, 0, 3);
        String activityCode = processorData.getActivityCode();
        StringBuilder directoryPathBuilder = new StringBuilder();
        StringBuilder filePathBuilder = new StringBuilder();
        Path path;
        try {
            Boolean conditionResult = (EducDistributionApiConstants.MONTHLYDIST.equalsIgnoreCase(activityCode) || "02".equalsIgnoreCase(schoolCategoryCode));

                if (Boolean.TRUE.equals(conditionResult)) {
                    directoryPathBuilder.append(EducDistributionApiConstants.TMP_DIR).append(processorData.getBatchId()).append(EducDistributionApiConstants.DEL).append(minCode).append(EducDistributionApiConstants.DEL);
                } else {
                    directoryPathBuilder.append(EducDistributionApiConstants.TMP_DIR).append(processorData.getBatchId()).append(EducDistributionApiConstants.DEL).append(districtCode).append(EducDistributionApiConstants.DEL).append(minCode);
                }
                path = Paths.get(directoryPathBuilder.toString());
                Files.createDirectories(path);

                if (Boolean.TRUE.equals(conditionResult)) {
                    filePathBuilder.append(EducDistributionApiConstants.TMP_DIR).append(processorData.getBatchId()).append(EducDistributionApiConstants.DEL).append(minCode);
                } else {
                    filePathBuilder.append(EducDistributionApiConstants.TMP_DIR).append(processorData.getBatchId()).append(EducDistributionApiConstants.DEL).append(districtCode).append(EducDistributionApiConstants.DEL).append(minCode);
                }


        } catch (Exception e) {
            logger.error(EXCEPTION, e.getLocalizedMessage());
        }

        return filePathBuilder;
    }

}
