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

package com.ericsson.oss.adc.service.output.topic;

import com.ericsson.oss.adc.PostStartup;
import com.ericsson.oss.adc.config.kafka.BootStrapServerConfigurationSupplier;
import com.ericsson.oss.adc.service.data.catalog.DataCatalogService;
import com.ericsson.oss.adc.service.input.topic.InputTopicService;
import com.ericsson.pm_event.PmEventOuterClass;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import lombok.SneakyThrows;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EmbeddedKafka(partitions = 3, kraft = false, brokerProperties = {
        "transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1"})
class OutputTopicServiceTest {

    @SpyBean
    private KafkaTemplate<String, PmEventOuterClass.PmEvent> kafkaOutputTemplate;

    //Required to stop dependency checks running due to "@EventListener(ApplicationReadyEvent.class)" during tests.
    @MockBean
    private PostStartup postStartup;

    @Autowired
    OutputTopicService outputTopicService;

    @Autowired
    KafkaAdmin kafkaAdmin;

    @Autowired
    BootStrapServerConfigurationSupplier bootStrapServerConfigurationSupplier;

    private final MockSchemaRegistryClient schemaRegistry = new MockSchemaRegistryClient();

    @Test
    @DisplayName("Should successfully setup the output topics")
    void test_setupOutputTopics() {
        outputTopicService.setupOutputTopics();
        /*
        Have to call the boolean check separately.
        EmbeddedKafka does not offer a way to check it's fully ready without using a consumer
        And Sonarqube is now scanning test code for sleeps or waits.
         */
        await().atMost(5, SECONDS).until(() -> outputTopicService.isTopicCreated(outputTopicService.getStandardizedTopicName()));
        await().atMost(5, SECONDS).until(() -> outputTopicService.isTopicCreated(outputTopicService.getNonStandardTopicName()));

    }

    @Test
    @DisplayName("Should successfully create topic name")
    void test_setStandardOutputTopicName() {
        String expected = "5g-pm-event-file-transfer-and-processing--standardized";
        assertEquals(expected, outputTopicService.getStandardizedTopicName());
    }

    @Test
    @DisplayName("Should successfully create topic name")
    void test_setNonStandardOutputTopicName() {
        String expected = "5g-pm-event-file-transfer-and-processing--ericsson";
        assertEquals(expected, outputTopicService.getNonStandardTopicName());
    }

    @Test
    @DisplayName("Should successfully build and create the topics on the embedded kafka server")
    void test_buildAndCreateTopics() {
        String standardizedTopicName = "5g-pm-event-file-transfer-and-processing--standardized";
        String nonStandardTopicName = "5g-pm-event-file-transfer-and-processing--ericsson";

        int expectedPartitions = 3;
        int expectedReplicas = 1;

        outputTopicService.buildAndCreateTopic(standardizedTopicName);
        outputTopicService.buildAndCreateTopic(nonStandardTopicName);
        Map<String, TopicDescription> topics = kafkaAdmin.describeTopics(standardizedTopicName, nonStandardTopicName);

        assertTrue(topics.containsKey(standardizedTopicName));
        assertTrue(topics.containsKey(nonStandardTopicName));
        assertEquals(expectedPartitions, topics.get(standardizedTopicName).partitions().size());
        assertEquals(expectedReplicas, topics.get(standardizedTopicName).partitions().get(0).replicas().size());
        assertEquals(expectedPartitions, topics.get(nonStandardTopicName).partitions().size());
        assertEquals(expectedReplicas, topics.get(nonStandardTopicName).partitions().get(0).replicas().size());

    }

    @Test
    @DisplayName("Should only add a topic once and not decrease the partitions")
    void test_buildAndCreateTopics_duplicateTopic() {
        String standardizedTopicName = "5g-pm-event-file-transfer-and-processing--standardized";
        int expectedPartitions = 3;
        int expectedReplicas = 1;

        int duplicatePartitions = 2;
        short duplicateReplicas = 1;
        NewTopic duplicateTopic = new NewTopic(standardizedTopicName, duplicatePartitions, duplicateReplicas);

        outputTopicService.buildAndCreateTopic(standardizedTopicName);
        kafkaAdmin.createOrModifyTopics(duplicateTopic);
        Map<String, TopicDescription> topics = kafkaAdmin.describeTopics(standardizedTopicName);

        assertNotNull(topics);
        assertEquals(expectedPartitions, topics.get(standardizedTopicName).partitions().size());
        assertEquals(expectedReplicas, topics.get(standardizedTopicName).partitions().get(0).replicas().size());

    }

    @Test
    @DisplayName("Should return true when the topic is created")
    void test_isTopicCreated_pass() {
        String standardizedTopicName = "5g-pm-event-file-transfer-and-processing--standardized";
        outputTopicService.buildAndCreateTopic(standardizedTopicName);
        assertTrue(outputTopicService.isTopicCreated(standardizedTopicName));
    }

    @Test
    @DisplayName("Should return false when the topic is not created")
    void test_isTopicCreated_fails() {
        String randomTopic = "5g-pm-event-file-transfer-and-processing--random";
        boolean isCreated = outputTopicService.isTopicCreated(randomTopic);
        assertFalse(isCreated);
    }

    @SneakyThrows
    @Test
    @DisplayName("Should return false when there is an Interrupted exception")
    void test_isTopicCreated_fails_interrupted_exception() {
        OutputTopicService spy = Mockito.spy(outputTopicService);
        AdminClient mock = Mockito.mock(AdminClient.class, Mockito.RETURNS_DEEP_STUBS);
        when(spy.getAdminClient()).thenReturn(mock);
        when(mock.listTopics().names().get()).thenThrow(InterruptedException.class);
        String standardizedTopicName = "5g-pm-event-file-transfer-and-processing--standardized";
        boolean isCreated = spy.isTopicCreated(standardizedTopicName);
        assertFalse(isCreated);
    }

    @SneakyThrows
    @Test
    @DisplayName("Should return false when there is an Execution exception")
    void test_isTopicCreated_fails_execution_exception() {
        OutputTopicService spy = Mockito.spy(outputTopicService);
        AdminClient mock = Mockito.mock(AdminClient.class, Mockito.RETURNS_DEEP_STUBS);
        when(spy.getAdminClient()).thenReturn(mock);
        when(mock.listTopics().names().get()).thenThrow(ExecutionException.class);
        String standardizedTopicName = "5g-pm-event-file-transfer-and-processing--standardized";
        boolean isCreated = spy.isTopicCreated(standardizedTopicName);
        assertFalse(isCreated);
    }
}
