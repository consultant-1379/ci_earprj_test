/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.nejob.cache;


/**
 * provides NE job level static data
 * 
 * @author tcsgusw
 * 
 */
public class NEJobStaticData {

    private final long neJobId;
    private final long mainJobId;
    private final String nodeName;
    private final String neJobBusinessKey;
    private final String platformType;
    private final long activityStartTime;
    private final String parentNodeName;

    public NEJobStaticData(final long neJobId, final long mainJobId, final String nodeName, final String neJobBusinessKey, final String platformType, final long activityStartTime,
            final String parentNodeName) {
        this.neJobId = neJobId;
        this.mainJobId = mainJobId;
        this.nodeName = nodeName;
        this.neJobBusinessKey = neJobBusinessKey;
        this.platformType = platformType;
        this.activityStartTime = activityStartTime;
        this.parentNodeName = parentNodeName;

    }

    public long getNeJobId() {
        return neJobId;
    }

    public long getMainJobId() {
        return mainJobId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getNeJobBusinessKey() {
        return neJobBusinessKey;
    }

    public String getPlatformType() {
        return platformType;
    }

    public long getActivityStartTime() {
        return activityStartTime;
    }

    /**
     * @return the parentNodeName
     */
    public String getParentNodeName() {
        return parentNodeName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "NEJobStaticData nodeName " + nodeName + " and parentNodeName " + parentNodeName;
    }

}
