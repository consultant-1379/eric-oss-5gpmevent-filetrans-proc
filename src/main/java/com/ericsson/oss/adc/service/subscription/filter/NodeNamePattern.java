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
package com.ericsson.oss.adc.service.subscription.filter;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Utility wrapper class for Pattern, this is to ensure that the equals and hashcode correctly
 * compares two equivalent nodeName patterns supplied by a Rapp, so they are logically grouped together
 */
@Data
@AllArgsConstructor
public class NodeNamePattern {
    private String originalString;
    private Pattern pattern;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeNamePattern that = (NodeNamePattern) o;
        return Objects.equals(originalString, that.originalString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalString);
    }

}
