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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventIDFilterTest {

    @Test
    @DisplayName("Add filter with empty list and test wildcard added")
    void addToFilterWithEmptyEventsForWildcard() {
        EventIDFilter eventIDFilter = new EventIDFilter();

        eventIDFilter.addToFilter("1", new ArrayList<>());
        assertThat(eventIDFilter.filterByEventID("*").contains("1")).isTrue();
    }

    @Test
    @DisplayName("Add filters to the list and test we can filter by subscribed event ID")
    void addToFilterAndFilterBySubscribedEventId() {
        EventIDFilter eventIDFilter = new EventIDFilter();

        eventIDFilter.addToFilter("1", List.of("1001", "1002"));
        assertThat(eventIDFilter.filterByEventID("1001").contains("1")).isTrue();
    }

    @Test
    @DisplayName("Add filters to the list and test filter for unsubscribed event")
    void addToFilterAndFilterByUnSubscribedEventId() {
        EventIDFilter eventIDFilter = new EventIDFilter();

        eventIDFilter.addToFilter("2", List.of("1001", "1002"));
        assertThat(eventIDFilter.filterByEventID("1003").isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Add filters to the list and test when multiple subscriptions added, one removed, the other remains")
    void addToFilterMultipleSubscriptionsForSameEventId() {
        EventIDFilter eventIDFilter = new EventIDFilter();

        eventIDFilter.addToFilter("1", List.of("1001", "1002"));
        eventIDFilter.addToFilter("2", List.of("1001", "1002"));

        assertThat(eventIDFilter.filterByEventID("1001").containsAll(List.of("1", "2"))).isTrue();
        eventIDFilter.removeFromFilter("1", List.of("1001", "1002"));

        assertThat(eventIDFilter.filterByEventID("1001").contains("2")).isTrue();
        assertThat(eventIDFilter.filterByEventID("1001").contains("1")).isFalse();
    }

    @Test
    @DisplayName("Remove filter from list")
    void removeFilterFromList() {
        EventIDFilter eventIDFilter = new EventIDFilter();

        eventIDFilter.addToFilter("2", List.of("1001", "1002"));
        assertThat(eventIDFilter.filterByEventID("1001").isEmpty()).isFalse();

        eventIDFilter.removeFromFilter("2", List.of("1001"));
        assertThat(eventIDFilter.filterByEventID("1001").isEmpty()).isTrue();

    }

    @Test
    @DisplayName("Remove * filter from list")
    void removeSpecialAstrixFilter() {
        EventIDFilter eventIDFilter = new EventIDFilter();

        eventIDFilter.addToFilter("1", List.of("*", "1001", "1002"));
        assertThat(eventIDFilter.filterByEventID("*").isEmpty()).isFalse();

        eventIDFilter.removeFromFilter("1", List.of("*"));
        assertThat(eventIDFilter.filterByEventID("*").isEmpty()).isTrue();

    }

    @Test
    @DisplayName("Remove filter with empty list and test wildcard removed")
    void removeFromFilterWithEmptyEventsForWildcard() {
        EventIDFilter eventIDFilter = new EventIDFilter();

        eventIDFilter.addToFilter("1", new ArrayList<>());
        assertThat(eventIDFilter.filterByEventID("*").contains("1")).isTrue();

        eventIDFilter.removeFromFilter("1", new ArrayList<>());
        assertThat(eventIDFilter.filterByEventID("*").contains("1")).isFalse();
    }
}