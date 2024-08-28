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

package com.ericsson.oss.adc.integration;

import com.ericsson.oss.adc.PostStartup;
import com.ericsson.oss.adc.availability.UnsatisfiedExternalDependencyException;
import com.ericsson.oss.adc.config.SchemaRegistryConfigMock;
import com.ericsson.oss.adc.config.kafka.BootStrapServerConfigurationSupplier;
import com.ericsson.oss.adc.kafka_components.ConsumerTestConfig;
import com.ericsson.oss.adc.kafka_components.ConsumerTester;
import com.ericsson.oss.adc.models.DecodedEvent;
import com.ericsson.oss.adc.models.Ids;
import com.ericsson.oss.adc.models.Subscription;
import com.ericsson.oss.adc.models.data.catalog.r1.DataJobSummary;
import com.ericsson.oss.adc.models.data.catalog.r1.InterfaceType;
import com.ericsson.oss.adc.service.output.topic.OutputTopicService;
import com.ericsson.oss.adc.service.schema.registry.SchemaRegistryService;
import com.ericsson.oss.adc.service.subscription.filter.SubscriptionCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.github.stefanbirkner.fakesftpserver.lambda.FakeSftpServer.withSftpServer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/*
 Active profiles used to contextually load ConsumerTester only for this test. Tried declaring ConsumerTester as a TestComponent only
 but this stopped Spring from loading all other beans/components/services as required. This way is far cleaner.
 */
@ActiveProfiles("E2E")
@Slf4j
@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@ContextConfiguration(classes = {SchemaRegistryConfigMock.class, ConsumerTestConfig.class})
@EmbeddedKafka(topics = {"5g-pm-event-file-transfer-and-processing--standardized",
        "5g-pm-event-file-transfer-and-processing--ericsson",
        "file-notification-service--5g-event--enm1"},
        partitions = 3,
        kraft = false,
        brokerProperties = {"transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1"})
class PmEventFileTransProcTest {

    private static final int PORT = 5678;
    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private static final int PARTITION_NUMBER = 3;

    @Value("${connected.systems.base-url}${connected.systems.port}${connected.systems.uri}?name=enm1")
    private String expectedConnectedSysUrl;

    @Value("${temp-directory}")
    private String tempDirectory;

    @Value("${spring.kafka.topics.input.name}")
    private String topicName;

    @SpyBean
    private OutputTopicService outputTopicService;

    @Autowired
    private ConsumerTester consumerTester;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private KafkaListenerEndpointRegistry registry; // Need to start our listener for test as it doesn't start automatically

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    //Required to stop dependency checks running due to "@EventListener(ApplicationReadyEvent.class)" during tests.
    @MockBean
    private PostStartup postStartup;

    @Autowired
    private SubscriptionCache subscriptionCache;

    private MockRestServiceServer mockServer;
    private Producer<String, String> producer;
    @Autowired
    private SchemaRegistryService schemaRegistryService;

    @AfterEach
    public void cleanUp() {
        consumerTester.reset();
        producer.close(Duration.ofMillis(50));
    }

    @BeforeEach
    public void init() throws URISyntaxException, UnsatisfiedExternalDependencyException {
        createProducer();
        toggleListeners(true);
        setupMockRestServer();
        schemaRegistryService.registerSchemasWithRetry();
        outputTopicService.setupOutputTopics();
    }

