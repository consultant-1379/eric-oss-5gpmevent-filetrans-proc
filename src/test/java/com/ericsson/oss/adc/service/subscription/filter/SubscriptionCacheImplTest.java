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

package com.ericsson.oss.adc.service.subscription.filter;

import com.ericsson.oss.adc.models.Ids;
import com.ericsson.oss.adc.models.Subscription;
import com.ericsson.oss.adc.models.data.catalog.r1.DataJobSummary;
import com.ericsson.oss.adc.models.data.catalog.r1.InterfaceType;
import com.ericsson.oss.adc.models.metrics.ActiveDataJobsGauge;
import com.ericsson.oss.adc.models.metrics.ActiveSubscriptionsGauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test with no spring context
 */
class SubscriptionCacheImplTest {

    private NodeNameFilter nodeNameFilter = new NodeNameFilter();
    private EventIDFilter eventIDFilter = new EventIDFilter();
    private final SimpleMeterRegistry simpleMeterRegistry = new SimpleMeterRegistry();
    private ActiveSubscriptionsGauge activeSubscriptionsGauge = new ActiveSubscriptionsGauge(simpleMeterRegistry);
    private ActiveDataJobsGauge activeDataJobsGauge = new ActiveDataJobsGauge(simpleMeterRegistry);
    private SubscriptionCache subscriptionCache = new SubscriptionCache(nodeNameFilter, eventIDFilter, activeSubscriptionsGauge, activeDataJobsGauge);

    private static final String nodeNameShouldMatchMessage = "NodeName: %s should match";
    private static final String nodeNameShouldNotMatchMessage = "NodeName: %s should not match";
    private static final String eventIDShouldMatchMessage = "EventID %s should match";
    private static final String eventIDShouldNotMatchMessage = "EventID %s should not match";

    @BeforeEach
    public void init() {
        nodeNameFilter = new NodeNameFilter();
        eventIDFilter = new EventIDFilter();
        activeSubscriptionsGauge = new ActiveSubscriptionsGauge(simpleMeterRegistry);
        activeDataJobsGauge = new ActiveDataJobsGauge(simpleMeterRegistry);
        subscriptionCache = new SubscriptionCache(nodeNameFilter, eventIDFilter, activeSubscriptionsGauge, activeDataJobsGauge);

    }

    @Test
    @DisplayName("Given a valid Subscription, the cache should return true for the matching NodeName pattern and EventID and false for non matching")
    void testSingleSubscriptionAdditionAndMatch() {
        String matchingNodeName = "Something-ManagedElement=5G-node";
        String matchingEventID = "1";
        String nonMatchingNodeName = "ManagedElement=4G";
        String nonMatchingEventID = "3000";

        DataJobSummary dataJobSummary = DataJobSummary.builder()
                .rAppID("1000")
                .interfaceType(InterfaceType.NON_R1)
                .dataDeliverySchemaId("PmEventOuterClass.PmEvent")
                .nodeNames(List.of("*ManagedElement=5G*"))
                .eventId(List.of("1", "2"))
                .build();

        subscriptionCache.addDataJobSummary(dataJobSummary);

        assertTrue(subscriptionCache.filterByNodeName(matchingNodeName), String.format(nodeNameShouldMatchMessage, matchingNodeName));
        assertTrue(subscriptionCache.filterByEventID(matchingEventID), String.format(eventIDShouldMatchMessage, matchingEventID));

        assertFalse(subscriptionCache.filterByNodeName(nonMatchingNodeName), String.format(nodeNameShouldNotMatchMessage, nonMatchingNodeName));
        assertFalse(subscriptionCache.filterByEventID(nonMatchingEventID), String.format(eventIDShouldNotMatchMessage, nonMatchingEventID));
    }

    @Test
    @DisplayName("Given a Subscription with empty predicates, the cache should interpret this as * for each predicate and allow all events")
    void testSingleSubscriptionNoPredicatesAndMatch() {
        String predicatesDontAskMe = "{}";
        Subscription subscription = new Subscription(1, new Ids("1000"),"sub1", "active", predicatesDontAskMe, true);

        String matchingNodeName = "Something-ManagedElement=5G-node";
        String matchingEventID = "1";

        DataJobSummary dataJobSummary = DataJobSummary.builder()
                .rAppID("1000")
                .interfaceType(InterfaceType.R1)
                .dataDeliverySchemaId("5G.PmEventOuterClass.PmEvent")
                .nodeNames(new ArrayList<>())
                .eventId(new ArrayList<>())
                .build();

        subscriptionCache.addDataJobSummary(dataJobSummary);

        System.out.println(subscription.getPredicates());
        assertTrue(subscriptionCache.filterByNodeName(matchingNodeName), String.format(nodeNameShouldMatchMessage, matchingNodeName));
        assertTrue(subscriptionCache.filterByEventID(matchingEventID), String.format(eventIDShouldMatchMessage, matchingEventID));
    }

