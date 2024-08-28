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
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@Slf4j
public abstract class NullRecordFilter {

    /**
     * If the {@link InputMessage} is not deserializable, we will have a null so filter out.
     * @param consumerRecord The {@link ConsumerRecord} to inspect.
     * @return true if null, false otherwise.
     */
    protected boolean filterNullInputMessages(final ConsumerRecord<String, InputMessage> consumerRecord) {
        if(consumerRecord.key() == null || consumerRecord.value() == null ) {
            log.info("Filtering unusable record, usually caused by deserialization failure");
            return true;
        }
        return false;
    }
}
