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

package com.ericsson.oss.adc.service.data.catalog;

import com.ericsson.oss.adc.models.DataCatalogProperties;
import com.ericsson.oss.adc.models.Subscription;
import com.ericsson.oss.adc.models.data.catalog.v2.MessageSchemaListV2;
import com.ericsson.oss.adc.util.ResponseEntityDTO;
import com.ericsson.oss.adc.util.RestExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;

import static com.ericsson.oss.adc.models.DataCatalogConstants.SUBSCRIPTION_STATUS;

/**
 * Implementation of a handler to communicate with the Data Management and Movement (DM&M) Data Catalog service.
 * This includes different ways of getting the input topic entity from the service, including by
 * id, name, name and message bus ID, and simply getting all input topics in message schema v2 format stored on the data catalog service.
 */
@Service
public class DataCatalogServiceV2 {

    @Value("${dmm.data-catalog.base-url}")
    private String dataCatalogBaseUrl;

    @Value("${dmm.data-catalog.base-port}")
    private String dataCatalogBasePort;

    @Value("${dmm.data-catalog.message-schema-uri-v2}")
    private String messageSchemaUriV2;

    @Value("${dmm.data-catalog.subscription-uri}")
    private String subscriptionUri;

    @Autowired
    private RestExecutor restExecutor;

    @Autowired
    DataCatalogProperties dataCatalogProperties;

    private static final String LOG_MESSAGE = "[Requesting {0} from data-catalog]";

    private static final Logger LOG = LoggerFactory.getLogger(DataCatalogService.class);

    /**
     * Gets all the message schema topics from the message v2 entity stored on data catalog
     * by its data space and data category.
     *
     * @param dataSpace        the data space type eg. 4G, 5G
     * @param dataCategory     the data category eg. PM_STATS
     * @return A list of the retrieved FileFormat objects based on the dataProviderType, dataSpace and dataCategory
     */
    public ResponseEntity<MessageSchemaListV2> getMessageSchemaListV2ByDataSpaceAndDataCategory(final String dataSpace, final String dataCategory) {
        final String url = MessageFormat.format("{0}{1}{2}?dataSpace={3}&dataCategory={4}",
                dataCatalogBaseUrl, dataCatalogBasePort, messageSchemaUriV2, dataSpace, dataCategory);
        LOG.info("GET Message Schema V2 list by Data Provider Type and Data Space: {}", url);
        final ResponseEntityDTO responseEntityDTO = restExecutor.exchange(url, MessageFormat.format(LOG_MESSAGE, "Message Schema V2 by Data Space and Data Category"), HttpMethod.GET, MessageSchemaListV2.class);
        return responseEntityDTO.getResponseEntity();
    }

    public Subscription[] getSubscriptions() {
        ResponseEntity<Subscription[]> response = getAllSubscriptionsByParams(dataCatalogProperties.getDataServiceName(), SUBSCRIPTION_STATUS);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            LOG.info("There are {} active subscriptions", response.getBody().length);//NOSONAR
            return response.getBody();
        } else {
            LOG.error("Cannot get subscriptions by the following params: {}, {}.", dataCatalogProperties.getDataServiceName(), SUBSCRIPTION_STATUS);
            return new Subscription[]{};
        }
    }

    public ResponseEntity<Subscription[]> getAllSubscriptionsByParams(String dataServiceName, String status) {
        String url = MessageFormat.format("{0}{1}{2}?serviceName={3}&status={4}",
                dataCatalogBaseUrl, dataCatalogBasePort, subscriptionUri, dataServiceName, status);
        final String message = String.format("Get All Subscriptions By [serviceName=%s, status=%s] Params", dataServiceName, status);
        final ResponseEntityDTO<Subscription[]> responseEntityDTO = restExecutor.exchange(url,
                MessageFormat.format(LOG_MESSAGE, message),
                HttpMethod.GET, Subscription[].class);
        return responseEntityDTO.getResponseEntity();
    }
}
