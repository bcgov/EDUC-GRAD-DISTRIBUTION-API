package ca.bc.gov.educ.api.distribution.util;

import com.jcraft.jsch.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants.TMP_DIR;

@Component
public class SFTPUtils {
    @Value("${sftp.bcmail.host}")
    private String BCMAIL_REMOTE_HOST;
    @Value("${sftp.bcmail.username}")
    private String BCMAIL_SFTP_USERNAME;
    @Value("${sftp.bcmail.priv-key}")
    private String BCMAIL_PRIVATE_KEY;
    @Value("${sftp.bcmail.pub-key}")
    private String BCMAIL_PUBLIC_KEY;
    @Value("${sftp.bcmail.known-hosts}")
    private String BCMAIL_KNOWN_HOSTS;

    @Value("${sftp.tsw.host}")
    private String TSW_REMOTE_HOST;
    @Value("${sftp.tsw.username}")
    private String TSW_SFTP_USERNAME;
    @Value("${sftp.tsw.priv-key}")
    private String TSW_PRIVATE_KEY;
    @Value("${sftp.tsw.pub-key}")
    private String TSW_PUBLIC_KEY;
    @Value("${sftp.tsw.known-hosts}")
    private String TSW_KNOWN_HOSTS;


    @Value("${sftp.bcmail.location}")
    private String BC_MAIL_LOCATION;

    private static final int REMOTE_PORT = 22;
    private static final int SESSION_TIMEOUT = 10000;
    private static final int CHANNEL_TIMEOUT = 5000;
    private static final String KNOWN_HOST = "/.ssh/known_hosts";
    private static final String RSA_PUB = "/.ssh/id_rsa.pub";
    private static final String RSA_PRV = "/.ssh/id_rsa";

    private static Logger logger = LoggerFactory.getLogger(SFTPUtils.class);

    public boolean sftpUploadBCMail(Long batchId) {
        return sftpUploadBCMail(batchId, TMP_DIR);
    }

    public boolean sftpUploadBCMail(Long batchId, String rootFolder) {
        return sftpUploadBCMail(batchId, rootFolder, null);
    }

    /**
     * Method specific for uploading to BCMail
     * @param batchId the batch id
     * @param rootFolder the root location of the cached files
     * @return true if successful
     */
    public boolean sftpUploadBCMail(Long batchId, String rootFolder, String mincode) {
        Map<String, List<String>> files = new HashMap<>();
        String mincodePath = StringUtils.isBlank(mincode) ? "" : mincode + ".";
        String zipFile = "/EDGRAD.BATCH." + batchId + mincodePath + ".zip";
        String controlFile = "/EDGRAD.BATCH." + batchId + mincodePath + ".txt";
        String localZipFile = rootFolder + zipFile;
        String remoteZipFile = BC_MAIL_LOCATION + zipFile;
        String localControlFile = rootFolder + controlFile;
        String remoteControlFile = BC_MAIL_LOCATION + controlFile;
        files.computeIfAbsent(localZipFile, v -> new ArrayList<>()).add(remoteZipFile);
        files.computeIfAbsent(localControlFile, v -> new ArrayList<>()).add(remoteControlFile);
        // GRAD2-2224 - Short term solution to also upload files to dev folder for business retrieval.
        if(!BC_MAIL_LOCATION.toLowerCase().contains("dev")){
            // also upload to dev
            String dev = "/Inbox/Dev/";
            files.computeIfAbsent(formatPath(localZipFile), v -> new ArrayList<>()).add(formatPath(dev + zipFile));
            files.computeIfAbsent(formatPath(localControlFile), v -> new ArrayList<>()).add(formatPath(dev + controlFile));
        }
        return sftpUpload(files, BCMAIL_SFTP_USERNAME, BCMAIL_REMOTE_HOST, REMOTE_PORT, BCMAIL_PRIVATE_KEY, BCMAIL_PUBLIC_KEY, BCMAIL_KNOWN_HOSTS);
    }

