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

import java.util.List;

import com.ericsson.oss.services.shm.es.impl.cpp.backup.CVActionMainResult;

/**
 * Each instance of this POJO holds CVMO attributes: actionID,mainResult and ,CVActionAdditionalResultData
 * 
 * @author xchedoo
 * 
 */

public class CvActionMainAndAdditionalResultHolder {
    final private int actionId;
    final private CVActionMainResult cvActionMainResult;
    final private String pathToDetailInformation;
    final private List<CvActionAdditionalInfo> actionAdditionalResult;

    /**
     * @param actionId
     * @param cvActionMainResult
     * @param pathToDetailInformation
     * @param actionAdditionalResult
     */
    public CvActionMainAndAdditionalResultHolder(final int actionId, final CVActionMainResult cvActionMainResult, final String pathToDetailInformation,
            final List<CvActionAdditionalInfo> actionAdditionalResult) {
        this.actionId = actionId;
        this.cvActionMainResult = cvActionMainResult;
        this.pathToDetailInformation = pathToDetailInformation;
        this.actionAdditionalResult = actionAdditionalResult;
    }

    /**
     * @return the actionAdditionalResult
     */
    public List<CvActionAdditionalInfo> getActionAdditionalResult() {
        return actionAdditionalResult;
    }

    /**
     * @return the pathToDetailInformation
     */
    public String getPathToDetailInformation() {
        return pathToDetailInformation;
    }

    /**
     * @return the actionId
     */
    public int getActionId() {
        return actionId;
    }

    /**
     * @return the cvActionMainResult
     */
    public CVActionMainResult getCvActionMainResult() {
        return cvActionMainResult;
    }

}
