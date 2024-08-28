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

package com.ericsson.oss.adc.availability;

import com.ericsson.oss.adc.config.CircuitBreakerConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;


import static com.ericsson.oss.adc.util.StartupUtil.buildCircuitBreakerRetry;
import static com.ericsson.oss.adc.util.StartupUtil.getRetryListener;

public abstract class DependentServiceAvailability {

    private static final Logger LOG = LoggerFactory.getLogger(DependentServiceAvailability.class);

    @Autowired
    CircuitBreakerConfig circuitBreakerConfig;

    @PostConstruct
    void initializeConfig() {
        circuitBreakerConfig.initialize(this.getClass());
    }

    /**
     * Checks if service is available in a retry until successful.
     *
     * @return false if all retries are exhausted, true if its up.
     */
    public boolean checkServiceWithCircuitBreaker() throws UnsatisfiedExternalDependencyException {
        final RetryTemplate infiniteRetry = getRetryTemplate();
        final RetryTemplate circuitBreakerRetryTemplate = buildCircuitBreakerRetry(circuitBreakerConfig.getCircuitBreakerRetryAttempts(),
                circuitBreakerConfig.getCircuitBreakerResetTimeOut(), circuitBreakerConfig.getCircuitBreakerOpenTimeout(), circuitBreakerConfig.getCircuitBreakerBackoff());
        return infiniteRetry.execute(infiniteRetryContext -> circuitBreakerRetryTemplate.execute(circuitBreakerRetryContext
                -> isServiceAvailable()));
    }

    /**
     * Check if service can be reached
     *
     * @return true once service is reached, false if max retries exhausted
     */
    public boolean checkService() {
        RetryTemplate retryTemplate = getRetryTemplate();
        try {
            return retryTemplate.execute(retryContext -> isServiceAvailable());
        } catch (UnsatisfiedExternalDependencyException e) {
            LOG.error("Hit max retry attempts {}", circuitBreakerConfig.getRetryAttempts(), e);
        }
        return false; // exhausted retries
    }

    private RetryTemplate getRetryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(circuitBreakerConfig.getRetryAttempts())
                .fixedBackoff(circuitBreakerConfig.getRetryInterval())
                .retryOn(UnsatisfiedExternalDependencyException.class)
                .withListener(getRetryListener())
                .build();
    }

    abstract boolean isServiceAvailable() throws UnsatisfiedExternalDependencyException;
}
