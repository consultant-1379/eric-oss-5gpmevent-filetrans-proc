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
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ConnectException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Implementation for listening to 5G ENM event file notification topic to consume event files
 */
@Component
public class InputTopicListenerImpl {
    /**
     * Logger for the class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(InputTopicListenerImpl.class);

    private static final String LISTENER_ID = "inputTopic5gEventKafkaListener";

    @Value("${spring.kafka.topics.input.name}")
    private String inputTopicName;

    private SFTPService sftpService;
    private FileProcessorService fileProcessorService;
    private InputTopicService inputTopicService;

    @Autowired
    public InputTopicListenerImpl(final SFTPService sftpService, final FileProcessorService fileProcessorService, final InputTopicService inputTopicService) {
        this.sftpService = sftpService;
        this.fileProcessorService = fileProcessorService;
        this.inputTopicService = inputTopicService;
    }

    /**
     * Batch Listener for the {@link InputTopicListenerImpl#inputTopicName} topic.
     * When a notification message is received an SFTP connection with an ENM Scripting VM will be established,
     * a batch of event files will be downloaded and then processed on one SFTP connection.
     * Infinite retry for SFTP connection exceptions.
     * Will retry the configured number of times for a single event file download where a STFP connection is established.
     * If a download fails after the configured number of attempts, a connect exception is thrown and all downloaded files are deleted.
     * If a file is not found on the server, it is skipped.
     * The SFTP connection can currently fail before or during a batch file download.
     * No retry for parsing / processing exceptions. Event file will be skipped.
     *
     * @param consumerRecords A {@link List}<{@link ConsumerRecord}> holding the {@link InputMessage}'s.
     * @throws JsonProcessingException Should only occur if payload is not valid JSON to be deserialized to {@link InputMessage}
     */
    @KafkaListener(
            id = LISTENER_ID,
            idIsGroup = false,
            containerFactory = "consumerKafkaListenerContainerFactory",
            topics = "${spring.kafka.topics.input.name}",
            concurrency = "${spring.kafka.topics.input.concurrency}",
            autoStartup = "false"
    )
    public void listen(final List<InputMessage> consumerRecords) throws ConnectException {
        final long start = System.currentTimeMillis();

        LOG.info("Batch '{}' received from topic of size: {}",
                consumerRecords.hashCode(), consumerRecords.size());


        int recordNumber = 0;

        final long startSftpDownload = System.nanoTime();
        final List<InputMessage> fileNotificationsWithDownloadedFiles = sftpService.setUpSFTPConnectionAndDownloadFiles(consumerRecords);

        final long timeToDownloadSftpFiles = System.nanoTime() - startSftpDownload;
        final long startProcessingFiles = System.nanoTime();
        if (fileNotificationsWithDownloadedFiles != null) {
            for (InputMessage inputMessage : fileNotificationsWithDownloadedFiles) {
                try {
                    fileProcessorService.processEventFile(inputMessage.getDownloadedFile(), inputMessage.getNodeName());
                    LOG.debug("Consumer record successfully processed");
                } catch (IOException e) {
                    LOG.error("Failed to process event file {}, event file will be skipped: {}",
                            inputMessage.getFileLocation(), e.getMessage());
                    LOG.debug("Stack trace - Failed to process event file {}, event file will be skipped: ",
                            inputMessage.getFileLocation(), e);
                }
                recordNumber++;
            }
        }
        final long timeToProcessFiles = System.nanoTime() - startProcessingFiles;

        final long timeToProcessBatch = System.currentTimeMillis() - start;
        inputTopicService.recordTimer(timeToProcessBatch, recordNumber);
        LOG.info("Finished processing batch '{}' of size {}, sftpDownloadTime ns {}, processingFilesTime ns {}, batch-time ms {}",
                consumerRecords.hashCode(),
                consumerRecords.size(),
                timeToDownloadSftpFiles,
                timeToProcessFiles,
                timeToProcessBatch);
    }
}
