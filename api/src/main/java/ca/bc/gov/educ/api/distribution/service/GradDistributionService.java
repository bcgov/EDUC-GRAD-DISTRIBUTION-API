package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.DistributionPrintRequest;
import ca.bc.gov.educ.api.distribution.model.dto.DistributionResponse;
import ca.bc.gov.educ.api.distribution.model.dto.ProcessorData;
import ca.bc.gov.educ.api.distribution.process.DistributionProcess;
import ca.bc.gov.educ.api.distribution.process.DistributionProcessFactory;
import ca.bc.gov.educ.api.distribution.process.DistributionProcessType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
public class GradDistributionService {

    private static Logger logger = LoggerFactory.getLogger(GradDistributionService.class);



    @Autowired
    DistributionProcessFactory distributionProcessFactory;

    @Autowired
    AccessTokenService accessTokenService;

    public DistributionResponse distributeCredentials(String runType, Long batchId, Map<String, DistributionPrintRequest> mapDist, String activityCode,String localDownload, String accessToken) {
        ProcessorData data = ProcessorData.builder().batchId(batchId).accessToken(accessToken).distributionResponse(null).mapDistribution(mapDist).activityCode(activityCode).localDownload(localDownload).build();
        DistributionResponse disRes = new DistributionResponse();
        disRes.setMergeProcessResponse(processDistribution(runType,data).getMergeProcessResponse());
        return disRes;
    }

    private DistributionResponse processDistribution(String processType, ProcessorData data) {
        DistributionProcessType pType = DistributionProcessType.valueOf(processType);
        DistributionProcess process = distributionProcessFactory.createProcess(pType);
        accessTokenService.fetchAccessToken(data);
        data = process.fire(data);
        return data.getDistributionResponse();
    }

    public byte[] getDownload(Long batchId) {
        String localFile = "/tmp/EDGRAD.BATCH."+batchId+".zip";
        Path path = Paths.get(localFile);
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            logger.debug("Error Message {}",e.getLocalizedMessage());
        }
        return new byte[0];
    }
}
