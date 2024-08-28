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
package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common;

import static org.mockito.Mockito.when;

import java.util.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityUtil;

@RunWith(MockitoJUnitRunner.class)
public class DeletePackageContextBuilderTest {

    @Mock
    private VranJobActivityUtil vranJobActivityUtil;

    @Mock
    private VranJobActivityServiceHelper vranJobActivityService;

    @InjectMocks
    private DeletePackageContextBuilder deletePackageContextBuilder;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private JobEnvironment jobContext;

    private static final long ACTIVITY_JOB_ID = 12345;

    private Map<String, Object> mainJobProperties = new HashMap<String, Object>();

    private String packageNames = "Package|Package2";

    @Before
    public void setUp() throws Exception {
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobContext);
        mainJobProperties.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, "j1");
        when(vranJobActivityService.getMainJobAttributes(jobContext)).thenReturn(mainJobProperties);
    }

    @Test
    public void testBuildDeletePackageContextForEnm() {
        when(vranJobActivityService.getPropertyFromNETypeJobPropeties(VranJobConstants.DELETE_VNF_PACKAGES_FROM_ENM, mainJobProperties, VranJobConstants.NFVO)).thenReturn(packageNames);
        when(vranJobActivityUtil.splitSoftwarePackages(packageNames)).thenReturn(packageNames.split("\\|"));
        deletePackageContextBuilder.buildDeletePackageContextForEnm(jobContext);
    }

    @Test
    public void testBuildDeletePackageContextForNfvo() {
        when(vranJobActivityService.getPropertyFromNETypeJobPropeties(VranJobConstants.DELETE_VNF_PACKAGES_FROM_NFVO, mainJobProperties, VranJobConstants.NFVO)).thenReturn(packageNames);
        when(vranJobActivityUtil.splitSoftwarePackages(packageNames)).thenReturn(packageNames.split("\\|"));
        deletePackageContextBuilder.buildDeletePackageContextForNfvo(jobContext);
    }
}
