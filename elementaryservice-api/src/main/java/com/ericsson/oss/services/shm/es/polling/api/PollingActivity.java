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
package com.ericsson.oss.services.shm.es.polling.api;

import javax.ejb.Local;

/**
 * This interface introduced for polling. Adding activity information into ConcurrentHashMap after action triggered on the node for an actions which involved node restart like(upgrade, restore). And
 * timer triggers at some configured interval of time. Whenever timer trigger this interface make a call to corresponding elementary service.
 * 
 * @author tcsgusw
 * 
 */
@Local
public interface PollingActivity {

    /**
     * This will be called by polling timer whenever the polling time expires,to evaluate the MO action status for the activities which are subscribed for polling.
     * 
     * @param activityJobId
     *            Id for the activity
     * @param moFdn
     *            MO Fdn on which this method going to check the MO action status.
     */
    @Deprecated
    void readActivityStatus(final long activityJobId, final String moFdn);
}