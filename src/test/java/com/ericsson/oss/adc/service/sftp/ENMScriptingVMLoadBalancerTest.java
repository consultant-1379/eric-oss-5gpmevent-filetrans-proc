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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {ENMScriptingVMLoadBalancer.class, ENMScriptingVMLoadBalancerTest.class})
class ENMScriptingVMLoadBalancerTest {

    private static final String SCRIPTING_VM1 = "vm1";
    private static final String SCRIPTING_VM2 = "vm2";
    private List<String> vmList;

    private ENMScriptingVMLoadBalancer enmScriptingVMLoadBalancer;

    @BeforeEach
    void init() {
        vmList = Arrays.asList(SCRIPTING_VM1, SCRIPTING_VM2);
        enmScriptingVMLoadBalancer = new ENMScriptingVMLoadBalancer();
    }

    @Test
    @DisplayName("Verify getting empty list of available scripting VMs at the start.")
    void verifyGetAllAvailableScriptingVmsEmpty() {
        assertEquals(0, enmScriptingVMLoadBalancer.getAllAvailableScriptingVMs().size());
    }

    @Test
    @DisplayName("Verify getting list of two available scripting VMs after setting list of two.")
    void verifyGetAllAvailableScriptingVmsNonEmpty() {
        assertEquals(0, enmScriptingVMLoadBalancer.getAllAvailableScriptingVMs().size());
        enmScriptingVMLoadBalancer.setScriptingVMs(vmList);
        assertEquals(2, enmScriptingVMLoadBalancer.getAllAvailableScriptingVMs().size());
    }

    @Test
    @DisplayName("Verify getting empty list of online scripting VMs at the start.")
    void verifyGetAllOnlineScriptingVmsEmpty() {
        assertEquals(0, enmScriptingVMLoadBalancer.getAllOnlineScriptingVMs().size());
    }

    @Test
    @DisplayName("Verify getting list of two online scripting VMs after setting list of two.")
    void verifyGetAllOnlineScriptingVmsNonEmpty() {
        assertEquals(0, enmScriptingVMLoadBalancer.getAllOnlineScriptingVMs().size());
        enmScriptingVMLoadBalancer.setScriptingVMs(vmList);
        assertEquals(2, enmScriptingVMLoadBalancer.getAllOnlineScriptingVMs().size());
    }

    @Test
    @DisplayName("Verify getting current connect scripting VM to be null at the start.")
    void verifyNullCurrentConnectedScriptingVm() {
        assertNull(enmScriptingVMLoadBalancer.getCurrentConnectedScriptingVM());
    }

    @Test
    @DisplayName("Verify getting current connect scripting VM to be not null when set.")
    void verifyNotNullCurrentConnectedScriptingVm() {
        enmScriptingVMLoadBalancer.setScriptingVMs(vmList);
        enmScriptingVMLoadBalancer.getNextScriptingVM();
        assertNotNull(enmScriptingVMLoadBalancer.getCurrentConnectedScriptingVM());
        assertTrue(vmList.contains(enmScriptingVMLoadBalancer.getCurrentConnectedScriptingVM()));
    }

    @Test
    @DisplayName("Verify setting all scripting VMs.")
    void verifySetScriptingVms() {
        assertEquals(0, enmScriptingVMLoadBalancer.getAllAvailableScriptingVMs().size());
        assertEquals(0, enmScriptingVMLoadBalancer.getAllOnlineScriptingVMs().size());
        enmScriptingVMLoadBalancer.setScriptingVMs(vmList);
        assertEquals(2, enmScriptingVMLoadBalancer.getAllAvailableScriptingVMs().size());
        assertEquals(2, enmScriptingVMLoadBalancer.getAllOnlineScriptingVMs().size());
    }

