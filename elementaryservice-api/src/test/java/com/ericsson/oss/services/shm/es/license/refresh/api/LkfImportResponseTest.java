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
package com.ericsson.oss.services.shm.es.license.refresh.api;

import java.util.Date;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LkfImportResponseTest {

    @InjectMocks
    LkfImportResponse lkfImportResponse;

    @Test
    public void testLkfImportResponse() {

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
