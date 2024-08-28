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

package com.ericsson.oss.adc.service.file.processor;

import com.ericsson.oss.adc.config.regulation.EventRegulation;
import com.ericsson.oss.adc.enums.EventType;
import com.ericsson.oss.adc.models.DecodedEvent;
import com.ericsson.oss.adc.models.Metrics;
import com.ericsson.oss.adc.service.output.topic.OutputTopicService;
import com.ericsson.oss.adc.service.subscription.filter.SubscriptionCache;
import com.ericsson.pm_event.PmEventOuterClass;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.zip.ZipException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
class FileProcessorServiceTest {
    private final String tempDirectory = "/tmp/";
    private final SimpleMeterRegistry simpleMeterRegistry = new SimpleMeterRegistry();
    @Mock
    private OutputTopicService outputTopicService;
    @Mock
    private SubscriptionCache subscriptionCache;

    @Mock
    private EventRegulation eventRegulation;

    private FileProcessorService fileProcessorService;

    @BeforeEach
    public void init() {
        fileProcessorService = new FileProcessorService(simpleMeterRegistry, outputTopicService, tempDirectory, subscriptionCache, eventRegulation);
    }

    @Test
    @DisplayName("Should successfully process the event file with allow all filtering and and not throw an Exception")
    void test_processEventFile() throws Exception {
        String testResourceDir = "src/test/resources/test-event-files/";
        String fileName = "5G-event-file-930.gpb";
        String fileLocation = testResourceDir + fileName;
        String nodeName = "5GTestNode";

        //need to create a copy of the test event file, or it will be deleted
        String copyFileName = "copy-5G-event-file-930.gpb";
        Path originalPath = Paths.get(fileLocation);
        Path copyPath = Paths.get(tempDirectory + copyFileName);
        Files.copy(originalPath, copyPath);
        File copyEventFile = copyPath.toFile();

        assertThat(copyPath).exists();

        when(eventRegulation.produceNonStandard()).thenReturn(true);
        when(eventRegulation.getEventPrivacy(anyString())).thenReturn(EventType.NON_STANDARD);
        doReturn(true).when(subscriptionCache).filterByNodeName(anyString());
        doReturn(true).when(subscriptionCache).filterByEventID(anyString());
        doNothing().when(outputTopicService).sendKafkaMessage(any(), anyString());
        fileProcessorService.processEventFile(copyEventFile, nodeName);

        verify(outputTopicService, times(930)).sendKafkaMessage(any(), anyString());
    }

    @Test
    @DisplayName("Should successfully process the event file but not send any prop events")
    void test_processEventFileAndSendNoPropEvents() throws Exception {
        String testResourceDir = "src/test/resources/test-event-files/";
        String fileName = "5G-event-file-930.gpb";
        String fileLocation = testResourceDir + fileName;
        String nodeName = "5GTestNode";

        //need to create a copy of the test event file, or it will be deleted
        String copyFileName = "copy-5G-event-file-930.gpb";
        Path originalPath = Paths.get(fileLocation);
        Path copyPath = Paths.get(tempDirectory + copyFileName);
        Files.copy(originalPath, copyPath);
        File copyEventFile = copyPath.toFile();

        assertThat(copyPath).exists();

        when(eventRegulation.produceNonStandard()).thenReturn(false);
        when(eventRegulation.getEventPrivacy(anyString())).thenReturn(EventType.NON_STANDARD);
        doReturn(true).when(subscriptionCache).filterByNodeName(anyString());
        fileProcessorService.processEventFile(copyEventFile, nodeName);

        verify(outputTopicService, times(0)).sendKafkaMessage(any(), anyString());
    }

    @Test
    @DisplayName("Should successfully process the event file with only 1 event being sent from filtering")
    void test_processEventFileWithFilter() throws Exception {
        String testResourceDir = "src/test/resources/test-event-files/";
        String fileName = "5G-event-file-930.gpb";
        String fileLocation = testResourceDir + fileName;
        String nodeName = "5GTestNode";
        String eventIDFileOpen = "1";

        //need to create a copy of the test event file, or it will be deleted
        String copyFileName = "copy-5G-event-file-930.gpb";
        Path originalPath = Paths.get(fileLocation);
        Path copyPath = Paths.get(tempDirectory + copyFileName);
        Files.copy(originalPath, copyPath);
        File copyEventFile = copyPath.toFile();

        assertThat(copyPath).exists();

        when(eventRegulation.produceNonStandard()).thenReturn(true);
        when(eventRegulation.getEventPrivacy(anyString())).thenReturn(EventType.NON_STANDARD);
        doReturn(true).when(subscriptionCache).filterByNodeName(anyString());
        doReturn(true).when(subscriptionCache).filterByEventID(eventIDFileOpen);
        doNothing().when(outputTopicService).sendKafkaMessage(any(), anyString());

        fileProcessorService.processEventFile(copyEventFile, nodeName);

        verify(outputTopicService, times(1)).sendKafkaMessage(any(), anyString());
    }

