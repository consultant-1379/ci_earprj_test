/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.cpp.common;

import java.util.Map;

public class UpgradePackageMO {

    final private String fdn;
    final private Map<String, Object> allAttributes;

    public UpgradePackageMO(final String fdn, final Map<String, Object> allAttributes) {
        this.allAttributes = allAttributes;
        this.fdn = fdn;
    }

    /**
     * @return the fdn
     */
    public String getFdn() {
        return fdn;
    }

    /**
     * @return the allAttributes
     */
    public Map<String, Object> getAllAttributes() {
        return allAttributes;
    }

    @Override
    public String toString() {
        return "UpgradePackageMO [fdn=" + fdn + ", allAttributes=" + allAttributes + "]";
    }

}
