package ca.bc.gov.educ.api.distribution.util;

import ca.bc.gov.educ.api.distribution.config.EducDistributionApiConfig;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

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
    private final Map<FILES, Path> files = new HashMap<>();

    @Before
    public void createCache() throws IOException {
        this.tmpDir = Files.createTempDirectory("cacheTestDir").toFile().getAbsolutePath();
        createTmpCacheFiles();
        EducDistributionApiConfig config = new EducDistributionApiConfig();
        FileVisitor<Path> fileVisitor = config.createCleanTmpCacheFilesFileVisitor("{hsperf*,undertow*,.nfs*,.java*,.nfs*,Batch,FTP,PAPER}", 1);
        Path startingDir = Paths.get(this.tmpDir);
        if(Files.exists(startingDir)){
            Files.walkFileTree(startingDir, fileVisitor);
        }
    }

    @After
    public void removeCache() throws IOException {
        Path tmp = Paths.get(tmpDir);
        FileUtils.deleteDirectory(tmp.toFile());
    }


    @Test
    public void testFilesAreExpiredButShouldNotBeDeleted() {
        for(Map.Entry<FILES, Path> entry : this.files.entrySet()){
            FILES f = entry.getKey();
            if(f != FILES.FTP_SUB &&
                f != FILES.FTP_SUB_FILE &&
                f != FILES.FTP_ZIP &&
                f != FILES.PAPER_SUB &&
                f != FILES.PAPER_SUB_FILE &&
                f != FILES.PAPER_ZIP){
                Assert.assertTrue(Files.exists(entry.getValue()));
            }
        }
    }

    @Test
    public void testFilesAreExpiredAndShouldBeDeleted() {
        for(Map.Entry<FILES, Path> entry : this.files.entrySet()){
            FILES f = entry.getKey();
            if(f == FILES.FTP_SUB_FILE ||
                f == FILES.PAPER_ZIP){
                Assert.assertFalse(Files.exists(entry.getValue()));
            }
        }
    }

    @Test
    public void testFilesAreNotExpiredThatShouldNotBeDeleted() {
        for(Map.Entry<FILES, Path> entry : this.files.entrySet()){
            FILES f = entry.getKey();
            if(f == FILES.FTP_ZIP || f == FILES.PAPER_SUB || f == FILES.PAPER_SUB_FILE){
                Assert.assertTrue(Files.exists(entry.getValue()));
            }
        }
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
        // Batch                                      (BATCH_DIR      expired, should not be deleted)
        //   |_FTP                                    (FTP_DIR        expired, should not be deleted)
        //      |_18873                               (FTP_SUB        expired, should be deleted)
        //          | FileA.pdf                       (FTP_SUB_FILE   expired, should be deleted)
        //      | EDGRAD.BATCH.18873.zip              (FTP_ZIP        not expired, should not be deleted)
        //   |_PAPER                                  (PAPER_DIR      expired should not be deleted)
        //      |_18874                               (PAPER_SUB      not expired, should not be deleted)
        //          | FileB.pdf                       (PAPER_SUB_FILE not expired, should not be deleted)
        //      | EDGRAD.BATCH.18874.zip              (PAPER_ZIP      expired, should be deleted)
        //
        // add files and update expiry times on some
        files.put(FILES.HSPERF, Paths.get(tmpDir + FILE_SEP + "hsperfdata_101279000"));
        files.put(FILES.UNDERT, Paths.get(tmpDir + FILE_SEP + "undertow-docbase.8080.11764866489534620758"));
        files.put(FILES.NFS, Paths.get(tmpDir + FILE_SEP + ".nfs000000002abc01e400010418"));
        files.put(FILES.JAV, Paths.get(tmpDir + FILE_SEP + ".java_pid1"));

        Files.createFile(files.get(FILES.HSPERF));
        Files.createFile(files.get(FILES.UNDERT));
        Files.createFile(files.get(FILES.NFS));
        Files.createFile(files.get(FILES.JAV));

        Files.setLastModifiedTime(files.get(FILES.HSPERF), expiry);
        Files.setLastModifiedTime(files.get(FILES.UNDERT), expiry);
        Files.setLastModifiedTime(files.get(FILES.NFS), expiry);
        Files.setLastModifiedTime(files.get(FILES.JAV), expiry);

        files.put(FILES.BATCH_DIR, Paths.get(tmpDir + FILE_SEP  + "Batch"));
        files.put(FILES.FTP_DIR, Paths.get(files.get(FILES.BATCH_DIR) + FILE_SEP + "FTP"));
        files.put(FILES.PAPER_DIR, Paths.get(files.get(FILES.BATCH_DIR) + FILE_SEP + "PAPER"));
        Files.createDirectory(files.get(FILES.BATCH_DIR));
        Files.createDirectory(files.get(FILES.FTP_DIR));
        Files.createDirectory(files.get(FILES.PAPER_DIR));

        files.put(FILES.FTP_SUB, Path.of(files.get(FILES.FTP_DIR) + FILE_SEP + "18873"));
        Files.createDirectory(files.get(FILES.FTP_SUB));


        files.put(FILES.FTP_SUB_FILE, Path.of(files.get(FILES.FTP_SUB) + FILE_SEP + "FileA.pdf"));
        Files.createFile(files.get(FILES.FTP_SUB_FILE));


        files.put(FILES.FTP_ZIP, Path.of(files.get(FILES.FTP_DIR) + FILE_SEP + "EDGRAD.BATCH.18873.zip"));
        Files.createFile(files.get(FILES.FTP_ZIP));

        files.put(FILES.PAPER_SUB, Path.of(files.get(FILES.PAPER_DIR) + FILE_SEP + "18874"));
        Files.createDirectory(files.get(FILES.PAPER_SUB));

        files.put(FILES.PAPER_SUB_FILE, Path.of(files.get(FILES.PAPER_SUB) + FILE_SEP + "FileB.pdf"));
        Files.createFile(files.get(FILES.PAPER_SUB_FILE));

        files.put(FILES.PAPER_ZIP, Path.of(files.get(FILES.PAPER_DIR) + FILE_SEP + "EDGRAD.BATCH.18874.zip"));
        Files.createFile(files.get(FILES.PAPER_ZIP));

        Files.setLastModifiedTime(files.get(FILES.FTP_SUB_FILE), expiry);
        Files.setLastModifiedTime(files.get(FILES.FTP_SUB), expiry);
        Files.setLastModifiedTime(files.get(FILES.FTP_DIR), expiry);
        Files.setLastModifiedTime(files.get(FILES.PAPER_ZIP), expiry);
        Files.setLastModifiedTime(files.get(FILES.PAPER_DIR), expiry);
        Files.setLastModifiedTime(files.get(FILES.BATCH_DIR), expiry);
    }

}
