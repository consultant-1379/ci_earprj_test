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
package com.ericsson.oss.services.shm.es.impl.ecim.licenserefresh;

import com.ericsson.oss.itpf.sdk.eventbus.classic.EMessageListener;
import com.ericsson.oss.services.shm.model.notification.ShmElisLicenseRefreshNotification;

public class ShmElisLicenseRefreshNotificationStub implements EMessageListener<ShmElisLicenseRefreshNotification> {

    @Override
    public void onMessage(final ShmElisLicenseRefreshNotification shmElisLicenseRefreshNotification) {
    }
}
