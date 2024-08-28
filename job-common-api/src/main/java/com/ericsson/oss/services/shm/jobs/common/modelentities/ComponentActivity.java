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
 * class to hold component activity information like component name and its selected activities
 * 
 * @author xkalkil
 *
 */
public class ComponentActivity implements Serializable {

    private static final long serialVersionUID = -1872086541054210982L;
    private String componentName;
    private List<String> activityNames;

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(final String componentName) {
        this.componentName = componentName;
    }

    public List<String> getActivityNames() {
        return activityNames;
    }

    public void setActivityNames(final List<String> activityNames) {
        this.activityNames = activityNames;
    }

}
