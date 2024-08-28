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
package com.ericsson.oss.services.shm.es.polling;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.PollingCallBack;
import com.ericsson.oss.services.shm.es.polling.api.PollCycleStatus;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.event.based.mediation.MOReadResponse;

@RunWith(MockitoJUnitRunner.class)
public class ClusteredCallbackNotificationQueueObserverTest {

    @InjectMocks
    private ClusteredCallbackNotificationQueueObserver objectUnderTest;

    @Mock
    private MOReadResponse moReadResponseEvent;

    @Mock
    private PollingActivityCallbackManager pollingActivityCallbackManager;

    @Mock
    private PollingCallBackResolver pollingCallBackResolver;

    @Mock
    private PollingCallBack pollingCallBack;

    private static final long ACTIVITYJOB_ID = 1;

    @SuppressWarnings("unchecked")
    @Test
    public void testOnMessage() {
        prepareMoReadResponse();
        final Map<String, Object> pollingActivityAttributes = new HashMap<>();
        pollingActivityAttributes.put(PollingActivityConstants.POLL_CYCLE_STATUS, PollCycleStatus.COMPLETED.toString());
        when(pollingCallBackResolver.getPollingCallBackService(Matchers.any(PlatformTypeEnum.class), Matchers.any(JobTypeEnum.class), Matchers.anyString())).thenReturn(pollingCallBack);
        objectUnderTest.onMessage(moReadResponseEvent);
        verify(pollingActivityCallbackManager, times(1)).updatePollingAttributesByActivityJobId(ACTIVITYJOB_ID, pollingActivityAttributes);
        verify(pollingCallBack, times(1)).processPollingResponse(Matchers.anyLong(), Matchers.anyMap());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOnMessageWithInvalidAttributes() {
        prepareMoReadResponse();
        final Map<String, Object> pollingActivityAttributes = new HashMap<>();
        pollingActivityAttributes.put(PollingActivityConstants.POLL_CYCLE_STATUS, PollCycleStatus.COMPLETED.toString());
        objectUnderTest.onMessage(moReadResponseEvent);
        verify(pollingActivityCallbackManager, times(1)).updatePollingAttributesByActivityJobId(ACTIVITYJOB_ID, pollingActivityAttributes);
        verify(pollingCallBack, times(0)).processPollingResponse(Matchers.anyLong(), Matchers.anyMap());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOnMessageWithErrorMessage() {
        prepareMoReadResponse();
        moReadResponseEvent.setErrorMessage("ErrorMessage");
        final Map<String, Object> pollingActivityAttributes = new HashMap<>();
        pollingActivityAttributes.put(PollingActivityConstants.POLL_CYCLE_STATUS, PollCycleStatus.COMPLETED.toString());
        objectUnderTest.onMessage(moReadResponseEvent);
        verify(pollingActivityCallbackManager, times(1)).updatePollingAttributesByActivityJobId(ACTIVITYJOB_ID, pollingActivityAttributes);
        verify(pollingCallBack, times(0)).processPollingResponse(Matchers.anyLong(), Matchers.anyMap());
    }

    private void prepareMoReadResponse() {
        moReadResponseEvent = new MOReadResponse();
        moReadResponseEvent.setActivityJobId(ACTIVITYJOB_ID);
        final Map<String, Object> additionalInformation = new HashMap<>();
        additionalInformation.put(ShmConstants.JOB_TYPE, JobTypeEnum.UPGRADE.name());
        additionalInformation.put(ShmConstants.ACTIVITYNAME, "activate");
        additionalInformation.put(ShmConstants.PLATFORM, PlatformTypeEnum.ECIM.name());
        moReadResponseEvent.setAdditionalInformation(additionalInformation);
    }
}
