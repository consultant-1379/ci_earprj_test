package com.ericsson.oss.services.shm.es.moaction;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.activity.callback.MOActionCallBackReslover;
import com.ericsson.oss.services.shm.es.api.MOActionCallBack;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.event.based.mediation.MOActionRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MOActionRequestQueueObserverTest {

    @InjectMocks
    private MOActionRequestQueueObserver objectUnderTest;

    @Mock
    private MOActionCallBackReslover moActionCallBackResolver;

    @Mock
    private MOActionCallBack moActionCallBack;

    @Mock
    private MOActionRequest moActionRequestEvent;

    @Mock
    private JobConfigurationService jobConfigurationService;

    @Mock
    private MoActionMTRManager moActionMTRManager;

    private static final long ACTIVITYJOB_ID = 564789563;


    @SuppressWarnings("unchecked")
    @Test
    public void testOnMessageShouldCallRepeatExecuteIfActivityJobIsNotCompletedAndRequestEventIsNotNull() {
        prepareMoActionRequest();
        when(moActionCallBackResolver.getMOActionCallBackService(Matchers.any(PlatformTypeEnum.class), Matchers.any(JobTypeEnum.class), Matchers.anyString())).thenReturn(moActionCallBack);
        when(jobConfigurationService.isJobResultEvaluated(ACTIVITYJOB_ID)).thenReturn(false);
        objectUnderTest.onMessage(moActionRequestEvent);
        verify(moActionMTRManager, times(0)).removeMoActionMTRFromCache(ACTIVITYJOB_ID);
        verify(moActionCallBack, times(1)).repeatExecute(ACTIVITYJOB_ID);

    }


    @SuppressWarnings("unchecked")
    @Test
    public void testOnMessageShouldRemoveMoActionMTRFromCacheIfActivityJobIsCompletedAndShouldNotCallRepeatExecute() {
        prepareMoActionRequest();
        when(moActionCallBackResolver.getMOActionCallBackService(Matchers.any(PlatformTypeEnum.class), Matchers.any(JobTypeEnum.class), Matchers.anyString())).thenReturn(moActionCallBack);
        when(jobConfigurationService.isJobResultEvaluated(ACTIVITYJOB_ID)).thenReturn(true);
        objectUnderTest.onMessage(moActionRequestEvent);
        verify(moActionMTRManager, times(1)).removeMoActionMTRFromCache(ACTIVITYJOB_ID);
        verify(moActionCallBack, times(0)).repeatExecute(ACTIVITYJOB_ID);


    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOnMessageShouldIgnoreTheRequestIfRequestEventIsNull() {
        objectUnderTest.onMessage(null);
        verify(moActionMTRManager, times(0)).removeMoActionMTRFromCache(ACTIVITYJOB_ID);
        verify(moActionCallBack, times(0)).repeatExecute(ACTIVITYJOB_ID);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOnMessageShouldIgnoreTheRequestIfExceptionOccurs() {
        prepareMoActionRequest();
        doThrow(Exception.class).when(jobConfigurationService).isJobResultEvaluated(ACTIVITYJOB_ID);
        objectUnderTest.onMessage(moActionRequestEvent);
        verify(moActionMTRManager, times(0)).removeMoActionMTRFromCache(ACTIVITYJOB_ID);
        verify(moActionCallBack, times(0)).repeatExecute(ACTIVITYJOB_ID);

    }

    private void prepareMoActionRequest() {
        moActionRequestEvent = new MOActionRequest();
        moActionRequestEvent.setActivityJobId(ACTIVITYJOB_ID);
        moActionRequestEvent.setActionName( "UPLOAD_BACKUP");
        moActionRequestEvent.setMoFdn("SubNetwork=NetSim,MeContext=Node1,ManagedElement=1,SystemFunctions=1,BrM=1,BrmBackupManager=1,BrmBackup=94");
        moActionRequestEvent.setMoName("Backup123");
        moActionRequestEvent.setMimVersion("1.2.1");
        moActionRequestEvent.setNamespace("Shm");
        final Map<String, Object> additionalInformation = new HashMap<>();
        additionalInformation.put(ShmConstants.JOB_TYPE, JobTypeEnum.BACKUP.name());
        additionalInformation.put(ShmConstants.ACTIVITYNAME, "UPLOAD_BACKUP");
        additionalInformation.put(ShmConstants.PLATFORM, PlatformTypeEnum.ECIM.name());
        moActionRequestEvent.setAdditionalInformation(additionalInformation);
    }
}
