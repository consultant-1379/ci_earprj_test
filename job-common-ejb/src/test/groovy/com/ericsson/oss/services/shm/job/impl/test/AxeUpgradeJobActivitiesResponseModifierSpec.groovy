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
package com.ericsson.oss.services.shm.job.impl.test

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.shm.filestore.swpackage.api.SoftwarePackage
import com.ericsson.oss.services.shm.filestore.swpackage.impl.axe.AxeSoftwarePackage
import com.ericsson.oss.services.shm.job.impl.AxeUpgradeJobActivitiesResponseModifier
import com.ericsson.oss.services.shm.jobs.common.api.*
import com.ericsson.oss.services.shm.swpackage.query.api.SoftwarePackageQueryService

public class AxeUpgradeJobActivitiesResponseModifierSpec extends SoftwarePackageData {

    @ObjectUnderTest
    private AxeUpgradeJobActivitiesResponseModifier axeUpgradeJobActivitiesResponseModifier

    @MockedImplementation
    private SoftwarePackageQueryService softwarePackageQueryServiceMock;

    def JobActivitiesResponse jobActivitiesResponse= new JobActivitiesResponse()


    final Map<String, SoftwarePackage> softwarePackageDataMap = new HashMap<String, SoftwarePackage>()

    def "Verify upgrade job activities response for AXE Nodes"() {
        given: "NeInfoQuery and Software package information"
        final NeInfoQuery neInfoQuery = getNeInfoQuery()
        final AxeSoftwarePackage axeSoftwarePackage = new AxeSoftwarePackage(buildSoftwarePackagePO())
        final SoftwarePackage softwarePackage = new SoftwarePackage(axeSoftwarePackage)
        softwarePackageDataMap.put("MSS18A_CM02_DB_UPGRADE_PA3",softwarePackage)
        softwarePackageQueryServiceMock.getSoftwarePackagesBasedOnPackageName(_) >> softwarePackageDataMap
        when: "Job Activities are fetched for AXE nodes"
        jobActivitiesResponse = axeUpgradeJobActivitiesResponseModifier.getUpdatedJobActivities(neInfoQuery,jobActivitiesResponse)
        then: "Verify Job Activities Response"
        jobActivitiesResponse.getUnsupportedNeTypes().equals(null)
        jobActivitiesResponse.getNeActivityInformation().getAt(0).getNeType().equals("MSC-DB")
        jobActivitiesResponse.getNeActivityInformation().getAt(0).getActivity().get(0).getActivityParams().get(0).getParam().get(0).getName().equals("_INTERACTION_LEVEL")
        jobActivitiesResponse.getNeActivityInformation().getAt(0).getActivity().get(0).getActivityParams().get(0).getParam().get(0).getDescription().equals("Top Down Menu")
        jobActivitiesResponse.getNeActivityInformation().getAt(0).getActivity().get(0).getActivityParams().get(0).getParam().get(0).getDefaultValue().equals("Display all message types")
        jobActivitiesResponse.getNeActivityInformation().getAt(0).getActivity().get(0).getActivityParams().get(0).getParam().get(0).getPrompt().equals("User Interaction level")
        jobActivitiesResponse.getNeActivityInformation().getAt(0).getActivity().get(0).getName().equals("Health Check")
        jobActivitiesResponse.getNeActivityInformation().getAt(0).getActivityParams().get(0).getParam().get(0).getName().equals("_PACKAGE_NAME")
        jobActivitiesResponse.getNeActivityInformation().getAt(0).getActivityParams().get(0).getParam().get(0).getDescription().equals("")
        jobActivitiesResponse.getNeActivityInformation().getAt(0).getActivityParams().get(0).getParam().get(0).getDefaultValue().equals("MSS18A_CM02_BC_UPGRADE_PA3")
        jobActivitiesResponse.getNeActivityInformation().getAt(0).getActivityParams().get(0).getParam().get(0).getPrompt().equals("Package Directory name")
        jobActivitiesResponse.getNeActivityInformation().getAt(0).getActivitySelection().get(0).getValid().equals(null)
        jobActivitiesResponse.getNeActivityInformation().getAt(0).getActivitySelection().get(0).getInvalid().equals(null)
    }

    def "Verify upgrade job activities response for AXE Nodes with out having Software Package Name in request json"() {
        given: "NeInfoQuery and Software package information"
        final NeInfoQuery neInfoQuery = getNeInfoQuerywithoutSoftwarePkgName()
        when: "Job Activities are fetched for AXE nodes"
        jobActivitiesResponse = axeUpgradeJobActivitiesResponseModifier.getUpdatedJobActivities(neInfoQuery,jobActivitiesResponse)
        then: "Verify Job Activities Response"
        jobActivitiesResponse.getUnsupportedNeTypes().get("MSC-DB").equals("No Software package is selected for neType \"MSC-DB\".")
    }

    def "Verify upgrade job activities response for AXE Nodes if given software Package does not exist in DataBase"() {
        given: "NeInfoQuery and Software package information"
        final NeInfoQuery neInfoQuery = getNeInfoQuery()
        final Map<String, SoftwarePackage> softwarePackageDataMap = new HashMap<String, SoftwarePackage>()
        softwarePackageQueryServiceMock.getSoftwarePackagesBasedOnPackageName(_) >> softwarePackageDataMap
        when: "Job Activities are fetched for AXE nodes"
        jobActivitiesResponse = axeUpgradeJobActivitiesResponseModifier.getUpdatedJobActivities(neInfoQuery,jobActivitiesResponse)
        then: "Verify Job Activities Response"
        jobActivitiesResponse.getUnsupportedNeTypes().get("MSC-DB").equals("Package Data for \"MSS18A_CM02_DB_UPGRADE_PA3\" doesn't exist in the database.")
    }

    public NeInfoQuery getNeInfoQuery(){
        final NeInfoQuery neInfoQuery = new NeInfoQuery()
        final NeParams neParam = new NeParams()
        List<String> NeFdns = new ArrayList<String>()
        List<NeParams> params = new ArrayList<String>()
        NeFdns.add("MSC-DB-BSP-18A-V201")
        neParam.setName("SWP_NAME")
        neParam.setValue("MSS18A_CM02_DB_UPGRADE_PA3")
        params.add(neParam)
        neInfoQuery.setNeType("MSC-DB")
        neInfoQuery.setNeFdns(NeFdns)
        neInfoQuery.setParams(params)
        return neInfoQuery;
    }
    public NeInfoQuery getNeInfoQuerywithoutSoftwarePkgName(){
        final NeInfoQuery neInfoQuery = new NeInfoQuery()
        final NeParams neParam = new NeParams()
        List<String> NeFdns = new ArrayList<String>()
        List<NeParams> params = new ArrayList<String>()
        NeFdns.add("MSC-DB-BSP-18A-V201")
        params.add(neParam)
        neInfoQuery.setNeType("MSC-DB")
        neInfoQuery.setNeFdns(NeFdns)
        neInfoQuery.setParams(params)
        return neInfoQuery;
    }
}

