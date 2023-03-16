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
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipOutputStream;

public abstract class BaseProcess implements DistributionProcess{

    private static final Logger logger = LoggerFactory.getLogger(BaseProcess.class);

    protected static final String LOC = "/tmp/";
    protected static final String DEL = "/";
    protected static final String EXCEPTION = "Error {} ";
    protected static final String SCHOOL_LABELS_CODE = "000000000";

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

    protected CommonSchool getBaseSchoolDetails(DistributionPrintRequest obj, String mincode, ProcessorData processorData, ExceptionMessage exception) {
        if(obj.getProperName() != null)
            return schoolService.getDetailsForPackingSlip(obj.getProperName());
        else
            return schoolService.getSchoolDetails(mincode,restUtils.getAccessToken(),exception);
    }

    protected void createZipFile(Long batchId) {
        StringBuilder sourceFileBuilder = new StringBuilder().append(LOC).append(batchId);
        try(FileOutputStream fos = new FileOutputStream(LOC+"EDGRAD.BATCH." + batchId + ".zip")) {
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            File fileToZip = new File(sourceFileBuilder.toString());
            EducDistributionApiUtils.zipFile(fileToZip, fileToZip.getName(), zipOut);
            zipOut.close();
        } catch (IOException e) {
            logger.debug(EXCEPTION,e.getLocalizedMessage());
        }

    }

    protected void createControlFile(Long batchId,int numberOfPdfs) {
        try(FileOutputStream fos = new FileOutputStream("/tmp/EDGRAD.BATCH." + batchId + ".txt")) {
            byte[] contentInBytes = String.valueOf(numberOfPdfs).getBytes();
            fos.write(contentInBytes);
            fos.flush();
        } catch (IOException e) {
            logger.debug(EXCEPTION,e.getLocalizedMessage());
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

    protected void postingProcess(Long batchId,ProcessorData processorData,Integer numberOfPdfs) {
        createZipFile(batchId);
        if(processorData.getLocalDownload() == null || !processorData.getLocalDownload().equalsIgnoreCase("Y")) {
            createControlFile(batchId, numberOfPdfs);
            sftpUtils.sftpUploadBCMail(batchId);
        }
    }

    protected Integer createDistrictSchoolYearEndReport(String accessToken, String schooLabelReportType, String districtReportType, String schoolReportType) {
        Integer reportCount = 0;
        final UUID correlationID = UUID.randomUUID();
        reportCount += webClient.get().uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndReport(),schooLabelReportType,districtReportType,schoolReportType))
                .headers(h -> { h.setBearerAuth(accessToken); h.set(EducDistributionApiConstants.CORRELATION_ID, correlationID.toString()); })
                .retrieve().bodyToMono(Integer.class).block();
        return reportCount;
    }

    protected Integer createSchoolLabelsReport(List<School> schools, String accessToken, String schooLabelReportType) {
        Integer reportCount = 0;
        final UUID correlationID = UUID.randomUUID();
        reportCount += webClient.post().uri(String.format(educDistributionApiConstants.getSchoolLabelsReport(),schooLabelReportType))
                .headers(h -> { h.setBearerAuth(accessToken); h.set(EducDistributionApiConstants.CORRELATION_ID, correlationID.toString()); })
                .body(BodyInserters.fromValue(schools)).retrieve().bodyToMono(Integer.class).block();
        return reportCount;
    }

    protected Integer createDistrictSchoolMonthReport(String accessToken, String schooLabelReportType, String districtReportType, String schoolReportType) {
        Integer reportCount = 0;
        final UUID correlationID = UUID.randomUUID();
        reportCount += webClient.get().uri(String.format(educDistributionApiConstants.getSchoolDistrictMonthReport(),schooLabelReportType,districtReportType,schoolReportType))
                .headers(h -> { h.setBearerAuth(accessToken); h.set(EducDistributionApiConstants.CORRELATION_ID, correlationID.toString()); })
                .retrieve().bodyToMono(Integer.class).block();
        return reportCount;
    }

