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

package com.ericsson.oss.adc.controller.subscription.listener;

import com.ericsson.oss.adc.models.DataCatalogProperties;
import com.ericsson.oss.adc.models.adapters.SubDataJobAdapter;
import com.ericsson.oss.adc.service.subscription.filter.SubscriptionCache;
import com.ericsson.oss.adc.util.StartupUtil;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {SubscriptionInputTopicListenerImplTest.class})
@EnableConfigurationProperties(value = DataCatalogProperties.class)
public class SubscriptionInputTopicListenerImplTest {

    private static final int PARTITION_NUMBER = 0;
    private static final long OFFSET = 1L;
    private static final String KEY = "";

    @Value("${spring.kafka.topics.subscriptionInput.name}")
    private String topicName;

    @Value("${spring.kafka.topics.dataJobTopic.name}")
    private String dataJobTopicName;

    @Autowired
    private DataCatalogProperties dataCatalogProperties;

    @MockBean
    private StartupUtil startupUtil;

    @MockBean
    private SubDataJobAdapter subDataJobAdapter;

    @MockBean
    private SubscriptionCache subscriptionCache;

    @SpyBean
    private SubscriptionInputTopicListenerImpl subscriptionTopicListener;

    @Test
    @DisplayName("Should invoke subscription handling logic when dataService and dataServiceInstance names match")
    public void test_Should_invoke_subscription_handling_logic_when_dataService_and_dataServiceInstance_names_match() {
        String subscription = """
                {"version":"1","eventType":"subscriptionCreated","event":{"dataServiceName":"eric-oss-5gpmevt-filetx-proc","dataServiceInstanceName":"eric-oss-5gpmevt-filetx-proc--enm1","subscription":{"name":"tomsSubscriptionForKafkaListenerConcurrencyIsThree","consumer":null,"isMandatory":true,"rAppId":26,"status":"Active"},"dataTypes":[{"dataSpace":"5G","dataCategory":"PM_EVENTS","dataProvider":"RAN","schemaName":"PmEventOuterClass.PmEvent","schemaVersion":"1"}, {"dataSpace":"5G","dataCategory":"PM_EVENTS","dataProvider":"RAN","schemaName":"PmEventOuterClass.PmEvent","schemaVersion":"1"}, {"dataSpace":"5G","dataCategory":"PM_EVENTS","dataProvider":"RAN","schemaName":"PmEventOuterClass.PmEvent","schemaVersion":"1"}],"predicates":{"nodeName":["ConcurrenyThree"," * ERBS * "],"eventId":["200","300"]}}}//""";

        ConsumerRecord<String, String> record = new ConsumerRecord<>(topicName, PARTITION_NUMBER, OFFSET, KEY, subscription);

        assertDoesNotThrow(() -> subscriptionTopicListener.listen(record, dataCatalogProperties.getDataServiceName() + "--" + dataCatalogProperties.getDataCollectorName(), dataCatalogProperties.getDataServiceName()));

        verify(startupUtil, times(1)).handleSubscriptions();
    }

    @Test
    @DisplayName("Should not invoke subscription handling logic when dataServiceInstance name does not match")
    public void test_Should_not_invoke_subscription_handling_logic_when_dataServiceInstance_name_does_not_match() {
        String nonMatchingServiceName = "eric-oss-4gpmevt-filetx-proc";
        String subscription = """
                {"version":"1","eventType":"subscriptionCreated","event":{"dataServiceName":"eric-oss-5gpmevt-filetx-proc","dataServiceInstanceName":"eric-oss-5gpmevt-filetx-proc--enm1","subscription":{"name":"tomsSubscriptionForKafkaListenerConcurrencyIsThree","consumer":null,"isMandatory":true,"rAppId":26,"status":"Active"},"dataTypes":[{"dataSpace":"5G","dataCategory":"PM_EVENTS","dataProvider":"RAN","schemaName":"PmEventOuterClass.PmEvent","schemaVersion":"1"}, {"dataSpace":"5G","dataCategory":"PM_EVENTS","dataProvider":"RAN","schemaName":"PmEventOuterClass.PmEvent","schemaVersion":"1"}, {"dataSpace":"5G","dataCategory":"PM_EVENTS","dataProvider":"RAN","schemaName":"PmEventOuterClass.PmEvent","schemaVersion":"1"}],"predicates":{"nodeName":["ConcurrenyThree"," * ERBS * "],"eventId":["200","300"]}}}""";

        ConsumerRecord<String, String> record = new ConsumerRecord<>(topicName, PARTITION_NUMBER, OFFSET, KEY, subscription);

        assertDoesNotThrow(() -> subscriptionTopicListener.listen(record, nonMatchingServiceName + "--ENM2", dataCatalogProperties.getDataServiceName()));

        verify(startupUtil, times(0)).handleSubscriptions();
    }

