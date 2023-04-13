package ca.bc.gov.educ.api.distribution.model.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DistributionResponse {
    private String transcriptResponse;
    private String yed2Response;
    private String yedrResponse;
    private String yedbResponse;
    private String mergeProcessResponse;
    private int numberOfPdfs;
    private List<School> schools = new ArrayList<>();
}
