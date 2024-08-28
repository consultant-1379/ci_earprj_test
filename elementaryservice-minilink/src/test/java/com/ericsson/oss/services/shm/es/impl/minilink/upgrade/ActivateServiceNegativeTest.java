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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

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
import com.ericsson.cds.cdi.support.rule.MockedImplementation;
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.ServerInternalException;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
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
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

public class ActivateServiceNegativeTest {
    private final InjectionProperties injectionProperties = new InjectionProperties().autoLocateFrom("com.ericsson.oss.services.shm.es.impl.minilink");

    private static final long ACTIVITY_JOB_SBL_PACKAGE_SBL_WAIT_ID = 11;
    private static final long ACTIVITY_JOB_RAU_MANUAL_WAIT_ID = 12;
    private static final long ACTIVITY_JOB_SBL_MANUAL_WAIT_ID = 21;
    private static final long ACTIVITY_JOB_RAU_SBL_WAIT_ID = 22;
    private static final long ACTIVITY_JOB_SBL_NOUPGRADE_ID = 31;
    private static final long ACTIVITY_JOB_RAU_NOUPGRADE_ID = 32;
    private static final long ACTIVITY_JOB_RAU_NO_OPERATOR_COMMIT_ID = 42;

    private static final String EXCEPTION_MESSAGE = "Test Error Message";
    @Rule
    public CdiInjectorRule cdiInjectorRule = new CdiInjectorRule(this, injectionProperties);

    @ObjectUnderTest
    private ActivateService activateService;

    @ImplementationInstance
    private final JobUpdateService jobUpdateService = new JobUpdateServiceImpl() {

        @Override
        public boolean readAndUpdateRunningJobAttributes(final long jobId, final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList) {
            testUtil.addJobLogList(jobId, jobLogList);
            return true;
        }
    };

