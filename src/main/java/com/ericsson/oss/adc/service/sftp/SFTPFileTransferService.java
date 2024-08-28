/*******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/
package com.ericsson.oss.adc.service.sftp;

import java.io.File;
import java.io.InputStream;
import java.net.ConnectException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import com.ericsson.oss.adc.config.EventFileDownloadConfiguration;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SFTPFileTransferService {

    private static final Logger LOG = LoggerFactory.getLogger(SFTPFileTransferService.class);
    private static final String CONNECTION_EXCEPTION_FILE_DOWNLOAD_RETRIES_EXCEEDED_SFTP = "EventFileDownloadRetriesExceededSftpIssue";
    private static final String CONNECTION_EXCEPTION_FILE_DOWNLOAD_RETRIES_EXCEEDED_EXCEPTION = "EventFileDownloadRetriesExceededException";

    private ENMScriptingVMLoadBalancer loadBalancer;
    private ChannelSftp channelSftp;
    private int numberOfSftpConnectRetries;
    private int numberOfEventFileDownloadRetries;
    private int sftpSessionTimeoutMs;
    private int sftpConnectBackoffMs;

    public SFTPFileTransferService(ENMScriptingVMLoadBalancer loadBalancer,
                                   EventFileDownloadConfiguration eventFileDownloadConfiguration) {
        this.loadBalancer = loadBalancer;
        this.numberOfSftpConnectRetries = eventFileDownloadConfiguration.getNumberOfSftpConnectRetries();
        this.numberOfEventFileDownloadRetries = eventFileDownloadConfiguration.getNumberOfEventFileDownloadRetries();
        this.sftpSessionTimeoutMs = eventFileDownloadConfiguration.getSftpSessionTimeoutMs();
        this.sftpConnectBackoffMs = eventFileDownloadConfiguration.getSftpConnectBackoffMs();
    }


    /**
     * Method to make sftp connection to the host
     *
     * @param port port number of the host
     * @param userName username for authentication
     * @param password password to authenticate with the host
     */
    public boolean connect(final Optional<Integer> port, final Optional<String> userName,
                           final Optional<String> password) {
        LOG.debug("LoadBalancer info '{}'", loadBalancer);

        if (!loadBalancer.getAllOnlineScriptingVMs().isEmpty()
                && userName.isPresent() && port.isPresent() && password.isPresent()) {
            return createSFTPChannel(port.get(), userName.get(), password.get());
        }
        LOG.error("SFTP connection details are not valid, unable to make connection");
        return false;
    }

    /**
     * Download a file from remote path to local path
     *
     * @param remoteFilePath remote path of file to be downloaded
     * @param localFilePath  local path for downloaded file to be stored
     * @return Optional {@link Optional<File>} object of type File representing the downloaded file locally
     */
    public Optional<File> downloadFile(final String remoteFilePath, final String localFilePath) throws ConnectException {
        File localFile = null;

        for (int i = 1; i <= numberOfEventFileDownloadRetries; i++) {
            try (final InputStream inputStream = channelSftp.get(remoteFilePath)) {
                LOG.debug("Successfully downloaded file {} ", remoteFilePath);
                localFile = new File(localFilePath);
                FileUtils.copyInputStreamToFile(inputStream, localFile);
                break;
            } catch (final SftpException exception) { // SFTP issue
                //Exception ID for a SFTP exception where no file is found.See JavaDoc for more info:https://epaul.github.io/jsch-documentation/javadoc/constant-values.html#com.jcraft.jsch.ChannelSftp.SSH_FX_NO_SUCH_FILE
                if (exception.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    LOG.error("Unable to download file, no file found for {}. Event file will be skipped", remoteFilePath);
                    return Optional.empty();
                }
                LOG.error("Try {}/{}: Failed to download file {}. Error msg: '{}'",
                        i, numberOfEventFileDownloadRetries, remoteFilePath, exception.getMessage());
                LOG.debug("Try {}/{}: Stack Trace - Failed to download file {}. File will be deleted: ",
                        i, numberOfEventFileDownloadRetries, remoteFilePath, exception);
                throwConnectionExceptionIfRetriesExhausted(i, true);
            } catch (final Exception exception) { // File related issues
                LOG.error("Try {}/{}: Failed to write output file {}. Error msg: '{}'",
                        i, numberOfEventFileDownloadRetries, localFilePath, exception.getMessage());
                LOG.debug("Try {}/{}: Stack Trace - Failed to write output file {}. File will be deleted: ",
                        i, numberOfEventFileDownloadRetries, localFilePath, exception);
                throwConnectionExceptionIfRetriesExhausted(i, false);
            }
        }
        return Optional.ofNullable(localFile);
    }

    private void throwConnectionExceptionIfRetriesExhausted(final int attemptedEventFiledDownloadRetries, final boolean isSFTPException) throws ConnectException {
        if (attemptedEventFiledDownloadRetries == numberOfEventFileDownloadRetries) {
            if (isSFTPException) {
                throw new ConnectException(CONNECTION_EXCEPTION_FILE_DOWNLOAD_RETRIES_EXCEEDED_SFTP);
            } else {
                throw new ConnectException(CONNECTION_EXCEPTION_FILE_DOWNLOAD_RETRIES_EXCEEDED_EXCEPTION);
            }
        }
    }

    private boolean createSFTPChannel(final int port, final String username, final String password) {
        while (!loadBalancer.getAllOnlineScriptingVMs().isEmpty()) {
            final String host = loadBalancer.getNextScriptingVM();
            for (long i = 1; i <= numberOfSftpConnectRetries; i++) {
                try {
                    LOG.debug("Creating SFTP channel: Attempt {} of {} for host {}",
                            i, numberOfSftpConnectRetries, host);
                    final JSch jSch = getJsch();
                    final Session session = jSch.getSession(username, host, port);
                    session.setConfig("StrictHostKeyChecking", "no"); //TODO check this
                    session.setPassword(password);
                    session.connect(sftpSessionTimeoutMs);

                    final Channel channel = session.openChannel("sftp");
                    channel.connect();
                    channelSftp = (ChannelSftp) channel;

                    LOG.debug("SFTP Channel successfully created for {}@{}:{}", username, host, port);
                    return true;
                } catch (final Exception exception) {
                    LOG.error("Try {}/{}: Failed to create SFTP channel. Error msg: '{}'",
                            i, numberOfSftpConnectRetries, exception.getMessage());
                    LOG.debug("Try {}/{}: Stack trace - Failed to create SFTP channel",
                            i, numberOfEventFileDownloadRetries, exception);
                }

                LOG.debug("Will attempt to recreate SFTP channel in {}", sftpConnectBackoffMs);
                try {
                    TimeUnit.MICROSECONDS.sleep(sftpConnectBackoffMs);
                } catch (final InterruptedException exception) {
                    LOG.error("Try {}/{}: Exception while sleeping thread. Error msg: '{}'", i, numberOfSftpConnectRetries, exception.getMessage());
                    LOG.debug("Try {}/{}: Stack trace - Exception while sleeping thread", i, numberOfEventFileDownloadRetries, exception);
                    Thread.currentThread().interrupt();
                }
            }
            LOG.debug("Disconnecting from host {}. Will try connecting to another host", host);
            loadBalancer.removeVMFromOnlineScriptingVMs(host);
        }
        loadBalancer.resetAllOnlineScriptingVMs();
        return false;
    }

    /**
     * Check if the SFTP channel for this object instance is open and connected
     *
     * @return boolean representing the connection open status
     */
    public boolean isConnectionOpen() {
        return (channelSftp != null) && (channelSftp.isConnected());
    }

    /**
     * Disconnect the SFTP channel of this object instance
     */
    public void disconnectChannel() {
        try {
            disconnectChannelSftp(channelSftp);
        } catch (final JSchException exception) {
            LOG.error("SFTP channel failed to disconnect correctly: {}", exception.getMessage());
            LOG.debug("Stack trace - SFTP channel failed to disconnect correctly: ", exception);
        }
    }

    /**
     * Get a new JSch instance
     *
     * @return new JSch instance
     */
    public JSch getJsch(){
        //Used in test
        return new JSch();
    }

    protected boolean disconnectChannelSftp(final ChannelSftp channelSftp) throws JSchException {
        // Disconnect the sftp channel first and then the JSch session
        LOG.debug("Disconnecting SFTP Channel and JSch session");
        if (channelSftp == null)
            return true;
        if (channelSftp.isConnected())
            channelSftp.disconnect();
        if (channelSftp.getSession() != null)
            channelSftp.getSession().disconnect();
        LOG.debug("Disconnected SFTP Channel and JSch session");
        return true;
    }
}
