package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.process.DistributionProcess;
import ca.bc.gov.educ.api.distribution.process.DistributionProcessFactory;
import ca.bc.gov.educ.api.distribution.process.DistributionProcessType;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GradDistributionService {

    private static Logger logger = LoggerFactory.getLogger(GradDistributionService.class);

	@Autowired
    WebClient webClient;

    @Autowired
    DistributionProcessFactory distributionProcessFactory;

    @Autowired
    GraduationReportService graduationReportService;
	
	@Autowired
    EducDistributionApiConstants educDistributionApiConstants;

    @Autowired
    AccessTokenService accessTokenService;

    public DistributionResponse distributeCredentials(Long batchId, String accessToken) {
        ExceptionMessage exception = new ExceptionMessage();
        ProcessorData data = ProcessorData.builder().batchId(batchId).accessToken(accessToken).distributionResponse(null).build();
        DistributionResponse disRes = new DistributionResponse();
        List<StudentCredentialDistribution> transcriptList = graduationReportService.getTranscriptList(accessToken,exception);
        if(!transcriptList.isEmpty()) {
            data.setTranscriptList(transcriptList);
            disRes.setTranscriptResponse(processDistribution("TPP", batchId, accessToken, data).getTranscriptResponse());
        }
        List<StudentCredentialDistribution> certificateList = graduationReportService.getCertificateList(accessToken,exception);
        if(!certificateList.isEmpty()) {
            List<StudentCredentialDistribution> yed2List = getCertList("YED2",certificateList);
            if(!yed2List.isEmpty()) {
                data.setYed2List(yed2List);
                data.setCertificateProcessType("YED2");
                disRes.setYed2Response(processDistribution("CPP",batchId,accessToken,data).getYed2Response());
            }

            List<StudentCredentialDistribution> yedbList = getCertList("YEDB",certificateList);
            if(!yedbList.isEmpty()) {
                data.setYedbList(yedbList);
                data.setCertificateProcessType("YEDB");
                disRes.setYedbResponse(processDistribution("CPP",batchId,accessToken,data).getYedbResponse());
            }

            List<StudentCredentialDistribution> yedrList = getCertList("YEDR",certificateList);
            if(!yedrList.isEmpty()) {
                data.setYedrList(yedrList);
                data.setCertificateProcessType("YEDR");
                disRes.setYedrResponse(processDistribution("CPP",batchId,accessToken,data).getYedrResponse());
            }
        }
        disRes.setMergeProcessResponse(processDistribution("MER",batchId,accessToken,data).getMergeProcessResponse());
        return disRes;
    }

    private List<StudentCredentialDistribution> getCertList(String code, List<StudentCredentialDistribution> certificateList) {
        return certificateList.stream().filter(scd->scd.getPaperType().compareTo(code)==0).collect(Collectors.toList());
    }

    private DistributionResponse processDistribution(String processType, Long batchId, String accessToken,ProcessorData data) {
        DistributionProcessType pType = DistributionProcessType.valueOf(processType);
        DistributionProcess process = distributionProcessFactory.createProcess(pType);
        accessTokenService.fetchAccessToken(data);
        process.setInputData(data);
        data = process.fire();
        return data.getDistributionResponse();
    }


}
