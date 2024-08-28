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
package com.ericsson.oss.adc.service.sftp;

import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Component
@ToString
public class ENMScriptingVMLoadBalancer {

    private static final Logger LOG = LoggerFactory.getLogger(ENMScriptingVMLoadBalancer.class);

    private List<String> allAvailableScriptingVMs =  new ArrayList<>();

    private List<String> onlineAvailableScriptingVMs =  new ArrayList<>();

    private String currentHost;

    /**
     * Get list of all available scripting VMs.
     *
     * @return List of Strings of all available scripting VMs.
     */
    public List<String> getAllAvailableScriptingVMs() {
        return new ArrayList<>(allAvailableScriptingVMs);
    }

    /**
     * Get list of all online scripting VMs.
     *
     * @return List of Strings of all online scripting VMs.
     */
    public List<String> getAllOnlineScriptingVMs() {
        return new ArrayList<>(onlineAvailableScriptingVMs);
    }

    /**
     * Get current connected scripting VM.
     *
     * @return String representing the current connected scripting VM.
     */
    public String getCurrentConnectedScriptingVM() {
        return currentHost;
    }

    /**
     * Get the next Scripting VM to be used in a round-robin fashion
     *
     * @return String representing next Scripting VM to be used
     */
    public String getNextScriptingVM() {
        if(currentHost == null){
            final SecureRandom rand = new SecureRandom();
            int randomIndex = rand.nextInt(onlineAvailableScriptingVMs.size());
            currentHost = onlineAvailableScriptingVMs.get(randomIndex);
        } else {
            int index = onlineAvailableScriptingVMs.indexOf(currentHost);
            currentHost = onlineAvailableScriptingVMs.get((index+1) % onlineAvailableScriptingVMs.size());
        }
        LOG.debug("Next Scripting VM is: '{}'", currentHost);
        return currentHost;
    }

    /**
     * Set all available and online available scripting VMs.
     *
     * @param scriptingVmList List of Strings of scripting VMs.
     */
    public void setScriptingVMs(final List<String> scriptingVmList) {
        LOG.debug("Setting scripting VMs to provided list");
        allAvailableScriptingVMs = new ArrayList<>(scriptingVmList);
        onlineAvailableScriptingVMs =  new ArrayList<>(scriptingVmList);
    }

    /**
     * Reset all online available scripting VMs to be all available scripting VMs.
     */
    public void resetAllOnlineScriptingVMs() {
        LOG.debug("Resetting all online scripting VMs to be all available scripting VMs");
        onlineAvailableScriptingVMs =  new ArrayList<>(allAvailableScriptingVMs);
    }

    /**
     * Remove VM from online available scripting VMs.
     *
     * @param scriptingVM String of scripting VM to remove.
     * @return List of String of new online available scripting VMs.
     */
    public List<String> removeVMFromOnlineScriptingVMs(final String scriptingVM) {
        LOG.debug("Removing scripting VM '{}' from all online scripting VMs.", scriptingVM);
        onlineAvailableScriptingVMs.remove(scriptingVM);
        return new ArrayList<>(onlineAvailableScriptingVMs);
    }
}
