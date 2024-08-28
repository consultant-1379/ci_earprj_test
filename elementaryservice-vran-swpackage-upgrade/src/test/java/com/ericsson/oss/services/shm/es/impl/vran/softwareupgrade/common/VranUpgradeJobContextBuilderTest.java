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
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common;

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

import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.common.VNFMInformationProvider;
import com.ericsson.oss.services.shm.vran.common.VranActivityUtil;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityUtil;

@RunWith(MockitoJUnitRunner.class)
public class VranUpgradeJobContextBuilderTest {

    @InjectMocks
    private VranUpgradeJobContextBuilder vranUpgradeJobContextBuilder;

    @Mock
    private UpgradePackageContext upgradePackageContext;

    @Mock
    private JobEnvironment jobEnvironment;

    @Mock
    private Map<String, Object> activityJobProperties;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private VranJobActivityServiceHelper vranJobActivityServiceHelper;

    @Mock
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    @Mock
    private VNFMInformationProvider vNFMInformationProvider;

    @Mock
    private VranJobActivityUtil vranJobActivityUtil;

    @Mock
    private VranActivityUtil vranActivityUtil;

    static final long activityJobId = 345;

    @Before
    public void mockJobEnvironment() {

        final String nodeName = "VRAN";
        final String softwarePackageName = "TESTVRAN";
        final String JobLogMessge = "";
        final int mainJobId = 12121;

        final List<Map<String, Object>> activityJobPropertiesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJobPropertiesMap = new HashMap<String, Object>();
        activityJobPropertiesMap.put(ActivityConstants.JOB_PROPERTIES, 1233445);
        activityJobPropertiesMap.put(ActivityConstants.JOB_PROP_KEY, "vnfJobId");
        activityJobPropertiesMap.put(ActivityConstants.JOB_PROP_VALUE, "12");
        activityJobPropertiesMap.put("neJobId", 1234L);
        activityJobPropertiesList.add(activityJobPropertiesMap);

        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertieslist = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperties = new HashMap<String, Object>();
        neJobProperties.put(VranJobConstants.VNF_DESCRIPTOR_ID, "VnfDescId");
        neJobProperties.put(VranJobConstants.VNF_PACKAGE_ID, "VnfPkgId");
        neJobProperties.put(VranJobConstants.VNF_ID, "VnfId");
        neJobProperties.put(VranJobConstants.VNF_JOB_ID, activityJobId);
        neJobPropertieslist.add(neJobProperties);
        when(vranJobActivityServiceHelper.getActivityJobAttributes(jobEnvironment)).thenReturn(activityJobPropertiesList);
        when(upgradePackageContext.getSoftwarePackageName()).thenReturn(nodeName);
        when(upgradePackageContext.getJobEnvironment()).thenReturn(jobEnvironment);
        when(upgradePackageContext.getJobEnvironment().getNodeName()).thenReturn(softwarePackageName);
        when(upgradePackageContext.getJobEnvironment().getActivityJobAttributes()).thenReturn(activityJobProperties);
        when(upgradePackageContext.getJobEnvironment().getActivityJobAttributes().get("neJobId")).thenReturn(1234L);
        when(upgradePackageContext.getJobEnvironment().getNeJobAttributes()).thenReturn(neJobAttributes);
        when(upgradePackageContext.getJobEnvironment().getNeJobAttributes().get(ActivityConstants.JOB_PROPERTIES)).thenReturn(neJobProperties);
        when(upgradePackageContext.getVnfJobId()).thenReturn(345);
        when(upgradePackageContext.getVnfId()).thenReturn("123");
        when(upgradePackageContext.getVnfmFdn()).thenReturn("vnfmFdn");
        when(activityUtils.additionalInfoForEvent(activityJobId, nodeName, JobLogMessge)).thenReturn("");
        when(jobEnvironment.getNeJobAttributes()).thenReturn(neJobProperties);
        when(activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.UPGRADE)).thenReturn("");
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(vranJobActivityServiceHelper.getJobPropertyValueAsString(VranJobConstants.ACTION_TRIGGERED, activityJobPropertiesList)).thenReturn(ActivityConstants.PREPARE);
        when(vranJobActivityServiceHelper.getNeJobAttributes(jobEnvironment)).thenReturn(activityJobPropertiesList);
        when(vranJobActivityServiceHelper.getJobPropertyValueAsString(VranJobConstants.VNF_DESCRIPTOR_ID, neJobPropertieslist)).thenReturn("VnfDescId");
        when(vranJobActivityServiceHelper.getJobPropertyValueAsString(VranJobConstants.VNF_PACKAGE_ID, neJobPropertieslist)).thenReturn("VnfPkgId");
        when(vranJobActivityServiceHelper.getJobPropertyValueAsString(VranJobConstants.VNF_ID, neJobPropertieslist)).thenReturn("VnfId");
        when(vranJobActivityServiceHelper.getJobPropertyValueAsInt(VranJobConstants.VNF_JOB_ID, neJobPropertieslist)).thenReturn(123);

    }

    @Test
    public void testBuild() {
        final List<String> neNames = new ArrayList<>();
        neNames.add("TESTVRAN");
        NetworkElement ne = new NetworkElement();
        ne.setNetworkElementFdn("neFDN");
        List<NetworkElement> networkElements = new ArrayList<NetworkElement>();
        networkElements.add(ne);
        when(fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(neNames, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(networkElements);

        vranUpgradeJobContextBuilder.build(activityJobId);
    }

}
