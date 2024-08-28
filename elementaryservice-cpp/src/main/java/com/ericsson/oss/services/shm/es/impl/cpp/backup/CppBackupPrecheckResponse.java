/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.cpp.backup;

import java.util.Map;

import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.common.PrecheckResponse;

/**
 * A POJO class for holding the common data required by Precheck and Execute steps of {@link CreateCvService}.
 * 
 * @author xarirud
 * 
 */
public class CppBackupPrecheckResponse extends PrecheckResponse {
    private final Map<String, Object> cvMoAttributes;
    private final Map<String, Object> actionArguments;

    /**
     * Parameterised constructor.
     * 
     * @param precheckStatus
     * @param cvMoAttributesMap
     * @param actionArguments
     */
    public CppBackupPrecheckResponse(final ActivityStepResultEnum precheckStatus, final Map<String, Object> cvMoAttributesMap, final Map<String, Object> actionArguments) {
        super(precheckStatus);
        this.cvMoAttributes = cvMoAttributesMap;
        this.actionArguments = actionArguments;
    }

    /**
     * @return the cvMoAttributes
     */
    public Map<String, Object> getCvMoAttributes() {
        return this.cvMoAttributes;
    }

    /**
     * @return the actionArguments
     */
    public Map<String, Object> getActionArguments() {
        return actionArguments;
    }

}
