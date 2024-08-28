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

package com.ericsson.oss.adc.service.data.catalog;

import com.ericsson.oss.adc.enums.MessageEncoding;
import com.ericsson.oss.adc.models.*;
import com.ericsson.oss.adc.models.data.catalog.v2.*;
import com.ericsson.oss.adc.util.RestExecutor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static wiremock.org.hamcrest.MatcherAssert.assertThat;
import static wiremock.org.hamcrest.Matchers.samePropertyValuesAs;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(classes = {DataCatalogService.class, RestExecutor.class, DataCatalogServiceV2.class})
@AutoConfigureWebClient(registerRestTemplate = true)
@AutoConfigureStubRunner(repositoryRoot = "https://arm.seli.gic.ericsson.se/artifactory/proj-eric-oss-release-local",
        stubsMode = StubRunnerProperties.StubsMode.REMOTE,
        ids = "com.ericsson.oss.dmi:eric-oss-data-catalog:1.0.299:stubs:9590")
@EnableConfigurationProperties(value = DataCatalogProperties.class)
class DataCatalogServiceTest {

    @Value("${dmm.data-catalog.base-url}")
    private String dataCatalogBaseUrl;

    @Value("${dmm.data-catalog.base-port}")
    private String dataCatalogBasePort;

    @Value("${dmm.data-catalog.subscription-uri}")
    private String subscriptionUri;

    @Autowired
    private RestTemplate restTemplate;

    @SpyBean
    DataCatalogService dataCatalogService;

    @SpyBean
    DataCatalogServiceV2 dataCatalogServiceV2;

    private final MessageSchemaPutRequest messageSchemaPutRequest = new MessageSchemaPutRequest(
            1,
            new DataSpace("4G"),
            new DataServiceForMessageSchemaPut("ds"),
            new DataServiceInstance("dsinst101", "http://localhost:8082", "4G", "4G", "4G", "SCH2", "2"),
            new DataCategory("CM_EXPORTS1"),
            new DataProviderTypeForMessageSchemaPUT("Vv101", "vv101"),
            new MessageStatusTopic("topic102", 1L, "SpecRef101", MessageEncoding.JSON),
            new MessageDataTopic("topic102", 1L, MessageEncoding.JSON),
            new DataType(1, "stream", "SCH2", "2", true, "4G", "4G", "4G", "4G", "2"),
            new ArrayList<>(Arrays.asList(new SupportedPredicateParameter("nodeName", true), new SupportedPredicateParameter("eventId", false))),
            new MessageSchema("SpecRef101")
    );

