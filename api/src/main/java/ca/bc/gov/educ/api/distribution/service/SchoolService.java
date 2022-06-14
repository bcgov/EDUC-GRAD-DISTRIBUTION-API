package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.CommonSchool;
import ca.bc.gov.educ.api.distribution.model.dto.ExceptionMessage;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class SchoolService {

	@Autowired
    WebClient webClient;
	
	@Autowired
	EducDistributionApiConstants educDistributionApiConstants;
	
	public CommonSchool getSchoolDetails(String mincode, String accessToken, ExceptionMessage exception) {
		try
		{
			return webClient.get().uri(String.format(educDistributionApiConstants.getSchoolDetails(),mincode)).headers(h -> h.setBearerAuth(accessToken)).retrieve().bodyToMono(CommonSchool.class).block();
		} catch (Exception e) {
			exception.setExceptionName("SCHOOL-API IS DOWN");
			exception.setExceptionDetails(e.getLocalizedMessage());
			return null;
		}
	}
}
