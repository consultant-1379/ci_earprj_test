/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.ecim.upgrade

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException
import com.ericsson.oss.services.shm.common.exception.NodeAttributesReaderException
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException
import com.ericsson.oss.services.shm.ecim.common.*
import com.ericsson.oss.services.shm.es.api.ActivityStepResult
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum
import com.ericsson.oss.services.shm.es.ecim.backup.common.*
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.*
import com.ericsson.oss.services.shm.inventory.software.ecim.api.*
import com.ericsson.oss.services.shm.jobs.common.constants.*
import com.ericsson.oss.services.shm.model.NetworkElementData
import com.ericsson.oss.services.shm.nejob.cache.*
import com.ericsson.oss.services.shm.notifications.api.*
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants

class ActivateServiceActivityTest extends EcimUpgradeDataProvider {

    @ObjectUnderTest
    ActivateService activateService


    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
    }

    def 'Test Precheck for activate Activity'() {

        given:"NetworkElement and the Activate Activity details"
        loadJobPropertiesForUpgrade(nodeName,"activate",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true
        swMHandler.isActivityAllowed(_ as String, _ as String, _ as String, _ as String, _ as NetworkElementData) >> getActivityAllowed(true)

        when:"Perform  precheck for the allocated job"
        ActivityStepResult activityStepResult=activateService.precheck(activityJobId)

        then:"Verify If Precheck action of activate Activity Step Result data is as expected"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION)
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains("One Go activation is selected"))
        }
    }

    def 'Test Precheck for activate Activity when exceptions occurred'() {

        given:"NetworkElement and the Activate Activity details"
        loadJobPropertiesForUpgrade(nodeName,"activate",SUCCESS)
        buildDataForUpgradeActivity(nodeName,senario)

        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;

        when:"Perform  precheck for the allocated job"
        ActivityStepResult activityStepResult=activateService.precheck(activityJobId)

        then:"Verify If Activity Step Result data is Failed"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(activityResult)

        where:
        activityResult                                        |senario
        ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION | "JOB_NOT_FOUND_EXCEPTION"
        ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION |  "EXCEPTION"
    }

    def 'When activate action is triggered and action is successful or Failed'() {

        given:"NetworkElement and the Activate Activity details"
        loadJobPropertiesForUpgrade(nodeName,"activate",senario)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        swMHandler.isActivityAllowed(_ as String, _ as String, _ as String, _ as String, _ as NetworkElementData) >> getActivityAllowed(isActivityAllowed)
        getActionResult(true)
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true

        when:"Activate action is triggered"
        activateService.execute(activityJobId)

        then:"Check if action is triggered and result as expected"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains(log))
        }

        where:
        jobResult |isActivityAllowed |senario           | log
        null      |true              |"SUCCESS"         |"\"Activate\" activity is triggered"
        null      |true              |"PRECHECK_DONE"   |"\"Activate\" activity is triggered"
        "FAILED"  |false             |"SUCCESS"         |"Unable to proceed \"Activate\" activity because \"Upgrade package is not in either PREPARE_COMPLETED or ACTIVATION_STEP_COMPLETE state\"."
        null      |false             |"PRECHECK_DONE"   |"\"Activate\" activity is triggered"
    }

    def 'When activate action is triggered and action is failed'() {

        given:"NetworkElement and the Activate Activity details"
        loadJobPropertiesForUpgrade(nodeName,"activate",senario)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true

        when:"activate action is triggered"
        activateService.execute(activityJobId)

        then:"Check if action is triggered and Failed"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)

        where:
        jobResult    |senario
        "FAILED"     |"SUCCESS"
        "FAILED"     |"PRECHECK_DONE"
    }

    def 'When activate action is triggered and action is failed due to JobDataNotFoundException'() {

        given:"NetworkElement and the Activate Activity details"
        loadJobPropertiesForUpgrade(nodeName,ActivityConstants.ACTIVATE,SUCCESS)
        buildDataForUpgradeActivity(nodeName,"JOB_NOT_FOUND_EXCEPTION")

        when:"activate action is triggered"
        activateService.execute(activityJobId)

        then:"Check if action is Failed"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains(log))
        }

        where:
        jobResult  |log
        "FAILED"   |"\"Activate\" activity has failed."
    }



    def 'When activate action is triggered and action is failed due to excpetions occurred in executeAction'() {

        given:"NetworkElement and the Activate Activity details"
        loadJobPropertiesForUpgrade(nodeName,"activate",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY) >> neJobStaticData
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true
        swMHandler.isActivityAllowed(_ as String, _ as String, _ as String, _ as String, _ as NetworkElementData) >> getActivityAllowed(true)

        switch(senario) {
            case "Exception_UpMO":
                swMHandler.updateMOAttributes(_ as Map, _, _, _,_ as  NetworkElementData) >> { throw new Exception("Exception Occurred") }
                break;

            case "Exception":
                swMHandler.executeMoAction(_, _, _, _, _ as NetworkElementData) >> { throw new Exception("Exception Occurred") }
                break;
        }

        when:"activate action is triggered"
        activateService.execute(activityJobId)

        then:"Check if action is Failed"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains(log))
        }

        where:
        jobResult|senario                      |log
        "FAILED" |"Exception_UpMO"             |"\"Activate\" activity has failed."
        "FAILED" |"Exception"                  |"\"Activate\" activity has failed."
    }
    def 'When activate action is triggered and user has no TBAC access'() {

        given:"NetworkElement and the Activate Activity details"
        loadJobPropertiesForUpgrade(nodeName,"activate",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> false;

        when:"activate action is triggered"
        activateService.execute(activityJobId)

        then:"Check if action is triggered"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        "FAILED" == getJobProperty(ShmConstants.RESULT, jobProperties)
    }

    def 'When Notifications are not received for activate activity and job went into handle timeout and it is successful or Failed'() {

        given:"NetworkElement and the Activate Activity details"
        loadJobPropertiesForUpgrade(nodeName,"activate",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        swMHandler.isActivityCompleted(_ as String, _ as String, _ as String, _ as String,  _ as NetworkElementData) >> true
        AsyncActionProgress asyncActionProgress=getAsyncActionProgress("Activate",ActionStateType.FINISHED,ActionResultType.SUCCESS)
        swMHandler.getAsyncActionProgress(_ as String, _ as String, _ as String, _ as String, _) >> asyncActionProgress
        swMHandler.getValidAsyncActionProgress(_ as Map) >> asyncActionProgress
        swMHandler.isValidAsyncActionProgress(_ as String, _ as AsyncActionProgress) >> isValidAsyncActionProgress

        when:"Activate action triggered got notifications"
        ActivityStepResult activityResult = activateService.handleTimeout(activityJobId)

        then:"Check if Job is triggered and result as expected"
        activityResult.getActivityResultEnum().toString()== activityStepResult
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)

        where:
        jobResult | activityStepResult                                     |isValidAsyncActionProgress
        "FAILED" |ActivityStepResultEnum.TIMEOUT_RESULT_FAIL.toString()    |true
        "SUCCESS" |ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS.toString()|false
    }

    def 'When Notifications are not received for activate activity and job went into handle timeout and it is still running'() {

        given:"NetworkElement and the Activate Activity details"
        loadJobPropertiesForUpgrade(nodeName,,"activate",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        swMHandler.isActivityCompleted(_ as String, _ as String, _ as String, _ as String,  _ as NetworkElementData) >> false
        AsyncActionProgress asyncActionProgress=getAsyncActionProgress("Activate",ActionStateType.FINISHED,ActionResultType.SUCCESS)
        swMHandler.getAsyncActionProgress(_ as String, _ as String, _ as String, _ as String, _) >> asyncActionProgress
        swMHandler.getValidAsyncActionProgress(_ as Map) >> asyncActionProgress

        when:"Activate action triggered got notifications"
        ActivityStepResult activityResult = activateService.handleTimeout(activityJobId)

        then:"Check if Job is Failed"
        activityResult.getActivityResultEnum().toString()== ActivityStepResultEnum.TIMEOUT_RESULT_FAIL.toString()
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        "FAILED" == getJobProperty(ShmConstants.RESULT, jobProperties)
    }

    def 'When Notifications are not received for activate activity and job went into handle timeout and it is failed' () {

        given:"NetworkElement and the Activate Activity details"
        loadJobPropertiesForUpgrade(nodeName,,"activate",SUCCESS)
        buildDataForUpgradeActivity(nodeName,senario)
        swMHandler.isActivityCompleted(_ as String, _ as String, _ as String, _ as String,  _ as NetworkElementData) >> false
        AsyncActionProgress asyncActionProgress=getAsyncActionProgress("Activate",ActionStateType.FINISHED,ActionResultType.FAILURE)
        swMHandler.getAsyncActionProgress(_ as String, _ as String, _ as String, _ as String, _) >> asyncActionProgress
        swMHandler.getValidAsyncActionProgress(_ as Map) >> asyncActionProgress

        when:"Activate action triggered got notifications"
        ActivityStepResult activityResult = activateService.handleTimeout(activityJobId)

        then:"Check if Job is Failed"
        activityResult.getActivityResultEnum().toString()== ActivityStepResultEnum.TIMEOUT_RESULT_FAIL.toString()
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)

        where:
        jobResult   |senario
        "FAILED"    |"SUCCESS"
        "FAILED"    |"FDN_EXCEPTION"
    }

    def 'When Notifications are not received for activate activity and it is failed due to exception' () {

        given:"NetworkElement and the Activate Activity details"
        loadJobPropertiesForUpgrade(nodeName,,"activate",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        swMHandler.isActivityCompleted(_ as String, _ as String, _ as String, _ as String, _ as NetworkElementData) >> false

        switch(exceptionSenario) {

            case "NodeAttributesReaderException":
                swMHandler.getAsyncActionProgress(_ as String, _ as String, _ as String, _ as String,  _ as NetworkElementData) >> { throw new NodeAttributesReaderException("NodeAttributesReaderException occurred") }
                break;
            case "UnsupportedFragmentException":
                swMHandler.getAsyncActionProgress(_ as String, _ as String, _ as String, _ as String,  _ as NetworkElementData) >> { throw new UnsupportedFragmentException("UnsupportedFragmentException occurred") }
                break;

            case "MoNotFoundException":
                swMHandler.getAsyncActionProgress(_ as String, _ as String, _ as String, _ as String,  _ as NetworkElementData) >> { throw new MoNotFoundException("MoNotFoundException occurred") }
                break;
            case "Exception":
                swMHandler.getAsyncActionProgress(_ as String, _ as String, _ as String, _ as String,  _ as NetworkElementData) >> { throw new Exception("Exception occurred") }
                break;
        }


        when:"Activate action triggered got notifications"
        ActivityStepResult activityResult = activateService.handleTimeout(activityJobId)

        then:"Check if Job is Failed"
        activityResult.getActivityResultEnum().toString()== ActivityStepResultEnum.TIMEOUT_RESULT_FAIL.toString()
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains(log))
        }

        where:
        jobResult|exceptionSenario                      |log
        "FAILED" |"Exception"                           |"Following exception has occurred in the middle of operation"
        "FAILED" |"MoNotFoundException"                 |"Following exception has occurred in the middle of operation"
        "FAILED" |"UnsupportedFragmentException"        |"Following exception has occurred in the middle of operation"
        "FAILED" |"NodeAttributesReaderException"       |"Following exception has occurred in the middle of operation"
    }

    def 'When Notifications are received for Activate Activity and Job is successful or Failed'() {

        given: "Data for Process Notification for Activate Action"
        loadJobPropertiesForUpgrade(nodeName,actionTriggered,SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        AsyncActionProgress asyncActionProgress=getAsyncActionProgress(actionTriggered,ActionStateType.FINISHED,actionResultType)
        swMHandler.getAsyncActionProgress(_ as String, _ as String, _ as String, _ as String,  _ as NetworkElementData) >> asyncActionProgress
        swMHandler.getValidAsyncActionProgress(_ as Map) >> asyncActionProgress
        swMHandler.isActivityAllowed(_ as String, _ as String, _ as String, _ as String, _ as NetworkElementData) >> getActivityAllowed(true)
        swMHandler.isValidAsyncActionProgress(_ as String, _ as AsyncActionProgress) >> isValidAsyncActionProgress
        swMHandler.getUpgradePackageState(_ as Map) >> ActivateState.ACTIVATE_STEP_COMPLETED
        notification.getNotificationEventType() >> NotificationEventTypeEnum.AVC

        when:"Activate action has received notifications"
        activateService.processNotification(notification)

        then: "Check if Job is successful or Failed"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains(log))
        }

        where:
        jobResult  |isValidAsyncActionProgress|actionTriggered         |actionResultType| log
        "CANCELLED"|    true                  |"Cancel Upgrade Package"|ActionResultType.SUCCESS |"\"Cancel Upgrade Package\" activity is completed successfully."
        "SUCCESS"  |   true                   |"activate"              |ActionResultType.SUCCESS |"\"Activate\" activity is completed successfully."
        null       |    false                 |"Cancel Upgrade Package"|ActionResultType.SUCCESS |"\"Cancel Upgrade Package\" activity is completed successfully."
        "SUCCESS"  |   false                  |"activate"              |ActionResultType.SUCCESS |"\"Activate\" activity is completed successfully."
        "FAILED"   |   true                   |"activate"              |ActionResultType.FAILURE |"\"Activate\" activity has failed."
    }

    def 'When Notifications are received for Activate Activity and Job is Failed'() {

        given: "Data for Process Notification for Activate Action"
        loadJobPropertiesForUpgrade(nodeName,"activate",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        notification.getNotificationEventType() >> NotificationEventTypeEnum.AVC
        switch(exceptionSenario) {

            case "NodeAttributesReaderException":
                swMHandler.getValidAsyncActionProgress(_ as Map)  >> { throw new NodeAttributesReaderException("NodeAttributesReaderException occurred") }
                break;
            case "UnsupportedFragmentException":
                swMHandler.getValidAsyncActionProgress(_ as Map)  >> { throw new UnsupportedFragmentException("UnsupportedFragmentException occurred") }
                break;

            case "MoNotFoundException":
                swMHandler.getValidAsyncActionProgress(_ as Map)  >> { throw new MoNotFoundException("MoNotFoundException occurred") }
                break;
            case "Exception":
                swMHandler.getValidAsyncActionProgress(_ as Map)  >> { throw new Exception("Exception occurred") }
                break;
        }

        when:"Activate action has received notifications"
        activateService.processNotification(notification)

        then: "Check if Job is Failed"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains(log))
        }

        where:
        jobResult|exceptionSenario                  |log
        null |"Exception"                           |"Following exception has occurred in the middle of operation"
        null |"MoNotFoundException"                 |"Following exception has occurred in the middle of operation"
        null |"UnsupportedFragmentException"        |"Following exception has occurred in the middle of operation"
        null |"NodeAttributesReaderException"       |"Following exception has occurred in the middle of operation"
    }

    def 'When Notifications are received for Activate Activity and Discarding non-AVC notification'() {

        given: "Data for Process Notification for Activate Action"
        loadJobPropertiesForUpgrade(nodeName,"activate",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)


        when:"Activate action has received notifications"
        activateService.processNotification(notification)

        then: "Check if Job is Failed"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        null == getJobProperty(ShmConstants.RESULT, jobProperties)
    }


    def 'Test cancel action for Activate Activity and Job is successful or Failed'() {

        given:"NetworkElement and the Activate activity details"
        loadJobPropertiesForUpgrade(nodeName,"activate",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        swMHandler.isActivityAllowed(_ as String, _ as String, _ as String, _ as String, _ as NetworkElementData) >> getActivityAllowed(true)

        if(isValidSenario) {
            actionResult.setActionId(1)
            actionResult.setTriggerSuccess(true)
        } else {
            actionResult.setActionId(-1)
            actionResult.setTriggerSuccess(false)
        }
        swMHandler.executeCancelAction(_, _, _, _, _ as NetworkElementData) >> actionResult

        when:"Activate  Activity has received notifications"
        ActivityStepResult activityStepResult=activateService.cancel(activityJobId)

        then:"Check if cancel action is as expected"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(activityResult)

        where:
        activityResult                          |isValidSenario
        ActivityStepResultEnum.EXECUTION_SUCESS |true
        ActivityStepResultEnum.EXECUTION_FAILED |false
    }

    def 'Test cancel action for Activate Activity for exceptions'() {

        given:"NetworkElement and the Activate activity details"
        loadJobPropertiesForUpgrade(nodeName,"activate",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        actionResult.setActionId(1)
        actionResult.setTriggerSuccess(true)

        switch(exceptionSenario) {

            case "UnsupportedFragmentException":
                swMHandler.executeCancelAction(_, _, _, _, _ as NetworkElementData)  >> { throw new UnsupportedFragmentException("UnsupportedFragmentException occurred") }
                break;

            case "Exception":
                swMHandler.executeCancelAction(_, _, _, _, _ as NetworkElementData) >> { throw new Exception("Exception occurred") }
                break;
        }


        when:"Activate  Activity has received notifications"
        ActivityStepResult activityStepResult=activateService.cancel(activityJobId)

        then:"Check if cancel action is Failed"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(activityResult)

        where:
        activityResult                          |exceptionSenario
        ActivityStepResultEnum.EXECUTION_FAILED |"Exception"
        ActivityStepResultEnum.EXECUTION_FAILED |"UnsupportedFragmentException"
    }

    def 'Test cancelTimeout action for Activate Activity'() {

        given:"NetworkElement and the Activate activity details"
        loadJobPropertiesForUpgrade(nodeName,"cancel",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        swMHandler.isValidAsyncActionProgress(_ as String, _ as AsyncActionProgress) >> isValidAsyncActionProgress

        getAsyncActionProgress("cancel",actionState,ActionResultType.SUCCESS)
        when:"Activate Activity has received notifications"
        ActivityStepResult activityStepResult=activateService.cancelTimeout(activityJobId,true)

        then:"Check if cancelTimeout action is as expected"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(activityResult)

        where:
        activityResult                             |actionState|isValidAsyncActionProgress
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL |ActionStateType.FINISHED|true
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL |ActionStateType.RUNNING|true
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL |ActionStateType.FINISHED|false
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL |ActionStateType.RUNNING|false
    }

    def 'Test cancelTimeout action for Activate Activity for exception'() {

        given:"NetworkElement and the Activate activity details"
        loadJobPropertiesForUpgrade(nodeName,"cancel",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        swMHandler.isValidAsyncActionProgress(_ as String, _) >> { throw new Exception("Exception occurred") }

        getAsyncActionProgress("cancel",actionState,ActionResultType.SUCCESS)
        when:"Activate Activity has received notifications"
        ActivityStepResult activityStepResult=activateService.cancelTimeout(activityJobId,true)

        then:"Check if cancelTimeout action is as expected"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(activityResult)

        where:
        activityResult                             |actionState
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL |ActionStateType.FINISHED
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL |ActionStateType.RUNNING
    }

    def 'Test asyncPrecheck action for Activate Activity and Job is successful or Failed'() {

        given:"NetworkElement and the Activate activity details"
        loadJobPropertiesForUpgrade(nodeName,"activate",SUCCESS)
        buildDataForUpgradeActivity(nodeName,senario)
        getAsyncActionProgress("Activate",ActionStateType.FINISHED,ActionResultType.SUCCESS)
        swMHandler.isActivityAllowed(_ as String, _ as String, _ as String, _ as String, _) >> getActivityAllowed(true)

        when:"Activate Activity has received notifications"
        activateService.asyncPrecheck(activityJobId)

        then:"Check if asyncPrecheck action is Success or Failed"
        activityJobId !=null

        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains(log))
        }


        where:
        jobResult|senario     |log
        null     |"EXCEPTION" |"Precheck for \"Activate\" is failed."
        null     |"SUCCESS"   |"One Go activation is selected"
    }

    def 'Test precheckHandleTimeout action for Activate Activity and Job is successful'() {

        given:"NetworkElement and the Activate activity details"
        loadJobPropertiesForUpgrade(nodeName,"activate",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        getAsyncActionProgress("Activate",ActionStateType.FINISHED,ActionResultType.SUCCESS)

        when:"Activate Activity has received notifications"
        activateService.precheckHandleTimeout(activityJobId)

        then:"Check if precheckHandleTimeout action is Success"
        activityJobId !=null
    }

    def 'Test asyncHandleTimeout action for Activate Activity'() {

        given:"NetworkElement and the Activate activity details"
        loadJobPropertiesForUpgrade(nodeName,"activate",SUCCESS)
        buildDataForUpgradeActivity(nodeName,senario)
        final AsyncActionProgress asyncActionProgress=getAsyncActionProgress("Activate",ActionStateType.FINISHED,ActionResultType.SUCCESS)
        swMHandler.getAsyncActionProgress(_ as String, _ as String, _ as String, _ as String, _) >> asyncActionProgress
        swMHandler.getValidAsyncActionProgress(_ as Map) >> asyncActionProgress
        //getAsyncActionProgress("Activate",ActionStateType.FINISHED,ActionResultType.SUCCESS)
        swMHandler.isValidAsyncActionProgress(_ as String, _ as AsyncActionProgress) >> false
        swMHandler.isActivityCompleted(_ as String, _ as String, _ as String, _ as String,  _ as NetworkElementData) >> isActivityCompleted

        when:"Activate Activity has received notifications"
        activateService.asyncHandleTimeout(activityJobId)

        then:"Check if asyncHandleTimeout action is as expected"
        activityJobId !=null

        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains(log))
        }

        where:
        log                                                | jobResult|senario        |isActivityCompleted
        "\"Activate\" activity is completed successfully." |"SUCCESS" |"SUCCESS"      |  true
        "\"Activate\" activity has failed."                |"FAILED"  |"SUCCESS"      |false
        "\"Activate\" action has failed in handle timeout."| "FAILED" |"FDN_EXCEPTION"|true
        "\"Activate\" action has failed in handle timeout."| "FAILED" |"EXCEPTION"    |true
    }

    def 'Test timeoutForAsyncHandleTimeout action for Activate Activity'() {

        given:"NetworkElement and the Activate activity details"
        loadJobPropertiesForUpgrade(nodeName,"activate",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        getAsyncActionProgress("Activate",ActionStateType.FINISHED,ActionResultType.SUCCESS)

        when:"Activate Activity has received notifications"
        activateService.timeoutForAsyncHandleTimeout(activityJobId)

        then:"Check if timeoutForAsyncHandleTimeout action is as expected"
        activityJobId !=null
    }

    def 'Test subscribeForPolling for Activate Jobs'() {

        given:"NetworkElement and Activate details"
        loadJobPropertiesForUpgrade(nodeName,"activate",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        getAsyncActionProgress("Activate",ActionStateType.FINISHED,ActionResultType.SUCCESS)

        when: "subscribeForPolling for Activate job"
        activateService.subscribeForPolling(activityJobId)

        then:"Check if subscribeForPolling for  Activate job success"
        1 * systemRecorder.recordEvent('SHM.POLLING_SUBSCRIPTION_SUCCESS', _, 'activate', 'UPGRADE', 'SHM:3')
    }

    def 'Test subscribeForPolling for Activate job Jobs When is is Failed due to Runtime Exception '() {

        given:"NetworkElement and Activate job details"
        loadJobPropertiesForUpgrade(nodeName,"activate",SUCCESS)
        buildDataForUpgradeActivity(nodeName,senario)
        getAsyncActionProgress("Activate",ActionStateType.FINISHED,ActionResultType.SUCCESS)

        when:"subscribeForPolling for Activate job"
        activateService.subscribeForPolling(activityJobId)

        then:"Check if subscribeForPolling for  Activate job is Failed"
        0 * systemRecorder.recordEvent('SHM.POLLING_SUBSCRIPTION_SUCCESS', _, 'activate', 'UPGRADE', 'SHM:1')

        where:
        activityResult                             |senario
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL |"SUCCESS"
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL |"FDN_EXCEPTION"
    }

    def 'Test processPollingResponse for Activate job Jobs'() {

        given:"NetworkElement and Activate job details"
        loadJobPropertiesForUpgrade(nodeName,actionName,SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        getAsyncActionProgress(actionName,state,actionResultType)
        final Map<String, Object> responseAttributes=new HashMap<>()
        final Map<String, Object> progressReportMap=new HashMap<>()
        progressReportMap.put(EcimCommonConstants.UpgradePackageMoConstants.UP_MO_REPORT_PROGRESS, getReportProgress(actionName,state,actionResultType))
        progressReportMap.put(EcimCommonConstants.UpgradePackageMoConstants.UP_MO_STATE,upgradePackageState)
        responseAttributes.put( ShmConstants.MO_ATTRIBUTES, progressReportMap)
        responseAttributes.put( ShmConstants.FDN,upMoFdn)
        swMHandler.isValidAsyncActionProgress(_ as String, _ as AsyncActionProgress) >> isValidAsync

        when:"processPollingResponse for Activate job action is triggered"
        activateService.processPollingResponse(activityJobId,responseAttributes)

        then:"Check if subscribeForPolling for Activate job action is success "

        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT,jobProperties)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains(log))
        }

        where:
        jobResult   |upgradePackageState                                     |isValidAsync|actionResultType         |state                   |actionName               |log
        null        |UpgradePackageState.DEACTIVATION_IN_PROGRESS.toString() |true        | ActionResultType.FAILURE|ActionStateType.CANCELLED|"activate"              |"\"Activate\" activity is completed successfully."
        "SUCCESS"   |UpgradePackageState.ACTIVATION_STEP_COMPLETED.toString()|true        | ActionResultType.SUCCESS|ActionStateType.FINISHED|"activate"               |"\"Activate\" activity is completed successfully."
        "SUCCESS"   |UpgradePackageState.COMMIT_COMPLETED.toString()         |true        | ActionResultType.SUCCESS|ActionStateType.FINISHED|"activate"               |"\"Activate\" activity is completed successfully."
        null        |UpgradePackageState.COMMIT_COMPLETED.toString()         |true        | ActionResultType.SUCCESS|ActionStateType.FINISHED|""                       |"\"Activate\" activity is completed successfully."
        "CANCELLED" |UpgradePackageState.ACTIVATION_STEP_COMPLETED.toString()|true        | ActionResultType.SUCCESS|ActionStateType.FINISHED|"Cancel Upgrade Package" |"\"Cancel Upgrade Package\" activity is completed successfully."
        null        |UpgradePackageState.ACTIVATION_STEP_COMPLETED.toString()|false       |ActionResultType.SUCCESS |ActionStateType.RUNNING |"activate"               |"Progress Percentage : 10; ProgressInfo : progressinfo; State: RUNNING;"
        null        |UpgradePackageState.ACTIVATION_STEP_COMPLETED.toString()|false       |ActionResultType.SUCCESS |ActionStateType.RUNNING |"Cancel Upgrade Package" |"Progress Percentage : 10; ProgressInfo : progressinfo; State: RUNNING;"
        "FAILED"    |UpgradePackageState.ACTIVATION_STEP_COMPLETED.toString()|true        | ActionResultType.FAILURE|ActionStateType.FINISHED|"activate"               |"\"Activate\" activity has failed."
        "FAILED"    |UpgradePackageState.COMMIT_COMPLETED.toString()         |true        |ActionResultType.FAILURE |ActionStateType.FINISHED |"Cancel Upgrade Package" |"\"Cancel Upgrade Package\" activity has failed."
    }

    def 'Test processPollingResponse for Activate job Jobs when Job Failed'() {

        given:"NetworkElement and Activate job details"
        loadJobPropertiesForUpgrade(nodeName,"activate",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        getAsyncActionProgress("activate",ActionStateType.FINISHED,ActionResultType.SUCCESS)
        final Map<String, Object> responseAttributes=new HashMap<>()

        when:"processPollingResponse for Activate job action is triggered"
        activateService.processPollingResponse(activityJobId,responseAttributes)

        then:"Check if subscribeForPolling for Activate job action is Failed"

        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        null == getJobProperty(ShmConstants.RESULT,jobProperties)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains("Following exception has occurred in the middle of operation"))
        }

    }
}
