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

import com.ericsson.oss.adc.availability.DependentServiceAvailabilityConnectedSystems;
import com.ericsson.oss.adc.availability.DependentServiceAvailabilityDataCatalog;
import com.ericsson.oss.adc.availability.DependentServiceAvailabilityKafka;
import com.ericsson.oss.adc.availability.DependentServiceAvailabilityScriptingVm;
import com.ericsson.oss.adc.config.kafka.BootStrapServerConfigurationSupplier;
import com.ericsson.oss.adc.service.schema.registry.SchemaRegistryService;
import com.ericsson.oss.adc.util.StartupUtil;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {PostStartup.class, DependentServiceAvailabilityDataCatalog.class,
        DependentServiceAvailabilityKafka.class, StartupUtil.class, DependentServiceAvailabilityConnectedSystems.class,
        KafkaListenerEndpointRegistry.class, BootStrapServerConfigurationSupplier.class})
class PostStartupTest {

    @Autowired
    private PostStartup postStartup;

    @MockBean
    private StartupUtil startupUtil;

    @MockBean
    private DependentServiceAvailabilityKafka dependentServiceAvailabilityKafka;

    @MockBean
    private DependentServiceAvailabilityDataCatalog dependentServiceAvailabilityDataCatalog;

    @MockBean
    private DependentServiceAvailabilityConnectedSystems dependentServiceAvailabilityConnectedSystems;

    @MockBean
    private DependentServiceAvailabilityScriptingVm dependentServiceAvailabilityScriptingVm;

    @MockBean
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @MockBean
    private SchemaRegistryService schemaRegistryService;

    @Mock
    private MessageListenerContainer messageListenerContainer;

    @SneakyThrows
    @Test
    @DisplayName("Should register data on DataCatalog and start listener when services are available")
    void test_allServicesAreAvailable() {
        when(dependentServiceAvailabilityScriptingVm.checkServiceWithCircuitBreaker()).thenReturn(true);
        when(dependentServiceAvailabilityDataCatalog.checkServiceWithCircuitBreaker()).thenReturn(true);
        when(startupUtil.queryDataCatalogForInputTopic()).thenReturn(true);
        when(dependentServiceAvailabilityKafka.checkService()).thenReturn(true);
        when(dependentServiceAvailabilityConnectedSystems.checkServiceWithCircuitBreaker()).thenReturn(true);
        doNothing().when(schemaRegistryService).registerSchemasWithRetry();
        when(kafkaListenerEndpointRegistry.getListenerContainer(any())).thenReturn(messageListenerContainer);
        when(messageListenerContainer.isAutoStartup()).thenReturn(false);
        when(messageListenerContainer.isRunning()).thenReturn(false);
        doNothing().when(messageListenerContainer).start();

        postStartup.afterStartUp();

        verify(startupUtil, times(1)).registerInDataCatalog();
        verify(messageListenerContainer, times(2)).start();
    }

    @SneakyThrows
    @Test
    @DisplayName("Should fail to register data on DataCatalog and start listener when services aren't available")
    void test_noServicesAreAvailable() {
        when(dependentServiceAvailabilityScriptingVm.checkServiceWithCircuitBreaker()).thenReturn(false);
        when(dependentServiceAvailabilityDataCatalog.checkServiceWithCircuitBreaker()).thenReturn(false);
        when(dependentServiceAvailabilityKafka.checkService()).thenReturn(false);
        when(dependentServiceAvailabilityConnectedSystems.checkServiceWithCircuitBreaker()).thenReturn(false);
        doNothing().when(schemaRegistryService).registerSchemasWithRetry();
        when(kafkaListenerEndpointRegistry.getListenerContainer(any())).thenReturn(messageListenerContainer);
        when(messageListenerContainer.isAutoStartup()).thenReturn(false);
        when(messageListenerContainer.isRunning()).thenReturn(false);
        doNothing().when(messageListenerContainer).start();

        postStartup.afterStartUp();

        verify(startupUtil, times(0)).registerInDataCatalog();
        verify(messageListenerContainer, times(0)).start();
    }

    @SneakyThrows
    @Test
    @DisplayName("Should fail to register data on DataCatalog when input topic entries are not available")
    void test_noDataCatalogEntriesArePresent() {
        when(dependentServiceAvailabilityScriptingVm.checkServiceWithCircuitBreaker()).thenReturn(true);
        when(dependentServiceAvailabilityDataCatalog.checkServiceWithCircuitBreaker()).thenReturn(true);
        when(startupUtil.queryDataCatalogForInputTopic()).thenReturn(false);
        when(dependentServiceAvailabilityKafka.checkService()).thenReturn(false);
        when(dependentServiceAvailabilityConnectedSystems.checkServiceWithCircuitBreaker()).thenReturn(true);
        doNothing().when(schemaRegistryService).registerSchemasWithRetry();
        when(kafkaListenerEndpointRegistry.getListenerContainer(any())).thenReturn(messageListenerContainer);
        when(messageListenerContainer.isAutoStartup()).thenReturn(false);
        when(messageListenerContainer.isRunning()).thenReturn(false);
        doNothing().when(messageListenerContainer).start();

        postStartup.afterStartUp();

        verify(startupUtil, times(0)).registerInDataCatalog();
        verify(messageListenerContainer, times(0)).start();
    }

    @SneakyThrows
    @Test
    @DisplayName("Should not start Listener if is already running")
    void test_tryToStartListenerWhileAlreadyRunning() {
        when(dependentServiceAvailabilityScriptingVm.checkServiceWithCircuitBreaker()).thenReturn(true);
        when(dependentServiceAvailabilityDataCatalog.checkServiceWithCircuitBreaker()).thenReturn(true);
        when(startupUtil.queryDataCatalogForInputTopic()).thenReturn(true);
        when(dependentServiceAvailabilityKafka.checkService()).thenReturn(true);
        when(dependentServiceAvailabilityConnectedSystems.checkServiceWithCircuitBreaker()).thenReturn(true);
        doNothing().when(schemaRegistryService).registerSchemasWithRetry();
        when(kafkaListenerEndpointRegistry.getListenerContainer(any())).thenReturn(messageListenerContainer);
        when(messageListenerContainer.isAutoStartup()).thenReturn(false);
        when(messageListenerContainer.isRunning()).thenReturn(true);
        doNothing().when(messageListenerContainer).start();
        postStartup.afterStartUp();

        verify(messageListenerContainer, times(0)).start();
    }

    @SneakyThrows
    @Test
    @DisplayName("Should not start Listener if autostart is true")
    void test_tryToStartListenerWhenAutoStartFalse() {
        when(dependentServiceAvailabilityScriptingVm.checkServiceWithCircuitBreaker()).thenReturn(true);
        when(dependentServiceAvailabilityDataCatalog.checkServiceWithCircuitBreaker()).thenReturn(true);
        when(startupUtil.queryDataCatalogForInputTopic()).thenReturn(true);
        when(dependentServiceAvailabilityKafka.checkService()).thenReturn(true);
        when(dependentServiceAvailabilityConnectedSystems.checkServiceWithCircuitBreaker()).thenReturn(true);
        doNothing().when(schemaRegistryService).registerSchemasWithRetry();
        when(kafkaListenerEndpointRegistry.getListenerContainer(any())).thenReturn(messageListenerContainer);
        when(messageListenerContainer.isAutoStartup()).thenReturn(true);
        when(messageListenerContainer.isRunning()).thenReturn(false);
        doNothing().when(messageListenerContainer).start();
        postStartup.afterStartUp();

        verify(messageListenerContainer, times(0)).start();
    }

}