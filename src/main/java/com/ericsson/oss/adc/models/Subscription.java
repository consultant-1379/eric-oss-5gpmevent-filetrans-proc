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

package com.ericsson.oss.adc.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@AllArgsConstructor
@Slf4j
public class Subscription {
    private int id;
    private Ids ids;
    private String name;
    private String status;
    private String predicates;
    @JsonProperty("isMandatory")
    private boolean isMandatory;

    //TODO This method is required as predicates is modeled as a String in the Response Object of /catalog/v2/subscriptions
    // we can remove this code when the end point is fixed as captured in this technical debt https://jira-oss.seli.wh.rnd.internal.ericsson.com/browse/IDUN-87639
    public Predicates getPredicates() {
        try {
            return new ObjectMapper().readValue(this.predicates, Predicates.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse predicates to json: {}", e.getMessage());
            return null;
        }
    }
}
