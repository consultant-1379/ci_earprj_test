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
package com.ericsson.oss.services.shm.es.impl;

import java.util.Map;

import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

public class JobEnvironment {

    private String nodeName;
    final private long activityJobId;
    private Map<String, Object> activityJobAttributes;
    private long neJobId = -1;
    private Map<String, Object> neJobAttributes;
    private long mainJobId = -1;
    private Map<String, Object> mainJobAttributes;
    final ActivityUtils activityUtil;

    /**
     * @param activityJobId
     * @param activityUtil
     */
    public JobEnvironment(final long activityJobId, final ActivityUtils activityUtil) {
        this.activityJobId = activityJobId;
        this.activityUtil = activityUtil;

    }

    /**
     * @return the nodeName
     */
    public String getNodeName() {
        nodeName = (nodeName != null ? nodeName : (String) this.getNeJobAttributes().get(ShmConstants.NE_NAME));
        return nodeName;
    }

    /**
     * @return the activityJobId
     */
    public long getActivityJobId() {
        return activityJobId;
    }

    /**
     * @return the activityJobAttributes
     */
    public Map<String, Object> getActivityJobAttributes() {
        activityJobAttributes = activityJobAttributes != null ? activityJobAttributes : activityUtil.getPoAttributes(activityJobId);
        return activityJobAttributes;
    }

    /**
     * @return the neJobId
     */
    public long getNeJobId() {
        neJobId = neJobId != -1 ? neJobId : (long) this.getActivityJobAttributes().get(ShmConstants.NE_JOB_ID);
        return neJobId;
    }

    /**
     * @return the neJobAttributes
     */
    public Map<String, Object> getNeJobAttributes() {
        neJobAttributes = neJobAttributes != null ? neJobAttributes : activityUtil.getPoAttributes(this.getNeJobId());
        return neJobAttributes;
    }

    /**
     * @return the neJobAttributes
     */
    public Map<String, Object> getNeJobAttributes(final long neJobId) {
        neJobAttributes = neJobAttributes != null ? neJobAttributes : activityUtil.getPoAttributes(neJobId);
        return neJobAttributes;
    }

    /**
     * @return the mainJobId
     */
    public long getMainJobId() {
        mainJobId = mainJobId != -1 ? mainJobId : (long) this.getNeJobAttributes().get(ShmConstants.MAIN_JOB_ID);
        return mainJobId;
    }

    /**
     * @return the mainJobAttributes
     */
    public Map<String, Object> getMainJobAttributes() {
        mainJobAttributes = mainJobAttributes != null ? mainJobAttributes : activityUtil.getPoAttributes(this.getMainJobId());
        return mainJobAttributes;
    }

    public Map<String, Object> getMainJobAttributes(final long mainJobId) {
        mainJobAttributes = mainJobAttributes != null ? mainJobAttributes : activityUtil.getPoAttributes(mainJobId);
        return mainJobAttributes;
    }
}
