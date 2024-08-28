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

package com.ericsson.oss.services.shm.es.impl.ecim.backup

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.itpf.sdk.recording.CommandPhase
import com.ericsson.oss.services.shm.common.exception.BackupDataNotFoundException
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException
import com.ericsson.oss.services.shm.ecim.common.ActionResultType
import com.ericsson.oss.services.shm.ecim.common.ActionStateType
import com.ericsson.oss.services.shm.ecim.common.EcimCommonConstants.ReportProgress
import com.ericsson.oss.services.shm.es.api.ActivityStepResult
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException

public class EcimCreateBackupServiceTest extends EcimBackupTestDataProvider {

    @ObjectUnderTest
    CreateBackupService createBackupService;
    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps)

    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
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

    def 'Test Create Backup Precheck for Success'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(SUCCESS);
        brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName) >> "3.4.1"

        when:"Perform  Create Backup precheck for the allocated job "
        ActivityStepResult activityStepResult=createBackupService.precheck(activityJobId)

        then:"Verify If Activity Step Result data is Successful"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION)
    }

    def 'Test Create Backup Precheck for Fail cases'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(EXCEPTION);
        brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName) >> "3.4.1"

        when:"Perform  Create Backup precheck for the allocated job "
        ActivityStepResult activityStepResult=createBackupService.precheck(activityJobId)

        then:"Verify If Activity Step Result data is Failed"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION)
    }

    def 'Test Create Backup Precheck Failied due to JobDataNotFoundException'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(JOB_NOT_FOUND_EXCEPTION);
        brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName) >> "3.4.1"

        when:"Perform  Create Backup precheck for the allocated job "
        ActivityStepResult activityStepResult=createBackupService.precheck(activityJobId)

        then:"Verify If Activity Step Result data is Failed due to JobDataNotFoundException"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION)
    }

    def 'Test Create Backup Precheck Failied due to  Some Exception'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(EXCEPTION);
        brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName) >> "3.4.1"

        when:"Perform  Create Backup precheck for the allocated job "
        ActivityStepResult activityStepResult=createBackupService.precheck(activityJobId)

        then:"Verify If Activity Step Result data is Failed due to  Some Exception"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION)
    }

    def 'Test Precheck of Create Backup for getBrmFragmentVersion'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(SUCCESS);
        switch(exceptionSenario) {

            case MO_EXCEPTION:
                brmMoServiceRetryProxy.getBrmFragmentVersion(_) >> { throw new MoNotFoundException("MoNotFoundException occurred") }
                break;
            case UNSUPPORTED_FRAGEMENT_EXCEPTION:
                brmMoServiceRetryProxy.getBrmFragmentVersion(_) >> { throw new UnsupportedFragmentException("UnsupportedFragmentException occurred") }
        }

        when:"Perform  Create Backup precheck for the allocated job "
        ActivityStepResult activityStepResult=createBackupService.precheck(activityJobId)

        then:"Verify If Activity Step Result data is as expected"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(activityResult)

        where:
        activityResult                                            |exceptionSenario
        ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION |"UNSUPPORTED_FRAGEMENT_EXCEPTION"
        ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION |"MO_EXCEPTION"
    }

    def 'When Create Backup action is triggered and action is successful'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(SUCCESS);
        brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName) >> "3.4.1"
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true

        when:"Create Backup Activity is triggered"
        createBackupService.execute(activityJobId)

        then:"Verify if Create Backup Action is triggered Successfully"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        assert(activityJob.getAttribute("lastLogMessage").contains(log))

        1 * brmMoServiceRetryProxy.executeMoAction(_, _,_, _)
        1 * systemRecorder.recordCommand('SHM.CREATE_BACKUP_EXECUTE ', CommandPhase.FINISHED_WITH_SUCCESS, _, _, _)

        where:
        jobResult|isActivityTriggered | log
        null     |    "true"          |"\"Createbackup\" activity is triggered (timeout = \"0\" minutes)"
    }

    def 'When Create Backup action is triggered and action is successful with PostValidation'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(SUCCESS);
        brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName) >> "3.4.1"
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true

        when:"Create Backup Activity is triggered"
        createBackupService.execute(activityJobId)

        then:"Verify if Create Backup Action is triggered Successfully"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        assert(activityJob.getAttribute("lastLogMessage").contains(log))
        1 * systemRecorder.recordEvent(_, _, _, _, _)

        where:
        jobResult|isActivityTriggered | log
        null     |    "true"          |"\"Createbackup\" activity is triggered (timeout = \"0\" minutes)"
    }

    def 'When Create Backup action is triggered and action is Failed due to  Some Exception'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(FAIL);
        brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName) >> "3.4.1"
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true

        when:"Create Backup Activity is triggered"
        createBackupService.execute(activityJobId)

        then:"Verify if Create Backup Action is is Failed due to some Exception"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
    }

    def 'When Create Backup action is triggered and action is Failed due to  Some Exception(getPrecheckResponse)'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(EXCEPTION);
        brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName) >> "3.4.1"
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true

        when:"Create Backup Activity is triggered"
        createBackupService.execute(activityJobId)

        then:"Verify if Create Backup Action is is Failed due to some Exception"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        assert(activityJob.getAttribute("lastLogMessage").contains(log))

        1 * systemRecorder.recordEvent(_, _, _, _, _)
        1 * workflowInstanceNotifier.sendActivate(_, _)

        where:
        jobResult|log
        "FAILED" |"Unable to proceed \"Createbackup\" activity"
    }

    def 'When Create Backup action is triggered and action is Failed due to  JobDataNotFoundException'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(JOB_NOT_FOUND_EXCEPTION);
        brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName) >> "3.4.1"
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true

        when:"Create Backup Activity is triggered"
        createBackupService.execute(activityJobId)

        then:"Verify if Create Backup Action is is Failed due to JobDataNotFoundException"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        null !=activityJob.getAttribute(ShmConstants.LOG)
        assert(activityJob.getAttribute("lastLogMessage").contains(log))

        where:
        jobResult| log
        null     | "Database service is not accessible."
    }

    def 'When Create Backup action is triggered and action is Failed due to  BackupDataNotFoundException'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(EXCEPTION);
        brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName) >> "3.4.1"
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true

        when:"Create Backup Activity is triggered"
        createBackupService.execute(activityJobId)

        then:"Verify if Create Backup Action is is Failed due to BackupDataNotFoundException"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        null !=activityJob.getAttribute(ShmConstants.LOG)
        assert(activityJob.getAttribute("lastLogMessage").contains(log))

        where:
        jobResult|log
        "FAILED"|"Unable to proceed \"Createbackup\" activity"
    }

    def 'When Notfications are received for Createbackup Activity and Job is successful'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(senario);
        brmMoServiceRetryProxy.getValidAsyncActionProgress(_,_,_ as Map) >> getAsyncActionProgress(actionName,resultType,state)

        when:"Create Backup action has received notifications"
        createBackupService.processNotification(notification)

        then:"Check if Job is Success through notifications"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        assert(activityJob.getAttribute("lastLogMessage").contains(log))

        where:
        jobResult|log                                                                         |actionName                                  | resultType                        |state                                |senario
        "SUCCESS"|"\"Createbackup\" activity is completed through Notifications."             |EcimBackupConstants.BACKUP_CREATE_ACTION_BSP|ActionResultType.SUCCESS.toString()|ActionStateType.FINISHED.toString()  | "SUCCESS"
        null     |"Action Name= \"createBackup\" ProgressPercentage=\"10\" State= \"RUNNING\""|EcimBackupConstants.BACKUP_CREATE_ACTION_BSP|ActionResultType.SUCCESS.toString()|ActionStateType.RUNNING.toString()   | "SUCCESS"
        null     |"Cancellation is in progress with Percentage = \"10\""                     |EcimBackupConstants.BACKUP_CREATE_ACTION_BSP|ActionResultType.FAILURE.toString()|ActionStateType.CANCELLING.toString()| "SUCCESS"
        null     |"Notification processing failed as an exception occurred"                   |EcimBackupConstants.BACKUP_CREATE_ACTION_BSP|ActionResultType.FAILURE.toString()|ActionStateType.CANCELLED.toString() | "FAILED"
    }

    def 'When Notfications are received for Createbackup Activity and Discarding invalid notification'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(SUCCESS);
        brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName) >> "3.4.1"
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true

        when:"Create Backup action has received invalid notifications"
        createBackupService.processNotification(notification)

        then:"Check if Job is failed through notifications"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
    }

    def 'When Notfications are received for Createbackup Activity and action is Failed due to Some Exception'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(FAIL);
        brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName) >> "3.4.1"
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true

        when:"Create Backup Activity is triggered"
        createBackupService.processNotification(notification)

        then:"Verify if Create Backup Action is is Failed due to some Exception"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        assert(activityJob.getAttribute("lastLogMessage").contains("Notification processing failed as an exception occurred"))
    }

    def 'When Notfications are received for Createbackup Activity and action is Failed due to  JobDataNotFoundException'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(JOB_NOT_FOUND_EXCEPTION);

        when:"Create Backup Activity is triggered"
        createBackupService.processNotification(notification)

        then:"Check if Job is Failed through notifications due to JobDataNotFoundException"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        null !=activityJob.getAttribute(ShmConstants.LOG)
        assert(activityJob.getAttribute("lastLogMessage").contains(log))

        where:
        jobResult|log
        "FAILED"|"Database service is not accessible"
    }

    def 'When Notfications are received for Createbackup Activity and action is Failed due to  UnsupportedFragmentException'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(EXCEPTION);
        brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName) >> "3.4.1"
        brmMoServiceRetryProxy.getValidAsyncActionProgress(_,_,_ as Map) >>  {throw new UnsupportedFragmentException("UnsupportedFragmentException occurred")}
        brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName) >> "3.4.1"

        when:"Create Backup Activity is triggered"
        createBackupService.processNotification(notification)

        then:"Check if Job is Failed through notifications due to UnsupportedFragmentException"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        null !=activityJob.getAttribute(ShmConstants.LOG)
        assert(activityJob.getAttribute("lastLogMessage").contains(log))

        where:
        jobResult|log
        "FAILED"|"UnSupported Fragment for the node \"LTE01dg2ERBS00002\", Unable to proceed for action."
    }

    def 'When Notfications are received for Createbackup Activity and action is Failed due to  MoNotFoundException'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(EXCEPTION);
        brmMoServiceRetryProxy.getValidAsyncActionProgress(_,_,_ as Map) >> { throw new MoNotFoundException("MoNotFoundException occurred") }

        when:"Create Backup Activity is triggered"
        createBackupService.processNotification(notification)

        then:"Check if Job is Failed through notifications due to MoNotFoundException"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        null !=activityJob.getAttribute(ShmConstants.LOG)
        assert(activityJob.getAttribute("lastLogMessage").contains(log))

        where:
        jobResult|log
        "FAILED"|"BrmBackupManager MO not found for the node \"LTE01dg2ERBS00002\", Unable to proceed for action."
    }

    def 'When Notifications are not received for Create Backup  activity and job went into handle timeout and it is Success'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(SUCCESS);
        brmMoServiceRetryProxy.getProgressFromBrmBackupManagerMO(_,_ as EcimBackupInfo) >> getAsyncActionProgress(EcimBackupConstants.BACKUP_CREATE_ACTION_BSP, ActionResultType.SUCCESS.toString(), ActionStateType.FINISHED.toString())

        when:"Create Backup Activity is triggered"
        ActivityStepResult activityStepResult=createBackupService.handleTimeout(activityJobId)

        then:"Check if Job is successful through Handle Timeout"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS)
    }

    def 'When Notifications are not received for Create Backup  activity and job went into handle timeout and it is Failed'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(FAIL);

        when:"Create Backup Activity is triggered"
        ActivityStepResult activityStepResult=createBackupService.handleTimeout(activityJobId)

        then:"Check if Job is Failed through Handle Timeout"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL)
    }


    def 'When Notifications are not received for Create Backup  activity and job went into handle timeout and it is Failed due to BackupDataNotFoundException'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(SUCCESS);
        brmMoServiceRetryProxy.getProgressFromBrmBackupManagerMO(_,_ as EcimBackupInfo) >> {throw new BackupDataNotFoundException("BackupDataNotFoundException occurred")}

        when:"Create Backup Activity is triggered"
        ActivityStepResult activityStepResult=createBackupService.handleTimeout(activityJobId)

        then:"Check if Job is Failed through Handle Timeout"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL)
    }

    def 'When Notifications are not received for Create Backup  activity and job went into handle timeout and it is Failed due to MoNotFoundException'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(SUCCESS);
        brmMoServiceRetryProxy.getProgressFromBrmBackupManagerMO(_,_ as EcimBackupInfo) >> {throw new MoNotFoundException("MoNotFoundException occurred")}

        when:"Create Backup Activity is triggered"
        ActivityStepResult activityStepResult=createBackupService.handleTimeout(activityJobId)

        then:"Check if Job is Failed through Handle Timeout"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL)
    }

    def 'When Notifications are not received for Create Backup  activity and job went into handle timeout and it is Failed due to UnsupportedFragmentException'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(SUCCESS);
        brmMoServiceRetryProxy.getProgressFromBrmBackupManagerMO(_,_ as EcimBackupInfo) >> {throw new UnsupportedFragmentException("UnsupportedFragmentException occurred")}

        when:"Create Backup Activity is triggered"
        ActivityStepResult activityStepResult=createBackupService.handleTimeout(activityJobId)

        then:"Check if Job is Failed through Handle Timeout"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL)
    }

    def 'When Notifications are not received for Create Backup  activity and job went into handle timeout and it is Failed due to JobDataNotFoundException'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(JOB_NOT_FOUND_EXCEPTION);
        brmMoServiceRetryProxy.getProgressFromBrmBackupManagerMO(_,_ as EcimBackupInfo) >> {throw new BackupDataNotFoundException("JobDataNotFoundException occurred")}

        when:"Create Backup Activity is triggered"
        ActivityStepResult activityStepResult=createBackupService.handleTimeout(activityJobId)

        then:"Check if Job is Failed through Handle Timeout"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL)
    }

    def 'When Notifications are not received for Create Backup  activity and job went into handle timeout and it is Failed due to some exception'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(FAIL);

        when:"Create Backup Activity is triggered"
        ActivityStepResult activityStepResult=createBackupService.handleTimeout(activityJobId)

        then:"Check if Job is Failed through Handle Timeout"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL)
    }

    def 'When Create Backup action is triggered and action is  cancelled and cancel action is successful'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(SUCCESS);

        when:"Create Backup action is triggered and action is  cancelled"
        ActivityStepResult activityStepResult=createBackupService.cancel(activityJobId)

        then:"Check if Create Backup is  cancelled"
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.EXECUTION_SUCESS)
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains("Cancel of \"Createbackup\" activity triggered on node"))
    }

    def 'When Create Backup action is triggered and action is  cancelled and cancel action is successful(backupName overwritten case)'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(SUCCESS);

        when:"Create Backup action is triggered and action is cancelled"
        ActivityStepResult activityStepResult=createBackupService.cancel(activityJobId)

        then:"Check if Create Backup is cancelled"
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.EXECUTION_SUCESS)
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains("Cancel of \"Createbackup\" activity triggered on node"))
    }

    def 'When Create Backup action is triggered and action is  cancelled and cancel action is Failed due to some exception'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(EXCEPTION);


        when:"Create Backup action is triggered and action is not cancelled"
        ActivityStepResult activityStepResult=createBackupService.cancel(activityJobId)

        then:"Check if Create Backup is not cancelled"
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.EXECUTION_FAILED)
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains("Unable to trigger \"cancelCurrentAction\" activity on the node. Exception occurred"))
    }

    def 'When Create Backup action is triggered and action is  cancelled and cancel action is Failed due to MoNotFoundException'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(MO_EXCEPTION);

        when:"Create Backup action is triggered and action is not cancelled"
        ActivityStepResult activityStepResult=createBackupService.cancel(activityJobId)

        then:"Check if Create Backup is not cancelled"
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.EXECUTION_FAILED)
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains("\"cancelCurrentAction\" activity cannot be triggered"))
    }

    def 'When Create Backup action is triggered and action is  cancelled and cancel action is Failed due to UnsupportedFragmentException'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(UNSUPPORTED_FRAGEMENT_EXCEPTION);

        when:"Create Backup action is triggered and action is not cancelled"
        ActivityStepResult activityStepResult=createBackupService.cancel(activityJobId)

        then:"Check if Create Backup is not cancelled"
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.EXECUTION_FAILED)
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains("UnSupported Fragment for the node \"LTE01dg2ERBS00002\", Unable to proceed for action."))
    }

    def 'When Create Backup action is triggered and action is  cancelled and cancel action is Failed due to JobDataNotFoundException'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(JOB_NOT_FOUND_EXCEPTION);

        when:"Create Backup action is triggered and action is not cancelled"
        ActivityStepResult activityStepResult=createBackupService.cancel(activityJobId)

        then:"Check if Create Backup is not cancelled"
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.EXECUTION_SUCESS)
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null !=activityJob.getAttribute(ShmConstants.LOG)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains("\"Createbackup\" activity triggered on node"))
    }

    def 'Test cancelTimeout activitiy for Create Backup Job'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(SUCCESS);
        brmMoServiceRetryProxy.isBackupExist(_,_ as EcimBackupInfo) >> isBackupExistFlag
        brmMoServiceRetryProxy.getProgressFromBrmBackupManagerMO(_,_ as EcimBackupInfo) >> progressReport

        when:"Create Backup action is triggered and cancelTimeout action is"
        ActivityStepResult activityStepResult=createBackupService.cancelTimeout(activityJobId,true)

        then:"Check if cancelTimeout is triggered"
        activityStepResult.getActivityResultEnum().equals(activityResult)

        where:
        activityResult                               |isBackupExistFlag  |progressReport
        ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS|  true             |getAsyncActionProgress(EcimBackupConstants.BACKUP_CANCEL_ACTION,ActionResultType.SUCCESS.toString(),ActionStateType.FINISHED.toString())
        ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS|  true             |getAsyncActionProgress(EcimBackupConstants.BACKUP_CANCEL_ACTION,ActionResultType.SUCCESS.toString(),ActionStateType.RUNNING.toString())
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL   | false             |null
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL   |  false            |getAsyncActionProgress(EcimBackupConstants.BACKUP_CANCEL_ACTION,ActionResultType.FAILURE.toString(),ActionStateType.FINISHED.toString())
        ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS|  true             |getAsyncActionProgress(EcimBackupConstants.BACKUP_CREATE_ACTION,ActionResultType.SUCCESS.toString(),ActionStateType.FINISHED.toString())
        ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS|  true             |getAsyncActionProgress(EcimBackupConstants.BACKUP_CREATE_ACTION,ActionResultType.SUCCESS.toString(),ActionStateType.RUNNING.toString())
    }


    def 'Test cancelTimeout activitiy for Create Backup Job When BrmMoService.isBackupExist throwing Exception'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(SUCCESS);
        brmMoServiceRetryProxy.isBackupExist(_,_ as EcimBackupInfo) >> throwExceptionSenario(exceptionSenario)


        when:"Create Backup action is triggered and cancelTimeout action is"
        ActivityStepResult activityStepResult=createBackupService.cancelTimeout(activityJobId,true)

        then:"Check if cancelTimeout is triggered and result as excpected"
        activityStepResult.getActivityResultEnum().equals(activityResult)

        where:
        activityResult                               |exceptionSenario
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL   |"BACKUP_DATA_NOT_FOUND"
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL   |"MO_EXCEPTION"
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL   |"UNSUPPORTED_FRAGEMENT_EXCEPTION"
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL   |"JOB_NOT_FOUND_EXCEPTION"
    }

    def 'Test cancelTimeout activitiy for Create Backup Job When BrmMoService.getProgressFromBrmBackupManagerMO throwing Exception'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(SUCCESS);
        switch(exceptionSenario) {
            case JOB_NOT_FOUND_EXCEPTION:
                brmMoServiceRetryProxy.getProgressFromBrmBackupManagerMO(_,_ as EcimBackupInfo) >> { throw new JobDataNotFoundException("JobDataNotFoundException occurred") }
                break;

            case MO_EXCEPTION:
                brmMoServiceRetryProxy.getProgressFromBrmBackupManagerMO(_,_ as EcimBackupInfo) >> { throw new MoNotFoundException("MoNotFoundException occurred") }
                break;
            case UNSUPPORTED_FRAGEMENT_EXCEPTION:
                brmMoServiceRetryProxy.getProgressFromBrmBackupManagerMO(_,_ as EcimBackupInfo) >> { throw new UnsupportedFragmentException("UnsupportedFragmentException occurred") }
                break;
        }


        when:"Create Backup action is triggered and cancelTimeout action is"
        ActivityStepResult activityStepResult=createBackupService.cancelTimeout(activityJobId,true)

        then:"Check if cancelTimeout is Failed"
        activityStepResult.getActivityResultEnum().equals(activityResult)

        where:
        activityResult                               |exceptionSenario
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL   |"MO_EXCEPTION"
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL   |"UNSUPPORTED_FRAGEMENT_EXCEPTION"
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL   |"JOB_NOT_FOUND_EXCEPTION"
    }

    def 'Test subscribeForPolling for Create Backup Jobs'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(SUCCESS)

        when: "subscribeForPolling for Create Backup"
        createBackupService.subscribeForPolling(activityJobId)

        then:"Check if subscribeForPolling for  Create Backup success"
        1 * systemRecorder.recordEvent('SHM.POLLING_SUBSCRIPTION_SUCCESS', _, 'createbackup', 'BACKUP', 'SHM:1')
    }

    def 'Test subscribeForPolling for Create Backup Jobs When is is Failed due to Runtime Exception '() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(RUNTIME_EXCEPTION);

        when:"subscribeForPolling for Create Backup"
        createBackupService.subscribeForPolling(activityJobId)

        then:"Check if subscribeForPolling for  Create Backup is Failed"
        0 * systemRecorder.recordEvent('SHM.POLLING_SUBSCRIPTION_SUCCESS', _, 'createbackup', 'BACKUP', 'SHM:1')
    }

    def 'Test processPollingResponse for Create Backup Jobs'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(SUCCESS);
        final Map<String, Object> responseAttributes=new HashMap<>()
        final Map<String, Object> moAttributes=new HashMap<>()
        moAttributes.put(ReportProgress.ASYNC_ACTION_PROGRESS,getReportProgress(actionName,ActionResultType.SUCCESS.toString(),state))
        responseAttributes.put(ShmConstants.MO_ATTRIBUTES, moAttributes)
        responseAttributes.put(ShmConstants.FDN,moFdn)
        when:"processPollingResponse for Create Backup action is triggered"
        createBackupService.processPollingResponse(activityJobId,responseAttributes)

        then:"Check if subscribeForPolling for Create Backup action is success "

        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT,jobProperties)
        assert(activityJob.getAttribute("lastLogMessage").contains(log))

        1 * systemRecorder.recordCommand('SHM.CREATE_BACKUP_SERVICE',_, _, _,_)
        1 * systemRecorder.recordEvent('SHM.ECIM.BACKUP.CREATEBACKUP_COMPLETED', _, _, _, 'SHM:1:"Createbackup" activity is completed through Polling.; Flow : COMPLETED_THROUGH_POLLING')
        1 * workflowInstanceNotifier.sendActivate(_, _)

        where:
        jobResult  |state                              |actionName                                 |log
        "CANCELLED"|ActionStateType.FINISHED.toString()|EcimBackupConstants.BACKUP_CANCEL_ACTION   |"\"Createbackup\" activity is completed through Polling."
        "FAILED"   |ActionStateType.RUNNING.toString() |EcimBackupConstants.BACKUP_CANCEL_ACTION   |"\"Createbackup\" activity is completed through Polling."
    }


    def 'Test asyncHandleTimeout for Create Backup Activity'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(senario);
        brmMoServiceRetryProxy.getProgressFromBrmBackupManagerMO(_,_ as EcimBackupInfo) >> getReportProgress(EcimBackupConstants.BACKUP_CANCEL_ACTION,ActionResultType.SUCCESS.toString(),state)

        when:"asyncHandleTimeout for Create Backup action is triggered"
        createBackupService.asyncHandleTimeout(activityJobId)

        then:"Check if asyncHandleTimeout Create Backup is as expected"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT,jobProperties)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if (lastLog != null && log != null ) {
            assert(lastLog.contains(log))
        }

        where:
        jobResult|senario     |state                                 |activityResult                                            |log
        "SUCCESS"| "SUCCESS"  |ActionStateType.FINISHED.toString()   |ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION | "\"Createbackup\" activity for the backup \"CXP101-R501_\" is completed successfully."
        "FAILED" | "SUCCESS"  |ActionStateType.RUNNING.toString()    |ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION |null
        "FAILED" | "SUCCESS"  |ActionStateType.CANCELLING.toString() |ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION |null
        "FAILED" |"SUCCESS"   |ActionStateType.CANCELLED.toString()  | ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION    |null
    }

    def 'Test asyncHandleTimeout for Create Backup Activity for Exception senario'() {

        given:"NetworkElement and Create Backup details"
        buildJobPO(SUCCESS);

        switch(exceptions) {
            case JOB_NOT_FOUND_EXCEPTION:
                brmMoServiceRetryProxy.getProgressFromBrmBackupManagerMO(_,_ as EcimBackupInfo) >> { throw new JobDataNotFoundException("JobDataNotFoundException occurred") }
                break;
            case BACKUP_DATA_NOT_FOUND:
                brmMoServiceRetryProxy.getProgressFromBrmBackupManagerMO(_,_ as EcimBackupInfo) >> { throw new BackupDataNotFoundException("BackupDataNotFoundException occurred") }
                break;
            case MO_EXCEPTION:
                brmMoServiceRetryProxy.getProgressFromBrmBackupManagerMO(_,_ as EcimBackupInfo) >> { throw new MoNotFoundException("MoNotFoundException occurred") }
                break;
            case UNSUPPORTED_FRAGEMENT_EXCEPTION:
                brmMoServiceRetryProxy.getProgressFromBrmBackupManagerMO(_,_ as EcimBackupInfo) >> { throw new UnsupportedFragmentException("UnsupportedFragmentException occurred") }
                break;
            case EXCEPTION:
                brmMoServiceRetryProxy.getProgressFromBrmBackupManagerMO(_,_ as EcimBackupInfo) >> { throw new Exception("Exception occurred") }
                break;
        }

        when:"asyncHandleTimeout for Create Backup action is triggered"
        createBackupService.asyncHandleTimeout(activityJobId)

        then:"Check if asyncHandleTimeout Create Backup is as expected"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT,jobProperties)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if (lastLog != null && log != null ) {
            assert(lastLog.contains(log))
        }

        1 * systemRecorder.recordEvent('SHM.POLLING_UNSUBSCRIPTION_SUCCESS', _, 'Createbackup', _, _)
        1 * systemRecorder.recordEvent('SHM.CREATE_BACKUP_TIME_OUT', _, _,_, _)

        where:
        jobResult |exceptions                       | log
        "FAILED"  | "BACKUP_DATA_NOT_FOUND"         |"\"Createbackup\" action has failed in handle timeout. Reason: \"BackupDataNotFoundException occurred\"."
        "FAILED"  | "MO_EXCEPTION"                  |"BrmBackupManager MO not found for the node \"LTE01dg2ERBS00002\", Unable to proceed for action."
        "FAILED"  | "JOB_NOT_FOUND_EXCEPTION"       |"Database service is not accessible."
        "FAILED"  |"UNSUPPORTED_FRAGEMENT_EXCEPTION"|null
        "FAILED"  |"EXCEPTION"                      |null
    }
    
    def 'Create Secure backup when user has given Password and userlabel'() {
        
                given:"NetworkElement and Create Backup details"
                buildJobPO(scenario);
                brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName) >> brmVersion
                activityJobTBACValidator.validateTBAC(_, _, _, _) >> true
                when:"Create Backup Activity is triggered"
                createBackupService.execute(activityJobId)
        
                then:"Verify if Create Backup Action is triggered Successfully"
                activityJobId !=null
                def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
                null != activityJob.getAttribute(ShmConstants.LOG)
                null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
                final List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
                jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
                null !=activityJob.getAttribute(ShmConstants.LOG)
                assert(activityJob.getAttribute("lastLogMessage").contains(log))
        
                1 * brmMoServiceRetryProxy.executeMoAction(_, _,_, _)
                1 * systemRecorder.recordCommand('SHM.CREATE_BACKUP_EXECUTE ', CommandPhase.FINISHED_WITH_SUCCESS, _, _, _)
                assert(activityJob.getAttribute("lastLogMessage").contains(log))
                where:
                jobResult|      scenario                             |  isActivityTriggered  | log                                                               |brmVersion
                null     |   "SECURE_BACKUP"                         |    "true"             |"\"Createbackup\" activity is triggered (timeout = \"0\" minutes)" |"3.7.1"
                null     |   "SECURE_BACKUP_WITH_EMPTY_USERLABEL"    |    "true"             |"\"Createbackup\" activity is triggered (timeout = \"0\" minutes)" |"3.7.1"
                null     |   "SECURE_BACKUP_WITH_EMPTY_PWD"          |    "true"             |"\"Createbackup\" activity is triggered (timeout = \"0\" minutes)" |"3.7.1"
                null     |   "SECURE_BACKUP_WITH_INVALID_BRMVERSION" |    "true"             |"\"Createbackup\" activity is triggered (timeout = \"0\" minutes)" |"R8B"
            }
}
