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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.ejb.*;
import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.activities.ProcessVariableBuilder;
import com.ericsson.oss.services.shm.activities.WorkflowDefinitionsProvider;
import com.ericsson.oss.services.shm.activity.axe.cache.AxeSynchronousActivityData;
import com.ericsson.oss.services.shm.activity.axe.cache.AxeUpgradeSynchronousActivityProvider;
import com.ericsson.oss.services.shm.common.FdnUtils;
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.WorkflowServiceInvocationException;
import com.ericsson.oss.services.shm.common.exception.utils.ExceptionParser;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;
import com.ericsson.oss.services.shm.common.wfs.WfsRetryConfigurationParamProvider;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.fa.FaBuildingBlocksResponseSender;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProviderImpl;
import com.ericsson.oss.services.shm.job.impl.CommonUtilityProvider;
import com.ericsson.oss.services.shm.jobexecutor.common.NeComponentsInfoBuilderImpl;
import com.ericsson.oss.services.shm.jobexecutorlocal.JobExecutorLocal;
import com.ericsson.oss.services.shm.jobexecutorremote.JobExecutorRemote;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.*;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobMapper;
import com.ericsson.oss.services.shm.jobs.common.modelentities.*;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.NetworkElementResponse;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.JobAdministratorTBACValidator;
import com.ericsson.oss.services.shm.tbac.TBACResponse;
import com.ericsson.oss.services.shm.tbac.models.TBACConfigurationProvider;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;
import com.ericsson.oss.services.wfs.api.query.*;
import com.ericsson.oss.services.wfs.api.query.instance.WorkflowInstanceQueryAttributes;
import com.ericsson.oss.services.wfs.api.query.usertask.UsertaskQueryAttributes;

/**
 * Job executor to create Main job, NE jobs,Activity jobs and submit NE jobs in workflow service
 * 
 */
