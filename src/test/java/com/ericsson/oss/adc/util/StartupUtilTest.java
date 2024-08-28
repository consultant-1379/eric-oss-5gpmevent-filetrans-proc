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

package com.ericsson.oss.adc.util;

import com.ericsson.oss.adc.models.*;
import com.ericsson.oss.adc.models.adapters.SubDataJobAdapter;
import com.ericsson.oss.adc.models.data.catalog.v2.*;
import com.ericsson.oss.adc.models.metrics.ActiveDataJobsGauge;
import com.ericsson.oss.adc.models.metrics.ActiveSubscriptionsGauge;
import com.ericsson.oss.adc.service.data.catalog.DataCatalogService;
import com.ericsson.oss.adc.service.data.catalog.DataCatalogServiceV2;
import com.ericsson.oss.adc.service.output.topic.OutputTopicService;
import com.ericsson.oss.adc.service.subscription.filter.EventIDFilter;
import com.ericsson.oss.adc.service.subscription.filter.NodeNameFilter;
import com.ericsson.oss.adc.service.subscription.filter.SubscriptionCache;
import com.ericsson.oss.adc.service.subscription.filter.SubscriptionCacheUtil;
import com.googlecode.catchexception.apis.BDDCatchException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.ericsson.oss.adc.models.DataCatalogConstants.SCHEMA_NAME_STANDARDIZED;
import static com.ericsson.oss.adc.models.DataCatalogConstants.SCHEMA_NAME_NON_STANDARD;
import static com.ericsson.oss.adc.models.DataCatalogConstants.SPECIFICATION_REFERENCE_NONSTANDARD;
import static com.ericsson.oss.adc.models.DataCatalogConstants.SPECIFICATION_REFERENCE_STANDARDIZED;
import static com.ericsson.oss.adc.service.subscription.filter.SubscriptionsDelegate.*;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {StartupUtil.class, SubscriptionCache.class, SubDataJobAdapter.class, NodeNameFilter.class, EventIDFilter.class})
@EnableConfigurationProperties(value = DataCatalogProperties.class)
public class StartupUtilTest {

    @Autowired
    private StartupUtil startupUtil;

    // required to be mocked for tests to run though never used in test
    @MockBean
    private OutputTopicService outputTopicService;

    @MockBean
    private DataCatalogService dataCatalogService;

    @MockBean
    private DataCatalogServiceV2 dataCatalogServiceV2;

    @MockBean
    private ActiveSubscriptionsGauge activeSubscriptionsGauge;

    @MockBean
    private ActiveDataJobsGauge activeDataJobsGauge;

    @Autowired
    private DataCatalogProperties dataCatalogProperties;

    @SpyBean
    private SubscriptionCacheUtil subscriptionCacheUtil;

    @Autowired
    private SubscriptionCache subscriptionCache;

    private final SubDataJobAdapter subDataJobAdapter = new SubDataJobAdapter();

    @Captor
    private ArgumentCaptor<MessageSchemaPutRequest> messageSchemaV2ArgumentCaptor;

    @Value("${dmm.data-catalog.data-space}")
    private String dataSpaceName;

    @Value("${dmm.data-catalog.data-category}")
    private String dataCategory;

    private static final String INPUT_TOPIC_MESSAGE_BUS_UNAVAILABLE = "No Input Topic Message Bus Name Available From Data Catalog";
    private static final String INPUT_TOPIC_MESSAGE_BUS_STATUS_TOPIC_NAME_DATA_SPACE_UNAVAILABLE = "No Input Topic Message Bus, Message Status Topic Name Or Data Space Available From Data Catalog";
    private final String standardizedOutputTopicName = "5g-pm-event-file-transfer-and-processing--standardized";
    private final String nonStandardOutputTopicName = "5g-pm-event-file-transfer-and-processing--ericsson";
    private final MessageSchemaV2 InputTopicMessageSchemaV2 = new MessageSchemaV2(
            1,
            new MessageDataTopicV2(
                    "file-notification-service--5g-event--enm1",
                    new DataProviderType(
                            "providerVersion",
                            new DataSpace("5G"),
                            "enm1",
                            "RAN"),
                    new MessageBus(
                            1L,
                            "name",
                            "clusterName",
                            "nameSpace",
                            new ArrayList<String>(Arrays.asList("http://endpoint1:1234/", "eric-oss-dmm-data-message-bus-kf-client:9092",
                                    "http://localhost:9092")),
                            new ArrayList<Long>(Collections.singletonList(1L)),
                            new ArrayList<Long>(Collections.singletonList(1L)),
                            new ArrayList<Long>(Collections.singletonList(1L))),
                    new MessageStatusTopic(
                            "file-notification-service--5g-event--enm1",
                            1L
                    )
            ),
            SPECIFICATION_REFERENCE_STANDARDIZED,
            new DataService(
                    "dataserviceinstanceName",
                    new DataServiceInstance[]{new DataServiceInstance()},
                    new ArrayList<>(Arrays.asList(new SupportedPredicateParameter("nodeName", true),
                            new SupportedPredicateParameter("eventId", false)))
            ),
            new DataType(SCHEMA_NAME_STANDARDIZED)
    );

