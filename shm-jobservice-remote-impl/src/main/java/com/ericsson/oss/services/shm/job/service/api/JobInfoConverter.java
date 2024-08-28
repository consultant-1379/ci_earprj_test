/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.service.api;

import com.ericsson.oss.services.shm.job.exceptions.NoMeFDNSProvidedException;
import com.ericsson.oss.services.shm.job.remote.api.ShmRemoteJobData;
import com.ericsson.oss.services.shm.job.remote.api.errorcodes.JobCreationResponseCode;
import com.ericsson.oss.services.shm.jobservice.common.JobInfo;
import com.ericsson.oss.services.topologyCollectionsService.exception.service.TopologyCollectionsServiceException;

/**
 * Converter to convert external POJO values to domain specific JobInfo to create job
 * 
 * @author tcsmaup
 * 
 */
public interface JobInfoConverter {

    /**
     * Convert Remote job data values to Job info values for creation of Job
     * 
     * @param shmRemoteJobData
     * @return
     * @throws TopologyCollectionsServiceException
     * @throws NoMeFDNSProvidedException
     */
    JobInfo prepareJobInfoData(ShmRemoteJobData shmRemoteJobData) throws TopologyCollectionsServiceException, NoMeFDNSProvidedException;

    /**
     * To validate remote shm remote job data if Job creation can be proceeded
     * 
     * @param shmRemoteJobData
     * @return
     */
    JobCreationResponseCode isValidData(final ShmRemoteJobData shmJobData);
}