    protected int processDistrictSchoolDistribution(ProcessorData processorData, String schooLabelReportType, String districtReportType, String schoolReportType) {
        int numberOfPdfs = 0;
        if(StringUtils.isNotBlank(schooLabelReportType)) {
            String accessTokenSl = restUtils.getAccessToken();
            List<SchoolReports> yeSchooLabelsReports = webClient.get().uri(String.format(educDistributionApiConstants.getSchoolReportsByReportType(), schooLabelReportType))
                    .headers(h ->
                            h.setBearerAuth(accessTokenSl)
                    ).retrieve().bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
                    }).block();
            assert yeSchooLabelsReports != null;
            numberOfPdfs += processDistrictSchoolReports(yeSchooLabelsReports, processorData, accessTokenSl);
        }
        if(StringUtils.isNotBlank(districtReportType)) {
            String accessTokenSd = restUtils.getAccessToken();
            List<SchoolReports> yeDistrictReports = webClient.get().uri(String.format(educDistributionApiConstants.getSchoolReportsByReportType(), districtReportType))
                    .headers(h ->
                            h.setBearerAuth(accessTokenSd)
                    ).retrieve().bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
                    }).block();
            assert yeDistrictReports != null;
            numberOfPdfs += processDistrictSchoolReports(yeDistrictReports, processorData, accessTokenSd);
        }
        if(StringUtils.isNotBlank(schoolReportType)) {
            String accessTokenSc = restUtils.getAccessToken();
            List<SchoolReports> yeSchoolReports = webClient.get().uri(String.format(educDistributionApiConstants.getSchoolReportsByReportType(), schoolReportType))
                    .headers(
                            h -> h.setBearerAuth(accessTokenSc)
                    ).retrieve().bodyToMono(new ParameterizedTypeReference<List<SchoolReports>>() {
                    }).block();
            assert yeSchoolReports != null;
            numberOfPdfs += processDistrictSchoolReports(yeSchoolReports, processorData, accessTokenSc);
        }
        return numberOfPdfs;
    }

    protected int processDistrictSchoolReports(List<SchoolReports> schoolReports, ProcessorData processorData, String accessToken) {
        int numberOfPdfs = 0;
        for (SchoolReports report : schoolReports) {
            try {
                byte[] gradReportPdf = webClient.get().uri(String.format(educDistributionApiConstants.getSchoolReport(), report.getSchoolOfRecord(), report.getReportTypeCode())).headers(h -> h.setBearerAuth(accessToken)).retrieve().bodyToMono(byte[].class).block();
                if (gradReportPdf != null) {
                    logger.debug("*** Added PDFs Current Report Type {}", report.getReportTypeCode());
                    uploadSchoolReportDocuments(
                            processorData.getBatchId(),
                            report.getSchoolOfRecord(),
                            report.getSchoolCategory(),
                            gradReportPdf);
                    numberOfPdfs++;
                } else {
                    logger.debug("*** Failed to Add PDFs Current Report Type {}", report.getReportTypeCode());
                }
            } catch (Exception e) {
                logger.debug(EXCEPTION, e.getLocalizedMessage());
            }
        }
        return numberOfPdfs;
    }

    protected void uploadSchoolReportDocuments(Long batchId, String mincode, String schoolCategory, byte[] gradReportPdf) {
        boolean isDistrict = StringUtils.isNotBlank(mincode) && StringUtils.length(mincode) == 3;
        String districtCode = StringUtils.substring(mincode, 0, 3);
        try {
            StringBuilder fileLocBuilder = new StringBuilder();
            if(SCHOOL_LABELS_CODE.equalsIgnoreCase(mincode)) {
                fileLocBuilder.append(LOC).append(batchId);
            } else if(isDistrict) {
                fileLocBuilder.append(LOC).append(batchId).append(DEL).append(districtCode);
            } else if("02".equalsIgnoreCase(schoolCategory)) {
                fileLocBuilder.append(LOC).append(batchId).append(DEL).append(mincode);
            } else {
                fileLocBuilder.append(LOC).append(batchId).append(DEL).append(districtCode).append(DEL).append(mincode);
            }
            Path path = Paths.get(fileLocBuilder.toString());
            Files.createDirectories(path);
            StringBuilder fileNameBuilder = new StringBuilder();
            if(SCHOOL_LABELS_CODE.equalsIgnoreCase(mincode)) {
                fileNameBuilder.append(LOC).append(batchId);
            } else if(isDistrict) {
                fileNameBuilder.append(LOC).append(batchId).append(DEL).append(districtCode);
            } else if("02".equalsIgnoreCase(schoolCategory)) {
                fileNameBuilder.append(LOC).append(batchId).append(DEL).append(mincode);
            } else {
                fileNameBuilder.append(LOC).append(batchId).append(DEL).append(districtCode).append(DEL).append(mincode);
            }
            if(SCHOOL_LABELS_CODE.equalsIgnoreCase(mincode)) {
                fileNameBuilder.append("/EDGRAD.L.");
            } else {
                fileNameBuilder.append("/EDGRAD.R.").append("324W.");
            }
            fileNameBuilder.append(EducDistributionApiUtils.getFileName()).append(".pdf");
            try (OutputStream out = new FileOutputStream(fileNameBuilder.toString())) {
                        out.write(gradReportPdf);
            }
        } catch (Exception e) {
            logger.debug(EXCEPTION,e.getLocalizedMessage());
        }
    }

    protected void mergeDocuments(ProcessorData processorData, String code, String fileName, String paperType, List<InputStream> locations) {
        try {
            PDFMergerUtility objs = new PDFMergerUtility();
            StringBuilder pBuilder = new StringBuilder();
            pBuilder.append(LOC).append(processorData.getBatchId()).append(DEL).append(code).append(DEL);
            Path path = Paths.get(pBuilder.toString());
            Files.createDirectories(path);
            pBuilder = new StringBuilder();
            pBuilder.append(LOC).append(processorData.getBatchId()).append(DEL).append(code).append(fileName).append(paperType).append(".").append(EducDistributionApiUtils.getFileName()).append(".pdf");
            objs.setDestinationFileName(pBuilder.toString());
            objs.addSources(locations);
            objs.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
        }catch (Exception e) {
            logger.debug(EXCEPTION,e.getLocalizedMessage());
        }
    }

}
