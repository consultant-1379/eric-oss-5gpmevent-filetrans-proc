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
import com.googlecode.catchexception.apis.BDDCatchException;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import java.io.File;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.github.stefanbirkner.fakesftpserver.lambda.FakeSftpServer.withSftpServer;
import static com.googlecode.catchexception.CatchException.caughtException;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.spy;
@SpringBootTest(classes = {EventFileDownloadConfiguration.class, SFTPFileTransferService.class, ENMScriptingVMLoadBalancer.class})
@AutoConfigureWebClient(registerRestTemplate = true)
public class SFTPFileTransferServiceTest {
    private static final String UNIX_SEPARATOR = "/";
    private static final int PORT = 5678;
    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private static final String ENM_FILE_PATH = "/ericsson/pmic1/CELLTRACE/SubNetwork=ONRM_ROOT_MO_R,SubNetwork=5G,MeContext=NE00000650,ManagedElement=NE00000650/";
    private static final String FILE_NAME = "A20200824.1330+0900-1345+0900_SubNetwork=ONRM_ROOT_MO_R,SubNetwork=5G,MeContext=NE00000650,ManagedElement=NE00000650_celltracefile_CUCP0_1_1.gpb.gz";
    private static final String SCRIPTING_VM = "localhost";
    private static final String CONNECTION_EXCEPTION_FILE_DOWNLOAD_RETRIES_EXCEEDED_SFTP = "EventFileDownloadRetriesExceededSftpIssue";
    private static final String CONNECTION_EXCEPTION_FILE_DOWNLOAD_RETRIES_EXCEEDED_EXCEPTION = "EventFileDownloadRetriesExceededException";

    @Mock
    private JSch jSch;

    @Mock
    private ChannelSftp channelSftp;

    @Mock
    private Session session;

    @Value("${temp-directory}")
    private String tempDirectory;

    @Autowired
    private EventFileDownloadConfiguration eventFileDownloadConfiguration;
    private ENMScriptingVMLoadBalancer loadBalancer;
    private SFTPFileTransferService sftpFileTransferService;


    @BeforeEach
    public void init() {
        loadBalancer = new ENMScriptingVMLoadBalancer();
        loadBalancer.setScriptingVMs(Arrays.asList(SCRIPTING_VM));
        sftpFileTransferService = new SFTPFileTransferService(loadBalancer, eventFileDownloadConfiguration);
    }

