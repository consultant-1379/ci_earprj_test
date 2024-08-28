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
package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.backup

import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils

import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.BACKUP_NAME
import static com.ericsson.oss.services.shm.shared.constants.ActivityConstants.JOB_LOG_MESSAGE
import com.ericsson.oss.services.shm.es.api.JobActivityInfo
import com.ericsson.oss.services.shm.es.impl.ActiveSoftwareProvider
import com.ericsson.oss.services.shm.es.impl.ActivityUtils
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider
import com.ericsson.oss.services.shm.notifications.api.Notification

import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants
import com.ericsson.oss.services.shm.job.cache.JobStaticData
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants
import com.ericsson.oss.services.shm.es.api.ActivityStepResult

import static org.junit.Assert.assertTrue
import static org.mockito.Mockito.when

import javax.inject.Inject

import org.spockframework.util.Assert

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.shm.es.impl.JobEnvironment
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum

class MiniLinkOutdoorJobUtilSpec extends CdiSpecification {

    @ObjectUnderTest
    MiniLinkOutdoorJobUtil miniLinkOutdoorJobUtil;

    @MockedImplementation
    NEJobStaticData neJobStaticData;

    @MockedImplementation
    JobStaticData jobStaticData;

    @MockedImplementation
    ActivityUtils activityUtils;

    @MockedImplementation
    JobEnvironment jobEnvironment;

    @MockedImplementation
    JobActivityInfo jobActivityInfo;

    @MockedImplementation
    Notification message;

    @MockedImplementation
    NeJobStaticDataProvider neJobStaticDataProvider

    @MockedImplementation
    ActiveSoftwareProvider activeSoftwareProvider

    @MockedImplementation
    JobStaticDataProvider jobStaticDataProvider

    @MockedImplementation
    @Inject
    JobPropertyUtils jobPropertyUtilsMock

    private static final long activityJobId = 123l;
    private static final long neJobId = 123l;
    private static final long mainJobId = 123l;
    private static final String neName = "CORE42ML6352";
    private static final String BACKUP_NAME_TEST = "backupName";

    private static final String nodeName = "nodeName";
    private static final String THIS_ACTIVITY = "backup";
    private static final Double PERCENT_ZERO = 0.0;
    private static final Double PERCENT_HUNDRED = 100.0;

    BackupActivityProperties backupActivityProperties

