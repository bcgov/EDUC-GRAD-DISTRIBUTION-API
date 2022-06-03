package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.DistributionPrintRequest;
import ca.bc.gov.educ.api.distribution.model.dto.DistributionResponse;
import ca.bc.gov.educ.api.distribution.model.dto.ProcessorData;
import ca.bc.gov.educ.api.distribution.process.DistributionProcess;
import ca.bc.gov.educ.api.distribution.process.DistributionProcessFactory;
import ca.bc.gov.educ.api.distribution.process.DistributionProcessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class GradDistributionService {

    @Autowired
    DistributionProcessFactory distributionProcessFactory;

    @Autowired
    AccessTokenService accessTokenService;

    public DistributionResponse distributeCredentials(String runType, Long batchId, Map<String, DistributionPrintRequest> mapDist, String accessToken) {
        ProcessorData data = ProcessorData.builder().batchId(batchId).accessToken(accessToken).distributionResponse(null).mapDistribution(mapDist).build();
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
}
