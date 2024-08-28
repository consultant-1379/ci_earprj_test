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
package com.ericsson.oss.services.shm.jobservice.axe;

/**
 * This is a pojo class which is used to send as response of /ops-sessionid-clusterid rest call.It is specific to AXE
 * 
 * @author Team Royals
 */
public class OpsSessionAndClusterIdInfo {

    //nodeName can be node name or node sub components(cluster name or spx name)
    private String nodeName;
    private String clusterID;
    private String sessionID;
    private String activityJobId;
    private String failureReason;

    /**
     * @return the nodeName
     */
    public String getNodeName() {
        return nodeName;
    }

    /**
     * @param nodeName
     *            the nodeName to set
     */
    public void setNodeName(final String nodeName) {
        this.nodeName = nodeName;
    }

    /**
     * @return the clusterID
     */
    public String getClusterID() {
        return clusterID;
    }

    /**
     * @param clusterID
     *            the clusterID to set
     */
    public void setClusterID(final String clusterID) {
        this.clusterID = clusterID;
    }

    /**
     * @return the sessionID
     */
    public String getSessionID() {
        return sessionID;
    }

    /**
     * @param sessionID
     *            the sessionID to set
     */
    public void setSessionID(final String sessionID) {
        this.sessionID = sessionID;
    }

    /**
     * @return the failureReason
     */
    public String getFailureReason() {
        return failureReason;
    }

    /**
     * @param failureReason
     *            the failureReason to set
     */
    public void setFailureReason(final String failureReason) {
        this.failureReason = failureReason;
    }

    /**
     * @return the activityJobId
     */
    public String getActivityJobId() {
        return activityJobId;
    }

    /**
     * @param activityJobId
     *            the activityJobId to set
     */
    public void setActivityJobId(final String activityJobId) {
        this.activityJobId = activityJobId;
    }

    @Override
    public String toString() {
        final StringBuilder opsSessionAndClusterIdInfo = new StringBuilder();
        opsSessionAndClusterIdInfo.append("{nodeName:").append(this.nodeName).append(",sessionID:").append(this.sessionID).append(",clusterID:").append(this.clusterID).append(",activityJobId:")
                .append(activityJobId).append(",failureReason:").append(this.failureReason).append("}");
        return opsSessionAndClusterIdInfo.toString();
    }
}
