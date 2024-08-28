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
package com.ericsson.oss.services.shm.activity.axe.cache;

/**
 * PoJo with activity order and Activity Name
 * 
 * @author ztamsra
 *
 */
public class AxeSynchronousActivityData implements Comparable<AxeSynchronousActivityData> {
    private final int order;
    private final String activityName;

    public AxeSynchronousActivityData(final String activityName, final int order) {
        this.activityName = activityName;
        this.order = order;
    }

    public int getOrder() {
        return order;
    }

    public String getActivityName() {
        return activityName;
    }

    @Override
    public String toString() {
        return "activityName :" + activityName + "Order :" + order;
    }

    @Override
    public int compareTo(final AxeSynchronousActivityData data) {
        return (this.order - data.order);
    }

    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }
        if (this.getClass() != o.getClass()) {
            return false;
        } else {
            final AxeSynchronousActivityData axeSynchronousActivityData = (AxeSynchronousActivityData) o;
            return (this.order == axeSynchronousActivityData.order);
        }
    }

    @Override
    public int hashCode() {
        return order;
    }
}
