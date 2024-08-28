/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.axe.upgrade

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities

public class AxeProcessNotificationTest extends AxeAbstractUpgradeTest {

    @ObjectUnderTest
    private AxeUpgradeActivitiesService axeUpgradeActivitiesService;


    def "Process notifications received from OPS" () {

        given: "Preparing activityJobId and neJobStatic Data"
        buildPo();
        buildActivityjobs(timeout ,isCancelTriggered,"Success","Success","","","COMPLETED","COMPLETED","CREATED","CREATED","");
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)>>neJobStaticData

        when: "OpsResponse attributes are given"
        axeUpgradeActivitiesService.processNotification(getOpsResponseAttributes(status,progressPercentage))

        then: "Verify expected log message"
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains(expected)

        where :
        isCancelTriggered |  timeout  |    status               | progressPercentage     | expected
        "true"            |  "false"  |   "NOT_STARTED"         |      0.0d              | "Activity \"activity1\" is in state \"NOT_STARTED\" with process percentage \"0.000000\""
        "true"            |  "false"  |   "INTERRUPTED"         |      50.0d              | "Activity \"activity1\" is in state \"INTERRUPTED\" with process percentage \"50.000000\""
        "true"            |  "false"  |   "FINISHED"            |      0.0d              | "Script execution is completed successfully for activity"
        "true"            |  "false"  |   "WAITING_FOR_INPUT"   |      0.0d              | "Activity \"activity1\" is in state \"WAITING_FOR_INPUT\" with process percentage \"0.000000\""
        "true"            |  "false"  |   "RUNNING"             |     10.0d              | "Activity \"activity1\" is in state \"RUNNING\" with process percentage \"10.000000\""
        "true"            |  "false"  |   "STOPPED"             |     -1.0d              | "activity is cancelled successfully"
        "true"            |  "false"  |   "STOPPED"             |     10.0d              | "Improper progress percentage received for STOPPED status from OPS on cancel Triggred"
        "false"           |  "false"  |   "INTERRUPTED"         |     50.0d              | "Activity \"activity1\" is in state \"INTERRUPTED\" with process percentage \"50.000000\""
        "false"           |  "true"   |   "INTERRUPTED"         |     60.0d              | "Received \"activity1\" Status : \"INTERRUPTED\" and  process percentage : \"60.000000\" for upgrade_status request"
        "false"           |  "false"  |   "STOPPED"             |    10.0d               | "Script execution is completed successfully for activity"
        "false"           |  "true"   |   "STOPPED"             |    0.0d                | "Script execution is completed successfully for activity"
        "false"           |  "false"  |   "STOPPED"             |    -1.0d               |"Script execution is failed for activity : \'activity1\'"
        "true"            |  "false"  |   "FAILED"              |     30.0d              | "Script execution is failed"
        "true"            |  "false"  |   "INVALID_STATUS"      |     10.0d              | "Received unknown or invalid Status"
        "false"           |  "true"   |   "RUNNING"             |     10.0d              | "Received \"activity1\" Status : \"RUNNING\" and  process percentage : \"10.000000\" for upgrade_status request"
        "false"           |  "true"   |   "FINISHED"            |    100.0d              | "Script execution is completed successfully for activity"
        "false"           |  "true"   |   "FAILED"              |     20.0d              | "Script execution is failed for activity : \'activity1\'"
        "false"           |  "true"   |   "NOT_STARTED"         |      0.0d              | "Received \"activity1\" Status : \"NOT_STARTED\" and  process percentage : \"0.000000\" for upgrade_status request"
        "false"           |  "true"   |   "WAITING_FOR_INPUT"   |      0.0d              | "Received \"activity1\" Status : \"WAITING_FOR_INPUT\" and  process percentage : \"0.000000\" for upgrade_status request"
        "false"           |  "true"   |    "STOP"               |      0.0d              | "Received unknown or invalid Status \"STOP\""
        "false"           |  "false"  |   "RUNNING"             |     10.0d              | "Activity \"activity1\" is in state \"RUNNING\" with process percentage \"10.000000\""
        "false"           |  "false"  |   "FINISHED"            |    100.0d              | "Script execution is completed successfully for activity"
        "false"           |  "false"  |   "FAILED"              |     20.0d              | "Script execution is failed for activity : \'activity1\'"
        "false"           |  "false"  |   "NOT_STARTED"         |      0.0d              | "Activity \"activity1\" is in state \"NOT_STARTED\" with process percentage \"0.000000\""
        "false"           |  "false"  |   "WAITING_FOR_INPUT"   |      0.0d              | "Activity \"activity1\" is in state \"WAITING_FOR_INPUT\" with process percentage \"0.000000\""
        "false"           |  "false"  |    "STOP"               |      0.0d              | "Received unknown or invalid Status \"STOP\""
        "false"           |   null    |   "RUNNING"             |     10.0d              | "Activity \"activity1\" is in state \"RUNNING\" with process percentage \"10.000000\""
        "false"           |   null    |   "FINISHED"            |    100.0d              | "Script execution is completed successfully for activity"
        "false"           |   null    |   "NOT_STARTED"         |      0.0d              | "Activity \"activity1\" is in state \"NOT_STARTED\" with process percentage \"0.000000\""
        "false"           |   null    |   "WAITING_FOR_INPUT"   |      0.0d              | "Activity \"activity1\" is in state \"WAITING_FOR_INPUT\" with process percentage \"0.000000\""
        "false"           |   null    |    "STOP"               |      0.0d              | "Received unknown or invalid Status \"STOP\""
    }
}
