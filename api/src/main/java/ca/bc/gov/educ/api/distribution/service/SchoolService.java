package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.CommonSchool;
import ca.bc.gov.educ.api.distribution.model.dto.ExceptionMessage;
import ca.bc.gov.educ.api.distribution.model.dto.TraxDistrict;
import ca.bc.gov.educ.api.distribution.model.dto.TraxSchool;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.RestUtils;
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
			exception.setExceptionName("SCHOOL-API IS DOWN");
			exception.setExceptionDetails(e.getLocalizedMessage());
		}
		return commonSchool;
	}

	public CommonSchool getCommonSchoolDetailsForPackingSlip(String properName) {
		CommonSchool fakeSchoolObj = new CommonSchool();
		fakeSchoolObj.setSchlNo(String.format("%09d" , 0));
		fakeSchoolObj.setSchoolName(properName);
		fakeSchoolObj.setDistNo(String.format("%03d" , 0));
		fakeSchoolObj.setScAddressLine1("4TH FLOOR 620 SUPERIOR");
		fakeSchoolObj.setScAddressLine2("PO BOX 9886 STN PROV GOVT");
		fakeSchoolObj.setScCity("VICTORIA");
		fakeSchoolObj.setScProvinceCode("BC");
		fakeSchoolObj.setScPostalCode("V8W 9T6");
		fakeSchoolObj.setScCountryCode("CN");
		return fakeSchoolObj;
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
