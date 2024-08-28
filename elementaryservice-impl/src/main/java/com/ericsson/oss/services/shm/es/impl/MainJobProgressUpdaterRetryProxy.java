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

import java.util.Map;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;

/**
 * Updates the Main Job attributes in a retry block
 * 
 * @author xrajeke
 * 
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MainJobProgressUpdaterRetryProxy {

    final static private Logger logger = LoggerFactory.getLogger(MainJobProgressUpdaterRetryProxy.class);

    @Inject
    private MainJobProgressUpdater mainJobProgressUpdater;

    @Inject
    private RetryManager retryManager;

    @Inject
    private DpsRetryPolicies dpsRetryPolicies;

    /**
     * To update the Main Job Progress Percentage asynchronously in retry block.
     * 
     * @param mainJobPoId
     */
    @Asynchronous
    public void updateMainJobProgress(final long mainJobPoId) {
        try {
            retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Void>() {
                @Override
                public Void execute() {
                    mainJobProgressUpdater.updateMainJobProgress(mainJobPoId);
                    return null;
                }
            });
        } catch (final RetriableCommandException retriableCommandException) {
            logger.error("All retries exhuasted while attempting to update main job progress for MainJob with PO Id : {}. Exception Occurred : {}", mainJobPoId, retriableCommandException);
        }

    }

    /**
     * Updates the Main job with required attributes to fulfill the completion criteria (like ENDTIME,STATE,RESULT,PROGRESSPERCENTAGE) within the retry block
     * 
     * @param mainJobPoId
     * @param jobResult
     * @param jobEndTime
     */
    public void updateMainJobEndDetails(final long mainJobPoId, final Map<String, Object> mainJobAttributes) {
        retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Void>() {
            @Override
            public Void execute() {
                mainJobProgressUpdater.updateMainJobEndDetails(mainJobPoId, mainJobAttributes);
                return null;
            }
        });

    }
}