    @Test
    @DisplayName("Given 2 subs, subscriptionCache should be able to match the predicates of either when successfully added")
    void testMultipleValidSubscriptions() {
        String sub1MatchingNodeName = "ManagedElement=5G";
        String sub1MatchingEventID = "1";

        String sub2MatchingNodeName = "ManagedElement=Texas";
        String sub2MatchingEventID = "2";

        DataJobSummary dataJobSummary1 = DataJobSummary.builder()
                .rAppID("1000")
                .interfaceType(InterfaceType.NON_R1)
                .dataDeliverySchemaId("PmEventOuterClass.PmEvent")
                .nodeNames(List.of("*ManagedElement=5G*"))
                .eventId(List.of("1"))
                .build();

        DataJobSummary dataJobSummary2 = DataJobSummary.builder()
                .rAppID("1001")
                .interfaceType(InterfaceType.R1)
                .dataDeliverySchemaId("5G.PmEventOuterClass.PmEvent")
                .nodeNames(List.of("*ManagedElement=Texas*"))
                .eventId(List.of("2"))
                .build();


        subscriptionCache.addDataJobSummary(dataJobSummary1);

        //Sub1 should match at this stage, but not sub2
        assertTrue(subscriptionCache.filterByNodeName(sub1MatchingNodeName), String.format(nodeNameShouldMatchMessage, sub1MatchingNodeName));
        assertTrue(subscriptionCache.filterByEventID(sub1MatchingEventID), String.format(eventIDShouldMatchMessage, sub1MatchingEventID));
        assertFalse(subscriptionCache.filterByEventID(sub2MatchingEventID), String.format(eventIDShouldNotMatchMessage, sub2MatchingEventID));
        assertFalse(subscriptionCache.filterByEventID(sub2MatchingEventID), String.format(eventIDShouldMatchMessage, sub2MatchingEventID));

        //update cache to include sub2, now all should match
        subscriptionCache.addDataJobSummary(dataJobSummary2);

        assertTrue(subscriptionCache.filterByNodeName(sub1MatchingNodeName), String.format(nodeNameShouldMatchMessage, sub1MatchingNodeName));
        assertTrue(subscriptionCache.filterByEventID(sub1MatchingEventID), String.format(eventIDShouldMatchMessage, sub1MatchingEventID));
        assertTrue(subscriptionCache.filterByNodeName(sub2MatchingNodeName), String.format(nodeNameShouldMatchMessage, sub2MatchingNodeName));
        assertTrue(subscriptionCache.filterByEventID(sub2MatchingEventID), String.format(eventIDShouldMatchMessage, sub2MatchingEventID));

    }

    @Test
    @DisplayName("Given an invalid pattern for NodeName filter, the subscription should be rejected and the filter should fail to match")
    void testForInvalidPredicates() {
        String nonMatchingNodeName = "ManagedElement=5G";
        String nonMatchingEventID = "1";

        DataJobSummary dataJobSummary = DataJobSummary.builder()
                .rAppID("1000")
                .interfaceType(InterfaceType.R1)
                .dataDeliverySchemaId("5G.PmEventOuterClass.PmEvent")
                .nodeNames(List.of("*ManagedElement=5G*[")) //contains non-closed [
                .eventId(List.of("1", "2"))
                .build();


        subscriptionCache.addDataJobSummary(dataJobSummary);

        assertFalse(subscriptionCache.filterByNodeName(nonMatchingNodeName), String.format(nodeNameShouldNotMatchMessage, nonMatchingNodeName));
        assertFalse(subscriptionCache.filterByEventID(nonMatchingEventID), String.format(eventIDShouldNotMatchMessage, nonMatchingEventID));
    }

    @Test
    @DisplayName("Given a single subscription in the cache, it should match predicates until it is removed from the cache")
    void testAddThenRemoveSub() {
        String matchingNodeName = "Something-ManagedElement=5G-node";
        String matchingEventID = "1";

        DataJobSummary dataJobSummary = DataJobSummary.builder()
                .rAppID("1000")
                .interfaceType(InterfaceType.R1)
                .dataDeliverySchemaId("5G.PmEventOuterClass.PmEvent")
                .nodeNames(List.of("*ManagedElement=5G*"))
                .eventId(List.of("1", "2"))
                .build();

        subscriptionCache.addDataJobSummary(dataJobSummary);

        assertTrue(subscriptionCache.filterByNodeName(matchingNodeName), String.format(nodeNameShouldMatchMessage, matchingNodeName));
        assertTrue(subscriptionCache.filterByEventID(matchingEventID), String.format(eventIDShouldMatchMessage, matchingEventID));

        subscriptionCache.removeDataJobSummary(dataJobSummary);

        assertFalse(subscriptionCache.filterByNodeName(matchingNodeName), String.format(nodeNameShouldNotMatchMessage, matchingNodeName));
        assertFalse(subscriptionCache.filterByEventID(matchingEventID), String.format(eventIDShouldNotMatchMessage, matchingEventID));
    }

