/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JobFailureAlarmParameterChangeListenerTest {

    @Test
    public void testAllAlaramParameters() {
        JobFailureAlarmParameterChangeListener jobFailureAlarmParameterChangeListener = createAndGetListnerObject();
        assertEquals(true, jobFailureAlarmParameterChangeListener.isAlarmNeededOnBackupHousekeepingJobFailure());
        assertEquals(true, jobFailureAlarmParameterChangeListener.isAlarmNeededOnBackupJobFailure());
        assertEquals(false, jobFailureAlarmParameterChangeListener.isAlarmNeededOnDeleteBackupJobFailure());
        assertEquals(true, jobFailureAlarmParameterChangeListener.isAlarmNeededOnDeleteSoftwarePackageJobFailure());
        assertEquals(false, jobFailureAlarmParameterChangeListener.isAlarmNeededOnDeleteUpgradepackageJobFailure());
        assertEquals(true, jobFailureAlarmParameterChangeListener.isAlarmNeededOnLicenseJobFailure());
        assertEquals(true, jobFailureAlarmParameterChangeListener.isAlarmNeededOnLicenseRefreshJobFailure());
        assertEquals(false, jobFailureAlarmParameterChangeListener.isAlarmNeededOnOboardJobFailure());
        assertEquals(true, jobFailureAlarmParameterChangeListener.isAlarmNeededOnRestoreJobFailure());
        assertEquals(true, jobFailureAlarmParameterChangeListener.isAlarmNeededOnShmJobFailure());
        assertEquals(false, jobFailureAlarmParameterChangeListener.isAlarmNeededOnUpgradeJobFailure());
    }

    /**
     * @return
     */
    private JobFailureAlarmParameterChangeListener createAndGetListnerObject() {
        JobFailureAlarmParameterChangeListener jobFailureAlarmParameterChangeListener = new JobFailureAlarmParameterChangeListener();

        jobFailureAlarmParameterChangeListener.listenForBackupHousekeepingJobFailureAlarmAttribute(true);
        jobFailureAlarmParameterChangeListener.listenForBackupJobFailureAlarmAttribute(true);
        jobFailureAlarmParameterChangeListener.listenForDefaultJobFailureAlarmAttribute(true);
        jobFailureAlarmParameterChangeListener.listenForDeleteBackupJobFailureAlarmAttribute(false);
        jobFailureAlarmParameterChangeListener.listenForDeleteSoftwarePackageJobFailureAlarmAttribute(true);
        jobFailureAlarmParameterChangeListener.listenForDeleteUpgradepackageJobFailureAlarmAttribute(false);
        jobFailureAlarmParameterChangeListener.listenForLicenseJobFailureAlarmAttribute(true);
        jobFailureAlarmParameterChangeListener.listenForLicenseRefreshJobFailureAlarmAttribute(true);
        jobFailureAlarmParameterChangeListener.listenForOnboardJobFailureAlarmAttribute(false);
        jobFailureAlarmParameterChangeListener.listenForRestoreJobFailureAlarmAttribute(true);
        jobFailureAlarmParameterChangeListener.listenForUpgradeJobFailureAlarmAttribute(false);

        return jobFailureAlarmParameterChangeListener;
    }

}