    @Test
    @DisplayName("Should read the message from the input topic, successfully download the event file and process it, sending 696 events out of a possible 930")
    void test_end_to_end_success() throws Exception {
        //Subscription for 696 events
        int expectedEventsTotal = 696;
        Set<String> expectedEventIDS = Set.of("2053", "2011", "2054");

        //Subbed for eventID 1 and 2, but because common events, they are not sent. Sanity check for NOT_FOUND eventType
        DataJobSummary dataJobSummary = DataJobSummary.builder()
                .rAppID("1000")
                .dataDeliverySchemaId("PmEventOuterClass.PmEvent")
                .interfaceType(InterfaceType.NON_R1)
                .nodeNames(List.of("*5GTest*"))
                .eventId(List.of("1", "2", "2053", "2011", "2054"))
                .build();
        subscriptionCache.addDataJobSummary(dataJobSummary);

        consumerTester.setCountDownLatch(expectedEventsTotal);

        String testResourceDir = "src/test/resources/test-gz-files/";
        String fileName = "5G-event-file-930.gpb.gz";
        String fileLocation = testResourceDir + fileName;
        String nodeName = "5GTestNode";

        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            final String serverFilePath = "/" + fileLocation;
            server.putFile(serverFilePath, new FileInputStream(fileLocation));

            createAndSendKafkaMessage(nodeName, serverFilePath);

            consumerTester.getLatch().await(15, TimeUnit.SECONDS);

            assertEquals(expectedEventsTotal, consumerTester.getRecordCount());
            assertThat(Paths.get(tempDirectory + "/" + fileName)).doesNotExist(); // expected download location
        });

        assertTrue(expectedEventIDS.containsAll(consumerTester.getEventsEncountered()));
        subscriptionCache.removeDataJobSummary(dataJobSummary);
    }

    @Test
    @DisplayName("Should catch IO exception when processing corrupt event file and not retry processing the batch")
    void test_end_to_end_failure_catches_ioException() throws Exception {
        DataJobSummary dataJobSummary = DataJobSummary.builder()
                .rAppID("1000")
                .dataDeliverySchemaId("PmEventOuterClass.PmEvent")
                .interfaceType(InterfaceType.NON_R1)
                .nodeNames(List.of("*5GTest*"))
                .eventId(List.of("*"))
                .build();
        subscriptionCache.addDataJobSummary(dataJobSummary);

        consumerTester.setCountDownLatch(930);
        String testResourceDir = "src/test/resources/test-gz-files/";
        String fileName = "corrupt.gz";
        String fileLocation = testResourceDir + fileName;
        String nodeName = "5GTestNode";

        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            final String serverFilePath = "/" + fileLocation;
            server.putFile(serverFilePath, new FileInputStream(fileLocation));

            createAndSendKafkaMessage(nodeName, serverFilePath);

            consumerTester.getLatch().await(5, TimeUnit.SECONDS);

            assertEquals(0, consumerTester.getRecordCount());
            assertThat(Paths.get(tempDirectory + "/" + fileName)).doesNotExist(); // expected download location
        });
        subscriptionCache.removeDataJobSummary(dataJobSummary);
    }

    @Test
    @DisplayName("Should throw RuntimeException on 2nd message being sent, aborting the transaction and retrying. Rapp should only receive each event once")
    void test_end_to_end_RollBackEOS() throws Exception {
        DataJobSummary dataJobSummary = DataJobSummary.builder()
                .rAppID("1000")
                .dataDeliverySchemaId("PmEventOuterClass.PmEvent")
                .interfaceType(InterfaceType.NON_R1)
                .nodeNames(List.of("*5GTest*"))
                .eventId(List.of("*"))
                .build();
        subscriptionCache.addDataJobSummary(dataJobSummary);

        int expectedEvents = 930 - 2; //minus the two common events that will not be sent

        consumerTester.setCountDownLatch(expectedEvents);

        String testResourceDir = "src/test/resources/test-gz-files/";
        String fileName = "5G-event-file-930.gpb.gz";
        String fileLocation = testResourceDir + fileName;
        String nodeName = "5GTestNode";

        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            final String serverFilePath = "/" + fileLocation;
            server.putFile(serverFilePath, new FileInputStream(fileLocation));

            /*
            This test class was using @DirtiesContext, which hid a breaking change to Transactions and EOS processing.
            Messages from a failed batch were still being committed, resulting in duplicate messages being broadcast to Rapp.
            Word of warning, don't use @DirtiesContext
             - Shaun
            */
            doCallRealMethod()
                    .doThrow(RuntimeException.class)
                    .doCallRealMethod()
                    .when(outputTopicService).sendKafkaMessage(any(DecodedEvent.class), anyString());

            createAndSendKafkaMessage(nodeName, serverFilePath);

            consumerTester.getLatch().await(15, TimeUnit.SECONDS);
            assertEquals(expectedEvents, consumerTester.getRecordCount());
            verify(outputTopicService, times(930)).sendKafkaMessage(any(), anyString());
            assertThat(Paths.get(tempDirectory + "/" + fileName)).doesNotExist(); // expected download location
        });
        subscriptionCache.removeDataJobSummary(dataJobSummary);
    }

    private void createAndSendKafkaMessage(String nodeName, String fileLocation) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("nodeName", nodeName);
        jsonObject.put("fileLocation", fileLocation);
        String payload = jsonObject.toString();

        ProducerRecord<String, String> producerRecord = createProducerRecord(nodeName, payload);
        sendToInputTopic(producerRecord);
    }

    private ProducerRecord<String, String> createProducerRecord(final String nodeName, final String jsonInputTopicPayload) {
        return new ProducerRecord<>(topicName, nodeName, jsonInputTopicPayload);
    }

    private void sendToInputTopic(ProducerRecord<String, String> producerRecord) {
        producer.send(producerRecord);
    }

    private void createProducer() {
        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));
        producer = new DefaultKafkaProducerFactory<>(configs, new StringSerializer(), new StringSerializer()).createProducer();
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

    private void setupMockRestServer() throws URISyntaxException {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        mockServer.expect(requestTo(new URI(expectedConnectedSysUrl)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new InputStreamResource(Objects.requireNonNull(getClass().getClassLoader()
                                .getResourceAsStream("GetSFTPSubsystemsResponse.json")))));
    }
}
