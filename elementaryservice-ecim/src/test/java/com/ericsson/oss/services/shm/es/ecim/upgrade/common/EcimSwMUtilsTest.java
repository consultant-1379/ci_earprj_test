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
package com.ericsson.oss.services.shm.es.ecim.upgrade.common;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.upgrade.remote.api.RemoteSoftwarePackageManager;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimCommonConstants;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimCommonConstants.UpgradePackageMoConstants;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

@RunWith(MockitoJUnitRunner.class)
public class EcimSwMUtilsTest {

    @Mock
    protected ActivityUtils activityUtils;

    @Mock
    protected JobUpdateService jobUpdateService;

    @Mock
    protected DataPersistenceService dataPersistenceService;

    @Mock
    protected JobPropertyUtils jobPropertyUtils;

    @Mock
    private List<NetworkElement> neElementListMock;

    @Mock
    private NetworkElement neElementMock;

    @Mock
    private JobEnvironment jobEnvironment;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    private NeJobStaticDataProvider jobStaticDataProvider;

    @Inject
    @Mock
    private RemoteSoftwarePackageManager remoteSoftwarePackageManager;

    @Mock
    private JobConfigurationServiceRetryProxy configurationServiceRetryProxy;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Mock
    private FragmentHandler fragmentHandler;

    @Mock
    private JobLogUtil jobLogUtil;

    long activityJobId = 123L;
    long neJobId = 123L;
    long mainJobId = 123L;
    final String nodeName = "NodeName";
    private static final String filePath = "/home/smrs/smrsroot/software/radionode/RadioNode_R14XT_release_upgrade_package";
    private static final String swPkgName = "swPkgName";

    @InjectMocks
    EcimSwMUtils ecimSwMUtils;

    NetworkElement networkElement = null;

    @SuppressWarnings("unchecked")
    @Test
    public void getUpgradeEnvironmentDataTest() throws JobDataNotFoundException, MoNotFoundException {

        mockJobEnvironment();
        createNetworkElement();

        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> jobConfiguration = new HashMap<String, Object>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, EcimCommonConstants.ACTION_TRIGGERED);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "prepare");
        jobPropertyList.add(jobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        neJobAttributes.put(ShmConstants.NE_NAME, nodeName);
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, "123@NodeName");
        mainJobAttributes.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfiguration);

        final Map<String, String> keyValueMap = new HashMap<String, String>();
        keyValueMap.put(UpgradePackageMoConstants.UP_MO_IGNORE_BREAK_POINTS, "false");
        keyValueMap.put(UpgradeActivityConstants.SWP_NAME, swPkgName);

        when(jobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(jobEnvironment.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        when(jobEnvironment.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttributes);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);

        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn("SGSN-MME");

        when(jobPropertyUtils.getPropertyValue(Matchers.anyListOf(String.class), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(keyValueMap);
        final List<String> filePaths = new ArrayList<String>();
        filePaths.add(filePath);
        when(remoteSoftwarePackageManager.getUpgradePackageDetails(swPkgName)).thenReturn(filePaths);
        final EcimUpgradeInfo upgradeEnvironment = ecimSwMUtils.getEcimUpgradeInformation(activityJobId);

        Assert.assertEquals("prepare", upgradeEnvironment.getActionTriggered());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetEcimUpgradeInformation() throws JobDataNotFoundException, MoNotFoundException {

        mockJobEnvironment();
        createNetworkElement();

        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> jobConfiguration = new HashMap<String, Object>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, EcimCommonConstants.ACTION_TRIGGERED);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "prepare");
        jobPropertyList.add(jobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        neJobAttributes.put(ShmConstants.NE_NAME, nodeName);
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, "123@NodeName");
        mainJobAttributes.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfiguration);

        final Map<String, String> keyValueMap = new HashMap<String, String>();
        keyValueMap.put(UpgradePackageMoConstants.UP_MO_IGNORE_BREAK_POINTS, "false");
        keyValueMap.put(UpgradeActivityConstants.SWP_NAME, swPkgName);
        keyValueMap.put(UpgradeActivityConstants.UPGRADE_TYPE, "SOFT");

        when(jobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(jobEnvironment.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        when(jobEnvironment.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttributes);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);

        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn("SGSN-MME");

        when(jobPropertyUtils.getPropertyValue(Matchers.anyListOf(String.class), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(keyValueMap);
        final List<String> filePaths = new ArrayList<String>();
        filePaths.add(filePath);
        when(remoteSoftwarePackageManager.getUpgradePackageDetails(swPkgName)).thenReturn(filePaths);
        final EcimUpgradeInfo upgradeEnvironment = ecimSwMUtils.getEcimUpgradeInformation(activityJobId);

        Assert.assertEquals("prepare", upgradeEnvironment.getActionTriggered());
    }

    private void createNetworkElement() {
        networkElement = new NetworkElement();
        networkElement.setPlatformType(PlatformTypeEnum.ECIM);
    }

    private void mockJobEnvironment() {
        Mockito.when(neJobStaticData.getNodeName()).thenReturn(nodeName);
    }

    @Test
    public void testInitiateActivity() {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        ecimSwMUtils.initializeActivityJobLogs(nodeName, ActivityConstants.PREPARE, jobLogList);

        verify(fragmentHandler, times(1)).logNodeFragmentInfo(nodeName, FragmentType.ECIM_SWM_TYPE, jobLogList);
    }

}
