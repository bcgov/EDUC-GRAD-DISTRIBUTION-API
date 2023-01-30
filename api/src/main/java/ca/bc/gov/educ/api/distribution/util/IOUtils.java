package ca.bc.gov.educ.api.distribution.util;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import java.io.File;
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
    public static final String LOC = "/tmp/";
    public static final String DEL = "/";

    private static final String FILE_PREFIX = "EDGRAD.BATCH.";

    public enum SUPPORTED_FILE_EXTENSIONS {
        ZIP(".zip"),
        TXT(".txt");
        public final String label;
        SUPPORTED_FILE_EXTENSIONS(String label){
            this.label = label;
        }
    }

    /**
     * Given a batch id, this method cleans batch artifacts
     * @param batchId the id of the batch
     */
    @Async
    public void removeDownloadFiles(Long batchId){
        try {
            FileUtils.deleteDirectory(new File(LOC + batchId));
            FileUtils.delete(new File(createFileNameFromBatchId(LOC, batchId, SUPPORTED_FILE_EXTENSIONS.ZIP)));
        } catch (IOException e) {
            //do nothing, files not there
        }
    }

    public byte[] getDownload(Long batchId) {
        String localFile = createFileNameFromBatchId(LOC, batchId, SUPPORTED_FILE_EXTENSIONS.ZIP);
        Path path = Paths.get(localFile);
        byte[] data;
        try {
            data = Files.readAllBytes(path);
            // cleanup artifacts
            removeDownloadFiles(batchId);
            return data;
        } catch (IOException e) {
            logger.debug("Error Message {}", e.getLocalizedMessage());
        }
        return new byte[0];
    }

    public String createFileNameFromBatchId(String location, Long batchId, SUPPORTED_FILE_EXTENSIONS fileExtension){
        return location + FILE_PREFIX + batchId + fileExtension.label;
    }

}
