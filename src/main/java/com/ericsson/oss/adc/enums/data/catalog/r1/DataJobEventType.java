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

package com.ericsson.oss.adc.enums.data.catalog.r1;


import com.fasterxml.jackson.annotation.JsonProperty;

public enum DataJobEventType {
    @JsonProperty("dataJobCreated")
    DATA_JOB_CREATED,
    @JsonProperty("dataJobUpdated")
    DATA_JOB_UPDATED,
    @JsonProperty("dataJobDeleted")
    DATA_JOB_DELETED,
}
