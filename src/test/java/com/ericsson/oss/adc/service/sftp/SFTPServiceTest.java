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
import com.ericsson.oss.adc.models.connected.systems.SubsystemType;
import com.ericsson.oss.adc.models.connected.systems.SubsystemUsers;
import com.ericsson.oss.adc.service.connected.systems.ConnectedSystemsService;
import com.googlecode.catchexception.apis.BDDCatchException;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import java.net.ConnectException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.github.stefanbirkner.fakesftpserver.lambda.FakeSftpServer.withSftpServer;
import static com.googlecode.catchexception.CatchException.caughtException;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doThrow;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {SFTPFileTransferService.class, SFTPService.class, SFTPServiceTest.class,
                ENMScriptingVMLoadBalancer.class, EventFileDownloadConfiguration.class, ConnectedSystemsService.class})
@AutoConfigureWebClient(registerRestTemplate = true)
@EnableAutoConfiguration
public class SFTPServiceTest {

    private static final String UNIX_SEPARATOR = "/";
    private static final String CONNECTION_EXCEPTION_FILE_DOWNLOAD_RETRIES_EXCEEDED_EXCEPTION = "EventFileDownloadRetriesExceededException";
    private static final String CONNECTION_EXCEPTION_FILE_DOWNLOAD_RETRIES_EXCEEDED_SFTP = "EventFileDownloadRetriesExceededSftpIssue";
    private static final int PORT = 5678;
    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private static final String ENM_FILE_PATH = "/ericsson/pmic1/CELLTRACE/SubNetwork=ONRM_ROOT_MO_R,SubNetwork=5G,MeContext=NE00000650,ManagedElement=NE00000650/";
    private static final String FILE_NAME = "A20200824.1330+0900-1345+0900_SubNetwork=ONRM_ROOT_MO_R,SubNetwork=5G,MeContext=NE00000650,ManagedElement=NE00000650_celltracefile_CUCP0_1_1.gpb.gz";
    private static final String SCRIPTING_VM = "localhost";
    private final String NODE_NAME = "SubNetwork=ONRM_ROOT_MO_R,SubNetwork=5G,MeContext=NE00000650,ManagedElement=NE00000650";
    private final String NODE_TYPE = "RadioNode";
    private final String DATA_TYPE = "PM_CELLTRACE";
    private final SubsystemUsers subsystemUsers1 = new SubsystemUsers(4L,3L);
    private final SubsystemUsers subsystemUsers2 = new SubsystemUsers(15L,3L);
    private final SubsystemUsers subsystemUsers3 = new SubsystemUsers(2L,3L);
    private final SubsystemType subsystemType  = new SubsystemType(null, "PhysicalDevice");
    private final String enm = "enm";
    private final ConnectionProperties connectionProperties = ConnectionProperties.builder()
            .id(5L)
            .subsystemId(2L)
            .name("localhost")
            .tenant("tenant1")
            .username("user")
            .password("password")
            .scriptingVMs("localhost,localhost,localhost")
            .sftpPort(5678)
            .encryptedKeys(List.of("encryptedKey"))
            .subsystemUsers(Arrays.asList(subsystemUsers1,subsystemUsers2, subsystemUsers3))
            .build();

    private final ConnectionProperties connectionPropertiesWrongPassword = ConnectionProperties.builder()
            .id(5L)
            .subsystemId(2L)
            .name("localhost")
            .tenant("tenant1")
            .username("user")
            .password("wrongPassword")
            .scriptingVMs("localhost,localhost,localhost")
            .sftpPort(5678)
            .encryptedKeys(List.of("encryptedKey"))
            .subsystemUsers(Arrays.asList(subsystemUsers1,subsystemUsers2, subsystemUsers3))
            .build();
    private final Subsystem subsystem = new Subsystem(
            2L,
            5L,
            "enm1",
            "https://test.subsystem-2/",
            null,
            new ArrayList<>(Collections.singletonList(connectionPropertiesWrongPassword)),
            "EricssonTEST",
            subsystemType,
            "eric-eo-ecm-adapter");
    private final Map<String, Subsystem> subsystemMap = new HashMap<>(Map.of(enm, subsystem));
    private final Map subSystemMapEmpty = Collections.EMPTY_MAP;

    @Value("${temp-directory}")
    private String tempDirectory;

    @MockBean
    private ConnectedSystemsService connectedSystemsService;

    @Autowired
    private EventFileDownloadConfiguration eventFileDownloadConfiguration;

    @SpyBean
    private SFTPService sftpService;

    @SpyBean
    SFTPFileTransferService sftpFileTransferServiceMock;

