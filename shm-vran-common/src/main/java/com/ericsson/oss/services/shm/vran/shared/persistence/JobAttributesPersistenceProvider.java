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
package com.ericsson.oss.services.shm.vran.shared.persistence;

import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import com.ericsson.oss.services.shm.es.api.JobUpdateService;

@Stateless
public class JobAttributesPersistenceProvider {

    @Inject
    private JobUpdateService jobUpdateService;

    public void persistJobProperties(final long jobId, final List<Map<String, Object>> jobProperties) {
        final Double activityProgressPercentage = null;
        final List<Map<String, Object>> jobLogs = null;
        jobUpdateService.readAndUpdateRunningJobAttributes(jobId, jobProperties, jobLogs, activityProgressPercentage);
    }

    public void persistJobLogs(final long jobId, final List<Map<String, Object>> jobLogs) {
        final Double activityProgressPercentage = null;
        final List<Map<String, Object>> jobProperties = null;
        jobUpdateService.readAndUpdateRunningJobAttributes(jobId, jobProperties, jobLogs, activityProgressPercentage);
    }

    public void persistJobPropertiesAndLogs(final long jobId, final List<Map<String, Object>> jobProperties, final List<Map<String, Object>> jobLogs) {
        final Double activityProgressPercentage = null;
        jobUpdateService.readAndUpdateRunningJobAttributes(jobId, jobProperties, jobLogs, activityProgressPercentage);
    }

}
