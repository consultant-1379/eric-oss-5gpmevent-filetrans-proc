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

import com.ericsson.oss.adc.models.data.catalog.r1.DataJobSummary;
import com.ericsson.oss.adc.models.data.catalog.r1.InterfaceType;
import com.ericsson.oss.adc.models.metrics.ActiveDataJobsGauge;
import com.ericsson.oss.adc.models.metrics.ActiveSubscriptionsGauge;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Responsible for Coordinating the removal of predicates and subscriptions for each of the owned Filter Classes.
 * Maintains a ThreadLocal instead to store NodeName filter results on a per-thread basis
 * <p>
 * {@link KafkaMessageListenerContainer Kafka Listener Containers} threads are created by a {@link org.springframework.core.task.SimpleAsyncTaskExecutor SimpleAsyncTaskExecutor} that does not return threads to a pool
 * once they are released. So each consumer->FileProcessor->OutputTopicService will run under its own thread.
 */
@Slf4j
@Service
public class SubscriptionCache {

    private final NodeNameFilter nodeNameFilter;
    private final EventIDFilter eventIDFilter;
    private final ConcurrentHashMap<String, DataJobSummary> dataJobSummaryMap = new ConcurrentHashMap<>();
    private static final ThreadLocal<Set<String>> nodeFilterResult = new ThreadLocal<>();
    private final ActiveSubscriptionsGauge activeSubscriptionsGauge;
    private final ActiveDataJobsGauge activeDataJobsGauge;

    public SubscriptionCache(NodeNameFilter nodeNameFilter, EventIDFilter eventIDFilter, ActiveSubscriptionsGauge activeSubscriptionsGauge, ActiveDataJobsGauge activeDataJobsGauge) {
        this.nodeNameFilter = nodeNameFilter;
        this.eventIDFilter = eventIDFilter;
        this.activeSubscriptionsGauge = activeSubscriptionsGauge;
        this.activeDataJobsGauge = activeDataJobsGauge;
    }

    /**
     * Coordinates the adding of predicates to each filter.
     * If a predicate has an empty value, then the specific filter can choose to
     * handle it according to their implementation.
     * <p>
     * When these predicates are successfully processed, the RAppID and DataJobSummary is added to the set to keep track
     * of predicates associated.
     *
     * @param dataJobSummary The Representation of either a Subscription or a DataJobs predicates
     */
    public void addDataJobSummary(DataJobSummary dataJobSummary) {
        if (nodeNameFilter.addToFilter(dataJobSummary.getRAppID(), dataJobSummary.getNodeNames())) {
            eventIDFilter.addToFilter(dataJobSummary.getRAppID(), dataJobSummary.getEventId());
            updateLocalDataJobSummary(dataJobSummary);
            incrementActiveGauge(dataJobSummary);
        } else {
            log.warn("Failed to add DataJobSummary: {}", dataJobSummary);
        }
    }

    /**
     * Handle if a DataJobSummary is incorrectly added twice, avoid double processing.
     *
     * @param dataJobSummary to add
     */
    private void updateLocalDataJobSummary(DataJobSummary dataJobSummary) {
        if (dataJobSummaryMap.putIfAbsent(dataJobSummary.getRAppID(), dataJobSummary) == null) {
            log.info("DataJobSummary successfully added: {}", dataJobSummary);
        } else {
            log.warn("DataJobSummary already exists, this should have been an update: {}", dataJobSummary);
        }
    }

    /**
     * Coordinates updates to already existing DataJobSummaries. RApps can only change their predicates once a Subscription/DataJob is created
     * Will remove the old predicates and DataJobSummary details before adding the new details to each filter and cache
     *
     * @param updatedDataJobSummary updated DataJobSummary containing new predicates.
     */
    public void updateDataJobSummary(DataJobSummary updatedDataJobSummary) {
        //get current predicates from map, remove all rather than try and compare the differences between old predicates and new ones.
        DataJobSummary currentDataJobSummary = dataJobSummaryMap.get(updatedDataJobSummary.getRAppID());
        //remove current predicates
        if (currentDataJobSummary != null) {
            removeDataJobSummary(currentDataJobSummary);
            //this will decrement sub count
        }
        //add current predicates, and increment sub count back
        addDataJobSummary(updatedDataJobSummary);
    }

