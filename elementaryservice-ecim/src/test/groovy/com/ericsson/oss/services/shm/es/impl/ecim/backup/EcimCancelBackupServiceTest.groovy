/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.ecim.backup

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.services.shm.ecim.common.*
import com.ericsson.oss.services.shm.es.api.*
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants

public class EcimCancelBackupServiceTest extends EcimBackupTestDataProvider {

    @ObjectUnderTest
    CancelBackupService cancelBackupService;
    private RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps)



    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job.service")
    }

    def 'Test evaluateCancelProgress for Cancel Backup Activity'() {

        given:"NetworkElement and Cancel Backup details"
        buildJobPO(SUCCESS);
        final AsyncActionProgress progressReport= getAsyncActionProgress(EcimBackupConstants.BACKUP_CANCEL_ACTION,ActionResultType.SUCCESS.toString(),state)

        when:"Perform Cancel Backup evaluateCancelProgress for the allocated job "
        cancelBackupService.evaluateCancelProgress(progressReport,jobActivityInfo, moFdn,neJobStaticData,actionName);


        then:"Verify If Activity is as per expected"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")
        jobResult == getJobProperty(ShmConstants.RESULT, jobProperties)
        assert(activityJob.getAttribute(ShmConstants.LOG).toString().contains(log))

        where:
        jobResult  |log                                                           |actionName                                  |state
        "CANCELLED"|"\"cancelCurrentAction\" activity is completed successfully." |EcimBackupConstants.BACKUP_CREATE_ACTION_BSP|ActionStateType.FINISHED.toString()
        null       |"Cancellation is in progress with Percentage = \"10\""        |EcimBackupConstants.BACKUP_CANCEL_ACTION    |ActionStateType.RUNNING.toString()
        "FAILED"   |"\"cancelCurrentAction\" activity has failed."                |EcimBackupConstants.BACKUP_CANCEL_ACTION    |ActionStateType.FINISHED.toString()
    }

    def 'Test verifyCancelHandleTimeout for Cancel Backup Activity'() {

        given:"NetworkElement and Cancel Backup details"
        buildJobPO(SUCCESS);

        when:"Perform Cancel Backup verifyCancelHandleTimeout for the allocated job "
        ActivityStepResult activityStepResult=cancelBackupService.verifyCancelHandleTimeout(progressReport,activityJobId);

        then:"Verify If Activity is as per expected"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(activityResult)

        where:
        activityResult                                 |progressReport
        ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS  |getAsyncActionProgress(EcimBackupConstants.BACKUP_CANCEL_ACTION,ActionResultType.SUCCESS.toString(),ActionStateType.FINISHED.toString())
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL     |null
        ActivityStepResultEnum.TIMEOUT_RESULT_FAIL     |getAsyncActionProgress(EcimBackupConstants.BACKUP_CANCEL_ACTION,ActionResultType.FAILURE.toString(),ActionStateType.FINISHED.toString())
    }

    def 'Test isCancelActionTriggerred for Cancel Backup Activity'() {

        given:"NetworkElement and Cancel Backup details"
        buildJobPO(SUCCESS);

        when:"Perform Cancel Backup isCancelActionTriggerred for the allocated job "
        boolean responseFlag=cancelBackupService.isCancelActionTriggerred(getAttributeMap());

        then:"Verify If Activity is as per expected"
        activityJobId !=null
        responseFlag==false
    }

    def 'Test validateActionProgressReport for Cancel Backup Activity'() {

        given:"NetworkElement and Cancel Backup details"
        buildJobPO(SUCCESS);
        final AsyncActionProgress progressReport= getAsyncActionProgress(EcimBackupConstants.BACKUP_CANCEL_ACTION,ActionResultType.SUCCESS.toString(),ActionStateType.FINISHED.toString())

        when:"Perform Cancel Backup validateActionProgressReport for the allocated job "
        boolean responseFlag=cancelBackupService.validateActionProgressReport(progressReport,EcimBackupConstants.BACKUP_CANCEL_ACTION);


        then:"Verify If Activity is as per expected"
        responseFlag==false
    }
}
