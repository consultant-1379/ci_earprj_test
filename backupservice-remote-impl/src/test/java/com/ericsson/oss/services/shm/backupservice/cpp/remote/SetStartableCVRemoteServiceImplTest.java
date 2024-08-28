package com.ericsson.oss.services.shm.backupservice.cpp.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CommonCvOperations;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;

@RunWith(MockitoJUnitRunner.class)
public class SetStartableCVRemoteServiceImplTest {

    private static final String MO_FDN = "moFdn";
    private static final String STARTABLE_CV = "STARTABLE_CV";

    @Mock
    SystemRecorder systemRecorder;

    @Mock
    CommonCvOperations commonCvOperations;

    @Mock
    DpsReader dpsReader;

    @Mock
    FdnNotificationSubject subject;

    @Mock
    DpsAttributeChangedEvent event;

    @Mock
    @Inject
    ActivityUtils activityUtils;

    Map<String, Object> actionParameters = new HashMap<String, Object>();

    @InjectMocks
    SetStartableCVRemoteServiceImpl setStartableCVRemoteServiceImpl;

    Map<String, Object> cvMo = new HashMap<String, Object>();

    @Before
    public void setup() {
        cvMo.put(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION, STARTABLE_CV);
        cvMo.put(MO_FDN, MO_FDN);
        actionParameters.put(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION, STARTABLE_CV);
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME, STARTABLE_CV);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheck() {
        when(commonCvOperations.precheckForSetStartCVSetFistInRolback(Matchers.anyMap(), Matchers.anyString())).thenReturn(true);
        assertTrue(setStartableCVRemoteServiceImpl.precheckOnMo(cvMo, actionParameters));
    }

    @Test
    public void testExecuteAction() {
        assertEquals(0, setStartableCVRemoteServiceImpl.executeAction(MO_FDN, "nodeName", actionParameters));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NumberFormatException.class)
    public void testExecuteActionWithException() {
        final NumberFormatException ex = new NumberFormatException();
        when(commonCvOperations.executeActionOnMo(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenThrow(ex);
        setStartableCVRemoteServiceImpl.executeAction(MO_FDN, "nodeName", actionParameters);
    }

}