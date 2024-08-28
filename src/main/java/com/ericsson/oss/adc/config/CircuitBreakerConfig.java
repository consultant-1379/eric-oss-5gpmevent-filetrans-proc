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

package com.ericsson.oss.adc.config;

import com.ericsson.oss.adc.availability.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CircuitBreakerConfig {

    private int retryInterval;
    private int retryAttempts;
    private int circuitBreakerRetryAttempts;
    private int circuitBreakerResetTimeOut;
    private int circuitBreakerOpenTimeout;
    private int circuitBreakerBackoff;

    @Value("${scriptingVM.availability.retry-interval}")
    private int scriptingVMRetryInterval;

    @Value("${scriptingVM.availability.retry-attempts}")
    private int scriptingVMRetryAttempts;

    @Value("${scriptingVM.availability.circuit-breaker-retry-attempts}")
    private int scriptingVMCircuitBreakerRetryAttempts;

    @Value("${scriptingVM.availability.circuit-breaker-reset-timeout}")
    private int scriptingVMCircuitBreakerResetTimeOut;

    @Value("${scriptingVM.availability.circuit-breaker-open-timeout}")
    private int scriptingVMCircuitBreakerOpenTimeout;

    @Value("${scriptingVM.availability.circuit-breaker-backoff}")
    private int scriptingVMCircuitBreakerBackoff;

    @Value("${connected.systems.availability.retry-interval}")
    private int csRetryInterval;

    @Value("${connected.systems.availability.retry-attempts}")
    private int csRetryAttempts;

    @Value("${connected.systems.availability.circuit-breaker-retry-attempts}")
    private int csCircuitBreakerRetryAttempts;

    @Value("${connected.systems.availability.circuit-breaker-reset-timeout}")
    private int csCircuitBreakerResetTimeOut;

    @Value("${connected.systems.availability.circuit-breaker-open-timeout}")
    private int csCircuitBreakerOpenTimeout;

    @Value("${connected.systems.availability.circuit-breaker-backoff}")
    private int csCircuitBreakerBackoff;

    @Value("${dmm.data-catalog.availability.retry-interval}")
    private int dcRetryInterval;

    @Value("${dmm.data-catalog.availability.retry-attempts}")
    private int dcRetryAttempts;

    @Value("${dmm.data-catalog.availability.circuit-breaker-retry-attempts}")
    private int dcCircuitBreakerRetryAttempts;

    @Value("${dmm.data-catalog.availability.circuit-breaker-reset-timeout}")
    private int dcCircuitBreakerResetTimeOut;

    @Value("${dmm.data-catalog.availability.circuit-breaker-open-timeout}")
    private int dcCircuitBreakerOpenTimeout;

    @Value("${dmm.data-catalog.availability.circuit-breaker-backoff}")
    private int dcCircuitBreakerBackoff;

    @Value("${spring.kafka.availability.retry-interval}")
    private int kafkaRetryInterval;

    @Value("${spring.kafka.availability.retry-attempts}")
    private int kafkaRetryAttempts;

    public void initialize(Class<? extends DependentServiceAvailability> className) {
        if (className.equals(DependentServiceAvailabilityConnectedSystems.class)) {
            retryInterval = csRetryInterval;
            retryAttempts = csRetryAttempts;
            circuitBreakerRetryAttempts = csCircuitBreakerRetryAttempts;
            circuitBreakerResetTimeOut = csCircuitBreakerResetTimeOut;
            circuitBreakerOpenTimeout = csCircuitBreakerOpenTimeout;
            circuitBreakerBackoff = csCircuitBreakerBackoff;
        } else if (className.equals(DependentServiceAvailabilityScriptingVm.class)) {
            retryInterval = scriptingVMRetryInterval;
            retryAttempts = scriptingVMRetryAttempts;
            circuitBreakerRetryAttempts = scriptingVMCircuitBreakerRetryAttempts;
            circuitBreakerResetTimeOut = scriptingVMCircuitBreakerResetTimeOut;
            circuitBreakerOpenTimeout = scriptingVMCircuitBreakerOpenTimeout;
            circuitBreakerBackoff = scriptingVMCircuitBreakerBackoff;
        } else if (className.equals(DependentServiceAvailabilityDataCatalog.class)) {
            retryInterval = dcRetryInterval;
            retryAttempts = dcRetryAttempts;
            circuitBreakerRetryAttempts = dcCircuitBreakerRetryAttempts;
            circuitBreakerResetTimeOut = dcCircuitBreakerResetTimeOut;
            circuitBreakerOpenTimeout = dcCircuitBreakerOpenTimeout;
            circuitBreakerBackoff = dcCircuitBreakerBackoff;
        } else if (className.equals(DependentServiceAvailabilityKafka.class)) {
            retryInterval = kafkaRetryInterval;
            retryAttempts = kafkaRetryAttempts;
        }
    }

    public int getRetryInterval() {
        return retryInterval;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public int getCircuitBreakerRetryAttempts() {
        return circuitBreakerRetryAttempts;
    }

    public int getCircuitBreakerResetTimeOut() {
        return circuitBreakerResetTimeOut;
    }

    public int getCircuitBreakerOpenTimeout() {
        return circuitBreakerOpenTimeout;
    }

    public int getCircuitBreakerBackoff() {
        return circuitBreakerBackoff;
    }
}
