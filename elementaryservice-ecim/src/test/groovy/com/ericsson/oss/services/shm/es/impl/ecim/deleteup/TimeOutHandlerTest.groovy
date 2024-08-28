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
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.es.api.*
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants
import com.ericsson.oss.services.shm.shared.enums.JobLogType
import com.ericsson.oss.services.shm.shared.util.JobLogUtil


class TimeOutHandlerTest extends EcimAbstractDeleteUpTest {

    @ObjectUnderTest
    private TimeoutHandler timeoutHandler;

    @ObjectUnderTest
    ExecuteHandler executeHandler;

    @ObjectUnderTest
    private PreCheckHandler preCheckHandler;

    @MockedImplementation
    private JobLogUtil jobLogUtil;

    def ActivityStepResult activityStepResult;


    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
        injectionProperties.autoLocateFrom("com.ericsson.oss.itpf.modeling.modelservice")
        injectionProperties.addProxyController(new EjbProxyController(true))
    }

    def 'remove upgrade package action on single package requests repeat execution in case of timeoutsuccess test' () {
        given: "node/network element and the upgrade package details"
        loadJobProperties(inputProductNumber, inputProductRevision, deleteReferredBackups, null)
        def List<Map<String,String>> productDataList = prepareInputProductData(inputProductNumber, inputProductRevision);
        buildNodeAndUpgradepackageData(productDataList,deleteReferredBackups);
        swMHandler.removeUpgradePackageAction(_ as String,_ as String,_ as Map) >> true;
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;
        final List<Map<String, Object>> jobLogs = new ArrayList<>();

        when: "Perform remove upgrade package timeout method for the allocated job after remove upgrade package execute method"
        executeHandler.execute(activityJobId, jobActivityInfo);
        activityStepResult=timeoutHandler.handleTimeout(activityJobId);

        then: "remove upgrade package timeout result success"
        final String jobLogMessage = "Notifications not received for the \"Delete Upgrade Package\" activity for UpgradePackage with ProductNumber: \"CXP\" and ProductRevision: \"RS101\". Verifying directly."
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString())
    }

    def 'remove upgrade package action on 2 packages requests repeat execution in case of timeout'() {

        given: "node/network element and the upgrade package details"
        loadJobPropertiesFor2Pkgs("CXP"+"**|**"+"RS101"+","+"CXP"+"**|**"+"RS102", deleteReferredBackups)
        def List<Map<String,String>> productDataList = prepareInputProductDataWith2Pkgs();
        buildNodeAndUpgradepackageData(productDataList,deleteReferredBackups);
        swMHandler.removeUpgradePackageAction(_ as String,_ as String,_ as Map) >> true;
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;

        when: "Perform remove upgrade package timeout method for the allocated job after remove upgrade package execute method"
        activityStepResult=preCheckHandler.performPreCheck(activityJobId)
        executeHandler.execute(activityJobId, jobActivityInfo);
        ActivityStepResult activityStepResult=timeoutHandler.handleTimeout(activityJobId);

        then: "remove upgrade package repeat excute true when input data/product data is given with 2 upgrade packages"
        activityStepResult.getActivityResultEnum().toString() == ActivityStepResultEnum.REPEAT_EXECUTE.toString()
    }

    def 'Fail in Timeout if no backup is deleted from Two Upgrade packages with System Created backups'() {
        given: "node/network element and the upgrade package details"
        loadJobPropertiesFor2PkgsForDeleteAllInactiveUps("", deleteReferredBackups, "true")
        def List<Map<String,String>> productDataList = prepareInputProductDataWith2Pkgs();
        buildNodeAndTwoUpgradepackageDataForTwoRefferedBackups(productDataList,deleteReferredBackups, true);
        brmMoServiceRetryProxy.isBackupDeletionCompleted(_ as String,_ as String, nodeName) >> false;
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;

        when: "Perform remove upgrade package timeout method for the allocated job after remove upgrade package execute method"
        executeHandler.execute(activityJobId, jobActivityInfo);
        ActivityStepResult activityStepResult=timeoutHandler.handleTimeout(activityJobId);

        then: "Activity step result in Timeout is set to Timeout fail"
        activityStepResult.getActivityResultEnum().toString() == ActivityStepResultEnum.TIMEOUT_RESULT_FAIL.toString()
    }

    def 'Repeat execute if a backup from Two Upgrade packages with System Created backups'() {
        given: "node/network element and the upgrade package details"
        loadJobPropertiesFor2PkgsForDeleteAllInactiveUps("", deleteReferredBackups, "true")
        def List<Map<String,String>> productDataList = prepareInputProductDataWith2Pkgs();
        buildNodeAndTwoUpgradepackageDataForTwoRefferedBackups(productDataList,deleteReferredBackups, true);
        brmMoServiceRetryProxy.isBackupDeletionCompleted(_ as String,_ as String, nodeName) >> true;
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;

        when: "Perform remove upgrade package timeout method for the allocated job after remove upgrade package execute method"
        executeHandler.execute(activityJobId, jobActivityInfo);
        ActivityStepResult activityStepResult=timeoutHandler.handleTimeout(activityJobId);

        then: "repeat the execute by setting Activity Result as Repeat execute"
        activityStepResult.getActivityResultEnum().toString() == ActivityStepResultEnum.REPEAT_EXECUTE.toString()
    }

    def loadJobPropertiesFor2Pkgs(final String productData, final String deleteReferredBackups) {
        def List<Map<String, Object>> neSpecificPropertyList = new ArrayList<Map<String, Object>>();
        def Map<String, Object> neJobPropertyUserInput = new HashMap<String, Object>();
        def Map<String, Object> neJobPropertyIsDeleteReferred = new HashMap<String, Object>();

        neJobPropertyUserInput.put(ShmJobConstants.KEY, UpgradeActivityConstants.DELETE_UP_LIST);
        neJobPropertyUserInput.put(ShmJobConstants.VALUE, productData);

        neJobPropertyIsDeleteReferred.put(ShmJobConstants.KEY, JobPropertyConstants.DELETE_REFERRED_BACKUPS);
        neJobPropertyIsDeleteReferred.put(ShmJobConstants.VALUE, deleteReferredBackups);

        neSpecificPropertyList.add(neJobPropertyUserInput);
        neSpecificPropertyList.add(neJobPropertyIsDeleteReferred);

        jobConfigurationDetails.putAt(ShmJobConstants.JOBPROPERTIES, neSpecificPropertyList);
        mainJobAttributesDetails.put(ShmConstants.PROGRESSPERCENTAGE,new java.lang.Double(0.0));
        mainJobAttributesDetails.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);

        Map<String, Object> attributeMap = new HashMap<>();


        attributeMap.put(ShmConstants.NE_NAME, nodeName);
        attributeMap.put("progressPercentage",Double.valueOf(0.1));
        PersistenceObject mainJob  = runtimeDps.addPersistenceObject().namespace("shm").type("MainJob").addAttributes(mainJobAttributesDetails).build();
        mainJobId = mainJob.getPoId();
        attributeMap.put(ShmConstants.MAIN_JOB_ID,mainJobId);
        PersistenceObject neJob  = runtimeDps.addPersistenceObject().namespace("shm").type("NeJob").addAttributes(attributeMap).build();
        neJobId = neJob.getPoId();
        attributeMap.put(ShmConstants.NE_JOB_ID, neJobId);
        PersistenceObject activityJob = runtimeDps.addPersistenceObject().namespace("shm").type("ActivityJob").addAttributes(attributeMap).build();
        activityJobId = activityJob.getPoId();
        neJobStaticData = new NEJobStaticData(neJobId, mainJobId, nodeName, "1234", "ECIM", 5L, null);
        jobActivityInfo = new JobActivityInfo(activityJobId, ShmConstants.DELETEUPGRADEPKG_ACTIVITY, JobTypeEnum.DELETE_UPGRADEPACKAGE, PlatformTypeEnum.ECIM)
    }
}
