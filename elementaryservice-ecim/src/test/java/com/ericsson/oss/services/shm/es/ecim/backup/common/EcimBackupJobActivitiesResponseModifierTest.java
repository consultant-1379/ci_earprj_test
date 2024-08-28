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
package com.ericsson.oss.services.shm.es.ecim.backup.common;

import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElementGroup;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElementGroupPreparator;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfo;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.inventory.backup.ecim.api.BrmHandler;
import com.ericsson.oss.services.shm.inventory.backup.ecim.api.BrmVersionHandlersProviderFactory;
import com.ericsson.oss.services.shm.inventory.backup.ecim.api.EcimBackupItem;
import com.ericsson.oss.services.shm.job.activity.Activity;
import com.ericsson.oss.services.shm.job.activity.ActivityParams;
import com.ericsson.oss.services.shm.job.activity.NeActivityInformation;
import com.ericsson.oss.services.shm.job.activity.NodeparamType;
import com.ericsson.oss.services.shm.jobs.common.api.JobActivitiesQuery;
import com.ericsson.oss.services.shm.jobs.common.api.JobActivitiesResponse;
import com.ericsson.oss.services.shm.jobs.common.api.NeInfoQuery;

@RunWith(MockitoJUnitRunner.class)
public class EcimBackupJobActivitiesResponseModifierTest {

    @InjectMocks
    private EcimBackupJobActivitiesResponseModifier objectUnderTest;

    @Mock
    private JobActivitiesResponse jobActivitiesResponseMock;

    @Mock
    private JobActivitiesQuery jobActivitiesQueryMock;

    @Mock
    private NeInfoQuery neInfoQueryMock;

    @Mock
    List<Activity> activityListMock;

    @Mock
    ListIterator<Activity> activityListIteratorMock;

    @Mock
    private Activity createBackupMock;

    @Mock
    private Activity UploadBackupActivityMock;

    @Mock
    private FdnServiceBean fdnServiceBeanMock;

    @Mock
    private NetworkElementGroupPreparator neBatchPreparatorMock;

    @Mock
    private BrmVersionHandlersProviderFactory handlersProviderFactoryMock;

    @Mock
    private NetworkElement neMoMock;

    @Mock
    private NetworkElementGroup networkElementGroupMock;

    @Mock
    private Set<Entry<OssModelInfo, List<NetworkElement>>> entrySetMock;

    @Mock
    private OssModelInfo ossModelInfoMock;

    @Mock
    private BrmHandler brmHandlerMock;

    @Mock
    private EcimBackupItem ecimBackupItemMock1;

    @Mock
    private EcimBackupItem ecimBackupItemMock2;

    @Mock
    private EcimBackupItem ecimBackupItemMock3;

    @Mock
    private EcimBackupItem ecimBackupItemMock4;

    @Mock
    private NeActivityInformation neActivityInformationMock;

    @Mock
    private Activity activityMock;

    @Mock
    private ActivityParams activityParamsMock;

    @Mock
    private NodeparamType nodeparamType;

    @Mock
    private List<String> nodeParamItemsMock;

    @Mock
    private Map<String, List<EcimBackupItem>> nodesBackupActivityItems;

    @Mock
    private EcimBackupUtils ecimBackupUtils;

    @Test
    public void testgetUpdatedJobActivitiesWithEmptyInput() throws UnsupportedFragmentException {
        when(fdnServiceBeanMock.getNetworkElements(anyList())).thenReturn(Arrays.asList(neMoMock));
        when(neBatchPreparatorMock.groupNetworkElementsByModelidentity(eq(Arrays.asList(neMoMock)), eq(FragmentType.ECIM_BRM_TYPE.getFragmentName()))).thenReturn(networkElementGroupMock);
        final JobActivitiesResponse response = objectUnderTest.getUpdatedJobActivities(neInfoQueryMock, jobActivitiesResponseMock);
        Assert.assertNotNull(response);
    }

    @Test
    public void testgetUpdatedJobActivities() throws UnsupportedFragmentException {
        when(fdnServiceBeanMock.getNetworkElements(anyList())).thenReturn(Arrays.asList(neMoMock));
        when(neBatchPreparatorMock.groupNetworkElementsByModelidentity(eq(Arrays.asList(neMoMock)), eq(FragmentType.ECIM_BRM_TYPE.getFragmentName()))).thenReturn(networkElementGroupMock);
        final JobActivitiesResponse response = objectUnderTest.getUpdatedJobActivities(neInfoQueryMock, jobActivitiesResponseMock);
        Assert.assertNotNull(response);
        Assert.assertEquals(jobActivitiesResponseMock, response);

    }

