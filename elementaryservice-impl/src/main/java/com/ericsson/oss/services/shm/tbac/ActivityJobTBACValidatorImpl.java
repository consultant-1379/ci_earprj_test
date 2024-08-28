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
package com.ericsson.oss.services.shm.tbac;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.tbac.api.SHMTBACHandler;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

@Traceable
@Profiled
public class ActivityJobTBACValidatorImpl implements ActivityJobTBACValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityJobTBACValidatorImpl.class);

    @Inject
    private ActivityJobTBACHelper activityJobTBACHelper;

    @Inject
    private SHMTBACHandler shmTbacHandler;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private JobLogUtil jobLogUtil;

    @Override
    public boolean validateTBAC(final long activityJobId, final NEJobStaticData neJobStaticData, final JobStaticData jobStaticData, final String activityName)
            throws JobDataNotFoundException, MoNotFoundException {
        final String jobExecutionUser = getExecutionUser(neJobStaticData, jobStaticData, activityName);
        boolean isUserAuthorized;
        if (neJobStaticData.getParentNodeName() != null) {
            isUserAuthorized = shmTbacHandler.isAuthorized(jobExecutionUser, neJobStaticData.getParentNodeName());
        } else {
            isUserAuthorized = shmTbacHandler.isAuthorized(jobExecutionUser, neJobStaticData.getNodeName());
        }
        LOGGER.debug("The authorization for user {} is : {}", jobExecutionUser, isUserAuthorized);

        if (!isUserAuthorized) {
            handleActivityFailure(activityJobId, jobExecutionUser);
        }
        return isUserAuthorized;

    }

    /**
     * @param neJobStaticData
     * @param jobStaticData
     * @param activityName
     * @return
     * @throws MoNotFoundException
     * @throws JobDataNotFoundException
     *             This method is used to fetch the job execution user. If the Execution Mode is Manual then the job execution user will be fetched from the database.
     */
    private String getExecutionUser(final NEJobStaticData neJobStaticData, final JobStaticData jobStaticData, final String activityName) throws MoNotFoundException, JobDataNotFoundException {
        String jobExecutionUser;
        if (ExecMode.MANUAL.getMode().equals(jobStaticData.getExecutionMode())) {
            jobExecutionUser = activityJobTBACHelper.getJobExecutionUserFromMainJob(neJobStaticData.getMainJobId());
            if (ExecMode.MANUAL.getMode().equals(activityJobTBACHelper.getActivityExecutionMode(neJobStaticData, jobStaticData, activityName))) {
                //get the shmJobExecUser from the NEJOB PO
                jobExecutionUser = activityJobTBACHelper.getJobExecutionUserFromNeJob(neJobStaticData.getNeJobId());
            }
        } else {
            if (ExecMode.MANUAL.getMode().equals(activityJobTBACHelper.getActivityExecutionMode(neJobStaticData, jobStaticData, activityName))) {
                //get the shmJobExecUser from the NEJOB PO
                jobExecutionUser = activityJobTBACHelper.getJobExecutionUserFromNeJob(neJobStaticData.getNeJobId());
                return jobExecutionUser;
            }
            jobExecutionUser = jobStaticData.getOwner();
        }
        return jobExecutionUser;
    }

    /**
     * @param activityJobId
     * @param jobExecutionUser
     *            This method is used to log the TBAC failure message to job logs.
     */
    private void handleActivityFailure(final long activityJobId, final String jobExecutionUser) {

        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final String message = String.format(JobLogConstants.TBAC_ACCESS_DENIED_AT_ACTIVITY_LEVEL, jobExecutionUser);
        LOGGER.debug(message);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, message, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
    }
}
