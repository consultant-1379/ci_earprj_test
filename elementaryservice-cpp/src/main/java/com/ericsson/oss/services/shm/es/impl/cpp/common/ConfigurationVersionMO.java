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

import java.util.*;

import com.ericsson.oss.shm.inventory.backup.entities.AdminProductData;

public class ConfigurationVersionMO {

    final private String fdn;
    private CvActivity cvActivity;
    private CvActionMainAndAdditionalResultHolder cvMainActionResultHolder;
    private List<AdminProductData> missingUps = new ArrayList<AdminProductData>();
    private List<AdminProductData> corruptedUps = new ArrayList<AdminProductData>();
    private Map<String, Object> cvMoAttributes;

    /**
     * @param fdn
     * @param cvActivity
     * @param cvMainActionHolder
     */
    public ConfigurationVersionMO(final String fdn, final CvActivity cvActivity, final CvActionMainAndAdditionalResultHolder cvMainActionHolder, final List<AdminProductData> missingUps,
            final List<AdminProductData> corruptedUps) {
        this.fdn = fdn;
        this.cvActivity = cvActivity;
        this.cvMainActionResultHolder = cvMainActionHolder;
        this.missingUps = missingUps;
        this.corruptedUps = corruptedUps;
    }

    /**
     * 
     */
    public ConfigurationVersionMO(final String fdn, final Map<String, Object> cvMoAttributes) {
        this.cvMoAttributes = cvMoAttributes;
        this.fdn = fdn;
    }

    /**
     * @return the missingUps
     */
    public List<AdminProductData> getMissingUps() {
        return missingUps;
    }

    /**
     * @return the corruptedUps
     */
    public List<AdminProductData> getCorruptedUps() {
        return corruptedUps;
    }

    /**
     * @return the fdn
     */
    public String getFdn() {
        return fdn;
    }

    /**
     * @return the cvActivity
     */
    public CvActivity getCvActivity() {
        return cvActivity;
    }

    /**
     * @return the cvMainActionHolder
     */
    public CvActionMainAndAdditionalResultHolder getCvMainActionResultHolder() {
        return cvMainActionResultHolder;
    }

    public Map<String, Object> getAllAttributes() {
        return cvMoAttributes;
    }
}
