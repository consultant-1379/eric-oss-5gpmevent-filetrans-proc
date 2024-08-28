/*******************************************************************************
 * COPYRIGHT Ericsson 2023
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

package com.ericsson.oss.adc.util;

import com.ericsson.oss.adc.models.DataProviderType;
import com.ericsson.oss.adc.models.DataSpace;
import com.ericsson.oss.adc.models.MessageBus;
import com.ericsson.oss.adc.models.MessageStatusTopic;
import com.ericsson.oss.adc.models.data.catalog.v2.MessageDataTopicV2;
import com.ericsson.oss.adc.models.data.catalog.v2.MessageSchemaV2;

import java.util.Optional;

/**
 * Implementation of utility class to verify the present of objects and sub-objects received from Data Management and Movement (DM&M) Data Catalog service.
 * This generally begins by traversing from the root object and verifying a sub-object and or verifying the presence of a sub-object and its values.
 */
public class MessageSchemaV2Utils {

    private MessageSchemaV2Utils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Checks if there is a {@link DataSpace} with a name by traversing the objects to ensure the object tree exists from {@link MessageSchemaV2} root
     *
     * @param messageSchema the {@link MessageSchemaV2} root object to traverse from.
     * @return true if the {@link DataSpace} name is present, false otherwise.
     */
    public static boolean canGetDataSpaceName(MessageSchemaV2 messageSchema) {
        return Optional.of(messageSchema)
                .map(MessageSchemaV2::getMessageDataTopic)
                .map(MessageDataTopicV2::getDataProviderType)
                .map(DataProviderType::getDataSpace)
                .map(DataSpace::getName)
                .isPresent();
    }

    /**
     * Checks if there is a {@link DataSpace} by name by traversing the objects to ensure the object tree exists from {@link MessageSchemaV2} root
     *
     * @param messageSchema the {@link MessageSchemaV2} root object to traverse from.
     * @param dataSpaceName the {@link String} the data space name to check for.
     * @return true if the {@link DataSpace} specific name is present, false otherwise.
     */
    public static boolean canGetAndCheckDataSpaceName(MessageSchemaV2 messageSchema, String dataSpaceName) {
        return Optional.of(messageSchema)
                .map(MessageSchemaV2::getMessageDataTopic)
                .map(MessageDataTopicV2::getDataProviderType)
                .map(DataProviderType::getDataSpace)
                .map(DataSpace::getName).stream().allMatch(s -> s.equals(dataSpaceName));

    }

    /**
     * Checks if there is a {@link MessageDataTopicV2} with a name (topic name) by traversing the objects to ensure the object tree exists from {@link MessageSchemaV2} root
     *
     * @param messageSchema the {@link MessageSchemaV2} root object to traverse from.
     * @return true if the {@link MessageDataTopicV2} name is present, false otherwise.
     */
    public static boolean canGetTopicName(MessageSchemaV2 messageSchema) {
        return Optional.of(messageSchema)
                .map(MessageSchemaV2::getMessageDataTopic)
                .map(MessageDataTopicV2::getName)
                .isPresent();
    }
    /**
     * Checks if there is a {@link MessageDataTopicV2} by name (topic name) by traversing the objects to ensure the object tree exists from {@link MessageSchemaV2} root
     *
     * @param messageSchema the {@link MessageSchemaV2} root object to traverse from.
     * @param inputTopic {@link String} name name to check for.
     * @return true if the {@link MessageDataTopicV2} name is present, false otherwise.
     */
    public static boolean canGetAndCheckInputTopicName(MessageSchemaV2 messageSchema, String inputTopic) {
        return Optional.of(messageSchema)
                .map(MessageSchemaV2::getMessageDataTopic)
                .map(MessageDataTopicV2::getMessageStatusTopic)
                .map(MessageStatusTopic::getName).isPresent() &&
                messageSchema.getMessageDataTopic().getName().equals(inputTopic);
    }

    /**
     * Checks if there is a {@link DataProviderType} by name (SubSystem Name) by traversing the objects to ensure the object tree exists from {@link MessageSchemaV2} root
     *
     * @param messageSchema the {@link MessageSchemaV2} root object to traverse from.
     * @param subSystemName {@link String} name name to check for.
     * @return true if the {@link DataProviderType} name is present, false otherwise.
     */
    public static boolean canGetAndCheckDataProviderID(MessageSchemaV2 messageSchema, String subSystemName) {
        return Optional.of(messageSchema)
                .map(MessageSchemaV2::getMessageDataTopic)
                .map(MessageDataTopicV2::getDataProviderType)
                .map(DataProviderType::getProviderTypeId).isPresent() &&
                messageSchema.getMessageDataTopic().getDataProviderType().getProviderTypeId().equals(subSystemName);
    }

    /**
     * Checks if there is a {@link com.ericsson.oss.adc.models.MessageBus} with a name by traversing the objects to ensure the object tree exists from {@link MessageSchemaV2} root
     * @param messageSchema the {@link MessageSchemaV2} root object to traverse from.
     * @return true if the {@link MessageBus} name is present, false otherwise.
     */
    public static boolean canGetMessageBusName(MessageSchemaV2 messageSchema) {
        return Optional.of(messageSchema)
                .map(MessageSchemaV2::getMessageDataTopic)
                .map(MessageDataTopicV2::getMessageBus)
                .map(MessageBus::getName)
                .isPresent();
    }

    /**
     * Checks if there is a {@link java.util.ArrayList<String>} with access endpoints by traversing the objects to ensure the object tree exists from {@link MessageSchemaV2} root
     * @param messageSchema the {@link MessageSchemaV2} root object to traverse from.
     * @return true if the {@link java.util.ArrayList<String>} name(s) is present, false otherwise.
     */
    public static boolean canGetAccessEndPoints(MessageSchemaV2 messageSchema) {
        return Optional.of(messageSchema)
                .map(MessageSchemaV2::getMessageDataTopic)
                .map(MessageDataTopicV2::getMessageBus)
                .map(MessageBus::getAccessEndpoints)
                .isPresent();
    }
}


