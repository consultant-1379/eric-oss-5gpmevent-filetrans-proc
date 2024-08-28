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

package com.ericsson.oss.adc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@Component
public class EventFileDownloadConfiguration {

    @Value("${eventFileDownload.numberOfSftpConnectRetries}")
    private int numberOfSftpConnectRetries;

    @Value("${eventFileDownload.numberOfEventFileDownloadRetries}")
    private int numberOfEventFileDownloadRetries;

    @Value("${eventFileDownload.sftpSessionTimeoutMs}")
    private int sftpSessionTimeoutMs;

    @Value("${eventFileDownload.sftpConnectBackoffMs}")
    private int sftpConnectBackoffMs;

    public int getNumberOfSftpConnectRetries() {
        return numberOfSftpConnectRetries;
    }

    public int getNumberOfEventFileDownloadRetries() {
        return numberOfEventFileDownloadRetries;
    }

    public int getSftpSessionTimeoutMs() {
        return sftpSessionTimeoutMs;
    }

    public int getSftpConnectBackoffMs() {
        return sftpConnectBackoffMs;
    }
}
