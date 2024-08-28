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

import com.ericsson.oss.adc.models.DecodedEvent;
import com.ericsson.pm_event.PmEventOuterClass;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static com.ericsson.oss.adc.enums.CustomKafkaHeaders.EVENT_ID;

/**
 * Implementation for creating the Kafka output topic
 */
@Component
@Slf4j
public class OutputTopicService {

    @Autowired
    private KafkaTemplate<String, PmEventOuterClass.PmEvent> kafkaOutputTemplate;

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Getter
    @Value("${spring.kafka.topics.output.prefix}standardized")
    private String standardizedTopicName;

    @Getter
    @Value("${spring.kafka.topics.output.prefix}ericsson")
    private String nonStandardTopicName;

    @Value("${spring.kafka.topics.output.partitions}")
    private int partitions;

    @Value("${spring.kafka.topics.output.replicas}")
    private short replicas;

    @Value("${spring.kafka.topics.output.retentionPeriodMS}")
    private String retentionPeriodMS;

    @Value("${spring.kafka.topics.output.retentionBytesTopic}")
    private String retentionBytesPerTopic;

    @Value("${event-regulation.producenonstandard}")
    private boolean produceNonStandard;

    private static final String COMPRESSION_TYPE = "producer";

    /**
     * Construct the output topic names and create the topics in Kafka
     */
    public boolean setupOutputTopics() {
        log.info("Creating the output topic");
        buildAndCreateTopic(standardizedTopicName);
        if (produceNonStandard) {
            buildAndCreateTopic(nonStandardTopicName);
            return isTopicCreated(standardizedTopicName) && isTopicCreated(nonStandardTopicName);
        }
        return isTopicCreated(standardizedTopicName);
    }

    /**
     * Build the output topic and create it on kafka server
     */
    protected void buildAndCreateTopic(String outputTopicName) {
        final String SEGMENT_MS = "300000";
        BigInteger minimumRetentionBytes = BigInteger.valueOf(1073741824); // 1GiB minimum retention per partition
        BigInteger bytesPerPartition = new BigInteger(retentionBytesPerTopic).divide(BigInteger.valueOf((long) partitions * replicas));
        bytesPerPartition = bytesPerPartition.max(minimumRetentionBytes);

        log.info("Creating or attempting to modify " +
                        "topic: '{}', partitions: '{}', replicas: '{}', " +
                        "compressionType: '{}', retentionPeriodMs: '{}, retentionBytesPerPartition: {}",
                outputTopicName, partitions, replicas, COMPRESSION_TYPE, retentionPeriodMS, bytesPerPartition);

        NewTopic outputTopic = TopicBuilder.name(outputTopicName)
                .partitions(partitions)
                .replicas(replicas)
                .config(TopicConfig.COMPRESSION_TYPE_CONFIG, COMPRESSION_TYPE)
                .config(TopicConfig.RETENTION_MS_CONFIG, retentionPeriodMS)
                .config(TopicConfig.RETENTION_BYTES_CONFIG, bytesPerPartition.toString())
                .config(TopicConfig.SEGMENT_MS_CONFIG, SEGMENT_MS)
                .build();

        // NOTE: this only creates a topic or changes the partition count
        kafkaAdmin.setModifyTopicConfigs(true);
        kafkaAdmin.createOrModifyTopics(outputTopic);
    }

    /**
     * Does the output topic exist in Kafka
     *
     * @param outputTopicName string of topic name to check if it has been created on kafka server
     * @return boolean to indicate the creation of topic
     */
    protected boolean isTopicCreated(final String outputTopicName) {
        try (AdminClient client = getAdminClient()) {
            Set<String> existingTopics = client.listTopics().names().get();
            log.info("Topics: {}", existingTopics);
            if (!existingTopics.contains(outputTopicName)) {
                log.debug("Topic was not created: {}", outputTopicName);
                return false;
            }
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error checking topic creation: {}", e.getMessage());
            log.debug("Stack trace - Error checking topic creation: ", e);
            Thread.currentThread().interrupt();
            return false;
        }
        return true;
    }

    /**
     * Creates and returns an {@link AdminClient} instance with bootstrap server configured.
     *
     * @return {@link AdminClient} instance
     */
    protected AdminClient getAdminClient() {
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }

    /**
     * Publish the record to Kafka output topic
     *
     * @param decodedEvent the decoded event to write to kafka topic
     * @param nodeName     the name of the node
     */
    public void sendKafkaMessage(final DecodedEvent decodedEvent, final String nodeName) {
        String topicName = switch (decodedEvent.getEventType()) {
            case STANDARDIZED -> standardizedTopicName;
            case NON_STANDARD -> nonStandardTopicName;
            default -> "";
        };

        if (!topicName.isEmpty()) {
            ProducerRecord<String, PmEventOuterClass.PmEvent> record = new ProducerRecord<>(topicName, nodeName, decodedEvent.getPmEvent());
            record.headers().add(EVENT_ID, (decodedEvent.getEventID()).getBytes(StandardCharsets.UTF_8));

            log.debug("Sending message={} to topic={}", record, topicName);
            kafkaOutputTemplate.send(record);
        }
    }
}
