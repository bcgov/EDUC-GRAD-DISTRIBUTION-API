package ca.bc.gov.educ.api.distribution.model.dto;

import ca.bc.gov.educ.api.distribution.util.GradLocalDateDeserializer;
import ca.bc.gov.educ.api.distribution.util.GradLocalDateSerializer;
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
	private String schoolOfRecord;
	private String documentStatusCode;

	private String pen;
	private String legalFirstName;
	private String legalMiddleNames;
	private String legalLastName;
	private String studentCitizenship;
	@JsonDeserialize(using = GradLocalDateDeserializer.class)
	@JsonSerialize(using = GradLocalDateSerializer.class)
	private LocalDate programCompletionDate;
	private LocalDateTime lastUpdateDate;
	private String honoursStanding;
	private String program;
	private String studentGrade;
	private List<GradRequirement> nonGradReasons;
}
