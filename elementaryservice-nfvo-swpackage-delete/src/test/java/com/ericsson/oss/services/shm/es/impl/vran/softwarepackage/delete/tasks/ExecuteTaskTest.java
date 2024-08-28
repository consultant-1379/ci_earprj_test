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
package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.tasks;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextBuilder;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextForEnm;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextForNfvo;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.MTRSender;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.SmrsFileDeleteService;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.persistence.DeleteJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.persistence.DeleteSoftwarePackagePersistenceProvider;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;
import com.ericsson.oss.services.shm.vran.shared.persistence.VnfSoftwarePackagePersistenceProvider;

@RunWith(MockitoJUnitRunner.class)
public class ExecuteTaskTest {

    @Mock
    private ActivityUtils activityUtils;

    @InjectMocks
    private ExecuteTask executeTask;

    @Mock
    private TasksBase tasksBase;

    @Mock
    private JobEnvironment jobContext;

    @Mock
    private DeleteJobPropertiesPersistenceProvider deleteJobPropertiesPersistenceProvider;

    @Mock
    private DeletePackageContextForEnm deletePackageContextForEnm;

    @Mock
    private DeletePackageContextForNfvo deletePackageContextForNfvo;

    @Mock
    private VranJobActivityServiceHelper vranJobActivityService;

    @Mock
    private DeleteSoftwarePackagePersistenceProvider deleteSoftwarePackagePersistenceProvider;

    @Mock
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Mock
    private VnfSoftwarePackagePersistenceProvider vnfSoftwarePackagePersistenceProvider;

    @Mock
    private MTRSender deleteSoftwarePackageEventSender;

    @Mock
    private DeletePackageContextBuilder deletePackageContextBuilder;

    @Mock
    private PersistenceObject vnfSoftwarePackageEntity;

    @Mock
    private SmrsFileDeleteService deleteSoftwarePackageSMRSService;

    private static final long ACTIVITY_JOB_ID = 1234;

    private static final String VRAN_SOFTWARE_PACKAGE = "vnfd-CXP9029327_1-R1HBC";

    private static final String NODE_FDN = "LTE52vSD0001";

    @Test
    public void testExecuteForDeletePackageAvailableOnlyInEnm() {
        when(deletePackageContextForEnm.getCurrentPackage()).thenReturn(VRAN_SOFTWARE_PACKAGE);
        when(vnfSoftwarePackagePersistenceProvider.getVnfSoftwarePackageEntity(VRAN_SOFTWARE_PACKAGE)).thenReturn(vnfSoftwarePackageEntity);
        when(vnfSoftwarePackageEntity.getAttribute(VranJobConstants.SW_PACKAGE_LOCATION)).thenReturn(VranJobConstants.SMRS);
        executeTask.deleteSoftwarePackageFromEnm(ACTIVITY_JOB_ID, deletePackageContextForEnm);
        verify(deletePackageContextForEnm,times(2)).getCurrentPackage();
        verify(vnfSoftwarePackagePersistenceProvider).getVnfSoftwarePackageEntity(VRAN_SOFTWARE_PACKAGE);
    }

    @Test
    public void testExecuteForDeletePackageAvailableOnlyInNfvo() {
        when(deletePackageContextForNfvo.getCurrentPackage()).thenReturn(VRAN_SOFTWARE_PACKAGE);
        when(deletePackageContextForNfvo.getNodeFdn()).thenReturn(NODE_FDN);
        when(vnfSoftwarePackagePersistenceProvider.getVnfSoftwarePackageEntity(VRAN_SOFTWARE_PACKAGE)).thenReturn(vnfSoftwarePackageEntity);
        executeTask.deleteSoftwarePackageFromNfvo(ACTIVITY_JOB_ID, deletePackageContextForNfvo);
        verify(deletePackageContextForNfvo,times(1)).getCurrentPackage();
        verify(deletePackageContextForNfvo,times(1)).getNodeFdn();
    }

    @Test
    public void testExecuteForDeletePackageWhichIsInUse() {
        when(deletePackageContextForEnm.getCurrentPackage()).thenReturn(VRAN_SOFTWARE_PACKAGE);
        when(vnfSoftwarePackagePersistenceProvider.getVnfSoftwarePackageEntity(VRAN_SOFTWARE_PACKAGE)).thenReturn(vnfSoftwarePackageEntity);
        when(vnfSoftwarePackageEntity.getAttribute(VranJobConstants.SW_PACKAGE_LOCATION)).thenReturn(VranJobConstants.SMRS_NFVO);
        when(deleteSoftwarePackagePersistenceProvider.isSoftwarePackageInUse(VranJobConstants.SW_PACKAGE_LOCATION)).thenReturn(true);
        executeTask.deleteSoftwarePackageFromEnm(ACTIVITY_JOB_ID,deletePackageContextForEnm);
        verify(deletePackageContextForEnm,times(2)).getCurrentPackage();
        verify(vnfSoftwarePackagePersistenceProvider).getVnfSoftwarePackageEntity(VRAN_SOFTWARE_PACKAGE);
    }

    @Test
    public void testExecuteForDeletePackageWhichIsNotInDB() {
        when(deletePackageContextForEnm.getCurrentPackage()).thenReturn(VRAN_SOFTWARE_PACKAGE);
        when(vnfSoftwarePackagePersistenceProvider.getVnfSoftwarePackageEntity(VRAN_SOFTWARE_PACKAGE)).thenReturn(null);
        executeTask.deleteSoftwarePackageFromEnm(ACTIVITY_JOB_ID, deletePackageContextForEnm);
        verify(vnfSoftwarePackagePersistenceProvider).getVnfSoftwarePackageEntity(VRAN_SOFTWARE_PACKAGE);
    }

}
