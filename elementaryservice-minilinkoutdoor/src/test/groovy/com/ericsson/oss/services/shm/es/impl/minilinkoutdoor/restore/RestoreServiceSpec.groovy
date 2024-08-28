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
package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.restore

import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.common.smrs.SmrsAccountInfo
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService
import com.ericsson.oss.services.shm.es.api.JobActivityInfo
import com.ericsson.oss.services.shm.es.impl.ActivityUtils
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.backup.BackupActivityProperties
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.backup.MiniLinkOutdoorJobUtil
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.BackupSmrs
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.DPSUtils
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants
import com.ericsson.oss.services.shm.generic.notification.SHMCommonCallBackNotificationJobProgressBean
import com.ericsson.oss.services.shm.job.cache.JobStaticData
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator
import com.ericsson.oss.services.shm.model.notification.SHMCommonCallbackNotification

import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.BACKUP
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.RESTORE
import com.ericsson.oss.services.shm.es.api.ActivityStepResult

import static org.mockito.Mockito.when

import org.spockframework.util.Assert
import spock.lang.Unroll

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.shm.es.impl.JobEnvironment
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum

class RestoreServiceSpec extends CdiSpecification {

    @MockedImplementation
    MiniLinkOutdoorJobUtil miniLinkOutdoorJobUtil;

    @MockedImplementation
    NEJobStaticData neJobStaticData;

    @MockedImplementation
    JobStaticData jobStaticData;

    @MockedImplementation
    ActivityUtils activityUtils;

    @MockedImplementation
    JobEnvironment jobEnvironment;

    @ObjectUnderTest
    RestoreService restoreService;

    @MockedImplementation
    JobActivityInfo jobActivityInfo;

    @MockedImplementation
    SHMCommonCallBackNotificationJobProgressBean message

    @MockedImplementation
    SHMCommonCallbackNotification commonNotification

    @MockedImplementation
    NeJobStaticDataProvider neJobStaticDataProvider

    @MockedImplementation
    JobStaticDataProvider jobStaticDataProvider

    @MockedImplementation
    BackupActivityProperties backupActivityProperties

    @MockedImplementation
    ActivityJobTBACValidator activityJobTBACValidator

    @MockedImplementation
    DPSUtils dpsUtils

    @MockedImplementation
    SmrsAccountInfo smrsAccountInfo

    @MockedImplementation
    SmrsFileStoreService smrsFileStoreService

    @MockedImplementation
    BackupSmrs backupSmrs

    @MockedImplementation
    ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator

    private static final long activityJobId = 123l;
    private static final String nodeName = "nodeName";
    private static final String THIS_ACTIVITY = "restore";
    List<Map<String, Object>> jobLogList = null;
    private static final Double PERCENT_ZERO = 0.0
    public static final String NODE_NAME = "ML-TN";
    public static final String PATH_ON_SERVER = "pathOnServer";
    public static final String BACKUP_NAME_TEST = "backup";
    public static final String MINI_LINK_OUTDOOR = "MINI_LINK_OUTDOOR";
    public static final String CONFIG_FILE_EXTENSION = "cdb";
    public static final String DOT = ".";
    public static final String SLASH = "/";
    public static final String UNDERSCORE = "_";
    public static final String EXPECTED_BACKUP_FILE_NAME = BACKUP_NAME_TEST + UNDERSCORE + NODE_NAME + DOT + CONFIG_FILE_EXTENSION;
    public static final String BACKUP_FILE_PATH_PREFIX = MINI_LINK_OUTDOOR + SLASH + EXPECTED_BACKUP_FILE_NAME;

