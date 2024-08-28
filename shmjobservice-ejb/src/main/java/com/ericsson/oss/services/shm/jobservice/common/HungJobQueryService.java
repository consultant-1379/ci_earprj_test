/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.collections4.IteratorUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.exception.DataPersistenceServiceException;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.ProjectionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.ServerInternalException;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.api.NEJob;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobCapabilityProvider;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

/**
 * 
 * @author xghamdg
 * 
 */

@Stateless
public class HungJobQueryService {

    private final Logger LOGGER = LoggerFactory.getLogger(HungJobQueryService.class);

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    @Inject
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private DpsReader dpsReader;

    @Inject
    private DpsWriter dpsWriter;

    @Inject
    private JobCapabilityProvider jobCapabilityProvider;

    @Inject
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    private static final String SYSTEM_CANCELLED = "\"%s\" is System Cancelled.";

    private static final String IS_ACTIVITY_JOBS_UPDATED = "isActivityJobsUpdated";

    /**
     * This method is used to get all long running jobs i.e which are in RUNNING state for a duration of time longer than the specified time.
     * 
     * @param maxTimeLimitForJobExecutionInHours
     * @return projectionQueryResult
     */
    public List<Object[]> getLongRunningJobs(final int maxTimeLimitForJobExecutionInHours) {
        boolean isDatabaseDown = false;
        LOGGER.debug("Fetching all hung jobs");
        try {
            final List<Object[]> projectionQueryResult = getQueryResult(maxTimeLimitForJobExecutionInHours);
            if (projectionQueryResult == null || projectionQueryResult.isEmpty()) {
                return Collections.emptyList();
            }
            return projectionQueryResult;
        } catch (final RuntimeException runtimeException) {
            isDatabaseDown = dpsAvailabilityInfoProvider.isDatabaseDown();
            LOGGER.error("Exception while fetching hung Jobs projection -Reason {} ,Checking is Database Down: {}", runtimeException, isDatabaseDown);
            return Collections.emptyList();
        }
    }

