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
package com.ericsson.oss.services.shm.cpp.inventory.service.upgrade;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.upgrade.UpgradeEvent;
import com.ericsson.oss.itpf.sdk.upgrade.UpgradePhase;

@RunWith(PowerMockRunner.class)
public class CppInventorySynchEventListenerUpgradeEventObserverTest {

    @Mock
    private UpgradeEvent eventMock;

    @Mock
    private SystemRecorder systemRecorderMock;

    @Mock
    private Logger loggerMock;

    @Mock
    private UpgradeEventHandler upgradeEventHandler;

    @InjectMocks
    private CppInventorySynchEventListenerUpgradeEventObserver cppObserver;

    @Test
    public void test_upgradeNotificationObserver_SERVICE_CLUSTER_UPGRADE_FAILED() {
        final UpgradePhase phase = UpgradePhase.SERVICE_CLUSTER_UPGRADE_FAILED;
        when(eventMock.getPhase()).thenReturn(phase);

        cppObserver.upgradeNotificationObserver(eventMock);

        verify(eventMock).accept(anyString());
    }

    @Test
    public void test_upgradeNotificationObserver_SERVICE_CLUSTER_UPGRADE_FINISHED_SUCCESSFULLY() {
        final UpgradePhase phase = UpgradePhase.SERVICE_CLUSTER_UPGRADE_FINISHED_SUCCESSFULLY;
        when(eventMock.getPhase()).thenReturn(phase);

        cppObserver.upgradeNotificationObserver(eventMock);

        verify(eventMock).accept(anyString());
    }

    @Test
    public void test_upgradeNotificationObserver_SERVICE_CLUSTER_UPGRADE_PREPARE() {
        final UpgradePhase phase = UpgradePhase.SERVICE_CLUSTER_UPGRADE_PREPARE;
        when(eventMock.getPhase()).thenReturn(phase);

        cppObserver.upgradeNotificationObserver(eventMock);

        verify(eventMock).accept(anyString());
    }

    @Test
    public void test_upgradeNotificationObserver_SERVICE_INSTANCE_UPGRADE_FAILED() {
        final UpgradePhase phase = UpgradePhase.SERVICE_INSTANCE_UPGRADE_FAILED;
        when(eventMock.getPhase()).thenReturn(phase);

        cppObserver.upgradeNotificationObserver(eventMock);

        verify(eventMock).accept(anyString());
    }

    @Test
    public void test_upgradeNotificationObserver_SERVICE_INSTANCE_UPGRADE_FINISHED_SUCCESSFULLY() {
        final UpgradePhase phase = UpgradePhase.SERVICE_INSTANCE_UPGRADE_FINISHED_SUCCESSFULLY;
        when(eventMock.getPhase()).thenReturn(phase);

        cppObserver.upgradeNotificationObserver(eventMock);

        verify(eventMock).accept(anyString());
    }

    @Test
    public void test_upgradeNotificationObserver_stopsNotifications_SERVICE_INSTANCE_UPGRADE_PREPARE() {
        final UpgradePhase phase = UpgradePhase.SERVICE_INSTANCE_UPGRADE_PREPARE;
        when(eventMock.getPhase()).thenReturn(phase);
        when(eventMock.isServiceRestartRequired()).thenReturn(true);

        cppObserver.upgradeNotificationObserver(eventMock);

        verify(upgradeEventHandler).verifyOngoingNotificationsAndAcceptUpgrade(eventMock);
    }

    @Test
    public void test_upgradeNotificationObserver_doesntStopsNotifications_SERVICE_INSTANCE_UPGRADE_PREPARE() {
        final UpgradePhase phase = UpgradePhase.SERVICE_INSTANCE_UPGRADE_PREPARE;
        when(eventMock.getPhase()).thenReturn(phase);
        when(eventMock.isServiceRestartRequired()).thenReturn(false);

        cppObserver.upgradeNotificationObserver(eventMock);

        verify(upgradeEventHandler, never()).verifyOngoingNotificationsAndAcceptUpgrade(eventMock);
    }

    @Test
    @PrepareOnlyThisForTest(UpgradePhase.class)
    public void test_upgradeNotificationObserver_defaultCase() {
        final UpgradePhase unknown_phase = PowerMockito.mock(UpgradePhase.class);
        Whitebox.setInternalState(unknown_phase, "name", "UNKNOWN");
        Whitebox.setInternalState(unknown_phase, "ordinal", 6);
        PowerMockito.mockStatic(UpgradePhase.class);
        final UpgradePhase[] values = new UpgradePhase[] { UpgradePhase.SERVICE_CLUSTER_UPGRADE_FAILED, UpgradePhase.SERVICE_CLUSTER_UPGRADE_FINISHED_SUCCESSFULLY,
                UpgradePhase.SERVICE_CLUSTER_UPGRADE_PREPARE, UpgradePhase.SERVICE_INSTANCE_UPGRADE_FAILED, UpgradePhase.SERVICE_INSTANCE_UPGRADE_FINISHED_SUCCESSFULLY,
                UpgradePhase.SERVICE_INSTANCE_UPGRADE_PREPARE, unknown_phase };

        PowerMockito.when(UpgradePhase.values()).thenReturn(values);
        when(eventMock.getPhase()).thenReturn(unknown_phase);
        cppObserver.upgradeNotificationObserver(eventMock);
        verify(eventMock).accept(anyString());
    }

}
