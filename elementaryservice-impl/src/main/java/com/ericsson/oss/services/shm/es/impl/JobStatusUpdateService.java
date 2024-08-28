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
package com.ericsson.oss.services.shm.es.impl;

import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

/**
 * This class updates the Job Status based on completed and submitted NEs.
 */
@Traceable
@Profiled
@Stateless
public class JobStatusUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobStatusUpdateService.class);

    @Inject
    DpsWriter dpsWriter;

    @Inject
    DpsReader dpsReader;

    /**
     * This method updates the NE Jobs completed count.
     * 
     * @param mainJobId
     * @return boolean
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public boolean updateNEJobsCompletedCount(final long mainJobId) {
        LOGGER.debug("Checking if NE Jobs are completed for Main Job {} ", mainJobId);
        int neCompleted = 0, neSubmitted = 0;
        boolean neJobDone = false;
        final Map<String, Object> mainJobAttributes = retrieveJob(mainJobId);
        if (mainJobAttributes != null && !mainJobAttributes.isEmpty()) {
            final List<Map<String, String>> mainJobPropertyList = (List<Map<String, String>>) mainJobAttributes.get(ShmConstants.JOBPROPERTIES);
            LOGGER.debug("Main Job Properties  {} ", mainJobPropertyList);
            if (mainJobPropertyList != null && mainJobPropertyList.size() > 0) {
                for (final Map<String, String> jobProperty : mainJobPropertyList) {
                    if (ShmConstants.SUBMITTED_NES.equals(jobProperty.get(ShmConstants.KEY))) {
                        final String value = jobProperty.get(ShmConstants.VALUE);
                        neSubmitted = Integer.parseInt(value);
                    }
                    if (ShmConstants.NE_COMPLETED.equals(jobProperty.get(ShmConstants.KEY))) {
                        final String value = jobProperty.get(ShmConstants.VALUE);
                        neCompleted = Integer.parseInt(value);
                        jobProperty.put(ShmConstants.VALUE, Integer.toString(++neCompleted));
                    }
                }
                mainJobAttributes.put(ShmConstants.JOBPROPERTIES, mainJobPropertyList);
                dpsWriter.update(mainJobId, mainJobAttributes);
            } else {
                LOGGER.error("Job Properties does not exist for {} ", mainJobId);
            }
        } else {
            LOGGER.error("Main Job attributes does not exist for {} ", mainJobId);
        }
        if (neSubmitted == neCompleted) {
            neJobDone = true;
        }
        LOGGER.info("Number of NE jobs completed : {}", neCompleted);
        return neJobDone;
    }

    /**
     * This method retrieves the attributes of Job PO.
     * 
     * @param jobId
     * @return Map<String, Object>
     */
    private Map<String, Object> retrieveJob(final long jobId) {
        final PersistenceObject po = dpsReader.findPOByPoId(jobId);
        return po.getAllAttributes();
    }
}
