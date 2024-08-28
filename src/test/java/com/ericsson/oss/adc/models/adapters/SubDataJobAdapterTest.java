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

package com.ericsson.oss.adc.models.adapters;

import com.ericsson.oss.adc.models.DataCatalogConstants;
import com.ericsson.oss.adc.models.Subscription;
import com.ericsson.oss.adc.models.data.catalog.r1.DataJobNotification;
import com.ericsson.oss.adc.models.data.catalog.r1.DataJobSummary;
import com.ericsson.oss.adc.models.data.catalog.r1.InterfaceType;
import com.ericsson.oss.adc.service.subscription.filter.SubscriptionsDelegate;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubDataJobAdapterTest {

    private final SubDataJobAdapter subDataJobAdapter = new SubDataJobAdapter();

    @Test
    @DisplayName("Should successfully convert a subscription to a DataJobSummary")
    void test_convertSubscription() {
        Subscription subscription = SubscriptionsDelegate.subscriptionWithTwoPredicates;
        DataJobSummary expectedSummary = DataJobSummary.builder()
                .rAppID("1000")
                .dataDeliverySchemaId(DataCatalogConstants.SCHEMA_NAME_STANDARDIZED)
                .nodeNames(List.of("*ManagedElement=*", "*ERBS*"))
                .eventId(List.of("200", "300"))
                .interfaceType(InterfaceType.NON_R1)
                .build();

        DataJobSummary actualSummary = subDataJobAdapter.convertSubscription(subscription);

        assertEquals(expectedSummary, actualSummary);
    }

    @SneakyThrows
    @Test
    @DisplayName("Should successfully convert a DataJob to DataJobSummary")
    void test_convertDataJob() {
        String testEvent = "{\"version\":\"1.0.0\",\"eventType\":\"dataJobCreated\",\"event\":{\"dataJob\":{\"id\":\"1\",\"name\":\"TwoNetElements\",\"clientId\":\"rapp1\",\"status\":\"RUNNING\",\"isMandatory\":\"true\",\"dataDelivery\":\"CONTINUOUS\",\"dataDeliveryMechanism\":\"STREAMING_KAFKA\",\"dataDeliverySchemaId\":\"5G.PmEventOuterClass.PmEvent.pmevent\",\"requester\":\"requester\",\"consumerType\":\"rApp\",\"dataRepository\":{\"hostName\":\"hostname\",\"portAddress\":9092},\"productionJobDefinition\":{\"targetSelector\":{\"nodeNameList\":[\"NF1\",\"NF2\"]},\"dataSelector\":{\"eventId\":[1001,1002]}},\"dataType\":{\"dataTypeId\":\"ERICSSON:NrRanOamPmStandardEventData:1.0.0\",\"isExternal\":true}}}}";

        DataJobSummary expected = DataJobSummary.builder()
                .rAppID("rapp1")
                .dataDeliverySchemaId("5G.PmEventOuterClass.PmEvent.pmevent")
                .nodeNames(List.of("NF1", "NF2"))
                .eventId(List.of("1001", "1002"))
                .interfaceType(InterfaceType.R1)
                .build();

        DataJobNotification dataJobNotification = new ObjectMapper().readValue(testEvent, DataJobNotification.class);
        DataJobSummary actualSummary = subDataJobAdapter.convertDataJob(dataJobNotification.getDataJobEvent().getDataJob());

        assertEquals(expected, actualSummary);
    }
}