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

import java.util.Date;

public class JobTemplate {

    private String name;
    private String owner;
    private Date creationTime;
    private String description;
    private JobType jobType;
    private JobConfiguration jobConfigurationDetails;
    private boolean isCancelled;
    private boolean isDeletable;
    private Long jobTemplateId;
    private JobCategory jobCategory;

    /**
     * @return the jobCategory
     */
    public JobCategory getJobCategory() {
        return jobCategory;
    }

    /**
     * @param jobCategory
     *            the jobCategory to set
     */
    public void setJobCategory(final JobCategory jobCategory) {
        this.jobCategory = jobCategory;
    }

    /**
     * @return the creationTime
     */
    public Date getCreationTime() {
        return creationTime;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the jobConfigurationDetails
     */
    public JobConfiguration getJobConfigurationDetails() {
        return jobConfigurationDetails;
    }

    /**
     * @return the jobType
     */
    public JobType getJobType() {
        return jobType;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * @return the isCancelled
     */
    public boolean isCancelled() {
        return isCancelled;
    }

    /**
     * @return the isDeletable
     */
    public boolean isDeletable() {
        return isDeletable;
    }

    /**
     * @param isCancelled
     *            the isCancelled to set
     */
    public void setCancelled(final boolean isCancelled) {
        this.isCancelled = isCancelled;
    }

    /**
     * @param creationTime
     *            the creationTime to set
     */
    public void setCreationTime(final Date creationTime) {
        this.creationTime = creationTime;
    }

    /**
     * @param isDeletable
     *            the isDeletable to set
     */
    public void setDeletable(final boolean isDeletable) {
        this.isDeletable = isDeletable;
    }

    /**
     * @param description
     *            the description to set
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * @param jobConfigurationDetails
     *            the jobConfigurationDetails to set
     */
    public void setJobConfigurationDetails(final JobConfiguration jobConfigurationDetails) {
        this.jobConfigurationDetails = jobConfigurationDetails;
    }

    /**
     * @param jobType
     *            the jobType to set
     */
    public void setJobType(final JobType jobType) {
        this.jobType = jobType;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @param owner
     *            the owner to set
     */
    public void setOwner(final String owner) {
        this.owner = owner;
    }

    /**
     * @return the jobTemplateId
     */
    public Long getJobTemplateId() {
        return jobTemplateId;
    }

    /**
     * @param jobTemplateId
     *            the jobTemplateId to set
     */
    public void setJobTemplateId(final Long jobTemplateId) {
        this.jobTemplateId = jobTemplateId;
    }

    @Override
    public String toString() {
        return "JobTemplate [name=" + name + ", owner=" + owner + ", creationTime=" + creationTime + ", description=" + description + ", jobType=" + jobType + ", jobConfigurationDetails="
                + jobConfigurationDetails + ", isCancelled=" + isCancelled + ", isDeletable=" + isDeletable + ", jobTemplateId=" + jobTemplateId + ", jobCategory=" + jobCategory + "]";
    }

}
