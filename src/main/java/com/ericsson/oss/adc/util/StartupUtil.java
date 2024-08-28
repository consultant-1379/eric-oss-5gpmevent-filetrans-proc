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

package com.ericsson.oss.adc.util;

import com.ericsson.oss.adc.availability.UnsatisfiedExternalDependencyException;
import com.ericsson.oss.adc.models.*;
import com.ericsson.oss.adc.models.data.catalog.v2.*;
import com.ericsson.oss.adc.service.data.catalog.DataCatalogService;
import com.ericsson.oss.adc.service.data.catalog.DataCatalogServiceV2;
import com.ericsson.oss.adc.service.output.topic.OutputTopicService;
import com.ericsson.oss.adc.service.subscription.filter.SubscriptionCacheUtil;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.CircuitBreakerRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.ericsson.oss.adc.models.DataCatalogConstants.*;

@Component
@Slf4j
public class StartupUtil {

    @Autowired
    private DataCatalogService dataCatalogService;

    @Autowired
    private DataCatalogServiceV2 dataCatalogServiceV2;

    @Autowired
    private DataCatalogProperties dataCatalogProperties;

    @Autowired
    private OutputTopicService outputTopicService;

    @Autowired
    private SubscriptionCacheUtil subscriptionCacheUtil;

    @Value("${dmm.data-catalog.availability.retry-interval}")
    private int retryInterval;

    @Value("${dmm.data-catalog.availability.retry-attempts}")
    private int retryAttempts;

    @Value("${spring.kafka.topics.input.name}")
    private String inputTopicName;

    @Value("${eventRegulation.produceNonStandard}")
    private boolean produceNonStandard;

    private static final String INPUT_TOPIC_MESSAGE_BUS_UNAVAILABLE = "No Input Topic Message Bus Name Available From Data Catalog";
    private static final String INPUT_TOPIC_MESSAGE_BUS_STATUS_TOPIC_NAME_DATA_SPACE_UNAVAILABLE = "No Input Topic Message Bus, Message Status Topic Name Or Data Space Available From Data Catalog";
    private MessageBus messageBus;

    /**
     * Check if we got an OK Response code.
     *
     * @param response The response to check the code for.
     * @return True if valid response code found.
     * @throws UnsatisfiedExternalDependencyException If not OK response code found.
     */
    public static boolean isOkResponseCode(final ResponseEntity<String> response) throws UnsatisfiedExternalDependencyException {
        if (response.getStatusCode().value() == Response.Status.OK.getStatusCode()) {
            return true;
        }
        throw new UnsatisfiedExternalDependencyException("OK status code not found, found: " + response.getStatusCode().value());
    }

    /**
     * Take an {@link java.net.URI} and if it doesn't end with a slash add one.
     *
     * @param uri The {@link java.net.URI} to check.
     * @return The {@link String} with a guaranteed slash appended.
     */
    public static String addTrailingSlash(String uri) {
        if (!uri.endsWith("/")) { // if not ending / append one
            uri = uri + "/";
        }
        return uri;
    }

    /**
     * Simpled retry template that use application variables set at deploy time for the number of
     * retry attempt and interval in mS.
     *
     * @returns false if all retries are exhausted
     */
    public RetryTemplate retryTemplate() {

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(retryAttempts);

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(retryInterval);

        RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(retryPolicy);
        template.setBackOffPolicy(backOffPolicy);

        return template;
    }

    /**
     * A simple generic {@link RetryListener} that logs the number of retry attempts.
     *
     * @return A @{@link RetryListener} that can be used in a {@link org.springframework.retry.support.RetryTemplate}.
     */
    public static RetryListener getRetryListener() {
        return new RetryListener() {
            @Override
            public <T, E extends Throwable> boolean open(final RetryContext retryContext, final RetryCallback<T, E> retryCallback) {
                return true; // called before first retry
            }

            @Override
            public <T, E extends Throwable> void close(final RetryContext retryContext, final RetryCallback<T, E> retryCallback, final Throwable throwable) {
                log.error("Retry closed after {} attempts", retryContext.getRetryCount(), throwable);
            }

            @Override
            public <T, E extends Throwable> void onError(final RetryContext retryContext, final RetryCallback<T, E> retryCallback, final Throwable throwable) {
                log.error("Retry Triggered {}", retryContext.getRetryCount(), throwable);
            }
        };
    }

