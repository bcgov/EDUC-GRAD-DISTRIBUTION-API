package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.constants.SchoolCategoryCodes;
import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.model.dto.v2.District;
import ca.bc.gov.educ.api.distribution.model.dto.v2.YearEndReportRequest;
import ca.bc.gov.educ.api.distribution.service.*;
import ca.bc.gov.educ.api.distribution.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static ca.bc.gov.educ.api.distribution.model.dto.ActivityCode.*;
import static ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants.*;

@Slf4j
public abstract class BaseProcess implements DistributionProcess {

    protected static final String EXCEPTION = "Error {} ";

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

    protected ca.bc.gov.educ.api.distribution.model.dto.v2.School getBaseSchoolDetails(
            DistributionPrintRequest distributionPrintRequest, StudentSearchRequest searchRequest, UUID schoolId, ExceptionMessage exception) {
        if (distributionPrintRequest != null &&
                (StringUtils.isNotBlank(distributionPrintRequest.getProperName()) || DEFAULT_SCHOOL_ID.equals(schoolId.toString())))
            return schoolService.getDefaultSchoolDetailsForPackingSlip(searchRequest, distributionPrintRequest.getProperName());
        else
            return schoolService.getSchool(schoolId, exception);
    }

    protected void setExtraDataForPackingSlip(ReportRequest packSlipReq, String paperType, int total, int quantity, int currentSlip, String orderType, Long batchId) {
        packSlipReq.getData().getPackingSlip().setTotal(total);
        packSlipReq.getData().getPackingSlip().setCurrent(currentSlip);
        packSlipReq.getData().getPackingSlip().setQuantity(quantity);
        packSlipReq.getData().getPackingSlip().getOrderType().getPackingSlipType().getPaperType().setCode(paperType);
        packSlipReq.getData().getPackingSlip().getOrderType().setName(orderType);
        packSlipReq.getData().getPackingSlip().setOrderNumber(batchId);
    }

    protected void setExtraDataForPackingSlip(ReportRequest packSlipReq, String paperType, String orderType, Long batchId) {
        packSlipReq.getData().getPackingSlip().getOrderType().getPackingSlipType().getPaperType().setCode(paperType);
        packSlipReq.getData().getPackingSlip().getOrderType().setName(orderType);
        packSlipReq.getData().getPackingSlip().setOrderNumber(batchId);
    }

    protected Boolean postingProcess(Long batchId, ProcessorData processorData, Integer numberOfPdfs) {
        return postingProcess(batchId, processorData.getLocalDownload(), numberOfPdfs, TMP_DIR);
    }

    protected Boolean postingProcess(Long batchId, ProcessorData processorData, Integer numberOfPdfs, String pathToZip) {
        return postingProcess(batchId, processorData.getLocalDownload(), numberOfPdfs, pathToZip);
    }

    protected Boolean postingProcess(Long batchId, String download, Integer numberOfPdfs, String pathToZip) {
        return postingDistributionService.zipBatchDirectory(batchId, download, numberOfPdfs, pathToZip);
    }

    protected Integer createSchoolLabelsReport(List<ca.bc.gov.educ.api.distribution.model.dto.School> schools, String schooLabelReportType) {
        return postingDistributionService.createSchoolLabelsReport(schools, schooLabelReportType);
    }

    //Grad2-2052 - setting SFTP root folder location where it has to pick zip folders from, to send them to BC mail - mchintha
    protected String getRootPathForFilesStorage(ProcessorData data) {
        log.debug("getZipFolderFromRootLocation {} transmission mode {}", TMP_DIR, StringUtils.trimToEmpty(data.getTransmissionMode()));
        return TMP_DIR;
    }

    protected int createDistrictSchoolYearEndReport(String schooLabelReportType, String districtReportType, String schoolReportType, YearEndReportRequest yearEndReportRequest) {
        return postingDistributionService.createDistrictSchoolYearEndReport(schooLabelReportType, districtReportType, schoolReportType, yearEndReportRequest);
    }

    public int processSchoolLabelsDistribution(Long batchId, String schooLabelReportType, String transmissionMode) {
        return postingDistributionService.processSchoolLabelsDistribution(batchId, schooLabelReportType, transmissionMode);
    }

