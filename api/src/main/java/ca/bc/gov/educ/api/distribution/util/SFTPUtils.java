package ca.bc.gov.educ.api.distribution.util;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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


    @Value("${sftp.bcmail.location}")
    private String BC_MAIL_LOCATION;

    private static final int REMOTE_PORT = 22;
    private static final int SESSION_TIMEOUT = 10000;
    private static final int CHANNEL_TIMEOUT = 5000;
    private static final String KNOWN_HOST = "/.ssh/known_hosts";
    private static final String RSA_PUB = "/.ssh/id_rsa.pub";
    private static final String RSA_PRV = "/.ssh/id_rsa";

    private static Logger logger = LoggerFactory.getLogger(SFTPUtils.class);
    //Grad2-1931 - setting SFTP root folder location where it has to pick zip folders from, to send to BC mail - mchintha
    public boolean sftpUploadBCMail(Long batchId, String rootFolder) {
        String localFile = rootFolder + "/EDGRAD.BATCH."+batchId+".zip";
        String remoteFile = BC_MAIL_LOCATION +"EDGRAD.BATCH."+batchId+".zip";
        String localControlFile = rootFolder + "/EDGRAD.BATCH."+batchId+".txt";
        String remoteControlFile = BC_MAIL_LOCATION+"EDGRAD.BATCH."+batchId+".txt";
        Session jschSession = null;

        setupBCMailSFTP();

        try {
            JSch jsch = new JSch();
            jsch.setKnownHosts(KNOWN_HOST);
            jschSession = jsch.getSession(BCMAIL_SFTP_USERNAME, BCMAIL_REMOTE_HOST, REMOTE_PORT);
            jsch.addIdentity(RSA_PRV);
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
            logger.debug("Error {} ",e.getLocalizedMessage());
            return false;
        } finally {
            if (jschSession != null) {
                jschSession.disconnect();
            }
        }
    }
    //Grad2-2052 set common root folder for all the dis runs to pick the folders and send to BC mail. - mchintha
    public boolean sftpUploadBCMail(Long batchId, String rootFolder, String mincode) {
        String localFile = rootFolder + "/EDGRAD.BATCH."+batchId+"."+mincode+".zip";
        String remoteFile = BC_MAIL_LOCATION+"EDGRAD.BATCH."+batchId+"."+mincode+".zip";
        String localControlFile = rootFolder + "/EDGRAD.BATCH."+batchId+"."+mincode+".txt";
        String remoteControlFile = BC_MAIL_LOCATION+"EDGRAD.BATCH."+batchId+"."+mincode+".txt";
        Session jschSession = null;

        setupBCMailSFTP();

        try {
            JSch jsch = new JSch();
            jsch.setKnownHosts(KNOWN_HOST);
            jschSession = jsch.getSession(BCMAIL_SFTP_USERNAME, BCMAIL_REMOTE_HOST, REMOTE_PORT);
            jsch.addIdentity(RSA_PRV);
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
            logger.debug("Error {} ",e.getLocalizedMessage());
            return false;
        } finally {
            if (jschSession != null) {
                jschSession.disconnect();
            }
        }
    }

    private boolean setupBCMailSFTP() {
        writeFile(RSA_PRV, BCMAIL_PRIVATE_KEY);
        writeFile(RSA_PUB, BCMAIL_PUBLIC_KEY);
        writeFile(KNOWN_HOST, BCMAIL_KNOWN_HOSTS);
        return true;
    }

    public boolean sftpUploadTSW(Long batchId,String mincode,String fileName) {
        String localFile = EducDistributionApiConstants.TMP_DIR + EducDistributionApiConstants.DEL + batchId + EducDistributionApiConstants.DEL + mincode + EducDistributionApiConstants.DEL + fileName+".pdf";
        String remoteFile = "/$1$dga5037/EDUC/XTD";
        String location1 = remoteFile+"/WEB/"+fileName+".pdf";
        String location2 = remoteFile+"/TSWSFTP/"+fileName+".pdf";
        String location3 = remoteFile+"/WEB/PST/"+fileName+".pdf";
        Session jschSession = null;

        setupTSWSFTP();

        try {
            JSch jsch = new JSch();
            jsch.setKnownHosts(KNOWN_HOST);
            jschSession = jsch.getSession(TSW_SFTP_USERNAME, TSW_REMOTE_HOST, REMOTE_PORT);
            jsch.addIdentity(RSA_PRV);
            jschSession.connect(SESSION_TIMEOUT);

            Channel sftp = jschSession.openChannel("sftp");
            sftp.connect(CHANNEL_TIMEOUT);
            ChannelSftp channelSftp = (ChannelSftp) sftp;

            // transfer file from local to remote server
            channelSftp.put(localFile, location1);
            channelSftp.put(localFile, location2);
            channelSftp.put(localFile, location3);
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
        writeFile(RSA_PRV, TSW_PRIVATE_KEY);
        writeFile(RSA_PUB, TSW_PUBLIC_KEY);
        writeFile(KNOWN_HOST, TSW_KNOWN_HOSTS);
        return true;
    }

    public boolean writeFile(String filename, String content) {
        try (FileWriter fileWriter = new FileWriter(filename)){
            fileWriter.write(content);
            logger.debug("Write File Complete! - {} ",filename);
        } catch (IOException e) {
            logger.error("Write File Failed! - {}",filename);
        }
        return true;
    }
}
