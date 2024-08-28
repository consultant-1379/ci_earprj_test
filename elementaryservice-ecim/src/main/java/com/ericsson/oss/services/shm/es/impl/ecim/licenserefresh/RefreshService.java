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
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.notifications.api.Notification;

@EServiceQualifier("ECIM.LICENSE_REFRESH.refresh")
@ActivityInfo(activityName = "refresh", jobType = JobTypeEnum.LICENSE_REFRESH, platform = PlatformTypeEnum.ECIM)
@Stateless
@Profiled
@Traceable
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class RefreshService implements Activity, ActivityCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshService.class);

    @Inject
    RefreshServiceHelper refreshServiceHelper;

    @Inject
    protected ActivityUtils activityUtils;

    @Override
    public ActivityStepResult precheck(long activityJobId) {
        LOGGER.debug("LicenseRefreshJob:Refresh activity {} - Initiating precheck", activityJobId);
        return refreshServiceHelper.precheck(activityJobId);
    }

    @Override
    @Asynchronous
    public void execute(long activityJobId) {

        LOGGER.debug("LicenseRefreshJob:Refresh activity {} - Initiating execute", activityJobId);
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
        refreshServiceHelper.execute(activityJobId, jobActivityInfo);
    }

    @Override
    public ActivityStepResult handleTimeout(long activityJobId) {
        LOGGER.debug("LicenseRefreshJob:Refresh activity {} - Initiating handleTimeout", activityJobId);
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
        return refreshServiceHelper.handleTimeout(activityJobId, jobActivityInfo);
    }

    @Override
    public ActivityStepResult cancel(long activityJobId) {
        return null;
    }

    @Override
    public void processNotification(Notification notification) {
        LOGGER.debug("LicenseRefreshJob:Refresh activity with event type {} - Initiating processNotification", notification.getNotificationEventType());
        final long activityJobId = activityUtils.getActivityJobId(notification.getNotificationSubject());
        final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
        refreshServiceHelper.processNotification(notification, jobActivityInfo);
    }

    @Override
    public ActivityStepResult cancelTimeout(long activityJobId, boolean finalizeResult) {
        return null;
    }

}
