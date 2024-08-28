/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
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

import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobTemplate;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobConfigurationDetails;

/**
 * JobConfigurationSummary interface
 * 
 * @author xeswpot
 * 
 */
public interface JobConfigurationSummary {

    List<JobConfigurationDetails> getJobConfigurationDetails(final JobTemplate jobTemplate, final JobConfiguration jobConfiguration);

    List<NetworkElement> getNetworkElementsByNeNames(final List<String> neNames, final String capability);
}