    @ImplementationInstance
    private final MiniLinkActivityUtil miniLinkActivityUtil = new MiniLinkActivityUtil() {

        @Override
        public Map<String, Object> createNewLogEntry(final JobLogLevel jobLogLevel, final String logMessage, final Object... placeHolders) {
            return activityUtils.createNewLogEntry(String.format(logMessage, placeHolders), jobLogLevel.toString());
        }

        @Override
        public ManagedObject getManagedObject(final long activityJobId, final String type) throws ServerInternalException {
            return testUtil.getManagedObject(activityJobId);
        }

        @Override
        public boolean isRAUPackage(final long activityJobId) {
            throw new RuntimeException(EXCEPTION_MESSAGE);
        }

        @Override
        public String getSwPkgName(final long activityJobId) {
            return "testPackageName";
        }

        @Override
        public String[] getProductNumberAndRevisionRAU(final String swpPkgName) {
            return new String[] { "" };
        }

        @Override
        public void updateManagedObject(final ManagedObject managedObject, final Map<String, Object> arguments) {
            throw new RuntimeException("testException");
        }

        @Override
        public void finishInstallActivity(final JobActivityInfo jobActivityInfo, final String unsubscribeEventFdn, final JobResult jobResult, final List<Map<String, Object>> jobLogList,
                                          final String activityName){
            testUtil.getManagedObject(jobActivityInfo.getActivityJobId()).setAttribute("result", jobResult);
        }

        @Override
        public void sendNotification(final JobActivityInfo jobActivityInfo, final String activityName){
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

    @MockedImplementation
    private JobEnvironment jobEnvironment;

    @ImplementationInstance
    private final ActivityUtils activityUtils = new ActivityUtils() {
        @Override
        public JobEnvironment getJobEnvironment(final long activityJobId) {
            return jobEnvironment;
        }
    };

    @Before
    public void setup() {
        when(jobEnvironment.getNodeName()).thenReturn("TEST NODE");
        testUtil.addManagedObjectToMap(ACTIVITY_JOB_SBL_PACKAGE_SBL_WAIT_ID, MiniLinkConstants.xfSwGlobalState.sblWaitForActivate.toString(), false);
        testUtil.addManagedObjectToMap(ACTIVITY_JOB_RAU_MANUAL_WAIT_ID, MiniLinkConstants.xfSwGlobalState.manualWaitForActivate.toString(), true, MiniLinkConstants.XF_SW_COMMIT_TYPE,
                MiniLinkConstants.xfswCommitType.operatorCommit.toString());
        testUtil.addManagedObjectToMap(ACTIVITY_JOB_SBL_MANUAL_WAIT_ID, MiniLinkConstants.xfSwGlobalState.manualWaitForActivate.toString(), false);
        testUtil.addManagedObjectToMap(ACTIVITY_JOB_RAU_SBL_WAIT_ID, MiniLinkConstants.xfSwGlobalState.sblWaitForActivate.toString(), true, MiniLinkConstants.XF_SW_COMMIT_TYPE,
                MiniLinkConstants.xfswCommitType.operatorCommit.toString());
        testUtil.addManagedObjectToMap(ACTIVITY_JOB_SBL_NOUPGRADE_ID, MiniLinkConstants.xfSwGlobalState.noUpgrade.toString(), false);
        testUtil.addManagedObjectToMap(ACTIVITY_JOB_RAU_NOUPGRADE_ID, MiniLinkConstants.xfSwGlobalState.noUpgrade.toString(), true, MiniLinkConstants.XF_SW_COMMIT_TYPE,
                MiniLinkConstants.xfswCommitType.operatorCommit.toString());
        testUtil.addManagedObjectToMap(ACTIVITY_JOB_RAU_NO_OPERATOR_COMMIT_ID, MiniLinkConstants.xfSwGlobalState.manualWaitForActivate.toString(), true, MiniLinkConstants.XF_SW_COMMIT_TYPE, null);
    }

    @Test
    public void testPrecheckResultEnums() {
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, activateService.precheck(ACTIVITY_JOB_SBL_PACKAGE_SBL_WAIT_ID).getActivityResultEnum());
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, activateService.precheck(ACTIVITY_JOB_RAU_MANUAL_WAIT_ID).getActivityResultEnum());
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, activateService.precheck(ACTIVITY_JOB_SBL_MANUAL_WAIT_ID).getActivityResultEnum());
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, activateService.precheck(ACTIVITY_JOB_RAU_SBL_WAIT_ID).getActivityResultEnum());
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, activateService.precheck(ACTIVITY_JOB_SBL_NOUPGRADE_ID).getActivityResultEnum());
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, activateService.precheck(ACTIVITY_JOB_RAU_NOUPGRADE_ID).getActivityResultEnum());
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, activateService.precheck(ACTIVITY_JOB_RAU_NO_OPERATOR_COMMIT_ID).getActivityResultEnum());
    }

    @Test
    public void testPrecheckJobLogList() {
        activateService.precheck(ACTIVITY_JOB_SBL_MANUAL_WAIT_ID);
        activateService.precheck(ACTIVITY_JOB_RAU_SBL_WAIT_ID);
        activateService.precheck(ACTIVITY_JOB_SBL_NOUPGRADE_ID);
        activateService.precheck(ACTIVITY_JOB_RAU_NOUPGRADE_ID);
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_SBL_MANUAL_WAIT_ID, EXCEPTION_MESSAGE));
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_RAU_SBL_WAIT_ID, EXCEPTION_MESSAGE));
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_SBL_NOUPGRADE_ID, EXCEPTION_MESSAGE));
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_RAU_NOUPGRADE_ID, EXCEPTION_MESSAGE));
    }

    @Test
    public void testFailedExecute() {
        activateService.execute(ACTIVITY_JOB_SBL_PACKAGE_SBL_WAIT_ID);
        activateService.execute(ACTIVITY_JOB_RAU_MANUAL_WAIT_ID);
        assertEquals(JobResult.FAILED, testUtil.getManagedObject(ACTIVITY_JOB_SBL_PACKAGE_SBL_WAIT_ID).getAllAttributes().get("result"));

        assertEquals(JobResult.FAILED, testUtil.getManagedObject(ACTIVITY_JOB_RAU_MANUAL_WAIT_ID).getAllAttributes().get("result"));

    }
}
