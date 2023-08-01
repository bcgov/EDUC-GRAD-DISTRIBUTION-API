package ca.bc.gov.educ.api.distribution.controller;

import ca.bc.gov.educ.api.distribution.model.dto.DistributionRequest;
import ca.bc.gov.educ.api.distribution.model.dto.DistributionResponse;
import ca.bc.gov.educ.api.distribution.service.GradDistributionService;
import ca.bc.gov.educ.api.distribution.service.PostingDistributionService;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.GradValidation;
import ca.bc.gov.educ.api.distribution.util.PermissionsConstants;
import ca.bc.gov.educ.api.distribution.util.RestUtils;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping(EducDistributionApiConstants.DISTRIBUTION_API_ROOT_MAPPING)
@OpenAPIDefinition(info = @Info(title = "API for Distributing Credentials to Schools.", description = "This API is for Distributing Credentials to Schools.", version = "1"), security = {@SecurityRequirement(name = "OAUTH2", scopes = {"GRAD_GRADUATE_STUDENT"})})
public class DistributionController {

    private static Logger logger = LoggerFactory.getLogger(DistributionController.class);

    private static final String CONTENT_DISPOSITION = "Content-Disposition";
    private static final String ZIP_FILE_NAME = "inline; filename=userdistributionbatch_%s.zip";

    @Autowired
    GradDistributionService gradDistributionService;

    @Autowired
    PostingDistributionService postingDistributionService;

    @Autowired
    GradValidation validation;

    @Autowired
    RestUtils response;

    @PostMapping(EducDistributionApiConstants.DISTRIBUTION_RUN)
    @PreAuthorize(PermissionsConstants.GRADUATE_STUDENT)
    @Operation(summary = "distribution run for Grad", description = "When triggered, run the distribution piece and send out credentials for printing", tags = { "Distribution" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<DistributionResponse> distributeCredentials(
            @PathVariable String runType, @RequestParam(required = false) Long batchId ,@RequestParam(required = false) String activityCode,
            @RequestParam(required = false) String transmissionType, @RequestBody DistributionRequest distributionRequest,
            @RequestParam(required = false) String localDownload, @RequestHeader(name="Authorization") String accessToken) {
        if (isAsyncDistribution(runType, activityCode)) {
            // non-blocking IO - launching async process to distribute credentials
            gradDistributionService.asyncDistributeCredentials(runType,batchId,distributionRequest,activityCode,transmissionType,localDownload,accessToken);

            // return as successful immediately
            DistributionResponse disRes = new DistributionResponse();
            disRes.setBatchId(batchId);
            disRes.setLocalDownload(localDownload);
            disRes.setMergeProcessResponse("Merge Successful and File Uploaded");
            return response.GET(disRes);
        } else {
            // blocking IO - launching sync process to distribute credentials
            return response.GET(gradDistributionService.distributeCredentials(runType,batchId,distributionRequest,activityCode,transmissionType,localDownload,accessToken));
        }
    }

    @GetMapping(EducDistributionApiConstants.LOCAL_DOWNLOAD)
    @PreAuthorize(PermissionsConstants.GRADUATE_STUDENT)
    @Operation(summary = "Read Student Reports by Student ID and Report Type", description = "Read Student Reports by Student ID and Report Type", tags = { "Reports" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<byte[]> downloadZipFile(@PathVariable(value = "batchId") Long batchId, @RequestParam(required = false) String transmissionMode) {
        logger.debug("downloadZipFile : ");
        byte[] resultBinary = gradDistributionService.getDownload(batchId, transmissionMode);
        byte[] encoded = Base64.encodeBase64(resultBinary);
        return handleBinaryResponse(encoded,MediaType.TEXT_PLAIN,batchId);
    }

    @PostMapping(EducDistributionApiConstants.POST_DISTRIBUTION)
    @PreAuthorize(PermissionsConstants.GRADUATE_STUDENT)
    @Operation(summary = "Read Student Reports by Student ID and Report Type", description = "Read Student Reports by Student ID and Report Type", tags = { "Reports" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<Boolean> postingDistribution(@RequestBody DistributionResponse distributionResponse) {
        logger.debug("zipBatchDirectory : ");
        return response.GET(postingDistributionService.postingProcess(distributionResponse));
    }

    private ResponseEntity<byte[]> handleBinaryResponse(byte[] resultBinary, MediaType contentType,Long batchId) {
        ResponseEntity<byte[]> responseEntity;

        if(resultBinary.length > 0) {
            HttpHeaders headers = new HttpHeaders();
            headers.add(CONTENT_DISPOSITION, String.format(ZIP_FILE_NAME,batchId));
            responseEntity = ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(contentType)
                    .body(resultBinary);
        } else {
            responseEntity = ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return responseEntity;
    }

    private boolean isAsyncDistribution(String runType, String activityCode) {
        return ( "MER".equals(runType) && ("MONTHLYDIST".equals(activityCode) || "NONGRADYERUN".equals(activityCode)) ) || "MERYER".equals(runType) || "MERSUPP".equals(runType);
    }
}
