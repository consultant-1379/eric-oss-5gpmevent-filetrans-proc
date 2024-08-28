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

package com.ericsson.oss.adc.availability;

import com.ericsson.oss.adc.PostStartup;
import com.ericsson.oss.adc.config.kafka.BootStrapServerConfigurationSupplier;
import com.ericsson.oss.adc.service.input.topic.InputTopicService;
import lombok.SneakyThrows;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest
@EmbeddedKafka
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DependentServiceAvailabilityKafkaTest {

    @Value("${spring.kafka.topics.input.name}")
    private String inputTopicName;

    @Value("${spring.kafka.topics.subscriptionInput.name}")
    private String subscriptionInputTopicName;

    //Required to stop dependency checks running due to "@EventListener(ApplicationReadyEvent.class)" during tests.
    @MockBean
    private PostStartup postStartup;

    @MockBean
    private InputTopicService inputTopicService;

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Autowired
    private DependentServiceAvailabilityKafka dependentServiceAvailabilityKafka;

    @Test
    @DisplayName("When input topic and subscription input topic doesn't exist, expect check to fail")
    @Order(1)
    void test_input_topic_subscription_input_topic_does_not_exist() {
        final boolean result = dependentServiceAvailabilityKafka.checkService();
        assertFalse(result);
    }

    @Test
    @DisplayName("When input topic exists, expect check to pass")
    @Order(2)
    void test_input_topic_does_exist() {
        List<String> inputTopicList = new ArrayList<>(List.of(inputTopicName, subscriptionInputTopicName));
        buildAndCreateTopic(inputTopicList);
        final boolean result = dependentServiceAvailabilityKafka.checkService();
        assertTrue(result);
    }

    @SneakyThrows
    @Test
    @DisplayName("When input topic and subscription input topic exist, expect check to pass")
    @Order(3)
    void test_input_topic_subscription_input_topic_does_exist() {
        List<String> inputTopicList = new ArrayList<>(Arrays.asList(inputTopicName, subscriptionInputTopicName));
        buildAndCreateTopic(inputTopicList);
        final boolean result = dependentServiceAvailabilityKafka.checkService();
        assertTrue(result);
    }

    @SneakyThrows
    @Test
    @Order(4)
    public void test_method_does_input_topic_exist_positive() {
        final boolean result = dependentServiceAvailabilityKafka.isServiceAvailable();
        assertTrue(result);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD) //required: kafkaAdmin does not purge topics after each test
    @DisplayName("When subscription input topic exists, but input topic does not exist expect check to fail with subscriptions enabled")
    @Order(5)
    void test_subscription_input_topic_does_exist_input_topic_does_not_exist() {
        List<String> inputTopicList = new ArrayList<>(Collections.singletonList(subscriptionInputTopicName));
        buildAndCreateTopic(inputTopicList);
        final boolean result = dependentServiceAvailabilityKafka.checkService();
        assertEquals(false, result);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD) //required: kafkaAdmin does not purge topics after each test
    @DisplayName("When input topic exists but subscription input topic does not exist, expect check to fail with subscriptions enabled")
    @Order(6)
    void test_input_topic_exists_subscription_input_topic_does_not_exist_subscriptions_enabled() {
        List<String> inputTopicList = new ArrayList<>(Collections.singletonList(inputTopicName));
        buildAndCreateTopic(inputTopicList);
        final boolean result = dependentServiceAvailabilityKafka.checkService();
        assertEquals(false, result);
    }

    @SneakyThrows
    @Test
    @Order(7)
    public void test_method_does_input_topic_exist_negative_execution_exception() {
        DependentServiceAvailabilityKafka spy = Mockito.spy(dependentServiceAvailabilityKafka);
        AdminClient mock = Mockito.mock(AdminClient.class, Mockito.RETURNS_DEEP_STUBS);
        when(spy.getAdminClient()).thenReturn(mock);
        when(mock.listTopics().names().get()).thenThrow(ExecutionException.class);
        assertThrows(UnsatisfiedExternalDependencyException.class, spy::isServiceAvailable);
    }

    @SneakyThrows
    @Test
    @Order(8)
    public void test_method_does_input_topic_exist_negative_interrupted_exception() {
        DependentServiceAvailabilityKafka spy = Mockito.spy(dependentServiceAvailabilityKafka);
        AdminClient mock = Mockito.mock(AdminClient.class, Mockito.RETURNS_DEEP_STUBS);
        when(spy.getAdminClient()).thenReturn(mock);
        when(mock.listTopics().names().get()).thenThrow(InterruptedException.class);
        assertThrows(UnsatisfiedExternalDependencyException.class, spy::isServiceAvailable);
    }

    private void buildAndCreateTopic(List<String> inputTopicList) {
        for (String inputTopic: inputTopicList) {
            NewTopic outputTopic = TopicBuilder.name(inputTopic)
                    .partitions(1)
                    .replicas(1)
                    .build();
            kafkaAdmin.createOrModifyTopics(outputTopic);
        }
    }
}