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
package com.ericsson.oss.services.shm.es.impl.cpp.deleteupgrade
import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.providers.custom.ejb.EjbProxyController
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy
import com.ericsson.oss.services.shm.common.InventoryQueryConfigurationProvider
import com.ericsson.oss.services.shm.common.NodeModelNameSpaceProvider
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.job.utils.NodeAttributesReader
import com.ericsson.oss.services.shm.common.retry.DpsRetryConfigurationParamProvider
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies
import com.ericsson.oss.services.shm.es.api.JobActivityInfo
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants
import com.ericsson.oss.services.shm.es.impl.cpp.common.UpgradePackageMO
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.DeleteUpgradePackageActionInfo
import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.UpgradePackageMoConstants
import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.UpgradePackageState;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator


public abstract class CPPAbstractDeleteUpTest extends CdiSpecification {

    @MockedImplementation
    protected DpsRetryConfigurationParamProvider dpsConfigurationParamProvider;

    @MockedImplementation
    protected RetryPolicy retryPolicy;

    @MockedImplementation
    protected DpsRetryPolicies dpsRetryPolicies;

    @MockedImplementation
    protected NeJobStaticDataProvider neJobStaticDataProvider;

    @MockedImplementation
    protected NodeModelNameSpaceProvider nodeModelNameSpaceProvider;

    @MockedImplementation
    protected NodeAttributesReader nodeAttributesReader;

    @MockedImplementation
    protected InventoryQueryConfigurationProvider inventoryQueryConfigurationProvider;

    @MockedImplementation
    protected ActivityJobTBACValidator activityJobTBACValidator;

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.es")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.shared")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.common")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.jobs")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.nejob")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.networkelement")
        injectionProperties.addProxyController(new EjbProxyController(true))
    }

    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps);
    protected StubbedCppUPConfigurationService stubbedCppUPConfigurationService=new StubbedCppUPConfigurationService(runtimeDps);

    def activityJobId = 1L;
    def String suppliedProductRevision="6";
    def nodeName="LTE02ERBS00001";
    def NEJobStaticData neJobStaticData = new NEJobStaticData(1L, 2L, nodeName, "1234", "CPP", 5L, null);
    def JobActivityInfo jobActivityInfo = new JobActivityInfo(activityJobId, ShmConstants.DELETEUPGRADEPKG_ACTIVITY, JobTypeEnum.DELETEBACKUP, PlatformTypeEnum.CPP)
    def String currentUPFdn = "MeContext=LTE02ERBS00001,ManagedElement=LTE02ERBS00001,SwManagement=LTE02ERBS00001,UpgradePackage=CXP102051_1_R4D21";
    def String deletableUPFdn = "MeContext=LTE02ERBS00001,ManagedElement=LTE02ERBS00001,SwManagement=LTE02ERBS00001,UpgradePackage=CXP102051_1_R4D22";
    def String deleteUpgradePackageActionInfoStr = new DeleteUpgradePackageActionInfo().toString()
    def String upgradePackageMOStr = new UpgradePackageMO("", new HashMap<String, Object>()).toString()

    def addAdminDataOnNode(String upgradePackageId,String productRevision,List<String> preventingCvs, String upState) {
        Map<String, Object> adminData = new HashMap();
        Map<String, Object> moAttributes = new HashMap();
        stubbedCppUPConfigurationService.addUpgradePackage(adminData,nodeName,upgradePackageId, upState);
        String upgradePackageFdn="MeContext="+nodeName+","+"ManagedElement="+nodeName+",SwManagement="+nodeName+",UpgradePackage="+upgradePackageId;
        adminData.put(UpgradeActivityConstants.UP_PO_PROD_NAME, "1_"+productRevision);
        adminData.put(UpgradeActivityConstants.PRODUCT_NUMBER, "1");
        adminData.put(UpgradeActivityConstants.PRODUCT_REVISION, productRevision);
        moAttributes.putAt(UpgradeActivityConstants.ADMINISTRATIVE_DATA, adminData);
        moAttributes.putAt(UpgradeActivityConstants.DELETE_PREVENTING_CVS,preventingCvs);
        moAttributes.putAt(UpgradeActivityConstants.DELETE_PREVENTING_UPS,new ArrayList());
        nodeAttributesReader.readAttributes(runtimeDps.stubbedDps.liveBucket.findMoByFdn(upgradePackageFdn),_ as String[]) >> moAttributes
    }
    def addUpgradepackageOnNode() {
        def Map<String, Object> moAttributes = new HashMap<String, Object>();
        moAttributes.put(ConfigurationVersionMoConstants.CURRENT_UPGRADE_PACKAGE, currentUPFdn);
        moAttributes.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.UPGRADE_EXECUTING.name());
        nodeAttributesReader.readAttributes(runtimeDps.stubbedDps.liveBucket.findMoByFdn("MeContext=LTE02ERBS00001,ManagedElement=LTE02ERBS00001,SwManagement=LTE02ERBS00001,ConfigurationVersion=LTE02ERBS00001"),_ as String[]) >>moAttributes;
    }
}
