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
package com.ericsson.oss.services.shm.system.restore.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.exception.DataPersistenceServiceException;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.exception.ServerInternalException;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.api.NEJob;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.jobs.common.modelentities.Schedule;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ScheduleProperty;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.wfs.api.query.WorkflowObject;

/**
 * Service Bean implementing Job retrieval operations.
 * 
 * @author tcsvisr
 * 
 */
@Traceable
@Profiled
@Stateless
public class JobQueryService {

    private static final Logger logger = LoggerFactory.getLogger(JobQueryService.class);

    @Inject
    WorkFlowQueryServiceImpl workFlowQueryServiceImpl;

    @EServiceRef
    DataPersistenceService dataPersistenceService;

    @Inject
    private DpsReader dpsReader;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    SystemRecorder systemRecorder;

    private final static int MAX_BATCH_SIZE = 20;

    /**
     * This method obtains Job Attributes from Work Flow Object.
     * 
     * @param workFlowObjectList
     * @return JobAttributes
     */
    public List<MainJob> retrieveMainJobAttributes(final List<WorkflowObject> workFlowObjectList) {
        final List<MainJob> mainJobList = new ArrayList<MainJob>();
        final List<MainJob> totalMainJobList = new ArrayList<MainJob>();
        //Retrieving list of work flow instance ID for the provided batch work flow
        final List<String> wfsInstanceIdList = workFlowQueryServiceImpl.getWorkFlowInstanceIdList(workFlowObjectList);
        logger.debug("Total work flow instances retrieved : {}", wfsInstanceIdList.size());

        final List<List<String>> totalWfsInstanceIdList = ListUtils.partition(wfsInstanceIdList, MAX_BATCH_SIZE);
        for (final List<String> wfsInsIdList : totalWfsInstanceIdList) {

            final Query<TypeRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE);

            final List<Restriction> restrictions = new ArrayList<Restriction>();
            for (int wfsInstanceIdListIndex = 0; wfsInstanceIdListIndex < wfsInstanceIdList.size(); wfsInstanceIdListIndex++) {
                restrictions.add(query.getRestrictionBuilder().equalTo(ShmConstants.WFS_ID, wfsInsIdList.get(wfsInstanceIdListIndex)));
            }
            Restriction finalRestriction = null;
            int index = 0;
            for (final Restriction restriction : restrictions) {
                if (index == 0) {
                    finalRestriction = query.getRestrictionBuilder().anyOf(restriction);
                } else {
                    finalRestriction = query.getRestrictionBuilder().anyOf(finalRestriction, restriction);
                }
                index++;
            }
            query.setRestriction(finalRestriction);

            final Iterator<PersistenceObject> jobTemplateIteraor = dataPersistenceService.getLiveBucket().getQueryExecutor().execute(query);
            while (jobTemplateIteraor.hasNext()) {

                final PersistenceObject jobTemplate = jobTemplateIteraor.next();
                final long templateJobId = jobTemplate.getPoId();

                final Schedule mainSchedule = getMainSchedule(jobTemplate);

                final Map<String, Object> templateJobIdRestrictionMap = new HashMap<String, Object>();
                templateJobIdRestrictionMap.put(ShmConstants.JOB_TEMPLATE_ID, templateJobId);
                final List<PersistenceObject> mainJobPos = dpsReader.findPOs(ShmConstants.NAMESPACE, ShmConstants.JOB, templateJobIdRestrictionMap);

                if (mainJobPos == null || mainJobPos.isEmpty()) {
                    logger.debug("No main Jobs are created for Job Template : {}", templateJobId);
                    continue;
                }

                for (final PersistenceObject mainJobPo : mainJobPos) {
                    final String jobState = (String) mainJobPo.getAllAttributes().get(ShmConstants.STATE);
                    final int executionIndex = (int) mainJobPo.getAllAttributes().get(ShmConstants.EXECUTIONINDEX);

                    if (JobState.COMPLETED.getJobStateName() == jobState || JobState.SYSTEM_CANCELLED.getJobStateName() == jobState) {
                        logger.debug("Main Job with Po Id {} is in not in active state.", mainJobPo.getPoId());
                        continue;
                    }

                    final MainJob mainJobAttributes = new MainJob();
                    mainJobAttributes.setMainWorkflowInstanceId((String) jobTemplate.getAttribute(ShmConstants.WFS_ID));
                    mainJobAttributes.setMainJobId(mainJobPo.getPoId());
                    mainJobAttributes.setMainJobState(JobState.getJobState(jobState));
                    mainJobAttributes.setTemplateJobId(templateJobId);
                    mainJobAttributes.setJobName((String) jobTemplate.getAttribute(ShmConstants.NAME));
                    mainJobAttributes.setJobType(JobType.getJobType((String) jobTemplate.getAttribute(ShmConstants.JOB_TYPE)));
                    mainJobAttributes.setExecutionIndex(executionIndex);
                    mainJobAttributes.setMainSchedule(mainSchedule);
                    mainJobList.add(mainJobAttributes);

                }
            }
            totalMainJobList.addAll(mainJobList);
        }
        return totalMainJobList;
    }

    /**
     * @param jobTemplate
     */
    @SuppressWarnings("unchecked")
    private Schedule getMainSchedule(final PersistenceObject jobTemplate) {
        final Schedule schedule = new Schedule();
        Map<String, Object> mainSchedule = null;
        final Map<String, Object> jobConfiguration = (Map<String, Object>) jobTemplate.getAllAttributes().get(ShmConstants.JOBCONFIGURATIONDETAILS);
        if (jobConfiguration != null) {
            mainSchedule = (Map<String, Object>) jobConfiguration.get(ShmConstants.MAIN_SCHEDULE);
            if (mainSchedule != null && !mainSchedule.isEmpty()) {
                final ExecMode execMode = ExecMode.valueOf((String) mainSchedule.get(ShmConstants.EXECUTION_MODE));
                final List<Map<String, Object>> schedulePropertyList = (List<Map<String, Object>>) mainSchedule.get(ShmConstants.SCHEDULINGPROPERTIES);
                final List<ScheduleProperty> scheduleAttributes = new ArrayList<ScheduleProperty>();
                if (schedulePropertyList != null) {
                    for (final Map<String, Object> schedulePropertyMap : schedulePropertyList) {
                        final ScheduleProperty scheduleProperty = new ScheduleProperty();
                        scheduleProperty.setName((String) schedulePropertyMap.get(ShmConstants.NAME));
                        scheduleProperty.setValue((String) schedulePropertyMap.get(ShmConstants.VALUE));
                        scheduleAttributes.add(scheduleProperty);
                    }
                }
                schedule.setExecMode(execMode);
                schedule.setScheduleAttributes(scheduleAttributes);
            }
        }

        return schedule;
    }

    /**
     * This method queries the work flow object to obtain job attributes.
     * 
     * @return JobAttributes
     */
    public List<NEJob> retrieveNEJobAttributes(final long mainJobId, final List<String> wfsIdList) {

        final List<NEJob> jobAttributesList = new ArrayList<NEJob>();
        final Map<String, Object> mainJobIdRestriction = new HashMap<String, Object>();
        mainJobIdRestriction.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        final List<PersistenceObject> neJobPoList = dpsReader.findPOs(ShmConstants.NAMESPACE, ShmConstants.NE_JOB, mainJobIdRestriction);

        if (neJobPoList == null || neJobPoList.isEmpty()) {
            logger.debug("No NE Jobs found for the main Job : {}", mainJobId);
            return jobAttributesList;
        }
        for (final PersistenceObject neJobPo : neJobPoList) {
            final String jobState = (String) neJobPo.getAllAttributes().get(ShmConstants.STATE);
            if (JobState.COMPLETED.getJobStateName() == jobState || JobState.SYSTEM_CANCELLED.getJobStateName() == jobState) {
                logger.debug("NE Job with Po Id {} is in not in active state.", neJobPo.getPoId());
                return jobAttributesList;
            }

            final String neWorkFlowInstanceId = (String) neJobPo.getAllAttributes().get(ShmConstants.WFS_ID);
            if (wfsIdList.contains(neWorkFlowInstanceId)) {
                final NEJob neJobAttributes = new NEJob();
                neJobAttributes.setNeJobId(neJobPo.getPoId());
                neJobAttributes.setNeWorkflowInstanceId(neWorkFlowInstanceId);
                neJobAttributes.setNodeName((String) neJobPo.getAllAttributes().get(ShmConstants.NE_NAME));
                neJobAttributes.setState(JobState.getJobState(jobState));
                jobAttributesList.add(neJobAttributes);
                logger.debug("NE Job Po ID : {} NE WorkFlow Instance : {}", neJobPo.getPoId(), neWorkFlowInstanceId);
            }
        }

        return jobAttributesList;
    }

    /**
     * This method retrieves the Main jobs in active state.
     * 
     * @return List<MainJobAttributes>
     */
    public List<MainJob> getMainJobsInActiveState() {
        final List<MainJob> mainJobAttributesList = new ArrayList<MainJob>();
        final Query<TypeRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOB);
        final TypeRestrictionBuilder restrictionBuilder = query.getRestrictionBuilder();
        query.setRestriction(restrictionBuilder.not(restrictionBuilder.in(ShmConstants.STATE, JobState.COMPLETED.toString(), JobState.SYSTEM_CANCELLED.toString())));
        final Iterator<PersistenceObject> jobPOsIterator = dataPersistenceService.getLiveBucket().getQueryExecutor().execute(query);
        while (jobPOsIterator.hasNext()) {
            final MainJob mainJobAttributes = new MainJob();
            final PersistenceObject jobPo = jobPOsIterator.next();
            mainJobAttributes.setExecutionIndex((int) jobPo.getAllAttributes().get(ShmConstants.EXECUTIONINDEX));
            final Map<String, String> jobTemplateAttributes = setJobNameAndJobType((long) jobPo.getAllAttributes().get(ShmConstants.JOBTEMPLATEID));
            mainJobAttributes.setJobName(jobTemplateAttributes.get(ShmConstants.JOBNAME));
            mainJobAttributes.setMainJobId(jobPo.getPoId());
            mainJobAttributes.setJobType(JobType.getJobType(jobTemplateAttributes.get(ShmConstants.JOB_TYPE)));
            mainJobAttributes.setMainJobState(JobState.getJobState((String) jobPo.getAllAttributes().get(ShmConstants.STATE)));

            mainJobAttributesList.add(mainJobAttributes);
        }
        return mainJobAttributesList;
    }

    /**
     * @param object
     */
    private Map<String, String> setJobNameAndJobType(final Long jobTemplateId) {
        final List<Long> poIds = new ArrayList<Long>();
        final Map<String, String> jobTemplateAttributes = new HashMap<String, String>();
        poIds.add(jobTemplateId);
        final List<PersistenceObject> jobTemplatePoList = dataPersistenceService.getLiveBucket().findPosByIds(poIds);
        final PersistenceObject jobTemplatePo = jobTemplatePoList.get(0);
        final String jobName = (String) jobTemplatePo.getAllAttributes().get(ShmConstants.NAME);
        final String jobType = (String) jobTemplatePo.getAllAttributes().get(ShmConstants.JOB_TYPE);
        jobTemplateAttributes.put(ShmConstants.JOBNAME, jobName);
        jobTemplateAttributes.put(ShmConstants.JOB_TYPE, jobType);
        return jobTemplateAttributes;
    }

    /**
     * This method retrieves the NE jobs in active state.
     * 
     * @param mainJobId
     * @return List<Long>
     */
    public List<Long> getNEJobsInActiveState(final long mainJobId) {
        final List<Long> jobIdList = new ArrayList<Long>();
        final Query<TypeRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.NE_JOB);
        final TypeRestrictionBuilder restrictionBuilder = query.getRestrictionBuilder();
        final Restriction neJobStateRestriction = restrictionBuilder.not(restrictionBuilder.in(ShmConstants.STATE, JobState.COMPLETED.toString(), JobState.SYSTEM_CANCELLED.toString()));
        final Restriction neJobIdRestriction = restrictionBuilder.equalTo(ShmConstants.MAIN_JOB_ID, mainJobId);
        final Restriction allRestrictions = restrictionBuilder.allOf(neJobIdRestriction, neJobStateRestriction);
        query.setRestriction(allRestrictions);
        final Iterator<PersistenceObject> neJobPOsIterator = dataPersistenceService.getLiveBucket().getQueryExecutor().execute(query);
        while (neJobPOsIterator.hasNext()) {
            final PersistenceObject neJobPo = neJobPOsIterator.next();
            jobIdList.add(neJobPo.getPoId());
        }
        return jobIdList;
    }

    /**
     * @param batchedNeJobAttributes
     * @return
     */
    public void cancelActivitiesAndUpdateState(final List<Long> neJobIds, final String jobName, final int executionIndex) {
        try {
            final Query<TypeRestrictionBuilder> jobTypeQuery = dataPersistenceService.getQueryBuilder().createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.ACTIVITY_JOB);
            //building final restriction from list of NE job ids
            final Restriction finalRestriction = buildRestriction(neJobIds, jobTypeQuery);
            jobTypeQuery.setRestriction(finalRestriction);
            final QueryExecutor queryExecutor = dataPersistenceService.getLiveBucket().getQueryExecutor();
            //executing query
            final Iterator<PersistenceObject> iterator = queryExecutor.execute(jobTypeQuery);
            while (iterator.hasNext()) {
                final Map<String, Object> attributesToBePersisted = new HashMap<String, Object>();
                final PersistenceObject persistenceObject = iterator.next();
                final Map<String, Object> poAttributes = persistenceObject.getAllAttributes();
                //building attributes to be updated(log message and state of the Activity job)
                final String cancelledLogMessage = String.format(JobRestoreLogConstants.SYSTEM_CANCELLED, poAttributes.get(ShmConstants.NAME));
                logger.debug("System Cancelling {}'s Job with Message: {}", poAttributes.get(ShmConstants.NAME), cancelledLogMessage);
                final Map<String, Object> attributesToBeUpdated = prepareAttributesToBeUpdated(JobState.SYSTEM_CANCELLED, cancelledLogMessage, attributesToBePersisted);
                final String additionalInfo = "Initial State = " + poAttributes.get(ShmConstants.STATE) + " and Execution Index = " + Integer.toString(executionIndex);
                systemRecorder.recordEvent(SHMEvents.SYSTEM_CANCELLED, EventLevel.COARSE, jobName, jobName, additionalInfo);
                // Updating the state and job log
                persistenceObject.setAttributes(attributesToBeUpdated);
            }
        } catch (final DataPersistenceServiceException e) {
            logger.error("Exception arised when finding PO with namespace {} type {} ", ShmConstants.NAMESPACE, ShmConstants.ACTIVITY_JOB, e);
            throw new ServerInternalException("findPO query with namespace" + ShmConstants.NAMESPACE + " type " + ShmConstants.ACTIVITY_JOB + " to the database failed due to internal server error ",
                    e);
        }
    }

    private Restriction buildRestriction(final List<Long> neJobIds, final Query<TypeRestrictionBuilder> jobTypeQuery) {
        final TypeRestrictionBuilder restrictionBuilder = jobTypeQuery.getRestrictionBuilder();
        final List<Restriction> jobRestrictions = new ArrayList<Restriction>();
        // NE job IDs restriction...
        for (final long neJobId : neJobIds) {
            jobRestrictions.add(restrictionBuilder.equalTo(ShmConstants.NE_JOB_ID, neJobId));
        }
        Restriction neJodIdRestriction = null;
        if (!jobRestrictions.isEmpty()) {
            int index = 0;
            for (final Restriction jobTemplateRestriction : jobRestrictions) {
                if (index == 0) {
                    neJodIdRestriction = restrictionBuilder.anyOf(jobTemplateRestriction);
                } else {
                    neJodIdRestriction = restrictionBuilder.anyOf(neJodIdRestriction, jobTemplateRestriction);
                }
                index++;
            }
        }
        // NE job STATE Restriction...
        final Restriction neJobStateRestriction = restrictionBuilder.not(restrictionBuilder.in(ShmConstants.STATE, JobState.COMPLETED.toString(), JobState.SYSTEM_CANCELLED.toString()));
        return restrictionBuilder.allOf(neJobStateRestriction, neJodIdRestriction);
    }

    /**
     * @param attributesToBePersisted
     * @param systemCancelling
     * @param jobName
     * @param initialJobState
     * @param executionIndex
     */
    private Map<String, Object> prepareAttributesToBeUpdated(final JobState state, final String logMessage, final Map<String, Object> attributesToBePersisted) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        attributesToBePersisted.put(ShmConstants.STATE, state.toString());
        attributesToBePersisted.put(ShmConstants.LOG, jobLogList);
        return attributesToBePersisted;
    }
}
