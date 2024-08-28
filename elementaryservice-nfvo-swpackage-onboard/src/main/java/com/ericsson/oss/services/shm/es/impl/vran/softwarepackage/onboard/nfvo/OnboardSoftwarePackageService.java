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
package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.nfvo;

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
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.persistence.OnboardJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.tasks.ExecuteTask;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.tasks.HandleTimeoutTask;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.tasks.ProcessNotificationTask;
import com.ericsson.oss.services.shm.es.vran.onboard.api.notifications.NfvoSoftwarePackageJobResponse;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.onboard.notification.NfvoSoftwarePackageJobNotificationWrapper;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobEvents;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

/**
 * Service to handle Software Package onboard activity for vRAN packages
 * 
 * @author xjhosye
 * 
 */
@EServiceQualifier("vRAN.ONBOARD.onboard")
@ActivityInfo(activityName = "onboard", jobType = JobTypeEnum.ONBOARD, platform = PlatformTypeEnum.vRAN)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class OnboardSoftwarePackageService implements Activity, ActivityCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnboardSoftwarePackageService.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private VranJobActivityServiceHelper vranJobActivityService;

    @Inject
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Inject
    private ExecuteTask executeTask;

    @Inject
    private ProcessNotificationTask processNotificationTask;

    @Inject
    private HandleTimeoutTask handleTimeoutTask;

    @Inject
    private OnboardJobPropertiesPersistenceProvider onboardJobPropertiesPersistenceProvider;

    /**
     * Pre check for Onboard activity of Software Package.
     * 
     * @param activityJobId
     * @return activityStepResult
     */
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
        try {

            LOGGER.info("ActivityJob ID - [{}] : precheck for software package onboard activity", activityJobId);

            jobLogs.add(vranJobActivityService.buildJobLog(String.format(JobLogConstants.ACTIVITY_INITIATED, VranJobConstants.ONBOARD), JobLogLevel.INFO.toString()));

            // Pre check will be passed by default for now.
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);

            onboardJobPropertiesPersistenceProvider.initializeOnboardActivityVariables(activityJobId);

            jobLogs.add(vranJobActivityService.buildJobLog(String.format(JobLogConstants.PRECHECK_SUCCESS, VranJobConstants.ONBOARD), JobLogLevel.INFO.toString()));

            jobAttributesPersistenceProvider.persistJobPropertiesAndLogs(activityJobId, null, jobLogs);

            LOGGER.info("ActivityJob ID - [{}] : Precheck for software package onboard activity is completed. Result : {}", activityJobId, activityStepResult.getActivityResultEnum());

        } catch (Exception e) {
            LOGGER.error("Failed to perform precheck of softwarepackage onboard activity due to : ", e);
        }
        return activityStepResult;
    }

    /**
     * Method to perform execute step
     * 
     * @param activityJobId
     * 
     */
    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        LOGGER.debug("ActivityJob ID - [{}] : Executing  onboard software package activity.", activityJobId);

        executeTask.execute(activityJobId);
    }

    /**
     * Method to process software package onboard job activity notifications.
     * 
     * @param Notification
     *            message
     * 
     */
    @Override
    public void processNotification(final Notification message) {

        final NfvoSoftwarePackageJobNotificationWrapper notification = (NfvoSoftwarePackageJobNotificationWrapper) message;
        final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse = notification.getNfvoSoftwarePackageJobNotification();
        final JobActivityInfo jobActivityInformation = activityUtils.getActivityInfo(nfvoSoftwarePackageJobResponse.getActivityJobId(), this.getClass());

        LOGGER.debug("ActivityJob ID - [{}] : Notification {} received for onboard software package activity with Result {}", nfvoSoftwarePackageJobResponse.getActivityJobId(),
                nfvoSoftwarePackageJobResponse, nfvoSoftwarePackageJobResponse.getResult());

        activityUtils.recordEvent(VranJobEvents.ONBOARD_SOFTWARE_PACKAGE_NOTIFICATION, nfvoSoftwarePackageJobResponse.getNodeAddress(), nfvoSoftwarePackageJobResponse.getVnfPackageId(),
                activityUtils.additionalInfoForEvent(nfvoSoftwarePackageJobResponse.getActivityJobId(), nfvoSoftwarePackageJobResponse.getNodeAddress(), nfvoSoftwarePackageJobResponse.toString()));

        processNotificationTask.processNotification(nfvoSoftwarePackageJobResponse, jobActivityInformation);

    }

    /**
     * Method to process timeout for onboard software package activity.
     * 
     * @param activityJobId
     */
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        LOGGER.info("ActivityJob ID - [{}] : Handling Onboard Software package activity in timeout.", activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final JobActivityInfo jobActivityInformation = activityUtils.getActivityInfo(activityJobId, this.getClass());
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        final boolean repeatExecute = handleTimeoutTask.handleTimeout(activityJobId, jobActivityInformation);
        if (repeatExecute) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.REPEAT_EXECUTE);
        }
        return activityStepResult;
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {

        return null;
    }

    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {

        return null;
    }

}