    @Test
    @DisplayName("Should not process any events from a NodeName that is unwanted")
    void test_processUnwantedNodeNameFile() throws Exception {
        String testResourceDir = "src/test/resources/test-event-files/";
        String fileName = "5G-event-file-930.gpb";
        String fileLocation = testResourceDir + fileName;
        String nodeName = "5GTestNode";
        String eventIDFileOpen = "1";

        //need to create a copy of the test event file, or it will be deleted
        String copyFileName = "copy-5G-event-file-930.gpb";
        Path originalPath = Paths.get(fileLocation);
        Path copyPath = Paths.get(tempDirectory + copyFileName);
        Files.copy(originalPath, copyPath);
        File copyEventFile = copyPath.toFile();

        assertThat(copyPath).exists();

        doReturn(false).when(subscriptionCache).filterByNodeName(nodeName);

        fileProcessorService.processEventFile(copyEventFile, nodeName);

        verify(outputTopicService, never()).sendKafkaMessage(any(), anyString());
    }

    @Test
    @DisplayName("Should throw RuntimeException after 2 messages sent, aborting the rest of the file processing")
    void test_processEventFile_TransactionIsAborted() throws Exception {
        String testResourceDir = "src/test/resources/test-event-files/";
        String fileName = "5G-event-file-930.gpb";
        String fileLocation = testResourceDir + fileName;
        String nodeName = "5GTestNode";

        //need to create a copy of the test event file or it will be deleted
        String copyFileName = "copy-5G-event-file-930.gpb";
        Path originalPath = Paths.get(fileLocation);
        Path copyPath = Paths.get(tempDirectory + copyFileName);
        Files.copy(originalPath, copyPath);
        File copyEventFile = copyPath.toFile();
        assertThat(copyPath).exists();

        when(eventRegulation.produceNonStandard()).thenReturn(true);
        when(eventRegulation.getEventPrivacy(anyString())).thenReturn(EventType.NON_STANDARD);
        doReturn(true).when(subscriptionCache).filterByNodeName(anyString());
        doReturn(true).when(subscriptionCache).filterByEventID(anyString());
        doNothing()
                .doThrow(RuntimeException.class).when(outputTopicService).sendKafkaMessage(any(DecodedEvent.class), anyString());

        assertThrows(RuntimeException.class, () -> fileProcessorService.processEventFile(copyEventFile, nodeName));
        verify(outputTopicService, times(2)).sendKafkaMessage(any(), anyString());
        assertThat(Paths.get(tempDirectory + "/" + fileName)).doesNotExist(); // expected download location
    }

    @Test
    @DisplayName("Should throw IOException when asked to process non-existent event file")
    void test_processEventFile_ioException() {
        String testResourceDir = "src/test/resources/test-event-files/";
        String fileName = "fake-file.gpb";
        String fileLocation = testResourceDir + fileName;
        String nodeName = "5GTestNode";

        Path eventFilePath = Paths.get(fileLocation);
        File eventFile = eventFilePath.toFile();

        doReturn(true).when(subscriptionCache).filterByNodeName(anyString());
        assertThrows(IOException.class, () -> fileProcessorService.processEventFile(eventFile, nodeName));

    }

    @Test
    @DisplayName("Should successfully decompress the event file and write to ephemeral storage")
    void test_decompressEventFile() throws IOException {
        String testResourceDir = "src/test/resources/test-gz-files/";
        String compressedFileName = "test_file.gz";
        String uncompressedFileName = "test_file";
        String outputFilePath = fileProcessorService.decompressEventFile(testResourceDir + compressedFileName, testResourceDir);

        File outputFile = new File(outputFilePath);
        assertTrue(outputFile.exists());

        byte[] expectedContents = Files.readAllBytes(Paths.get(testResourceDir + uncompressedFileName));
        byte[] actualContents = Files.readAllBytes(Paths.get(outputFilePath));
        assertArrayEquals(expectedContents, actualContents);
        assertTrue(outputFile.delete());
    }

    @Test
    @DisplayName("Should throw FileNotFound Exception when trying to decompress non-existing event file")
    void test_decompressEventFile_fileDoesNotExist() {
        String testResourceDir = "src/test/resources/test-gz-files/";
        String nonExistingFileName = "notHere.gz";
        assertThrows(FileNotFoundException.class,
                () -> fileProcessorService.decompressEventFile(testResourceDir + nonExistingFileName, testResourceDir));
    }

