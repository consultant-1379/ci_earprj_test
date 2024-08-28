/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.ecim.polling

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants
import com.ericsson.oss.services.shm.es.impl.ecim.backup.CreateBackupService
import com.ericsson.oss.services.shm.es.impl.ecim.backup.UploadBackupService
import com.ericsson.oss.services.shm.es.impl.ecim.upgrade.ActivateService

class ProcessPollingResponseTest extends AbstractPollingData{

    @ObjectUnderTest
    private ActivateService activateService

    @ObjectUnderTest
    private UploadBackupService uploadBackupService

    @ObjectUnderTest
    private CreateBackupService createBackupService

    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps)

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
    }

    def Map<String, Object> responseAttributes=new HashMap<>();

    def 'Verify polling response when action is Success on the node'(){
        given : "Activity Job Id and response received from mediation"
        responseAttributes = buildResponseData(PollingTestConstants.upMoFdn, state, progressPercentage, additionalInfo, progressInfo, upMoState, PollingTestConstants.resultSuccess, PollingTestConstants.ACTIVITY_ACTIVATE);
        buildJobPO(false,PollingTestConstants.ACTIVITY_ACTIVATE,"ECIM_SwM","4.2.0","ECIM_SwM")
        when: "Response is recieved from mediation"
        activateService.processPollingResponse(activityJobId, responseAttributes);
        then: "Verify activate activity is finised through polling or not."
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("\"Activate\" activity is completed successfully");
        where:
        state                            | progressPercentage   | additionalInfo                 | progressInfo                            | upMoState
        "FINISHED"                       | 100                  | "step 1 (step1) performed"     | "activate started; all steps succeeded" |"WAITING_FOR_COMMIT"
    }
    def 'Verify polling response when action is In-progress'(){
        given : "Activity Job Id and response received from mediation"
        responseAttributes = buildResponseData(PollingTestConstants.upMoFdn, state, progressPercentage, additionalInfo, progressInfo, upMoState, PollingTestConstants.resultSuccess, PollingTestConstants.ACTIVITY_ACTIVATE);
        buildJobPO(false,PollingTestConstants.ACTIVITY_ACTIVATE,"ECIM_SwM","4.2.0","ECIM_SwM")
        when: "Response is recieved from mediation2"
        activateService.processPollingResponse(activityJobId, responseAttributes);
        then: "Verify whether activity progress logs are persisted in database or not."
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("Progress Percentage : 56; ProgressInfo : activate started; all steps succeeded; State: RUNNING;");
        where:
        state                           | progressPercentage   | additionalInfo                 | progressInfo                            | upMoState
        "RUNNING"                       | 56                   | "step 1 (step1) performed"     | "activate started; all steps succeeded" |"ACTIVATION_STEP_COMPLETED"
    }

    def 'Verify polling response when action is Failed on the node'(){
        given : "Activity Job Id and response received from mediation"
        responseAttributes = buildResponseData(PollingTestConstants.upMoFdn, state, progressPercentage, additionalInfo, progressInfo, upMoState, PollingTestConstants.resultFailure, PollingTestConstants.ACTIVITY_ACTIVATE);
        buildJobPO(false,PollingTestConstants.ACTIVITY_ACTIVATE,"ECIM_SwM","4.2.0","ECIM_SwM")
        when: "Response is recieved from mediation"
        activateService.processPollingResponse(activityJobId, responseAttributes);
        then: "Verify activate activity is failed through polling or not"
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("\"Activate\" activity has failed");
        where:
        state                            | progressPercentage   | additionalInfo                 | progressInfo                            | upMoState
        "FINISHED"                       | 100                  | "step 1 (step1) Failed"        | "activate started; all steps Failed"    |"PREPARE_COMPLETED"
    }
    def 'Verify polling response when activity is already completed'(){
        given : "Activity Job Id and response received from mediation"
        responseAttributes = buildResponseData(PollingTestConstants.upMoFdn, state, progressPercentage, additionalInfo, progressInfo, upMoState, PollingTestConstants.resultFailure, PollingTestConstants.ACTIVITY_ACTIVATE);
        buildJobPO(true,PollingTestConstants.ACTIVITY_ACTIVATE,"ECIM_SwM","4.2.0","ECIM_SwM")
        when: "Response is recieved from mediation"
        activateService.processPollingResponse(activityJobId, responseAttributes);
        then: "Verify polling response is not processed when activity is already completed"
        0 * remoteSoftwarePackageManager.getUpgradePackageDetails(PollingTestConstants.softwarePackageName);
        0 * activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, PollingTestConstants.ACTIVITY_ACTIVATE, processVariables)
        where:
        state                            | progressPercentage   | additionalInfo                    | progressInfo                               | upMoState
        "FINISHED"                       | 100                  | "step 1 (step1) performed"        | "activate started; all steps succeeded"    |"WAITING_FOR_COMMIT"
    }
    def 'Verify polling response when action is Success on the node for CreateBackup'(){
        given : "Activity Job Id and response received from mediation"
        responseAttributes = buildResponseData(PollingTestConstants.brmBackupMoFdn,state, progressPercentage, additionalInfo, progressInfo, "", result, EcimBackupConstants.BACKUP_CREATE_ACTION)
        buildJobPO(false,PollingTestConstants.ACTIVITY_CREATE,"ECIM_BrM","2.3.0","RcsBrM")
        when: "Response is received from mediation"
        createBackupService.processPollingResponse(activityJobId, responseAttributes)
        then: "Verify create activity is finished through polling or not"
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("log")[2].get("message").contains("\"Createbackup\" activity is completed through Polling")
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("log")[1].get("message").contains("\"Createbackup\" activity for the backup \""+PollingTestConstants.backupName+"\" is completed successfully.")
        runtimeDps.stubbedDps.liveBucket.findPoById(poId) == null
        where:
        state                            | progressPercentage   | additionalInfo                 | progressInfo                                | result
        "FINISHED"                       | 100                  | "step 1 (step1) performed"     | "createBackup started; all steps succeeded" |"SUCCESS"
    }

    def 'Verify polling response when action is In-progress for CreateBackup'(){
        given : "Activity Job Id and response received from mediation"
        responseAttributes = buildResponseData(PollingTestConstants.brmBackupMoFdn,state, progressPercentage, additionalInfo, progressInfo, "", result, EcimBackupConstants.BACKUP_CREATE_ACTION)
        buildJobPO(false,PollingTestConstants.ACTIVITY_CREATE,"ECIM_BrM","2.3.0","RcsBrM")
        when: "Response is received from mediation2"
        createBackupService.processPollingResponse(activityJobId, responseAttributes)
        then: "Verify whether activity progress logs are persisted in database or not."
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("Action Name= \"CREATE\" ProgressPercentage=\"80\" State= \"RUNNING");
        where:
        state                           | progressPercentage   | additionalInfo                 | progressInfo                               | result
        "RUNNING"                       | 80                   | "step 1 (step1) performed"     | "createBackup started; all steps succeeded"|null
    }
    def 'Verify polling response when action is Failed on the node  for CreateBackup'(){
        given : "Activity Job Id and response received from mediation"
        responseAttributes = buildResponseData(PollingTestConstants.brmBackupMoFdn,state, progressPercentage, additionalInfo, progressInfo, "", result, EcimBackupConstants.BACKUP_CREATE_ACTION)
        buildJobPO(false,PollingTestConstants.ACTIVITY_CREATE,"ECIM_BrM","2.3.0","RcsBrM")
        when: "Response is received from mediation"
        createBackupService.processPollingResponse(activityJobId, responseAttributes)
        then: "Verify create activity is failed through polling or not"
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("log")[2].get("message").contains("\"Createbackup\" activity is completed through Polling")
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("log")[1].get("message").contains("Createbackup for "+PollingTestConstants.backupName+"\" activity has failed.")
        runtimeDps.stubbedDps.liveBucket.findPoById(poId) == null
        where:
        state                            | progressPercentage   | additionalInfo                 | progressInfo                             | result
        "FINISHED"                       | 100                  | "step 1 (step1) Failed"        | "createBackup started; all steps Failed" |"FAILURE"
    }
    def 'Verify polling response when activity is already completed  for CreateBackup'(){
        given : "Activity Job Id and response received from mediation"
        responseAttributes = buildResponseData(PollingTestConstants.brmBackupMoFdn,state, progressPercentage, additionalInfo, progressInfo, "", result, EcimBackupConstants.BACKUP_CREATE_ACTION)
        buildJobPO(true,PollingTestConstants.ACTIVITY_CREATE,"ECIM_BrM","2.3.0","RcsBrM")
        when: "Response is received from mediation"
        createBackupService.processPollingResponse(activityJobId, responseAttributes)
        then: "Verify polling response is not processed when activity is already completed"
        0 * activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, "createbackup", processVariables)
        where:
        state                            | progressPercentage   | additionalInfo                    | progressInfo                                | result
        "FINISHED"                       | 100                  | "step 1 (step1) performed"        | "createBackup started; all steps succeeded" |"FAILURE"
    }
    def 'Verify polling response when action is Success on the node for UploadBackup'(){
        given : "Activity Job Id and response received from mediation"
        responseAttributes = buildResponseData(PollingTestConstants.brmBackupMoFdn,state, progressPercentage, additionalInfo, progressInfo, "", result, EcimBackupConstants.BACKUP_EXPORT_ACTION)
        buildJobPO(false,PollingTestConstants.ACTIVITY_UPLOAD,"ECIM_BrM","2.3.0","RcsBrM")
        when: "Response is received from mediation"
        uploadBackupService.processPollingResponse(activityJobId, responseAttributes)
        then: "Verify upload activity is finished through polling or not"
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("log")[2].get("message").contains("\"Uploadbackup\" activity is completed through Polling for "+PollingTestConstants.backupName)
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("log")[3].get("message").contains("\"Uploadbackup\" activity is completed successfully.")
        where:
        state                            | progressPercentage   | additionalInfo                 | progressInfo                                | result
        "FINISHED"                       | 100                  | "step 1 (step1) performed"     | "uploadBackup started; all steps succeeded" |"SUCCESS"
    }
    def 'Verify polling response when action is In-progress for UploadBackup'(){
        given : "Activity Job Id and response received from mediation"
        responseAttributes = buildResponseData(PollingTestConstants.brmBackupMoFdn,state, progressPercentage, additionalInfo, progressInfo, "", result, EcimBackupConstants.BACKUP_EXPORT_ACTION)
        buildJobPO(false,PollingTestConstants.ACTIVITY_UPLOAD,"ECIM_BrM","2.3.0","RcsBrM")
        when: "Response is received from mediation2"
        uploadBackupService.processPollingResponse(activityJobId, responseAttributes)
        then: "Verify whether activity progress logs are persisted in database or not."
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("Action Name= \"EXPORT\" ProgressPercentage=\"80\" State= \"RUNNING");
        where:
        state                           | progressPercentage   | additionalInfo                 | progressInfo                               | result
        "RUNNING"                       | 80                   | "step 1 (step1) performed"     | "uploadBackup started; all steps succeeded"|null
    }
    def 'Verify polling response when action is Failed on the node  for UploadBackup'(){
        given : "Activity Job Id and response received from mediation"
        responseAttributes = buildResponseData(PollingTestConstants.brmBackupMoFdn,state, progressPercentage, additionalInfo, progressInfo, "", result, EcimBackupConstants.BACKUP_EXPORT_ACTION)
        buildJobPO(false,PollingTestConstants.ACTIVITY_UPLOAD,"ECIM_BrM","2.3.0","RcsBrM")
        when: "Response is received from mediation"
        uploadBackupService.processPollingResponse(activityJobId, responseAttributes)
        then: "Verify upload activity is failed through polling or not"
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("log")[2].get("message").contains("\"Uploadbackup\" activity is completed through Polling for "+PollingTestConstants.backupName)
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("\"Uploadbackup\" activity has failed");
        runtimeDps.stubbedDps.liveBucket.findPoById(poId) == null
        where:
        state                            | progressPercentage   | additionalInfo                 | progressInfo                             | result
        "FINISHED"                       | 100                  | "step 1 (step1) Failed"        | "uploadBackup started; all steps Failed" |"FAILURE"
    }
    def 'Verify polling response when activity is already completed  for UploadBackup'(){
        given : "Activity Job Id and response received from mediation"
        responseAttributes = buildResponseData(PollingTestConstants.brmBackupMoFdn,state, progressPercentage, additionalInfo, progressInfo, "", result, EcimBackupConstants.BACKUP_EXPORT_ACTION)
        buildJobPO(true,PollingTestConstants.ACTIVITY_UPLOAD,"ECIM_BrM","2.3.0","RcsBrM")
        when: "Response is received from mediation"
        uploadBackupService.processPollingResponse(activityJobId, responseAttributes)
        then: "Verify polling response is not processed when activity is already completed"
        0 * activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, "uploadbackup", processVariables)
        where:
        state                            | progressPercentage   | additionalInfo                    | progressInfo                                | result
        "FINISHED"                       | 100                  | "step 1 (step1) performed"        | "uploadBackup started; all steps succeeded" |"FAILURE"
    }
}
