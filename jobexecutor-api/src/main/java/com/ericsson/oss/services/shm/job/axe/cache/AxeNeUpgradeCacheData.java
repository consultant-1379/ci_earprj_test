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
package com.ericsson.oss.services.shm.job.axe.cache;

/**
 * This class holds the static data of each AXE ne in upgrade job which contains cp function and number of APGs
 * 
 * @author tcsanpr
 * 
 */
public class AxeNeUpgradeCacheData {

    private final String cpFunction;
    private final String numberOfApg;
    private final String parentName;

    public AxeNeUpgradeCacheData(final String cpFunction, final String numberOfApg, final String parentName) {
        this.cpFunction = cpFunction;
        this.numberOfApg = numberOfApg;
        this.parentName = parentName;
    }

    /**
     * @return the cpFunction
     */
    public String getCpFunction() {
        return cpFunction;
    }

    /**
     * @return the numberOfApg
     */
    public String getNumberOfApg() {
        return numberOfApg;
    }

    /**
     * @return the parentName
     */
    public String getParentName() {
        return parentName;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("[ numberOfApg : ").append(numberOfApg).append(", cpFunction : ").append(cpFunction).append(", parentName : ").append(parentName).append("]");
        return builder.toString();
    }
}
