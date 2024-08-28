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

/**
 * @author zviskar
 */

public class NEJobInfo {

    //nodeName can be node name or node sub components(cluster name or spx name)
    private String nodeName;
    private long neJobId;

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
     * @return the neJobId
     */
    public long getNeJobId() {
        return neJobId;
    }

    /**
     * @param neJobId
     *            the neJobId to set
     */
    public void setNeJobId(final long neJobId) {
        this.neJobId = neJobId;
    }

    @Override
    public String toString() {
        final StringBuilder neJobInfo = new StringBuilder();
        neJobInfo.append("{nodeName:").append(this.nodeName).append(",neJobId:").append(this.neJobId).append("}");
        return neJobInfo.toString();
    }
}
