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
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Class Responsible for registering PmEvent Protobuf schema on startup of the service.
 * Will override Compatibility level of Schema Subject in order to not block any new versions introduced
 * by Ericsson RAN
 */
@Slf4j
@Service
public class SchemaRegistryService {

    private final SchemaRegistryClient schemaRegistryClient;

    private final KafkaProtobufSerializer<PmEventOuterClass.PmEvent> serializer;

    private final RetryTemplate retryTemplate;


    public SchemaRegistryService(SchemaRegistryClient schemaRegistryClient, KafkaProtobufSerializer<PmEventOuterClass.PmEvent> serializer,
                                 RetryTemplate retryTemplate) {
        this.schemaRegistryClient = schemaRegistryClient;
        this.serializer = serializer;
        this.retryTemplate = retryTemplate;
    }

    /**
     * Registers the top level PmEvent schema, retrying infinitely in the case of Schema Registry
     * not being available, or temporary service outage.
     * @throws UnsatisfiedExternalDependencyException in the case of the Schema Registry not being available.
     */
    public void registerSchemasWithRetry() throws UnsatisfiedExternalDependencyException {
        String subjectName = PmEventOuterClass.PmEvent.getDescriptor().getFullName();
        ProtobufSchema pmEventSchema = new ProtobufSchema(PmEventOuterClass.PmEvent.getDescriptor());

        retryTemplate.execute(context -> {
            registerSchemas(subjectName, pmEventSchema);
            return true;
        });
    }

    /**
     * Attempts to set the compatibility level of already existing Schemas to NONE, then register new Schemas.
     * This is to cover upgrade scenarios when 5G takes in new versions of the RAN PmEvent packages.
     * @param subjectName The name under which the Schema is to be registered
     * @param pmEventSchema The OuterPmEventWrapper class that is used for both Proprietary and Standard Topics
     * @throws UnsatisfiedExternalDependencyException in case of Schema Registry outage or temporary errors.
     */
    protected void registerSchemas(String subjectName, ProtobufSchema pmEventSchema) throws UnsatisfiedExternalDependencyException {
        log.info("Checking if schema already exists before registering");
        setCompatibility(subjectName);
        try {
            int schemaVersion = serializer.register(subjectName, pmEventSchema);
            log.info("Schema {} registered with schema ID {}", subjectName, schemaVersion);
        } catch (IOException | RestClientException e) {
            throw new UnsatisfiedExternalDependencyException("Issue registering schema to Schema registry", e);
        }
    }

    /**
     * Change the Subject compatibilty to None for already Existing schemas.
     * Retries on any exception apart from "40401" schema/subject not found, which occurs when this is the first
     * schema to be registered belonging to the 5G topics.
     * @param subjectName The name under which the Schema is registered
     * @throws UnsatisfiedExternalDependencyException thrown when any other issue other than status "40401" occurs.
     */
    protected void setCompatibility(String subjectName) throws UnsatisfiedExternalDependencyException {
        int schemaNotFoundErrorCode = 40401;
        String errorMessage = "Issue setting compatibility of schema";

        try {
            String updatedCompatibility = schemaRegistryClient.updateCompatibility(subjectName, CompatibilityLevel.NONE.name());
            log.info("Compatibility set to {} for subject {}", updatedCompatibility, subjectName);
        } catch (RestClientException e) {
            if (e.getErrorCode() == schemaNotFoundErrorCode) {
                log.info("Schema subject {} not found, registering now", subjectName);
                return;
            }
            throw new UnsatisfiedExternalDependencyException(errorMessage, e);
        } catch (IOException e) {
            throw new UnsatisfiedExternalDependencyException(errorMessage, e);
        }
    }
}
