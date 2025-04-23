package ca.bc.gov.educ.api.distribution.model.dto;

import ca.bc.gov.educ.api.distribution.util.GradLocalDateDeserializer;
import ca.bc.gov.educ.api.distribution.util.GradLocalDateSerializer;
import ca.bc.gov.educ.api.distribution.util.GradLocalDateTimeDeserializer;
import ca.bc.gov.educ.api.distribution.util.GradLocalDateTimeSerializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class StudentCredentialDistribution {

	private UUID id;
	private String credentialTypeCode;
	private UUID studentID;
	private String paperType;
	private UUID schoolId;
	private String documentStatusCode;

	private String pen;
	private String legalFirstName;
	private String legalMiddleNames;
	private String legalLastName;
	private String studentCitizenship;
	@JsonSerialize(using = GradLocalDateSerializer.class)
	@JsonDeserialize(using = GradLocalDateDeserializer.class)
	private LocalDate programCompletionDate;
	@JsonSerialize(using = GradLocalDateTimeSerializer.class)
	@JsonDeserialize(using = GradLocalDateTimeDeserializer.class)
	private LocalDateTime lastUpdateDate;
	private String honoursStanding;
	private String program;
	private String studentGrade;
	private List<GradRequirement> nonGradReasons;

	@JsonIgnore
	private UUID schoolAtGradId;
	@JsonIgnore
	private UUID schoolOfRecordOriginId;
	@JsonIgnore
	private UUID districtId;
	@JsonIgnore
	private String schoolOfRecord; // minCode is required for the print file name

	public LocalDateTime getLastUpdateDate() {
		return lastUpdateDate == null ? LocalDateTime.now() : lastUpdateDate;
	}
	//Start of YE specific properties
	private String reportingSchoolTypeCode;
	private String transcriptTypeCode;
	private String certificateTypeCode;
	//End of YE specific properties
}
