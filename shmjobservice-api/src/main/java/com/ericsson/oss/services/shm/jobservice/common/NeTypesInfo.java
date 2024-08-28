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

import java.util.Set;

import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

public class NeTypesInfo {

    private JobTypeEnum jobType;
    private Set<String> neTypes;

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
     * @return the neTypes
     */
    public Set<String> getNeTypes() {
        return neTypes;
    }

    /**
     * @param neTypes
     *            the neTypes to set
     */
    public void setNeTypes(final Set<String> neTypes) {
        this.neTypes = neTypes;
    }

}
