package com.ericsson.oss.services.shm.es.noderestart;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.ActivityCompleteCallBack;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.NodeRestartJobActivityInfo;
import com.ericsson.oss.services.shm.es.api.NodeRestartValidator;
import com.ericsson.oss.services.shm.es.impl.ActivityCompleteCallBackProvider;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

@RunWith(MockitoJUnitRunner.class)
public class NodeRestartActivityTimerTest {

    @Mock
    TimerService timerServiceMock;

    @Mock
    Timer timerMock;

    @Mock
    ActivityCompleteCallBackProvider timedActivityServiceProviderMock;

    @InjectMocks
    NodeRestartActivityTimer activityCallBackHandlerMock;

    @Mock
    ActivityCompleteCallBack timedActivityCallBackMock;

    @Mock
    JobEnvironment jobEnvironment;

    @Mock
    @Inject
    protected ActivityUtils activityUtils;

    @Mock
    @Inject
    protected NodeRestartValidator nodeRestartValidator;

    @Mock
    List<NetworkElement> neElementList;

    @Mock
    NetworkElement neElement;

    @Mock
    JobLogUtil jobLogUtil;

    @Mock
    JobUpdateService jobUpdateService;

    @Inject
    @Mock
    private NodeRestartPlatformFactory nodeRestartPlatformFactory;

    @Mock
    private JobStaticData jobStaticDataMock;

    @Mock
    private JobStaticDataProvider jobStaticDataProvider;

    @Mock
    protected SystemRecorder systemRecorder;

    long activityJobId = 1234L;
    long neJobId = 123L;
    long mainJobId = 123L;

    String neName = "Some Ne Name";
    String neType = "Standard";
    String platformType = "CPP";
    String jobExecutedUser = "xprapav";

    @Test
    public void testStartTimer() {
        final NodeRestartJobActivityInfo nodeRestartJobActivityInfo = new NodeRestartJobActivityInfo(activityJobId, "verify", JobTypeEnum.NODERESTART, PlatformTypeEnum.CPP, 600, 10, 600);
        when(timerServiceMock.createCalendarTimer(Matchers.any(ScheduleExpression.class), Matchers.any(TimerConfig.class))).thenReturn(timerMock);
        activityCallBackHandlerMock.startTimer(nodeRestartJobActivityInfo);
    }

    @Test
    public void testcheckIfNodeIsReachablewithnodeRestartStatusSuccess() throws Exception {
        final NodeRestartJobActivityInfo nodeRestartJobActivityInfo = new NodeRestartJobActivityInfo(activityJobId, "verify", JobTypeEnum.NODERESTART, PlatformTypeEnum.CPP, 600, 10, 600);
        when(timerMock.getInfo()).thenReturn(nodeRestartJobActivityInfo);
        when(timedActivityServiceProviderMock.onActionCompleteHandler(PlatformTypeEnum.CPP, JobTypeEnum.NODERESTART, "verify")).thenReturn(timedActivityCallBackMock);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(neElementList.get(0)).thenReturn(neElement);
        when(neElement.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        when(nodeRestartPlatformFactory.getNodeRestartValidator(nodeRestartJobActivityInfo.getPlatform())).thenReturn(nodeRestartValidator);
        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutedUser);
        when(nodeRestartValidator.isNodeReachable(neName)).thenReturn(true);
        doNothing().when(timedActivityCallBackMock).onActionComplete(Matchers.anyLong());
        Whitebox.invokeMethod(activityCallBackHandlerMock, "checkIfNodeIsReachable", timerMock);
        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble());
    }

    @Test
    public void testcheckIfNodeIsReachablewithnodeRestartStatusFial() throws Exception {
        final NodeRestartJobActivityInfo nodeRestartJobActivityInfo = new NodeRestartJobActivityInfo(activityJobId, "verify", JobTypeEnum.NODERESTART, PlatformTypeEnum.CPP, 600, 10, 600);
        when(timerMock.getInfo()).thenReturn(nodeRestartJobActivityInfo);
        when(timedActivityServiceProviderMock.onActionCompleteHandler(PlatformTypeEnum.CPP, JobTypeEnum.NODERESTART, "verify")).thenReturn(timedActivityCallBackMock);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(neElementList.get(0)).thenReturn(neElement);
        when(neElement.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        when(nodeRestartPlatformFactory.getNodeRestartValidator(nodeRestartJobActivityInfo.getPlatform())).thenReturn(nodeRestartValidator);
        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutedUser);
        when(nodeRestartValidator.isNodeReachable(neName)).thenReturn(false);
        doNothing().when(timedActivityCallBackMock).onActionComplete(Matchers.anyLong());
        Whitebox.invokeMethod(activityCallBackHandlerMock, "checkIfNodeIsReachable", timerMock);

    }

    @Test
    public void testcheckIfNodeIsReachablewithnodeRestartStatusFial2() throws Exception {
        final NodeRestartJobActivityInfo nodeRestartJobActivityInfo = new NodeRestartJobActivityInfo(activityJobId, "verify", JobTypeEnum.NODERESTART, PlatformTypeEnum.CPP, 600, 10, 600);
        nodeRestartJobActivityInfo.setTimeElapsedForCppNodeRestart(610);
        when(timerMock.getInfo()).thenReturn(nodeRestartJobActivityInfo);
        when(timedActivityServiceProviderMock.onActionCompleteHandler(PlatformTypeEnum.CPP, JobTypeEnum.NODERESTART, "verify")).thenReturn(timedActivityCallBackMock);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
        when(neElementList.get(0)).thenReturn(neElement);
        when(neElement.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        when(nodeRestartPlatformFactory.getNodeRestartValidator(nodeRestartJobActivityInfo.getPlatform())).thenReturn(nodeRestartValidator);
        when(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId())).thenReturn(jobExecutedUser);
        when(nodeRestartValidator.isNodeReachable(neName)).thenReturn(false);
        doNothing().when(timedActivityCallBackMock).onActionComplete(Matchers.anyLong());
        Whitebox.invokeMethod(activityCallBackHandlerMock, "checkIfNodeIsReachable", timerMock);

    }

}
