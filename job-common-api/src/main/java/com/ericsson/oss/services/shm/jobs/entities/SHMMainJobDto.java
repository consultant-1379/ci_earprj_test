/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.jobs.entities;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

public class SHMMainJobDto extends AbstractJobDto implements Serializable {

    private static final long serialVersionUID = 1L;
    private final long jobId;
    private final long jobTemplateId;
    private final String jobName;
    private final String createdBy;
    private final String totalNoOfNEs;
    private final Date creationTime;
    private final Boolean periodic;
    private final String jobType;

    public SHMMainJobDto(final Map<String, Object> templateAttributes, final Map<String, Object> mainJobAttributes) {

        super(mainJobAttributes);

        this.jobId = (long) mainJobAttributes.get(ShmConstants.PO_ID);
        this.jobTemplateId = (long) mainJobAttributes.get(ShmConstants.JOBTEMPLATEID);
        this.jobName = getAsString(templateAttributes.get(ShmConstants.NAME));
        this.createdBy = getAsString(templateAttributes.get(ShmConstants.OWNER));
        this.creationTime = getAsDate(templateAttributes.get(ShmConstants.CREATION_TIME));
        final int totalNoOfNodes = getAsInt(mainJobAttributes.get(ShmConstants.NO_OF_NETWORK_ELEMENTS));
        this.totalNoOfNEs = totalNoOfNodes > 0 ? String.valueOf(totalNoOfNodes) : ShmConstants.NE_NOT_AVAILABLE;
        this.jobType = getAsString(templateAttributes.get(ShmConstants.JOB_TYPE));
        this.periodic = templateAttributes.get(ShmConstants.PERIODIC) != null ? (Boolean) templateAttributes.get(ShmConstants.PERIODIC) : Boolean.FALSE;

    }

    public final long getJobId() {
        return jobId;
    }

    public final long getJobTemplateId() {
        return jobTemplateId;
    }

    public final String getJobName() {
        return jobName;
    }

    public final String getCreatedBy() {
        return createdBy;
    }

    public final String getTotalNoOfNEs() {
        return totalNoOfNEs;
    }

    public final Date getCreationTime() {
        return creationTime;
    }

    public final Boolean getPeriodic() {
        return periodic;
    }

    public final String getJobType() {
        return jobType;
    }

}
