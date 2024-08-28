/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.remote.api;

import javax.ejb.Remote;

import com.ericsson.oss.itpf.sdk.core.annotation.EService;
import com.ericsson.oss.services.shm.job.remote.api.exceptions.JobStatusException;

/**
 * Interface to provide job information
 * 
 * @author xgudpra
 * 
 */
@EService
@Remote
public interface ShmRemoteJobInformationService {

    /**
     * Method to retrieve job status based on job name.
     * 
     * @param jobName
     *            Name of the job
     * @return JobStatus
     * @throws JobStatusException
     * 
     * @since 1.71.14
     * 
     */
    JobStatus getJobStatus(final String jobName) throws JobStatusException;

}
