package ca.bc.gov.educ.api.distribution.model.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class SchoolReportDistribution {

	private UUID id;
	private String reportTypeCode;
	private String schoolOfRecord;

	public SchoolReportDistribution(UUID id, String reportTypeCode, String schoolOfRecord) {
		this.id = id;
		this.reportTypeCode = reportTypeCode;
		this.schoolOfRecord = schoolOfRecord;
	}
}
