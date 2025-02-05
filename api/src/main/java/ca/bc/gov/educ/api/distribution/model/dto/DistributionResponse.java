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
    private Long batchId;
    private String localDownload;
    private String activityCode;
    private String transmissionMode;
    private List<School> schools = new ArrayList<>();
    private List<School> districts = new ArrayList<>();
    private StudentSearchRequest studentSearchRequest;

    private List<String> districtSchools = new ArrayList<>();
}
