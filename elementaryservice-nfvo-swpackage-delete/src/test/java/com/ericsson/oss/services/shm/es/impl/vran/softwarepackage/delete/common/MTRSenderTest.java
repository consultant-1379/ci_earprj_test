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
package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common;

import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.mediation.vran.model.request.NfvoSwPackageCreateDeleteJobRequest;
import com.ericsson.oss.services.shm.es.vran.configuration.ConfigurationParameterValueProvider;

@RunWith(MockitoJUnitRunner.class)
public class MTRSenderTest {

    @InjectMocks
    private MTRSender mtrSender;

    @Mock
    private EventSender<NfvoSwPackageCreateDeleteJobRequest> eventSender;

    @Mock
    private DeleteJobStatusCheckScheduler deleteJobStatusCheckScheduler;

    @Mock
    private ConfigurationParameterValueProvider configurationParameterValueProvider;

    private String vnfPackageId;
    private String nodeAddress;
    private String jobId;

    @Before
    public void setUp() throws Exception {
        vnfPackageId = "vnfPkg_1-0";
        nodeAddress = "VtfNode";
        jobId = "JOB_1";
    }

    @Test
    public void sendDeleteSoftwarePackageRequestTest() {
        mtrSender.sendDeleteSoftwarePackageRequest(nodeAddress, vnfPackageId);
    }

    @Test
    public void sendJobStatusRequestTest() {
        when(configurationParameterValueProvider.getSoftwarePackageOnboardStatusCheckInterval()).thenReturn(5);
        mtrSender.sendJobStatusRequest(nodeAddress, vnfPackageId, jobId);
    }

}
