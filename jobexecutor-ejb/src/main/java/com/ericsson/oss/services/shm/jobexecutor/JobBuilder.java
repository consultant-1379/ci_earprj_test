/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.jobexecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.services.shm.activities.NeComponentBuilder;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.Activity;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

@Stateless
public class JobBuilder {

    @Inject
    private JobExecutorServiceHelper executorServiceHelper;

    @Inject
    private JobConfigurationService jobConfigurationService;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private NeComponetsInfoBuilderFactory neComponetsInfoBuilderFactory;

    private static final Logger LOGGER = LoggerFactory.getLogger(JobBuilder.class);
    private static final String FAILURE_MSG = "Job creation failed";

    public Map<String, Object> createNeJob(final long mainJobId, final String businessKey, final NetworkElement selectedNE, final Map<String, String> neDetailsWithParentName,
            final Map<String, Object> supportedAndUnsupported) {
        LOGGER.debug("Inside createAndSkipNeJob with input: mainJobId:{}, node:{}, supported and unsupported nodes::{}", mainJobId, selectedNE.getName(), supportedAndUnsupported);
        final Map<String, Object> neJobDetails = new HashMap<>();
        final String neType = selectedNE.getNeType();

        long neJobId = 0L;
        final Map<String, Object> neJobAttributes = createNeJobAttributes(mainJobId, selectedNE, businessKey, neType, neDetailsWithParentName, supportedAndUnsupported);
        //If main job state is canceling then skipping ne job creation.
        if (isMainJobInCompletedOrCancellingState(mainJobId)) {
            neJobDetails.put(ShmConstants.JOB_STATUS, ShmConstants.CANCELLED);
            neJobDetails.put(ShmConstants.NE_JOB_ID, neJobId);
            return neJobDetails;
        }
        final Map<String, Object> neJobPO = createJobPO(ShmConstants.NAMESPACE, ShmConstants.NE_JOB, ShmConstants.VERSION, neJobAttributes);
        if (neJobPO.isEmpty()) {
            neJobDetails.put(ShmConstants.JOB_STATUS, ShmConstants.CREATION_FAILED);
            neJobDetails.put(ShmConstants.CREATION_FAILURE_CAUSE, FAILURE_MSG);
        } else {
            neJobId = (long) neJobPO.get(ShmConstants.PO_ID);
        }
        neJobDetails.put(ShmConstants.NE_JOB_ID, neJobId);

        return neJobDetails;
    }

    public Map<String, Object> createAndEndNeJob(final long mainJobId, final NetworkElement selectedNE, final String jobResult, final String logMessage, final String businessKey) {
        LOGGER.info("Inside createAndEndNeJob with input: mainJobId:{}, node:{}, result:{} and logMessage:{}", mainJobId, selectedNE.getName(), jobResult, logMessage);
        final Map<String, Object> neJobDetails = new HashMap<>();
        long neJobId = 0l;
        final boolean isNeJobExists = isNeJobExists(mainJobId, selectedNE.getName(), businessKey);
        if (!isNeJobExists) {
            neJobDetails.put(ShmConstants.JOB_STATUS, ShmConstants.CREATION_STARTED);
            final Map<String, Object> neJobAttributes = createNeJobAttributes(mainJobId, selectedNE.getName(), businessKey, logMessage, JobState.COMPLETED.getJobStateName(), selectedNE.getNeType(),
                    jobResult);
            final Map<String, Object> neJobPO = createJobPO(ShmConstants.NAMESPACE, ShmConstants.NE_JOB, ShmConstants.VERSION, neJobAttributes);
            if (neJobPO.isEmpty()) {
                LOGGER.warn("NEJob creation failed for node={}", selectedNE);
                neJobDetails.put(ShmConstants.JOB_STATUS, ShmConstants.CREATION_FAILED);
                neJobDetails.put(ShmConstants.CREATION_FAILURE_CAUSE, FAILURE_MSG);
            } else {
                neJobId = (long) neJobPO.get(ShmConstants.PO_ID);
                neJobDetails.put(ShmConstants.JOB_STATUS, ShmConstants.CREATION_COMPLETED);
            }
        } else {
            neJobDetails.put(ShmConstants.JOB_STATUS, ShmConstants.EXISTED);
        }
        neJobDetails.put(ShmConstants.NE_JOB_ID, neJobId);
        return neJobDetails;
    }

