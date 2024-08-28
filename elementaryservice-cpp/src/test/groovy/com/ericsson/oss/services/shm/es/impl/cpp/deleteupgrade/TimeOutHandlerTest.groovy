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

import javax.inject.Inject

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.shm.common.*
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.es.api.*
import com.ericsson.oss.services.shm.es.impl.cpp.common.*
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.*
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.handlers.ExecuteHandler
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.handlers.PreCheckHandler
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.handlers.TimeoutHandler

public class TimeOutHandlerTest extends CPPAbstractDeleteUpTest {

    @ObjectUnderTest
    private TimeoutHandler timeoutHandler;

    @Inject
    private ExecuteHandler executeHandler;

    @Inject
    private PreCheckHandler preCheckHandler;

    private ActivityStepResult activityStepResult ;

    def setup() {
        stubbedCppUPConfigurationService.addActivityJobPOForTimeout(deletableUPFdn);
        stubbedCppUPConfigurationService.addNetworkElementMOs(null,nodeName);
        addUpgradepackageOnNode();
        inventoryQueryConfigurationProvider.getNeFdnBatchSize() >> 3;
        List<String> preventingCvs=new ArrayList<String>();
        addAdminDataOnNode("CXP102051_1_R4D21","3",preventingCvs);
        addAdminDataOnNode("CXP102051_1_R4D22","6",preventingCvs);
        addAdminDataOnNode("CXP102051_1_R4D23","4",preventingCvs);
        nodeModelNameSpaceProvider.getNamespaceByNodeName(nodeName) >> "OSS_NE_DEF";
    }

    def 'delete upgrade package request in case of timeout success' () {
        given: "node/network element and the upgrade package details"
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY) >> neJobStaticData;
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;

        when: "Perform remove upgrade package timeout method for the allocated job after remove upgrade package execute method"
        executeHandler.execute(activityJobId, jobActivityInfo);
        activityStepResult=timeoutHandler.handleTimeout(activityJobId);

        then: "upgrade package has to be removed and activity result should be success"
        runtimeDps.stubbedDps.liveBucket.findMoByFdn(deletableUPFdn)==null;
        activityStepResult.getActivityResultEnum().toString() == ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS.toString()
    }

    def 'delete upgrade package request in case of timeout failure' () {
        given: "node/network element and the upgrade package details"
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY) >> neJobStaticData;
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;

        when: "Perform remove upgrade package timeout method for the allocated job"
        preCheckHandler.performPreCheck(activityJobId);
        activityStepResult=timeoutHandler.handleTimeout(activityJobId);

        then: "upgrade package is not removed and activity result should be repeat execute"
        runtimeDps.stubbedDps.liveBucket.findMoByFdn(deletableUPFdn)!=null;
        activityStepResult.getActivityResultEnum().toString() == ActivityStepResultEnum.REPEAT_EXECUTE.toString()
    }
}
