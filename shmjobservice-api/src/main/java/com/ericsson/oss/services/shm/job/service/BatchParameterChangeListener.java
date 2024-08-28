/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.service;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;

/**
 * This class listens for the batch parameter value change for Job query.
 * 
 * @author zkurswa
 * 
 */
public class BatchParameterChangeListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchParameterChangeListener.class);
    private static final String JOB_DETAILS_QUERY_BATCH_SIZE = "job_details_query_max_batch_size";

    @Inject
    @Configured(propertyName = JOB_DETAILS_QUERY_BATCH_SIZE)
    private int jobDetailsQueryBatchSize;

    /**
     * Listener for jobDetailsQueryBatchSize attribute value
     * 
     * @param jobDetailsQueryBatchSize
     */
    void listenForJobDetailsQueryBatchAttribute(@Observes @ConfigurationChangeNotification(propertyName = JOB_DETAILS_QUERY_BATCH_SIZE) final int jobDetailsQueryBatchSize) {
        this.jobDetailsQueryBatchSize = jobDetailsQueryBatchSize;
        LOGGER.info("JobDetailsQueryBatchSize Parameter value : {}", jobDetailsQueryBatchSize);
    }

    public int getJobDetailsQueryBatchSize() {
        return jobDetailsQueryBatchSize;
    }
}
