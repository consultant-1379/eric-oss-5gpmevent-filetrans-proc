/*******************************************************************************
 * COPYRIGHT Ericsson 2022
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
package com.ericsson.oss.adc.controller.topiclistener;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.File;

/**
 * Class for test serialization of messages put on topic with JSON Serializer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter(AccessLevel.PUBLIC)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileNotificationDTO {
    private String nodeName;
    private String nodeType;
    private String fileLocation;
    private String dataType;
    private File downloadedFile;
}
