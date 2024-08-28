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
import com.ericsson.oss.services.shm.ecim.common.ActionResultType
import com.ericsson.oss.services.shm.ecim.common.ActionStateType
import com.ericsson.oss.services.shm.es.api.ActivityStepResult
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum
import com.ericsson.oss.services.shm.inventory.software.ecim.api.UpgradePackageState
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.model.NetworkElementData

class ConfirmServiceActivityTest extends EcimUpgradeDataProvider {

    @ObjectUnderTest
    ConfirmService confirmService

    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
    }


    def 'Test Precheck for confirm Activity'() {

        given:"NetworkElement and the confirm details"
        loadJobPropertiesForUpgrade(nodeName,"confirm",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true
        swMHandler.isActivityAllowed(_ as String, _ as String, _ as String, _ as String, _) >> getActivityAllowed(true)

        when:"Perform  precheck for the allocated job"
        ActivityStepResult activityStepResult=confirmService.precheck(activityJobId)

        then:"Verify If Precheck action of confirm Activity Step Result data is as expected"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION)
    }

    def 'Test Precheck for confirm Activity when exceptions occurred'() {

        given:"NetworkElement and the confirm details"
        loadJobPropertiesForUpgrade(nodeName,"confirm",SUCCESS)
        buildDataForUpgradeActivity(nodeName,senario)

        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;

        when:"Perform  precheck for the allocated job"
        ActivityStepResult activityStepResult=confirmService.precheck(activityJobId)

        then:"Verify If Activity Step Result data is Failed"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(activityResult)

        where:
        activityResult                                        |senario
        ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION | "JOB_NOT_FOUND_EXCEPTION"
        ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION |  "EXCEPTION"
    }
    def 'When confirm action is triggered and action is successful or Failed'() {

        given:"Data for confirm Action"
        loadJobPropertiesForUpgrade(nodeName,"confirm",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        swMHandler.isActivityAllowed(_ as String, _ as String, _ as String, _ as String, _) >> getActivityAllowed(true)
        getActionResult(true)
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> isValidTBAC

        when:"confirm action is triggered"
        confirmService.execute(activityJobId)

        then:"Check if action is triggered and result is as expected"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains(log))
        }

        where:
        jobResult     |isValidTBAC | log
        "SUCCESS"     |true        |"\"Confirm\" activity is completed successfully."
        "FAILED"      |false       |null
    }

    def 'When confirm action is triggered and action is failed'() {

        given:"Data for confirm Action"
        loadJobPropertiesForUpgrade(nodeName,"confirm",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        getActionResult(false)
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true
        swMHandler.isActivityAllowed(_ as String, _ as String, _ as String, _ as String, _) >> getActivityAllowed(false)

        when:"confirm action is triggered"
        confirmService.execute(activityJobId)

        then:"Check if action is triggered and Failed"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains(log))
        }
        where:
        jobResult     | log
        "FAILED"      |"Unable to proceed \"Confirm\" activity because \"Upgrade package is not in a expected state to proceed to Confirm\"."
        "FAILED"      |""
    }

    def 'When confirm action is triggered Job Failed due to exception'() {

        given:"Data for confirm Action"
        loadJobPropertiesForUpgrade(nodeName,"confirm",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> { throw new Exception("Exception occurred") };

        when:"confirm action is triggered"
        confirmService.execute(activityJobId)

        then:"Check if action is triggered and Failed"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains(log))
        }

        where:
        jobResult    |log
        "FAILED"     |"Unable to proceed \"Confirm\" activity because \"null\""
    }

    def 'When confirm action is triggered Job Failed due to exceptions'() {

        given:"Data for confirm Action"
        loadJobPropertiesForUpgrade(nodeName,"activate",SUCCESS)
        buildDataForUpgradeActivity(nodeName,exceptionSenario)
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY) >> neJobStaticData
        swMHandler.isActivityAllowed(_ as String, _ as String, _ as String, _ as String, _) >> getActivityAllowed(true)

        when:"confirm action is triggered"
        confirmService.execute(activityJobId)

        then:"Check if confirm action is Failed"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains("Unable to proceed \"Confirm\" activity because"))
        }

        where:
        jobResult|exceptionSenario
        "FAILED" |"FDN_EXCEPTION"
        "FAILED" |"FDN_UNSUPPORTED_FRAGMENT"
        "FAILED" |"FDN_MO_NOT_FOUND"
        "FAILED" |"FDN_NODE_ATTR_READER_EXCEPTION"
        "FAILED" |"FDN_SOFTWARE_PCGPO_EXCEPTION"
        "FAILED" |"FDN_SOFTWARE_PCGNAME_EXCEPTION"
        "FAILED" |"FDN_ARG_BUILDER_EXCEPTION"
        "FAILED" |"JOB_NOT_FOUND_EXCEPTION"
    }


    def 'Test executeAction of confirm Action for Success and Failed senario'() {

        given:"Data for confirm Action"
        loadJobPropertiesForUpgrade(nodeName,"activate",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY) >> neJobStaticData
        swMHandler.isActivityAllowed(_ as String, _ as String, _ as String, _ as String, _) >> getActivityAllowed(true)
        swMHandler.getUpgradePkgState(_, _, _, _) >> upgradePackageState
        swMHandler.executeMoAction(_, _, _, _, _ as NetworkElementData) >> { throw new Exception("Exception occurred") }

        when:"confirm action is triggered and result is as expected"
        confirmService.execute(activityJobId)

        then:"Check if confirm action is Failed"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains(log))
        }

        where:
        jobResult|upgradePackageState                   |exceptionSenario|log
        "FAILED" |UpgradePackageState.WAITING_FOR_COMMIT|"Exception"     |"\"Confirm\" activity has failed."
        "SUCCESS" |UpgradePackageState.COMMIT_COMPLETED  |"Exception"     |"\"Confirm\" activity is completed successfully."
    }

    def 'Test executeAction of confirm Action when getUpgradePkgState throws exception'() {

        given:"Data for confirm Action"
        loadJobPropertiesForUpgrade(nodeName,"activate",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY) >> neJobStaticData
        swMHandler.isActivityAllowed(_ as String, _ as String, _ as String, _ as String, _) >> getActivityAllowed(true)
        swMHandler.getUpgradePkgState(_, _, _, _) >> { throw new Exception("Exception occurred") }


        when:"confirm action is triggered and result is as expected"
        confirmService.execute(activityJobId)

        then:"Check if confirm action is Failed"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains(log))
        }

        where:
        jobResult|upgradePackageState                   |log
        "FAILED" |UpgradePackageState.WAITING_FOR_COMMIT|"\"Confirm\" activity has failed."
    }

    def 'When Notifications are not received for confirm activity and job went into handle timeout and it is successful'() {

        given:"Data for HandleTimoeut confirm Action"
        loadJobPropertiesForUpgrade(nodeName,"confirm",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS,)
        getAsyncActionProgress("Confirm",ActionStateType.FINISHED,ActionResultType.SUCCESS)
        swMHandler.isActivityCompleted(_ as String, _ as String, _ as String, _ as String,  _ as NetworkElementData) >> isActivityCompleted

        when:"Confirm action triggered got notifications"
        ActivityStepResult activityResult = confirmService.handleTimeout(activityJobId)

        then:"Check if Job is successful"
        activityResult.getActivityResultEnum().toString()== activityStepResult
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains(log))
        }

        where:
        jobResult|       activityStepResult                                                  |isActivityCompleted| log
        "SUCCESS"|ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS.toString() |true               |"\"Confirm\" activity is completed successfully."
        "FAILED" |ActivityStepResultEnum.TIMEOUT_RESULT_FAIL.toString() |false              |"\"Confirm\" activity has failed."
    }

    def 'When Notifications are not received for confirm activity and job went into handle timeout and it is still running'() {

        given:"Data for HandleTimoeut Confirm Action"
        loadJobPropertiesForUpgrade(nodeName,"confirm",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        getAsyncActionProgress("Confirm",ActionStateType.FINISHED,ActionResultType.FAILURE)
        swMHandler.isActivityCompleted(_ as String, _ as String, _ as String, _ as String,  _ as NetworkElementData) >> false

        when:"Confirm action triggered got notifications"
        ActivityStepResult activityResult = confirmService.handleTimeout(activityJobId)

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
        jobResult     |isValidCcvMoFdn   | log
        "FAILED"      |true               |"\"Confirm\" activity has failed."
    }

    def 'When Notifications are not received for confirm activity and job went into handle timeout and it is failed' () {

        given:"Data for HandleTimoeut Confirm Action"
        loadJobPropertiesForUpgrade(nodeName,"confirm",SUCCESS)
        buildDataForUpgradeActivity(nodeName,senario)
        getAsyncActionProgress("Confirm",ActionStateType.FINISHED,ActionResultType.FAILURE)
        swMHandler.isActivityCompleted(_ as String, _ as String, _ as String, _ as String,  _ as NetworkElementData) >> false

        when:"Confirm action triggered got notifications"
        ActivityStepResult activityResult = confirmService.handleTimeout(activityJobId)

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
        jobResult    |senario   | log
        "FAILED"     |"SUCCESS"  |"\"Confirm\" activity has failed."
        "FAILED"     |"EXCEPTION"| "\"Confirm\" action has failed in handle timeout."
    }


    def 'When Notifications are not received for confirm activity and job went into handle and Failed due to exceptions'() {

        given:"Data for HandleTimoeut confirm Action"
        loadJobPropertiesForUpgrade(nodeName,"confirm",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS,)
        getAsyncActionProgress("Confirm",ActionStateType.FINISHED,ActionResultType.SUCCESS)

        switch(exceptionSenario) {

            case "NodeAttributesReaderException":
                swMHandler.isActivityCompleted(_ as String, _ as String, _ as String, _ as String, _ as NetworkElementData)  >> { throw new NodeAttributesReaderException("NodeAttributesReaderException occurred") }
                break;
            case "UnsupportedFragmentException":
                swMHandler.isActivityCompleted(_ as String, _ as String, _ as String, _ as String,  _ as NetworkElementData) >> { throw new UnsupportedFragmentException("UnsupportedFragmentException occurred") }
                break;

            case "MoNotFoundException":
                swMHandler.isActivityCompleted(_ as String, _ as String, _ as String, _ as String,  _ as NetworkElementData)  >> { throw new MoNotFoundException("MoNotFoundException occurred") }
                break;
            case "Exception":
                swMHandler.isActivityCompleted(_ as String, _ as String, _ as String, _ as String,  _ as NetworkElementData)  >> { throw new Exception("Exception occurred") }
                break;
        }

        when:"Confirm action triggered got notifications"
        ActivityStepResult activityResult = confirmService.handleTimeout(activityJobId)

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
        "FAILED" |"Exception"                           |"\"Confirm\" activity has failed."
        "FAILED" |"MoNotFoundException"                 |"\"Confirm\" activity has failed."
        "FAILED" |"UnsupportedFragmentException"        |"\"Confirm\" activity has failed."
        "FAILED" |"NodeAttributesReaderException"       |"\"Confirm\" activity has failed."
    }


    def 'Test cancel action for confirm Activity and Job is successful'() {

        given:"NetworkElement and the confirm activity details"
        loadJobPropertiesForUpgrade(nodeName,"confirm",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        getAsyncActionProgress("confirm",ActionStateType.FINISHED,ActionResultType.SUCCESS)

        when:"confirm Activity has received notifications"
        confirmService.cancel(activityJobId)

        then:"Check if cancel action is Success"
        activityJobId !=null
    }

    def 'Test cancelTimeout action for confirm Activity and Job is successful'() {

        given:"NetworkElement and the confirm activity details"
        loadJobPropertiesForUpgrade(nodeName,"confirm",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        getAsyncActionProgress("confirm",ActionStateType.FINISHED,ActionResultType.SUCCESS)

        when:"confirm Activity has received notifications"
        confirmService.cancelTimeout(activityJobId,true)

        then:"Check if cancelTimeout action is Success"
        activityJobId !=null
    }

    def 'Test asyncPrecheck action for confirm Activity and Job is successful or Failed'() {

        given:"NetworkElement and the confirm activity details"
        loadJobPropertiesForUpgrade(nodeName,"confirm",SUCCESS)
        buildDataForUpgradeActivity(nodeName,senario)

        swMHandler.isActivityAllowed(_ as String, _ as String, _ as String, _ as String, _) >> getActivityAllowed(true)
        getAsyncActionProgress("confirm",ActionStateType.FINISHED,ActionResultType.SUCCESS)

        when:"confirm Activity has received notifications"
        confirmService.asyncPrecheck(activityJobId)

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
        null     |"EXCEPTION" |"Precheck for \"Confirm\" is failed."
        null     |"SUCCESS"   |"Precheck for \"Confirm\" activity is successful."
    }

    def 'Test precheckHandleTimeout action for confirm Activity and Job is successful'() {

        given:"NetworkElement and the confirm activity details"
        loadJobPropertiesForUpgrade(nodeName,"confirm",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        getAsyncActionProgress("confirm",ActionStateType.FINISHED,ActionResultType.SUCCESS)

        when:"confirm Activity has received notifications"
        confirmService.precheckHandleTimeout(activityJobId)

        then:"Check if precheckHandleTimeout action is Success"
        activityJobId !=null
    }

    def 'Test asyncHandleTimeout action for confirm Activity and Job is successful'() {

        given:"NetworkElement and the confirm activity details"
        loadJobPropertiesForUpgrade(nodeName,"confirm",SUCCESS)
        buildDataForUpgradeActivity(nodeName,senario)
        getAsyncActionProgress("confirm",ActionStateType.FINISHED,ActionResultType.SUCCESS)
        swMHandler.isActivityCompleted(_ as String, _ as String, _ as String, _ as String,  _ as NetworkElementData) >> true

        when:"confirm Activity has received notifications"
        confirmService.asyncHandleTimeout(activityJobId)

        then:"Check if asyncHandleTimeout action is Success"
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
        "FAILED" |"EXCEPTION" |"\"Confirm\" action has failed in handle timeout."
        "SUCCESS" |"SUCCESS"   |"\"Confirm\" activity is completed successfully."
    }

    def 'Test timeoutForAsyncHandleTimeout action for confirm Activity and Job is successful'() {

        given:"NetworkElement and the confirm activity details"
        loadJobPropertiesForUpgrade(nodeName,"confirm",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        getAsyncActionProgress("confirm",ActionStateType.FINISHED,ActionResultType.SUCCESS)

        when:"confirm Activity has received notifications"
        confirmService.timeoutForAsyncHandleTimeout(activityJobId)

        then:"Check if timeoutForAsyncHandleTimeout action is Success"
        activityJobId !=null
    }
}
