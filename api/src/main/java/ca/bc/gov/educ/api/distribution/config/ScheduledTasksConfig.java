package ca.bc.gov.educ.api.distribution.config;

import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class ScheduledTasksConfig {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasksConfig.class);

    EducDistributionApiConstants educDistributionApiConstants;
    FileVisitor<Path> fileVisitor;

    @Autowired
    public ScheduledTasksConfig(@Qualifier("TmpCacheFileVisitor") FileVisitor<Path> fileVisitor, EducDistributionApiConstants educDistributionApiConstants) {
        this.fileVisitor = fileVisitor;
        this.educDistributionApiConstants = educDistributionApiConstants;
    }

    @Scheduled(cron = "${scheduler.clean-tmp-cache-cron}")
    public void cleanTmpCacheFiles() {
        Path startingDir = Paths.get(educDistributionApiConstants.getCleanTmpCacheBaseDir());
        if(Files.exists(startingDir)){
            try {
                Files.walkFileTree(startingDir, fileVisitor);
            } catch (IOException e) {
                logger.error("ScheduledTasksConfig: There was an error removing file cache: {}", e.getLocalizedMessage());
            }
        }

    }


}
