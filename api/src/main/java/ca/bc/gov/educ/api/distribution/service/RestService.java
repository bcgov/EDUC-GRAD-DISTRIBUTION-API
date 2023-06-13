package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.exception.ServiceException;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
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
        String serviceUrl = parseUrlParameters(url, params);
        final UUID correlationId = UUID.randomUUID();
        try {
            return webClient.get().uri(serviceUrl).headers(h -> {
                h.setBearerAuth(restUtils.getAccessToken());
                h.set(EducDistributionApiConstants.CORRELATION_ID, correlationId.toString());
            })
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError,
                            clientResponse -> Mono.error(new ServiceException(getErrorMessage(url, "5xx error."), clientResponse.statusCode().value())))
                    .bodyToMono(boundClass)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                            .filter(ServiceException.class::isInstance)
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                throw new ServiceException(getErrorMessage(url, "Service failed to process after max retries."), HttpStatus.SERVICE_UNAVAILABLE.value());
                            }))
                    .block();
        } catch(Exception e) {
            logger.error(REST_SERVICE_ERROR, "GET", serviceUrl, Arrays.toString(params));
            throw new ServiceException(getErrorMessage(url, e.getLocalizedMessage()), HttpStatus.SERVICE_UNAVAILABLE.value(), e);
        }
    }

    public <T> T executeGet(String url, ParameterizedTypeReference<T> typeReference, String... params) {
        String serviceUrl = parseUrlParameters(url, params);
        final UUID correlationId = UUID.randomUUID();
        try {
            return webClient.get().uri(serviceUrl).headers(h -> {
                h.setBearerAuth(restUtils.getAccessToken());
                h.set(EducDistributionApiConstants.CORRELATION_ID, correlationId.toString());
            })
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError,
                            clientResponse -> Mono.error(new ServiceException(getErrorMessage(url, "5xx error."), clientResponse.statusCode().value())))
                    .bodyToMono(typeReference)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                            .filter(ServiceException.class::isInstance)
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                throw new ServiceException(getErrorMessage(url, "Service failed to process after max retries."), HttpStatus.SERVICE_UNAVAILABLE.value());
                            }))
                    .block();
        } catch(Exception e) {
            logger.error(REST_SERVICE_ERROR, "GET", serviceUrl, Arrays.toString(params));
            throw new ServiceException(getErrorMessage(url, e.getLocalizedMessage()), HttpStatus.SERVICE_UNAVAILABLE.value(), e);
        }
    }

    public <T> T executePost(String url, Class<T> boundClass, Object requestBody, String... params) {
        String serviceUrl = parseUrlParameters(url, params);
        final UUID correlationId = UUID.randomUUID();
        try {
            return webClient.post().uri(serviceUrl).headers(h -> {
                h.setBearerAuth(restUtils.getAccessToken());
                h.set(EducDistributionApiConstants.CORRELATION_ID, correlationId.toString());
            })
                    .body(BodyInserters.fromValue(requestBody))
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError,
                            clientResponse -> Mono.error(new ServiceException(getErrorMessage(url, "5xx error."), clientResponse.statusCode().value())))
                    .bodyToMono(boundClass)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                            .filter(ServiceException.class::isInstance)
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                throw new ServiceException(getErrorMessage(url, "Service failed to process after max retries."), HttpStatus.SERVICE_UNAVAILABLE.value());
                            }))
                    .block();
        } catch(Exception e) {
            logger.error(REST_SERVICE_ERROR, "POST", serviceUrl, Arrays.toString(params));
            throw new ServiceException(getErrorMessage(url, e.getLocalizedMessage()), HttpStatus.SERVICE_UNAVAILABLE.value(), e);
        }
    }

    public <T> T executeDelete(String url, Class<T> boundClass, String... params) {
        String serviceUrl = parseUrlParameters(url, params);
        final UUID correlationId = UUID.randomUUID();
        try {
            return webClient.delete().uri(serviceUrl).headers(h -> {
                h.setBearerAuth(restUtils.getAccessToken());
                h.set(EducDistributionApiConstants.CORRELATION_ID, correlationId.toString());
            })
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError,
                            clientResponse -> Mono.error(new ServiceException(getErrorMessage(url, "5xx error."), clientResponse.statusCode().value())))
                    .bodyToMono(boundClass)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                            .filter(ServiceException.class::isInstance)
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                throw new ServiceException(getErrorMessage(url, "Service failed to process after max retries."), HttpStatus.SERVICE_UNAVAILABLE.value());
                            }))
                    .block();
        } catch(Exception e) {
            logger.error(REST_SERVICE_ERROR, "DELETE", serviceUrl, Arrays.toString(params));
            throw new ServiceException(getErrorMessage(url, e.getLocalizedMessage()), HttpStatus.SERVICE_UNAVAILABLE.value(), e);
        }
    }

    private final String parseUrlParameters(String url, String... params) {
        return String.format(url, params);
    }

    private String getErrorMessage(String url, String errorMessage) {
        return "Service failed to process at url: " + url + " due to: " + errorMessage;
    }
}
