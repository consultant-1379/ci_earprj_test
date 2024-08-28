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
package com.ericsson.oss.services.shm.jobexecutor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.Activity;

/**
 * Retry Proxy class for retrying create/update on Job PersistenceObjects.
 * 
 * @author tcsgusw
 * 
 */
public class JobBuilderRetryProxy {

    @Inject
    private DpsRetryPolicies dpsRetryPolicies;

    @Inject
    private RetryManager retryManager;

    @Inject
    private JobBuilder jobBuilder;

    private static final Logger LOGGER = LoggerFactory.getLogger(JobExecutionService.class);

    public Map<String, Object> createNeJob(final long mainJobId, final String businessKey, final NetworkElement selectedNE, final Map<String, String> neDetailsWithParentName,
            final Map<String, Object> supportedAndUnsupported) {

        try {
            return retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Map<String, Object>>() {
                @Override
                public Map<String, Object> execute() {
                    return jobBuilder.createNeJob(mainJobId, businessKey, selectedNE, neDetailsWithParentName, supportedAndUnsupported);
                }
            });
        } catch (final RetriableCommandException ex) {
            LOGGER.error("Unable to create neJobPO for node: {}. Due to: {}", selectedNE, ex);
            final Map<String, Object> neJobDetails = new HashMap<>();
            neJobDetails.put(ShmConstants.JOB_STATUS, ShmConstants.CREATION_FAILED);
            neJobDetails.put(ShmConstants.CREATION_FAILURE_CAUSE, String.format(ShmConstants.CREATION_FAILED_MSG, ex.getMessage()));
            neJobDetails.put(ShmConstants.NE_JOB_ID, 0L);
            return neJobDetails;
        }
    }

    public List<Map<String, Object>> createActivityJobs(final long neJobId, final List<Activity> activities, final Map<String, String> neDetailsWithParentName, final String selectedNe) {
        try {
            return retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<List<Map<String, Object>>>() {
                @Override
                public List<Map<String, Object>> execute() {
                    return jobBuilder.createActivityJobs(neJobId, activities, neDetailsWithParentName, selectedNe);
                }
            });
        } catch (final RetriableCommandException ex) {
            LOGGER.error("Unable to create ActivityJob PO for node: {}. Due to:", selectedNe, ex);
            return new ArrayList<>();
        }
    }

    public Map<String, Object> createNEJobWithCompletedState(final long mainJobId, final NetworkElement selectedNE, final String jobResult, final String logMessage, final String businessKey) {
        try {
            return retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Map<String, Object>>() {
                @Override
                public Map<String, Object> execute() {
                    return jobBuilder.createAndEndNeJob(mainJobId, selectedNE, jobResult, logMessage, businessKey);
                }
            });
        } catch (final RetriableCommandException ex) {
            LOGGER.error("Exception occurred while creating NEJob with failed state on un-supported node: {}. Exception is: ", selectedNE.getName(), ex);
            final Map<String, Object> neJobDetails = new HashMap<>();
            neJobDetails.put(ShmConstants.JOB_STATUS, ShmConstants.CREATION_FAILED);
            neJobDetails.put(ShmConstants.CREATION_FAILURE_CAUSE, String.format(ShmConstants.CREATION_FAILED_MSG, ex.getMessage()));
            neJobDetails.put(ShmConstants.NE_JOB_ID, 0L);
            return neJobDetails;
        }
    }
}
