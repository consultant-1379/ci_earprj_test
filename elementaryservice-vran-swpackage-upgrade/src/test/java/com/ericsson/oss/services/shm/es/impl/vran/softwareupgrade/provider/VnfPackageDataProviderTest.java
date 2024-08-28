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
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.UpgradePackageContext;
import com.ericsson.oss.services.shm.es.vran.notifications.api.VranSoftwareUpgradeJobResponse;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.common.VNFMInformationProvider;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityUtil;
import com.ericsson.oss.services.shm.vran.shared.persistence.VnfSoftwarePackagePersistenceProvider;

@RunWith(MockitoJUnitRunner.class)
public class VnfPackageDataProviderTest {

    @InjectMocks
    private VnfPackageDataProvider vnfPackageDataProvider;

    @Mock
    private Map<String, Object> jobConfigurationDetails;

    @Mock
    private VranJobActivityServiceHelper vranJobActivityServiceHelper;

    @Mock
    private VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse;

    @Mock
    private UpgradePackageContext upgradePackageContext;

    @Mock
    private Map<String, Object> activityJobProperties;

    @Mock
    private JobActivityInfo jobActivityInfo;

    @Mock
    private JobEnvironment jobEnvironment;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private VranJobActivityUtil vranJobActivityUtil;

    @Mock
    private JobPropertyUtils jobPropertyUtils;

    @Mock
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    @Mock
    private DpsReader dpsReader;

    @Mock
    private VNFMInformationProvider virtualNetwrokFunctionManagerInformationProvider;

    @Mock
    private VnfSoftwarePackagePersistenceProvider vnfSoftwarePackagePersistenceProvider;

    private static final String NODE_NAME = "vRC0001";
    private static final String NODE_FDN = "NetworkElement=" + NODE_NAME;
    private static final String VNFM_FDN = "VirtualNetworkFunctionManager=VNFM";

    static final long activityJobId = 345;

    @Before
    public void mockJobEnvironment() {

        final String nodeName = NODE_NAME;
        final String JobLogMessge = "";
        final int mainJobId = 12121;
        jobActivityInfo = new JobActivityInfo(activityJobId, ActivityConstants.PREPARE, JobTypeEnum.UPGRADE, PlatformTypeEnum.vRAN);
        when(activityUtils.additionalInfoForEvent(activityJobId, nodeName, JobLogMessge)).thenReturn("");
        when(activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.UPGRADE)).thenReturn("");

    }

    @Test
    public void testBuildVnfDetails() {
        Map<String, Object> attributes = new HashMap<String, Object>();
        Map<String, Object> nfvoProductDetail = new HashMap<String, Object>();
        nfvoProductDetail.put(VranJobConstants.VNFD_ID, "vnfd123");
        nfvoProductDetail.put(VranJobConstants.VNF_PACKAGE_ID, "vnfpackage123");
        List<Map<String, Object>> nfvoProductDetails = new ArrayList<Map<String, Object>>();
        nfvoProductDetails.add(nfvoProductDetail);
        attributes.put("nfvoDetails", nfvoProductDetails);
        vnfPackageDataProvider.buildVnfDetails(nfvoProductDetails, null);

    }

    @Test
    public void testFetchVnfPackageDetailsWhenMutliptleNFVOsArePresent() {

        List<NetworkElement> networkElements = new ArrayList<NetworkElement>();
        NetworkElement networkElement = new NetworkElement();
        networkElement.setName(NODE_NAME);
        networkElement.setNeType("vRC");
        networkElement.setPlatformType(PlatformTypeEnum.vRAN);
        networkElement.setNodeRootFdn(NODE_FDN);
        networkElement.setNetworkElementFdn(NODE_FDN);
        networkElements.add(networkElement);

        final List<String> neNames = new ArrayList<>();
        neNames.add(NODE_NAME);

        final Map<String, Object> vnfdId = new HashMap<>();
        vnfdId.put(ActivityConstants.JOB_PROP_KEY, VranJobConstants.VNFD_ID);
        vnfdId.put(ActivityConstants.JOB_PROP_VALUE, "0000-0000-0001");

        final Map<String, Object> vnfPackageId = new HashMap<>();
        vnfPackageId.put(ActivityConstants.JOB_PROP_KEY, VranJobConstants.VNF_PACKAGE_ID);
        vnfPackageId.put(ActivityConstants.JOB_PROP_VALUE, "package1");

        when(jobEnvironment.getNodeName()).thenReturn(NODE_NAME);
        when(virtualNetwrokFunctionManagerInformationProvider.getVnfmFdn(NODE_FDN)).thenReturn(VNFM_FDN);
        when(virtualNetwrokFunctionManagerInformationProvider.getNfvoRefFromVnfmFDN(VNFM_FDN)).thenReturn("NetworkFunctionVirtualizationOrchestrator=NFVO1");
        when(vnfSoftwarePackagePersistenceProvider.getVnfPackageNfvoDetails(null)).thenReturn(getNvfoDetails());
        when(vranJobActivityServiceHelper.buildJobProperty(VranJobConstants.VNFD_ID, "0000-0000-0001")).thenReturn(vnfdId);
        when(vranJobActivityServiceHelper.buildJobProperty(VranJobConstants.VNF_PACKAGE_ID, "package1")).thenReturn(vnfPackageId);
        when(vranJobActivityServiceHelper.getMainJobAttributes(jobEnvironment)).thenReturn(jobConfigurationDetails);
        final List<Map<String, Object>> vnfPackageDetails = vnfPackageDataProvider.fetchVnfPackageDetails(jobEnvironment, networkElements);
        Assert.assertEquals(2, vnfPackageDetails.size());
    }

    private List<Map<String, Object>> getNvfoDetails() {

        final List<Map<String, Object>> nfvoDetails = new ArrayList<>();

        for (int i = 1; i < 3; i++) {
            final Map<String, Object> nvfoInfo = new HashMap<>();
            nvfoInfo.put("operationalState", "");
            nvfoInfo.put("nfvoId", "NFVO" + i);
            nvfoInfo.put("vnfdId", "0000-0000-000" + i);
            nvfoInfo.put("vnfdVersion", "1234");
            nvfoInfo.put("vnfProvider", "dummy");
            nvfoInfo.put("usageState", "IN_USE");
            nvfoInfo.put("vnfPackageId", "package" + i);

            nfvoDetails.add(nvfoInfo);
        }
        return nfvoDetails;
    }
}