    /**
     * Removes a no longer required DataJobSummary and its predicates from each filter.
     *
     * @param dataJobSummary the DataJobSummary to remove from cache
     */
    public void removeDataJobSummary(DataJobSummary dataJobSummary) {
        nodeNameFilter.removeFromFilter(dataJobSummary.getRAppID(), dataJobSummary.getNodeNames());
        eventIDFilter.removeFromFilter(dataJobSummary.getRAppID(), dataJobSummary.getEventId());
        dataJobSummaryMap.remove(dataJobSummary.getRAppID());

        //metrics, check type
        decrementActiveGauge(dataJobSummary);
    }

    /**
     * In order to find if a DataJobSummary is interested in particular combo of NodeName/EventID, track the subscribers
     * interested in the current files nodeName, and store in threadLocal instance. The previous files result is cleared.
     * <p>
     * This avoids have to filter by nodeName for every event read, instead filtering once per file.
     *
     * @param nodeName the files nodeName to match by
     * @return returns true if there are DataJobSummary interested in this nodeName
     */
    public boolean filterByNodeName(String nodeName) {
        //Clear previous usage by this thread as a safety net. Otherwise, the results of the last nodeName could be stored.
        nodeFilterResult.remove();

        Set<String> subsWantingNodeName = nodeNameFilter.filterByNodeName(nodeName);
        nodeFilterResult.set(subsWantingNodeName);

        return !(subsWantingNodeName.isEmpty());
    }

    /**
     * For a given eventID, find any subscriptions interested in either this or ALL events, then compare the resulting
     * set with the set of subscriptions interested in the current nodeName. Retrieves the set of NodeName subscribers
     * from threadLocal instance.
     * <p>
     * Once any match is found between RappId for nodeName and RappId for eventID, then return true as we know
     * *someone* wants this event
     *
     * @param eventID EventId to filter by
     * @return boolean of whether to send the event to kafka or not
     */
    public boolean filterByEventID(String eventID) {
        //get stored RappIds for the nodeName from ThreadLocal
        Set<String> subsWantingNodeName = nodeFilterResult.get();
        Set<String> subsWantingEventID = eventIDFilter.filterByEventID(eventID);

        //faster than retainAll, escapes soon as match is found in common.
        for (String subIDForEventID : subsWantingEventID) {
            if (subsWantingNodeName.contains(subIDForEventID)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Filter method specifically for KafkaRecordStrategy
     *
     * @param nodeName NodeName to filter by
     * @return returns true if message should be discarded (no subscription is interested)
     */
    public boolean filterKafkaRecordNodeName(String nodeName) {
        //Specifically calling nodeName filter here instead of this classes method to stop pollution of thread local
        return (nodeNameFilter.filterByNodeName(nodeName)).isEmpty();
    }

    public DataJobSummary getDataJobSummaryByRAppId(String dataJobRAppId) {
        return dataJobSummaryMap.get(dataJobRAppId);
    }

    public List<String> getAllDataJobSummaryRAppIds() {
        return new ArrayList<>(dataJobSummaryMap.keySet());
    }

    private void incrementActiveGauge(DataJobSummary dataJobSummary) {
        if(dataJobSummary.getInterfaceType().equals(InterfaceType.NON_R1)){
            activeSubscriptionsGauge.increment();
        } else if (dataJobSummary.getInterfaceType().equals(InterfaceType.R1)){
            activeDataJobsGauge.increment();
        }
    }

    private void decrementActiveGauge(DataJobSummary dataJobSummary) {
        if(dataJobSummary.getInterfaceType().equals(InterfaceType.NON_R1)){
            activeSubscriptionsGauge.decrement();
        } else if (dataJobSummary.getInterfaceType().equals(InterfaceType.R1)){
            activeDataJobsGauge.decrement();
        }
    }
}

