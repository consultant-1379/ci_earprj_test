package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.softwareupgrade.tasks;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.DPSUtils;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@RunWith(MockitoJUnitRunner.class)
public class PrecheckTaskTest {

    @InjectMocks
    private PrecheckTask precheckTask;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Mock
    private JobStaticDataProvider jobStaticDataProvider;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    private JobStaticData jobStaticData;

    @Mock
    private JobEnvironment jobEnvironment;

    @Mock
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private DPSUtils dpsUtils;

    @Mock
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculator;

    private static final Logger LOGGER = LoggerFactory.getLogger(PrecheckTaskTest.class);
    private static final String EXCEPTION = "Exception caught :: {}";
    private static final long ACTIVITY_JOB_ID = 123;

    @Test
    public void testPrecheckFailedSkipExecution() {
        try {
            when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITY_JOB_ID, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
            when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
            when(activityJobTBACValidator.validateTBAC(ACTIVITY_JOB_ID, neJobStaticData, jobStaticData, ActivityConstants.UPGRADE)).thenReturn(false);
            final ActivityStepResult precheckResult = precheckTask.activityPreCheck(ACTIVITY_JOB_ID, ActivityConstants.UPGRADE);
            assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, precheckResult.getActivityResultEnum());
        } catch (JobDataNotFoundException | MoNotFoundException e) {
            LOGGER.info(EXCEPTION, e);
        }
    }

    @Test
    public void testPrecheckInventorySupervisionDisabled() {
        try {
            when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobEnvironment);
            when(jobEnvironment.getNodeName()).thenReturn("ML-6352");
            when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITY_JOB_ID, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
            when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
            when(activityJobTBACValidator.validateTBAC(ACTIVITY_JOB_ID, neJobStaticData, jobStaticData, ActivityConstants.UPGRADE)).thenReturn(true);
            when(dpsUtils.isInventorySupervisionEnabled("ML-6352")).thenReturn(false);
            final ActivityStepResult precheckResult = precheckTask.activityPreCheck(ACTIVITY_JOB_ID, ActivityConstants.UPGRADE);
            assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, precheckResult.getActivityResultEnum());
        } catch (JobDataNotFoundException | MoNotFoundException e) {
            LOGGER.info(EXCEPTION, e);
        }
    }

    @Test
    public void testPrecheckSucceedsInValidState() {
        try {
            when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobEnvironment);
            when(jobEnvironment.getNodeName()).thenReturn("ML-6352");
            when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITY_JOB_ID, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
            when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
            when(activityJobTBACValidator.validateTBAC(ACTIVITY_JOB_ID, neJobStaticData, jobStaticData, ActivityConstants.UPGRADE)).thenReturn(true);
            when(dpsUtils.isInventorySupervisionEnabled("ML-6352")).thenReturn(true);
            final ActivityStepResult precheckResult = precheckTask.activityPreCheck(ACTIVITY_JOB_ID, ActivityConstants.UPGRADE);
            assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, precheckResult.getActivityResultEnum());
        } catch (JobDataNotFoundException | MoNotFoundException e) {
            LOGGER.info(EXCEPTION, e);
        }
    }
}