    @Test
    @DisplayName("Should return the path of the already uncompressed file")
    void test_decompressEventFile_fileAlreadyUnzipped() throws IOException {
        String testResourceDir = "src/test/resources/test-gz-files/";
        String uncompressedFileName = "test_file";

        String fileOutPath = fileProcessorService.decompressEventFile(testResourceDir + uncompressedFileName, testResourceDir);

        assertEquals(testResourceDir + uncompressedFileName, fileOutPath);
    }

    @Test
    @DisplayName("Should throw ZipException when file is not in gzip format and is corrupted.")
    void test_decompressEventFile_corruptedFile() {
        String testResourceDir = "src/test/resources/test-gz-files/";
        String corruptFileName = "corrupt.gz";
        String fileLocation = testResourceDir + corruptFileName;

        Exception exception = assertThrows(ZipException.class, () -> fileProcessorService.decompressEventFile(fileLocation, testResourceDir));

        String expectedMessage = "Not in GZIP format";
        String actualMessage = exception.getMessage();
        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    @DisplayName("Should successfully split 5g file into individual event records")
    void test_splitEvents() {
        //Expected number of events taken from parsing files using https://eteamspace.internal.ericsson.com/pages/viewpage.action?spaceKey=PB&title=LogTool+-+Download
        String testResourceDir = "src/test/resources/test-event-files/";
        String node = "5GTestNode";

        String firstFileName = "5G-event-file-930.gpb";
        long firstFileExpectedSentRecords = 930;
        long firstFileExpectedTotalRecords = 930;
        String firstFileLocation = testResourceDir + firstFileName;

        String secondFileName = "5G-event-file-3602.gpb";
        long secondFileExpectedSentRecords = 3602;
        long secondFileExpectedTotalRecords = 3602;
        String secondFileLocation = testResourceDir + secondFileName;

        String thirdFileName = "5G-event-file-9996.gpb";
        long thirdFileExpectedSentRecords = 9996;
        long thirdFileExpectedTotalRecords = 9996;
        String thirdFileLocation = testResourceDir + thirdFileName;

        outputTopicService.setupOutputTopics();

        when(eventRegulation.produceNonStandard()).thenReturn(true);
        when(eventRegulation.getEventPrivacy(anyString())).thenReturn(EventType.NON_STANDARD);
        doReturn(true).when(subscriptionCache).filterByNodeName(anyString());
        doReturn(true).when(subscriptionCache).filterByEventID(anyString());
        doNothing().when(outputTopicService).sendKafkaMessage(any(), anyString());

        Metrics firstMetrics = assertDoesNotThrow(() -> fileProcessorService.splitEvents(firstFileLocation, node));

        assertEquals(firstFileExpectedSentRecords, firstMetrics.getSentRecordCount());
        assertEquals(firstFileExpectedTotalRecords, firstMetrics.getTotalRecordCount());

        Metrics secondMetrics = assertDoesNotThrow(() -> fileProcessorService.splitEvents(secondFileLocation, node));

        assertEquals(secondFileExpectedSentRecords, secondMetrics.getSentRecordCount());
        assertEquals(secondFileExpectedTotalRecords, secondMetrics.getTotalRecordCount());

        Metrics thirdMetrics = assertDoesNotThrow(() -> fileProcessorService.splitEvents(thirdFileLocation, node));

        assertEquals(thirdFileExpectedSentRecords, thirdMetrics.getSentRecordCount());
        assertEquals(thirdFileExpectedTotalRecords, thirdMetrics.getTotalRecordCount());
    }

    @Test
    @DisplayName("Should throw IOException when splitting corrupted 5g event file")
    void test_splitEvents_corrupted() {
        String testResourceDir = "src/test/resources/test-event-files/";
        String corrupted5GFileName = "corrupted-5G-event-file.gpb";
        String corrupted5GFileLocation = testResourceDir + corrupted5GFileName;
        String node = "5GTestNode";

        doReturn(true).when(subscriptionCache).filterByNodeName(anyString());

        assertThrows(IOException.class, () -> fileProcessorService.splitEvents(corrupted5GFileLocation, node));
    }

    @Test
    @DisplayName("Should throw IOException when splitting a non-existent 5G event file")
    void test_splitEvents_fileNotFound() {
        String testResourceDir = "src/test/resources/test-event-files/";
        String fake5GFileName = "this-file-does-not-exist.gpb";
        String fake5GFileLocation = testResourceDir + fake5GFileName;
        String node = "5GTestNode";

        doReturn(true).when(subscriptionCache).filterByNodeName(anyString());

        assertThrows(IOException.class, () -> fileProcessorService.splitEvents(fake5GFileLocation, node));
    }

    @Test
    @DisplayName("Should correctly parse individual events from event file")
    void test_decodeEvent() throws IOException {
        String testResourceDir = "src/test/resources/test-event-files/";
        String testFile = "5G-event-file-930.gpb";
        long pmEventGroupVersion = 56;
        long pmEventCommonVersion = 13;
        long pmEventCorrectionVersion = 0;
        long eventID = 1L;
        String computeName = "RadioNode";
        String networkManagedElement = "";

        PmEventOuterClass.PmEventHeader header = PmEventOuterClass.PmEventHeader.newBuilder()
                .setTimeStamp(1622447600999L)
                .setSystemUuid(ByteString.copyFromUtf8("3771c000-75db-11e9-8e92-78d34771b165"))
                .setComputeName(computeName)
                .setNetworkManagedElement(networkManagedElement)
                .addPmEventJobIds(10000)
                .setUeTraceId(ByteString.copyFromUtf8(""))
                .setTraceReference(ByteString.copyFromUtf8(""))
                .setTraceRecordingSessionReference(ByteString.copyFromUtf8(""))
                .build();

        PmEventOuterClass.PmEvent event = PmEventOuterClass.PmEvent.newBuilder()
                .setPmEventGroupVersion(pmEventGroupVersion)
                .setPmEventCommonVersion(pmEventCommonVersion)
                .setPmEventCorrectionVersion(pmEventCorrectionVersion)
                .setEventId(eventID)
                .setPayload(ByteString.copyFrom(new byte[]{10, 2, 8, 56}))
                .setGroupValue(1)
                .setEtcmVersion(56)
                .setHeader(header)
                .build();

        DecodedEvent expectedDecodedEvent = new DecodedEvent(String.valueOf(eventID), event, EventType.STANDARDIZED);

        when(eventRegulation.getEventPrivacy(anyString())).thenReturn(EventType.STANDARDIZED);

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(testResourceDir + testFile, "r")) {
            FileChannel fileChannel = randomAccessFile.getChannel();
            MappedByteBuffer mapBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
            CodedInputStream eventStream = CodedInputStream.newInstance(mapBuffer);
            final int recordLength = eventStream.readRawVarint32();
            eventStream.pushLimit(recordLength);
            eventStream.getTotalBytesRead();

            DecodedEvent decodedEvent = assertDoesNotThrow(() -> fileProcessorService.decodeEvent(eventStream, new HashMap<>()));
            assertEquals(expectedDecodedEvent, decodedEvent);
        }
    }

