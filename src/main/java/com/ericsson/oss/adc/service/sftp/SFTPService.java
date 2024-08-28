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

import com.ericsson.oss.adc.config.EventFileDownloadConfiguration;
import com.ericsson.oss.adc.models.InputMessage;
import com.ericsson.oss.adc.models.connected.systems.ConnectionProperties;
import com.ericsson.oss.adc.models.connected.systems.Subsystem;
import com.ericsson.oss.adc.service.connected.systems.ConnectedSystemsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class SFTPService {

    private static final Logger LOG = LoggerFactory.getLogger(SFTPService.class);

    private static final String UNIX_SEPARATOR = "/";
    private static final String CONNECTION_EXCEPTION_SFTP_CONNECTION_NEVER_ESTABLISHED = "SftpConnectionNeverEstablishedNoFilesDownloadedOrProcessed";

    public final Counter numSuccessfulFilesTransferred;
    public final Counter numFailedFilesTransferred;
    public final Counter numFilesDeletedDueToConnectionException;
    public final Counter transferredFileDataVolume;
    private Map<String, Subsystem> subsystemsDetailsMap;

    @Value("${temp-directory}")
    private String tempDirectory;

    @Autowired
    private ConnectedSystemsService connectedSystemsService;

    @Autowired
    private EventFileDownloadConfiguration eventFileDownloadConfiguration;

    public SFTPService(MeterRegistry meterRegistry) {
        this.numSuccessfulFilesTransferred = meterRegistry.counter("eric.oss.5gpmevt.filetx.proc:num.successful.file.transfer");
        this.numFailedFilesTransferred = meterRegistry.counter("eric.oss.5gpmevt.filetx.proc:num.failed.file.transfer");
        this.transferredFileDataVolume = meterRegistry.counter("eric.oss.5gpmevt.filetx.proc:transferred.file.data.volume");
        this.numFilesDeletedDueToConnectionException = meterRegistry.counter("eric.oss.5gpmevt.filetx.proc:num.files.deleted.sftp.connection.exception");
    }

    /**
     * Set up SFTP connection, download event files as a batch & disconnect SFTP connection.
     * For the purpose of parallel processing / method retry, each SFTP connection established will be unique.
     * And so each SFTPFileTransferService will have a unique reference to ENMScriptingVMLoadBalancer (list of scripting VMs).
     *
     * @param parsedConsumerRecordsInputMessages A List {@link List<InputMessage>} of InputMessages containing file paths on the remote server
     * @return List {@link List<InputMessage>} of InputMessages representing successfully downloaded files appended to their corresponding input message
     */
    public List<InputMessage> setUpSFTPConnectionAndDownloadFiles(final List<InputMessage> parsedConsumerRecordsInputMessages) throws ConnectException {
        ENMScriptingVMLoadBalancer loadBalancer = new ENMScriptingVMLoadBalancer();
        SFTPFileTransferService sftpFileTransferService = new SFTPFileTransferService(loadBalancer, eventFileDownloadConfiguration);

        if (!isConnectionOpen(sftpFileTransferService)
                && !setUpConnectionToSFTPServerUsingConnectedSystemsResponse(sftpFileTransferService, loadBalancer)) {
            LOG.error("Failed to establish SFTP connection. No files in batch were downloaded or processed");
            throw new ConnectException(CONNECTION_EXCEPTION_SFTP_CONNECTION_NEVER_ESTABLISHED);
        }

        try {
            LOG.debug("Connection to SFTP Server open, continuing with file download");
            return download(sftpFileTransferService, parsedConsumerRecordsInputMessages);
        } finally {
            LOG.debug("Disconnecting the SFTP channel");
            sftpFileTransferService.disconnectChannel();
        }
    }

    /**
     * Setup connection to SFTP server using Connected Systems response - Fetches SubsystemDetails at first attempt or if available uses existing SubsystemDetails
     *
     * @param sftpFileTransferService instance of SFTPFileTransferService
     * @param loadBalancer instance of ENMScriptingVMLoadBalancer
     * @return boolean representing if connection was successful or not
     */
    public boolean setUpConnectionToSFTPServerUsingConnectedSystemsResponse(final SFTPFileTransferService sftpFileTransferService,
                                                                            final ENMScriptingVMLoadBalancer loadBalancer) {

        if (subsystemsDetailsMap == null || subsystemsDetailsMap.isEmpty()) {
            subsystemsDetailsMap = connectedSystemsService.getSubsystemDetails();
            LOG.info("Requested Subsystems from Connected Systems");
        }

        if (subsystemsDetailsMap.isEmpty()) {
            LOG.error("Unable to open connection since no connected subsystem details available");
            return false;
        } else {
            final ConnectionProperties connectionProperties = connectedSystemsService.getConnectionPropertiesBySubsystemsName(subsystemsDetailsMap);
            loadBalancer.setScriptingVMs(connectionProperties.getScriptingVMs());
            boolean connected = connect(sftpFileTransferService,
                    connectionProperties.getSftpPort(),
                    connectionProperties.getUsername(),
                    connectionProperties.getPassword());

            if (!connected) {
                //TODO temporary measure to clear out cached sub details in case of password change. Full refactor during DynamicENM handling /Shaun
                subsystemsDetailsMap.clear();
                return false;
            } else {
                return true;
            }
        }
    }

    /**
     * Connect to SFTP server
     *
     * @param sftpFileTransferService instance of SFTPFileTransferService
     * @param port port for connection
     * @param username username for connection
     * @param password password for connection
     * @return boolean representing if connection was successful or not
     */
    public boolean connect(final SFTPFileTransferService sftpFileTransferService, final int port, final String username, final String password) {
        return sftpFileTransferService.connect(Optional.of(port),
                Optional.of(username),
                Optional.of(password));
    }

    /**
     * Download a batch of files from SFTP service using the object instance's SFTP connection and append them to the corresponding InputMessage.
     *
     * @param sftpFileTransferService instance of SFTPFileTransferService
     * @param inputMessages A List of {@link List<InputMessage>} InputMessages containing file paths on the remote server
     * @return A List {@link List<InputMessage>} of InputMessages representing successfully downloaded files appended to their corresponding input message
     */
    public List<InputMessage> download(final SFTPFileTransferService sftpFileTransferService, final List<InputMessage> inputMessages) throws ConnectException {
        List<InputMessage> downloadedInputMessages = new ArrayList<>();
        try {
            for (InputMessage inputMessage : inputMessages) {
                final String remoteFilePath = inputMessage.getFileLocation();
                final String localFilePath = tempDirectory + UNIX_SEPARATOR + new File(remoteFilePath).getName();
                LOG.debug("Downloading '{}' to '{}'", remoteFilePath, localFilePath);
                Optional<File> optionalFile = sftpFileTransferService.downloadFile(remoteFilePath, localFilePath);
                if (optionalFile.isPresent()) {
                    File downloadedFile = optionalFile.get();
                    inputMessage.setDownloadedFile(downloadedFile);
                    downloadedInputMessages.add(inputMessage);
                    numSuccessfulFilesTransferred.increment();
                    transferredFileDataVolume.increment(downloadedFile.length());
                } else {
                    numFailedFilesTransferred.increment();
                }
            }
        } catch (ConnectException exception) {
            for (InputMessage messageWithFileToDelete : downloadedInputMessages) {
                deleteFile(messageWithFileToDelete);
            }
            throw exception;
        }
        return downloadedInputMessages;
    }

    protected void deleteFile(final InputMessage inputMessageWithFileToDelete) {
        final String remoteFilePath = inputMessageWithFileToDelete.getFileLocation();
        final String localFilePath = tempDirectory + UNIX_SEPARATOR + new File(remoteFilePath).getName();
        try {
            Path filePath = new File(localFilePath).toPath();
            LOG.info("Deleting file {}", filePath);
            Files.delete(filePath);
            numFilesDeletedDueToConnectionException.increment();
        } catch (Exception e) {
            LOG.error("Error Attempting to delete file: {}", e.getMessage());
            LOG.debug("Stack trace - Error Attempting to delete file: ", e);
        }
    }

    /**
     * Checks if the SFTP connection is open
     *
     * @param sftpFileTransferService instance of SFTPFileTransferService
     * @return boolean representing of SFTP connection is open or not
     */
    public boolean isConnectionOpen(final SFTPFileTransferService sftpFileTransferService) {
        return sftpFileTransferService.isConnectionOpen();
    }

}
