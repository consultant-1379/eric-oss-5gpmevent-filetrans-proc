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

package com.ericsson.oss.adc.kafka_components;

import com.ericsson.pm_event.PmEventOuterClass;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

@TestConfiguration
public class ConsumerTestConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServerConfig;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupIdConsumerConfig;

    @Value("${spring.kafka.topics.input.partition.assignment.strategy}")
    private String partitionAssignmentStrategy;

    @Value("${spring.kafka.topics.input.session.timeout.ms}")
    private int sessionTimeoutMs;

    @Value("${dmm.schema-registry.url}")
    private String schemaRegistryUrl;

    @Autowired
    private KafkaProtobufDeserializer<PmEventOuterClass.PmEvent> deserializer;


    public ConsumerFactory<String, PmEventOuterClass.PmEvent> consumerOutputTestFactory() {

        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServerConfig);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupIdConsumerConfig);
        config.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, partitionAssignmentStrategy);
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaProtobufDeserializer.class);
        config.put(KafkaProtobufDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        //Need to specify how specific schema to use. Parsing is still possible without but the event won't be available as a PmEvent, only a protobuf DynamicMessage.
        //I think this is a protobuf peculiarity, doesn't seem to be the case for avro
        // https://docs.confluent.io/platform/current/schema-registry/serdes-develop/serdes-protobuf.html
        config.put(KafkaProtobufDeserializerConfig.SPECIFIC_PROTOBUF_VALUE_TYPE, PmEventOuterClass.PmEvent.class.getName());
        //Both "enable auto commit config=false" and "isolation level config= read_committed"" need to be set for transactions to work downstream
        config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PmEventOuterClass.PmEvent> consumerKafkaListenerOutputTestContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PmEventOuterClass.PmEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerOutputTestFactory());
        return factory;
    }
}
