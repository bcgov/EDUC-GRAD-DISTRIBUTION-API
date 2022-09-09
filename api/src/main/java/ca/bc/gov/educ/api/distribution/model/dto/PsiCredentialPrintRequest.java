package ca.bc.gov.educ.api.distribution.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class PsiCredentialPrintRequest {

	private Long batchId;
	private String psId;
	private Integer count;
	private List<PsiCredentialDistribution> psiList;
}