    @Test
    @DisplayName("Should correctly drop PI containing events from event stream")
    void test_piContainingDecodedEventDropped() {
        long pmEventGroupVersion = 56;
        long pmEventCommonVersion = 13;
        long pmEventCorrectionVersion = 0;
        long eventID = 3014L;
        String computeName = "RadioNode";
        String networkManagedElement = "";

        PmEventOuterClass.PmEventHeader header = PmEventOuterClass.PmEventHeader.newBuilder()
                .setTimeStamp(1622447600999L)
                .setSystemUuid(ByteString.copyFromUtf8("3771c000-75db-11e9-8e92-78d34771b165"))
                .setComputeName(computeName)
                .setNetworkManagedElement(networkManagedElement)
                .addPmEventJobIds(10000)
                .setUeTraceId(ByteString.copyFromUtf8(""))
                .setTraceReference(ByteString.copyFromUtf8(""))
                .setTraceRecordingSessionReference(ByteString.copyFromUtf8(""))
                .build();

        PmEventOuterClass.PmEvent event = PmEventOuterClass.PmEvent.newBuilder()
                .setPmEventGroupVersion(pmEventGroupVersion)
                .setPmEventCommonVersion(pmEventCommonVersion)
                .setPmEventCorrectionVersion(pmEventCorrectionVersion)
                .setEventId(eventID)
                .setPayload(ByteString.copyFrom(new byte[]{10, 2, 8, 56}))
                .setGroupValue(3)
                .setEtcmVersion(56)
                .setHeader(header)
                .build();

        DecodedEvent expectedDecodedEvent = new DecodedEvent();
        CodedInputStream eventStream = CodedInputStream.newInstance(event.toByteArray());

        when(eventRegulation.getEventPrivacy("3014")).thenReturn(EventType.PI_CONTAINING_EVENT);

        DecodedEvent decodedEvent = assertDoesNotThrow(() -> fileProcessorService.decodeEvent(eventStream, new HashMap<>()));
        assertEquals(expectedDecodedEvent, decodedEvent);

    }