    /**
     * For Max attempts, try to execute request within open timeout. Repeat on interval until successful.
     *
     * @param circuitBreakerRetryAttempts Retry attempts before opening the circuit.
     * @param circuitBreakerResetTimeOut  After circuit opens, it will re-close after this time, need to re-close circuit before next attempt to
     *                                    reset the circuit.
     * @param circuitBreakerOpenTimeout   If delegate policy cannot retry and this timeout has not elapsed, the circuit is opened, and we exit the
     *                                    retry.
     * @param circuitBreakerBackoff       Back off for this amount of time before retrying between attempts.
     * @return RetryTemplate with the desired configuration.
     */
    public static RetryTemplate buildCircuitBreakerRetry(final int circuitBreakerRetryAttempts,
                                                         final long circuitBreakerResetTimeOut,
                                                         final long circuitBreakerOpenTimeout,
                                                         final long circuitBreakerBackoff) {
        RetryTemplate retryTemplate = new RetryTemplate();
        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
        // Max number of times to retry before opening the circuit and exiting retry
        // Note: If circuit closed after tripping, this count is reset (desired scenario)
        simpleRetryPolicy.setMaxAttempts(circuitBreakerRetryAttempts);

        CircuitBreakerRetryPolicy circuitBreakerRetryPolicy = new CircuitBreakerRetryPolicy(simpleRetryPolicy);

        // After circuit opens, it will re-close after this time, need to re-close circuit before next attempt to reset the circuit (retry count)
        circuitBreakerRetryPolicy.setResetTimeout(circuitBreakerResetTimeOut);

        // if delegate policy cannot retry and this timeout has not elapsed.
        // The circuit is opened, and we exit the retry (undesirable scenario for startup)
        circuitBreakerRetryPolicy.setOpenTimeout(circuitBreakerOpenTimeout);

        retryTemplate.setRetryPolicy(circuitBreakerRetryPolicy);

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        // Back off for this amount of time before retrying between attempts
        fixedBackOffPolicy.setBackOffPeriod(circuitBreakerBackoff);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy); // number of seconds to wait between retries

        retryTemplate.registerListener(new RetryListener() {
            @Override
            public <T, E extends Throwable> boolean open(final RetryContext retryContext, final RetryCallback<T, E> retryCallback) {
                log.error("Circuit Breaker open with retry count {}, retry status {}", retryContext.getRetryCount(),
                        circuitBreakerRetryPolicy.canRetry(retryContext));

                return circuitBreakerRetryPolicy.canRetry(retryContext);
            }

            @Override
            public <T, E extends Throwable> void close(final RetryContext retryContext, final RetryCallback<T, E> retryCallback,
                                                       final Throwable throwable) {
                log.error("Circuit Breaker exit after {} attempts, retry status {}", retryContext.getRetryCount(),
                        circuitBreakerRetryPolicy.canRetry(retryContext), throwable);
            }

            @Override
            public <T, E extends Throwable> void onError(final RetryContext retryContext, final RetryCallback<T, E> retryCallback,
                                                         final Throwable throwable) {
                log.error("Circuit Breaker Retry Triggered {}, retry status {}", retryContext.getRetryCount(), circuitBreakerRetryPolicy.canRetry(retryContext),
                        throwable);
            }
        });

