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
package com.ericsson.oss.services.shm.notifications.impl.license;

import javax.ejb.Singleton;
import javax.ejb.Startup;

import com.ericsson.oss.itpf.sdk.eventbus.classic.EventConsumerBean;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.notifications.impl.AbstractNotificationListener;

@Singleton
@Startup
public class LicenseNotificationObserver extends AbstractNotificationListener {

    @Override
    protected String getFilter() {
        return ShmCommonConstants.SHM_LICENSE_NOTFICATION_FILTER;
    }

    @Override
    protected boolean startListeningForJobNotifications(final EventConsumerBean eventConsumerBean) {
        return eventConsumerBean.startListening(new LicenseNotificationQueueListener(notificationReciever));
    }
}