    @Test
    @DisplayName("Verify removing an online available scripting VM from a list of two scripting VMs.")
    void verifyRemovingVmFromOnlineAvailableScriptingVms() {
        assertEquals(0, enmScriptingVMLoadBalancer.getAllAvailableScriptingVMs().size());
        enmScriptingVMLoadBalancer.setScriptingVMs(vmList);
        assertEquals(2, enmScriptingVMLoadBalancer.getAllAvailableScriptingVMs().size());
        assertTrue(enmScriptingVMLoadBalancer.getAllAvailableScriptingVMs().contains(SCRIPTING_VM1));

        final List<String> list = enmScriptingVMLoadBalancer.removeVMFromOnlineScriptingVMs(SCRIPTING_VM1);
        assertEquals(1, list.size());
        assertFalse(list.contains(SCRIPTING_VM1));
    }

    @Test
    @DisplayName("Verify resetting all online scripting VMs.")
    void verifyResetAllOnlineScriptingVms() {
        assertEquals(0, enmScriptingVMLoadBalancer.getAllAvailableScriptingVMs().size());
        assertEquals(0, enmScriptingVMLoadBalancer.getAllOnlineScriptingVMs().size());
        enmScriptingVMLoadBalancer.setScriptingVMs(vmList);
        assertEquals(2, enmScriptingVMLoadBalancer.getAllAvailableScriptingVMs().size());
        assertEquals(2, enmScriptingVMLoadBalancer.getAllOnlineScriptingVMs().size());

        assertTrue(enmScriptingVMLoadBalancer.getAllAvailableScriptingVMs().contains(SCRIPTING_VM1));
        enmScriptingVMLoadBalancer.removeVMFromOnlineScriptingVMs(SCRIPTING_VM1);

        assertEquals(1, enmScriptingVMLoadBalancer.getAllOnlineScriptingVMs().size());
        assertFalse(enmScriptingVMLoadBalancer.getAllOnlineScriptingVMs().contains(SCRIPTING_VM1));

        enmScriptingVMLoadBalancer.resetAllOnlineScriptingVMs();

        assertEquals(2, enmScriptingVMLoadBalancer.getAllOnlineScriptingVMs().size());
        assertTrue(enmScriptingVMLoadBalancer.getAllOnlineScriptingVMs().contains(SCRIPTING_VM1));
        assertTrue(enmScriptingVMLoadBalancer.getAllOnlineScriptingVMs().contains(SCRIPTING_VM2));
    }

    @Test
    @DisplayName("Verify loadBalancer selects random VM then proceeds to round robin")
    void verifyRoundRobinLoadBalancesSuccessful() {
        assertEquals(0, enmScriptingVMLoadBalancer.getAllAvailableScriptingVMs().size());
        assertEquals(0, enmScriptingVMLoadBalancer.getAllOnlineScriptingVMs().size());
        enmScriptingVMLoadBalancer.setScriptingVMs(vmList);

        //get initial scripting vm
        String firstChosenVM = enmScriptingVMLoadBalancer.getNextScriptingVM();
        int index = vmList.indexOf(firstChosenVM);
        String expectedNextVM = vmList.get((index+1) % vmList.size());

        assertEquals(expectedNextVM, enmScriptingVMLoadBalancer.getNextScriptingVM());
    }

    @Test
    @DisplayName("Verify that after removing a Scripting VM the correct VM is chosen")
    void verifyRoundRobinLoadBalancesAfterRemoval() {
        assertEquals(0, enmScriptingVMLoadBalancer.getAllAvailableScriptingVMs().size());
        assertEquals(0, enmScriptingVMLoadBalancer.getAllOnlineScriptingVMs().size());
        enmScriptingVMLoadBalancer.setScriptingVMs(vmList);
        enmScriptingVMLoadBalancer.getNextScriptingVM();
        enmScriptingVMLoadBalancer.resetAllOnlineScriptingVMs();
        enmScriptingVMLoadBalancer.removeVMFromOnlineScriptingVMs(SCRIPTING_VM2);

        assertEquals(SCRIPTING_VM1, enmScriptingVMLoadBalancer.getNextScriptingVM());
    }

}