    private final MessageSchemaV2 messageSchemaV2StandardizedOutputTopic = new MessageSchemaV2(
            1,
            new MessageDataTopicV2(
                    standardizedOutputTopicName,
                    new DataProviderType(
                            "providerVersion",
                            new DataSpace("5G"),
                            "enm1",
                            "RAN"),
                    new MessageBus(
                            1L,
                            "name",
                            "clusterName",
                            "nameSpace",
                            new ArrayList<String>(Arrays.asList("http://endpoint1:1234/", "eric-oss-dmm-data-message-bus-kf-client:9092",
                                    "http://localhost:9092")),
                            new ArrayList<Long>(Collections.singletonList(1L)),
                            new ArrayList<Long>(Collections.singletonList(1L)),
                            new ArrayList<Long>(Collections.singletonList(1L))),
                    new MessageStatusTopic(
                            standardizedOutputTopicName,
                            1L
                    )
            ),
            SPECIFICATION_REFERENCE_STANDARDIZED,
            new DataService(
                    "dataserviceinstanceName",
                    new DataServiceInstance[]{new DataServiceInstance()},
                    new ArrayList<>(Arrays.asList(new SupportedPredicateParameter("nodeName", true),
                            new SupportedPredicateParameter("eventId", false)))
            ),
            new DataType(SCHEMA_NAME_STANDARDIZED)
    );

    private final MessageSchemaV2 messageSchemaV2NonStandardOutputTopic = new MessageSchemaV2(
            1,
            new MessageDataTopicV2(
                    nonStandardOutputTopicName,
                    new DataProviderType(
                            "providerVersion",
                            new DataSpace("5G"),
                            "enm1",
                            "RAN"),
                    new MessageBus(
                            1L,
                            "name",
                            "clusterName",
                            "nameSpace",
                            new ArrayList<String>(Arrays.asList("http://endpoint1:1234/", "eric-oss-dmm-data-message-bus-kf-client:9092",
                                    "http://localhost:9092")),
                            new ArrayList<Long>(Collections.singletonList(1L)),
                            new ArrayList<Long>(Collections.singletonList(1L)),
                            new ArrayList<Long>(Collections.singletonList(1L))),
                    new MessageStatusTopic(
                            nonStandardOutputTopicName,
                            1L
                    )
            ),
            SPECIFICATION_REFERENCE_NONSTANDARD,
            new DataService(
                    "dataserviceinstanceName",
                    new DataServiceInstance[]{new DataServiceInstance()},
                    new ArrayList<>(Arrays.asList(new SupportedPredicateParameter("nodeName", true),
                            new SupportedPredicateParameter("eventId", false)))
            ),
            new DataType(SCHEMA_NAME_NON_STANDARD)
    );

    private final MessageSchemaV2 messageSchemaV2NullDataSpaceName = new MessageSchemaV2(
            1,
            new MessageDataTopicV2(
                    "file-notification-service--5g-event--enm1",
                    new DataProviderType(
                            "providerVersion",
                            new DataSpace(""),
                            "enm1",
                            "RAN"),
                    new MessageBus(
                            1L,
                            "name",
                            "clusterName",
                            "nameSpace",
                            new ArrayList<String>(Arrays.asList("http://endpoint1:1234/", "eric-oss-dmm-data-message-bus-kf-client:9092",
                                    "http://localhost:9092")),
                            new ArrayList<Long>(Collections.singletonList(1L)),
                            new ArrayList<Long>(Collections.singletonList(1L)),
                            new ArrayList<Long>(Collections.singletonList(1L))),
                    new MessageStatusTopic(
                            "file-notification-service--5g-event--enm1",
                            1L
                    )
            ),
            SPECIFICATION_REFERENCE_STANDARDIZED,
            new DataService(
                    "dataserviceinstanceName",
                    new DataServiceInstance[]{new DataServiceInstance()},
                    new ArrayList<>(Arrays.asList(new SupportedPredicateParameter("nodeName", true),
                            new SupportedPredicateParameter("eventId", false)))
            ),
            new DataType(SCHEMA_NAME_STANDARDIZED)
    );

