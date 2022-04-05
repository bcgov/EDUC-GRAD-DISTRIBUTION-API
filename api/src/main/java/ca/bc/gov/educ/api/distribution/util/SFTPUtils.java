package ca.bc.gov.educ.api.distribution.util;

import org.springframework.beans.factory.annotation.Value;
import com.jcraft.jsch.*;
import org.springframework.stereotype.Component;

@Component
public class SFTPUtils {
    @Value("${sftp.remote.host}")
    private String REMOTE_HOST;
    @Value("${sftp.username}")
    private String SFTP_USERNAME;
    private static final int REMOTE_PORT = 22;
    private static final int SESSION_TIMEOUT = 10000;
    private static final int CHANNEL_TIMEOUT = 5000;

    public boolean sftpUpload(Long batchId) {
        String localFile = "/tmp/EDGRAD.BATCH."+batchId+".zip";
        String remoteFile = "/Inbox/Dev/EDGRAD.BATCH."+batchId+".zip";
        Session jschSession = null;

        try {
            JSch jsch = new JSch();
            jschSession = jsch.getSession(SFTP_USERNAME, REMOTE_HOST, REMOTE_PORT);
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
}
