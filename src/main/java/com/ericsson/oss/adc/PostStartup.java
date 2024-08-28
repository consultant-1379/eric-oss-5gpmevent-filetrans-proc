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

package com.ericsson.oss.adc;

import com.ericsson.oss.adc.availability.*;
import com.ericsson.oss.adc.service.schema.registry.SchemaRegistryService;
import com.ericsson.oss.adc.util.StartupUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class PostStartup {

    private static final Logger LOG = LoggerFactory.getLogger(PostStartup.class);
    private static final String LISTENER_ID = "inputTopic5gEventKafkaListener";
    private static final String SUBSCRIPTION_LISTENER_ID = "subscriptionTopic5gEventKafkaListener";

    @Autowired
    private StartupUtil startupUtil;

    @Autowired
    private DependentServiceAvailabilityKafka dependentServiceAvailabilityKafka;

    @Autowired
    private DependentServiceAvailabilityDataCatalog dependentServiceAvailabilityDataCatalog;

    @Autowired
    private DependentServiceAvailabilityConnectedSystems dependentServiceAvailabilityConnectedSystems;

    @Autowired
    private DependentServiceAvailabilityScriptingVm dependentServiceAvailabilityScriptingVm;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Autowired
    private SchemaRegistryService schemaRegistryService;

    /**
     * Async Listener for when the spring boot application is started and ready. Needs to be Async so that liveliness and readiness probes are
     * unaffected by retries.
     *
     * @throws UnsatisfiedExternalDependencyException This exception cannot be thrown as it is in effectively an infinite retry.
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void afterStartUp() throws UnsatisfiedExternalDependencyException {
        //Query if DataCatalog is available through health check, then ensure relevant info is available.
        if (dependentServiceAvailabilityDataCatalog.checkServiceWithCircuitBreaker() && startupUtil.queryDataCatalogForInputTopic()) {
            LOG.info("Input topic details received from data catalog ");
        }

        if (dependentServiceAvailabilityKafka.checkService()
                && dependentServiceAvailabilityConnectedSystems.checkServiceWithCircuitBreaker()
                && dependentServiceAvailabilityScriptingVm.checkServiceWithCircuitBreaker()) {
            schemaRegistryService.registerSchemasWithRetry();
            startupUtil.registerInDataCatalog();
            startKafkaListener(SUBSCRIPTION_LISTENER_ID);
            startupUtil.handleSubscriptions();
            startKafkaListener(LISTENER_ID);
        }
    }

    private void startKafkaListener(final String listenerId) {
        MessageListenerContainer messageListenerContainer = Objects.requireNonNull(registry.getListenerContainer(listenerId));

        if (!messageListenerContainer.isAutoStartup() && !messageListenerContainer.isRunning()) {
            messageListenerContainer.start();
            LOG.info("Kafka Listener with Id: {} started", listenerId);
        }
    }
}
