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
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.HashedMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NeJobPropertiesBuilderTest extends ProviderTestBase {

    @InjectMocks
    private NeJobPropertiesBuilder neJobPropertiesBuilder;

    @Before
    public void mockJobEnvironment() {
        super.mockJobEnvironment();

    }

    @Test
    public void testBuildVnfJobId() {
        neJobPropertiesBuilder.buildVnfJobId(upgradePackageContext, vranSoftwareUpgradeJobResponse);
    }

    @Test
    public void testBuildVnfPackageAndVnfId() {
        final List<Map<String, Object>> vnfPackageDetails = new ArrayList<Map<String, Object>>();
        final Map<String, Object> vnfIdProperty = new HashedMap<String, Object>();

        neJobPropertiesBuilder.buildVnfPackageAndVnfId(vnfPackageDetails, vnfIdProperty, jobEnvironment);
    }

    @Test
    public void testBuildFromVnfAndToVnfIds() {

        neJobPropertiesBuilder.buildFromVnfAndToVnfIds("VNF: 12345", "VNF: 54321", jobEnvironment);
    }

}
