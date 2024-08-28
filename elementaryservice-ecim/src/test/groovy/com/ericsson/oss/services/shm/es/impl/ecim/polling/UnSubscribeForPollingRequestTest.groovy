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
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.es.impl.ecim.backup.CreateBackupService
import com.ericsson.oss.services.shm.es.impl.ecim.backup.UploadBackupService
import com.ericsson.oss.services.shm.es.impl.ecim.upgrade.ActivateService
import com.ericsson.oss.services.shm.es.polling.PollingActivityManager
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum
import com.ericsson.oss.services.shm.notifications.api.Notification


public class UnSubscribeForPollingRequestTest extends AbstractPollingData {

    @ObjectUnderTest
    private PollingActivityManager pollingActivityManager

    @MockedImplementation
    private UploadBackupService uploadBackupService

    @MockedImplementation
    private CreateBackupService createBackupService

    @ObjectUnderTest
    private ActivateService activateService

    @MockedImplementation
    private Notification notification

    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps)

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
    }

    def 'unsubscribe for polling by using PollingActivity PO Id when job is finished based on notifications or using polling'(){
        given : "Details of the node, type of the job and activity of the job"
        buildJobPO(false, null,"ECIM_SwM","4.2.0","ECIM_SwM")
        buildDataToSubscribeForPolling(SHMCapabilities.UPGRADE_JOB_CAPABILITY, PollingTestConstants.ACTIVITY_ACTIVATE, JobTypeEnum.UPGRADE, "ECIM_SwM","4.2.0","ECIM_SwM")
        dspStatusInfoProvider.isDatabaseAvailable() >> true
        activateService.subscribeForPolling(activityJobId)
        when: "Poll entry is available in the DPS and the job is finished"
        pollingActivityManager.unsubscribeByPOId(poId)
        then: "Verify whether the polling entry is deleted from DPS or not"
        final PersistenceObject persistenceObject = getPollingActivityPos(activityJobId)
        persistenceObject == null;
    }

    def 'unsubscribe for polling by using activity job Id when job is finished based on notifications or using polling'(){
        given : "Details of the node, type of the job and activity of the job"
        buildJobPO(false, null,"ECIM_SwM","4.2.0","ECIM_SwM")
        buildDataToSubscribeForPolling(SHMCapabilities.UPGRADE_JOB_CAPABILITY, PollingTestConstants.ACTIVITY_ACTIVATE, JobTypeEnum.UPGRADE, "ECIM_SwM","4.2.0","ECIM_SwM")
        dspStatusInfoProvider.isDatabaseAvailable() >> true
        activateService.subscribeForPolling(activityJobId)
        when: "Poll entry is available in the DPS and the job is finished"
        pollingActivityManager.unsubscribeByActivityJobId(activityJobId,PollingTestConstants.ACTIVITY_ACTIVATE,PollingTestConstants.nodeName)
        then: "Verify whether the polling entry is deleted from DPS or not"
        final PersistenceObject persistenceObject = getPollingActivityPos(activityJobId)
        persistenceObject == null;
    }
    def 'unsubscribe for polling for upload backup when job is finished through processnotification'(){
        given : "Details of the node, type and activity of the job"
        buildJobPO(false, PollingTestConstants.ACTIVITY_UPLOAD,"ECIM_BrM","2.3.0","RcsBrM")
        buildDataToSubscribeForPolling(SHMCapabilities.BACKUP_JOB_CAPABILITY, PollingTestConstants.ACTIVITY_UPLOAD, JobTypeEnum.BACKUP, "ECIM_BrM","2.3.0","RcsBrM")
        dspStatusInfoProvider.isDatabaseAvailable() >> true
        uploadBackupService.subscribeForPolling(activityJobId)
        when: "when registered for process notification"
        uploadBackupService.processNotification(notification)
        then: "Verify whether the polling entry is deleted from DPS or not"
        final PersistenceObject persistenceObject = getPollingActivityPos(activityJobId)
        persistenceObject == null;
    }
    def 'unsubscribe for polling for upload backup when job is finished through handletimeout'(){
        given : "Details of the node, type and activity of the job"
        buildJobPO(false, PollingTestConstants.ACTIVITY_UPLOAD,"ECIM_BrM","2.3.0","RcsBrM")
        buildDataToSubscribeForPolling(SHMCapabilities.BACKUP_JOB_CAPABILITY, PollingTestConstants.ACTIVITY_UPLOAD, JobTypeEnum.BACKUP, "ECIM_BrM","2.3.0","RcsBrM")
        dspStatusInfoProvider.isDatabaseAvailable() >> true
        uploadBackupService.subscribeForPolling(activityJobId)
        when: "when job is finished after handle time-out"
        uploadBackupService.handleTimeout(activityJobId)
        then: "Verify whether the polling entry is deleted from DPS or not"
        final PersistenceObject persistenceObject = getPollingActivityPos(activityJobId)
        persistenceObject == null;
    }
    def 'unsubscribe for polling for create backup when job is finished either through notifications or polling'(){
        given : "Details of the node, type and activity of the job"
        buildJobPO(false, PollingTestConstants.ACTIVITY_CREATE,"ECIM_BrM","2.3.0","RcsBrM")
        buildDataToSubscribeForPolling(SHMCapabilities.BACKUP_JOB_CAPABILITY, PollingTestConstants.ACTIVITY_CREATE, JobTypeEnum.BACKUP, "ECIM_BrM","2.3.0","RcsBrM")
        dspStatusInfoProvider.isDatabaseAvailable() >> true
        createBackupService.subscribeForPolling(activityJobId)
        when: "when job is finished and unsubscribed for polling"
        pollingActivityManager.unsubscribeByPOId(poId)
        then: "Verify whether the polling entry is deleted from DPS or not"
        final PersistenceObject persistenceObject = getPollingActivityPos(activityJobId)
        persistenceObject == null;
    }
    def 'unsubscribe for polling for create backup when job is finished through processnotification'(){
        given : "Details of the node, type and activity of the job"
        buildJobPO(false, PollingTestConstants.ACTIVITY_CREATE,"ECIM_BrM","2.3.0","RcsBrM")
        buildDataToSubscribeForPolling(SHMCapabilities.BACKUP_JOB_CAPABILITY, PollingTestConstants.ACTIVITY_CREATE, JobTypeEnum.BACKUP, "ECIM_BrM","2.3.0","RcsBrM")
        dspStatusInfoProvider.isDatabaseAvailable() >> true
        createBackupService.subscribeForPolling(activityJobId)
        when: "when registered for process notification"
        createBackupService.processNotification(notification)
        then: "Verify whether the polling entry is deleted from DPS or not"
        final PersistenceObject persistenceObject = getPollingActivityPos(activityJobId)
        persistenceObject == null;
    }
    def 'unsubscribe for polling for create backup when job is finished through handletimeout'(){
        given : "Details of the node, type and activity of the job"
        buildJobPO(false, PollingTestConstants.ACTIVITY_CREATE,"ECIM_BrM","2.3.0","RcsBrM")
        buildDataToSubscribeForPolling(SHMCapabilities.BACKUP_JOB_CAPABILITY, PollingTestConstants.ACTIVITY_CREATE, JobTypeEnum.BACKUP, "ECIM_BrM","2.3.0","RcsBrM")
        dspStatusInfoProvider.isDatabaseAvailable() >> true
        createBackupService.subscribeForPolling(activityJobId)
        when: "when job is finished after handle time-out"
        createBackupService.handleTimeout(activityJobId)
        then: "Verify whether the polling entry is deleted from DPS or not"
        final PersistenceObject persistenceObject = getPollingActivityPos(activityJobId)
        persistenceObject == null;
    }
}
