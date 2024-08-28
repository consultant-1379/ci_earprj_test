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
package com.ericsson.oss.services.shm.jobs.common.modelentities;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;

public class Activity {

    private String name;
    private int order;
    private PlatformTypeEnum platform;
    private Schedule schedule;
    private String neType;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the order
     */
    public int getOrder() {
        return order;
    }

    /**
     * @param order
     *            the order to set
     */
    public void setOrder(final int order) {
        this.order = order;
    }

    /**
     * @return the platform
     */
    public PlatformTypeEnum getPlatform() {
        return platform;
    }

    /**
     * @param platform
     *            the platform to set
     */
    public void setPlatform(final PlatformTypeEnum platform) {
        this.platform = platform;
    }

    /**
     * @return the schedule
     */
    public Schedule getSchedule() {
        return schedule;
    }

    /**
     * @param schedule
     *            the schedule to set
     */
    public void setSchedule(final Schedule schedule) {
        this.schedule = schedule;
    }

    /**
     * @return the neType
     */
    public String getNeType() {
        return neType;
    }

    /**
     * @param neType
     *            the neType to set
     */
    public void setNeType(final String neType) {
        this.neType = neType;
    }
}
