/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks;

import static org.mockito.Mockito.times;
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

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.exception.NoVNFManagerFoundException;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider.JobLogsPersistenceProvider;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider.VnfInformationProvider;
import com.ericsson.oss.services.shm.es.vran.configuration.ConfigurationParameterValueProvider;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@RunWith(MockitoJUnitRunner.class)
public class TaskBaseTest extends ExecuteTaskTest {

    @InjectMocks
    private TaskBase taskBase;

    @Mock
    private ConfigurationParameterValueProvider configurationParameterValueProvider;

    @Mock
    private VnfInformationProvider vnfInformationProvider;

    @Mock
    private JobPropertyUtils jobPropertyUtils;

    @Mock
    private JobLogsPersistenceProvider jobLogsPersistenceProvider;

    @Before
    public void setupJobEnvironment() {
        final List<String> jobPropertyKeys = new ArrayList<>();
        final Map<String, String> propertyMap = new HashMap<String, String>();
        jobPropertyKeys.add(ActivityConstants.FALLBACK_TIMEOUT);
        Map<String,Object> jobConfigurationDetails = new HashMap<String, Object>();
        jobConfigurationDetails.put("nodeName", "LTEvSD001");
        propertyMap.put(ActivityConstants.FALLBACK_TIMEOUT, "3600");
        when(configurationParameterValueProvider.getRetriesForActivityWhenFailure()).thenReturn(5);
        when(configurationParameterValueProvider.getRetryIntervalInSec()).thenReturn(100);
        when(vnfInformationProvider.fetchNetworkElements(jobEnvironment)).thenReturn(getNetworkElements());
        when(vranJobActivityServiceHelper.getMainJobAttributes(jobEnvironment)).thenReturn(jobConfigurationDetails);
        when(jobPropertyUtils.getPropertyValue(jobPropertyKeys, jobConfigurationDetails, "LTEvSD001", "vSD", PlatformTypeEnum.vRAN.name())).thenReturn(propertyMap);
    }

    @Test
    public void testBuildEventAttributes() {
        taskBase.buildEventAttributes(vranJobInformation, "ACTIVATE", activityJobId);
        verifyAsserts();
    }

    @Test
    public void testPerformSoftwareUpgrade() {
        taskBase.performSoftwareUpgrade(activityJobId, vranJobInformation, "ACTIVATE", jobActivityInfo, "deleteActivityInitiatedFrom");
        verifyAsserts();
        verify(vranJobInformation,times(2)).getNodeName();
    }

    @Test(expected = NoVNFManagerFoundException.class)
    public void testPerformSoftwareUpgradeForNullVnfmFdn() {
        when(vranJobInformation.getVnfmFdn()).thenReturn(null);
        taskBase.performSoftwareUpgrade(activityJobId, vranJobInformation, "ACTIVATE", jobActivityInfo, "deleteActivityInitiatedFrom");
    }

    private List<NetworkElement> getNetworkElements() {
        List<NetworkElement> networkElements = new ArrayList<NetworkElement>();
        NetworkElement networkElement = new NetworkElement();
        networkElement.setName("LTEvSD001");
        networkElement.setPlatformType(PlatformTypeEnum.vRAN);
        networkElement.setNeType("vSD");
        networkElements.add(networkElement);
        return networkElements;
    }

    private void verifyAsserts() {
        verify(configurationParameterValueProvider,times(1)).getRetriesForActivityWhenFailure();
        verify(configurationParameterValueProvider,times(1)).getRetryIntervalInSec();
        verify(vnfInformationProvider,times(1)).fetchNetworkElements(jobEnvironment);
    }
}
