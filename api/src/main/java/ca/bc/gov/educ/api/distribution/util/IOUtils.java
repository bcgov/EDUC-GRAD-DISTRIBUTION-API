package ca.bc.gov.educ.api.distribution.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

@Slf4j
public class IOUtils {

    private IOUtils(){}

    /**
     * Creates a secured temp dir for processing files, it is up to
     * calling method to also remove directory (see removeFileOrDirectory
     * method in this class)
     *
     * @param location
     * @param prefix
     * @return
     * @throws IOException
     */
    public static File createTempDirectory(String location, String prefix) throws IOException {
        File temp;
        Path loc = Paths.get(location);
        if (SystemUtils.IS_OS_UNIX) {
            FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
            temp = Files.createTempDirectory(loc, prefix, attr).toFile(); // Compliant
        } else {
            temp = Files.createTempDirectory(loc, prefix).toFile();  // Compliant
            temp.setReadable(true, true);
            temp.setWritable(true, true);
            temp.setExecutable(true, true);
        }
        return temp;
    }

    /**
     * Removes a directory or file recursively
     * @param file
     */
    public static void removeFileOrDirectory(File file) {
        try {
            if(file.isDirectory() && file.exists()){
                FileSystemUtils.deleteRecursively(file);
            } else {
                Files.deleteIfExists(Path.of(file.getAbsolutePath()));
            }
        } catch (IOException e) {
            log.error("Unable to delete file or folder {}", file.getAbsolutePath());
        }
    }

}
