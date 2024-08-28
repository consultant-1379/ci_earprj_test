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
package com.ericsson.oss.services.shm.jobs.common.api;

import java.util.List;

public class JobActivitiesQuery {

    private String jobType;
    private List<NeInfoQuery> neTypes;

    /**
     * @return the neTypes
     */
    public List<NeInfoQuery> getNeTypes() {
        return neTypes;
    }

    /**
     * @param neTypes
     *            the neTypes to set
     */
    public void setNeTypes(final List<NeInfoQuery> neTypes) {
        this.neTypes = neTypes;
    }

    /**
     * @return the jobType
     */
    public String getJobType() {
        return jobType;
    }

    /**
     * @param jobType
     *            the jobType to set
     */
    public void setJobType(final String jobType) {
        this.jobType = jobType;
    }

    @Override
    public String toString() {
        final StringBuilder neTypeBuilder = new StringBuilder();
        final List<NeInfoQuery> neTypeList = this.getNeTypes();
        if (neTypeList != null && (!neTypeList.isEmpty())) {
            for (int index = 0; index < neTypeList.size(); index++) {
                neTypeBuilder.append(neTypeList.get(index) + ";");
            }

        }
        final String neTypes = neTypeBuilder.toString();
        return "jobType : " + this.getJobType() + "; neTypes : " + neTypes;
    }

}
