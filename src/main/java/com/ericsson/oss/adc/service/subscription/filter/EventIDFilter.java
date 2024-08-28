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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Responsible for maintaining EventID -> Subs Interested in EventID.
 * Handles in place add/remove/updates
 */
@Slf4j
@Component
public class EventIDFilter {

    private final ConcurrentHashMap<String, Set<String>> eventIDtoRappId = new ConcurrentHashMap<>();
    private static final String WILD_CARD = "*";

    /**
     * Add the EventID predicates for a new subscription. This stores them
     * in a map of [EventID] -> [RappIDS interested in EventID], so that any EventIDS in common will be grouped.
     * If no eventID is specified we assume * and match for all events, and continue to track the RappID for that,
     * so it can be removed easily
     * <p>
     * Thread safe.
     *
     * @param subscriptionRAppId
     *         the id of the rApp associated with the new Subscription
     * @param eventIDs
     *         List of EventIDS that sub is interested in, can be list of specific events or *
     */
    protected void addToFilter(String subscriptionRAppId, List<String> eventIDs) {
        if (eventIDs.isEmpty()) {
            //If the Rapp has not supplied specific eventID predicate then they want all, so cover here.
            log.info("EventID's list is empty, * wildcard added for all events");
            eventIDs.add(WILD_CARD);
        }
        for (String currentEventID : eventIDs) {

            log.debug("Adding/merging filter for eventId {}, subscriptionRAppId {}", currentEventID, subscriptionRAppId);

            Set<String> subscriptionInterestedInEventID = new HashSet<>(1);
            subscriptionInterestedInEventID.add(subscriptionRAppId);

            // add new the subs to the existing subs in a set and merge it to the key of the current event ID.
            eventIDtoRappId.merge(currentEventID, subscriptionInterestedInEventID, (oldSubSet, newSubSet) -> {
                oldSubSet.addAll(newSubSet);
                return oldSubSet;
            });
        }
    }

    /**
     * Remove a filter for a {@link List} eventId's for a given subscriptionId
     *
     * @param subscriptionRAppId
     *         The id of the rApp associated with the subscription to remove the eventId's for.
     * @param eventIDs
     *         A {@link List} of eventId's to remove.
     */
    protected void removeFromFilter(String subscriptionRAppId, List<String> eventIDs) {
        if (eventIDs.isEmpty()) {
            //If the Rapp has not supplied specific eventID predicate then they want to remove all, so cover here.
            log.info("EventID's list is empty, * wildcard removed for all events");
            eventIDs.add(WILD_CARD);
        }

        for (String eventId : eventIDs) {
            eventIDtoRappId.computeIfPresent(eventId, (event, subsInterested) -> {

                log.debug("Removing/ filter for eventId {}, subscriptionRAppId {}", event, subscriptionRAppId);
                subsInterested.remove(subscriptionRAppId);

                //if no other subscriptions interested then remove event key by setting to null, not allowed in chMap.
                if (subsInterested.isEmpty()) {
                    return null;
                }
                return subsInterested;
            });
        }
    }

    /**
     * Look up all RappIDs interesting in this event. Always check if any sub has requested * or all events. Then combines that with the set of
     * specific event requesters.
     * <p>
     * Required as although multiple subscribers might want "Event 3000" they possibly do not want it from this particular node.
     *
     * @param eventID
     *         eventID to query for
     * @return set of all subscribers that want this event or ALL events
     */
    protected Set<String> filterByEventID(String eventID) {
        //This is to check here to see if anyone wants all events
        Set<String> subIDsWantingAllEvents = eventIDtoRappId.getOrDefault(WILD_CARD, new HashSet<>());

        Set<String> subIDsWantingSpecificEvents = eventIDtoRappId.getOrDefault(eventID, new HashSet<>());
        subIDsWantingAllEvents.addAll(subIDsWantingSpecificEvents);

        return subIDsWantingAllEvents;
    }
}
