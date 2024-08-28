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
package com.ericsson.oss.services.shm.jobexecutor;

import static org.mockito.Mockito.verify;
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
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElementGroup;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElementGroupPreparator;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.instantaneouslicensing.InstantaneousLicensingNesValidateService;
import com.ericsson.oss.services.shm.jobexecutor.ecim.EcimJobExecutionValidatorImpl;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

/**
 * JUnit Test class for EcimJobExecutionValidatorImpl
 * 
 * @author xyerrsr
 * 
 */

@RunWith(MockitoJUnitRunner.class)
public class EcimJobExecutionValidatorImplTest {

    private static final String UNSUPPORTED_FRAGMENT = "Unsupported Fragment";

    @InjectMocks
    EcimJobExecutionValidatorImpl classUnderTest;

    @Mock
    private NetworkElementGroupPreparator neGroupPreparator;

    @Mock
    NetworkElementGroup networkElementGroupMock;

    @Mock
    private InstantaneousLicensingNesValidateService instantaniousLicensingNesValidateService;

    public static final String NODE_NAME = "shmDummyContext";
    public static final String NODE_FDN = "NetworkElement=" + NODE_NAME;

    private List<NetworkElement> sampleEcimNEList = null;

    @Before
    public void setUp() {
        sampleEcimNEList = new ArrayList<NetworkElement>();
        for (Integer index = 1; index < 5; index++) {
            final String ne = NODE_FDN + index.toString();
            final NetworkElement networkElement = new NetworkElement();
            networkElement.setNetworkElementFdn(ne);
            networkElement.setName(ne);
            networkElement.setPlatformType(PlatformTypeEnum.ECIM);
            sampleEcimNEList.add(networkElement);
        }
    }

    @Test
    public void testPerformJobExecutionValidations_upgradeJob_success() {
        final Map<String, String> unsupportedNEs = new HashMap<String, String>();
        unsupportedNEs.put("NetworkElement=shmDummyContext1", UNSUPPORTED_FRAGMENT);
        unsupportedNEs.put("NetworkElement=shmDummyContext3", UNSUPPORTED_FRAGMENT);

        final List<NetworkElement> expectedNEList = new ArrayList<NetworkElement>();
        expectedNEList.add(sampleEcimNEList.get(0));

        when(neGroupPreparator.groupNetworkElementsByModelidentity(sampleEcimNEList, FragmentType.ECIM_SWM_TYPE.getFragmentName())).thenReturn(networkElementGroupMock);
        when(networkElementGroupMock.getUnSupportedNetworkElements()).thenReturn(unsupportedNEs);
        final Map<NetworkElement, String> neList = classUnderTest.findUnSupportedNEs(JobTypeEnum.UPGRADE, sampleEcimNEList);
        //Assert.assertTrue(neList.containsValue(expectedNEList));

    }

    @Test
    public void testPerformJobExecutionValidations_restartJob_success() {
        final Map<String, String> unsupportedNEs = new HashMap<String, String>();
        unsupportedNEs.put("NetworkElement=shmDummyContext1", UNSUPPORTED_FRAGMENT);
        unsupportedNEs.put("NetworkElement=shmDummyContext3", UNSUPPORTED_FRAGMENT);

        final List<NetworkElement> expectedNEList = new ArrayList<NetworkElement>();
        expectedNEList.add(sampleEcimNEList.get(0));

        when(neGroupPreparator.groupNetworkElementsByModelidentity(sampleEcimNEList, FragmentType.ECIM_SWM_TYPE.getFragmentName())).thenReturn(networkElementGroupMock);
        when(networkElementGroupMock.getUnSupportedNetworkElements()).thenReturn(unsupportedNEs);
        final Map<NetworkElement, String> neList = classUnderTest.findUnSupportedNEs(JobTypeEnum.NODERESTART, sampleEcimNEList);
        Assert.assertEquals(4, neList.size());

    }

    @Test
    public void testPerformJobExecutionValidationsNhcJobSuccess() {
        final Map<String, String> unsupportedNEs = new HashMap<>();
        unsupportedNEs.put("NetworkElement=shmDummyContext1", UNSUPPORTED_FRAGMENT);

        final List<NetworkElement> expectedNEList = new ArrayList<>();
        expectedNEList.add(sampleEcimNEList.get(0));

        when(neGroupPreparator.groupNetworkElementsByModelidentity(sampleEcimNEList, FragmentType.ECIM_HCM_TYPE.getFragmentName())).thenReturn(networkElementGroupMock);
        when(networkElementGroupMock.getUnSupportedNetworkElements()).thenReturn(unsupportedNEs);
        final Map<NetworkElement, String> neList = classUnderTest.findUnSupportedNEs(JobTypeEnum.NODE_HEALTH_CHECK, sampleEcimNEList);
        Assert.assertEquals(1, neList.size());

    }

    @Test
    public void testPerformJobExecutionValidationsLicenseRefreshJobSuccess() {
        final Map<String, String> unsupportedNEs = new HashMap<>();
        unsupportedNEs.put("NetworkElement=shmDummyContext3", UNSUPPORTED_FRAGMENT);

        final List<NetworkElement> expectedNEList = new ArrayList<>();
        expectedNEList.add(sampleEcimNEList.get(0));

        when(neGroupPreparator.groupNetworkElementsByModelidentity(sampleEcimNEList, FragmentType.ECIM_LM_TYPE.getFragmentName())).thenReturn(networkElementGroupMock);
        when(networkElementGroupMock.getUnSupportedNetworkElements()).thenReturn(unsupportedNEs);
        final Map<NetworkElement, String> neList = classUnderTest.findUnSupportedNEs(JobTypeEnum.LICENSE_REFRESH, sampleEcimNEList);
        when(instantaniousLicensingNesValidateService.filterInstantaneousLicensingSupportedNes(Matchers.anyList())).thenReturn(neList);
        Assert.assertEquals(1, neList.size());
        verify(instantaniousLicensingNesValidateService).filterInstantaneousLicensingSupportedNes(Matchers.anyList());
        verify(neGroupPreparator).groupNetworkElementsByModelidentity(sampleEcimNEList, FragmentType.ECIM_LM_TYPE.getFragmentName());
        verify(networkElementGroupMock).getUnSupportedNetworkElements();

    }
}