    protected int processDistrictSchoolDistribution(Long batchId, List<String> schoolIds, List<String> districtIds,  String schooLabelReportType, String districtReportType, String schoolReportType, String transmissionMode) {
        return postingDistributionService.processDistrictSchoolDistribution(batchId, schoolIds, districtIds, schooLabelReportType, districtReportType, schoolReportType, transmissionMode);
    }

    protected void mergeDocumentsPDFs(ProcessorData processorData, String mincode, String schoolCategoryCode, String fileName,
                                      String paperType, List<InputStream> locations) {
        File bufferDirectory = null;
        try {
            String rootDirectory = StringUtils.isNotBlank(processorData.getTransmissionMode()) ?
                    TMP_DIR + EducDistributionApiConstants.FILES_FOLDER_STRUCTURE + StringUtils.upperCase(processorData.getTransmissionMode()) : TMP_DIR;
            StringBuilder filePathBuilder = createFolderStructureInTempDirectory(rootDirectory, processorData, mincode, schoolCategoryCode);
            bufferDirectory = IOUtils.createTempDirectory(TMP_DIR, "buffer");
            PDFMergerUtility pdfMergerUtility = new PDFMergerUtility();
            //Naming the file with extension
            filePathBuilder.append(fileName).append(paperType).append(".").append(EducDistributionApiUtils.getFileNameSchoolReports(mincode)).append(".pdf");
            pdfMergerUtility.setDestinationFileName(filePathBuilder.toString());
            MemoryUsageSetting memoryUsageSetting = MemoryUsageSetting.setupMixed(50000000)
                    .setTempDir(bufferDirectory);
            log.info("mergeDocumentsPDFs :: starting merge step step");
            List<InputStream> batch = new ArrayList<>();
            for (int i = 0; i < locations.size(); i++) {
              batch.add(locations.get(i));

              // Merge every 10 PDFs or at the last file
              if (batch.size() == 10 || i == locations.size() - 1) {
                log.info("Merging batch of {} PDFs", batch.size());
                pdfMergerUtility.addSources(batch);
                pdfMergerUtility.mergeDocuments(memoryUsageSetting);
                batch.clear(); // Clear the batch after merging
              }
            }
        } catch (Exception e) {
            log.error(EXCEPTION, e.getLocalizedMessage());
        } finally {
            if (bufferDirectory != null) {
                IOUtils.removeFileOrDirectory(bufferDirectory);
            }
        }
    }

    protected int addStudentTranscriptToLocations(String studentId, List<InputStream> locations) {
        int numberOfPdfs = 0;
        List<GradStudentTranscripts> studentTranscripts = restService.executeGet(educDistributionApiConstants.getTranscriptUsingStudentID(), new ParameterizedTypeReference<List<GradStudentTranscripts>>() {
        }, studentId);
        if (studentTranscripts != null && !studentTranscripts.isEmpty()) {
            GradStudentTranscripts studentTranscript = studentTranscripts.get(0);
            byte[] transcriptPdf = Base64.decodeBase64(studentTranscript.getTranscript());
            if (transcriptPdf != null) {
                locations.add(new ByteArrayInputStream(transcriptPdf));
                numberOfPdfs = 1;
            }
        }
        return numberOfPdfs;
    }

