package ca.bc.gov.educ.api.distribution.config;

import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.modelmapper.ModelMapper;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.FileFilter;
import java.time.format.DateTimeFormatter;

import static ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants.DATETIME_FORMAT;

@Configuration
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


    @Bean
    public FileFilter createFileFilter(){
        return pathname -> {
            String name = pathname.getName();
            return !name.startsWith(".") &&
                    !name.contains("undertow") &&
                    !name.contains("hsperfdata");
        };
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonObjectMapperCustomization() {
        LocalDateTimeSerializer localDateTimeSerializer = new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATETIME_FORMAT));
        return jacksonObjectMapperBuilder ->
                jacksonObjectMapperBuilder
                        .serializers(localDateTimeSerializer);
    }
}
