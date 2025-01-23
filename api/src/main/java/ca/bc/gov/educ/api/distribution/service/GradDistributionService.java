package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.DistributionRequest;
import ca.bc.gov.educ.api.distribution.model.dto.DistributionResponse;
import ca.bc.gov.educ.api.distribution.model.dto.ProcessorData;
import ca.bc.gov.educ.api.distribution.process.DistributionProcess;
import ca.bc.gov.educ.api.distribution.process.DistributionProcessFactory;
import ca.bc.gov.educ.api.distribution.process.DistributionProcessType;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class GradDistributionService {

    private static Logger logger = LoggerFactory.getLogger(GradDistributionService.class);

    DistributionProcessFactory distributionProcessFactory;

    RestUtils restUtils;

    @Autowired
    public GradDistributionService(DistributionProcessFactory distributionProcessFactory, RestUtils restUtils) {
        this.distributionProcessFactory = distributionProcessFactory;
        this.restUtils = restUtils;
    }

    public DistributionResponse distributeCredentials(String runType, Long batchId, DistributionRequest distributionRequest, String activityCode, String transmissionMode,String localDownload, String accessToken) {
        ProcessorData data = ProcessorData.builder().batchId(batchId).accessToken(accessToken).distributionResponse(null).distributionRequest(distributionRequest).activityCode(activityCode).transmissionMode(transmissionMode).localDownload(localDownload).build();
        DistributionResponse disRes = processDistribution(runType,data);
        disRes.setMergeProcessResponse(disRes.getMergeProcessResponse());
        //Grad2-1931 setting the batchId and enabling local download for Users - mchintha
        disRes.setBatchId(data.getBatchId());
        disRes.setLocalDownload(data.getLocalDownload());
        return disRes;
    }

    private DistributionResponse processDistribution(String processType, ProcessorData data) {
        DistributionProcessType pType = DistributionProcessType.valueOf(processType);
        DistributionProcess process = distributionProcessFactory.createProcess(pType);
        //restUtils.fetchAccessToken(data);
        data = process.fire(data);
        return data.getDistributionResponse();
    }

    @Async("asyncExecutor")
    public void asyncDistributeCredentials(String runType, Long batchId, DistributionRequest distributionRequest, String activityCode, String transmissionMode, String localDownload, String accessToken) {
        ProcessorData data = ProcessorData.builder().batchId(batchId).accessToken(accessToken).distributionResponse(new DistributionResponse()).distributionRequest(distributionRequest).activityCode(activityCode).transmissionMode(transmissionMode).localDownload(localDownload).build();
        asyncProcessDistribution(runType, data);
    }

    private void asyncProcessDistribution(String processType, ProcessorData data) {
        String status;
        DistributionProcessType pType = DistributionProcessType.valueOf(processType);
        DistributionProcess process = distributionProcessFactory.createProcess(pType);

        try {
            restUtils.fetchAccessToken(data);
            data = process.fire(data);
            if (data.getDistributionResponse().getMergeProcessResponse().toLowerCase().contains("successful")) {
                status = "success";
            } else {
                status = "error";
            }
        } catch (Exception ex) {
            logger.error("Distribution Process - unexpected exception occurred: {}", ex.getLocalizedMessage());
            status = "error";
        }
        restUtils.fetchAccessToken(data);
        DistributionResponse response = data.getDistributionResponse();
        response.setJobStatus(status);
        restUtils.notifyDistributionJobIsCompleted(data);
        logger.info("Async distribution job is completed and notify it's status back to grad-batch-api: batchId [{}]", data.getBatchId());
    }

    //Grad2-1931 Changed the zipped folder path to fetch - mchintha
    public byte[] getDownload(Long batchId, String transmissionMode) {
        String localFile = null;
        if((transmissionMode != null) && (transmissionMode.equalsIgnoreCase(EducDistributionApiConstants.TRANSMISSION_MODE_FTP) || transmissionMode.equalsIgnoreCase(EducDistributionApiConstants.TRANSMISSION_MODE_PAPER))) {
            localFile = EducDistributionApiConstants.TMP_DIR + EducDistributionApiConstants.FILES_FOLDER_STRUCTURE + transmissionMode.toUpperCase() + "/EDGRAD.BATCH." + batchId + ".zip";
        }
        else {
            localFile = EducDistributionApiConstants.TMP_DIR + "/EDGRAD.BATCH." + batchId + ".zip";
        }
        Path path = Paths.get(localFile);
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            logger.debug("Error Message {}",e.getLocalizedMessage());
        }
        return new byte[0];
    }
}
