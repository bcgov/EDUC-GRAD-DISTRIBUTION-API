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
    private String jobStatus;
    //Grad2-1931
    private Long batchId;
    private String localDownload;
    private int totalCyclesCount;
    private int processedCyclesCount;
    private String activityCode;
    private List<School> schools = new ArrayList<>();
    private List<School> districts = new ArrayList<>();
    private StudentSearchRequest studentSearchRequest;
}
