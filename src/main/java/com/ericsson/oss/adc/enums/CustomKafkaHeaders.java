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
package com.ericsson.oss.adc.enums;

/**
 * The supported KafkaHeader sent per message.
 */

public final class CustomKafkaHeaders {

    /**
     * Represents the ID of the event, a {@link Long} value
     */
    public static final String EVENT_ID = "event_id";

    private CustomKafkaHeaders() {
        // intentionally private, no need to create instance
    }
}
