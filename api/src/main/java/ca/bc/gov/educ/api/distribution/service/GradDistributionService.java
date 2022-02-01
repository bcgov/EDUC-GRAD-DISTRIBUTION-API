package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class GradDistributionService {

	@Autowired
    WebClient webClient;
	
	@Autowired
    EducDistributionApiConstants educDistributionApiConstants;

}