    def setup() {
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY) >> neJobStaticData
        neJobStaticData.getNeJobId() >> 123l
        jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId()) >> jobStaticData
        activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, THIS_ACTIVITY) >> true

        activityUtils.getJobEnvironment(activityJobId) >> jobEnvironment;
        Map<String, Object> mainJobAttributes = new HashMap<>();
        activityUtils.getJobConfigurationDetails(activityJobId) >> mainJobAttributes
        activityUtils.getMainJobAttributes(activityJobId) >> mainJobAttributes
        jobEnvironment.getNodeName() >> nodeName
        jobLogList = new ArrayList<Map<String, Object>>();
        activityUtils.getActivityInfo(activityJobId, getClass()) >> jobActivityInfo;

        message.getCommonNotification() >> commonNotification
        commonNotification.getFdn() >> "fdn"
        Map<String, Object> additionalAttributes = new HashMap<>()
        additionalAttributes.put(Constants.ACTIVITY_JOB_ID, activityJobId)
        commonNotification.getAdditionalAttributes() >> additionalAttributes
        commonNotification.getProgressPercentage() >> "0.0"
    }

    def "testCancel" () {
        given: "initialize"
        miniLinkOutdoorJobUtil.getBackupActivityProperties(activityJobId, "restore", RestoreService.class) >> backupActivityProperties
        miniLinkOutdoorJobUtil.getSubscriptionKey(backupActivityProperties.getNodeName(), RESTORE) >> "subscriptionKey"
        activityUtils.getActivityInfo(activityJobId, RestoreService.class) >> jobActivityInfo
        activityUtils.unSubscribeToMoNotifications("subscriptionKey", activityJobId, jobActivityInfo) >> true
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
        activityUtils.getActivityStepResult(ActivityStepResultEnum.EXECUTION_FAILED) >> activityStepResult
        when: "invoke cancel"
        ActivityStepResult activityStepResultA = restoreService.cancel(activityJobId)
        then: "return value should not be null"
        Assert.notNull(activityStepResultA)
        activityStepResultA.getActivityResultEnum() ==  ActivityStepResultEnum.EXECUTION_FAILED
    }

    def "testPreCheck"() {
        given: "initialize"
        miniLinkOutdoorJobUtil.getBackupActivityProperties(activityJobId, "restore", RestoreService.class) >> backupActivityProperties
        backupActivityProperties.getBackupName() >> "testBackup"
        dpsUtils.getNeType(backupActivityProperties.getNodeName()) >> MINI_LINK_OUTDOOR
        smrsFileStoreService.getSmrsDetails(BACKUP, MINI_LINK_OUTDOOR, backupActivityProperties.getNodeName()) >> smrsAccountInfo
        backupSmrs.checkExistenceOfBackupFile(backupActivityProperties, smrsAccountInfo) >> true
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        miniLinkOutdoorJobUtil.precheckSuccess(PERCENT_ZERO, backupActivityProperties) >> activityStepResult
        when: "invoke precheck"
        ActivityStepResult activityStepResultA = restoreService.precheck(activityJobId);
        then: "return value should not be null"
        Assert.notNull(activityStepResultA)
        activityStepResultA.getActivityResultEnum() ==  ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
    }

    def "testExecute"() {
        given: "initialize"
        miniLinkOutdoorJobUtil.getBackupActivityProperties(activityJobId, "restore", RestoreService.class) >> backupActivityProperties
        when: "invoke execute"
        restoreService.execute(activityJobId)
        then : "expect nothing"
    }

    def "testCancelTimeout"() {
        when: "invoke cancel timeout"
        ActivityStepResult activityStepResult = restoreService.cancelTimeout(activityJobId, true);
        then: "return value should not be null"
        Assert.notNull(activityStepResult)
    }

    def "testHandleTimeout"() {
        given: "initialize"
        miniLinkOutdoorJobUtil.getBackupActivityProperties(activityJobId, "restore", RestoreService.class) >> backupActivityProperties
        backupActivityProperties.getBackupName() >> "testBackup"
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        miniLinkOutdoorJobUtil.timeoutFail(PERCENT_ZERO, backupActivityProperties) >> activityStepResult
        when: "invoke handle timeout"
        ActivityStepResult activityStepResultA = restoreService.handleTimeout(activityJobId);
        then: "return value should not be null"
        Assert.notNull(activityStepResultA)
        activityStepResultA.getActivityResultEnum() ==  ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
    }

    @Unroll("state=#state, result=#result")
    def 'testprocessNotification'() {
        given: "initialize"
        activityUtils.getActivityInfo(activityJobId, RestoreService.class) >> jobActivityInfo
        jobActivityInfo.getActivityJobId() >> activityJobId
        miniLinkOutdoorJobUtil.getBackupActivityProperties(activityJobId, "restore", RestoreService.class) >> backupActivityProperties
        backupActivityProperties.getBackupName() >> "testBackup"
        activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(activityJobId)
        when: "invoke process notification"
        commonNotification.getState() >> state
        restoreService.processNotification(message);
        then: "return value should not be null"
        if(state == "COMPLETE") {
            1 * miniLinkOutdoorJobUtil.succeedBackupRestoreActivity(neJobStaticData, activityJobId, null);
        } else if(state == "IDLE") {
            1 * miniLinkOutdoorJobUtil.succeedBackupRestoreActivity(neJobStaticData, activityJobId, null);
        } else if(state == "UPLOADING") {
            1 * activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(activityJobId);
        } else {
            1 * miniLinkOutdoorJobUtil.failBackupRestoreActivity(neJobStaticData, activityJobId, null, state, null);
        }

        where: "params for state"
        state                 |   result
        "COMPLETE"            |  "pass"
        "IDLE"                |  "pass"
        "UPLOADING"           |  "pass"
        "NONE"                |  "pass"
    }
}
