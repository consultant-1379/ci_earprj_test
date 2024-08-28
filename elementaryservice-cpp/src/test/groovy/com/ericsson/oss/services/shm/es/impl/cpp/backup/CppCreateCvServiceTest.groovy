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
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.es.api.*
import com.ericsson.oss.services.shm.es.impl.ActivityUtils
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.job.cache.JobStaticData
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData



public class CppCreateCvServiceTest extends CppBackupTestDataProvider {

    @ObjectUnderTest
    CreateCvService createCvService;


    @MockedImplementation
    JobStaticDataProvider jobStaticDataProvider

    @MockedImplementation
    JobStaticData jobStaticData

    private static final String jobExecutionUser = "Test_user"

    def NEJobStaticData neJobStaticData = new NEJobStaticData(neJobId, mainJobId, nodeName, businessKey, String.valueOf(PlatformTypeEnum.CPP), (new Date()).getTime(), "LTE17");
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


    def 'Test Precheck for CreateCV activity for success.'() {

        given: "NetworkElement and the createcv details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, SUCCESS)
        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP
        configurationVersionService.getCVMoFdn(nodeName) >> cvMoFdn
        jobStaticDataProvider.getJobStaticData(neJobStaticData.mainJobId) >> jobStaticData
        jobStaticData.getJobExecutionUser() >> jobExecutionUser
       
        when: "Perform CreateCV precheck for the allocated job"
        final ActivityStepResult activityStepResult = createCvService.precheck(activityJobId)

        then: "Verify If Precheck action of CreateCv Activity Step Result data is success"

        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION)
        1 * ActivityUtils.recordEvent(jobExecutionUser,'SHM.CREATE_BACKUP_PRECHECK "', _, _, _, _)
    }

    def 'Test Precheck for CreateCV activity when exception occurred'() {

        given: "NetworkElement and the createcv details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true
        if (scenario.equals("JOB_NOT_FOUND_EXCEPTION")) {
            buildTestDataForBackupAction(activityName, JOB_NOT_FOUND_EXCEPTION)
            configurationVersionService.getCVMoFdn(nodeName) >> cvMoFdn
        } else {
            buildTestDataForBackupAction(activityName, SUCCESS)
            configurationVersionService.getCVMoFdn(nodeName) >> null
        }

        platformTypeProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.CPP

        when: "Perform  precheck for the allocated job"
        ActivityStepResult activityStepResult = createCvService.precheck(activityJobId)

        then: "Verify If Activity Step Result data is Failed"

        activityStepResult.getActivityResultEnum().equals(activityResult)

        where:
        activityResult                                        | scenario
        ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION | "JOB_NOT_FOUND_EXCEPTION"
        ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION | "NUll"

    }

    def 'When CreateCV activity is triggered and action is successful or Failed'() {

        given: "NetworkElement and CreateCV details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, SUCCESS)
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true
        networkElementRetrivalBean.getNeType(_) >> "ERBS"
        if (isValidCvMoFdn) {
            configurationVersionService.getCVMoFdn(nodeName) >> cvMoFdn
        } else {
            configurationVersionService.getCVMoFdn(nodeName) >> null
        }
        jobStaticDataProvider.getJobStaticData(neJobStaticData.mainJobId) >> jobStaticData
        jobStaticData.getJobExecutionUser() >> jobExecutionUser
      

        when: "CreateCV activity is triggered"
        createCvService.execute(activityJobId)

        then: "Verify if CreateCV activity is triggered Successful or Failed"
        activityJobId != null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        activityJob.getAttribute(ShmConstants.LOG) != null
        activityJob.getAttribute(ShmConstants.JOBPROPERTIES) != null
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        null != activityJob.getAttribute(ShmConstants.LOG)
        assert (activityJob.getAttribute("lastLogMessage").contains(log))
        1 * workflowInstanceNotifier.sendActivate('Some Business Key', _)
        1 * ActivityUtils.recordEvent(jobExecutionUser,'SHM.CREATE_BACKUP_EXECUTE "', _, _, _, _)

        where:
        jobResult | isValidCvMoFdn | log
        "SUCCESS" | true           | "Configuration Version \"TestCvBackup1\" is created successfully."
        "FAILED"  | false          | "Precheck for \"Create Configuration Version\" is failed. Reason: \"ConfigurationVersion\" MO not found."
    }

    def 'Test excute of  CreateCV activity When TBAC is Success or Failed'() {

        given: "NetworkElement and CreateCV details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, SUCCESS)

        networkElementRetrivalBean.getNeType(_) >> "ERBS"
        configurationVersionService.getCVMoFdn(nodeName) >> cvMoFdn
        if (isValidTBAC) {
            activityJobTBACValidator.validateTBAC(_, _, _, _) >> true
        } else {
            activityJobTBACValidator.validateTBAC(_, _, _, _) >> false
        }

        jobStaticDataProvider.getJobStaticData(neJobStaticData.mainJobId) >> jobStaticData
        jobStaticData.getJobExecutionUser() >> jobExecutionUser

        when: "CreateCV activity is triggered"
        createCvService.execute(activityJobId)

        then: "Verify if CreateCV activity is triggered Successful or Failed"
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
        "SUCCESS" | true        | "Configuration Version \"TestCvBackup1\" is created successfully."
        "FAILED"  | false       | "Precheck for \"Create Configuration Version\" is failed. Reason: \"ConfigurationVersion\" MO not found."
    }

    def 'Test execute of CreateCV activity When Exceptions occurred'() {

        given: "NetworkElement and CreateCV details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, scenario)
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true
        networkElementRetrivalBean.getNeType(_) >> "ERBS"

        when: "CreateCV activity is triggered"
        createCvService.execute(activityJobId)

        then: "Verify if CreateCV activity is Failed"
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
        null      | "JOB_NOT_FOUND_EXCEPTION" | "Unable to trigger \"Create Configuration Version\" activity on the node. Failure reason: \"JobDataNotFoundException occurred while executing...\""
        null      | "EXCEPTION"               | "Create Configuration Version execute failed.Failure reason: \"java.lang.Exception: Exception occurred while executing...\""
    }

    def 'Test execute of CreateCV activity When NeJobStaticData is Null (businessKey=null)'() {

        given: "NetworkElement and CreateCV details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, NeJobStaticData)
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true
        networkElementRetrivalBean.getNeType(_) >> "ERBS"

        when: "CreateCV activity is triggered"
        createCvService.execute(activityJobId)

        then: "Verify if CreateCV activity is Failed"
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
        null      | "NULL"          | "Create Configuration Version execute failed.Failure reason: \"\""
    }


    def 'When Notifications are not received for CreateCV activity and job went into handle timeout and it is Success'() {

        given: "NetworkElement and the CreateCV activity details"
        final Map<String, Object> moAttributesMap = new HashMap<>()
        moAttributesMap.put(ShmConstants.FDN, cvMoFdn)
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, SUCCESS)
        configurationVersionService.getCVMoFdn(nodeName) >> cvMoFdn
        configurationVersionService.getCVMoAttr(_) >> buildMoAttributeMap()

        when: "CreateCV Activity is triggered"
        ActivityStepResult activityStepResult = createCvService.handleTimeout(activityJobId)

        then: "Check if Job is successful through handleTimeout"

        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS)
    }

    def 'When Notifications are not received for CreateCV activity and job went into handle timeout and it is throwing Exception'() {

        given: "NetworkElement and the CreateCV activity details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, scenario)
        configurationVersionService.getCVMoFdn(nodeName) >> cvMoFdn

        when: "CreateCV Activity has received notifications"
        ActivityStepResult activityStepResult = createCvService.handleTimeout(activityJobId)

        then: "Check if Job is failing through handleTimeout"

        activityStepResult.getActivityResultEnum().equals(activityResult)

        where:
        activityResult                             | scenario
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL | "JOB_NOT_FOUND_EXCEPTION"
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL | "EXCEPTION"
    }

    def 'When CreateCV action is triggered and action is cancelled and cancel action is successful'() {

        given: "NetworkElement and the CreateCV activity details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, SUCCESS)
        configurationVersionService.getCVMoFdn(nodeName) >> cvMoFdn

        when: "CreateCV action is triggered and action is cancelled"
        ActivityStepResult activityStepResult = createCvService.cancel(activityJobId)

        then: "Check if cancel action is success"

        activityStepResult.getActivityResultEnum() == null
    }

    def 'When CreateCV action is triggered and action is cancelled and cancel action is Failed'() {

        given: "NetworkElement and the CreateCV activity details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, scenario)
        configurationVersionService.getCVMoFdn(nodeName) >> cvMoFdn

        when: "Check if CreateCV is not cancelled"
        ActivityStepResult activityStepResult = createCvService.cancel(activityJobId)

        then: "Check if cancel action is Failed"

        activityStepResult.getActivityResultEnum().equals(activityResult)

        where:
        activityResult | scenario
        null           | "JOB_NOT_FOUND_EXCEPTION"
        null           | "EXCEPTION"
    }

    def 'Test cancelTimeout action for CreateCV Activity and Job is successful'() {

        given: "NetworkElement and the CreateCV activity details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, SUCCESS)
        configurationVersionService.getCVMoFdn(nodeName) >> cvMoFdn
        configurationVersionService.getCVMoAttr(_) >> buildMoAttributeMap()

        when: "CreateCV Activity has received notifications"
        ActivityStepResult activityStepResult = createCvService.cancelTimeout(activityJobId, true)

        then: "Check if cancelTimeout action is Success"

        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS)
    }

    def 'Test cancelTimeout action for CreateCV Activity and Job is Failed'() {

        given: "NetworkElement and the CreateCV activity details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, scenario)
        configurationVersionService.getCVMoFdn(nodeName) >> cvMoFdn
        configurationVersionService.getCVMoAttr(_) >> buildMoAttributeMap()

        when: "CreateCV Activity has received notifications"
        ActivityStepResult activityStepResult = createCvService.cancelTimeout(activityJobId, true)

        then: "Check if cancelTimeout action is Failed"

        activityStepResult.getActivityResultEnum().equals(activityResult)

        where:
        activityResult                             | scenario
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL | "JOB_NOT_FOUND_EXCEPTION"
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL | "EXCEPTION"
    }

    def 'Test asyncHandleTimeout of CreateCV activity For Success scenario'() {

        given: "NetworkElement and CreateCV details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, scenario)
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true
        networkElementRetrivalBean.getNeType(_) >> "ERBS"
        configurationVersionService.getCVMoFdn(nodeName) >> cvMoFdn
        configurationVersionService.getCVMoAttr(_) >> buildMoAttributeMap()
        jobStaticDataProvider.getJobStaticData(neJobStaticData.mainJobId) >> jobStaticData
        jobStaticData.getJobExecutionUser() >> jobExecutionUser
        
        when: "asyncHandleTimeout action of CreateCV activity is triggered"
        createCvService.asyncHandleTimeout(activityJobId)

        then: "Verify if asyncHandleTimeout action of CreateCV activity is Success"
        activityJobId != null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        def lastLog = activityJob.getAttribute("lastLogMessage")
        if (lastLog != null) {
            assert (lastLog).contains(log)
        }

        where:
        scenario  | log
        "SUCCESS" | "Configuration Version \"TestCvBackup1\" is created successfully."

    }


    def 'Test asyncHandleTimeout of  CreateCV activity For JOB_NOT_FOUND_EXCEPTION scenario'() {

        given: "NetworkElement and CreateCV details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, scenario)
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true
        networkElementRetrivalBean.getNeType(_) >> "ERBS"
        configurationVersionService.getCVMoFdn(nodeName) >> cvMoFdn
        configurationVersionService.getCVMoAttr(_) >> buildMoAttributeMap()
        jobStaticDataProvider.getJobStaticData(neJobStaticData.mainJobId) >> jobStaticData
        jobStaticData.getJobExecutionUser() >> jobExecutionUser
       
        when: "asyncHandleTimeout action of CreateCV activity is triggered"
        createCvService.asyncHandleTimeout(activityJobId)

        then: "Verify if asyncHandleTimeout action of CreateCV activity is Failed"
        activityJobId != null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        def lastLog = activityJob.getAttribute("lastLogMessage")
        if (lastLog != null) {
            assert (lastLog).contains(log)
        }
        where:
        scenario                  | log
        "JOB_NOT_FOUND_EXCEPTION" | "Database service is not accessible."

    }

    def 'Test asyncHandleTimeout of CreateCV activity For Failure'() {

        given: "NetworkElement and CreateCV details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, scenario)
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true
        networkElementRetrivalBean.getNeType(_) >> "ERBS"
        configurationVersionService.getCVMoFdn(nodeName) >> cvMoFdn
        configurationVersionService.getCVMoAttr(_) >> buildMoAttributeMapForFailure()

        when: "asyncHandleTimeout action of CreateCV activity is triggered"
        createCvService.asyncHandleTimeout(activityJobId)

        then: "Verify if asyncHandleTimeout action of CreateCV activity is Failed"
        activityJobId != null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        def lastLog = activityJob.getAttribute("message")
        if (lastLog != null) {
            assert (lastLog).contains(log)
        }

        where:
        scenario | log
        "FAILED" | "Unable to Create Configuration Version"

    }

    def 'Test timeoutForAsyncHandleTimeout action for CreateCV activity failure'() {

        given: "NetworkElement and the CreateCV activity details"
        activityJobId = loadJobProperties(nodeName, activityName, cvMoFdn)
        buildTestDataForBackupAction(activityName, SUCCESS)
        configurationVersionService.getCVMoFdn(nodeName) >> cvMoFdn

        when: "CreateCV Activity action has received notifications"
        createCvService.timeoutForAsyncHandleTimeout(activityJobId)

        then: "Check if timeoutForAsyncHandleTimeout action is Failed"
        activityJobId != null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        "FAILED" == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog = activityJob.getAttribute("lastLogMessage")
        if (lastLog != null) {
            assert (lastLog).contains("Failed to get the \"Create Configuration Version\" activity status from the node within \"0\" minutes. Failing the Activity.")
        }
    }
}
