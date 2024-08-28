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
package com.ericsson.oss.adc.config.kafka.record.filter;

import com.ericsson.oss.adc.models.InputMessage;
import com.ericsson.oss.adc.service.subscription.filter.SubscriptionCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class SubscriptionRecordFilterStrategy extends NullRecordFilter implements RecordFilterStrategy<String, InputMessage> {

    private final SubscriptionCache subscriptionCache;

    @Autowired
    public SubscriptionRecordFilterStrategy(final SubscriptionCache subscriptionCache) {
        this.subscriptionCache = subscriptionCache;
    }

    @Override
    public boolean filter(@NotNull final ConsumerRecord<String, InputMessage> consumerRecord) {

        // The record can be populated but with null value due to deserialization failure
        if (filterNullInputMessages(consumerRecord)) {
            return true;
        }
        return filterNodeNameSubscriptions(consumerRecord);
    }

    @NotNull
    @Override
    public List<ConsumerRecord<String, InputMessage>> filterBatch(@NotNull final List<ConsumerRecord<String, InputMessage>> consumerRecords) {
        return RecordFilterStrategy.super.filterBatch(consumerRecords);
    }

    private boolean filterNodeNameSubscriptions(final ConsumerRecord<String, InputMessage> consumerRecord) {
        return subscriptionCache.filterKafkaRecordNodeName(consumerRecord.key());
    }
}
