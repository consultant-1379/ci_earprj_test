package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.softwareupgrade.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.backup.MiniLinkOutdoorJobUtil;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@RunWith(MockitoJUnitRunner.class)
public class HandleTimeoutTaskTest {

    public static final long ACTIVITY_JOB_ID = 123;
    public static final String NODE_NAME = "ML-TN";
    public static final int FTP_TABLE_ENTRY_INDEX = 15;
    public static final int FTP_ENTRY_INDEX = 1;
    public static final String PATH_ON_SERVER = "pathOnServer";
    public static final String BACKUP_NAME_TEST = "backup";
    public static final String MINI_LINK_OUTDOOR = "MINI_LINK_OUTDOOR";
    public static final String CONFIG_FILE_EXTENSION = "cdb";
    public static final String DOT = ".";
    public static final String SLASH = "/";
    public static final String UNDERSCORE = "_";
    public static final String EXPECTED_BACKUP_FILE_NAME = BACKUP_NAME_TEST + UNDERSCORE + NODE_NAME + DOT + CONFIG_FILE_EXTENSION;
    public static final String BACKUP_FILE_PATH_PREFIX = MINI_LINK_OUTDOOR + SLASH + EXPECTED_BACKUP_FILE_NAME;
    public static final String SUBSCRIPTIONKEY = "subscriptionKey";

    @InjectMocks
    private HandleTimeoutTask handleTimeoutTask;

    @Mock
    private JobActivityInfo jobActivityInfo;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private MiniLinkOutdoorJobUtil miniLinkOutdoorJobUtil;

    @Mock
    private JobUpdateService jobUpdateService;

    @Mock
    private ActivityStepResult activityStepResult;

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleTimeoutWithJobFail() throws JobDataNotFoundException {
            when(jobActivityInfo.getActivityName()).thenReturn(ActivityConstants.UPGRADE);
            when(jobActivityInfo.getActivityJobId()).thenReturn(ACTIVITY_JOB_ID);
            when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITY_JOB_ID, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
            when(neJobStaticData.getNodeName()).thenReturn(NODE_NAME);
            when(miniLinkOutdoorJobUtil.getSubscriptionKey(NODE_NAME, ActivityConstants.UPGRADE)).thenReturn(SUBSCRIPTIONKEY);
            when(activityUtils.unSubscribeToMoNotifications(SUBSCRIPTIONKEY, ACTIVITY_JOB_ID, jobActivityInfo)).thenReturn(true);
            when(jobUpdateService.readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble())).thenReturn(true);
            final ActivityStepResult result = handleTimeoutTask.handleTimeout(jobActivityInfo);
            assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, result.getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleTimeoutWithException() throws JobDataNotFoundException {
            when(jobActivityInfo.getActivityName()).thenReturn(ActivityConstants.UPGRADE);
            when(jobActivityInfo.getActivityJobId()).thenReturn(ACTIVITY_JOB_ID);
            when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITY_JOB_ID, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenThrow(JobDataNotFoundException.class);
            assertNull(handleTimeoutTask.handleTimeout(jobActivityInfo).getActivityResultEnum());
    }
}