    @Test
    @DisplayName("Should not invoke subscription handling logic when dataService name does not match")
    public void test_Should_not_invoke_subscription_handling_logic_when_dataService_name_does_not_match() {
        String nonMatchingServiceName = "eric-oss-4gpmevt-filetx-proc";
        String subscription = """
                {"version":"1","eventType":"subscriptionCreated","event":{"dataServiceName":"eric-oss-5gpmevt-filetx-proc","dataServiceInstanceName":"eric-oss-5gpmevt-filetx-proc--enm1","subscription":{"name":"tomsSubscriptionForKafkaListenerConcurrencyIsThree","consumer":null,"isMandatory":true,"rAppId":26,"status":"Active"},"dataTypes":[{"dataSpace":"5G","dataCategory":"PM_EVENTS","dataProvider":"RAN","schemaName":"PmEventOuterClass.PmEvent","schemaVersion":"1"}, {"dataSpace":"5G","dataCategory":"PM_EVENTS","dataProvider":"RAN","schemaName":"PmEventOuterClass.PmEvent","schemaVersion":"1"}, {"dataSpace":"5G","dataCategory":"PM_EVENTS","dataProvider":"RAN","schemaName":"PmEventOuterClass.PmEvent","schemaVersion":"1"}],"predicates":{"nodeName":["ConcurrenyThree"," * ERBS * "],"eventId":["200","300"]}}}//""";

        ConsumerRecord<String, String> record = new ConsumerRecord<>(topicName, PARTITION_NUMBER, OFFSET, KEY, subscription);

        assertDoesNotThrow(() -> subscriptionTopicListener.listen(record, dataCatalogProperties.getDataServiceName() + "--ENM2", nonMatchingServiceName));

        verify(startupUtil, times(0)).handleSubscriptions();
    }

