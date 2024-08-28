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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {EventRegulation.class})
@EnableConfigurationProperties
public class EventRegulationTest {

    @Autowired
    private EventRegulation eventRegulation;

    @Test
    @DisplayName("Unknown EventIDs return NOT_FOUND")
    public void test_unknownEventIDsReturnNOT_FOUND() {
        final List<String> unknownEventIDs = List.of("9999", "9998", "9997", "8888", "8887", "8886");
        for (String eventID : unknownEventIDs) {
            assertEquals(EventType.NOT_FOUND, eventRegulation.getEventPrivacy(eventID), "EventIDs not in map (9999,9998,9997,8888,8887,8886) return NOT_FOUND");
        }
    }

    @Test
    @DisplayName("Common EventIDs return COMMON")
    public void test_commonEventIDSReturnCOMMON() {
        final List<String> commonEventIDS = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13");
        for (String eventID : commonEventIDS) {
            assertEquals(EventType.COMMON, eventRegulation.getEventPrivacy(eventID));
        }
    }

    @Test
    @DisplayName("Non Standard EventIDs return NON_STANDARD")
    public void test_nonStandardEventIDsReturn_NON_STANDARD() {
        final List<String> nonStandardEventIDs = List.of("3308", "3114", "2013", "3205", "2052", "3048", "3074");
        for (String eventID : nonStandardEventIDs) {
            assertEquals(EventType.NON_STANDARD, eventRegulation.getEventPrivacy(eventID), "EventIDs in map (3308,3114,2013,3205,2052,3048,3074) return NON_STANDARD");
        }
    }

    @Test
    @DisplayName("Standardized EventIDs return STANDARDIZED")
    public void test_standardizedEventIDsReturn_STANDARDIZED() {
        final List<String> standardizedEventIDs = List.of("2028", "2035", "3030", "2026", "3286", "3028");
        for (String eventID : standardizedEventIDs) {
            assertEquals(EventType.STANDARDIZED, eventRegulation.getEventPrivacy(eventID), "EventIDs in map (2028,2035,3030,2026,3286,3028) return STANDARDIZED");
        }
    }

    @Test
    @DisplayName("Can get produceNonStandard value")
    public void test_produceProprietary() {
        assertTrue(eventRegulation.produceNonStandard(), "produceNonStandard is true");
    }

    @Test
    @DisplayName("PI containing EventIDS, return PI_CONTAINING_EVENT")
    public void test_piContainingEvents(){
        final List<String> piTestEvents = List.of("3014", "3080", "3078", "3029", "3100", "3031",
                "3025", "3038", "3023", "3090", "3041", "3019", "3046", "3042", "3231", "3230");

        for (String eventID : piTestEvents){
            assertEquals(EventType.PI_CONTAINING_EVENT, eventRegulation.getEventPrivacy(eventID));
        }
    }

}
