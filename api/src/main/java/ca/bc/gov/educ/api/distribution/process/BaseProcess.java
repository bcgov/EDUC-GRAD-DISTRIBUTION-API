package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.service.*;
import ca.bc.gov.educ.api.distribution.util.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipOutputStream;

import static ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants.TMP_DIR;

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
    protected static final String ADDRESS_LABEL_PSI = "ADDRESS_LABEL_PSI";
    protected static final String DISTREP_SD = "DISTREP_SD";
    protected static final String DISTREP_SC = "DISTREP_SC";


    protected static final String NONGRADDISTREP_SC = "NONGRADDISTREP_SC";

    private static final String EDGRAD_BATCH = "/EDGRAD.BATCH.";

    @Autowired
    GradValidation validation;

    @Autowired
    RestService restService;

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

    protected void createZipFile(Long batchId) {
        StringBuilder sourceFileBuilder = new StringBuilder().append(TMP_DIR).append(DEL).append(batchId);
        try (FileOutputStream fos = new FileOutputStream(TMP_DIR + EDGRAD_BATCH + batchId + ".zip")) {
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            File fileToZip = new File(sourceFileBuilder.toString());
            EducDistributionApiUtils.zipFile(fileToZip, fileToZip.getName(), zipOut);
            zipOut.finish();
        } catch (IOException e) {
            logger.debug(EXCEPTION, e.getLocalizedMessage());
        }
    }

    protected void createControlFile(Long batchId, int numberOfPdfs) {
        try (FileOutputStream fos = new FileOutputStream(TMP_DIR + EDGRAD_BATCH + batchId + ".txt")) {
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
        postingProcess(batchId, processorData.getLocalDownload(), numberOfPdfs, TMP_DIR);
    }

    protected void postingProcess(Long batchId, ProcessorData processorData, Integer numberOfPdfs, String pathToZip) {
        postingProcess(batchId, processorData.getLocalDownload(), numberOfPdfs, pathToZip);
    }

    protected void postingProcess(Long batchId, String download, Integer numberOfPdfs, String pathToZip) {
        postingDistributionService.zipBatchDirectory(batchId, download, numberOfPdfs, pathToZip);
    }

    protected Integer createDistrictSchoolYearEndReport(String schooLabelReportType, String districtReportType, String schoolReportType) {
        return postingDistributionService.createDistrictSchoolYearEndReport(schooLabelReportType, districtReportType, schoolReportType);
    }

    protected Integer createSchoolLabelsReport(List<School> schools, String schooLabelReportType) {
        return postingDistributionService.createSchoolLabelsReport(schools, schooLabelReportType);
    }
    //Grad2-2052 - setting SFTP root folder location where it has to pick zip folders from, to send them to BC mail - mchintha
    protected String getRootPathForFilesStorage(ProcessorData data) {
        logger.debug("getZipFolderFromRootLocation {} transmission mode {}", TMP_DIR, StringUtils.trimToEmpty(data.getTransmissionMode()));
        return TMP_DIR;
    }

    protected Integer createDistrictLabelsReport(List<TraxDistrict> traxDistricts, String districtLabelReportType) {
        return postingDistributionService.createDistrictLabelsReport(traxDistricts, districtLabelReportType);
    }

    protected Integer createDistrictSchoolMonthReport(String schooLabelReportType, String districtReportType, String schoolReportType) {
        return postingDistributionService.createDistrictSchoolMonthReport(schooLabelReportType, districtReportType, schoolReportType);
    }

    protected Integer createDistrictSchoolSuppReport(String schooLabelReportType, String districtReportType, String schoolReportType) {
        return postingDistributionService.createDistrictSchoolSuppReport(schooLabelReportType, districtReportType, schoolReportType);
    }

    public int processSchoolLabelsDistribution(Long batchId, String schooLabelReportType, String transmissionMode) {
        return postingDistributionService.processSchoolLabelsDistribution(batchId, schooLabelReportType, transmissionMode);
    }

    protected int processDistrictSchoolDistribution(Long batchId, Collection<String> mincodes, String schooLabelReportType, String districtReportType, String schoolReportType, String transmissionMode) {
        return postingDistributionService.processDistrictSchoolDistribution(batchId, mincodes, schooLabelReportType, districtReportType, schoolReportType, transmissionMode);
    }

    /**
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
    }**/

    protected StringBuilder buildFileLocationPath(Long batchId, String mincode, String schoolCategory, boolean isDistrict, String districtCode) {
        StringBuilder fileLocBuilder = new StringBuilder();
        if (SCHOOL_LABELS_CODE.equalsIgnoreCase(mincode)) {
            fileLocBuilder.append(TMP_DIR).append(EducDistributionApiConstants.DEL).append(batchId);
        } else if (isDistrict) {
            fileLocBuilder.append(TMP_DIR).append(EducDistributionApiConstants.DEL).append(batchId).append(EducDistributionApiConstants.DEL).append(districtCode);
        } else if ("02".equalsIgnoreCase(schoolCategory)) {
            fileLocBuilder.append(TMP_DIR).append(EducDistributionApiConstants.DEL).append(batchId).append(EducDistributionApiConstants.DEL).append(mincode);
        } else {
            fileLocBuilder.append(TMP_DIR).append(EducDistributionApiConstants.DEL).append(batchId).append(EducDistributionApiConstants.DEL).append(districtCode).append(EducDistributionApiConstants.DEL).append(mincode);
        }
        return fileLocBuilder;
    }

    protected void mergeDocumentsPDFs(ProcessorData processorData, String mincode, String schoolCategoryCode, String fileName, String paperType, List<InputStream> locations) {
        File bufferDirectory = null;
        try {
            String transmissionMode = processorData.getTransmissionMode();
            String rootDirectory = StringUtils.isNotBlank(transmissionMode) ? TMP_DIR + EducDistributionApiConstants.FILES_FOLDER_STRUCTURE + StringUtils.upperCase(transmissionMode) : TMP_DIR;
            StringBuilder filePathBuilder = createFolderStructureInTempDirectory(rootDirectory, processorData, mincode, schoolCategoryCode);
            bufferDirectory = IOUtils.createTempDirectory(TMP_DIR, "buffer");
            PDFMergerUtility pdfMergerUtility = new PDFMergerUtility();
            //Naming the file with extension
            filePathBuilder.append(fileName).append(paperType).append(".").append(EducDistributionApiUtils.getFileNameSchoolReports(mincode)).append(".pdf");
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

    protected int addStudentTranscriptToLocations(String studentId, List<InputStream> locations) {
        int numberOfPdfs = 0;
        List<GradStudentTranscripts> studentTranscripts = restService.executeGet(educDistributionApiConstants.getTranscriptUsingStudentID(), new ParameterizedTypeReference<List<GradStudentTranscripts>>() {}, studentId);
        if(studentTranscripts != null && !studentTranscripts.isEmpty() ) {
            GradStudentTranscripts studentTranscript = studentTranscripts.get(0);
            byte[] transcriptPdf = Base64.decodeBase64(studentTranscript.getTranscript());
            if(transcriptPdf != null) {
                locations.add(new ByteArrayInputStream(transcriptPdf));
                numberOfPdfs = 1;
            }
        }
        return numberOfPdfs;
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
        School existSchool = schools.stream().filter(s->mincode.equalsIgnoreCase(s.getMincode())).findAny().orElse(null);
        if(existSchool != null) return;
        TraxSchool traxSchool = schoolService.getTraxSchool(mincode, exception);
        if (traxSchool != null) {
            School school = new School();
            school.setMincode(traxSchool.getMinCode());
            school.setName(traxSchool.getSchoolName());
            school.setTypeBanner("PRINCIPAL");
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

    protected void processDistrictsForLabels(List<School> schools, String distcode, ExceptionMessage exception) {
        School existSchool = schools.stream().filter(s->distcode.equalsIgnoreCase(s.getMincode())).findAny().orElse(null);
        if(existSchool != null) {
            logger.debug("District {} already exists in the district labels", existSchool.getMincode());
            return;
        }
        logger.debug("Acquiring new district {} from TRAX API", distcode);
        TraxDistrict traxDistrict = schoolService.getTraxDistrict(distcode, exception);
        if (traxDistrict != null) {
            School school = new School();
            school.setMincode(traxDistrict.getDistrictNumber());
            school.setName(traxDistrict.getSuperIntendent());
            school.setTypeBanner("SUPERINTENDANT");
            Address address = new Address();
            address.setStreetLine1(traxDistrict.getAddress1());
            address.setStreetLine2(traxDistrict.getAddress2());
            address.setCity(traxDistrict.getCity());
            address.setRegion(traxDistrict.getProvCode());
            address.setCountry(traxDistrict.getCountryCode());
            address.setCode(traxDistrict.getPostal());
            school.setAddress(address);
            schools.add(school);
            logger.debug("District {} has been added to the district labels", school.getMincode());
        }
    }

    //Grad2-1931 : Creates folder structure in temp directory for all the batch runs - mchintha/
    public StringBuilder createFolderStructureInTempDirectory(String rootDirectory, ProcessorData processorData, String minCode, String schoolCategoryCode) {
        String districtCode = StringUtils.substring(minCode, 0, 3);
        String activityCode = processorData.getActivityCode();
        StringBuilder directoryPathBuilder = new StringBuilder();
        StringBuilder filePathBuilder = new StringBuilder();
        Path path;
        try {
            Boolean conditionResult = (MONTHLYDIST.equalsIgnoreCase(activityCode) || SUPPDIST.equalsIgnoreCase(activityCode) || "02".equalsIgnoreCase(schoolCategoryCode));
            if (Boolean.TRUE.equals(conditionResult)) {
                directoryPathBuilder.append(rootDirectory).append(EducDistributionApiConstants.DEL).append(processorData.getBatchId()).append(EducDistributionApiConstants.DEL).append(minCode).append(EducDistributionApiConstants.DEL);
            } else {
                directoryPathBuilder.append(rootDirectory).append(EducDistributionApiConstants.DEL).append(processorData.getBatchId()).append(EducDistributionApiConstants.DEL).append(districtCode).append(EducDistributionApiConstants.DEL).append(minCode);
            }
            path = Paths.get(directoryPathBuilder.toString());
            Files.createDirectories(path);

            if (Boolean.TRUE.equals(conditionResult)) {
                filePathBuilder.append(rootDirectory).append(EducDistributionApiConstants.DEL).append(processorData.getBatchId()).append(EducDistributionApiConstants.DEL).append(minCode);
            } else {
                filePathBuilder.append(rootDirectory).append(EducDistributionApiConstants.DEL).append(processorData.getBatchId()).append(EducDistributionApiConstants.DEL).append(districtCode).append(EducDistributionApiConstants.DEL).append(minCode);
            }

        } catch (Exception e) {
            logger.error(EXCEPTION, e.getLocalizedMessage());
        }

        return filePathBuilder;
    }

}