        return retryTemplate;
    }

    /**
     * Queries DataCatalog for Message Schema V2 relating to the InputTopic.
     * This query must succeed before the setting up of the Output Topic on both DataCatalog and KafkaMessageBus
     *
     * @return boolean representing if the Message Schema V2 was present or not
     */
    public boolean queryDataCatalogForInputTopic() {
        return retryTemplate().execute(context -> retrieveDataCatalogEntries());
    }

    private MessageSchemaPutRequest createMessageSchemaRequest(final String outputTopicName, final String specRef, final String schemaName) {
        //setting dataServiceInstance name to be dataServiceName--dataCollectorName for now. If a requirement comes in the future for 5g to have separate deployments this will need to change
        //If any of the data in the models change the message schema should be updated as long as dataServiceName + messageDataTopic.name + specificationReference are the same
        String dataServiceInstanceName = dataCatalogProperties.getDataServiceName() + "--" + dataCatalogProperties.getDataCollectorName();
        return MessageSchemaPutRequest
                .builder()
                .dataSpace(new DataSpace(dataCatalogProperties.getDataSpace()))
                .dataService(new DataServiceForMessageSchemaPut(dataCatalogProperties.getDataServiceName()))
                .dataServiceInstance(new DataServiceInstance(dataServiceInstanceName, dataCatalogProperties.getDataCollectorName()))
                .dataCategory(new DataCategory())
                .dataProviderType(new DataProviderTypeForMessageSchemaPUT())
                //messageStatusTopic not used, not deprecated
                .messageStatusTopic(new MessageStatusTopic(outputTopicName, messageBus.getId()))
                .messageDataTopic(new MessageDataTopic(outputTopicName, messageBus.getId()))
                .dataType(new DataType(schemaName))
                .supportedPredicateParameters(Collections.singleton(new SupportedPredicateParameter(NODE_NAME, true)))
                .supportedPredicateParameters(Collections.singleton(new SupportedPredicateParameter(EVENT_ID, false)))
                .messageSchema(new MessageSchema(specRef))
                .build();
    }

    /**
     * Utility function for the registration and retrieval of details from DataCatalog
     *
     * @return boolean if retrieval/registration was successful
     */
    public boolean registerInDataCatalog() {
        //TODO boolean not necessary. These retries are infinite. If the method returns without exception it succeeded
        try{
            retryTemplate().execute(context -> createTopicsOnKafka());
            retryTemplate().execute(context -> registerMessageSchema(outputTopicService.getStandardizedTopicName(), SPECIFICATION_REFERENCE_STANDARDIZED,
                    SCHEMA_NAME_STANDARDIZED));
            if (produceNonStandard) {
                retryTemplate().execute(context -> registerMessageSchema(outputTopicService.getNonStandardTopicName(),
                        SPECIFICATION_REFERENCE_NONSTANDARD, SCHEMA_NAME_NON_STANDARD));
            }
            log.info("Message Schema registered in Data Catalog");
        } catch (Exception e){
            //Revisit logging exceptions, retrytemplate/listener logs attempts, and given its infinite retries, we also should not be hitting this.
            log.error("Failed to register in Data Catalog: {}", e.getMessage());
            log.debug("Stack trace - Failed to register in Data Catalog:", e);
            return false;
        }
        return true;
    }

    private Boolean retrieveDataCatalogEntries() throws NullPointerException, NotFoundException {
        MessageSchemaV2 inputTopicMessageSchemaV2Response = getMessageSchemaBasedOnTopic(inputTopicName);
        if (inputTopicMessageSchemaV2Response.getMessageDataTopic().getMessageBus().getName() != null) {

            DataSpace dataSpace = inputTopicMessageSchemaV2Response.getMessageDataTopic().getDataProviderType().getDataSpace();
            messageBus = inputTopicMessageSchemaV2Response.getMessageDataTopic().getMessageBus();

            return (dataSpace.getName() != null && messageBus != null);
        }

        log.error("Input Topic Message Bus Not Available From Data Catalog");
        throw new NullPointerException(INPUT_TOPIC_MESSAGE_BUS_UNAVAILABLE);
    }

    private Boolean createTopicsOnKafka() throws UnsatisfiedExternalDependencyException {
        final boolean topicsCreated = outputTopicService.setupOutputTopics();
        if (topicsCreated) {

            //Can ditch this after flag is removed.
            String outputTopicNames = outputTopicService.getStandardizedTopicName();
            if (produceNonStandard) {
                outputTopicNames += ", " + outputTopicService.getNonStandardTopicName();
            }
            log.info("OutputTopic names set to: {}", outputTopicNames);
            return true;
        }
        throw new UnsatisfiedExternalDependencyException("Failed to create output topics on Kafka");
    }

    /**
     * Registers Message Schema in Data Catalog through REST calls
     * If response returns 409 (already exists) or 401 (failed to register) the DTO will be set by
     * retrieving the existing Message Schema in Data Catalog if available else null pointer exception is thrown
     *
     * @return Response Entity Contain the MessageSchema DTO
     */
    private MessageSchemaV2 registerMessageSchema(final String outputTopicName, final String specRef, final String schemaName) {
        log.debug("Register Message Schemas");

        MessageSchemaPutRequest messageSchemaRequest = createMessageSchemaRequest(outputTopicName, specRef, schemaName);

        ResponseEntity<MessageSchemaV2> messageSchemaResponseEntity = dataCatalogService.registerMessageSchema(messageSchemaRequest);
        if (messageSchemaResponseEntity.getStatusCode().is2xxSuccessful()) {
            return messageSchemaResponseEntity.getBody();
        }

        if (messageSchemaResponseEntity.getStatusCode().value() == 409) {
            log.info("Unable to register Message Schema as it already exists");
            log.info("The message schema request sent to Data Catalog : {}", messageSchemaRequest);
            //todo, https://httpwg.org/specs/rfc7231.html#status.409 Server should return enough context by best practices. Check if catalog fulfils this or if we are cutting info out
            log.info("Existing message Schema: {}", getMessageSchemaBasedOnTopic(outputTopicName));
            return messageSchemaResponseEntity.getBody();
        }
        //todo collapse 408 and other 400 client errors if no need. Can forward exception message from client
        if (messageSchemaResponseEntity.getStatusCode().value() == 408) {
            log.info("Unable to register Message Schema as another process is already attempting to register. We should retry");
            throw new RestClientException("Unable to register Message Schema. Error code from Data Catalog: " + messageSchemaResponseEntity.getStatusCode());
        }
        if (messageSchemaResponseEntity.getStatusCode().is4xxClientError()) {
            log.error("Unable to register Message Schema. Error code: {}", messageSchemaResponseEntity.getStatusCode());
            log.info("The message schema request sent to Data Catalog : {}", messageSchemaRequest);
            log.info("Existing message Schema: {}", getMessageSchemaBasedOnTopic(outputTopicName));
            throw new RestClientException("Unable to register Message Schema. Error code from Data Catalog: " + messageSchemaResponseEntity.getStatusCode());
        }
        log.error("Unable to register Message Schema, params not set correctly");
        throw new NullPointerException("Message Schema Params Not Set Correctly");
    }

    private MessageSchemaV2 getMessageSchemaBasedOnTopic(String topicName) {
        log.debug("Get Input Topic details in Message Schema V2 format");
        ResponseEntity<MessageSchemaListV2> responseEntity =
                dataCatalogServiceV2.getMessageSchemaListV2ByDataSpaceAndDataCategory(dataCatalogProperties.getDataSpace(), dataCatalogProperties.getDataCategory());
        MessageSchemaListV2 messageSchemaListV2 = responseEntity.getBody();
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            for (MessageSchemaV2 messageSchemaV2 : messageSchemaListV2) {
                if (MessageSchemaV2Utils.canGetTopicName(messageSchemaV2) &&
                        MessageSchemaV2Utils.canGetDataSpaceName(messageSchemaV2) &&
                        messageSchemaV2.getMessageDataTopic().getName().equals(topicName) &&
                        messageSchemaV2.getMessageDataTopic().getDataProviderType().getDataSpace().getName().equals(dataCatalogProperties.getDataSpace())) {
                    return messageSchemaV2;
                }
            }
        }
        log.error("Input topic not available yet");
        throw new NotFoundException(INPUT_TOPIC_MESSAGE_BUS_STATUS_TOPIC_NAME_DATA_SPACE_UNAVAILABLE);
    }

    public void handleSubscriptions() {
        Subscription[] activeSubscriptions = dataCatalogServiceV2.getSubscriptions();
        if (activeSubscriptions.length > 0) {
            List<Subscription> subscriptions = new ArrayList<>(List.of(activeSubscriptions));
            subscriptionCacheUtil.reconstituteSubscriptionCache(subscriptions);
            log.info("Subscription cache has been reconstituted");
        } else {
            subscriptionCacheUtil.reconstituteSubscriptionCache(Collections.emptyList());
            log.info("The call to the subscriptions end point returned an empty array and Subscription cache has been reconstituted with an empty list");
        }
    }
}