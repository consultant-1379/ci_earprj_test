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
package com.ericsson.oss.services.shm.es.impl.ecim.backup

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.model.event.based.mediation.ShmEcimMOActionMediationTaskRequest

public class ProcessMoActionResponseTest extends EcimBackupTestDataProvider{

    @ObjectUnderTest
    private UploadBackupService uploadBackupService;

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job.service")
    }

    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps)

    def 'Verify mo action response when action is triggered on the node'() {

        given : "Activity Job Id and response received from mediation"
        responseAttributes = buildMoActionResponseAttributes(actionId, isActionAlreadyRunningOnTheNode, null);
        buildJobPO(UPLOAD_BACKUP);

        when: "Response is recieved from mediation"
        uploadBackupService.processMoActionResponse(activityJobId, responseAttributes);

        then: "Verify response from mediation is processed or not"
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")

        activityJob.attributes.get("lastLogMessage").contains("\"Uploadbackup\" activity is triggered");
        getJobProperty(EcimBackupConstants.TOTAL_BACKUPS, jobProperties) == totalBackups
        getJobProperty(BackupActivityConstants.PROCESSED_BACKUPS, jobProperties) == processedBackups
        getJobProperty("fdn",jobProperties) == fdn


        where:
        actionId   | isActionAlreadyRunningOnTheNode | totalBackups | processedBackups | fdn
        0          | false                           | "1"          | "1"              | "SubNetwork=LTE01dg2ERBS00002,MeContext=LTE01dg2ERBS00002,ManagedElement=LTE01dg2ERBS00002,SystemFunctions=1,BrM=1,BrmBackupManager=1,BrmBackup=2"
    }

    def 'Verify mo action response when action triggering is failed on the node'(){

        given : "Activity Job Id and response received from mediation"
        responseAttributes = buildMoActionResponseAttributes(actionId, isActionAlreadyRunningOnTheNode, null);
        buildJobPO(UPLOAD_BACKUP);

        when: "Response is recieved from mediation"
        uploadBackupService.processMoActionResponse(activityJobId, responseAttributes);
        then: "Verify response from mediation is processed or not"

        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        final List<Map<String, Object>> jobProperties =activityJob.attributes.get("jobProperties")

        activityJob.attributes.get("lastLogMessage").contains("\"Uploadbackup\" activity has failed for backup \"" +backupNameFromResponse+"\"");
        getJobProperty(EcimBackupConstants.TOTAL_BACKUPS, jobProperties) == totalBackups
        getJobProperty(BackupActivityConstants.PROCESSED_BACKUPS, jobProperties) == processedBackups
        getJobProperty("fdn",jobProperties) != fdn

        where:
        actionId   | isActionAlreadyRunningOnTheNode | totalBackups | processedBackups | fdn
        1          | false                           | "1"          | "1"              | "moFdn"
    }

    def 'Verify mo action response when action triggering is failed on the node with error message'(){

        given : "Activity Job Id and response received from mediation"
        responseAttributes = buildMoActionResponseAttributes(actionId, isActionAlreadyRunningOnTheNode, "Action triggered failed on the node");
        buildJobPO(UPLOAD_BACKUP);

        when: "Response is recieved from mediation"
        uploadBackupService.processMoActionResponse(activityJobId, responseAttributes);

        then: "Verify response from mediation is processed or not"
        1 * eventSender.send(_ as ShmEcimMOActionMediationTaskRequest)

        where:
        actionId   | isActionAlreadyRunningOnTheNode
        1          | false
    }

    def 'Verify mo action response when action triggering is failed on the node with ActionNotAllowedException'(){

        given : "Activity Job Id and response received from mediation"
        responseAttributes = buildMoActionResponseAttributes(actionId, isActionAlreadyRunningOnTheNode, "ActionNotAllowedException");
        buildJobPO(UPLOAD_BACKUP);

        when: "Response is recieved from mediation"
        uploadBackupService.processMoActionResponse(activityJobId, responseAttributes);

        then: "Verify response from mediation is processed or not"
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("\"Uploadbackup\" activity has failed for backup \"" +backupNameFromResponse+"\"");
        0 * eventSender.send(_ as ShmEcimMOActionMediationTaskRequest)

        where:
        actionId   | isActionAlreadyRunningOnTheNode
        1          | false
    }
}
