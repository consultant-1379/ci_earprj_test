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

package com.ericsson.oss.services.shm.es.impl.minilink.upgrade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.ericsson.cds.cdi.support.configuration.InjectionProperties;
import com.ericsson.cds.cdi.support.rule.CdiInjectorRule;
import com.ericsson.cds.cdi.support.rule.ImplementationInstance;
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.NodeAttributesReaderException;
import com.ericsson.oss.services.shm.common.exception.ServerInternalException;
import com.ericsson.oss.services.shm.common.exception.SmrsServiceUnavailableException;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.JobUpdateServiceImpl;
import com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants;
import com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.xfSwGlobalState;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

public class DownloadServiceTest {

    private final InjectionProperties injectionProperties = new InjectionProperties().autoLocateFrom("com.ericsson.oss.services.shm.es.impl.minilink.upgrade");

    private static final long ACTIVITY_JOB_RAU_ID = 11;
    private static final long ACTIVITY_JOB_RAU_NOUPGRADE_ID = 12;
    private static final long ACTIVITY_JOB_RAU_RAU_MISSING_ID = 13;
    private static final long ACTIVITY_JOB_SBL_ID = 21;
    private static final long ACTIVITY_JOB_SBL_NOUPGRADE_ID = 22;
    private static final long ACTIVITY_JOB_SBL_PASSIVE_ID = 23;
    private static final long ACTIVITY_JOB_SBL_ABORTED_ID = 24;
    private static final long ACTIVITY_JOB_SBL_NOUPGRADE_PASSIVE_ID = 25;
    private static final long ACTIVITY_JOB_SBL_NOUPGRADE_ABORTED_ID = 26;
    private static final long ACTIVITY_JOB_SBL_NOUPGRADE_CANCELLED_ID = 27;

    private static final String REASON_FOR_FAILURE = "Node is not in the right state.";
    private static final String XFSWCOMMITTYPE = "xfSwCommitType";

    private int cancelLogged = 0;

    @Rule
    public CdiInjectorRule cdiInjectorRule = new CdiInjectorRule(this, injectionProperties);

    @ObjectUnderTest
    private DownloadService downloadService;

    @ImplementationInstance
    private final MiniLinkActivityUtil miniLinkActivityUtil = new MiniLinkActivityUtil() {

        @Override
        public ManagedObject getManagedObject(final long activityJobId, final String type) throws ServerInternalException {
            return testUtil.getManagedObject(activityJobId);
        }

        @Override
        public ManagedObject getXfSwReleaseEntryMO(final ManagedObject xfSwObjectsMO) {
            return xfSwObjectsMO;
        }

        @Override
        public boolean isRAUPackage(final long activityJobId) {
            return testUtil.isRAUPackage(activityJobId);
        }

        @Override
        public Map<String, Object> createNewLogEntry(final JobLogLevel jobLogLevel, final String logMessage, final Object... placeHolders) {
            return activityUtils.createNewLogEntry(String.format(logMessage, placeHolders), jobLogLevel.toString());
        }

        @Override
        public String fetchJobProperty(final long activityJobId, final String propertyKey) {
            if (activityJobId == ACTIVITY_JOB_SBL_NOUPGRADE_CANCELLED_ID) {
                return "true";
            }
            return testUtil.getPropertyValue(activityJobId, propertyKey);
        }

        @Override
        public void setJobProperty(final String key, final String value, final long activityJobId) {
            final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
            activityUtils.addJobProperty(key, value, jobPropertyList);
            testUtil.addJobPropertyList(activityJobId, jobPropertyList);
        }

        @Override
        public void finishInstallActivity(final JobActivityInfo jobActivityInfo, final String unsubscribeEventFdn, final JobResult jobResult, final List<Map<String, Object>> jobLogList,
                                          final String activityName){
            testUtil.addJobLogList(jobActivityInfo.getActivityJobId(), jobLogList);
            testUtil.getManagedObject(jobActivityInfo.getActivityJobId()).setAttribute("result", jobResult);
        }

        @Override
        public void sendNotification(final JobActivityInfo jobActivityInfo, final String activityName){
        }

        @Override
        public void updateManagedObject(final long activityJobId, final String type, final Map<String, Object> attributes) {
            testUtil.updateManageObject(testUtil.getManagedObject(activityJobId).getPoId(), attributes);
        }

        @Override
        public void subscribeToMoNotifications(final String moFdn, final long activityJobId, final JobActivityInfo jobActivityInfo) {
        }

        @Override
        public String getSwPkgName(final long activityJobId) {
            return Long.toString(activityJobId);
        }

        @Override
        public boolean isRAUPackage(final String packageName) {
            return testUtil.isRAUPackage(Long.valueOf(packageName));
        }

        @Override
        public boolean updateXfSwLmUpgradeTableEntry(final long activityJobId, final String xfSwObjectsFdn, final String productNumber, final Map<String, Object> arguments) {
            return ACTIVITY_JOB_RAU_RAU_MISSING_ID == activityJobId ? false : true;
        }

        @Override
        public String[] getProductNumberAndRevisionRAU(final String swpPkgName) {
            return new String[] { "RAU", "01" };
        }

        @Override
        public String[] getProductNumberAndRevisionSBL(final String swpPkgName) {
            return new String[] { "SBL", "01" };
        }

        @Override
        public void getErrorJobLog(final List<Map<String, Object>> jobLogList, final boolean isRAU, final long activityJobID, final List<String> globalState) {
        }

        @Override
        public boolean isRAUUpgradeFailure(final long activityJobId) {
            return false;
        }

        @Override
        public ManagedObject getXfSwLmUpgradeEntryMO(final String xfSwObjectsFdn, final String entryNumber) {
            return testUtil.getManagedObject(ACTIVITY_JOB_RAU_ID);
        }

        @Override
        public void setSmrsFtpOnNode(final long activityJobId, final String nodeName) {
        }

        @Override
        public void setXfSwGlobalStateWithoutMediation(final long activityJobId, final xfSwGlobalState globalState) {
        }

        @Override
        public void unsubscribeFromMoNotifications(final String moFdn, final long activityJobId, final JobActivityInfo jobActivityInfo) {
        }

        @Override
        public boolean isSmrsDetailFoundOnNode(final long activityJobId) throws SmrsServiceUnavailableException, NodeAttributesReaderException {
            return true;
        }
    };

