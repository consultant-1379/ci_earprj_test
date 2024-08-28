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

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.es.api.*
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.handlers.PreCheckHandler
import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.UpgradePackageState
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants

public class PreCheckHandlerTest extends CPPAbstractDeleteUpTest {

    @ObjectUnderTest
    private PreCheckHandler preCheckHandler;

    def setup() {
        inventoryQueryConfigurationProvider.getNeFdnBatchSize() >> 3;
        nodeModelNameSpaceProvider.getNamespaceByNodeName(nodeName) >> "OSS_NE_DEF";
        final Map<String, Object> neJobAttributesMap=new HashMap<String, Object>();
        neJobAttributesMap.putAt(ShmConstants.NE_NAME,nodeName);
    }

    def configureMos() {
        stubbedCppUPConfigurationService.addNetworkElementMOs(null,nodeName);
        List<String> preventingCvs=new ArrayList<String>();
        preventingCvs.add("CV1");
        preventingCvs.add("CV2");
        addAdminDataOnNode("CXP102051_1_R4D21","3",preventingCvs, UpgradePackageState.NOT_INSTALLED.name());
        addAdminDataOnNode("CXP102051_1_R4D22","6",preventingCvs, UpgradePackageState.UPGRADE_COMPLETED.name());
        addAdminDataOnNode("CXP102051_1_R4D23","4",preventingCvs, UpgradePackageState.INSTALL_COMPLETED.name());
        addUpgradepackageOnNode();
    }

    def configureMosWithoutCVMO() {
        stubbedCppUPConfigurationService.addNetworkElementMOsWihtoutCVMO(null,nodeName);
    }

    def 'delete upgrade package precheck verification tests' () {

        given: "node/network element and the upgrade package details"
        stubbedCppUPConfigurationService.buildJobPO(suppliedProductRevision,isCvDeletable,deleteNonActiveUps);
        configureMos()
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY) >> neJobStaticData;
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;

        when:  "Perform remove upgrade package precheck for the allocated job"
        ActivityStepResult activityStepResult=preCheckHandler.performPreCheck(activityJobId)

        then:  "Precheck validation is 'success proceed' / 'fail skip' / 'success skip'"
        activityResult == activityStepResult.getActivityResultEnum()

        where: "test case when user selects valid Upgrade Package, test case when user selects Upgrade Package which is containing preventing cvs, test case when user selects current/active Upgrade Package and test case when selected upgrade package is not available"
        suppliedProductRevision | isCvDeletable       |   deleteNonActiveUps        | activityResult
        "6"                     |  "true"             |  "true"                     | ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION /* test case when user selects valid Upgrade Package */
        "6"                     |  "false"            |  "true"                     | ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION /* test case when user selects Upgrade Package which is containing preventing cvs*/
        "3"                     |  "true"             |  "false"                    | ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION   /* test case when user selects current/active Upgrade Package */
        "1"                     |  "false"            |  "false"                    | ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION /* test case when selected upgrade package is not available */
    }

    def "Check for precheck fail when configuration version MO is not available on the node" () {
        given: "node/network element and the upgrade package details"
        configureMosWithoutCVMO()
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY) >> neJobStaticData;
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;

        when:  "Perform remove upgrade package precheck for the allocated job"
        ActivityStepResult activityStepResult=preCheckHandler.performPreCheck(activityJobId)

        then:  "Precheck validation is 'fail skip'"
        ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION == activityStepResult.getActivityResultEnum()
    }

    def "Check for precheck fail when TBAC Failed" () {
        given: "node/network element and the upgrade package details"
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY) >> neJobStaticData;
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> false;

        when:  "Perform remove upgrade package precheck for the allocated job"
        ActivityStepResult activityStepResult=preCheckHandler.performPreCheck(activityJobId)

        then:  "Precheck validation is 'fail skip'"
        ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION == activityStepResult.getActivityResultEnum()
    }
}
