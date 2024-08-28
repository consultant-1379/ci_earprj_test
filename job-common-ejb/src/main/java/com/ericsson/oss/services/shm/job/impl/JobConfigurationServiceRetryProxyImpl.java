/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.impl;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;

/**
 * This class provides methods to add retries for DPS calls.
 * 
 * @author tcsgusw
 * 
 */
public class JobConfigurationServiceRetryProxyImpl implements JobConfigurationServiceRetryProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobConfigurationServiceRetryProxyImpl.class);

    @Inject
    private RetryManager retryManager;

    @Inject
    private DpsRetryPolicies dpsRetryPolicies;

    @Inject
    private JobConfigurationService jobConfigurationService;

    @Override
    public Map<String, Object> getActivityJobAttributes(final long activityJobId) {

        final Map<String, Object> poAttributes = getPOAttributes(activityJobId);

        return poAttributes;
    }

    @Override
    public Map<String, Object> getNeJobAttributes(final long neJobId) {

        final Map<String, Object> poAttributes = getPOAttributes(neJobId);

        return poAttributes;
    }

    @Override
    public Map<String, Object> getMainJobAttributes(final long mainJobId) {

        final Map<String, Object> poAttributes = getPOAttributes(mainJobId);

        return poAttributes;
    }

    @Override
    public Map<String, Object> getPOAttributes(final long poId) {
        LOGGER.debug("Inside JobConfigurationReaderRetryProxy getPOAttributes with poId {}", poId);

        final Map<String, Object> poAttributes = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Map<String, Object>>() {
            @Override
            public Map<String, Object> execute() {
                final Map<String, Object> poAttributes = jobConfigurationService.retrieveJob(poId);
                LOGGER.debug("PO Attributes: {} for the PO Id: {}", poAttributes, poId);
                return poAttributes;
            }
        });
        return poAttributes;
    }

    public List<Map<String, Object>> getProjectedAttributes(final String namespace, final String type, final Map<Object, Object> restrictions, final List<String> reqdAttributes) {

        final List<Map<String, Object>> projectedAttributes = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<List<Map<String, Object>>>() {
            @Override
            public List<Map<String, Object>> execute() {
                return jobConfigurationService.getProjectedAttributes(namespace, type, restrictions, reqdAttributes);
            }
        });
        return projectedAttributes;
    }

    public List<Long> getJobPoIdsFromParentJobId(final long neJobPoId, final String typeOfJob, final String restrictionAttribute) {
        return retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<List<Long>>() {
            @Override
            public List<Long> execute() {
                return jobConfigurationService.getJobPoIdsFromParentJobId(neJobPoId, typeOfJob, restrictionAttribute);
            }
        });

    }

    @Override
    public List<Map<String, Object>> getActivityJobAttributesByNeJobId(final long neJobId, final Map<String, Object> restrictions) {
        return retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<List<Map<String, Object>>>() {
            @Override
            public List<Map<String, Object>> execute() {
                return jobConfigurationService.getActivityAttributesByNeJobId(neJobId, restrictions);
            }
        });

    }

}
