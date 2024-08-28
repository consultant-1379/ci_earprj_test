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
package com.ericsson.oss.services.shm.es.impl.cpp.polling

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.services.shm.es.impl.cpp.backup.UploadCvService
import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.UpgradeService
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants

class ProcessPollingResponseTest extends AbstractPollingData{

    @ObjectUnderTest
    private UpgradeService upgradeService;

    @ObjectUnderTest
    private UploadCvService uploadCvService;

    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps)

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
    }

    def 'Verify polling response for upgrade activity when action is Success on the node'(){
        given : "Activity Job Id and response received from mediation"
        activityName = ACTIVITY_NAME_UPGRADE;
        buildResponseDataForUpgrade(UP_STATE_AWAITING_CONFIRMATION)
        buildJobPO(false);
        when: "Response is recieved from mediation"
        upgradeService.processPollingResponse(activityJobId, responseAttributes)
        then: "Verify upgrade activity is finised through polling or not."
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("\"Upgrade\" activity is completed successfully");
    }

    def 'Verify polling response for upgrade activity when action is in progress on the node'(){
        given : "Activity Job Id and response received from mediation"
        activityName = ACTIVITY_NAME_UPGRADE;
        buildResponseDataForUpgrade(UP_STATE_VERIFY_EXECUTING)
        buildJobPO(false);
        when: "Response is recieved from mediation"
        upgradeService.processPollingResponse(activityJobId, responseAttributes)
        then: "Verify whether activity progress logs are persisted in database or not."
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("UpgradePackage header : \"a verify upgrade trigger has been sent to the participating AUEs so that they can do their task in sequence with built in pre-checks performed by the SU logic.\"");
    }

    def 'Verify polling response for upgrade activity when activity is already completed'(){
        given : "Activity Job Id and response received from mediation"
        activityName = ACTIVITY_NAME_UPGRADE;
        buildResponseDataForUpgrade(UP_STATE_AWAITING_CONFIRMATION)
        buildJobPO(true);
        when: "Response is recieved from mediation"
        upgradeService.processPollingResponse(activityJobId, responseAttributes)
        then: "Verify polling response is not processed when activity is already completed"
        0 * activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, activityName, processVariables)
    }

    def 'Verify polling response for upload activity when action is Success on the node'(){
        given : "Activity Job Id and response received from mediation"
        activityName = ACTIVITY_NAME_UPLOAD;
        buildResponseDataForBackup(CURRENTDETAILEDACTIVITY_IDLE, MAIN_ACTION_RESULT_EXECUTED)
        buildJobPO(false);
        when: "Response is recieved from mediation"
        uploadCvService.processPollingResponse(activityJobId, responseAttributes)
        then: "Verify uploadcv activity is finised through polling or not."
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("\"Upload Configuration Version\" activity for the CV \"testbackup1\" is completed successfully.");
    }

    def 'Verify polling response for upload activity when action is in progress on the node'(){
        given : "Activity Job Id and response received from mediation"
        activityName = ACTIVITY_NAME_UPLOAD;
        buildResponseDataForBackup(CURRENTDETAILEDACTIVITY_IDLE, MAIN_ACTION_RESULT_EXECUTING)
        buildJobPO(false);
        when: "Response is recieved from mediation"
        uploadCvService.processPollingResponse(activityJobId, responseAttributes)
        then: "Verify whether activity progress logs are persisted in database or not."
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("The CV function is idle.");
        0 * activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, ShmConstants.WFS_ACTIVATE_EXECUTE, processVariables);
    }

    def 'Verify polling response for upload activity when activity is already completed'(){
        given : "Activity Job Id and response received from mediation"
        activityName = ACTIVITY_NAME_UPLOAD;
        buildResponseDataForBackup(CURRENTDETAILEDACTIVITY_IDLE, MAIN_ACTION_RESULT_EXECUTED)
        buildJobPO(true);
        when: "Response is recieved from mediation"
        upgradeService.processPollingResponse(activityJobId, responseAttributes)
        then: "Verify polling response is not processed when activity is already completed"
        0 * activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, ShmConstants.WFS_ACTIVATE_EXECUTE, processVariables);
    }
}
