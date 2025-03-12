package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.exception.ServiceException;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.JsonTransformer;
import ca.bc.gov.educ.api.distribution.util.RestUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class RestService {

    private static final String GET_REST_SERVICE_ERROR = "Unable to call rest service {} method of {} with params {}";
    private static final String POST_REST_SERVICE_ERROR = "Unable to call rest service {} method of {} with params {} and payload: {}";
    private static final String REST_SERVICE_WARN = "Rest service call {} method of {} with params {} and payload: {} returns unexpected result {}";
    private static final String ERROR_500 = "5xx error.";

    private static final String NO_CONTENT = "NO_CONTENT";

    private static final String RETRY_MESSAGE = "Service failed to process after max retries.";

    private static final String DELETE_REST_SERVICE_ERROR = GET_REST_SERVICE_ERROR;

    final WebClient webClient;

    final RestUtils restUtils;

    final JsonTransformer jsonTransformer;

    @Autowired
    public RestService(WebClient webClient, RestUtils restUtils, JsonTransformer jsonTransformer) {
        this.webClient = webClient;
        this.restUtils = restUtils;
        this.jsonTransformer = jsonTransformer;
    }

    public <T> T executeGet(String url, Class<T> boundClass, String... params) {
        T obj;
        String executeUrl = null;
        try {
            final String serviceUrl = parseUrlParameters(url, params);
            executeUrl = serviceUrl;
            obj = this.webClient
                    .get()
                    .uri(serviceUrl)
                    .headers(h -> h.setBearerAuth(restUtils.getAccessToken()))
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError,
                            clientResponse -> Mono.error(new ServiceException(getErrorMessage(serviceUrl, ERROR_500), clientResponse.statusCode().value())))
                    .onStatus(
                            HttpStatus.NO_CONTENT::equals,
                            response -> response.bodyToMono(String.class).thenReturn(new ServiceException(NO_CONTENT, response.statusCode().value()))
                    )
                    .bodyToMono(boundClass)
                    // only does retry if initial error was 5xx as service may be temporarily down
                    // 4xx errors will always happen if 404, 401, 403, etc. so does not retry
                    .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(2))
                            .filter(ServiceException.class::isInstance)
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                throw new ServiceException(getErrorMessage(serviceUrl, RETRY_MESSAGE), HttpStatus.SERVICE_UNAVAILABLE.value());
                            }))
                    .block();
        } catch(ServiceException e) {
            if(HttpStatus.NO_CONTENT.value() == e.getStatusCode()) {
                log.warn(REST_SERVICE_WARN, "GET", executeUrl, Arrays.toString(params), "", e.getStatusCode());
                return null;
            }
            log.error(GET_REST_SERVICE_ERROR, "GET", executeUrl, Arrays.toString(params));
            throw new ServiceException(getErrorMessage(url, e.getLocalizedMessage()), HttpStatus.SERVICE_UNAVAILABLE.value(), e);
        }
        return obj;
    }

    public <T> T executeGet(String url, ParameterizedTypeReference<T> typeReference, String... params) {
        T obj;
        String executeUrl = null;
        try {
            final String serviceUrl = parseUrlParameters(url, params);
            executeUrl = serviceUrl;
            obj = this.webClient
                    .get()
                    .uri(serviceUrl)
                    .headers(h -> h.setBearerAuth(restUtils.getAccessToken()))
                    .retrieve()
                    // if 5xx errors, throw Service error
                    .onStatus(HttpStatusCode::is5xxServerError,
                            clientResponse -> Mono.error(new ServiceException(getErrorMessage(serviceUrl, ERROR_500), clientResponse.statusCode().value())))
                    .onStatus(
                            HttpStatus.NO_CONTENT::equals,
                            response -> response.bodyToMono(String.class).thenReturn(new ServiceException(NO_CONTENT, response.statusCode().value()))
                    )
                    .bodyToMono(typeReference)
                    // only does retry if initial error was 5xx as service may be temporarily down
                    // 4xx errors will always happen if 404, 401, 403, etc. so does not retry
                    .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(2))
                            .filter(ServiceException.class::isInstance)
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                throw new ServiceException(getErrorMessage(serviceUrl, RETRY_MESSAGE), HttpStatus.SERVICE_UNAVAILABLE.value());
                            }))
                    .block();
        } catch(ServiceException e) {
            if(HttpStatus.NO_CONTENT.value() == e.getStatusCode()) {
                log.warn(REST_SERVICE_WARN, "GET", executeUrl, Arrays.toString(params), "", e.getStatusCode());
                return null;
            }
            log.error(GET_REST_SERVICE_ERROR, "GET", executeUrl, Arrays.toString(params));
            throw new ServiceException(getErrorMessage(url, e.getLocalizedMessage()), HttpStatus.SERVICE_UNAVAILABLE.value(), e);
        }
        return obj;
    }

    public <T> T executePost(String url, Class<T> boundClass, Object requestBody, String... params) {
        T obj;
        String executeUrl = null;
        try {
            final String serviceUrl = parseUrlParameters(url, params);
            executeUrl = serviceUrl;
            obj = this.webClient.post()
                    .uri(serviceUrl)
                    .headers(h -> h.setBearerAuth(restUtils.getAccessToken()))
                    .body(BodyInserters.fromValue(requestBody))
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError,
                            clientResponse -> Mono.error(new ServiceException(getErrorMessage(url, ERROR_500), clientResponse.statusCode().value())))
                    .onStatus(
                            HttpStatus.NO_CONTENT::equals,
                            clientResponse -> clientResponse.bodyToMono(String.class).thenReturn(new ServiceException(NO_CONTENT, clientResponse.statusCode().value()))
                    )
                    .bodyToMono(boundClass)
                    .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(2))
                            .filter(ServiceException.class::isInstance)
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                throw new ServiceException(getErrorMessage(url, RETRY_MESSAGE), HttpStatus.SERVICE_UNAVAILABLE.value());
                            }))
                    .block();
        } catch(ServiceException e) {
            if(HttpStatus.NO_CONTENT.value() == e.getStatusCode()) {
                log.warn(REST_SERVICE_WARN, "POST", executeUrl, Arrays.toString(params), jsonTransformer.marshall(requestBody), e.getStatusCode());
                return null;
            }
            log.error(POST_REST_SERVICE_ERROR, "POST", executeUrl, Arrays.toString(params), jsonTransformer.marshall(requestBody));
            throw new ServiceException(getErrorMessage(url, e.getLocalizedMessage()), HttpStatus.SERVICE_UNAVAILABLE.value(), e);
        }
        return obj;
    }

    public <T> T executeDelete(String url, Class<T> boundClass, String... params) {
        T obj;
        String executeUrl = null;
        try {
            final String serviceUrl = parseUrlParameters(url, params);
            executeUrl = serviceUrl;
            obj = webClient.delete().uri(serviceUrl).headers(h -> h.setBearerAuth(restUtils.getAccessToken()))
                    .retrieve()
                    .bodyToMono(boundClass)
                    .block();
        } catch(Exception e) {
            log.error(DELETE_REST_SERVICE_ERROR, "DELETE", executeUrl, Arrays.toString(params));
            throw new ServiceException(getErrorMessage(url, e.getLocalizedMessage()), HttpStatus.SERVICE_UNAVAILABLE.value(), e);
        }
        return obj;
    }

    protected String parseUrlParameters(String url, String... params) {
        List<String> l = Arrays.asList(params);
        l.replaceAll(t-> Objects.isNull(t) ? "" : t);
        return String.format(url, l.toArray());
    }

    private String getErrorMessage(String url, String errorMessage) {
        return "Service failed to process at url: " + url + " due to: " + errorMessage;
    }
}
