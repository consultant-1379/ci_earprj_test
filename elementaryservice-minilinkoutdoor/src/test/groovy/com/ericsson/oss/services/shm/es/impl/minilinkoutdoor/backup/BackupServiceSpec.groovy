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

import com.ericsson.oss.services.shm.common.ResourceOperations
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.common.smrs.SmrsAccountInfo
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService
import com.ericsson.oss.services.shm.es.api.JobActivityInfo
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants
import com.ericsson.oss.services.shm.es.impl.ActivityUtils
import com.ericsson.oss.services.shm.es.impl.JobEnvironment
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.DPSUtils
import com.ericsson.oss.services.shm.es.api.ActivityStepResult
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider
import com.ericsson.oss.services.shm.model.notification.SHMCommonCallbackNotification
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator

import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.BACKUP
import static org.mockito.Mockito.when

import org.spockframework.util.Assert
import spock.lang.Unroll

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.shm.generic.notification.SHMCommonCallBackNotificationJobProgressBean
import com.ericsson.oss.services.shm.job.cache.JobStaticData
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum

class BackupServiceSpec extends CdiSpecification {

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
    BackupService backupService;

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
    ActivityJobTBACValidator activityJobTBACValidator

    @MockedImplementation
    BackupActivityProperties backupActivityProperties

    @MockedImplementation
    DPSUtils dpsUtils

    @MockedImplementation
    SmrsAccountInfo smrsAccountInfo

    @MockedImplementation
    SmrsFileStoreService smrsFileStoreService

    @MockedImplementation
    ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator

    @MockedImplementation
    ResourceOperations resourceOperations

    private static final long activityJobId = 123l;
    private static final String nodeName = "nodeName";
    private static final String THIS_ACTIVITY = "backup";
    private static final Double PERCENT_ZERO = 0.0
    List<Map<String, Object>> jobLogList = null;
    public static final String BACKUP_NAME_TEST = "backup";
    public static final String NODE_NAME = "ML-TN";
    public static final String MINI_LINK_OUTDOOR = "MINI_LINK_OUTDOOR";
    public static final String CONFIG_FILE_EXTENSION = "cdb";
    public static final String DOT = ".";
    public static final String SLASH = "/";
    public static final String UNDERSCORE = "_";
    public static final String EXPECTED_BACKUP_FILE_NAME = BACKUP_NAME_TEST + UNDERSCORE + NODE_NAME + DOT + CONFIG_FILE_EXTENSION;
    public static final String BACKUP_FILE_PATH_PREFIX = MINI_LINK_OUTDOOR + SLASH + EXPECTED_BACKUP_FILE_NAME;

