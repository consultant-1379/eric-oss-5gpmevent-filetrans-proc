/*******************************************************************************
 * COPYRIGHT Ericsson 2021
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

import com.ericsson.oss.adc.enums.EventType;
import com.ericsson.pm_event.PmEventOuterClass;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DecodedEvent {
    //Default value required for valid event check, do not remove
    private String eventID = "";
    private PmEventOuterClass.PmEvent pmEvent;
    private EventType eventType;
}
