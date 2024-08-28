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
import com.ericsson.oss.itpf.sdk.recording.CommandPhase
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.es.api.ActivityStepResult
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants


public class CppInstallServiceTest extends CppUpgradeTestDataProvider {

    @ObjectUnderTest
    InstallService installService;
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

    def 'Test Upgrade Install Precheck when TBAC is Success'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"install",upMoFdn)
        buildTestDataForInstallAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEvent("install"))
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        Map<String, Object> upgradePackageMoData=new HashMap<String, Object>();
        upgradePackageService.getUpMoData(_,_,_,_) >> upgradePackageMoData

        when:"Perform  upgrade package precheck for the allocated job "
        ActivityStepResult activityStepResult=installService.precheck(activityJobId)

        then:"Verify If Activity Step Result data is Successful"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION)
    }

    def 'Test Upgrade Install Precheck when TBAC is Failed'() {

        given:"NetworkElement and the upgrade package details"

        activityJobId=loadJobProperties(nodeName,"install",upMoFdn)
        buildTestDataForInstallAction(productNumber,productRevision,nodeName,buildDpsAttributeChangedEvent("install"))
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> false
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP

        when:"Perform  upgrade package precheck for the allocated job"
        ActivityStepResult activityStepResult=installService.precheck(activityJobId)

        then:"Verify If Activity Step Result data is Failed"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION)
    }

    def 'When install action is triggered and action is successful'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"install",upMoFdn)
        buildTestDataForInstallAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEvent("install"))
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        dpsWriterProxy.performAction(_,_,_) >> 1

        when:"Install Activity is triggered "
        installService.execute(activityJobId)

        then:"Verify if Install Action is triggered Successfully"
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

        1 * systemRecorder.recordCommand(_, CommandPhase.FINISHED_WITH_SUCCESS,_,_,_)
        1 * notificationRegistry.register(_)
        1 * dpsWriterProxy.performAction(_, _, _,_)
        1 * systemRecorder.recordEvent('SHM.INSTALL_EXECUTE ', _,_, _,_)

        where:
        jobResult  |activityTriggered |isActivityTriggered | log
        null       |  "install"       |"true"              |"\"Install\" activity is triggered (timeout = \"0\" minutes)"
    }

    def 'When install action is triggered and action is Failed'() {

        given:"NetworkElement and the invalid upgrade package details"
        upMoFdn=null
        productNumber=null
        productRevision=null
        activityJobId=loadJobProperties(nodeName,"install",upMoFdn)
        buildTestDataForInstallAction(productNumber,productRevision,nodeName,buildDpsAttributeChangedEvent("install"))
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> false
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP

        when:"Install Activity is triggered"
        installService.execute(activityJobId)

        then:"Verify if Install Action is Failed"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == extractJobProperty(ShmConstants.RESULT, jobProperties)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        assert(activityJob.getAttribute("lastLogMessage").contains(log))
        1 * systemRecorder.recordEvent('SHM.INSTALL_EXECUTE ', _, _, _, _)
        1 * workflowInstanceNotifier.sendActivate(_, _)

        where:
        jobResult  |log
        "FAILED"   | "Unable to proceed \"Install\" activity"
    }

    def 'When install action is triggered and action is Failed due to Exception'() {

        given:"NetworkElement and the invalid upgrade package details"
        upMoFdn=null
        activityJobId=loadJobProperties(nodeName,"install",upMoFdn)
        buildTestDataForInstallAction(productNumber,productRevision,nodeName,buildDpsAttributeChangedEventForFail("install"))
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> { throw new JobDataNotFoundException("Exception occurred while executing upgrade install job") }
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        dpsWriterProxy.performAction(_,_,_) >> -1
        when:"Install Activity is triggered"
        installService.execute(activityJobId)

        then:"Verify if Install Action is Failed due to Exception"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == extractJobProperty(ShmConstants.RESULT, jobProperties)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        assert(activityJob.getAttribute("lastLogMessage").contains(log))
        1 * systemRecorder.recordEvent('SHM.INSTALL_EXECUTE ', _, _, _, _)
        1 * workflowInstanceNotifier.sendActivate(_, _)

        where:
        jobResult  |log
        "FAILED"   | "Unable to proceed \"Install\" activity"
    }

    def 'When Notfications are received for install Activity and Job is successful'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"install",upMoFdn)
        buildTestDataForInstallAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEvent("install"))
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        notificationRegistry.removeSubject(_) >> true

        when:"Install action has received notifications"
        installService.processNotification(notification)

        then:"Check if Job is succesful through notifications"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == extractJobProperty(ShmConstants.RESULT, jobProperties)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains(log))
        1 * systemRecorder.recordEvent('SHM.INSTALL_PROCESS_NOTIFICATION', _, _, _, _)
        1 * workflowInstanceNotifier.sendActivate(_, _)

        where:
        jobResult  |log
        "SUCCESS"  | "\"Install\" activity is completed successfully"
    }

    def 'When Notfications are received for install Activity and Job is failed as it is not successful on the node'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"install",upMoFdn)
        buildTestDataForInstallAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEventForFail("install"))
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        notificationRegistry.removeSubject(_) >> false

        when:"Install action has received notifications"
        installService.processNotification(notification)

        then:"Check if Job is Failed through notifications"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == extractJobProperty(ShmConstants.RESULT, jobProperties)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains(log))

        where:
        jobResult|log
        null     | "UpgradePackage State : \"INSTALL_NOT_COMPLETED\""
    }

    def 'When Notfications are received for install Activity and Job is still running'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"install",upMoFdn)
        buildTestDataForInstallAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEventForFail("install"))
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        notificationRegistry.removeSubject(_) >> true

        when:"Install action has received notifications and job is still running"
        installService.processNotification(notification)

        then:"Check if Job is failed as it is still running on the node after specified time out"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains(log))
        List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == extractJobProperty(ShmConstants.RESULT, jobProperties)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains(log))

        where:
        jobResult|log
        null     | "UpgradePackage State : \"INSTALL_NOT_COMPLETED\""
    }

    def 'When Notfications are received for install Activity and Job is failed due to exception'() {

        given:"NetworkElement and the upgrade package details"
        upMoFdn=null
        activityJobId=loadJobProperties(nodeName,"install",upMoFdn)
        buildTestDataForInstallAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEventForFail("install"))
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> { throw new JobDataNotFoundException("Exception occurred while executing upgrade install job") }
        notificationRegistry.removeSubject(_) >> false
        activityUtils.getModifiedAttributes(_) >> { throw new JobDataNotFoundException("Exception occurred while executing upgrade install job") }

        when:"Install action has received notifications"
        installService.processNotification(notification)

        then:"Check if Job is Failed through notifications due to exception"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == extractJobProperty(ShmConstants.RESULT, jobProperties)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains(log))

        where:
        jobResult|log
        null     | "INSTALL_NOT_COMPLETED"
    }

    def 'When Notifications are not received for install activity and job went into handle timeout and it is successful'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"install",upMoFdn)
        buildTestDataForInstallAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEvent("install"))
        upgradePackageService.getUpMoAttributesByFdn(_,_) >> buildUpMoAttributeMap(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS,"INSTALL_COMPLETED",ActionResultInformation.EXECUTED.toString())
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP

        when:"Install action has received notifications"
        ActivityStepResult activityStepResult=installService.handleTimeout(activityJobId)

        then:"Check if Job is successful through Handle Timeout"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS)
        1 * systemRecorder.recordEvent('SHM.INSTALL_TIME_OUT ', _, _, _, _)
        1 * notificationRegistry.removeSubject(_)
    }

    def 'When Notifications are not received for install activity and job went into handle timeout and it is Failed'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"install",upMoFdn)
        buildTestDataForInstallAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEvent("install"))
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        upgradePackageService.getUpMoAttributesByFdn(_,_) >> buildUpMoAttributeMap(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL,"INSTALL_NOT_COMPLETED",ActionResultInformation.EXECUTION_FAILED.toString())

        when:"Install action has received notifications"
        ActivityStepResult activityStepResult=installService.handleTimeout(activityJobId)

        then:"Check if Job is Failed through Handle Timeout"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL)
        1 * systemRecorder.recordEvent('SHM.INSTALL_TIME_OUT ', _, _, _, _)
        1 * notificationRegistry.removeSubject(_)
    }

    def 'When Notifications are not received for install activity and job went into handle timeout and it is still running'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"install",upMoFdn)
        buildTestDataForInstallAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEvent("install"))
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        upgradePackageService.getUpMoAttributesByFdn(_,_) >> buildUpMoAttributeMap(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL,"INSTALL_EXECUTING",ActionResultInformation.EXECUTION_FAILED.toString())

        when:"Install action has received notifications"
        ActivityStepResult activityStepResult=installService.handleTimeout(activityJobId)

        then:"Check if Job is Failed through Handle Timeout"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL)
        1 * systemRecorder.recordEvent('SHM.INSTALL_TIME_OUT ', _, _, _, _)
        1 * notificationRegistry.removeSubject(_)
    }

    def 'When install action is triggered and action is  cancelled and cancel action is successful'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"install",upMoFdn)
        buildTestDataForInstallAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEvent("install"))
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP

        when:"install action is triggered and action is  cancelled"
        ActivityStepResult activityStepResult=installService.cancel(activityJobId)

        then:"Check if action is  cancelled"
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.EXECUTION_SUCESS)
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains("\"cancelInstall\" activity is triggered"))
        1 *  notificationRegistry.register(_);
        1 * systemRecorder.recordEvent('SHM.INSTALL_CANCEL', _, _, _, _)
    }

    def 'When install action is triggered and action is  cancelled and cancel action is Failed'() {

        given:"NetworkElement and the upgrade package details"
        upMoFdn=null
        activityJobId=loadJobProperties(nodeName,"install",upMoFdn)
        buildTestDataForInstallAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEventForFail("install"))
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        when:"install action is triggered and action is not cancelled"
        ActivityStepResult activityStepResult=installService.cancel(activityJobId)

        then:"Check if action is not cancelled"
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.EXECUTION_FAILED)
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains("\"null\" activity is triggered on the node"))
        1 * systemRecorder.recordEvent('SHM.INSTALL_CANCEL', _, _, _, _)
    }
}
