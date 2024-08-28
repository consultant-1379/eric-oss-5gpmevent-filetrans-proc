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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

/**
 * Using only mandatory fields for our usecase
 */
@Getter
@Builder
@ToString
@Jacksonized
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataJob {
    private String id;
    private String name;
    private String clientId;
    private String dataDelivery;
    private String dataDeliveryMechanism;
    private String dataDeliverySchemaId; //5G.PmEventOuterClass.PmEvent.pmevent
    private ProductionJobDefinition productionJobDefinition;
    private R1DataType dataType;
}
