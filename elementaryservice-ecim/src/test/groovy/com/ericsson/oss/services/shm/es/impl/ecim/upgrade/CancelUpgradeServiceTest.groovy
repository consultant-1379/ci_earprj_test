/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.ecim.upgrade
import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.shm.es.api.ActivityStepResult
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.model.NetworkElementData

class CancelUpgradeServiceTest extends EcimUpgradeDataProvider {

    @ObjectUnderTest
    CancelUpgradeService cancelUpgradeService


    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
    }
    def 'Test cancel action for Activate Activity and Job is successful or Failed'() {

        given:"NetworkElement and the CreateCV activity details"
        loadJobPropertiesForUpgrade(nodeName,"activate",SUCCESS)
        buildDataForUpgradeActivity(nodeName,SUCCESS)
        swMHandler.isValidAsyncActionProgress(_ as String, _) >> false

        if(isValidSenario) {
            actionResult.setActionId(1)
            actionResult.setTriggerSuccess(true)
        } else {
            actionResult.setActionId(-1)
            actionResult.setTriggerSuccess(false)
        }
        swMHandler.executeCancelAction(_, _, _, _, _ as NetworkElementData) >> actionResult
        swMHandler.isActivityAllowed(_ as String, _ as String, _ as String, _ as String, _ as NetworkElementData) >> getActivityAllowed(true)

        when:"Activate  Activity has received notifications"
        ActivityStepResult activityStepResult=cancelUpgradeService.cancel(activityJobId,"cancel")

        then:"Check if cancel action is triggered and is expected"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(activityResult)

        where:
        activityResult                          |isValidSenario
        ActivityStepResultEnum.EXECUTION_SUCESS |true
        ActivityStepResultEnum.EXECUTION_FAILED |false
    }

    def 'Test cancel action for Activate Activity and Job is Failed due to exception'() {

        given:"NetworkElement and the CreateCV activity details"
        loadJobPropertiesForUpgrade(nodeName,"activate",SUCCESS)
        buildDataForUpgradeActivity(nodeName,exceptionSenario)

        when:"Activate  Activity has received notifications"
        ActivityStepResult activityStepResult=cancelUpgradeService.cancel(activityJobId,"cancel")

        then:"Check if cancel action is Failed"
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(activityResult)
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains("\"cancel\" activity initiated."))
        }

        where:
        activityResult                          |exceptionSenario
        ActivityStepResultEnum.EXECUTION_FAILED |"FDN_EXCEPTION"
        ActivityStepResultEnum.EXECUTION_FAILED |"FDN_UNSUPPORTED_FRAGMENT"
        ActivityStepResultEnum.EXECUTION_FAILED |"FDN_MO_NOT_FOUND"
        ActivityStepResultEnum.EXECUTION_FAILED |"FDN_NODE_ATTR_READER_EXCEPTION"
        ActivityStepResultEnum.EXECUTION_FAILED |"FDN_SOFTWARE_PCGPO_EXCEPTION"
        ActivityStepResultEnum.EXECUTION_FAILED |"FDN_SOFTWARE_PCGNAME_EXCEPTION"
        ActivityStepResultEnum.EXECUTION_FAILED |"FDN_ARG_BUILDER_EXCEPTION"
        ActivityStepResultEnum.EXECUTION_FAILED |"CANCEL_EXCEPTION"
    }
}
