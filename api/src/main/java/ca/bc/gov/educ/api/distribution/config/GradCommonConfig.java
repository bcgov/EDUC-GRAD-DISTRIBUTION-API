package ca.bc.gov.educ.api.distribution.config;

import ca.bc.gov.educ.api.distribution.util.GradLocalDateDeserializer;
import ca.bc.gov.educ.api.distribution.util.GradLocalDateSerializer;
import ca.bc.gov.educ.api.distribution.util.GradLocalDateTimeDeserializer;
import ca.bc.gov.educ.api.distribution.util.GradLocalDateTimeSerializer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Configuration
@PropertySource("classpath:messages.properties")
public class GradCommonConfig implements WebMvcConfigurer {

	@Autowired
	RequestResponseInterceptor requestInterceptor;
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(requestInterceptor);
	}

	@Bean
	@Primary
	ObjectMapper jacksonObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule simpleModule = new SimpleModule();
		simpleModule.addSerializer(LocalDate.class, new GradLocalDateSerializer());
		simpleModule.addSerializer(LocalDateTime.class, new GradLocalDateTimeSerializer());
		simpleModule.addDeserializer(LocalDate.class, new GradLocalDateDeserializer());
		simpleModule.addDeserializer(LocalDateTime.class, new GradLocalDateTimeDeserializer());
		mapper.findAndRegisterModules();
		mapper.registerModule(simpleModule);
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		return mapper;
	}
}
