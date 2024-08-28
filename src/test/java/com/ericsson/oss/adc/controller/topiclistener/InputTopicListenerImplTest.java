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

package com.ericsson.oss.adc.controller.topiclistener;

import com.ericsson.oss.adc.models.InputMessage;
import com.ericsson.oss.adc.service.file.processor.FileProcessorService;
import com.ericsson.oss.adc.service.input.topic.InputTopicService;
import com.ericsson.oss.adc.service.sftp.SFTPService;
import com.googlecode.catchexception.apis.BDDCatchException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static com.googlecode.catchexception.CatchException.caughtException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(classes = {InputTopicListenerImpl.class})
public class InputTopicListenerImplTest {

    @MockBean
    private SFTPService sftpServiceMock;

    @MockBean
    private FileProcessorService fileProcessorServiceMock;

    @MockBean
    private InputTopicService inputTopicService;

    @SpyBean
    private InputTopicListenerImpl inputTopicListener;

    @Value("${spring.kafka.topics.input.name}")
    private String topicName;

    private static final String CONNECTION_EXCEPTION_SFTP_CONNECTION_NEVER_ESTABLISHED = "SftpConnectionNeverEstablishedNoFilesDownloadedOrProcessed";
    private static final String CONNECTION_EXCEPTION_FILE_DOWNLOAD_RETRIES_EXCEEDED_EXCEPTION = "EventFileDownloadRetriesExceededException";

    private static final String NODE_NAME = "SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=ERBS-SUBNW-1,MeContext=ieatnetsimv6034-10_LTE35ERBS00104";
    private static final String NODE_TYPE = "RadioNode";
    private static final String DATA_TYPE = "PM_CELLTRACE";

    private static final String TEST_RESOURCE_DIR = "src/test/resources/test-event-files/";
    private static final String FILE_NAME = "5G-event-file-930.gpb";
    private static final String FILE_LOCATION = TEST_RESOURCE_DIR + FILE_NAME;
    private static final File EVENT_FILE = Paths.get(FILE_LOCATION).toFile();
    private static final InputMessage INPUT_MESSAGE = new InputMessage(NODE_NAME, NODE_TYPE, FILE_LOCATION, DATA_TYPE, EVENT_FILE);


    @Test
    @DisplayName("Should map the json correctly to the InputMessage model and call setUpSFTPConnectionAndDownloadFile and processEventFile methods once")
    public void test_input_topic_listener_parses_data_correctly() throws Exception {

        final List<InputMessage> downloadedInputMessages = Collections.singletonList(INPUT_MESSAGE);

        when(sftpServiceMock.setUpSFTPConnectionAndDownloadFiles(any())).thenReturn(downloadedInputMessages);
        doNothing().when(fileProcessorServiceMock).processEventFile(any(), any());

        assertDoesNotThrow(() -> inputTopicListener.listen(Collections.singletonList(INPUT_MESSAGE)));

        verify(sftpServiceMock, times(1)).setUpSFTPConnectionAndDownloadFiles(any());
        verify(fileProcessorServiceMock, times(1)).processEventFile(any(), any());
    }

    @Test
    @DisplayName("Should throw a ConnectException from failing to establish SFTP connection")
    public void test_input_topic_listener_catches_jsonProcessingException_when_deserializing_message() throws Exception {
        doThrow(ConnectException.class).when(sftpServiceMock).setUpSFTPConnectionAndDownloadFiles(any());

        BDDCatchException.when(() -> inputTopicListener.listen(Collections.singletonList(INPUT_MESSAGE)));

        verify(sftpServiceMock, times(1)).setUpSFTPConnectionAndDownloadFiles(any());
        verifyNoInteractions(fileProcessorServiceMock);
        assertThat(caughtException()).isInstanceOf(ConnectException.class);
    }

    @Test
    @DisplayName("Should throw a ConnectException when a SFTP connection is never established")
    public void test_input_topic_listener_throws_connectException_when_sftp_connection_fails() throws Exception {
        doThrow(ConnectException.class).when(sftpServiceMock).setUpSFTPConnectionAndDownloadFiles(any());

        BDDCatchException.when(() -> inputTopicListener.listen(Collections.singletonList(INPUT_MESSAGE)));
        doThrow(new ConnectException(CONNECTION_EXCEPTION_SFTP_CONNECTION_NEVER_ESTABLISHED)).when(sftpServiceMock).setUpSFTPConnectionAndDownloadFiles(any());

        BDDCatchException.when(() -> inputTopicListener.listen(Collections.singletonList(INPUT_MESSAGE)));
        verify(sftpServiceMock, times(2)).setUpSFTPConnectionAndDownloadFiles(any()); // twice, listener retries because connection not established
        verifyNoInteractions(fileProcessorServiceMock);
        assertThat(caughtException()).isInstanceOf(ConnectException.class);
    }

    @Test
    @DisplayName("Verify event file is skipped after failing to download for any file in a batch")
    public void test_input_topic_listener_skips_event_file_when_download_fails() throws Exception {
        when(sftpServiceMock.setUpSFTPConnectionAndDownloadFiles(any())).thenReturn(null);

        BDDCatchException.when(() -> inputTopicListener.listen(Collections.singletonList(INPUT_MESSAGE)));

        verify(sftpServiceMock, times(1)).setUpSFTPConnectionAndDownloadFiles(any());
        verifyNoInteractions(fileProcessorServiceMock);
        assertThat(caughtException()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Batch fails when the number of event file retry attempts is exceeded for any file in the batch causing a connect exception")
    public void test_input_topic_listener_fails_batch_download_when_numberOfEventFileDownloadRetries_exceeded() throws Exception {
        when(sftpServiceMock.setUpSFTPConnectionAndDownloadFiles(any())).thenThrow(new ConnectException(CONNECTION_EXCEPTION_FILE_DOWNLOAD_RETRIES_EXCEEDED_EXCEPTION));
        BDDCatchException.when(() -> inputTopicListener.listen(Collections.nCopies(5, INPUT_MESSAGE)));

        verify(sftpServiceMock, times(1)).setUpSFTPConnectionAndDownloadFiles(any());
        verifyNoInteractions(fileProcessorServiceMock);
        assertThat(caughtException()).isInstanceOf(ConnectException.class);
    }

    @Test
    @DisplayName("Should throw an IOException from failing to process the event file after download")
    public void test_input_topic_listener_throws_ioException_when_processEventFile_fails() throws Exception {
        final List<InputMessage> downloadedInputMessages = Collections.singletonList(INPUT_MESSAGE);

        when(sftpServiceMock.setUpSFTPConnectionAndDownloadFiles(any())).thenReturn(downloadedInputMessages);
        doThrow(IOException.class).when(fileProcessorServiceMock).processEventFile(any(), any());

        BDDCatchException.when(() -> inputTopicListener.listen(Collections.singletonList(INPUT_MESSAGE)));
        verify(sftpServiceMock, times(1)).setUpSFTPConnectionAndDownloadFiles(any());
        verify(fileProcessorServiceMock, times(1)).processEventFile(any(), any());
        assertThat(caughtException()).doesNotThrowAnyException();
    }
}