package ca.bc.gov.educ.api.distribution.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class DeleteExpiredFilesFileVisitorImpl implements FileVisitor<Path> {

    private static final Logger logger = LoggerFactory.getLogger(DeleteExpiredFilesFileVisitorImpl.class);
    private final PathMatcher pathMatcher;
    private final LocalDateTime fileExpiry;

    public DeleteExpiredFilesFileVisitorImpl(String filter, LocalDateTime fileExpiry){
        this.pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + filter);
        this.fileExpiry = fileExpiry;
    }
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if(fileOrDirectoryIsExpired(file)){
            logger.info("Deleting: {}", file.getFileName());
            Files.delete(file);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if(fileOrDirectoryIsExpired(dir)){
            logger.info("Deleting: {}", dir.getFileName());
            Files.delete(dir);
        }
        return FileVisitResult.CONTINUE;
    }

    /**
     * Checks whether file or directory is expired
     * @param file the file to check
     * @return whether or not to delete
     */
    private boolean fileOrDirectoryIsExpired(Path file){
        if(!pathMatcher.matches(file.getFileName())){
            File theFile = file.toFile();
            LocalDateTime lastModified = LocalDateTime.ofInstant(Instant.ofEpochMilli(theFile.lastModified()), ZoneId.systemDefault());
            return lastModified.isBefore(fileExpiry);
        }
        return false;
    }
}
