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

import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextBuilder;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextForEnm;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextForNfvo;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.MTRSender;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.persistence.DeleteJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

@RunWith(MockitoJUnitRunner.class)
public class HandleTimeoutTaskTest {

    @Mock
    private ActivityUtils activityUtils;

    @InjectMocks
    private HandleTimeoutTask handleTimeoutTask;

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
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Mock
    private MTRSender deleteSoftwarePackageEventSender;

    @Mock
    private DeletePackageContextBuilder deletePackageContextBuilder;

    @Mock
    private JobActivityInfo jobActivityInformation;

    private static final long ACTIVITY_JOB_ID = 1234;

    @Before
    public void setUp() throws Exception {

        final Map<String, Object> mainJobProperties = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobProperties = new ArrayList<Map<String, Object>>();

        when(vranJobActivityService.getMainJobAttributes(jobContext)).thenReturn(mainJobProperties);
        when(vranJobActivityService.getNeJobAttributes(jobContext)).thenReturn(neJobProperties);

        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobContext);

        when(deletePackageContextBuilder.buildDeletePackageContextForEnm(jobContext)).thenReturn(deletePackageContextForEnm);
        when(deletePackageContextBuilder.buildDeletePackageContextForNfvo(jobContext)).thenReturn(deletePackageContextForNfvo);

    }

    @Test
    public void testHandleTimeoutForEnmPackagesDeletionNotComplete() {
        when(deletePackageContextForEnm.isComplete()).thenReturn(false);
        handleTimeoutTask.handleTimeout(ACTIVITY_JOB_ID, jobActivityInformation);

        verify(deleteJobPropertiesPersistenceProvider).incrementFailedSoftwarePackageCountForEnm(ACTIVITY_JOB_ID, jobContext);
    }

    @Test
    public void testHandleTimeoutForEnmPackagesDeletionComplete() {
        when(deletePackageContextForEnm.isComplete()).thenReturn(true);
        handleTimeoutTask.handleTimeout(ACTIVITY_JOB_ID, jobActivityInformation);

        verify(deleteJobPropertiesPersistenceProvider).incrementFailedSoftwarePackageCountInNfvo(ACTIVITY_JOB_ID, jobContext);
    }

    @Test
    public void testHandleTimeoutForNfvoPackagesDeletionComplete() {
        final List<Map<String, Object>> activityJobProperties = new ArrayList<Map<String, Object>>();
        when(deletePackageContextForEnm.isComplete()).thenReturn(true);
        when(deletePackageContextForNfvo.isComplete()).thenReturn(true);
        when(deletePackageContextForNfvo.areAllPackagesFailedToDelete()).thenReturn(false);
        when(deletePackageContextForNfvo.getNodeFdn()).thenReturn(TasksBaseTest.NFVO_FDN);
        handleTimeoutTask.handleTimeout(ACTIVITY_JOB_ID, jobActivityInformation);

        verify(tasksBase).markSoftwarePackageDeleteActivityResult(activityJobProperties, 0);
        verify(tasksBase).sendNfvoVnfPackagesSyncRequest(ACTIVITY_JOB_ID, deletePackageContextForNfvo);
    }

}
