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
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
public class GradDistributionService {

    private static Logger logger = LoggerFactory.getLogger(GradDistributionService.class);

    private static final String CONTENT_DISPOSITION = "Content-Disposition";
    private static final String ZIP_FILE_NAME = "inline; filename=userdistributionbatch_%s.zip";

    @Autowired
    DistributionProcessFactory distributionProcessFactory;

    @Autowired
    AccessTokenService accessTokenService;

    public DistributionResponse distributeCredentials(String runType, Long batchId, Map<String, DistributionPrintRequest> mapDist, String activityCode, String accessToken) {
        ProcessorData data = ProcessorData.builder().batchId(batchId).accessToken(accessToken).distributionResponse(null).mapDistribution(mapDist).activityCode(activityCode).build();
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

    public ResponseEntity<InputStreamResource> getDownload(Long batchId) {
        String localFile = "/tmp/EDGRAD.BATCH."+batchId+".zip";
        Path path = Paths.get(localFile);
        try {
            byte[] data = Files.readAllBytes(path);
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            HttpHeaders headers = new HttpHeaders();
            headers.add(CONTENT_DISPOSITION, String.format(ZIP_FILE_NAME,batchId));
            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(bis));
        } catch (IOException e) {
            logger.debug("Error Message {}",e.getLocalizedMessage());
        }
        return null;
    }
}
