package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.DistributionResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class PostingProcessServiceTest {

    @Mock
    private PostingDistributionService postingDistributionService;

    @Test
    public void testPostingProcess() {
        DistributionResponse response = new DistributionResponse();
        Mockito.doReturn(Boolean.TRUE).when(this.postingDistributionService).postingProcess(response);
    }

}
