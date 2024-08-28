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
package com.ericsson.oss.services.shm.es.impl.cpp.deleteupgrade;

import static org.mockito.Matchers.*

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.shm.common.*
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.es.api.*
import com.ericsson.oss.services.shm.es.impl.cpp.common.*
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.*
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.handlers.ExecuteHandler

public class ExecuteHandlerTest extends CPPAbstractDeleteUpTest {

    @ObjectUnderTest
    private ExecuteHandler executeHandler;

    def setup() {
        stubbedCppUPConfigurationService.buildJobPO("6","true");
        stubbedCppUPConfigurationService.addNetworkElementMOs(null,nodeName);
        addUpgradepackageOnNode();
        inventoryQueryConfigurationProvider.getNeFdnBatchSize() >> 3;
        List<String> preventingCvs=new ArrayList<String>();
        addAdminDataOnNode("CXP102051_1_R4D21","3",preventingCvs);
        addAdminDataOnNode("CXP102051_1_R4D22","6",preventingCvs);
        addAdminDataOnNode("CXP102051_1_R4D23","4",preventingCvs);
        nodeModelNameSpaceProvider.getNamespaceByNodeName(nodeName) >> "OSS_NE_DEF";
    }

    def 'delete upgrade package plain up removal test' () {

        given: "node/network element and the upgrade package details"
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY) >> neJobStaticData;
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;

        when: "Perform remove upgrade package execute for the allocated job"
        executeHandler.execute(activityJobId, jobActivityInfo);

        then: "upgrade package is removed on the node"
        runtimeDps.stubbedDps.liveBucket.findMoByFdn(deletableUPFdn)==null;
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("\"Delete Upgrade Package\" activity is completed successfully.");
    }
}
