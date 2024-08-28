package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.common;
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

import java.util.HashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.mediation.vran.model.request.NfvoSwPackageCreateOnBoardJobRequest;

/**
 * Test class to test OnboardJobServiceDpsUtil methods
 * 
 * @author xjhosye
 * 
 */
@RunWith(value = MockitoJUnitRunner.class)
public class MTRSenderTest {

    @InjectMocks
    MTRSender mtrSender;

    @Mock
    NfvoSwPackageCreateOnBoardJobRequest nfvoSwPackageCreateOnboardJobRequest;

    @Mock
    private EventSender<NfvoSwPackageCreateOnBoardJobRequest> eventSender;

    @Mock
    private OnboardSoftwarePackageStatusCheckScheduler onboardSoftwarePackageStatusCheckScheduler;

    public static final String TEST_NODE = "testNode";
    public static final String VNF_PACKAGE_ID = "testVnfPackageId";
    public static final String JOB_ID = "testVnfPackageId";
    public static final String FULL_FOLDER_PATH = "/smrs/smrsroot/vrc/testpackage";

    @Test
    public void testSendOnboardSoftwarePackageRequest() {
        final NfvoSwPackageCreateOnBoardJobRequest onboardActionRequest = new NfvoSwPackageCreateOnBoardJobRequest();
        onboardActionRequest.setNodeAddress(TEST_NODE);
        onboardActionRequest.setfullFolderPath(FULL_FOLDER_PATH);
        mtrSender.sendOnboardSoftwarePackageRequest(TEST_NODE, VNF_PACKAGE_ID,new HashMap<String,Object>());
    }

    @Test
    public void testSendJobStatusRequest() {
        mtrSender.sendJobStatusRequest(TEST_NODE, VNF_PACKAGE_ID, JOB_ID,new HashMap<String,Object>());
    }

}
