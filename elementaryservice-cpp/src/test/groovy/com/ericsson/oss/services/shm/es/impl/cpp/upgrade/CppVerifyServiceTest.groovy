/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.shm.es.impl.cpp.upgrade

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.modelservice.api.*
import com.ericsson.oss.services.shm.es.api.*
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException
import com.ericsson.oss.services.shm.notifications.api.*
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants

public class CppVerifyServiceTest extends CppUpgradeTestDataProvider {

    @ObjectUnderTest
    VerifyService verifyService;
    private long activityJobId ;

    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.common")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.es")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.shared")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.es.axe.common")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.jobs")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.nejob")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.networkelement")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job.cache")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job.api")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.common.job.utils")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.es.upgrade.remote")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.rest")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.jobs.common.modelentities")
    }

    def 'Test Upgrade Verify Precheck when TBAC is Success'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"verify",upMoFdn)
        buildTestDataForInstallAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEvent("verify"))
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        Map<String, Object> upgradePackageMoData=new HashMap<String, Object>();
        upgradePackageService.getUpMoData(_,_,_,_) >> upgradePackageMoData

        when:"Perform  upgrade package precheck for the allocated job "
        ActivityStepResult activityStepResult=verifyService.precheck(activityJobId)

        then:"Verify If Activity Step Result data is Successful"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION)
    }

    def 'When Verify action is triggered and action is successful'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"verify",upMoFdn)
        buildTestDataForInstallAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEvent("verify"))
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        dpsWriterProxy.performAction(_,_,_) >> 1

        when:"Verify Activity is triggered "
        verifyService.execute(activityJobId)

        then:"Verify if Verify Action is triggered Successfully"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == extractJobProperty(ShmConstants.RESULT, jobProperties)
        activityTriggered == extractJobProperty(ActivityConstants.ACTION_TRIGGERED, jobProperties)
        isActivityTriggered == extractJobProperty(ActivityConstants.IS_ACTIVITY_TRIGGERED, jobProperties)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        assert(activityJob.getAttribute("lastLogMessage").contains(log))

        1 * systemRecorder.recordEvent('SHM.VERIFY_EXECUTE ', _,_, _, _)
        2 * systemRecorder.recordCommand('SHM.VERIFY_SERVICE', _, _, _, _)

        where:
        jobResult  |activityTriggered |isActivityTriggered | log
        null       |  "verify"       |"true"              |"\"Verify\" activity is triggered (timeout = \"0\" minutes)"
    }

    def 'When Verify action is triggered and action is Failed'() {

        given:"NetworkElement and the invalid upgrade package details"
        upMoFdn=null
        productNumber=null
        productRevision=null
        activityJobId=loadJobProperties(nodeName,"verify",upMoFdn)
        buildTestDataForInstallAction(productNumber,productRevision,nodeName,buildDpsAttributeChangedEvent("verify"))
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> false
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP

        when:"Verify Activity is triggered"
        verifyService.execute(activityJobId)

        then:"Verify if Verify Action is Failed"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == extractJobProperty(ShmConstants.RESULT, jobProperties)
        log ==activityJob.getAttribute(ShmConstants.LOG)
        1 * systemRecorder.recordEvent(_,_,_,_, _)
        1 * workflowInstanceNotifier.sendActivate(_, _)

        where:
        jobResult  |log
        "FAILED"   | null
    }

    def 'When Verify action is triggered and action is Failed due to Exception'() {
        given:"NetworkElement and the invalid upgrade package details"
        upMoFdn=null
        activityJobId=loadJobProperties(nodeName,"verify",upMoFdn)
        buildTestDataForInstallAction(productNumber,productRevision,nodeName,buildDpsAttributeChangedEvent("verify"))
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> { throw new JobDataNotFoundException("Exception occurred while executing verify upgrade job") }
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        dpsWriterProxy.performAction(_,_,_) >> -1

        when:"Verify Activity is triggered"
        verifyService.execute(activityJobId)

        then:"Verify if Verify Action is Failed due to Exception"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == extractJobProperty(ShmConstants.RESULT, jobProperties)
        log ==activityJob.getAttribute(ShmConstants.LOG)
        1 * systemRecorder.recordEvent(_, _, _, _, _)
        1 * workflowInstanceNotifier.sendActivate(_, _)

        where:
        jobResult  |log
        "FAILED"   | null
    }

    def 'When Notfications are received for Verify Activity and Job is successful'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"verify",upMoFdn)
        buildTestDataForInstallAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEvent("verify"))
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        notificationRegistry.removeSubject(_) >> true

        when:"Verify action has received notifications"
        verifyService.processNotification(notification)

        then:"Check if Job is succesful through notifications"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == extractJobProperty(ShmConstants.RESULT, jobProperties)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains(log))

        where:
        jobResult  |log
        null  | "UpgradePackage State : \"VERIFICATION_EXECUTING\""
    }

    def 'When Notfications are received for Verify Activity and Job is failed as it is not successful on the node'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"verify",upMoFdn)
        buildTestDataForInstallAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEventForFail("verify"))
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        notificationRegistry.removeSubject(_) >> false

        when:"Verify action has received notifications"
        verifyService.processNotification(notification)

        then:"Check if Job is Failed through notifications"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == extractJobProperty(ShmConstants.RESULT, jobProperties)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains(log))

        where:
        jobResult|log
        null     | "Execution of an install or an upgrade action has failed"
    }

    def 'When Notfications are received for Verify Activity and Job is still running'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"verify",upMoFdn)
        buildTestDataForInstallAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEventForFail("verify"))
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        notificationRegistry.removeSubject(_) >> true

        when:"Verify action has received notifications and job is still running"
        verifyService.processNotification(notification)

        then:"Check if Job is failed as it is still running on the node after specified time out"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains(log))
        List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == extractJobProperty(ShmConstants.RESULT, jobProperties)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains(log))

        where:
        jobResult|log
        null     | "Execution of an install or an upgrade action has failed"
    }

    def 'When Notfications are received for Verify Activity and Job is failed due to exception'() {

        given:"NetworkElement and the upgrade package details"
        upMoFdn=null
        activityJobId=loadJobProperties(nodeName,"verify",upMoFdn)
        buildTestDataForInstallAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEventForFail("verify"))
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> { throw new JobDataNotFoundException("Exception occurred while executing upgrade verify job") }
        notificationRegistry.removeSubject(_) >> false
        activityUtils.getModifiedAttributes(_) >> { throw new JobDataNotFoundException("Exception occurred while executing upgrade verify job") }

        when:"Verify action has received notifications"
        verifyService.processNotification(notification)

        then:"Check if Job is Failed through notifications due to exception"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == extractJobProperty(ShmConstants.RESULT, jobProperties)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains(log))

        where:
        jobResult|log
        null     | "Execution of an install or an upgrade action has failed"
    }

    def 'When Notifications are not received for Verify activity and job went into handle timeout and it is successful'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"verify",upMoFdn)
        buildTestDataForInstallAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEvent("verify"))
        upgradePackageService.getUpMoAttributesByFdn(_,_) >> buildUpMoAttributeMap(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS,"VERIFY_COMPLETED",ActionResultInformation.EXECUTED_WITH_WARNINGS.toString())
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP

        when:"Verify action has received notifications"
        ActivityStepResult activityStepResult=verifyService.handleTimeout(activityJobId)

        then:"Check if Job is successful through Handle Timeout"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS)
        1 * systemRecorder.recordEvent('SHM.VERIFY_TIME_OUT ', _, _, _,_)
        1 * notificationRegistry.removeSubject(_)
    }

    def 'When Notifications are not received for Verify activity and job went into handle timeout and it is Failed'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"verify",upMoFdn)
        buildTestDataForInstallAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEvent("verify"))
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        upgradePackageService.getUpMoAttributesByFdn(_,_) >> buildUpMoAttributeMap(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL,"VERIFY_NOT_COMPLETED",ActionResultInformation.EXECUTION_FAILED.toString())

        when:"Verify action has received notifications"
        ActivityStepResult activityStepResult=verifyService.handleTimeout(activityJobId)

        then:"Check if Job is Failed through Handle Timeout"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL)
        1 * systemRecorder.recordEvent('SHM.VERIFY_TIME_OUT ', _, _, _, _)
    }

    def 'When Notifications are not received for Verify activity and job went into handle timeout and it is still running'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"verify",upMoFdn)
        buildTestDataForInstallAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEvent("verify"))
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        upgradePackageService.getUpMoAttributesByFdn(_,_) >> buildUpMoAttributeMap(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL,"VERIFY_EXECUTING",ActionResultInformation.EXECUTION_FAILED.toString())

        when:"Verify action has received notifications"
        ActivityStepResult activityStepResult=verifyService.handleTimeout(activityJobId)

        then:"Check if Job is Failed through Handle Timeout"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL)
        1 * systemRecorder.recordEvent('SHM.VERIFY_TIME_OUT ', _, _, _,_)
    }

    def 'When Verify action is triggered and action is  cancelled and cancel action is successful'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"verify",upMoFdn)
        buildTestDataForInstallAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEvent("verify"))
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP

        when:"Verify action is triggered and action is  cancelled"
        ActivityStepResult activityStepResult=verifyService.cancel(activityJobId)

        then:"Check if action is  cancelled"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        null !=activityJob.getAttribute(ShmConstants.LOG)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains(log))
        jobResult == extractJobProperty(ShmConstants.RESULT, jobProperties)

        where:
        jobResult|log
        null     | "Node does not support cancellation of \"Verify\" activity. Proceeding ahead for completion of the activity."
    }
}
