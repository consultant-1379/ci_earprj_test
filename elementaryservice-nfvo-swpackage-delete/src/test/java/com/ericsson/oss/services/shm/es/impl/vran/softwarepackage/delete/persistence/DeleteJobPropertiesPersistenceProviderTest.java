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

import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityUtil;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

@RunWith(MockitoJUnitRunner.class)
public class DeleteJobPropertiesPersistenceProviderTest {

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private JobPropertyUtils jobPropertyUtils;

    @Mock
    private VranJobActivityUtil vranJobActivityUtil;

    @Mock
    private VranJobActivityServiceHelper vranJobActivityService;

    @Mock
    private DeleteSoftwarePackagePersistenceProvider deleteSoftwarePackagePersistenceProvider;

    @InjectMocks
    private DeleteJobPropertiesPersistenceProvider deleteJobPropertiesPersistenceProvider;

    private static final long ACTIVITY_JOB_ID = 12345;

    @Mock
    private JobEnvironment jobContext;

    private String failedPackageName = "Package-Vpp";

    @Mock
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Before
    public void setUp() throws Exception {
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobContext);
        when(jobContext.getNeJobId()).thenReturn(ACTIVITY_JOB_ID);
        when(jobContext.getNodeName()).thenReturn("VrcNode");
    }

    @Test
    public void test_InitializeActivityVariables() {
        List<Map<String, Object>> neJobProperties = buildneJobPropertiesMap();
        deleteJobPropertiesPersistenceProvider.initializeActivityVariables(ACTIVITY_JOB_ID);
        when(vranJobActivityService.getNeJobAttributes(jobContext)).thenReturn(null);
        deleteJobPropertiesPersistenceProvider.initializeActivityVariables(ACTIVITY_JOB_ID);
        verify(jobAttributesPersistenceProvider, times(2)).persistJobProperties(ACTIVITY_JOB_ID, neJobProperties);
    }

    @Test
    public void test_IncrementSoftwarePackageCurrentIndexInNfvo() {
        List<Map<String, Object>> neJobProperties = setNeJobProperties(VranJobConstants.CURRENT_DELETED_PACKAGE_INDEX_IN_NFVO);
        when(vranJobActivityService.getJobPropertyValue(VranJobConstants.CURRENT_DELETED_PACKAGE_INDEX_IN_NFVO, neJobProperties)).thenReturn("1");
        deleteJobPropertiesPersistenceProvider.incrementSoftwarePackageCurrentIndexInNfvo(ACTIVITY_JOB_ID, jobContext);
        verify(jobAttributesPersistenceProvider).persistJobProperties(ACTIVITY_JOB_ID, neJobProperties);
    }

    @Test
    public void test_IncrementSoftwarePackageCurrentIndexInEnm() {
        List<Map<String, Object>> neJobProperties = setNeJobProperties(VranJobConstants.CURRENT_DELETED_PACKAGE_INDEX_IN_ENM);
        when(vranJobActivityService.getJobPropertyValue(VranJobConstants.CURRENT_DELETED_PACKAGE_INDEX_IN_ENM, neJobProperties)).thenReturn("1");
        deleteJobPropertiesPersistenceProvider.incrementSoftwarePackageCurrentIndexInEnm(ACTIVITY_JOB_ID, jobContext);
        verify(jobAttributesPersistenceProvider).persistJobProperties(ACTIVITY_JOB_ID, neJobProperties);
    }

    @Test
    public void test_IncrementFailedSoftwarePackageCountInNfvo() {
        List<Map<String, Object>> neJobProperties = setNeJobProperties(VranJobConstants.NUMBER_OF_FAILURE_PACKAGES_IN_NFVO);
        when(vranJobActivityService.getJobPropertyValue(VranJobConstants.NUMBER_OF_FAILURE_PACKAGES_IN_NFVO, neJobProperties)).thenReturn("1");
        deleteJobPropertiesPersistenceProvider.incrementFailedSoftwarePackageCountInNfvo(ACTIVITY_JOB_ID, jobContext);
        verify(jobAttributesPersistenceProvider).persistJobProperties(ACTIVITY_JOB_ID, neJobProperties);
    }

    @Test
    public void test_IncrementFailedSoftwarePackageCountForEnm() {
        List<Map<String, Object>> neJobProperties = setNeJobProperties(VranJobConstants.NUMBER_OF_FAILURE_PACKAGES_IN_ENM);
        when(vranJobActivityService.getJobPropertyValue(VranJobConstants.NUMBER_OF_FAILURE_PACKAGES_IN_ENM, neJobProperties)).thenReturn("1");
        deleteJobPropertiesPersistenceProvider.incrementFailedSoftwarePackageCountForEnm(ACTIVITY_JOB_ID, jobContext);
        verify(jobAttributesPersistenceProvider).persistJobProperties(ACTIVITY_JOB_ID, neJobProperties);
    }

    @Test
    public void test_IncrementSuccessSoftwarePackageCountInNfvo() {
        List<Map<String, Object>> neJobProperties = setNeJobProperties(VranJobConstants.NUMBER_OF_SUCCESS_PACKAGES_IN_NFVO);
        when(vranJobActivityService.getJobPropertyValue(VranJobConstants.NUMBER_OF_SUCCESS_PACKAGES_IN_NFVO, neJobProperties)).thenReturn("1");
        deleteJobPropertiesPersistenceProvider.incrementSuccessSoftwarePackageCountInNfvo(ACTIVITY_JOB_ID, jobContext);
        verify(jobAttributesPersistenceProvider).persistJobProperties(ACTIVITY_JOB_ID, neJobProperties);
    }

    @Test
    public void test_IncrementSuccessSoftwarePackageCountForEnm() {
        List<Map<String, Object>> neJobProperties = setNeJobProperties(VranJobConstants.NUMBER_OF_SUCCESS_PACKAGES_IN_ENM);
        when(vranJobActivityService.getJobPropertyValue(VranJobConstants.NUMBER_OF_SUCCESS_PACKAGES_IN_ENM, neJobProperties)).thenReturn("1");
        deleteJobPropertiesPersistenceProvider.incrementSuccessSoftwarePackageCountForEnm(ACTIVITY_JOB_ID, jobContext);
        verify(jobAttributesPersistenceProvider).persistJobProperties(ACTIVITY_JOB_ID, neJobProperties);
    }

    @Test
    public void test_UpdateFailedSoftwarePackagesInNfvo() {
        deleteJobPropertiesPersistenceProvider.updateFailedSoftwarePackagesInNfvo(ACTIVITY_JOB_ID, failedPackageName, jobContext);

        when(vranJobActivityService.getNeJobAttributes(jobContext)).thenReturn(null);
        deleteJobPropertiesPersistenceProvider.updateFailedSoftwarePackagesInNfvo(ACTIVITY_JOB_ID, failedPackageName, jobContext);

        when(vranJobActivityService.getJobPropertyValue(VranJobConstants.FAILED_PACKAGES_FROM_NFVO, null)).thenReturn("package1");
        deleteJobPropertiesPersistenceProvider.updateFailedSoftwarePackagesInNfvo(ACTIVITY_JOB_ID, failedPackageName, jobContext);
    }

    @Test
    public void test_UpdateFailedSoftwarePackagesInEnm() {
        deleteJobPropertiesPersistenceProvider.updateFailedSoftwarePackagesInEnm(ACTIVITY_JOB_ID, failedPackageName, jobContext);

        when(vranJobActivityService.getNeJobAttributes(jobContext)).thenReturn(null);
        deleteJobPropertiesPersistenceProvider.updateFailedSoftwarePackagesInEnm(ACTIVITY_JOB_ID, failedPackageName, jobContext);

        when(vranJobActivityService.getJobPropertyValue(VranJobConstants.FAILED_PACKAGES_FROM_ENM, null)).thenReturn("package1");
        deleteJobPropertiesPersistenceProvider.updateFailedSoftwarePackagesInEnm(ACTIVITY_JOB_ID, failedPackageName, jobContext);
    }

    private List<Map<String, Object>> setNeJobProperties(String propertyName) {
        List<Map<String, Object>> neJobProperties = new ArrayList<Map<String, Object>>();
        Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, propertyName);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "1");
        neJobProperties.add(jobProperty);
        when(vranJobActivityService.getNeJobAttributes(jobContext)).thenReturn(neJobProperties);
        return neJobProperties;
    }

    private List<Map<String, Object>> buildneJobPropertiesMap() {
        List<Map<String, Object>> neJobProperties = new ArrayList<Map<String, Object>>();
        neJobProperties.add(buildJobProperty(VranJobConstants.TOTAL_NUMBER_OF_PACKAGES_IN_NFVO, Integer.toString(2)));
        neJobProperties.add(buildJobProperty(VranJobConstants.CURRENT_DELETED_PACKAGE_INDEX_IN_NFVO, Integer.toString(0)));
        neJobProperties.add(buildJobProperty(VranJobConstants.NUMBER_OF_FAILURE_PACKAGES_IN_NFVO, Integer.toString(0)));
        neJobProperties.add(buildJobProperty(VranJobConstants.NUMBER_OF_SUCCESS_PACKAGES_IN_NFVO, Integer.toString(0)));

        neJobProperties.add(buildJobProperty(VranJobConstants.TOTAL_NUMBER_OF_PACKAGES_IN_ENM, Integer.toString(2)));
        neJobProperties.add(buildJobProperty(VranJobConstants.CURRENT_DELETED_PACKAGE_INDEX_IN_ENM, Integer.toString(0)));
        neJobProperties.add(buildJobProperty(VranJobConstants.NUMBER_OF_FAILURE_PACKAGES_IN_ENM, Integer.toString(0)));
        neJobProperties.add(buildJobProperty(VranJobConstants.NUMBER_OF_SUCCESS_PACKAGES_IN_ENM, Integer.toString(0)));
        return neJobProperties;
    }

    public Map<String, Object> buildJobProperty(final String key, final Object value) {
        final Map<String, Object> jobProperty = new HashMap<String, Object>(2);
        return jobProperty;
    }

}
