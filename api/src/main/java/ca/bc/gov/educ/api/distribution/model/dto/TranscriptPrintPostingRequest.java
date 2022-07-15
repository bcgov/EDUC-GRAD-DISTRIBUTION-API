package ca.bc.gov.educ.api.distribution.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class TranscriptPrintPostingRequest {

	private Long batchId;
	private String psId;
	private Integer count;
	private List<SchoolStudentCredentialDistribution> transcriptList;
}
