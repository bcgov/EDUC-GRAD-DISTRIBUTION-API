package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.ExceptionMessage;
import ca.bc.gov.educ.api.distribution.model.dto.Psi;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class PsiService {

	@Autowired
    WebClient webClient;
	
	@Autowired
	EducDistributionApiConstants educDistributionApiConstants;
	
	public Psi getPsiDetails(String psiCode, String accessToken, ExceptionMessage exception) {
		try
		{
			return webClient.get().uri(String.format(educDistributionApiConstants.getPsiDetails(),psiCode)).headers(h -> h.setBearerAuth(accessToken)).retrieve().bodyToMono(Psi.class).block();
		} catch (Exception e) {
			exception.setExceptionName("SCHOOL-API IS DOWN");
			exception.setExceptionDetails(e.getLocalizedMessage());
			return null;
		}
	}
}
