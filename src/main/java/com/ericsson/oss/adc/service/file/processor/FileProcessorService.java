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
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

/**
 * Implementation for processing the 5G event files. This involves reading an event file from ephemeral storage,
 * decompressing it, breaking the events up into records and writing the records to Kafka.
 */
@Slf4j
@Component
public class FileProcessorService {

    private static final int BUFFER_SIZE = 65536;
    private final Counter numEventFilesProcessed;
    private final Counter numEventRead;
    private final Counter processedFileDataVolume;
    private final String outputPathPrefix;

    private final OutputTopicService outputTopicService;
    private final SubscriptionCache subscriptionCache;
    private final EventRegulation eventRegulation;

    public FileProcessorService(MeterRegistry meterRegistry, OutputTopicService outputTopicService,
                                @Value("${temp-directory}") String outputPathPrefix, SubscriptionCache subscriptionCache, EventRegulation eventRegulation) {
        //TODO consider consolidated metric class to initialize counters/gauges and remove direct dependency on MeterRegistry in classes
        this.numEventFilesProcessed = meterRegistry.counter("eric.oss.5gpmevt.filetx.proc:event.files.processed");
        this.numEventRead = meterRegistry.counter("eric.oss.5gpmevt.filetx.proc:event.read");
        this.processedFileDataVolume = meterRegistry.counter("eric.oss.5gpmevt.filetx.proc:processed.file.data.volume");
        this.outputTopicService = outputTopicService;
        this.outputPathPrefix = outputPathPrefix;
        this.subscriptionCache = subscriptionCache;
        this.eventRegulation = eventRegulation;
    }


    /**
     * Process the event file downloaded from ENM.
     * Transactional -If processing of file fails, exception will be thrown and transaction will be aborted.
     *
     * @param downloadedFile event file downloaded from ENM
     * @param nodeName       string name of ENM Node from which the event file came
     */
    public void processEventFile(File downloadedFile, String nodeName) throws IOException {
        Instant start = Instant.now();
        String decompressedFileLocation = null;
        Optional<String> downloadedFileLocation = Optional.empty();
        try {
            downloadedFileLocation = Optional.of(downloadedFile.getPath());
            log.debug("Processing file: '{}'", downloadedFileLocation.get());
            decompressedFileLocation = decompressEventFile(downloadedFileLocation.get(), outputPathPrefix);
            splitEvents(decompressedFileLocation, nodeName);
            this.numEventFilesProcessed.increment();
        } catch (IOException e) {
            log.error("Error processing file: '{}'", downloadedFile.getPath());
            log.debug("Stack trace - Error processing file: '{}': ", downloadedFile.getPath(), e);
            throw new IOException(e);
        } finally {
            log.debug("Cleaning up files now: [{}]  [{}]", downloadedFileLocation, decompressedFileLocation);
            if (downloadedFileLocation.isPresent()) {
                cleanUp(downloadedFileLocation.get());

                //clean up needed only if file downloaded needed to be decompressed
                if (!(downloadedFileLocation.get().equals(decompressedFileLocation))) {
                    cleanUp(decompressedFileLocation);
                }
            }
        }
        Instant end = Instant.now();
        long millis = Duration.between(start, end).toMillis();
        log.debug("Finished processing file '{}' in milli: {}", decompressedFileLocation, millis); // maybe remove this, replace with actual metrics
    }

