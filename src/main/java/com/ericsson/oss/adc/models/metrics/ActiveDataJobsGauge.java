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

package com.ericsson.oss.adc.models.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ActiveDataJobsGauge {
    private final AtomicInteger gauge;

    public ActiveDataJobsGauge(MeterRegistry meterRegistry) {
        this.gauge = meterRegistry.gauge("eric.oss.5gpmevt.filetx.proc:active.data.jobs", new AtomicInteger());
    }

    public void increment() {
        gauge.incrementAndGet();
    }

    public void decrement() {
        gauge.decrementAndGet();
    }
}
