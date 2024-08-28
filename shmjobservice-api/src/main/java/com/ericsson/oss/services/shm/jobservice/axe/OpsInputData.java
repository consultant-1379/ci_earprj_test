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

import java.util.List;
import java.util.Map;

import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobservice.common.NEJobInfo;

/**
 * This is a pojo class which is used to map Request body of /ops-sessionid-clusterid rest call.It is specific to AXE
 * 
 * @author Team Royals
 */
public class OpsInputData {

    private JobTypeEnum jobType;
    private Map<String, List<NEJobInfo>> neTypeToNeJobs;
    private String user;
    private Long mainJobId;

    /**
     * @return the jobType
     */
    public JobTypeEnum getJobType() {
        return jobType;
    }

    /**
     * @param jobType
     *            the jobType to set
     */
    public void setJobType(final JobTypeEnum jobType) {
        this.jobType = jobType;
    }

    /**
     * @return the neTypeToNeJobs
     */
    public Map<String, List<NEJobInfo>> getNeTypeToNeJobs() {
        return neTypeToNeJobs;
    }

    /**
     * @param neTypeToNeJobs
     *            the neTypeToNeJobs to set
     */
    public void setNeTypeToNeJobIds(final Map<String, List<NEJobInfo>> neTypeToNeJobs) {
        this.neTypeToNeJobs = neTypeToNeJobs;
    }

    /**
     * returns logged in user
     */
    public String getUser() {
        return user;
    }

    /**
     * @param user
     *            this is loggedin user to set
     */
    public void setUser(final String user) {
        this.user = user;
    }

    public Long getMainJobId() {
        return mainJobId;
    }

    public void setMainJobId(final Long mainJobId) {
        this.mainJobId = mainJobId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final StringBuilder opsInputData = new StringBuilder();
        opsInputData.append("{job Type:").append(this.jobType).append(",neTypeToNeJobs:").append(this.neTypeToNeJobs).append(",user:").append(user).append(",mainJobId:").append(mainJobId).append("}");
        return opsInputData.toString();
    }
}