    @Test
    @DisplayName("Verify a successful SFTP connection.")
    public void verifySuccessfulConnection() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);

            assertTrue(sftpFileTransferService.connect(Optional.of(PORT), Optional.of(USER), Optional.of(PASSWORD)));
            assertTrue(sftpFileTransferService.isConnectionOpen());
        });
    }

    @Test
    @DisplayName("Verify an unsuccessful SFTP connection due to invalid username.")
    public void verifyUnsuccessfulConnectionWithInvalidUsername() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER + 1, PASSWORD);

            assertFalse(sftpFileTransferService.connect(Optional.of(PORT), Optional.of(USER), Optional.of(PASSWORD)));
            assertFalse(sftpFileTransferService.isConnectionOpen());
        });
    }

    @Test
    @DisplayName("Verify an unsuccessful SFTP connection due to invalid password.")
    public void verifyUnsuccessfulConnectionWithInvalidPassword() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD + 1);

            assertFalse(sftpFileTransferService.connect(Optional.of(PORT), Optional.of(USER), Optional.of(PASSWORD)));
            assertFalse(sftpFileTransferService.isConnectionOpen());
        });
    }

    @Test
    @DisplayName("Verify an unsuccessful SFTP connection due to missing params.")
    public void verifyUnsuccessfulConnectionWithMissingParams() throws Exception {
        assertFalse(sftpFileTransferService.connect(Optional.empty(), Optional.empty(), Optional.empty()));
        assertFalse(sftpFileTransferService.isConnectionOpen());
    }

    @Test
    @DisplayName("Verify a successful SFTP reconnection.")
    public void verifySuccessfulReconnection() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);

            assertTrue(sftpFileTransferService.connect(Optional.of(PORT), Optional.of(USER), Optional.of(PASSWORD)));
            assertTrue(sftpFileTransferService.isConnectionOpen());

            sftpFileTransferService.disconnectChannel();
            server.addUser(USER + 1, PASSWORD);

            assertTrue(sftpFileTransferService.connect(Optional.of(PORT), Optional.of(USER + 1), Optional.of(PASSWORD)));
            assertTrue(sftpFileTransferService.isConnectionOpen());
        });
    }

    @Test
    @DisplayName("Verify an unsuccessful SFTP reconnection due to invalid username during reconnection.")
    public void verifyUnsuccessfulReconnectionWithInvalidUsernameSecondTime() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);

            assertTrue(sftpFileTransferService.connect(Optional.of(PORT), Optional.of(USER), Optional.of(PASSWORD)));
            assertTrue(sftpFileTransferService.isConnectionOpen());

            sftpFileTransferService.disconnectChannel();
            server.addUser(USER + 1, PASSWORD);

            assertFalse(sftpFileTransferService.connect(Optional.of(PORT), Optional.of(USER + 2), Optional.of(PASSWORD)));
            assertFalse(sftpFileTransferService.isConnectionOpen());
        });
    }

    @Test
    @DisplayName("Verify an unsuccessful SFTP reconnection due to invalid password during reconnection.")
    public void verifyUnsuccessfulReconnectionWithInvalidPasswordSecondTime() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);

            assertTrue(sftpFileTransferService.connect(Optional.of(PORT), Optional.of(USER), Optional.of(PASSWORD)));
            assertTrue(sftpFileTransferService.isConnectionOpen());

            sftpFileTransferService.disconnectChannel();
            server.addUser(USER + 1, PASSWORD + 1);

            assertFalse(sftpFileTransferService.connect(Optional.of(PORT), Optional.of(USER + 1), Optional.of(PASSWORD)));
            assertFalse(sftpFileTransferService.isConnectionOpen());
        });
    }

    @Test
    @DisplayName("Verify a successful file download.")
    public void verifySuccessfulFileDownload() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);

            final String remoteFilePath = ENM_FILE_PATH + FILE_NAME;
            final String localFilePath = tempDirectory + UNIX_SEPARATOR + new File(remoteFilePath).getName();
            server.putFile(remoteFilePath, "Content of file", UTF_8);

            assertTrue(sftpFileTransferService.connect(Optional.of(PORT), Optional.of(USER), Optional.of(PASSWORD)));
            assertTrue(sftpFileTransferService.isConnectionOpen());
            assertNotNull(sftpFileTransferService.downloadFile(remoteFilePath, localFilePath));

            // cleanup
            Path expectedDownloadedFilePath = new File(tempDirectory + "/" + FILE_NAME).toPath();
            Files.delete(expectedDownloadedFilePath);
            assertThat(expectedDownloadedFilePath).doesNotExist();
        });
    }

    @Test
    @DisplayName("Verify an unsuccessful file download because file is not on server.")
    public void verifyUnsuccessfulFileDownload() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);

            final String remoteFilePath = ENM_FILE_PATH + FILE_NAME;
            final String localFilePath = tempDirectory + UNIX_SEPARATOR + new File(remoteFilePath).getName();

            assertTrue(sftpFileTransferService.connect(Optional.of(PORT), Optional.of(USER), Optional.of(PASSWORD)));
            assertTrue(sftpFileTransferService.isConnectionOpen());
            assertThat(sftpFileTransferService.downloadFile(remoteFilePath, localFilePath)).isEmpty();
        });
    }

    @Test
    @DisplayName("Verify an unsuccessful file download because of invalid local file path.")
    public void verifyUnsuccessfulFileDownloadInvalidLocalFilePath() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);

            final String remoteFilePath = ENM_FILE_PATH + FILE_NAME;
            final String localFilePath = null;
            server.putFile(remoteFilePath, "Content of file", UTF_8);

            assertTrue(sftpFileTransferService.connect(Optional.of(PORT), Optional.of(USER), Optional.of(PASSWORD)));
            assertTrue(sftpFileTransferService.isConnectionOpen());

            BDDCatchException.when(() -> sftpFileTransferService.downloadFile(remoteFilePath, localFilePath));

            AssertionsForClassTypes.assertThat(caughtException()).isInstanceOf(ConnectException.class);
            assertTrue(caughtException().getMessage().equals(CONNECTION_EXCEPTION_FILE_DOWNLOAD_RETRIES_EXCEEDED_EXCEPTION));
        });
    }
    @Test
    @DisplayName("Verify an unsuccessful batch download because of channel disconnection resulting in connect exception being thrown")
    public void verifyUnsuccessfulBatchDownload_channelDisconnection() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);

            final String remoteFilePath = ENM_FILE_PATH + FILE_NAME;
            final String localFilePath = tempDirectory + UNIX_SEPARATOR + new File(remoteFilePath).getName();
            server.putFile(remoteFilePath, "Content of file", UTF_8);

            assertTrue(sftpFileTransferService.connect(Optional.of(PORT), Optional.of(USER), Optional.of(PASSWORD)));
            assertTrue(sftpFileTransferService.isConnectionOpen());
            assertNotNull(sftpFileTransferService.downloadFile(remoteFilePath, localFilePath));

            sftpFileTransferService.disconnectChannel();
            BDDCatchException.when(() -> sftpFileTransferService.downloadFile(remoteFilePath, localFilePath));

            AssertionsForClassTypes.assertThat(caughtException()).isInstanceOf(ConnectException.class);
            assertTrue(caughtException().getMessage().equals(CONNECTION_EXCEPTION_FILE_DOWNLOAD_RETRIES_EXCEEDED_SFTP));

            // cleanup
            Path expectedDownloadedFilePath = new File(tempDirectory + "/" + FILE_NAME).toPath();
            Files.delete(expectedDownloadedFilePath);
            assertThat(expectedDownloadedFilePath).doesNotExist();
        });
    }

    @Test
    @DisplayName("Verify an unsuccessful batch download because of invalid local file path causing a connect exception to be thrown.")
    public void verifyUnsuccessfulBatchDownloadInvalidLocalFilePath_exceptionThrown() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);

            final String remoteFilePath = ENM_FILE_PATH + FILE_NAME;
            final String localFilePathBad = null;
            final String localFilePathGood = tempDirectory + UNIX_SEPARATOR + new File(remoteFilePath).getName();

            server.putFile(remoteFilePath, "Content of file", UTF_8);

            assertTrue(sftpFileTransferService.connect(Optional.of(PORT), Optional.of(USER), Optional.of(PASSWORD)));
            assertTrue(sftpFileTransferService.isConnectionOpen());

            assertNotNull(sftpFileTransferService.downloadFile(remoteFilePath, localFilePathGood));
            BDDCatchException.when(() -> sftpFileTransferService.downloadFile(remoteFilePath, localFilePathBad));

            AssertionsForClassTypes.assertThat(caughtException()).isInstanceOf(ConnectException.class);
            assertTrue(caughtException().getMessage().equals(CONNECTION_EXCEPTION_FILE_DOWNLOAD_RETRIES_EXCEEDED_EXCEPTION));

            // cleanup
            Path expectedDownloadedFilePath = new File(tempDirectory + "/" + FILE_NAME).toPath();
            Files.delete(expectedDownloadedFilePath);
            assertThat(expectedDownloadedFilePath).doesNotExist();
        });
    }

    @Test
    @DisplayName("Verify a successful channel disconnection.")
    public void verifySuccessfulChannelDisconnect() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);

            assertTrue(sftpFileTransferService.connect(Optional.of(PORT), Optional.of(USER), Optional.of(PASSWORD)));
            assertTrue(sftpFileTransferService.isConnectionOpen());
            sftpFileTransferService.disconnectChannel();
            assertFalse(sftpFileTransferService.isConnectionOpen());
        });
    }

    @Test
    @DisplayName("Verify an unsuccessful channel disconnection.")
    public void verifyUnsuccessfulChannelDisconnect() throws Exception {
        SFTPFileTransferService spy = spy(sftpFileTransferService);
        when(spy.disconnectChannelSftp(any())).thenThrow(JSchException.class);
        assertThrows(JSchException.class, () -> spy.disconnectChannelSftp(new ChannelSftp()));
        assertDoesNotThrow(spy::disconnectChannel);
    }

    @Test
    @DisplayName("Verify a successful return of JSch instance")
    public void verifySuccessfulJSchReturn() {
        assertNotNull(sftpFileTransferService.getJsch());
    }

    @Test
    public void verifyThatHostIsRetriedMultipleTimesWhenFailedConnection() throws Exception {
        final int numberOfSftpConnectRetries = eventFileDownloadConfiguration.getNumberOfSftpConnectRetries();

        final List<String> hosts = Arrays.asList(SCRIPTING_VM + 1);
        loadBalancer = new ENMScriptingVMLoadBalancer();
        loadBalancer.setScriptingVMs(hosts);
        sftpFileTransferService = new SFTPFileTransferService(loadBalancer, eventFileDownloadConfiguration);

        //have to clone as spy won't accept parameterized constructor
        SFTPFileTransferService spy =  spy(sftpFileTransferService);

        when(spy.getJsch()).thenReturn(jSch);
        when(jSch.getSession(anyString(), anyString(), anyInt())).thenReturn(session);
        when(session.openChannel(anyString())).thenReturn(channelSftp);
        doThrow(JSchException.class).when(channelSftp).connect();

        assertFalse(spy.connect(Optional.of(PORT), Optional.of(USER), Optional.of(PASSWORD)));
        verify(channelSftp, times(numberOfSftpConnectRetries)).connect();
    }

    @Test
    public void verifyThatAllHostsAreRetriedMultipleTimesWhenFailedConnection() throws Exception {
        final int numberOfSftpConnectRetries = eventFileDownloadConfiguration.getNumberOfSftpConnectRetries();

        final List<String> hosts = Arrays.asList(SCRIPTING_VM + 1, SCRIPTING_VM + 2);
        loadBalancer = new ENMScriptingVMLoadBalancer();
        loadBalancer.setScriptingVMs(hosts);
        sftpFileTransferService = new SFTPFileTransferService(loadBalancer, eventFileDownloadConfiguration);

        //have to clone as spy won't accept parameterized constructor
        SFTPFileTransferService spy =  spy(sftpFileTransferService);

        when(spy.getJsch()).thenReturn(jSch);
        when(jSch.getSession(anyString(), anyString(), anyInt())).thenReturn(session);
        when(session.openChannel( anyString())).thenReturn(channelSftp);
        doThrow(JSchException.class).when(channelSftp).connect();

        assertFalse(spy.connect(Optional.of(PORT), Optional.of(USER), Optional.of(PASSWORD)));
        verify(channelSftp, times(numberOfSftpConnectRetries * hosts.size())).connect();
    }
}
