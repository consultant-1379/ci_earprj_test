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
package com.ericsson.oss.services.shm.jobs.common.retry;

import javax.ejb.EJBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetryContext;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.common.exception.WorkflowServiceInvocationException;
import com.ericsson.oss.services.wfs.api.WorkflowMessageCorrelationException;
import com.ericsson.oss.services.wfs.api.WorkflowServiceException;

/**
 * RetriableCommand implementation file, for implementing retry of WFS calls in case of connectivity failures.
 * 
 * @author xrajeke
 * 
 * @param <T>
 */
public abstract class ShmWfsRetriableCommand<T> implements RetriableCommand<T> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand#execute(com.ericsson.oss.itpf.sdk.core.retry.RetryContext)
     */
    @Override
    public T execute(final RetryContext retryContext) {
        try {
            if (retryContext.getCurrentAttempt() > 1) {
                logger.info("Retrying WFS call for ({}) times", retryContext.getCurrentAttempt());
            }
            return execute();
        } catch (final WorkflowMessageCorrelationException e) {
            throw new WorkflowServiceInvocationException(ShmCommonConstants.WORKFLOW_SERVICE_INTERNAL_ERROR, e);
        } catch (RuntimeException e) {
            logger.error("call to WFS failed in retrymanager due to {}, ", e);
            throwWFSException(e);
            throw e;
        }
    }

    /**
     * @param e
     */
    private void throwWFSException(final RuntimeException e) {
        if (e instanceof IllegalStateException || e instanceof EJBException || e instanceof WorkflowServiceException) {
            throw new WorkflowServiceInvocationException(ShmCommonConstants.WORKFLOW_SERVICE_INTERNAL_ERROR, e);
        }
    }

    /**
     * Method to be called with retry mechanism must be called in this implemented method
     * 
     * @return
     */
    protected abstract T execute() throws WorkflowMessageCorrelationException;
}