    /**
     * Decompress the 5G event file and write to ephemeral storage on the pod
     *
     * @param inputFilePath    path to the compressed file to be decompressed
     * @param outputPathPrefix the beginning of output path to the decompressed file
     * @return String representing full path to decompressed file
     */
    protected String decompressEventFile(final String inputFilePath, final String outputPathPrefix) throws IOException {
        String fileName = new File(inputFilePath).getName();
        String hash = String.valueOf(String.valueOf(System.currentTimeMillis()).hashCode());
        if (!fileName.substring(fileName.lastIndexOf('.') + 1).equalsIgnoreCase("gz")) {
            log.debug("File already decompressed: {}", inputFilePath);
            return inputFilePath;
        }
        String decompressedFilePath = outputPathPrefix + fileName.substring(0, fileName.lastIndexOf('.')) + "-" + hash;
        byte[] buffer = new byte[BUFFER_SIZE];
        try (FileInputStream fileInputStream = new FileInputStream(inputFilePath);
             GZIPInputStream gZipInputStream = new GZIPInputStream(fileInputStream, BUFFER_SIZE);
             FileOutputStream fileOutputStream = new FileOutputStream(decompressedFilePath)) {
            int bytesRead;
            while ((bytesRead = gZipInputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
        }
        log.debug("File decompressed: {}", decompressedFilePath);

        return decompressedFilePath;
    }

    /**
     * Split the 5g event file into individual events
     *
     * @param fileLocation location of the PM event file to be processed
     * @param nodeName     string name of ENM Node from which the event file came
     * @return Metrics of total records read and total records sent
     */
    protected Metrics splitEvents(final String fileLocation, final String nodeName) throws IOException {
        Metrics metrics;
        long maxRecordLength = 0;
        long sumRecordLength = 0;
        long sentRecordCount = 0;
        long totalRecordCount = 0;
        Map<String, Long> unknownEventCount = new HashMap<>();

        if (subscriptionCache.filterByNodeName(nodeName)) {
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(fileLocation, "r");
                 FileChannel fileChannel = randomAccessFile.getChannel()) {
                log.debug("Starting to split events in file '{}'", fileLocation);
                MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
                final CodedInputStream eventStream = CodedInputStream.newInstance(mappedByteBuffer);
                while (!eventStream.isAtEnd()) {
                    final int recordLength = eventStream.readRawVarint32();
                    final int limit = eventStream.pushLimit(recordLength);
                    final int msgLengthSize = eventStream.getTotalBytesRead();
                    DecodedEvent decodedEvent = decodeEvent(eventStream, unknownEventCount);
                    log.debug("Message Length = {}, Push limit = {}, Message Length Size = {}, Event ID: {}, Node Name = {}",
                            recordLength, limit, msgLengthSize, decodedEvent.getEventID(), nodeName);
                    sumRecordLength += recordLength;
                    if (recordLength > maxRecordLength) {
                        maxRecordLength = recordLength;
                    }
                    log.debug("Max record length: {}, Sum record length: {}", maxRecordLength, sumRecordLength);
                    if (!decodedEvent.getEventID().isEmpty() && subscriptionCache.filterByEventID(decodedEvent.getEventID())) {
                        outputTopicService.sendKafkaMessage(decodedEvent, nodeName);
                        sentRecordCount++;
                    }
                    eventStream.popLimit(limit);
                    eventStream.resetSizeCounter();
                    numEventRead.increment();
                    totalRecordCount++;
                }
                processedFileDataVolume.increment(sumRecordLength);
            } catch (IOException e) {
                log.error("Error splitting events: {}", e.getMessage());
                log.debug("Stack trace - Error splitting events: ", e);
                throw e;
            }
        }
        if(!unknownEventCount.isEmpty()){
            log.warn("Number of unknown events dropped for node: {}, {}", nodeName, unknownEventCount);
        }
        metrics = new Metrics(sentRecordCount, totalRecordCount); // maybe remove this, replace with actual metrics
        return metrics;
    }

    /**
     * Decodes the body of individual events to retrieve the event's payload.
     *
     * @param eventStream stream of the current Event to be decoded
     * @return the decoded event
     */
    protected DecodedEvent decodeEvent(final CodedInputStream eventStream, Map<String, Long> unknownEventCount) throws InvalidProtocolBufferException {
        final PmEventOuterClass.PmEvent message = PmEventOuterClass.PmEvent.parser().parseFrom(eventStream);

        final String eventID = String.valueOf(message.getEventId());

        EventType eventType = eventRegulation.getEventPrivacy(eventID);
        //Always Drop Common Events, PI Containing Events, or if prop is disabled, Proprietary events.
        if ((eventType.equals(EventType.NON_STANDARD) && !eventRegulation.produceNonStandard())
                || eventType.equals(EventType.COMMON)
                || eventType.equals(EventType.PI_CONTAINING_EVENT)) {
            return new DecodedEvent();
        } else if (eventType.equals(EventType.NOT_FOUND)){
            unknownEventCount.merge(eventID, 1L, Long::sum);
            return new DecodedEvent();
        }
        return new DecodedEvent(eventID, message, eventRegulation.getEventPrivacy(eventID));
    }

    /**
     * Perform clean up after processing the 5G event file - removing temporary files from ephemeral storage
     *
     * @param fileToDelete the path to the file to be deleted
     */
    protected void cleanUp(final String fileToDelete) {
        if (fileToDelete != null) {
            try {
                log.debug("Deleting {}", fileToDelete);
                Path filePath = new File(fileToDelete).toPath();
                Files.delete(filePath);
            } catch (Exception e) {
                log.error("Error Attempting to delete file: {}", e.getMessage());
                log.debug("Stack trace - Error Attempting to delete file: ", e);
            }
        } else {
            log.error("File path provided for deletion is null");
        }
    }
}
