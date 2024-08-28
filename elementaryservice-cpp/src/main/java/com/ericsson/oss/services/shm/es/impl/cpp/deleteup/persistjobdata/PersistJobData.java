/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.cpp.deleteup.persistjobdata;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;

public class PersistJobData {

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    @Inject
    private JobUpdateService jobUpdateService;

    public void persistPropertiesLogsAndProgress(final long activityJobId, final long neJobId, final List<Map<String, Object>> jobProperties, final List<Map<String, Object>> jobLogs,
            final double activityProgressPercentage) {
        activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobProperties, jobLogs, activityProgressPercentage);
        if (activityProgressPercentage != 0L) {
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
        }
        if (jobLogs != null) {
            jobLogs.clear();

        }
    }

    public void persistPropertiesLogsForCancel(final long activityJobId, final List<Map<String, Object>> jobProperties, final List<Map<String, Object>> jobLogs) {
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobProperties, jobLogs);
    }

}
