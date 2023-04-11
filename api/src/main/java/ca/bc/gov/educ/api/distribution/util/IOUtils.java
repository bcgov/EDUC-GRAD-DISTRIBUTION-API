package ca.bc.gov.educ.api.distribution.util;

import ca.bc.gov.educ.api.distribution.model.dto.ProcessorData;
import ca.bc.gov.educ.api.distribution.process.BaseProcess;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public class IOUtils {

    private static final Logger logger = LoggerFactory.getLogger(IOUtils.class);
    private static final String EXCEPTION = "Error {}";

    private IOUtils() {
    }

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
     *
     * @param file
     */
    public static void removeFileOrDirectory(File file) {
        try {
            if (file.isDirectory()) {
                FileUtils.deleteDirectory(file);
            } else {
                Files.deleteIfExists(Path.of(file.getAbsolutePath()));
            }
        } catch (IOException e) {
        }
    }

    //Grad2-1931 : Creates folder structure path for files at temporary location - mchintha
    public static StringBuilder createFolderStructureInTempDirectory(ProcessorData processorData, String minCode, String schoolCategoryCode) {
        String districtCode = StringUtils.substring(minCode, 0, 3);
        String activityCode = processorData.getActivityCode();
        String transmissionMode = processorData.getTransmissionMode();
        StringBuilder directoryPathBuilder = new StringBuilder();
        StringBuilder filePathBuilder = new StringBuilder();
        try {

            if (EducDistributionApiConstants.MONTHLYDIST.equalsIgnoreCase(activityCode) || "02".equalsIgnoreCase(schoolCategoryCode)) {
                directoryPathBuilder.append(EducDistributionApiConstants.TMP_DIR).append(EducDistributionApiConstants.FILES_FOLDER_STRUCTURE).append(transmissionMode.toUpperCase()).append(EducDistributionApiConstants.DEL).append(processorData.getBatchId()).append(EducDistributionApiConstants.DEL).append(minCode).append(EducDistributionApiConstants.DEL);

            } else {
                directoryPathBuilder.append(EducDistributionApiConstants.TMP_DIR).append(EducDistributionApiConstants.FILES_FOLDER_STRUCTURE).append(transmissionMode.toUpperCase()).append(EducDistributionApiConstants.DEL).append(processorData.getBatchId()).append(EducDistributionApiConstants.DEL).append(districtCode).append(EducDistributionApiConstants.DEL).append(minCode);
            }
            Path path = Paths.get(directoryPathBuilder.toString());
            Files.createDirectories(path);

            if (EducDistributionApiConstants.MONTHLYDIST.equalsIgnoreCase(activityCode) || "02".equalsIgnoreCase(schoolCategoryCode)) {
                filePathBuilder.append(EducDistributionApiConstants.TMP_DIR).append(EducDistributionApiConstants.FILES_FOLDER_STRUCTURE).append(transmissionMode.toUpperCase()).append(EducDistributionApiConstants.DEL).append(processorData.getBatchId()).append(EducDistributionApiConstants.DEL).append(minCode).append(EducDistributionApiConstants.DEL);
            } else {
                filePathBuilder.append(EducDistributionApiConstants.TMP_DIR).append(EducDistributionApiConstants.FILES_FOLDER_STRUCTURE).append(transmissionMode.toUpperCase()).append(EducDistributionApiConstants.DEL).append(processorData.getBatchId()).append(EducDistributionApiConstants.DEL).append(districtCode).append(EducDistributionApiConstants.DEL).append(minCode);
            }
        } catch (Exception e) {
            logger.error(EXCEPTION, e.getLocalizedMessage());
        }

        return filePathBuilder;
    }

}
