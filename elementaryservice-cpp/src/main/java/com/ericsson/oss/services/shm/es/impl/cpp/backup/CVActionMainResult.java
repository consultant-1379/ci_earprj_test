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
package com.ericsson.oss.services.shm.es.impl.cpp.backup;

/**
 * 
 * @author xchedoo added NOTFOUND to handle as default value in case at the time of reading from the node, main action result is not updated yet.
 */

public enum CVActionMainResult {

    NOTFOUND(-1, "The Main action result not found"), EXECUTED(0, "The invoked action has been successfully executed without warnings."), EXECUTED_WITH_WARNINGS(1,
            "The action has been successfully executed but warnings were generated."), EXECUTION_FAILED(2, "The execution of the invoked action failed.");

    private int mainResultId;
    private String mainResultMessage;

    private CVActionMainResult(final int mainResultId, final String mainResultMessage) {
        this.mainResultId = mainResultId;
        this.mainResultMessage = mainResultMessage;
    }

    public int getMainResultId() {
        return mainResultId;
    }

    public String getMainResultMessage() {
        return mainResultMessage;
    }

    public static CVActionMainResult getCvActionMainResult(final String cvActionMainResult) {

        for (final CVActionMainResult s : CVActionMainResult.values()) {
            if (s.name().equalsIgnoreCase(cvActionMainResult)) {
                return s;
            }
        }
        return CVActionMainResult.NOTFOUND;
    }
}
