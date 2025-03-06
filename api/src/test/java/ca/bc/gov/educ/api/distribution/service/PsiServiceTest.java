package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.Psi;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.RestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
class PsiServiceTest {

  @Mock
  private RestService restService;

  @Mock
  private EducDistributionApiConstants educDistributionApiConstants;

  @Mock
  private RestUtils restUtils;

  @InjectMocks
  private PsiService psiService;

  @BeforeEach
  void setUp() {
    psiService = new PsiService(restService, educDistributionApiConstants, restUtils);
  }

  @Test
  void testGetPsiDetails_NotInCache_FoundInAPI() {
    String psiCode = "678";
    Psi psi = new Psi();
    psi.setPsiCode(psiCode);

    when(educDistributionApiConstants.getPsiByPsiCode()).thenReturn("api-url");
    when(restService.executeGet(anyString(), eq(Psi.class), eq(psiCode))).thenReturn(psi);

    Optional<Psi> result = psiService.getPsiDetails(psiCode);

    assertTrue(result.isPresent());
    assertEquals(psiCode, result.get().getPsiCode());

    verify(restService, times(1)).executeGet(anyString(), eq(Psi.class), eq(psiCode));

    // Call again, should be cached now
    psiService.getPsiDetails(psiCode);
    verify(restService, times(1)).executeGet(anyString(), eq(Psi.class), eq(psiCode)); // API still called once
  }

  @Test
  void testGetPsiDetails_NotInCache_NotFoundInAPI(CapturedOutput output) {
    String psiCode = "000";

    when(educDistributionApiConstants.getPsiByPsiCode()).thenReturn("api-url");
    when(restService.executeGet(anyString(), eq(Psi.class), eq(psiCode))).thenReturn(null);

    Optional<Psi> result = psiService.getPsiDetails(psiCode);

    assertFalse(result.isPresent());

    verify(restService, times(1)).executeGet(anyString(), eq(Psi.class), eq(psiCode));
    assertTrue(output.getOut().contains("PSI code 000 not found in in Trax API"), "Expected log message not found!");
  }

  @Test
  void testGetPsiDetails_APIError(CapturedOutput output) {
    String psiCode = "999";

    when(educDistributionApiConstants.getPsiByPsiCode()).thenReturn("api-url");
    when(restService.executeGet(anyString(), eq(Psi.class), eq(psiCode)))
        .thenThrow(new RuntimeException("API error"));

    Optional<Psi> result = psiService.getPsiDetails(psiCode);

    assertFalse(result.isPresent());

    verify(restService, times(1)).executeGet(anyString(), eq(Psi.class), eq(psiCode));
    assertTrue(output.getOut().contains("Error fetching PSI code 999 from Trax API:"), "Expected log message not found!");
  }

  @Test
  void testLoadPsiCache_Success() {
    Psi psi1 = new Psi();
    psi1.setPsiCode("11111");

    Psi psi2 = new Psi();
    psi2.setPsiCode("22222");

    List<Psi> psiList = List.of(psi1, psi2);

    when(educDistributionApiConstants.getAllPsi()).thenReturn("api-url");
    when(restService.executeGet(eq("api-url"), any(ParameterizedTypeReference.class))).thenReturn(psiList);

    psiService.loadPsiCache();

    verify(restService, times(1)).executeGet(eq("api-url"), any(ParameterizedTypeReference.class));

    // Check if a PSI is retrievable from cache
    Optional<Psi> cachedPsi1 = psiService.getPsiDetails("11111");
    Optional<Psi> cachedPsi2 = psiService.getPsiDetails("22222");

    assertTrue(cachedPsi1.isPresent());
    assertTrue(cachedPsi2.isPresent());
  }

  @Test
  void testLoadPsiCache_APIError(CapturedOutput output) {
    when(educDistributionApiConstants.getAllPsi()).thenReturn("api-url");
    when(restService.executeGet(eq("api-url"), any(ParameterizedTypeReference.class)))
        .thenThrow(new RuntimeException("API error"));

    psiService.loadPsiCache();

    verify(restService, times(1)).executeGet(eq("api-url"), any(ParameterizedTypeReference.class));
    assertTrue(output.getOut().contains("Failed to load PSI cache"), "Expected log message not found!");
  }
}