/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.shm.es.impl.minilink.upgrade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.ericsson.cds.cdi.support.configuration.InjectionProperties;
import com.ericsson.cds.cdi.support.rule.CdiInjectorRule;
import com.ericsson.cds.cdi.support.rule.ImplementationInstance;
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.ServerInternalException;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobUpdateServiceImpl;
import com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

public class ConfirmServiceTest {

    private final InjectionProperties injectionProperties = new InjectionProperties().autoLocateFrom("com.ericsson.oss.services.shm.es.impl.minilink.upgrade");

    public static final long ACTIVITY_JOB_RAU_ID = 11;
    public static final long ACTIVITY_JOB_SBL_ID = 12;
    public static final long ACTIVITY_JOB_RAU_MANUAL_WAIT_FOR_COMMIT = 21;
    public static final long ACTIVITY_JOB_SBL_PACKAGE_SBL_WAIT_FOR_COMMIT = 22;
    public int activeRelease = 1;

    @Rule
    public CdiInjectorRule cdiInjectorRule = new CdiInjectorRule(this, injectionProperties);

    @ObjectUnderTest
    private ConfirmService confirmService;

    @ImplementationInstance
    private final JobUpdateService jobUpdateService = new JobUpdateServiceImpl() {

        @Override
        public boolean readAndUpdateRunningJobAttributes(final long jobId, final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList) {
            testUtil.addJobLogList(jobId, jobLogList);
            if (jobPropertyList != null) {
                testUtil.addJobPropertyList(jobId, jobPropertyList);
            }
            return true;
        }

    };

