package ca.bc.gov.educ.api.distribution.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class GradService {

    private Instant start;
    private static final Logger logger = LoggerFactory.getLogger(GradService.class);

    void start() {
        start = Instant.now();
    }

    void end() {
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        logger.info("Time taken: {} milliseconds",timeElapsed.toMillis());
    }
}
