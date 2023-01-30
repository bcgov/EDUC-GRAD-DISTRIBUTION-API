package ca.bc.gov.educ.api.distribution.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Methods for interacting with filesystem
 */
@Component
@EnableAsync
public class IOUtils {

    private static final Logger logger = LoggerFactory.getLogger(IOUtils.class);

    /**
     * Given a batch id, this method cleans batch artifacts
     * @param batchId the id of the batch
     */
    @Async
    public void removeDownloadFiles(String batchId){

    }

    public byte[] getDownload(Long batchId) {
        String localFile = "/tmp/EDGRAD.BATCH."+batchId+".zip";
        Path path = Paths.get(localFile);
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            logger.debug("Error Message {}", e.getLocalizedMessage());
        }
        return new byte[0];
    }

}
