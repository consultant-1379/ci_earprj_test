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
package com.ericsson.oss.services.shm.job.impl;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

import java.util.*;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.modelservice.api.*;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.job.activity.Activity;
import com.ericsson.oss.services.shm.job.activity.NeActivityInformation;
import com.ericsson.oss.services.shm.jobs.common.api.JobActivitiesResponse;

@RunWith(MockitoJUnitRunner.class)
public class CPPBackupJobActivitiesResponseModifierTest {

    @InjectMocks
    private CppBackupJobActivitiesResponseModifier objectUnderTest;

    @Mock
    private JobActivitiesResponse jobActivitiesResponseMock;

    @Mock
    private NetworkElement neMoMock;

    @Mock
    private NetworkElementGroupPreparator neBatchPreparatorMock;

    @Mock
    private NeActivityInformation neActivityInformationMock;

    @Mock
    private Activity createActivityMock;

    @Mock
    private Activity setStartableActivityMock;

    @Mock
    private Activity setFirstInRollbackListActivityMock;

    @Mock
    private Activity exportActivityMock;

    @Mock
    List<Activity> activityListMock;

    @Mock
    ListIterator<Activity> activityListIteratorMock;

    @Mock
    private FdnServiceBean fdnServiceBeanMock;

    @Mock
    private NetworkElementGroup networkElementGroupMock;

    @Test
    public void testgetManageBackupActivities() {
        when(fdnServiceBeanMock.getNetworkElements(anyList())).thenReturn(Arrays.asList(neMoMock));
        when(neBatchPreparatorMock.groupNetworkElementsByModelidentity(eq(Arrays.asList(neMoMock)), eq(FragmentType.ECIM_BRM_TYPE.getFragmentName()))).thenReturn(networkElementGroupMock);
        when(jobActivitiesResponseMock.getNeActivityInformation()).thenReturn(Arrays.asList(neActivityInformationMock));
        when(neActivityInformationMock.getActivity()).thenReturn(activityListMock);
        when(activityListMock.listIterator()).thenReturn(activityListIteratorMock);
        when(activityListIteratorMock.hasNext()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(false);
        when(activityListIteratorMock.next()).thenReturn(createActivityMock).thenReturn(setStartableActivityMock).thenReturn(setFirstInRollbackListActivityMock).thenReturn(exportActivityMock);
        when(createActivityMock.getName()).thenReturn("createcv");
        when(setStartableActivityMock.getName()).thenReturn("setcvasstartable");
        when(setFirstInRollbackListActivityMock.getName()).thenReturn("setcvfirstinrollbacklist");
        when(exportActivityMock.getName()).thenReturn("exportcv");
        final JobActivitiesResponse response = objectUnderTest.getManageBackupActivities(jobActivitiesResponseMock, false);
        Assert.assertFalse(response.toString().contains("createcv"));
    }

    @Test
    public void testgetManageBackupActivitieswithMultipleBackups() {
        when(fdnServiceBeanMock.getNetworkElements(anyList())).thenReturn(Arrays.asList(neMoMock));
        when(neBatchPreparatorMock.groupNetworkElementsByModelidentity(eq(Arrays.asList(neMoMock)), eq(FragmentType.ECIM_BRM_TYPE.getFragmentName()))).thenReturn(networkElementGroupMock);
        when(jobActivitiesResponseMock.getNeActivityInformation()).thenReturn(Arrays.asList(neActivityInformationMock));
        when(neActivityInformationMock.getActivity()).thenReturn(activityListMock);
        when(activityListMock.listIterator()).thenReturn(activityListIteratorMock);
        when(activityListIteratorMock.hasNext()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(false);
        when(activityListIteratorMock.next()).thenReturn(createActivityMock).thenReturn(setStartableActivityMock).thenReturn(setFirstInRollbackListActivityMock).thenReturn(exportActivityMock);
        when(createActivityMock.getName()).thenReturn("createcv");
        when(setStartableActivityMock.getName()).thenReturn("setcvasstartable");
        when(setFirstInRollbackListActivityMock.getName()).thenReturn("setcvfirstinrollbacklist");
        when(exportActivityMock.getName()).thenReturn("exportcv");
        final JobActivitiesResponse response = objectUnderTest.getManageBackupActivities(jobActivitiesResponseMock, true);
        Assert.assertFalse(response.toString().contains("setcvasstartable"));
        Assert.assertFalse(response.toString().contains("setcvfirstinrollbacklist"));
    }

}
