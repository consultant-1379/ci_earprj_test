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
package com.ericsson.oss.services.shm.jobs.common.modelentities;

import java.io.Serializable;
import java.util.List;

/**
 * class to hold component activity information for each NE type
 * 
 * @author xkalkil
 *
 */
public class NeTypeComponentActivityDetails implements Serializable {

    private static final long serialVersionUID = 909962997608130606L;
    private String neType;
    private List<ComponentActivity> componentActivities;

    public String getNeType() {
        return neType;
    }

    public void setNeType(final String neType) {
        this.neType = neType;
    }

    public List<ComponentActivity> getComponentActivities() {
        return componentActivities;
    }

    public void setComponentActivities(final List<ComponentActivity> componentActivities) {
        this.componentActivities = componentActivities;
    }

}
