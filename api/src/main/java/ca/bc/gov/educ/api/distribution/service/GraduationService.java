package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.ReportData;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.RestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class GraduationService {

    WebClient webClient;
    EducDistributionApiConstants educDistributionApiConstants;
    RestUtils restUtils;

    @Autowired
    public GraduationService(WebClient webClient, EducDistributionApiConstants educDistributionApiConstants, RestUtils restUtils) {
        this.webClient = webClient;
        this.educDistributionApiConstants = educDistributionApiConstants;
        this.restUtils = restUtils;
    }

    public ReportData getReportData(String pen) {
        return webClient.get().uri(String.format(educDistributionApiConstants.getTranscriptCSVData(), pen)).headers(h -> h.setBearerAuth(restUtils.fetchAccessToken())).retrieve().bodyToMono(ReportData.class).block();
    }

}
