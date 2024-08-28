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
import java.util.Map;

import com.ericsson.oss.services.shm.job.activity.NeActivityInformation;

public class JobActivitiesResponse {
    private String platform;
    private String jobType;
    private List<NeActivityInformation> neActivityInformation;
    private Map<String, String> unsupportedNeTypes;

    /**
     * @return the platform
     */
    public String getPlatform() {
        return platform;
    }

    /**
     * @param platform
     *            the platform to set
     */
    public void setPlatform(final String platform) {
        this.platform = platform;
    }

    /**
     * @return the neActivityInformation
     */
    public List<NeActivityInformation> getNeActivityInformation() {
        return neActivityInformation;
    }

    /**
     * @param neActivityInformation
     *            the neActivityInformation to set
     */
    public void setNeActivityInformation(final List<NeActivityInformation> neActivityInformation) {
        this.neActivityInformation = neActivityInformation;
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

    /**
     * @return the unsupportedNeTypes
     */
    public Map<String, String> getUnsupportedNeTypes() {
        return unsupportedNeTypes;
    }

    /**
     * @param unsupportedNeTypes
     *            the unsupportedNeTypes to set
     */
    public void setUnsupportedNeTypes(final Map<String, String> unsupportedNeTypes) {
        this.unsupportedNeTypes = unsupportedNeTypes;
    }

    @Override
    public String toString() {
        final StringBuilder neActivityInformationBuilder = new StringBuilder();
        final List<NeActivityInformation> neActivityInformationList = this.getNeActivityInformation();
        if (neActivityInformationList != null && (!neActivityInformationList.isEmpty())) {
            for (int index = 0; index < neActivityInformationList.size(); index++) {
                neActivityInformationBuilder.append(neActivityInformationList.get(index) + ";");
            }

        }
        final String neActivityInformation = (neActivityInformationBuilder != null && !("".equals(neActivityInformationBuilder.toString()))) ? "Additional Info : "
                + neActivityInformationBuilder.toString() : "";

        return "platform : " + this.getPlatform() + "; jobType : " + this.getJobType() + "; neActivityInformation : " + neActivityInformation + "; unsupportedNeTypes : "
                + this.getUnsupportedNeTypes();
    }
}
