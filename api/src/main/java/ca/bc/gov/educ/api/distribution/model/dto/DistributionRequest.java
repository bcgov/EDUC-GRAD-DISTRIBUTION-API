package ca.bc.gov.educ.api.distribution.model.dto;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DistributionRequest {
    private String activityCode;
    private List<School> schools;
    private Map<String, DistributionPrintRequest> mapDist;
    private StudentSearchRequest studentSearchRequest;

    public StudentSearchRequest getStudentSearchRequest() {
        return ObjectUtils.defaultIfNull(studentSearchRequest, new StudentSearchRequest());
    }
}
