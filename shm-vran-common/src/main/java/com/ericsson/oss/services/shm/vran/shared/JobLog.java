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
package com.ericsson.oss.services.shm.vran.shared;

import java.util.Date;
import java.util.HashMap;

import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

public class JobLog extends HashMap<String, Object> {

    private static final long serialVersionUID = 3979638287823966166L;

    public void setMessage(final String message) {
        put(ActivityConstants.JOB_LOG_MESSAGE, message);
    }

    public void setTime(final Date entryTime) {
        put(ActivityConstants.JOB_LOG_ENTRY_TIME, entryTime);
    }

    public void setSource(final String source) {
        put(ActivityConstants.JOB_LOG_TYPE, source);
    }

    public void setServerity(final String severity) {
        put(ActivityConstants.JOB_LOG_LEVEL, severity);
    }
}