    private final MessageSchemaV2 messageSchemaV2NullMessageBus = new MessageSchemaV2(
            1,
            new MessageDataTopicV2(
                    "file-notification-service--5g-event--enm1",
                    new DataProviderType(
                            "providerVersion",
                            new DataSpace("5G"),
                            "enm1",
                            "RAN"),
                    new MessageBus(),
                    new MessageStatusTopic(
                            "file-notification-service--5g-event--enm1",
                            1L
                    )
            ),
            SPECIFICATION_REFERENCE_STANDARDIZED,
            new DataService(
                    "dataserviceinstanceName",
                    new DataServiceInstance[]{new DataServiceInstance()},
                    new ArrayList<>(Arrays.asList(new SupportedPredicateParameter("nodeName", true),
                            new SupportedPredicateParameter("eventId", false)))
            ),
            new DataType(SCHEMA_NAME_STANDARDIZED)
    );

    private final MessageSchemaV2 messageSchemaV2NullDataSpaceMessageStatusTopicNameMessageBusName = new MessageSchemaV2(
            1,
            new MessageDataTopicV2(
                    "name",
                    new DataProviderType(
                            "providerVersion",
                            new DataSpace("XL"),
                            "enm1",
                            "RAN"),
                    new MessageBus(
                            1L,
                            "name",
                            "clusterName",
                            "nameSpace",
                            new ArrayList<String>(Arrays.asList("http://endpoint1:1234/", "eric-oss-dmm-data-message-bus-kf-client:9092",
                                    "http://localhost:9092")),
                            new ArrayList<Long>(Collections.singletonList(1L)),
                            new ArrayList<Long>(Collections.singletonList(1L)),
                            new ArrayList<Long>(Collections.singletonList(1L))),
                    new MessageStatusTopic(
                            "file-notification-service--5g-event--enm1",
                            1L
                    )
            ),
            SPECIFICATION_REFERENCE_STANDARDIZED,
            new DataService(
                    "dataserviceinstanceName",
                    new DataServiceInstance[]{new DataServiceInstance()},
                    new ArrayList<>(Arrays.asList(new SupportedPredicateParameter("nodeName", true),
                            new SupportedPredicateParameter("eventId", false)))
            ),
            new DataType(SCHEMA_NAME_STANDARDIZED)
    );

    private final MessageSchemaListV2 messageSchemaListV2 = new MessageSchemaListV2();

    @Test
    @DisplayName("Should return true after successfully fetching access end points from DataCatalog")
    void test_allObjectsSetupCorrectly() {
        messageSchemaListV2.add(InputTopicMessageSchemaV2);
        messageSchemaListV2.add(messageSchemaV2StandardizedOutputTopic);
        ResponseEntity<MessageSchemaV2> messageSchemaResponseEntity = new ResponseEntity<>(InputTopicMessageSchemaV2, HttpStatus.OK);
        ResponseEntity<MessageSchemaListV2> messageSchemaListV2ResponseEntity = new ResponseEntity<>(messageSchemaListV2, HttpStatus.OK);

        when(dataCatalogServiceV2.getMessageSchemaListV2ByDataSpaceAndDataCategory(dataSpaceName, dataCategory)).thenReturn(messageSchemaListV2ResponseEntity);
        when(dataCatalogService.registerMessageSchema(any(MessageSchemaPutRequest.class))).thenReturn(messageSchemaResponseEntity);
        when(outputTopicService.getStandardizedTopicName()).thenReturn(standardizedOutputTopicName);
        when(outputTopicService.getStandardizedTopicName()).thenReturn(nonStandardOutputTopicName);
        when(outputTopicService.setupOutputTopics()).thenReturn(true);

        assertTrue(startupUtil.queryDataCatalogForInputTopic());
        assertTrue(startupUtil.registerInDataCatalog());
    }

