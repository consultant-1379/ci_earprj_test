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
package com.ericsson.oss.services.shm.jobexecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.tbac.api.SHMTBACHandler;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobTemplate;
import com.ericsson.oss.services.shm.networkelement.NetworkElementResponse;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.tbac.TBACResponse;
import com.ericsson.oss.services.shm.tbac.models.TBACConfigurationProvider;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

@ApplicationScoped
public class TBACValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TBACValidator.class);
    private static final String MAIN_JOB_ID_STRING = "mainJobId:";
    private static final String JOB_TEMPLATE_ID_STRING = "job template id:";

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private SHMTBACHandler shmTbacHandler;

    @Inject
    private TBACConfigurationProvider tbacConfigurationProvider;

    @Inject
    private WorkflowInstanceNotifier workflowInstanceNotifier;

    @Inject
    private SystemRecorder systemRecorder;

    /**
     * This method will validate the TBAC authorization for user
     * 
     * @param networkElementsResponse
     * @param jobTemplate
     * @return
     */
    public TBACResponse validateTBAC(final NetworkElementResponse networkElementsResponse, final JobTemplate jobTemplate, final long mainJobId, final Map<String, Object> mainJobAttribute) {
        String owner = "";
        final TBACResponse tbacResponse = new TBACResponse();
        final boolean isTBACAtJobLevel = tbacConfigurationProvider.isTBACAtJobLevel();
        final Set<String> networkElementsForTbacAuthorization = getNeNamesForTbacAuthorization(networkElementsResponse);
        final String executionMode = getExecutionMode(jobTemplate);

        if (executionMode.equalsIgnoreCase(ExecMode.MANUAL.getMode())) {
            owner = (String) (mainJobAttribute.get(ShmConstants.SHM_JOB_EXEC_USER) != null ? (String) mainJobAttribute.get(ShmConstants.SHM_JOB_EXEC_USER) : "");
        } else {
            owner = jobTemplate.getOwner(); // Read the owner from job template for SCHEDULE and IMIDIATE job execution
        }
        try {
            return getAuthorizationResponse(isTBACAtJobLevel, networkElementsForTbacAuthorization, owner);
        } catch (Exception e) {
            final String message = JobExecutorConstants.TBAC_FAILURE_REASON + e.getMessage();
            LOGGER.error(message, e);
            handleTBACExceptions(message, mainJobId, jobTemplate.getJobTemplateId());
            tbacResponse.setTBACValidationSuccess(false);
            systemRecorder.recordEvent(SHMEvents.JOB_END, EventLevel.COARSE, MAIN_JOB_ID_STRING + mainJobId, JOB_TEMPLATE_ID_STRING + jobTemplate.getJobTemplateId(), message);
            return tbacResponse;
        }

    }

    private TBACResponse getAuthorizationResponse(final boolean isTbacValidationToBeDoneForAllNodesAsSingleTarget, final Set<String> allNetworkElementToTbacAuthorization, final String owner) {
        TBACResponse tbacResponse;
        LOGGER.debug("[{}] NetworkElements selected for TBAC authorization for user:{}", allNetworkElementToTbacAuthorization.size(), owner);
        if (isTbacValidationToBeDoneForAllNodesAsSingleTarget) {
            tbacResponse = getTBACAuthorizationForCollectionsOfNodes(owner, allNetworkElementToTbacAuthorization);
        } else {
            tbacResponse = getTBACAuthorizationForIndividualNodes(owner, allNetworkElementToTbacAuthorization);
        }
        return tbacResponse;
    }

    private List<NetworkElement> buildNetworkElement(final Set<String> neNames) {
        LOGGER.debug("Building NetworkElements for : {}", neNames);
        final List<NetworkElement> networkElementsList = new ArrayList<>();
        for (final String neName : neNames) {
            final NetworkElement networkElement = new NetworkElement();
            networkElement.setName(neName);
            networkElementsList.add(networkElement);
        }
        return networkElementsList;
    }

    private String getExecutionMode(final JobTemplate jobTemplate) {
        final JobConfiguration jobConfiguration = jobTemplate.getJobConfigurationDetails();
        final com.ericsson.oss.services.shm.jobs.common.modelentities.Schedule mainSchedule = jobConfiguration.getMainSchedule();
        return mainSchedule.getExecMode().name();
    }

    /**
     * 
     * @param networkElementsResponse
     * @return
     */
    private Set<String> getNeNamesForTbacAuthorization(final NetworkElementResponse networkElementsResponse) {
        final Set<String> networkElementsForTbacAuthorization = new HashSet<>();
        for (final NetworkElement networkElement : networkElementsResponse.getSupportedNes()) {
            networkElementsForTbacAuthorization.add(networkElement.getName());
        }
        for (final NetworkElement networkElement : networkElementsResponse.getUnsupportedNes().keySet()) {
            networkElementsForTbacAuthorization.add(networkElement.getName());
        }
        networkElementsForTbacAuthorization.addAll(networkElementsResponse.getNesWithComponents().keySet());
        return networkElementsForTbacAuthorization;
    }

    private TBACResponse getTBACAuthorizationForCollectionsOfNodes(final String owner, final Set<String> allNetworkElementToTbacAuthorization) {
        final TBACResponse tbacResponse = new TBACResponse();
        String[] networkElementsArray = new String[allNetworkElementToTbacAuthorization.size()];
        networkElementsArray = allNetworkElementToTbacAuthorization.toArray(networkElementsArray);
        tbacResponse.setTBACValidationSuccess(true);
        tbacResponse.setTbacValidationToBeDoneForAllNodesAsSingleTarget(true);
        if (owner != null && owner != "") {
            if (shmTbacHandler.isAuthorized(owner, networkElementsArray)) {
                tbacResponse.setUnAuthorizedNes(Collections.<NetworkElement> emptyList());
            } else {
                tbacResponse.setUnAuthorizedNes(buildNetworkElement(allNetworkElementToTbacAuthorization));
            }
        } else {
            if (shmTbacHandler.isAuthorized(networkElementsArray)) {
                tbacResponse.setUnAuthorizedNes(Collections.<NetworkElement> emptyList());
            } else {
                tbacResponse.setUnAuthorizedNes(buildNetworkElement(allNetworkElementToTbacAuthorization));
            }
        }
        return tbacResponse;
    }

    private TBACResponse getTBACAuthorizationForIndividualNodes(final String owner, final Set<String> allNetworkElementToTbacAuthorization) {
        final TBACResponse tbacResponse = new TBACResponse();
        final Set<String> unAuthorizedNodes = new HashSet<>();
        if (owner != null && owner != "") {
            for (final String neName : allNetworkElementToTbacAuthorization) {
                if (!shmTbacHandler.isAuthorized(owner, neName)) {
                    unAuthorizedNodes.add(neName);
                }
                tbacResponse.setTBACValidationSuccess(true);
            }
        } else {
            for (final String neName : allNetworkElementToTbacAuthorization) {
                if (!shmTbacHandler.isAuthorized(neName)) {
                    unAuthorizedNodes.add(neName);
                }
                tbacResponse.setTBACValidationSuccess(true);
            }
        }
        tbacResponse.setUnAuthorizedNes(buildNetworkElement(unAuthorizedNodes));
        return tbacResponse;

    }

    private void handleTBACExceptions(final String message, final long mainJobId, final long jobTemplateId) {
        final Map<String, Object> jobLogList = new HashMap<>();
        final Map<String, Object> attributeMap = new HashMap<>();

        jobLogList.put(ActivityConstants.JOB_LOG_MESSAGE, message);
        jobLogList.put(ActivityConstants.JOB_LOG_ENTRY_TIME, new Date());
        jobLogList.put(ActivityConstants.JOB_LOG_TYPE, JobLogType.SYSTEM.toString());
        jobLogList.put(ActivityConstants.JOB_LOG_LEVEL, JobLogLevel.ERROR.toString());

        attributeMap.put(ShmConstants.LOG, Arrays.asList(jobLogList));
        attributeMap.put(ShmConstants.RESULT, JobResult.FAILED.toString());
        attributeMap.put(ShmConstants.STATE, JobState.COMPLETED.getJobStateName());
        attributeMap.put(ShmConstants.ENDTIME, new Date());
        jobUpdateService.updateJobAttributes(mainJobId, attributeMap);
        workflowInstanceNotifier.sendAllNeDone(Long.toString(jobTemplateId));
    }

}
