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
package com.ericsson.oss.services.shm.es.impl.ecim.restore;

import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRolledbackException;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.retry.DpsRetryConfigurationParamProvider;

@ApplicationScoped
@Traceable
public class RestoreJobHandlerRetryProxy {

    private final static Logger logger = LoggerFactory.getLogger(RestoreJobHandlerRetryProxy.class);

    private static final int NUMBER_OF_DEFAULT_DPS_RETRIES = 5;

    @Inject
    private DpsRetryConfigurationParamProvider dpsConfigurationParamProvider;

    @Inject
    private RestoreJobHandler restoreJobHandler;

    @Inject
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    public boolean determineActivityCompletionAndUpdateCurrentProperty(final long activityJobId, final String nodeName, final String propertyToBeUpdated) {
        boolean isCurrentPropertyPersisted = false;
        boolean isActivityCompleted = false;
        final int noOfRetries = getNoOfRetries();
        for (int i = 1; i <= noOfRetries; i++) {
            try {
                logger.debug("Determining Activity Completion for job {} having activityJobId {} : trial {} ", nodeName, activityJobId, i);
                isActivityCompleted = restoreJobHandler.determineActivityCompletionAndUpdateCurrentProperty(activityJobId, nodeName, propertyToBeUpdated);
                isCurrentPropertyPersisted = true;
                break;
            } catch (final Exception ex) {
                if (i == noOfRetries) {
                    logger.error("Determining Activity Completion and Update Current Property of node {} and jobId : {} with property {} in {} retry attempt is failed because :", nodeName, activityJobId, propertyToBeUpdated,
                            i, ex);
                } else {
                    logger.error("Determining Activity Completion and Update Current Property of node {} and jobId : {} with property {} in {} retry attempt is failed because :", nodeName, activityJobId, propertyToBeUpdated, 
                            i, ex.getMessage());
                }
                sleep(ex);
            }
        }
        if (!isCurrentPropertyPersisted) {
            logger.error("Retries exhausted for Determining Activity Completion for JobId={} , nodeName {} and property {}", activityJobId, nodeName, propertyToBeUpdated);
        }
        return isActivityCompleted;
    }

    private int getNoOfRetries() {
        final int configuredRetries = dpsConfigurationParamProvider.getdpsRetryCount();
        return configuredRetries > NUMBER_OF_DEFAULT_DPS_RETRIES ? configuredRetries : NUMBER_OF_DEFAULT_DPS_RETRIES;
    }

    private void sleep(final Exception exception) {
        try {
            if (exception instanceof EJBException && dpsAvailabilityInfoProvider.isDatabaseDown()) {
                logger.warn("Database is down, waiting to retry");
                Thread.sleep(dpsConfigurationParamProvider.getdpsWaitIntervalInMS());
            } else if (exception instanceof EJBTransactionRolledbackException) {
                logger.warn("Optimistic Lock issue occurred, waiting to retry");
                Thread.sleep(dpsConfigurationParamProvider.getDpsOptimisticLockWaitIntervalInMS());
            }
        } catch (final InterruptedException ie) {
            logger.error("Job updation is failed, because:", ie);
            Thread.currentThread().interrupt();
        }
    }
}