    /**
     * Method specific for uploading files to TSW
     * @param batchId batch id
     * @param mincode mincode
     * @param fileName the filename
     * @return true if successful
     */
    public boolean sftpUploadTSW(Long batchId,String mincode,String fileName) {
        Map<String, List<String>> files = new HashMap<>();
        String localFile = formatPath(EducDistributionApiConstants.TMP_DIR + EducDistributionApiConstants.DEL + batchId + EducDistributionApiConstants.DEL + mincode + EducDistributionApiConstants.DEL + fileName+".pdf");
        String remoteFile = "/$1$dga5037/EDUC/XTD";
        String location1 = remoteFile+"/WEB/"+fileName+".pdf";
        String location2 = remoteFile+"/TSWSFTP/"+fileName+".pdf";
        String location3 = remoteFile+"/WEB/PST/"+fileName+".pdf";
        files.computeIfAbsent(localFile, v -> new ArrayList<>()).add(location1);
        files.computeIfAbsent(localFile, v -> new ArrayList<>()).add(location2);
        files.computeIfAbsent(localFile, v -> new ArrayList<>()).add(location3);
        return sftpUpload(files, TSW_SFTP_USERNAME, TSW_REMOTE_HOST, REMOTE_PORT, TSW_PRIVATE_KEY, TSW_PUBLIC_KEY, TSW_KNOWN_HOSTS);
    }

    /**
     * Method for uploading files to an sftp server
     * @param files a map of local -> remote file paths. Use of Map<String, List<String>> enables the key (the file to upload)
     *              to have multiple upload locations (the List<String> represents the upload locations)
     * @param userName the username to log into the sftp server
     * @param host remote host
     * @param port the port to use (usually 22)
     * @param privateKey the private key of the client
     * @param publicKey the public key of the host
     * @param knownHosts known hosts
     * @return true if successful
     */
    public boolean sftpUpload(
            Map<String, List<String>> files,
            String userName,
            String host,
            int port,
            String privateKey,
            String publicKey,
            String knownHosts) {

        // set up keys, etc.
        writeFile(RSA_PRV, privateKey);
        writeFile(RSA_PUB, publicKey);
        writeFile(KNOWN_HOST, knownHosts);

        // session
        Session jschSession = null;
        try {
            JSch jsch = new JSch();
            jsch.setKnownHosts(KNOWN_HOST);
            jschSession = jsch.getSession(userName, host, port);
            jsch.addIdentity(RSA_PRV);
            jschSession.connect(SESSION_TIMEOUT);

            Channel sftp = jschSession.openChannel("sftp");
            sftp.connect(CHANNEL_TIMEOUT);
            ChannelSftp channelSftp = (ChannelSftp) sftp;

            // transfer file(s) from local to remote server
            for (Map.Entry<String, List<String>> entry : files.entrySet()) {
                entry.getValue().forEach(
                        file -> {
                            try {
                                channelSftp.put(entry.getKey(), file);
                                logger.debug("Transferring file via ftp: {}", entry.getKey());
                            } catch (SftpException e) {
                                logger.error("Error during sftp transfer {} ", e.getLocalizedMessage());
                            }
                        }
                );
            }
            channelSftp.exit();
            return true;
        } catch (JSchException e) {
            logger.error("Error during sftp transfer {} ", e.getLocalizedMessage());
            return false;
        } finally {
            if (jschSession != null) {
                jschSession.disconnect();
            }
        }
    }

    private boolean writeFile(String filename, String content) {
        try (FileWriter fileWriter = new FileWriter(filename)){
            fileWriter.write(content);
            logger.debug("Write File Complete! - {} ", filename);
        } catch (IOException e) {
            logger.error("SFTP ERROR: Write File Failed! - {}", filename);
            e.printStackTrace();
        }
        return true;
    }

    private String formatPath(String path){
        return path.replaceAll("/+", "/");
    }

}
