package ca.bc.gov.educ.api.distribution.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Data
@Component
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessorData {
	private DistributionResponse distributionResponse;
	private String accessToken;
	private Long batchId;
	private String certificateProcessType;
	private String activityCode;
	private String localDownload;
	private List<StudentCredentialDistribution> transcriptList;
	private List<StudentCredentialDistribution> yed2List;
	private List<StudentCredentialDistribution> yedrList;
	private List<StudentCredentialDistribution> yedbList;
	private Map<String,DistributionPrintRequest> mapDistribution;
}
