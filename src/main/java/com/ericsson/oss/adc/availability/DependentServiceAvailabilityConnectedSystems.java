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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import static com.ericsson.oss.adc.util.StartupUtil.isOkResponseCode;

@Service
public class DependentServiceAvailabilityConnectedSystems extends DependentServiceAvailability {

    private static final Logger LOG = LoggerFactory.getLogger(DependentServiceAvailabilityConnectedSystems.class);
    private static final String HEALTH_PATH = "actuator/health";

    @Value("${connected.systems.base-url}${connected.systems.port}/")
    private String connectedSystemsUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    boolean isServiceAvailable() throws UnsatisfiedExternalDependencyException {
        LOG.info("Checking if Connected Systems is reachable, GET request on: '{}'", connectedSystemsUrl);
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<Object> entity = new HttpEntity<>(headers);

        try {
            final ResponseEntity<String> response = restTemplate.exchange(
                    connectedSystemsUrl + HEALTH_PATH, HttpMethod.GET, entity, String.class);
            return isOkResponseCode(response);
        } catch (Exception e) {
            LOG.error("Connected Systems Unreachable, GET request on: '{}'", connectedSystemsUrl);
            throw new UnsatisfiedExternalDependencyException("Connected Systems Unreachable", e);
        }
    }
}
