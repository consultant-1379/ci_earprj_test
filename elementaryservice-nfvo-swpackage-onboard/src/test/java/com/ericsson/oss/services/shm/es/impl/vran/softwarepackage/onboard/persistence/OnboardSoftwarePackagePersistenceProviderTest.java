package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.persistence;
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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.shared.persistence.VnfSoftwarePackagePersistenceProvider;

/**
 * Test class to test OnboardJobServiceDpsUtil methods
 * 
 * @author xjhosye
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class OnboardSoftwarePackagePersistenceProviderTest {

    @InjectMocks
    private VnfSoftwarePackagePersistenceProvider vnfSoftwarePackagePersistenceProvider;

    @Mock
    private DpsReader dpsReader;

    @Mock
    private PersistenceObject persistenceObj;

    @Mock
    private PersistenceObject persistenceObj1;

    @Mock
    private PersistenceObject persistenceObj2;

    @Mock
    private PersistenceObject persistenceObj3;

    @Mock
    private List<Map<String, Object>> jobProperties;

    @Mock
    private JobUpdateService jobUpdateService;

    public static final String FILE_PATH = "/home/smrs/smrsroot/software/vpp/vRC-1-0_90_1.0";
    public static final long JOB_ID = 281474977695902L;

    @Test
    public void testSMRSFilePathBasesOnPackageName() {
        final String packageName = "TestPackageName";
        final List<PersistenceObject> nfvoPackages = new ArrayList<PersistenceObject>();
        nfvoPackages.add(persistenceObj);
        nfvoPackages.add(persistenceObj1);
        nfvoPackages.add(persistenceObj2);
        nfvoPackages.add(persistenceObj3);
        final Map<String, Object> smrsMap = new HashMap<String, Object>();
        smrsMap.put(UpgradeActivityConstants.UP_PO_FILE_PATH, FILE_PATH);

        final Map<String, Object> restrictionsMap = new HashMap<String, Object>();
        restrictionsMap.put(UpgradeActivityConstants.UP_PO_PACKAGE_NAME, packageName);
        when(dpsReader.findPOs(UpgradeActivityConstants.UPPKG_NAMESPACE, VranJobConstants.VNF_PACKAGE_TYPE, restrictionsMap)).thenReturn(nfvoPackages);
        vnfSoftwarePackagePersistenceProvider.getVnfPackageSMRSPath(packageName);
        verify(dpsReader, times(1)).findPOs(UpgradeActivityConstants.UPPKG_NAMESPACE, VranJobConstants.VNF_PACKAGE_TYPE, restrictionsMap);

    }

}