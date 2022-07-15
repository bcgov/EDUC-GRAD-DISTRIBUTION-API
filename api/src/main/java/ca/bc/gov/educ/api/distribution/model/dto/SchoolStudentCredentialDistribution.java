package ca.bc.gov.educ.api.distribution.model.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class SchoolStudentCredentialDistribution {

	private UUID id;
	private String credentialTypeCode;
	private UUID studentID;
	private String schoolOfRecord;
	private String pen;
	private String documentStatusCode;
}
