/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.activity.callback;

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
import com.ericsson.oss.services.shm.es.api.MOActionCallBack;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.event.based.mediation.MOActionResponse;

/**
 * Test class to check number of calls made by the to the MoActionresponse to the elementary services .
 * 
 * @author zdonkri
 * 
 */

@RunWith(MockitoJUnitRunner.class)
public class MOActionResponseQueueObserverTest {
    @InjectMocks
    private MOActionResponseQueueObserver objectUnderTest;

    @Mock
    private MOActionResponse moActionResponseEvent;

    @Mock
    private MOActionCallBackReslover moActionCallBackResolver;

    @Mock
    private MOActionCallBack moActionCallBack;

    private static final long ACTIVITYJOB_ID = 1;

    @SuppressWarnings("unchecked")
    @Test
    public void testOnMessage() {
        prepareMoActionResponse();
        when(moActionCallBackResolver.getMOActionCallBackService(Matchers.any(PlatformTypeEnum.class), Matchers.any(JobTypeEnum.class), Matchers.anyString())).thenReturn(moActionCallBack);
        objectUnderTest.onMessage(moActionResponseEvent);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOnMessageWithErrorMessage() {
        prepareMoActionResponse();
        moActionResponseEvent.setErrorMessage("ErrorMessage");
        objectUnderTest.onMessage(moActionResponseEvent);
        verify(moActionCallBack, times(0)).processMoActionResponse(Matchers.anyLong(), Matchers.anyMap());
    }

    private void prepareMoActionResponse() {
        moActionResponseEvent = new MOActionResponse();
        moActionResponseEvent.setActivityJobId(ACTIVITYJOB_ID);
        final Map<String, Object> additionalInformation = new HashMap<>();
        additionalInformation.put(ShmConstants.JOB_TYPE, JobTypeEnum.BACKUP.name());
        additionalInformation.put(ShmConstants.ACTIVITYNAME, "UPLOAD_BACKUP");
        additionalInformation.put(ShmConstants.PLATFORM, PlatformTypeEnum.ECIM.name());
        moActionResponseEvent.setAdditionalInformation(additionalInformation);
    }
}
