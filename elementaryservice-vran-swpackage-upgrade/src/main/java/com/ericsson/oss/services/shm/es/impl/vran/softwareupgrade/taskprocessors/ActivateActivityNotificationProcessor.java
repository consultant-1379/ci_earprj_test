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
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.UpgradePackageContext;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider.JobLogsPersistenceProvider;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks.ProcessNotificationFailureHandler;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks.ProcessNotificationTask;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks.TaskBase;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskutils.NotificationTaskUtils;
import com.ericsson.oss.services.shm.es.vran.notifications.api.VranSoftwareUpgradeJobResponse;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobEvents;
import com.ericsson.oss.services.shm.vran.constants.VranUprgradeConstants;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

public class ActivateActivityNotificationProcessor extends ProcessNotificationTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActivateActivityNotificationProcessor.class);

    @Inject
    private JobCancelActivityNotificationProcessor jobCancelActivityNotificationProcessor;

    @Inject
    private ProcessNotificationFailureHandler processNotificationFailureHandler;

    @Inject
    private NotificationTaskUtils notificationTaskUtils;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private TaskBase taskBase;

    @Inject
    private JobLogsPersistenceProvider jobLogsPersistenceProvider;

    @Inject
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    public void processNotification(final JobActivityInfo jobActivityInformation, final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse,
            final UpgradePackageContext upgradePackageContext) {
        final long activityJobId = vranSoftwareUpgradeJobResponse.getActivityJobId();

        LOGGER.debug("ActivityJob ID - [{}] : Job notification received : {}", activityJobId, vranSoftwareUpgradeJobResponse);

        recordNotification(vranSoftwareUpgradeJobResponse, activityJobId, upgradePackageContext, VranJobEvents.ACTIVATE_PROCESS_NOTIFICATION);

        LOGGER.debug("ActivityJob ID - [{}] : Activate Service - processing notification for activity [{}] with event type : [{}]", activityJobId, vranSoftwareUpgradeJobResponse.getActivityName(),
                vranSoftwareUpgradeJobResponse.getFlowType());

        if (isActivateInitiationSuccessResponse(vranSoftwareUpgradeJobResponse)) {
            if (notificationTaskUtils.isFlowTypeAction(vranSoftwareUpgradeJobResponse)) {
                processActivityActionNotification(vranSoftwareUpgradeJobResponse, upgradePackageContext, upgradePackageContext.getJobEnvironment(), jobActivityInformation,
                        VranUprgradeConstants.ACTIVATE_OPERATION);
            } else if (notificationTaskUtils.isFlowTypeProgress(vranSoftwareUpgradeJobResponse)) {
                processActivityProgressNotification(vranSoftwareUpgradeJobResponse, upgradePackageContext, jobActivityInformation, VranUprgradeConstants.ACTIVATE_OPERATION);
            } else {
                LOGGER.warn("Unsupported FlowType in processing notification");
            }
        } else if (isActivateInitiationCancelResponse(vranSoftwareUpgradeJobResponse)) {
            LOGGER.debug("ActivityJob ID - [{}] : Processing Cancel action during Activate activity of software upgrade on node", activityJobId);
            jobCancelActivityNotificationProcessor.processNotification(jobActivityInformation, vranSoftwareUpgradeJobResponse, upgradePackageContext);
        } else {
            LOGGER.debug("ActivityJob ID - [{}] : Processing failure notification for activity [{}] of software upgrade on node", activityJobId, vranSoftwareUpgradeJobResponse.getActivityName());
            processNotificationFailureHandler.handle(vranSoftwareUpgradeJobResponse, jobActivityInformation.getActivityName(), upgradePackageContext, jobActivityInformation);
        }
    }

    @Override
    public void proceedWithNextSteps(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse, final UpgradePackageContext upgradePackageContext,
            final JobActivityInfo jobActivityInformation, final String nodeName, final String operation) {

        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        final Map<String, Object> processVariables = new HashMap<>();
        final long activityJobId = vranSoftwareUpgradeJobResponse.getActivityJobId();

        taskBase.unSubscribeNotification(jobActivityInformation, upgradePackageContext, activityJobId);

        recordActivitySucess(vranSoftwareUpgradeJobResponse, upgradePackageContext, nodeName, VranJobEvents.ACTIVATE_PROCESS_NOTIFICATION);

        activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.toString());

        final String toVnfId = extractToVnfId(vranSoftwareUpgradeJobResponse.getAdditionalInfo());

        persistNeJobProperties(toVnfId, upgradePackageContext);

        jobLogsPersistenceProvider.persistActivityJobLogs(vranSoftwareUpgradeJobResponse, VranUprgradeConstants.ACTIVATE_OPERATION, jobProperties);

        activityUtils.sendNotificationToWFS(upgradePackageContext.getJobEnvironment(), activityJobId, vranSoftwareUpgradeJobResponse.getActivityName(), processVariables);

        LOGGER.debug("ActivityJob ID - [{}] : {} activity has been completed successfully. Notifying to workflow service.", activityJobId, vranSoftwareUpgradeJobResponse.getActivityName());
    }

    private void persistNeJobProperties(final String toVnfId, final UpgradePackageContext vranUpgradeInformation) {
        final JobEnvironment jobEnvironment = vranUpgradeInformation.getJobEnvironment();
        final Map<String, Object> neJobAttributes = vranUpgradeInformation.getJobEnvironment().getNeJobAttributes();
        final List<Map<String, Object>> neJobProperties = (List<Map<String, Object>>) neJobAttributes.get(ActivityConstants.JOB_PROPERTIES);
        activityUtils.prepareJobPropertyList(neJobProperties, VranJobConstants.TO_VNF_ID, toVnfId);
        jobAttributesPersistenceProvider.persistJobProperties(jobEnvironment.getNeJobId(), neJobProperties);
    }

    /*
     * This method extracts toVnfId  from additionalInfo
     * additionalInfo is combination of different Strings , which are separated by a COMMA(ex : [To-VNF ID: f0d3f4b6-6310-11e8-90e2-fa163ea2762d, ppUpgradeHandlerX64Lm(CXC2011284_1): OK:UPI binary])
     * toVnfId format will be ^[a-fA-F0-9]{8}(-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}$
     */
    private String extractToVnfId(final String additionalInfo) {
        String[] vnfIdInfo = null;
        String toVnfId = null;
        if (additionalInfo != null && !additionalInfo.isEmpty()) {
            vnfIdInfo = additionalInfo.substring(1, additionalInfo.length() - 1).split(ActivityConstants.COMMA);
            for (int i = 0; i < vnfIdInfo.length; i++) {
                final String vnfIdAttributeInfo = vnfIdInfo[i].replaceAll("\\s+", ActivityConstants.EMPTY);
                if (vnfIdAttributeInfo.toLowerCase().contains("to-vnfid")) {
                    final String[] toVnfIdInfo = vnfIdAttributeInfo.split(ActivityConstants.COLON);
                    toVnfId = toVnfIdInfo[1];
                    final boolean isValidToVnfId = Pattern.matches("^[a-fA-F0-9]{8}(-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}$", toVnfId);
                    if (isValidToVnfId) {
                        return toVnfId;
                    } else {
                        LOGGER.warn("Invalid To-VNF ID : {}", toVnfId);
                        return null;
                    }
                }
            }
        }
        return toVnfId;
    }

    private boolean isActivateInitiationSuccessResponse(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse) {
        return notificationTaskUtils.isJobSuccessResponse(vranSoftwareUpgradeJobResponse);
    }

    private boolean isActivateInitiationCancelResponse(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse) {
        return vranSoftwareUpgradeJobResponse != null && ShmConstants.CANCELLED.equalsIgnoreCase(vranSoftwareUpgradeJobResponse.getResult());
    }

    @Override
    public String getActivityName() {
        return ActivityConstants.ACTIVATE;
    }

}
