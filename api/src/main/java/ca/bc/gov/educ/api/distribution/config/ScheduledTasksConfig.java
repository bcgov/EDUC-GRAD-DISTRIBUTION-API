package ca.bc.gov.educ.api.distribution.config;

import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.io.FileFilter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

@Configuration
public class ScheduledTasksConfig {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasksConfig.class);

    FileFilter fileFilter;

    @Autowired
    public ScheduledTasksConfig(FileFilter fileFilter) {
        this.fileFilter = fileFilter;
    }

    @Scheduled(cron = "${scheduler.clean-tmp-cache-cron}")
    public void cleanTmpCacheFiles() {
        File dir = new File(EducDistributionApiConstants.TMP_DIR);
        List<File> files = Arrays.asList(dir.listFiles(this.fileFilter));
        LocalDateTime fileExpiry = LocalDateTime.now().minusHours(3);
        files.forEach(file -> {
            LocalDateTime lastModified = LocalDateTime.ofInstant(Instant.ofEpochMilli(file.lastModified()), ZoneId.systemDefault());
            if(lastModified.isBefore(fileExpiry)){
                logger.debug("Removing file or directory: {}", file.getName());
                IOUtils.removeFileOrDirectory(file);
            }
        });
    }

}
