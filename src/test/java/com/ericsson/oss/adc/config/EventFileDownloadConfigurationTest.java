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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {EventFileDownloadConfiguration.class, EventFileDownloadConfigurationTest.class})
public class EventFileDownloadConfigurationTest {

    @Autowired
    EventFileDownloadConfiguration eventFileDownloadConfiguration;

    @Test
    @DisplayName("Should get numberOfSftpConnectRetries")
    public void test_getNumberOfSftpConnectRetries()  {
        assertEquals(3, eventFileDownloadConfiguration.getNumberOfSftpConnectRetries());
    }

    @Test
    @DisplayName("Should get numberOfEventFileDownloadRetries")
    public void test_getNumberOfEventFileDownloadRetries()  {
        assertEquals(3, eventFileDownloadConfiguration.getNumberOfEventFileDownloadRetries());
    }

    @Test
    @DisplayName("Should get sftpSessionTimeoutMs")
    public void test_getSftpSessionTimeoutMs()  {
        assertEquals(15000, eventFileDownloadConfiguration.getSftpSessionTimeoutMs());
    }

    @Test
    @DisplayName("Should get sftpConnectBackoffMs")
    public void test_getSftpConnectBackoffMs()  {
        assertEquals(1000, eventFileDownloadConfiguration.getSftpConnectBackoffMs());
    }
}
