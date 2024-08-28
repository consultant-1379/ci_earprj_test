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
package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.deletebackup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.cds.cdi.support.configuration.InjectionProperties;
import com.ericsson.cds.cdi.support.rule.CdiInjectorRule;
import com.ericsson.cds.cdi.support.rule.ImplementationInstance;
import com.ericsson.cds.cdi.support.rule.MockedImplementation;
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest;
import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.DeleteSmrsBackupUtil;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.ActivityUtilsStub;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

public class DeleteBackupServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteBackupService.class);
    private static final long ACTIVITY_JOB_ID = 123;
    private static final String NODE_NAME = "ML-TN";
    private static final String BACKUP_NAME_TEST = "backup";
    private static final String DELETEBACKUP_EXECUTE_LOG_0 = "Executing \"deletebackup\" activity on backup file = \"" + BACKUP_NAME_TEST + "\".";
    private static final String DELETEBACKUP_EXECUTE_LOG_1 = "Backup On Enm: backup has been deleted successfully.";
    private static final String DELETEBACKUP_EXECUTE_LOG_2 = "Backup On Enm: backup cannot be deleted .";
    private static final String DELETEBACKUP_PRECHECK_LOG = "Precheck for \"deletebackup\" is successful.";
    private static final String DELETEBACKUP_CANCELTIMEOUT_LOG_0 = "Notifications not received for the \"deletebackup\" activity after cancel is triggered. Retrieving status from node.";
    private static final String DELETEBACKUP_CANCELTIMEOUT_LOG_1 = "Backup On Enm: backup has been deleted successfully after timeout.";
    private static final String SUCCESS = "SUCCESS";
    private static final String FAILED = "FAILED";
    private static final String MESSAGE = "message";
    private static final String DO_NOTHING = "Do Nothing";

    private final InjectionProperties injectionProperties = new InjectionProperties().autoLocateFrom("com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.deletebackup");
    @Rule
    public CdiInjectorRule cdiInjectorRule = new CdiInjectorRule(this, injectionProperties);

    @ObjectUnderTest
    private DeleteBackupService deleteBackupService;

    @ImplementationInstance
    private final ActivityUtils activityUtils = new ActivityUtilsStub(ACTIVITY_JOB_ID, NODE_NAME, BACKUP_NAME_TEST);

    @MockedImplementation
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    @MockedImplementation
    private FdnServiceBean fdnServiceBean;

    @MockedImplementation
    private JobPropertyUtils jobPropertyUtils;

    @MockedImplementation
    private JobUpdateService jobUpdateService;

    @MockedImplementation
    private DeleteSmrsBackupUtil deleteSmrsBackupService;

    @Before
    public void setup() {
        final NetworkElement ne = mock(NetworkElement.class);
        when(ne.getPlatformType()).thenReturn(PlatformTypeEnum.MINI_LINK_OUTDOOR);
        when(ne.getNeType()).thenReturn(PlatformTypeEnum.MINI_LINK_OUTDOOR.getName());
        when(fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(Matchers.anyListOf(String.class))).thenReturn(Collections.singletonList(ne));
        when(fdnServiceBean.getNetworkElementsByNeNames(Matchers.anyListOf(String.class))).thenReturn(Collections.<NetworkElement> singletonList(ne));
        when(jobPropertyUtils.getPropertyValue(Matchers.anyListOf(String.class), Matchers.anyMapOf(String.class, Object.class), Matchers.anyString(), Matchers.anyString(), Matchers.anyString()))
                .thenReturn(Collections.<String, String> singletonMap("BACKUP_NAME", BACKUP_NAME_TEST));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testPrecheck() {
        LOGGER.info("Test Precheck");
        final ArgumentCaptor<List> jobLogListCaptor = ArgumentCaptor.forClass(List.class);
        final ActivityStepResult result = deleteBackupService.precheck(ACTIVITY_JOB_ID);

        verify(jobUpdateService).readAndUpdateRunningJobAttributes(eq(ACTIVITY_JOB_ID), Matchers.anyList(), jobLogListCaptor.capture(), Matchers.anyDouble());

        assertTrue(result.getActivityResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);

        final List<Map<String, Object>> jobLogList = jobLogListCaptor.getValue();
        assertEquals(DELETEBACKUP_PRECHECK_LOG, jobLogList.get(0).get(MESSAGE));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testExecuteDeleteSuccessfully() {
        LOGGER.info("Test execute in case of success.");
        final ArgumentCaptor<List> jobLogListCaptor = ArgumentCaptor.forClass(List.class);
        final ArgumentCaptor<List> jobPropertyListCaptor = ArgumentCaptor.forClass(List.class);

        when(deleteSmrsBackupService.deleteBackupOnSmrs(NODE_NAME, BACKUP_NAME_TEST, PlatformTypeEnum.MINI_LINK_OUTDOOR.getName())).thenReturn(true);

        deleteBackupService.execute(ACTIVITY_JOB_ID);

        verify(deleteSmrsBackupService).deleteBackupOnSmrs(NODE_NAME, BACKUP_NAME_TEST, PlatformTypeEnum.MINI_LINK_OUTDOOR.getName());
        verify(jobUpdateService, times(3)).readAndUpdateRunningJobAttributes(eq(ACTIVITY_JOB_ID), jobPropertyListCaptor.capture(), jobLogListCaptor.capture(), Matchers.anyDouble());

        final List<Map<String, Object>> jobLogList = jobLogListCaptor.getValue();
        final List<Map<String, Object>> jobPropertyList = jobPropertyListCaptor.getValue();

        assertEquals(2, jobLogList.size());
        assertEquals(DELETEBACKUP_EXECUTE_LOG_0, jobLogList.get(0).get(MESSAGE));
        assertEquals(DELETEBACKUP_EXECUTE_LOG_1, jobLogList.get(1).get(MESSAGE));

        assertEquals(2, jobPropertyList.size());
        assertEquals(ActivityConstants.IS_ACTIVITY_TRIGGERED, jobPropertyList.get(0).get(ActivityConstants.JOB_PROP_KEY));
        assertEquals("true", jobPropertyList.get(0).get(ActivityConstants.JOB_PROP_VALUE));
        assertEquals("result", jobPropertyList.get(1).get(ActivityConstants.JOB_PROP_KEY));
        assertEquals(SUCCESS, jobPropertyList.get(1).get(ActivityConstants.JOB_PROP_VALUE));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testExecuteCannotDelete() {
        LOGGER.info("Test execute in case of cannot delete backup.");
        final ArgumentCaptor<List> jobLogListCaptor = ArgumentCaptor.forClass(List.class);
        final ArgumentCaptor<List> jobPropertyListCaptor = ArgumentCaptor.forClass(List.class);

        when(deleteSmrsBackupService.deleteBackupOnSmrs(NODE_NAME, BACKUP_NAME_TEST, PlatformTypeEnum.MINI_LINK_OUTDOOR.getName())).thenReturn(false);

        deleteBackupService.execute(ACTIVITY_JOB_ID);

        verify(deleteSmrsBackupService).deleteBackupOnSmrs(NODE_NAME, BACKUP_NAME_TEST, PlatformTypeEnum.MINI_LINK_OUTDOOR.getName());
        verify(jobUpdateService, times(3)).readAndUpdateRunningJobAttributes(eq(ACTIVITY_JOB_ID), jobPropertyListCaptor.capture(), jobLogListCaptor.capture(), Matchers.anyDouble());

        final List<Map<String, Object>> jobLogList = jobLogListCaptor.getValue();
        final List<Map<String, Object>> jobPropertyList = jobPropertyListCaptor.getValue();

        assertEquals(2, jobLogList.size());
        assertEquals(DELETEBACKUP_EXECUTE_LOG_0, jobLogList.get(0).get(MESSAGE));
        assertEquals(DELETEBACKUP_EXECUTE_LOG_2, jobLogList.get(1).get(MESSAGE));

        assertEquals(3, jobPropertyList.size());
        assertEquals(ActivityConstants.IS_ACTIVITY_TRIGGERED, jobPropertyList.get(0).get(ActivityConstants.JOB_PROP_KEY));
        assertEquals("true", jobPropertyList.get(0).get(ActivityConstants.JOB_PROP_VALUE));
        assertEquals("INTERMEDIATE_FAILURE", jobPropertyList.get(1).get(ActivityConstants.JOB_PROP_KEY));
        assertEquals(FAILED, jobPropertyList.get(1).get(ActivityConstants.JOB_PROP_VALUE));
        assertEquals("result", jobPropertyList.get(2).get(ActivityConstants.JOB_PROP_KEY));
        assertEquals(FAILED, jobPropertyList.get(2).get(ActivityConstants.JOB_PROP_VALUE));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testCancel() {
        LOGGER.info("Test cancel.");
        final ArgumentCaptor<List> jobLogListCaptor = ArgumentCaptor.forClass(List.class);
        final ArgumentCaptor<List> jobPropertyListCaptor = ArgumentCaptor.forClass(List.class);
        deleteBackupService.cancel(ACTIVITY_JOB_ID);

        verify(jobUpdateService).readAndUpdateJobAttributesForCancel(eq(ACTIVITY_JOB_ID), jobPropertyListCaptor.capture(), jobLogListCaptor.capture());

        final List<Map<String, Object>> jobPropertyList = jobPropertyListCaptor.getValue();

        assertEquals(1, jobPropertyList.size());
        assertEquals(ActivityConstants.IS_CANCEL_TRIGGERED, jobPropertyList.get(0).get(ActivityConstants.JOB_PROP_KEY));
        assertEquals("true", jobPropertyList.get(0).get(ActivityConstants.JOB_PROP_VALUE));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testCancelTimeout() {
        LOGGER.info("Test CancelTimeout");
        final ArgumentCaptor<List> jobLogListCaptor = ArgumentCaptor.forClass(List.class);
        final ArgumentCaptor<List> jobPropertyListCaptor = ArgumentCaptor.forClass(List.class);
        deleteBackupService.cancelTimeout(ACTIVITY_JOB_ID, true);
        verify(jobUpdateService, times(2)).readAndUpdateJobAttributesForCancel(eq(ACTIVITY_JOB_ID), jobPropertyListCaptor.capture(), jobLogListCaptor.capture());

        final List<Map<String, Object>> jobLogList = jobLogListCaptor.getValue();

        assertEquals(2, jobLogList.size());
        assertEquals(DELETEBACKUP_CANCELTIMEOUT_LOG_0, jobLogList.get(0).get(MESSAGE));
        assertEquals(DELETEBACKUP_CANCELTIMEOUT_LOG_1, jobLogList.get(1).get(MESSAGE));
    }

    @ImplementationInstance
    private final NeJobStaticDataProvider neJobStaticDataProvider = new NeJobStaticDataProvider() {

        @Override
        public NEJobStaticData getNeJobStaticData(final long activityJobId, final String capability) {
            return new NEJobStaticData(123L, 345L, "ML-TN", "businessKey", PlatformTypeEnum.MINI_LINK_OUTDOOR.getName(), new Date().getTime(), null);
        }

        @Override
        public void updateNeJobStaticDataCache(final long activityJobId, final String platformCapbility, final long activityStartTime) throws JobDataNotFoundException {
            LOGGER.info(DO_NOTHING);
        }

        @Override
        public void clear(final long activityJobId) {
            LOGGER.info(DO_NOTHING);
        }

        @Override
        public void clearAll() {
            LOGGER.info(DO_NOTHING);
        }

        @Override
        public void put(final long activityJobId, final NEJobStaticData neJobStaticData) {
            LOGGER.info(DO_NOTHING);
        }

        @Override
        public long getActivityStartTime(final long activityJobId) {
            return 0;
        }
    };

    @ImplementationInstance
    private final JobStaticDataProvider jobStaticDataProvider = new JobStaticDataProvider() {

        @Override
        public JobStaticData getJobStaticData(final long mainJobId) {
            return new JobStaticData("", new HashMap<String, Object>(), "", JobType.DELETEBACKUP, "");
        }

        @Override
        public void clear(final long activityJobId) {
            LOGGER.info(DO_NOTHING);
        }

        @Override
        public void clearAll() {
            LOGGER.info(DO_NOTHING);
        }

        @Override
        public void put(final long mainJobId, final JobStaticData jobStaticData) {
            LOGGER.info(DO_NOTHING);
        }
    };

    @ImplementationInstance
    private final ActivityJobTBACValidator activityJobTBACValidator = new ActivityJobTBACValidator() {

        @Override
        public boolean validateTBAC(final long activityJobId, final NEJobStaticData neJobStaticData, final JobStaticData jobStaticData, final String activityName) {
            return true;
        }
    };
}