    def setup() {
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY) >> neJobStaticData
        neJobStaticData.getNeJobId() >> 123l
        jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId()) >> jobStaticData
        activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, THIS_ACTIVITY) >> true

        activityUtils.getJobEnvironment(activityJobId) >> jobEnvironment
        Map<String, Object> mainJobAttributes = new HashMap<>()
        activityUtils.getJobConfigurationDetails(activityJobId) >> mainJobAttributes
        activityUtils.getMainJobAttributes(activityJobId) >> mainJobAttributes
        jobEnvironment.getNodeName() >> nodeName
        jobLogList = new ArrayList<Map<String, Object>>()
        activityUtils.getActivityInfo(activityJobId, getClass()) >> jobActivityInfo

        message.getCommonNotification() >> commonNotification
        commonNotification.getFdn() >> "fdn"
        Map<String, Object> additionalAttributes = new HashMap<>()
        additionalAttributes.put(Constants.ACTIVITY_JOB_ID, activityJobId)
        commonNotification.getAdditionalAttributes() >> additionalAttributes
        commonNotification.getProgressPercentage() >> "0.0"
    }

    def "testCancel" () {
        given: "initialize"
        miniLinkOutdoorJobUtil.getBackupActivityProperties(activityJobId, ActivityConstants.BACKUP, BackupService.class) >> backupActivityProperties
        miniLinkOutdoorJobUtil.getSubscriptionKey(backupActivityProperties.getNodeName(), BACKUP) >> "subscriptionKey"
        activityUtils.getActivityInfo(activityJobId, BackupService.class) >> jobActivityInfo
        activityUtils.unSubscribeToMoNotifications("subscriptionKey", activityJobId, jobActivityInfo) >> true
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
        activityUtils.getActivityStepResult(ActivityStepResultEnum.EXECUTION_FAILED) >> activityStepResult
        when: "invoke cancel"
        ActivityStepResult activityStepResultA = backupService.cancel(activityJobId)
        then: "return value should not be null"
        Assert.notNull(activityStepResultA)
        activityStepResultA.getActivityResultEnum() ==  ActivityStepResultEnum.EXECUTION_FAILED
    }

    def "testPreCheck"() {
        given: "initialize"
        miniLinkOutdoorJobUtil.getBackupActivityProperties(activityJobId, ActivityConstants.BACKUP, BackupService.class) >> backupActivityProperties
        backupActivityProperties.getBackupName() >> "testBackup"
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        miniLinkOutdoorJobUtil.precheckSuccess(PERCENT_ZERO, backupActivityProperties) >> activityStepResult
        when: "invoke precheck"
        ActivityStepResult activityStepResultA = backupService.precheck(activityJobId);
        then: "return value should not be null"
        Assert.notNull(activityStepResultA)
        activityStepResultA.getActivityResultEnum() ==  ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
    }

    def "testExecute"() {
        given: "initialize"
        miniLinkOutdoorJobUtil.getBackupActivityProperties(activityJobId, ActivityConstants.BACKUP, BackupService.class) >> backupActivityProperties
        backupActivityProperties.getNodeName() >> BACKUP_FILE_PATH_PREFIX
        dpsUtils.getNeType(BACKUP_FILE_PATH_PREFIX) >> MINI_LINK_OUTDOOR
        smrsFileStoreService.getSmrsDetails(BACKUP, MINI_LINK_OUTDOOR, BACKUP_FILE_PATH_PREFIX) >> smrsAccountInfo
        smrsAccountInfo.getPathOnServer() >> "pathOnServer"
        resourceOperations.fileExists("pathOnServer" + BACKUP_FILE_PATH_PREFIX) >> true
        backupActivityProperties.getBackupFileWithPath() >> BACKUP_FILE_PATH_PREFIX
        smrsAccountInfo.getServerIpAddress() >> "ipAddress"
        smrsAccountInfo.getUser() >> "user"
        smrsAccountInfo.getPassword() >> {'p'}
        when: "invoke execute"
        backupService.execute(activityJobId)
        then : "expect nothing"
    }

    def "testCancelTimeout"() {
        when: "invoke cancel timeout"
        ActivityStepResult activityStepResult = backupService.cancelTimeout(activityJobId, true);
        then: "return value should not be null"
        Assert.notNull(activityStepResult)
    }

    def "testHandleTimeout"() {
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        activityUtils.getActivityStepResult(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL) >> activityStepResult
        when: "invoke handle timeout"
        ActivityStepResult activityStepResultA = backupService.handleTimeout(activityJobId);
        then: "return value should not be null"
        Assert.notNull(activityStepResultA)
        activityStepResultA.getActivityResultEnum() ==  ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
    }

    @Unroll("state=#state, result=#result")
    def 'testprocessNotification'() {
        given: "initialize"
        activityUtils.getActivityInfo(activityJobId, BackupService.class) >> jobActivityInfo
        jobActivityInfo.getActivityJobId() >> activityJobId
        miniLinkOutdoorJobUtil.getBackupActivityProperties(activityJobId, ActivityConstants.BACKUP, BackupService.class) >> backupActivityProperties
        backupActivityProperties.getNodeName() >> BACKUP_FILE_PATH_PREFIX
        activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(activityJobId)
        miniLinkOutdoorJobUtil.getBackupActivityProperties(activityJobId, ActivityConstants.BACKUP, BackupService.class) >> backupActivityProperties
        backupActivityProperties.getNodeName() >> BACKUP_FILE_PATH_PREFIX
        dpsUtils.getNeType(BACKUP_FILE_PATH_PREFIX) >> MINI_LINK_OUTDOOR
        smrsFileStoreService.getSmrsDetails(BACKUP, MINI_LINK_OUTDOOR, BACKUP_FILE_PATH_PREFIX) >> smrsAccountInfo
        smrsAccountInfo.getPathOnServer() >> "pathOnServer"
        resourceOperations.fileExists("pathOnServer" + BACKUP_FILE_PATH_PREFIX) >> true
        backupActivityProperties.getBackupFileWithPath() >> BACKUP_FILE_PATH_PREFIX
        when: "invoke process notification"
        commonNotification.getState() >> state
        backupService.processNotification(message);
        then: "return value should not be null"
        if(state == "COMPLETE") {
            1 * miniLinkOutdoorJobUtil.succeedBackupRestoreActivity(neJobStaticData, activityJobId, null);
        } else if(state == "UPLOADING") {
            1 * activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(activityJobId);
        } else {
            1 * miniLinkOutdoorJobUtil.failBackupRestoreActivity(neJobStaticData, activityJobId, null, state, null);
        }

        where: "params for state"
        state                 |   result
        "COMPLETE"            |  "pass"
        "UPLOADING"           |  "pass"
        "NONE"                |  "pass"
    }
}
