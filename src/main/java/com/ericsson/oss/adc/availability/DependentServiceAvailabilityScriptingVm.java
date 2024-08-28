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

import com.ericsson.oss.adc.config.EventFileDownloadConfiguration;
import com.ericsson.oss.adc.service.sftp.ENMScriptingVMLoadBalancer;
import com.ericsson.oss.adc.service.sftp.SFTPFileTransferService;
import com.ericsson.oss.adc.service.sftp.SFTPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DependentServiceAvailabilityScriptingVm extends DependentServiceAvailability {

    private static final Logger LOG = LoggerFactory.getLogger(DependentServiceAvailabilityScriptingVm.class);

    @Autowired
    private SFTPService sftpService;

    @Autowired
    private EventFileDownloadConfiguration eventFileDownloadConfiguration;

    @Override
    boolean isServiceAvailable() throws UnsatisfiedExternalDependencyException {
        ENMScriptingVMLoadBalancer loadBalancer = new ENMScriptingVMLoadBalancer();
        SFTPFileTransferService sftpFileTransferService = new SFTPFileTransferService(loadBalancer, eventFileDownloadConfiguration);
        boolean sftpConnectionIsEstablished = sftpService.setUpConnectionToSFTPServerUsingConnectedSystemsResponse(sftpFileTransferService, loadBalancer);

        if (!sftpConnectionIsEstablished) {
            LOG.error("SFTP service is not reachable");
            throw new UnsatisfiedExternalDependencyException("SFTP service is not reachable");
        }
        sftpFileTransferService.disconnectChannel();
        LOG.info("SFTP service is reachable");
        return true;
    }
}