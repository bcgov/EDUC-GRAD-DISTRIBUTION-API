package ca.bc.gov.educ.api.distribution.model.dto;

import lombok.Data;

@Data
public class SchoolReportPostRequest {

	private Long batchId;
	private String psId;
	private Integer count;
	private SchoolReportDistribution gradReport;
	private SchoolReportDistribution nongradReport;
	private SchoolReportDistribution nongradprjreport;
}
