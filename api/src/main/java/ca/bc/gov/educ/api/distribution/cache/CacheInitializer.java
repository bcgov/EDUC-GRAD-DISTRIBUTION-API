package ca.bc.gov.educ.api.distribution.cache;

import ca.bc.gov.educ.api.distribution.service.PsiService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("!test")
@Slf4j
@Component
public class CacheInitializer {
    private final PsiService psiService;

    public CacheInitializer(PsiService psiService) {
        this.psiService = psiService;
    }

    @PostConstruct
    public void loadCacheOnStartup() {
        log.info("Initializing cache at startup...");
        psiService.loadPsiCache();
    }

    @Scheduled(cron = "0 0 23 * * ?")
    public void scheduledCacheRefresh() {
        log.info("Refreshing cache...");
        psiService.loadPsiCache();
    }
}