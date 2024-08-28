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
package com.ericsson.oss.services.shm.es.api;

import javax.ejb.Local;

import com.ericsson.oss.services.shm.notifications.api.Notification;

/**
 * This interface helps to process notifications those are received on Topic.
 * 
 * @author tcssagu
 * 
 */
@Local
public interface RemoteActivityCallBack {

    void processNotification(Notification message);
}
