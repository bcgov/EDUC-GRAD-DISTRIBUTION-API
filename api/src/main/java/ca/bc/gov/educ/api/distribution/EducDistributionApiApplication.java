package ca.bc.gov.educ.api.distribution;

import ca.bc.gov.educ.api.distribution.util.SFTPUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;

import java.io.FileWriter;
import java.io.IOException;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class, 
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class})
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableCaching
public class EducDistributionApiApplication {

    private static Logger logger = LoggerFactory.getLogger(EducDistributionApiApplication.class);

    public static void main(String[] args) {
        logger.debug("#######Starting API");
        SpringApplication.run(EducDistributionApiApplication.class, args);
        logger.debug("#######Started API");
    }

    @Bean
    public ModelMapper modelMapper() {

        ModelMapper modelMapper = new ModelMapper();
        return modelMapper;
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

    @Configuration
    static
    class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {
        /**
         * Instantiates a new Web security configuration.
         * This makes sure that security context is propagated to async threads as well.
         */
        public WebSecurityConfiguration() {
            super();
            SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
        }

        @Override
        public void configure(WebSecurity web) {
            web.ignoring().antMatchers("/api/v1/api-docs-ui.html",
                    "/api/v1/swagger-ui/**", "/api/v1/api-docs/**",
                    "/actuator/health", "/actuator/prometheus", "/health");
        }
        @Override
        protected void configure(final HttpSecurity http) throws Exception {
            http.authorizeRequests()
                    .anyRequest()
                    .authenticated()
                    .and()
                    .oauth2ResourceServer()
                    .jwt();
        }
    }
}