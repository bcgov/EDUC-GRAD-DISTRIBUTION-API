package ca.bc.gov.educ.api.distribution.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ReportOptions implements Serializable {

	private boolean cacheReport;
	private String convertTo;
	private boolean overwrite;
	private String reportName;
	private String reportFile;

	public ReportOptions() {
	}

	public ReportOptions(String reportName) {
		switch(reportName) {
			case "achievement":
				this.cacheReport = false;
				this.convertTo = "pdf";
				this.overwrite = true;
				this.reportFile = "studentAchievementReport.pdf";
				break;
			case "transcript":
				this.cacheReport = false;
				this.convertTo = "pdf";
				this.overwrite = true;
				this.reportFile = "studentTranscriptReport.pdf";
				break;
			case "certificate":
				this.cacheReport = false;
				this.convertTo = "pdf";
				this.overwrite = true;
				this.reportFile = "studentCertificate.pdf";
				break;
			default:
				throw new RuntimeException("Unknown Report");
		}
	}
}