    @Test
    @DisplayName("Should return true when DataCatalog returns no access end points and configmap access end point is provided instead")
    void test_allObjectsSetupFailScenario() {
        messageSchemaListV2.add(InputTopicMessageSchemaV2);
        messageSchemaListV2.add(messageSchemaV2StandardizedOutputTopic);
        ResponseEntity<MessageSchemaV2> messageSchemaResponseEntity = new ResponseEntity<>(InputTopicMessageSchemaV2, HttpStatus.OK);
        ResponseEntity<MessageSchemaListV2> messageSchemaListV2ResponseEntity = new ResponseEntity<>(messageSchemaListV2, HttpStatus.OK);

        when(dataCatalogServiceV2.getMessageSchemaListV2ByDataSpaceAndDataCategory(dataSpaceName, dataCategory)).thenReturn(messageSchemaListV2ResponseEntity);
        when(dataCatalogService.registerMessageSchema(any(MessageSchemaPutRequest.class))).thenReturn(messageSchemaResponseEntity);
        when(outputTopicService.getStandardizedTopicName()).thenReturn(standardizedOutputTopicName);
        when(outputTopicService.getStandardizedTopicName()).thenReturn(nonStandardOutputTopicName);
        when(outputTopicService.setupOutputTopics()).thenReturn(true);

        assertTrue(startupUtil.queryDataCatalogForInputTopic());
        assertTrue(startupUtil.registerInDataCatalog());
    }

    @Test
    @DisplayName("Failure to get Message Bus in Data Catalog should throw NullPointer Exception")
    void test_FailureMessageBusUnavailableInDataCatalogThrowNullPointer() {
        messageSchemaListV2.add(messageSchemaV2NullMessageBus);
        messageSchemaListV2.add(messageSchemaV2StandardizedOutputTopic);
        ResponseEntity<MessageSchemaV2> messageSchemaResponseEntity = new ResponseEntity<>(messageSchemaV2NullMessageBus, HttpStatus.OK);
        ResponseEntity<MessageSchemaListV2> messageSchemaListV2ResponseEntity = new ResponseEntity<>(messageSchemaListV2, HttpStatus.OK);

        when(dataCatalogServiceV2.getMessageSchemaListV2ByDataSpaceAndDataCategory("5G", dataCategory)).thenReturn(messageSchemaListV2ResponseEntity);
        when(dataCatalogService.registerMessageSchema(any(MessageSchemaPutRequest.class))).thenReturn(messageSchemaResponseEntity);

        assertThrows(NullPointerException.class, () -> startupUtil.queryDataCatalogForInputTopic());
        BDDCatchException.when(() -> startupUtil.queryDataCatalogForInputTopic());
        assertEquals(INPUT_TOPIC_MESSAGE_BUS_UNAVAILABLE, caughtException().getMessage());
    }

    @Test
    @DisplayName("Failure to get Message Status Topic Name, Message Bus and DataSpace in Data Catalog should throw NotFound Exception")
    void test_FailureMessageBusMessageStatusTopicNameDataSpaceUnavailableInDataCatalogThrowNotFound() {
        messageSchemaListV2.add(messageSchemaV2NullDataSpaceMessageStatusTopicNameMessageBusName);
        messageSchemaListV2.add(messageSchemaV2StandardizedOutputTopic);
        ResponseEntity<MessageSchemaV2> messageSchemaResponseEntity = new ResponseEntity<>(messageSchemaV2NullDataSpaceMessageStatusTopicNameMessageBusName, HttpStatus.OK);
        ResponseEntity<MessageSchemaListV2> messageSchemaListV2ResponseEntity = new ResponseEntity<>(messageSchemaListV2, HttpStatus.OK);

        when(dataCatalogServiceV2.getMessageSchemaListV2ByDataSpaceAndDataCategory("5G", dataCategory)).thenReturn(messageSchemaListV2ResponseEntity);
        when(dataCatalogService.registerMessageSchema(any(MessageSchemaPutRequest.class))).thenReturn(messageSchemaResponseEntity);

        assertThrows(NotFoundException.class, () -> startupUtil.queryDataCatalogForInputTopic());
        BDDCatchException.when(() -> startupUtil.queryDataCatalogForInputTopic());
        assertEquals(INPUT_TOPIC_MESSAGE_BUS_STATUS_TOPIC_NAME_DATA_SPACE_UNAVAILABLE, caughtException().getMessage());
    }

