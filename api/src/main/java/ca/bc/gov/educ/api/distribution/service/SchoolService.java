package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.CommonSchool;
import ca.bc.gov.educ.api.distribution.model.dto.ExceptionMessage;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class SchoolService {

	private static Logger logger = LoggerFactory.getLogger(SchoolService.class);
	RestUtils restUtils;
    WebClient webClient;
	EducDistributionApiConstants educDistributionApiConstants;

	@Autowired
	public SchoolService(RestUtils restUtils, WebClient webClient, EducDistributionApiConstants educDistributionApiConstants) {
		this.restUtils = restUtils;
		this.webClient = webClient;
		this.educDistributionApiConstants = educDistributionApiConstants;
	}

	public CommonSchool getSchoolDetails(String mincode, String accessToken, ExceptionMessage exception) {
		try
		{
			return webClient.get().uri(String.format(educDistributionApiConstants.getSchoolDetails(),mincode)).headers(h -> h.setBearerAuth(restUtils.fetchAccessToken())).retrieve().bodyToMono(CommonSchool.class).block();
		} catch (Exception e) {
			exception.setExceptionName("SCHOOL-API IS DOWN");
			exception.setExceptionDetails(e.getLocalizedMessage());
			logger.error(exception.getExceptionName(), e);
			return null;
		}
	}

	public CommonSchool getDetailsForPackingSlip(String properName) {
		CommonSchool fakeSchoolObj = new CommonSchool();
		fakeSchoolObj.setSchlNo(String.format("%09d" , 0));
		fakeSchoolObj.setSchoolName(properName);
		fakeSchoolObj.setDistNo(String.format("%03d" , 0));
		fakeSchoolObj.setScAddressLine1("4TH FLOOR 620 SUPERIOR");
		fakeSchoolObj.setScAddressLine2("PO BOX 9886 STN PROV GOVT");
		fakeSchoolObj.setScCity("VICTORIA");
		fakeSchoolObj.setScProvinceCode("BC");
		fakeSchoolObj.setScPostalCode("V8W9T6");
		fakeSchoolObj.setScCountryCode("CN");
		return fakeSchoolObj;
	}
}
