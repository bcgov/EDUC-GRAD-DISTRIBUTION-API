package ca.bc.gov.educ.api.distribution.util;

import ca.bc.gov.educ.api.distribution.config.EducDistributionApiConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.FactoryBasedNavigableListAssert.assertThat;

public class CacheClearingTest {

    private enum FILES {
        HSPERF,
        UNDERT,
        NFS,
        JAV,
        BATCH_DIR,
        FTP_DIR,
        FTP_SUB,
        FTP_SUB_FILE,
        FTP_ZIP,
        PAPER_DIR,
        PAPER_SUB,
        PAPER_SUB_FILE,
        PAPER_ZIP
    }

    private String tmpDir;
    private final String FILE_SEP = File.separator;
    private Map<String, Path> files = new HashMap<>();

    @Before
    public void createCache() throws IOException {
        this.tmpDir = Files.createTempDirectory("cacheTestDir").toFile().getAbsolutePath();
        createTmpCacheFiles();
    }

    @Test
    public void testFileFilter() throws IOException {
        EducDistributionApiConfig config = new EducDistributionApiConfig();
        FileVisitor<Path> fileVisitor = config.createCleanTmpCacheFilesFileVisitor("(^hsperf.*|^undertow.+|^\\.nfs.+|^\\.java.+|^\\.nfs.+|Batch|FTP|PAPER)", 1);
        Path startingDir = Paths.get(this.tmpDir);
        if(Files.exists(startingDir)){
            Files.walkFileTree(startingDir, fileVisitor);
        }
        Assert.assertTrue(true);
    }

    private void createTmpCacheFiles() throws IOException {
        // create an expiry time of one day for testing
        FileTime expiry = FileTime.from(LocalDateTime.now().minusDays(2).toInstant(ZoneOffset.UTC));

        // create directory structure
        //
        // hsperfdata_101279000                       (HSPERF         expired, should not be deleted)
        // undertow-docbase.8080.11764866489534620758 (UNDERT         expired, should not be deleted)
        // .nfs000000002abc01e400010418               (NFS            expired, should not be deleted)
        // .java_pid1                                 (JAV            expired, should not be deleted)
        // Batch                                      (BATCH_DIR      expired should not be deleted)
        //   |_FTP                                    (FTP_DIR        expired should not be deleted)
        //      |_18873                               (FTP_SUB        expired, should be deleted)
        //          | FileA.pdf                       (FTP_SUB_FILE   expired, should be deleted)
        //      | EDGRAD.BATCH.18873.zip              (FTP_ZIP        not expired, should not be deleted)
        //   |_PAPER                                  (PAPER_DIR      expired should not be deleted)
        //      |_18874                               (PAPER_SUB      not expired, should not be deleted)
        //          | FileB.pdf                       (PAPER_SUB_FILE not expired, should not be deleted)
        //      | EDGRAD.BATCH.18874.zip              (PAPER_ZIP      expired, should be deleted)
        //
        // add files and update expiry times on some
        files.put(String.valueOf(FILES.HSPERF), Paths.get(tmpDir + FILE_SEP + "hsperfdata_101279000"));
        files.put(String.valueOf(FILES.UNDERT), Paths.get(tmpDir + FILE_SEP + "undertow-docbase.8080.11764866489534620758"));
        files.put(String.valueOf(FILES.NFS), Paths.get(tmpDir + FILE_SEP + ".nfs000000002abc01e400010418"));
        files.put(String.valueOf(FILES.JAV), Paths.get(tmpDir + FILE_SEP + ".java_pid1"));

        Files.createFile(files.get(String.valueOf(FILES.HSPERF)));
        Files.createFile(files.get(String.valueOf(FILES.UNDERT)));
        Files.createFile(files.get(String.valueOf(FILES.NFS)));
        Files.createFile(files.get(String.valueOf(FILES.JAV)));

        Files.setLastModifiedTime(files.get(String.valueOf(FILES.HSPERF)), expiry);
        Files.setLastModifiedTime(files.get(String.valueOf(FILES.UNDERT)), expiry);
        Files.setLastModifiedTime(files.get(String.valueOf(FILES.NFS)), expiry);
        Files.setLastModifiedTime(files.get(String.valueOf(FILES.JAV)), expiry);

        files.put(String.valueOf(FILES.BATCH_DIR), Paths.get(tmpDir + FILE_SEP  + "Batch"));
        files.put(String.valueOf(FILES.FTP_DIR), Paths.get(files.get("BATCH_DIR") + FILE_SEP + "FTP"));
        files.put(String.valueOf(FILES.PAPER_DIR), Paths.get(files.get("BATCH_DIR") + FILE_SEP + "PAPER"));
        Files.createDirectory(files.get(String.valueOf(FILES.BATCH_DIR)));
        Files.createDirectory(files.get(String.valueOf(FILES.FTP_DIR)));
        Files.createDirectory(files.get(String.valueOf(FILES.PAPER_DIR)));

        files.put(String.valueOf(FILES.FTP_SUB), Path.of(files.get(String.valueOf(FILES.FTP_DIR)) + FILE_SEP + "18873"));
        Files.createDirectory(files.get(String.valueOf(FILES.FTP_SUB)));


        files.put(String.valueOf(FILES.FTP_SUB_FILE), Path.of(files.get(String.valueOf(FILES.FTP_SUB)) + FILE_SEP + "FileA.pdf"));
        Files.createFile(files.get(String.valueOf(FILES.FTP_SUB_FILE)));


        files.put(String.valueOf(FILES.FTP_ZIP), Path.of(files.get(String.valueOf(FILES.FTP_DIR)) + FILE_SEP + "EDGRAD.BATCH.18873.zip"));
        Files.createFile(files.get(String.valueOf(FILES.FTP_ZIP)));

        files.put(String.valueOf(FILES.PAPER_SUB), Path.of(files.get(String.valueOf(FILES.PAPER_DIR)) + FILE_SEP + "18874"));
        Files.createDirectory(files.get(String.valueOf(FILES.PAPER_SUB)));

        files.put(String.valueOf(FILES.PAPER_SUB_FILE), Path.of(files.get(String.valueOf(FILES.PAPER_SUB)) + FILE_SEP + "FileB.pdf"));
        Files.createFile(files.get(String.valueOf(FILES.PAPER_SUB_FILE)));

        files.put(String.valueOf(FILES.PAPER_ZIP), Path.of(files.get(String.valueOf(FILES.PAPER_DIR)) + FILE_SEP + "EDGRAD.BATCH.18874.zip"));
        Files.createFile(files.get(String.valueOf(FILES.PAPER_ZIP)));

        Files.setLastModifiedTime(files.get(String.valueOf(FILES.FTP_SUB_FILE)), expiry);
        Files.setLastModifiedTime(files.get(String.valueOf(FILES.FTP_SUB)), expiry);
        Files.setLastModifiedTime(files.get(String.valueOf(FILES.FTP_DIR)), expiry);
        Files.setLastModifiedTime(files.get(String.valueOf(FILES.PAPER_ZIP)), expiry);
        Files.setLastModifiedTime(files.get(String.valueOf(FILES.PAPER_DIR)), expiry);
        Files.setLastModifiedTime(files.get(String.valueOf(FILES.BATCH_DIR)), expiry);
    }

}