    @ImplementationInstance
    private final MiniLinkActivityUtil miniLinkJobUtil = new MiniLinkActivityUtil() {

        @Override
        public boolean isRAUPackage(final long activityJobId) {
            return testUtil.isRAUPackage(activityJobId);
        }

        @Override
        public int getActiveRelease(final long activityJobId) {
            return activeRelease;
        }

        @Override
        public String getSwPkgName(final long activityJobId) {
            return "testPackageName";
        }

        @Override
        public ManagedObject getManagedObject(final long activityJobId, final String type) throws ServerInternalException {
            return testUtil.getManagedObject(activityJobId);
        }

        @Override
        public boolean isXfSwBoardTableUpdated(final Long activityJobId, final List<Map<String, Object>> jobLogList) {
            return Boolean.parseBoolean(testUtil.getManagedObject(activityJobId).getAttribute(MiniLinkConstants.XF_SW_BOARD_TABLE).toString());
        }

        @Override
        public Map<String, Object> createNewLogEntry(final JobLogLevel jobLogLevel, final String logMessage, final Object... placeHolders) {
            return activityUtils.createNewLogEntry(String.format(logMessage, placeHolders), jobLogLevel.toString());
        }

        @Override
        public void finishInstallActivity(final JobActivityInfo jobActivityInfo, final String unsubscribeEventFdn, final JobResult jobResult, final List<Map<String, Object>> jobLogList,
                                          final String activityName){
            testUtil.getManagedObject(jobActivityInfo.getActivityJobId()).setAttribute("result", jobResult);
        }

        @Override
        public void sendNotification(final JobActivityInfo jobActivityInfo, final String activityName){
        }

        @Override
        public void getErrorJobLog(final List<Map<String, Object>> jobLogList, final boolean isRAU, final long activityJobID, final List<String> globalState) {

        }

        @Override
        public String getXfSwReleaseOperStatus(final long activityJobId) {
            return "";
        }

        @Override
        public void updateXfSwReleaseEntry(final long activityJobId, final Map<String, Object> parameters) {
            for (final Map.Entry<String, Object> entry : parameters.entrySet()) {
                testUtil.getManagedObject(activityJobId).getAllAttributes().put(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public void updateXfSwLmUpgradeTable(final long activityJobId, final String xfSwObjectsFdn, final Map<String, Object> parameters) {
            for (final Map.Entry<String, Object> entry : parameters.entrySet()) {
                testUtil.getManagedObject(activityJobId).getAllAttributes().put(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public String fetchJobProperty(final long activityJobId, final String propertyKey) {
            return "1";
        }

        @Override
        public void subscribeToMoNotifications(final String moFdn, final long activityJobId, final JobActivityInfo jobActivityInfo) {
            activityUtils.subscribeToMoNotifications(moFdn, activityJobId, jobActivityInfo);
            activityUtils.subscribeToMoNotifications(getParentFdn(moFdn), activityJobId, jobActivityInfo);
        }

        @Override
        public void setJobProperty(final String key, final String value, final long activityJobId) {

        }

        @Override
        public void unsubscribeFromMoNotifications(final String moFdn, final long activityJobId, final JobActivityInfo jobActivityInfo) {
        }
    };

    @ImplementationInstance
    private final NeJobStaticDataProvider neJobStaticDataProvider = new NeJobStaticDataProvider() {

        @Override
        public NEJobStaticData getNeJobStaticData(final long activityJobId, final String capability) {
            final NEJobStaticData neJobStaticData = new NEJobStaticData(123L, 345L, "testFDN", "businessKey", PlatformTypeEnum.MINI_LINK_INDOOR.getName(), new Date().getTime(), null);
            return neJobStaticData;
        }

        @Override
        public void updateNeJobStaticDataCache(final long activityJobId, final String platformCapbility, final long activityStartTime) throws JobDataNotFoundException {
        }

        @Override
        public void clear(final long activityJobId) {
        }

        @Override
        public void clearAll() {
        }

        @Override
        public void put(final long activityJobId, final NEJobStaticData neJobStaticData) {
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
            final JobStaticData jobStaticData = new JobStaticData("", new HashMap<String, Object>(), "", JobType.UPGRADE,"");
            return jobStaticData;
        }

        @Override
        public void clear(final long activityJobId) {
        }

        @Override
        public void clearAll() {
        }

        @Override
        public void put(final long mainJobId, final JobStaticData jobStaticData) {
        }
    };

    @ImplementationInstance
    private final ActivityJobTBACValidator activityJobTBACValidator = new ActivityJobTBACValidator() {

        @Override
        public boolean validateTBAC(final long activityJobId, final NEJobStaticData neJobStaticData, final JobStaticData jobStaticData, final String activityName) {
            return true;
        }
    };

    private final UpgradeTestUtil testUtil = new UpgradeTestUtil();

    @Inject
    private ActivityUtils activityUtils;

    @Before
    public void setup() {
        testUtil.addManagedObjectToMap(ACTIVITY_JOB_RAU_ID, null, true, MiniLinkConstants.XF_SW_BOARD_TABLE, "false");
        testUtil.addManagedObjectToMap(ACTIVITY_JOB_SBL_ID, null, false);
        testUtil.addManagedObjectToMap(ACTIVITY_JOB_RAU_MANUAL_WAIT_FOR_COMMIT, MiniLinkConstants.xfSwGlobalState.manualWaitForCommit.toString(), true, MiniLinkConstants.XF_SW_COMMIT_TYPE,
                MiniLinkConstants.xfswCommitType.operatorCommit.toString());
        testUtil.addManagedObjectToMap(ACTIVITY_JOB_SBL_PACKAGE_SBL_WAIT_FOR_COMMIT, MiniLinkConstants.xfSwGlobalState.sblWaitForCommit.toString(), false, MiniLinkConstants.XF_SW_COMMIT_TYPE,
                MiniLinkConstants.xfswCommitType.operatorCommit.toString());
    }

    @Test
    public void testPrecheckResultEnum() {
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, confirmService.precheck(ACTIVITY_JOB_RAU_ID).getActivityResultEnum());
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, confirmService.precheck(ACTIVITY_JOB_SBL_ID).getActivityResultEnum());
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, confirmService.precheck(ACTIVITY_JOB_RAU_MANUAL_WAIT_FOR_COMMIT).getActivityResultEnum());
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, confirmService.precheck(ACTIVITY_JOB_SBL_PACKAGE_SBL_WAIT_FOR_COMMIT).getActivityResultEnum());
    }

    @Test
    public void testHandleTimeoutMethod() {
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, confirmService.handleTimeout(ACTIVITY_JOB_SBL_ID).getActivityResultEnum());
    }

    @Test
    public void testCancelMethod() {
        assertEquals(ActivityStepResultEnum.EXECUTION_FAILED, confirmService.cancel(ACTIVITY_JOB_SBL_ID).getActivityResultEnum());
    }

    @Test
    public void testCancelTimeoutMethod() {
        assertEquals(null, confirmService.cancelTimeout(ACTIVITY_JOB_RAU_ID, true).getActivityResultEnum());
    }

    @Test
    public void testProcessNotificationAVC() {
        // RAU
        confirmService.processNotification(testUtil.createNotification(testUtil.createDpsAttributeChangedEvent(null, "noUpgrade"), ACTIVITY_JOB_RAU_ID, NotificationEventTypeEnum.AVC));
        assertEquals(JobResult.FAILED, testUtil.getManagedObject(ACTIVITY_JOB_RAU_ID).getAllAttributes().get("result"));

        testUtil.getManagedObject(ACTIVITY_JOB_RAU_ID).setAttribute(MiniLinkConstants.XF_SW_BOARD_TABLE, "true");
        confirmService.processNotification(testUtil.createNotification(testUtil.createDpsAttributeChangedEvent(null, "noUpgrade"), ACTIVITY_JOB_RAU_ID, NotificationEventTypeEnum.AVC));
        assertEquals(JobResult.SUCCESS, testUtil.getManagedObject(ACTIVITY_JOB_RAU_ID).getAllAttributes().get("result"));

        // SBL
        confirmService.processNotification(testUtil.createNotification(testUtil.createDpsAttributeChangedEvent(null, "noUpgrade"), ACTIVITY_JOB_SBL_ID, NotificationEventTypeEnum.AVC));
        assertEquals(JobResult.FAILED, testUtil.getManagedObject(ACTIVITY_JOB_SBL_ID).getAllAttributes().get("result"));

        activeRelease = 2;

        confirmService.processNotification(testUtil.createNotification(testUtil.createDpsAttributeChangedEvent(null, "noUpgrade"), ACTIVITY_JOB_SBL_ID, NotificationEventTypeEnum.AVC));
        assertEquals(JobResult.SUCCESS, testUtil.getManagedObject(ACTIVITY_JOB_SBL_ID).getAllAttributes().get("result"));
    }

    @Test
    public void testProcessNotificationCreate() {
        // RAU
        confirmService.processNotification(testUtil.createNotification(testUtil.createDpsObjectCreatedEvent("noUpgrade"), ACTIVITY_JOB_RAU_ID, NotificationEventTypeEnum.CREATE));
        assertEquals(JobResult.FAILED, testUtil.getManagedObject(ACTIVITY_JOB_RAU_ID).getAllAttributes().get("result"));

        // SBL
        confirmService.processNotification(testUtil.createNotification(testUtil.createDpsObjectCreatedEvent("noUpgrade"), ACTIVITY_JOB_SBL_ID, NotificationEventTypeEnum.CREATE));
        assertEquals(JobResult.FAILED, testUtil.getManagedObject(ACTIVITY_JOB_SBL_ID).getAllAttributes().get("result"));
    }

    @Test
    public void testExecute() {
        confirmService.execute(ACTIVITY_JOB_RAU_ID);
        assertNotNull(testUtil.getManagedObject(ACTIVITY_JOB_RAU_ID).getAllAttributes().get(MiniLinkConstants.XF_SW_LMUPGRADE_ADMIN_STATUS));
        assertEquals(MiniLinkConstants.XfSwReleaseAdminStatus.activeAndRunning.toString(),
                testUtil.getManagedObject(ACTIVITY_JOB_RAU_ID).getAllAttributes().get(MiniLinkConstants.XF_SW_LMUPGRADE_ADMIN_STATUS));

        confirmService.execute(ACTIVITY_JOB_SBL_ID);
        assertNotNull(testUtil.getManagedObject(ACTIVITY_JOB_SBL_ID).getAllAttributes().get(MiniLinkConstants.XF_SW_RELEASE_ADMIN_STATUS));
        assertEquals(MiniLinkConstants.XfSwReleaseAdminStatus.activeAndRunning.toString(),
                testUtil.getManagedObject(ACTIVITY_JOB_SBL_ID).getAllAttributes().get(MiniLinkConstants.XF_SW_RELEASE_ADMIN_STATUS));
    }
}
