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
package com.ericsson.oss.services.shm.notifications.api;

/**
 * Enum for the types of events for which SHM receives notifications.
 * 
 * @author xyerrsr
 * 
 */
public enum NotificationEventTypeEnum {

    CREATE("CREATE"), AVC("AVC"), DELETE("DELETE");

    private String eventType;

    /**
     * Constructor that takes the event type for initialization.
     * 
     * @param eventType
     */
    NotificationEventTypeEnum(final String eventType) {
        this.eventType = eventType;
    }

    /**
     * @return the notification event type.
     */
    public String getEventType() {
        return eventType;
    }

}