    @Test
    @DisplayName("Failure to get Dataspace in Data Catalog should throw NullPointer Exception")
    void test_FailureDataSpaceUnavailableInDataCatalogThrowNullPointer() {
        messageSchemaListV2.add(messageSchemaV2NullDataSpaceName);
        messageSchemaListV2.add(messageSchemaV2StandardizedOutputTopic);
        ResponseEntity<MessageSchemaV2> messageSchemaResponseEntity = new ResponseEntity<>(messageSchemaV2NullDataSpaceName, HttpStatus.OK);
        ResponseEntity<MessageSchemaListV2> messageSchemaListV2ResponseEntity = new ResponseEntity<>(messageSchemaListV2, HttpStatus.OK);

        when(dataCatalogServiceV2.getMessageSchemaListV2ByDataSpaceAndDataCategory("5G", dataCategory)).thenReturn(messageSchemaListV2ResponseEntity);
        when(dataCatalogService.registerMessageSchema(any(MessageSchemaPutRequest.class))).thenReturn(messageSchemaResponseEntity);

        assertThrows(NotFoundException.class, () -> startupUtil.queryDataCatalogForInputTopic());
        BDDCatchException.when(() -> startupUtil.queryDataCatalogForInputTopic());
        assertEquals(INPUT_TOPIC_MESSAGE_BUS_STATUS_TOPIC_NAME_DATA_SPACE_UNAVAILABLE, caughtException().getMessage());
    }

    @Test
    @DisplayName("Failure to register MessageSchema because of 409 response in Data Catalog should not cause error scenario")
    void test_RegistrationFailureMessageSchemaBecauseOf409ShouldNotCauseError() {
        messageSchemaListV2.add(InputTopicMessageSchemaV2);
        messageSchemaListV2.add(messageSchemaV2StandardizedOutputTopic);
        messageSchemaListV2.add(messageSchemaV2NonStandardOutputTopic);
        ResponseEntity<MessageSchemaListV2> messageSchemaListV2ResponseEntity = new ResponseEntity<>(messageSchemaListV2, HttpStatus.OK);
        ResponseEntity<MessageSchemaV2> messageSchemaResponseEntity = new ResponseEntity<>(InputTopicMessageSchemaV2, HttpStatus.valueOf(409));

        when(dataCatalogService.registerMessageSchema(any())).thenReturn(messageSchemaResponseEntity);
        when(dataCatalogServiceV2.getMessageSchemaListV2ByDataSpaceAndDataCategory(dataSpaceName, dataCategory)).thenReturn(messageSchemaListV2ResponseEntity);
        when(outputTopicService.getStandardizedTopicName()).thenReturn(standardizedOutputTopicName);
        when(outputTopicService.getNonStandardTopicName()).thenReturn(nonStandardOutputTopicName);
        when(outputTopicService.setupOutputTopics()).thenReturn(true);

        assertTrue(startupUtil.queryDataCatalogForInputTopic());
        assertTrue(startupUtil.registerInDataCatalog());
    }

    @Test
    @DisplayName("Failure to register MessageSchema because of 408 response in Data Catalog should cause error scenario")
    void test_RegistrationFailureMessageSchemaBecauseOf408ShouldCauseError() {
        messageSchemaListV2.add(InputTopicMessageSchemaV2);
        messageSchemaListV2.add(messageSchemaV2StandardizedOutputTopic);
        ResponseEntity<MessageSchemaListV2> messageSchemaListV2ResponseEntity = new ResponseEntity<>(messageSchemaListV2, HttpStatus.OK);
        ResponseEntity<MessageSchemaV2> messageSchemaResponseEntity = new ResponseEntity<>(InputTopicMessageSchemaV2, HttpStatus.valueOf(408));

        when(dataCatalogService.registerMessageSchema(any())).thenReturn(messageSchemaResponseEntity);
        when(dataCatalogServiceV2.getMessageSchemaListV2ByDataSpaceAndDataCategory(dataSpaceName, dataCategory)).thenReturn(messageSchemaListV2ResponseEntity);
        when(outputTopicService.getStandardizedTopicName()).thenReturn(standardizedOutputTopicName);
        when(outputTopicService.getNonStandardTopicName()).thenReturn(nonStandardOutputTopicName);
        when(outputTopicService.setupOutputTopics()).thenReturn(true);

        assertTrue(startupUtil.queryDataCatalogForInputTopic());
        assertFalse(startupUtil.registerInDataCatalog());
    }

