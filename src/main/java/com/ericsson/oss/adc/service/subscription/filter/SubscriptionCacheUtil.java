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
import com.ericsson.oss.adc.models.data.catalog.r1.DataJobSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import static java.util.Objects.nonNull;

@Slf4j
@Component
public class SubscriptionCacheUtil {

    private final SubscriptionCache subscriptionCacheImpl;
    private final SubDataJobAdapter subDataJobAdapter;

    public SubscriptionCacheUtil(SubscriptionCache subscriptionCache, SubDataJobAdapter subDataJobAdapter) {
        this.subscriptionCacheImpl = subscriptionCache;
        this.subDataJobAdapter = subDataJobAdapter;
    }

    public void reconstituteSubscriptionCache(List<Subscription> newSubscriptions) {
        List<DataJobSummary> newDataJobSummaries = new ArrayList<>(newSubscriptions.size());
        for (Subscription subscription : newSubscriptions) {
            DataJobSummary dataJobSummary = subDataJobAdapter.convertSubscription(subscription);
            newDataJobSummaries.add(dataJobSummary);
        }
        updateSubscriptionCache(newDataJobSummaries);
        removeInActiveDataJobs(newDataJobSummaries);
    }

    private void updateSubscriptionCache(List<DataJobSummary> dataJobSummaries) {
        for (DataJobSummary dataJobSummary : dataJobSummaries) {
            String newDataJobSummaryRAppID = dataJobSummary.getRAppID();
            if(nonNull(subscriptionCacheImpl.getDataJobSummaryByRAppId(newDataJobSummaryRAppID))) {
                log.info("Updating subscription cache for DataJob: {}", newDataJobSummaryRAppID);
                subscriptionCacheImpl.updateDataJobSummary(dataJobSummary);
            } else {
                log.info("Adding DataJob named {} to the cache", newDataJobSummaryRAppID);
                subscriptionCacheImpl.updateDataJobSummary(dataJobSummary);
            }
        }
    }

    private void removeInActiveDataJobs(List<DataJobSummary> dataJobSummaries) {
        List<DataJobSummary> inActiveDataJobs = getInActiveDataJobs(dataJobSummaries);
        if (!inActiveDataJobs.isEmpty()) {
            for (DataJobSummary inActiveDataJob : inActiveDataJobs) {
                log.info("Removing DataJob named {} from the cache", inActiveDataJob);
                subscriptionCacheImpl.removeDataJobSummary(inActiveDataJob);
            }
        }
    }

    private List<DataJobSummary> getInActiveDataJobs(List<DataJobSummary> dataJobSummaries) {
        List<DataJobSummary> inActiveDataJobSummaries = new ArrayList<>();
        List<String> oldSubscriptionRAppIds = subscriptionCacheImpl.getAllDataJobSummaryRAppIds();
        for (String oldSubscriptionRAppId : oldSubscriptionRAppIds) {
            if (!getNewDataJobNames(dataJobSummaries).contains(oldSubscriptionRAppId)) {
                inActiveDataJobSummaries.add(subscriptionCacheImpl.getDataJobSummaryByRAppId(oldSubscriptionRAppId));
            }
        }
        return inActiveDataJobSummaries;
    }

    private List<String> getNewDataJobNames(List<DataJobSummary> newDataJobSummaries) {
        List<String> newDataJobSummaryRAppIds = new ArrayList<>();
        for (DataJobSummary newDataJobSummary : newDataJobSummaries) {
            newDataJobSummaryRAppIds.add(newDataJobSummary.getRAppID());
        }
        return newDataJobSummaryRAppIds;
    }
}
