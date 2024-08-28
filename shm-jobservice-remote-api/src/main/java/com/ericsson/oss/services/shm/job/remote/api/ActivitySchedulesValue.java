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
package com.ericsson.oss.services.shm.job.remote.api;

import java.io.Serializable;

public class ActivitySchedulesValue implements Serializable {

    private static final long serialVersionUID = 1L;
    
     private String neType; 
     private ScheduledTaskValue [] scheduledTaskValue;
    /**
     * @return the neType
     */
    public String getNeType() {
        return neType;
    }
    
    /**
     * @param neType the neType to set
     */
    public void setNeType(final String neType) {
        this.neType = neType;
    }
    
    /**
     * @return the value
     */
    public ScheduledTaskValue[] getValue() {
        return scheduledTaskValue;
    }
    
    /**
     * @param value the value to set
     */
    public void setValue(final ScheduledTaskValue [] value) {
        this.scheduledTaskValue = value;
    }
    
}
