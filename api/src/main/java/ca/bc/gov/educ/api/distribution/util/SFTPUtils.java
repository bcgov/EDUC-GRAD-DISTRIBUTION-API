package ca.bc.gov.educ.api.distribution.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.jcraft.jsch.*;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;

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

    private static final int REMOTE_PORT = 22;
    private static final int SESSION_TIMEOUT = 10000;
    private static final int CHANNEL_TIMEOUT = 5000;

    private static Logger logger = LoggerFactory.getLogger(SFTPUtils.class);

    public boolean sftpUploadBCMail(Long batchId) {
        String localFile = "/tmp/EDGRAD.BATCH."+batchId+".zip";
        String remoteFile = "/Inbox/Dev/EDGRAD.BATCH."+batchId+".zip";
        String localControlFile = "/tmp/EDGRAD.BATCH."+batchId+".txt";
        String remoteControlFile = "/Inbox/Dev/EDGRAD.BATCH."+batchId+".txt";
        Session jschSession = null;

        setupBCMailSFTP();

        try {
            JSch jsch = new JSch();
            jsch.setKnownHosts("/.ssh/known_hosts");
            jschSession = jsch.getSession(BCMAIL_SFTP_USERNAME, BCMAIL_REMOTE_HOST, REMOTE_PORT);
            jsch.addIdentity("/.ssh/id_rsa");
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

    private boolean setupBCMailSFTP() {
        writeFile("/.ssh/id_rsa", BCMAIL_PRIVATE_KEY);
        writeFile("/.ssh/id_rsa.pub", BCMAIL_PUBLIC_KEY);
        writeFile("/.ssh/known_hosts", BCMAIL_KNOWN_HOSTS);
        return true;
    }

    public boolean sftpUploadTSW(Long batchId) {
        String localFile = "api/target/classes/static/upload-this.file";
        String remoteFile = "/$1$dga5037/EDUC/XTD/USERS/EDUC_XTD_MGR/GRAD_TSW_TEST/uploaded-this.file";
        Session jschSession = null;

        setupTSWSFTP();

        try {
            JSch jsch = new JSch();
            jsch.setKnownHosts("/.ssh/known_hosts");
            jschSession = jsch.getSession(BCMAIL_SFTP_USERNAME, BCMAIL_REMOTE_HOST, REMOTE_PORT);
            jsch.addIdentity("/.ssh/id_rsa");
            jschSession.connect(SESSION_TIMEOUT);

            Channel sftp = jschSession.openChannel("sftp");
            sftp.connect(CHANNEL_TIMEOUT);
            ChannelSftp channelSftp = (ChannelSftp) sftp;

            // transfer file from local to remote server
            channelSftp.put(localFile, remoteFile);
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

    private boolean setupTSWSFTP() {
        writeFile("/.ssh/id_rsa", TSW_PRIVATE_KEY);
        writeFile("/.ssh/id_rsa.pub", TSW_PUBLIC_KEY);
        writeFile("/.ssh/known_hosts", TSW_KNOWN_HOSTS);
        return true;
    }

    public boolean writeFile(String filename, String content) {
        try {
            FileWriter fileWriter = new FileWriter(filename);
            fileWriter.write(content);
            fileWriter.close();
            logger.debug("Write File Complete! - " + filename);
        } catch (IOException e) {
            logger.debug("Write File Failed! - " + filename);
            e.printStackTrace();
        }
        return true;
    }
}
