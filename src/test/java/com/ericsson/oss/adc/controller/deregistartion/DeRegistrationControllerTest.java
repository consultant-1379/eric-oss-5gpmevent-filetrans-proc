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

package com.ericsson.oss.adc.controller.deregistartion;

import com.ericsson.oss.adc.enums.DeregisterStatus;
import com.ericsson.oss.adc.responses.deregister.DeregisterResponse;
import com.ericsson.oss.adc.service.data.catalog.DataCatalogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest(classes = { DeRegistrationController.class })
class DeRegistrationControllerTest {

    @Autowired
    DeRegistrationController deRegistrationController;
    @MockBean
    private DataCatalogService dataCatalogService;

    @Test
    @DisplayName("verify get OK, SUCCESS response for No Content response from DataCatalog")
    void test_successfullyGenerateResponseForNoContentResponse() {
        mockDataCatalogResponse(HttpStatus.NO_CONTENT);

        ResponseEntity<DeregisterResponse> deregisterResponseResponseEntity = deRegistrationController.deleteDataServiceInstance();
        DeregisterResponse deregisterResponse = deregisterResponseResponseEntity.getBody();

        assertEquals(HttpStatus.OK, deregisterResponseResponseEntity.getStatusCode());
        assertEquals(DeregisterStatus.SUCCESS, deregisterResponse.getDeregisterStatus());
    }

    @Test
    @DisplayName("verify get OK, SUCCESS response for NOT_FOUND response from DataCatalog")
    void test_successfullyGenerateResponseForNotFoundResponse() {
        mockDataCatalogResponse(HttpStatus.NOT_FOUND);

        ResponseEntity<DeregisterResponse> deregisterResponseResponseEntity = deRegistrationController.deleteDataServiceInstance();
        DeregisterResponse deregisterResponse = deregisterResponseResponseEntity.getBody();

        assertEquals(HttpStatus.OK, deregisterResponseResponseEntity.getStatusCode());
        assertEquals(DeregisterStatus.SUCCESS, deregisterResponse.getDeregisterStatus());
    }

    @Test
    @DisplayName("verify get BAD_REQUEST, FAILURE response for BAD_REQUEST response from DataCatalog")
    void test_successfullyGenerateResponseForBadRequest() {
        mockDataCatalogResponse(HttpStatus.BAD_REQUEST);

        ResponseEntity<DeregisterResponse> deregisterResponseResponseEntity = deRegistrationController.deleteDataServiceInstance();
        DeregisterResponse deregisterResponse = deregisterResponseResponseEntity.getBody();

        assertEquals(HttpStatus.BAD_REQUEST, deregisterResponseResponseEntity.getStatusCode());
        assertEquals(DeregisterStatus.FAILURE, deregisterResponse.getDeregisterStatus());
    }

    private void mockDataCatalogResponse(final HttpStatus status) {
        ResponseEntity<Void> responseEntity = new ResponseEntity<>(status);
        Mockito.when(dataCatalogService.deleteDataServiceInstance(anyString(), anyString())).thenReturn(responseEntity);
    }
}