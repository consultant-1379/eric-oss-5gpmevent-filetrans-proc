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

package com.ericsson.oss.adc.service.schema.registry;

import com.ericsson.oss.adc.availability.UnsatisfiedExternalDependencyException;
import com.ericsson.pm_event.PmEventOuterClass;
import io.confluent.kafka.schemaregistry.CompatibilityLevel;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.retry.support.RetryTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Objects;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@Slf4j
class SchemaRegistryServiceTest {

    private SchemaRegistryClient schemaRegistryClient;
    private KafkaProtobufSerializer<PmEventOuterClass.PmEvent> serializer;

    private final RetryTemplate retryTemplate = RetryTemplate.builder().maxAttempts(3).fixedBackoff(Duration.ofMillis(10)).build();
    private SchemaRegistryService schemaRegistryService;

    @BeforeEach
    public void init() {
        schemaRegistryClient = new MockSchemaRegistryClient();
        serializer = new KafkaProtobufSerializer<>(schemaRegistryClient);
        schemaRegistryService = new SchemaRegistryService(schemaRegistryClient, serializer, retryTemplate);

    }

    @SneakyThrows
    @Test
    @DisplayName("No exception thrown when a subject is 404 when trying to update Compatibility")
    void test_setCompatibilityDoesntThrow409() {
        //Explicit mock to allow stubbing for this test
        schemaRegistryClient = mock(SchemaRegistryClient.class);
        schemaRegistryService = new SchemaRegistryService(schemaRegistryClient, serializer, retryTemplate);

        doThrow(new RestClientException("Dont exist", 40401, 40401)).when(schemaRegistryClient).updateCompatibility(anyString(), anyString());
        String nonExistingSchema = "I_dont_exist";
        assertDoesNotThrow(() -> schemaRegistryService.setCompatibility(nonExistingSchema));
    }

    @SneakyThrows
    @Test
    @DisplayName("Throws UnsatisfiedDependencyException when trying to set compatibility and Rest/IO exception triggered")
    void test_setCompatibilityThrows() {
        //Explicit mock to allow stubbing for this test
        schemaRegistryClient = mock(SchemaRegistryClient.class);
        serializer = mock(KafkaProtobufSerializer.class);
        schemaRegistryService = new SchemaRegistryService(schemaRegistryClient, serializer, retryTemplate);

        //Schema Registry specific RestClientException, not spring
        doThrow(RestClientException.class, IOException.class)
                .when(schemaRegistryClient).updateCompatibility(anyString(), anyString());

        doThrow(IOException.class)
                .when(serializer).register(anyString(), any(ProtobufSchema.class));

        assertThrows(UnsatisfiedExternalDependencyException.class, () -> schemaRegistryService.registerSchemasWithRetry());
        assertThrows(UnsatisfiedExternalDependencyException.class, () -> schemaRegistryService.registerSchemasWithRetry());
        assertThrows(UnsatisfiedExternalDependencyException.class, () -> schemaRegistryService.registerSchemasWithRetry());
    }

    @SneakyThrows
    @Test
    @DisplayName("Given a blank Schema Registry, test initial registration is ok")
    void test_initialRegister() {
        //todo get string version of protobuf
        assertDoesNotThrow(() -> schemaRegistryService.registerSchemasWithRetry());
        String subject = PmEventOuterClass.PmEvent.getDescriptor().getFullName();
        SchemaMetadata metadata = schemaRegistryClient.getLatestSchemaMetadata(subject);

        assertEquals(1, metadata.getVersion(), "Should be only version");
        //compatibility would at this stage be Backward, but the Mock returns None. Will set explicity in next tests.
    }

    @SneakyThrows
    @Test
    @DisplayName("Ensure that a new schema with NBC can be registered even if old Schema was set to Backwards Compatible")
    void test_registerOldThenNewNBC() {
        //Register "old schema"
        String subject = PmEventOuterClass.PmEvent.getDescriptor().getFullName();
        try (InputStream stream = (getClass().getClassLoader().getResourceAsStream("test-schemas/pm_event_old.proto"))) {
            String oldSchemaString = new String(Objects.requireNonNull(stream).readAllBytes());
            ProtobufSchema oldSchema = new ProtobufSchema(oldSchemaString);
            assertDoesNotThrow(() -> schemaRegistryService.registerSchemas(subject, oldSchema));
        }


        //set compatability to backward (because of mock schema client)
        String changedCompatibility = schemaRegistryClient.updateCompatibility(subject, CompatibilityLevel.BACKWARD.name());
        assertEquals(CompatibilityLevel.BACKWARD.name(), changedCompatibility);

        //Register new schema under same subject, this has two fields removed out of the oneOf CommonEvent and their tags are marked as reserved
        try (InputStream stream = (getClass().getClassLoader().getResourceAsStream("test-schemas/pm_event_new_nbc.proto"))) {
            String newSchemaNBCString = new String(Objects.requireNonNull(stream).readAllBytes());
            ProtobufSchema newSchemaNBC = new ProtobufSchema(newSchemaNBCString);
            assertDoesNotThrow(() -> schemaRegistryService.registerSchemas(subject, newSchemaNBC));

        }

        String compatability = schemaRegistryClient.getCompatibility(subject);
        assertEquals(CompatibilityLevel.NONE.name(), compatability);

        int expectedVersion = 2;
        SchemaMetadata metadata = schemaRegistryClient.getLatestSchemaMetadata(subject);
        assertEquals(expectedVersion, metadata.getVersion());
    }
}