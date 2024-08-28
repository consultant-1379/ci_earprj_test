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
package com.ericsson.oss.services.shm.es.impl.ecim.upgrade

import static org.junit.Assert.*

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.shm.es.api.ActivityStepResult
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult


class PrepareServiceActivityTest extends EcimUpgradeDataProvider {

    @ObjectUnderTest
    PrepareService prepareService;

    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
    }

    def 'When prepare action is triggered and action is successful'() {
        given:"Data for prepare Action"

        loadJobProperties(nodeName,"prepare")
        buildDataForPrepareAction(nodeName)
        getActionResultWhenActionTriggeredisSuccess()
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;

        when:"Prepare action is triggered and triggering is successful"
        prepareService.execute(activityJobId)

        then:"Check if action is triggered"
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("\"Prepare\" activity is triggered");
        List<Map<String, Object>> jobProperties = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("jobProperties")
        for (Map<String, Object> jobProperty :jobProperties ) {
            String jobPropertyValue = jobProperty.get(ShmJobConstants.VALUE)
            String jobPropertyKey = jobProperty.get(ShmJobConstants.KEY)
            if(jobPropertyKey.equals(ShmJobConstants.RESULT)) {
                jobPropertyValue = JobResult.SUCCESS.toString()
            }
        }
    }

    def 'When prepare action is triggered and action is failed'() {
        given:"Data for prepare Action"

        loadJobProperties(nodeName,"prepare")
        buildDataForPrepareAction(nodeName)
        getActionResultWhenActionTriggeredisFailed()
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;

        when:"Prepare action is triggered"
        prepareService.execute(activityJobId)

        then:"Check if prepare action is triggered and it is failed"
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("\"prepare\" activity has failed");
        List<Map<String, Object>> jobProperties = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("jobProperties")
        for (Map<String, Object> jobProperty :jobProperties ) {
            String jobPropertyValue = jobProperty.get(ShmJobConstants.VALUE)
            String jobPropertyKey = jobProperty.get(ShmJobConstants.KEY)
            if(jobPropertyKey.equals(ShmJobConstants.RESULT)) {
                jobPropertyValue = JobResult.FAILED.toString()
            }
        }
    }

    def 'When prepare action is triggered and user has no TBAC access'() {
        given:"Data for prepare Action"

        loadJobProperties(nodeName,"prepare")
        buildDataForPrepareAction(nodeName)
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> false;

        when:"Prepare action is triggered"
        prepareService.execute(activityJobId)

        then:"Check if job is failed as there is no TBAC access"

        List<Map<String, Object>> jobProperties = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("jobProperties")
        for (Map<String, Object> jobProperty :jobProperties ) {
            String jobPropertyValue = jobProperty.get(ShmJobConstants.VALUE)
            String jobPropertyKey = jobProperty.get(ShmJobConstants.KEY)
            if(jobPropertyKey.equals("actionTriggered")) {
                jobPropertyValue = "prepare"
            }
            if(jobPropertyKey.equals(ShmJobConstants.RESULT)) {
                jobPropertyValue = JobResult.FAILED.toString()
            }
        }
    }


    def 'When Notifications are not received for prepare activity and job went into handle timeout and it is successful'() {
        given:"Data for HandleTimoeut Prepare Action"

        loadJobProperties(nodeName,"prepare")
        buildDataForPrepareAction(nodeName)
        getAsyncActionProgressWhenJobisSuccess("Prepare")

        when:"Prepare action triggered didnot receive notifications"
        ActivityStepResult activityResult = prepareService.handleTimeout(activityJobId)

        then:"Check if Job is successful through Handle Timeout"

        activityResult.getActivityResultEnum().toString()== ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS.toString()
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("\"Prepare\" activity is completed successfully");
        List<Map<String, Object>> jobProperties = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("jobProperties")
        for (Map<String, Object> jobProperty :jobProperties ) {
            String jobPropertyValue = jobProperty.get(ShmJobConstants.VALUE)
            String jobPropertyKey = jobProperty.get(ShmJobConstants.KEY)
            if(jobPropertyKey.equals(ShmJobConstants.RESULT)) {
                jobPropertyValue = JobResult.SUCCESS.toString()
            }
        }
    }

    def 'When Notfications are received for Prepare Activity and Job is successful'() {
        given: "Data for Process Notification for Prepare Action"

        loadJobProperties(nodeName,"prepare")
        buildDataForPrepareAction(nodeName)
        getAsyncActionProgressWhenJobisSuccess("Prepare")

        when:"Prepare action has received notifications"
        prepareService.processNotification(notification)

        then: "Check if Job is succesful through notifications"

        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("\"Prepare\" activity is completed successfully");
        List<Map<String, Object>> jobProperties = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("jobProperties")
        for (Map<String, Object> jobProperty :jobProperties ) {
            String jobPropertyValue = jobProperty.get(ShmJobConstants.VALUE)
            String jobPropertyKey = jobProperty.get(ShmJobConstants.KEY)
            if(jobPropertyKey.equals(ShmJobConstants.RESULT)) {
                jobPropertyValue = JobResult.SUCCESS.toString()
            }
        }
    }

    def 'When Notfications are received for Prepare Activity and Job is failed as it is not successful on the node'() {
        given: "Data for Process Notification for Prepare Action"

        loadJobProperties(nodeName,"prepare")
        buildDataForPrepareAction(nodeName)
        getAsyncActionProgressWhenJobisFailed("Prepare")

        when:"Prepare action has received notifications"
        prepareService.processNotification(notification)

        then: "Check if Job is failed as it not successful on the node"

        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("\"Prepare\" activity has failed");
        List<Map<String, Object>> jobProperties = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("jobProperties")
        for (Map<String, Object> jobProperty :jobProperties ) {
            String jobPropertyValue = jobProperty.get(ShmJobConstants.VALUE)
            String jobPropertyKey = jobProperty.get(ShmJobConstants.KEY)
            if(jobPropertyKey.equals(ShmJobConstants.RESULT)) {
                jobPropertyValue = JobResult.FAILED.toString()
            }
        }
    
    }

    def 'When Notfications are received for Prepare Activity and Job is still running'() {
        given: "Data for Process Notification for Prepare Action"

        loadJobProperties(nodeName,"prepare")
        buildDataForPrepareAction(nodeName)
        getAsyncActionProgressWhenJobisFailed("Prepare")

        when:"Prepare action has received notifications and job is still running"
        prepareService.processNotification(notification)

        then: "Check if Job is failed as it is still running on the node after specified time out"

        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("\"Prepare\" activity has failed");
        List<Map<String, Object>> jobProperties = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("jobProperties")
        for (Map<String, Object> jobProperty :jobProperties ) {
            String jobPropertyValue = jobProperty.get(ShmJobConstants.VALUE)
            String jobPropertyKey = jobProperty.get(ShmJobConstants.KEY)
            if(jobPropertyKey.equals(ShmJobConstants.RESULT)) {
                jobPropertyValue = JobResult.FAILED.toString()
            }
        }
    
    }

    def 'When Notifications are not received for prepare activity and job went into handle timeout and it is failed' () {
        given:"Data for HandleTimoeut Prepare Action"

        loadJobProperties(nodeName,,"prepare")
        buildDataForPrepareAction(nodeName)
        getAsyncActionProgressWhenJobisFailed("Prepare")

        when:"Prepare action triggered didnot got notifications and went to handle timeout"
        ActivityStepResult activityResult = prepareService.handleTimeout(activityJobId)

        then:"Check if Job is failed as there is no it is not successful on the node"

        activityResult.getActivityResultEnum().toString()== ActivityStepResultEnum.TIMEOUT_RESULT_FAIL.toString()
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("\"Prepare\" activity has failed");
        List<Map<String, Object>> jobProperties = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("jobProperties")
        for (Map<String, Object> jobProperty :jobProperties ) {
            String jobPropertyValue = jobProperty.get(ShmJobConstants.VALUE)
            String jobPropertyKey = jobProperty.get(ShmJobConstants.KEY)
            if(jobPropertyKey.equals(ShmJobConstants.RESULT)) {
                jobPropertyValue = JobResult.FAILED.toString()
            }
        }
    
    }

    def 'When Notifications are not received for prepare activity and job went into handle timeout and it is still running'() {
        given:"Data for HandleTimoeut Prepare Action"

        loadJobProperties(nodeName,"prepare")
        buildDataForPrepareAction(nodeName)
        getAsyncActionProgressWhenJobisFailed("Prepare")

        when:"Prepare action triggered didnot got notifications and went to handle timeout"
        ActivityStepResult activityResult = prepareService.handleTimeout(activityJobId)

        then:"Check if Job is failed as it is still running on the node"
        activityResult.getActivityResultEnum().toString()== ActivityStepResultEnum.TIMEOUT_RESULT_FAIL.toString()
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("\"Prepare\" activity has failed");
        List<Map<String, Object>> jobProperties = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("jobProperties")
        for (Map<String, Object> jobProperty :jobProperties ) {
            String jobPropertyValue = jobProperty.get(ShmJobConstants.VALUE)
            String jobPropertyKey = jobProperty.get(ShmJobConstants.KEY)
            if(jobPropertyKey.equals(ShmJobConstants.RESULT)) {
                jobPropertyValue = JobResult.FAILED.toString()
            }
        }
    }

}
