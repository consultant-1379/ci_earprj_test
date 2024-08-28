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
package com.ericsson.oss.services.shm.es.impl.cpp.deleteupgrade

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants



public class StubbedCppUPConfigurationService {

    def Map<String, Object> jobConfigurationMock=new HashMap<String,Object>();
    def Map<String, Object> activityJobAttributesMock=new HashMap<String,Object>();
    def Map<String, Object> mainJobAttributesMock=new HashMap<String,Object>();

    public RuntimeConfigurableDps runtimeDps;

    public StubbedCppUPConfigurationService(RuntimeConfigurableDps runtimeDps) {
        this.runtimeDps = runtimeDps;
        this.runtimeDps.withTransactionBoundaries()
    }

    def addNetworkElementMOs(Map<String, Object> adminData,final String nodeName){
        addNEMOs(nodeName)
        addCVMO(nodeName)
    }

    def addNetworkElementMOsWihtoutCVMO(Map<String, Object> adminData,final String nodeName){
        addNEMOs(nodeName)
    }

    private addCVMO(String nodeName) {
        runtimeDps.addManagedObject().withFdn("MeContext="+nodeName+",ManagedElement="+nodeName+",SwManagement="+nodeName+",ConfigurationVersion="+nodeName)
                .addAttribute('ConfigurationVersionId', nodeName)
                .addAttribute("currentUpgradePackage", "ManagedElement=LTE02ERBS00001,SwManagement=LTE02ERBS00001,UpgradePackage=CXP102051_1_R4D21")
                .namespace('OSS_NE_DEF')
                .type("ConfigurationVersion")
                .build()
    }

    private addNEMOs(String nodeName) {
        runtimeDps.addManagedObject().withFdn("MeContext="+nodeName)
                .addAttribute('MeContextId', nodeName)
                .addAttribute('neType', 'ERBS')
                .addAttribute('platformType', 'CPP')
                .namespace('OSS_TOP')
                .version("3.0.0")
                .type("MeContext")
                .build()
        runtimeDps.addManagedObject().withFdn("NetworkElement="+nodeName)
                .addAttribute("NetworkElementId",nodeName)
                .addAttribute("ossModelIdentity", "17A-H.1.160")
                .addAttribute('neType', 'ERBS')
                .addAttribute('ossPrefix','MeContext='+nodeName)
                .addAttribute('platformType', 'CPP')
                .namespace('OSS_NE_DEF')
                .version("2.0.0")
                .type("NetworkElement")
                .build()
        runtimeDps.addManagedObject().withFdn("NetworkElement="+nodeName+",CmFunction=1")
                .type("NetworkElement")
                .build()
        runtimeDps.build().getLiveBucket().findMoByFdn("NetworkElement="+nodeName+",CmFunction=1").addAssociation("nodeRootRef", runtimeDps.build().getLiveBucket().findMoByFdn("MeContext="+nodeName))
        runtimeDps.build().getLiveBucket().findMoByFdn("NetworkElement="+nodeName).addAssociation("nodeRootRef", runtimeDps.build().getLiveBucket().findMoByFdn("MeContext="+nodeName))
        runtimeDps.addManagedObject().withFdn("MeContext="+nodeName+",ManagedElement="+nodeName)
                .addAttribute('ManagedElementId',nodeName)
                .addAttribute('productType', 'Node')
                .addAttribute('neType', 'ERBS')
                .type("ManagedElement")
                .build()
        runtimeDps.addManagedObject().withFdn("MeContext="+nodeName+",ManagedElement="+nodeName+",SwManagement="+nodeName)
                .addAttribute('SwManagementId', nodeName)
                .type("SwManagement")
                .build()
    }

