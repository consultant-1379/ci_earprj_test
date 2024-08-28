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
package com.ericsson.oss.services.shm.onboard.notification;

import static org.mockito.Mockito.verify;

import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.oss.mediation.vran.model.response.NfvoSwPackageMediationResponse;

/**
 * Class to test the NfvoSoftwarePackageJobProgressQueueObserver class with the help of jUnits.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(NfvoSoftwarePackageJobProgressQueueObserver.class)
public class NfvoSoftwarePackageJobProgressQueueObserverTest {

    @InjectMocks
    NfvoSoftwarePackageJobProgressQueueObserver objectUnderTest;

    @Mock
    private NfvoSwPackageMediationResponse nfvoSwPackageMediationResponse;

    @Mock
    private NfvoSoftwarePackageJobProgressHandler nfvoSoftwarePackageJobProgressHandler;

    /**
     * Test case to test onMessage method in NfvoSoftwarePackageJobProgressQueueObserver.
     * 
     * @throws Exception
     */
    @Test
    public void testOnMessage() throws Exception {
        final Date notificationReceivedDate = new Date();

        PowerMockito.whenNew(Date.class).withNoArguments().thenReturn(notificationReceivedDate);
        objectUnderTest.onMessage(nfvoSwPackageMediationResponse);
        verify(nfvoSoftwarePackageJobProgressHandler).handleJobProgressResponse(nfvoSwPackageMediationResponse, notificationReceivedDate);
    }

}
