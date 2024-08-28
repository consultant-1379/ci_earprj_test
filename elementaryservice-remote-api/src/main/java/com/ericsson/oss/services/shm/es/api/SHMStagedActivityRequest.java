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
package com.ericsson.oss.services.shm.es.api;

import java.io.Serializable;
import java.util.Map;

import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

public class SHMStagedActivityRequest extends SHMActivityRequest implements Serializable {

    private long stagedActivityPoId;
    private static final long serialVersionUID = 1L;

    public SHMStagedActivityRequest(final Map<String, Object> stagedActivityPO) {
        super((String) stagedActivityPO.get(ShmConstants.WORKFLOW_INSTANCE_ID), (String) stagedActivityPO.get(ShmConstants.BUSINESS_KEY), (String) stagedActivityPO.get(ShmConstants.JOB_TYPE));
        setActivityName((String) stagedActivityPO.get(ShmConstants.ACTIVITYNAME));
        setPlatformType((String) stagedActivityPO.get(ShmConstants.PLATFORM));
        setRetryCount((int) stagedActivityPO.get(ShmConstants.RETRY_COUNT));
        setActivityJobId((long) stagedActivityPO.get(ShmConstants.ACTIVITY_JOB_ID));
    }

    public long getStagedActivityPoId() {
        return stagedActivityPoId;
    }

    public void setStagedActivityPoId(final long stagedActivityPoId) {
        this.stagedActivityPoId = stagedActivityPoId;
    }

    @Override
    public String toString() {
        return "SHMStagedActivityRequest [businessKey=" + getBusinessKey() + ", platformType=" + getPlatformType() + ", activityName=" + getActivityName() + ", jobType()=" + getJobType() + "]";
    }
}
