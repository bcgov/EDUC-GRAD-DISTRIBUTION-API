package ca.bc.gov.educ.api.distribution.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DistributionRequest {
    private int totalCyclesCount;
    private int processedCyclesCount;
    private String activityCode;
    private List<School> schools;
    private Map<String, DistributionPrintRequest> mapDist;
    private StudentSearchRequest studentSearchRequest;
}
