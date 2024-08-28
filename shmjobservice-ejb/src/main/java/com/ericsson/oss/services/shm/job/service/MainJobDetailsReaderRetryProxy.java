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
package com.ericsson.oss.services.shm.job.service;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.exception.ServerInternalException;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;
import com.ericsson.oss.services.shm.job.entities.JobInput;

public class MainJobDetailsReaderRetryProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainJobDetailsReaderRetryProxy.class);

    @Inject
    private MainJobsDetailsReader mainJobDetailsReader;

    @Inject
    private RetryManager retryManager;

    @Inject
    private DpsRetryPolicies dpsRetryPolicies;

    @Inject
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    public Map<Long, Map<String, Object>> getTemplates(final JobInput jobInput) {
        try {
            return retryManager.executeCommand(dpsRetryPolicies.getJobDetailsRetryPolicy(), new ShmDpsRetriableCommand<Map<Long, Map<String, Object>>>() {
                @Override
                protected Map<Long, Map<String, Object>> execute() {
                    return mainJobDetailsReader.getSHMMainJobTemplates(jobInput);
                }
            });
        } catch (final RetriableCommandException retriableCommandException) {
            LOGGER.error("All retries exhausted while retrieving SHM Job templates. Reason: ", retriableCommandException);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(retriableCommandException);
            throw new ServerInternalException(String.format("Unable to read SHM Job templates due to: %s", retriableCommandException.getMessage()));
        }
    }

    public Map<Long, Map<String, Object>> getMainJobs(final List<Long> eachBatchOfTemplatePoIds) {
        try {
            return retryManager.executeCommand(dpsRetryPolicies.getJobDetailsRetryPolicy(), new ShmDpsRetriableCommand<Map<Long, Map<String, Object>>>() {
                @Override
                protected Map<Long, Map<String, Object>> execute() {
                    return mainJobDetailsReader.getMainJobs(eachBatchOfTemplatePoIds);
                }
            });
        } catch (final RetriableCommandException retriableCommandException) {
            LOGGER.error("All retries exhausted while retrieving SHM Main Jobs. Reason: ", retriableCommandException);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(retriableCommandException);
            throw new ServerInternalException(String.format("Unable to read SHM Main Jobs due to: %s", retriableCommandException.getMessage()));
        }
    }

    public Map<String, Object> getMainJob(final long poId) {
        try {
            return retryManager.executeCommand(dpsRetryPolicies.getViewJobDetailsRetryPolicy(), new ShmDpsRetriableCommand<Map<String, Object>>() {
                @Override
                protected Map<String, Object> execute() {
                    return mainJobDetailsReader.retrieveJob(poId);
                }
            });
        } catch (final RetriableCommandException retriableCommandException) {
            LOGGER.error("All retries exhausted while retrieving SHM Main Job. Reason: ", retriableCommandException);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(retriableCommandException);
            throw new ServerInternalException(String.format("Unable to read SHM Main Job due to: %s", retriableCommandException.getMessage()));
        }
    }
}
