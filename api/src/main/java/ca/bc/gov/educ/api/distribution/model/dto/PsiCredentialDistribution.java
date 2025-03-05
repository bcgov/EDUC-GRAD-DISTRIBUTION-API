package ca.bc.gov.educ.api.distribution.model.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class PsiCredentialDistribution {

	private String pen;
	private String psiCode;
	private UUID psiId;
	private String psiYear;
	private UUID studentID;
}