    @Test
    public void testgetUpdatedJobActivities_noNEGroups_BrmHandlerNotCalled() throws UnsupportedFragmentException {
        when(fdnServiceBeanMock.getNetworkElements(anyList())).thenReturn(Arrays.asList(neMoMock));
        when(neBatchPreparatorMock.groupNetworkElementsByModelidentity(eq(Arrays.asList(neMoMock)), eq(FragmentType.ECIM_BRM_TYPE.getFragmentName()))).thenReturn(networkElementGroupMock);
        when(networkElementGroupMock.getNetworkElementMap()).thenReturn(Collections.EMPTY_MAP);

        final JobActivitiesResponse response = objectUnderTest.getUpdatedJobActivities(neInfoQueryMock, jobActivitiesResponseMock);
        Assert.assertNotNull(response);
    }

    @Test
    public void testgetUpdatedJobActivities_calledBrmHandlerButnotSetNodeParams() throws UnsupportedFragmentException {
        when(fdnServiceBeanMock.getNetworkElements(anyList())).thenReturn(Arrays.asList(neMoMock));
        when(neBatchPreparatorMock.groupNetworkElementsByModelidentity(eq(Arrays.asList(neMoMock)), eq(FragmentType.ECIM_BRM_TYPE.getFragmentName()))).thenReturn(networkElementGroupMock);
        final Map<OssModelInfo, List<NetworkElement>> mapMock = new HashMap<OssModelInfo, List<NetworkElement>>();
        mapMock.put(ossModelInfoMock, Arrays.asList(neMoMock));
        when(networkElementGroupMock.getNetworkElementMap()).thenReturn(mapMock);
        when(handlersProviderFactoryMock.getBrmHandler(null)).thenReturn(brmHandlerMock);
        when(brmHandlerMock.getBackupActivityItems(Arrays.asList(neMoMock), null)).thenReturn(nodesBackupActivityItems);

        final JobActivitiesResponse response = objectUnderTest.getUpdatedJobActivities(neInfoQueryMock, jobActivitiesResponseMock);
        Assert.assertNotNull(response);
        Assert.assertEquals(jobActivitiesResponseMock, response);
    }

    @Test
    public void testgetUpdatedJobActivitiesNotUpdatesTheNodeParam() throws UnsupportedFragmentException {
        when(fdnServiceBeanMock.getNetworkElements(anyList())).thenReturn(Arrays.asList(neMoMock));
        when(neBatchPreparatorMock.groupNetworkElementsByModelidentity(eq(Arrays.asList(neMoMock)), eq(FragmentType.ECIM_BRM_TYPE.getFragmentName()))).thenReturn(networkElementGroupMock);
        final Map<OssModelInfo, List<NetworkElement>> mapMock = new HashMap<OssModelInfo, List<NetworkElement>>();
        mapMock.put(ossModelInfoMock, Arrays.asList(neMoMock));
        when(networkElementGroupMock.getNetworkElementMap()).thenReturn(mapMock);
        when(handlersProviderFactoryMock.getBrmHandler(null)).thenReturn(brmHandlerMock);
        when(brmHandlerMock.getBackupActivityItems(Arrays.asList(neMoMock), null)).thenReturn(nodesBackupActivityItems);
        when(jobActivitiesResponseMock.getNeActivityInformation()).thenReturn(Arrays.asList(neActivityInformationMock));
        when(neActivityInformationMock.getActivity()).thenReturn(Arrays.asList(activityMock));
        when(activityMock.getActivityParams()).thenReturn(Arrays.asList(activityParamsMock));
        when(activityParamsMock.getNodeparam()).thenReturn(Arrays.asList(nodeparamType));
        when(nodeparamType.getItem()).thenReturn(nodeParamItemsMock);

        final JobActivitiesResponse response = objectUnderTest.getUpdatedJobActivities(neInfoQueryMock, jobActivitiesResponseMock);
        Assert.assertNotNull(response);
        Assert.assertEquals(jobActivitiesResponseMock, response);
    }

