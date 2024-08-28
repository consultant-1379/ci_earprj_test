package com.ericsson.oss.services.shm.es.impl.cpp.common;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import javax.ejb.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.ActivityCompleteCallBack;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityCompleteCallBackProvider;
import com.ericsson.oss.services.shm.es.impl.ActivityCompleteTimer;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

@RunWith(PowerMockRunner.class)
public class ActivityCompleteTimerTest {

    @Mock
    TimerService timerServiceMock;

    @Mock
    Timer timerMock;

    @Mock
    private ActivityCompleteCallBackProvider timedActivityServiceProviderMock;

    @InjectMocks
    ActivityCompleteTimer activityCallBackHandlerMock;

    @Mock
    ActivityCompleteCallBack timedActivityCallBackMock;

    long activityJobId = 1234;

    @Test
    public void testStartTimer() {

        final JobActivityInfo jobInfo = new JobActivityInfo(activityJobId, "verify", JobTypeEnum.UPGRADE, PlatformTypeEnum.CPP);

        when(timerServiceMock.createSingleActionTimer(Matchers.anyLong(), Matchers.any(TimerConfig.class))).thenReturn(timerMock);
        activityCallBackHandlerMock.startTimer(jobInfo);
    }

    @Test
    public void testHandleTimeout() throws Exception {

        final JobActivityInfo jobInfo = new JobActivityInfo(activityJobId, "verify", JobTypeEnum.UPGRADE, PlatformTypeEnum.CPP);

        when(timerMock.getInfo()).thenReturn(jobInfo);
        when(timedActivityServiceProviderMock.onActionCompleteHandler(PlatformTypeEnum.CPP, JobTypeEnum.UPGRADE, "verify")).thenReturn(timedActivityCallBackMock);
        //doNothing().when(timedActivityCallBackMock.onActionComplete(Matchers.anyLong()));

        doNothing().when(timedActivityCallBackMock).onActionComplete(Matchers.anyLong());
        Whitebox.invokeMethod(activityCallBackHandlerMock, "handleTimeout", timerMock);

    }

}
