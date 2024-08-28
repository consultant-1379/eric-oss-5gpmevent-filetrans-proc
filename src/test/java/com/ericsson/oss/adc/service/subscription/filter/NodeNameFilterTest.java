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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class NodeNameFilterTest {


    private NodeNameFilter nodeNameFilter = new NodeNameFilter();

    @BeforeEach
    public void init() {
        nodeNameFilter = new NodeNameFilter();
    }


    @Test
    @DisplayName("Should fail to match pattern as no wildcard at beginning so not complete match")
    public void addSubAndFailToMatch() {
        String rappID = "1";
        String nodeRegexString = "Node*";

        assertTrue(nodeNameFilter.addToFilter(rappID, List.of(nodeRegexString)), "Expected filter to be successfully added");

        Set<String> actual = nodeNameFilter.filterByNodeName("BNode");

        assertTrue(actual.isEmpty(), "Expected empty subset");
    }

    @Test
    @DisplayName("Should add wild card when empty nodename list and return sub")
    public void addSubAndEmptyNode() {
        String rappID = "1";
        Set<String> expectedSub = Set.of(rappID);
        List<String> nodeNames = new ArrayList<>();

        assertTrue(nodeNameFilter.addToFilter(rappID, nodeNames), "Expected filter to be successfully added");

        Set<String> actual = nodeNameFilter.filterByNodeName("BNode");

        assertEquals(expectedSub, actual, "Expected subset to be returned");
    }

    @Test
    @DisplayName("Given a nodeName regex string with wildcard at start and end should match")
    public void addDoubleWildcardAndMatch() {
        String rappID = "2";
        String nodeRegexString = "*Node*";
        Set<String> expectedSub = Set.of(rappID);

        assertTrue(nodeNameFilter.addToFilter(rappID, List.of(nodeRegexString)), "Expected filter to be successfully added");

        Set<String> actual = nodeNameFilter.filterByNodeName("BNode");

        assertEquals(expectedSub, actual, "Expected matching subs :");
    }

    @Test
    @DisplayName("If given FDN node name with no wildcard, should match")
    public void testFullNodeName() {
        String rappID = "1";
        String nodeRegexString = "Node,subnetwork=5G";
        Set<String> expectedSub = Set.of(rappID);

        assertTrue(nodeNameFilter.addToFilter(rappID, List.of(nodeRegexString)), "Expected filter to be successfully added");

        Set<String> actual = nodeNameFilter.filterByNodeName("Node,subnetwork=5G");

        assertEquals(expectedSub, actual, "Expected sets to match:");
    }

    @Test
    @DisplayName("Given multiple Subs, should only return matching one")
    public void testWithMultipleSubs() {
        String rappID1 = "1";
        String nodeRegexString1 = "*Node,subnetwork=5G*";
        String rappID2 = "2";
        String nodeRegexString2 = "*Node,subnetwork=4G*";
        Set<String> expectedSub = Set.of(rappID1);

        assertTrue(nodeNameFilter.addToFilter(rappID1, List.of(nodeRegexString1)), "Expected filter to be successfully added");
        assertTrue(nodeNameFilter.addToFilter(rappID2, List.of(nodeRegexString2)), "Expected filter to be successfully added");

        Set<String> actual = nodeNameFilter.filterByNodeName("Node,subnetwork=5G");

        assertEquals(expectedSub, actual, "Expected only 1 sub to match:");
        assertTrue(actual.contains(rappID1), "Expected 1 only");
    }

    @Test
    @DisplayName("Given multiple Subs of same Node Predicate, should return both Subscriptions")
    public void testAddingMultipleMatchingSubs() {
        String rappID1 = "1";
        String nodeRegexString1 = "*Node,subnetwork=5G*";
        String rappID2 = "2";
        String nodeRegexString2 = "*Node,subnetwork=5G*";
        Set<String> expectedSub = Set.of(rappID1, rappID2);

        assertTrue(nodeNameFilter.addToFilter(rappID1, List.of(nodeRegexString1)), "Expected filter to be successfully added");
        assertTrue(nodeNameFilter.addToFilter(rappID2, List.of(nodeRegexString2)), "Expected filter to be successfully added");

        Set<String> actual = nodeNameFilter.filterByNodeName("Node,subnetwork=5G");

        assertEquals(expectedSub, actual, "Expected both subs to match:");
        assertTrue(actual.contains(rappID1));
        assertTrue(actual.contains(rappID2));
    }

    @Test
    @DisplayName("Given one subscription, removing it from the cache results in its nodePattern not being matched")
    public void testRemovalOfSingleSubscription() {
        String rappID = "1";
        String nodeRegexString = "Node*";
        Set<String> expectedSubMatch = Set.of(rappID);

        assertTrue(nodeNameFilter.addToFilter(rappID, List.of(nodeRegexString)), "Expected filter to be successfully added");

        Set<String> actual = nodeNameFilter.filterByNodeName("Node*");

        assertEquals(expectedSubMatch, actual, "Expected subset to be returned before it is removed");

        nodeNameFilter.removeFromFilter(rappID, List.of(nodeRegexString));
        actual = nodeNameFilter.filterByNodeName("Node*");
        assertTrue(actual.isEmpty(), "Expected no match to occur after removal");
    }

    @Test
    @DisplayName("Given two subscriptions, removing one from the cache results in its NodePattern still matching for the remaining subscription")
    public void testRemovalOfSingleSubscriptionWithMultipleInCache() {
        String rappID1 = "1";
        String rappID2 = "2";
        String nodeRegexString = "Node*";
        Set<String> expectedSubMatch = Set.of(rappID1, rappID2);
        Set<String> expectedSubAfterRemoval = Set.of(rappID2);

        assertTrue(nodeNameFilter.addToFilter(rappID1, List.of(nodeRegexString)), "Expected filter to be successfully added");
        assertTrue(nodeNameFilter.addToFilter(rappID2, List.of(nodeRegexString)), "Expected filter to be successfully added");

        Set<String> actual = nodeNameFilter.filterByNodeName("Node*");

        assertEquals(expectedSubMatch, actual, "Expected subset to be returned before it is removed");

        nodeNameFilter.removeFromFilter(rappID1, List.of(nodeRegexString));
        actual = nodeNameFilter.filterByNodeName("Node*");
        assertEquals(expectedSubAfterRemoval, actual, "Expected 2 to match nodename after removing 1");
    }

    @Test
    @DisplayName("Given two subscriptions with different NodePatterns, expect removal of one to not affect the other")
    public void testRemovalOfUnrelatedSubscription() {
        String rappID1 = "1";
        String rappID2 = "2";
        String nodeRegexString1 = "Node*";
        String nodeRegexString2 = "GnodeB*";
        Set<String> expectedSubMatch = Set.of(rappID1);
        Set<String> expectedSubAfterRemoval = Set.of(rappID2);

        assertTrue(nodeNameFilter.addToFilter(rappID1, List.of(nodeRegexString1)), "Expected filter to be successfully added");
        assertTrue(nodeNameFilter.addToFilter(rappID2, List.of(nodeRegexString2)), "Expected filter to be successfully added");

        Set<String> actual = nodeNameFilter.filterByNodeName("Node*");

        assertEquals(expectedSubMatch, actual, "Expected subset to be returned before it is removed");

        nodeNameFilter.removeFromFilter(rappID1, List.of(nodeRegexString1));
        actual = nodeNameFilter.filterByNodeName("GnodeB*");
        assertEquals(expectedSubAfterRemoval, actual, "Expected 2 to match nodename after removing 1");
        assertTrue(nodeNameFilter.filterByNodeName("Node*").isEmpty(), "Expected sub not to match");
    }

    @Test
    @DisplayName("Given a subscription wanting * all nodes, expect removal of subscription to stop matching any node name")
    public void testRemovalOfWildCardSubscription() {
        String rappID = "1";
        Set<String> expectedBeforeRemoval = Set.of(rappID);

        assertTrue(nodeNameFilter.addToFilter(rappID, new ArrayList<>()), "Expected filter to be successfully added");

        Set<String> actual = nodeNameFilter.filterByNodeName("AnyNode");

        assertEquals(expectedBeforeRemoval, actual, "Expected any node to be matched before removal");

        nodeNameFilter.removeFromFilter("1", new ArrayList<>());

        assertTrue(nodeNameFilter.filterByNodeName("AnyNode").isEmpty(), "Expected no matching rappID");
    }

    @Test
    @DisplayName("Given an invalid regex pattern, expect no exception to be thrown and the cache to not contain the rappID")
    public void testAddingInvalidRegex() {
        String rappID = "1";
        String invalidRegex = "node[";

        boolean addFilterResult = assertDoesNotThrow(() -> nodeNameFilter.addToFilter(rappID, List.of(invalidRegex)));
        assertFalse(addFilterResult, "Filter should not have been added");
    }

    @Test
    @DisplayName("Given the removal invalid regex pattern, expect no exception to be thrown")
    public void testRemovingInvalidRegex() {
        String rappID = "1";
        String invalidRegex = "node[";

        assertDoesNotThrow(() -> nodeNameFilter.removeFromFilter(rappID, List.of(invalidRegex)));
    }

}


