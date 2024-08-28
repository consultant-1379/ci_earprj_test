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

import java.util.Map;

import org.apache.commons.collections4.map.HashedMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.mediation.vran.model.request.VranUpgradeJobMediationTaskRequest;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.service.common.SoftwareUpgradeStatusCheckScheduler;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@RunWith(MockitoJUnitRunner.class)
public class VranSoftwareUpgradeEventSenderTest {
    @InjectMocks
    private VranSoftwareUpgradeEventSender vranSoftwareUpgradeEventSender;

    @Mock
    @Modeled
    private EventSender<VranUpgradeJobMediationTaskRequest> actionInvocationRequestSender;

    @Mock
    private SoftwareUpgradeStatusCheckScheduler softwareUpgradeStatusCheckScheduler;

    @Mock
    private ActivityUtils activityUtils;

    private Map<String, Object> eventAttributes;

    private String nodeAddress;

    @Before
    public void mockJobEnvironment() {
        eventAttributes = new HashedMap<String, Object>();

    }

    @Test
    public void testSendSoftwareUpgradeActionRequest() {

        vranSoftwareUpgradeEventSender.sendSoftwareUpgradeActionRequest(123, ActivityConstants.PREPARE, nodeAddress, eventAttributes);
    }

    @Test
    public void testSendUpgradeJobStatusRequest() {
        vranSoftwareUpgradeEventSender.sendUpgradeJobStatusRequest(ActivityConstants.PREPARE, 222, nodeAddress, eventAttributes);
    }

}
