/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.ecim.licenserefresh;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.ecim.licenserefresh.notification.LkfImportProcessNotificationImpl;
import com.ericsson.oss.services.shm.es.license.refresh.api.LkfImportResponse;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

@EServiceQualifier("ECIM.LICENSE_REFRESH.request")
@ActivityInfo(activityName = "request", jobType = JobTypeEnum.LICENSE_REFRESH, platform = PlatformTypeEnum.ECIM)
@Stateless
@Profiled
@Traceable
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class RequestService implements Activity, ActivityCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestService.class);

    @Inject
    protected ActivityUtils activityUtils;

    @Inject
    protected JobLogUtil jobLogUtil;

    @Inject
    private RequestServiceProcessNotificationTask requestServiceProcessNotificationTask;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private RequestServiceExecuteTask requestServiceExecuteTask;

    @Inject
    protected RequestServiceHandleTimeoutTask requestServiceHandleTimeoutTask;

    @Override
    public ActivityStepResult precheck(long activityJobId) {

        LOGGER.debug("LicenseRefreshJob:Request activity {} - Initiating precheck", activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.PRECHECK_SUCCESS, ActivityConstants.LICENSE_REFRESH_REQUEST), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.INFO.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogs, null);
        return activityStepResult;
    }

    @Override
    @Asynchronous
    public void execute(long activityJobId) {

        LOGGER.debug("LicenseRefreshJob:Request activity {} - Initiating execute", activityJobId);
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
        requestServiceExecuteTask.execute(activityJobId, jobActivityInfo);
    }

    @Override
    public void processNotification(Notification message) {

        LOGGER.debug("LicenseRefreshJob:Request activity- Initiating processNotification");
        final LkfImportProcessNotificationImpl notification = (LkfImportProcessNotificationImpl) message;
        final LkfImportResponse lkfImportResponse = notification.getLkfImportResponse();
        final JobActivityInfo jobActivityInformation = activityUtils.getActivityInfo(lkfImportResponse.getActivityJobId(), this.getClass());
        requestServiceProcessNotificationTask.processNotification(lkfImportResponse, jobActivityInformation);
    }

    @Override
    public ActivityStepResult handleTimeout(long activityJobId) {

        LOGGER.debug("LicenseRefreshJob:Request activity {} - Initiating handleTimeout", activityJobId);
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
        return requestServiceHandleTimeoutTask.handleTimeout(activityJobId, jobActivityInfo);
    }

    @Override
    public ActivityStepResult cancel(long activityJobId) {
        return null;
    }

    @Override
    public ActivityStepResult cancelTimeout(long activityJobId, boolean finalizeResult) {
        return null;
    }

}