    @Test
    @DisplayName("Given a wildcard subscription in the cache, it should match predicates until removed from the cache")
    void testAddThenRemoveWildCard() {
        String matchingNodeName = "Something-ManagedElement=5G-node";
        String matchingEventID = "1";

        DataJobSummary dataJobSummary = DataJobSummary.builder()
                .rAppID("1000")
                .interfaceType(InterfaceType.R1)
                .dataDeliverySchemaId("5G.PmEventOuterClass.PmEvent")
                .nodeNames(new ArrayList<>())
                .eventId(new ArrayList<>())
                .build();

        subscriptionCache.addDataJobSummary(dataJobSummary);

        assertTrue(subscriptionCache.filterByNodeName(matchingNodeName), String.format(nodeNameShouldMatchMessage, matchingNodeName));
        assertTrue(subscriptionCache.filterByEventID(matchingEventID), String.format(eventIDShouldMatchMessage, matchingEventID));

        subscriptionCache.removeDataJobSummary(dataJobSummary);

        assertFalse(subscriptionCache.filterByNodeName(matchingNodeName), String.format(nodeNameShouldNotMatchMessage, matchingNodeName));
        assertFalse(subscriptionCache.filterByEventID(matchingEventID), String.format(eventIDShouldNotMatchMessage, matchingEventID));
    }


    @Test
    @DisplayName("Given a subscription, updating its predicates should result in the filter only matching the new predicates")
    void testAddThenUpdateSub() {
        String initiallyMatchingNodeName = "Something-ManagedElement=5G-node";
        String initiallyMatchingEventID = "1";

        String updatedMatchingNodeName = "ManagedElement=Texas";
        String updatedMatchingEventID = "5";

        DataJobSummary dataJobSummary = DataJobSummary.builder()
                .rAppID("1000")
                .interfaceType(InterfaceType.R1)
                .dataDeliverySchemaId("5G.PmEventOuterClass.PmEvent")
                .nodeNames(List.of("*ManagedElement=5G*"))
                .eventId(List.of("1", "2"))
                .build();

        DataJobSummary updatedDataJobSummary = DataJobSummary.builder()
                .rAppID("1000")
                .interfaceType(InterfaceType.R1)
                .dataDeliverySchemaId("5G.PmEventOuterClass.PmEvent")
                .nodeNames(List.of("*ManagedElement=Texas*"))
                .eventId(List.of("5"))
                .build();

        subscriptionCache.addDataJobSummary(dataJobSummary);

        assertTrue(subscriptionCache.filterByNodeName(initiallyMatchingNodeName), String.format(nodeNameShouldMatchMessage, initiallyMatchingNodeName));
        assertTrue(subscriptionCache.filterByEventID(initiallyMatchingEventID), String.format(eventIDShouldMatchMessage, initiallyMatchingEventID));

        subscriptionCache.updateDataJobSummary(updatedDataJobSummary);

        //fail to match old predicates
        assertFalse(subscriptionCache.filterByNodeName(initiallyMatchingNodeName), String.format(nodeNameShouldNotMatchMessage, initiallyMatchingNodeName));
        assertFalse(subscriptionCache.filterByEventID(initiallyMatchingEventID), String.format(eventIDShouldNotMatchMessage, initiallyMatchingEventID));

        //match only new predicates
        assertTrue(subscriptionCache.filterByNodeName(updatedMatchingNodeName), String.format(nodeNameShouldMatchMessage, updatedMatchingNodeName));
        assertTrue(subscriptionCache.filterByEventID(updatedMatchingEventID), String.format(eventIDShouldMatchMessage, updatedMatchingEventID));

    }

    @Test
    @DisplayName("Given a subscription, the kafka filter should correctly return whether to discard a message or not based on nodeName")
    void testSubscriptionKafkaFilter() {
        String matchingNodeName = "Something-ManagedElement=5G-node";
        String nonMatchingNodeName = "ManagedElement=4G";

        DataJobSummary dataJobSummary = DataJobSummary.builder()
                .rAppID("1000")
                .interfaceType(InterfaceType.R1)
                .dataDeliverySchemaId("5G.PmEventOuterClass.PmEvent")
                .nodeNames(List.of("*ManagedElement=5G*"))
                .eventId(List.of("1", "2"))
                .build();

        subscriptionCache.addDataJobSummary(dataJobSummary);

        //Do not discard as subscription wants it
        assertFalse(subscriptionCache.filterKafkaRecordNodeName(matchingNodeName), "Listener filter should not discard matching nodeName");

        //Do discard, as not wanted
        assertTrue(subscriptionCache.filterKafkaRecordNodeName(nonMatchingNodeName), "Listener filter should discard non-matching nodeName");
    }
}