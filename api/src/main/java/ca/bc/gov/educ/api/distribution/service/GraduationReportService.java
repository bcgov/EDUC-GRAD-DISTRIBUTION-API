package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.ExceptionMessage;
import ca.bc.gov.educ.api.distribution.model.dto.StudentCredentialDistribution;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class GraduationReportService {

	@Autowired
    WebClient webClient;
	
	@Autowired
	EducDistributionApiConstants educDistributionApiConstants;
	
	public List<StudentCredentialDistribution> getTranscriptList(String accessToken, ExceptionMessage exception) {
		try
		{
			return webClient.get().uri(educDistributionApiConstants.getTranscriptDistributionList()).headers(h -> h.setBearerAuth(accessToken)).retrieve().bodyToMono(new ParameterizedTypeReference<List<StudentCredentialDistribution>>(){}).block();
		} catch (Exception e) {
			exception.setExceptionName("GRAD-GRADUATION-REPORT-API IS DOWN");
			exception.setExceptionDetails(e.getLocalizedMessage());
			return null;
		}
	}

	public List<StudentCredentialDistribution> getCertificateList(String accessToken, ExceptionMessage exception) {
		try
		{
			return webClient.get().uri(educDistributionApiConstants.getCertificateDistributionList()).headers(h -> h.setBearerAuth(accessToken)).retrieve().bodyToMono(new ParameterizedTypeReference<List<StudentCredentialDistribution>>(){}).block();
		} catch (Exception e) {
			exception.setExceptionName("GRAD-GRADUATION-REPORT-API IS DOWN");
			exception.setExceptionDetails(e.getLocalizedMessage());
			return null;
		}
	}

}
