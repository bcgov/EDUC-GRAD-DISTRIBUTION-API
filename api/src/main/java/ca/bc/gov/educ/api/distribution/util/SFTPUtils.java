package ca.bc.gov.educ.api.distribution.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import com.jcraft.jsch.*;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;

@Component
public class SFTPUtils {
    @Value("${sftp.bcmail.host}")
    private static String BCMAIL_REMOTE_HOST;
    @Value("${sftp.bcmail.username}")
    private static String BCMAIL_SFTP_USERNAME;
    @Value("${sftp.bcmail.priv-key}")
    private static String BCMAIL_PRIVATE_KEY;
    @Value("${sftp.bcmail.pub-key}")
    private static String BCMAIL_PUBLIC_KEY;
    @Value("${sftp.bcmail.known-host}")
    private static String BCMAIL_KNOWN_HOST;

    private static final int REMOTE_PORT = 22;
    private static final int SESSION_TIMEOUT = 10000;
    private static final int CHANNEL_TIMEOUT = 5000;

    private static Logger logger = LoggerFactory.getLogger(SFTPUtils.class);

    public static boolean sftpUpload(Long batchId) {
        String localFile = "/tmp/EDGRAD.BATCH."+batchId+".zip";
        String remoteFile = "/Inbox/Dev/EDGRAD.BATCH."+batchId+".zip";
        String localControlFile = "/tmp/EDGRAD.BATCH."+batchId+".txt";
        String remoteControlFile = "/Inbox/Dev/EDGRAD.BATCH."+batchId+".txt";
        Session jschSession = null;

        try {
            JSch jsch = new JSch();
            jsch.setKnownHosts("/.ssh/known_hosts");
            jschSession = jsch.getSession(BCMAIL_SFTP_USERNAME, BCMAIL_REMOTE_HOST, REMOTE_PORT);
            jsch.addIdentity("/.ssh/bcmail_id_rsa");
            jschSession.connect(SESSION_TIMEOUT);

            Channel sftp = jschSession.openChannel("sftp");
            sftp.connect(CHANNEL_TIMEOUT);
            ChannelSftp channelSftp = (ChannelSftp) sftp;

            // transfer file from local to remote server
            channelSftp.put(localFile, remoteFile);
            channelSftp.put(localControlFile, remoteControlFile);
            channelSftp.exit();
            return true;
        } catch (JSchException | SftpException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (jschSession != null) {
                jschSession.disconnect();
            }
        }
    }

    public static boolean setupBcmailSftp() {
        writeFile("/.ssh/bcmail_id_rsa", BCMAIL_PRIVATE_KEY);
        writeFile("/.ssh/bcmail_id_rsa.pub", BCMAIL_PUBLIC_KEY);
        writeFile("/.ssh/known_hosts", BCMAIL_KNOWN_HOST);
        return true;
    }

    public static boolean writeFile(String filename, String content) {
        try {
            FileWriter myWriter = new FileWriter(filename);
            myWriter.write(content);
            myWriter.close();
            logger.debug("Write File Complete! - " + filename);
        } catch (IOException e) {
            logger.debug("Write File Failed! - " + filename);
            e.printStackTrace();
        }
        return true;
    }
}
