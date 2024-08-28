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
package com.ericsson.oss.services.shm.es.impl.stn.softwareupgrade.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.stn.softwareupgrade.common.*;
import com.ericsson.oss.services.shm.es.impl.stn.softwareupgrade.constants.*;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
/**
 * This class performs the operation followed by cancel operation
 * 
 * @author xsamven/xvenupe
 *
 */

@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CancelSoftwareUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CancelSoftwareUpgradeService.class);

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private StnSoftwareEndUpgradeEventSender stnSoftwareEndUpgradeEventSender;
    
    @Inject
    private StnSoftwareUpgradeJobService stnSoftwareUpgradeActivityService;

    /**
     * This method will process the cancel action of software upgrade operation
     * 
     * @param activityJobId
     * @param activityName
     * @param jobActivityInformation
     * @return activityStepResult
     */
    @SuppressWarnings("deprecation")
    public ActivityStepResult cancel(final long activityJobId, final String activityName, final JobActivityInfo jobActivityInformation) {
        ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        LOGGER.debug("ActivityJob ID - [{}] :Processing cancel action for activity [{}]", activityJobId, activityName);
        activityUtils.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.ACTIVITY_INITIATED, StnJobConstants.JOB_CANCEL), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.INFO.toString());
        try {
            activityStepResult = performCancelAction(activityJobId, activityName, jobLogs, jobActivityInformation);
            LOGGER.debug("ActivityJob ID - [{}] : Processed cancel action for activity {} and the result is : [{}]", activityJobId, activityName, activityStepResult.getActivityResultEnum());
            return activityStepResult;
        } catch (final Exception e) {
            LOGGER.error("Failed to perform cancel action for ActivityJob ID " + activityJobId + " . Reason : ", e);
        }
        return activityStepResult;
    }

    @SuppressWarnings("deprecation")
    private ActivityStepResult performCancelAction(final long activityJobId, final String activityName, final List<Map<String, Object>> jobLogs, final JobActivityInfo jobActivityInformation) {
        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final Map<String, Object> processVariables = new HashMap<>();
        final Map<String, Object> eventAttributes = new HashMap<>();
        LOGGER.debug("ActivityJob ID - [{}] :Processing cancel action for activity {} ", activityJobId, activityName);
        final StnJobInformation stnUpgradeInformation = stnSoftwareUpgradeActivityService.buildStnUpgradeInformation(activityJobId);
        final JobEnvironment jobContext = stnUpgradeInformation.getJobEnvironment();
        final String nodeAddress = stnUpgradeInformation.getNodeFdn();
        eventAttributes.put(ShmConstants.NE_NAME, stnUpgradeInformation.getNeName());
        eventAttributes.put(ActivityConstants.ACTIVITY_JOB_ID, activityJobId);
        final String subscriptionKey = StnJobConstants.SUBSCRIPTION_KEY_DELIMETER + nodeAddress + StnJobConstants.SUBSCRIPTION_KEY_DELIMETER + activityName +StnJobConstants.SUBSCRIPTION_KEY_DELIMETER;
        activityUtils.unSubscribeToMoNotifications(subscriptionKey, activityJobId, jobActivityInformation);
        activityUtils.logCancelledByUser(jobLogs, jobContext, jobActivityInformation.getActivityName());
        activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.IS_CANCEL_TRIGGERED, "true");
        stnSoftwareEndUpgradeEventSender.sendSoftwareUpgradeActionRequest(activityJobId, activityName, nodeAddress, eventAttributes);
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_SUCESS);
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobProperties, jobLogs);
        activityUtils.sendNotificationToWFS(jobContext, jobActivityInformation.getActivityJobId(), StnJobConstants.JOB_CANCEL, processVariables);
        return activityStepResult;
    }
  }
