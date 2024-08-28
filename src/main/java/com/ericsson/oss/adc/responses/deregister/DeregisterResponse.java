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

package com.ericsson.oss.adc.responses.deregister;

import com.ericsson.oss.adc.enums.DeregisterStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatusCode;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeregisterResponse {

    private DeregisterStatus deregisterStatus;
    private String message;

    /**
     * Build the response.
     * @param dataServiceInstanceName The DataServiceInstance deleted.
     * @param deregisterStatus The status of the deletion (i.e. SUCCESS or FAILURE).
     * @param dataCatalogStatusCode The HTTP status code of the response.
     * @return A {@link DeregisterResponse}
     */
    public static DeregisterResponse build(String dataServiceInstanceName, DeregisterStatus deregisterStatus, HttpStatusCode dataCatalogStatusCode) {
        String messageHeadline;
        switch (deregisterStatus) {
            case SUCCESS:
                messageHeadline = "Data Service Instance '" + dataServiceInstanceName + "' deleted";
                break;
            case FAILURE:
                messageHeadline = "FAILED to delete Data Service Instance: '" + dataServiceInstanceName + "'";
                break;
            default:
                messageHeadline = "'" + dataServiceInstanceName + "' <No Message>";
        }

        return builder().deregisterStatus(deregisterStatus)
                .message(messageHeadline + ", Data Catalog HTTP Status: " + dataCatalogStatusCode)
                .build();
    }

}
