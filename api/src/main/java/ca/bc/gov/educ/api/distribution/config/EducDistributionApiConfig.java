package ca.bc.gov.educ.api.distribution.config;

import ca.bc.gov.educ.api.distribution.util.DeleteExpiredFilesFileVisitorImpl;
import ca.bc.gov.educ.api.distribution.util.IOUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

@Configuration
@IntegrationComponentScan
@EnableIntegration
public class EducDistributionApiConfig {

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public WebClient webClient() {
        HttpClient client = HttpClient.create();
        client.warmup().block();
        return WebClient.builder().exchangeStrategies(ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(20 * 1024 * 1024))
                .build()).build();
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean("TmpCacheFileVisitor")
    public FileVisitor<Path> createCleanTmpCacheFilesFileVisitor(@Value("(^hsperf.*|^undertow.+|^\\.nfs.+|^\\.java.+|^\\.nfs.+|Batch|FTP|PAPER)") String filter, @Value("${scheduler.clean-tmp-cache-interval-in-days}") int days) {
        return new DeleteExpiredFilesFileVisitorImpl(filter, LocalDateTime.now().minusDays(days));
    }
}
