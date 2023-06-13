package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.exception.ServiceException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
public class RestServiceTest {

    @Autowired
    private RestService restService;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersMock;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriMock;
    @Mock
    private WebClient.RequestBodySpec requestBodyMock;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriMock;
    @Mock
    private WebClient.ResponseSpec responseMock;
    @MockBean
    WebClient webClient;

    private static final byte[] TEST_BYTES = "The rain in Spain stays mainly on the plain.".getBytes();

    @Test
    public void testGet_GivenProperData_Expect200Response(){
        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(any(String.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(String.class)).thenReturn(Mono.just("200 OK"));

        String response = this.restService.executeGet("https://httpstat.us/200", String.class, "1234");
        Assert.assertEquals("200 OK", response);
    }

    @Test(expected = ServiceException.class)
    public void testGet_Given5xxErrorFromService_ExpectServiceError(){
        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(any(String.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(ServiceException.class)).thenReturn(Mono.just(new ServiceException()));

        this.restService.executeGet("https://httpstat.us/503", String.class, "1234");
    }

    @Test(expected = ServiceException.class)
    public void testGet_Given4xxErrorFromService_ExpectServiceError(){
        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(any(String.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(ServiceException.class)).thenReturn(Mono.just(new ServiceException()));

        this.restService.executeGet("https://httpstat.us/403", String.class, "1234");
    }

    @Test
    public void testPost_GivenProperData_Expect200Response(){
        String testBody = "test";
        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(any(String.class))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(TEST_BYTES));

        byte[] response = this.restService.executePost("https://fake.url.com", byte[].class, testBody,"1234");
        Assert.assertArrayEquals(TEST_BYTES, response);
    }

    @Test(expected = ServiceException.class)
    public void testPost_Given4xxErrorFromService_ExpectServiceError() {
        String testBody = "test";
        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri("https://fake.url.com?p=1")).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenThrow(new ServiceException());
        when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(TEST_BYTES));

        this.restService.executePost("https://fake.url.com", byte[].class, testBody, "");
    }

    @Test
    public void testDelete_GivenProperData_Expect200Response(){
        when(this.webClient.delete()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(any(String.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(String.class)).thenReturn(Mono.just("200 OK"));

        String response = this.restService.executeDelete("https://httpstat.us/200", String.class, "");
        Assert.assertEquals("200 OK", response);
    }

    @Test(expected = ServiceException.class)
    public void testDelete_Given5xxErrorFromService_ExpectServiceError(){
        when(this.webClient.delete()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(any(String.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(ServiceException.class)).thenReturn(Mono.just(new ServiceException()));

        this.restService.executeDelete("https://httpstat.us/503", String.class, "");
    }

}
