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
import com.ericsson.oss.adc.models.Ids;

public class SubscriptionsDelegate {

    public static final Subscription subscriptionWithTwoPredicates = new Subscription(
            1,
            new Ids("1000"),
            "subscriptionWithTwoPredicates",
            "Active",
//            This predicates String is similar to what may be returned currently from /catalog/v2/subscriptions
            "{\"nodeName\":[\"*ManagedElement=*\",\"*ERBS*\"],\"eventId\":[\"200\",\"300\"]}",
            true
    );
    public static final Subscription subscriptionWithTwoPredicatesSameNameDiffPredicates = new Subscription(
            1,
            new Ids("1000"),
            "subscriptionWithTwoPredicates",
            "Active",
//            This predicates String is similar to what may be returned currently from /catalog/v2/subscriptions
            "{\"nodeName\":[\"*ManagedElement=*\",\"*5*\"],\"eventId\":[\"100\",\"700\"]}",
            true
    );
    public static final Subscription subscriptionWithTwoPredicates1 = new Subscription(
            1,
            new Ids("1001"),
            "subscriptionWithTwoPredicates1",
            "Active",
//            This predicates String is similar to what may be returned currently from /catalog/v2/subscriptions
            "{\"nodeName\":[\"*ManagedElement=*\",\"*ERBS*\"],\"eventId\":[\"210\",\"330\"]}",
            true
    );
    static final Subscription subscriptionWithTwoPredicates2 = new Subscription(
            1,
            new Ids("1002"),
            "subscriptionWithTwoPredicates2",
            "Active",
//            This predicates String is similar to what may be returned currently from /catalog/v2/subscriptions
            "{\"nodeName\":[\"*ManagedElement=*\",\"*ERBS*\"],\"eventId\":[\"210\",\"330\"]}",
            true
    );
    public static final Subscription subscriptionWithNoPredicates = new Subscription(
            1,
            new Ids("1003"),
            "subscriptionWithNoPredicates",
            "Active",
            "{}",
            true
    );
    public static final Subscription subscriptionWithJustEventId = new Subscription(
            1,
            new Ids("1004"),
            "subscriptionWithJustEventId",
            "Active",
            "{\"eventId\":[\"200\",\"300\"]}",
            true
    );
    public static final Subscription subscriptionWithJustNodeName = new Subscription(
            1,
            new Ids("1005"),
            "subscriptionWithJustNodeName",
            "Active",
            "{\"nodeName\":[\"NR100gNOdeBRadio00001995\"]}",
            true
    );
}