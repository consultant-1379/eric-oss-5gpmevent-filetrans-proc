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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Slf4j
@Component
public class NodeNameFilter {

    private final ConcurrentHashMap<NodeNamePattern, Set<String>> nodeNamePatternsToRappId = new ConcurrentHashMap<>();
    private static final String WILD_CARD = "*";

    /**
     * Add the NodeName predicates for a new subscription. This will preprocess all predicates to patterns and store them
     * in a map of [Pattern] -> [RappIds interested in Pattern], so that any patterns in common will be grouped.
     * <p>
     * Thread safe.
     *
     * @param subscriptionRAppId rAppId associated with new Subscription
     * @param nodeNames      List of nodeName predicates, they can be of the form "*node=*" or full specific node names.
     * @return boolean did the NodeNamePattern filters for subscription get added successfully.
     */
    protected boolean addToFilter(String subscriptionRAppId, List<String> nodeNames) {
        if (nodeNames.isEmpty()) {
            nodeNames.add(WILD_CARD);
        }
        try {
            List<NodeNamePattern> nodeNamePatterns = convertNodeStringToPattern(nodeNames);
            for (NodeNamePattern nodeNamePattern : nodeNamePatterns) {
                Set<String> subscriptionInterestedInNode = new HashSet<>();
                subscriptionInterestedInNode.add(subscriptionRAppId);

                nodeNamePatternsToRappId.merge(nodeNamePattern, subscriptionInterestedInNode, (oldSubSet, newSubSet) ->
                {
                    oldSubSet.addAll(newSubSet);
                    return oldSubSet;
                });
            }
        } catch (PatternSyntaxException e) {
            log.debug("Stack trace -Failed to parse regex ", e);
            log.error("Failed to parse Regex for subscriptionRAppId:{}, patterns:{}", subscriptionRAppId, nodeNames);
            return false;
        }
        return true;
    }

    /**
     * Removes the nodeName Patterns associated with a subscription, leaves other subscriptions interested in same nodeName patterns intact.
     * <p>
     * Thread safe.
     *
     * @param subscriptionRAppId rAppId associated with the Subscription to remove
     * @param nodeNames      List of nodeName predicates the Subscription was interested in.
     */
    protected void removeFromFilter(String subscriptionRAppId, List<String> nodeNames) {
        if (nodeNames.isEmpty()) {
            nodeNames.add(WILD_CARD);
        }
        try {
            List<NodeNamePattern> nodeNamePatterns = convertNodeStringToPattern(nodeNames);
            for (NodeNamePattern nodeNamePattern : nodeNamePatterns) {
                nodeNamePatternsToRappId.computeIfPresent(nodeNamePattern, (node, subsInterested) -> {
                    subsInterested.remove(subscriptionRAppId);
                    //if no other subscriptions interested then remove nodeNamePattern key by setting to null, not allowed in chMap.
                    if (subsInterested.isEmpty()) {
                        return null;
                    }
                    return subsInterested;
                });
            }

        } catch (PatternSyntaxException e) {
            log.debug("Stack trace -Failed to parse regex ", e);
            log.error("Failed to parse Regex for subscriptionName:{}, patterns:{}", subscriptionRAppId, nodeNames);
        }
    }


    /**
     * Filter for any SubscriptionNames that are potentially interested in this NodeName.
     * Note that we implicitly check the subs that have not defined a pattern as we insert * in that case
     * and are now iterating through the entire pattern map.
     *
     * @param nodeName nodeName we want to match
     * @return the Set of all RappIds interested in this NodeName.
     */
    protected Set<String> filterByNodeName(String nodeName) {
        //Iterate over all known patterns to see which ones match nodeName. As user supplied regexes may be unique yet still match the node.
        Set<String> matchingRappIds = new HashSet<>();
        for (Map.Entry<NodeNamePattern, Set<String>> currentPattern : nodeNamePatternsToRappId.entrySet()) {
            if (currentPattern.getKey().getPattern().asMatchPredicate().test(nodeName)) {
                matchingRappIds.addAll(currentPattern.getValue());
            }
        }
        return matchingRappIds;
    }

    /**
     * Precompiles the nodeNames to Pattern to allow faster filtering
     *
     * @param nodeNames nodeName String patterns to be compiled
     * @return List of compiled nodeNames
     * @throws PatternSyntaxException if any pattern fails to parse we throw an exception up to the add/update method, so it can cancel the operation
     */
    private List<NodeNamePattern> convertNodeStringToPattern(List<String> nodeNames) throws PatternSyntaxException {
        List<NodeNamePattern> nodeNamePatterns = new ArrayList<>(nodeNames.size());
        for (String currentNode : nodeNames) {
            String wildStarsExpanded = currentNode.replace(WILD_CARD, ".*");
            Pattern pattern = Pattern.compile(wildStarsExpanded, Pattern.CASE_INSENSITIVE);

            NodeNamePattern nodeNamePattern = new NodeNamePattern(currentNode, pattern);
            nodeNamePatterns.add(nodeNamePattern);
        }
        return nodeNamePatterns;
    }
}
