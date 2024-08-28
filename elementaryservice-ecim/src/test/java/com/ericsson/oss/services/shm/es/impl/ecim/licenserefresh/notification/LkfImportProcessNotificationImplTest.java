/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.ecim.licenserefresh.notification;

import java.util.Date;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.services.shm.es.license.refresh.api.LkfImportResponse;
import com.ericsson.oss.services.shm.model.NotificationSubject;

@RunWith(MockitoJUnitRunner.class)
public class LkfImportProcessNotificationImplTest {

    @InjectMocks
    LkfImportProcessNotificationImpl lkfImportProcessNotificationImpl;

    @Mock
    DpsDataChangedEvent dataPersistanceServiceDataChangedEvent;

    @Mock
    NotificationSubject notifySubjec;

    @Test
    public void testLkfImportProcessNotificationImpl() {

        LkfImportResponse lkfImportResponse = new LkfImportResponse();

        lkfImportResponse.setActivityJobId(12347);
        lkfImportResponse.setActivityName("request");
        lkfImportResponse.setAdditionalInfo("Import is successful");
        lkfImportResponse.setEventAttributes(new HashMap<>());
        lkfImportResponse.setFingerprint("LTE26dg2ERBS00001_fp");
        lkfImportResponse.setNeJobId("12346");
        lkfImportResponse.setNotificationReceivedTime(new Date());
        lkfImportResponse.setState("IMPORT_COMPLETED");
        lkfImportResponse.setStatus("SUCCESS");
        lkfImportResponse.toString();
        lkfImportProcessNotificationImpl.setLkfImportResponse(lkfImportResponse);
        Assert.assertNotNull(lkfImportProcessNotificationImpl.getNotificationEventType().AVC.toString());
        Assert.assertNotNull(lkfImportProcessNotificationImpl.getDpsDataChangedEvent());
        Assert.assertNotNull(lkfImportProcessNotificationImpl.getNotificationSubject());
        Assert.assertNotNull(lkfImportProcessNotificationImpl.getLkfImportResponse());
        Assert.assertNotNull(lkfImportResponse.getActivityName());
        Assert.assertNotNull(lkfImportResponse.getAdditionalInfo());
        Assert.assertNotNull(lkfImportResponse.getFingerprint());
        Assert.assertNotNull(lkfImportResponse.getNeJobId());
        Assert.assertNotNull(lkfImportResponse.getState());
        Assert.assertNotNull(lkfImportResponse.getStatus());
        Assert.assertNotNull(lkfImportResponse.getActivityJobId());
        Assert.assertNotNull(lkfImportResponse.getEventAttributes());
        Assert.assertNotNull(lkfImportResponse.getNotificationReceivedTime());

    }
}
