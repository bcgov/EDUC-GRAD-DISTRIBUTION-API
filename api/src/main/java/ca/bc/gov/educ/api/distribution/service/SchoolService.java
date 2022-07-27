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

	public CommonSchool getDetailsForPackingSlip(String properName) {
		CommonSchool fakeSchoolObj = new CommonSchool();
		fakeSchoolObj.setSchlNo(String.format("%09d" , 0));
		fakeSchoolObj.setSchoolName(properName);
		fakeSchoolObj.setDistNo(String.format("%03d" , 0));
		fakeSchoolObj.setPhysAddressLine1("4TH FLOOR 620 SUPERIOR");
		fakeSchoolObj.setPhysAddressLine2("PO BOX 9886 STN PROV GOVT");
		fakeSchoolObj.setPhysCity("VICTORIA");
		fakeSchoolObj.setPhysProvinceCode("BC");
		fakeSchoolObj.setScPostalCode("V8W9T6");
		return fakeSchoolObj;
	}
}
