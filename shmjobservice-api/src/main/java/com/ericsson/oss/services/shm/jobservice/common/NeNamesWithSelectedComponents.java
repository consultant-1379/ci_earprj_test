/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.jobservice.common;

import java.io.Serializable;
import java.util.List;

/**
 * This class holds ne names with their selected components coming from UI during job creation
 * 
 * @author xaniama
 * 
 */
public class NeNamesWithSelectedComponents implements Serializable {

    private static final long serialVersionUID = 1L;
    private String parentNeName;
    private List<String> selectedComponents;

    public String getParentNeName() {
        return parentNeName;
    }

    public void setParentNeName(final String neName) {
        this.parentNeName = neName;
    }

    public List<String> getSelectedComponents() {
        return selectedComponents;
    }

    public void setSelectedComponents(final List<String> compList) {
        this.selectedComponents = compList;
    }

    /**
     * Overriding toString() of this object, to print the object data.
     * 
     * @return string
     */
    @Override
    public String toString() {
        final List<String> components = this.getSelectedComponents();
        if (components != null) {
            return "NeNamesWithComponents : neName : " + this.getParentNeName() + "; getComponents : " + components;
        }
        return "NeNamesWithComponents : neName : " + this.getParentNeName() + "; getComponents : " + components;
    }

}
