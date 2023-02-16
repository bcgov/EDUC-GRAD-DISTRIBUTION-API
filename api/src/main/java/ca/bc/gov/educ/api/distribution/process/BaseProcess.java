package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.service.PsiService;
import ca.bc.gov.educ.api.distribution.service.ReportService;
import ca.bc.gov.educ.api.distribution.service.SchoolService;
import ca.bc.gov.educ.api.distribution.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipOutputStream;

public abstract class BaseProcess implements DistributionProcess{

    private static Logger logger = LoggerFactory.getLogger(BaseProcess.class);

    protected static final String LOC = "/tmp/";
    protected static final String DEL = "/";
    protected static final String EXCEPTION = "Error {} ";

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
            return schoolService.getSchoolDetails(mincode,processorData.getAccessToken(),exception);
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
        logger.info("Created Control file ");

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

}
