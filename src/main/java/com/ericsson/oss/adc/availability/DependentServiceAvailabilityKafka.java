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

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Component
public class DependentServiceAvailabilityKafka extends DependentServiceAvailability {

    private static final Logger LOG = LoggerFactory.getLogger(DependentServiceAvailabilityKafka.class);

    @Value("${spring.kafka.topics.input.name}")
    private String inputTopicName;

    @Value("${spring.kafka.topics.subscriptionInput.name}")
    private String subscriptionInputTopicName;

    @Autowired
    private KafkaAdmin kafkaAdmin;

    protected AdminClient getAdminClient() {
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }

    @Override
    boolean isServiceAvailable() throws UnsatisfiedExternalDependencyException {

        Map<String, Object> kafkaMap = kafkaAdmin.getConfigurationProperties();
        String bootstrapServerUrl = (String) kafkaMap.get(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG);

        if (bootstrapServerUrl == null || bootstrapServerUrl.equals("")) {
            throw new UnsatisfiedExternalDependencyException("No bootstrap server found");
        }

        try (AdminClient client = getAdminClient()) {

            LOG.info("Checking if Input Topic '{}' and Subscription Input Topic '{}' exists, GET request on: '{}'",
                    inputTopicName, subscriptionInputTopicName, bootstrapServerUrl);
            boolean inputTopicExists = doesInputTopicExist(client, inputTopicName);
            boolean subscriptionInputTopicExists = doesInputTopicExist(client, subscriptionInputTopicName);
            if (!subscriptionInputTopicExists && !inputTopicExists) {
                throw new UnsatisfiedExternalDependencyException("Subscription topic: " + subscriptionInputTopicName + " and Input Topic: " + inputTopicName + " not found");
            }
            if (!subscriptionInputTopicExists) {
                throw new UnsatisfiedExternalDependencyException("Subscription topic not found: " + subscriptionInputTopicName);
            }
            if (!inputTopicExists) {
                throw new UnsatisfiedExternalDependencyException("Input topic not found: " + inputTopicName);
            }
            return true;

        } catch (ExecutionException e) {
            LOG.error("Threading Error Reaching Kafka: {}", e.getMessage());
            LOG.debug("Stack trace - Threading Error Reaching Kafka ", e);
            throw new UnsatisfiedExternalDependencyException("Threading Error", e);
        } catch (InterruptedException e) {
            LOG.error("Threading Interrupted Error Reaching Kafka: {}", e.getMessage());
            LOG.debug("Stack trace - Threading Interrupted Error Reaching Kafka ", e);
            Thread.currentThread().interrupt();
            throw new UnsatisfiedExternalDependencyException("Threading Interrupted Error", e);
        } catch (Exception e) {
            LOG.error("Error Reaching Kafka: {}", e.getMessage());
            LOG.debug("Stack trace - Error Reaching Kafka ", e);
            throw new UnsatisfiedExternalDependencyException("Kafka Unreachable", e);
        }
    }

    private boolean doesInputTopicExist(AdminClient client, String topicName) throws InterruptedException, ExecutionException {
        Set<String> existingTopics = client.listTopics().names().get();

        if (existingTopics.contains(topicName)) {
            LOG.info("Kafka reachable on input topic {}", topicName);
            return true;
        }
        return false;
    }
}
