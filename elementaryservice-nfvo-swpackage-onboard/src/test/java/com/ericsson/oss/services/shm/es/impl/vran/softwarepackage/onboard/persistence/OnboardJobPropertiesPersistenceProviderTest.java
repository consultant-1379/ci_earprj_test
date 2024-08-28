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
 *----------------------------------------------------------------------------
 **/
package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.persistence;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.vran.onboard.api.notifications.NfvoSoftwarePackageJobResponse;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityUtil;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;
import com.ericsson.oss.services.shm.vran.shared.persistence.VnfSoftwarePackagePersistenceProvider;

@RunWith(MockitoJUnitRunner.class)
public class OnboardJobPropertiesPersistenceProviderTest {

    @InjectMocks
    private OnboardJobPropertiesPersistenceProvider onboardJobPropertiesPersistenceService;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private VranJobActivityServiceHelper vranJobActivityService;

    @Mock
    private JobEnvironment jobContext;

    @Mock
    private VnfSoftwarePackagePersistenceProvider vnfSoftwarePackagePersistenceProvider;

    @Mock
    private JobActivityInfo jobActivityInformation;

    @Mock
    private NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse;

    @Mock
    private VranJobActivityUtil vranJobActivityUtil;

    @Mock
    private JobPropertyUtils jobPropertyUtils;

    @Mock
    private Map<String, Object> activityJobProperties;

    @Mock
    private Map<String, String> onboardPackagesDetails;

    @Mock
    private Map<String, Object> activityJobAttributes;

    @Mock
    private JobUpdateService jobUpdateService;

    @Mock
    private List<Map<String, Object>> neJobProperties;

    @Mock
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    public static final long ACTIVITY_JOB_ID = 12345L;
    public static final long NE_JOB_ID = 12345L;
    public static final String PACKAGES_LIST = "PKG1|PKG2|PKG3";
    protected static final String[] VNF_PACKAGES = { "PKG1", "PKG2", "PKG3" };

    private String neName = "HPE-NFV-Director-001";

    @Test
    public void testInitializeOnboardActivityVariables() {

        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobProperties = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        neJobProperties.add(map);
        final Map<String, Object> map1 = new HashMap<String, Object>();
        neJobProperties.add(map1);
        final Map<String, Object> map2 = new HashMap<String, Object>();
        neJobProperties.add(map2);
        final Map<String, Object> map3 = new HashMap<String, Object>();
        neJobProperties.add(map3);
        final List<String> jobPropertyKeys = new ArrayList<String>();
        when(jobContext.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        jobPropertyKeys.add(VranJobConstants.VNF_PACKAGES_TO_ONBOARD);
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobContext);
        when(jobContext.getNodeName()).thenReturn(neName);
        when(vranJobActivityService.getPropertyFromNEJobPropeties(VranJobConstants.VNF_PACKAGES_TO_ONBOARD, jobConfigurationDetails, jobContext.getNodeName())).thenReturn(PACKAGES_LIST);
        when(vranJobActivityService.getMainJobAttributes(jobContext)).thenReturn(jobConfigurationDetails);
        when(onboardPackagesDetails.get(VranJobConstants.VNF_PACKAGES_TO_ONBOARD)).thenReturn(PACKAGES_LIST);
        when(vranJobActivityUtil.splitSoftwarePackages(PACKAGES_LIST)).thenReturn(VNF_PACKAGES);
        when(activityJobAttributes.get(ShmConstants.NE_JOB_ID)).thenReturn(NE_JOB_ID);
        onboardJobPropertiesPersistenceService.initializeOnboardActivityVariables(ACTIVITY_JOB_ID);
        verify(jobAttributesPersistenceProvider).persistJobProperties(NE_JOB_ID, neJobProperties);
    }

    @Test
    public void testupdateCurrentSoftwarePackageIndex() {
        final List<Map<String, Object>> neJobProperties = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobContext);
        when(jobContext.getActivityJobAttributes()).thenReturn(activityJobAttributes);

        when(vranJobActivityService.getJobPropertyValue(VranJobConstants.CURRENT_PACKAGE_INDEX_TO_BE_ONBOARDED, neJobProperties)).thenReturn("2");
        when(activityJobAttributes.get(ShmConstants.NE_JOB_ID)).thenReturn(NE_JOB_ID);
        onboardJobPropertiesPersistenceService.updateCurrentSoftwarePackageIndex(ACTIVITY_JOB_ID, VranJobConstants.CURRENT_PACKAGE_INDEX_TO_BE_ONBOARDED);
        neJobProperties.add(neJobAttributes);
        verify(jobAttributesPersistenceProvider).persistJobProperties(NE_JOB_ID, neJobProperties);
    }

    @Test
    public void testIncrementSoftwarePackageCurrentIndexToBeOnboarded() {
        final List<Map<String, Object>> neJobProperties = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();

        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobContext);
        when(jobContext.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(jobContext.getNeJobId()).thenReturn(NE_JOB_ID);

        when(vranJobActivityService.getJobPropertyValue(VranJobConstants.CURRENT_PACKAGE_INDEX_TO_BE_ONBOARDED, neJobProperties)).thenReturn("2");
        onboardJobPropertiesPersistenceService.incrementSoftwarePackageCurrentIndexToBeOnboarded(ACTIVITY_JOB_ID, jobContext);
        neJobProperties.add(neJobAttributes);
        verify(jobAttributesPersistenceProvider).persistJobProperties(NE_JOB_ID, neJobProperties);
    }

    @Test
    public void testIncrementOnboardFailedSoftwarePackagesCount() {
        final List<Map<String, Object>> neJobProperties = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();

        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobContext);
        when(jobContext.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(jobContext.getNeJobId()).thenReturn(NE_JOB_ID);

        when(vranJobActivityService.getJobPropertyValue(VranJobConstants.ONBOARD_FAILURE_PACKAGES_COUNT, neJobProperties)).thenReturn("2");
        onboardJobPropertiesPersistenceService.incrementOnboardFailedSoftwarePackagesCount(ACTIVITY_JOB_ID, jobContext);
        neJobProperties.add(neJobAttributes);
        verify(jobAttributesPersistenceProvider).persistJobProperties(NE_JOB_ID, neJobProperties);

    }

    @Test
    public void testIncrementOnboardSuccessSoftwarePackagesCount() {
        final List<Map<String, Object>> neJobProperties = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobContext);
        when(jobContext.getActivityJobAttributes()).thenReturn(activityJobAttributes);

        when(vranJobActivityService.getJobPropertyValue(VranJobConstants.ONBOARD_SUCCESS_PACKAGES_COUNT, neJobProperties)).thenReturn("2");
        neJobProperties.add(neJobAttributes);
        onboardJobPropertiesPersistenceService.incrementOnboardSuccessSoftwarePackagesCount(ACTIVITY_JOB_ID, jobContext);

    }

}
