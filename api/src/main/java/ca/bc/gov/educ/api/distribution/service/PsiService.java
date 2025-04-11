package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.Psi;
import ca.bc.gov.educ.api.distribution.model.dto.ReportData;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.RestUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class PsiService {

	RestService restService;
	EducDistributionApiConstants educDistributionApiConstants;
	RestUtils restUtils;
	private final Map<String, Psi> psiCache = new ConcurrentHashMap<>();

	@Autowired
	public PsiService(RestService restService, EducDistributionApiConstants educDistributionApiConstants, RestUtils restUtils) {
		this.restService = restService;
		this.educDistributionApiConstants = educDistributionApiConstants;
		this.restUtils = restUtils;
	}

	public Optional<Psi> getPsiDetails(String psiCode) {
		Psi psi = psiCache.get(psiCode);
		if(psi != null) {
			return Optional.of(psi);
		}
		log.debug("PSI code {} not found in cache, fetching from API", psiCode);

		try {
			psi = restService.executeGet(educDistributionApiConstants.getPsiByPsiCode(), Psi.class, psiCode);
			if(psi != null) {
				psiCache.put(psiCode, psi);
				log.debug("PSI code {} found and loaded to cache", psiCode);
				return Optional.of(psi);
			} else {
				log.warn("PSI code {} not found in in Trax API", psiCode);
				return Optional.empty();
			}
		} catch (Exception e) {
			log.error("Error fetching PSI code {} from Trax API: {}", psiCode, e.getMessage(), e);
			return Optional.empty();
		}
	}

	@Async("cacheExecutor")
	public void loadPsiCache() {
		log.info("Loading PSI cache");
		try {
			List<Psi> psiList = restService.executeGet(
					educDistributionApiConstants.getAllPsi(),
					new ParameterizedTypeReference<>() {}
			);
			Map<String, Psi> newCache = new ConcurrentHashMap<>();
			psiList.forEach(psi -> newCache.put(psi.getPsiCode(), psi));
			psiCache.clear();
			psiCache.putAll(newCache);
			log.info("PSI cache successfully loaded with {} entries.", psiList.size());
		} catch (Exception e) {
			log.error("Failed to load PSI cache: {}", e.getMessage(), e);
		}
	}

    //Grad2-1931 Retrieving Student Transcript - mchintha
	public ReportData getReportData(String pen) {
		return restService.executeGet(
				educDistributionApiConstants.getTranscriptCSVData(),
				ReportData.class,
				pen
		);
	}
}
