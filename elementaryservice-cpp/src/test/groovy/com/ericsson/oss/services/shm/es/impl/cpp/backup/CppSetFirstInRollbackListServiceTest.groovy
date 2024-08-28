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

import com.ericsson.oss.services.shm.es.api.ActivityStepResult
import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.es.api.*
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants

class CppSetFirstInRollbackListServiceTest extends CppBackupTestDataProvider {

    @ObjectUnderTest
    private SetFirstInRollbackListService setFirstInRollbackListService

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

    def 'Test Precheck for SetFirstInRollbackList activity'() {

        given: "NetworkElement and the SetFirstInRollbackList details"
        activityJobId = loadJobProperties(nodeName, "setcvfirstinrollbacklist", cvMoFdn)
        buildTestDataForBackupAction("setcvfirstinrollbacklist", SUCCESS)
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP
        configurationVersionService.getCVMoFdn(nodeName) >> cvMoFdn
        configurationVersionService.getCVMoAttr(_) >> buildMoAttributeMap()
        networkElementRetrivalBean.getNeType(_) >> "ERBS"

        when: "Perform  precheck for the allocated job"
        ActivityStepResult activityStepResult = setFirstInRollbackListService.precheck(activityJobId)

        then: "Verify If Precheck action of SetFirstInRollbackList Activity Step Result data is as expected"

        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION)
    }

    def 'Test Precheck for SetFirstInRollbackList activity for Failure'() {

        given: "NetworkElement and the SetFirstInRollbackList details"
        activityJobId = loadJobProperties(nodeName, "setcvfirstinrollbacklist", cvMoFdn)
        buildTestDataForBackupAction("setcvfirstinrollbacklist", SUCCESS)
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP
        configurationVersionService.getCVMoFdn(nodeName) >> cvMoFdn

        when: "Perform  precheck for the allocated job"
        ActivityStepResult activityStepResult = setFirstInRollbackListService.precheck(activityJobId)

        then: "Verify If Precheck action of SetFirstInRollbackList Activity Step Result data is as expected"

        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION)
    }

    def 'Test Precheck for SetFirstInRollbackList activity for exception'() {

        given: "NetworkElement and the SetFirstInRollbackList details"
        activityJobId = loadJobProperties(nodeName, "setcvfirstinrollbacklist", cvMoFdn)
        buildTestDataForBackupAction("setcvfirstinrollbacklist", "JOB_NOT_FOUND_EXCEPTION")
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP
        configurationVersionService.getCVMoFdn(nodeName) >> cvMoFdn
        configurationVersionService.getCVMoAttr(_) >> buildMoAttributeMap()
        networkElementRetrivalBean.getNeType(_) >> "ERBS"

        when: "Perform  precheck for the allocated job"
        ActivityStepResult activityStepResult = setFirstInRollbackListService.precheck(activityJobId)

        then: "Verify If Precheck action of SetFirstInRollbackList Activity Step Result data is as expected"

        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION)
    }

    def 'Test execute is scuccess or failed for SetFirstInRollbackList activity'() {

        given: "NetworkElement and the SetFirstInRollbackList details"
        activityJobId = loadJobProperties(nodeName, "setcvfirstinrollbacklist", cvMoFdn)
        buildTestDataForBackupAction("setcvfirstinrollbacklist", SUCCESS)
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP
        configurationVersionService.getCVMoFdn(nodeName) >> cvMoFdn
        configurationVersionService.getCVMoAttr(_) >> buildMoAttributeMap()
        networkElementRetrivalBean.getNeType(_) >> "ERBS"
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> isValidTBAC

        when: "Perform  execute for the allocated job"
        setFirstInRollbackListService.execute(activityJobId)

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
        jobResult | isValidTBAC | log
        "SUCCESS" | true        | "Configuration Version TestCvBackup1 has been set first in rollback list."
        "FAILED"  | false       | "\"Create Configuration Version\" completed, notifying wfs has failed. Waiting until timeout."
    }

    def 'Test execute for SetFirstInRollbackList activity for exception'() {

        given: "NetworkElement and the SetFirstInRollbackList details"
        activityJobId = loadJobProperties(nodeName, "setcvfirstinrollbacklist", cvMoFdn)
        buildTestDataForBackupAction("setcvfirstinrollbacklist", scenario)
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP


        when: "Perform  execute for the allocated job"
        setFirstInRollbackListService.execute(activityJobId)

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
        null      | "JOB_NOT_FOUND_EXCEPTION" | "Unable to trigger \"Set first in rollback list\" activity on the node. Failure reason: \"JobDataNotFoundException occurred while executing..."
    }

    def 'Test execute for SetFirstInRollbackList activity  When NeJobStaticData is Null (businessKey=null)'() {

        given: "NetworkElement and the SetFirstInRollbackList details"
        activityJobId = loadJobProperties(nodeName, "setcvfirstinrollbacklist", cvMoFdn)
        buildTestDataForBackupAction("setcvfirstinrollbacklist", NeJobStaticData)
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP


        when: "Perform  execute for the allocated job"
        setFirstInRollbackListService.execute(activityJobId)

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
        jobResult | NeJobStaticData | log
        null      | "NULL"          | "Set First in the Rollback list activity has Failed"
    }


    def 'Test handleTimeout for SetFirstInRollbackList activity'() {

        given: "NetworkElement and the SetFirstInRollbackList details"
        activityJobId = loadJobProperties(nodeName, "setcvfirstinrollbacklist", cvMoFdn)
        buildTestDataForBackupAction("setcvfirstinrollbacklist", SUCCESS)
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP
        configurationVersionService.getCVMoFdn(nodeName) >> cvMoFdn
        configurationVersionService.getCVMoAttr(_) >> buildMoAttributeMap()
        networkElementRetrivalBean.getNeType(_) >> "ERBS"

        when: "Perform  handleTimeout for the allocated job"
        ActivityStepResult activityStepResult = setFirstInRollbackListService.handleTimeout(activityJobId)

        then: "Verify If handleTimeout action of SetFirstInRollbackList Activity Step Result data is as expected"

        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS)
    }

    def 'Test handleTimeout for SetFirstInRollbackList activity when exception occured'() {

        given: "NetworkElement and the SetFirstInRollbackList details"
        activityJobId = loadJobProperties(nodeName, "setcvfirstinrollbacklist", cvMoFdn)
        buildTestDataForBackupAction("setcvfirstinrollbacklist", scenario)
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP

        when: "Perform  handleTimeout for the allocated job"
        ActivityStepResult activityStepResult = setFirstInRollbackListService.handleTimeout(activityJobId)

        then: "Verify If handleTimeout action of SetFirstInRollbackList Activity Step Result data is as expected"

        activityStepResult.getActivityResultEnum().equals(activityResult)

        where:
        activityResult                             | scenario
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL | "JOB_NOT_FOUND_EXCEPTION"

    }

    def 'Test cancel for SetFirstInRollbackList activity'() {

        given: "NetworkElement and the SetFirstInRollbackList details"
        activityJobId = loadJobProperties(nodeName, "setcvfirstinrollbacklist", cvMoFdn)
        buildTestDataForBackupAction("setcvfirstinrollbacklist", SUCCESS)
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP

        when: "Perform  cancel for the allocated job"
        ActivityStepResult activityStepResult = setFirstInRollbackListService.cancel(activityJobId)

        then: "Verify If cancel action of SetFirstInRollbackList Activity Step Result data is as expected"

        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        null == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog = activityJob.getAttribute("lastLogMessage")
        if (lastLog != null) {
            assert (lastLog).contains("Node does not support cancellation of \"Set first in rollback list\" activity. Proceeding ahead for completion of the activity.")
        }
    }

    def 'Test cancel for SetFirstInRollbackList activity when exception occured'() {

        given: "NetworkElement and the SetFirstInRollbackList details"
        activityJobId = loadJobProperties(nodeName, "setcvfirstinrollbacklist", cvMoFdn)
        buildTestDataForBackupAction("setcvfirstinrollbacklist", scenario)
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP

        when: "Perform  cancel for the allocated job"
        ActivityStepResult activityStepResult = setFirstInRollbackListService.cancel(activityJobId)

        then: "Verify If cancel action of SetFirstInRollbackList Activity Step Result data is as expected"

        activityStepResult.getActivityResultEnum().equals(activityResult)

        where:
        activityResult | scenario
        null           | "JOB_NOT_FOUND_EXCEPTION"
        null           | "EXCEPTION"
    }

    def 'Test cancelTimeout for SetFirstInRollbackList activity'() {

        given: "NetworkElement and the SetFirstInRollbackList details"
        activityJobId = loadJobProperties(nodeName, "setcvfirstinrollbacklist", cvMoFdn)
        buildTestDataForBackupAction("setcvfirstinrollbacklist", SUCCESS)
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP
        configurationVersionService.getCVMoFdn(nodeName) >> cvMoFdn
        configurationVersionService.getCVMoAttr(_) >> buildMoAttributeMap()
        networkElementRetrivalBean.getNeType(_) >> "ERBS"

        when: "Perform  cancelTimeout for the allocated job"
        ActivityStepResult activityStepResult = setFirstInRollbackListService.cancelTimeout(activityJobId, true)

        then: "Verify If cancelTimeout action of SetFirstInRollbackList Activity Step Result data is as expected"

        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS)
    }

    def 'Test cancelTimeout for SetFirstInRollbackList activity when exception occured'() {

        given: "NetworkElement and the SetFirstInRollbackList details"
        activityJobId = loadJobProperties(nodeName, "setcvfirstinrollbacklist", cvMoFdn)
        buildTestDataForBackupAction("setcvfirstinrollbacklist", scenario)
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP

        when: "Perform  cancel for the allocated job"
        ActivityStepResult activityStepResult = setFirstInRollbackListService.cancelTimeout(activityJobId, true)

        then: "Verify If cancel action of SetFirstInRollbackList Activity Step Result data is as expected"

        activityStepResult.getActivityResultEnum().equals(activityResult)

        where:
        activityResult                             | scenario
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL | "JOB_NOT_FOUND_EXCEPTION"
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL | "EXCEPTION"

    }


    def 'Test asyncHandleTimeout for SetFirstInRollbackList activity'() {

        given: "NetworkElement and the SetFirstInRollbackList details"
        activityJobId = loadJobProperties(nodeName, "setcvfirstinrollbacklist", cvMoFdn)
        buildTestDataForBackupAction("setcvfirstinrollbacklist", scenario)
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP
        configurationVersionService.getCVMoFdn(nodeName) >> cvMoFdn
        configurationVersionService.getCVMoAttr(_) >> buildMoAttributeMap()
        networkElementRetrivalBean.getNeType(_) >> "ERBS"

        when: "Perform  asyncHandleTimeout for the allocated job"
        setFirstInRollbackListService.asyncHandleTimeout(activityJobId)

        then: "Verify If asyncHandleTimeout action of SetFirstInRollbackList Activity Step Result data is as expected"
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
        "SUCCESS" | "SUCCESS"                 | "Configuration Version TestCvBackup1 has been set as first CV in Rollback List."
        "FAILED"  | "JOB_NOT_FOUND_EXCEPTION" | "Database service is not accessible."

    }


    def 'Test timeoutForAsyncHandleTimeout for SetFirstInRollbackList activity'() {

        given: "NetworkElement and the SetFirstInRollbackList details"
        activityJobId = loadJobProperties(nodeName, "setcvfirstinrollbacklist", cvMoFdn)
        buildTestDataForBackupAction("setcvfirstinrollbacklist", SUCCESS)
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP
        configurationVersionService.getCVMoFdn(nodeName) >> cvMoFdn
        configurationVersionService.getCVMoAttr(_) >> buildMoAttributeMap()
        networkElementRetrivalBean.getNeType(_) >> "ERBS"
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> isValidTBAC

        when: "Perform  timeoutForAsyncHandleTimeout for the allocated job"
        setFirstInRollbackListService.timeoutForAsyncHandleTimeout(activityJobId)

        then: "Verify If timeoutForAsyncHandleTimeout action of SetFirstInRollbackList Activity Step Result data is as expected"
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
        jobResult | isValidTBAC | log
        "FAILED"  | true        | "Failed to get the \"Set first in rollback list\" activity status from the node within \"0\" minutes. Failing the Activity."

    }


}
