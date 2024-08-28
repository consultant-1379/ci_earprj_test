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
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.UpgradePackageContext;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks.ProcessNotificationTask;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks.TaskBase;
import com.ericsson.oss.services.shm.es.vran.notifications.api.VranSoftwareUpgradeJobResponse;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobLogMessageTemplate;
import com.ericsson.oss.services.shm.vran.constants.VranUprgradeConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

public class DeleteActivityNotificationProcessor extends ProcessNotificationTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteActivityNotificationProcessor.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private TaskBase taskBase;

    @Inject
    private VranJobActivityServiceHelper vranJobActivityServiceHelper;

    @Inject
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Override
    public void processNotification(final JobActivityInfo jobActivityInformation, final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse,
            final UpgradePackageContext upgradePackageContext) {
        final long activityJobId = vranSoftwareUpgradeJobResponse.getActivityJobId();
        LOGGER.trace("ActivityJob ID - [{}] : Processing delete job notification {}.", activityJobId, vranSoftwareUpgradeJobResponse);
        processDeleteJobNotification(vranSoftwareUpgradeJobResponse, activityJobId, upgradePackageContext, jobActivityInformation, jobActivityInformation.getActivityName());

    }

    private void processDeleteJobNotification(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse, final long activityJobId, final UpgradePackageContext upgradePackageContext,
            final JobActivityInfo jobActivityInfo, final String deleteInitiatedFrom) {
        final List<Map<String, Object>> activityJobProperties = new ArrayList<>();

        taskBase.unSubscribeNotification(jobActivityInfo, upgradePackageContext, activityJobId);

        proceedWithNextStep(vranSoftwareUpgradeJobResponse, upgradePackageContext, jobActivityInfo, deleteInitiatedFrom, activityJobProperties);

        persistJobLogs(vranSoftwareUpgradeJobResponse, activityJobProperties, upgradePackageContext);

        LOGGER.debug("ActivityJob ID - [{}] : Updated DELETE action details successfully in jobProperties {}", activityJobId, activityJobProperties);
    }

    private void persistJobLogs(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse, final List<Map<String, Object>> activityJobProperties,
            final UpgradePackageContext upgradePackageContext) {
        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final String additionalInfo = vranSoftwareUpgradeJobResponse.getAdditionalInfo();
        if (additionalInfoExists(additionalInfo)) {
            jobLogs.add(vranJobActivityServiceHelper.buildJobLog(String.format(" \"%s\" on VNFM: \"%s\" ", additionalInfo, upgradePackageContext.getVnfmName()),
                    vranSoftwareUpgradeJobResponse.getNotificationReceivedTime(), JobLogLevel.INFO.toString()));
        } else {
            jobLogs.add(vranJobActivityServiceHelper.buildJobLog(String.format(VranJobLogMessageTemplate.FOUND_NO_UPGRADE_JOBS, upgradePackageContext.getVnfmName()),
                    vranSoftwareUpgradeJobResponse.getNotificationReceivedTime(), JobLogLevel.INFO.toString()));
        }
        jobAttributesPersistenceProvider.persistJobPropertiesAndLogs(vranSoftwareUpgradeJobResponse.getActivityJobId(), activityJobProperties, jobLogs);
    }

    private boolean additionalInfoExists(final String additionalInfo) {
        return additionalInfo != null && !additionalInfo.isEmpty();
    }

    private void proceedWithNextStep(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse, final UpgradePackageContext vranUpgradeInformation, final JobActivityInfo jobActivityInfo,
            final String deleteInitiatedFrom, final List<Map<String, Object>> activityJobProperties) {
        final Map<String, Object> processVariables = new HashMap<>();
        if (deleteIniatiedFromConfirm(deleteInitiatedFrom)) {
            activityUtils.prepareJobPropertyList(activityJobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.toString());
            activityUtils.sendNotificationToWFS(vranUpgradeInformation.getJobEnvironment(), jobActivityInfo.getActivityJobId(), ActivityConstants.CONFIRM, processVariables);
        } else {
            performCreateActivity(vranSoftwareUpgradeJobResponse, vranUpgradeInformation, jobActivityInfo);
        }
    }

    private boolean deleteIniatiedFromConfirm(final String deleteInitiatedFrom) {
        return deleteInitiatedFrom.equalsIgnoreCase(ActivityConstants.CONFIRM);
    }

    private void performCreateActivity(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse, final UpgradePackageContext vranUpgradeInformation, final JobActivityInfo jobActivityInfo) {
        final int jobId = -1;
        // Last vnfJobId is deleted. To trigger create activity we are setting jobId to -1.
        vranUpgradeInformation.setVnfJobId(jobId);
        taskBase.performSoftwareUpgrade(vranSoftwareUpgradeJobResponse.getActivityJobId(), vranUpgradeInformation, ActivityConstants.CREATE, jobActivityInfo, VranUprgradeConstants.DELETE_ACTIVITY);
    }

    @Override
    public String getActivityName() {
        return VranUprgradeConstants.DELETE_ACTIVITY;
    }
}