    @Test
    @Order(1)
    @DisplayName("Verify unsuccessful SFTP connection")
    public void test_setupSFTPConnectionVerifyUnsuccessfulServerConnection() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD + 1);
            ENMScriptingVMLoadBalancer loadBalancer = new ENMScriptingVMLoadBalancer();
            loadBalancer.setScriptingVMs(Collections.singletonList(SCRIPTING_VM));
            SFTPFileTransferService sftpFileTransferService = new SFTPFileTransferService(loadBalancer, eventFileDownloadConfiguration);

            assertFalse(sftpService.connect(sftpFileTransferService, PORT, USER, PASSWORD));
            assertFalse(sftpService.isConnectionOpen(sftpFileTransferService));
        });
    }

    @Test
    @Order(2)
    @DisplayName("Verify successful SFTP connection")
    public void test_setupSFTPConnectionVerifySuccessfulServerConnection() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            final ENMScriptingVMLoadBalancer loadBalancer = new ENMScriptingVMLoadBalancer();
            loadBalancer.setScriptingVMs(Collections.singletonList(SCRIPTING_VM));
            final SFTPFileTransferService sftpFileTransferService = new SFTPFileTransferService(loadBalancer, eventFileDownloadConfiguration);

            assertTrue(sftpService.connect(sftpFileTransferService, PORT, USER, PASSWORD));
            assertTrue(sftpService.isConnectionOpen(sftpFileTransferService));
        });
    }

    @Test
    @Order(3)
    @DisplayName("Verify unsuccessful SFTP connection based on empty connected systems response")
    public void test_setupSFTPConnectionAndEmptyConnectedSystemsResponseVerifyNoConnectionToSftpServer() throws Exception {
        withSftpServer(server -> {
            when(connectedSystemsService.getConnectionPropertiesBySubsystemsName(any())).thenReturn(null);

            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            ENMScriptingVMLoadBalancer loadBalancer = new ENMScriptingVMLoadBalancer();
            SFTPFileTransferService sftpFileTransferService = new SFTPFileTransferService(loadBalancer, eventFileDownloadConfiguration);

            assertFalse(sftpService.setUpConnectionToSFTPServerUsingConnectedSystemsResponse(sftpFileTransferService, loadBalancer));
            assertFalse(sftpService.isConnectionOpen(sftpFileTransferService));
        });
    }

    @Test
    @Order(4)
    @DisplayName("Should throw ConnectException when empty connected systems response is returned")
    public void test_setUpSFTPConnectionAndDownloadFile_connectException() throws Exception {
        withSftpServer(server -> {
            when(connectedSystemsService.getSubsystemDetails()).thenReturn(subSystemMapEmpty);

            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);

            final InputMessage testInputMessage = new InputMessage();
            testInputMessage.setNodeName(NODE_NAME);
            testInputMessage.setNodeType(NODE_TYPE);
            final String remoteFilePath = ENM_FILE_PATH + FILE_NAME;
            testInputMessage.setFileLocation(remoteFilePath);
            testInputMessage.setDataType(DATA_TYPE);

            final List<InputMessage> inputMessagesToAttemptDownloading = Collections.unmodifiableList(
                    Arrays.asList(testInputMessage));
            server.putFile(remoteFilePath, "Content of file", UTF_8);

            assertThrows(ConnectException.class, () -> sftpService.setUpSFTPConnectionAndDownloadFiles(inputMessagesToAttemptDownloading));
        });
    }

    @Test
    @Order(5)
    @DisplayName("Should fetch SubsystemDetails only once, establish SFTP connection and successfully download a single batch, single file")
    public void test_getSubsystemDetailsSetUpSingleSFTPConnectionAndDownloadFiles() throws Exception {
        withSftpServer(server -> {
            when(connectedSystemsService.getSubsystemDetails()).thenReturn(subsystemMap);
            when(connectedSystemsService.getConnectionPropertiesBySubsystemsName(subsystemMap)).thenReturn(connectionProperties);

            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);

            final InputMessage testInputMessage = new InputMessage();
            testInputMessage.setNodeName(NODE_NAME);
            testInputMessage.setNodeType(NODE_TYPE);
            final String remoteFilePath = ENM_FILE_PATH + FILE_NAME;
            testInputMessage.setFileLocation(remoteFilePath);
            testInputMessage.setDataType(DATA_TYPE);

            final List<InputMessage> inputMessagesToAttemptDownloading = Collections.unmodifiableList(
                    Arrays.asList(testInputMessage));
            server.putFile(remoteFilePath, "Content of file", UTF_8);

            assertEquals(1, inputMessagesToAttemptDownloading.size());
            // Called twice to assert that 1 REST call is made at first setUpConnectionToSFTPServerUsingConnectedSystemsResponse attempt where
            // getSubsystemDetails is called once and then stored. Prevents unneeded REST calls for each setUpSFTPConnectionAndDownloadFile call
            assertFalse(sftpService.setUpSFTPConnectionAndDownloadFiles(inputMessagesToAttemptDownloading).stream().map(InputMessage::getDownloadedFile).anyMatch(Objects::isNull));
            assertFalse(sftpService.setUpSFTPConnectionAndDownloadFiles(inputMessagesToAttemptDownloading).stream().map(InputMessage::getDownloadedFile).anyMatch(Objects::isNull));

            verify(connectedSystemsService, times(1)).getSubsystemDetails();
            verify(sftpService, times(2)).setUpSFTPConnectionAndDownloadFiles(inputMessagesToAttemptDownloading);

            // cleanup
            Path expectedDownloadedFilePath = new File(tempDirectory + "/" + FILE_NAME).toPath();
            Files.delete(expectedDownloadedFilePath);
            assertThat(expectedDownloadedFilePath).doesNotExist();
        });
    }

    @Test
    @Order(6)
    @DisplayName("Verify unsuccessful SFTP connection based on connected systems response due to invalid password then reload with correct password")
    public void test_SetupSFTPConnectionAndConnectedSystemsResponseFailedSFTPServerAuthenticationInvalidPassword() throws Exception {
        withSftpServer(server -> {
            when(connectedSystemsService.getSubsystemDetails()).thenReturn(subsystemMap);
            when(connectedSystemsService.getConnectionPropertiesBySubsystemsName(any()))
                    .thenReturn(connectionPropertiesWrongPassword)
                    .thenReturn(connectionProperties);

            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            ENMScriptingVMLoadBalancer loadBalancer = new ENMScriptingVMLoadBalancer();
            SFTPFileTransferService sftpFileTransferService = new SFTPFileTransferService(loadBalancer, eventFileDownloadConfiguration);

            assertFalse(sftpService.setUpConnectionToSFTPServerUsingConnectedSystemsResponse(sftpFileTransferService, loadBalancer));
            assertFalse(sftpService.isConnectionOpen(sftpFileTransferService));

            //connection details now reloaded after initial retries
            assertTrue(sftpService.setUpConnectionToSFTPServerUsingConnectedSystemsResponse(sftpFileTransferService, loadBalancer));
        });
    }

    @Test
    @Order(7)
    @DisplayName("Verify successful single batch, single file download verifying server connection")
    public void test_SetupSFTPConnectionVerifyServerConnectionAndSingleFileDownloadedSuccessfully() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            ENMScriptingVMLoadBalancer loadBalancer = new ENMScriptingVMLoadBalancer();
            loadBalancer.setScriptingVMs(Collections.singletonList(SCRIPTING_VM));
            SFTPFileTransferService sftpFileTransferService = new SFTPFileTransferService(loadBalancer, eventFileDownloadConfiguration);

            final InputMessage testInputMessage = new InputMessage();
            testInputMessage.setNodeName(NODE_NAME);
            testInputMessage.setNodeType(NODE_TYPE);
            final String remoteFilePath = ENM_FILE_PATH + FILE_NAME;
            testInputMessage.setFileLocation(remoteFilePath);
            testInputMessage.setDataType(DATA_TYPE);

            final List<InputMessage> inputMessagesToAttemptDownloading = Collections.singletonList((testInputMessage));
            server.putFile(remoteFilePath, "Content of file", UTF_8);

            assertFalse(sftpService.isConnectionOpen(sftpFileTransferService));
            assertTrue(sftpService.connect(sftpFileTransferService, PORT, USER, PASSWORD));
            assertTrue(sftpService.isConnectionOpen(sftpFileTransferService));

            final List<InputMessage> downloadedInputMessages = sftpService.download(sftpFileTransferService, inputMessagesToAttemptDownloading);

            assertEquals(1, downloadedInputMessages.stream().map(InputMessage::getDownloadedFile).filter(Objects::nonNull).count());
            assertEquals(0, inputMessagesToAttemptDownloading.size() - downloadedInputMessages.stream().map(InputMessage::getDownloadedFile).filter(Objects::nonNull).count());

            verify(sftpService, times(1)).connect(sftpFileTransferService, PORT, USER, PASSWORD);

            // cleanup
            Path expectedDownloadedFilePath = new File(tempDirectory + "/" + FILE_NAME).toPath();
            Files.delete(expectedDownloadedFilePath);
            assertThat(expectedDownloadedFilePath).doesNotExist();
        });
    }

    @Test
    @Order(8)
    @DisplayName("Verify single batch, single file download with downloaded file attached")
    public void test_SetupSingleSFTPConnectionAndDownloadOneFile() throws Exception {
        withSftpServer(server -> {
            when(connectedSystemsService.getSubsystemDetails()).thenReturn(subsystemMap);
            when(connectedSystemsService.getConnectionPropertiesBySubsystemsName(any())).thenReturn(connectionProperties);

            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);

            final InputMessage testInputMessage = new InputMessage();
            testInputMessage.setNodeName(NODE_NAME);
            testInputMessage.setNodeType(NODE_TYPE);
            final String remoteFilePath = ENM_FILE_PATH + FILE_NAME;
            testInputMessage.setFileLocation(remoteFilePath);
            testInputMessage.setDataType(DATA_TYPE);

            final List<InputMessage> inputMessagesToAttemptDownloading = Collections.singletonList((testInputMessage));
            server.putFile(remoteFilePath, "Content of file", UTF_8);
            final List<InputMessage> downloadedInputMessages = sftpService.setUpSFTPConnectionAndDownloadFiles(inputMessagesToAttemptDownloading);

            assertEquals(1, downloadedInputMessages.stream().map(InputMessage::getDownloadedFile).filter(Objects::nonNull).count());
            assertEquals(0, inputMessagesToAttemptDownloading.size() - downloadedInputMessages.stream().map(InputMessage::getDownloadedFile).filter(Objects::nonNull).count());

            verify(sftpService, times(1)).setUpSFTPConnectionAndDownloadFiles(inputMessagesToAttemptDownloading);

            // cleanup
            Path expectedDownloadedFilePath = new File(tempDirectory + "/" + FILE_NAME).toPath();
            Files.delete(expectedDownloadedFilePath);
            assertThat(expectedDownloadedFilePath).doesNotExist();
        });
    }

    @Test
    @Order(9)
    @DisplayName("Should establish single successful SFTP connection and fail single batch, single file download")
    public void test_setUpSFTPConnectionAndDownloadFile_downloadFails() throws Exception {
        withSftpServer(server -> {
            when(connectedSystemsService.getSubsystemDetails()).thenReturn(subsystemMap);
            when(connectedSystemsService.getConnectionPropertiesBySubsystemsName(any())).thenReturn(connectionProperties);

            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);

            final InputMessage testInputMessage = new InputMessage();
            testInputMessage.setNodeName(NODE_NAME);
            testInputMessage.setNodeType(NODE_TYPE);
            final String remoteFilePath = ENM_FILE_PATH + FILE_NAME;
            testInputMessage.setFileLocation(remoteFilePath + 1);
            testInputMessage.setDataType(DATA_TYPE);

            final List<InputMessage> inputMessagesToAttemptDownloading = Collections.singletonList((testInputMessage));
            server.putFile(remoteFilePath, "Content of file", UTF_8);
            final List<InputMessage> downloadedInputMessages = sftpService.setUpSFTPConnectionAndDownloadFiles(inputMessagesToAttemptDownloading);

            assertEquals(0, downloadedInputMessages.stream().map(InputMessage::getDownloadedFile).filter(Objects::nonNull).count());
            assertEquals(1, inputMessagesToAttemptDownloading.size() - downloadedInputMessages.stream().map(InputMessage::getDownloadedFile).filter(Objects::nonNull).count());

            verify(sftpService, times(1)).setUpSFTPConnectionAndDownloadFiles(inputMessagesToAttemptDownloading);
        });
    }

    @Test
    @Order(10)
    @DisplayName("Verify unsuccessful single batch, single file download with null attached when not found on server")
    public void test_setupSFTPConnectionVerifyServerConnectionAndSingleFileDownloadedFail() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            ENMScriptingVMLoadBalancer loadBalancer = new ENMScriptingVMLoadBalancer();
            loadBalancer.setScriptingVMs(Collections.singletonList(SCRIPTING_VM));
            SFTPFileTransferService sftpFileTransferService = new SFTPFileTransferService(loadBalancer, eventFileDownloadConfiguration);

            final InputMessage testInputMessage = new InputMessage();
            testInputMessage.setNodeName(NODE_NAME);
            testInputMessage.setNodeType(NODE_TYPE);
            final String remoteFilePath = ENM_FILE_PATH + FILE_NAME;
            testInputMessage.setFileLocation(remoteFilePath + 1);
            testInputMessage.setDataType(DATA_TYPE);

            final List<InputMessage> inputMessagesToAttemptDownloading = Collections.singletonList((testInputMessage));

            server.putFile(remoteFilePath, "Content of file", UTF_8);

            assertTrue(sftpService.connect(sftpFileTransferService, PORT, USER, PASSWORD));
            assertTrue(sftpService.isConnectionOpen(sftpFileTransferService));
            final List<InputMessage> downloadedInputMessages = sftpService.download(sftpFileTransferService,inputMessagesToAttemptDownloading);

            assertEquals(0, downloadedInputMessages.stream().map(InputMessage::getDownloadedFile).filter(Objects::nonNull).count());
            assertEquals(1, inputMessagesToAttemptDownloading.size() - downloadedInputMessages.stream().map(InputMessage::getDownloadedFile).filter(Objects::nonNull).count());

            verify(sftpService, times(1)).connect(sftpFileTransferService, PORT, USER, PASSWORD);

        });
    }

    @Test
    @Order(11)
    @DisplayName("Verify successful SFTP connection based on connected systems response")
    public void test_setupSFTPConnectionAndConnectedSystemsResponseVerifyConnectionToSftpServer() throws Exception {
        withSftpServer(server -> {
            when(connectedSystemsService.getSubsystemDetails()).thenReturn(subsystemMap);
            when(connectedSystemsService.getConnectionPropertiesBySubsystemsName(any())).thenReturn(connectionProperties);

            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            ENMScriptingVMLoadBalancer loadBalancer = new ENMScriptingVMLoadBalancer();
            SFTPFileTransferService sftpFileTransferService = new SFTPFileTransferService(loadBalancer, eventFileDownloadConfiguration);

            assertTrue(sftpService.setUpConnectionToSFTPServerUsingConnectedSystemsResponse(sftpFileTransferService, loadBalancer));
            assertTrue(sftpService.isConnectionOpen(sftpFileTransferService));
        });
    }

    @Test
    @Order(12)
    @DisplayName("Verify single batch, four unsuccessful file downloads with nulls attached when not found on server")
    public void test_SetupSFTPConnectionVerifyServerConnectionAndFourFileDownloadedFail() throws Exception {
        withSftpServer(server -> {
            when(connectedSystemsService.getSubsystemDetails()).thenReturn(subsystemMap);
            when(connectedSystemsService.getConnectionPropertiesBySubsystemsName(any())).thenReturn(connectionProperties);

            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            ENMScriptingVMLoadBalancer loadBalancer = new ENMScriptingVMLoadBalancer();
            loadBalancer.setScriptingVMs(Collections.singletonList(SCRIPTING_VM));
            SFTPFileTransferService sftpFileTransferService = new SFTPFileTransferService(loadBalancer, eventFileDownloadConfiguration);

            final InputMessage testInputMessage = new InputMessage();
            testInputMessage.setNodeName(NODE_NAME);
            testInputMessage.setNodeType(NODE_TYPE);
            final String remoteFilePath = ENM_FILE_PATH + FILE_NAME;
            testInputMessage.setFileLocation(remoteFilePath + 1);
            testInputMessage.setDataType(DATA_TYPE);

            final List<InputMessage> testInputMessageCopies = Collections.nCopies(4,testInputMessage);
            server.putFile(remoteFilePath, "Content of file", UTF_8);

            assertTrue(sftpService.connect(sftpFileTransferService, PORT, USER, PASSWORD));
            assertTrue(sftpService.isConnectionOpen(sftpFileTransferService));
            final List<InputMessage> downloadedInputMessages = sftpService.download(sftpFileTransferService, testInputMessageCopies);

            assertEquals(0, downloadedInputMessages.stream().map(InputMessage::getDownloadedFile).filter(Objects::nonNull).count());
            assertEquals(4, testInputMessageCopies.size() - downloadedInputMessages.stream().map(InputMessage::getDownloadedFile).filter(Objects::nonNull).count());

            verify(sftpService, times(1)).connect(sftpFileTransferService, PORT, USER, PASSWORD);
        });
    }

    @Test
    @Order(13)
    @DisplayName("Verify single batch, successful second and fourth file download, unsuccessful first and third file download when not found on the server")
    public void test_setupSFTPConnectionVerifyServerConnectionAndSecondFourthFileDownloadedSuccessfullyFirstThirdUnSuccessfully_SingleBatch() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            ENMScriptingVMLoadBalancer loadBalancer = new ENMScriptingVMLoadBalancer();
            loadBalancer.setScriptingVMs(Collections.singletonList(SCRIPTING_VM));
            SFTPFileTransferService sftpFileTransferService = new SFTPFileTransferService(loadBalancer, eventFileDownloadConfiguration);

            final String remoteFilePath = ENM_FILE_PATH + FILE_NAME;

            final InputMessage testInputMessage1 = new InputMessage();
            testInputMessage1.setNodeName(NODE_NAME);
            testInputMessage1.setNodeType(NODE_TYPE);
            testInputMessage1.setFileLocation(remoteFilePath + 1);
            testInputMessage1.setDataType(DATA_TYPE);

            final InputMessage testInputMessage2 = new InputMessage();
            testInputMessage2.setNodeName(NODE_NAME);
            testInputMessage2.setNodeType(NODE_TYPE);
            testInputMessage2.setFileLocation(remoteFilePath);
            testInputMessage2.setDataType(DATA_TYPE);

            final List<InputMessage> inputMessagesToAttemptDownloading = Collections.unmodifiableList(
                    Arrays.asList(testInputMessage2, testInputMessage1, testInputMessage1, testInputMessage2));

            server.putFile(remoteFilePath, "Content of file", UTF_8);

            assertFalse(sftpService.isConnectionOpen(sftpFileTransferService));
            assertTrue(sftpService.connect(sftpFileTransferService, PORT, USER, PASSWORD));
            assertTrue(sftpService.isConnectionOpen(sftpFileTransferService));

            final List<InputMessage> downloadedInputMessages = sftpService.download(sftpFileTransferService, inputMessagesToAttemptDownloading);

            assertEquals(2, downloadedInputMessages.stream().map(InputMessage::getDownloadedFile).filter(Objects::nonNull).count());
            assertEquals(2, inputMessagesToAttemptDownloading.size() - downloadedInputMessages.stream().map(InputMessage::getDownloadedFile).filter(Objects::nonNull).count());

            verify(sftpService, times(1)).connect(sftpFileTransferService, PORT, USER, PASSWORD);

            // cleanup
            Path expectedDownloadedFilePath = new File(tempDirectory + "/" + FILE_NAME).toPath();
            Files.delete(expectedDownloadedFilePath);
            assertThat(expectedDownloadedFilePath).doesNotExist();
        });
    }

    @Test
    @Order(14)
    @DisplayName("Verify double batch, successful second and fourth file download, unsuccessful first and third file download when not found on the server")
    public void test_setupSingleSFTPConnectionVerifyServerConnectionAndSecondFourthFileDownloadedSuccessfullyFirstThirdUnSuccessfully_DoubleBatch() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            ENMScriptingVMLoadBalancer loadBalancer = new ENMScriptingVMLoadBalancer();
            loadBalancer.setScriptingVMs(Collections.singletonList(SCRIPTING_VM));
            SFTPFileTransferService sftpFileTransferService = new SFTPFileTransferService(loadBalancer, eventFileDownloadConfiguration);

            final String remoteFilePath = ENM_FILE_PATH + FILE_NAME;

            final InputMessage testInputMessage1 = new InputMessage();
            testInputMessage1.setNodeName(NODE_NAME);
            testInputMessage1.setNodeType(NODE_TYPE);
            testInputMessage1.setFileLocation(remoteFilePath + 1);
            testInputMessage1.setDataType(DATA_TYPE);

            final InputMessage testInputMessage2 = new InputMessage();
            testInputMessage2.setNodeName(NODE_NAME);
            testInputMessage2.setNodeType(NODE_TYPE);
            testInputMessage2.setFileLocation(remoteFilePath);
            testInputMessage2.setDataType(DATA_TYPE);

            final List<InputMessage> inputMessagesToAttemptDownloading1 = Collections.unmodifiableList(
                    Arrays.asList(testInputMessage2, testInputMessage1, testInputMessage1, testInputMessage2));
            final List<InputMessage> inputMessagesToAttemptDownloading2 = Collections.unmodifiableList(
                    Arrays.asList(testInputMessage2, testInputMessage1, testInputMessage1, testInputMessage2));

            server.putFile(remoteFilePath, "Content of file", UTF_8);

            assertFalse(sftpService.isConnectionOpen(sftpFileTransferService));
            assertTrue(sftpService.connect(sftpFileTransferService, PORT, USER, PASSWORD));
            assertTrue(sftpService.isConnectionOpen(sftpFileTransferService));

            final List<InputMessage> downloadedInputMessages1 = sftpService.download(sftpFileTransferService, inputMessagesToAttemptDownloading1);

            assertEquals(2, downloadedInputMessages1.stream().map(InputMessage::getDownloadedFile).filter(Objects::nonNull).count());
            assertEquals(2, inputMessagesToAttemptDownloading1.size() - downloadedInputMessages1.stream().map(InputMessage::getDownloadedFile).filter(Objects::nonNull).count());

            final List<InputMessage> downloadedInputMessages2 = sftpService.download(sftpFileTransferService, inputMessagesToAttemptDownloading1);

            assertEquals(2, downloadedInputMessages2.stream().map(InputMessage::getDownloadedFile).filter(Objects::nonNull).count());
            assertEquals(2, inputMessagesToAttemptDownloading2.size() - downloadedInputMessages2.stream().map(InputMessage::getDownloadedFile).filter(Objects::nonNull).count());

            verify(sftpService, times(1)).connect(sftpFileTransferService, PORT, USER, PASSWORD);
            verify(sftpService, times(0)).setUpSFTPConnectionAndDownloadFiles(any());

            // cleanup
            Path expectedDownloadedFilePath = new File(tempDirectory + "/" + FILE_NAME).toPath();
            Files.delete(expectedDownloadedFilePath);
            assertThat(expectedDownloadedFilePath).doesNotExist();
        });
    }

    @Test
    @Order(15)
    @DisplayName("Verify single batch, four files download with downloaded files attached")
    public void test_SetUpSingleSFTPConnectionAndDownloadFourFiles_SingleBatch() throws Exception {
        withSftpServer(server -> {
            when(connectedSystemsService.getSubsystemDetails()).thenReturn(subsystemMap);
            when(connectedSystemsService.getConnectionPropertiesBySubsystemsName(any())).thenReturn(connectionProperties);

            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);

            final InputMessage testInputMessage = new InputMessage();
            testInputMessage.setNodeName(NODE_NAME);
            testInputMessage.setNodeType(NODE_TYPE);
            final String remoteFilePath = ENM_FILE_PATH + FILE_NAME;
            testInputMessage.setFileLocation(remoteFilePath);
            testInputMessage.setDataType(DATA_TYPE);

            final List<InputMessage> inputMessagesToAttemptDownloadingCopies = Collections.nCopies(4,testInputMessage);

            server.putFile(remoteFilePath, "Content of file", UTF_8);
            final List<InputMessage> downloadedInputMessages = sftpService.setUpSFTPConnectionAndDownloadFiles(inputMessagesToAttemptDownloadingCopies);

            assertEquals(4, downloadedInputMessages.stream().map(InputMessage::getDownloadedFile).filter(Objects::nonNull).count());
            assertEquals(0, inputMessagesToAttemptDownloadingCopies.size() - downloadedInputMessages.stream().map(InputMessage::getDownloadedFile).filter(Objects::nonNull).count());

            verify(sftpService, times(1)).setUpSFTPConnectionAndDownloadFiles(inputMessagesToAttemptDownloadingCopies);

            // cleanup
            Path expectedDownloadedFilePath = new File(tempDirectory + "/" + FILE_NAME).toPath();
            Files.delete(expectedDownloadedFilePath);
            assertThat(expectedDownloadedFilePath).doesNotExist();
        });
    }

    @Test
    @Order(16)
    @DisplayName("Verify double batch, four files download with downloaded files attached")
    public void test_SetUpSingleSFTPConnectionAndDownloadFourFiles_DoubleBatch() throws Exception {
        withSftpServer(server -> {
            when(connectedSystemsService.getSubsystemDetails()).thenReturn(subsystemMap);
            when(connectedSystemsService.getConnectionPropertiesBySubsystemsName(any())).thenReturn(connectionProperties);

            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);

            final InputMessage testInputMessage = new InputMessage();
            testInputMessage.setNodeName(NODE_NAME);
            testInputMessage.setNodeType(NODE_TYPE);
            final String remoteFilePath = ENM_FILE_PATH + FILE_NAME;
            testInputMessage.setFileLocation(remoteFilePath);
            testInputMessage.setDataType(DATA_TYPE);

            final List<InputMessage> inputMessagesToAttemptDownloadingCopies1 = Collections.nCopies(4,testInputMessage);
            final List<InputMessage> inputMessagesToAttemptDownloadingCopies2 = Collections.nCopies(4,testInputMessage);

            server.putFile(remoteFilePath, "Content of file", UTF_8);
            final List<InputMessage> downloadedInputMessages1 = sftpService.setUpSFTPConnectionAndDownloadFiles(inputMessagesToAttemptDownloadingCopies1);

            assertEquals(4, downloadedInputMessages1.stream().map(InputMessage::getDownloadedFile).filter(Objects::nonNull).count());
            assertEquals(0, inputMessagesToAttemptDownloadingCopies1.size() - downloadedInputMessages1.stream().map(InputMessage::getDownloadedFile).filter(Objects::nonNull).count());

            final List<InputMessage> downloadedInputMessages2 = sftpService.setUpSFTPConnectionAndDownloadFiles(inputMessagesToAttemptDownloadingCopies2);

            assertEquals(4, downloadedInputMessages2.stream().map(InputMessage::getDownloadedFile).filter(Objects::nonNull).count());
            assertEquals(0, inputMessagesToAttemptDownloadingCopies2.size() - downloadedInputMessages2.stream().map(InputMessage::getDownloadedFile).filter(Objects::nonNull).count());

            verify(sftpService, times(2)).setUpSFTPConnectionAndDownloadFiles(any());

            // cleanup
            Path expectedDownloadedFilePath = new File(tempDirectory + "/" + FILE_NAME).toPath();
            Files.delete(expectedDownloadedFilePath);
            assertThat(expectedDownloadedFilePath).doesNotExist();
        });
    }

    @Test
    @Order(17)
    @DisplayName("Verify unsuccessful SFTP connection based on connected systems response due to invalid user")
    public void test_setupSFTPConnectionAndConnectedSystemsResponseFailedSFTPServerAuthenticationInvalidUser() throws Exception {
        withSftpServer(server -> {
            when(connectedSystemsService.getSubsystemDetails()).thenReturn(subsystemMap);
            when(connectedSystemsService.getConnectionPropertiesBySubsystemsName(any())).thenReturn(connectionProperties);

            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER + 1, PASSWORD);
            ENMScriptingVMLoadBalancer loadBalancer = new ENMScriptingVMLoadBalancer();
            SFTPFileTransferService sftpFileTransferService = new SFTPFileTransferService(loadBalancer, eventFileDownloadConfiguration);

            assertFalse(sftpService.setUpConnectionToSFTPServerUsingConnectedSystemsResponse(sftpFileTransferService, loadBalancer));
            assertFalse(sftpService.isConnectionOpen(sftpFileTransferService));
        });
    }

    @Test
    @Order(18)
    @DisplayName("Verify SFTP based connect exception during batch download deletes all downloaded files, restarts the transaction.")
    public void test_SetupSFTPConnectionVerifyServerConnectionBatchDownloadAndDeleteDownloadedFilesSftpException() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            final ENMScriptingVMLoadBalancer loadBalancer = new ENMScriptingVMLoadBalancer();
            loadBalancer.setScriptingVMs(Collections.singletonList(SCRIPTING_VM));
            final SFTPFileTransferService sftpFileTransferServiceSpy = spy(new SFTPFileTransferService(loadBalancer, eventFileDownloadConfiguration));

            assertTrue(sftpService.connect(sftpFileTransferServiceSpy, PORT, USER, PASSWORD));
            assertTrue(sftpService.isConnectionOpen(sftpFileTransferServiceSpy));

            final String remoteFilePathBad = ENM_FILE_PATH + FILE_NAME;
            final String remoteFilePathGood1 = ENM_FILE_PATH + FILE_NAME + 1;
            final String remoteFilePathGood2 = ENM_FILE_PATH + FILE_NAME + 2;
            final String localFilePath = tempDirectory + UNIX_SEPARATOR + new File(remoteFilePathBad).getName();

            final InputMessage testInputMessageGood1 = new InputMessage();
            testInputMessageGood1.setNodeName(NODE_NAME);
            testInputMessageGood1.setNodeType(NODE_TYPE);
            testInputMessageGood1.setFileLocation(remoteFilePathGood1);
            testInputMessageGood1.setDataType(DATA_TYPE);

            final InputMessage testInputMessageGood2 = new InputMessage();
            testInputMessageGood2.setNodeName(NODE_NAME);
            testInputMessageGood2.setNodeType(NODE_TYPE);
            testInputMessageGood2.setFileLocation(remoteFilePathGood2);
            testInputMessageGood2.setDataType(DATA_TYPE);

            final InputMessage testInputMessageBad = new InputMessage();
            testInputMessageBad.setNodeName(NODE_NAME);
            testInputMessageBad.setNodeType(NODE_TYPE);
            testInputMessageBad.setFileLocation("");
            testInputMessageBad.setDataType(DATA_TYPE);

            final List<InputMessage> inputMessagesToAttemptDownloading = Collections.unmodifiableList(
                    Arrays.asList(testInputMessageGood1,testInputMessageGood2,testInputMessageBad));
            server.putFile(remoteFilePathGood1, "Content of file", UTF_8);
            server.putFile(remoteFilePathGood2, "Content of file", UTF_8);

            doThrow(ConnectException.class).when(sftpFileTransferServiceSpy).downloadFile("",localFilePath);
            //Cleanup completed in logic flow
            BDDCatchException.when(() -> sftpService.download(sftpFileTransferServiceSpy, inputMessagesToAttemptDownloading));
            verify(sftpService, times(2)).deleteFile(any());
            AssertionsForClassTypes.assertThat(caughtException()).isInstanceOf(ConnectException.class);
            assertEquals(CONNECTION_EXCEPTION_FILE_DOWNLOAD_RETRIES_EXCEEDED_SFTP, caughtException().getMessage());
        });
    }

    @Test
    @Order(19)
    @DisplayName("Verify SFTP based connect exception during single file download does not download file.")
    public void test_SetupSFTPConnectionVerifyServerConnectionSingleFileAndDoesNotDeleteSftpException() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            final ENMScriptingVMLoadBalancer loadBalancer = new ENMScriptingVMLoadBalancer();
            loadBalancer.setScriptingVMs(Collections.singletonList(SCRIPTING_VM));
            final SFTPFileTransferService sftpFileTransferServiceSpy = spy(new SFTPFileTransferService(loadBalancer, eventFileDownloadConfiguration));

            assertTrue(sftpService.connect(sftpFileTransferServiceSpy, PORT, USER, PASSWORD));
            assertTrue(sftpService.isConnectionOpen(sftpFileTransferServiceSpy));

            final String remoteFilePathBad = ENM_FILE_PATH + FILE_NAME;
            final String remoteFilePathGood1 = ENM_FILE_PATH + FILE_NAME + 1;
            final String localFilePath = tempDirectory + UNIX_SEPARATOR + new File(remoteFilePathBad).getName();

            final InputMessage testInputMessageGood1 = new InputMessage();
            testInputMessageGood1.setNodeName(NODE_NAME);
            testInputMessageGood1.setNodeType(NODE_TYPE);
            testInputMessageGood1.setFileLocation("");
            testInputMessageGood1.setDataType(DATA_TYPE);

            final List<InputMessage> inputMessagesToAttemptDownloading = Collections.singletonList((testInputMessageGood1));
            server.putFile(remoteFilePathGood1, "Content of file", UTF_8);

            doThrow(ConnectException.class).when(sftpFileTransferServiceSpy).downloadFile("",localFilePath);
            BDDCatchException.when(() -> sftpService.download(sftpFileTransferServiceSpy, inputMessagesToAttemptDownloading));
            verify(sftpService, times(0)).deleteFile(any());
            AssertionsForClassTypes.assertThat(caughtException()).isInstanceOf(ConnectException.class);
            assertEquals(CONNECTION_EXCEPTION_FILE_DOWNLOAD_RETRIES_EXCEEDED_SFTP, caughtException().getMessage());
        });
    }

    @Test
    @Order(20)
    @DisplayName("Verify file based connect exception during batch download deletes all downloaded files.")
    public void test_SetupSFTPConnectionVerifyServerConnectionBatchDownloadAndDeleteDownloadedFilesFileException() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            final ENMScriptingVMLoadBalancer loadBalancer = new ENMScriptingVMLoadBalancer();
            loadBalancer.setScriptingVMs(Collections.singletonList(SCRIPTING_VM));
            final SFTPFileTransferService sftpFileTransferServiceSpy = spy(new SFTPFileTransferService(loadBalancer, eventFileDownloadConfiguration));

            assertTrue(sftpService.connect(sftpFileTransferServiceSpy, PORT, USER, PASSWORD));
            assertTrue(sftpService.isConnectionOpen(sftpFileTransferServiceSpy));

            final String remoteFilePathBad = ENM_FILE_PATH + FILE_NAME;
            final String remoteFilePathGood1 = ENM_FILE_PATH + FILE_NAME + 1;
            final String remoteFilePathGood2 = ENM_FILE_PATH + FILE_NAME + 2;
            final String localFilePath = tempDirectory + UNIX_SEPARATOR + new File(remoteFilePathBad).getName();

            final InputMessage testInputMessageGood1 = new InputMessage();
            testInputMessageGood1.setNodeName(NODE_NAME);
            testInputMessageGood1.setNodeType(NODE_TYPE);
            testInputMessageGood1.setFileLocation(remoteFilePathGood1);
            testInputMessageGood1.setDataType(DATA_TYPE);

            final InputMessage testInputMessageGood2 = new InputMessage();
            testInputMessageGood2.setNodeName(NODE_NAME);
            testInputMessageGood2.setNodeType(NODE_TYPE);
            testInputMessageGood2.setFileLocation(remoteFilePathGood2);
            testInputMessageGood2.setDataType(DATA_TYPE);

            final InputMessage testInputMessageBad = new InputMessage();
            testInputMessageBad.setNodeName(NODE_NAME);
            testInputMessageBad.setNodeType(NODE_TYPE);
            testInputMessageBad.setFileLocation(remoteFilePathBad);
            testInputMessageBad.setDataType(DATA_TYPE);

            final List<InputMessage> inputMessagesToAttemptDownloading = Collections.unmodifiableList(
                    Arrays.asList(testInputMessageGood1,testInputMessageGood2,testInputMessageBad));
            server.putFile(remoteFilePathGood1, "Content of file", UTF_8);
            server.putFile(remoteFilePathGood2, "Content of file", UTF_8);
            server.putFile(remoteFilePathBad, "Content of file", UTF_8);

            doThrow(new ConnectException(CONNECTION_EXCEPTION_FILE_DOWNLOAD_RETRIES_EXCEEDED_EXCEPTION)).when(sftpFileTransferServiceSpy).downloadFile(remoteFilePathBad, localFilePath);

            //Cleanup completed in logic flow
            BDDCatchException.when(() -> sftpService.download(sftpFileTransferServiceSpy, inputMessagesToAttemptDownloading));
            verify(sftpService, times(2)).deleteFile(any());
            AssertionsForClassTypes.assertThat(caughtException()).isInstanceOf(ConnectException.class);
        });
    }

    @Test
    @Order(21)
    @DisplayName("Verify file based connect exception during single download does not download file.")
    public void test_SetupSFTPConnectionVerifyServerConnectionSingleFileAndDoesNotDownloadFileException() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            final ENMScriptingVMLoadBalancer loadBalancer = new ENMScriptingVMLoadBalancer();
            loadBalancer.setScriptingVMs(Collections.singletonList(SCRIPTING_VM));
            final SFTPFileTransferService sftpFileTransferServiceSpy = spy(new SFTPFileTransferService(loadBalancer, eventFileDownloadConfiguration));

            assertTrue(sftpService.connect(sftpFileTransferServiceSpy, PORT, USER, PASSWORD));
            assertTrue(sftpService.isConnectionOpen(sftpFileTransferServiceSpy));

            final String remoteFilePathBad = ENM_FILE_PATH + FILE_NAME;
            final String localFilePath = tempDirectory + UNIX_SEPARATOR + new File(remoteFilePathBad).getName();

            final InputMessage testInputMessageBad = new InputMessage();
            testInputMessageBad.setNodeName(NODE_NAME);
            testInputMessageBad.setNodeType(NODE_TYPE);
            testInputMessageBad.setFileLocation(remoteFilePathBad);
            testInputMessageBad.setDataType(DATA_TYPE);

            final List<InputMessage> inputMessagesToAttemptDownloading = Collections.singletonList((testInputMessageBad));

            server.putFile(remoteFilePathBad, "Content of file", UTF_8);

            doThrow(new ConnectException(CONNECTION_EXCEPTION_FILE_DOWNLOAD_RETRIES_EXCEEDED_EXCEPTION)).when(sftpFileTransferServiceSpy).downloadFile(remoteFilePathBad, localFilePath);

            BDDCatchException.when(() -> sftpService.download(sftpFileTransferServiceSpy, inputMessagesToAttemptDownloading));
            verify(sftpService, times(0)).deleteFile(any());
            AssertionsForClassTypes.assertThat(caughtException()).isInstanceOf(ConnectException.class);
        });
    }
}