    /**
     * @param maxTimeLimitForJobExecutionInHours
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> getQueryResult(final int maxTimeLimitForJobExecutionInHours) {
        final DataBucket liveBucket = dataPersistenceService.getLiveBucket();
        final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
        final Query<TypeRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOB);
        final Restriction startTimequeryRestriction = query.getRestrictionBuilder().lessThan(ShmConstants.STARTTIME, new DateTime().minusHours(maxTimeLimitForJobExecutionInHours).toDate());
        final Restriction jobstateRestriction = query.getRestrictionBuilder().in(ShmConstants.STATE, JobState.RUNNING.getJobStateName(), JobState.CANCELLING.getJobStateName());
        final Restriction endTimequeryRestriction = query.getRestrictionBuilder().nullValue(ShmConstants.ENDTIME);
        final Restriction finalRestrictionForJobsWithRunningORCancellingState = query.getRestrictionBuilder().allOf(startTimequeryRestriction, endTimequeryRestriction, jobstateRestriction);
        final Restriction creationTimequeryRestriction = query.getRestrictionBuilder().lessThan(ShmConstants.CREATION_TIME, new DateTime().minusHours(maxTimeLimitForJobExecutionInHours).toDate());
        final Restriction createdjobstateRestriction = query.getRestrictionBuilder().equalTo(ShmConstants.STATE, JobState.CREATED.getJobStateName());
        final Restriction startTimeEmptyRestriction = query.getRestrictionBuilder().nullValue(ShmConstants.STARTTIME);
        final Restriction finalRestrictionForJobswithCreatedState = query.getRestrictionBuilder().allOf(creationTimequeryRestriction, endTimequeryRestriction, createdjobstateRestriction,
                startTimeEmptyRestriction);
        final Restriction finalRestriction = query.getRestrictionBuilder().anyOf(finalRestrictionForJobsWithRunningORCancellingState, finalRestrictionForJobswithCreatedState);
        query.setRestriction(finalRestriction);
        final List<PersistenceObject> queryResult = queryExecutor.getResultList(query);

        final List<Object[]> longRunningMainJobs = new ArrayList<>();
        for (final PersistenceObject eachLongRunningMainJob : queryResult) {
            final Object[] mainJobAttributes = new Object[6];
            mainJobAttributes[0] = eachLongRunningMainJob.getPoId();
            mainJobAttributes[1] = eachLongRunningMainJob.getAttribute(ShmConstants.EXECUTIONINDEX);
            mainJobAttributes[2] = eachLongRunningMainJob.getAttribute(ShmConstants.STATE);
            mainJobAttributes[3] = eachLongRunningMainJob.getAttribute(ShmConstants.JOB_TEMPLATE_ID);
            mainJobAttributes[4] = eachLongRunningMainJob.getAttribute(ShmConstants.BUSINESS_KEY);

            final Map<String, Object> jobConfigurationDetails = eachLongRunningMainJob.getAttribute(ShmConstants.JOBCONFIGURATIONDETAILS);
            final Map<String, Object> mainSchedule = (Map<String, Object>) jobConfigurationDetails.get(ShmConstants.MAIN_SCHEDULE);
            final List<Map<String, Object>> schedulePropertiesList = (List<Map<String, Object>>) mainSchedule.get(ShmConstants.SCHEDULINGPROPERTIES);
            mainJobAttributes[5] = schedulePropertiesList;

            longRunningMainJobs.add(mainJobAttributes);
        }
        return longRunningMainJobs;
    }

    /**
     * This method will return all hung NE jobs attributes
     * 
     * @param mainJobId
     * @param maxTimeLimitForJobExecutionInHours
     * @param maxTimeLimitForAxeUpgradeJobExecutionInHours
     * @param jobType
     * @return
     */
    public List<NEJob> getHungNeJobs(final long mainJobId, final int maxTimeLimitForJobExecutionInHours, final int maxTimeLimitForAxeUpgradeJobExecutionInHours, final String jobType) {
        final List<NEJob> jobAttributesList = new ArrayList<NEJob>();
        boolean isDatabaseDown = false;
        final DataBucket liveBucket = dataPersistenceService.getLiveBucket();

        try {
            final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
            final Query<TypeRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.NE_JOB);
            final Restriction poQueryRestriction = query.getRestrictionBuilder().in(ShmConstants.MAIN_JOB_ID, mainJobId);
            final Restriction startTimeQueryRestriction = query.getRestrictionBuilder().lessThan(ShmConstants.STARTTIME, new DateTime().minusHours(maxTimeLimitForJobExecutionInHours).toDate());

            final Restriction jobStateQueryRestriction = query.getRestrictionBuilder().in(ShmConstants.STATE, JobState.RUNNING.getJobStateName(), JobState.CANCELLING.getJobStateName());
            final Restriction endTimeQueryRestriction = query.getRestrictionBuilder().nullValue(ShmConstants.ENDTIME);
            final Restriction finalRestriction = query.getRestrictionBuilder().allOf(poQueryRestriction, startTimeQueryRestriction, endTimeQueryRestriction, jobStateQueryRestriction);

            query.setRestriction(finalRestriction);

            final Iterator<PersistenceObject> iterator = queryExecutor.execute(query);
            final List<PersistenceObject> neJobPoList = IteratorUtils.toList(iterator);
            final List<String> neNames = fetchNeNamesFromNeJobPOs(neJobPoList);
            if (neJobPoList == null || neJobPoList.isEmpty()) {
                LOGGER.debug("No NE jobs are in hung state for the mainJob id [{}]", mainJobId);
                return jobAttributesList;
            }
            final List<NetworkElement> networkElements = fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(neNames, jobCapabilityProvider.getCapability(JobTypeEnum.getJobType(jobType)));
            for (final PersistenceObject neJobPo : neJobPoList) {
                final long jobId = neJobPo.getPoId();
                final Map<String, Object> neJobPoAttributes = neJobPo.getAllAttributes();
                final String jobState = (String) neJobPoAttributes.get(ShmConstants.STATE);
                final String neWorkFlowInstanceId = (String) neJobPoAttributes.get(ShmConstants.WFS_ID);
                final Date startTime = (Date) neJobPoAttributes.get(ShmConstants.STARTTIME);
                final List<Map<String, String>> jobProperties = (List<Map<String, String>>) neJobPoAttributes.get(ShmConstants.JOBPROPERTIES);
                final String neName = fetchNodeName(neJobPoAttributes);
                LOGGER.debug("NE JOB State for Job id: {}, {}", jobId, jobState);

                final NEJob neJobAttributes = new NEJob();

                for (final NetworkElement networkElement : networkElements) {
                    if ((neName.contains(networkElement.getName()) && isComponentNeJob(jobProperties)) || networkElement.getName().equals(neName)) {
                        neJobAttributes.setPlatformType(networkElement.getPlatformType().name());
                        break;
                    }
                }
                neJobAttributes.setNeJobId(jobId);
                neJobAttributes.setNeWorkflowInstanceId(neWorkFlowInstanceId);
                neJobAttributes.setNodeName(neName);
                neJobAttributes.setState(JobState.getJobState(jobState));
                if (JobType.UPGRADE.equals(JobType.getJobType(jobType)) && PlatformTypeEnum.AXE.equals(PlatformTypeEnum.getPlatform(neJobAttributes.getPlatformType()))) {
                    filterAxeNeJobs(startTime, maxTimeLimitForAxeUpgradeJobExecutionInHours, jobAttributesList, neJobAttributes);
                } else {
                    jobAttributesList.add(neJobAttributes);
                }
                LOGGER.debug("NE Job Po ID : {} NE WorkFlow Instance : {}", jobId, neWorkFlowInstanceId);

            }

        } catch (final RuntimeException runtimeException) {
            isDatabaseDown = dpsAvailabilityInfoProvider.isDatabaseDown();
            LOGGER.error("Exception while fetching Hung Ne Jobs -Reason {} ,Checking is Database Down: {}", runtimeException, isDatabaseDown);
            throw new ServerInternalException("Exception Ocuured while fetching hung NE Jobs for mainJob :" + mainJobId);
        }
        return jobAttributesList;

    }

    /**
     * @param neJobPoList
     */
    private List<String> fetchNeNamesFromNeJobPOs(final List<PersistenceObject> neJobPoList) {
        final List<String> neNames = new ArrayList<String>();
        for (PersistenceObject neJobPo : neJobPoList) {
            final Map<String, Object> neJobPoAttributes = neJobPo.getAllAttributes();
            final String nodeName = fetchNodeName(neJobPoAttributes);
            neNames.add(nodeName);
        }
        return neNames;
    }

    private void filterAxeNeJobs(final Date startTime, final int maxTimeLimitForAxeUpgradeJobExecutionInHours, final List<NEJob> jobAttributesList, final NEJob neJobAttributes) {
        final Date limitTime = new DateTime().minusHours(maxTimeLimitForAxeUpgradeJobExecutionInHours).toDate();
        if (startTime.compareTo(limitTime) <= 0) {
            LOGGER.debug("Found axe nejob running for more than {} hours", maxTimeLimitForAxeUpgradeJobExecutionInHours);
            jobAttributesList.add(neJobAttributes);
        }
    }

    /**
     * @param jobProperties
     * @return
     */
    private boolean isComponentNeJob(final List<Map<String, String>> jobProperties) {
        if (jobProperties != null && !jobProperties.isEmpty()) {
            for (final Map<String, String> jobProperty : jobProperties) {
                if (ShmConstants.PARENT_NAME.equals(jobProperty.get(ShmConstants.KEY))) {
                    LOGGER.debug("Node identified as GSM node, so considering parent name as nodeName");
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private String fetchNodeName(final Map<String, Object> neJobPoAttributes) {
        final List<Map<String, String>> jobProperties = (List<Map<String, String>>) neJobPoAttributes.get(ShmConstants.JOBPROPERTIES);
        String nodeName = ActivityConstants.EMPTY;
        if (jobProperties != null && !jobProperties.isEmpty()) {
            for (final Map<String, String> jobProperty : jobProperties) {
                if (ShmConstants.PARENT_NAME.equals(jobProperty.get(ShmConstants.KEY))) {
                    LOGGER.debug("Node identified as GSM node, so considering parent name as nodeName");
                    nodeName = jobProperty.get(ShmConstants.VALUE);
                    break;
                }
            }
        }
        if (nodeName == null || ActivityConstants.EMPTY.equals(nodeName)) {
            nodeName = (String) neJobPoAttributes.get(ShmConstants.NE_NAME);
        }
        return nodeName;
    }

    /**
     * This method will update all hung Activity JobState to SYSTEM CANCELLED. Return false if any of the Activity job in schedule or waiting for user input, it will
     * 
     * @param neJobIds
     * @param jobName
     * @param executionIndex
     * @param maxTimeLimitForJobExecutionInHours
     * @return
     */
    public Map<String, Object> cancelActivitiesAndUpdateState(final long neJobId, final String jobName, final int executionIndex, final int maxTimeLimitForJobExecutionInHours) {
        LOGGER.debug("Enter into HungJobQueryService.cancelActivitiesAndUpdateState with jobName: {}, excecutionIndex: {}, Number of NejobId: {}", jobName, executionIndex, neJobId);
        boolean isDatabaseDown = false;
        final Map<String, Object> activityJobsUpdatedStatusMap = new HashMap<String, Object>();
        try {
            final Query<TypeRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.ACTIVITY_JOB);

            //building final restriction from list of NE job ids
            final Restriction neJodIdsRestriction = query.getRestrictionBuilder().in(ShmConstants.NE_JOB_ID, neJobId);
            final Restriction startTimeQueryRestriction = query.getRestrictionBuilder().lessThan(ShmConstants.STARTTIME, new DateTime().minusHours(maxTimeLimitForJobExecutionInHours).toDate());

            final Restriction jobStateRunningRestriction = query.getRestrictionBuilder().equalTo(ShmConstants.STATE, JobState.RUNNING.getJobStateName());
            final Restriction endTimeQueryRestriction = query.getRestrictionBuilder().nullValue(ShmConstants.ENDTIME);
            final Restriction finalRestrictionOnTimeLimit = query.getRestrictionBuilder().allOf(neJodIdsRestriction, startTimeQueryRestriction, endTimeQueryRestriction, jobStateRunningRestriction);

            final Restriction jobStateScheduledAndWaitForUserInputRestriction = query.getRestrictionBuilder().in(ShmConstants.STATE, JobState.SCHEDULED.getJobStateName(),
                    JobState.WAIT_FOR_USER_INPUT.getJobStateName());
            final Restriction neJobStateRestriction = query.getRestrictionBuilder().allOf(neJodIdsRestriction, jobStateScheduledAndWaitForUserInputRestriction);

            final Restriction finalRestriction = query.getRestrictionBuilder().anyOf(finalRestrictionOnTimeLimit, neJobStateRestriction);

            query.setRestriction(finalRestriction);

            final QueryExecutor queryExecutor = dataPersistenceService.getLiveBucket().getQueryExecutor();

            //executing query
            final Iterator<PersistenceObject> iterator = queryExecutor.execute(query);

            final List<PersistenceObject> persistenceObjects = new ArrayList<PersistenceObject>();
            while (iterator.hasNext()) {
                final PersistenceObject persistenceObject = iterator.next();
                final String jobState = (String) persistenceObject.getAttribute(ShmConstants.STATE);
                LOGGER.info("ActivityJob State for NeJobId:{}, State:{}", neJobId, jobState);
                if (JobState.SCHEDULED.getJobStateName().equalsIgnoreCase(jobState) || JobState.WAIT_FOR_USER_INPUT.getJobStateName().equalsIgnoreCase(jobState)) {
                    LOGGER.info("NE Job with NEJobId {} is in scheduled or waiting for user input...Skiping System cancel", neJobId);
                    activityJobsUpdatedStatusMap.put(IS_ACTIVITY_JOBS_UPDATED, false);
                    return activityJobsUpdatedStatusMap;
                }
                persistenceObjects.add(persistenceObject);
            }

            if (persistenceObjects.isEmpty()) {
                LOGGER.info("No Activity jobs for SYSTEM CANCELLED for the neJobId : {}, with the job name : {}", neJobId, jobName);
                activityJobsUpdatedStatusMap.put(IS_ACTIVITY_JOBS_UPDATED, true);
                return activityJobsUpdatedStatusMap;
            }

            String activityName = null;
            for (final PersistenceObject persistenceObject : persistenceObjects) {
                final Map<String, Object> poAttributes = persistenceObject.getAllAttributes();
                //building attributes to be updated(log message and state of the Activity job)
                activityName = (String) poAttributes.get(ShmConstants.NAME);
                final String cancelledLogMessage = String.format(SYSTEM_CANCELLED, poAttributes.get(ShmConstants.NAME));
                LOGGER.debug("System Cancelling {}'s Job with Message: {}", poAttributes.get(ShmConstants.NAME), cancelledLogMessage);
                final Map<String, Object> attributesToBeUpdated = prepareAttributesToBeUpdated(JobState.SYSTEM_CANCELLED, cancelledLogMessage);

                final String additionalInfo = "Initial State = " + poAttributes.get(ShmConstants.STATE) + " and Execution Index = " + Integer.toString(executionIndex);
                systemRecorder.recordEvent(SHMEvents.SYSTEM_CANCELLED, EventLevel.COARSE, jobName, jobName, additionalInfo);
                // Updating the state and job log
                persistenceObject.setAttributes(attributesToBeUpdated);
            }
            activityJobsUpdatedStatusMap.put(IS_ACTIVITY_JOBS_UPDATED, true);
            activityJobsUpdatedStatusMap.put(ShmConstants.ACTIVITYNAME, activityName);

        } catch (final DataPersistenceServiceException e) {
            isDatabaseDown = dpsAvailabilityInfoProvider.isDatabaseDown();
            LOGGER.error("Exception while fetching activity PO with namespace {} Reason {} ,Checking is Database Down: {}", ShmConstants.ACTIVITY_JOB, e, isDatabaseDown);
            throw new ServerInternalException("findPO query with namespace" + ShmConstants.NAMESPACE + " type " + ShmConstants.ACTIVITY_JOB + " to the database failed due to internal server error ",
                    e);
        }
        return activityJobsUpdatedStatusMap;
    }

    /**
     * This method will return jobName and wfsId
     * 
     * @param jobTemplateId
     * @return
     */
    public Map<String, Object> getJobNameAndWorkflowId(final long jobTemplateId) {
        final Map<String, Object> jobAttributes = new HashMap<>();
        boolean isDatabaseDown = false;
        LOGGER.debug("Fetching workflow id and jobName with jobTeTemplate id for hung jobs {}", jobTemplateId);

        try {
            final PersistenceObject jobTemplatePo = dpsReader.findPOByPoId(jobTemplateId);
            if (jobTemplatePo != null) {
                final String wfsId = jobTemplatePo.getAttribute(ShmConstants.WFS_ID);
                jobAttributes.put(ShmConstants.WFS_ID, wfsId);
                final String jobName = jobTemplatePo.getAttribute(ShmConstants.NAME);
                jobAttributes.put(ShmConstants.NAME, jobName);
                final String jobtype = jobTemplatePo.getAttribute(ShmConstants.JOB_TYPE);
                jobAttributes.put(ShmConstants.JOB_TYPE, jobtype);
            }
            return jobAttributes;

        } catch (final RuntimeException runtimeException) {
            isDatabaseDown = dpsAvailabilityInfoProvider.isDatabaseDown();
            LOGGER.error("Exception while fetching Workflow Ids -Reason {} ,Checking is Database Down: {}", runtimeException, isDatabaseDown);
            throw new ServerInternalException("Exception occurred while fetching mainJon workflow Ids");
        }

    }

    @SuppressWarnings("unchecked")
    public void updateJobsInBatch(final Map<Long, Map<String, Object>> batchJobsToBeUpdated) {
        LOGGER.debug("Inside updateJobsInBatch");
        final Set<Long> jobIdSet = batchJobsToBeUpdated.keySet();
        final List<Long> jobIdList = new ArrayList<Long>(jobIdSet);
        final List<PersistenceObject> jobPOsList = dpsReader.findPOsByPoIds(jobIdList);
        for (final PersistenceObject jobPo : jobPOsList) {

            final Map<String, Object> attributeMap = batchJobsToBeUpdated.get(jobPo.getPoId());

            final Map<String, Object> poAttributes = jobPo.getAllAttributes();
            if (batchJobsToBeUpdated.get(jobPo.getPoId()).get(ShmConstants.LOG) != null) {
                final List<Map<String, Object>> jobLogList = (List<Map<String, Object>>) batchJobsToBeUpdated.get(jobPo.getPoId()).get(ShmConstants.LOG);
                if ((!poAttributes.isEmpty()) && (poAttributes != null)) {

                    List<Map<String, Object>> activityJobLogList = new ArrayList<Map<String, Object>>();
                    if (poAttributes.get(ActivityConstants.JOB_LOG) != null) {
                        activityJobLogList = (List<Map<String, Object>>) poAttributes.get(ActivityConstants.JOB_LOG);
                    }
                    activityJobLogList.addAll(jobLogList);
                    attributeMap.put(ShmConstants.LOG, activityJobLogList);

                }
            }
            LOGGER.debug("Updating Job with PO Id {} to SYSTEM_CANCELLED ", jobPo.getPoId());
            dpsWriter.update(jobPo.getPoId(), attributeMap);
        }
    }

    public Map<String, Object> prepareAttributesToBeUpdated(final JobState state, final String logMessage) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> attributesToBePersisted = new HashMap<String, Object>();
        prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());

        attributesToBePersisted.put(ShmConstants.STATE, state.toString());
        attributesToBePersisted.put(ShmConstants.ENDTIME, new Date());
        attributesToBePersisted.put(ShmConstants.RESULT, JobResult.FAILED.getJobResult());
        attributesToBePersisted.put(ShmConstants.LOG, jobLogList);
        return attributesToBePersisted;
    }

    private void prepareJobLogAtrributesList(final List<Map<String, Object>> jobLogList, final String activityLogMessage, final Date entryTime, final String logType, final String logLevel) {
        final Map<String, Object> activityAttributes = new HashMap<String, Object>();
        activityAttributes.put(ActivityConstants.JOB_LOG_MESSAGE, activityLogMessage);
        activityAttributes.put(ActivityConstants.JOB_LOG_ENTRY_TIME, entryTime);
        activityAttributes.put(ActivityConstants.JOB_LOG_TYPE, logType);
        activityAttributes.put(ActivityConstants.JOB_LOG_LEVEL, logLevel);
        jobLogList.add(activityAttributes);
    }

    /**
     * This method checks the running NE Jobs
     * 
     * @param mainJobId
     * @return true if NE jobs are running state otherwise false
     */
    public boolean checkRunningNeJobs(final long mainJobId) {
        final DataBucket liveBucket = dataPersistenceService.getLiveBucket();
        boolean isDatabaseDown = false;

        try {
            final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
            final Query<TypeRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.NE_JOB);
            final Restriction poQueryRestriction = query.getRestrictionBuilder().in(ShmConstants.MAIN_JOB_ID, mainJobId);
            final Restriction jobStateQueryRestriction = query.getRestrictionBuilder().equalTo(ShmConstants.STATE, JobState.RUNNING.getJobStateName());
            final Restriction endTimequeryRestriction = query.getRestrictionBuilder().nullValue(ShmConstants.ENDTIME);

            final Restriction finalRestriction = query.getRestrictionBuilder().allOf(poQueryRestriction, endTimequeryRestriction, jobStateQueryRestriction);
            query.setRestriction(finalRestriction);
            final List<Object> projectionQueryResult = queryExecutor.executeProjection(query, ProjectionBuilder.attribute(ShmConstants.STATE));

            if (projectionQueryResult == null || projectionQueryResult.isEmpty()) {
                LOGGER.debug("No NE Jobs are in running state for mainJob : {}", mainJobId);
                return false;
            }
            return true;

        } catch (final RuntimeException runtimeException) {
            isDatabaseDown = dpsAvailabilityInfoProvider.isDatabaseDown();
            LOGGER.error("Exception while fetching running NE Jobs -Reason {} ,Checking is Database Down: {}", runtimeException, isDatabaseDown);
            throw new ServerInternalException("Exception occurred while fetching running NE Jobs for hung job");
        }

    }

    /**
     * This method will return ShmStagedActivity POs which are in "READY" state.
     * 
     * @param maxTimeLimitForJobExecutionInHours
     * @return List<PersistenceObject
     */
    public List<PersistenceObject> getStagedActivityPOs(final int maxTimeLimitForJobExecutionInHours) {
        final long startTime = System.currentTimeMillis();
        final List<PersistenceObject> persistenceObjects = new ArrayList<>();
        final QueryExecutor queryExecutor = dataPersistenceService.getLiveBucket().getQueryExecutor();
        final Query<TypeRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.SHM_STAGED_ACTIVITY);
        final Restriction stagedActivityStatusRestriction = query.getRestrictionBuilder().equalTo(ShmConstants.STAGED_ACTIVITY_STATUS, ShmConstants.READY_STATE);
        final Restriction waitTimeQueryRestriction = query.getRestrictionBuilder().lessThan(ShmConstants.STAGED_ACTIVITY_WAIT_TIME,
                new DateTime().minusHours(maxTimeLimitForJobExecutionInHours).toDate());
        query.setRestriction(stagedActivityStatusRestriction);
        final Restriction finalRestriction = query.getRestrictionBuilder().allOf(stagedActivityStatusRestriction, waitTimeQueryRestriction);
        query.setRestriction(finalRestriction);
        LOGGER.debug("Executing Query to get READY state StagedActivityPOs");
        final Iterator<PersistenceObject> pollingEntryPos = queryExecutor.execute(query);
        while (pollingEntryPos.hasNext()) {
            final PersistenceObject persistenceObject = pollingEntryPos.next();
            persistenceObjects.add(persistenceObject);
        }
        LOGGER.info("{} millisecs taken to retrieve READY state stagedactivity PO's: size {}", System.currentTimeMillis() - startTime, persistenceObjects.size());
        return persistenceObjects;
    }

    /**
     * deletes the required po
     * 
     * @param poId
     */
    public void deleteStagedActivityPO(final long stagedActivityPOId) {
        dataPersistenceService.getLiveBucket().deletePo(dpsReader.findPOByPoId(stagedActivityPOId));
        LOGGER.info("StagedActivity PO {} Deleted successfully ", stagedActivityPOId);
    }
}