    @ImplementationInstance
    private final ActivityUtils activityUtils = new ActivityUtils() {
        @Override
        public JobEnvironment getJobEnvironment(final long activityJobId) {
            return new JobEnvironment(activityJobId, this);
        }

        @Override
        public Map<String, Object> getPoAttributes(final long poId) {
            final Map<String, Object> retVal = new HashMap<String, Object>();
            retVal.put(ShmConstants.NE_NAME, "ML-TN-" + poId);
            retVal.put(ShmConstants.NE_JOB_ID, poId);
            return retVal;
        }

        @Override
        public void logCancelledByUser(final List<Map<String, Object>> jobLogList, final JobEnvironment jobEnvironment, final String activityName) {
            cancelLogged++;
        }
    };

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

    @Before
    public void setup() {
        testUtil.addManagedObjectToMap(ACTIVITY_JOB_RAU_ID, null, true);
        testUtil.addManagedObjectToMap(ACTIVITY_JOB_RAU_NOUPGRADE_ID, MiniLinkConstants.xfSwGlobalState.noUpgrade.toString(), true);
        testUtil.addManagedObjectToMap(ACTIVITY_JOB_RAU_RAU_MISSING_ID, null, true);
        testUtil.addManagedObjectToMap(ACTIVITY_JOB_SBL_ID, null, false);
        testUtil.addManagedObjectToMap(ACTIVITY_JOB_SBL_NOUPGRADE_ID, MiniLinkConstants.xfSwGlobalState.noUpgrade.toString(), false);
        testUtil.addManagedObjectToMap(ACTIVITY_JOB_SBL_PASSIVE_ID, null, false, MiniLinkConstants.XF_SW_RELEASE_OPER_STATUS, MiniLinkConstants.XfSwReleaseOperStatus.passive.toString());
        testUtil.addManagedObjectToMap(ACTIVITY_JOB_SBL_ABORTED_ID, null, false, MiniLinkConstants.XF_SW_RELEASE_OPER_STATUS, MiniLinkConstants.XfSwReleaseOperStatus.upgradeAborted.toString());
        testUtil.addManagedObjectToMap(ACTIVITY_JOB_SBL_NOUPGRADE_PASSIVE_ID, MiniLinkConstants.xfSwGlobalState.noUpgrade.toString(), false, MiniLinkConstants.XF_SW_RELEASE_OPER_STATUS,
                MiniLinkConstants.XfSwReleaseOperStatus.passive.toString());
        testUtil.addManagedObjectToMap(ACTIVITY_JOB_SBL_NOUPGRADE_ABORTED_ID, MiniLinkConstants.xfSwGlobalState.noUpgrade.toString(), false, MiniLinkConstants.XF_SW_RELEASE_OPER_STATUS,
                MiniLinkConstants.XfSwReleaseOperStatus.upgradeAborted.toString());
        testUtil.addManagedObjectToMap(ACTIVITY_JOB_SBL_NOUPGRADE_CANCELLED_ID, MiniLinkConstants.xfSwGlobalState.noUpgrade.toString(), false, MiniLinkConstants.XF_SW_RELEASE_OPER_STATUS,
                MiniLinkConstants.XfSwReleaseOperStatus.upgradeAborted.toString());
    }

