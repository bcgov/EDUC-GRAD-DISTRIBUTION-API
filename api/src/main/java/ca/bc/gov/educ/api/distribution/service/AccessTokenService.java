package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.ProcessorData;
import ca.bc.gov.educ.api.distribution.model.dto.ResponseObj;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class AccessTokenService {

    private static Logger logger = LoggerFactory.getLogger(AccessTokenService.class);

    private final EducDistributionApiConstants constants;

    private final WebClient webClient;

    @Autowired
    public AccessTokenService(final EducDistributionApiConstants constants, final WebClient webClient) {
        this.constants = constants;
        this.webClient = webClient;
    }

    private ResponseObj getTokenResponseObject() {
        HttpHeaders httpHeadersKC = EducDistributionApiUtils.getHeaders(
                constants.getUserName(), constants.getPassword());
        MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
        map.add("grant_type", "client_credentials");
        return this.webClient.post().uri(constants.getTokenUrl())
                .headers(h -> h.addAll(httpHeadersKC))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(map))
                .retrieve()
                .bodyToMono(ResponseObj.class).block();
    }

    public void fetchAccessToken(ProcessorData data) {
        logger.info("Fetching the access token from KeyCloak API");
        ResponseObj res = getTokenResponseObject();
        if (res != null) {
            data.setAccessToken(res.getAccess_token());
            logger.info("Setting the new access token in summaryDTO.");
        }
    }


}
