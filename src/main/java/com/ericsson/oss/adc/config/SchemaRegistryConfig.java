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
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes Singleton Protobuf Serializer and Schema Registry client to be used across the service.
 * Responsible for registering and serializing all PmEvent schemas registered to the SchemaRegistry
 */
@Configuration
public class SchemaRegistryConfig {

    private final String schemaRegistryUrl;

    public SchemaRegistryConfig(@Value("${dmm.schema-registry.url}") String schemaRegistryUrl) {
        this.schemaRegistryUrl = schemaRegistryUrl;
    }

    @Bean
    public SchemaRegistryClient getSchemaRegistryClient() {
        int cacheCapacity = 1000;
        return new CachedSchemaRegistryClient(schemaRegistryUrl, cacheCapacity);
    }

    @Bean
    public KafkaProtobufSerializer<PmEventOuterClass.PmEvent> getSerializer(SchemaRegistryClient schemaRegistryClient) {
        return new KafkaProtobufSerializer<>(schemaRegistryClient);
    }

}
