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

package com.ericsson.oss.adc.controller.subscription.listener;

import com.ericsson.oss.adc.models.DataCatalogProperties;
import com.ericsson.oss.adc.models.adapters.SubDataJobAdapter;
import com.ericsson.oss.adc.models.data.catalog.r1.DataJobNotification;
import com.ericsson.oss.adc.models.data.catalog.r1.DataJobSummary;
import com.ericsson.oss.adc.service.subscription.filter.SubscriptionCache;
import com.ericsson.oss.adc.util.StartupUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Implementation for listening to dcc notification topic and R1 Compliant DataJob Topic to consume subscription events
 */
@Slf4j
@Component
public class SubscriptionInputTopicListenerImpl {

    /**
     * Logger for the class.
     */
    private static final String SUBSCRIPTION_LISTENER_ID = "subscriptionTopic5gEventKafkaListener";
    private static final String DATA_JOB_LISTENER_ID = "dataJobTopic5gEventKafkaListener";

    private final StartupUtil startupUtil;

    private final DataCatalogProperties dataCatalogProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SubDataJobAdapter subDataJobAdapter;
    private final SubscriptionCache subscriptionCache;

    public SubscriptionInputTopicListenerImpl(
            StartupUtil startupUtil,
            DataCatalogProperties dataCatalogProperties,
            SubDataJobAdapter subDataJobAdapter,
            SubscriptionCache subscriptionCache) {
        this.startupUtil = startupUtil;
        this.dataCatalogProperties = dataCatalogProperties;
        this.subDataJobAdapter = subDataJobAdapter;
        this.subscriptionCache = subscriptionCache;
    }

    /**
     * Listener for the shared SubscriptionNotification topic.
     *
     * @param consumerRecord A <{@link ConsumerRecord}> holding the {@link String}.
     * @throws NullPointerException Should only occur if header data is missing.
     */
    @KafkaListener(
            id = SUBSCRIPTION_LISTENER_ID,
            idIsGroup = false,
            containerFactory = "subscriptionConsumerKafkaListenerContainerFactory",
            topics = "${spring.kafka.topics.subscriptionInput.name}",
            autoStartup = "false")
    public void listen(final ConsumerRecord<String, String> consumerRecord,
                       @Header(name = "serviceInstanceName") String serviceInstanceName,
                       @Header(name = "serviceName") String serviceName) throws NullPointerException {

        String dataServiceInstanceName = dataCatalogProperties.getDataServiceName() + "--" + dataCatalogProperties.getDataCollectorName();
        if (serviceName.equals(dataCatalogProperties.getDataServiceName()) && serviceInstanceName.equals(dataServiceInstanceName)) {
            log.info("Subscription received for service name '{}' and data service instance name '{}'", serviceName, dataServiceInstanceName);
            startupUtil.handleSubscriptions();
        }
    }

    /**
     * Listener for dedicated DataJob topic for 5G PmEvent Service. Updates the service on any DataJob based events.
     * @param consumerRecord Contains the String format of the DataJobEvent
     */
    //TODO enable after DC deliver Topic creation functionality. Topic is only created after 5G registers R1 complaint types in catalog.
    @KafkaListener(
            id = DATA_JOB_LISTENER_ID,
            idIsGroup = false,
            containerFactory = "subscriptionConsumerKafkaListenerContainerFactory",
            topics = "${spring.kafka.topics.dataJobTopic.name}",
            autoStartup = "false")
    public void listenDataJob(final ConsumerRecord<String, String> consumerRecord) {
        String dataJobString = consumerRecord.value();

        try {
            DataJobNotification dataJobNotification = objectMapper.readValue(dataJobString, DataJobNotification.class);
            log.debug("DataJobEvent received: {}", dataJobNotification);

            DataJobSummary dataJobSummary = subDataJobAdapter.convertDataJob(dataJobNotification.getDataJobEvent().getDataJob());
            log.debug("DataJobSummary created: {}", dataJobSummary);

            switch (dataJobNotification.getEventType()) {
                case DATA_JOB_CREATED -> subscriptionCache.addDataJobSummary(dataJobSummary);
                case DATA_JOB_UPDATED -> subscriptionCache.updateDataJobSummary(dataJobSummary);
                case DATA_JOB_DELETED -> subscriptionCache.removeDataJobSummary(dataJobSummary);
            }

        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.error("Could not deserialize DataJobEvent, data job will be ignored ", e);
        }
    }
}
