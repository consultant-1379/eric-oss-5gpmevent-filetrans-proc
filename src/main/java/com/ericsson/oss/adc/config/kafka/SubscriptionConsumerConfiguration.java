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

package com.ericsson.oss.adc.config.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class SubscriptionConsumerConfiguration {

    public static final String SUBSCRIPTION_CONSUMER_CONFIG = "subscriptionConsumerConfig";

    @Value("${spring.kafka.subscriptionConsumer.group-id}")
    private String groupIdSubscriptionConsumerConfig;

    @Value("${spring.kafka.subscriptionConsumer.auto.offset.reset}")
    private String autoOffsetReset;

    @Value("${spring.kafka.topics.subscriptionInput.session.timeout.ms}")
    private int sessionTimeoutMs;

    @Value("${spring.kafka.topics.subscriptionInput.partition.assignment.strategy}")
    private String partitionAssignmentStrategy;

    @Value("${spring.kafka.subscriptionConsumer.retry.backoff.ms}")
    private int subscriptionConsumerRetryBackoffMs;

    @Value("${spring.kafka.subscriptionConsumer.reconnect.backoff.ms}")
    private int subscriptionConsumerReconnectBackoffMs;

    @Value("${spring.kafka.subscriptionConsumer.reconnect.backoff.max.ms}")
    private int subscriptionConsumerReconnectBackoffMaxMs;

    @Value("${spring.kafka.subscriptionConsumer.request.timeout.ms}")
    private int subscriptionConsumerRequestTimeoutMs;


    @Bean
    public Map<String, Object> subscriptionConsumerConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupIdSubscriptionConsumerConfig);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        config.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, partitionAssignmentStrategy);
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        addSubscriptionConsumerRobustnessValues(config);

        return config;
    }

    /**
     * Robustness/Resiliency values for subscription consumer read from configmap/values file.
     *
     * @param config Kafka Config {@link Map}
     */

    private void addSubscriptionConsumerRobustnessValues(final Map<String, Object> config) {
        config.put(ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, subscriptionConsumerRetryBackoffMs);
        config.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, subscriptionConsumerReconnectBackoffMs);
        config.put(ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, subscriptionConsumerReconnectBackoffMaxMs);
        config.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, subscriptionConsumerRequestTimeoutMs);
    }
}