    def setup() {
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY) >> neJobStaticData
        neJobStaticData.getNeJobId() >> 123l
        jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId()) >> jobStaticData

        activityUtils.getJobEnvironment(activityJobId) >> jobEnvironment;
        Map<String, Object> mainJobAttributes = new HashMap<>();
        activityUtils.getJobConfigurationDetails(activityJobId) >> mainJobAttributes
        activityUtils.getMainJobAttributes(activityJobId) >> mainJobAttributes
        jobEnvironment.getNodeName() >> nodeName
        activityUtils.getActivityInfo(activityJobId, getClass()) >> jobActivityInfo;

        jobEnvironment.getActivityJobId() >> activityJobId
        jobEnvironment.getNeJobId() >> neJobId
        jobEnvironment.getMainJobId() >> mainJobId
        jobEnvironment.getNodeName() >> neName

        backupActivityProperties = new BackupActivityProperties(activityJobId, jobEnvironment, Constants.BACKUP_NAME,
                THIS_ACTIVITY, getClass())
    }

    def "testGetBackupWithAutoGenerateTrue"() {
        given: "initialize"
        final Map<String, String> backupDataMap = new HashMap<>()
        backupDataMap.put(JobPropertyConstants.AUTO_GENERATE_BACKUP, "true")
        backupDataMap.put(BACKUP_NAME, BACKUP_NAME_TEST)
        final Map<String, String> activeSoftwareMap = new HashMap<>();
        activeSoftwareMap.put(neName, "CXP9010021_1||R34S108")
        activityUtils.getJobConfigurationDetails(activityJobId) >> new HashMap<>()
        jobPropertyUtilsMock.getPropertyValue(Arrays.asList(BACKUP_NAME, JobPropertyConstants.AUTO_GENERATE_BACKUP), new HashMap<>()) >> backupDataMap
        jobStaticData.getOwner() >> "administrator"
        neJobStaticData.getNodeName() >> neName
        jobEnvironment.getNeJobAttributes() >> new HashMap<>()
        activeSoftwareProvider.getActiveSoftwareDetails(Arrays.asList(neName)) >> activeSoftwareMap
        when: "invoke get backup name"
        String backupName = miniLinkOutdoorJobUtil.getBackupName(activityJobId, jobEnvironment)
        then: "return value should not be null"
        Assert.notNull(backupName)
        assertTrue(backupName.contains("CXP9010021"))
    }

    def "testGetBackupWithAutoGenerateFalse"() {
        given: "initialize"
        final Map<String, String> backupDataMap = new HashMap<>()
        backupDataMap.put(JobPropertyConstants.AUTO_GENERATE_BACKUP, "false")
        backupDataMap.put(Constants.BACKUP_NAME, BACKUP_NAME_TEST)
        activityUtils.getJobConfigurationDetails(activityJobId) >> new HashMap<>()
        jobPropertyUtilsMock.getPropertyValue(Arrays.asList(BACKUP_NAME, JobPropertyConstants.AUTO_GENERATE_BACKUP), new HashMap<>()) >> backupDataMap
        when: "invoke get backup name"
        String backupName = miniLinkOutdoorJobUtil.getBackupName(activityJobId, jobEnvironment)
        then: "return value should not be null"
        Assert.notNull(backupName)
    }

    def "testGetBackupWithAutoGenerateNull"() {
        given: "initialize"
        final Map<String, String> backupDataMap = new HashMap<>()
        backupDataMap.put(Constants.BACKUP_NAME, BACKUP_NAME_TEST)
        activityUtils.getJobConfigurationDetails(activityJobId) >> new HashMap<>()
        jobPropertyUtilsMock.getPropertyValue(Arrays.asList(BACKUP_NAME, JobPropertyConstants.AUTO_GENERATE_BACKUP), new HashMap<>()) >> backupDataMap
        when: "invoke get backup name"
        String backupName = miniLinkOutdoorJobUtil.getBackupName(activityJobId, jobEnvironment)
        then: "return value should not be null"
        Assert.notNull(backupName)
    }

    def "testGetBackupWithNoAutoGenerateAndBackupName"() {
        given: "initialize"
        activityUtils.getJobConfigurationDetails(activityJobId) >> new HashMap<>()
        jobPropertyUtilsMock.getPropertyValue(Arrays.asList(BACKUP_NAME, JobPropertyConstants.AUTO_GENERATE_BACKUP), new HashMap<>()) >> new HashMap<>()
        when: "invoke get backup name"
        String backupName = miniLinkOutdoorJobUtil.getBackupName(activityJobId, jobEnvironment)
        then: "return value should be null"
        backupName == null
    }

    def "testPreCheckSuccess"() {
        when: "invoke precheck success"
        ActivityStepResult activityStepResult = miniLinkOutdoorJobUtil.precheckSuccess(PERCENT_ZERO, backupActivityProperties);
        then: "return value should not be null"
        Assert.notNull(activityStepResult)
        activityStepResult.getActivityResultEnum() ==  ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
    }

    def "testPreCheckFailure"() {
        when: "invoke precheck failure"
        ActivityStepResult activityStepResult = miniLinkOutdoorJobUtil.precheckFailure(PERCENT_ZERO, backupActivityProperties, "precheckFailed");
        then: "return value should not be null"
        Assert.notNull(activityStepResult)
        activityStepResult.getActivityResultEnum() ==  ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
    }

    def "testTimeoutSuccess"() {
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        activityUtils.getActivityStepResult(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS) >> activityStepResult
        when: "invoke timeout success"
        ActivityStepResult activityStepResultA = miniLinkOutdoorJobUtil.timeoutSuccess(PERCENT_HUNDRED, backupActivityProperties);
        then: "return value should not be null"
        Assert.notNull(activityStepResultA)
        activityStepResultA.getActivityResultEnum() ==  ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS;
    }

    def "testTimeoutFail"() {
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        activityUtils.getActivityStepResult(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL) >> activityStepResult
        when: "invoke timeout fail"
        ActivityStepResult activityStepResultA = miniLinkOutdoorJobUtil.timeoutFail(PERCENT_ZERO, backupActivityProperties);
        then: "return value should not be null"
        Assert.notNull(activityStepResultA)
        activityStepResultA.getActivityResultEnum() ==  ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
    }

    def "testSucceedBackupRestoreActivity"() {
        when: "invoke succeedBackupRestoreActivity"
        miniLinkOutdoorJobUtil.succeedBackupRestoreActivity(neJobStaticData, backupActivityProperties.getActivityJobId(),
                backupActivityProperties.getActivityName())
        then : "expect nothing"
    }

    def "testFailWithException"() {
        when: "invoke fail with exception"
        miniLinkOutdoorJobUtil.failBackupRestoreActivity(neJobStaticData, backupActivityProperties.getActivityJobId(),
                backupActivityProperties.getActivityName(), Constants.EXCEPTION_OCCURED_FAILURE_REASON, Constants.BACKUP_JOB)
        then : "expect nothing"
    }

    def "testFinishActivity"() {
        given: "initialize"
        List<Map<String, Object>> jobLogList = new ArrayList<>();
        Map<String, Object> jobLog = new HashMap<>();
        jobLog.put(JOB_LOG_MESSAGE, "logMessageTest");
        jobLogList.add(jobLog);
        when: "invoke finish activity"
        miniLinkOutdoorJobUtil.finishActivity(jobActivityInfo, null, JobResult.FAILED, jobLogList, THIS_ACTIVITY)
        then : "expect nothing"
    }
}
