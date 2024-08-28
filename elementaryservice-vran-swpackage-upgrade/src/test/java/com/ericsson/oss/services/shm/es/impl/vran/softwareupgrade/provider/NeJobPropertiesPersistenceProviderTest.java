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

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.exception.NoNetworkElementAssociatedException;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;

@RunWith(MockitoJUnitRunner.class)
public class NeJobPropertiesPersistenceProviderTest extends ProviderTestBase {

    @InjectMocks
    private NeJobPropertiesPersistenceProvider neJobPropertiesPersistenceProvider;

    @Before
    public void mockJobEnvironment() {
        super.mockJobEnvironment();
        NetworkElement ne = new NetworkElement();
        ne.setName("NeName");
        List<NetworkElement> networkElements = new ArrayList<NetworkElement>();
        networkElements.add(ne);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(vnfInformationProvider.fetchNetworkElements(jobEnvironment)).thenReturn(networkElements);
        when(vnfInformationProvider.getVnfId(networkElements)).thenReturn("VNF ID:12345");
        when(vnfPackageDataProvider.fetchVnfPackageDetails(jobEnvironment, networkElements)).thenReturn(new ArrayList<Map<String, Object>>());
    }

    @Test
    public void testPersistVnfJobId() {
        neJobPropertiesPersistenceProvider.persistVnfJobId(upgradePackageContext, vranSoftwareUpgradeJobResponse, jobEnvironment);
    }

    @Test
    public void testPersistVnfInformation_NetworkElementsExist() {

        neJobPropertiesPersistenceProvider.persistVnfInformation(activityJobId);
    }

    @Test(expected = NoNetworkElementAssociatedException.class)
    public void testPersistVnfInformation_NetworkElementsDoNotExist() {
        List<NetworkElement> networkElements = new ArrayList<NetworkElement>();
        when(vnfInformationProvider.fetchNetworkElements(jobEnvironment)).thenReturn(networkElements);
        neJobPropertiesPersistenceProvider.persistVnfInformation(activityJobId);
    }

    @Test
    public void testPersistFromAndToVnfIds() {
        neJobPropertiesPersistenceProvider.persistFromAndToVnfIds(activityJobId, "VNF :12345", "VNF : 54321", jobEnvironment);
    }

}
