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
package com.ericsson.oss.adc.models.connected.systems;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Builder
@Getter
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectionProperties {
    private Long id;
    private Long subsystemId;
    private String name;
    private String tenant;
    @NonNull
    private String username;
    @NonNull
    private String password;
    @NonNull
    private String scriptingVMs;
    private int sftpPort;
    private List<String> encryptedKeys;
    private List<SubsystemUsers> subsystemUsers;

    public List<String> getScriptingVMs() {
        final String delimiter = ",";
        if(scriptingVMs == null){
            return Collections.emptyList();
        }
        return Arrays.asList(scriptingVMs.split(delimiter));
    }
}
