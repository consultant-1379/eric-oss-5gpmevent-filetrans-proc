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

import org.junit.jupiter.api.Test;
import org.springframework.retry.support.RetryTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class RetryFactoryTest {

    private final RetryFactory retryFactory = new RetryFactory();

    @Test
    void testRetry() {
        RetryTemplate retryTemplate = retryFactory.infiniteRetryTemplate();

        List<Integer> list = mock(List.class);

        doThrow(RuntimeException.class).doReturn(true).when(list).add(anyInt());

        boolean result = retryTemplate.execute(context -> list.add(1));
        assertTrue(result);
    }

}