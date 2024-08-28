/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.ecim.deleteup

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.providers.custom.ejb.EjbProxyController
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest

class ExecuteHandlerTest extends EcimAbstractDeleteUpTest {

    @ObjectUnderTest
    ExecuteHandler executeHandler;

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
        injectionProperties.autoLocateFrom("com.ericsson.oss.itpf.modeling.modelservice")
        injectionProperties.addProxyController(new EjbProxyController(true))
    }

    def 'do not attempt to remove upgrade package when there is only one inactive UP' () {
        given: "node/network element and the upgrade package details"
        loadJobProperties(inputProductNumber, inputProductRevision, deleteReferredBackupsTrue, "true")
        def List<Map<String,String>> productDataList = prepareInputProductData(inputProductNumber, inputProductRevision);
        buildNodeAndUpgradepackageDataForRefferedBackup1(productDataList,deleteReferredBackupsTrue);
        swMHandler.removeUpgradePackageAction(_ as String,_ as String,_ as Map) >> true;
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;
        final List<Map<String, Object>> jobLogs = new ArrayList<>();

        when: "Perform remove upgrade package timeout method for the allocated job after remove upgrade package execute method1"
        executeHandler.execute(activityJobId, jobActivityInfo);

        then: "remove upgrade package action not triggred"
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("Delete Upgrade Package\" activity is skipped. Since only one inactive Upgrade Package is available");
    }

    def 'remove upgrade package action on single up success test' () {
        given: "node/network element and the upgrade package details"
        loadJobProperties(inputProductNumber, inputProductRevision, deleteReferredBackups, null)
        def List<Map<String,String>> productDataList = prepareInputProductData(inputProductNumber, inputProductRevision);
        buildNodeAndUpgradepackageData(productDataList,deleteReferredBackups);
        swMHandler.removeUpgradePackageAction(_ as String,_ as String,_ as Map) >> true;
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;

        when: "Perform remove upgrade package execute for the allocated job"
        executeHandler.execute(activityJobId, jobActivityInfo);

        then: "remove upgrade package action is triggerd on the node"
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("\"Delete Upgrade Package\" activity is triggered");
    }

    def 'remove upgrade package action on single up fail test' () {
        given: "node/network element and the upgrade package details"
        loadJobProperties(inputProductNumber, inputProductRevision, deleteReferredBackups, null)
        def List<Map<String,String>> productDataList = prepareInputProductData(inputProductNumber, inputProductRevision);
        buildNodeAndUpgradepackageData(productDataList,deleteReferredBackups);
        swMHandler.removeUpgradePackageAction(_ as String,_ as String,_ as Map) >> false;
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;

        when: "Perform remove upgrade package execute for the allocated job"
        executeHandler.execute(activityJobId, jobActivityInfo);

        then: "remove upgrade package action is failed on the node"
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("Unable to trigger \"Delete Upgrade Package\" activity");
    }
}
