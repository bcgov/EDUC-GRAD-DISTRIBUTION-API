package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.exception.ServiceException;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.UUID;

@Service
public class RestService {

    private static Logger logger = LoggerFactory.getLogger(RestService.class);

    private static final String REST_SERVICE_ERROR = "Unable to call rest service {} method of {} with params {}";

    final WebClient webClient;

    final RestUtils restUtils;

    @Autowired
    public RestService(WebClient webClient, RestUtils restUtils) {
        this.webClient = webClient;
        this.restUtils = restUtils;
    }

    public <T> T executeGet(String url, Class<T> boundClass, String... params) {
        String serviceUrl = null;
        try {
            serviceUrl = parseUrlParameters(url, params);
            final UUID correlationId = UUID.randomUUID();
            return webClient.get().uri(serviceUrl).headers(h -> {
                h.setBearerAuth(restUtils.getAccessToken());
                h.set(EducDistributionApiConstants.CORRELATION_ID, correlationId.toString());
            }).retrieve().bodyToMono(boundClass).block();
        } catch(Exception e) {
            logger.error(REST_SERVICE_ERROR, "GET", serviceUrl, Arrays.toString(params));
            throw new ServiceException(getErrorMessage(url, e.getLocalizedMessage()), HttpStatus.SERVICE_UNAVAILABLE.value(), e);
        }
    }

    public <T> T executeGet(String url, ParameterizedTypeReference<T> typeReference, String... params) {
        String serviceUrl = null;
        try {
            serviceUrl = parseUrlParameters(url, params);
            final UUID correlationId = UUID.randomUUID();
            return webClient.get().uri(serviceUrl).headers(h -> {
                h.setBearerAuth(restUtils.getAccessToken());
                h.set(EducDistributionApiConstants.CORRELATION_ID, correlationId.toString());
            }).retrieve().bodyToMono(typeReference).block();
        } catch(Exception e) {
            logger.error(REST_SERVICE_ERROR, "GET", serviceUrl, Arrays.toString(params));
            throw new ServiceException(getErrorMessage(url, e.getLocalizedMessage()), HttpStatus.SERVICE_UNAVAILABLE.value(), e);
        }
    }

    public <T> T executePost(String url, Class<T> boundClass, Object requestBody, String... params) {
        String serviceUrl = null;
        try {
            serviceUrl = parseUrlParameters(url, params);
            final UUID correlationId = UUID.randomUUID();
            return webClient.post().uri(serviceUrl).headers(h -> {
                h.setBearerAuth(restUtils.getAccessToken());
                h.set(EducDistributionApiConstants.CORRELATION_ID, correlationId.toString());
            }).body(BodyInserters.fromValue(requestBody)).retrieve().bodyToMono(boundClass).block();
        } catch(Exception e) {
            logger.error(REST_SERVICE_ERROR, "POST", serviceUrl, Arrays.toString(params));
            throw new ServiceException(getErrorMessage(url, e.getLocalizedMessage()), HttpStatus.SERVICE_UNAVAILABLE.value(), e);
        }
    }

    public <T> T executeDelete(String url, Class<T> boundClass, String... params) {
        String serviceUrl = null;
        try {
            serviceUrl = parseUrlParameters(url, params);
            final UUID correlationId = UUID.randomUUID();
            return webClient.delete().uri(serviceUrl).headers(h -> {
                h.setBearerAuth(restUtils.getAccessToken());
                h.set(EducDistributionApiConstants.CORRELATION_ID, correlationId.toString());
            }).retrieve().bodyToMono(boundClass).block();
        } catch(Exception e) {
            logger.error(REST_SERVICE_ERROR, "DELETE", serviceUrl, Arrays.toString(params));
            throw new ServiceException(getErrorMessage(url, e.getLocalizedMessage()), HttpStatus.SERVICE_UNAVAILABLE.value(), e);
        }
    }

    protected String parseUrlParameters(String url, String... params) {
        return String.format(url, params);
    }

    private String getErrorMessage(String url, String errorMessage) {
        return "Service failed to process at url: " + url + " due to: " + errorMessage;
    }
}
