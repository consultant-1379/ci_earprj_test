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
package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.persistence;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.DpsWriterImpl;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.shared.persistence.VnfSoftwarePackagePersistenceProvider;

@RunWith(MockitoJUnitRunner.class)
public class VnfSoftwarePackagePersistenceProviderTest {

    private static final String PACKAGE_NAME = "Package";

    private static final long PO_ID = 12345;

    private static final String VNF_PACKAGE_ID = "00-11-er4-654";

    private static final String NFVO_FDN = "NetworkFunctionVirtualizationOrchestrator=HPE-NFV-Director-001";

    @Mock
    private DpsReader dpsReader;

    @Mock
    private DpsWriter dpsWriter;

    @Mock
    private DpsWriterImpl dpsWriterImpl;

    @InjectMocks
    private VnfSoftwarePackagePersistenceProvider vnfSoftwarePackagePersistenceProvider;

    @Mock
    private PersistenceObject persistenceObj;

    @Mock
    private PersistenceObject persistenceObject;

    private Map<String, Object> restrictionsMap = new HashMap<String, Object>();

    private List<PersistenceObject> matchingPOList = new ArrayList<PersistenceObject>();

    @Before
    public void setUp() throws Exception {
        restrictionsMap.put(UpgradeActivityConstants.UP_PO_PACKAGE_NAME, PACKAGE_NAME);
        matchingPOList.add(0, persistenceObj);
        matchingPOList.add(1, persistenceObject);
    }

    @Test
    public void testGetVnfSoftwarePackageEntityWithListValues() {
        when(dpsReader.findPOs(UpgradeActivityConstants.UPPKG_NAMESPACE, VranJobConstants.VNF_PACKAGE_TYPE, restrictionsMap)).thenReturn(matchingPOList);
        vnfSoftwarePackagePersistenceProvider.getVnfSoftwarePackageEntity(PACKAGE_NAME);
        verify(dpsReader).findPOs(UpgradeActivityConstants.UPPKG_NAMESPACE, VranJobConstants.VNF_PACKAGE_TYPE, restrictionsMap);
    }

    @Test
    public void testGetVnfSoftwarePackageEntityWithEmptyList() {
        when(dpsReader.findPOs(UpgradeActivityConstants.UPPKG_NAMESPACE, VranJobConstants.VNF_PACKAGE_TYPE, restrictionsMap)).thenReturn(null);
        vnfSoftwarePackagePersistenceProvider.getVnfSoftwarePackageEntity(PACKAGE_NAME);
        verify(dpsReader).findPOs(UpgradeActivityConstants.UPPKG_NAMESPACE, VranJobConstants.VNF_PACKAGE_TYPE, restrictionsMap);
    }

    @Test
    public void testGetVnfPackageIdWithListValues() {
        final Map<String, Object> attributes = new HashMap<String, Object>();
        final List<Map<String, Object>> productDetailsInNfvo = new ArrayList<Map<String, Object>>();
        final Map<String, Object> productDetailsMap = new HashMap<String, Object>();
        productDetailsMap.put(VranJobConstants.VNF_PACKAGE_ID, VNF_PACKAGE_ID);
        productDetailsInNfvo.add(productDetailsMap);
        attributes.put(VranJobConstants.SOFTWAREPACKAGE_NFVODETAILS, productDetailsInNfvo);
        when(persistenceObj.getAllAttributes()).thenReturn(attributes);

        when(dpsReader.findPOs(UpgradeActivityConstants.UPPKG_NAMESPACE, VranJobConstants.VNF_PACKAGE_TYPE, restrictionsMap)).thenReturn(matchingPOList);
        vnfSoftwarePackagePersistenceProvider.getVnfPackageId(PACKAGE_NAME, NFVO_FDN);
        verify(dpsReader).findPOs(UpgradeActivityConstants.UPPKG_NAMESPACE, VranJobConstants.VNF_PACKAGE_TYPE, restrictionsMap);
    }

    @Test
    public void testGetVnfPackageIdWithEmptyList() {
        when(dpsReader.findPOs(UpgradeActivityConstants.UPPKG_NAMESPACE, VranJobConstants.VNF_PACKAGE_TYPE, restrictionsMap)).thenReturn(null);
        vnfSoftwarePackagePersistenceProvider.getVnfPackageId(PACKAGE_NAME, NFVO_FDN);
        verify(dpsReader).findPOs(UpgradeActivityConstants.UPPKG_NAMESPACE, VranJobConstants.VNF_PACKAGE_TYPE, restrictionsMap);
    }

    @Test
    public void testUpdateSoftwarePackageEntity() {
        when(dpsReader.findPOs(UpgradeActivityConstants.UPPKG_NAMESPACE, VranJobConstants.VNF_PACKAGE_TYPE, restrictionsMap)).thenReturn(matchingPOList);
        vnfSoftwarePackagePersistenceProvider.updateSoftwarePackageEntity(PO_ID, restrictionsMap);
    }

    @Test
    public void testDeleteSoftwarePackageEntity() {
        when(dpsReader.findPOs(UpgradeActivityConstants.UPPKG_NAMESPACE, VranJobConstants.VNF_PACKAGE_TYPE, restrictionsMap)).thenReturn(matchingPOList);
        vnfSoftwarePackagePersistenceProvider.deleteSoftwarePackageEntity(PO_ID);
    }
}
