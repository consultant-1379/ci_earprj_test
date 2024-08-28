/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.jobservice.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.job.utils.CreateJobAdditionalDataProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.WorkFlowConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ComponentActivity;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.modelentities.NeTypeComponentActivityDetails;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ScheduleProperty;
import com.ericsson.oss.services.shm.shared.util.JobPropertyUtil;
import com.ericsson.oss.services.shm.shared.util.ProcessVariablesUtil;
import com.ericsson.oss.services.shm.workflow.BatchWorkFlowProcessVariables;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

/**
 * This class populates the Job Configuration details and persists Job Template persistence object. Also it invokes WFS for main Job, NE job and
 * Activity Job creation.
 * 
 */
@Stateless
@Traceable
public class ShmJobHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShmJobHandler.class);

    @Inject
    private WorkflowInstanceNotifier workflowInstanceHelper;

    @Inject
    SystemRecorder systemRecorder;

    @Inject
    private JobTemplatePersistenceService jobTemplatePersistenceService;

    @Inject
    private DpsWriter dpsWriter;

    @Inject
    private ProcessVariablesUtil processVariablesUtil;

    @Inject
    CreateJobAdditionalDataProvider axeBackupJobAdditionalDataProvider;

    /**
     * @param jobInfo
     * @return
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> populateAndPersistJobConfigurationData(final JobInfo jobInfo) {
        LOGGER.debug("PopulateAndPersisting JobConfigurationData");
        Map<String, Object> processVariables = new HashMap<String, Object>();
        final Map<String, Object> response = new HashMap<String, Object>();
        response.put("errorCode", JobHandlerErrorCodes.SUCCESS.getResponseDescription());
        Map<String, Object> jobTemplate = null;
        String jobName = "";
        final List<ScheduleProperty> schedulePropertyList = new ArrayList<ScheduleProperty>();
        final String mainJobExeMode = jobInfo.getMainSchedule().get(ShmConstants.EXECUTION_MODE).toString();
        final String batchStartup = mainJobExeMode.toLowerCase();

        if (batchStartup.equalsIgnoreCase(ExecMode.SCHEDULED.getMode())) {
            final Map<String, Object> responseMap = schedulingAttributesValidation(jobInfo);
            if (responseMap.get("errorCode") != null) {
                return responseMap;
            }
        }
        jobTemplate = fillJobTemplateDetailsMap(jobInfo);
        LOGGER.info("Job template is : {}", jobTemplate);

        final long poid = jobTemplatePersistenceService.createJobTemplate(jobTemplate, jobInfo.getName());

        if (poid != 0) {
            jobName = jobInfo.getName();
            response.put(ShmConstants.JOBCONFIGID, poid);
            response.put(ShmConstants.JOBNAME, jobName);
            LOGGER.debug("Persisting job configuration data. Response is : {}", response);

            final String logMessage = "Job is created successfully with poid:" + "" + poid;

            systemRecorder.recordEvent(SHMEvents.JOB_CREATE, EventLevel.COARSE, "", jobName, logMessage);

            processVariables.put(BatchWorkFlowProcessVariables.TEMPLATE_JOB_ID, poid);
            processVariables.put(BatchWorkFlowProcessVariables.BATCH_STARTUP, batchStartup);
            processVariables.put(BatchWorkFlowProcessVariables.JOB_TYPE, jobInfo.getJobType().getAttribute());
            if (batchStartup.equals(JobVariables.ACTIVITY_STARTUP_SCHEDULED)) {
                final List<Map<String, Object>> scheduleAttributes = (List<Map<String, Object>>) jobInfo.getMainSchedule()
                        .get(ShmConstants.SCHEDULINGPROPERTIES);
                for (final Map<String, Object> schedules : scheduleAttributes) {
                    final ScheduleProperty scheduleProperty = new ScheduleProperty();
                    scheduleProperty.setName((String) schedules.get(ShmConstants.NAME));
                    scheduleProperty.setValue((String) schedules.get(ShmConstants.VALUE));
                    schedulePropertyList.add(scheduleProperty);
                }
                try {
                    processVariables = processVariablesUtil.setProcessVariablesForSchedule(schedulePropertyList, processVariables);
                } catch (final IllegalArgumentException illegalArgumentException) {
                    final String failureReason = "Job creation is failed because " + illegalArgumentException;

                    systemRecorder.recordEvent(SHMEvents.JOB_CREATE, EventLevel.COARSE, "", jobName, failureReason);
                    LOGGER.error(failureReason);

                    response.put("errorCode", JobHandlerErrorCodes.JOB_CFGN_PERSISTENCE_FAILED.getResponseDescription());
                    return response;
                }

            }
            final String wfsId = submitWorkFlowInstance(processVariables, poid);
            if (wfsId != null) {
                final Map<String, Object> jobTemplateAttributes = new HashMap<String, Object>();
                jobTemplateAttributes.put(ShmConstants.BUSINESS_KEY, Long.toString(poid));
                jobTemplateAttributes.put(ShmConstants.WFS_ID, wfsId);
                dpsWriter.update(poid, jobTemplateAttributes);
                LOGGER.debug("Updated business key and WFS Id of jobtemplate with PO ID : {} and WFS ID : {} ", poid, wfsId);
            } else {
                final String failureReason = "Job creation failed as Creation of workflow instance failed";
                systemRecorder.recordEvent(SHMEvents.JOB_CREATE, EventLevel.COARSE, "", jobName, failureReason);
                LOGGER.error("Job failed for {} and message {}", jobName, JobHandlerErrorCodes.JOB_CFGN_PERSISTENCE_FAILED.getResponseDescription());
                response.put("errorCode", JobHandlerErrorCodes.JOB_CFGN_PERSISTENCE_FAILED.getResponseDescription());
                return response;
            }
        } else {
            final String logMessage = "Job creation is failed";
            systemRecorder.recordEvent(SHMEvents.JOB_CREATE, EventLevel.COARSE, "", jobName, logMessage);
            LOGGER.error("Job failed for {} and message {}", jobName, JobHandlerErrorCodes.JOB_CFGN_PERSISTENCE_FAILED.getResponseDescription());
            response.put("errorCode", JobHandlerErrorCodes.JOB_CFGN_PERSISTENCE_FAILED.getResponseDescription());
            return response;
        }
        return response;
    }

    /**
     * @param processVariables
     */
    private String submitWorkFlowInstance(final Map<String, Object> processVariables, final long poId) {
        try {
            final String workflowInstance = workflowInstanceHelper.submitWorkFlowInstance(processVariables, Long.toString(poId));
            LOGGER.info("Work flow instance is submitted for over all batch execution with wfs id:{} & job template id:{}", workflowInstance,
                    processVariables.get(WorkFlowConstants.TEMPLATE_JOB_ID));
            return workflowInstance;
        } catch (Exception ex) {
            LOGGER.error("Creation of workflow instance failed for job with POID : {} due to exception : {}", poId, ex);
            try {
                dpsWriter.deletePoByPoId(poId);
            } catch (Exception deletePOException) {
                LOGGER.error("Deletion for job with POID : {} failed due to exception : {}", poId, ex);
            }
            return null;
        }
    }

    /**
     * This method is used to prepare job template details
     * 
     * @param jobInfo
     * @return jobTemplate
     */
    private Map<String, Object> fillJobTemplateDetailsMap(final JobInfo jobInfo) {

        final HashMap<String, Object> selectedNEs = new HashMap<>();
        selectedNEs.put(ShmConstants.NENAMES, jobInfo.getFdns());
        //Collection Names actually sets collectionIds        
        selectedNEs.put(ShmConstants.COLLECTION_NAMES, jobInfo.getcollectionNames());
        //Setting SavedSearchIds        
        selectedNEs.put(ShmConstants.SAVED_SEARCH_IDS, jobInfo.getSavedSearchIds());
        updateSelectedNEsWithComponentInfo(jobInfo, selectedNEs);
        //updating NEInfo for each ne type components activities info
        updateNeTypeComponentActivityDetails(jobInfo, selectedNEs);
        final Map<String, Object> jobConfiguration = new HashMap<String, Object>();
        
        updateNeTypeJobPropertiesInJobConfiguration(jobInfo, jobConfiguration);
        
        updateNeJobPropertiesInJobConfiguration(jobInfo, jobConfiguration);
        updateJobConfigWithNeTypeActivityJobProperties(jobInfo, jobConfiguration);

        if ((jobInfo.getPlatformJobProperties() != null) && !(jobInfo.getPlatformJobProperties().isEmpty())) {

            final List<Map<String, Object>> platformJobProperties = new ArrayList<Map<String, Object>>();
            for (final PlatformProperty platformjobProperty : jobInfo.getPlatformJobProperties()) {
                final Map<String, Object> platformMapProperty = new HashMap<String, Object>();
                platformMapProperty.put(ShmConstants.PLATFORM, platformjobProperty.getPlatform());
                platformMapProperty.put(ShmConstants.JOBPROPERTIES, platformjobProperty.getJobProperties());
                platformJobProperties.add(platformMapProperty);
            }
            jobConfiguration.put(ShmConstants.PLATFORMJOBPROPERTIES, platformJobProperties);

        }

        jobConfiguration.put(ShmConstants.SELECTED_NES, selectedNEs);
        jobConfiguration.put(ShmConstants.MAIN_SCHEDULE, jobInfo.getMainSchedule());
        jobConfiguration.put(ShmConstants.ACTIVITIES, jobInfo.getActivitySchedules());
        LOGGER.info("JobConfiguration {} and Job Category value is :{} ", jobConfiguration, jobInfo.getJobCategory().getAttribute());
        final Map<String, Object> jobTemplate = new HashMap<String, Object>();
        jobTemplate.put(ShmConstants.NAME, jobInfo.getName());
        jobTemplate.put(ShmConstants.JOB_TYPE, jobInfo.getJobType().getAttribute());
        jobTemplate.put(ShmConstants.OWNER, jobInfo.getOwner());
        jobTemplate.put(ShmConstants.CREATION_TIME, new Date());
        jobTemplate.put(ShmConstants.DESCRIPTION, jobInfo.getDescription());

        jobTemplate.put(ShmConstants.JOB_CATEGORY, jobInfo.getJobCategory().getAttribute());
        if ((jobInfo.getJobProperties() != null) && !(jobInfo.getJobProperties().isEmpty())) {
            jobConfiguration.put(ShmConstants.JOBPROPERTIES, jobInfo.getJobProperties());
        }
        jobTemplate.put(ShmConstants.JOBCONFIGURATIONDETAILS, jobConfiguration);
        return jobTemplate;
    }

    private void updateNeJobPropertiesInJobConfiguration(final JobInfo jobInfo, final Map<String, Object> jobConfiguration) {
        if ((jobInfo.getNeJobProperties() != null) && !(jobInfo.getNeJobProperties().isEmpty())) {
            final List<Map<String, Object>> neJobProperties = new ArrayList<>();
            for (final NeJobProperty jobProperty : jobInfo.getNeJobProperties()) {
                final Map<String, Object> neJobProperty = new HashMap<>();
                neJobProperty.put(ShmConstants.NE_NAME, jobProperty.getNeName());
                neJobProperty.put(ShmConstants.JOBPROPERTIES, jobProperty.getJobProperties());
                neJobProperties.add(neJobProperty);
            }
            jobConfiguration.put(ShmConstants.NEJOB_PROPERTIES, neJobProperties);
        }
    }

    private void updateNeTypeJobPropertiesInJobConfiguration(final JobInfo jobInfo,
            final Map<String, Object> jobConfiguration) {
        if ((jobInfo.getNETypeJobProperties() != null) && !(jobInfo.getNETypeJobProperties().isEmpty())) {

            final List<Map<String, Object>> neTypeJobProperties = new ArrayList<Map<String, Object>>();
            for (final NeTypeJobProperty neTypeJobProperty : jobInfo.getNETypeJobProperties()) {
                final Map<String, Object> neTypeMapProperty = new HashMap<String, Object>();
                neTypeMapProperty.put(ShmConstants.NETYPE, neTypeJobProperty.getNeType());
                if (jobInfo.getJobType().equals(JobTypeEnum.BACKUP)) {
                    validateAndEncryptPasswordProperty(neTypeJobProperty);
                }
                neTypeMapProperty.put(ShmConstants.JOBPROPERTIES, neTypeJobProperty.getJobProperties());
                neTypeJobProperties.add(neTypeMapProperty);
            }
            jobConfiguration.put(ShmConstants.NETYPEJOBPROPERTIES, neTypeJobProperties);

        }
    }

    /**
     * @param neTypeJobProperty
     * @return 
     */
    private void validateAndEncryptPasswordProperty(final NeTypeJobProperty neTypeJobProperty) {
        final String password = JobPropertyUtil.getProperty(neTypeJobProperty.getJobProperties(), JobPropertyConstants.SECURE_BACKUP_KEY);
        if (password != null && !password.isEmpty()) {
            axeBackupJobAdditionalDataProvider.readAndEncryptPasswordProperty(neTypeJobProperty.getJobProperties(), password);
        } else {
            neTypeJobProperty.getJobProperties().removeIf(neTypeJobProp -> neTypeJobProp.containsValue(JobPropertyConstants.SECURE_BACKUP_KEY));

        }
    }

    /**
     * @param jobInfo
     * @param jobConfiguration
     */
    private void updateJobConfigWithNeTypeActivityJobProperties(final JobInfo jobInfo, final Map<String, Object> jobConfiguration) {
        if ((jobInfo.getNeTypeActivityJobProperties() != null) && !(jobInfo.getNeTypeActivityJobProperties().isEmpty())) {

            final List<Map<String, Object>> neTypeUpActivityProperties = new ArrayList<>();
            for (final NeTypeActivityJobProperties neTypeSwpUpActivityProperties : jobInfo.getNeTypeActivityJobProperties()) {
                final Map<String, Object> activityProperty = new HashMap<>();
                activityProperty.put(ShmConstants.NETYPE, neTypeSwpUpActivityProperties.getNeType());
                activityProperty.put(ShmConstants.ACTIVITYJOB_PROPERTIES, neTypeSwpUpActivityProperties.getActivityJobProperties());
                neTypeUpActivityProperties.add(activityProperty);
            }
            jobConfiguration.put(ShmConstants.NETYPE_ACTIVITYJOBPROPERTIES, neTypeUpActivityProperties);

        }
    }

    /**
     * @param jobInfo
     * @param selectedNEs
     */
    private void updateSelectedNEsWithComponentInfo(final JobInfo jobInfo, final Map<String, Object> selectedNEs) {
        if ((jobInfo.getParentNeWithComponents() != null) && !(jobInfo.getParentNeWithComponents().isEmpty())) {
            final List<Map<String, Object>> neNamesWithCompos = new ArrayList<>();
            for (final NeNamesWithSelectedComponents neNamesWithComponents : jobInfo.getParentNeWithComponents()) {
                final Map<String, Object> neNamesVsComponents = new HashMap<>();
                neNamesVsComponents.put(ShmConstants.NE_NAME, neNamesWithComponents.getParentNeName());
                neNamesVsComponents.put(ShmConstants.SELECTED_COMPONENTS, neNamesWithComponents.getSelectedComponents());
                neNamesWithCompos.add(neNamesVsComponents);
            }
            selectedNEs.put(ShmConstants.NE_WITH_COMPONENT_INFO, neNamesWithCompos);
        }
    }

    private void updateNeTypeComponentActivityDetails(final JobInfo jobInfo, final Map<String, Object> selectedNEs) {
        final List<NeTypeComponentActivityDetails> neTypeComponentActivityDetails = jobInfo.getNeTypeComponentActivityDetails();
        if ((neTypeComponentActivityDetails != null) && !(neTypeComponentActivityDetails.isEmpty())) {
            final List<Map<String, Object>> neTypeComponentActivities = new ArrayList<>();
            for (NeTypeComponentActivityDetails neTypeComponentActivity : neTypeComponentActivityDetails) {
                final Map<String, Object> neTypeComponentActivityMap = new HashMap<>();
                neTypeComponentActivityMap.put(ShmConstants.NETYPE, neTypeComponentActivity.getNeType());
                final List<Map<String, Object>> componentActivityList = getComponentActivties(neTypeComponentActivity.getComponentActivities());
                neTypeComponentActivityMap.put(ShmConstants.COMPONENT_ACTIVITIES, componentActivityList);
                neTypeComponentActivities.add(neTypeComponentActivityMap);
            }
            selectedNEs.put(ShmConstants.NETYPE_COMPONENT_ACTIVITYDETAILS, neTypeComponentActivities);
        }
        LOGGER.debug("In fillJobTemplateDetailsMap updateNeTypeComponentActivityDetails selectedNEs {}", selectedNEs);
    }

    private List<Map<String, Object>> getComponentActivties(final List<ComponentActivity> componentActivities) {
        final List<Map<String, Object>> componentActivityList = new ArrayList<>();
        for (ComponentActivity componentActivity : componentActivities) {
            final Map<String, Object> componentActivityMap = new HashMap<>();
            final List<String> activityNameList = new ArrayList<>();
            componentActivityMap.put(ShmConstants.COMPONENT_NAME, componentActivity.getComponentName());
            for (String activityName : componentActivity.getActivityNames()) {
                activityNameList.add(activityName);
            }
            componentActivityMap.put(ShmConstants.ACTIVITY_NAMES, activityNameList);
            componentActivityList.add(componentActivityMap);
        }
        return componentActivityList;
    }

    /**
     * Validates Scheduling Property
     * 
     * @param jobInfo
     * @return Map<String, Object>
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> schedulingAttributesValidation(final JobInfo jobInfo) {
        final List<ScheduleProperty> schedulePropertyList = new ArrayList<ScheduleProperty>();
        final Map<String, Object> response = new HashMap<String, Object>();
        final List<Map<String, Object>> scheduleAttributes = (List<Map<String, Object>>) jobInfo.getMainSchedule()
                .get(ShmConstants.SCHEDULINGPROPERTIES);
        for (final Map<String, Object> schedules : scheduleAttributes) {
            final ScheduleProperty scheduleProperty = new ScheduleProperty();
            scheduleProperty.setName((String) schedules.get(ShmConstants.NAME));
            scheduleProperty.setValue((String) schedules.get(ShmConstants.VALUE));
            schedulePropertyList.add(scheduleProperty);
        }
        if (JobTypeEnum.UPGRADE.equals(jobInfo.getJobType())) {
            for (final ScheduleProperty scheduleProperty : schedulePropertyList) {
                if (scheduleProperty.getName().equalsIgnoreCase(ShmConstants.REPEAT_TYPE)) {
                    response.put("errorCode",
                            JobHandlerErrorCodes.JOB_CFGN_PERSISTENCE_FAILED.getResponseDescription() + ". " + ShmConstants.ERROR_MSG);
                    return response;
                }
            }
        }
        return response;
    }

}
