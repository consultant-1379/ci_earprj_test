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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.services.shm.common.enums.JobStateEnum;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;
import com.ericsson.oss.services.shm.job.service.SHMJobService;

/**
 * To Update all the Main Job's progress synchronously
 * 
 * @author xrajeke
 * 
 */
public class MainJobsProgressUpdateService {

    final static private Logger LOGGER = LoggerFactory.getLogger(MainJobsProgressUpdateService.class);

    @Inject
    private SHMJobService shmJobService;

    @Inject
    private RetryManager retryManager;

    @Inject
    private DpsRetryPolicies dpsRetryPolicies;

    @Inject
    private MainJobProgressUpdater mainJobProgressUpdater;

    /**
     * Will be invoked for each defined time interval by a Timer service.
     * <p>
     * For every invocation finds all the running jobs and updates the progress percentage asynchronously for all Main jobs.
     */
    public void invokeMainJobsProgressUpdate() {
        List<Long> mainJobIds = new ArrayList<Long>();
        //Find and get all the Main Job PO ids, which are currently in RUNNING or CANCELLING mode with retry mechanism
        try {
            mainJobIds = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<List<Long>>() {
                @Override
                public List<Long> execute() {
                    return shmJobService.getMainJobIds(JobStateEnum.RUNNING.name(), JobStateEnum.CANCELLING.name());
                }
            });
        } catch (final RetriableCommandException retriableCommandException) {
            LOGGER.error("All retries exhausted while retrieving Main Jobs which are in RUNNING/CANCELLING state. Exception Occurred : {}", retriableCommandException);
        }
        LOGGER.trace("Currently there are {} running or cancelling main job(s) in the system.", mainJobIds.size());

        //Update the progress percentage to all the running/cancelling jobs in parallel/asynchronously. 
        for (final Long mainJobPoId : mainJobIds) {
            mainJobProgressUpdater.updateMainJobProgress(mainJobPoId);
        }
    }
}