    @Test
    public void testPrecheckResultEnum() {
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, downloadService.precheck(ACTIVITY_JOB_RAU_ID).getActivityResultEnum());
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, downloadService.precheck(ACTIVITY_JOB_RAU_NOUPGRADE_ID).getActivityResultEnum());
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, downloadService.precheck(ACTIVITY_JOB_SBL_ID).getActivityResultEnum());
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, downloadService.precheck(ACTIVITY_JOB_SBL_NOUPGRADE_ID).getActivityResultEnum());
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, downloadService.precheck(ACTIVITY_JOB_SBL_PASSIVE_ID).getActivityResultEnum());
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, downloadService.precheck(ACTIVITY_JOB_SBL_ABORTED_ID).getActivityResultEnum());
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, downloadService.precheck(ACTIVITY_JOB_SBL_NOUPGRADE_PASSIVE_ID).getActivityResultEnum());
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, downloadService.precheck(ACTIVITY_JOB_SBL_NOUPGRADE_ABORTED_ID).getActivityResultEnum());
    }

    @Test
    public void testPrecheckJobLogList() {
        downloadService.precheck(ACTIVITY_JOB_RAU_ID);
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_RAU_ID, String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.DOWNLOAD, REASON_FOR_FAILURE)));
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_RAU_ID, String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.DOWNLOAD)));

        downloadService.precheck(ACTIVITY_JOB_RAU_NOUPGRADE_ID);
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_RAU_NOUPGRADE_ID, String.format(JobLogConstants.PRE_CHECK_SUCCESS, ActivityConstants.DOWNLOAD)));

        downloadService.precheck(ACTIVITY_JOB_SBL_ID);
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_SBL_ID, String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.DOWNLOAD)));

        downloadService.precheck(ACTIVITY_JOB_SBL_NOUPGRADE_ID);
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_SBL_NOUPGRADE_ID, String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.DOWNLOAD)));

        downloadService.precheck(ACTIVITY_JOB_SBL_PASSIVE_ID);
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_SBL_PASSIVE_ID, String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.DOWNLOAD)));

        downloadService.precheck(ACTIVITY_JOB_SBL_ABORTED_ID);
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_SBL_ABORTED_ID, String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.DOWNLOAD)));

        downloadService.precheck(ACTIVITY_JOB_SBL_NOUPGRADE_PASSIVE_ID);
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_SBL_NOUPGRADE_PASSIVE_ID, String.format(JobLogConstants.PRE_CHECK_SUCCESS, ActivityConstants.DOWNLOAD)));

        downloadService.precheck(ACTIVITY_JOB_SBL_NOUPGRADE_ABORTED_ID);
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_SBL_NOUPGRADE_ABORTED_ID, String.format(JobLogConstants.PRE_CHECK_SUCCESS, ActivityConstants.DOWNLOAD)));
    }

    @Test
    public void testHandleTimeoutMethod() {
        final ActivityStepResult result = downloadService.handleTimeout(ACTIVITY_JOB_SBL_ID);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, result.getActivityResultEnum());
    }

    @Test
    public void testCancelTimeoutMethod() {
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, downloadService.cancelTimeout(ACTIVITY_JOB_RAU_ID, true).getActivityResultEnum());
        assertEquals(ActivityStepResultEnum.TIMEOUT_ACTIVITY_ONGOING, downloadService.cancelTimeout(ACTIVITY_JOB_RAU_ID, false).getActivityResultEnum());
    }

    @Test
    public void testExecute() {
        downloadService.execute(ACTIVITY_JOB_RAU_ID);
        assertNotNull(testUtil.getManagedObject(ACTIVITY_JOB_RAU_ID).getAllAttributes().get(XFSWCOMMITTYPE));
        assertEquals("operatorCommit", testUtil.getManagedObject(ACTIVITY_JOB_RAU_ID).getAllAttributes().get(XFSWCOMMITTYPE));
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_RAU_ID, String.format(MiniLinkConstants.DOWNLOAD_PACKAGE, ACTIVITY_JOB_RAU_ID, "Radio unit")));

        downloadService.execute(ACTIVITY_JOB_RAU_RAU_MISSING_ID);
        assertEquals(JobResult.FAILED, testUtil.getManagedObject(ACTIVITY_JOB_RAU_RAU_MISSING_ID).getAllAttributes().get("result"));

        downloadService.execute(ACTIVITY_JOB_SBL_ID);
        assertNotNull(testUtil.getManagedObject(ACTIVITY_JOB_SBL_ID).getAllAttributes().get(MiniLinkConstants.XF_SW_RELEASE_ADMIN_STATUS));
        assertEquals(MiniLinkConstants.XfSwReleaseAdminStatus.upgradeStarted.toString(),
                testUtil.getManagedObject(ACTIVITY_JOB_SBL_ID).getAllAttributes().get(MiniLinkConstants.XF_SW_RELEASE_ADMIN_STATUS));
        assertNotNull(testUtil.getManagedObject(ACTIVITY_JOB_SBL_ID).getAllAttributes().get(MiniLinkConstants.XF_SW_VERSION_CONTROL));
        assertEquals(MiniLinkConstants.xfSwVersionControl.enable.toString(), testUtil.getManagedObject(ACTIVITY_JOB_SBL_ID).getAllAttributes().get(MiniLinkConstants.XF_SW_VERSION_CONTROL));
        assertNotNull("xfSwCommitType should not be null!", testUtil.getManagedObject(ACTIVITY_JOB_SBL_ID).getAllAttributes().get(XFSWCOMMITTYPE));
        assertEquals("xfSwCommitType should be operatorCommit!", "operatorCommit", testUtil.getManagedObject(ACTIVITY_JOB_SBL_ID).getAllAttributes().get(XFSWCOMMITTYPE));
        assertTrue(testUtil.getManagedObject(ACTIVITY_JOB_SBL_ID).getAllAttributes().containsKey("xfSwBootTime"));
    }

    @Test
    public void testProcessNotificationAVC() {
        // RAU
        downloadService.processNotification(testUtil.createNotification(testUtil.createDpsAttributeChangedEvent(null, "manualWaitForActivate"), ACTIVITY_JOB_RAU_ID, NotificationEventTypeEnum.AVC));
        assertEquals(JobResult.SUCCESS, testUtil.getManagedObject(ACTIVITY_JOB_RAU_ID).getAllAttributes().get("result"));

        downloadService.processNotification(testUtil.createNotification(testUtil.createDpsAttributeChangedEvent(null, "manualStarted"), ACTIVITY_JOB_RAU_ID, NotificationEventTypeEnum.AVC));
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_RAU_ID, MiniLinkConstants.DOWNLOAD_STARTED));

        downloadService.processNotification(testUtil.createNotification(testUtil.createDpsAttributeChangedEvent(null, "sblWaitForActivate"), ACTIVITY_JOB_RAU_ID, NotificationEventTypeEnum.AVC));
        assertEquals(JobResult.FAILED, testUtil.getManagedObject(ACTIVITY_JOB_RAU_ID).getAllAttributes().get("result"));

        // SBL
        downloadService.processNotification(testUtil.createNotification(testUtil.createDpsAttributeChangedEvent(null, "sblWaitForActivate"), ACTIVITY_JOB_SBL_ID, NotificationEventTypeEnum.AVC));
        assertEquals(JobResult.SUCCESS, testUtil.getManagedObject(ACTIVITY_JOB_SBL_ID).getAllAttributes().get("result"));

        downloadService.processNotification(testUtil.createNotification(testUtil.createDpsAttributeChangedEvent(null, "manualStarted"), ACTIVITY_JOB_SBL_ID, NotificationEventTypeEnum.AVC));
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_SBL_ID, MiniLinkConstants.DOWNLOAD_STARTED));

        downloadService.processNotification(testUtil.createNotification(testUtil.createDpsAttributeChangedEvent(null, "manualWaitForActivate"), ACTIVITY_JOB_SBL_ID, NotificationEventTypeEnum.AVC));
        assertEquals(JobResult.FAILED, testUtil.getManagedObject(ACTIVITY_JOB_SBL_ID).getAllAttributes().get("result"));

        // SBL && JOB_CANCELLED
        downloadService
                .processNotification(testUtil.createNotification(testUtil.createDpsAttributeChangedEvent(null, "noUpgrade"), ACTIVITY_JOB_SBL_NOUPGRADE_CANCELLED_ID, NotificationEventTypeEnum.AVC));
        assertEquals(JobResult.FAILED, testUtil.getManagedObject(ACTIVITY_JOB_SBL_NOUPGRADE_CANCELLED_ID).getAllAttributes().get("result"));
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_SBL_NOUPGRADE_CANCELLED_ID, "Download is properly cancelled on node."));
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_SBL_NOUPGRADE_CANCELLED_ID, "cancelled successfully"));

        downloadService
                .processNotification(testUtil.createNotification(testUtil.createDpsAttributeChangedEvent(null, "noUpgrade"), ACTIVITY_JOB_SBL_NOUPGRADE_ABORTED_ID, NotificationEventTypeEnum.AVC));
        assertEquals(JobResult.FAILED, testUtil.getManagedObject(ACTIVITY_JOB_SBL_NOUPGRADE_ABORTED_ID).getAllAttributes().get("result"));
        assertTrue(!testUtil.logContainsMessage(ACTIVITY_JOB_SBL_NOUPGRADE_ABORTED_ID, "Download is properly cancelled on node."));
        assertTrue(!testUtil.logContainsMessage(ACTIVITY_JOB_SBL_NOUPGRADE_ABORTED_ID, "cancelled successfully"));

    }

    @Test
    public void testProcessNotificationCreate() {
        // RAU
        downloadService.processNotification(testUtil.createNotification(testUtil.createDpsObjectCreatedEvent("manualWaitForActivate"), ACTIVITY_JOB_RAU_ID, NotificationEventTypeEnum.CREATE));
        assertEquals(JobResult.SUCCESS, testUtil.getManagedObject(ACTIVITY_JOB_RAU_ID).getAllAttributes().get("result"));

        downloadService.processNotification(testUtil.createNotification(testUtil.createDpsObjectCreatedEvent("manualStarted"), ACTIVITY_JOB_RAU_ID, NotificationEventTypeEnum.CREATE));
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_RAU_ID, MiniLinkConstants.DOWNLOAD_STARTED));

        downloadService.processNotification(testUtil.createNotification(testUtil.createDpsObjectCreatedEvent("sblWaitForActivate"), ACTIVITY_JOB_RAU_ID, NotificationEventTypeEnum.CREATE));
        assertEquals(JobResult.FAILED, testUtil.getManagedObject(ACTIVITY_JOB_RAU_ID).getAllAttributes().get("result"));
        // SBL
        downloadService.processNotification(testUtil.createNotification(testUtil.createDpsObjectCreatedEvent("sblWaitForActivate"), ACTIVITY_JOB_SBL_ID, NotificationEventTypeEnum.CREATE));
        assertEquals(JobResult.SUCCESS, testUtil.getManagedObject(ACTIVITY_JOB_SBL_ID).getAllAttributes().get("result"));

        downloadService.processNotification(testUtil.createNotification(testUtil.createDpsObjectCreatedEvent("manualStarted"), ACTIVITY_JOB_SBL_ID, NotificationEventTypeEnum.CREATE));
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_SBL_ID, MiniLinkConstants.DOWNLOAD_STARTED));

        downloadService.processNotification(testUtil.createNotification(testUtil.createDpsObjectCreatedEvent("manualWaitForActivate"), ACTIVITY_JOB_SBL_ID, NotificationEventTypeEnum.CREATE));
        assertEquals(JobResult.FAILED, testUtil.getManagedObject(ACTIVITY_JOB_SBL_ID).getAllAttributes().get("result"));
    }

    @Test
    public void testCancelRAU() {
        assertEquals(ActivityStepResultEnum.EXECUTION_FAILED, downloadService.cancel(ACTIVITY_JOB_RAU_ID).getActivityResultEnum());
        assertEquals("true", miniLinkActivityUtil.fetchJobProperty(ACTIVITY_JOB_RAU_ID, ActivityConstants.IS_CANCEL_TRIGGERED));
        assertEquals(1, cancelLogged);
        assertEquals("upgradeAborted", testUtil.getManagedObject(ACTIVITY_JOB_RAU_ID).getAllAttributes().get(MiniLinkConstants.XF_SW_LMUPGRADE_ADMIN_STATUS));
    }

    @Test
    public void testCancelSBL() {
        assertEquals(ActivityStepResultEnum.EXECUTION_FAILED, downloadService.cancel(ACTIVITY_JOB_SBL_ID).getActivityResultEnum());
        assertEquals("true", miniLinkActivityUtil.fetchJobProperty(ACTIVITY_JOB_SBL_ID, ActivityConstants.IS_CANCEL_TRIGGERED));
        assertEquals(1, cancelLogged);
        assertEquals("upgradeAborted", testUtil.getManagedObject(ACTIVITY_JOB_SBL_ID).getAllAttributes().get(MiniLinkConstants.XF_SW_RELEASE_ADMIN_STATUS));
    }
}
