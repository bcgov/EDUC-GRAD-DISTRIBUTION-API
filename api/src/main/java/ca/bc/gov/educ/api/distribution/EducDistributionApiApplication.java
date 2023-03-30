package ca.bc.gov.educ.api.distribution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class, 
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class})
@EnableCaching
@EnableScheduling
public class EducDistributionApiApplication {

    private static Logger logger = LoggerFactory.getLogger(EducDistributionApiApplication.class);

    public static void main(String[] args) {
        logger.debug("#######Starting API");
        SpringApplication.run(EducDistributionApiApplication.class, args);
        logger.debug("#######Started API");
    }

}