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
package com.ericsson.oss.services.shm.es.impl.cpp.deleteup;

import java.util.ArrayList;
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
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.handlers.CancelHandler;
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.handlers.ExecuteHandler;
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.handlers.ProcessNotificationHandler;
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.handlers.TimeoutHandler;
import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.AbstractUpgradeActivity;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

/**
 * This elementary service class to delete upgrade packages on CPP nodes.
 * 
 * @author xneranu, xkareve
 * 
 */
@EServiceQualifier("CPP.DELETE_UPGRADEPACKAGE.deleteupgradepackage")
@ActivityInfo(activityName = "deleteupgradepackage", jobType = JobTypeEnum.DELETE_UPGRADEPACKAGE, platform = PlatformTypeEnum.CPP)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class DeleteUpgradePackageService extends AbstractUpgradeActivity implements Activity, ActivityCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteUpgradePackageService.class);

    @Inject
    private ExecuteHandler executeHandler;

    @Inject
    private ProcessNotificationHandler processNotificationHandler;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private TimeoutHandler timeoutHandler;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private CancelHandler cancelHandler;

    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.PRECHECK);
        } catch (final JobDataNotFoundException ex) {
            LOGGER.error("DeleteUpgradePackageService.Precheck- {} ", ex.getMessage());
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            activityUtils.addJobLog(String.format(JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, ShmConstants.DELETEUPGRADEPKG_ACTIVITY, ex.getMessage()), JobLogLevel.ERROR.toString(), jobLogList,
                    JobLogType.SYSTEM.toString());
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        }
        return activityStepResult;
    }

    @Asynchronous
    @Override
    public void execute(final long activityJobId) {
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
        executeHandler.execute(activityJobId, jobActivityInfo);

    }

    @Override
    public void processNotification(final Notification notification) {
        LOGGER.debug("Entered in deleteup Activity processNotification()");
        if (!NotificationEventTypeEnum.DELETE.equals(notification.getNotificationEventType())) {
            LOGGER.debug("Cpp deleteupgradepackage activity - Discarding non-Delete notification.");
            return;
        }
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        final long activityJobId = activityUtils.getActivityJobId(notificationSubject);
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
        processNotificationHandler.processNotification(activityJobId, jobActivityInfo, notification);

    }

    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        return timeoutHandler.handleTimeout(activityJobId);
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        return cancelHandler.cancel(activityJobId);

    }

    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        return cancelHandler.cancelTimeout(activityJobId, finalizeResult);
    }

}
