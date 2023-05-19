package ca.bc.gov.educ.api.distribution.config;

import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "asyncExecutor")
    public TaskExecutor asyncExecutor(EducDistributionApiConstants constants) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(constants.getThreadPoolCoreSize());
        executor.setMaxPoolSize(constants.getThreadPoolMaxSize());
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