    protected void processSchoolsForLabels(List<ca.bc.gov.educ.api.distribution.model.dto.School> schools, Psi psi) {
        ca.bc.gov.educ.api.distribution.model.dto.School school = new ca.bc.gov.educ.api.distribution.model.dto.School();
        school.setSchoolId(DEFAULT_SCHOOL_ID);
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

    protected void processSchoolsForLabels(String recipient, List<ca.bc.gov.educ.api.distribution.model.dto.School> schools, ca.bc.gov.educ.api.distribution.model.dto.v2.School school) {
        ca.bc.gov.educ.api.distribution.model.dto.School traxSchool = new ca.bc.gov.educ.api.distribution.model.dto.School();
        traxSchool.setSchoolId(school.getSchoolId());
        traxSchool.setMincode(school.getMinCode());
        traxSchool.setName(school.getSchoolName());
        traxSchool.setTypeBanner(ObjectUtils.defaultIfNull(StringUtils.trimToNull(recipient), "PRINCIPAL"));
        Address address = new Address();
        address.setStreetLine1(school.getAddress1());
        address.setStreetLine2(school.getAddress2());
        address.setCity(school.getCity());
        address.setRegion(school.getProvCode());
        address.setCountry(school.getCountryCode());
        address.setCode(school.getPostal());
        traxSchool.setAddress(address);
        schools.add(traxSchool);
    }

    protected void processDistrictsForLabels(List<District> districts, String districtId, ExceptionMessage exception) {
        District existDistrict = districts.stream().filter(s -> districtId.equalsIgnoreCase(s.getDistrictId())).findAny().orElse(null);
        if (existDistrict != null) {
            log.debug("District {} already exists in the district labels", existDistrict.getDistrictNumber());
            return;
        }
        log.debug("Acquiring new district {} from TRAX API", districtId);
        District district = schoolService.getDistrict(UUID.fromString(districtId), exception);
        if (district != null) {
            districts.add(district);
            log.debug("District {} has been added to the district labels", district.getDistrictNumber());
        }
    }

    /**
     * Creates a folder structure in a temporary directory based on the provided parameters
     * and returns the file path as a StringBuilder.
     *
     * @param rootDirectory     The root directory where the folder structure will be created.
     * @param processorData     The processor data containing information like batch ID and activity code.
     * @param minCode           The ministry code, used for determining district and folder structure.
     * @param schoolCategoryCode The school category code, used to influence folder structure decisions.
     * @return A {@link StringBuilder} representing the path of the file or folder created.
     *
     * @throws NullPointerException if rootDirectory or processorData is null.
     * @throws IllegalArgumentException if minCode is invalid or empty.
     * @implNote
     * - It checks specific conditions based on `activityCode` and `schoolCategoryCode`
     *   to determine the directory structure.
     *
     * <p> The folder structure follows these rules:
     * <ul>
     *   <li>If `activityCode` matches certain constants or `schoolCategoryCode` is "02", "03", or "09",
     *       the path includes `minCode` as a subdirectory.</li>
     *   <li>Otherwise, the path includes `districtCode` (derived from the first 3 characters of `minCode`)
     *       and `minCode` as subdirectories.</li>
     * </ul>
     */
    public StringBuilder createFolderStructureInTempDirectory(
            String rootDirectory, ProcessorData processorData, String minCode, String schoolCategoryCode) {
        String districtCode = StringUtils.substring(minCode, 0, 3);
        String activityCode = processorData.getActivityCode();
        StringBuilder directoryPathBuilder = new StringBuilder();
        StringBuilder filePathBuilder = new StringBuilder();
        Path path;
        try {
            Boolean conditionResult = StringUtils.containsAnyIgnoreCase(activityCode, USERDIST.getValue(), USERDISTOC.getValue(),
                    USERDISTRC.getValue(), MONTHLYDIST.getValue(), SUPPDIST.getValue()) ||
                SchoolCategoryCodes.getSchoolTypesWithoutDistricts().contains(schoolCategoryCode);
            if (Boolean.TRUE.equals(conditionResult)) {
                directoryPathBuilder.append(rootDirectory).append(DEL)
                        .append(processorData.getBatchId()).append(DEL)
                        .append(minCode).append(DEL);
            } else {
                directoryPathBuilder.append(rootDirectory).append(DEL)
                        .append(processorData.getBatchId()).append(DEL)
                        .append(districtCode).append(DEL).append(minCode);
            }
            path = Paths.get(directoryPathBuilder.toString());
            Files.createDirectories(path);

            if (Boolean.TRUE.equals(conditionResult)) {
                filePathBuilder.append(rootDirectory).append(DEL)
                        .append(processorData.getBatchId()).append(DEL).append(minCode);
            } else {
                filePathBuilder.append(rootDirectory).append(DEL)
                        .append(processorData.getBatchId()).append(DEL)
                        .append(districtCode).append(DEL).append(minCode);
            }
        } catch (Exception e) {
            log.error(EXCEPTION, e.getLocalizedMessage());
        }
        return filePathBuilder;
    }
}
