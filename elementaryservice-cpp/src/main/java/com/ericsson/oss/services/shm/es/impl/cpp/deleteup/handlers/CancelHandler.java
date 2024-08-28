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
package com.ericsson.oss.services.shm.es.impl.cpp.deleteup.handlers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.persistjobdata.PersistJobData;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

@RequestScoped
@Traceable
public class CancelHandler {

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private PersistJobData persistJobData;

    final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
    final List<Map<String, Object>> jobProperties = new ArrayList<Map<String, Object>>();

    public ActivityStepResult cancel(final long activityJobId) {
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        activityUtils.logCancelledByUser(jobLogs, jobEnvironment, ActivityConstants.DELETE_UP_DISPLAY_NAME);
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.CANCEL_NOT_POSSIBLE, ActivityConstants.DELETE_UP_DISPLAY_NAME), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.WARN.toString());
        activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.IS_CANCEL_TRIGGERED, "true");
        persistJobData.persistPropertiesLogsForCancel(activityJobId, jobProperties, jobLogs);
        return new ActivityStepResult();
    }

    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.CANCEL_NOT_POSSIBLE, ActivityConstants.DELETE_UP_DISPLAY_NAME), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.WARN.toString());
        persistJobData.persistPropertiesLogsForCancel(activityJobId, jobProperties, jobLogs);
        return activityStepResult;
    }

}
