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
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@RunWith(MockitoJUnitRunner.class)
public class UpgradePackageContextTest {

    @InjectMocks
    private UpgradePackageContext upgradePackageContext;

    @Test
    public void testGetNodeFdn() {
        upgradePackageContext.getNodeFdn();
    }

    @Test
    public void testSetNodeFdn() {
        upgradePackageContext.setNodeFdn("NodeFdn");
    }

    @Test
    public void testGetNodeName() {
        upgradePackageContext.getNodeName();
    }

    @Test
    public void testSetNodeName() {
        upgradePackageContext.setNodeName("NodeName");
    }

    @Test
    public void testGetActionTriggered() {
        upgradePackageContext.getActionTriggered();
    }

    @Test
    public void testSetActionTriggered() {
        upgradePackageContext.setActionTriggered(ActivityConstants.PREPARE);
    }

    @Test
    public void testGetJobEnvironment() {
        upgradePackageContext.getJobEnvironment();
    }

    @Test
    public void testGetBusinessKey() {
        upgradePackageContext.getBusinessKey();
    }

    @Test
    public void testSetBusinessKey() {
        upgradePackageContext.setBusinessKey("businessKey");
    }

    @Test
    public void testGetJobContext() {
        upgradePackageContext.getJobContext();
    }

    @Test
    public void testSetJobEnvironment() {
        upgradePackageContext.setJobEnvironment(null);
    }

    @Test
    public void testGetPackageLocation() {
        upgradePackageContext.getPackageLocation();
    }

    @Test
    public void testSetPackageLocation() {
        upgradePackageContext.setPackageLocation("packageLocation");
    }

    @Test
    public void testGetSoftwarePackageName() {
        upgradePackageContext.getSoftwarePackageName();
    }

    @Test
    public void testSetSoftwarePackageName() {
        upgradePackageContext.setSoftwarePackageName("swPackageeName");
    }

    @Test
    public void testGetSoftwarePackageId() {
        upgradePackageContext.getSoftwarePackageId();
    }

    @Test
    public void testSetSoftwarePackageId() {
        upgradePackageContext.setSoftwarePackageId("PkgID :12232");
    }

    @Test
    public void testGetVnfId() {
        upgradePackageContext.getVnfId();
    }

    @Test
    public void testGetVnfJobId() {
        upgradePackageContext.getVnfJobId();
    }

    @Test
    public void testSetVnfJobId() {
        upgradePackageContext.setVnfJobId(1234);
    }

    @Test
    public void testSetVnfId() {
        upgradePackageContext.setVnfId("VNF :1234");
    }

    @Test
    public void testGetVnfPackageId() {
        upgradePackageContext.getVnfPackageId();
    }

    @Test
    public void testSetVnfPackageId() {
        upgradePackageContext.setVnfPackageId("VNFP:321");
    }

    @Test
    public void testGetVnfDescription() {
        upgradePackageContext.getVnfDescription();
    }

    @Test
    public void testSetVnfDescription() {
        upgradePackageContext.setVnfDescription("VnfDescription");

    }

    @Test
    public void testGetVnfmFdn() {
        upgradePackageContext.getVnfmFdn();

    }

    @Test
    public void testSetVnfmFdn() {
        upgradePackageContext.setVnfmFdn("vnfmFdn");
    }

    @Test
    public void testGetVnfmName() {
        upgradePackageContext.getVnfmName();
    }

    @Test
    public void testSetVnfmName() {
        upgradePackageContext.setVnfmName("VnfmName");
    }

    @Test
    public void testToString() {
        upgradePackageContext.toString();
    }

}
