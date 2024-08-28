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

import com.ericsson.oss.adc.models.Subscription;
import com.ericsson.oss.adc.models.adapters.SubDataJobAdapter;
import com.ericsson.oss.adc.models.metrics.ActiveDataJobsGauge;
import com.ericsson.oss.adc.models.metrics.ActiveSubscriptionsGauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.ericsson.oss.adc.service.subscription.filter.SubscriptionsDelegate.*;
import static org.junit.jupiter.api.Assertions.*;

class SubscriptionCacheUtilTest {

    @Test
    @DisplayName("Given the subscription Cache contains two DataJobs and fetching subscriptions results in one, the inactive subscription should be removed")
    void testRemoveInactiveSubscriptions() {
        SubscriptionCache subscriptionCache = getSubscriptionCacheImpl();
        SubDataJobAdapter subDataJobAdapter = new SubDataJobAdapter();
        subscriptionCache.addDataJobSummary(subDataJobAdapter.convertSubscription(subscriptionWithTwoPredicates));
        subscriptionCache.addDataJobSummary(subDataJobAdapter.convertSubscription(subscriptionWithTwoPredicates1));

        assertEquals(2, subscriptionCache.getAllDataJobSummaryRAppIds().size());

        List<Subscription> newSubscriptions = new ArrayList<>();
        newSubscriptions.add(subscriptionWithTwoPredicates);
        SubscriptionCacheUtil subscriptionCacheUtil = new SubscriptionCacheUtil(subscriptionCache, subDataJobAdapter);
        subscriptionCacheUtil.reconstituteSubscriptionCache(newSubscriptions);

        assertEquals(1, subscriptionCache.getAllDataJobSummaryRAppIds().size());
    }

    @Test
    @DisplayName("Given the subscription Cache contains one subscriptions and fetching subscriptions results in five, the new subscription should be added")
    void testAddNewSubscriptions() {
        SubscriptionCache subscriptionCache = getSubscriptionCacheImpl();
        SubDataJobAdapter subDataJobAdapter = new SubDataJobAdapter();
        subscriptionCache.addDataJobSummary(subDataJobAdapter.convertSubscription(subscriptionWithTwoPredicates));

        assertEquals(1, subscriptionCache.getAllDataJobSummaryRAppIds().size());

        List<Subscription> newSubscriptions = new ArrayList<>();
        newSubscriptions.add(subscriptionWithTwoPredicates);
        newSubscriptions.add(subscriptionWithTwoPredicates1);
        newSubscriptions.add(subscriptionWithNoPredicates);
        newSubscriptions.add(subscriptionWithJustEventId);
        newSubscriptions.add(subscriptionWithJustNodeName);
        SubscriptionCacheUtil subscriptionCacheUtil = new SubscriptionCacheUtil(subscriptionCache, subDataJobAdapter);
        subscriptionCacheUtil.reconstituteSubscriptionCache(newSubscriptions);

        assertEquals(5, subscriptionCache.getAllDataJobSummaryRAppIds().size());
    }

    @Test
    @DisplayName("Given the subscription Cache is updated with the same subscriptions but changed predicates")
    void testUpdateSubscriptions() {
        SubscriptionCache subscriptionCache = getSubscriptionCacheImpl();
        SubDataJobAdapter subDataJobAdapter = new SubDataJobAdapter();
        subscriptionCache.addDataJobSummary(subDataJobAdapter.convertSubscription(subscriptionWithTwoPredicates));
        subscriptionCache.addDataJobSummary(subDataJobAdapter.convertSubscription(subscriptionWithTwoPredicates1));
        subscriptionCache.addDataJobSummary(subDataJobAdapter.convertSubscription(subscriptionWithTwoPredicates2));
        List<String> nodeNames = subscriptionCache.getDataJobSummaryByRAppId("1000").getNodeNames();
        List<String> eventIds = subscriptionCache.getDataJobSummaryByRAppId("1000").getEventId();

        assertEquals(3, subscriptionCache.getAllDataJobSummaryRAppIds().size());
        assertTrue(nodeNames.contains("*ManagedElement=*"));
        assertTrue(nodeNames.contains("*ERBS*"));
        assertTrue(eventIds.contains("200"));
        assertTrue(eventIds.contains("300"));

        List<Subscription> newSubscriptions = new ArrayList<>();
        newSubscriptions.add(subscriptionWithTwoPredicatesSameNameDiffPredicates);
        newSubscriptions.add(subscriptionWithTwoPredicates1);
        SubscriptionCacheUtil subscriptionCacheUtil = new SubscriptionCacheUtil(subscriptionCache, subDataJobAdapter);
        subscriptionCacheUtil.reconstituteSubscriptionCache(newSubscriptions);
        List<String> newNodeNames = subscriptionCache.getDataJobSummaryByRAppId("1000").getNodeNames();
        List<String> newEventIds = subscriptionCache.getDataJobSummaryByRAppId("1000").getEventId();

        assertTrue(nodeNames.contains("*ManagedElement=*"));
        assertTrue(newNodeNames.contains("*5*"));
        assertTrue(newEventIds.contains("100"));
        assertTrue(newEventIds.contains("700"));
        assertFalse(newNodeNames.contains("*ERBS*"));
        assertFalse(newEventIds.contains("200"));
        assertFalse(newEventIds.contains("300"));
        assertEquals(2, subscriptionCache.getAllDataJobSummaryRAppIds().size());
    }

    @Test
    @DisplayName("Given the call to Data catalog returns with an empty list, all subscriptions should be removed from the cache")
    void testUpdateSubscriptionsWithEmptySubsriptionList() {
        SubscriptionCache subscriptionCache = getSubscriptionCacheImpl();
        SubDataJobAdapter subDataJobAdapter = new SubDataJobAdapter();
        subscriptionCache.addDataJobSummary(subDataJobAdapter.convertSubscription(subscriptionWithTwoPredicates));
        subscriptionCache.addDataJobSummary(subDataJobAdapter.convertSubscription(subscriptionWithTwoPredicates1));
        subscriptionCache.addDataJobSummary(subDataJobAdapter.convertSubscription(subscriptionWithTwoPredicates2));

        assertEquals(3, subscriptionCache.getAllDataJobSummaryRAppIds().size());

        SubscriptionCacheUtil subscriptionCacheUtil = new SubscriptionCacheUtil(subscriptionCache, new SubDataJobAdapter());
        List<Subscription> newSubscriptions = new ArrayList<>();
        subscriptionCacheUtil.reconstituteSubscriptionCache(newSubscriptions);
        assertEquals(0, subscriptionCache.getAllDataJobSummaryRAppIds().size());
    }

    private SubscriptionCache getSubscriptionCacheImpl() {
        NodeNameFilter nodeNameFilter = new NodeNameFilter();
        EventIDFilter eventIDFilter = new EventIDFilter();
        SimpleMeterRegistry simpleMeterRegistry = new SimpleMeterRegistry();
        ActiveSubscriptionsGauge activeSubscriptionsGauge = new ActiveSubscriptionsGauge(simpleMeterRegistry);
        ActiveDataJobsGauge activeDataJobsGauge = new ActiveDataJobsGauge(simpleMeterRegistry);
        return new SubscriptionCache(nodeNameFilter, eventIDFilter, activeSubscriptionsGauge, activeDataJobsGauge);
    }
}
