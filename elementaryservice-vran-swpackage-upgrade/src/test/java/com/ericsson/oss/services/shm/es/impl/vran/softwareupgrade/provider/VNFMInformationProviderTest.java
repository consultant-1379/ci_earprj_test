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
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider;

import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.shm.vran.common.VNFMInformationProvider;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;

@RunWith(MockitoJUnitRunner.class)
public class VNFMInformationProviderTest extends ProviderTestBase {

    @InjectMocks
    private VNFMInformationProvider vnfmInformationProvider;

    @Mock
    private DataPersistenceService dataPersistenceService;

    @Mock
    ManagedObject managedObject;

    @Mock
    DataBucket dataBucket;

    @Mock
    private PersistenceObject neJobEntity;

    private static final String NODE_NAME = "string";
    public static final String NODE_FDN = "NetworkElement=" + NODE_NAME;

    @Test
    public void testGetVnfmFdn() {
        String vnfDataMoFdn = NODE_FDN + "," + VranJobConstants.VIRTUAL_NETWORK_FUNCTION_DATA_RDN;
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataPersistenceService.getLiveBucket().findMoByFdn(vnfDataMoFdn)).thenReturn(managedObject);
        when(managedObject.getAttribute(VranJobConstants.VIRTUAL_MANAGER)).thenReturn("vnfmFDN");
        vnfmInformationProvider.getVnfmFdn(NODE_FDN);
    }

    @Test
    public void testGetVnfmFdn_NoMO() {

        when(dataPersistenceService.getLiveBucket()).thenReturn(null);

        vnfmInformationProvider.getVnfmFdn(NODE_FDN);
    }

}