    public List<Map<String, Object>> createActivityJobs(final long neJobId, final List<Activity> activities, final Map<String, String> neDetailsWithParentName, final String selectedNe) {
        final List<Map<String, Object>> activityJobList = new ArrayList<>();
        if (activities == null) {
            return activityJobList;
        }
        for (final Activity activity : activities) {
            final Map<String, Object> activityAttributes = new HashMap<>();
            activityAttributes.put(ShmConstants.ACTIVITY_NAME, activity.getName());
            activityAttributes.put(ShmConstants.ORDER, activity.getOrder());
            activityAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
            activityAttributes.put(ShmConstants.CREATION_TIME, new Date());
            if (neDetailsWithParentName.containsKey(selectedNe)) {
                updateJobProperties(activityAttributes, neDetailsWithParentName, selectedNe);
            }
            final Map<String, Object> activityJobPO = createJobPO(ShmConstants.NAMESPACE, ShmConstants.ACTIVITY_JOB, ShmConstants.VERSION, activityAttributes);
            LOGGER.debug("Activity PO ID: {}", activityJobPO.get(ShmConstants.PO_ID));
            activityJobList.add(activityJobPO);
        }
        return activityJobList;
    }

    private Map<String, Object> createJobPO(final String namespace, final String type, final String version, final Map<String, Object> jobAttributes) {
        return executorServiceHelper.createPO(namespace, type, version, jobAttributes);
    }

