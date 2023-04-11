package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.Psi;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.RestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class PsiService {

    WebClient webClient;
	EducDistributionApiConstants educDistributionApiConstants;
	RestUtils restUtils;

	@Autowired
	public PsiService(WebClient webClient, EducDistributionApiConstants educDistributionApiConstants, RestUtils restUtils) {
		this.webClient = webClient;
		this.educDistributionApiConstants = educDistributionApiConstants;
		this.restUtils = restUtils;
	}

	public Psi getPsiDetails(String psiCode, String accessToken) {
		return webClient.get().uri(String.format(educDistributionApiConstants.getPsiByPsiCode(),psiCode)).headers(h -> h.setBearerAuth(restUtils.fetchAccessToken())).retrieve().bodyToMono(Psi.class).block();
	}
}
