package ca.bc.gov.educ.api.distribution.controller;

import ca.bc.gov.educ.api.distribution.model.dto.DistributionResponse;
import ca.bc.gov.educ.api.distribution.service.GradDistributionService;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.GradValidation;
import ca.bc.gov.educ.api.distribution.util.PermissionsContants;
import ca.bc.gov.educ.api.distribution.util.ResponseHelper;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping(EducDistributionApiConstants.DISTRIBUTION_API_ROOT_MAPPING)
@EnableResourceServer
@OpenAPIDefinition(info = @Info(title = "API for Distributing Credentials to Schools.", description = "This API is for Distributing Credentials to Schools.", version = "1"), security = {@SecurityRequirement(name = "OAUTH2", scopes = {"GRAD_GRADUATE_STUDENT"})})
public class DistributionController {

    private static Logger logger = LoggerFactory.getLogger(DistributionController.class);

    @Autowired
    GradDistributionService gradDistributionService;

    @Autowired
    GradValidation validation;

    @Autowired
    ResponseHelper response;

    @GetMapping(EducDistributionApiConstants.DISTRIBUTION_RUN)
    @PreAuthorize(PermissionsContants.GRADUATE_STUDENT)
    @Operation(summary = "distribution run for Grad", description = "When triggered, run the distribution piece and send out credentials for printing", tags = { "Distribution" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<DistributionResponse> distributeCredentials(@RequestParam(required = false) Long batchId) {
        OAuth2AuthenticationDetails auth = (OAuth2AuthenticationDetails) SecurityContextHolder.getContext().getAuthentication().getDetails();
        String accessToken = auth.getTokenValue();
        return response.GET(gradDistributionService.distributeCredentials(batchId,accessToken));
    }

}
