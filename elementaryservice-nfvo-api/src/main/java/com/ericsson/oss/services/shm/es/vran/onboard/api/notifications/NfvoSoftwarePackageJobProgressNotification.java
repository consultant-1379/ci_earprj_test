/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.shm.es.vran.onboard.api.notifications;

import com.ericsson.oss.services.shm.notifications.api.Notification;



/**
 * This interface will provide abstract methods for getting job progress notification.
 */
public interface NfvoSoftwarePackageJobProgressNotification extends Notification {

    /**
     * method declaration to get NfvoSoftwarePackageJobNotification object.
     */
    NfvoSoftwarePackageJobResponse getNfvoSoftwarePackageJobNotification();

    /**
     * method declaration to set NfvoSoftwarePackageJobNotification object.
     *
     * @param OnboardNotification
     */
    void setNfvoSoftwarePackageJobNotification(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse);

}
