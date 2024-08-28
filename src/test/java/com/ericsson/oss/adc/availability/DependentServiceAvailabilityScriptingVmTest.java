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
import com.ericsson.oss.adc.config.EventFileDownloadConfiguration;
import com.ericsson.oss.adc.service.sftp.SFTPService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {EventFileDownloadConfiguration.class, DependentServiceAvailabilityScriptingVm.class,
        CircuitBreakerConfig.class})
public class DependentServiceAvailabilityScriptingVmTest {

    @MockBean
    private SFTPService sftpServiceMock;

    @Autowired
    DependentServiceAvailabilityScriptingVm dependentServiceAvailabilityScriptingVm;

    @Test
    @DisplayName("Check if Scripting VM is reachable")
    void test_scripting_vm_is_reachable() {
        Mockito.when(sftpServiceMock.setUpConnectionToSFTPServerUsingConnectedSystemsResponse(any(), any())).thenReturn(true);
        assertDoesNotThrow(() -> dependentServiceAvailabilityScriptingVm.checkServiceWithCircuitBreaker());
    }

    @Test
    @DisplayName("Check if Scripting VM is initially unreachable")
    void test_scripting_vm_is_initially_unreachable() throws UnsatisfiedExternalDependencyException {
        Mockito.when(sftpServiceMock.setUpConnectionToSFTPServerUsingConnectedSystemsResponse(any(), any())).
                thenReturn(false).thenReturn(false).thenReturn(true);
        dependentServiceAvailabilityScriptingVm.checkServiceWithCircuitBreaker();
        verify(sftpServiceMock, times(3)).setUpConnectionToSFTPServerUsingConnectedSystemsResponse(any(), any());
    }
}