    @Test
    public void testgetUpdatedJobActivitiesUpdatesTheNodeParam() throws UnsupportedFragmentException {
        when(fdnServiceBeanMock.getNetworkElements(anyList())).thenReturn(Arrays.asList(neMoMock));
        when(neBatchPreparatorMock.groupNetworkElementsByModelidentity(eq(Arrays.asList(neMoMock)), eq(FragmentType.ECIM_BRM_TYPE.getFragmentName()))).thenReturn(networkElementGroupMock);
        final Map<OssModelInfo, List<NetworkElement>> mapMock = new HashMap<OssModelInfo, List<NetworkElement>>();
        mapMock.put(ossModelInfoMock, Arrays.asList(neMoMock));
        when(networkElementGroupMock.getNetworkElementMap()).thenReturn(mapMock);
        when(handlersProviderFactoryMock.getBrmHandler(null)).thenReturn(brmHandlerMock);
        nodesBackupActivityItems = new HashMap<String, List<EcimBackupItem>>();
        nodesBackupActivityItems.put("1", Arrays.asList(ecimBackupItemMock1, ecimBackupItemMock4));
        nodesBackupActivityItems.put("2", Arrays.asList(ecimBackupItemMock2, ecimBackupItemMock4));
        nodesBackupActivityItems.put("3", Arrays.asList(ecimBackupItemMock3, ecimBackupItemMock4));
        when(ecimBackupItemMock1.getDomain()).thenReturn("D1");
        when(ecimBackupItemMock1.getType()).thenReturn("T1");
        when(ecimBackupItemMock2.getDomain()).thenReturn("D2");
        when(ecimBackupItemMock2.getType()).thenReturn("T2");
        when(ecimBackupItemMock3.getDomain()).thenReturn("D3");
        when(ecimBackupItemMock3.getType()).thenReturn("T3");
        when(ecimBackupItemMock4.getDomain()).thenReturn("D4");
        when(ecimBackupItemMock4.getType()).thenReturn("T4");
        final Set<String> domainType = new HashSet<String>();
        domainType.add("system/systemdata");
        domainType.add("system/systemlocal");

        when(brmHandlerMock.getBackupActivityItems(Arrays.asList(neMoMock), null)).thenReturn(nodesBackupActivityItems);
        when(ecimBackupUtils.getNodeBackupActivityItems(neInfoQueryMock)).thenReturn(nodesBackupActivityItems);
        when(ecimBackupUtils.getCommonBackupActivityItems(nodesBackupActivityItems)).thenReturn(domainType);
        when(jobActivitiesResponseMock.getNeActivityInformation()).thenReturn(Arrays.asList(neActivityInformationMock));
        when(neActivityInformationMock.getActivity()).thenReturn(Arrays.asList(activityMock));
        when(activityMock.getActivityParams()).thenReturn(Arrays.asList(activityParamsMock));
        when(activityParamsMock.getNodeparam()).thenReturn(Arrays.asList(nodeparamType));
        when(nodeparamType.getItem()).thenReturn(nodeParamItemsMock);

        final JobActivitiesResponse response = objectUnderTest.getUpdatedJobActivities(neInfoQueryMock, jobActivitiesResponseMock);
        Assert.assertNotNull(response);
        Assert.assertEquals(jobActivitiesResponseMock, response);

        verify(nodeParamItemsMock, times(1)).addAll(anyCollection());
    }

    @Test
    public void testgetManageBackupActivities() {
        when(fdnServiceBeanMock.getNetworkElements(anyList())).thenReturn(Arrays.asList(neMoMock));
        when(neBatchPreparatorMock.groupNetworkElementsByModelidentity(eq(Arrays.asList(neMoMock)), eq(FragmentType.ECIM_BRM_TYPE.getFragmentName()))).thenReturn(networkElementGroupMock);
        when(jobActivitiesResponseMock.getNeActivityInformation()).thenReturn(Arrays.asList(neActivityInformationMock));
        when(neActivityInformationMock.getActivity()).thenReturn(activityListMock);
        when(activityListMock.listIterator()).thenReturn(activityListIteratorMock);
        when(activityListIteratorMock.hasNext()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(false);
        when(activityListIteratorMock.next()).thenReturn(createBackupMock).thenReturn(UploadBackupActivityMock);
        when(createBackupMock.getName()).thenReturn("createbackup");
        when(UploadBackupActivityMock.getName()).thenReturn("uploadbackup");
        final JobActivitiesResponse response = objectUnderTest.getManageBackupActivities(jobActivitiesResponseMock, false);
        Assert.assertFalse(response.toString().contains("createbackup"));
    }

}