    @Test
    @DisplayName("Should throw InvalidProtocolBuffer Exception when decoding a corrupt Event")
    void test_decodeEvent_corrupted() throws IOException {
        String testResourceDir = "src/test/resources/test-event-files/";
        String testFile = "corrupted-5G-event-file.gpb";

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(testResourceDir + testFile, "r")) {
            FileChannel fileChannel = randomAccessFile.getChannel();
            MappedByteBuffer mapBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
            CodedInputStream eventStream = CodedInputStream.newInstance(mapBuffer);
            final int recordLength = eventStream.readRawVarint32();
            eventStream.pushLimit(recordLength);
            eventStream.getTotalBytesRead();

            assertThrows(InvalidProtocolBufferException.class, () -> fileProcessorService.decodeEvent(eventStream, new HashMap<>()));
        }
    }

    @Test
    @DisplayName("Should successfully perform the clean up")
    void test_cleanUp() throws IOException {
        String testResourceDir = "src/test/resources/test-gz-files/";
        String compressedFileName = "5G-event-file-930.gpb.gz";
        String copyCompressedFileName = "copy-5G-event-file-930.gpb.gz";

        String uncompressedTestResDir = "src/test/resources/test-event-files/";
        String uncompressedFileName = "5G-event-file-930.gpb";
        String copyUncompressedFileName = "copy-5G-event-file-930.gpb";

        Path originalCompressedPath = Paths.get(testResourceDir + compressedFileName);
        Path copyCompressedPath = Paths.get(testResourceDir + copyCompressedFileName);
        Files.copy(originalCompressedPath, copyCompressedPath);

        Path originalUncompressedPath = Paths.get(uncompressedTestResDir + uncompressedFileName);
        Path copyUncompressedPath = Paths.get(uncompressedTestResDir + copyUncompressedFileName);
        Files.copy(originalUncompressedPath, copyUncompressedPath);

        assertThat(copyCompressedPath).exists();
        assertThat(copyUncompressedPath).exists();

        fileProcessorService.cleanUp(copyCompressedPath.toString());
        fileProcessorService.cleanUp(copyUncompressedPath.toString());

        assertThat(copyCompressedPath).doesNotExist();
        assertThat(copyUncompressedPath).doesNotExist();
    }

    @Test
    @DisplayName("Should successfully delete uncompressed file even if compressed no longer exists")
    void test_cleanUp_compressedDoesNotExist() throws IOException {
        String compressedTestResDir = "/src/test/resources/test-gz-files/";
        String nonExistentCompressedFile = "nothere.gz";
        Path nonExistingPath = Paths.get(compressedTestResDir + nonExistentCompressedFile);

        String uncompressedTestResDir = "src/test/resources/test-event-files/";
        String uncompressedFileName = "5G-event-file-930.gpb";
        String copyUncompressedFileName = "copy-5G-event-file-930.gpb";

        Path originalUncompressedPath = Paths.get(uncompressedTestResDir + uncompressedFileName);
        Path copyUncompressedPath = Paths.get(uncompressedTestResDir + copyUncompressedFileName);
        Files.copy(originalUncompressedPath, copyUncompressedPath);

        fileProcessorService.cleanUp(nonExistingPath.toString());
        fileProcessorService.cleanUp(copyUncompressedPath.toString());

        assertThat(nonExistingPath).doesNotExist();
        assertThat(copyUncompressedPath).doesNotExist();
    }

    @Test
    @DisplayName("Should catch FileNotFound exception if neither file exists")
    void test_cleanUp_neitherFileExists() {
        String testRestDir = "/src/test/resources/test-gz-files/";
        String nonExistingCompressed = "nothere.gpb.gz";
        String nonExistingUncompressed = "nothere.gpb";

        assertDoesNotThrow(() -> fileProcessorService.cleanUp(testRestDir + nonExistingCompressed));
        assertDoesNotThrow(() -> fileProcessorService.cleanUp(testRestDir + nonExistingUncompressed));
    }

    @Test
    @DisplayName("Should catch exception when all parameters are null")
    void test_cleanUp_bothNull() {
        String compressedNull = null;
        String uncompressedNull = null;
        String pathNull = null;

        assertDoesNotThrow(() -> fileProcessorService.cleanUp(pathNull + compressedNull));
        assertDoesNotThrow(() -> fileProcessorService.cleanUp(pathNull + uncompressedNull));
    }
}