    @Test
    @Order(1)
    @DisplayName("Fail to retrieve MessageSchemaV2 List by params returns null when topic with unknown dataCategory not found")
    void test_getMessageSchemaListV2ByDataProviderTypeAndDataSpace_400() {  // TODO: Add positive test case when stub updated
        ResponseEntity<MessageSchemaListV2> response = dataCatalogServiceV2.getMessageSchemaListV2ByDataSpaceAndDataCategory(
                "pvid_1",
                "name");
        assertNull(response.getBody());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode()); // TODO: revisit when stub returning correctly
    }

    @Test
    @Order(2)
    @DisplayName("Registers message schema and verifies message schema params in PUT response")
    public void test_registerMessageSchema_201() {
        ResponseEntity<MessageSchemaV2> response = dataCatalogService.registerMessageSchema(messageSchemaPutRequest);
        assertNotNull(response);
        MessageSchemaV2 messageSchemaV2 = response.getBody();
        //More will be added to this test in a later commit
    }

    @Test
    @Order(3)
    @DisplayName("Deletes a Data Service Instance successfully and verifies 204 status code")
    public void test_deleteDataServiceInstance_200() {
        ResponseEntity<Void> response = dataCatalogService.deleteDataServiceInstance("ds", "dsinstance");
        assertNotNull(response);
        HttpStatusCode status = response.getStatusCode();

        assertEquals(HttpStatus.NO_CONTENT, status);
    }

    @Test
    @Order(4)
    @DisplayName("Test get all subscription with params and returns JSON with Subscription array")
    void testGetAllSubscriptionsByParams() throws Exception {
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
        String serviceName = "eric-oss-5gpmevt-filetx-proc";
        String status = "Active";
        String subscriptionQuery = "?serviceName=" + serviceName + "&status=" + status;
        final String url = String.format("%s%s%s%s", dataCatalogBaseUrl, dataCatalogBasePort, subscriptionUri, subscriptionQuery);
        mockServer.expect(requestTo(new URI(url)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new InputStreamResource(Objects.requireNonNull(getClass().getClassLoader()
                                .getResourceAsStream("Subscriptions.json")))));
        ResponseEntity<Subscription[]> result = dataCatalogServiceV2.getAllSubscriptionsByParams(serviceName, status);
        assertTrue(result.getStatusCode().is2xxSuccessful());
        assertEquals(4, result.getBody().length);

        verifySub1(result.getBody()[0]);
        verifySub2(result.getBody()[1]);
        verifySub3(result.getBody()[2]);
        verifySub4(result.getBody()[3]);
    }

    @Test
    @Order(5)
    @DisplayName("Test getSubscription when getAllSubscriptionsByParams returns JSON with Subscription array")
    void testGetSubscriptions() throws Exception {
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
        String serviceName = "eric-oss-5gpmevt-filetx-proc";
        String status = "Active";
        String subscriptionQuery = "?serviceName=" + serviceName + "&status=" + status;
        final String url = String.format("%s%s%s%s", dataCatalogBaseUrl, dataCatalogBasePort, subscriptionUri, subscriptionQuery);
        mockServer.expect(requestTo(new URI(url)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new InputStreamResource(Objects.requireNonNull(getClass().getClassLoader()
                                .getResourceAsStream("Subscriptions.json")))));
        assertEquals(4, dataCatalogServiceV2.getSubscriptions().length);
    }

    @Test
    @Order(6)
    @DisplayName("Test getSubscription returns empty list when getAllSubscriptionsByParams returns 503")
    void testGetSubscriptionReturnsEmptyListsWhenGetAllSubscriptionsByParamsReturns503() throws Exception {
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
        String serviceName = "eric-oss-5gpmevt-filetx-proc";
        String status = "Active";
        String subscriptionQuery = "?serviceName=" + serviceName + "&status=" + status;
        final String url = String.format("%s%s%s%s", dataCatalogBaseUrl, dataCatalogBasePort, subscriptionUri, subscriptionQuery);
        mockServer.expect(requestTo(new URI(url)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new InputStreamResource(Objects.requireNonNull(getClass().getClassLoader()
                                .getResourceAsStream("Subscriptions.json")))));
        assertEquals(0, dataCatalogServiceV2.getSubscriptions().length);
    }

    @Test
    @Order(7)
    @DisplayName("Test getSubscription returns empty list when getAllSubscriptionsByParams returns null body")
    void testGetSubscriptionReturnsEmptyListsWhenGetAllSubscriptionsByParamsReturnsNullBody() throws Exception {
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
        String serviceName = "eric-oss-5gpmevt-filetx-proc";
        String status = "Active";
        String subscriptionQuery = "?serviceName=" + serviceName + "&status=" + status;
        final String url = String.format("%s%s%s%s", dataCatalogBaseUrl, dataCatalogBasePort, subscriptionUri, subscriptionQuery);
        mockServer.expect(requestTo(new URI(url)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON));
        assertEquals(0, dataCatalogServiceV2.getSubscriptions().length);
    }

    private static void verifySub1(Subscription subBody) {
        assertEquals(1, subBody.getId());
        assertEquals("191", subBody.getIds().getRAppId());
        assertEquals("subscriptionWithTwoPredicate", subBody.getName());
        assertEquals("Active", subBody.getStatus());
        Predicates predicates = new Predicates();
        predicates.setNodeName(List.of("*ManagedElement=*", "*ERBS*"));
        predicates.setEventId(List.of("200", "300"));
        assertThat(predicates, samePropertyValuesAs(subBody.getPredicates()));
        assertTrue(subBody.isMandatory());
    }

    private static void verifySub2(Subscription subBody) {
        assertEquals(2, subBody.getId());
        assertEquals("8", subBody.getIds().getRAppId());
        assertEquals("subscriptionWithNoPredicate", subBody.getName());
        assertEquals("Inactive", subBody.getStatus());
        Predicates predicates = new Predicates();
        assertThat(predicates, samePropertyValuesAs(subBody.getPredicates()));
        assertFalse(subBody.isMandatory());
    }

    private static void verifySub3(Subscription subBody) {
        assertEquals(3, subBody.getId());
        assertEquals("9", subBody.getIds().getRAppId());
        assertEquals("subscriptionWithNodeNamePredicate", subBody.getName());
        assertEquals("Processing", subBody.getStatus());
        Predicates predicates = new Predicates();
        predicates.setNodeName(List.of("NR100gNOdeBRadio00001995"));
        assertThat(predicates, samePropertyValuesAs(subBody.getPredicates()));
        assertTrue(subBody.isMandatory());
    }

    private static void verifySub4(Subscription subBody) {
        assertEquals(4, subBody.getId());
        assertEquals("10", subBody.getIds().getRAppId());
        assertEquals("subscriptionWithEventIdPredicate", subBody.getName());
        assertEquals("Error", subBody.getStatus());
        Predicates predicates = new Predicates();
        predicates.setEventId(List.of("200", "300"));
        assertThat(predicates, samePropertyValuesAs(subBody.getPredicates()));
        assertFalse(subBody.isMandatory());
    }
}
