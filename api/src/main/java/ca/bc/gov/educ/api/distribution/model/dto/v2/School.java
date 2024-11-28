package ca.bc.gov.educ.api.distribution.model.dto.v2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class School {

	private String minCode;
	private String schoolId;
	private String schoolName;
	private String districtName;
	private String transcriptEligibility;
	private String certificateEligibility;
	private String address1;
	private String address2;
	private String city;
	private String provCode;
	private String countryCode;
	private String postal;
	private String openFlag;
	private String schoolCategoryCode;
	private String schoolCategoryLegacyCode;

	@JsonIgnore
	private String schlNo;
	@JsonIgnore
	private String distNo;

	@Override
	public String toString() {
		return "School [minCode=" + minCode + ", schoolId=" + schoolId + ", schoolCategoryCode=" + schoolCategoryCode + ", schoolCategoryLegacyCode=" + schoolCategoryLegacyCode
				+ ", schoolName=" + schoolName + ", districtName=" + districtName + ", transcriptEligibility=" + transcriptEligibility + ", certificateEligibility=" + certificateEligibility
				+ ", address1=" + address1 + ", address2=" + address2 + ", city=" + city + ", provCode=" + provCode + ", countryCode=" + countryCode + ", postal=" + postal + ", openFlag=" + openFlag
				+ "]";
	}
}
