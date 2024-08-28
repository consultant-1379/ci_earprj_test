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
package com.ericsson.oss.services.shm.es.impl.ecim.deleteup;

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
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.notifications.api.Notification;

/**
 * This elementary service class to delete upgrade package on ECIM nodes.
 * 
 * @author xkalkil
 * 
 */

@EServiceQualifier("ECIM.DELETE_UPGRADEPACKAGE.deleteupgradepackage")
@ActivityInfo(activityName = "deleteupgradepackage", jobType = JobTypeEnum.DELETE_UPGRADEPACKAGE, platform = PlatformTypeEnum.ECIM)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class DeleteUpgradePackageService implements Activity, ActivityCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteUpgradePackageService.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private ExecuteHandler executeHandler;

    @Inject
    private ProcessNotificationHandler processNotificationHandler;

    @Inject
    private TimeoutHandler handleTimeoutHandler;

    @Inject
    private CancelHandler cancelHandler;

    /**
     * This method validates given UP MO is existed on the node and is not an active then only deleteupgradepackage activity can be started or not and sends back the activity result to Work Flow
     * Service.
     * 
     * @param activityJobId
     * @return ActivityStepResult
     * 
     */
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        LOGGER.debug("Inside DeleteUpgradePackageService precheck() with activityJobId {}", activityJobId);

        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
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
        LOGGER.debug("Entered ECIM -delete upgradepackage - processNotification with event type : {}", notification.getNotificationEventType());
        processNotificationHandler.processNotification(notification);

    }

    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        return handleTimeoutHandler.handleTimeout(activityJobId);
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        LOGGER.debug("Inside Ecim DeleteUpgradePakageService.cancel() with activityJobId:{}", activityJobId);
        return cancelHandler.cancel(activityJobId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancelTimeout(long, boolean)
     */
    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        LOGGER.debug("Inside Ecim DeleteUpgradePakageService.cancel() with activityJobId:{}", activityJobId);
        return cancelHandler.cancelTimeout(activityJobId, finalizeResult);
    }

}
