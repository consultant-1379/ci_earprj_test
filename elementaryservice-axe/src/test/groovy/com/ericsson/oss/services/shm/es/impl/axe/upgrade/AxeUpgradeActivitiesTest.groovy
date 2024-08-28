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
package com.ericsson.oss.services.shm.es.impl.axe.upgrade

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.shm.common.InventoryQueryConfigurationProvider
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.common.modelservice.CapabilityProviderImpl
import com.ericsson.oss.services.shm.es.api.ActivityStepResult
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum
import com.ericsson.oss.services.shm.inventory.remote.axe.node.topology.api.AxeNodeTopologyRemoteService
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException
import com.ericsson.oss.services.shm.topology.axe.rest.api.AxeNodeTopologyResponse


public class AxeUpgradeActivitiesTest extends AxeAbstractUpgradeTest {

    @ObjectUnderTest
    private AxeUpgradeActivitiesService axeUpgradeActivitiesService;

    @MockedImplementation
    protected InventoryQueryConfigurationProvider inventoryQueryConfigurationListener;

    @MockedImplementation
    private AxeNodeTopologyRemoteService axeNodeTopologyRemoteService

    @MockedImplementation
    private CapabilityProviderImpl capabilityProviderImpl


    def ' upgrade package precheck verification tests when Success' () {

        given: "Preparing activityJobId and neJobStatic Data"
        buildPo();
        addNetworkElementMOs()
        AxeNodeTopologyResponse axeNodeTopologyResponse=prepareTopologyResponse();
        capabilityProviderImpl.getCapabilityValueForNeType(_,_) >> "MSC"
        buildActivityjobs("true","true","Success","Success","","","COMPLETED","COMPLETED","CREATED","CREATED","");
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)>>neJobStaticData
        jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())>>jobStaticData
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;
        inventoryQueryConfigurationListener.getNeFdnBatchSize() >> 10
        axeNodeTopologyRemoteService.getNodeTopologyAndApgInfo(_)>> axeNodeTopologyResponse

        when:  "Perform  upgrade package precheck for the allocated job"
        ActivityStepResult activityStepResult=axeUpgradeActivitiesService.precheck(activityJobId)

        then:  "verify Activity Step Result data"
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION)
    }


    def ' upgrade package precheck verification tests when tbac is failed' () {

        given: "Preparing activityJobId and neJobStatic Data"
        buildPo();
        buildActivityjobs("true","true","Success","Success","","","COMPLETED","COMPLETED","CREATED","CREATED","");
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)>>neJobStaticData
        jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())>>jobStaticData
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> false;

        when:  "Perform  upgrade package precheck for the allocated job"
        ActivityStepResult activityStepResult=axeUpgradeActivitiesService.precheck(activityJobId)

        then:  "verify Activity Step Result data"
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION)
    }

    def ' upgrade package precheck verification tests when activity details are not populated' () {

        given: "Preapring activityJobId and neJobStatic Data"
        buildPo();
        buildActivityjobs("true","true","Success","Success","","","COMPLETED","COMPLETED","CREATED","CREATED","");
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)>>neJobStaticData
        jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())>>jobStaticData
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> {throw new JobDataNotFoundException("")};

        when:  "Perform  upgrade package precheck for the allocated job"
        ActivityStepResult activityStepResult=axeUpgradeActivitiesService.precheck(activityJobId)

        then:  "verify Activity Step Result data"
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION)
    }

    def ' upgrade package precheck verification tests when exception occured during TBAC validation for activity' () {

        given: "networkElement Details"
        buildPo();
        buildActivityjobs("true","true","Success","Success","","","COMPLETED","COMPLETED","CREATED","CREATED","");
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)>>neJobStaticData
        jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())>>jobStaticData
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> {throw new JobDataNotFoundException("MediationServiceException")};

        when:  "Perform  upgrade package precheck for the allocated job"
        ActivityStepResult activityStepResult=axeUpgradeActivitiesService.precheck(activityJobId)

        then:  "verify Activity Step Result data"
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION)
    }

    def 'Verify cancel job is triggered for activity job id'(){

        given : "building activity job details"
        buildPo();
        buildActivityjobs("true","true","","","","","","","","","");
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)>>neJobStaticData

        when :"Perform cancel job for activity job id"
        ActivityStepResult activityStepResult=axeUpgradeActivitiesService.cancel(activityJobId)

        then :"Check cancel job is triggered for activity job id"
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.EXECUTION_SUCESS)
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("Cancel request received")
    }

    def 'Verify cancel job is triggered or not if NeJob details are not getting from DB'(){

        given : "building activity job details"
        buildPo();
        buildActivityjobs("true","true","","","","","","","","","");
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)>>null

        when :"Perform cancel job for activity job id"
        ActivityStepResult activityStepResult=axeUpgradeActivitiesService.cancel(activityJobId)

        then :"Check cancel job is triggered for activity job id"
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.EXECUTION_FAILED)
    }

    def 'Verify cancel time out is triggered or not for given activity job id'(){

        given : "building activity job details"
        buildPo();
        buildActivityjobs("true","true","","","","","","","","","");
        boolean isCancelTimeoutExhausted = true
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)>>neJobStaticData

        when :"Perform cancel job for activity job id"
        ActivityStepResult activityStepResult=axeUpgradeActivitiesService.cancelTimeout(activityJobId,isCancelTimeoutExhausted)

        then :"Check cancel job is triggered for activity job id"
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL)
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("Failing \"CANCEL_UPGRADE\" Activity as it is taking more than expected time")
    }

    def 'upgrade package precheck verification tests when Cache preparation is failed' () {

        given: "Preparing activityJobId and neJobStatic Data"
        buildPo();
        addNetworkElementMOs()
        AxeNodeTopologyResponse axeNodeTopologyResponse=prepareTopologyFailureResponse();
        capabilityProviderImpl.getCapabilityValueForNeType(_,_) >> "MSC"
        buildActivityjobs("true","true","Success","Success","","","COMPLETED","COMPLETED","CREATED","CREATED","");
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)>>neJobStaticData
        jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())>>jobStaticData
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;
        inventoryQueryConfigurationListener.getNeFdnBatchSize() >> 10
        axeNodeTopologyRemoteService.getNodeTopologyAndApgInfo(_)>> axeNodeTopologyResponse

        when:  "Perform  upgrade package precheck for the allocated job"
        ActivityStepResult activityStepResult=axeUpgradeActivitiesService.precheck(activityJobId)

        then:  "verify Activity Step Result data"
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION)
    }
}
