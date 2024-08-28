/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.shm.es.vran.notifications.api;

import com.ericsson.oss.services.shm.notifications.api.Notification;

/**
 * This interface will provide abstract methods for getting job progress notification.
 */
public interface VranUpgradeJobNotification extends Notification {

    /**
     * method declaration to get VranNotification object.
     */
    VranSoftwareUpgradeJobResponse getVranNotification();

    /**
     * method declaration to set VranNotification object.
     *
     * @param VranSoftwareUpgradeJobResponse
     */
    void setVranNotification(final VranSoftwareUpgradeJobResponse vranNotification);

}
