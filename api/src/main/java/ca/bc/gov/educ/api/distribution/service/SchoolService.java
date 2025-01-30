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
import java.util.UUID;

import static ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants.*;

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

	public School getDefaultSchoolDetailsForPackingSlip(StudentSearchRequest searchRequest, String properName) {
		School commonSchool = new ca.bc.gov.educ.api.distribution.model.dto.v2.School();
		Address address = (searchRequest == null || searchRequest.getAddress() == null) ? null : searchRequest.getAddress();
		String userName = searchRequest == null ? null : searchRequest.getUser();
		commonSchool.setSchoolId(DEFAULT_SCHOOL_ID);
		commonSchool.setMinCode(DEFAULT_MINCODE);
		commonSchool.setSchoolName(ObjectUtils.defaultIfNull(properName, ObjectUtils.defaultIfNull(userName, "")));
		commonSchool.setAddress1(address == null ? DEFAULT_ADDRESS_LINE_1 : address.getStreetLine1());
		commonSchool.setAddress2(address == null ? DEFAULT_ADDRESS_LINE_2 : address.getStreetLine2());
		commonSchool.setCity(address == null ? DEFAULT_CITY : address.getCity());
		commonSchool.setProvCode(address == null ? DEFAULT_PROVINCE_CODE : address.getRegion());
		commonSchool.setPostal(address == null ? DEFAULT_POSTAL_CODE : address.getCode());
		commonSchool.setCountryCode(address == null ? DEFAULT_COUNTRY_CODE : address.getCountry());
		return commonSchool;
	}

	public School getSchool(UUID schoolId, ExceptionMessage exception) {
		School school = null;
		if(schoolId != null) {
			try {
				school = restService.executeGet(
								educDistributionApiConstants.getSchoolById(),
								School.class,
						schoolId.toString()
						);
			} catch (Exception e) {
				exception.setExceptionName("TRAX-API IS DOWN");
				exception.setExceptionDetails(e.getLocalizedMessage());
			}
		}
		return school;
	}

	public District getDistrict(UUID distId, ExceptionMessage exception) {
		District district = null;
		if(distId != null) {
			try {
				district = restService.executeGet(
						educDistributionApiConstants.getDistrictById(),
						District.class,
						distId.toString()
						);
			} catch (Exception e) {
				exception.setExceptionName("TRAX-API IS DOWN");
				exception.setExceptionDetails(e.getLocalizedMessage());
			}
		}
		return district;
	}

	public District getDistrictByDistrictNumber(String districtNumber, ExceptionMessage exception) {
		District district = null;
		if(StringUtils.isNotBlank(districtNumber)) {
			try {
				district = restService.executeGet(
						educDistributionApiConstants.getDistrictByDistrictNumber(),
						District.class,
						districtNumber
				);
			} catch (Exception e) {
				exception.setExceptionName("TRAX-API IS DOWN");
				exception.setExceptionDetails(e.getLocalizedMessage());
			}
		}
		return district;
	}

}
