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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
public class Predicates {

    //If fieldName is null, then jackson skips deserialization, and the default new arraylist value is used.
    @JsonSetter(nulls = Nulls.SKIP)
    private List<String> nodeName = new ArrayList<>();

    @JsonSetter(nulls = Nulls.SKIP)
    private List<String> eventId = new ArrayList<>();
}

