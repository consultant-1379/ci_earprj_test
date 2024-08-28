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
import com.ericsson.oss.services.shm.es.upgrade.remote.api.RemoteSoftwarePackageManager
import com.ericsson.oss.services.shm.inventory.remote.axe.node.topology.api.AxeNodeTopologyRemoteService
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData
import com.ericsson.oss.services.shm.topology.axe.rest.api.AxeNodeTopologyResponse

public class AxeExecuteUpgradeTest extends AxeAbstractUpgradeTest{

    @ObjectUnderTest
    private AxeUpgradeActivitiesService axeUpgradeActivitiesService;

    @MockedImplementation
    protected InventoryQueryConfigurationProvider inventoryQueryConfigurationListener;

    @MockedImplementation
    private CapabilityProviderImpl capabilityProviderImpl

    @MockedImplementation
    private RemoteSoftwarePackageManager remoteSoftwarePackageManager

    @MockedImplementation
    private AxeNodeTopologyRemoteService axeNodeTopologyRemoteService


    def 'Execute Upgrade Package Test for Success case'(){

        given:"Creating activityJobId , MainJobId , NeJobId , TemplateId and JobConfigurationDetails Data"
        buildPo()
        AxeNodeTopologyResponse axeNodeTopologyResponse=prepareTopologyResponse();
        buildActivityjobs("true","true","Success","Success","","","COMPLETED","COMPLETED","CREATED","CREATED","");
        final List<String> filePath= new ArrayList<>();
        filePath.add("\01hw\134545")
        addNetworkElementMOs()
        inventoryQueryConfigurationListener.getNeFdnBatchSize() >> 10
        capabilityProviderImpl.getCapabilityValueForNeType(_,_) >> "MSC"
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)>>neJobStaticData
        remoteSoftwarePackageManager.getSoftwarPackagePath(_) >> filePath
        jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())>>jobStaticData
        axeNodeTopologyRemoteService.getNodeTopologyAndApgInfo(_)>> axeNodeTopologyResponse

        when:"Perform execute upgrade for allocated job and update running JobAttributes"
        axeUpgradeActivitiesService.execute(activityJobId);

        then: "check the execution job is submitted"
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("execution is submitted by the user")
    }

    def 'Execute Upgrade Package without providing activityId to test Failure case '(){

        given:"Creating activityId for failure case"
        buildPo()
        buildActivityjobs("true","true","Success","Success","","","COMPLETED","COMPLETED","CREATED","CREATED","");
        def activityJobIdForException = 0

        when:"Perform execute upgrade for allocated job and update running JobAttributes"
        axeUpgradeActivitiesService.execute(activityJobIdForException);

        then: "check the execution job is submitted"
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage")==null
    }

    def 'Execute Upgrade Package  providing wrong neJobStaticData to test Failure case '(){

        given:"Creating neJobStaticData for failure case"
        buildPo()
        buildActivityjobs("true","true","Success","Success","","","COMPLETED","COMPLETED","CREATED","CREATED","");
        def NEJobStaticData neJobStaticData = new NEJobStaticData(1L, 1L, "MSC-BC-BSP-01__BC01", "1234", "AXE", 1L,"MSC-BC-BSP-01");
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)>>neJobStaticData

        when:"Perform execute upgrade for allocated job and update running JobAttributes"
        axeUpgradeActivitiesService.execute(activityJobId);

        then: "check the execution job is submitted"
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("Failed to submit script execution")
    }
}