    @Test
    @DisplayName("Failure to register MessageSchema because of 4XX response in Data Catalog should cause error scenario")
    void test_RegistrationFailureMessageSchemaBecauseOf4XXShouldCauseError() {
        messageSchemaListV2.add(InputTopicMessageSchemaV2);
        messageSchemaListV2.add(messageSchemaV2StandardizedOutputTopic);
        ResponseEntity<MessageSchemaListV2> messageSchemaListV2ResponseEntity = new ResponseEntity<>(messageSchemaListV2, HttpStatus.OK);
        ResponseEntity<MessageSchemaV2> messageSchemaResponseEntity = new ResponseEntity<>(InputTopicMessageSchemaV2, HttpStatus.valueOf(400));

        when(dataCatalogService.registerMessageSchema(any())).thenReturn(messageSchemaResponseEntity);
        when(dataCatalogServiceV2.getMessageSchemaListV2ByDataSpaceAndDataCategory(dataSpaceName, dataCategory)).thenReturn(messageSchemaListV2ResponseEntity);
        when(outputTopicService.getStandardizedTopicName()).thenReturn(standardizedOutputTopicName);
        when(outputTopicService.getNonStandardTopicName()).thenReturn(nonStandardOutputTopicName);
        when(outputTopicService.setupOutputTopics()).thenReturn(true);

        assertTrue(startupUtil.queryDataCatalogForInputTopic());
        assertFalse(startupUtil.registerInDataCatalog());
    }

    @Test
    @DisplayName("Should return true after successfully fetching access end points from DataCatalog with valid message schema PUT request")
    void test_allObjectsSetupCorrectlyWithValidMessageSchemaPutRequest() {
        messageSchemaListV2.add(InputTopicMessageSchemaV2);
        messageSchemaListV2.add(messageSchemaV2StandardizedOutputTopic);
        messageSchemaListV2.add(messageSchemaV2NonStandardOutputTopic);
        ResponseEntity<MessageSchemaV2> messageSchemaResponseEntity = new ResponseEntity<>(InputTopicMessageSchemaV2, HttpStatus.OK);
        ResponseEntity<MessageSchemaListV2> messageSchemaListV2ResponseEntity = new ResponseEntity<>(messageSchemaListV2, HttpStatus.OK);
        MessageSchemaPutRequest messageSchemaPutRequestStandard = createMessageSchemaRequest(standardizedOutputTopicName, SPECIFICATION_REFERENCE_STANDARDIZED,
                SCHEMA_NAME_STANDARDIZED);
        MessageSchemaPutRequest messageSchemaPutRequestNonStandardized = createMessageSchemaRequest(nonStandardOutputTopicName,
                SPECIFICATION_REFERENCE_NONSTANDARD, SCHEMA_NAME_NON_STANDARD);

        validatedConfigurableFields(messageSchemaPutRequestStandard);
        validatedConfigurableFields(messageSchemaPutRequestNonStandardized);

        when(dataCatalogServiceV2.getMessageSchemaListV2ByDataSpaceAndDataCategory(dataSpaceName, dataCategory)).thenReturn(messageSchemaListV2ResponseEntity);
        when(dataCatalogService.registerMessageSchema(any())).thenReturn(messageSchemaResponseEntity);
        when(outputTopicService.getStandardizedTopicName()).thenReturn(standardizedOutputTopicName);
        when(outputTopicService.getNonStandardTopicName()).thenReturn(nonStandardOutputTopicName);
        when(outputTopicService.setupOutputTopics()).thenReturn(true);

        assertTrue(startupUtil.queryDataCatalogForInputTopic());
        assertTrue(startupUtil.registerInDataCatalog());
        verify(dataCatalogService, times(2)).registerMessageSchema(messageSchemaV2ArgumentCaptor.capture());

        List<MessageSchemaPutRequest> expectedMethodArgs = List.of(messageSchemaPutRequestNonStandardized, messageSchemaPutRequestStandard);
        assertTrue(expectedMethodArgs.containsAll(messageSchemaV2ArgumentCaptor.getAllValues()));
    }

