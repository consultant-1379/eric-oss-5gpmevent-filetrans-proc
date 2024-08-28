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

package com.ericsson.oss.adc.kafka_components;

import com.ericsson.oss.adc.enums.EventType;
import com.ericsson.oss.adc.models.DecodedEvent;
import com.ericsson.pm_event.PmEventOuterClass;
import lombok.Getter;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static com.ericsson.oss.adc.enums.CustomKafkaHeaders.EVENT_ID;

/**
 * Kafka consumer for testing purposes
 */
@Profile("E2E")
@Component
@Getter
public class ConsumerTester {

    private static final String STANDARDIZED_TOPIC = "5g-pm-event-file-transfer-and-processing--standardized";
    private static final String NON_STANDARD_TOPIC = "5g-pm-event-file-transfer-and-processing--ericsson";

    private CountDownLatch latch = new CountDownLatch(1);
    private DecodedEvent eventData = null;
    private int recordCount = 0;

    private Set<String> eventsEncountered = new HashSet<>();

    @KafkaListener(containerFactory = "consumerKafkaListenerOutputTestContainerFactory", topics = {STANDARDIZED_TOPIC, NON_STANDARD_TOPIC})
    public void receive(@Payload PmEventOuterClass.PmEvent message,
                        @Header(EVENT_ID) String eventId,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        EventType eventType = switch (topic) {
            case STANDARDIZED_TOPIC -> EventType.STANDARDIZED;
            case NON_STANDARD_TOPIC -> EventType.NON_STANDARD;
            default -> EventType.NOT_FOUND;

        };
        eventData = new DecodedEvent(
                eventId,
                message,
                eventType
        );

        eventsEncountered.add(eventId);
        recordCount++;
        latch.countDown();
    }

    /**
     * Sets countdown latch
     *
     * @param count
     */
    public void setCountDownLatch(int count) {
        latch = new CountDownLatch(count);
    }

    /**
     * Resets consumer variables
     */
    public void reset() {
        latch = new CountDownLatch(1);
        eventData = null;
        recordCount = 0;
        eventsEncountered = new HashSet<>();
    }
}
