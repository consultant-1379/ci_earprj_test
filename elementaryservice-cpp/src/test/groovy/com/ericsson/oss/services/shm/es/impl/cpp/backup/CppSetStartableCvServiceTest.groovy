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

package com.ericsson.oss.services.shm.es.impl.cpp.backup

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.es.api.ActivityStepResult
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants

class CppSetStartableCvServiceTest extends CppBackupTestDataProvider {

    @ObjectUnderTest
    SetStartableCvService setStartableCvService;


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

    def 'Test Precheck for SetStartableCv activity'() {

        given: "NetworkElement and the createcv details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, SUCCESS)
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP
        configurationVersionService.getCVMoAttr(_) >> buildMoAttributeMap()

        when: "Perform  precheck for the allocated job"
        ActivityStepResult activityStepResult = setStartableCvService.precheck(activityJobId)

        then: "Verify If Precheck action of SetStartableCv Activity Step Result data is as expected"

        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION)

    }

    def 'Test Precheck for SetStartableCv activity for exception'() {

        given: "NetworkElement and the createcv details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, JOB_NOT_FOUND_EXCEPTION)
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP
        configurationVersionService.getCVMoAttr(_) >> buildMoAttributeMap()

        when: "Perform  precheck for the allocated job"
        ActivityStepResult activityStepResult = setStartableCvService.precheck(activityJobId)

        then: "Verify If Precheck action of SetStartableCv Activity is Failed"

        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION)

    }

    def 'Test execute of  SetStartableCv activity When TBAC is Success or Failed'() {

        given: "NetworkElement and SetStartableCv details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, SUCCESS)
        configurationVersionService.getCVMoAttr(_) >> buildMoAttributeMap()
        networkElementRetrivalBean.getNeType(_) >> "ERBS"
        configurationVersionService.getCVMoFdn(nodeName) >> cvMoFdn
        if (isValidTBAC) {
            activityJobTBACValidator.validateTBAC(_, _, _, _) >> true
        } else {
            activityJobTBACValidator.validateTBAC(_, _, _, _) >> false
        }

        when: "SetStartableCv activity is triggered"
        setStartableCvService.execute(activityJobId)
        configurationVersionService.getCVMoAttr(_) >> buildMoAttributeMap()

        then: "Verify if SetStartableCv activity is triggered Successful or Failed"
        activityJobId != null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog = activityJob.getAttribute("lastLogMessage")
        if (lastLog != null) {
            assert (lastLog).contains(log)
        }
        1 * workflowInstanceNotifier.sendActivate('Some Business Key', _)

        where:
        jobResult | isValidTBAC | log
        "SUCCESS" | true        | "Configuration Version \"TestCvBackup1\" has been set as a Startable CV Successfully."
        "FAILED"  | false       | "Precheck for \"Create Configuration Version\" is failed. Reason: \"ConfigurationVersion\" MO not found."
    }

    def 'Test execute for SetStartableCv activity for JobDataNotFoundException'() {

        given: "NetworkElement and the SetFirstInRollbackList details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, scenario)
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP

        when: "Perform  execute for the allocated job"
        setStartableCvService.execute(activityJobId)

        then: "Verify If execute action of SetFirstInRollbackList Activity Step Result data is as expected"
        activityJobId != null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)


        def lastLog = activityJob.getAttribute("lastLogMessage")
        if (lastLog != null) {
            assert (lastLog).contains(log)
        }

        where:
        jobResult | scenario                  | log
        null      | "JOB_NOT_FOUND_EXCEPTION" | "Unable to trigger \"Set as Startable CV\" activity on the node. Failure reason: \"JobDataNotFoundException occurred while executing...\""
    }

    def 'Test execute for SetStartableCv activity When NeJobStaticData is Null (businessKey=null)'() {

        given: "NetworkElement and the SetFirstInRollbackList details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, NeJobStaticData)
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP

        when: "Perform  execute for the allocated job"
        setStartableCvService.execute(activityJobId)

        then: "Verify If execute action of SetFirstInRollbackList Activity Step Result data is as expected"
        activityJobId != null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)


        def lastLog = activityJob.getAttribute("lastLogMessage")
        if (lastLog != null) {
            assert (lastLog).contains(log)
        }

        where:
        jobResult | NeJobStaticData                  | log
        null      | "NULL" | "Set as startable action has failed for configuration version \"null\".Failure reason: \"null\""
    }


    def 'Test handleTimeout for SetStartableCv activity'() {

        given: "NetworkElement and the createcv details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, SUCCESS)
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP
        configurationVersionService.getCVMoAttr(_) >> buildMoAttributeMap()

        when: "Perform  handleTimeout for the allocated job"
        ActivityStepResult activityStepResult = setStartableCvService.handleTimeout(activityJobId)

        then: "Verify If handleTimeout action of SetStartableCv Activity Step Result data is as expected"

        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS)

    }

    def 'Test handleTimeout for SetStartableCv activity for exception'() {

        given: "NetworkElement and the createcv details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, JOB_NOT_FOUND_EXCEPTION)
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP
        configurationVersionService.getCVMoAttr(_) >> buildMoAttributeMap()

        when: "Perform  handleTimeout for the allocated job"
        ActivityStepResult activityStepResult = setStartableCvService.handleTimeout(activityJobId)

        then: "Verify If handleTimeout action of SetStartableCv Activity is Failed"

        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL)

    }

    def 'Test cancel for SetStartableCv activity'() {

        given: "NetworkElement and the createcv details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, SUCCESS)
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP
        configurationVersionService.getCVMoAttr(_) >> buildMoAttributeMap()

        when: "Perform  handleTimeout for the allocated job"
        ActivityStepResult activityStepResult = setStartableCvService.handleTimeout(activityJobId)

        then: "Verify If handleTimeout action of SetStartableCv Activity Step Result data is as expected"

        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS)

    }

    def 'Test cancel for SetStartableCv activity for exception'() {

        given: "NetworkElement and the createcv details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, JOB_NOT_FOUND_EXCEPTION)
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP
        configurationVersionService.getCVMoAttr(_) >> buildMoAttributeMap()

        when: "Perform  handleTimeout for the allocated job"
        ActivityStepResult activityStepResult = setStartableCvService.cancel(activityJobId)

        then: "Verify If handleTimeout action of SetStartableCv Activity is Failed"

        activityStepResult.getActivityResultEnum() == null

    }

    def 'Test cancelTimeout success for SetStartableCv activity'() {

        given: "NetworkElement and the createcv details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, SUCCESS)
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP
        configurationVersionService.getCVMoAttr(_) >> buildMoAttributeMap()

        when: "Perform  cancelTimeout for the allocated job"
        ActivityStepResult activityStepResult = setStartableCvService.cancelTimeout(activityJobId, true)

        then: "Verify If cancelTimeout action of SetStartableCv Activity Step Result data is as expected"

        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS)

    }

    def 'Test cancelTimeout failure for SetStartableCv activity'() {

        given: "NetworkElement and the createcv details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, "JOB_NOT_FOUND_EXCEPTION")
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP
        configurationVersionService.getCVMoAttr(_) >> buildMoAttributeMap()

        when: "Perform  cancelTimeout for the allocated job"
        ActivityStepResult activityStepResult = setStartableCvService.cancelTimeout(activityJobId, true)

        then: "Verify If cancelTimeout action of SetStartableCv Activity Step Result data is as expected"

        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL)

    }


    def 'Test asyncHandleTimeout success for SetStartableCv activity'() {

        given: "NetworkElement and the createcv details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, SUCCESS)
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP
        configurationVersionService.getCVMoAttr(_) >> buildMoAttributeMap()

        when: "Perform  asyncHandleTimeout for the allocated job"
        setStartableCvService.asyncHandleTimeout(activityJobId)

        then: "Verify If asyncHandleTimeout action of SetStartableCv Activity Step Result data is as expected"
        activityJobId != null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        "SUCCESS" == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog = activityJob.getAttribute("lastLogMessage")
        if (lastLog != null) {
            assert (lastLog).contains("Success")
        }
    }

    def 'Test asyncHandleTimeout failure for SetStartableCv activity'() {

        given: "NetworkElement and the createcv details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, "JOB_NOT_FOUND_EXCEPTION")
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP
        configurationVersionService.getCVMoAttr(_) >> buildMoAttributeMap()

        when: "Perform  asyncHandleTimeout for the allocated job"
        setStartableCvService.asyncHandleTimeout(activityJobId)

        then: "Verify If asyncHandleTimeout action of SetStartableCv Activity Step Result data is as expected"
        activityJobId != null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        "FAILED" == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog = activityJob.getAttribute("lastLogMessage")
        if (lastLog != null) {
            assert (lastLog).contains("Database service is not accessible.")
        }

    }

    def 'Test timeoutForAsyncHandleTimeout for SetStartableCv activity'() {

        given: "NetworkElement and the createcv details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, SUCCESS)
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP
        configurationVersionService.getCVMoAttr(_) >> buildMoAttributeMap()

        when: "Perform  timeoutForAsyncHandleTimeout for the allocated job"
        setStartableCvService.timeoutForAsyncHandleTimeout(activityJobId)

        then: "Verify If timeoutForAsyncHandleTimeout action of SetStartableCv Activity Step Result data is as expected"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        "FAILED" == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog = activityJob.getAttribute("lastLogMessage")
        if (lastLog != null) {
            assert (lastLog).contains("Failed to get the \"Set as Startable CV\" activity status from the node within \"0\" minutes. Failing the Activity.")
        }

    }

}