    @Test
    @DisplayName("handleSubscriptions should call reconstituteSubscriptionCache with an empty list if getSubscriptions returns empty array")
    void test_getSubscriptionsShouldReturnAnEmptyArrayIfGetAllSubscriptionsByParamsShouldFails() {
        //Populate subscriptionCache with two subs
        subscriptionCache.addDataJobSummary(subDataJobAdapter.convertSubscription(subscriptionWithTwoPredicates));
        subscriptionCache.addDataJobSummary(subDataJobAdapter.convertSubscription(subscriptionWithJustEventId));
        assertEquals(2, subscriptionCache.getAllDataJobSummaryRAppIds().size());

        when(dataCatalogServiceV2.getSubscriptions()).thenReturn(new Subscription[]{});

        startupUtil.handleSubscriptions();

        List<Subscription> emptySubscriptions = new ArrayList<>();
        verify(subscriptionCacheUtil).reconstituteSubscriptionCache(emptySubscriptions);
        assertEquals(0, subscriptionCache.getAllDataJobSummaryRAppIds().size());
    }

    @Test
    @DisplayName("handleSubscriptions should populate cache with subscriptions returned from Data Catalog")
    void test_handleSubscriptionsShouldPopulateCacheWithSubscriptionsReturnedFromDataCatalog() {

        subscriptionCache.addDataJobSummary(subDataJobAdapter.convertSubscription(subscriptionWithTwoPredicates));
        subscriptionCache.addDataJobSummary(subDataJobAdapter.convertSubscription(subscriptionWithJustEventId));
        assertEquals(2, subscriptionCache.getAllDataJobSummaryRAppIds().size());

        Subscription[] subscriptions = new Subscription[4];
        subscriptions[0] = subscriptionWithTwoPredicates;
        subscriptions[1] = subscriptionWithNoPredicates;
        subscriptions[2] = subscriptionWithJustEventId;
        subscriptions[3] = subscriptionWithJustNodeName;

        when(dataCatalogServiceV2.getSubscriptions()).thenReturn(subscriptions);

        startupUtil.handleSubscriptions();

        assertEquals(4, subscriptionCache.getAllDataJobSummaryRAppIds().size());
    }

    private void validatedConfigurableFields(MessageSchemaPutRequest messageSchemaPutRequest) {
        assertEquals("5G", messageSchemaPutRequest.getDataSpace().getName());
        assertEquals("eric-oss-5gpmevt-filetx-proc", messageSchemaPutRequest.getDataService().getDataServiceName());
        assertEquals("eric-oss-5gpmevt-filetx-proc--enm1", messageSchemaPutRequest.getDataServiceInstance().getDataServiceInstanceName());
        assertEquals("enm1", messageSchemaPutRequest.getDataServiceInstance().getConsumedDataProvider());

        assertEquals(1L, messageSchemaPutRequest.getMessageDataTopic().getMessageBusId());
        assertEquals(1L, messageSchemaPutRequest.getMessageStatusTopic().getMessageBusId());

        assertEquals(2, messageSchemaPutRequest.getSupportedPredicateParameters().size());
    }

    private MessageSchemaPutRequest createMessageSchemaRequest(final String topicName, final String specRef, final String schemaName) {
        String dataServiceInstanceName = dataCatalogProperties.getDataServiceName() + "--" + dataCatalogProperties.getDataCollectorName();
        return MessageSchemaPutRequest
                .builder()
                .dataSpace(new DataSpace(dataCatalogProperties.getDataSpace()))
                .dataService(new DataServiceForMessageSchemaPut(dataCatalogProperties.getDataServiceName()))
                .dataServiceInstance(new DataServiceInstance(dataServiceInstanceName, dataCatalogProperties.getDataCollectorName()))
                .dataCategory(new DataCategory())
                .dataProviderType(new DataProviderTypeForMessageSchemaPUT())
                .messageStatusTopic(new MessageStatusTopic(topicName, 1L))
                .messageDataTopic(new MessageDataTopic(topicName, 1L))
                .dataType(new DataType(schemaName))
                .supportedPredicateParameters(Collections.singleton(new SupportedPredicateParameter("nodeName", true)))
                .supportedPredicateParameters(Collections.singleton(new SupportedPredicateParameter("eventId", false)))
                .messageSchema(new MessageSchema(specRef))
                .build();
    }
}