@SuppressWarnings("PMD")
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class JobExecutionService implements JobExecutorRemote, JobExecutorLocal {

    private static final String DATE_FORMATE = "yyyy-MM-dd HH:mm:ss Z";
    private static final Logger LOGGER = LoggerFactory.getLogger(JobExecutionService.class);
    private static final int FIRST_EXECUTION_INDEX = 1;
    private static final long DEFAULT_MAIN_JOB_ID = -1;
    private static final int NUMBER_OF_DEFAULT_WFS_RETRIES = 10;
    private static final int NUMBER_OF_DEFAULT_WFS_RETRYTIME = 2000;
    private static final String MAIN_JOB_ID_STRING = "mainJobId:";
    private static final String WORKFLOW_ID_STRING = "wfsId:";
    private static final String TEMPLATE_ID_STRING = "templateJobId:";

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private JobStaticDataProviderImpl jobStaticDataProviderImpl;

    @Inject
    private JobExecutorServiceHelper executorServiceHelper;

    @Inject
    private WorkflowDefinitionsProvider workflowDefinitionsProvider;

    @Inject
    private ProcessVariableBuilder processVariableBuilder;

    @Inject
    private JobMapper jobMapper;

    @Inject
    private WorkflowInstanceNotifier workflowInstanceNotifier;

    @Inject
    private JobConfigurationService jobConfigurationService;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private RetryManager retryManager;

    @Inject
    private DpsRetryPolicies dpsRetryPolicies;

    @Inject
    private WfsRetryConfigurationParamProvider wfsRetryConfigurationParamProvider;

    @Inject
    private JobCancelHandler jobCancellingHandler;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private TBACValidator tbacValidator;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private JobAdministratorTBACValidator jobAdministratorTBACValidator;

    @Inject
    private TBACConfigurationProvider tbacConfigurationProvider;

    @Inject
    SupportedNesListBuilder supportedNesListBuilder;

    @Inject
    private TopologyEvaluationServiceManager topologyEvaluationServiceManager;

    @Inject
    private FaBuildingBlocksResponseSender faBuildingBlocksResponseSender;

    @Inject
    private NeComponentsInfoBuilderImpl neComponentsInfoBuilderImpl;

    @Inject
    private SynchronousActivityProvider synchronousActivityProvider;

    @Inject
    NeComponetsInfoBuilderFactory neComponetsInfoBuilderFactory;

    @Inject
    private AxeUpgradeSynchronousActivityProvider axeSyncActivityDatProvider;

    @Inject
    private JobBuilderRetryProxy builderRetryProxy;

    @Override
    public long prepareMainJob(final String wfsId, final long jobTemplateId, final Date nextExecutionTime) {
        LOGGER.info("Execution starts for job with template Id: {} wfsId={}", jobTemplateId, wfsId);
        long poId = DEFAULT_MAIN_JOB_ID;
        try {
            final Map<String, Object> jobTemplateAttributes = retrieveJobWithRetry(jobTemplateId);
            if (jobTemplateAttributes == null || jobTemplateAttributes.isEmpty()) {
                LOGGER.error("Unable to retrieve job template from db for the template Id : {}", jobTemplateId);
                return poId;
            }
            final JobTemplate jobTemplate = jobMapper.getJobTemplateDetails(jobTemplateAttributes, jobTemplateId);

            final JobConfiguration jobConfiguration = jobTemplate.getJobConfigurationDetails();
            if (jobConfiguration == null) {
                LOGGER.info("Job template is not available for the provided configuration id : {}", jobTemplateId);
                return poId;
            }
            JobExecutionIndexAndState latestExecutionIndexAndState = getLatestJobExecutionIndexAndState(jobTemplateId);

            if (latestExecutionIndexAndState == null) {
                latestExecutionIndexAndState = new JobExecutionIndexAndState();
                latestExecutionIndexAndState.setJobExecutionIndex(FIRST_EXECUTION_INDEX);
                poId = createJobAndReturnPoId(jobConfiguration, latestExecutionIndexAndState.getJobExecutionIndex(), jobTemplateId, wfsId, nextExecutionTime, jobTemplate);
                populateJobCache(poId, jobTemplate, jobConfiguration);
                return poId;
            } else {
                if (JobState.isJobInactive(latestExecutionIndexAndState.getJobState())) {
                    LOGGER.info("Now another Main Job can be created, since previous job state is {}", latestExecutionIndexAndState.getJobState());
                    poId = createJobAndReturnPoId(jobConfiguration, latestExecutionIndexAndState.getJobExecutionIndex() + 1, jobTemplateId, wfsId, nextExecutionTime, jobTemplate);
                    populateJobCache(poId, jobTemplate, jobConfiguration);
                    return poId;
                } else {
                    logSkippedEvent(jobTemplate.getName(), latestExecutionIndexAndState.getJobExecutionIndex() + 1, latestExecutionIndexAndState.getJobState());
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Exception occured during main job creation {}", e.getMessage());
            return poId;
        }
        return poId;
    }

    private void populateJobCache(final long mainJobId, final JobTemplate jobTemplate, final JobConfiguration jobConfiguration) {

        final Map<String, Object> activitySchedules = prepareActivitySchedules(mainJobId, jobConfiguration);
        final String execMode = jobConfiguration.getMainSchedule().getExecMode().getMode();
        final String jobExecutionUser = jobStaticDataProviderImpl.getShmJobExecUser(mainJobId, execMode, jobTemplate.getOwner());
        final JobStaticData jobStaticData = new JobStaticData(jobTemplate.getOwner(), activitySchedules, jobConfiguration.getMainSchedule().getExecMode().getMode(), jobTemplate.getJobType(),
                jobExecutionUser);
        jobStaticDataProvider.put(mainJobId, jobStaticData);
    }

    private static Map<String, Object> prepareActivitySchedules(final long mainJobId, final JobConfiguration jobConfiguration) {
        final List<Activity> activitiesList = jobConfiguration.getActivities();
        final Map<String, Object> activitySchedules = new HashMap<String, Object>();
        for (final Activity activity : activitiesList) {
            final String key = mainJobId + "_" + activity.getNeType() + "_" + activity.getName().toLowerCase();
            if (!activitySchedules.containsKey(key)) {
                activitySchedules.put(key, activity.getSchedule().getExecMode().getMode());
            }
        }
        return activitySchedules;
    }

    @SuppressWarnings("unchecked")
    @Override
    @Asynchronous
    public void execute(final String wfsId, final long mainJobId) {
        // Checking job state before updating the jobstate to RUNNING
        final Map<String, Object> mainJobAttributes = jobUpdateService.retrieveJobWithRetry(mainJobId);
        final String jobState = (String) mainJobAttributes.get(ShmConstants.STATE);
        // Update jobState to RUNNING only when job is in SUBMMITED state
        if (jobState != null && JobState.isJobCreated(JobState.valueOf(jobState))) {

            final Map<String, Object> jobProperties = new HashMap<>();
            final Map<String, Object> mainJobAttribute = new HashMap<>();
            mainJobAttribute.put(ShmConstants.STATE, JobState.RUNNING.getJobStateName());
            mainJobAttribute.put(ShmConstants.STARTTIME, new Date());

            updateJobAtributes(mainJobId, mainJobAttribute);

            final List<Map<String, Object>> jobPropertiesList = new ArrayList<>();

            final List<Map<String, Object>> collectionIdsOrsaveSearchedIdsInfo = getCollectionOrSavedSearchDetails(mainJobId);
            if (collectionIdsOrsaveSearchedIdsInfo != null && !collectionIdsOrsaveSearchedIdsInfo.isEmpty()) {
                final String collectionString = CommonUtilityProvider.convertListToString(collectionIdsOrsaveSearchedIdsInfo);
                jobProperties.put(ShmConstants.KEY, ShmConstants.COLLECTIONORSSINFO);
                jobProperties.put(ShmConstants.VALUE, collectionString);
                jobPropertiesList.add(jobProperties);
            }

            jobUpdateService.readAndUpdateRunningJobAttributes(mainJobId, jobPropertiesList, null, null);

        }
        final Map<String, Object> poAttributes = retrieveJobWithRetry(mainJobId);
        final Map<String, Object> jobConfiguration = (Map<String, Object>) poAttributes.get(ShmConstants.JOBCONFIGURATIONDETAILS);
        final Map<String, Object> mainSchedule = (Map<String, Object>) jobConfiguration.get(ShmConstants.MAIN_SCHEDULE);
        final String executionMode = (String) mainSchedule.get(ShmConstants.EXECUTION_MODE);
        systemRecorder.recordEvent(SHMEvents.JOB_START, EventLevel.COARSE, executionMode + "JOB", "Job", "SHM:JOB" + ":Proceeding execution for mainJobId " + mainJobId + " and wfsId " + wfsId);
        createNEJobs(mainJobId, wfsId);
        updateAllNeJobsCreated(mainJobId);
    }

    private void updateAllNeJobsCreated(final long mainJobId) {
        LOGGER.debug("Updating allNeJobscreated status for mainJobId : {}", mainJobId);
        final Map<String, Object> jobProperty = new HashMap<>();
        final List<Map<String, Object>> jobPropertiesList = new ArrayList<>();
        jobProperty.put(ShmConstants.KEY, ShmJobConstants.ALL_NE_JOBS_CREATED);
        jobProperty.put(ShmConstants.VALUE, ShmConstants.TRUE);
        jobPropertiesList.add(jobProperty);
        jobUpdateService.readAndUpdateRunningJobAttributes(mainJobId, jobPropertiesList, null, null);
    }

    private List<Map<String, Object>> getCollectionOrSavedSearchDetails(final long mainJobId) {
        List<Map<String, Object>> listOfCollections = null;
        final Map<String, Object> mainJobAttribute = retrieveJobWithRetry(mainJobId);
        final long templateJobId = mainJobAttribute.get(ShmConstants.JOBTEMPLATEID) != null ? (long) mainJobAttribute.get(ShmConstants.JOBTEMPLATEID) : 0;
        final Map<String, Object> jobTemplateAttributes = retrieveJobWithRetry(templateJobId);
        final JobTemplate jobTemplate = jobMapper.getJobTemplateDetails(jobTemplateAttributes, templateJobId);
        final JobConfiguration jobConfiguration = jobTemplate.getJobConfigurationDetails();
        final NEInfo selectedNEInfo = jobConfiguration.getSelectedNEs();
        if (selectedNEInfo != null) {
            final List<String> collectionIds = selectedNEInfo.getCollectionNames();
            final List<String> savedSearchIds = selectedNEInfo.getSavedSearchIds();
            listOfCollections = getCollectionOrSSInfo(collectionIds, savedSearchIds, jobTemplate);
            return listOfCollections;
        }
        return listOfCollections;
    }

    private List<Map<String, Object>> getCollectionOrSSInfo(final List<String> collectionIds, final List<String> savedSearchIds, final JobTemplate jobTemplate) {
        final List<Map<String, Object>> listOfCollections = new ArrayList<>();
        if (!collectionIds.isEmpty() || !savedSearchIds.isEmpty()) {
            final String jobOwner = jobTemplate.getOwner();
            if (!collectionIds.isEmpty()) {
                getCollectionInformation(jobOwner, collectionIds, listOfCollections);
            }
            if (!savedSearchIds.isEmpty()) {
                getSaveSearchInformation(jobOwner, savedSearchIds, listOfCollections);
            }
            return listOfCollections;
        }
        return listOfCollections;
    }

    private void getCollectionInformation(final String jobOwner, final List<String> collectionIds, final List<Map<String, Object>> listOfCollections) {
        Map<String, Object> collectionIdsOrSavedSearchIds = null;

        for (final String collectionId : collectionIds) {
            collectionIdsOrSavedSearchIds = new HashMap<>();
            final Set<String> collectionData = prepareFinalListOfNENames(topologyEvaluationServiceManager.getCollectionInfo(jobOwner, collectionId));
            try {
                collectionIdsOrSavedSearchIds.put(collectionId, collectionData);
                collectionIdsOrSavedSearchIds.put(ShmConstants.COLLECTIONORSSNAME, topologyEvaluationServiceManager.getCollectionName(jobOwner, collectionId));
                listOfCollections.add(collectionIdsOrSavedSearchIds);
            } catch (final Exception exception) {
                LOGGER.debug("Exception caught while retrieving NEs from Collection with message {}", exception);
            }
        }
    }

    private void getSaveSearchInformation(final String jobOwner, final List<String> savedSearchIds, final List<Map<String, Object>> listOfCollections) {
        Map<String, Object> collectionIdsOrSavedSearchIds = null;
        for (final String savedSearchId : savedSearchIds) {
            collectionIdsOrSavedSearchIds = new HashMap<>();
            final Set<String> savedSearchData = prepareFinalListOfNENames(topologyEvaluationServiceManager.getSavedSearchInfo(savedSearchId, jobOwner));
            try {
                collectionIdsOrSavedSearchIds.put(savedSearchId, savedSearchData);
                collectionIdsOrSavedSearchIds.put(ShmConstants.COLLECTIONORSSNAME, topologyEvaluationServiceManager.getSavedSearchName(savedSearchId, jobOwner));
                listOfCollections.add(collectionIdsOrSavedSearchIds);
            } catch (final Exception exception) {
                LOGGER.debug("Exception caught while retrieving NEs from savedsearch with message {}", exception);
            }
        }
    }

    private Set<String> prepareFinalListOfNENames(final Set<String> neFdns) {
        final Set<String> neNames = new HashSet<>();
        for (final String netWorkElementFdn : neFdns) {
            prepareListofNEsFromFdn(netWorkElementFdn, neNames);
        }
        return neNames;
    }

    private void prepareListofNEsFromFdn(final String nodeFdn, final Set<String> neNames) {
        final String nodeName = FdnUtils.getNodeName(nodeFdn);
        LOGGER.debug("Extracted  Fdn {} from Topology Collection service ", nodeFdn);
        if (nodeName != null && !neNames.contains(nodeName)) {
            neNames.add(nodeName);
        }
    }

    private long createJobAndReturnPoId(final JobConfiguration jobConfiguration, final int jobExecutionIndex, final long jobTemplateId, final String wfsId, final Date nextExecutionTime,
            final JobTemplate jobTemplate) {
        final String startTime = null;
        long jobPOId;
        final Map<String, Object> jobAttributes = populateJobAttributes(jobConfiguration, jobExecutionIndex, jobTemplateId, wfsId, jobTemplate);
        final ExecMode executionMode = setJobAttributesAndReturnExecutionMode(jobConfiguration, nextExecutionTime, startTime, jobAttributes);
        final Map<String, Object> jobPO = createJobPO(ShmConstants.NAMESPACE, ShmConstants.JOB, ShmConstants.VERSION, jobAttributes);
        if (jobPO == null || jobPO.isEmpty()) {
            LOGGER.error("Failed to create a JOB as jobPO is :{}", jobPO);
            jobPOId = DEFAULT_MAIN_JOB_ID;
        } else {
            systemRecorder.recordEvent(SHMEvents.JOB_CREATE, EventLevel.COARSE, "", executionMode + "JOB","Main job is created with Id: " + jobPO.get(ShmConstants.PO_ID) + " with jobTemplateId: " + jobTemplateId + " and wfs id: " + wfsId);

            //setting NE count
            updateNeCountInMainJob(jobConfiguration, jobTemplate, jobPO);
            //end
            jobPOId = (long) jobPO.get(ShmConstants.PO_ID);
        }
        return jobPOId;
    }

    private void updateNeCountInMainJob(final JobConfiguration jobConfiguration, final JobTemplate jobTemplate, final Map<String, Object> jobPO) {
        final NEInfo neInfo = jobConfiguration.getSelectedNEs();
        final long mainJobId = (long) jobPO.get(ShmConstants.PO_ID);
        final Map<String, Object> poAttr = new HashMap<String, Object>();
        final List<String> expectedNEs = new ArrayList<String>();
        if (neInfo.getNeNames() != null && !neInfo.getNeNames().isEmpty()) {
            handleNeNames(expectedNEs, neInfo);
        }
        if (neInfo.getCollectionNames() != null && !neInfo.getCollectionNames().isEmpty()) {
            handleCollections(mainJobId, expectedNEs, neInfo, jobTemplate);
        }
        if (neInfo.getSavedSearchIds() != null && !neInfo.getSavedSearchIds().isEmpty()) {
            handleSavedSearches(mainJobId, expectedNEs, neInfo, jobTemplate);
        }
        final int expectedNeCount = expectedNEs.size();
        LOGGER.info("Excpected NE count for the jobs is {}", expectedNeCount);
        poAttr.put(ShmConstants.NO_OF_NETWORK_ELEMENTS, expectedNeCount);
        updateJobAtributes(mainJobId, poAttr);
    }

    private void handleNeNames(final List<String> expectedCountOfNEs, final NEInfo neInfo) {
        final List<String> neNames = neInfo.getNeNames();
        expectedCountOfNEs.addAll(neNames);
    }

    private void handleCollections(final long mainJobId, final List<String> expectedCountOfNEs, final NEInfo neInfo, final JobTemplate jobTemplate) {
        final List<Map<String, Object>> topologyJobLogList = new ArrayList<Map<String, Object>>();
        executorServiceHelper.populateNeNamesFromCollections(mainJobId, expectedCountOfNEs, topologyJobLogList, neInfo.getCollectionNames(), jobTemplate.getOwner());
    }

    private void handleSavedSearches(final long mainJobId, final List<String> expectedCountOfNEs, final NEInfo neInfo, final JobTemplate jobTemplate) {
        final List<Map<String, Object>> topologyJobLogList = new ArrayList<Map<String, Object>>();
        executorServiceHelper.populateNeNamesFromSavedSearches(mainJobId, expectedCountOfNEs, topologyJobLogList, neInfo.getSavedSearchIds(), jobTemplate.getOwner());
    }

    private Map<String, Object> populateJobAttributes(final JobConfiguration jobConfiguration, final int jobExecutionIndex, final long jobTemplateId, final String wfsId,
            final JobTemplate jobTemplate) {
        final Map<String, Object> jobAttributes = new HashMap<String, Object>();
        jobAttributes.put(ShmConstants.EXECUTIONINDEX, jobExecutionIndex);
        jobAttributes.put(ShmConstants.JOB_TEMPLATE_ID, jobTemplateId);
        jobAttributes.put(ShmConstants.WFS_ID, wfsId);
        jobAttributes.put(ShmConstants.BUSINESS_KEY, Long.toString(jobTemplateId));
        jobAttributes.put(ShmConstants.JOBCONFIGURATIONDETAILS, preapareJobConfigurationDetails(jobConfiguration));
        jobAttributes.put(ShmConstants.STATE, JobState.CREATED.getJobStateName());
        jobAttributes.put(ShmConstants.CREATION_TIME, new Date());
        jobAttributes.put(ShmConstants.JOBPROPERTIES, prepareJobProperties(jobTemplate));
        return jobAttributes;
    }

    /**
     * provides the required parameters like jobName and JobCategory
     * 
     * @param jobTemplate
     * @return
     */
    private List<Map<String, String>> prepareJobProperties(final JobTemplate jobTemplate) {

        final List<Map<String, String>> jobProperties = new ArrayList<>();
        jobProperties.add(prepareJobProperty(ShmConstants.JOB_CATEGORY, jobTemplate.getJobCategory().name()));
        jobProperties.add(prepareJobProperty(ShmConstants.JOBNAME, jobTemplate.getName()));
        return jobProperties;
    }

    private Map<String, String> prepareJobProperty(final String propertyName, final String propertyValue) {

        final Map<String, String> property = new HashMap<>();
        property.put(ShmConstants.KEY, propertyName);
        property.put(ShmConstants.VALUE, propertyValue);
        return property;
    }

    private ExecMode setJobAttributesAndReturnExecutionMode(final JobConfiguration jobConfiguration, final Date nextExecutionTime, final String startTime, final Map<String, Object> jobAttributes) {
        ExecMode execMode = null;
        final com.ericsson.oss.services.shm.jobs.common.modelentities.Schedule schedule = jobConfiguration.getMainSchedule();
        LOGGER.debug("Inside method :: setJobAttributesAndReturnExecutionMode(), Schedule :{}", schedule);
        if (schedule != null && schedule.getExecMode() != null) {
            execMode = schedule.getExecMode();
            LOGGER.debug("Execution Mode: {}", execMode);
            populateJobAttributesBasedOnExecutionMode(nextExecutionTime, startTime, jobAttributes, execMode, schedule);
        }
        return execMode;
    }

    private void populateJobAttributesBasedOnExecutionMode(final Date nextExecutionTime, final String startTime, final Map<String, Object> jobAttributes, final ExecMode execMode,
            final com.ericsson.oss.services.shm.jobs.common.modelentities.Schedule schedule) {
        if (ExecMode.SCHEDULED.equals(execMode)) {
            populateJobAttributesForScheduledExecution(nextExecutionTime, startTime, jobAttributes, schedule);
        } else if (ExecMode.MANUAL.equals(execMode)) {
            populateJobAttributesForManualExecution(jobAttributes);
        }
    }

    private void populateJobAttributesForManualExecution(final Map<String, Object> jobAttributes) {
        jobAttributes.put(ShmConstants.STATE, JobState.WAIT_FOR_USER_INPUT.getJobStateName());
    }

    private void populateJobAttributesForScheduledExecution(final Date nextExecutionTime, final String startTime, final Map<String, Object> jobAttributes,
            final com.ericsson.oss.services.shm.jobs.common.modelentities.Schedule schedule) {
        if (nextExecutionTime != null) {
            jobAttributes.put(ShmConstants.STARTTIME, nextExecutionTime);
        } else if (isScheduledJob(schedule)) {
            Date parsedDate = null;
            final String scheduledDate = prepareScheduledDate(startTime, schedule);
            LOGGER.debug("Scheduled Date :{}", scheduledDate);
            final SimpleDateFormat dateFormatter = new SimpleDateFormat(ShmConstants.DATE_FORMAT);
            try {
                LOGGER.debug("Start time to be parsed :{}", scheduledDate);
                parsedDate = dateFormatter.parse(scheduledDate);
                LOGGER.debug("Start Date :{}", parsedDate);
            } catch (final ParseException e) {
                LOGGER.error("Cannot parse start Date :{} with exception :{}", parsedDate, e.getMessage());
            }
            jobAttributes.put("startTime", parsedDate);
        }
        jobAttributes.put(ShmConstants.STATE, JobState.SCHEDULED.getJobStateName());
    }

    private String prepareScheduledDate(String startTime, final com.ericsson.oss.services.shm.jobs.common.modelentities.Schedule schedule) {
        startTime = obtainStartTimeFromSchedulingProperties(startTime, schedule);
        final String timeZoneId = getTimeZoneIdFromDate(startTime);
        final String delims = " ";
        final StringTokenizer st = new StringTokenizer(startTime, delims);
        final String formattedScheduleTime = st.nextToken() + "T" + st.nextToken();
        return convertTimeZones(timeZoneId, TimeZone.getDefault().getID(), formattedScheduleTime);

    }

    private boolean isScheduledJob(final com.ericsson.oss.services.shm.jobs.common.modelentities.Schedule schedule) {
        return schedule.getScheduleAttributes() != null;
    }

    private String obtainStartTimeFromSchedulingProperties(String startTime, final com.ericsson.oss.services.shm.jobs.common.modelentities.Schedule schedule) {
        final List<ScheduleProperty> schedulingProperties = schedule.getScheduleAttributes();
        for (final ScheduleProperty scheduleProperty : schedulingProperties) {
            if (scheduleProperty != null && scheduleProperty.getName().equalsIgnoreCase(ShmConstants.START_DATE)) {
                startTime = scheduleProperty.getValue();
                LOGGER.debug("Start Time :{}", startTime);
                break;
            }
        }
        return startTime;
    }

    /**
     * Temporary fix to avoid Exceptions w.r.t timezonechanges.Need to be removed later after the improvement w.r.t date format change in UI and server side
     * 
     * @param date
     * @return
     */
    private String getTimeZoneIdFromDate(final String date) {
        String timeZoneId = null;
        if (date.contains(ShmConstants.GMT)) {
            timeZoneId = date.substring(date.indexOf(ShmConstants.GMT) + 3);
        }
        return timeZoneId;
    }

    /**
     * This method converts the date from client time zone to local time zone.
     * 
     * @param fromTimeZoneString
     * @param toTimeZoneString
     * @param fromDateTime
     * 
     * @return String
     */
    private static String convertTimeZones(final String fromTimeZoneString, final String toTimeZoneString, final String fromDateTime) {
        final DateTimeZone fromTimeZone = DateTimeZone.forID(fromTimeZoneString);
        final DateTimeZone toTimeZone = DateTimeZone.forID(toTimeZoneString);
        final DateTime dateTime = new DateTime(fromDateTime, fromTimeZone);
        final DateTimeFormatter outputFormatter = DateTimeFormat.forPattern(DATE_FORMATE).withZone(toTimeZone);
        return outputFormatter.print(dateTime);
    }

    private Map<String, Object> preapareJobConfigurationDetails(final JobConfiguration jobConfiguration) {
        final Map<String, Object> selectedNEsMap = new HashMap<String, Object>();

        final NEInfo selectedNEs = jobConfiguration.getSelectedNEs();
        if (selectedNEs != null) {
            selectedNEsMap.put(ShmConstants.NENAMES, selectedNEs.getNeNames());
            selectedNEsMap.put(ShmConstants.COLLECTION_NAMES, selectedNEs.getCollectionNames());
            selectedNEsMap.put(ShmConstants.SAVED_SEARCH_IDS, selectedNEs.getSavedSearchIds());
        }
        final Map<String, Object> jobConfigurationMap = new HashMap<String, Object>();

        jobConfigurationMap.put(ShmConstants.PLATFORMJOBPROPERTIES, preparePlatformJobProperties(jobConfiguration.getPlatformJobProperties()));
        jobConfigurationMap.put(ShmConstants.NETYPEJOBPROPERTIES, prepareNeTypeJobProperties(jobConfiguration.getNeTypeJobProperties()));
        jobConfigurationMap.put(ShmConstants.SELECTED_NES, selectedNEsMap);
        final com.ericsson.oss.services.shm.jobs.common.modelentities.Schedule mainSchedule = jobConfiguration.getMainSchedule();
        jobConfigurationMap.put(ShmConstants.MAIN_SCHEDULE, prepareSchedule(mainSchedule));
        final List<com.ericsson.oss.services.shm.jobs.common.modelentities.Activity> activities = jobConfiguration.getActivities();
        jobConfigurationMap.put(ShmConstants.ACTIVITIES, prepareActvities(activities));
        LOGGER.info("Placing activiteies in jobConfiguration");
        jobConfigurationMap.put(ShmConstants.NEJOB_PROPERTIES, prepareNeJobProperties(jobConfiguration.getNeJobProperties()));
        jobConfigurationMap.put(ShmConstants.NETYPE_ACTIVITYJOBPROPERTIES, prepareNeTypeActivityJobProperties(jobConfiguration.getNeTypeActivityJobProperties()));

        return jobConfigurationMap;
    }

    private List<Map<String, Object>> getJobPropertyList(final List<JobProperty> jobProperties) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        for (final JobProperty jobProperty : jobProperties) {
            final Map<String, Object> jobPropertyMap = new HashMap<String, Object>();
            jobPropertyMap.put(ShmConstants.KEY, jobProperty.getKey());
            jobPropertyMap.put(ShmConstants.VALUE, jobProperty.getValue());
            jobPropertyList.add(jobPropertyMap);
        }
        return jobPropertyList;
    }

    private List<Map<String, Object>> prepareNeJobProperties(final List<NEJobProperty> neJobProperties) {
        final List<Map<String, Object>> neJobPropList = new ArrayList<Map<String, Object>>();
        for (final NEJobProperty neJobProperty : neJobProperties) {
            final Map<String, Object> neJobPropMap = new HashMap<String, Object>();
            neJobPropMap.put(ShmConstants.NE_NAME, neJobProperty.getNeName());
            neJobPropMap.put(ShmConstants.JOBPROPERTIES, getJobPropertyList(neJobProperty.getJobProperties()));
            neJobPropList.add(neJobPropMap);
        }
        return neJobPropList;
    }

    private List<Map<String, Object>> prepareNeTypeActivityJobProperties(final List<NeTypeActivityJobProperties> neTypeActivityJobProperties) {

        final List<Map<String, Object>> neTypeActivityJobProps = new ArrayList<>();

        if (neTypeActivityJobProperties != null && !neTypeActivityJobProperties.isEmpty()) {
            for (final NeTypeActivityJobProperties neTypeActivityProperties : neTypeActivityJobProperties) {
                final Map<String, Object> activityProperty = new HashMap<>();
                activityProperty.put(ShmConstants.NETYPE, neTypeActivityProperties.getNeType());
                activityProperty.put(ShmConstants.ACTIVITYJOB_PROPERTIES, neTypeActivityProperties.getActivityJobProperties());
                neTypeActivityJobProps.add(activityProperty);
            }
        }
        return neTypeActivityJobProps;

    }

    private List<Map<String, Object>> preparePlatformJobProperties(final List<PlatformJobProperty> neJobProperties) {
        final List<Map<String, Object>> platformJobPropList = new ArrayList<Map<String, Object>>();
        for (final PlatformJobProperty platformJobProperty : neJobProperties) {
            final Map<String, Object> platformJobPropMap = new HashMap<String, Object>();
            platformJobPropMap.put(ShmConstants.PLATFORM, platformJobProperty.getPlatform());
            platformJobPropMap.put(ShmConstants.JOBPROPERTIES, getJobPropertyList(platformJobProperty.getJobProperties()));
            platformJobPropList.add(platformJobPropMap);
        }
        return platformJobPropList;
    }

    private List<Map<String, Object>> prepareNeTypeJobProperties(final List<NeTypeJobProperty> neTypeJobProperties) {
        final List<Map<String, Object>> neJobPropList = new ArrayList<Map<String, Object>>();
        for (final NeTypeJobProperty netypeJobProperty : neTypeJobProperties) {
            final Map<String, Object> neTypeJobPropMap = new HashMap<String, Object>();
            neTypeJobPropMap.put(ShmConstants.NETYPE, netypeJobProperty.getNeType());
            neTypeJobPropMap.put(ShmConstants.JOBPROPERTIES, getJobPropertyList(netypeJobProperty.getJobProperties()));
            neJobPropList.add(neTypeJobPropMap);
        }
        return neJobPropList;
    }

    private List<Map<String, Object>> prepareActvities(final List<com.ericsson.oss.services.shm.jobs.common.modelentities.Activity> activities) {
        final List<Map<String, Object>> activityList = new ArrayList<Map<String, Object>>();

        for (final com.ericsson.oss.services.shm.jobs.common.modelentities.Activity activity : activities) {
            final Map<String, Object> activityMap = new HashMap<String, Object>();
            activityMap.put(ShmConstants.ACTIVITY_NAME, activity.getName());
            activityMap.put(JobSchedulerConstants.PLATFORM, activity.getPlatform().name());
            activityMap.put(JobSchedulerConstants.SCHEDULE, prepareSchedule(activity.getSchedule()));
            activityMap.put(ShmConstants.NETYPE, activity.getNeType());
            activityMap.put(ShmConstants.ORDER, activity.getOrder());

            activityList.add(activityMap);
        }
        return activityList;
    }

    private Map<String, Object> prepareSchedule(final com.ericsson.oss.services.shm.jobs.common.modelentities.Schedule mainSchedule) {
        final Map<String, Object> mainScheduleMap = new HashMap<String, Object>();
        mainScheduleMap.put(ShmConstants.EXECUTION_MODE, mainSchedule.getExecMode().name());
        final List<ScheduleProperty> scheduleAttributes = mainSchedule.getScheduleAttributes();
        final List<Map<String, Object>> scheduleAttributesList = new ArrayList<Map<String, Object>>();
        for (final ScheduleProperty scheduleProperty : scheduleAttributes) {
            final Map<String, Object> schedulePropertyMap = new HashMap<String, Object>();
            schedulePropertyMap.put(ShmConstants.NAME, scheduleProperty.getName());
            schedulePropertyMap.put(ShmConstants.VALUE, scheduleProperty.getValue());
            scheduleAttributesList.add(schedulePropertyMap);
        }
        mainScheduleMap.put(JobSchedulerConstants.SCHEDULE_ATTRIBUTES, scheduleAttributesList);
        return mainScheduleMap;
    }

    @SuppressWarnings("unchecked")
    private void createNEJobs(final long mainJobId, final String wfsId) {
        final Map<String, Long> unsubmittedNeJobIds = new HashMap<>();
        final Map<String, Object> attributeMap = new HashMap<>();
        List<Map<String, Object>> activityJobPos = null;
        boolean result = false;
        int existingNeJobsCount = 0;
        final List<String> submittedNEs = new ArrayList<>();
        final List<String> creationFailedJobs = new ArrayList<>();
        final List<NetworkElement> allunSupportedNetworkElements = new ArrayList<>();

        final Map<String, Object> mainJobAttribute = retrieveJobWithRetry(mainJobId);
        final List<Map<String, String>> jobProperties = mainJobAttribute.get(ShmConstants.JOBPROPERTIES) != null ? (List<Map<String, String>>) mainJobAttribute.get(ShmConstants.JOBPROPERTIES)
                : new ArrayList<Map<String, String>>();

        final long templateJobId = mainJobAttribute.get(ShmConstants.JOBTEMPLATEID) != null ? (long) mainJobAttribute.get(ShmConstants.JOBTEMPLATEID) : 0;
        final Map<String, Object> jobTemplateAttributes = retrieveJobWithRetry(templateJobId);
        final JobTemplate jobTemplate = jobMapper.getJobTemplateDetails(jobTemplateAttributes, templateJobId);

        final String jobType = jobTemplate.getJobType().name();
        final JobConfiguration jobConfiguration = jobTemplate.getJobConfigurationDetails();
        LOGGER.info("Create NEJobs: input information is [MainJobId={}, TemplateJobId={}, jobType={}, jobConfiguration={}]", mainJobId, templateJobId, jobType, jobConfiguration);

        final NEInfo neInfo = jobConfiguration.getSelectedNEs();
        final List<NeTypeComponentActivityDetails> neTypeComponentActivityDetails = neInfo.getNeTypeComponentActivityDetails() != null ? neInfo.getNeTypeComponentActivityDetails()
                : new ArrayList<NeTypeComponentActivityDetails>();
        final Map<String, Long> neJobDetails = new HashMap<>();
        final Map<String, String> neDetailsWithParentName = new HashMap<>();
        final Map<String, String> creationFailedNeJobDetails = new HashMap<>();
        final NetworkElementResponse networkElementsResponse = executorServiceHelper.getNetworkElementDetails(mainJobId, jobTemplate, jobProperties, neDetailsWithParentName);
        if (networkElementsResponse != null) {
            TBACResponse tbacResponse = null;
            List<NetworkElement> unAuthorizedNes = new ArrayList<>();
            if (!networkElementsResponse.getSupportedNes().isEmpty() || !networkElementsResponse.getUnsupportedNes().isEmpty()) {
                tbacResponse = tbacValidator.validateTBAC(networkElementsResponse, jobTemplate, mainJobId, mainJobAttribute);
                unAuthorizedNes.addAll(tbacResponse.getUnAuthorizedNes());
                unAuthorizedNes = buildUnAuthorizedNetworkElementData(unAuthorizedNes, networkElementsResponse);
                LOGGER.warn("All UnAuthorized NetworkElements are:{}", unAuthorizedNes);
            }
            if (exitIfTBACValidationFailed(tbacResponse, mainJobId, jobTemplate, attributeMap, mainJobAttribute)) {
                return;
            }
            final Map<NetworkElement, String> invalidNetworkElements = networkElementsResponse.getInvalidNes();
            final Map<String, List<NetworkElement>> nesWithComponents = networkElementsResponse.getNesWithComponents();
            final Map<NetworkElement, String> unSupportedNetworkElements = executorServiceHelper.getFilteredUnSupportedNodes(networkElementsResponse.getUnsupportedNes(), unAuthorizedNes);
            LOGGER.debug("Unsupported NetworkElements are:{}", unSupportedNetworkElements);

            List<NetworkElement> supportedNetworkElements = executorServiceHelper.getFilteredSupportedNodes(networkElementsResponse.getSupportedNes(), unAuthorizedNes);
            LOGGER.debug("All supported networkElements count:{} and NEs:{}", supportedNetworkElements.size(), supportedNetworkElements);
            if (nesWithComponents != null && !nesWithComponents.isEmpty()) {
                executorServiceHelper.prepareUnAuthorizedNes(unAuthorizedNes, nesWithComponents);
                executorServiceHelper.prepareInValidNes(invalidNetworkElements, nesWithComponents);
            }
            supportedNetworkElements = supportedNesListBuilder.buildSupportedNesListForNeJobsCreation(nesWithComponents, supportedNetworkElements);
            // allunSupportedNetworkElements contains unsupported and invalid and unauthorized networkElements
            allunSupportedNetworkElements.addAll(unSupportedNetworkElements.keySet());
            allunSupportedNetworkElements.addAll(invalidNetworkElements.keySet());
            allunSupportedNetworkElements.addAll(unAuthorizedNes);
            LOGGER.debug("All unsupported networkElements count: {} and NEs:{}", allunSupportedNetworkElements.size(), allunSupportedNetworkElements);

            updateNeCompletedCount(mainJobId, allunSupportedNetworkElements.size(), supportedNetworkElements.size(), attributeMap, jobProperties);

            LOGGER.debug("For main job id : {},  Number of Supported NEs : {}  and Number of UnSupported NEs : {}", mainJobId, supportedNetworkElements.size(), unSupportedNetworkElements.size());
            final JobTypeEnum jobTypeEnum = JobTypeEnum.getJobType(jobType);
            final Map<String, Object> neTypeGroupedActivities = executorServiceHelper.groupActivitiesByNeType(jobConfiguration.getActivities());
            final Map<String, Map<Object, Object>> neTypeSynchronousActivityMap = synchronousActivityProvider.prepareNextSynchronousActivityMap(jobConfiguration, neTypeGroupedActivities, jobTypeEnum);
            long neJobId = -1L;
            final List<NetworkElement> completedNes = new ArrayList<>();
            final List<NetworkElement> neJobsToCancel = new ArrayList<>();
            final Map<String, Object> supportedAndUnsupported = new HashMap<>();
            supportedAndUnsupported.put(ShmConstants.SUPPORTED_NES, supportedNetworkElements);
            supportedAndUnsupported.put(ShmConstants.UNSUPPORTED_NES, allunSupportedNetworkElements);
            List<NetworkElement> duplicateSupportedNeElements = new ArrayList<>();
            final List<String> existingNeNames = getExistingNeNames(mainJobId);
            for (final String existingNeName : existingNeNames) {
                if(null != existingNeName) {
                    for(final NetworkElement networkElement : supportedNetworkElements) {
                        if(null!=networkElement && existingNeName.equals(networkElement.getName())) {
                            duplicateSupportedNeElements.add(networkElement);
                            LOGGER.warn("Duplicate NeElements existing in DB : {}", networkElement.getName());
                        }
                    }
                }
            }
            //Removing duplicate NE jobs from the list
            supportedNetworkElements.removeAll(duplicateSupportedNeElements);
            for (final NetworkElement selectedNE : supportedNetworkElements) {
                try {
                    LOGGER.debug("Creation Processing started for NEJob on supported Node: {}", selectedNE);
                    final String businessKey = prepareBusinessKey(mainJobId, selectedNE.getName());
                    final String neType = selectedNE.getNeType();
                    final Map<String, Object> neJobCreationDetails = builderRetryProxy.createNeJob(mainJobId, businessKey, selectedNE, neDetailsWithParentName, supportedAndUnsupported);
                    neJobId = (Long) neJobCreationDetails.get(ShmConstants.NE_JOB_ID);
                    if (neJobId > 0) {
                        try {
                            activityJobPos = createActivityJobs(neJobId, selectedNE, neDetailsWithParentName, neTypeComponentActivityDetails, neTypeGroupedActivities);
                            LOGGER.info("Submitting to WFS for node[{}] with information as [businessKey={}, platformType={},neType={},jobTypeEnum={} ]", selectedNE, businessKey,
                                    selectedNE.getPlatformType(), selectedNE.getNeType(), jobTypeEnum);
                            result = submitWorkflowInstance(neJobId, businessKey, activityJobPos, jobConfiguration, jobTypeEnum, selectedNE, neTypeSynchronousActivityMap);
                            neJobDetails.put(selectedNE.getName(), neJobId);
                            if (result) {
                                submittedNEs.add(selectedNE.getName());
                                final Map<String, String> selectedNeWithParentNameMap = neComponentsInfoBuilderImpl.findParentNeNameForSelectedNe(selectedNE, neDetailsWithParentName);
                                final NEJobStaticData neJobStaticData = new NEJobStaticData(neJobId, mainJobId, selectedNE.getName(), businessKey, selectedNE.getPlatformType().toString(), 0,
                                        selectedNeWithParentNameMap.get(selectedNE.getName()));
                                populateCache(activityJobPos, neJobStaticData, jobType, neType, jobConfiguration, selectedNE.getPlatformType());
                            }
                            completedNes.add(selectedNE);
                        } catch (final WorkflowServiceInvocationException e) {
                            unsubmittedNeJobIds.put(selectedNE.getName(), neJobId);
                            LOGGER.error("Job submission to workflows is failed for node: {} due to ::{}", selectedNE, e.getMessage());
                            creationFailedNeJobDetails.put(selectedNE.getName(), String.format(ShmConstants.CREATION_FAILED_MSG, e.getMessage()));
                        } catch (final Exception e) {
                            LOGGER.error("Exception while submitting NE job for the selected NE: {} with job type : {}", selectedNE, jobType, e);
                            final String logMessage = String.format("NEjob submission failed for the selected nodes: \"%s\" for mainJobId: \"%s\"", selectedNE, mainJobId);
                            systemRecorder.recordEvent(SHMEvents.NEJOBS_SUBMISSION_FAILED, EventLevel.COARSE, MAIN_JOB_ID_STRING + mainJobId, WORKFLOW_ID_STRING + wfsId, logMessage);
                            creationFailedNeJobDetails.put(selectedNE.getName(), String.format(ShmConstants.CREATION_FAILED_MSG, e.getMessage()));
                            unsubmittedNeJobIds.put(selectedNE.getName(), neJobId);
                        }
                    } else if (neJobCreationDetails.containsValue(ShmConstants.CANCELLED)) {
                        supportedNetworkElements.removeAll(completedNes);
                        neJobsToCancel.addAll(supportedNetworkElements);
                        final String message = String.format("Skipping NE Job on node: %s  execution as main job: %s is in COMPLETED/CANCELLING/SYSTEM_CANCELLED/DELETING/SYSTEM_CANCELLING state",
                                selectedNE.getName(), mainJobId);
                        LOGGER.info(message);
                        creationFailedNeJobDetails.put(selectedNE.getName(), message);
                        break;
                    } else if (neJobCreationDetails.containsValue(ShmConstants.CREATION_FAILED)) {
                        LOGGER.warn("NEJob creation failed for node: {}", selectedNE.getName());
                        creationFailedJobs.add(selectedNE.getName());
                        if (neJobCreationDetails.containsKey(ShmConstants.CREATION_FAILURE_CAUSE)) {
                            creationFailedNeJobDetails.put(selectedNE.getName(), (String) neJobCreationDetails.get(ShmConstants.CREATION_FAILURE_CAUSE));
                        }
                    } else {
                        LOGGER.info("An NEjob already exists for NE: {}", selectedNE.getName());
                        creationFailedNeJobDetails.put(selectedNE.getName(), "Job already exists");
                        existingNeJobsCount++;
                    }
                } catch (final Exception e) {
                    LOGGER.error("Exception while creating NE job for the selected NE : {} with job type :{}", selectedNE, jobType, e);
                    final String logMessage = String.format("NEjob creation failed for the selected nodes: \"%s\" for mainJobId: \"%s\"", selectedNE, mainJobId);
                    systemRecorder.recordEvent(SHMEvents.NEJOBS_CREATION_FAILED, EventLevel.COARSE, MAIN_JOB_ID_STRING + mainJobId, WORKFLOW_ID_STRING + wfsId, logMessage);
                    unsubmittedNeJobIds.put(selectedNE.getName(), neJobId);
                    creationFailedNeJobDetails.put(selectedNE.getName(), String.format(ShmConstants.CREATION_FAILED_MSG, e.getMessage()));
                }
            }
            if (!neJobsToCancel.isEmpty()) {
                allunSupportedNetworkElements.addAll(neJobsToCancel);
            }
            for (final NetworkElement networkElement : allunSupportedNetworkElements) {
                try {
                    LOGGER.debug("Creation Processing started for NEJob on un-supported node: {}", networkElement);

                    final String businessKey = prepareBusinessKey(mainJobId, networkElement.getName());
                    String logMessage = "";
                    String jobResult = null;
                    if (invalidNetworkElements.containsKey(networkElement)) {
                        logMessage = invalidNetworkElements.get(networkElement);
                        jobResult = JobResult.FAILED.getJobResult();
                    } else if (unAuthorizedNes.contains(networkElement)) {
                        final String owner = getJobExecutionUser(mainJobAttribute, jobTemplate);
                        logMessage = String.format(JobExecutorConstants.TBAC_ACCESS_DENIED_AT_NE_LEVEL, owner);
                        jobResult = JobResult.FAILED.getJobResult();
                    } else if (unSupportedNetworkElements.containsKey(networkElement)) {
                        logMessage = unSupportedNetworkElements.get(networkElement);
                        jobResult = JobResult.FAILED.getJobResult();
                    } else if (neJobsToCancel.contains(networkElement)) {
                        logMessage = "Cancelling job execution as cancel triggered on main job for networkElement " + networkElement.getName();
                        jobResult = JobResult.CANCELLED.getJobResult();
                    }
                    final Map<String, Object> creationDetails = builderRetryProxy.createNEJobWithCompletedState(mainJobId, networkElement, jobResult, logMessage, businessKey);
                    if (creationDetails.containsValue(ShmConstants.CREATION_FAILED)) {
                        LOGGER.warn("NEJob creation failed for node={}", networkElement);
                        creationFailedJobs.add(networkElement.getName());
                        if (creationDetails.containsKey(ShmConstants.CREATION_FAILURE_CAUSE)) {
                            creationFailedNeJobDetails.put(networkElement.getName(), (String) creationDetails.get(ShmConstants.CREATION_FAILURE_CAUSE));
                        }
                        continue;
                    } else if (creationDetails.containsValue(ShmConstants.EXISTED)) {
                        LOGGER.info("NEjob already exists for mainJobId: {} , businessKey: {}, networkElement: {}", mainJobId, businessKey, networkElement.getName());
                        existingNeJobsCount++;
                    } else {
                        creationFailedNeJobDetails.put(networkElement.getName(), logMessage);
                    }
                } catch (final Exception e) {
                    LOGGER.error("Exception while creating NE job for the selected NE: {} with job type :{}", networkElement.getName(), jobType, e);
                    creationFailedNeJobDetails.put(networkElement.getName(), String.format(ShmConstants.CREATION_FAILED_MSG, e.getMessage()));
                }
            }
        }
        notifyWFSwithSendAllNeDone(existingNeJobsCount, submittedNEs, templateJobId);
        LOGGER.info("So finally we have , submittedNEs=={}, existingNeJobsCount=={}, creationFailedJobs=={}, unsubmittedNeJobIds=={}", submittedNEs.size(), existingNeJobsCount,
                creationFailedJobs.size(), unsubmittedNeJobIds.size());
        final List<Map<String, Object>> jobLogList = updateJobLogs(submittedNEs, unsubmittedNeJobIds, mainJobId, wfsId);
        final Date startTime = (Date) mainJobAttribute.get(ShmConstants.STARTTIME);
        final long timeForNeJobsCreation = new Date().getTime() - startTime.getTime();
        final Map<String, Object> property = new HashMap<>();
        property.put(ShmConstants.KEY, ShmConstants.DURATION_FOR_NEJOBS_CREATION);
        property.put(ShmConstants.VALUE, String.valueOf(timeForNeJobsCreation));
        jobUpdateService.readAndUpdateRunningJobAttributes(mainJobId, Arrays.asList(property), jobLogList, null);
        finishUnsubmittedJobsAsFailed(mainJobId, unsubmittedNeJobIds);
        if (JobCategory.FA == jobTemplate.getJobCategory() || JobCategory.NHC_FA == jobTemplate.getJobCategory()) {
            sendNeJobDetailsToFA(neJobDetails, creationFailedNeJobDetails, mainJobId, jobTemplate, jobTemplate.getJobCategory());
        }

    }

    private List<NetworkElement> buildUnAuthorizedNetworkElementData(final List<NetworkElement> unAuthorizedNes, final NetworkElementResponse networkElementsResponse) {
        final List<NetworkElement> unAuthorizedNesData = new ArrayList<>();
        for (final NetworkElement unAuthorizedNe : unAuthorizedNes) {
            for (final NetworkElement supportedNetworkElement : networkElementsResponse.getSupportedNes()) {
                if (unAuthorizedNe.getName().equals(supportedNetworkElement.getName())) {
                    unAuthorizedNesData.add(supportedNetworkElement);
                }
            }
            for (final NetworkElement unSupportedNetworkElement : networkElementsResponse.getUnsupportedNes().keySet()) {
                if (unAuthorizedNe.getName().equals(unSupportedNetworkElement.getName())) {
                    unAuthorizedNesData.add(unSupportedNetworkElement);
                }
            }
        }
        return unAuthorizedNesData;
    }

    private List<Map<String, Object>> prepareActivityJobsList(final List<Map<String, Object>> activityJobPos, final JobConfiguration jobConfiguration, final NetworkElement networkElement) {
        final List<Map<String, Object>> activityJobsList = new ArrayList<Map<String, Object>>();
        final String delims = " ";

        for (final Map<String, Object> activityJobAttributes : activityJobPos) {
            final Map<String, Object> activityJobs = new HashMap<>();
            activityJobs.put(JobVariables.POID, activityJobAttributes.get(ShmConstants.PO_ID));
            activityJobs.put(ShmConstants.NAME, activityJobAttributes.get(ShmConstants.NAME));
            activityJobs.put(ShmConstants.ORDER, activityJobAttributes.get(ShmConstants.ORDER));
            if (jobConfiguration.getActivities() != null) {
                prepareActivityJobAttributes(jobConfiguration, networkElement, delims, activityJobAttributes, activityJobs);
            }
            LOGGER.debug(" ActivityJobs :{}", activityJobs);
            activityJobsList.add(activityJobs);
        }

        return activityJobsList;
    }

    private List<Map<String, Object>> createActivityJobs(final long neJobId, final NetworkElement selectedNE, final Map<String, String> neDetailsWithParentName,
            final List<NeTypeComponentActivityDetails> neTypeComponentActivityDetails, final Map<String, Object> neTypeGroupedActivities) {
        List<Map<String, Object>> activityJobPos = null;
        final List<Activity> selectedComponentActivities = executorServiceHelper.getComponentsActivities(neTypeComponentActivityDetails, neTypeGroupedActivities, selectedNE.getName());
        if (selectedComponentActivities.isEmpty()) {
            activityJobPos = builderRetryProxy.createActivityJobs(neJobId, (List<Activity>) neTypeGroupedActivities.get(selectedNE.getNeType()), neDetailsWithParentName, selectedNE.getName());
        } else {
            activityJobPos = builderRetryProxy.createActivityJobs(neJobId, selectedComponentActivities, neDetailsWithParentName, selectedNE.getName());
        }
        return activityJobPos;
    }

    private void prepareActivityJobAttributes(final JobConfiguration jobConfiguration, final NetworkElement networkElement, final String delims, final Map<String, Object> activityJobAttributes,
            final Map<String, Object> activityJobs) {
        String activityName;
        String executionMode;
        for (final Activity activity : jobConfiguration.getActivities()) {
            if (activity.getName().equals(activityJobAttributes.get(ShmConstants.NAME)) && activity.getNeType().equals(networkElement.getNeType())) {
                executionMode = activity.getSchedule().getExecMode().toString().toLowerCase();
                activityJobs.put(ShmConstants.EXECUTION_MODE, executionMode);
                activityName = activity.getName();

                LOGGER.debug("Activity Name :{} and its execMode is :{}", activityName, executionMode);

                if (activity.getSchedule().getExecMode().toString().equalsIgnoreCase(JobVariables.ACTIVITY_STARTUP_SCHEDULED)) {
                    final List<ScheduleProperty> scheduleProperties = activity.getSchedule().getScheduleAttributes();
                    prepareScheduleProperties(delims, activityJobs, scheduleProperties);
                }
            }
        }
    }

    private void prepareScheduleProperties(final String delims, final Map<String, Object> activityJobs, final List<ScheduleProperty> scheduleProperties) {
        final String formattedScheduleTime;
        final String scheduleTime;
        for (final ScheduleProperty scheduleProperty : scheduleProperties) {
            if (scheduleProperty.getName().equals(ShmConstants.START_DATE)) {
                scheduleTime = scheduleProperty.getValue();
                LOGGER.debug("Schedule time : {}", scheduleTime);
                final StringTokenizer st = new StringTokenizer(scheduleTime, delims);
                formattedScheduleTime = st.nextToken() + "T" + st.nextToken();
                activityJobs.put(ShmConstants.SCHEDULE_TIME, formattedScheduleTime);
                break;
            }
        }
    }

    private void sendNeJobDetailsToFA(final Map<String, Long> neJobDetails, final Map<String, String> creationFailedNeJobDetails, final long mainJobId, final JobTemplate jobTemplate,
            final JobCategory jobCategory) {
        final List<JobProperty> properties = jobTemplate.getJobConfigurationDetails().getJobProperties();
        LOGGER.debug("Job properties from job template: {}", properties);
        final HashMap<String, Object> faResponse = new HashMap<>();
        faResponse.put(ShmJobConstants.FaCommonConstants.NE_JOB_DETAILS, neJobDetails);
        faResponse.put(ShmJobConstants.MAIN_JOB_ID, mainJobId);
        faResponse.put(ShmJobConstants.FaCommonConstants.FAILED_NE_JOB_DETAILS, creationFailedNeJobDetails);
        for (final JobProperty property : properties) {
            if (ShmJobConstants.FaCommonConstants.FA_REQUEST_ID.equals(property.getKey())) {
                faResponse.put(ShmJobConstants.FaCommonConstants.FA_REQUEST_ID, property.getValue());
            } else if (ShmJobConstants.FaCommonConstants.FLOW_EXECUTION_NAME.equals(property.getKey())) {
                faResponse.put(ShmJobConstants.FaCommonConstants.FLOW_EXECUTION_NAME, property.getValue());
            } else if (ShmJobConstants.USERNAME.equals(property.getKey())) {
                faResponse.put(ShmJobConstants.USERNAME, property.getValue());
            } else if (ShmJobConstants.FaCommonConstants.FBB_TYPE.equals(property.getKey())) {
                faResponse.put(ShmJobConstants.FaCommonConstants.FBB_TYPE, property.getValue());
            }
        }
        faBuildingBlocksResponseSender.send(faResponse, jobCategory);
    }

    private String getJobExecutionUser(final Map<String, Object> mainJobAttribute, final JobTemplate jobTemplate) {
        final String executionMode = jobTemplate.getJobConfigurationDetails().getMainSchedule().getExecMode().getMode();
        String owner = jobTemplate.getOwner();
        if (executionMode.equalsIgnoreCase(ExecMode.MANUAL.getMode())) {
            owner = mainJobAttribute.get(ShmConstants.SHM_JOB_EXEC_USER) != null ? (String) mainJobAttribute.get(ShmConstants.SHM_JOB_EXEC_USER) : "";
        }
        return owner;
    }

    private void populateCache(final List<Map<String, Object>> activityJobPos, final NEJobStaticData jobStaticData, final String jobType, final String neType, final JobConfiguration jobConfiguration,
            final PlatformTypeEnum platformType) {
        for (final Map<String, Object> activityJobPo : activityJobPos) {
            final long activityJobId = (long) activityJobPo.get(ShmConstants.PO_ID);
            neJobStaticDataProvider.put(activityJobId, jobStaticData);
        }
        final List<AxeSynchronousActivityData> cacheList = axeSyncActivityDatProvider.get(jobStaticData.getMainJobId(), neType);
        if (cacheList.isEmpty() && JobType.UPGRADE.equals(JobType.getJobType(jobType)) && PlatformTypeEnum.AXE.equals(platformType)) {
            final List<AxeSynchronousActivityData> axeSyncActivityDataList = new ArrayList<>();
            LOGGER.debug("populateCache for Sync Activity");
            for (final Map<String, Object> activityJobPo : activityJobPos) {
                final boolean isSyncActivity = synchronousActivityProvider.isActivitySynchronous(jobConfiguration, neType, (String) activityJobPo.get(ShmConstants.ACTIVITY_NAME));
                LOGGER.debug("Populate isSyncActivity {} in cache {}", activityJobPo.get(ShmConstants.ACTIVITY_NAME), isSyncActivity);
                if (isSyncActivity) {
                    final AxeSynchronousActivityData axeSyncActivityData = new AxeSynchronousActivityData((String) activityJobPo.get(ShmConstants.ACTIVITY_NAME),
                            (int) activityJobPo.get(ShmConstants.ACTIVITY_ORDER));
                    axeSyncActivityDataList.add(axeSyncActivityData);
                }
            }
            axeSyncActivityDatProvider.put(jobStaticData.getMainJobId(), neType, axeSyncActivityDataList);
        }
    }

    public boolean submitWorkflowInstance(final long neJobId, final String businessKey, final List<Map<String, Object>> activityJobPos, final JobConfiguration jobConfiguration,
            final JobTypeEnum jobTypeEnum, final NetworkElement networkElement, final Map<String, Map<Object, Object>> neTypeSynchronousActivityMap) {
        final List<Map<String, Object>> activityJobsList = prepareActivityJobsList(activityJobPos, jobConfiguration, networkElement);
        boolean result = false;
        final Map<String, Object> processVariables = processVariableBuilder.build(jobTypeEnum, networkElement, activityJobsList, neJobId, neTypeSynchronousActivityMap);
        try {
            final WorkFlowObject workFlowObject = new WorkFlowObject();
            workFlowObject.setWorkflowDefinitionId(workflowDefinitionsProvider.getWorkflowDefinition(networkElement.getPlatformType(), jobTypeEnum));
            workFlowObject.setProcessVariables(processVariables);
            LOGGER.info("Submitting the workflow with definionId :: {} and process variables : {}", workFlowObject.getWorkflowDefinitionId(), processVariables);
            final String workFlowInstanceId = workflowInstanceNotifier.submitWorkFlowInstance(businessKey, workFlowObject);
            final Map<String, Object> validatedAttributes = new HashMap<>();
            if (workFlowInstanceId != null) {
                validatedAttributes.put(ShmConstants.WFS_ID, workFlowInstanceId);
            }
            jobUpdateService.updateJobAttributes(neJobId, validatedAttributes);
            result = true;
        } catch (final WorkflowServiceInvocationException e) {
            LOGGER.error("Failed to instantiate workflow due to:", e);
            updateJobLog(neJobId, String.format(JobExecutorConstants.WORKFLOW_SUBMISSION_FAILED, neJobId));
        }

        return result;
    }

    protected void updateJobLog(final long jobId, final String message) {
        final Map<String, Object> activityAttributes = new HashMap<String, Object>();
        activityAttributes.put(ActivityConstants.JOB_LOG_MESSAGE, message);
        activityAttributes.put(ActivityConstants.JOB_LOG_ENTRY_TIME, new Date());
        activityAttributes.put(ActivityConstants.JOB_LOG_TYPE, JobLogType.SYSTEM.toString());
        activityAttributes.put(ActivityConstants.JOB_LOG_LEVEL, JobLogLevel.INFO.toString());

        final Map<String, Object> jobAttributes = new HashMap<String, Object>();
        jobAttributes.put(ShmConstants.LOG, Arrays.asList(activityAttributes));
        jobUpdateService.updateJobAttributes(jobId, jobAttributes);
    }

    private void updateNeCompletedCount(final long mainJobId, final int unSupportedNesSize, final int supportedNesSize, final Map<String, Object> attributeMap,
            final List<Map<String, String>> neJobsPropertyList) {
        final Map<String, String> neCompleted = new HashMap<String, String>();
        final Map<String, String> neSubmitted = new HashMap<String, String>();
        neSubmitted.put(ShmConstants.KEY, ShmConstants.SUBMITTED_NES);
        neSubmitted.put(ShmConstants.VALUE, Integer.toString(supportedNesSize + unSupportedNesSize));
        neJobsPropertyList.add(neSubmitted);
        neCompleted.put(ShmConstants.KEY, ShmConstants.NE_COMPLETED);
        neCompleted.put(ShmConstants.VALUE, Integer.toString(unSupportedNesSize));
        neJobsPropertyList.add(neCompleted);
        attributeMap.put(ShmConstants.JOBPROPERTIES, neJobsPropertyList);
        attributeMap.put(ShmConstants.NO_OF_NETWORK_ELEMENTS, supportedNesSize + unSupportedNesSize);
        updateJobAtributes(mainJobId, attributeMap);
        LOGGER.debug("Attributes Updated for the Job with Id {} with attributes {}", mainJobId, attributeMap);
    }

    /**
     * getting all unsupported NEs map from list of NEs MAPs
     * 
     * @param unsupportedNEsList
     * @return unsupportedNesMap
     */
    public Map<NetworkElement, String> getUnsupportedNes(final List<Map<NetworkElement, String>> unsupportedNEsList) {
        final Map<NetworkElement, String> unsupportedNesMap = new HashMap<NetworkElement, String>();
        if (unsupportedNEsList != null && !unsupportedNEsList.isEmpty()) {
            for (final Map<NetworkElement, String> unsupportedNes : unsupportedNEsList) {
                if (unsupportedNes != null && !unsupportedNes.isEmpty()) {
                    unsupportedNesMap.putAll(unsupportedNes);
                }
            }
        }
        return unsupportedNesMap;
    }

    public List<Map<String, Object>> prepareJobPropertyList(final List<Map<String, Object>> jobPropertyList, final String propertyName, final String propertyValue) {
        final Map<String, Object> jobProperty = new HashMap<>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, propertyName);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, propertyValue);
        jobPropertyList.add(jobProperty);
        return jobPropertyList;

    }

    private void notifyWFSwithSendAllNeDone(final int existingNeJobsCount, final List<String> submittedNEs, final long templateJobId) {
        if (submittedNEs.isEmpty() && existingNeJobsCount == 0 && templateJobId != -1) {
            try {
                workflowInstanceNotifier.sendAllNeDone(Long.toString(templateJobId));
                LOGGER.info("NE Job Done message sent to wrokflow for : {}", templateJobId);
            } catch (final WorkflowServiceInvocationException e) {
                LOGGER.error("Failed to send message to WorkFlow Service due to:", e);
            }
        }
    }

    private List<Map<String, Object>> updateJobLogs(final List<String> submittedNEs, final Map<String, Long> unsubmittedNeJobIds, final long mainJobId, final String wfsId) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobLog = new HashMap<String, Object>();
        String logMessage;
        if (!submittedNEs.isEmpty()) {
            logMessage = "NE level Jobs created and submitted for execution for nodes: " + submittedNEs;
            jobLog.put(ActivityConstants.JOB_LOG_ENTRY_TIME, new Date());
            jobLog.put(ActivityConstants.JOB_LOG_TYPE, JobLogType.SYSTEM.toString());
            jobLog.put(ActivityConstants.JOB_LOG_LEVEL, JobLogLevel.DEBUG.toString());
            jobLog.put(ActivityConstants.JOB_LOG_MESSAGE, logMessage);
            jobLogList.add(jobLog);
            systemRecorder.recordEvent(SHMEvents.NEJOBS_SUBMITTED, EventLevel.COARSE, MAIN_JOB_ID_STRING + mainJobId, WORKFLOW_ID_STRING + wfsId, logMessage);
        }

        if (!unsubmittedNeJobIds.isEmpty()) {
            logMessage = "execute called again for main job skipping the already created Ne jobs for following node names." + unsubmittedNeJobIds.keySet();
            jobLog.put(ActivityConstants.JOB_LOG_ENTRY_TIME, new Date());
            jobLog.put(ActivityConstants.JOB_LOG_TYPE, JobLogType.SYSTEM.toString());
            jobLog.put(ActivityConstants.JOB_LOG_MESSAGE, logMessage);
            jobLogList.add(jobLog);
            LOGGER.warn(logMessage);
            systemRecorder.recordEvent(SHMEvents.NEJOBS_SUBMITTED, EventLevel.COARSE, MAIN_JOB_ID_STRING + mainJobId, WORKFLOW_ID_STRING + wfsId, logMessage);
        }
        return jobLogList;
    }

    private void finishUnsubmittedJobsAsFailed(final long mainJobId, final Map<String, Long> unsubmittedNeJobIds) {
        LOGGER.info("Jobs that are failed to submit to workflow service are :: {}", unsubmittedNeJobIds);
        for (final Map.Entry<String, Long> entrySet : unsubmittedNeJobIds.entrySet()) {
            LOGGER.debug("Now finishing the  NE job as skipped. [mainJobId=={}, nejobId=={}]", mainJobId, entrySet.getValue());
            final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
            neJobAttributes.put(ShmConstants.RESULT, JobResult.FAILED.getJobResult());
            neJobAttributes.put(ShmConstants.STATE, JobState.COMPLETED.getJobStateName());
            neJobAttributes.put(ShmConstants.ENDTIME, new Date());
            jobUpdateService.updateJobAttributes(entrySet.getValue(), neJobAttributes);
        }
    }

    private String prepareBusinessKey(final long mainJobId, final String selectedNE) {
        return mainJobId + "@" + selectedNE;
    }

    private void logSkippedEvent(final String jobName, final int latestJobExecutionIndex, final JobState jobState) {
        final String jobNameWithExecutionIndex = jobName + ShmConstants.DELIMITER_UNDERSCORE + latestJobExecutionIndex;
        LOGGER.info("Job({}) execution is skipped as the previous job is still in {} state", jobNameWithExecutionIndex, jobState.getJobStateName());
        systemRecorder.recordEvent(SHMEvents.JOB_SKIPPED, EventLevel.COARSE, jobNameWithExecutionIndex, ShmConstants.JOB,
                "Job execution is skipped as the previous job is still in " + jobState.getJobStateName() + ShmConstants.STATE);
    }

    @Override
    @Asynchronous
    public void cancelJobs(final List<Long> jobIds, final String cancelledBy) {
        // Get Job type of one of the jobs. Rest of the jobs will be of the same
        // type
        boolean isMainJob = false;
        final List<Map<String, Object>> jobs = retriveJobsWithRetry(jobIds);
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        if (!jobs.isEmpty() && jobs.get(0) != null && ((String) jobs.get(0).get(ShmConstants.NAMESPACE)).equals(ShmConstants.NAMESPACE)
                && ((String) jobs.get(0).get(ShmConstants.TYPE)).equals(ShmConstants.JOB)) {
            isMainJob = true;
        }
        if (isMainJob) {
            LOGGER.debug("Main jobs cancelling starts");
            for (final Map<String, Object> job : jobs) {
                final String jobStateName = (String) job.get(ShmConstants.STATE);
                final JobState jobState = JobState.getJobState(jobStateName);
                final long poId = (long) job.get(ShmConstants.PO_ID);
                if (JobState.isJobInactive(jobState) || JobState.isJobCancelInProgress(jobState)) {
                    skipCancelAndUpdateMainJobLogs(jobLogList, job, jobState, poId);
                } else {
                    validateAndCancelMainJob(job, cancelledBy, jobState);
                }
            }
        } else { // NE JOB
            LOGGER.debug("NE jobs cancelling starts");
            final List<Map<String, Object>> neJobs = retriveJobsWithRetry(jobIds);
            final List<Map<String, Object>> tbacValidatedNeJobs = getTbacValidatedNeJobs(SHMEvents.JOB_CANCEL_SKIPPED, cancelledBy, neJobs);
            cancelNEJobs(tbacValidatedNeJobs, cancelledBy);
        }
    }

    private void validateAndCancelMainJob(final Map<String, Object> job, final String cancelledBy, final JobState jobState) {

        final long jobId = (long) job.get(ShmConstants.PO_ID);
        logCancelledBy(jobId, cancelledBy, "main job");
        final Long mainJobTemplateId = (Long) job.get(ShmConstants.JOB_TEMPLATE_ID);
        if (JobState.isJobCreated(jobState)) {
            final boolean isUserAuthorized = jobAdministratorTBACValidator.validateTBACForMainJob(jobId, mainJobTemplateId, cancelledBy);
            if (!isUserAuthorized) {
                logTBACFailureAtMainJobLevel(SHMEvents.JOB_CANCEL_SKIPPED, jobId, cancelledBy, (String) job.get(ShmConstants.NAME));
                return;
            }
            cancelMainJob(jobId, mainJobTemplateId);
        } else {// Get NE Jobs for main job
            LOGGER.debug("Fetch NE jobs for cancellation ");
            final List<Map<String, Object>> neJobs = getNeJobProjectedAttributes(jobId);
            if (jobState.getJobStateName().equals(JobState.RUNNING.getJobStateName()) && neJobs.isEmpty()) {
                final boolean isUserAuthorized = jobAdministratorTBACValidator.validateTBACForMainJob(jobId, mainJobTemplateId, cancelledBy);
                if (!isUserAuthorized) {
                    logTBACFailureAtMainJobLevel(SHMEvents.JOB_CANCEL_SKIPPED, jobId, cancelledBy, (String) job.get(ShmConstants.NAME));
                    return;
                }
                cancelMainJob(jobId, mainJobTemplateId);
            } else {
                validateAndCancelNeJobs(job, cancelledBy, jobId, neJobs);
            }
        }
    }

    private void logTBACFailureAtMainJobLevel(final String eventType, final long mainJobId, final String loggedInUser, final String jobName) {
        final String logMessage = String.format(JobExecutorConstants.TBAC_ACCESS_DENIED_AT_JOB_LEVEL, loggedInUser);
        systemRecorder.recordEvent(eventType, EventLevel.COARSE, jobName, ShmConstants.JOB, logMessage);
        logTBACFailure(mainJobId, logMessage);
    }

    private void logTBACFailure(final long jobid, final String message) {
        LOGGER.debug("Logging TBAC Failure for jobId {} with info {}", jobid, message);
        final List<Map<String, Object>> jobLogList = prepareJobLogs(message, JobLogLevel.WARN);
        jobUpdateService.addOrUpdateOrRemoveJobProperties(jobid, new HashMap<String, String>(), jobLogList);
    }

    private void cancelMainJob(final Long poId, final Long mainJobTemplateId) {
        // Set job state as canceling
        updateJobStateAsCancelling(poId);
        try {
            workflowInstanceNotifier.sendMessageToWaitingWFSInstance(WorkFlowConstants.CANCELMAINJOB_WFMESSAGE, mainJobTemplateId.toString());
        } catch (final WorkflowServiceInvocationException e) {
            LOGGER.error("Failed to send message to WorkFlow Services. Due to:", e);
            propagateCancelToMainJob(poId, mainJobTemplateId.toString());
        }
    }

    private void propagateCancelToMainJob(final long mainjobId, final String businessKey) {
        final Map<String, Object> poAttributes = new HashMap<>();
        final Map<String, Object> mainJobAttributes = jobUpdateService.retrieveJobWithRetry(mainjobId);
        final String jobState = (String) mainJobAttributes.get(ShmConstants.STATE);
        final String wfsId = (String) mainJobAttributes.get(ShmConstants.WFS_ID);
        LOGGER.debug("jobState {} and wfsId {} in propagateCancelToMainJob", jobState, wfsId);
        if (isWFSInstanceAlive(wfsId)) {
            LOGGER.debug("Workflow instance for businessKey::{} is still active,so explicitly making mainjob {} as cancelled.", businessKey, mainjobId);
            final boolean isJobCompleted = JobState.isJobCompleted(JobState.getJobState(jobState));
            if (!isJobCompleted) {
                updateMainJobAsCancelled(mainjobId, poAttributes, wfsId);
            }
        } else {
            if (jobState != JobState.COMPLETED.getJobStateName()) {
                updateMainJobAsCancelled(mainjobId, poAttributes, wfsId);
            }
        }
    }

    //Update MainJob Result as CANCELLED and cancel the workflow instance
    private void updateMainJobAsCancelled(final long mainjobId, final Map<String, Object> poAttributes, final String wfsId) {
        poAttributes.put(ShmConstants.STATE, JobState.COMPLETED.getJobStateName());
        LOGGER.debug("updating Main job Result to Cancelled for the  mainjobid: {}", mainjobId);
        poAttributes.put(ShmConstants.RESULT, JobResult.CANCELLED.getJobResult());
        poAttributes.put(ShmConstants.ENDTIME, new Date());
        updateJobAtributes(mainjobId, poAttributes);
        workflowInstanceNotifier.cancelWorkflowInstance(wfsId);
    }

    /**
     *
     * Queries the postgres DB , whether the workflow instance exists for given wfsID or not.
     * <p>
     * Returns:-
     * <li>TRUE - If workflow instance is found in postgres DB. (OR ) Incase of any exception occurs (assumes the
     * Workflow is active for retrying).
     * <li>FALSE - If workflow instance is not found.
     *
     * @param wfsId
     * @return
     *         <li>Boolean
     */
    private boolean isWFSInstanceAlive(final String wfsId) {
        try {
            final QueryBuilder batchWorkflowQueryBuilder = QueryBuilderFactory.getDefaultQueryBuilder();
            final Query wfsQuery = batchWorkflowQueryBuilder.createTypeQuery(QueryType.WORKFLOW_INSTANCE_QUERY);
            final RestrictionBuilder restrictionBuilder = wfsQuery.getRestrictionBuilder();
            final Restriction restrictionOnWfsId = restrictionBuilder.isEqual(WorkflowInstanceQueryAttributes.QueryParameters.WORKFLOW_INSTANCE_ID, wfsId);
            wfsQuery.setRestriction(restrictionOnWfsId);
            final List<WorkflowObject> wfsInstances = workflowInstanceNotifier.executeWorkflowQuery(wfsQuery);
            return wfsInstances != null && !wfsInstances.isEmpty();
        } catch (final Exception e) {
            LOGGER.error("Workflow execution query failed for wfsID:{}, due to :", wfsId, e);
            return true;
        }

    }

    private void updateJobStateAsCancelling(final Long jobPoId) {
        final Map<String, Object> poAttr = new HashMap<>();
        poAttr.put(ShmConstants.STATE, JobState.CANCELLING.getJobStateName());
        updateJobAtributes(jobPoId, poAttr);
    }

    private void validateAndCancelNeJobs(final Map<String, Object> job, final String cancelledBy, final long jobId, final List<Map<String, Object>> neJobs) {
        if (tbacConfigurationProvider.isTBACAtJobLevel()) {
            final List<String> nodeNames = getNodeNames(neJobs);
            final boolean isUserAuthorized = jobAdministratorTBACValidator.validateTBACAtJobLevel(nodeNames, cancelledBy);
            if (!isUserAuthorized) {
                logTBACFailureAtMainJobLevel(SHMEvents.JOB_CANCEL_SKIPPED, jobId, cancelledBy, (String) job.get(ShmConstants.NAME));
                return;
            }
            // Set job state as canceling
            updateJobStateAsCancelling(jobId);
            cancelNEJobs(neJobs, cancelledBy);
        } else {
            final List<Map<String, Object>> tbacValidatedNeJobs = getTbacValidatedNeJobs(SHMEvents.JOB_CANCEL_SKIPPED, cancelledBy, neJobs);
            if (!tbacValidatedNeJobs.isEmpty()) {
                // Set job state as canceling
                updateJobStateAsCancelling(jobId);
                cancelNEJobs(tbacValidatedNeJobs, cancelledBy);
            }
        }
    }

    private List<String> getNodeNames(final List<Map<String, Object>> neJobs) {
        final List<String> nodeNames = new ArrayList<>();
        for (final Map<String, Object> neJob : neJobs) {
            final String nodeName = (String) neJob.get(ShmConstants.NE_NAME);
            nodeNames.add(nodeName);
        }
        return nodeNames;
    }

    private List<Map<String, Object>> getTbacValidatedNeJobs(final String eventType, final String loggedInUser, final List<Map<String, Object>> neJobs) {
        final List<Map<String, Object>> tbacValidatedNeJobs = new ArrayList<>();
        for (final Map<String, Object> neJob : neJobs) {
            final String nodeName = getNodeName(neJob);
            final boolean isUserAuthorized = jobAdministratorTBACValidator.validateTBACForNEJob(nodeName, loggedInUser);
            if (!isUserAuthorized) {
                logTBACFailureAtNeJobLevel(eventType, (Long) neJob.get(ShmConstants.PO_ID), loggedInUser, nodeName);
                continue;
            }
            tbacValidatedNeJobs.add(neJob);
        }
        return tbacValidatedNeJobs;
    }

    private String getNodeName(final Map<String, Object> neJob) {
        final List<Map<String, String>> jobProperties = (List<Map<String, String>>) neJob.get(ShmJobConstants.JOBPROPERTIES);
        final Map<String, String> jobProperty = new HashMap<>();
        jobProperty.put(ShmConstants.KEY, ShmConstants.IS_COMPONENT_JOB);
        jobProperty.put(ShmConstants.VALUE, "true");
        String nodeName = (String) neJob.get(ShmConstants.NE_NAME);
        if (jobProperties != null && !jobProperties.isEmpty() && jobProperties.contains(jobProperty)) {
            for (final Map<String, String> nejobProperty : jobProperties) {
                if (nejobProperty.get(ShmConstants.KEY).equals(ShmConstants.PARENT_NAME)) {
                    nodeName = nejobProperty.get(ShmConstants.VALUE);
                }
            }
        }
        return nodeName;
    }

    private List<Map<String, Object>> getNeJobProjectedAttributes(final long poId) {
        final Map<Object, Object> restrictionAttributes = new HashMap<>();
        restrictionAttributes.put(ShmConstants.MAIN_JOB_ID, poId);
        final List<String> projectedAttributes = new ArrayList<>();
        projectedAttributes.add(ShmConstants.WFS_ID);
        projectedAttributes.add(ShmConstants.BUSINESS_KEY);
        projectedAttributes.add(ShmConstants.STATE);
        projectedAttributes.add(ShmConstants.NE_NAME);
        return getProjectedAttributesForJobs(ShmConstants.NAMESPACE, ShmConstants.NEJOB_TYPE, restrictionAttributes, projectedAttributes);
    }

    private void skipCancelAndUpdateMainJobLogs(final List<Map<String, Object>> jobLogList, final Map<String, Object> job, final JobState jobState, final long poId) {
        final Map<String, Object> cancelJobSkipped = new HashMap<>();
        systemRecorder.recordEvent(SHMEvents.JOB_CANCEL_SKIPPED, EventLevel.COARSE, (String) job.get(ShmConstants.NAME), ShmConstants.JOB,
                "cancel Job action is skipped as job reached" + jobState.getJobStateName() + ShmConstants.STATE);
        cancelJobSkipped.put(ActivityConstants.JOB_LOG_MESSAGE, String.format(JobExecutorConstants.JOB_CANCEL_SKIPPED, "main"));
        cancelJobSkipped.put(ActivityConstants.JOB_LOG_ENTRY_TIME, new Date());
        cancelJobSkipped.put(ActivityConstants.JOB_LOG_TYPE, JobLogType.SYSTEM.toString());
        cancelJobSkipped.put(ActivityConstants.JOB_LOG_LEVEL, JobLogLevel.INFO.toString());
        jobLogList.add(cancelJobSkipped);
        jobUpdateService.updateRunningJobAttributes(poId, null, jobLogList);
    }

    private void cancelNEJobs(final List<Map<String, Object>> neJobs, final String cancelledBy) {
        LOGGER.info("Cancelling neJobs with data {}", neJobs);
        for (final Map<String, Object> neJob : neJobs) {
            final String jobStateName = (String) neJob.get(ShmConstants.STATE);
            final String nodeName = (String) neJob.get(ShmConstants.NE_NAME);
            final JobState jobState = JobState.getJobState(jobStateName);
            if (JobState.isJobInactive(jobState) || JobState.isJobCancelInProgress(jobState)) {
                skipCancelAndUpdateNeJobLogs(neJob, nodeName, jobState);
            } else if (JobState.isJobCreated(jobState)) {
                logCancelledBy((Long) neJob.get(ShmConstants.PO_ID), cancelledBy, nodeName);
                cancelNeJob(neJob);
            } else {
                if (neJob.get(ShmConstants.WFS_ID) != null) {
                    logCancelledBy((Long) neJob.get(ShmConstants.PO_ID), cancelledBy, nodeName);
                    cancelNeJob(neJob);
                }
            }
        }
    }

    private void logTBACFailureAtNeJobLevel(final String eventType, final long neJobId, final String loggedInUser, final String nodeName) {
        final String logMessage = String.format(JobExecutorConstants.TBAC_ACCESS_DENIED_AT_NE_LEVEL, loggedInUser);
        systemRecorder.recordEvent(eventType, EventLevel.COARSE, nodeName, ShmConstants.NE_JOB, logMessage);
        logTBACFailure(neJobId, logMessage);
    }

    private void skipCancelAndUpdateNeJobLogs(final Map<String, Object> neJob, final String nodeName, final JobState jobState) {
        final Map<String, Object> cancelJobSkipped = new HashMap<>();
        final List<Map<String, Object>> neJobLogList = new ArrayList<>();
        systemRecorder.recordEvent(SHMEvents.JOB_CANCEL_SKIPPED, EventLevel.COARSE, nodeName, ShmConstants.JOB,
                "cancel Job action is skipped as job reached" + jobState.getJobStateName() + ShmConstants.STATE);
        cancelJobSkipped.put(ActivityConstants.JOB_LOG_MESSAGE, JobExecutorConstants.SKIP_NE_JOB_CANCEL);
        cancelJobSkipped.put(ActivityConstants.JOB_LOG_ENTRY_TIME, new Date());
        cancelJobSkipped.put(ActivityConstants.JOB_LOG_TYPE, JobLogType.SYSTEM.toString());
        cancelJobSkipped.put(ActivityConstants.JOB_LOG_LEVEL, JobLogLevel.INFO.toString());
        neJobLogList.add(cancelJobSkipped);
        jobUpdateService.updateRunningJobAttributes((Long) neJob.get(ShmConstants.PO_ID), null, neJobLogList);
    }

    private void cancelNeJob(final Map<String, Object> neJob) {
        try {
            jobCancellingHandler.cancelNEJobWorkflows((String) neJob.get(ShmConstants.BUSINESS_KEY));
        } catch (final Exception e) {
            LOGGER.error("Failed to cancel the NE Job. Reason:", e);
        }
    }

    private void logCancelledBy(final long jobid, final String cancelledBy, final String mainOrNe) {
        LOGGER.debug("Logging loggedIn user name:{} for the corresponding cancelled job", cancelledBy);
        final String jobLogMessage = String.format(JobExecutorConstants.CANCEL_INVOKED, mainOrNe, cancelledBy);
        final List<Map<String, Object>> jobLogList = prepareJobLogs(jobLogMessage, JobLogLevel.WARN);
        final Map<String, String> propertiesTobeAdded = new HashMap<>();
        propertiesTobeAdded.put(ShmConstants.CANCELLEDBY, cancelledBy);
        jobUpdateService.addOrUpdateOrRemoveJobProperties(jobid, propertiesTobeAdded, jobLogList);
    }

    private List<Map<String, Object>> prepareJobLogs(final String message, final JobLogLevel jobLogLevel) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final Map<String, Object> jobLogs = new HashMap<>();
        jobLogs.put(ActivityConstants.JOB_LOG_MESSAGE, message);
        jobLogs.put(ActivityConstants.JOB_LOG_ENTRY_TIME, new Date());
        jobLogs.put(ActivityConstants.JOB_LOG_TYPE, JobLogType.SYSTEM.toString());
        jobLogs.put(ActivityConstants.JOB_LOG_LEVEL, jobLogLevel.toString());
        jobLogList.add(jobLogs);
        return jobLogList;
    }

    @Override
    @Asynchronous
    public void invokeMainJobsManually(final List<Long> jobIds, final String loggedInUser) {

        if (jobIds.isEmpty()) {
            LOGGER.error("No Job Ids found to invoke main job manually : {}", jobIds);
        } else {
            final String userTaskName = ShmConstants.USER_TASK;
            final List<Map<String, Object>> jobPOs = retriveJobsWithRetry(jobIds);
            final List<Map<String, Object>> validatedJobPOs = new ArrayList<>();
            for (final Map<String, Object> jobPO : jobPOs) {
                final long jobPoId = (long) jobPO.get(ShmConstants.PO_ID);
                final long jobTemplateId = (long) jobPO.get(ShmConstants.JOBTEMPLATEID);

                final boolean validateTBAC = jobAdministratorTBACValidator.validateTBACForMainJob(jobPoId, jobTemplateId, loggedInUser);
                if (validateTBAC) {
                    validatedJobPOs.add(jobPO);
                } else {
                    logTBACFailureAtMainJobLevel(SHMEvents.JOB_CONTINUE_SKIPPED, jobPoId, loggedInUser, (String) jobPO.get(ShmConstants.NAME));
                }
            }
            queryWfsToInvokeJob(validatedJobPOs, userTaskName, loggedInUser);
        }
    }

    /**
     * Method to initiate NE level Jobs manually
     * 
     * @param neJobIds
     */
    @Override
    @Asynchronous
    public void invokeNeJobsManually(final List<Long> neJobIds, final String loggedInUser) {
        if (neJobIds.isEmpty()) {
            LOGGER.error("No NE Job Ids found to invoke nejobs manually : {}", neJobIds);
        } else {
            final String userTaskName = ShmConstants.USER_INPUT;
            final List<Map<String, Object>> jobPOs = retriveJobsWithRetry(neJobIds);
            final List<Map<String, Object>> tbacValidatedNeJobs = getTbacValidatedNeJobs(SHMEvents.JOB_CONTINUE_SKIPPED, loggedInUser, jobPOs);
            queryWfsToInvokeJob(tbacValidatedNeJobs, userTaskName, loggedInUser);
        }
    }

    @Override
    public Map<String, Object> getSupportedNes(final List<String> neNames, final JobTypeEnum jobTypeEnum) {
        final Map<String, Object> supportedAndUnsupported = new HashMap<>();
        final List<Map<String, Object>> nesWithComponentInfo = new ArrayList<>();
        final Map<String, String> neDetailsWithParentName = new HashMap<>();
        final NetworkElementResponse networkElementsResponse = executorServiceHelper.getSupportedAndUnSupportedNetworkElementDetails(neNames, jobTypeEnum, nesWithComponentInfo,
                neDetailsWithParentName);
        final Set<NetworkElement> unSupportedNes = new HashSet<>(networkElementsResponse.getUnsupportedNes().keySet());
        final Set<NetworkElement> invalidNes = networkElementsResponse.getInvalidNes().keySet();
        unSupportedNes.addAll(invalidNes);
        supportedAndUnsupported.put(ShmConstants.SUPPORTED_NES, networkElementsResponse.getSupportedNes());
        supportedAndUnsupported.put(ShmConstants.UNSUPPORTED_NES, new ArrayList(unSupportedNes));
        LOGGER.debug("supported and unsupported network elements are: {}", supportedAndUnsupported);
        return supportedAndUnsupported;
    }

    /**
     * Prepare a wfs query to get the workflowObject which matches with given business key value
     * 
     * @param businessKey
     */
    private void queryWfsToInvokeJob(final List<Map<String, Object>> jobPOs, final String userTaskName, final String loggedInUser) {

        LOGGER.info("Querying WFS to invoke jobs for usertaskname {} for user {}", userTaskName, loggedInUser);

        for (final Map<String, Object> jobAttributes : jobPOs) {
            final long jobPoId = (long) jobAttributes.get(ShmConstants.PO_ID);
            final Map<String, Object> jobAttributesToUpdate = new HashMap<String, Object>();
            jobAttributesToUpdate.put(ShmConstants.SHM_JOB_EXEC_USER, loggedInUser);
            updateJobStaticData(jobPoId, loggedInUser);
            updateJobAtributes(jobPoId, jobAttributesToUpdate);
            final String businessKey = (String) jobAttributes.get(ShmConstants.BUSINESS_KEY);
            final QueryBuilder queryBuilder = QueryBuilderFactory.getDefaultQueryBuilder();
            final Query wfsQuery = queryBuilder.createTypeQuery(QueryType.USERTASK_QUERY);
            final RestrictionBuilder restrictionBuilder = wfsQuery.getRestrictionBuilder();
            final Restriction businessKeyRestriction = restrictionBuilder.isEqual(UsertaskQueryAttributes.QueryParameters.BUSINESS_KEY, businessKey);
            final Restriction userTaskRestriction = restrictionBuilder.isEqual(UsertaskQueryAttributes.QueryParameters.NAME, userTaskName);

            final Restriction allRestrictions = restrictionBuilder.allOf(businessKeyRestriction, userTaskRestriction);
            wfsQuery.setRestriction(allRestrictions);
            LOGGER.info("Fetching workflow instance for wfsQuery {} ", wfsQuery);

            String message = "";
            List<WorkflowObject> workflows = new ArrayList<WorkflowObject>();
            final int retryCount = getWfsRetryCount();
            final int retryTimeOut = getWfsWaitIntervalInMS();
            for (int i = 1; i <= retryCount; i++) {
                try {
                    workflows = workflowInstanceNotifier.executeWorkflowQueryForJobContinue(wfsQuery);
                    if (workflows != null && !workflows.isEmpty()) {
                        break;
                    } else {
                        // This scenario can occur when the work flow not in WAIT_FOR_USER_INPUT(updation of job state in postgres server is slow)
                        message = JobExecutorConstants.WORKFLOW_NOT_IN_WAIT_FOR_USER_INPUT_STATE;
                        sleep(retryTimeOut);
                    }
                } catch (final WorkflowServiceInvocationException | EJBException ex) {
                    LOGGER.error("Job Initiation is failed  due to :", ex);
                    message = ExceptionParser.getReason(ex);
                    sleep(retryTimeOut);
                }
            }
            completeUserTask(workflows, jobAttributes, message, loggedInUser);
        }
    }

    /**
     * Method to update jobExecutionUser in Job Static Data.
     * 
     * @param jobPoId
     * @param loggedInUser
     */
    private void updateJobStaticData(final long jobPoId, final String loggedInUser) {
        try {
            JobStaticData jobStaticData = jobStaticDataProviderImpl.getJobStaticData(jobPoId);
            JobStaticData newJobStaticData = new JobStaticData(jobStaticData.getOwner(), jobStaticData.getActivitySchedules(), jobStaticData.getExecutionMode(), jobStaticData.getJobType(),
                    loggedInUser);
            jobStaticDataProvider.put(jobPoId, newJobStaticData);
        } catch (JobDataNotFoundException e) {
            // exception will be thrown when trying to update Job static data with Ne Job id or unable to find job
            // static data
            LOGGER.error("Failed to get Job static data either from cache or from DPS for jobPoId", e);
        }

    }

    private void updateJobAtributes(final long jobId, final Map<String, Object> jobAttributesToUpdate) {
        LOGGER.debug("Updating job Attributes {} for jobId {} ", jobAttributesToUpdate, jobId);
        jobUpdateService.updateJobAttributes(jobId, jobAttributesToUpdate);
    }

    /**
     * Waiting job starts its execution by taking user input.
     * 
     * @param workflows
     * @param jobAttributes
     * @param logMessage
     */
    private void completeUserTask(final List<WorkflowObject> workflows, final Map<String, Object> jobAttributes, final String logMessage, final String loggedInUser) {
        if (workflows != null && !workflows.isEmpty()) {
            List<String> workflowIdList = new ArrayList<>();
            for (WorkflowObject workflow : workflows) {
                workflowIdList.add((String) workflow.getAttribute(UsertaskQueryAttributes.QueryParameters.ID));
            }
            LOGGER.info("User Task Id List : {}", workflowIdList);
            try {
                updateJobLogs(jobAttributes, loggedInUser);
                for (String workflowId : workflowIdList) {
                    workflowInstanceNotifier.completeUserTask(workflowId);
                    LOGGER.info("Job initiated successfully for workflowId {}", workflowId);
                }
            } catch (final WorkflowServiceInvocationException ex) {
                LOGGER.error("Job Initiation is failed  due to:", ex);
                persistFailureInfoInJobLogs(jobAttributes, String.format(JobExecutorConstants.CORRELATION_FAILED, ex.getMessage()), loggedInUser);
            }
        } else {
            persistFailureInfoInJobLogs(jobAttributes, logMessage, loggedInUser);
        }
    }

    /**
     * Updates job logs for continuing a Manual Job
     * 
     * @param jobAttributes
     * @param logMessage
     */
    private void updateJobLogs(final Map<String, Object> jobAttributes, final String loggedInUser) {
        LOGGER.debug("Inside updateJobLogs with jobAttributes : {}", jobAttributes);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertiesList = new ArrayList<Map<String, Object>>();
        final String nameSpace = (String) jobAttributes.get(ShmConstants.NAMESPACE);
        final String type = (String) jobAttributes.get(ShmConstants.TYPE);
        final long jobId = (Long) jobAttributes.get(ShmConstants.PO_ID);
        boolean isMainJob = false;
        if (ShmConstants.NAMESPACE.equals(nameSpace) && ShmConstants.JOB.equals(type)) {
            isMainJob = true;
        }
        if (isMainJob) {
            jobLogList.add(jobLogUtil.createNewLogEntry(String.format(JobExecutorConstants.CONTINUE_INVOKED_AT_MAINJOB, loggedInUser), new Date(), JobLogLevel.INFO.toString()));
        } else {
            final Map<String, Object> activityAttributes = jobConfigurationService.retrieveWaitingActivityDetails(jobId);
            jobLogList.add(jobLogUtil.createNewLogEntry(String.format(JobExecutorConstants.CONTINUE_INVOKED_AT_NEJOB, loggedInUser, activityAttributes.get(ShmConstants.NAME)), new Date(),
                    JobLogLevel.INFO.toString()));
        }
        jobUpdateService.updateRunningJobAttributes(jobId, jobPropertiesList, jobLogList);

    }

    /**
     * Updates joblog attributes if continuing of manual job fails.
     * 
     * @param poAttributes
     * @param jobId
     * @param logMessage
     */
    private void persistFailureInfoInJobLogs(final Map<String, Object> poAttributes, final String logMessage, final String loggedInUser) {
        LOGGER.debug("Inside persistFailureInfoInJobLogs");
        final String nameSpace = (String) poAttributes.get(ShmConstants.NAMESPACE);
        final String type = (String) poAttributes.get(ShmConstants.TYPE);
        final long jobId = (Long) poAttributes.get(ShmConstants.PO_ID);
        boolean isMainJob = false;

        if (ShmConstants.NAMESPACE.equals(nameSpace) && ShmConstants.JOB.equals(type)) {
            isMainJob = true;
        }
        if (isMainJob) {
            updateMainJobLogs((Long) poAttributes.get(ShmConstants.JOBTEMPLATEID), jobId, logMessage, loggedInUser);
        } else {
            updateActivityJobLogs(jobId, logMessage, loggedInUser);
        }
    }

    private void updateMainJobLogs(final long jobTemplateId, final long jobId, final String logMessage, final String loggedInUser) {
        LOGGER.debug("Inside JobExecutionService.updateMainJobLogs");
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final Map<Object, Object> restrictions = new HashMap<Object, Object>();
        // Querying for job template for the job name and updating in job logs.
        restrictions.put(ObjectField.PO_ID, jobTemplateId);
        final List<Map<String, Object>> jobTemplateAttributes = jobConfigurationService.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE, restrictions,
                Arrays.asList(ShmConstants.NAME));
        if (jobTemplateAttributes != null && !jobTemplateAttributes.isEmpty()) {
            final String jobName = (String) jobTemplateAttributes.get(0).get(ShmConstants.NAME);
            LOGGER.debug("Persisting main job logs for the Job Job : {}", jobName);
            jobLogList.add(jobLogUtil.createNewLogEntry(String.format(JobExecutorConstants.FETCH_WAITING_MAINJOB_WORKFLOW_ID_FAILED, jobName, loggedInUser, logMessage), new Date(),
                    JobLogLevel.DEBUG.toString()));
            jobUpdateService.readAndUpdateRunningJobAttributes(jobId, null, jobLogList);
        }
    }

    private void updateActivityJobLogs(final long jobId, final String logMessage, final String loggedInUser) {
        LOGGER.debug("Inside JobExecutionService.updateActivityJobLogs");
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();

        final Map<String, Object> activityAttributes = jobConfigurationService.retrieveWaitingActivityDetails(jobId);
        jobLogList
                .add(jobLogUtil.createNewLogEntry(String.format(JobExecutorConstants.FETCH_WAITING_ACTIVITYJOB_WORKFLOW_ID_FAILED, activityAttributes.get(ShmConstants.NAME), loggedInUser, logMessage),
                        new Date(), JobLogLevel.DEBUG.toString()));
        LOGGER.debug("Persisting Activity Job logs for the activity : {}", activityAttributes.get(ShmConstants.PO_ID));
        jobUpdateService.readAndUpdateRunningJobAttributes((Long) activityAttributes.get(ShmConstants.PO_ID), null, jobLogList);
    }

    private void sleep(final int retryTimeOut) {
        try {
            Thread.sleep(retryTimeOut);
        } catch (final InterruptedException ex) {
            LOGGER.error("Sleep interrupted due to : ", ex);
            Thread.currentThread().interrupt();
        }
    }

    private Map<String, Object> retrieveJobWithRetry(final long jobId) {
        final Map<String, Object> jobAttributes = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Map<String, Object>>() {
            @Override
            public Map<String, Object> execute() {
                return jobUpdateService.retrieveJobWithRetry(jobId);
            }
        });
        return jobAttributes;

    }

    private List<Map<String, Object>> retriveJobsWithRetry(final List<Long> jobIds) {

        final List<Map<String, Object>> jobAttributes = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<List<Map<String, Object>>>() {
            @Override
            public List<Map<String, Object>> execute() {
                return jobConfigurationService.retrieveJobs(jobIds);
            }
        });
        return jobAttributes;
    }

    private JobExecutionIndexAndState getLatestJobExecutionIndexAndState(final long jobTemplateId) {

        final JobExecutionIndexAndState jobExecutionIndexAndState = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<JobExecutionIndexAndState>() {
            @Override
            public JobExecutionIndexAndState execute() {
                return executorServiceHelper.getLatestJobExecutionIndexAndState(jobTemplateId);
            }
        });
        return jobExecutionIndexAndState;
    }

    private Map<String, Object> createJobPO(final String namespace, final String type, final String version, final Map<String, Object> jobAttributes) {
        try {
            return createPOWithRetry(namespace, type, version, jobAttributes);
        } catch (final RetriableCommandException e) {
            LOGGER.error("Unable to create a PO for [namespace={},type={},]  due to:", namespace, type, e);
            return new HashMap<String, Object>();
        }
    }

    private Map<String, Object> createPOWithRetry(final String namespace, final String type, final String version, final Map<String, Object> jobAttributes) {

        final Map<String, Object> createdjobAttributes = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Map<String, Object>>() {
            @Override
            public Map<String, Object> execute() {
                return executorServiceHelper.createPO(namespace, type, version, jobAttributes);
            }
        });
        return createdjobAttributes;
    }

    private List<Map<String, Object>> getProjectedAttributesForJobs(final String namespace, final String type, final Map<Object, Object> restrictionAttributes,
            final List<String> projectedAttributes) {

        final List<Map<String, Object>> jobAttributes = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<List<Map<String, Object>>>() {
            @Override
            public List<Map<String, Object>> execute() {
                return jobConfigurationService.getProjectedAttributes(namespace, type, restrictionAttributes, projectedAttributes);
            }
        });
        return jobAttributes;

    }

    private int getWfsRetryCount() {
        int retryCountFromDb = 0;
        try {
            retryCountFromDb = wfsRetryConfigurationParamProvider.getWfsRetryCount();
        } catch (final IllegalStateException ex) {
            LOGGER.error("Failed to read wfsRetryCount configuration property from database:", ex);
        }
        final int retryCount = retryCountFromDb > NUMBER_OF_DEFAULT_WFS_RETRIES ? retryCountFromDb : NUMBER_OF_DEFAULT_WFS_RETRIES;
        LOGGER.info("retryCount : {}", retryCount);
        return retryCount;
    }

    private int getWfsWaitIntervalInMS() {
        int retryTimeoutFromDB = 0;
        try {
            retryTimeoutFromDB = wfsRetryConfigurationParamProvider.getWfsWaitIntervalInMS();

        } catch (final IllegalStateException ex) {
            LOGGER.error("Failed to read wfsWaitInterval_ms configuration property from database:", ex);
        }
        final int retryTime = retryTimeoutFromDB > NUMBER_OF_DEFAULT_WFS_RETRYTIME ? retryTimeoutFromDB : NUMBER_OF_DEFAULT_WFS_RETRYTIME;
        LOGGER.info("retryTime : {}", retryTime);
        return retryTime;
    }

    private void endJobAndNotifyWFS(final String message, final long mainJobId, final long jobTemplateId, final Map<String, Object> attributeMap) {
        final Map<String, Object> jobLogList = new HashMap<>();
        jobLogList.put(ActivityConstants.JOB_LOG_MESSAGE, message);
        jobLogList.put(ActivityConstants.JOB_LOG_ENTRY_TIME, new Date());
        jobLogList.put(ActivityConstants.JOB_LOG_TYPE, JobLogType.SYSTEM.toString());
        jobLogList.put(ActivityConstants.JOB_LOG_LEVEL, JobLogLevel.ERROR.toString());
        attributeMap.put(ActivityConstants.JOB_LOG, Arrays.asList(jobLogList));
        attributeMap.put(ShmConstants.RESULT, JobResult.FAILED.getJobResult());
        attributeMap.put(ShmConstants.STATE, JobState.COMPLETED.getJobStateName());
        attributeMap.put(ShmConstants.ENDTIME, new Date());
        updateJobAtributes(mainJobId, attributeMap);
        workflowInstanceNotifier.sendAllNeDone(Long.toString(jobTemplateId));
    }

    private boolean exitIfTBACValidationFailed(final TBACResponse tbacResponse, final long mainJobId, final JobTemplate jobTemplate, final Map<String, Object> attributeMap,
            final Map<String, Object> mainJobAttribute) {
        if (tbacResponse != null && !tbacResponse.isTBACValidationSuccess()) {
            LOGGER.warn("TBAC authiorzation failed due to unexpected error, Exiting from create NEJob");
            return true;
        }
        final String owner = getJobExecutionUser(mainJobAttribute, jobTemplate);
        if (tbacResponse != null && tbacResponse.isTbacValidationToBeDoneForAllNodesAsSingleTarget() && !tbacResponse.getUnAuthorizedNes().isEmpty()) {
            final String message = String.format(JobExecutorConstants.TBAC_ACCESS_DENIED_AT_JOB_LEVEL, owner);
            LOGGER.debug(message);
            endJobAndNotifyWFS(message, mainJobId, jobTemplate.getJobTemplateId(), attributeMap);
            systemRecorder.recordEvent(SHMEvents.JOB_END, EventLevel.COARSE, MAIN_JOB_ID_STRING + mainJobId, TEMPLATE_ID_STRING + jobTemplate.getJobTemplateId(), message);
            return true;
        }
        return false;
    }

    private List<String> getExistingNeNames(final long mainJobId) {
        final List<String> existingNeNames = new ArrayList<>();
        try {
            final Map<Object, Object> restrictionAttributes = new HashMap<>();
            restrictionAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
            final List<String> projectedAttributes = new ArrayList<>();
            projectedAttributes.add(ShmConstants.NE_NAME);
            final List<Map<String, Object>> existingNeJobs = jobConfigurationService.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.NEJOB_TYPE, restrictionAttributes, projectedAttributes);
            if (null != existingNeJobs && !existingNeJobs.isEmpty()) {
                for (final Map<String, Object> existingNeJob : existingNeJobs) {
                    if (null != existingNeJob && !existingNeJob.isEmpty()) {
                        existingNeNames.add((String) existingNeJob.get(ShmConstants.NE_NAME));
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.error("The Exception occurred while fetching NE jobs for the selected mainJobId: {} is : {}", mainJobId, ex);
        }
        return existingNeNames;
    }
}
