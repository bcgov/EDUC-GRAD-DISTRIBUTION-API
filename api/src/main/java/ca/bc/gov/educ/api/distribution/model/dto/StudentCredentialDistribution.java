package ca.bc.gov.educ.api.distribution.model.dto;

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
	private String schoolOfRecord;
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

	public LocalDateTime getLastUpdateDate() {
		return lastUpdateDate == null ? LocalDateTime.now() : lastUpdateDate;
	}
}
