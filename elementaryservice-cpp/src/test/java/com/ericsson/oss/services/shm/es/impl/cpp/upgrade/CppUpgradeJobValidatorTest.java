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
package com.ericsson.oss.services.shm.es.impl.cpp.upgrade;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.security.accesscontrol.EAccessControl;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.job.utils.UpgradeJobConfigurationListener;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.upgrade.remote.api.RemoteSoftwarePackageRetryProxy;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

@RunWith(MockitoJUnitRunner.class)
public class CppUpgradeJobValidatorTest {

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private NetworkElement neElementMock;

    @Mock
    private JobPropertyUtils jobPropertyUtils;

    @Mock
    private List<NetworkElement> neElementListMock;

    @Mock
    private RemoteSoftwarePackageRetryProxy remoteSoftwarePackageRetryProxy;

    @Mock
    private UpgradeJobConfigurationListener upgradeJobConfigurationListener;

    @Mock
    private JobUpdateService jobUpdateService;

    @Mock
    private JobConfigurationServiceRetryProxy jobConfigurationServiceRetryProxy;

    @Mock
    private JobLogUtil jobLogUtil;

    @Mock
    Map<String, Object> mapMock;

    @Mock
    EAccessControl accessControl;

    @Mock
    private FdnServiceBeanRetryHelper fdnServiceBeanMock;

    @InjectMocks
    CppUpgradeJobValidator objectUnderTest;

    private static final String nodeName = "LTE01";
    private static final long neJobId = 12l;
    private static final long mainJobId = 13l;
    private static final long templateId = 13l;

    @SuppressWarnings("unchecked")
    @Test
    public void testPreValidateWithFailSkipExecution() {
        when(activityUtils.getPoAttributes(neJobId)).thenReturn(getNeJobAttributes(neJobId));
        when(activityUtils.getPoAttributes(mainJobId)).thenReturn(getMainJobAttributes(mainJobId));
        when(activityUtils.getPoAttributes(templateId)).thenReturn(getTemplateJobAttributes(templateId));

        when(mapMock.get(ActivityConstants.JOB_CONFIGURATION_DETAILS)).thenReturn(mapMock);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);

        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(UpgradeActivityConstants.SWP_NAME, "CXP1020511_R4D73");
        jobProperty.put(UpgradeActivityConstants.UCF, "CXP1020511_R4D73.xml");
        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(jobProperty);

        when(remoteSoftwarePackageRetryProxy.validateUPMoState(Matchers.anyMap())).thenReturn(mapMock);
        when(mapMock.get(UpgradeActivityConstants.IS_UP_ACTIVE_ON_NODE)).thenReturn(true);
        when(mapMock.get(UpgradeActivityConstants.UP_PO_SWP_PRODUCT_DETAILS)).thenReturn("CXP1020511_R4D73");
        when(upgradeJobConfigurationListener.isSkipUpgradeEnabled()).thenReturn(true);
        when(jobUpdateService.readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble())).thenReturn(true);

        assertTrue(objectUnderTest.validate(neJobId));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPreValidateWithSuccessProceedExecution() {
        when(activityUtils.getPoAttributes(neJobId)).thenReturn(getNeJobAttributes(neJobId));
        when(activityUtils.getPoAttributes(mainJobId)).thenReturn(getMainJobAttributes(mainJobId));
        when(activityUtils.getPoAttributes(templateId)).thenReturn(getTemplateJobAttributes(templateId));

        when(mapMock.get(ActivityConstants.JOB_CONFIGURATION_DETAILS)).thenReturn(mapMock);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);

        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(UpgradeActivityConstants.SWP_NAME, "CXP1020511_R4D73");
        jobProperty.put(UpgradeActivityConstants.UCF, "CXP1020511_R4D73.xml");
        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(jobProperty);

        when(remoteSoftwarePackageRetryProxy.validateUPMoState(Matchers.anyMap())).thenReturn(mapMock);
        when(mapMock.get(UpgradeActivityConstants.IS_UP_ACTIVE_ON_NODE)).thenReturn(true);
        when(mapMock.get(UpgradeActivityConstants.UP_PO_SWP_PRODUCT_DETAILS)).thenReturn("CXP1020511_R4D73");
        when(upgradeJobConfigurationListener.isSkipUpgradeEnabled()).thenReturn(false);
        when(jobUpdateService.readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble())).thenReturn(true);

        assertFalse(objectUnderTest.validate(neJobId));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPreValidateWithSkipProceedExecution() {
        when(activityUtils.getPoAttributes(neJobId)).thenReturn(getNeJobAttributes(neJobId));
        when(activityUtils.getPoAttributes(mainJobId)).thenReturn(getMainJobAttributes(mainJobId));
        when(activityUtils.getPoAttributes(templateId)).thenReturn(getTemplateJobAttributes(templateId));

        when(mapMock.get(ActivityConstants.JOB_CONFIGURATION_DETAILS)).thenReturn(mapMock);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);

        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(UpgradeActivityConstants.SWP_NAME, "CXP1020511_R4D73");
        jobProperty.put(UpgradeActivityConstants.UCF, "CXP1020511_R4D73.xml");
        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(jobProperty);

        when(remoteSoftwarePackageRetryProxy.validateUPMoState(Matchers.anyMap())).thenReturn(mapMock);
        when(mapMock.get(UpgradeActivityConstants.IS_UP_ACTIVE_ON_NODE)).thenReturn(true);
        when(mapMock.get(UpgradeActivityConstants.UP_PO_SWP_PRODUCT_DETAILS)).thenReturn("CXP1020511_R4D73");
        when(upgradeJobConfigurationListener.isSkipUpgradeEnabled()).thenReturn(true);
        when(jobConfigurationServiceRetryProxy.getJobPoIdsFromParentJobId(neJobId, ShmConstants.ACTIVITY_JOB, ShmConstants.NE_JOB_ID)).thenReturn(Arrays.asList(11l, 12l));
        objectUnderTest.validate(neJobId);
        Mockito.verify(jobUpdateService, times(2)).updateActivityAsSkipped(Matchers.anyLong());
        Mockito.verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble());

    }

    private Map<String, Object> getNeJobAttributes(final long neJobId) {
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        neJobAttributes.put(ShmConstants.NE_NAME, nodeName);
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        return neJobAttributes;
    }

    private Map<String, Object> getMainJobAttributes(final long mainJobId) {
        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        mainJobAttributes.put(ShmConstants.JOBTEMPLATEID, templateId);
        return mainJobAttributes;
    }

    private Map<String, Object> getTemplateJobAttributes(final long templateId) {
        final Map<String, Object> jobTemplate = new HashMap<String, Object>();
        jobTemplate.put("owner", "administrator");
        return jobTemplate;
    }
}
