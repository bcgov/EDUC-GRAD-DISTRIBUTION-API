package ca.bc.gov.educ.api.distribution.model.dto;

import lombok.Data;

@Data
public class DistributionPrintRequest {

	private TranscriptPrintRequest transcriptPrintRequest;
	private SchoolDistributionRequest schoolDistributionRequest;
	private CertificatePrintRequest yed2CertificatePrintRequest;
	private CertificatePrintRequest yedbCertificatePrintRequest;
	private CertificatePrintRequest yedrCertificatePrintRequest;
	private SchoolReportPostRequest schoolReportPostRequest;
	private TranscriptPrintPostingRequest transcriptPrintPostingRequest;
	private TVRReportPrintPostingRequest tvrReportPrintPostingRequest;
	private int total=0;
}
