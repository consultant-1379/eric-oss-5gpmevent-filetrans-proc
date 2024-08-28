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
package com.ericsson.oss.adc.models.data.catalog.r1;

import com.ericsson.oss.adc.enums.EventType;
import com.ericsson.oss.adc.models.DataCatalogConstants;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@ToString
@Jacksonized
@EqualsAndHashCode
public class DataJobSummary {
    private String rAppID;
    private String dataDeliverySchemaId;
    private InterfaceType interfaceType;
    private EventType eventSubscriptionType;
    @Builder.Default
    private List<String> nodeNames = new ArrayList<>(); // Do not remove instantiation
    @Builder.Default
    private List<String> eventId = new ArrayList<>(); // Do not remove instantiation

    public static class DataJobSummaryBuilder {
        //custom builder setter to set eventType based on deliverySchema
        public DataJobSummaryBuilder dataDeliverySchemaId(String dataDeliverySchemaId) {
            this.dataDeliverySchemaId = dataDeliverySchemaId;
            return eventSubscriptionType(this.dataDeliverySchemaId);
        }

        /**
         * Associates the EventType, NON_STANDARD or STANDARDIZED with the subscription by mapping the requested
         * dataDeliverySchemaID/DataType MessageSchema to the expected EventType.
         * This allows us to check if a subscription of eventID [*] should receive proprietary or standard events.
         *
         * @param dataDeliverySchemaId the Schema requested, containing either PmEventOuterClass.PmEvent or PmEventOuterClass.EricssonPmEvent
         * @return builder with the appropriate EventType set
         */
        private DataJobSummaryBuilder eventSubscriptionType(String dataDeliverySchemaId) {
            //Using contains allows us to check R1 and NonR1 at the same time as R1 *currently* contains the substring of NonR1
            if (DataCatalogConstants.SCHEMA_NAME_STANDARDIZED.toLowerCase().contains(dataDeliverySchemaId.toLowerCase())) {
                this.eventSubscriptionType = EventType.STANDARDIZED;
            } else if (DataCatalogConstants.SCHEMA_NAME_NON_STANDARD.toLowerCase().contains(dataDeliverySchemaId.toLowerCase())) {
                this.eventSubscriptionType = EventType.NON_STANDARD;
            } else {
                this.eventSubscriptionType = EventType.STANDARDIZED;
            }
            return this;
        }
    }

}
