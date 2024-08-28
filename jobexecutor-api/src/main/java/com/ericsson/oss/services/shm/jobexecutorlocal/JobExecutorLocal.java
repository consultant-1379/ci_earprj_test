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
package com.ericsson.oss.services.shm.jobexecutorlocal;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

@Local
public interface JobExecutorLocal {

    long prepareMainJob(String wfsId, long jobTemplateId, Date nextExecutionTime);

    void execute(String wfsId, long jobTemplateId);

    /**
     * Method to initiate Jobs manually
     * 
     * @param jobIds
     */
    void invokeMainJobsManually(final List<Long> jobIds, final String loggedInUser);

    /**
     * Method to initiate NE level Jobs manually
     * 
     * @param neJobIds
     */
    void invokeNeJobsManually(final List<Long> neJobIds, final String loggedInUser);

    /**
     * Cancels executing main jobs and NE jobs. When a main job is cancelled, all sub-jobs(NE Jobs, activity jobs) are cancelled. When a NE Job is cancelled, only the NE Job(and associated Activity
     * Jobs) is cancelled, the main job will continue running the other NE Jobs.
     * 
     * @param jobIds
     *            - List of Job PO ids - ["poid1", "poid2", "poid3"]. In a single invocation, it could be either a list of main job ids or NE Jobs ids, but not a mixture of both.
     * @return - Response object having Job cancellation details
     */

    void cancelJobs(final List<Long> jobIds, String cancelledBy);

    /**
     * Method to retrieve supported and unsupported network elements for job creation
     * 
     * @param neNames
     * @param jobType
     */
    Map<String, Object> getSupportedNes(final List<String> neNames, final JobTypeEnum jobType);

}
