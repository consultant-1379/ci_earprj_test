package com.ericsson.oss.services.shm.jobs.common.mapper;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.ACTIVITY_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.jobs.common.constants.JobConfigurationConstants.*;
import static com.ericsson.oss.services.shm.jobs.common.constants.JobSchedulerConstants.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import javax.ejb.TransactionAttribute;
import javax.inject.Inject;
import javax.ejb.TransactionAttributeType;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.FilterUtils;
import com.ericsson.oss.services.shm.common.enums.JobStateEnum;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.jobs.common.api.ActivityDetails;
import com.ericsson.oss.services.shm.jobs.common.api.ActivityJobDetail;
import com.ericsson.oss.services.shm.jobs.common.api.JobReportDetails;
import com.ericsson.oss.services.shm.jobs.common.api.NeDetails;
import com.ericsson.oss.services.shm.jobs.common.api.NeJobDetails;
import com.ericsson.oss.services.shm.jobs.common.api.NeJobInput;
import com.ericsson.oss.services.shm.jobs.common.api.ShmJobDetailsFilter;
import com.ericsson.oss.services.shm.jobs.common.constants.JobConfigurationConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.JobModelConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.*;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;


@SuppressWarnings("unchecked")
public class JobMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobMapper.class);
    private static final String SUCCESS = "SUCCESS";
    private static final String SKIPPED = "SKIPPED";
    private static final String CANCELLED = "CANCELLED";

    @Inject
    private JobLogMapper jobLogMapper;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    public ShmJobs getSHMJobsDetails(final List<Map<String, Object>> response, final ShmJobs shmJobs) {
        for (final Map<String, Object> mainJobPo : response) {

            LOGGER.trace("Main Job persistence object : {}", mainJobPo);

            final Job job = new Job();

            job.setId((long) mainJobPo.get(ShmConstants.PO_ID));
            job.setProgressPercentage((double) mainJobPo.get(ShmConstants.PROGRESSPERCENTAGE));
            job.setResult((String) mainJobPo.get(ShmConstants.RESULT));
            job.setStartTime((Date) mainJobPo.get(ShmConstants.STARTTIME));
            job.setEndTime((Date) mainJobPo.get(ShmConstants.ENDTIME));
            job.setState((String) mainJobPo.get(ShmConstants.STATE));
            job.setNumberOfNetworkElements((int) mainJobPo.get(ShmConstants.NO_OF_NETWORK_ELEMENTS));

            final List<Map<String, Object>> commentList = (List<Map<String, Object>>) mainJobPo.get(JobModelConstants.JOB_COMMENT);
            job.setComment(commentList);

            final long jobId = (long) mainJobPo.get(ShmConstants.JOBTEMPLATEID);
            if (shmJobs.getJobDetailsMap() != null && shmJobs.getJobDetailsMap().size() != 0 && shmJobs.getJobDetailsMap().containsKey(jobId)) {
                shmJobs.getJobDetailsMap().get(mainJobPo.get(ShmConstants.JOBTEMPLATEID)).getJobList().add(job);
            } else {
                final JobDetails jobDetails = new JobDetails();
                jobDetails.setId(jobId);
                final List<Job> jobList = new ArrayList<Job>();
                jobList.add(job);
                jobDetails.setJobList(jobList);
                shmJobs.getJobDetailsMap().put(jobDetails.getId(), jobDetails);
            }
        }
        return shmJobs;
    }

    public ShmJobs getJobConfigurationDetails(final Map<Long, Map<String, Object>> jobConfigurationAttributesHolder, final ShmJobs shmJobs) {

        for (final Entry<Long, Map<String, Object>> jobTemplateIdDetails : jobConfigurationAttributesHolder.entrySet()) {

            final JobDetails jobDetails = shmJobs.getJobDetailsMap().get(jobTemplateIdDetails.getKey());
            if (jobDetails != null) {
                final Map<String, Object> jobTemplateAttributes = jobTemplateIdDetails.getValue();
                final String name = (String) jobTemplateAttributes.get("name");
                final String jobType = (String) jobTemplateAttributes.get("jobType");
                final String owner = (String) jobTemplateAttributes.get("owner");
                final Date creationDate = (Date) jobTemplateAttributes.get("creationTime");
                String jobCategory = (String) jobTemplateAttributes.get("jobCategory");

                List<String> collectionNames = null;
                List<String> neNames = new ArrayList<String>();
                Schedule schedule = null;

                if (jobTemplateAttributes.get(ShmConstants.JOBCONFIGURATIONDETAILS) != null) {
                    final Map<String, Object> jobConfigurationdetails = (Map<String, Object>) jobTemplateAttributes.get(ShmConstants.JOBCONFIGURATIONDETAILS);

                    if (jobConfigurationdetails.get(SELECTED_NES) != null) {
                        final Map<String, Object> neInfo = (Map<String, Object>) jobConfigurationdetails.get(SELECTED_NES);

                        neNames = null;
                        if (neInfo != null) {
                            collectionNames = (List<String>) neInfo.get(COLLECTION_NAMES);
                            neNames = (List<String>) neInfo.get(NENAMES);
                        }
                    }

                    final Map<String, Object> scheduleMap = (Map<String, Object>) jobConfigurationdetails.get(MAIN_SCHEDULE);
                    schedule = getSchedule(scheduleMap);

                }
                final JobTemplate jobTemplate = new JobTemplate();
                final JobConfiguration jobConfiguration = new JobConfiguration();
                final NEInfo neInfo = new NEInfo();
                neInfo.setCollectionNames(collectionNames);
                neInfo.setNeNames(neNames);
                jobConfiguration.setSelectedNEs(neInfo);
                jobConfiguration.setMainSchedule(schedule);
                jobTemplate.setName(name);
                jobTemplate.setJobType(JobType.valueOf(jobType));
                jobTemplate.setOwner(owner);
                jobTemplate.setJobConfigurationDetails(jobConfiguration);
                jobTemplate.setCreationTime(creationDate);
                //handling null values for backward comparability
                jobCategory = (jobCategory != null) ? jobCategory : JobCategory.UI.getAttribute();
                jobTemplate.setJobCategory(JobCategory.valueOf(jobCategory));
                jobDetails.setJobTemplate(jobTemplate);
            }
        }
        return shmJobs;
    }

    /**
     * Method to filter and update the fields to <code>JobConfiguration</code> from given <code>Response</code>
     * 
     * @param jobTemplateAttributes
     * @return
     */
    public JobTemplate getJobTemplateDetails(final Map<String, Object> jobTemplateAttributes, final Long jobTemplateId) {
        final String jobName = (String) jobTemplateAttributes.get(JOB_NAME);
        final JobType jobType = JobType.valueOf((String) jobTemplateAttributes.get(JOB_TYPE));
        final String owner = (String) jobTemplateAttributes.get(OWNER);
        final Date creationTime = (Date) jobTemplateAttributes.get(CREATION_TIME);

        JobCategory jobCategory;
        if (jobTemplateAttributes.get(JOB_CATEGORY) != null) {
            jobCategory = JobCategory.valueOf((String) jobTemplateAttributes.get(JOB_CATEGORY));
        } else {
            jobCategory = JobCategory.UI;
        }

        final String description = (String) jobTemplateAttributes.get(DESCRIPTION);
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) jobTemplateAttributes.get(JOB_CONFIGURATIONDETAILS);

        final JobTemplate jobTemplate = new JobTemplate();
        jobTemplate.setCreationTime(creationTime);
        jobTemplate.setDescription(description);
        jobTemplate.setName(jobName);
        jobTemplate.setJobType(jobType);
        jobTemplate.setOwner(owner);
        jobTemplate.setJobCategory(jobCategory);
        jobTemplate.setJobConfigurationDetails(getJobConfigurationDetails(jobConfigurationDetails));
        jobTemplate.setJobTemplateId(jobTemplateId);

        LOGGER.debug("Returning from mapper as {}", jobTemplate);
        return jobTemplate;
    }

    /**
     * Maps the data to JobConfiguration
     * 
     * @param jobConfigurationDetailsMap
     * @return JobConfiguration
     */
    public JobConfiguration getJobConfigurationDetails(final Map<String, Object> jobConfigurationDetailsMap) {
        final JobConfiguration jobConfigurationDetails = new JobConfiguration();
        if (jobConfigurationDetailsMap != null) {
            final List<Map<String, Object>> jobPropertiesList = (List<Map<String, Object>>) jobConfigurationDetailsMap.get(ShmConstants.JOBPROPERTIES);
            final List<Map<String, Object>> neTypeJobPropertiesMap = (List<Map<String, Object>>) jobConfigurationDetailsMap.get(ShmConstants.NETYPEJOBPROPERTIES);
            final List<Map<String, Object>> platformJobPropertiesMap = (List<Map<String, Object>>) jobConfigurationDetailsMap.get(ShmConstants.PLATFORMJOBPROPERTIES);
            final Map<String, Object> neInfo = (Map<String, Object>) jobConfigurationDetailsMap.get(SELECTED_NES);
            final Map<String, Object> mainScheduleMap = (Map<String, Object>) jobConfigurationDetailsMap.get(MAIN_SCHEDULE);
            final List<Map<String, Object>> activitiesMap = (List<Map<String, Object>>) jobConfigurationDetailsMap.get(ACTIVITIES);
            LOGGER.debug("activitiesMap in JobMapper {}", activitiesMap);
            final List<Map<String, Object>> neJobPropertiesMap = (List<Map<String, Object>>) jobConfigurationDetailsMap.get(ShmConstants.NEJOB_PROPERTIES);
            List<Map<String, Object>> neTypeActivityJobPropertiesMap = null;
            if (jobConfigurationDetailsMap.get(ShmConstants.NETYPE_ACTIVITYJOBPROPERTIES) != null) {
                neTypeActivityJobPropertiesMap = (List<Map<String, Object>>) jobConfigurationDetailsMap.get(ShmConstants.NETYPE_ACTIVITYJOBPROPERTIES);
            }

            jobConfigurationDetails.setActivities(getActivities(activitiesMap));
            jobConfigurationDetails.setJobProperties(getJobProperties(jobPropertiesList));
            jobConfigurationDetails.setNeTypeJobProperties(getNETypeJobProperties(neTypeJobPropertiesMap));
            jobConfigurationDetails.setPlatformJobProperties(getPlatformJobProperties(platformJobPropertiesMap));
            jobConfigurationDetails.setMainSchedule(getSchedule(mainScheduleMap));
            jobConfigurationDetails.setSelectedNEs(getNEInfo(neInfo));
            jobConfigurationDetails.setNeJobProperties(getNEJobProperties(neJobPropertiesMap));
            if (neTypeActivityJobPropertiesMap != null && !neTypeActivityJobPropertiesMap.isEmpty()) {
                jobConfigurationDetails.setNeTypeActivityJobProperties(getNeTypeActivityJobProperties(neTypeActivityJobPropertiesMap));
            }
        }
        return jobConfigurationDetails;
    }

    private List<NEJobProperty> getNEJobProperties(final List<Map<String, Object>> neJobPropertiesMap) {
        final List<NEJobProperty> neJobPropertiesList = new ArrayList<NEJobProperty>();
        if (neJobPropertiesMap != null) {
            for (final Map<String, Object> neJobProperty : neJobPropertiesMap) {
                final String neName = (String) neJobProperty.get(ShmConstants.NE_NAME);
                final List<JobProperty> jobPropertiesList = getJobProperties((List<Map<String, Object>>) neJobProperty.get(ShmConstants.JOBPROPERTIES));
                final NEJobProperty neJProperty = new NEJobProperty();
                neJProperty.setJobProperties(jobPropertiesList);
                neJProperty.setNeName(neName);
                neJobPropertiesList.add(neJProperty);
            }
        }
        return neJobPropertiesList;
    }

    private List<NeTypeActivityJobProperties> getNeTypeActivityJobProperties(final List<Map<String, Object>> neTypeActivityJobPropertiesMap) {
        final List<NeTypeActivityJobProperties> neTypeActivityJobProps = new ArrayList<>();
        if (neTypeActivityJobPropertiesMap != null) {
            for (final Map<String, Object> neTypeActivityProps : neTypeActivityJobPropertiesMap) {
                final NeTypeActivityJobProperties neTypeActivityJobProperties = new NeTypeActivityJobProperties();
                final String neType = (String) neTypeActivityProps.get(ShmConstants.NETYPE);
                final List<Map<String, Object>> activityJobProperties = (List<Map<String, Object>>) neTypeActivityProps.get(ShmConstants.ACTIVITYJOB_PROPERTIES);
                neTypeActivityJobProperties.setNeType(neType);
                neTypeActivityJobProperties.setActivityJobProperties(activityJobProperties);
                neTypeActivityJobProps.add(neTypeActivityJobProperties);
            }
        }
        return neTypeActivityJobProps;
    }

    private List<NeTypeJobProperty> getNETypeJobProperties(final List<Map<String, Object>> neTypeJobPropertiesMap) {
        final List<NeTypeJobProperty> neJobPropertiesList = new ArrayList<NeTypeJobProperty>();
        if (neTypeJobPropertiesMap != null) {
            for (final Map<String, Object> neJobProperty : neTypeJobPropertiesMap) {
                final String neType = (String) neJobProperty.get(ShmConstants.NETYPE);
                final List<JobProperty> jobPropertiesList = getJobProperties((List<Map<String, Object>>) neJobProperty.get(ShmConstants.JOBPROPERTIES));
                final NeTypeJobProperty neTypeJProperty = new NeTypeJobProperty();
                neTypeJProperty.setJobProperties(jobPropertiesList);
                neTypeJProperty.setNeType(neType);
                neJobPropertiesList.add(neTypeJProperty);
            }
        }
        return neJobPropertiesList;
    }

    private List<PlatformJobProperty> getPlatformJobProperties(final List<Map<String, Object>> platformJobPropertiesMap) {
        final List<PlatformJobProperty> platformJobPropertiesList = new ArrayList<PlatformJobProperty>();
        if (platformJobPropertiesMap != null) {
            for (final Map<String, Object> platformJobProperty : platformJobPropertiesMap) {
                final String platform = (String) platformJobProperty.get(ShmConstants.PLATFORM);
                final List<JobProperty> jobPropertiesList = getJobProperties((List<Map<String, Object>>) platformJobProperty.get(ShmConstants.JOBPROPERTIES));
                final PlatformJobProperty platformJProperty = new PlatformJobProperty();
                platformJProperty.setJobProperties(jobPropertiesList);
                platformJProperty.setPlatform(platform);
                platformJobPropertiesList.add(platformJProperty);
            }
        }
        return platformJobPropertiesList;
    }

    private List<JobProperty> getJobProperties(final List<Map<String, Object>> jobPropertiesList) {
        final List<JobProperty> jobProperties = new ArrayList<JobProperty>();
        if (jobPropertiesList != null) {
            for (final Map<String, Object> jobPropertyMap : jobPropertiesList) {
                final String key = (String) jobPropertyMap.get(KEY);
                final String value = (String) jobPropertyMap.get(VALUE);
                final JobProperty jobProperty = new JobProperty(key, value);

                jobProperties.add(jobProperty);
            }
        }
        return jobProperties;
    }

    private List<Activity> getActivities(final List<Map<String, Object>> activitiesList) {
        final List<Activity> activities = new ArrayList<Activity>();
        if (activitiesList != null) {
            for (final Map<String, Object> activityMap : activitiesList) {
                final Activity activity = new Activity();
                LOGGER.debug("activitiesMap in getActivities {}", activityMap);
                activity.setName((String) activityMap.get(NAME));
                activity.setOrder((int) activityMap.get(ORDER));
                final PlatformTypeEnum pltaform = PlatformTypeEnum.valueOf((String) activityMap.get(PLATFORM));
                activity.setPlatform(pltaform);
                final Map<String, Object> scheduleMap = (Map<String, Object>) activityMap.get(SCHEDULE);
                activity.setSchedule(getSchedule(scheduleMap));
                activity.setNeType((String) activityMap.get(NETYPE));
                activities.add(activity);
            }
        }
        return activities;
    }

    private Schedule getSchedule(final Map<String, Object> scheduleMap) {
        final Schedule schedule = new Schedule();
        if (scheduleMap != null) {
            final ExecMode execMode = ExecMode.valueOf((String) scheduleMap.get(EXEC_MODE));
            final List<Map<String, Object>> schedulePropertyList = (List<Map<String, Object>>) scheduleMap.get(SCHEDULE_ATTRIBUTES);
            final List<ScheduleProperty> scheduleAttributes = new ArrayList<ScheduleProperty>();
            if (schedulePropertyList != null) {
                for (final Map<String, Object> schedulePropertyMap : schedulePropertyList) {
                    final ScheduleProperty scheduleProperty = new ScheduleProperty();
                    scheduleProperty.setName((String) schedulePropertyMap.get(NAME));
                    scheduleProperty.setValue((String) schedulePropertyMap.get(VALUE));
                    scheduleAttributes.add(scheduleProperty);
                }
            }
            schedule.setExecMode(execMode);
            schedule.setScheduleAttributes(scheduleAttributes);
        }
        return schedule;
    }

    private NEInfo getNEInfo(final Map<String, Object> neInfoMap) {
        final NEInfo neInfo = new NEInfo();
        if (neInfoMap != null) {
            neInfo.setCollectionNames((List<String>) neInfoMap.get(COLLECTION_NAMES));
            neInfo.setSavedSearchIds((List<String>) neInfoMap.get(ShmConstants.SAVED_SEARCH_IDS));
            neInfo.setNeNames((List<String>) neInfoMap.get(NENAMES));
            if (neInfoMap.get(NE_WITH_COMPONENT_INFO) != null) {
                neInfo.setNeWithComponentInfo((List<Map<String, Object>>) neInfoMap.get(NE_WITH_COMPONENT_INFO));
            }
            if (neInfoMap.get(ShmConstants.NETYPE_COMPONENT_ACTIVITYDETAILS) != null) {
                final List<NeTypeComponentActivityDetails> neTypeComponentActivityDetailsList = getNeTypeComponentActivities(
                        (List<Map<String, Object>>) neInfoMap.get(ShmConstants.NETYPE_COMPONENT_ACTIVITYDETAILS));
                neInfo.setNeTypeComponentActivityDetails(neTypeComponentActivityDetailsList);
            }
        }
        LOGGER.debug("In JobMapper getNEInfo - neInfo {}", neInfo);
        return neInfo;
    }

    private List<NeTypeComponentActivityDetails> getNeTypeComponentActivities(final List<Map<String, Object>> listOfActivitySchedules) {
        final List<NeTypeComponentActivityDetails> neTypeComponentActivityDetailsList = new ArrayList<>();
        for (final Map<String, Object> activityMap : listOfActivitySchedules) {
            if (activityMap.containsKey(NETYPE)) {
                final NeTypeComponentActivityDetails neTypeComponentActivityDetails = new NeTypeComponentActivityDetails();
                neTypeComponentActivityDetails.setNeType((String) activityMap.get(NETYPE));
                final List<ComponentActivity> componentActivitiesList = new ArrayList<>();
                final List<Map<String, Object>> componentsActivityDataList = (List<Map<String, Object>>) activityMap.get(ShmConstants.COMPONENT_ACTIVITIES);
                for (Map<String, Object> componentsActivityDataMap : componentsActivityDataList) {
                    final ComponentActivity componentActivity = new ComponentActivity();
                    componentActivity.setComponentName((String) componentsActivityDataMap.get(ShmConstants.COMPONENT_NAME));
                    componentActivity.setActivityNames((List<String>) componentsActivityDataMap.get(ShmConstants.ACTIVITY_NAMES));
                    componentActivitiesList.add(componentActivity);
                }
                neTypeComponentActivityDetails.setComponentActivities(componentActivitiesList);
                neTypeComponentActivityDetailsList.add(neTypeComponentActivityDetails);
            }
        }
        return neTypeComponentActivityDetailsList;
    }

    /**
     * To Prepare the complete data to be displayed at Nelevel Jobs Page It takes care of activity sorting based on its order attribute
     * 
     * @param mainJobPo
     *            -To be displayed at overview section
     * @param jobType
     *            - Ne level Job Data to be displayed for a particular job type
     * @param argumentMap
     *            - A map that contains the matchedNeJobs,Activities data to be displayed at activity overview section and jobInput
     * @return - JobReportData
     */
    public NeDetails getJobReportRefined(final Map<String, Object> jobPOAttributes, final String jobType, final Map<String, Object> argumentMap) {

        final NeJobInput jobInput = (NeJobInput) argumentMap.get(ShmConstants.JOBINPUT);
        final NeDetails neDetails = new NeDetails();

        final List<NeJobDetails> neJobsDataList = getNeJobDetails(jobPOAttributes, argumentMap);

        if (jobInput.getFilterDetails() != null && !jobInput.getFilterDetails().isEmpty()) {
            filterShmJobDetails(neJobsDataList, jobInput);
        }
        neDetails.setClearOffset(FilterUtils.isClearOffsetRequired(neJobsDataList.size(), jobInput.getOffset()));
        if (ShmConstants.ASENDING.equals(jobInput.getOrderBy())) {
            ascendingNeJob(neJobsDataList, jobInput);
        } else if (ShmConstants.DESENDING.equals(jobInput.getOrderBy())) {
            descendingNeJob(neJobsDataList, jobInput);
        }
        final List<NeJobDetails> resultList = getPaginationResult(jobInput, neJobsDataList);
        LOGGER.debug("In ne level job details view neJobsDataList size:{}", neJobsDataList.size());
        neDetails.setTotalCount(neJobsDataList.size());
        neDetails.setResult(new ArrayList<NeJobDetails>(resultList));

        final List<Map<String, Object>> allNeJobsDataWithCustomColumns = getAllNeJobsDataWithCustomColumns(jobInput, neJobsDataList);
        neDetails.setNeDetailsWithCustomColumns(allNeJobsDataWithCustomColumns);
        return neDetails;
    }

    private List<NeJobDetails> getNeJobDetails(final Map<String, Object> jobPOAttributes, final Map<String, Object> argumentMap) {

        final List<Map<String, Object>> matchedNeJobs = (List<Map<String, Object>>) argumentMap.get(ShmConstants.MATCHED_NE_JOB_ATTRIBUTES);
        final Map<Long, List<ActivityJobDetail>> activityResponseList = (HashMap<Long, List<ActivityJobDetail>>) argumentMap.get(ShmConstants.ACTIVITY_RESPONSE_LIST);

        LOGGER.debug("List of activities recieved are {} ,machedNEJobs size:{}", activityResponseList.size(), matchedNeJobs.size());
        final Map<String, Object> neJobPropertiesMap = prepareNeJobPropertiesMap((Map<String, Object>) jobPOAttributes.get(JOB_CONFIGURATIONDETAILS));
        final List<NeJobDetails> neJobsDataList = new ArrayList<NeJobDetails>();

        for (final Map<String, Object> neJob : matchedNeJobs) {
            final NeJobDetails neJobDetails = new NeJobDetails();
            final List<ActivityDetails> activityDetailsList = new ArrayList<ActivityDetails>();
            final List<ActivityDetails> activityDetailsListRunning = new ArrayList<ActivityDetails>();
            final ActivityDetails activityDetailsRunning = new ActivityDetails();
            final List<ActivityDetails> activityDetailsListCancel = new ArrayList<ActivityDetails>();
            final ActivityDetails activityDetailsCancel = new ActivityDetails();
            final List<ActivityDetails> activityDetailsListWait = new ArrayList<ActivityDetails>();
            final ActivityDetails activityDetailsWait = new ActivityDetails();
            final String nodeName = setNodeDetails(neJobPropertiesMap, (Long) neJob.get(ShmConstants.PO_ID), neJob, neJobDetails);

            setDate(neJobDetails, (Date) neJob.get(ShmConstants.NE_START_DATE), (Date) neJob.get(ShmConstants.NE_END_DATE));
            final List<JobLogDetails> jobLogList = new ArrayList<JobLogDetails>();
            final List<String> lastLogList = new ArrayList<String>();

            List<ActivityJobDetail> activityJobDetails = activityResponseList.get((Long) neJob.get(ShmConstants.PO_ID));
            activityJobDetails = ((activityJobDetails == null) ? new ArrayList<ActivityJobDetail>() : activityJobDetails);

            for (final ActivityJobDetail activityJob : activityJobDetails) {

                try {
                    final ActivityDetails activityDetails = new ActivityDetails();
                    activityDetails.setActivityJobIdAsString(activityJob.getActivityJobIdAsString());
                    final Long activityNeJobId = activityJob.getNeJobId();
                    final String activityName = activityJob.getActivityName();
                    activityDetails.setActivityName(activityName);
                    final int activityOrder = activityJob.getActivityOrder();
                    final String activityState = activityJob.getState();
                    activityDetails.setActivityOrder(activityOrder);
                    activityDetailsList.add(activityDetails);
                    LOGGER.debug("activityDetails---activityNeJobId:{},activityName:{},activityOrder:{},activityState:{}", activityNeJobId, activityName, activityOrder, activityState);
                    if (neJob.get(ShmConstants.NE_PROG_PERCENTAGE).equals(100)) {
                        neJobDetails.setNeActivity(" ");
                    } else if (activityState.equals(JobState.RUNNING.name())) {
                        activityDetailsRunning.setActivityName(activityName);
                        activityDetailsRunning.setActivityOrder(activityOrder);
                        activityDetailsListRunning.add(activityDetailsRunning);
                        LOGGER.debug("activityDetailsListRunning .. {}", activityDetailsListRunning);
                    } else if (activityState.equals(JobState.CANCELLING.name()) || activityState.equals(JobState.SYSTEM_CANCELLING.name())) {
                        activityDetailsCancel.setActivityName(activityName);
                        activityDetailsCancel.setActivityOrder(activityOrder);
                        activityDetailsListCancel.add(activityDetailsCancel);
                        LOGGER.debug("activityDetailsListCancel .. {}", activityDetailsListCancel);
                    } else if (!isNeJobCancelled(neJobDetails) && (activityState.equals(JobState.WAIT_FOR_USER_INPUT.name()) || activityState.equals(JobState.SCHEDULED.name()))) {
                        activityDetailsWait.setActivityName(activityName);
                        activityDetailsWait.setActivityOrder(activityOrder);
                        activityDetailsListWait.add(activityDetailsWait);
                        LOGGER.debug("activityDetailsListWait .. {}", activityDetailsListWait);
                        neJobDetails.setNeStatus(activityState);
                    } else {
                        neJobDetails.setNeActivity(" ");
                    }
                    getActivityDetailsState(neJobDetails, activityDetailsListRunning, activityDetailsRunning, activityDetailsListCancel, activityDetailsCancel, activityDetailsListWait);
                    if (activityJob.getLastLogMessage() != null) {
                        lastLogList.add(activityJob.getLastLogMessage());
                    } else {
                        final Map<String, Object> jobAttributes = activityJob.getLog();
                        if (jobAttributes != null) {
                            jobAttributes.put(JOB_NAME, activityJob.getActivityName());
                        }
                        jobLogList.add(jobLogMapper.mapJobAttributesToJobLogDetails(jobAttributes));
                    }
                } catch (final Exception e) {
                    LOGGER.error("Exception while mapping the activity details from neJobDetails - {}", e.getMessage());
                }
            }

            if (neJob.get(ShmConstants.LAST_LOG_MESSAGE) != null) {
                lastLogList.add((String) neJob.get(ShmConstants.LAST_LOG_MESSAGE));
            } else {
                jobLogList.add(jobLogMapper.mapJobAttributesToJobLogDetails(neJob));
            }

            if (lastLogList != null && !lastLogList.isEmpty()) {
                neJobDetails.setLastLogMessage(getLastLogMessage(lastLogList));
            } else {
                neJobDetails.setLastLogMessage(getLastLogMessage(nodeName, jobLogList, neJobDetails.getNodeType()));
            }
            sortActivites(activityDetailsList);
            neJobDetails.setActivityDetailsList(activityDetailsList);
            neJobsDataList.add(neJobDetails);
        }
        return neJobsDataList;
    }

    private List<Map<String, Object>> getAllNeJobsDataWithCustomColumns(final NeJobInput jobInput, final List<NeJobDetails> neJobsDataList) {
        final List<Map<String, Object>> allNeJobsDataWithCustomColumns = new ArrayList<>();
        if (jobInput.isSelectAll()) {
            LOGGER.debug("Select all for job details data size {} ", neJobsDataList.size());
            for (final NeJobDetails neJobData : neJobsDataList) {
                final Map<String, Object> neJobsDataWithCustomColumns = new HashMap<>();
                neJobsDataWithCustomColumns.put(NE_JOBID, neJobData.getNeJobId());
                neJobsDataWithCustomColumns.put(NE_NODE_NAME, neJobData.getNeNodeName());
                neJobsDataWithCustomColumns.put(NE_STATUS, neJobData.getNeStatus());
                allNeJobsDataWithCustomColumns.add(neJobsDataWithCustomColumns);
            }
        }
        return allNeJobsDataWithCustomColumns;
    }

    private boolean isNeJobCancelled(final NeJobDetails neJobDetails) {
        return JobStateEnum.COMPLETED.name().equals(neJobDetails.getNeStatus());
    }

    private List<NeJobDetails> getPaginationResult(final NeJobInput jobInput, final List<NeJobDetails> neJobsDataList) {
        List<NeJobDetails> resultList = Collections.emptyList();
        int start = -1;
        int end = -1;
        if (jobInput.getOffset() <= neJobsDataList.size()) {
            start = jobInput.getOffset() - 1;
            if (jobInput.getLimit() > neJobsDataList.size()) {
                end = neJobsDataList.size();
            } else {
                end = jobInput.getLimit();
            }
        } else {
            if (jobInput.getFilterDetails() != null) {
                start = 0;
                end = neJobsDataList.size();
            }
        }
        if (start != -1 && end != -1) {
            resultList = neJobsDataList.subList(start, end);
        }
        return resultList;
    }

    /**
     * This method is used to get the last log of NE job and Activity Job (sorted by Entry Time) from the Last Log messages list
     * 
     * @param lastLogList
     */
    private String getLastLogMessage(final List<String> lastLogList) {
        final List<Map<String, String>> jobLogMapList = new ArrayList<Map<String, String>>();
        String lastLogMessage = "";
        for (final String jobLog : lastLogList) {
            if (jobLog != null && jobLog.contains(ShmConstants.DELIMITER_PIPE)) {
                final Map<String, String> jobLogMap = new HashMap<String, String>();
                final int lastIndexofDelimiterPipe = jobLog.lastIndexOf(ShmConstants.DELIMITER_PIPE);
                jobLogMap.put(ShmConstants.LAST_LOG_MESSAGE, jobLog.substring(0, lastIndexofDelimiterPipe));
                jobLogMap.put(ShmConstants.ENTRY_TIME, jobLog.substring(lastIndexofDelimiterPipe + ShmConstants.DELIMITER_PIPE.length()));
                jobLogMapList.add(jobLogMap);
            }
        }

        if (!jobLogMapList.isEmpty()) {
            Collections.sort(jobLogMapList, new Comparator<Map<String, String>>() {

                @Override
                public int compare(final Map<String, String> map1, final Map<String, String> map2) {
                    final Date date1 = new Date(Long.parseLong(map1.get(ShmConstants.ENTRY_TIME)));
                    final Date date2 = new Date(Long.parseLong(map2.get(ShmConstants.ENTRY_TIME)));
                    return date1.compareTo(date2);
                }
            });
            lastLogMessage = jobLogMapList.get(jobLogMapList.size() - 1).get(ShmConstants.LAST_LOG_MESSAGE);
        }

        return lastLogMessage;
    }

    /**
     * This method is used to get the last log of NE job and Activity Job from the Job Logs List
     * 
     * @param nodeName
     * @param jobLogList
     */
    private String getLastLogMessage(final String nodeName, final List<JobLogDetails> jobLogList, final String nodeType) {
        String lastLogMessage = "";
        final NeJobLogDetails neJobLogDetails = jobLogMapper.mapNEJobLogDetailsFromJobLogDetails(jobLogList, nodeName, nodeType);
        final List<JobLogResponse> jobLogResponseForEachNode = jobLogMapper.getNEJobLogResponse(neJobLogDetails);
        if (jobLogResponseForEachNode != null && !jobLogResponseForEachNode.isEmpty()) {
            sortJobLogResponseOnEntryTime(jobLogResponseForEachNode);
            lastLogMessage = jobLogResponseForEachNode.get(0).getMessage();
        }
        return lastLogMessage;
    }
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<ActivityDetails> getJobActivityDetails(final List<Map<String, Object>> activityResponseList, final Long neJobId) {
    LOGGER.debug("List of activities recieved are in getJobActivityDetails : {} size {}", activityResponseList, activityResponseList.size());
        final List<ActivityDetails> activityJobsDataList = new ArrayList<ActivityDetails>();
        for (final Map<String, Object> activityJob : activityResponseList) {
            final ActivityDetails activityDetails = new ActivityDetails();
            final String activityName = (String) activityJob.get(ShmConstants.ACTIVITY_NAME);
            final Object actResult = activityJob.get(ShmConstants.ACTIVITY_RESULT);
            setActivityState(activityJob, activityDetails, actResult);
            final Date actEndDate = (Date) activityJob.get(ShmConstants.ACTIVITY_END_DATE);
            final Date actStartDate = (Date) activityJob.get(ShmConstants.ACTIVITY_START_DATE);
            final int activityOrder = (int) activityJob.get(ShmConstants.ACTIVITY_ORDER);
            activityDetails.setActivityName(activityName);
            String activityScheduleTime = null;
            if (activityJob.get(ShmConstants.START_DATE) != null) {
                activityScheduleTime = getFormattedDate((String) activityJob.get(ShmConstants.START_DATE));
            }
            activityDetails.setActivityScheduleTime(activityScheduleTime);
            activityDetails.setActivitySchedule((String) activityJob.get(ShmConstants.EXECUTION_MODE));
            setDate(activityDetails, actEndDate, actStartDate);
            activityDetails.setActivityJobIdAsString(String.valueOf(activityJob.get(ShmConstants.PO_ID)));
            Double activityProgressPercentage = (Double) activityJob.get(ShmConstants.PROGRESSPERCENTAGE);
            final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
            final String activityResult = activityDetails.getActivityResult();
            if (activityResult != null && !(ActivityConstants.EMPTY.equals(activityResult))) {
                if (activityProgressPercentage == null || activityProgressPercentage == 0.0) {
                    switch (activityResult) {
                    case SUCCESS:
                    case SKIPPED:
                    case CANCELLED:
                        activityProgressPercentage = ACTIVITY_END_PROGRESS_PERCENTAGE;
                        activityJobAttributes.put(ShmConstants.PROGRESSPERCENTAGE, ACTIVITY_END_PROGRESS_PERCENTAGE);
                        jobUpdateService.updateJobAttributes((long) activityJob.get(ShmConstants.PO_ID), activityJobAttributes);
                        break;
                    default:
                        activityProgressPercentage = (Double) activityJob.get(ShmConstants.PROGRESSPERCENTAGE);
                        break;
                    }

                } else {
                    activityProgressPercentage = (Double) activityJob.get(ShmConstants.PROGRESSPERCENTAGE);
                }
            }
            activityDetails.setActivityProgress(activityProgressPercentage);
            activityDetails.setActivityOrder(activityOrder);
            final Map<String, Map<String, String>> activityConfiguration = (Map<String, Map<String, String>>) activityJob.get(ShmConstants.ACTIVITY_CONFIGURATION);
            activityDetails.setActivityConfiguration(activityConfiguration);
            activityJobsDataList.add(activityDetails);
        }
        sortActivites(activityJobsDataList);
        LOGGER.debug("activityJobsDataList in getJobActivityDetails : {} size {}", activityJobsDataList, activityJobsDataList.size());
        return activityJobsDataList;
    }

    /**
     * @param startTime
     * @return parsedDate
     */
    public String getFormattedDate(final String date) {
        final SimpleDateFormat dateFormatter = new SimpleDateFormat(ShmConstants.DATE_WITH_TIME_FORMAT_FOR_FILTER);
        String formattedDate = "";
        try {
            final String delims = " ";
            Date formattedTime = new Date();
            final StringTokenizer st = new StringTokenizer(date, delims);
            final String formattedScheduleTime = st.nextToken() + " " + st.nextToken();
            LOGGER.debug("formattedScheduleTime :{}", formattedScheduleTime);
            try {
                formattedTime = dateFormatter.parse(formattedScheduleTime);
                LOGGER.debug("startTime or endTime :{}", formattedTime);
            } catch (final ParseException e) {
                LOGGER.error("Cannot parse date. due to :", e);
            }
            if (formattedTime != null) {
                formattedDate = String.valueOf(formattedTime.getTime());
            } else {
                formattedDate = "";
            }
            LOGGER.debug("parsedDate:{}", formattedDate);
        } catch (final StringIndexOutOfBoundsException e) {
            LOGGER.error("StartTime does not contain Time Zone info. StartTime : {}", date);
        }
        return formattedDate;
    }

    /*
     * filter shm job details based on NeJobDetails information.
     */

    public void filterShmJobDetails(final List<NeJobDetails> neJobsDataList, final NeJobInput jobInput) {
        final long startTime = System.currentTimeMillis();
        for (final Iterator<NeJobDetails> iterator = neJobsDataList.iterator(); iterator.hasNext();) {
            final boolean isFilterValueMatched = ShmJobDetailsFilter.applyFilter(iterator.next(), jobInput.getFilterDetails());
            if (!isFilterValueMatched) {
                iterator.remove();
            }
        }
        LOGGER.debug("Size of NeJobDetails after filtering {} and total time taken for filtering : {} millis ", neJobsDataList.size(), (System.currentTimeMillis() - startTime));

    }

    private void setActivityState(final Map<String, Object> activityJob, final ActivityDetails activityDetails, final Object actResult) {
        final String activityState = (String) activityJob.get(ShmConstants.ACTIVITY_NE_STATUS);
        final int activityOrder = (int) activityJob.get(ShmConstants.ACTIVITY_ORDER);
        LOGGER.info("Activity state is: {}", activityState);

        if (actResult != null && ((String) actResult).equals(JobResult.CANCELLED.toString()) && activityOrder == 1 && JobState.isJobCancelled(JobState.getJobState(activityState))) {
            activityDetails.setActivityResult(JobState.COMPLETED.getJobStateName());
        } else if (actResult != null && !(((String) actResult)).isEmpty()) {
            activityDetails.setActivityResult((String) actResult);
        } else {
            if (JobState.isJobCancelled(JobState.getJobState(activityState))) {
                activityDetails.setActivityResult(activityState);
            } else if (JobState.isJobCancelled(JobState.getJobState((String) activityJob.get(ShmConstants.NE_JOB_STATE)))) {
                if (!activityState.equals(JobState.CREATED.getJobStateName())) {
                    activityDetails.setActivityResult(JobState.COMPLETED.getJobStateName());
                }
            } else {
                activityDetails.setActivityResult("");
            }
        }
    }

    private void ascendingNeJob(final List<NeJobDetails> neJobsDataList, final NeJobInput jobInput) {
        if (jobInput.getSortBy().equals(JobConfigurationConstants.NE_JOBID)) {
            Collections.sort(neJobsDataList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return neJobDetails1.getNeJobId().compareTo(neJobDetails2.getNeJobId());
                }
            });
        } else if (jobInput.getSortBy().equals(JobConfigurationConstants.NE_RESULT)) {
            Collections.sort(neJobsDataList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return performStringComparision(neJobDetails1.getNeResult(), neJobDetails2.getNeResult());
                }
            });
        } else if (jobInput.getSortBy().equals(JobConfigurationConstants.NE_NODE_NAME)) {
            Collections.sort(neJobsDataList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return neJobDetails1.getNeNodeName().compareTo(neJobDetails2.getNeNodeName());
                }
            });
        } else if (jobInput.getSortBy().equals(JobConfigurationConstants.NE_ACTIVITY)) {
            Collections.sort(neJobsDataList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return neJobDetails1.getNeActivity().compareTo(neJobDetails2.getNeActivity());
                }
            });
        } else if (jobInput.getSortBy().equals(JobConfigurationConstants.NE_PROGRESS)) {
            Collections.sort(neJobsDataList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    final Double progressPercentageValue1 = neJobDetails1.getNeProgress();
                    final Double progressPercentageValue2 = neJobDetails2.getNeProgress();
                    return progressPercentageValue1.compareTo(progressPercentageValue2);
                }
            });
        } else if (jobInput.getSortBy().equals(JobConfigurationConstants.NE_STATUS)) {
            Collections.sort(neJobsDataList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return neJobDetails1.getNeStatus().compareTo(neJobDetails2.getNeStatus());
                }
            });
        } else if (jobInput.getSortBy().equals(JobConfigurationConstants.NE_START_DATE)) {
            Collections.sort(neJobsDataList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    LOGGER.debug("Start Date 1::" + neJobDetails1.getNeStartDate() + "Start Date 2::" + neJobDetails2.getNeStartDate());
                    int compare = 0;
                    final Long neStartDate_1 = neJobDetails1.getNeStartDate().isEmpty() ? 0 : Long.parseLong(neJobDetails1.getNeStartDate());
                    final Long neStartDate_2 = neJobDetails2.getNeStartDate().isEmpty() ? 0 : Long.parseLong(neJobDetails2.getNeStartDate());
                    compare = neStartDate_1.compareTo(neStartDate_2);
                    return compare;
                }
            });
        } else if (jobInput.getSortBy().equals(JobConfigurationConstants.NE_END_DATE)) {
            Collections.sort(neJobsDataList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    int compare = 0;
                    final Long neEndDate_1 = neJobDetails1.getNeEndDate().isEmpty() ? 0 : Long.parseLong(neJobDetails1.getNeEndDate());
                    final Long neEndDate_2 = neJobDetails2.getNeEndDate().isEmpty() ? 0 : Long.parseLong(neJobDetails2.getNeEndDate());
                    compare = neEndDate_1.compareTo(neEndDate_2);
                    return compare;
                }
            });
        } else if (jobInput.getSortBy().equals(JobConfigurationConstants.LAST_LOG_MESSAGE)) {
            Collections.sort(neJobsDataList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return neJobDetails1.getLastLogMessage().compareTo(neJobDetails2.getLastLogMessage());
                }
            });
        } else if (jobInput.getSortBy().equals(JobConfigurationConstants.NE_TYPE)) {
            Collections.sort(neJobsDataList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return neJobDetails1.getNodeType().compareTo(neJobDetails2.getNodeType());
                }
            });
        }
    }

    private void descendingNeJob(final List<NeJobDetails> neJobsDataList, final NeJobInput jobInput) {
        if (jobInput.getSortBy().equals(JobConfigurationConstants.NE_JOBID)) {
            Collections.sort(neJobsDataList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return neJobDetails2.getNeJobId().compareTo(neJobDetails1.getNeJobId());
                }
            });
        } else if (jobInput.getSortBy().equals(JobConfigurationConstants.NE_RESULT)) {
            Collections.sort(neJobsDataList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return performStringComparision(neJobDetails2.getNeResult(), neJobDetails1.getNeResult());
                }
            });
        } else if (jobInput.getSortBy().equals(JobConfigurationConstants.NE_NODE_NAME)) {
            Collections.sort(neJobsDataList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return neJobDetails2.getNeNodeName().compareTo(neJobDetails1.getNeNodeName());
                }
            });
        } else if (jobInput.getSortBy().equals(JobConfigurationConstants.NE_ACTIVITY)) {
            Collections.sort(neJobsDataList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return neJobDetails2.getNeActivity().compareTo(neJobDetails1.getNeActivity());
                }
            });
        } else if (jobInput.getSortBy().equals(JobConfigurationConstants.NE_PROGRESS)) {
            Collections.sort(neJobsDataList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    final Double progressPercentageValue1 = neJobDetails1.getNeProgress();
                    final Double progressPercentageValue2 = neJobDetails2.getNeProgress();
                    return progressPercentageValue2.compareTo(progressPercentageValue1);
                }
            });
        } else if (jobInput.getSortBy().equals(JobConfigurationConstants.NE_STATUS)) {
            Collections.sort(neJobsDataList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return neJobDetails2.getNeStatus().compareTo(neJobDetails1.getNeStatus());
                }
            });
        } else if (jobInput.getSortBy().equals(JobConfigurationConstants.NE_START_DATE)) {
            Collections.sort(neJobsDataList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    LOGGER.debug("Start Date 1::" + neJobDetails1.getNeStartDate() + "Start Date 2::" + neJobDetails2.getNeStartDate());
                    int compare = 0;
                    final Long neStartDate_1 = ActivityConstants.EMPTY.equals(neJobDetails1.getNeStartDate()) ? 0 : Long.parseLong(neJobDetails1.getNeStartDate());
                    final Long neStartDate_2 = ActivityConstants.EMPTY.equals(neJobDetails2.getNeStartDate()) ? 0 : Long.parseLong(neJobDetails2.getNeStartDate());
                    compare = neStartDate_2.compareTo(neStartDate_1);
                    return compare;
                }
            });
        } else if (jobInput.getSortBy().equals(JobConfigurationConstants.NE_END_DATE)) {
            Collections.sort(neJobsDataList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    int compare = 0;
                    final Long neEndDate_1 = ActivityConstants.EMPTY.equals(neJobDetails1.getNeEndDate()) ? 0 : Long.parseLong(neJobDetails1.getNeEndDate());
                    final Long neEndDate_2 = ActivityConstants.EMPTY.equals(neJobDetails2.getNeEndDate()) ? 0 : Long.parseLong(neJobDetails2.getNeEndDate());
                    compare = neEndDate_2.compareTo(neEndDate_1);
                    return compare;
                }
            });
        } else if (jobInput.getSortBy().equals(JobConfigurationConstants.LAST_LOG_MESSAGE)) {
            Collections.sort(neJobsDataList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return neJobDetails2.getLastLogMessage().compareTo(neJobDetails1.getLastLogMessage());
                }
            });
        } else if (jobInput.getSortBy().equals(JobConfigurationConstants.NE_TYPE)) {
            Collections.sort(neJobsDataList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return neJobDetails2.getNodeType().compareTo(neJobDetails1.getNodeType());
                }
            });
        }
    }

    private static int performStringComparision(final String neJobDetails1, final String neJobDetails2) {
        if (neJobDetails1 == null && neJobDetails2 == null) {
            return 0;
        } else if (neJobDetails1 == null && neJobDetails2 != null) {
            return -1;
        } else if (neJobDetails1 != null && neJobDetails2 == null) {
            return 1;
        } else {
            return neJobDetails1.compareTo(neJobDetails2);
        }
    }

    private void setDate(final NeJobDetails neJobDetails, final Date startDate, final Date endDate) {
        if (startDate != null) {
            final String neStartDate = String.valueOf(startDate.getTime());
            neJobDetails.setNeStartDate(neStartDate);
        } else {
            neJobDetails.setNeStartDate("");
        }
        if (endDate != null) {
            final String neEndDate = String.valueOf(endDate.getTime());
            neJobDetails.setNeEndDate(neEndDate);
        } else {
            neJobDetails.setNeEndDate("");
        }
    }

    private void getActivityDetailsState(final NeJobDetails neJobDetails, final List<ActivityDetails> activityDetailsListRunning, final ActivityDetails activityDetailsRunning,
            final List<ActivityDetails> activityDetailsListCancel, final ActivityDetails activityDetailsCancel, final List<ActivityDetails> activityDetailsListWait) {
        if (!activityDetailsListRunning.isEmpty()) {
            LOGGER.debug(" Running activities found");
            neJobDetails.setNeActivity(activityDetailsRunning.getActivityName());
        } else if (!activityDetailsListCancel.isEmpty() && activityDetailsListRunning.isEmpty()) {
            LOGGER.debug("Cancelled actvities found and no running activities");
            neJobDetails.setNeActivity(activityDetailsCancel.getActivityName());
        } else if (!activityDetailsListWait.isEmpty() && activityDetailsListCancel.isEmpty() && activityDetailsListRunning.isEmpty()) {
            // sorting the list
            LOGGER.debug("Waiting activities found...");
            sortActivites(activityDetailsListWait);
            LOGGER.debug("activityDetailsListWait .. {}", activityDetailsListWait);
            final String currentActivityName = activityDetailsListWait.get(0).getActivityName();
            LOGGER.debug("activityName .. {}", currentActivityName);
            neJobDetails.setNeActivity(currentActivityName);
        }
    }

    private void setDate(final ActivityDetails activityDetails, final Date actEndDate, final Date actStartDate) {
        if (actStartDate != null) {
            final String actStartTime = String.valueOf(actStartDate.getTime());
            activityDetails.setActivityStartTime(actStartTime);
        } else {
            activityDetails.setActivityStartTime("");
        }
        if (actEndDate != null) {
            final String actEndTime = String.valueOf(actEndDate.getTime());
            activityDetails.setActivityEndTime(actEndTime);
        } else {
            activityDetails.setActivityEndTime("");
        }
    }

    private String setNodeDetails(final Map<String, Object> neJobPropertiesMap, final Long neJobId, final Map<String, Object> neJobAttributes, final NeJobDetails neJobDetails) {
        final String neNodeName = (String) neJobAttributes.get(ShmConstants.NE_NAME);
        final String neResult = (String) neJobAttributes.get(ShmConstants.NE_RESULT);
        final String neType = (String) neJobAttributes.get(ShmConstants.NETYPE);
        neJobDetails.setNeNodeName(neNodeName);
        neJobDetails.setNeJobId(neJobId);
        neJobDetails.setNeProgress((double) neJobAttributes.get(ShmConstants.NE_PROG_PERCENTAGE));
        neJobDetails.setNeStatus((String) neJobAttributes.get(ShmConstants.NE_STATUS));
        setNeTypeDetails(neJobDetails, neNodeName, neType);
        if (neResult == null) {
            neJobDetails.setNeJobConfiguration((Map<String, Map<String, String>>) neJobPropertiesMap.get(neNodeName));
        } else {
            neJobDetails.setNeResult(neResult);
        }
        return neNodeName;
    }

    private void setNeTypeDetails(final NeJobDetails neJobDetails, final String neName, final String nodeType) {
        if (nodeType != null && !"".equals(nodeType)) {
            neJobDetails.setNodeType(nodeType);
        } else {
            try {
                neJobDetails.setNodeType(networkElementRetrievalBean.getNeType(neName));
            } catch (MoNotFoundException e) {
                neJobDetails.setNodeType("");
            }
        }
    }

    public JobReportDetails getMainJobDeatils(final Map<String, Object> mainJobAttributes, final Map<String, Object> jobTemplatePOAttributes) {
        final JobReportDetails jobReportDetails = new JobReportDetails();
        /* -----For Main Job Overview Display, retrieving Main Job Details ----- */
        final Object jobResult = mainJobAttributes.get(ShmConstants.RESULT);
        final Date jobStartTime = (Date) mainJobAttributes.get(ShmConstants.STARTTIME);
        final Date jobEndTime = (Date) mainJobAttributes.get(ShmConstants.ENDTIME);
        // Setting main Job details----------------
        jobReportDetails.setJobName((String) jobTemplatePOAttributes.get(ShmConstants.NAME));
        jobReportDetails.setJobCreatedBy((String) jobTemplatePOAttributes.get(ShmConstants.OWNER));
        jobReportDetails.setJobProgress((double) mainJobAttributes.get(ShmConstants.PROGRESSPERCENTAGE));
        final String jobDescription = (String) jobTemplatePOAttributes.get(ShmConstants.DESCRIPTION);
        jobReportDetails.setJobStatus((String) mainJobAttributes.get(ShmConstants.STATE));
        jobReportDetails.setJobType((String) jobTemplatePOAttributes.get(ShmConstants.JOB_TYPE));
        if (jobResult != null) {
            jobReportDetails.setJobResult((String) jobResult);
        } else {
            jobReportDetails.setJobResult("");
        }
        if (jobStartTime != null) {
            final String startDate = String.valueOf(jobStartTime.getTime());
            jobReportDetails.setJobStartTime(startDate);
        } else {
            jobReportDetails.setJobStartTime("");
        }
        if (jobDescription == null) {

            jobReportDetails.setDescription("");
        } else {
            jobReportDetails.setDescription(jobDescription);
        }

        if (jobEndTime != null) {
            final String jobEndDate = String.valueOf(jobEndTime.getTime());
            jobReportDetails.setJobEndTime(jobEndDate);
        } else {
            jobReportDetails.setJobEndTime("");
        }
        // added by namrata for add comment;
        List<Map<String, Object>> commentList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> formattedCommentList = new ArrayList<Map<String, Object>>();
        if (mainJobAttributes.get(JobModelConstants.JOB_COMMENT) != null) {

            commentList = (List<Map<String, Object>>) mainJobAttributes.get(JobModelConstants.JOB_COMMENT);
            LOGGER.debug("Ne Job Page commentList : {} ", commentList);
            for (final Map<String, Object> comment : commentList) {
                LOGGER.debug("Ne Job Page Comment Date Class : {} ", ((Date) comment.get(ShmConstants.DATE)).getClass());
                final Date commentDate = (Date) comment.get(ShmConstants.DATE);
                final String commentMessage = (String) comment.get(ShmConstants.COMMENT);
                final String commentUser = (String) comment.get(ShmConstants.USERNAME);

                final Map<String, Object> formattedComment = new HashMap<String, Object>();
                formattedComment.put(ShmConstants.DATE, String.valueOf(commentDate.getTime()));
                formattedComment.put(ShmConstants.COMMENT, commentMessage);
                formattedComment.put(ShmConstants.USERNAME, commentUser);
                formattedCommentList.add(formattedComment);
            }
            LOGGER.debug("Ne Job Page formattedCommentList: {}", formattedCommentList);
            jobReportDetails.setJobComment(formattedCommentList);
        } else {
            jobReportDetails.setJobComment(commentList);
        }
        //For Displaying JobConfiguration
        jobReportDetails.setJobConfigurationDetails((Map<String, Object>) jobTemplatePOAttributes.get(ShmConstants.JOBCONFIGURATIONDETAILS));
        return jobReportDetails;
    }

    /**
     * Retrieves the neJobProperties from jobConfigurationDetails and prepares into a map.
     * 
     * @param jobConfigurationDetails
     * @return
     */
    // At present DPS supports Maps with keys and values of primitive, String
    // and Enum types.
    // This Method is not Needed when dps supports the creation of a Map,
    // key/vale pair type other than mentioned above,
    public Map<String, Object> prepareNeJobPropertiesMap(final Map<String, Object> jobConfigurationDetails) {
        final Map<String, Object> neJobConfiguration = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobProperties = (List<Map<String, Object>>) jobConfigurationDetails.get(ShmConstants.NEJOB_PROPERTIES);
        if (neJobProperties == null) {
            LOGGER.debug("neJobProperties not Found");
            return neJobConfiguration;
        }
        for (final Map<String, Object> neJobProperty : neJobProperties) {
            final String neName = (String) neJobProperty.get(ShmConstants.NE_NAME);
            final Map<String, String> jobPropertiesMap = new HashMap<String, String>();
            final List<Map<String, Object>> jobPropertiesList = (List<Map<String, Object>>) neJobProperty.get(ShmConstants.NETYPEJOBPROPERTIES);
            if (jobPropertiesList != null) {
                for (final Map<String, Object> jobProperty : jobPropertiesList) {
                    String key = (String) jobProperty.get(ShmConstants.KEY);
                    String value = (String) jobProperty.get(ShmConstants.VALUE);
                    if (key.endsWith(JobPropertyConstants.PROP_LICENSE_KEYFILE_PATH)) {
                        LOGGER.debug("License Key file({}) found for current NE:{} ", key, value);
                        key = JobPropertyConstants.DISPLAYNAME_FOR_LICENSE_KEY_FILE;
                        value = FilenameUtils.getName(value);
                        LOGGER.debug("License Key file({},{}) found for current NE:{} ", JobPropertyConstants.DISPLAYNAME_FOR_LICENSE_KEY_FILE, value, neName);
                    }
                    jobPropertiesMap.put(key, value);
                }
            }
            neJobConfiguration.put(neName, jobPropertiesMap);
        }
        return neJobConfiguration;
    }

    private void sortActivites(final List<ActivityDetails> activityDetailsList) {
        Collections.sort(activityDetailsList, new Comparator<ActivityDetails>() {
            @Override
            public int compare(final ActivityDetails activityDetailsList1, final ActivityDetails activityDetailsList2) {
                return activityDetailsList1.getActivityOrder() - activityDetailsList2.getActivityOrder();
            }
        });
    }

    private void sortJobLogResponseOnEntryTime(final List<JobLogResponse> jobLogResponseForEachNode) {

        for (final JobLogResponse logDetails : jobLogResponseForEachNode) {
            if (logDetails.getEntryTime() == null) {
                logDetails.setEntryTime("null");
            }
            if (logDetails.getMessage() == null) {
                logDetails.setMessage("null");
            }
        }
        Collections.sort(jobLogResponseForEachNode, new Comparator<JobLogResponse>() {
            @Override
            public int compare(final JobLogResponse logDetail1, final JobLogResponse logDetail2) {
                return logDetail2.getEntryTime().compareTo(logDetail1.getEntryTime());
            }
        });
    }
}
