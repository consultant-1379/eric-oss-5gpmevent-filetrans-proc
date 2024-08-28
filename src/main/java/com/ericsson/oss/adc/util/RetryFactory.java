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

package com.ericsson.oss.adc.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Class responsible for providing Retry templates of various configurations
 */
@Slf4j
@Component
public class RetryFactory {

    /**
     * Infinitely Retrying template with an exponential backoff with random jitter applied.
     * @return Autowire-able Retry Template
     */
    @Bean
    public RetryTemplate infiniteRetryTemplate() {
        var initialInterval = Duration.of(1, ChronoUnit.SECONDS);
        var multiplier = 3.0;
        var maxInterval = Duration.of(20, ChronoUnit.SECONDS);
        var useJitter = true;

        return RetryTemplate.builder()
                .infiniteRetry()
                .exponentialBackoff(initialInterval, multiplier, maxInterval, useJitter)
                .withListener(new RetryListener() {
                    @Override
                    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                        log.warn("Error encountered: {}. Threw {}th exception, retrying", throwable.toString(),
                                context.getRetryCount());
                        RetryListener.super.onError(context, callback, throwable);
                    }
                })
                .build();
    }
}
