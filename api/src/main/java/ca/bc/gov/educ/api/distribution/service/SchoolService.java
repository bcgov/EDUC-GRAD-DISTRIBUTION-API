package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.model.dto.v2.District;
import ca.bc.gov.educ.api.distribution.model.dto.v2.School;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.RestUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SchoolService {

	RestUtils restUtils;
    RestService restService;
	EducDistributionApiConstants educDistributionApiConstants;

	@Autowired
	public SchoolService(RestUtils restUtils, RestService restService, EducDistributionApiConstants educDistributionApiConstants) {
		this.restUtils = restUtils;
		this.restService = restService;
		this.educDistributionApiConstants = educDistributionApiConstants;
	}

	public ca.bc.gov.educ.api.distribution.model.dto.v2.School getDefaultSchoolDetailsForPackingSlip(StudentSearchRequest searchRequest, String properName) {
		School commonSchool = new ca.bc.gov.educ.api.distribution.model.dto.v2.School();
		Address address = (searchRequest == null || searchRequest.getAddress() == null) ? null : searchRequest.getAddress();
		String userName = searchRequest == null ? null : searchRequest.getUser();
		commonSchool.setMinCode(String.format("%09d" , 0));

		commonSchool.setSchoolName(ObjectUtils.defaultIfNull(properName, ObjectUtils.defaultIfNull(userName, "")));
		commonSchool.setAddress1(address == null ? "4TH FLOOR 620 SUPERIOR" : address.getStreetLine1());
		commonSchool.setAddress2(address == null ? "PO BOX 9886 STN PROV GOVT" : address.getStreetLine2());
		commonSchool.setCity(address == null ? "VICTORIA" : address.getCity());
		commonSchool.setProvCode(address == null ? "BC" : address.getRegion());
		commonSchool.setPostal(address == null ? "V8W 9T6" : address.getCode());
		commonSchool.setCountryCode(address == null ? "CN" : address.getCountry());
		return commonSchool;
	}

	public ca.bc.gov.educ.api.distribution.model.dto.v2.School getSchool(String minCode, ExceptionMessage exception) {
		ca.bc.gov.educ.api.distribution.model.dto.v2.School school = null;
		if(!StringUtils.isBlank(minCode)) {
			try {
				school = restService.executeGet(
								educDistributionApiConstants.getSchoolByMincode(),
								ca.bc.gov.educ.api.distribution.model.dto.v2.School.class,
								minCode
						);
			} catch (Exception e) {
				exception.setExceptionName("TRAX-API IS DOWN");
				exception.setExceptionDetails(e.getLocalizedMessage());
			}
		}
		return school;
	}

	public District getDistrict(String distCode, ExceptionMessage exception) {
		District district = null;
		if(!StringUtils.isBlank(distCode)) {
			try {
				district = restService.executeGet(
								educDistributionApiConstants.getDistrictByDistcode(),
								District.class,
								distCode
						);
			} catch (Exception e) {
				exception.setExceptionName("TRAX-API IS DOWN");
				exception.setExceptionDetails(e.getLocalizedMessage());
			}
		}
		return district;
	}
}
