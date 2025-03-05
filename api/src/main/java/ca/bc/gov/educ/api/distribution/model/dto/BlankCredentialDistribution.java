package ca.bc.gov.educ.api.distribution.model.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class BlankCredentialDistribution {

	private String credentialTypeCode;
	private UUID schoolId;
	private int quantity;
	private String paperType;

}
