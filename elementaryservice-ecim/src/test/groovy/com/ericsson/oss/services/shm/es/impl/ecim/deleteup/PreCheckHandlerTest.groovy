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

import static org.mockito.Matchers.*

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.providers.custom.ejb.EjbProxyController
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.shm.common.*
import com.ericsson.oss.services.shm.es.api.*

public class PreCheckHandlerTest extends EcimAbstractDeleteUpTest {

    @ObjectUnderTest
    private PreCheckHandler preCheckHandler;

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
        injectionProperties.autoLocateFrom("com.ericsson.oss.itpf.modeling.modelservice")
        injectionProperties.addProxyController(new EjbProxyController(true))
    }

    def 'remove upgrade package precheck verification test' () {
        given: "node/network element and the upgrade package details"
        loadJobProperties(inputProductNumber, inputProductRevision, deleteReferredBackups, null)
        def List<Map<String,String>> productDataList = prepareInputProductData(inputProductNumber, inputProductRevision);
        buildNodeAndUpgradepackageData(productDataList,deleteReferredBackups);
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;

        when: "Perform remove upgrade package precheck for the allocated job"
        ActivityStepResult activityStepResult=preCheckHandler.performPreCheck(activityJobId)

        then: "Precheck validation is 'success proceed' / 'success skip' / 'fail skip'"
        activityResult == activityStepResult.getActivityResultEnum()

        where: "Product details existed on the node, empty product details and if there referred backups for the given upgrade package when deleteReferredBackups option not selected"
        inputProductNumber  | inputProductRevision | deleteReferredBackups  | activityResult
        //with correct product details(package with the same product details existed on the node)
        "CXP"               |     "RS101"          |    "true"              |  ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION
        //empty product details
        ""                  |      ""              |    "true"              |  ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION
        //if deleteReferredBackups is set as false (user does not want to delete referred backups) but still referred backups for the given upgrade package are there
        "CXP"               |     "RS103"          |    "false"             |  ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION
    }

    def 'remove upgrade package precheck to delete a single ups skip job execution' () {
        given: "node/network element and the upgrade package details"
        loadJobProperties(null, null, deleteReferredBackups, "true")
        def List<Map<String,String>> productDataList = prepareInputProductData(inputProductNumber, inputProductRevision);
        buildNodeAndUpgradepackageData(productDataList,deleteReferredBackups);
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;

        when: "Perform remove upgrade package precheck to delete all non active "
        ActivityStepResult activityStepResult=preCheckHandler.performPreCheck(activityJobId)

        then: "Precheck validation is 'precheck success skip execution' "
        activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION
    }
}
