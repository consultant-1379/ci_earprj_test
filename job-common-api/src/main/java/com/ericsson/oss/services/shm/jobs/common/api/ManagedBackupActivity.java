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
package com.ericsson.oss.services.shm.jobs.common.api;

import java.util.List;

/**
 * To provide the Activity Information for ManageBackup
 * 
 * @author xmalsru
 * 
 */
public interface ManagedBackupActivity {

    /**
     * @param jobActivitiesQueryList
     * @return
     */
    List<JobActivitiesResponse> getManageBackupNeTypeActivities(List<ManageBackupActivitiesQuery> jobActivitiesQueryList);

}
