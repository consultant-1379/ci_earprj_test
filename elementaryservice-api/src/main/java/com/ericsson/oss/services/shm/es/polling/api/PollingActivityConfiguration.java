/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.polling.api;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

/**
 * Interface for polling related configurations.
 * 
 * @author xsrirda
 * 
 */

public interface PollingActivityConfiguration {

    boolean isPollingEnabledForJobsOnCppNodes();

    boolean isPollingEnabledForJobsOnEcimNodes();

    boolean isPollingEnabledForUpgradeJobOnCppNodes();

    boolean isPollingEnabledForBackupJobOnCppNodes();

    boolean isPollingEnabledForRestoreJobOnCppNodes();

    boolean isPollingEnabledForUpgradeJobOnEcimNodes();

    boolean isPollingEnabledForBackupJobOnEcimNodes();

    boolean isPollingEnabledForRestoreJobOnEcimNodes();

    int getInitialDelayToStartTimerAfterServiceStartUp();

    int getCppUpgradeJobUpgradeActivityWaitTimeToStartPolling();

    int getPollingIntervalDelay();

    int getCppRestoreJobRestoreActivityWaitTimeToStartPolling();

    int getShmHeartBeatIntervalForEcim();

    boolean isPollingEnabled(final PlatformTypeEnum platformType, final JobTypeEnum jobTypeEnum);

    long getOperationTimeOutBasedOnPlatformType(final String platform);

    int getIntervalTimeForMoActionCache();

    boolean isPollingEnabledForJobsOnAxeNodes();

    boolean isPollingEnabledForBackupJobOnAxeNodes();

}
