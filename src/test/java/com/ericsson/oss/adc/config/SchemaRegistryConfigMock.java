/*******************************************************************************
 * COPYRIGHT Ericsson 2024
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

package com.ericsson.oss.adc.config;

import com.ericsson.pm_event.PmEventOuterClass;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@Profile("E2E")
@TestConfiguration
public class SchemaRegistryConfigMock {

    @Bean
    public SchemaRegistryClient getSchemaRegistryClient() {
        return new MockSchemaRegistryClient();
    }

    @Bean
    public KafkaProtobufSerializer<PmEventOuterClass.PmEvent> getSerializer() {
        return new KafkaProtobufSerializer<>(getSchemaRegistryClient());
    }

    /**
     * Strictly for the Test Consumer, shares the same underlying schema registry client so schemas are accessible to
     * our producer and the "Rapp" consumer
     */
    @Bean
    public KafkaProtobufDeserializer<PmEventOuterClass.PmEvent> getDeserializer() {
        return new KafkaProtobufDeserializer<>(getSchemaRegistryClient());
    }
}
