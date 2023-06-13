package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.DistributionResponse;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import org.wildfly.common.Assert;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

import static ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
public class PostingProcessServiceTest {

    @Mock
    private PostingDistributionService postingDistributionService;
    @Autowired
    private EducDistributionApiConstants educDistributionApiConstants;

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


    @Test
    public void testPostingProcess() {
        DistributionResponse response = new DistributionResponse();
        response.setBatchId(12345L);
        response.setActivityCode(YEARENDDIST);

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(educDistributionApiConstants.getSchoolDistrictYearEndReport(), "", DISTREP_YE_SD, DISTREP_YE_SC))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

        Mockito.doReturn(1).when(postingDistributionService).createDistrictSchoolYearEndReport("", DISTREP_YE_SD, DISTREP_YE_SC);
        var result = this.postingDistributionService.postingProcess(response);
        Assert.assertNotNull(result);
    }

}
