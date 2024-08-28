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

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants

class EcimUPConfigurationProvider {

    public RuntimeConfigurableDps runtimeDps;

    public EcimUPConfigurationProvider(RuntimeConfigurableDps runtimeDps) {
        this.runtimeDps = runtimeDps;
    }

    def addSwMParentMO(final String nodeName){

        runtimeDps.addManagedObject().withFdn("SubNetwork="+nodeName)
                .addAttribute('SubNetworkId', nodeName)
                .namespace('OSS_TOP')
                .version("3.0.0")
                .type("SubNetwork")
                .build()
        runtimeDps.addManagedObject().withFdn("SubNetwork="+nodeName+",MeContext="+nodeName)
                .addAttribute("MeContextId",nodeName)
                .addAttribute('neType', 'RadioNode')
                .namespace('OSS_TOP')
                .version("3.0.0")
                .type("MeContext")
                .build()
        runtimeDps.addManagedObject().withFdn("NetworkElement="+nodeName)
                .addAttribute("NetworkElementId",nodeName)
                .addAttribute('neType', 'RadioNode')
                .addAttribute("ossModelIdentity","17A-R2YX")
                .addAttribute("nodeModelIdentity","17A-R2YX")
                .addAttribute('ossPrefix',"SubNetwork="+nodeName+",MeContext="+nodeName)
                .namespace('OSS_NE_DEF')
                .version("2.0.0")
                .type("NetworkElement")
                .build()
        runtimeDps.addManagedObject().withFdn("NetworkElement="+nodeName+",CmFunction=1")
                .type("NetworkElement")
                .build()
        runtimeDps.build().getLiveBucket().findMoByFdn("NetworkElement="+nodeName+",CmFunction=1").addAssociation("nodeRootRef", runtimeDps.build().getLiveBucket().findMoByFdn("SubNetwork="+nodeName))
        runtimeDps.build().getLiveBucket().findMoByFdn("NetworkElement="+nodeName).addAssociation("nodeRootRef", runtimeDps.build().getLiveBucket().findMoByFdn("SubNetwork="+nodeName))

        runtimeDps.addManagedObject().withFdn("SubNetwork="+nodeName+",MeContext="+nodeName+",ManagedElement="+nodeName)
                .addAttribute('ManagedElementId',nodeName)
                .addAttribute('managedElementType ', 'RadioNode')
                .addAttribute('neType', 'RadioNode')
                .type("ManagedElement")
                .build()
        runtimeDps.addManagedObject().withFdn("SubNetwork="+nodeName+",MeContext="+nodeName+",ManagedElement="+nodeName+",SystemFunctions=1,SwM=1")
                .addAttribute('swMId',1)
                .addAttribute('neType', 'RadioNode')
                .namespace('RcsSwM')
                .version("3.1.1")
                .type("SwM")
                .build()
        runtimeDps.addManagedObject().withFdn("SubNetwork="+nodeName+",MeContext="+nodeName+"ManagedElement="+nodeName+",NodeSupport=1,"+EcimCommonConstants.AutoProvisioningMoConstants.AP_MO+"=1")
                .addAttribute(EcimCommonConstants.AutoProvisioningMoConstants.RBS_CONFIG_LEVEL_KEY, EcimCommonConstants.AutoProvisioningMoConstants.SITE_CONFIG_COMPLETE)
                .version("3.1.1")
                .type(EcimCommonConstants.AutoProvisioningMoConstants.AP_MO)
                .namespace(EcimCommonConstants.AutoProvisioningMoConstants.AP_MO_NAMESPACE)
                .build()
    }


    def String addUpgradePackageOnNode(final String productNumber, final String productRevision, final String nodeName) {

        Map<String, Object> nodeAdminData=new HashMap<String, Object>();
        nodeAdminData.put(EcimCommonConstants.ProductData.PRODUCT_NUMBER, productNumber);
        nodeAdminData.put(EcimCommonConstants.ProductData.PRODUCT_REVISION, productRevision);

        ManagedObject MO =  runtimeDps.addManagedObject().withFdn("SubNetwork="+nodeName+",MeContext="+nodeName+",ManagedElement="+nodeName+",SystemFunctions=1,SwM=1,UpgradePackage="+productNumber+"_"+productRevision)
                .addAttribute("UpgradePackageId", productNumber+"_"+productRevision)
                .addAttribute("administrativeData", nodeAdminData)
                .addAttribute('neType', 'RadioNode')
                .namespace('RcsSwM')
                .type("UpgradePackage")

                .build()

        return MO.getFdn();
    }
}
