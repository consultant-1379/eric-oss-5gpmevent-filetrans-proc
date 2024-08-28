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
package com.ericsson.oss.adc.service.connected.systems;

import com.ericsson.oss.adc.models.connected.systems.ConnectionProperties;
import com.ericsson.oss.adc.models.connected.systems.Subsystem;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Service
public class ConnectedSystemsService {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectedSystemsService.class);

    @Value("${connected.systems.base-url}${connected.systems.port}${connected.systems.uri}")
    private String connectedSystemsUrl;

    @Value("${spring.kafka.topics.input.prefix}")
    private String inputTopicPrefix;

    @Value("${spring.kafka.topics.input.name}")
    private String inputTopicName;


    @Autowired
    private RestTemplate restTemplate;


    /**
     * Get subsystems details and populate the systemsByNameMap instant attribute and return the Map.
     *
     * @return Map of String to Subsystem retrieved from connected systems.
     */
    public Map<String, Subsystem> getSubsystemDetails() {
        final String requestURL = MessageFormat.format("{0}?name={1}", connectedSystemsUrl, getSubsystemName());
        LOG.info("Requesting subsystem details at url '{}'", requestURL);
        try {
            final Map<String, Subsystem> subsystemsByNameMap =  new HashMap<>();
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            final HttpEntity<Object> entity = new HttpEntity<>(headers);
            final ResponseEntity<Subsystem[]> response = restTemplate.exchange(requestURL, HttpMethod.GET, entity, Subsystem[].class);
            formatResponse(response, subsystemsByNameMap);
            LOG.debug("Successfully executed request {}, response {} ", requestURL, response.getStatusCode().value());
            return subsystemsByNameMap;

        } catch (final Exception exception) {
            LOG.error("Failed to execute {} request: {}", requestURL, exception.getMessage());
            LOG.debug("Stack trace - Failed to execute {} request: ", requestURL, exception);
        }
        return Collections.emptyMap();
    }

    private Map<String, Subsystem> formatResponse(final ResponseEntity<Subsystem[]> response, final Map<String, Subsystem> subsystemsByNameMap) {
        LOG.debug("Formatting response '{}'", response);
        if (response.getStatusCode().value() == Response.Status.OK.getStatusCode()) {
            final List<Subsystem> subsystemList =  Arrays.asList(response.getBody());

            if (!subsystemList.isEmpty()) {
                for (final Subsystem subsystem: subsystemList) {
                    subsystemsByNameMap.put(subsystem.getName(), subsystem);
                }
            }
            return subsystemsByNameMap;
        }
        return Collections.emptyMap();
    }

    /**
     * Get connection properties by subsystems name for the instance subsystem.
     *
     * @param  subsystemsByNameMap - the subsystems map to read the connection properties from
     * @return ConnectionProperties representing the connection properties of the instance subsystem.
     */
    public ConnectionProperties getConnectionPropertiesBySubsystemsName(final Map<String, Subsystem> subsystemsByNameMap) {
        String subsystemName = this.getSubsystemName();

        LOG.debug("Getting connection property by subsystem name '{}'", subsystemName);
        if (subsystemsByNameMap.isEmpty()) {
            return null;
        }
        return subsystemsByNameMap.get(subsystemName).getConnectionProperties().get(0);
    }

    /**
     * @return the subsystem name - ENM instance.
     */
    public String getSubsystemName() {
        return inputTopicName.replace(inputTopicPrefix, "");
    }
}
