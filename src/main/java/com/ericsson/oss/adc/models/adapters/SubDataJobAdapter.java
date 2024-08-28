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
import com.ericsson.oss.adc.models.data.catalog.r1.DataJobSummary;
import com.ericsson.oss.adc.models.data.catalog.r1.DataJob;
import com.ericsson.oss.adc.models.data.catalog.r1.InterfaceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Responsible for converting both Subscriptions and DataJobs to a common representation that can be used by SubscriptionCache for filtering
 */
@Slf4j
@Component
public class SubDataJobAdapter {


    public DataJobSummary convertSubscription(Subscription subscription) {
        return DataJobSummary.builder()
                .rAppID(subscription.getIds().getRAppId())
                .dataDeliverySchemaId(DataCatalogConstants.SCHEMA_NAME_STANDARDIZED) //TODO parse from subscription when Catalog exposes API to retrieve schema requested.
                .nodeNames(subscription.getPredicates().getNodeName())
                .eventId(subscription.getPredicates().getEventId())
                .interfaceType(InterfaceType.NON_R1)
                .build();

    }

    public DataJobSummary convertDataJob(DataJob dataJob) {
        return DataJobSummary.builder()
                .rAppID(dataJob.getClientId())
                .dataDeliverySchemaId(dataJob.getDataDeliverySchemaId())
                .nodeNames(dataJob.getProductionJobDefinition().getTargetSelector().getNodeNameList())
                .eventId(dataJob.getProductionJobDefinition().getDataSelector().getEventId())
                .interfaceType(InterfaceType.R1)
                .build();
    }
}
