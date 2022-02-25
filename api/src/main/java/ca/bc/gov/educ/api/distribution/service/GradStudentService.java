package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.GradSearchStudent;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class GradStudentService {

    private static final Logger logger = LoggerFactory.getLogger(GradStudentService.class);
    
    @Autowired WebClient webClient;
    @Autowired EducDistributionApiConstants constants;

    public GradSearchStudent getStudentData(String studentID, String accessToken) {

		GradSearchStudent result = webClient.get()
                .uri(String.format(constants.getStudentInfo(),studentID))
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(GradSearchStudent.class)
                .block();

		if(result != null)
        	logger.info("**** # of Graduation Record : {}",result.getStudentID());

        return result;
    }

}
