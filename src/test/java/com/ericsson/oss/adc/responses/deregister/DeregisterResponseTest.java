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
package com.ericsson.oss.adc.responses.deregister;

import com.ericsson.oss.adc.enums.DeregisterStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.Assert.assertEquals;

class DeregisterResponseTest {

    @Test
    @DisplayName("Verify can create Success response")
    public void test_canBuildSuccessResponse() {
        final String successResponse = "Data Service Instance 'dsin' deleted, Data Catalog HTTP Status: 204 NO_CONTENT";
        DeregisterResponse response = DeregisterResponse.build("dsin", DeregisterStatus.SUCCESS, HttpStatus.NO_CONTENT);

        assertEquals(DeregisterStatus.SUCCESS, response.getDeregisterStatus());
        assertEquals(successResponse, response.getMessage());
    }

    @Test
    @DisplayName("Verify can create Failure response")
    public void test_canBuildFailureResponse() {
        final String failureResponse = "FAILED to delete Data Service Instance: 'dsin', Data Catalog HTTP Status: 404 NOT_FOUND";
        DeregisterResponse response = DeregisterResponse.build("dsin", DeregisterStatus.FAILURE, HttpStatus.NOT_FOUND);

        assertEquals(DeregisterStatus.FAILURE, response.getDeregisterStatus());
        assertEquals(failureResponse, response.getMessage());
    }

    @Test
    @DisplayName("Verify can create Exception response")
    public void test_canBuildDefaultResponse() {
        final String exceptionResponse = "'dsin' <No Message>, Data Catalog HTTP Status: 404 NOT_FOUND";
        DeregisterResponse response = DeregisterResponse.build("dsin", DeregisterStatus.EXCEPTION, HttpStatus.NOT_FOUND);

        assertEquals(DeregisterStatus.EXCEPTION, response.getDeregisterStatus());
        assertEquals(exceptionResponse, response.getMessage());
    }
}