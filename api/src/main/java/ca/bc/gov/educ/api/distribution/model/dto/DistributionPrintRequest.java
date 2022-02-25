package ca.bc.gov.educ.api.distribution.model.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class DistributionPrintRequest {

	private TranscriptPrintRequest transcriptPrintRequest;
	private CertificatePrintRequest yed2CertificatePrintRequest;
	private CertificatePrintRequest yedbCertificatePrintRequest;
	private CertificatePrintRequest yedrCertificatePrintRequest;
	private int total=0;
}
