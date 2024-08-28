/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2015
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
import static org.junit.Assert.assertTrue;

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
import com.ericsson.oss.services.shm.common.NodeModelNameSpaceProvider;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.ServerInternalException;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.JobUpdateServiceImpl;
import com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants;
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

public class DownloadServiceNegativeTest {
    private final InjectionProperties injectionProperties = new InjectionProperties().autoLocateFrom("com.ericsson.oss.services.shm.es.impl.minilink.upgrade");

    private static final long ACTIVITY_JOB_EXCEPTION_WITH_MESSAGE_ID = 1;
    private static final long ACTIVITY_JOB_EXCEPTION_WITHOUT_MESSAGE_ID = 2;
    private static final long ACTIVITY_JOB_DOWNLOAD_FAILURE = 3;
    private static final String EXCEPTION_MESSAGE = "Test MediationServiceException Exception!";
    @Rule
    public CdiInjectorRule cdiInjectorRule = new CdiInjectorRule(this, injectionProperties);

    @ObjectUnderTest
    private DownloadService downloadService;

    @ImplementationInstance
    public final MiniLinkActivityUtil miniLinkActivityUtil = new MiniLinkActivityUtil() {

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
        public void setJobProperty(final String key, final String value, final long activityJobId) {
            //
        }

        @Override
        public Map<String, Object> createNewLogEntry(final JobLogLevel jobLogLevel, final String logMessage, final Object... placeHolders) {
            return activityUtils.createNewLogEntry(String.format(logMessage, placeHolders), jobLogLevel.toString());
        }

        @Override
        public boolean isRAUPackage(final long activityJobId) {
            return true;
        }

        @Override
        public boolean isRAUUpgradeFailure(final long activityJobId) {
            return true;
        }

        @Override
        public void getErrorJobLog(final List<Map<String, Object>> jobLogList, final boolean isRAU, final long activityJobID, final List<String> globalState) {
            return;
        }

        @Override
        public ManagedObject getManagedObject(final long activityJobId, final String type) throws ServerInternalException {
            return testUtil.getManagedObject(activityJobId);
        }

        @Override
        public ManagedObject getXfSwLmUpgradeEntryMO(final String xfSwObjectsFdn, final String entryNumber) {
            return testUtil.getManagedObject(ACTIVITY_JOB_DOWNLOAD_FAILURE);
        }

        @Override
        public void updateManagedObject(final long activityJobId, final String type, final Map<String, Object> attributes) {
            testUtil.updateManageObject(testUtil.getManagedObject(activityJobId).getPoId(), attributes);
        }

        @Override
        public String fetchJobProperty(final long activityJobId, final String propertyKey) {
            return "1";
        }

        @Override
        public void setSmrsFtpOnNode(final long activityJobId, final String nodeName) {
            if (nodeName.endsWith(Long.toString(ACTIVITY_JOB_EXCEPTION_WITHOUT_MESSAGE_ID))) {
                throw new RuntimeException();
            } else {
                final Exception exception = new Exception(EXCEPTION_MESSAGE);
                throw new RuntimeException(exception);
            }
        }
    };

    @ImplementationInstance
    private final JobUpdateService jobUpdateService = new JobUpdateServiceImpl() {

        @Override
        public boolean readAndUpdateRunningJobAttributes(final long jobId, final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList) {
            testUtil.addJobLogList(jobId, jobLogList);
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
    };

    @ImplementationInstance
    private final NodeModelNameSpaceProvider nodeModelNameSpaceProvider = new NodeModelNameSpaceProvider() {
        @Override
        public String getNamespaceByNodeName(final String nodeName) {
            return nodeName;
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
        testUtil.addManagedObjectToMap(ACTIVITY_JOB_EXCEPTION_WITH_MESSAGE_ID, null, true);
        testUtil.addManagedObjectToMap(ACTIVITY_JOB_EXCEPTION_WITHOUT_MESSAGE_ID, null, true);
        testUtil.addManagedObjectToMap(ACTIVITY_JOB_DOWNLOAD_FAILURE, "manualWaitForCommit", true, "xfSwLmUpgradeOperStatus", "upgradeFailed", "xfSwLmUpgradeFailure", "downloadFailure");
    }

    @Test
    public void testPrecheckResultEnum() {
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, downloadService.precheck(ACTIVITY_JOB_EXCEPTION_WITH_MESSAGE_ID).getActivityResultEnum());
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_EXCEPTION_WITH_MESSAGE_ID, String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.DOWNLOAD)));

    }

    @Test
    public void testFailedExecute() {
        downloadService.execute(ACTIVITY_JOB_EXCEPTION_WITH_MESSAGE_ID);
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_EXCEPTION_WITH_MESSAGE_ID, String.format(JobLogConstants.FAILURE_REASON, EXCEPTION_MESSAGE)));
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_EXCEPTION_WITH_MESSAGE_ID, String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.DOWNLOAD)));

        downloadService.execute(ACTIVITY_JOB_EXCEPTION_WITHOUT_MESSAGE_ID);
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_EXCEPTION_WITHOUT_MESSAGE_ID, String.format(JobLogConstants.FAILURE_REASON, null)));
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_EXCEPTION_WITHOUT_MESSAGE_ID, String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.DOWNLOAD)));

    }

    @Test
    public void testProcessNotificationAVC() {
        // RAU
        downloadService
                .processNotification(testUtil.createNotification(testUtil.createDpsAttributeChangedEvent(null, "manualWaitForActivate"), ACTIVITY_JOB_DOWNLOAD_FAILURE, NotificationEventTypeEnum.AVC));
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_DOWNLOAD_FAILURE, String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.DOWNLOAD)));
        assertEquals(MiniLinkConstants.xfSwLmUpgradeAdminStatus.upgradeAborted.toString(),
                testUtil.getManagedObject(ACTIVITY_JOB_DOWNLOAD_FAILURE).getAllAttributes().get(MiniLinkConstants.XF_SW_LMUPGRADE_ADMIN_STATUS));
    }

}
