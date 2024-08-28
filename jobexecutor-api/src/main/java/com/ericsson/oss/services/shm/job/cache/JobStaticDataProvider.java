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
package com.ericsson.oss.services.shm.job.cache;

import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;

/**
 * Provides method/methods to get Job static data.
 * 
 * @author tcssbop
 * 
 */
public interface JobStaticDataProvider {

    /**
     * This method will be called from elementary services to get the JobStaticData from cache. It will get JobStaticData from DPS if not exists in cache.
     * 
     * @param mainJobId
     * @param capability
     * @return
     * @throws JobDataNotFoundException
     */

    JobStaticData getJobStaticData(final long mainJobId) throws JobDataNotFoundException;

    /**
     * This method will clear the cache object mapped with given mainJobId.
     * 
     * @param mainJobId
     */
    void clear(final long mainJobId);

    /**
     * This method will clear the cache.
     * 
     */
    void clearAll();

    /**
     * This method adds the jobStaticData into cache.
     * 
     * @param mainJobId
     * @param jobStaticContext
     */

    void put(final long mainJobId, final JobStaticData jobStaticData);
}
