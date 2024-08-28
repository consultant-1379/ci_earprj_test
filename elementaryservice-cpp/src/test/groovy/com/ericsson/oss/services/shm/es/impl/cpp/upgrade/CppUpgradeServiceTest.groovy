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
package com.ericsson.oss.services.shm.es.impl.cpp.upgrade

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.es.api.ActivityStepResult
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants

public class CppUpgradeServiceTest extends CppUpgradeTestDataProvider {

    @ObjectUnderTest
    private UpgradeService upgradeService;

    private long activityJobId ;

    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.common")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.es")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.shared")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.jobs")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.nejob")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.networkelement")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.rest")
    }

    def 'Test upgradeService Precheck when  Success'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"upgrade",upMoFdn)
        buildTestDataForUpgradeAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEventForUpgradeActivity())
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        upgradePackageService.getUpMoData(_,_,_,_) >> upMoAndAttributesForUpgradeAndConfirm("INSTALL_COMPLETED");

        when:"Perform  upgrade package precheck for the allocated job "
        ActivityStepResult activityStepResult=upgradeService.precheck(activityJobId)

        then:"Verify If Activity Step Result data is Successful"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION)
    }

    def 'Test upgradeService Precheck when failed  due to upState null '() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"upgrade",upMoFdn)
        buildTestDataForUpgradeAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEventForUpgradeActivity())
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        upgradePackageService.getUpMoData(_,_,_,_) >> upMoAndAttributesForUpgradeAndConfirm("null");

        when:"Perform  upgrade package precheck for the allocated job "
        ActivityStepResult activityStepResult=upgradeService.precheck(activityJobId)

        then:"Verify If Activity Step Result data is Successful"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION)
    }

    def 'Test upgradeService Precheck when failed due to MO does not exists'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"upgrade",upMoFdn)
        buildTestDataForUpgradeAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEventForUpgradeActivity())
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        upgradePackageService.getUpMoData(_,_,_,_) >> new HashMap<>();

        when:"Perform  upgrade package precheck for the allocated job "
        ActivityStepResult activityStepResult=upgradeService.precheck(activityJobId)

        then:"Verify If Activity Step Result data is Successful"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION)
    }

    def 'when upgrade action is triggered and action is successfull'() {
        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"upgrade",upMoFdn)
        buildTestDataForUpgradeAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEventForUpgradeActivity())
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        upgradePackageService.getUpMoData(_,_,_,_) >>  upMoAndAttributesForUpgradeAndConfirm("INSTALL_COMPLETED");

        when:"Perform  upgrade package execute for the allocated job "
        upgradeService.execute(activityJobId)

        then:"Verify If Activity Step Result data is Successful"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        activityTriggered == extractJobProperty(ActivityConstants.ACTION_TRIGGERED, jobProperties)
        isActivityTriggered == extractJobProperty(ActivityConstants.IS_ACTIVITY_TRIGGERED, jobProperties)
        assert(activityJob.getAttribute("lastLogMessage").contains(log))

        1 * notificationRegistry.register(_)
        1 * dpsWriterProxy.performAction(_, _, _,_)

        where:
        activityTriggered |isActivityTriggered | log
        "upgrade"       |"true"              |"\"Upgrade\" activity is triggered (timeout = \"0\" minutes)"
    }

    def 'when upgrade action is triggered and tbac is failed'() {
        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"upgrade",upMoFdn)
        buildTestDataForUpgradeAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEventForUpgradeActivity())
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> false
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        upgradePackageService.getUpMoData(_,_,_,_) >>  upMoAndAttributesForUpgradeAndConfirm("INSTALL_COMPLETED");

        when:"Perform  upgrade package precheck for the allocated job "
        upgradeService.execute(activityJobId)

        then:"Verify If Activity Step Result data is Successful"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        "FAILED" == extractJobProperty(ShmConstants.RESULT, jobProperties)
    }

    def 'When upgrade action is triggered and action is Failed due to Exception'() {
        given:"NetworkElement and the invalid upgrade package details"
        upMoFdn=null
        activityJobId=loadJobProperties(nodeName,"upgrade",upMoFdn)
        buildTestDataForUpgradeAction(productNumber,productRevision,nodeName,buildDpsAttributeChangedEventForUpgradeActivity())
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> { throw new JobDataNotFoundException("Exception in performAction execution") }
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        dpsWriterProxy.performAction(_,_,_) >> -1

        when:"upgrade Activity is triggered"
        upgradeService.execute(activityJobId)

        then:"Verify if upgrade Action is Failed due to Exception"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == extractJobProperty(ShmConstants.RESULT, jobProperties)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        assert(activityJob.getAttribute("lastLogMessage").contains(log))
        1 * systemRecorder.recordEvent('SHM.UPGRADE_EXECUTE ', _, _, _, _)

        where:
        jobResult  |log
        "FAILED"   | "Unable to trigger \"Upgrade\" activity on the node. "
    }

    def 'When Notfications are received for upgrade Activity and Job is successful'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"upgrade",upMoFdn)
        buildTestDataForUpgradeAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEventForUpgradeActivity())
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        notificationRegistry.removeSubject(_) >> true

        when:"upgrade action has received notifications"
        upgradeService.processNotification(notification)

        then:"Check if Job is succesful through notifications"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == extractJobProperty(ShmConstants.RESULT, jobProperties)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains(log))


        where:
        jobResult  |log
        "SUCCESS"  | "\"Upgrade\" activity is completed through Notifications."
    }

    def 'When Notfications are received for upgrade  Activity and Job is failed as it is not successful on the node'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"upgrade",upMoFdn)
        buildTestDataForUpgradeAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEventForFail("upgrade"))
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        notificationRegistry.removeSubject(_) >> false

        when:"upgrade action has received notifications"
        upgradeService.processNotification(notification)

        then:"Check if Job is Failed through notifications"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == extractJobProperty(ShmConstants.RESULT, jobProperties)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains(log))

        where:
        jobResult|log
        null     | "UpgradePackage State : \"INSTALL_COMPLETED\""
    }

    def 'When Notfications are received for upgrade  Activity and Job is still running'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"upgrade",upMoFdn)
        buildTestDataForUpgradeAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEventForFail("upgrade"))
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        notificationRegistry.removeSubject(_) >> true

        when:"Upgrade action has received notifications and job is still running"
        upgradeService.processNotification(notification)

        then:"Check if Job is failed as it is still running on the node after specified time out"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains(log))
        List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == extractJobProperty(ShmConstants.RESULT, jobProperties)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains(log))

        where:
        jobResult|log
        null     | "UpgradePackage State : \"INSTALL_COMPLETED\""
    }

    def 'When Notfications are received for upgrade Activity and Job is failed due to exception'() {

        given:"NetworkElement and the upgrade package details"
        upMoFdn=null
        activityJobId=loadJobProperties(nodeName,"upgrade",upMoFdn)
        buildTestDataForUpgradeAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEventForFail("upgrade"))
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> { throw new JobDataNotFoundException("Exception occurred while executing upgrade upgrade job") }
        notificationRegistry.removeSubject(_) >> false
        activityUtils.getModifiedAttributes(_) >> { throw new JobDataNotFoundException("Exception occurred while executing upgrade upgrade job") }

        when:"Upgrade action has received notifications"
        upgradeService.processNotification(notification)

        then:"Check if Job is Failed through notifications due to exception"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == extractJobProperty(ShmConstants.RESULT, jobProperties)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains(log))

        where:
        jobResult|log
        null     | "UpgradePackage State : \"INSTALL_COMPLETED\""
    }

    def 'When Notifications are not received for upgrade activity and job went into handle timeout and it is successful'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"upgrade",upMoFdn)
        buildTestDataForUpgradeAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEventForUpgradeActivity())
        upgradePackageService.getUpMoAttributesByFdn(_,_) >> buildUpMoAttributeMap(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS,"AWAITING_CONFIRMATION",ActionResultInformation.EXECUTED.toString())
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP

        when:"Upgrade action has received notifications"
        ActivityStepResult activityStepResult=upgradeService.handleTimeout(activityJobId)

        then:"Check if Job is successful through Handle Timeout"
        activityJobId !=null
        assert(activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS))
        1 * notificationRegistry.removeSubject(_)
    }

    def 'When Notifications are not received for upgrade activity and job went into handle timeout and it is Failed'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"upgrade",upMoFdn)
        buildTestDataForUpgradeAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEventForUpgradeActivity())
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        upgradePackageService.getUpMoAttributesByFdn(_,_) >> buildUpMoAttributeMap(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL,"INSTALL_COMPLETED",ActionResultInformation.EXECUTION_FAILED.toString())

        when:"Upgrade action has received notifications"
        ActivityStepResult activityStepResult=upgradeService.handleTimeout(activityJobId)

        then:"Check if Job is Failed through Handle Timeout"
        activityJobId !=null
        assert(activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL))
        1 * notificationRegistry.removeSubject(_)
    }

    def 'When Notifications are not received for upgrade activity and job went into handle timeout and it is still running'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"upgrade",upMoFdn)
        buildTestDataForUpgradeAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEventForUpgradeActivity())
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        upgradePackageService.getUpMoAttributesByFdn(_,_) >> buildUpMoAttributeMap(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL,"UPGRADE_EXECUTING",ActionResultInformation.EXECUTION_FAILED.toString())

        when:"Upgrade action has received notifications"
        ActivityStepResult activityStepResult=upgradeService.handleTimeout(activityJobId)

        then:"Check if Job is Failed through Handle Timeout"
        activityJobId !=null
        assert(activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL))
        1 * notificationRegistry.removeSubject(_)
    }

    def 'When upgrade action is triggered and action is  cancelled and cancel action is successful'() {

        given:"NetworkElement and the upgrade package details"
        activityJobId=loadJobProperties(nodeName,"upgrade",upMoFdn)
        buildTestDataForUpgradeAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEventForUpgradeActivity())
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP

        when:"upgrade action is triggered and action is  cancelled"
        ActivityStepResult activityStepResult=upgradeService.cancel(activityJobId)

        then:"Check if action is  cancelled"
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.EXECUTION_SUCESS)
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains("Cancel Upgrade action is successfully invoked"))
    }

    def 'When upgrade action is triggered and action is  cancelled and cancel action is Failed'() {

        given:"NetworkElement and the upgrade package details"
        upMoFdn=null
        activityJobId=loadJobProperties(nodeName,"upgrade",upMoFdn)
        buildTestDataForUpgradeAction(productNumber, productRevision,nodeName,buildDpsAttributeChangedEventForUpgradeActivity())
        platformTypeProvider.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.CPP
        when:"upgrade action is triggered and action is not cancelled"
        ActivityStepResult activityStepResult=upgradeService.cancel(activityJobId)

        then:"Check if action is not cancelled"
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.EXECUTION_FAILED)
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains("Unable to trigger \"\" activity on the node. "))
    }
}
