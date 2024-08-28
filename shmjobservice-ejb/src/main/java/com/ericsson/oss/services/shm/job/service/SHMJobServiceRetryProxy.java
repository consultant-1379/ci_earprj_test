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
package com.ericsson.oss.services.shm.job.service;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;
import com.ericsson.oss.services.shm.jobs.common.api.ActivityJobDetail;

public class SHMJobServiceRetryProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(SHMJobServiceRetryProxy.class);

    @Inject
    private RetryManager retryManager;

    @Inject
    private DpsRetryPolicies dpsRetryPolicies;

    @Inject
    private SHMJobServiceImplHelper shmJobServiceImplHelper;

    public Map<Long, List<ActivityJobDetail>> getActivityJobAttributesIncludingLastLogMessage(final List<Long> eachBatchOfNeJobIds) {
        try {
            final Map<Long, List<ActivityJobDetail>> activityJobs = retryManager.executeCommand(dpsRetryPolicies.getViewJobDetailsRetryPolicy(),
                    new ShmDpsRetriableCommand<Map<Long, List<ActivityJobDetail>>>() {

                        @Override
                        public Map<Long, List<ActivityJobDetail>> execute() {
                            return shmJobServiceImplHelper.getActivityJobAttributesIncludingLastLogMessage(eachBatchOfNeJobIds);
                        }
                    });
            return activityJobs;
        } catch (final RetriableCommandException ex) {
            LOGGER.error("RetriableCommandException. Reason: ", ex);
            throw ex;
        }
    }

    public Map<Long, List<Map<String, Object>>> getActivityJobPoAttributes(final QueryExecutor queryExecutor, final QueryBuilder queryBuilder, final List<Long> eachBatchOfNeJobIds) {
        try {
            final Map<Long, List<Map<String, Object>>> activityJobs = retryManager.executeCommand(dpsRetryPolicies.getViewJobDetailsRetryPolicy(),
                    new ShmDpsRetriableCommand<Map<Long, List<Map<String, Object>>>>() {

                        @Override
                        public Map<Long, List<Map<String, Object>>> execute() {
                            return shmJobServiceImplHelper.getActivityJobPoAttributes(queryExecutor, queryBuilder, eachBatchOfNeJobIds);
                        }
                    });
            return activityJobs;
        } catch (final RetriableCommandException ex) {
            LOGGER.error("RetriableCommandException. Reason: ", ex);
            throw ex;
        }
    }

}