    private Map<String, Object> createNeJobAttributes(final long mainJobId, final NetworkElement selectedNE, final String businessKey, final String neType,
            final Map<String, String> neDetailsWithParentName, final Map<String, Object> supportedAndUnsupported) {

        final Map<String, Object> neJobAttributes = new HashMap<>();

        neJobAttributes.put(ShmConstants.NE_NAME, selectedNE.getName());
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, businessKey);
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ShmConstants.CREATION_TIME, new Date());
        neJobAttributes.put(ShmConstants.NETYPE, neType);
        updateNeJobPoJobProperties(neJobAttributes, neDetailsWithParentName, selectedNE, (List<NetworkElement>) supportedAndUnsupported.get(ShmConstants.SUPPORTED_NES),
                (List<NetworkElement>) supportedAndUnsupported.get(ShmConstants.UNSUPPORTED_NES));
        return neJobAttributes;
    }

    @SuppressWarnings("unchecked")
    private void updateJobProperties(final Map<String, Object> jobAttributes, final Map<String, String> neDetailsWithParentName, final String selectedNE) {
        LOGGER.debug("Updating JobProperties with neJobAttributes entry {} ", jobAttributes);
        List<Map<String, Object>> neJobPropertiesList = (List<Map<String, Object>>) jobAttributes.get(ActivityConstants.JOB_PROPERTIES);
        if (neJobPropertiesList == null) {
            neJobPropertiesList = new ArrayList<>();
        }
        prepareJobPropertyList(neJobPropertiesList, ShmConstants.IS_COMPONENT_JOB, ShmConstants.IS_COMPONENT_JOB_TRUE);
        prepareJobPropertyList(neJobPropertiesList, ShmConstants.PARENT_NAME, neDetailsWithParentName.get(selectedNE));
        jobAttributes.put(ActivityConstants.JOB_PROPERTIES, neJobPropertiesList);
        LOGGER.debug("Updating JobProperties with neJobAttributes exit {} ", jobAttributes);
    }

    public List<Map<String, Object>> prepareJobPropertyList(final List<Map<String, Object>> jobPropertyList, final String propertyName, final String propertyValue) {
        final Map<String, Object> jobProperty = new HashMap<>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, propertyName);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, propertyValue);
        jobPropertyList.add(jobProperty);
        return jobPropertyList;

    }

    private Map<String, Object> createNeJobAttributes(final long mainJobId, final String selectedNE, final String businessKey, final String logMessage, final String jobState, final String neType,
            final String jobResult) {

        final Map<String, Object> neJobAttributes = new HashMap<>();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        executorServiceHelper.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        neJobAttributes.put(ShmConstants.NE_NAME, selectedNE);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, businessKey);
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ShmConstants.STATE, jobState);
        neJobAttributes.put(ShmConstants.LOG, jobLogList);
        neJobAttributes.put(ShmConstants.STARTTIME, new Date());
        neJobAttributes.put(ShmConstants.ENDTIME, new Date());
        neJobAttributes.put(ShmConstants.RESULT, jobResult);
        neJobAttributes.put(ShmConstants.NETYPE, neType);
        LOGGER.debug("Prepared NeJobAttributes are : {}", neJobAttributes);

        return neJobAttributes;
    }

    private boolean isMainJobInCompletedOrCancellingState(final long mainJobId) {
        LOGGER.debug("Checking Main job state for the job id: {}", mainJobId);

        final Map<Object, Object> restrictionAttributes = new HashMap<>();
        restrictionAttributes.put(ObjectField.PO_ID, mainJobId);
        final List<String> projectedAttributes = new ArrayList<>();
        projectedAttributes.add(ShmConstants.STATE);
        final List<Map<String, Object>> mainJobProjectionAttributes = jobConfigurationService.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.JOB, restrictionAttributes,
                projectedAttributes);
        if (mainJobProjectionAttributes != null && !mainJobProjectionAttributes.isEmpty()) {
            final String jobStateName = (String) mainJobProjectionAttributes.get(0).get(ShmConstants.STATE);
            final JobState mainJobState = JobState.getJobState(jobStateName);
            final boolean isCompletedOrCancelling = JobState.isJobInactive(mainJobState) || JobState.isJobCancelInProgress(mainJobState);
            if (isCompletedOrCancelling) {
                LOGGER.debug("Main job state is : {}", mainJobState);
                return isCompletedOrCancelling;
            }
        }
        return false;
    }

    /**
     * @param mainJobId
     * @param selectedNE
     * @param businessKey
     * @return
     */
    private boolean isNeJobExists(final long mainJobId, final String selectedNE, final String businessKey) {
        final boolean isNeJobExists = false;
        final Map<Object, Object> restrictionAttributes = new HashMap<>();
        restrictionAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        restrictionAttributes.put(ShmConstants.BUSINESS_KEY, businessKey);
        restrictionAttributes.put(ShmConstants.NE_NAME, selectedNE);

        final List<String> projectedAttributes = new ArrayList<>();
        projectedAttributes.add(ShmConstants.STATE);

        final List<Map<String, Object>> neJobs = jobConfigurationService.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.NEJOB_TYPE, restrictionAttributes, projectedAttributes);
        if (!neJobs.isEmpty()) {
            LOGGER.warn("Already {} NEJob(s) found on the same node[{}]. so no further Ne Jobs created for selected node ", neJobs.size(), selectedNE);
            return true;
        }
        LOGGER.info("There are no Jobs running on the node '{}' so will proceed to create the child jobs.", selectedNE);
        return isNeJobExists;
    }

    private void updateJobAtributes(final long jobId, final Map<String, Object> jobAttributesToUpdate) {
        LOGGER.debug("Updating job Attributes {} for jobId {} ", jobAttributesToUpdate, jobId);
        jobUpdateService.updateJobAttributesWihoutRetries(jobId, jobAttributesToUpdate);
    }

    private void updateNeJobPoJobProperties(final Map<String, Object> jobAttributes, final Map<String, String> neDetailsWithParentName, final NetworkElement selectedNE,
            final List<NetworkElement> supportedNEtworkElemnts, final List<NetworkElement> allunSupportedNetworkElements) {
        LOGGER.debug("Updating JobProperties with neJobAttributes entry {} ", jobAttributes);
        final Map<PlatformTypeEnum, List<NetworkElement>> supportedNesGroupedByPlatform = executorServiceHelper.groupNetworkElementsByPlatform(supportedNEtworkElemnts);
        final Map<PlatformTypeEnum, List<NetworkElement>> unSupportedNesGroupedByPlatform = executorServiceHelper.groupNetworkElementsByPlatform(allunSupportedNetworkElements);
        LOGGER.info("unSupportedNesGroupedByPlatform :{}", unSupportedNesGroupedByPlatform);

        for (final Map.Entry<PlatformTypeEnum, List<NetworkElement>> supportedNeEntry : supportedNesGroupedByPlatform.entrySet()) {
            if (supportedNeEntry.getKey().equals(selectedNE.getPlatformType())) {
                final NeComponentBuilder neComponentBuilder = neComponetsInfoBuilderFactory.getNeComponentBuilderInstance(supportedNeEntry.getKey());
                if (neComponentBuilder != null) {
                    neComponentBuilder.prepareNeJobPoProperties(supportedNeEntry.getValue(), jobAttributes, neDetailsWithParentName, selectedNE,
                            unSupportedNesGroupedByPlatform.get(selectedNE.getPlatformType()));
                }
                break;
            }
        }
        LOGGER.debug("Updating JobProperties with neJobAttributes exit {} ", jobAttributes);
    }

    /**
     * @param neNames
     * @param jobId
     */
    protected void updateJobLog(final long jobId, final String message) {
        final Map<String, Object> activityAttributes = new HashMap<>();
        activityAttributes.put(ActivityConstants.JOB_LOG_MESSAGE, message);
        activityAttributes.put(ActivityConstants.JOB_LOG_ENTRY_TIME, new Date());
        activityAttributes.put(ActivityConstants.JOB_LOG_TYPE, JobLogType.SYSTEM.toString());
        activityAttributes.put(ActivityConstants.JOB_LOG_LEVEL, JobLogLevel.INFO.toString());

        final Map<String, Object> jobAttributes = new HashMap<>();
        jobAttributes.put(ShmConstants.LOG, Arrays.asList(activityAttributes));
        updateJobAtributes(jobId, jobAttributes);
    }

}
