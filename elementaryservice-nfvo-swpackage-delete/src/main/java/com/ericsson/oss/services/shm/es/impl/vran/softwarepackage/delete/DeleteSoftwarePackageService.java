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
package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete;

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
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextBuilder;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextForEnm;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextForNfvo;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.persistence.DeleteJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.tasks.ExecuteTask;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.tasks.HandleTimeoutTask;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.tasks.ProcessNotificationTask;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.tasks.TasksBase;
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
 * This class performs Delete Software package activity for VRAN packages
 * 
 * @author xjhosye
 * 
 */
@EServiceQualifier("vRAN.DELETE_SOFTWAREPACKAGE.delete_softwarepackage")
@ActivityInfo(activityName = VranJobConstants.DEL_SW_ACTIVITY, jobType = JobTypeEnum.DELETE_SOFTWAREPACKAGE, platform = PlatformTypeEnum.vRAN)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class DeleteSoftwarePackageService implements Activity, ActivityCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteSoftwarePackageService.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private DeleteJobPropertiesPersistenceProvider deleteJobPropertiesPersistenceProvider;

    @Inject
    private VranJobActivityServiceHelper vranJobActivityService;

    @Inject
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Inject
    private ProcessNotificationTask processNotificationTask;

    @Inject
    private HandleTimeoutTask handleTimeoutTask;

    @Inject
    private ExecuteTask executeTask;

    @Inject
    private TasksBase tasksBase;

    @Inject
    private DeletePackageContextBuilder deletePackageContextBuilder;

    /**
     * Precheck for delete software package activity for VRAN package
     *
     * @param activityJobId
     * @return activityStepResult
     */
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        try {
            final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
            LOGGER.info("ActivityJob ID - [{}] : processing precheck of delete software package activity ", activityJobId);
            jobLogs.add(vranJobActivityService.buildJobLog(String.format(JobLogConstants.ACTIVITY_INITIATED, VranJobConstants.DEL_SW_ACTIVITY), JobLogLevel.INFO.toString()));

            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
            deleteJobPropertiesPersistenceProvider.initializeActivityVariables(activityJobId);

            jobLogs.add(vranJobActivityService.buildJobLog(String.format(JobLogConstants.PRECHECK_SUCCESS, VranJobConstants.DEL_SW_ACTIVITY), JobLogLevel.INFO.toString()));

            jobAttributesPersistenceProvider.persistJobPropertiesAndLogs(activityJobId, null, jobLogs);
            LOGGER.info("ActivityJob ID - [{}] : Precheck of delete software package activity is completed. Result : {}", activityJobId, activityStepResult.getActivityResultEnum());

        } catch (final Exception exception) {
            LOGGER.error("Failed to perform precheck of delete software package activity. Reason : {}", exception.getMessage(), exception);
        }
        return activityStepResult;
    }

    /**
     * Method for executing delete software package activity
     *
     * @param activityJobId
     *
     */
    @Override
    @Asynchronous
    public void execute(final long activityJobId) {

        LOGGER.debug("ActivityJob ID - [{}] : Executing  delete software package activity.", activityJobId);

        final JobEnvironment jobContext = activityUtils.getJobEnvironment(activityJobId);

        final DeletePackageContextForEnm deletePackageContextForEnm = deletePackageContextBuilder.buildDeletePackageContextForEnm(jobContext);
        final DeletePackageContextForNfvo deletePackageContextForNfvo = deletePackageContextBuilder.buildDeletePackageContextForNfvo(jobContext);

        if (!deletePackageContextForEnm.isComplete()) {
            executeTask.deleteSoftwarePackageFromEnm(activityJobId, deletePackageContextForEnm);
            //At this stage there are three possibilities
            //1. Delete another selected package from Enm
            //2. Proceed to delete to software packages from Nfvo.
            //3. Complete the delete package activity.
            tasksBase.proceedWithNextStep(activityJobId);
        } else if (!deletePackageContextForNfvo.isComplete()) {
            executeTask.deleteSoftwarePackageFromNfvo(activityJobId, deletePackageContextForNfvo);
        }

    }

    /**
     * Method to process the notifications for delete software package activity.
     *
     * @param message
     *
     */
    @Override
    public void processNotification(final Notification message) {

        final NfvoSoftwarePackageJobNotificationWrapper notificationWrapper = (NfvoSoftwarePackageJobNotificationWrapper) message;
        final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse = notificationWrapper.getNfvoSoftwarePackageJobNotification();
        final JobActivityInfo jobActivityInformation = activityUtils.getActivityInfo(nfvoSoftwarePackageJobResponse.getActivityJobId(), this.getClass());

        LOGGER.info("ActivityJob ID - [{}] : Notification {} received for delete software package activity with Result {}", nfvoSoftwarePackageJobResponse.getActivityJobId(),
                nfvoSoftwarePackageJobResponse, nfvoSoftwarePackageJobResponse.getResult());

        activityUtils.recordEvent(VranJobEvents.DELETE_SOFTWARE_PACKAGE_NOTIFICATION, nfvoSoftwarePackageJobResponse.getNodeAddress(), nfvoSoftwarePackageJobResponse.getVnfPackageId(),
                activityUtils.additionalInfoForEvent(nfvoSoftwarePackageJobResponse.getActivityJobId(), nfvoSoftwarePackageJobResponse.getNodeAddress(), nfvoSoftwarePackageJobResponse.toString()));

        processNotificationTask.processNotification(nfvoSoftwarePackageJobResponse, jobActivityInformation);
    }

    /**
     * Method to to handle delete software package activity in timeout scenario.
     *
     * @param activityJobId
     * @return activityStepResult
     *
     */
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {

        LOGGER.info("ActivityJob ID - [{}] : Handling Delete Software package activity in timeout.", activityJobId);
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
