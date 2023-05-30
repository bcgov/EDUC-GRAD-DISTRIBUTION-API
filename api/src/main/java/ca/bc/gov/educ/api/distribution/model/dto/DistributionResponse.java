package ca.bc.gov.educ.api.distribution.model.dto;

import lombok.Data;

@Data
public class DistributionResponse {
    private String transcriptResponse;
    private String yed2Response;
    private String yedrResponse;
    private String yedbResponse;
    private String mergeProcessResponse;
    //Grad2-1931
    private String batchId;
    private String localDownload;
}
