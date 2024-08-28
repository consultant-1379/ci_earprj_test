/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.ecim.backup

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.sdk.recording.CommandPhase
import com.ericsson.oss.itpf.sdk.recording.EventLevel
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.exception.BackupDataNotFoundException
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException
import com.ericsson.oss.services.shm.ecim.common.ActionResultType
import com.ericsson.oss.services.shm.ecim.common.ActionStateType
import com.ericsson.oss.services.shm.ecim.common.EcimCommonConstants.ReportProgress
import com.ericsson.oss.services.shm.es.api.ActivityStepResult
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum
import com.ericsson.oss.services.shm.model.event.based.mediation.ShmEcimMOActionMediationTaskRequest
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException


public class EcimUploadBackupServiceTest extends EcimBackupTestDataProvider {


    @ObjectUnderTest
    private UploadBackupService uploadBackupService;

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job.service")
    }

    def 'Get the Required Data for building MTR for MoAction and Send it to the Mediation Queue'() {

        given: "Create Required Data for UploadBackup MTR"
        buildDataToProcessMTRAttributes();

        when: "Invoking ProcessMTRData Service"
        uploadBackupService.prepareActionMTR(activityJobId, ecimBackupInfo, neJobStaticData, jobLogList, "");

        then: "Check if the data is kept in the queue"
        1 * eventSender.send(_ as ShmEcimMOActionMediationTaskRequest)
    }

    def 'Test Upload Backup Precheck for Success'() {

        given: "NetworkElement and Create Backup details"
        buildJobPO(UPLOAD_BACKUP);
        brmMoServiceRetryProxy.isBackupExist(_, _ as EcimBackupInfo) >> isBackupExistFlag

        when: "Perform  Upload Backup precheck for the allocated job "
        ActivityStepResult activityStepResult = uploadBackupService.precheck(activityJobId)

        then: "Verify If Activity Step Result data is Successful"
        activityJobId != null
        activityStepResult.getActivityResultEnum().equals(activityResult)

        where:
        activityResult                                            | isBackupExistFlag
        ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION | true
        ActivityStepResultEnum.REPEAT_PRECHECK                    | false
    }

    def 'Test Precheck for Upload Backup Job When BrmMoService.isBackupExist throwing Exception'() {

        given: "NetworkElement and Upload Backup details"
        buildJobPO(UPLOAD_BACKUP);
        brmMoServiceRetryProxy.isBackupExist(_, _ as EcimBackupInfo) >> throwExceptionSenario(exceptionSenario)


        when: "Perform  Upload Backup precheck for the allocated job"
        ActivityStepResult activityStepResult = uploadBackupService.precheck(activityJobId)

        then: "Verify If Activity Step Result data is Failed"
        activityStepResult.getActivityResultEnum().equals(activityResult)

        where:
        activityResult                                        | exceptionSenario
        ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION | "EXCEPTION"
        ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION | "JOB_NOT_FOUND_EXCEPTION"
    }

    def 'When Upload Backup action is triggered and action is successful'() {

        given: "NetworkElement and Create Backup details"
        buildJobPO(UPLOAD_BACKUP);
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true


        when: "Upload Backup Activity is triggered"
        uploadBackupService.execute(activityJobId)

        then: "Verify if Upload Backup Action is triggered Successfully"
        activityJobId != null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        null != activityJob.getAttribute(ShmConstants.LOG)
        assert (activityJob.getAttribute("lastLogMessage").contains(log))

        1 * systemRecorder.recordCommand('UPLOAD_BACKUP_SERVICE', CommandPhase.STARTED, _, _, _)
        1 * systemRecorder.recordCommand('UPLOAD_BACKUP_SERVICE', CommandPhase.FINISHED_WITH_SUCCESS, _, _, _)
        1 * systemRecorder.recordEvent('SHM.UPLOAD_BACKUP_EXECUTE ', _, _, _, _)
        1 * moActionCacheProvider.remove(activityJobId)

        where:
        jobResult | isActivityTriggered | log
        null      | "true"              | "\"Uploadbackup\" activity is triggered (timeout = \"0\" minutes)"
    }

    def 'When Upload Backup action is triggered and action is Failed due to exceptions'() {

        given: "NetworkElement and Create Backup details"
        buildJobPO(UPLOAD_BACKUP);
        switch (exceptionSenario) {
            case JOB_NOT_FOUND_EXCEPTION:
                activityJobTBACValidator.validateTBAC(_, _, _, _) >> {
                    throw new JobDataNotFoundException("JobDataNotFoundException occurred")
                }
                break;
            case BACKUP_DATA_NOT_FOUND:
                activityJobTBACValidator.validateTBAC(_, _, _, _) >> {
                    throw new BackupDataNotFoundException("BackupDataNotFoundException occurred")
                }
                break;
            case MO_EXCEPTION:
                activityJobTBACValidator.validateTBAC(_, _, _, _) >> {
                    throw new MoNotFoundException("MoNotFoundException occurred")
                }
                break;

            case EXCEPTION:
                activityJobTBACValidator.validateTBAC(_, _, _, _) >> { throw new Exception("Exception occurred") }
                break;
        }


        when: "Upload Backup Activity is triggered"
        uploadBackupService.execute(activityJobId)

        then: "Verify if Upload Backup Action is triggered  Failed due to exceptions"
        activityJobId != null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog = activityJob.getAttribute("lastLogMessage")
        if (lastLog != null) {
            assert (lastLog.contains(log))
        }
        numberOfInvocationsForRemoveMoActionCache * moActionCacheProvider.remove(activityJobId)
        where:
        jobResult | exceptionSenario          | log                                                       | numberOfInvocationsForRemoveMoActionCache
        null      | "BACKUP_DATA_NOT_FOUND"   | null                                                      | 0
        "FAILED"  | "MO_EXCEPTION"            | "Unable to trigger \"Uploadbackup\" activity on the node" | 1
        "FAILED"  | "JOB_NOT_FOUND_EXCEPTION" | "Database service is not accessible."                     | 1
        "FAILED"  | "EXCEPTION"               | null                                                      | 1
    }


    def 'When Upload Backup action is triggered and action is Failed due to exceptions on performActionOnNode '() {

        given: "NetworkElement and Create Backup details"
        buildJobPO(UPLOAD_BACKUP);
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true

        switch (exceptionSenario) {
            case UNSUPPORTED_FRAGEMENT_EXCEPTION:
                brmMoServiceRetryProxy.executeMoAction(_, _ as EcimBackupInfo, _, _) >> {
                    throw new UnsupportedFragmentException("UnsupportedFragmentException occurred")
                }
                break;
            case ARGUMENT_BUILDER_EXCEPTION:
                brmMoServiceRetryProxy.executeMoAction(_, _ as EcimBackupInfo, _, _) >> {
                    throw new ArgumentBuilderException("ArgumentBuilderException occurred")
                }
                break;
            case EXCEPTION:
                brmMoServiceRetryProxy.executeMoAction(_, _ as EcimBackupInfo, _, _) >> {
                    throw new Exception("Exception occurred")
                }
                break;
        }


        when: "Upload Backup Activity is triggered"
        uploadBackupService.execute(activityJobId)

        then: "Verify if Upload Backup Action is triggered  Failed due to exceptions"
        activityJobId != null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog = activityJob.getAttribute("lastLogMessage")
        if (lastLog != null) {
            assert (lastLog.contains("\"Uploadbackup\" activity has failed for backup \"CXP101-R501_\""))
        }
        1 * moActionCacheProvider.remove(activityJobId)

        where:
        jobResult | exceptionSenario                  | log
        "FAILED"  | "UNSUPPORTED_FRAGEMENT_EXCEPTION" | "\"Uploadbackup\" activity has failed for backup \"CXP101-R501_\""
        "FAILED"  | "EXCEPTION"                       | "\"Uploadbackup\" activity has failed for backup \"CXP101-R501_\""
        "FAILED"  | "ARGUMENT_BUILDER_EXCEPTION"      | "Database service is not accessible."
    }

    def 'When Notifications are received for Upload Backup Activity'() {

        given: "NetworkElement and Upload Backup details"
        buildJobPO(UPLOAD_BACKUP);
        brmMoServiceRetryProxy.getValidAsyncActionProgress(_, _, _ as Map) >> getAsyncActionProgress(actionName, resultType, state)

        when: "Upload Backup action has received notifications"
        uploadBackupService.processNotification(notification)

        then: "Check if Job is as expected through notifications"
        activityJobId != null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        assert (activityJob.getAttribute("lastLogMessage").contains(log))
        1 * moActionCacheProvider.remove(activityJobId)


        where:
        jobResult | log                                                                    | actionName                                   | resultType                          | state
        "SUCCESS" | "\"Uploadbackup\" activity is completed successfully."                 | EcimBackupConstants.BACKUP_EXPORT_ACTION_BSP | ActionResultType.SUCCESS.toString() | ActionStateType.FINISHED.toString()
        null      | "Action Name= \"export\" ProgressPercentage=\"10\" State= \"RUNNING\"" | EcimBackupConstants.BACKUP_EXPORT_ACTION_BSP | ActionResultType.SUCCESS.toString() | ActionStateType.RUNNING.toString()
        null      | "Cancellation is in progress with Percentage = \"10\""                 | EcimBackupConstants.BACKUP_EXPORT_ACTION_BSP | ActionResultType.FAILURE.toString() | ActionStateType.CANCELLING.toString()
        "FAILED"  | "\"Uploadbackup\" activity has failed."                                | EcimBackupConstants.BACKUP_EXPORT_ACTION_BSP | ActionResultType.FAILURE.toString() | ActionStateType.CANCELLED.toString()
        "SUCCESS" | "\"Uploadbackup\" activity is completed successfully."                 | EcimBackupConstants.BACKUP_CANCEL_ACTION     | ActionResultType.SUCCESS.toString() | ActionStateType.FINISHED.toString()
        "FAILED"  | "\"Uploadbackup\" activity has failed."                                | EcimBackupConstants.BACKUP_CANCEL_ACTION     | ActionResultType.SUCCESS.toString() | ActionStateType.RUNNING.toString()
        null      | "Cancellation is in progress with Percentage = \"10\""                 | EcimBackupConstants.BACKUP_EXPORT_ACTION_BSP | ActionResultType.FAILURE.toString() | ActionStateType.CANCELLING.toString()
    }


    def 'When Notifications are received for Upload Backup Activity and Job Failed due to exceptions'() {

        given: "NetworkElement and Upload Backup details"
        buildJobPO(UPLOAD_BACKUP)

        switch (exceptionSenario) {
            case JOB_NOT_FOUND_EXCEPTION:
                brmMoServiceRetryProxy.getValidAsyncActionProgress(_, _, _ as Map) >> {
                    throw new JobDataNotFoundException("JobDataNotFoundException occurred")
                }
                break;
            case MO_EXCEPTION:
                brmMoServiceRetryProxy.getValidAsyncActionProgress(_, _, _ as Map) >> {
                    throw new MoNotFoundException("MoNotFoundException occurred")
                }
                break;
            case UNSUPPORTED_FRAGEMENT_EXCEPTION:
                brmMoServiceRetryProxy.getValidAsyncActionProgress(_, _, _ as Map) >> {
                    throw new UnsupportedFragmentException("UnsupportedFragmentException occurred")
                }
                break;
            case EXCEPTION:
                brmMoServiceRetryProxy.getValidAsyncActionProgress(_, _, _ as Map) >> {
                    throw new Exception("Exception occurred")
                }
                break;
        }


        when: "Upload Backup action has received notifications"
        uploadBackupService.processNotification(notification)

        then: "Check if Job is Failed through notifications"
        activityJobId != null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog = activityJob.getAttribute("lastLogMessage")
        if (lastLog != null && log != null) {
            assert (lastLog.contains(log))
        }
        1 * moActionCacheProvider.remove(activityJobId)

        where:
        jobResult | exceptionSenario                  | log
        null      | "MO_EXCEPTION"                    | "Precheck for \"Backup File \"CXP101-R501_\" doesn't exist on node to upload.\" is failed. Reason: \"BrmBackup\" MO not found."
        null      | "JOB_NOT_FOUND_EXCEPTION"         | "Database service is not accessible."
        null      | "UNSUPPORTED_FRAGEMENT_EXCEPTION" | null
        null      | "EXCEPTION"                       | null
    }

    def 'When Notifications are not received for Upload Backup  activity and job went into handle timeout'() {

        given: "NetworkElement and Upload Backup details"
        buildJobPO(UPLOAD_BACKUP);
        brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(_, _, _ as EcimBackupInfo) >> progressReport
        def eventName = activityUtils.getActivityCompletionEvent(PlatformTypeEnum.ECIM, JobTypeEnum.BACKUP, EcimBackupConstants.UPLOAD_BACKUP)

        when: "Upload Backup Activity is triggered"
        ActivityStepResult activityStepResult = uploadBackupService.handleTimeout(activityJobId)

        then: "Check if Job is as expected through Handle Timeout"
        activityJobId != null
        activityStepResult.getActivityResultEnum().equals(activityResult)
        1 * systemRecorder.recordEvent(eventName, EventLevel.COARSE, _, _, _)

        where:
        activityResult                                | progressReport
        ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS | getAsyncActionProgress(EcimBackupConstants.BACKUP_CANCEL_ACTION, ActionResultType.SUCCESS.toString(), ActionStateType.FINISHED.toString())
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL    | null
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL    | getAsyncActionProgress(EcimBackupConstants.BACKUP_CANCEL_ACTION, ActionResultType.FAILURE.toString(), ActionStateType.FINISHED.toString())
    }


    def 'When Notifications are not received for Upload Backup  activity and job went into handle timeout and Job Failed'() {

        given: "NetworkElement and Upload Backup details"
        buildJobPO(UPLOAD_BACKUP)
        switch (exceptionSenario) {
            case JOB_NOT_FOUND_EXCEPTION:
                brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(_, _, _ as Map) >> {
                    throw new JobDataNotFoundException("JobDataNotFoundException occurred")
                }
                break;
            case MO_EXCEPTION:
                brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(_, _, _ as Map) >> {
                    throw new MoNotFoundException("MoNotFoundException occurred")
                }
                break;
            case UNSUPPORTED_FRAGEMENT_EXCEPTION:
                brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(_, _, _ as Map) >> {
                    throw new UnsupportedFragmentException("UnsupportedFragmentException occurred")
                }
                break;
            case EXCEPTION:
                brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(_, _, _ as Map) >> {
                    throw new Exception("Exception occurred")
                }
                break;
        }
        def eventName = activityUtils.getActivityCompletionEvent(PlatformTypeEnum.ECIM, JobTypeEnum.BACKUP, EcimBackupConstants.UPLOAD_BACKUP)

        when: "Upload Backup Activity is triggered"
        ActivityStepResult activityStepResult = uploadBackupService.handleTimeout(activityJobId)

        then: "Check if Job is Failed through Handle Timeout"
        activityJobId != null
        activityStepResult.getActivityResultEnum().equals(activityResult)
        1 * systemRecorder.recordEvent(eventName, EventLevel.COARSE, _, _, 'SHM:1:"Uploadbackup" activity has failed.; Flow : COMPLETED_THROUGH_TIMEOUT')

        where:
        activityResult                             | exceptionSenario
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL | "MO_EXCEPTION"
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL | "JOB_NOT_FOUND_EXCEPTION"
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL | "UNSUPPORTED_FRAGEMENT_EXCEPTION"
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL | "EXCEPTION"
    }

    def 'Test cancel for UploadBackup Activity'() {

        given: "NetworkElement and Upload Backup details"
        buildJobPO(senario);

        when: "cancel action of Upload Backup Activity is triggered"
        ActivityStepResult activityStepResult = uploadBackupService.cancel(activityJobId)

        then: "Check if Job is as expected"
        activityJobId != null
        activityStepResult.getActivityResultEnum().equals(activityResult)

        where:
        activityResult                          | senario
        ActivityStepResultEnum.EXECUTION_SUCESS | "UPLOAD_BACKUP"
        ActivityStepResultEnum.EXECUTION_FAILED | "MO_EXCEPTION"
        ActivityStepResultEnum.EXECUTION_FAILED | "EXCEPTION"
        ActivityStepResultEnum.EXECUTION_FAILED | "UNSUPPORTED_FRAGEMENT_EXCEPTION"
    }

    def 'Test cancelTimeout for UploadBackup Activity'() {

        given: "NetworkElement and Upload Backup details"
        buildJobPO(UPLOAD_BACKUP);
        brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(_, _, _ as EcimBackupInfo) >> progressReport

        when: "cancelTimeout of Upload Backup Activity is triggered"
        ActivityStepResult activityStepResult = uploadBackupService.cancelTimeout(activityJobId, true)

        then: "Check if Job is as expected"
        activityJobId != null
        activityStepResult.getActivityResultEnum().equals(activityResult)

        where:
        activityResult                                | progressReport
        ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS | getAsyncActionProgress(EcimBackupConstants.BACKUP_CANCEL_ACTION, ActionResultType.SUCCESS.toString(), ActionStateType.FINISHED.toString())
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL    | null
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL    | getAsyncActionProgress(EcimBackupConstants.BACKUP_CANCEL_ACTION, ActionResultType.FAILURE.toString(), ActionStateType.FINISHED.toString())
    }


    def 'Test cancelTimeout activitiy for Upload Backup Job'() {

        given: "NetworkElement and Upload Backup details"
        buildJobPO(UPLOAD_BACKUP);
        brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(_, _, _ as EcimBackupInfo) >> progressReport
        when: "Upload Backup Activity is triggered"
        ActivityStepResult activityStepResult = uploadBackupService.cancelTimeout(activityJobId, true)

        then: "Check if Job is as expected "
        activityJobId != null
        activityStepResult.getActivityResultEnum().equals(activityResult)

        where:
        activityResult                                | progressReport
        ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS | getAsyncActionProgress(EcimBackupConstants.BACKUP_CANCEL_ACTION, ActionResultType.SUCCESS.toString(), ActionStateType.FINISHED.toString())
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL    | null
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL    | getAsyncActionProgress(EcimBackupConstants.BACKUP_CANCEL_ACTION, ActionResultType.FAILURE.toString(), ActionStateType.FINISHED.toString())
    }


    def 'Test cancelTimeout activitiy for Upload Backup Job when exceptions occurred'() {

        given: "NetworkElement and Upload Backup details"
        buildJobPO(UPLOAD_BACKUP)

        switch (exceptionSenario) {
            case JOB_NOT_FOUND_EXCEPTION:
                brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(_, _, _ as EcimBackupInfo) >> {
                    throw new JobDataNotFoundException("JobDataNotFoundException occurred")
                }
                break;
            case MO_EXCEPTION:
                brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(_, _, _ as EcimBackupInfo) >> {
                    throw new MoNotFoundException("MoNotFoundException occurred")
                }
                break;
            case UNSUPPORTED_FRAGEMENT_EXCEPTION:
                brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(_, _, _ as EcimBackupInfo) >> {
                    throw new UnsupportedFragmentException("UnsupportedFragmentException occurred")
                }
                break;
        }

        when: "Upload Backup Activity is triggered"
        ActivityStepResult activityStepResult = uploadBackupService.cancelTimeout(activityJobId, true)

        then: "Check if Job is Failed"
        activityJobId != null
        activityStepResult.getActivityResultEnum().equals(activityResult)

        where:
        activityResult                             | exceptionSenario
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL | "MO_EXCEPTION"
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL | "JOB_NOT_FOUND_EXCEPTION"
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL | "UNSUPPORTED_FRAGEMENT_EXCEPTION"
    }

    def 'Test asyncHandleTimeout for Upload Backup Activity'() {

        given: "NetworkElement and Upload Backup details"
        buildJobPO(UPLOAD_BACKUP);
        brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(_, _, _ as EcimBackupInfo) >> progressReport
        def eventName = activityUtils.getActivityCompletionEvent(PlatformTypeEnum.ECIM, JobTypeEnum.BACKUP, EcimBackupConstants.UPLOAD_BACKUP)

        when: "asyncHandleTimeout for Upload Backup action is triggered"
        uploadBackupService.asyncHandleTimeout(activityJobId)

        then: "Check if asyncHandleTimeout Upload Backup is as expected"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog = activityJob.getAttribute("lastLogMessage")
        if (lastLog != null && log != null) {
            assert (lastLog.contains(log))
        }
        1 * systemRecorder.recordEvent(eventName, EventLevel.COARSE, nodeName, _, _)

        where:
        jobResult | progressReport                                                                                                                             | log
        "SUCCESS" | getAsyncActionProgress(EcimBackupConstants.BACKUP_CANCEL_ACTION, ActionResultType.SUCCESS.toString(), ActionStateType.FINISHED.toString()) | "\"Uploadbackup\" activity for the backup \"CXP101-R501_\" is completed successfully."
        "FAILED"  | null                                                                                                                                       | "\"Uploadbackup\" activity has failed."
        "FAILED"  | getAsyncActionProgress(EcimBackupConstants.BACKUP_CANCEL_ACTION, ActionResultType.FAILURE.toString(), ActionStateType.FINISHED.toString()) | "\"Uploadbackup\" activity has failed."
    }

    def 'Test asyncHandleTimeout for Upload Backup Activity for Exception senario'() {

        given: "NetworkElement and Upload Backup details"
        buildJobPO(UPLOAD_BACKUP);
        def eventName = activityUtils.getActivityCompletionEvent(PlatformTypeEnum.ECIM, JobTypeEnum.BACKUP, EcimBackupConstants.UPLOAD_BACKUP)

        switch (exceptionSenario) {

            case JOB_NOT_FOUND_EXCEPTION:
                brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(_, _, _ as EcimBackupInfo) >> {
                    throw new JobDataNotFoundException("JobDataNotFoundException occurred")
                }
                break;
            case BACKUP_DATA_NOT_FOUND:
                brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(_, _, _ as EcimBackupInfo) >> {
                    throw new BackupDataNotFoundException("BackupDataNotFoundException occurred")
                }
                break;
            case MO_EXCEPTION:
                brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(_, _, _ as EcimBackupInfo) >> {
                    throw new MoNotFoundException("MoNotFoundException occurred")
                }
                break;
            case UNSUPPORTED_FRAGEMENT_EXCEPTION:
                brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(_, _, _ as EcimBackupInfo) >> {
                    throw new UnsupportedFragmentException("UnsupportedFragmentException occurred")
                }
                break;
            case EXCEPTION:
                brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(_, _, _ as EcimBackupInfo) >> {
                    throw new Exception("Exception occurred")
                }
                break;
        }

        when: "asyncHandleTimeout for Upload Backup action is triggered"
        uploadBackupService.asyncHandleTimeout(activityJobId)

        then: "Check if asyncHandleTimeout Upload Backup is as expected"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog = activityJob.getAttribute("lastLogMessage")
        if (lastLog != null && log != null) {
            assert (lastLog.contains(log))
        }

        1 * systemRecorder.recordEvent('SHM.POLLING_UNSUBSCRIPTION_SUCCESS', _, 'Uploadbackup', 'LTE01dg2ERBS00002', 'ActivityJobId:1')
        1 * workflowInstanceNotifier.sendPrecheckOrTimeoutMsgToWfsInstance(_, _, 'timeoutForHandleTimeoutCompleted')
        1 * systemRecorder.recordEvent(eventName, EventLevel.COARSE, _, _, _)

        where:
        jobResult | exceptionSenario                  | log
        "FAILED"  | "MO_EXCEPTION"                    | "Backup File \"CXP101-R501_\" doesn't exist on node to upload."
        "FAILED"  | "JOB_NOT_FOUND_EXCEPTION"         | "Database service is not accessible."
        "FAILED"  | "UNSUPPORTED_FRAGEMENT_EXCEPTION" | "UnSupported Fragment for the node \"LTE01dg2ERBS00002\", Unable to proceed for action."
        "FAILED"  | "EXCEPTION"                       | "Failed to get the Uploadbackup activity status from the node."
    }

    def 'Test timeoutForAsyncHandleTimeout for Upload Backup Activity'() {

        given: "NetworkElement and Upload Backup details"
        buildJobPO(UPLOAD_BACKUP);

        when: "timeoutForAsyncHandleTimeout for Upload Backup action is triggered"
        uploadBackupService.timeoutForAsyncHandleTimeout(activityJobId)

        then: "Check if timeoutForAsyncHandleTimeout Upload Backup is as expected"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog = activityJob.getAttribute("lastLogMessage")
        if (lastLog != null && log != null) {
            assert (lastLog.contains(log))
        }

        where:
        jobResult | log
        "FAILED"  | "Failed to get the \"Uploadbackup\" activity status from the node within \"0\" minutes. Failing the Activity."
    }


    def 'Test subscribeForPolling for Upload Backup Jobs'() {

        given: "NetworkElement and Upload Backup details"
        buildJobPO(UPLOAD_BACKUP)

        when: "subscribeForPolling for Upload Backup"
        uploadBackupService.subscribeForPolling(activityJobId)

        then: "Check if subscribeForPolling for  Upload Backup success"
        1 * systemRecorder.recordEvent('SHM.POLLING_SUBSCRIPTION_SUCCESS', _, 'uploadbackup', 'BACKUP', 'SHM:1')
    }

    def 'Test subscribeForPolling for Upload Backup Jobs When is is Failed due to Runtime Exception '() {

        given: "NetworkElement and Upload Backup details"
        buildJobPO(RUNTIME_EXCEPTION);

        when: "subscribeForPolling for Upload Backup"
        uploadBackupService.subscribeForPolling(activityJobId)

        then: "Check if subscribeForPolling for  Upload Backup is Failed"
        0 * systemRecorder.recordEvent('SHM.POLLING_SUBSCRIPTION_SUCCESS', _, 'uploadbackup', 'BACKUP', 'SHM:1')
    }

    def 'Test processPollingResponse for Upload Backup Jobs'() {

        given: "NetworkElement and Upload Backup details"
        buildJobPO(UPLOAD_BACKUP);
        final Map<String, Object> responseAttributes = new HashMap<>()
        final Map<String, Object> moAttributes = new HashMap<>()
        moAttributes.put(ReportProgress.ASYNC_ACTION_PROGRESS, getReportProgress(actionName, ActionResultType.SUCCESS.toString(), state))
        responseAttributes.put(ShmConstants.MO_ATTRIBUTES, moAttributes)
        responseAttributes.put(ShmConstants.FDN, moFdn)
        when: "processPollingResponse for Upload Backup action is triggered"
        uploadBackupService.processPollingResponse(activityJobId, responseAttributes)

        then: "Check if subscribeForPolling for Upload Backup action is success "

        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties = activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        assert (activityJob.getAttribute("lastLogMessage").contains(log))

        1 * systemRecorder.recordEvent('SHM.ECIM.BACKUP.UPLOADBACKUP_COMPLETED', _, _, _, 'SHM:1:"Uploadbackup" activity is completed through Polling for CXP101-R501_; Flow : COMPLETED_THROUGH_POLLING')
        1 * systemRecorder.recordEvent('SHM.POLLING_UNSUBSCRIPTION_SUCCESS', _, 'Uploadbackup', _, _)
        1 * systemRecorder.recordCommand('UPLOAD_BACKUP_SERVICE', _, _, _, _)


        where:
        jobResult | state                               | actionName                               | log
        "SUCCESS" | ActionStateType.FINISHED.toString() | EcimBackupConstants.BACKUP_CANCEL_ACTION | "\"Uploadbackup\" activity is completed successfully."
        "FAILED"  | ActionStateType.RUNNING.toString()  | EcimBackupConstants.BACKUP_CANCEL_ACTION | "\"Uploadbackup\" activity has failed."
    }
}
