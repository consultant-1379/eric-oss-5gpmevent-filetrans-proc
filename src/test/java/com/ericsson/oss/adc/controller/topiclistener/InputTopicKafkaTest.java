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

import com.ericsson.oss.adc.PostStartup;
import com.ericsson.oss.adc.config.kafka.BootStrapServerConfigurationSupplier;
import com.ericsson.oss.adc.models.InputMessage;
import com.ericsson.oss.adc.models.Subscription;
import com.ericsson.oss.adc.models.Ids;
import com.ericsson.oss.adc.models.data.catalog.r1.DataJobSummary;
import com.ericsson.oss.adc.models.data.catalog.r1.InterfaceType;
import com.ericsson.oss.adc.service.file.processor.FileProcessorService;
import com.ericsson.oss.adc.service.input.topic.InputTopicService;
import com.ericsson.oss.adc.service.sftp.SFTPService;
import com.ericsson.oss.adc.service.subscription.filter.SubscriptionCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.json.JSONException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@EmbeddedKafka(partitions = 3, brokerProperties = {"transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class InputTopicKafkaTest {

    private static final int TEN_SECONDS = 10;
    private static final int PARTITION_NUMBER = 0;

    @Autowired
    private KafkaListenerEndpointRegistry registry; // Need to start our listener for test as it doesn't start automatically

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private SubscriptionCache subscriptionCache;

    @MockBean
    private SFTPService sftpServiceMock;

    @MockBean
    private InputTopicService inputTopicServiceMock;

    @MockBean
    private FileProcessorService fileProcessorServiceMock;

    @SpyBean
    private InputTopicListenerImpl inputTopicListenerImpl;

    //Required to stop dependency checks running due to "@EventListener(ApplicationReadyEvent.class)" during tests.
    @MockBean
    private PostStartup postStartup;

    @Value("${spring.kafka.topics.input.name}")
    private String topicName;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupIdConsumerConfig;

    private Producer<String, FileNotificationDTO> producer;
    private static final String TEST_RESOURCE_DIR = "src/test/resources/test-event-files/";
    private static final String FILE_NAME = "5G-event-file-930.gpb";
    private static final String FILE_LOCATION = TEST_RESOURCE_DIR + FILE_NAME;
    private static final File EVENT_FILE = Paths.get(FILE_LOCATION).toFile();

    private static final String NODE_NAME = "SubNetwork=ONRM_ROOT_MO_R,SubNetwork=5G,MeContext=NE00000650,ManagedElement=NE00000650";
    private static final String NODE_TYPE = "RadioNode";
    private static final String DATA_TYPE = "PM_CELLTRACE";
    private static final InputMessage INPUT_MESSAGE = new InputMessage(NODE_NAME, NODE_TYPE, FILE_LOCATION, DATA_TYPE, EVENT_FILE);


    @BeforeAll
    void setUpClass() throws InterruptedException {
        // bypass filter by accepting all node names
        DataJobSummary dataJobSummary = DataJobSummary.builder()
                .rAppID("1000")
                .dataDeliverySchemaId("PmEventOuterClass.PmEvent")
                .interfaceType(InterfaceType.NON_R1)
                .nodeNames(List.of("*"))
                .eventId(List.of("1", "2"))
                .build();

        subscriptionCache.addDataJobSummary(dataJobSummary);
        createProducer();
        toggleListeners(true);
        TimeUnit.SECONDS.sleep(TEN_SECONDS); // Give Spring Boot time to boot!
    }

    @Test
    @Order(1)
    @DisplayName("1 Should run the test with batch size 1 verifying the listener was called exactly once")
    public void the_acknowledge_file_processed() throws Exception {

        final int NUMBER_OF_FILES = 1;

        List<InputMessage> downloadedInputMessages = Collections.singletonList(INPUT_MESSAGE);

        when(sftpServiceMock.setUpSFTPConnectionAndDownloadFiles(any())).thenReturn(downloadedInputMessages);
        doNothing().when(fileProcessorServiceMock).processEventFile(any(), any());

        createAndSendBatchOfMessages(0, NUMBER_OF_FILES);
        TimeUnit.SECONDS.sleep(TEN_SECONDS); // Wait 10 seconds for consumer to consume message in another thread.
        verify(inputTopicListenerImpl, times(1)).listen(any());
        verify(sftpServiceMock, times(NUMBER_OF_FILES)).setUpSFTPConnectionAndDownloadFiles(any());
        verify(fileProcessorServiceMock, times(NUMBER_OF_FILES)).processEventFile(any(), any());

        OffsetAndMetadata offsetAndMetadata = KafkaTestUtils.getCurrentOffset(embeddedKafkaBroker.getBrokersAsString(), groupIdConsumerConfig,
                topicName, PARTITION_NUMBER);
        assertEquals(NUMBER_OF_FILES, offsetAndMetadata.offset());
    }

    @Test
    @Order(2)
    @DisplayName("2 Should run the test and process the batches as expected")
    public void the_batch_files_processed() throws Exception {
        final int PREVIOUS_TESTS_FILES = 1;
        final int NUMBER_OF_FILES = 20;
        final int MINIMUM_NUMBER_OF_BATCHES = 4;

        List<InputMessage> downloadedInputMessages = Collections.singletonList(INPUT_MESSAGE);

        when(sftpServiceMock.setUpSFTPConnectionAndDownloadFiles(any())).thenReturn(downloadedInputMessages);
        doNothing().when(fileProcessorServiceMock).processEventFile(any(), any());

        createAndSendBatchOfMessages(PREVIOUS_TESTS_FILES, NUMBER_OF_FILES);

        TimeUnit.SECONDS.sleep(TEN_SECONDS);  // Wait 10 seconds for consumer to consume message in another thread.

        ArgumentCaptor<List> filesSentCaptor = ArgumentCaptor.forClass(List.class);

        // Captures arguments supplied to setUpSFTPConnectionAndDownloadFiles
        verify(sftpServiceMock, atLeast(MINIMUM_NUMBER_OF_BATCHES)).setUpSFTPConnectionAndDownloadFiles((List<InputMessage>) filesSentCaptor.capture());
        List<List> filesSent = filesSentCaptor.getAllValues();

        int sentFilesCount = filesSent.stream().map(List::size).reduce(Integer::sum).get();

        // We can assert the number of downloaded files is equal to the number of consumer records
        assertEquals(sentFilesCount, NUMBER_OF_FILES);

        // We verify every message hits process file to ensure batches are being processed
        // We cannot predict how many files the listener will receive at a time, so we cannot correctly setup the list of downloaded files to
        // be returned by the mock SFTPServer. We know that the download was called the correct number of times by the size of the Captor.
        // All we can assert on is that the processEventFile method is called for the minimum number of required batches.
        verify(fileProcessorServiceMock, atLeast(MINIMUM_NUMBER_OF_BATCHES)).processEventFile(any(), any());

        OffsetAndMetadata offsetAndMetadata = KafkaTestUtils.getCurrentOffset(embeddedKafkaBroker.getBrokersAsString(), groupIdConsumerConfig,
                topicName, PARTITION_NUMBER);

        assertEquals(NUMBER_OF_FILES + PREVIOUS_TESTS_FILES, offsetAndMetadata.offset());
    }

    @AfterAll
    void tearDownClass() {
        producer.close();
        toggleListeners(false);
    }

    private void createAndSendBatchOfMessages(int startingIndex, int count) throws JSONException {

        for (int i = startingIndex; i < count + startingIndex; i++) {

            FileNotificationDTO fileNotificationDTO = new FileNotificationDTO();  // We use a different object to instantiate to verify loose coupling on deserialization through Spring Kafka Consumer

            fileNotificationDTO.setNodeName("testDynamicNode" + i);
            fileNotificationDTO.setFileLocation(FILE_LOCATION);

            ProducerRecord<String, FileNotificationDTO> producerRecord = createProducerRecord(fileNotificationDTO);
            sendToInputTopic(producerRecord);
        }
    }

    private ProducerRecord<String, FileNotificationDTO> createProducerRecord(final FileNotificationDTO fileNotificationDTO) {
        return new ProducerRecord<>(topicName, "", fileNotificationDTO);
    }

    private void sendToInputTopic(ProducerRecord<String, FileNotificationDTO> producerRecord) {
        producer.send(producerRecord);
    }

    private void createProducer() {
        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));
        producer = new DefaultKafkaProducerFactory<>(configs, new StringSerializer(), new JsonSerializer<FileNotificationDTO>()).createProducer();
    }

    private void toggleListeners(final boolean start) {
        Collection<MessageListenerContainer> messageListenerContainers = registry.getAllListenerContainers();
        log.info("Toggle Listeners: " + messageListenerContainers.size());

        for (MessageListenerContainer messageListenerContainer : messageListenerContainers) {
            if (start) {
                if (!messageListenerContainer.isRunning()) {
                    messageListenerContainer.start();
                    // Will break if we add new listeners or partition assignment changes
                    ContainerTestUtils.waitForAssignment(messageListenerContainer, PARTITION_NUMBER);
                }
            } else {
                messageListenerContainer.stop();
            }
        }
    }
}