    def buildJobPO(final String prodrevision,final String isCvDeletable,final String deleteNonActiveUps){
        loadJobProperties(prodrevision,isCvDeletable,deleteNonActiveUps);
        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put(ShmConstants.ACTIVITY_NE_JOB_ID, 1L);
        attributeMap.put(ShmConstants.MAIN_JOB_ID,2L);
        attributeMap.put(ShmConstants.NE_NAME,"LTE02ERBS00001");
        attributeMap.put("progressPercentage",Double.valueOf(0.1));
        runtimeDps.addPersistenceObject().namespace("shm").type("ActivityJob").addAttributes(attributeMap).build();
        runtimeDps.addPersistenceObject().namespace("shm").type("MainJob").addAttributes(mainJobAttributesMock).build();
        runtimeDps.addPersistenceObject().namespace("shm").type("NeJob").addAttributes(attributeMap).build();
    }
    def addActivityJobPOForTimeout(def deletableUPFdn){
        loadJobProperties("6","true");
        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put(ShmConstants.ACTIVITY_NE_JOB_ID, 1L);
        attributeMap.put(ShmConstants.MAIN_JOB_ID,2L);
        attributeMap.put(ShmConstants.NE_NAME,"LTE02ERBS00001");
        attributeMap.put("progressPercentage",Double.valueOf(0.1));
        List<Map<String, String>> listproperties=new ArrayList<HashMap<String, String>>();
        HashMap hmp=new HashMap();
        hmp.put(ActivityConstants.JOB_PROP_KEY,UpgradeActivityConstants.CURRENT_PROCESSING_UP);
        hmp.put(ActivityConstants.JOB_PROP_VALUE,"1"+"**|**"+"6");
        HashMap hmp1=new HashMap();
        hmp1.put(ActivityConstants.JOB_PROP_KEY, ShmConstants.FDN);
        hmp1.put(ActivityConstants.JOB_PROP_VALUE,deletableUPFdn);
        listproperties.add(hmp);
        listproperties.add(hmp1);
        attributeMap.put(ActivityConstants.JOB_PROPERTIES, listproperties);
        PersistenceObject activityJobPo = runtimeDps.addPersistenceObject().namespace("shm").type("ActivityJob").addAttributes(attributeMap).build();
        runtimeDps.addPersistenceObject().namespace("shm").type("MainJob").addAttributes(mainJobAttributesMock).build();
        runtimeDps.addPersistenceObject().namespace("shm").type("NeJob").addAttributes(attributeMap).build();
        activityJobPo.getPoId()
    }

    def loadJobProperties(final String prodrevision,final String isCvDeletable,final String deleteNonActiveUps) {
        def List<Map<String, Object>> neSpecificPropertyList = new ArrayList<Map<String, Object>>();
        def Map<String, Object> currentProcessingUP = new HashMap<String, Object>();
        def Map<String, Object> deleteUpList = new HashMap<String, Object>();
        def Map<String, Object> neJobProperty1 = new HashMap<String, Object>();
        def Map<String, Object> neJobProperty2 = new HashMap<String, Object>();
        def Map<String, Object> neJobProperty3 = new HashMap<String, Object>();
        def Map<String, Object> neJobProperty4 = new HashMap<String, Object>();
        currentProcessingUP.put("key", UpgradeActivityConstants.CURRENT_PROCESSING_UP);
        currentProcessingUP.put("value", "1"+"**|**"+"6");

        deleteUpList.put("key", UpgradeActivityConstants.DELETE_UP_LIST);
        deleteUpList.put("value", "1"+"**|**"+prodrevision);
        neJobProperty1.put("key", UpgradeActivityConstants.IS_PREVENT_CV_DELETABALE_FROM_ROLLBACKLIST);
        neJobProperty1.put("value", isCvDeletable);
        neJobProperty2.put("key", UpgradeActivityConstants.IS_PREVENT_UP_DELETABALE);
        neJobProperty2.put("value", "true");
        neJobProperty3.put("key", ActivityConstants.IS_PRECHECK_DONE);
        neJobProperty3.put("value", "true");
        neJobProperty4.put("key", UpgradeActivityConstants.PROCESSED_UPS);
        neJobProperty4.put("value", "1");
        neJobProperty4.put("key", JobPropertyConstants.DELETE_NON_ACTIVE_UPS);
        neJobProperty4.put("value", deleteNonActiveUps);

        neSpecificPropertyList.add(currentProcessingUP)
        neSpecificPropertyList.add(deleteUpList)
        neSpecificPropertyList.add(neJobProperty1);
        neSpecificPropertyList.add(neJobProperty2);
        neSpecificPropertyList.add(neJobProperty3);
        neSpecificPropertyList.add(neJobProperty4);
        if(deleteNonActiveUps=="true") {
            neSpecificPropertyList.remove(deleteUpList)
        }
        jobConfigurationMock.putAt("jobProperties", neSpecificPropertyList);
        mainJobAttributesMock.put(ShmConstants.JOBCONFIGURATIONDETAILS, jobConfigurationMock);
    }
    def addUpgradePackage(Map<String, Object> adminData,final String nodeName,final String currentUP, final String upState) {
        runtimeDps.addManagedObject().withFdn("MeContext="+nodeName+","+"ManagedElement="+nodeName+",SwManagement="+nodeName+",UpgradePackage="+currentUP)
                .addAttribute("UpgradePackageId", currentUP)
                .addAttributes(newSoftwareVersion(adminData, upState))
                .namespace('OSS_NE_DEF')
                .version("2.0.0")
                .type("UpgradePackage")
                .build()
    }

    def newSoftwareVersion(final Map<String, Object> adminData, final String upState){
        Map<String, Object> attributes = new HashMap();
        attributes.put("state", upState);
        attributes.put("administrativeData", adminData);
        attributes.put("userLabel", "label21");
        return attributes;
    }
}
