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
package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.softwareupgrade.service

import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.es.api.JobActivityInfo
import com.ericsson.oss.services.shm.es.impl.ActivityUtils
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.softwareupgrade.helper.SoftwareUpgradeJobService
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.softwareupgrade.helper.UpgradeJobInformation
import com.ericsson.oss.services.shm.es.api.ActivityStepResult

import static org.mockito.Mockito.when

import org.spockframework.util.Assert
import spock.lang.Unroll

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.shm.es.impl.JobEnvironment
import com.ericsson.oss.services.shm.generic.notification.SHMCommonCallBackNotificationJobProgressBean
import com.ericsson.oss.services.shm.job.cache.JobStaticData
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider
import com.ericsson.oss.services.shm.model.NetworkElementData
import com.ericsson.oss.services.shm.model.notification.SHMCommonCallbackNotification
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum

class ActivateServiceSpec extends CdiSpecification {

    @MockedImplementation
    ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    @MockedImplementation
    NEJobStaticData neJobStaticData;

    @MockedImplementation
    JobStaticData jobStaticData;

    @MockedImplementation
    ActivityUtils activityUtils;

    @MockedImplementation
    JobEnvironment jobEnvironment;

    @ObjectUnderTest
    ActivateService activateService;

    @MockedImplementation
    JobActivityInfo jobActivityInfo;

    @MockedImplementation
    SHMCommonCallBackNotificationJobProgressBean message;

    @MockedImplementation
    SHMCommonCallbackNotification commonNotification

    @MockedImplementation
    NeJobStaticDataProvider neJobStaticDataProvider

    @MockedImplementation
    JobStaticDataProvider jobStaticDataProvider

    @MockedImplementation
    NetworkElementRetrievalBean networkElementRetrivalBean

    @MockedImplementation
    NetworkElementData networkElement

    @MockedImplementation
    ActivityJobTBACValidator activityJobTBACValidator

    @MockedImplementation
    SoftwareUpgradeJobService softwareUpgradeActivityService

    @MockedImplementation
    UpgradeJobInformation upgradeJobInformation

    private static final long activityJobId = 123l;
    private static final long neJobId = 123l;
    private static final String nodeName = "nodeName";
    private static final String THIS_ACTIVITY = "activate";
    List<Map<String, Object>> jobLogList = null;

    def setup() {
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY) >> neJobStaticData
        neJobStaticData.getNeJobId() >> 123l
        neJobStaticData.getNodeName() >> nodeName

        activityUtils.getJobEnvironment(activityJobId) >> jobEnvironment;
        Map<String, Object> mainJobAttributes = new HashMap<>();
        activityUtils.getJobConfigurationDetails(activityJobId) >> mainJobAttributes
        activityUtils.getMainJobAttributes(activityJobId) >> mainJobAttributes
        jobEnvironment.getNodeName() >> nodeName
        jobLogList = new ArrayList<Map<String, Object>>();
        activityUtils.getActivityInfo(activityJobId, ActivateService.class) >> jobActivityInfo;
        jobActivityInfo.getActivityName() >> THIS_ACTIVITY;
        jobActivityInfo.getActivityJobId() >> activityJobId
        jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId()) >> jobStaticData

        activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, THIS_ACTIVITY) >> true
        networkElementRetrivalBean.getNetworkElementData(nodeName) >> networkElement
        networkElement.getNeFdn() >> "nodeFdn"

        message.getCommonNotification() >> commonNotification
        commonNotification.getFdn() >> "fdn"
        Map<String, Object> additionalAttributes = new HashMap<>()
        additionalAttributes.put(Constants.ACTIVITY_JOB_ID, activityJobId)
        commonNotification.getAdditionalAttributes() >> additionalAttributes
        commonNotification.getProgressPercentage() >> "0.0"
    }

    def "testCancel" () {
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
        activityUtils.getActivityStepResult(ActivityStepResultEnum.EXECUTION_FAILED) >> activityStepResult
        when: "invoke cancel"
        ActivityStepResult activityStepResultA = activateService.cancel(activityJobId);
        then: "return value should not be null"
        Assert.notNull(activityStepResultA)
        activityStepResultA.getActivityResultEnum() ==  ActivityStepResultEnum.EXECUTION_FAILED;
    }

    def "testPreCheck"() {
        when: "invoke precheck"
        ActivityStepResult activityStepResult = activateService.precheck(activityJobId);
        then: "return value should not be null"
        Assert.notNull(activityStepResult)
        activityStepResult.getActivityResultEnum() ==  ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
    }

    def "testExecute"() {
        when: "invoke execute"
        activateService.execute(activityJobId)
        then : "expect nothing"
    }

    def "testExecuteUpgradeInfoNull"() {
        given: "initialize"
        softwareUpgradeActivityService.buildUpgradeInformation(activityJobId) >> upgradeJobInformation
        when: "invoke execute"
        activateService.execute(activityJobId)
        then : "expect nothing"
    }

    def "testCancelTimeout"() {
        when: "invoke cancel timeout"
        ActivityStepResult activityStepResult = activateService.cancelTimeout(activityJobId, true);
        then: "return value should not be null"
        Assert.notNull(activityStepResult)
    }

    def "testHandleTimeout"() {
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        activityUtils.getActivityStepResult(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL) >> activityStepResult
        when: "invoke handle timeout"
        ActivityStepResult activityStepResultA = activateService.handleTimeout(activityJobId);
        then: "return value should not be null"
        Assert.notNull(activityStepResultA)
        activityStepResultA.getActivityResultEnum() ==  ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
    }

    @Unroll("state=#state, result=#result")
    def 'testprocessNotification'() {
        given: "initialize"
        activityUtils.getActivityInfo(activityJobId, ActivateService.class) >> jobActivityInfo
        jobActivityInfo.getActivityJobId() >> activityJobId
        when: "invoke process notification"
        commonNotification.getState() >> state
        activateService.processNotification(message)
        then: "return value should not be null"
        if(state == "DOWNLOADED") {
            1 * activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
        } else if(state == "ACTIVATING") {
            1 * activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
        } else if(state == "IDLE") {
            1 * activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
        } else if(state == "AWAITING_DOWNLOAD") {
            1 * activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
        }

        where: "params for state"
        state                 |   result
        "DOWNLOADED"          |  "pass"
        "ACTIVATING"          |  "pass"
        "IDLE"                |  "pass"
        "AWAITING_DOWNLOAD"   |  "pass"
        "NONE"                |  "fail"
    }
}
