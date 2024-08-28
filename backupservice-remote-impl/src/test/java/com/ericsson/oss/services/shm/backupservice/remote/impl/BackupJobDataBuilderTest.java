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
package com.ericsson.oss.services.shm.backupservice.remote.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

@RunWith(MockitoJUnitRunner.class)
public class BackupJobDataBuilderTest {

    @InjectMocks
    BackupJobDataBuilder objectUnderTest;

    @Mock
    FdnServiceBean fdnServiceBean;

    @Mock
    NetworkElement networkElement;

    @Test
    public void testGetNeTypeAndPlatformType() {

        final String neName = "LTE01";
        final List<NetworkElement> networkElementList = new ArrayList<NetworkElement>();
        networkElementList.add(networkElement);

        when(fdnServiceBean.getNetworkElementsByNeNames(Arrays.asList(neName), SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(networkElementList);
        when(networkElement.getNeType()).thenReturn("ERBS");
        when(networkElement.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        final Map<String, String> neTypeAndPlatFormType = objectUnderTest.getNeTypeAndPlatformType(neName);
        assertEquals("ERBS", neTypeAndPlatFormType.get(ShmConstants.NETYPE));
        assertEquals("CPP", neTypeAndPlatFormType.get(ShmConstants.PLATFORM));
        verify(fdnServiceBean, times(1)).getNetworkElementsByNeNames(Arrays.asList(neName), SHMCapabilities.BACKUP_JOB_CAPABILITY);

    }
}