    @Test
    @DisplayName("Should successfully parse the DataJobNotification and invoke convertDataJob")
    public void test_shouldParseValidDataJob() {
        String dataJobJsonCreated = "{\"version\":\"1.0.0\",\"eventType\":\"dataJobCreated\",\"event\":{\"dataJob\":{\"id\":\"1\",\"name\":\"TwoNetElements\",\"clientId\":\"rapp1\",\"status\":\"RUNNING\",\"isMandatory\":\"true\",\"dataDelivery\":\"CONTINUOUS\",\"dataDeliveryMechanism\":\"STREAMING_KAFKA\",\"dataDeliverySchemaId\":\"5G.PmEventOuterClass.PmEvent.pmevent\",\"requester\":\"requester\",\"consumerType\":\"rApp\",\"dataRepository\":{\"hostName\":\"hostname\",\"portAddress\":9092},\"productionJobDefinition\":{\"targetSelector\":{\"nodeNameList\":[\"NF1\",\"NF2\"]},\"dataSelector\":{\"eventId\":[1001,1002]}},\"dataType\":{\"dataTypeId\":\"ERICSSON:NrRanOamPmStandardEventData:1.0.0\",\"isExternal\":true}}}}";
        String dataJobJsonUpdated = "{\"version\":\"1.0.0\",\"eventType\":\"dataJobUpdated\",\"event\":{\"dataJob\":{\"id\":\"1\",\"name\":\"TwoNetElements\",\"clientId\":\"rapp1\",\"status\":\"RUNNING\",\"isMandatory\":\"true\",\"dataDelivery\":\"CONTINUOUS\",\"dataDeliveryMechanism\":\"STREAMING_KAFKA\",\"dataDeliverySchemaId\":\"5G.PmEventOuterClass.PmEvent.pmevent\",\"requester\":\"requester\",\"consumerType\":\"rApp\",\"dataRepository\":{\"hostName\":\"hostname\",\"portAddress\":9092},\"productionJobDefinition\":{\"targetSelector\":{\"nodeNameList\":[\"NF1\",\"NF2\"]},\"dataSelector\":{\"eventId\":[1001,1002]}},\"dataType\":{\"dataTypeId\":\"ERICSSON:NrRanOamPmStandardEventData:1.0.0\",\"isExternal\":true}}}}";
        ConsumerRecord<String, String> createdRecord = new ConsumerRecord<>(dataJobTopicName, PARTITION_NUMBER, OFFSET, KEY, dataJobJsonCreated);
        ConsumerRecord<String, String> updatedRecord = new ConsumerRecord<>(dataJobTopicName, PARTITION_NUMBER, OFFSET, KEY, dataJobJsonUpdated);

        assertDoesNotThrow(() -> subscriptionTopicListener.listenDataJob(createdRecord));
        assertDoesNotThrow(() -> subscriptionTopicListener.listenDataJob(updatedRecord));
        verify(subDataJobAdapter, times(2)).convertDataJob(any()); //TODO when subcache impl is done mock and assert correct method was called.
        verify(subscriptionCache, times(1)).addDataJobSummary(any());
        verify(subscriptionCache, times(1)).updateDataJobSummary(any());
    }

    @Test
    @DisplayName("Should gracefully recover from the null DataJobNotification and refrain from calling convertDataJob")
    public void test_shouldNotThrowWhenDataJobNull() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(dataJobTopicName, PARTITION_NUMBER, OFFSET, KEY, null);
        assertDoesNotThrow(() -> subscriptionTopicListener.listenDataJob(record));
        verify(subDataJobAdapter, times(0)).convertDataJob(any());
    }

    @Test
    @DisplayName("Should successfully parse the DataJobNotification and invoke convertDataJob with empty TargetSelector/DataSelector predicates")
    public void test_noPredicates() {
        String dataJobJson = "{\"version\":\"1.0.0\",\"eventType\":\"dataJobDeleted\",\"event\":{\"dataJob\":{\"id\":\"1\",\"name\":\"TwoNetElements\",\"clientId\":\"rapp1\",\"status\":\"RUNNING\",\"isMandatory\":\"true\",\"dataDelivery\":\"CONTINUOUS\",\"dataDeliveryMechanism\":\"STREAMING_KAFKA\",\"dataDeliverySchemaId\":\"5G.PmEventOuterClass.PmEvent.pmevent\",\"requester\":\"requester\",\"consumerType\":\"rApp\",\"dataRepository\":{\"hostName\":\"hostname\",\"portAddress\":9092},\"productionJobDefinition\":{\"targetSelector\":{\"nodeNameList\":[\"NF1\",\"NF2\"]},\"dataSelector\":{\"eventId\":[1001,1002]}},\"dataType\":{\"dataTypeId\":\"ERICSSON:NrRanOamPmStandardEventData:1.0.0\",\"isExternal\":true}}}}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>(dataJobTopicName, PARTITION_NUMBER, OFFSET, KEY, dataJobJson);
        assertDoesNotThrow(() -> subscriptionTopicListener.listenDataJob(record));
        verify(subDataJobAdapter, times(1)).convertDataJob(any());
        verify(subscriptionCache, times(1)).removeDataJobSummary(any());
    }
}
