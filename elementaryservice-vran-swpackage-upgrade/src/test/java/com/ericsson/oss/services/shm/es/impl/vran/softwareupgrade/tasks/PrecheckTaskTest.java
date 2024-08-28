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
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.nejob.cache.*;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

@RunWith(MockitoJUnitRunner.class)
public class PrecheckTaskTest {

    @InjectMocks
    private PrecheckTask precheckTask;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Mock
    private VranJobActivityServiceHelper vranJobActivityServiceHelper;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Mock
    private JobStaticDataProvider jobStaticDataProvider;

    @Mock
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    private JobStaticData jobStaticData;

    @Mock
    private ActivityStepResult activityStepResult;

    static final long activityJobId = 345;

    final long MAINJOB_ID = 4;

    @Test
    public void testActivityPreCheck_Prepare() {
        precheckTask.activityPreCheck(activityJobId, ActivityConstants.PREPARE);
    }

    @Test
    public void testActivityPreCheck_Verify() {

        precheckTask.activityPreCheck(activityJobId, ActivityConstants.VERIFY);
    }

    @Test
    public void testActivityPreCheck_Activate() throws JobDataNotFoundException, MoNotFoundException {
        final ActivityStepResultEnum activityResultEnum = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
        when(activityStepResult.getActivityResultEnum()).thenReturn(activityResultEnum);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ActivityConstants.ACTIVATE)).thenReturn(true);
        precheckTask.activityPreCheck(activityJobId, ActivityConstants.ACTIVATE);
    }

    @Test
    public void testActivityPreCheck_Confirm() {

        precheckTask.activityPreCheck(activityJobId, ActivityConstants.CONFIRM);
    }

    @Test
    public void testPrecheck_WhenTbacNotEnabled() throws JobDataNotFoundException, MoNotFoundException {
        final ActivityStepResultEnum activityResultEnum = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
        when(activityStepResult.getActivityResultEnum()).thenReturn(activityResultEnum);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ActivityConstants.ACTIVATE)).thenReturn(false);
        precheckTask.activityPreCheck(activityJobId, ActivityConstants.ACTIVATE);
        assertNotNull(activityStepResult);
    }

}
