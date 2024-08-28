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
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.JobUpdateServiceImpl;
import com.ericsson.oss.services.shm.es.impl.minilink.common.MiniLinkJobUtil;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

public class ConfirmServiceNegativeTest {

    private final InjectionProperties injectionProperties =
            new InjectionProperties().autoLocateFrom("com.ericsson.oss.services.shm.es.impl.minilink.upgrade");

    private static final long ACTIVITY_JOB_EXCEPTION_WITH_MESSAGE_ID = 1;
    private static final long ACTIVITY_JOB_EXCEPTION_WITHOUT_MESSAGE_ID = 2;
    private static final String EXCEPTION_MESSAGE = "Test MediationServiceException Exception!";

    @Rule
    public CdiInjectorRule cdiInjectorRule = new CdiInjectorRule(this, injectionProperties);

    @ObjectUnderTest
    private ConfirmService confirmService;

    @ImplementationInstance
    private final MiniLinkActivityUtil miniLinkActivityUtil = new MiniLinkActivityUtil() {

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
        public void getErrorJobLog(final List<Map<String, Object>> jobLogList, final boolean isRAU, final long activityJobID,
                final List<String> globalState) {
            return;
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
        public List<String> getGlobalState(final long activityJobId) {
            final Exception exception = new Exception("TestException");
            throw new RuntimeException(exception);
        }

        @Override
        public String getXfSwObjectsFdn(final Long activityJobId) {
            if (activityJobId == ACTIVITY_JOB_EXCEPTION_WITHOUT_MESSAGE_ID) {
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
        public boolean readAndUpdateRunningJobAttributes(final long jobId, final List<Map<String, Object>> jobPropertyList,
                final List<Map<String, Object>> jobLogList) {
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

    private final UpgradeTestUtil testUtil = new UpgradeTestUtil();

    @Before
    public void setup() {
        testUtil.addManagedObjectToMap(ACTIVITY_JOB_EXCEPTION_WITH_MESSAGE_ID, null, true);
        testUtil.addManagedObjectToMap(ACTIVITY_JOB_EXCEPTION_WITHOUT_MESSAGE_ID, null, true);
    }

    @Test
    public void testPrecheckResultEnum() {
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION,
                confirmService.precheck(ACTIVITY_JOB_EXCEPTION_WITH_MESSAGE_ID).getActivityResultEnum());
        assertTrue(
                testUtil.logContainsMessage(ACTIVITY_JOB_EXCEPTION_WITH_MESSAGE_ID,
                        String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.CONFIRM)));
    }

    @Test
    public void testFailedExecute() {
        confirmService.execute(ACTIVITY_JOB_EXCEPTION_WITH_MESSAGE_ID);
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_EXCEPTION_WITH_MESSAGE_ID,
                String.format(JobLogConstants.FAILURE_REASON, EXCEPTION_MESSAGE)));
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_EXCEPTION_WITH_MESSAGE_ID,
                String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.CONFIRM)));
        confirmService.execute(ACTIVITY_JOB_EXCEPTION_WITHOUT_MESSAGE_ID);

        confirmService.execute(ACTIVITY_JOB_EXCEPTION_WITHOUT_MESSAGE_ID);
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_EXCEPTION_WITHOUT_MESSAGE_ID,
                String.format(JobLogConstants.FAILURE_REASON, null)));
        assertTrue(testUtil.logContainsMessage(ACTIVITY_JOB_EXCEPTION_WITHOUT_MESSAGE_ID,
                String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.CONFIRM)));
    }
}
