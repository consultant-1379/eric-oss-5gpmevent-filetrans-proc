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

package com.ericsson.oss.adc.config.regulation;

import com.ericsson.oss.adc.enums.EventType;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * Class responsible for parsing list of eventID to ownership type mappings, standardized vs non-standard, common and pi containing
 * Application will fail if this list is unparseable or not available
 */
@ConfigurationProperties(prefix = "event-mapping")
@Configuration
@Setter
public class EventRegulation {

    private Map<String, EventType> eventRegulationMap;

    private final List<String> commonEventIDS = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13");

    private final List<String> piContainingEventIDS = List.of("3014", "3080", "3078", "3029", "3100", "3031",
            "3025", "3038", "3023", "3090", "3041", "3019", "3046", "3042", "3231", "3230");

    @Accessors(fluent = true)
    @Getter
    @Value("${eventRegulation.produceNonStandard}")
    private boolean produceNonStandard;


    //Any PM Events that are not standardized or non-standard are to be dropped, e.g Common PM Events
    public EventType getEventPrivacy(String eventID) {
        return eventRegulationMap.getOrDefault(eventID, EventType.NOT_FOUND);
    }

    /**
     * Creates static mapping of design time known Common and PI Containing Events and maps them to EventType Common and Pi Containing Events respectfully.
     * Any new common event introduced will generate a warning, and it can be updated.
     * Common events do not appear in the radio node xml artifact.
     */
    @PostConstruct
    private void postConstruct() {
        commonEventIDS.forEach(id -> eventRegulationMap.put(id, EventType.COMMON));
        piContainingEventIDS.forEach(id -> eventRegulationMap.put(id, EventType.PI_CONTAINING_EVENT));
    }
}
