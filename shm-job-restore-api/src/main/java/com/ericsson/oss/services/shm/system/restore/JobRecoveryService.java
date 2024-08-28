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
package com.ericsson.oss.services.shm.system.restore;

import java.util.List;
import java.util.concurrent.Future;

public interface JobRecoveryService {

    Future<List<JobRestoreResult>> handleJobRestore();

    /**
     * 
     */
    void cancelAllWorkFlows();
}
