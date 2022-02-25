package ca.bc.gov.educ.api.distribution.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PackingSlipRequest {
    String mincode;
    String paperType;
    int total;
    int currentSlip;
}
