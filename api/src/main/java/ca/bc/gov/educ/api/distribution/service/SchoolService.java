package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.RestUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SchoolService {

	private static Logger logger = LoggerFactory.getLogger(SchoolService.class);
	RestUtils restUtils;
    RestService restService;
	EducDistributionApiConstants educDistributionApiConstants;

	@Autowired
	public SchoolService(RestUtils restUtils, RestService restService, EducDistributionApiConstants educDistributionApiConstants) {
		this.restUtils = restUtils;
		this.restService = restService;
		this.educDistributionApiConstants = educDistributionApiConstants;
	}

	public CommonSchool getCommonSchoolDetails(String mincode, ExceptionMessage exception) {
		CommonSchool commonSchool = null;
		try
		{
			commonSchool = restService.executeGet(
					educDistributionApiConstants.getCommonSchoolByMincode(),
					CommonSchool.class,
					mincode
			);
		} catch (Exception e) {
			exception.setExceptionName(EducDistributionApiConstants.TRAX_API_STATUS);
			exception.setExceptionDetails(e.getLocalizedMessage());
		}
		return commonSchool;
	}

	public CommonSchool getDefaultSchoolDetailsForPackingSlip(StudentSearchRequest searchRequest, String properName) {
		CommonSchool commonSchool = new CommonSchool();
		Address address = (searchRequest == null || searchRequest.getAddress() == null) ? null : searchRequest.getAddress();
		String userName = searchRequest == null ? null : searchRequest.getUser();
		commonSchool.setSchlNo(String.format("%09d" , 0));
		commonSchool.setSchoolName(ObjectUtils.defaultIfNull(properName, ObjectUtils.defaultIfNull(userName, "")));
		commonSchool.setDistNo(String.format("%03d" , 0));
		commonSchool.setScAddressLine1(address == null ? "4TH FLOOR 620 SUPERIOR" : address.getStreetLine1());
		commonSchool.setScAddressLine2(address == null ? "PO BOX 9886 STN PROV GOVT" : address.getStreetLine2());
		commonSchool.setScCity(address == null ? "VICTORIA" : address.getCity());
		commonSchool.setScProvinceCode(address == null ? "BC" : address.getRegion());
		commonSchool.setScPostalCode(address == null ? "V8W 9T6" : address.getCode());
		commonSchool.setScCountryCode(address == null ? "CN" : address.getCountry());
		return commonSchool;
	}

	public TraxSchool getTraxSchool(String minCode, ExceptionMessage exception) {
		TraxSchool traxSchool = null;
		if(!StringUtils.isBlank(minCode)) {
			try {
				traxSchool = restService.executeGet(
								educDistributionApiConstants.getTraxSchoolByMincode(),
								TraxSchool.class,
								minCode
						);
			} catch (Exception e) {
				exception.setExceptionName("TRAX-API IS DOWN");
				exception.setExceptionDetails(e.getLocalizedMessage());
			}
		}
		return traxSchool;
	}

	public TraxDistrict getTraxDistrict(String distCode, ExceptionMessage exception) {
		TraxDistrict traxDistrict = null;
		if(!StringUtils.isBlank(distCode)) {
			try {
				traxDistrict = restService.executeGet(
								educDistributionApiConstants.getTraxDistrictByDistcode(),
								TraxDistrict.class,
								distCode
						);
			} catch (Exception e) {
				exception.setExceptionName("TRAX-API IS DOWN");
				exception.setExceptionDetails(e.getLocalizedMessage());
			}
		}
		return traxDistrict;
	}
}
