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
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.es.impl.ecim.backup.CreateBackupService
import com.ericsson.oss.services.shm.es.impl.ecim.backup.UploadBackupService
import com.ericsson.oss.services.shm.es.impl.ecim.upgrade.ActivateService
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConstants
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum

public class SubscribeForPollingRequestTest extends AbstractPollingData {

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

    def 'subscribe for polling when job progress notifications are getting delayed'(){
        given : "Details of the node and type of the job and activity of the job"
        buildJobPO(false, PollingTestConstants.ACTIVITY_ACTIVATE,"ECIM_SwM","4.2.0","ECIM_SwM")
        buildDataToSubscribeForPolling(SHMCapabilities.UPGRADE_JOB_CAPABILITY, PollingTestConstants.ACTIVITY_ACTIVATE, JobTypeEnum.UPGRADE, "ECIM_SwM","4.2.0","ECIM_SwM")
        dspStatusInfoProvider.isDatabaseAvailable() >> true
        when: "subscribe for polling when notifications are delayed"
        activateService.subscribeForPolling(activityJobId)
        then: "Verify moFdn, mimVersion, namespace and pollCycleStatus are persisted in the DPS or not"
        final PersistenceObject persistenceObject = getPollingActivityPos(activityJobId)
        persistenceObject.attributes.get(PollingActivityConstants.MO_FDN).equals(PollingTestConstants.upMoFdn)
        persistenceObject.attributes.get(PollingActivityConstants.MIM_VERSION).equals("4.2.0")
        persistenceObject.attributes.get(PollingActivityConstants.NAMESPACE).equals("ECIM_SwM")
        persistenceObject.attributes.get(PollingActivityConstants.POLL_CYCLE_STATUS).equals("READY")
    }
    def 'subscribe for polling for upload backup when job progress notifications are getting delayed'() {
        given : "Details of node, type and activity of the job"
        buildJobPO(false, PollingTestConstants.ACTIVITY_UPLOAD,"ECIM_BrM","2.3.0","RcsBrM")
        buildDataToSubscribeForPolling(SHMCapabilities.BACKUP_JOB_CAPABILITY, PollingTestConstants.ACTIVITY_UPLOAD, JobTypeEnum.BACKUP, "ECIM_BrM","2.3.0","RcsBrM")
        dspStatusInfoProvider.isDatabaseAvailable() >> true
        when: "subscribed for polling for upload backup"
        uploadBackupService.subscribeForPolling(activityJobId)
        then: "verify polling attributes moFdn, mimVersion, namespace and pollCycleStatus are persisted in the DPS or not"
        final PersistenceObject persistenceObject = getPollingActivityPos(activityJobId)
        persistenceObject.attributes.get(PollingActivityConstants.MO_FDN).equals(PollingTestConstants.brmBackupMoFdn)
        persistenceObject.attributes.get(PollingActivityConstants.MIM_VERSION).equals("2.3.0")
        persistenceObject.attributes.get(PollingActivityConstants.NAMESPACE).equals("RcsBrM")
        persistenceObject.attributes.get(PollingActivityConstants.POLL_CYCLE_STATUS).equals("READY")
    }
    def 'subscribe for polling for create backup when job progress notifications are getting delayed'() {
        given : "Details of node, type and activity of the job"
        buildJobPO(false, PollingTestConstants.ACTIVITY_CREATE,"ECIM_BrM","2.3.0","RcsBrM")
        buildDataToSubscribeForPolling(SHMCapabilities.BACKUP_JOB_CAPABILITY, PollingTestConstants.ACTIVITY_CREATE, JobTypeEnum.BACKUP, "ECIM_BrM","2.3.0","RcsBrM")
        dspStatusInfoProvider.isDatabaseAvailable() >> true
        when: "subscribed for polling for create backup"
        createBackupService.subscribeForPolling(activityJobId)
        then: "verify polling attributes moFdn, mimVersion, namespace and pollCycleStatus are persisted in the DPS or not"
        final PersistenceObject persistenceObject = getPollingActivityPos(activityJobId)
        persistenceObject.attributes.get(PollingActivityConstants.MO_FDN).equals(PollingTestConstants.brmBackupMoFdn)
        persistenceObject.attributes.get(PollingActivityConstants.MIM_VERSION).equals("2.3.0")
        persistenceObject.attributes.get(PollingActivityConstants.NAMESPACE).equals("RcsBrM")
        persistenceObject.attributes.get(PollingActivityConstants.POLL_CYCLE_STATUS).equals("READY")
    }
    def 'add polling entries to cache for upload backup when dps is not available'() {
        given : "Details of node, type and activity of the job"
        buildJobPO(false, PollingTestConstants.ACTIVITY_UPLOAD,"ECIM_BrM","2.3.0","RcsBrM")
        buildDataToSubscribeForPolling(SHMCapabilities.BACKUP_JOB_CAPABILITY, PollingTestConstants.ACTIVITY_UPLOAD, JobTypeEnum.BACKUP, "ECIM_BrM","2.3.0","RcsBrM")
        dspStatusInfoProvider.isDatabaseAvailable() >> false
        when: "DPS is not available before preparing polling data"
        uploadBackupService.subscribeForPolling(activityJobId)
        then: "verify whether polling activity data is added to cache or not"
        pollingActivityCacheManager.getPollingEntriesFromCache().get(0).getActivityJobId().equals(activityJobId)
    }
    def 'add polling entries to cache for create backup when dps is not available'() {
        given : "Details of node, type and activity of the job"
        buildJobPO(false, PollingTestConstants.ACTIVITY_CREATE,"ECIM_BrM","2.3.0","RcsBrM")
        buildDataToSubscribeForPolling(SHMCapabilities.BACKUP_JOB_CAPABILITY, PollingTestConstants.ACTIVITY_CREATE, JobTypeEnum.BACKUP, "ECIM_BrM","2.3.0","RcsBrM")
        dspStatusInfoProvider.isDatabaseAvailable() >> false
        when: "DPS is not available before preparing polling data"
        createBackupService.subscribeForPolling(activityJobId)
        then: "verify whether polling activity data is added to cache or not"
        pollingActivityCacheManager.getPollingEntriesFromCache().get(0).getActivityJobId().equals(activityJobId)
    }
    def 'add polling entries to cache after subscribe method is called and dps is not available'() {
        given : "Details of node, type and activity of the job"
        buildJobPO(false, PollingTestConstants.ACTIVITY_UPLOAD,"ECIM_BrM","2.3.0","RcsBrM")
        buildDataToSubscribeForPolling(SHMCapabilities.BACKUP_JOB_CAPABILITY, PollingTestConstants.ACTIVITY_UPLOAD, JobTypeEnum.BACKUP, "ECIM_BrM","2.3.0","RcsBrM")
        dspStatusInfoProvider.isDatabaseAvailable() >> false
        when: "DPS is not available after subscribe for polling"
        pollingActivityManager.subscribe(jobActivityInfo, networkElementData, "ECIM_BrM", PollingTestConstants.brmBackupMoFdn, moAttributes)
        then: "verify whether polling activity data is added to cache or not"
        pollingActivityCacheManager.getPollingEntriesFromCache().get(0).getActivityJobId().equals(activityJobId)
    }
}
