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
package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.softwareupgrade.tasks;

import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.INVENTORY_SUPERVISION_DISABLED;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.UNAUTHORIZED_USER;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.DPSUtils;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

public class PrecheckTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrecheckTask.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculator;

    @Inject
    private DPSUtils dpsUtils;

    private static final Double PERCENT_ZERO = 0.0;

    public ActivityStepResult activityPreCheck(final long activityJobId, final String activityName) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();

        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
            final String nodeName = jobEnvironment.getNodeName();
            jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.ACTIVITY_INITIATED, activityName), JobLogLevel.INFO.getLogLevel()));
            jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.PROCESSING_PRECHECK, activityName), JobLogLevel.INFO.getLogLevel()));
            activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PERCENT_ZERO);
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, activityName);
            if (!isUserAuthorized) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
                jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.PRE_CHECK_FAILURE, activityName, UNAUTHORIZED_USER),
                        JobLogLevel.ERROR.getLogLevel()));
                jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.ACTIVITY_FAILED, activityName),
                        JobLogLevel.INFO.getLogLevel()));
            } else if (!dpsUtils.isInventorySupervisionEnabled(nodeName)) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
                jobLogList.add(activityUtils.createNewLogEntry(
                        String.format(JobLogConstants.PRE_CHECK_FAILURE, activityName, INVENTORY_SUPERVISION_DISABLED),
                        JobLogLevel.ERROR.getLogLevel()));
                jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.ACTIVITY_FAILED, activityName),
                        JobLogLevel.INFO.getLogLevel()));
            } else {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
                jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.PRE_CHECK_SUCCESS, activityName),
                        JobLogLevel.INFO.getLogLevel()));
            }
        } catch (Exception exception) {
            LOGGER.error("Exception occured in precheck() for activityJobId:{}. Reason: ", activityJobId, exception);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
            final String errorMessage = String.format(JobLogConstants.FAILURE_REASON, exception.getMessage());
            jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.PRE_CHECK_FAILURE, activityName, errorMessage), JobLogLevel.ERROR.getLogLevel()));
            jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.ACTIVITY_FAILED, activityName), JobLogLevel.ERROR.getLogLevel()));
        }
        activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PERCENT_ZERO);
        LOGGER.info("ActivityJob ID - [{}] : Precheck of {} activity is completed. Result : {}", activityJobId, activityName, activityStepResult.getActivityResultEnum());
        return activityStepResult;
    }

}
