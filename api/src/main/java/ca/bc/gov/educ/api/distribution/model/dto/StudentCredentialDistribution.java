package ca.bc.gov.educ.api.distribution.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
	private LocalDate programCompletionDate;
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
}
