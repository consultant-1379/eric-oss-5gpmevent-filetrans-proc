/*******************************************************************************
 * COPYRIGHT Ericsson 2023
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

package com.ericsson.oss.adc.controller.subscription.listener;

import com.ericsson.oss.adc.PostStartup;
import com.ericsson.oss.adc.config.kafka.BootStrapServerConfigurationSupplier;

import com.ericsson.oss.adc.models.DataCatalogProperties;
import com.ericsson.oss.adc.models.Subscription;
import com.ericsson.oss.adc.models.adapters.SubDataJobAdapter;
import com.ericsson.oss.adc.service.data.catalog.DataCatalogServiceV2;
import com.ericsson.oss.adc.service.subscription.filter.SubscriptionCache;
import com.googlecode.catchexception.apis.BDDCatchException;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.ericsson.oss.adc.service.subscription.filter.SubscriptionsDelegate.subscriptionWithJustEventId;
import static com.ericsson.oss.adc.service.subscription.filter.SubscriptionsDelegate.subscriptionWithTwoPredicates;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//TODO revisit this after deprecation of NonR1. 40 seconds and tests things outside the scope of class under test
@SpringBootTest
@EmbeddedKafka(partitions = 3, brokerProperties = {"transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SubscriptionInputTopicKafkaTest {

    private static final Logger LOG = LoggerFactory.getLogger(SubscriptionInputTopicKafkaTest.class);
    private static final int TEN_SECONDS = 10;
    private static final int PARTITION_NUMBER = 0;
    private static final String KEY = "";
    private static final String SERVICE_INSTANCE_NAME = "serviceInstanceName";
    private static final String SERVICE_NAME = "serviceName";
    private static final String PRODUCER_PAYLOAD_VALUE = """
            {"version":"1","eventType":"subscriptionCreated","event":{"dataServiceName":"eric-oss-5gpmevt-filetx-proc","dataServiceInstanceName":"eric-oss-5gpmevt-filetx-proc--enm1","subscription":{"name":"tomsSubscriptionForKafkaListenerConcurrencyIsThree","consumer":null,"isMandatory":true,"rAppId":26,"status":"Active"},"dataTypes":[{"dataSpace":"5G","dataCategory":"PM_EVENTS","dataProvider":"RAN","schemaName":"PmEventOuterClass.PmEvent","schemaVersion":"1"}, {"dataSpace":"5G","dataCategory":"PM_EVENTS","dataProvider":"RAN","schemaName":"PmEventOuterClass.PmEvent","schemaVersion":"1"}, {"dataSpace":"5G","dataCategory":"PM_EVENTS","dataProvider":"RAN","schemaName":"PmEventOuterClass.PmEvent","schemaVersion":"1"}],"predicates":{"nodeName":["ConcurrenyThree"," * ERBS * "],"eventId":["200","300"]}}}//""";

    @Autowired
    private DataCatalogProperties dataCatalogProperties;

    @Autowired
    private KafkaListenerEndpointRegistry registry; // Need to start our listener for test as it doesn't start automatically

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    SubscriptionCache subscriptionCache;

    @SpyBean
    private SubscriptionInputTopicListenerImpl subscriptionTopicListener;

    @MockBean
    private PostStartup postStartup;

    @MockBean
    private DataCatalogServiceV2 dataCatalogServiceV2;

    @Value("${spring.kafka.topics.subscriptionInput.name}")
    private String topicName;

    @Value("${spring.kafka.subscriptionConsumer.group-id}")
    private String groupIdSubscriptionConsumerConfig;

    private Producer<String, String> producer;

    private final SubDataJobAdapter subDataJobAdapter = new SubDataJobAdapter();

    @BeforeAll
    void setUpClass() throws InterruptedException {
        createProducer();
        toggleListeners(true);
        TimeUnit.SECONDS.sleep(TEN_SECONDS); // Give Spring Boot time to boot!
    }

    @Test
    @Order(1)
    @DisplayName("Send message to the subscription listener, verifying the listener was called exactly once")
    public void subscriptionTopicMessageRecieved_1() throws Exception {
        final int PREVIOUS_SENT_MESSAGES = 0;
        final int NUMBER_OF_MESSAGES = 1;
        //the mock result contains only one subscription
        mockResponseFromDataCatalog();

        //Populate subscriptionCache with two subs
        subscriptionCache.addDataJobSummary(subDataJobAdapter.convertSubscription(subscriptionWithTwoPredicates));
        subscriptionCache.addDataJobSummary(subDataJobAdapter.convertSubscription(subscriptionWithJustEventId));
        assertEquals(2, subscriptionCache.getAllDataJobSummaryRAppIds().size());

        createAndSendMessages(PREVIOUS_SENT_MESSAGES, NUMBER_OF_MESSAGES, true);
        TimeUnit.SECONDS.sleep(TEN_SECONDS); // Wait 10 seconds for consumer to consume message in another thread.
        verify(subscriptionTopicListener, times(NUMBER_OF_MESSAGES)).listen(any(), any(), any());
        OffsetAndMetadata offsetAndMetadata = KafkaTestUtils.getCurrentOffset(embeddedKafkaBroker.getBrokersAsString(), groupIdSubscriptionConsumerConfig,
                topicName, PARTITION_NUMBER);
        assertEquals(NUMBER_OF_MESSAGES, offsetAndMetadata.offset());
        assertEquals(1, subscriptionCache.getAllDataJobSummaryRAppIds().size());

        subscriptionCache.removeDataJobSummary(subDataJobAdapter.convertSubscription(subscriptionWithTwoPredicates));
    }

    @Test
    @Order(2)
    @DisplayName("Send forty four messages to the subscription listener, verifying the listener was called exactly forty four times and subscription cache was updated")
    public void subscriptionTopicMessageReceived_44_subscriptionCacheUpdated() throws Exception {
        final int PREVIOUS_SENT_MESSAGES = 1;
        final int NUMBER_OF_MESSAGES = 44;
        //the mock result contains only one subscription
        mockResponseFromDataCatalog();

        //SubscriptionCache has no subscriptions
        assertEquals(0, subscriptionCache.getAllDataJobSummaryRAppIds().size());
        createAndSendMessages(PREVIOUS_SENT_MESSAGES, NUMBER_OF_MESSAGES, true);
        TimeUnit.SECONDS.sleep(TEN_SECONDS); // Wait 10 seconds for consumer to consume message in another thread.

        verify(subscriptionTopicListener, times(NUMBER_OF_MESSAGES)).listen(any(), any(), any());
        OffsetAndMetadata offsetAndMetadata = KafkaTestUtils.getCurrentOffset(embeddedKafkaBroker.getBrokersAsString(), groupIdSubscriptionConsumerConfig,
                topicName, PARTITION_NUMBER);
        assertEquals(NUMBER_OF_MESSAGES + PREVIOUS_SENT_MESSAGES, offsetAndMetadata.offset());

        //SubscriptionCache has been updated with one subscription
        assertEquals(1, subscriptionCache.getAllDataJobSummaryRAppIds().size());

        subscriptionCache.removeDataJobSummary(subDataJobAdapter.convertSubscription(subscriptionWithTwoPredicates));
    }

    @Test
    @Order(3)
    @DisplayName("Cache is updated with empty list if there is an empty array returned from Data Catalog")
    public void subscriptionTopicMessagesNoSubscriptionInDataCatalog() throws Exception {
        final int PREVIOUS_SENT_MESSAGES = 0;
        final int NUMBER_OF_MESSAGES = 1;
        final int ACCUMULATED_NUMBER_OF_MESSAGES = 46;

        //the mock result contains no subscription
        mockResponseFromDataCatalogNoSubscriptions();

        //Populate subscriptionCache with two subs
        subscriptionCache.addDataJobSummary(subDataJobAdapter.convertSubscription(subscriptionWithTwoPredicates));
        subscriptionCache.addDataJobSummary(subDataJobAdapter.convertSubscription(subscriptionWithJustEventId));
        assertEquals(2, subscriptionCache.getAllDataJobSummaryRAppIds().size());

        createAndSendMessages(PREVIOUS_SENT_MESSAGES, NUMBER_OF_MESSAGES, true);
        TimeUnit.SECONDS.sleep(TEN_SECONDS); // Wait 10 seconds for consumer to consume message in another thread.
        verify(subscriptionTopicListener, times(NUMBER_OF_MESSAGES)).listen(any(), any(), any());
        OffsetAndMetadata offsetAndMetadata = KafkaTestUtils.getCurrentOffset(embeddedKafkaBroker.getBrokersAsString(), groupIdSubscriptionConsumerConfig,
                topicName, PARTITION_NUMBER);
        assertEquals(ACCUMULATED_NUMBER_OF_MESSAGES, offsetAndMetadata.offset());

        //SubscriptionCache has not been updated
        assertEquals(0, subscriptionCache.getAllDataJobSummaryRAppIds().size());
    }

    @Test
    @Order(4)
    @DisplayName("Send messages to listener with missing headers, causing null pointer exception")
    public void subscriptionTopicMessagesNoHeaders() throws Exception {
        final int PREVIOUS_SENT_MESSAGES = 46;
        final int NUMBER_OF_MESSAGES = 10;

        createAndSendMessages(PREVIOUS_SENT_MESSAGES, NUMBER_OF_MESSAGES, false);
        TimeUnit.SECONDS.sleep(TEN_SECONDS); // Wait 10 seconds for consumer to consume message in another thread.
        BDDCatchException.when(() -> subscriptionTopicListener.listen(any(), any(), any()));
        assertThat(caughtException()).isInstanceOf(NullPointerException.class);
        verify(dataCatalogServiceV2, times(0)).getSubscriptions();

        //SubscriptionCache has not been updated
        assertEquals(0, subscriptionCache.getAllDataJobSummaryRAppIds().size());
    }

    @AfterAll
    void tearDownClass() {
        producer.close();
        toggleListeners(false);
    }

    private void createAndSendMessages(int startingIndex, int count, boolean addHeaders) {

        for (int i = startingIndex; i < count + startingIndex; i++) {
            ProducerRecord<String, String> producerRecord = createProducerRecord(PRODUCER_PAYLOAD_VALUE, addHeaders);
            sendToInputTopic(producerRecord);
        }
    }

    private ProducerRecord<String, String> createProducerRecord(final String value, boolean addHeaders) {
        List<Header> headers = new ArrayList<>();
        headers.add(new RecordHeader(SERVICE_INSTANCE_NAME, (dataCatalogProperties.getDataServiceName() + "--" + dataCatalogProperties.getDataCollectorName()).getBytes()));
        headers.add(new RecordHeader(SERVICE_NAME, dataCatalogProperties.getDataServiceName().getBytes()));
        if (addHeaders) {
            return new ProducerRecord<>(topicName, PARTITION_NUMBER, KEY, value, headers);
        } else {
            return new ProducerRecord<>(topicName, PARTITION_NUMBER, KEY, value);
        }
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
        LOG.info("Toggle Listeners: " + messageListenerContainers.size());

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

    private void mockResponseFromDataCatalog() {
        Subscription[] subscriptions = new Subscription[]{subscriptionWithTwoPredicates};
        when(dataCatalogServiceV2.getSubscriptions()).thenReturn(subscriptions);
    }

    private void mockResponseFromDataCatalogNoSubscriptions() {
        when(dataCatalogServiceV2.getSubscriptions()).thenReturn(new Subscription[]{});
    }
}
