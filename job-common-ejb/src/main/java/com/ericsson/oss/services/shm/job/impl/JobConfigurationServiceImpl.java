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

package com.ericsson.oss.services.shm.job.impl;

import static com.ericsson.oss.services.shm.shared.constants.ActivityConstants.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

import javax.ejb.*;
import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.*;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.ProjectionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.*;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.HealthStatus;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.*;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.shm.job.entities.SHMJobData;

@SuppressWarnings({ "unchecked", "PMD.ExcessiveClassLength" })
@Traceable
@Profiled
@Stateless
public class JobConfigurationServiceImpl implements JobConfigurationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobConfigurationServiceImpl.class);

    @Inject
    private DpsReader dpsReader;

    @Inject
    private DpsWriter dpsWriter;

    @Inject
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    @Inject
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    @Inject
    private JobUpdateService jobUpdateService;

    @Override
    public List<Map<String, String>> fetchJobProperty(final long jobId) {
        LOGGER.debug("Inside JobConfigurationServiceImpl.fetchJobProperty() with jobId:{} ", jobId);
        final Map<String, Object> poAttr = retrieveJob(jobId);

        List<Map<String, String>> jobPropertiesList = new ArrayList<Map<String, String>>();
        if (poAttr != null && !poAttr.isEmpty()) {
            if (poAttr.get(ActivityConstants.JOB_PROPERTIES) != null) {
                jobPropertiesList = (List<Map<String, String>>) poAttr.get(ActivityConstants.JOB_PROPERTIES);
            } else {
                LOGGER.error("No Job Properties found with jobId: {}", jobId);
            }
        }
        return jobPropertiesList;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Map<String, Object> retrieveJob(final long jobId) {
        LOGGER.debug("Job id received is {}", jobId);
        final PersistenceObject po = dpsReader.findPOByPoId(jobId);
        if (po != null) {
            return po.getAllAttributes();
        } else {
            return new HashMap<>();
        }

    }

    @Override
    public List<Map<String, Object>> retrieveJobs(final List<Long> jobIds) {
        final List<Map<String, Object>> persistenceObjectAttributes = new ArrayList<>();
        final List<PersistenceObject> persistenceObjects = dpsReader.findPOsByPoIds(jobIds);
        for (final PersistenceObject persistenceObject : persistenceObjects) {
            final Map<String, Object> poAttributes = persistenceObject.getAllAttributes();
            poAttributes.put(ShmConstants.PO_ID, persistenceObject.getPoId());
            poAttributes.put(ShmConstants.NAMESPACE, persistenceObject.getNamespace());
            poAttributes.put(ShmConstants.TYPE, persistenceObject.getType());
            persistenceObjectAttributes.add(poAttributes);
        }
        return persistenceObjectAttributes;
    }

    @Override
    public Map<String, String> retrieveWorkflowAttributes(final long activityJobId) {
        final Map<String, String> workflowAttributes = new HashMap<>();
        final Map<String, Object> activityJobAttributes = retrieveJob(activityJobId);
        if (activityJobAttributes != null && !activityJobAttributes.isEmpty()) {
            final long neJobId = (long) activityJobAttributes.get(ShmConstants.ACTIVITY_NE_JOB_ID);
            final List<Map<String, Object>> activityJobPropertyList = (List<Map<String, Object>>) activityJobAttributes.get(ActivityConstants.JOB_PROPERTIES);
            if (activityJobPropertyList != null) {
                for (final Map<String, Object> jobProperty : activityJobPropertyList) {
                    if (jobProperty != null) {
                        if (ActivityConstants.ACTIVITY_RESULT.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                            workflowAttributes.put(ActivityConstants.ACTIVITY_RESULT, (String) jobProperty.get(ActivityConstants.JOB_PROP_VALUE));
                        }
                    }
                }
            }
            final Map<String, Object> neJobAttributes = retrieveJob(neJobId);
            if (neJobAttributes != null && !neJobAttributes.isEmpty()) {
                workflowAttributes.put(ShmConstants.BUSINESS_KEY, (String) neJobAttributes.get(ShmConstants.BUSINESS_KEY));
            }
        }
        LOGGER.debug("retrieved workflowAttributes for job:{} are : {}", activityJobId, workflowAttributes);
        return workflowAttributes;
    }

    private boolean isJobTemplateDeletable(final Map<String, Object> jobTemplateAttributes) {
        return (boolean) jobTemplateAttributes.get(ShmConstants.JobTemplateConstants.ISDELETABLE);
    }

    private String deriveJobStatus(final Map<String, Object> jobTemplateAttributes, final String execMode) {

        final boolean isJobTemplateCancelled = (boolean) jobTemplateAttributes.get(ShmConstants.JobTemplateConstants.ISCANCELLED);
        if (isJobTemplateCancelled) {
            return JobState.COMPLETED.getJobStateName();
        }
        if (execMode != null && ExecMode.SCHEDULED.getMode().equalsIgnoreCase(execMode)) {
            return JobState.SCHEDULED.getJobStateName();
        }
        return JobState.SUBMITTED.getJobStateName();
    }

    @Override
    public String retrieveActivityJobResult(final long neJobId) {
        LOGGER.debug("Entered Into retrieveActivityJobResult method with neJobId {}", neJobId);
        Integer skippedActivitySize = 0;
        String jobResult = JobResult.FAILED.toString();
        final Map<Object, Object> restrictions = new HashMap<Object, Object>();
        restrictions.put(ShmConstants.NE_JOB_ID, neJobId);
        final PersistenceObject neJobPO = findPOByPoId(neJobId);
        final String neJobState = neJobPO.getAttribute(ShmConstants.STATE);
        final List<Map<String, Object>> activityJobPos = dpsReader.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.ACTIVITY_JOB, restrictions, Arrays.asList(ShmConstants.RESULT));
        final boolean isLastActivity = isLastActivity(activityJobPos);

        if (!activityJobPos.isEmpty()) {
            for (final Map<String, Object> activityJob : activityJobPos) {
                if (activityJob.get(ShmConstants.RESULT) != null && !activityJob.get(ShmConstants.RESULT).toString().isEmpty()) {
                    final String activity_result = activityJob.get(ShmConstants.RESULT).toString();
                    if (JobResult.SUCCESS.toString().equals(activity_result)) {
                        jobResult = JobResult.SUCCESS.getJobResult();
                        continue;
                    } else if (JobResult.SKIPPED.toString().equals(activity_result)) {
                        skippedActivitySize++;
                        continue;
                    } else if (JobResult.CANCELLED.toString().equals(activity_result)) {
                        jobResult = JobResult.CANCELLED.toString();
                        break;
                    } else {
                        jobResult = JobResult.FAILED.getJobResult();
                        break;
                    }
                }
            }
            if (skippedActivitySize == activityJobPos.size()) {
                jobResult = JobResult.SKIPPED.getJobResult();
            }
        } else {
            if (JobState.CANCELLING.getJobStateName().equals(neJobState)) {
                jobResult = JobResult.CANCELLED.getJobResult();
            }
        }
        if (isLastActivity && JobState.CANCELLING.getJobStateName().equals(neJobState)) {
            return jobResult;
        } else if (JobState.CANCELLING.getJobStateName().equals(neJobState)) {
            jobResult = JobResult.CANCELLED.toString();
        }
        LOGGER.debug("Exiting retrieveActivityJobResult() method with jobResult : {}", jobResult);
        return jobResult;
    }

    private static boolean isLastActivity(final List<Map<String, Object>> activityJobPos) {
        boolean isLastActivity = true;
        for (final Map<String, Object> activityJob : activityJobPos) {
            if (activityJob.get(ShmConstants.RESULT) != null && !activityJob.get(ShmConstants.RESULT).toString().isEmpty()) {
                continue;
            } else {
                isLastActivity = false;
            }
        }
        return isLastActivity;
    }

    @Override
    public String retrieveNeJobResult(final long mainjobId) {
        LOGGER.info("Inside the retrieveNeJobResult with mainjobid:{}", mainjobId);
        String jobResult = JobResult.FAILED.toString();
        final Map<Object, Object> restrictions = new HashMap<>();
        restrictions.put(ShmConstants.MAINJOBID, mainjobId);
        final List<Map<String, Object>> neJobPos = dpsReader.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.NE_JOB, restrictions, Arrays.asList(ShmConstants.RESULT));
        Integer skippedNesSize = 0;
        Integer failedNesSize = 0;
        LOGGER.debug("NEjob POs size is : {}", neJobPos.size());
        if (!neJobPos.isEmpty()) {
            final Iterator<Map<String, Object>> iterator = neJobPos.iterator();
            while (iterator.hasNext()) {
                final Map<String, Object> neJobPoAttributes = iterator.next();
                String neJobResult = "";
                try {
                    neJobResult = neJobPoAttributes.get(ShmConstants.RESULT).toString();
                } catch (final NullPointerException ne) {
                    //To discard the duplicate NEJob POs(they will have empty result) which could generate by Neo4J.
                    LOGGER.error("Job result attribute for the NEJobPO with PO ID :: {} is null", neJobPoAttributes.get(ShmConstants.PO_ID));
                    iterator.remove();
                    continue;
                }
                if (JobResult.SUCCESS.getJobResult().equals(neJobResult)) {
                    jobResult = JobResult.SUCCESS.toString();
                    continue;
                } else if (JobResult.SKIPPED.getJobResult().equals(neJobResult)) {
                    skippedNesSize++;
                    continue;
                } else if (JobResult.CANCELLED.getJobResult().equals(neJobResult)) {
                    jobResult = JobResult.CANCELLED.getJobResult();
                    break;
                } else {
                    jobResult = JobResult.FAILED.getJobResult();
                    failedNesSize++;
                    continue;
                }
            }
            jobResult = evaluateNeJobResult(jobResult, neJobPos, skippedNesSize, failedNesSize);
        } else {
            jobResult = evaluateJobResult(mainjobId, jobResult);
        }
        LOGGER.debug("Exiting retrieveNeJobResult() with NE jobResult : {}", jobResult);
        return jobResult;
    }

    private String evaluateJobResult(final long mainjobId, String jobResult) {
        final PersistenceObject mainJobPO = findPOByPoId(mainjobId);
        final String mainJobState = mainJobPO.getAttribute(ShmConstants.STATE);
        if (JobState.CANCELLING.getJobStateName().equals(mainJobState)) {
            jobResult = JobResult.CANCELLED.getJobResult();
        }
        return jobResult;
    }

    private String evaluateNeJobResult(String jobResult, final List<Map<String, Object>> neJobPos, final Integer skippedNesSize, final Integer failedNesSize) {
        if (skippedNesSize == neJobPos.size()) {
            jobResult = JobResult.SKIPPED.getJobResult();
        } else if (failedNesSize > 0 && !jobResult.equals(JobResult.CANCELLED.getJobResult())) {
            jobResult = JobResult.FAILED.getJobResult();
        }
        return jobResult;
    }

    /**
     * get JobType from mainJobId
     *
     * @param mainJobId
     */
    @Override
    public String getJobType(final long mainJobId) {
        return getJobData(mainJobId, ShmConstants.JOB_TYPE);
    }

    /**
     * get JobCategory from mainJobId
     *
     * @param mainJobId
     */
    @Override
    public String getJobCategory(final long mainJobId) {
        return getJobData(mainJobId, ShmConstants.JOB_CATEGORY);
    }

    private String getJobData(final long mainJobId, final String attributeName) {

        String data = "";
        try {
            final Map<String, Object> mainJobAttributes = jobUpdateService.retrieveJobWithRetry(mainJobId);
            final long templateJobId = (long) mainJobAttributes.get(ShmConstants.JOB_TEMPLATE_ID);
            final Map<String, Object> templateJobAttributes = jobUpdateService.retrieveJobWithRetry(templateJobId);
            data = (String) templateJobAttributes.get(attributeName);

        } catch (final RuntimeException ex) {
            LOGGER.error("Unable to generate alarm Exception while fetching attributeName: {}, {}", attributeName, ex.getMessage());
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
        }

        LOGGER.debug("Attribute value retrieved for key: {} is :{}", attributeName, data);

        return data;
    }

    /**
     * retrieve all details needed to raise an Internal alarm from mainJobId
     *
     * @param mainJobId
     */
    @Override
    public Map<String, Object> getJobDetailsToRaiseAlarm(final long mainJobId) {
        final Map<String, Object> jobDetails = new HashMap<>();
        Date creationDate;
        long templateJobId = 0;
        try {
            final PersistenceObject mainJobPo = dpsReader.findPOByPoId(mainJobId);
            if (mainJobPo != null) {
                creationDate = mainJobPo.getAttribute(ShmConstants.STARTTIME);
                jobDetails.put(ShmConstants.CREATION_TIME, creationDate);
                templateJobId = mainJobPo.getAttribute(ShmConstants.JOBTEMPLATEID);
            }
        } catch (final RuntimeException ex) {
            LOGGER.error("Unable to generate alarm  Exception while fetching Main job details {}", ex.getMessage());
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
        }

        jobDetails.putAll(getTemplateJobDetailsForAlarm(templateJobId));
        jobDetails.putAll(getNeCountForAlarm(mainJobId));

        return jobDetails;
    }

    /**
     * retrieve total Ne's count and Failed NE's count
     *
     * @param mainJobId
     */
    private Map<String, Object> getNeCountForAlarm(final long mainJobId) {
        final Map<String, Object> jobDetails = new HashMap<String, Object>();
        final Map<Object, Object> restrictions = new HashMap<Object, Object>();
        Integer totalNesSize = 0;
        Integer failedNesSize = 0;
        Integer skippedNesSize = 0;
        restrictions.put(ShmConstants.MAINJOBID, mainJobId);
        try {
            final List<Map<String, Object>> neJobPos = dpsReader.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.NE_JOB, restrictions, Arrays.asList(ShmConstants.RESULT));

            LOGGER.debug("NEjob POs size is : {}", neJobPos.size());
            if (!neJobPos.isEmpty()) {
                for (final Map<String, Object> neJobPoAttributes : neJobPos) {
                    final String neJobResult = neJobPoAttributes.get(ShmConstants.RESULT).toString();
                    if (JobResult.SUCCESS.getJobResult().equals(neJobResult)) {
                        continue;
                    } else if (JobResult.SKIPPED.getJobResult().equals(neJobResult)) {
                        skippedNesSize++;
                    } else {
                        failedNesSize++;
                    }
                }
                totalNesSize = neJobPos.size();
            }
        } catch (final RuntimeException ex) {
            LOGGER.error("Exception while fetching count for alarm details {}", ex.getMessage());
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
        }
        jobDetails.put(ShmConstants.TOTAL_NES, totalNesSize);
        jobDetails.put(ShmConstants.FAILED_NES, failedNesSize);
        jobDetails.put(ShmConstants.SKIPPED_NES, skippedNesSize);
        LOGGER.debug("Unable to generate alarm for alarm restcall total NEs: {}  failed NEs: {} skipped NEs: {} ", totalNesSize, failedNesSize, skippedNesSize);

        return jobDetails;
    }

    /**
     * retrieve details of the TemPlateJob PO from template JobId
     *
     * @param templateJobId
     */
    private Map<String, Object> getTemplateJobDetailsForAlarm(final long templateJobID) {
        final Map<String, Object> jobDetails = new HashMap<>();
        try {
            final PersistenceObject templateJobPo = dpsReader.findPOByPoId(templateJobID);

            final String jobName = (String) templateJobPo.getAttribute(ShmConstants.NAME);
            final String user = (String) templateJobPo.getAttribute(ShmConstants.OWNER);
            final String jobType = templateJobPo.getAttribute(ShmConstants.JOB_TYPE);
            jobDetails.put(ShmConstants.NAME, jobName);
            jobDetails.put(ShmConstants.OWNER, user);
            jobDetails.put(ShmConstants.JOB_TYPE, jobType);
        } catch (final RuntimeException ex) {
            LOGGER.error("Unable to generate alarm Exception while fetching template job details {}", ex.getMessage());
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
        }
        return jobDetails;
    }

    @Override
    public Map<String, Object> getActivitiesCount(final long neJobId) {
        final Map<String, Object> restrictions = new HashMap<String, Object>();
        final Map<String, Object> activityCount = new HashMap<String, Object>();
        int completedActivities = 0;
        int totalActivities = 0;
        restrictions.put(ShmConstants.NE_JOB_ID, neJobId);
        final List<PersistenceObject> activityJobPos = dpsReader.findPOs(ShmConstants.NAMESPACE, ShmConstants.ACTIVITY_JOB, restrictions);

        if (activityJobPos != null && !activityJobPos.isEmpty()) {
            for (final PersistenceObject activityJobPo : activityJobPos) {
                if (activityJobPo != null) {
                    totalActivities++;
                    final Map<String, Object> attributeMap = activityJobPo.getAllAttributes();
                    final String activityState = attributeMap.get(ShmConstants.STATE).toString();
                    if (JobState.isJobInactive(JobState.getJobState(activityState))) {
                        completedActivities++;
                    }
                }
            }
        }
        activityCount.put("totalActivities", totalActivities);
        activityCount.put("completedActivities", completedActivities);
        return activityCount;
    }

    @Override
    public List<SHMJobData> getJobTemplateDetails(final List<SHMJobData> shmJobDataList) {
        LOGGER.debug("Inside JobConfigurationServiceImpl.getJobTemplateDetails() method's with job data size is: {}", shmJobDataList.size());
        final List<SHMJobData> shmJobsDataList = new ArrayList<SHMJobData>();
        final List<PersistenceObject> jobTemplatePOs = dpsReader.findPOs(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE, new HashMap<String, Object>());
        final List<Long> jobTemplateIdList = new ArrayList<Long>();
        for (final SHMJobData shmJobData : shmJobDataList) {
            final Long jobTemplatePoId = Long.parseLong(shmJobData.getJobTemplateId());
            jobTemplateIdList.add(jobTemplatePoId);
        }
        for (final PersistenceObject jobTemplatePO : jobTemplatePOs) {
            String name = null;
            String jobType = null;
            String owner = null;
            Date creationTime = null;
            String createdDate = null;
            String parsedDate = null;
            final Date date = null;
            final Long jobTempPoId = jobTemplatePO.getPoId();
            if (!jobTemplateIdList.contains(jobTempPoId)) {
                final SHMJobData shmJobData = new SHMJobData();
                final Map<String, Object> jobTemplateAttributes = jobTemplatePO.getAllAttributes();
                if (isJobTemplateDeletable(jobTemplateAttributes)) {
                    continue;
                }
                if (jobTemplateAttributes.get(ShmConstants.NAME) != null) {
                    name = (String) jobTemplateAttributes.get(ShmConstants.NAME);
                } else {
                    name = "";
                }
                if (jobTemplateAttributes.get(ShmConstants.JOB_TYPE) != null) {
                    jobType = (String) jobTemplateAttributes.get(ShmConstants.JOB_TYPE);
                } else {
                    jobType = "";
                }
                if (jobTemplateAttributes.get(ShmConstants.OWNER) != null) {
                    owner = (String) jobTemplateAttributes.get(ShmConstants.OWNER);
                } else {
                    owner = "";
                }
                if (jobTemplateAttributes.get(ShmConstants.CREATION_TIME) != null) {
                    creationTime = (Date) jobTemplateAttributes.get(ShmConstants.CREATION_TIME);
                    LOGGER.debug("Creation Time:{}", creationTime);
                    createdDate = String.valueOf(creationTime.getTime());
                    LOGGER.debug("Creation Date:{}", createdDate);
                }
                String startDate = null;
                List<String> neNames = new ArrayList<>();
                String execMode = null;
                if (jobTemplateAttributes.get(ShmConstants.JOBCONFIGURATIONDETAILS) != null) {
                    final Map<String, Object> jobConfigurationdetails = (Map<String, Object>) jobTemplateAttributes.get(ShmConstants.JOBCONFIGURATIONDETAILS);
                    neNames = getSelectedNEs(neNames, jobConfigurationdetails);
                    if (jobConfigurationdetails.get(ShmConstants.MAINSCHEDULE) != null) {
                        final Map<String, Object> schedule = (Map<String, Object>) jobConfigurationdetails.get(ShmConstants.MAINSCHEDULE);
                        if (schedule != null) {
                            final List<Map<String, Object>> schedulePropertyList = (List<Map<String, Object>>) schedule.get(ShmConstants.SCHEDULINGPROPERTIES);
                            if (schedulePropertyList != null) {
                                LOGGER.debug("Scheduling Properties:{}", schedulePropertyList.size());
                                for (final Map<String, Object> scheduleProperty : schedulePropertyList) {
                                    if (scheduleProperty.get(ShmConstants.NAME).equals(ShmConstants.START_DATE)) {
                                        startDate = (String) scheduleProperty.get(ShmConstants.VALUE);
                                        if (!startDate.equals("") || startDate != null) {
                                            parsedDate = parseDates(parsedDate, date, startDate);
                                        }
                                    }
                                }
                                execMode = (String) schedule.get(ShmConstants.JOB_ACTIVITY_SCHEDULE);
                            }
                        }
                    }
                }

                shmJobData.setCreatedBy(owner);
                shmJobData.setJobName(name);
                shmJobData.setJobType(jobType);
                shmJobData.setNoOfMEs((neNames != null) ? neNames.size() : 0);
                shmJobData.setJobTemplateId(jobTempPoId);
                LOGGER.debug("Execution Mode: {}", execMode);
                if (execMode != null && execMode.equals(ExecMode.SCHEDULED.getMode()) && parsedDate != null) {
                    shmJobData.setStartDate(parsedDate);
                } else {
                    shmJobData.setStartDate("");
                    shmJobData.setCreationTime(createdDate);
                    LOGGER.debug("Creation Time set for id: {}", jobTempPoId);
                }
                shmJobData.setEndDate("");
                shmJobData.setResult("");
                final String jobTemplateStatus = deriveJobStatus(jobTemplateAttributes, execMode);
                shmJobData.setStatus(jobTemplateStatus);
                LOGGER.debug("Checking SHMJOBDATA: {}", shmJobData);
                shmJobsDataList.add(shmJobData);
            }
        }
        LOGGER.debug("The getJobTemplateDetails Method Exit: {}", shmJobDataList.size());
        return shmJobsDataList;

    }

    private String parseDates(String parsedDate, Date date, final String startDate) {
        if (startDate.contains("+")) {
            final String timeZoneId = getTimeZoneIdFromDate(startDate);
            final String delims = " ";
            final StringTokenizer st = new StringTokenizer(startDate, delims);
            final String formattedScheduleTime = st.nextToken() + "T" + st.nextToken();
            final DateTimeZone fromTimeZone = DateTimeZone.forID(timeZoneId);
            final DateTime dateTime = new DateTime(formattedScheduleTime, fromTimeZone);
            final DateTimeFormatter outputFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ssZ").withZone(DateTimeZone.getDefault());
            final String sheduledDate = outputFormatter.print(dateTime);
            final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
            try {
                date = formatter.parse(sheduledDate);
            } catch (final ParseException e) {
                LOGGER.error("Cannot parse Date:: {}", sheduledDate);
            }
            final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'ZUTC'XXX");
            LOGGER.debug("Start time to be parsed: {}", date);
            parsedDate = dateFormatter.format(date);
            LOGGER.debug("Start Date: {}", parsedDate);
        }
        return parsedDate;
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

    private List<String> getSelectedNEs(List<String> neNames, final Map<String, Object> jobConfigurationdetails) {
        List<String> collectionNames;
        if (jobConfigurationdetails.get(ShmConstants.SELECTED_NES) != null) {
            final Map<String, Object> neInfo = (Map<String, Object>) jobConfigurationdetails.get(ShmConstants.SELECTED_NES);

            if (neInfo != null) {
                collectionNames = (List<String>) neInfo.get(ShmConstants.COLLECTION_NAMES);
                LOGGER.debug("Job template collections data:{}", collectionNames);
                neNames = (List<String>) neInfo.get(ShmConstants.NENAMES);
            }
        }
        return neNames;
    }

    @Override
    public List<Map<String, Object>> getProjectedAttributes(final String namespace, final String type, final Map<Object, Object> restrictionAttributes, final List<String> projectedAttributes) {
        return dpsReader.getProjectedAttributes(namespace, type, restrictionAttributes, projectedAttributes);
    }

    @Override
    public boolean persistRunningJobAttributes(final long jobId, final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList) {
        if (!isResultEvaluated(jobId)) {
            persistJobAttributes(jobId, jobPropertyList, jobLogList);
            return true;
        }
        return false;
    }

    @Override
    public boolean readAndPersistRunningJobAttributes(final long jobId, final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList,
            final Double activityProgressPercentage) {
        if (CollectionUtils.isNotEmpty(jobPropertyList) || CollectionUtils.isNotEmpty(jobLogList) || activityProgressPercentage != null) {
            final boolean isJobResultEvaluated = isResultEvaluated(jobId);
            LOGGER.debug("JobConfigurationService - readAndPersistRunningJobAttributes : isJobResultEvaluated : {}", isJobResultEvaluated);
            if (!isJobResultEvaluated) {
                readAndPersistJobAttributes(jobId, jobPropertyList, jobLogList, activityProgressPercentage);
                return true;
            }
        } else {
            LOGGER.debug("Job log list and Job property list and activityProgressPercentage are empty/null and hence skipped updating job attributes for job Id : {}", jobId);
        }
        return false;
    }

    @Override
    public boolean isJobResultEvaluated(final long jobId) {
        return isResultEvaluated(jobId);
    }

    private boolean isResultEvaluated(final long jobId) {
        boolean isJobResultEvaluated = false;
        try {
            final Map<String, Object> jobAttributes = retrieveJob(jobId);
            if (jobAttributes != null && !jobAttributes.isEmpty()) {
                final List<Map<String, String>> jobPropertyList = (List<Map<String, String>>) jobAttributes.get(ShmConstants.JOBPROPERTIES);
                LOGGER.debug("Job Property List for job {} : {}", jobId, jobPropertyList);
                if (jobPropertyList != null && jobPropertyList.size() > 0) {
                    for (final Map<String, String> jobProperty : jobPropertyList) {
                        if (ActivityConstants.ACTIVITY_RESULT.equals(jobProperty.get(ShmConstants.KEY))) {
                            isJobResultEvaluated = true;
                            break;
                        }
                    }
                }
            }
        } catch (final Exception ex) {
            LOGGER.error("Failed to Verify job result for the job id: {}", jobId);
        }
        return isJobResultEvaluated;
    }

    private void persistJobAttributes(final long jobId, final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList) {

        final Map<String, Object> validatedAttributes = new HashMap<String, Object>();

        if (jobPropertyList != null && jobPropertyList.size() > 0) {
            validatedAttributes.put("jobProperties", jobPropertyList);
        }

        if (jobLogList != null && jobLogList.size() > 0) {
            final Map<String, Object> activityJobAttr = retrieveJob(jobId);
            if (activityJobAttr != null && !activityJobAttr.isEmpty()) {

                List<Map<String, Object>> activityJobLogList = new ArrayList<Map<String, Object>>();
                if (activityJobAttr.get(ActivityConstants.JOB_LOG) != null) {
                    activityJobLogList = (List<Map<String, Object>>) activityJobAttr.get(ActivityConstants.JOB_LOG);
                }
                activityJobLogList.addAll(jobLogList);
                validatedAttributes.put(ActivityConstants.JOB_LOG, activityJobLogList);
                if (!isMainJob(activityJobAttr)) {
                    final String lastLogMessage = retrieveLastLogMessage(activityJobLogList);
                    validatedAttributes.put(ShmConstants.LAST_LOG_MESSAGE, lastLogMessage);
                }
            }
        }
        dpsWriter.update(jobId, validatedAttributes);
    }

    private void readAndPersistJobAttributes(final long jobId, final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList, final Double activityProgressPercentage) {

        LOGGER.debug("JobConfigurationService - readAndPersistJobAttributes : jobId - {} jobProperties - {}", jobId, jobPropertyList);
        final Map<String, Object> validatedAttributes = new HashMap<String, Object>();
        final Map<String, Object> activityJobAttr = retrieveJob(jobId);
        LOGGER.trace("Existing job attributs:{}, for jobid:{}", activityJobAttr, jobId);
        final List<Map<String, Object>> storedJobPropertiesList = (List<Map<String, Object>>) activityJobAttr.get(ActivityConstants.JOB_PROPERTIES);
        LOGGER.debug("JobConfigurationService - storedjobpropertieslist : {}", storedJobPropertiesList);
        if (jobPropertyList != null && !jobPropertyList.isEmpty()) {

            final List<Map<String, Object>> updatedJobPropertyList = getUpdatedJobProperties(jobPropertyList, storedJobPropertiesList);

            validatedAttributes.put(ActivityConstants.JOB_PROPERTIES, updatedJobPropertyList);
            LOGGER.debug("JobConfigurationService - readAndPersistJobAttributes : updated jobproperties : {}", updatedJobPropertyList);

        }
        prepareLatestJobLogsToPersistIntoDB(jobLogList, validatedAttributes, activityJobAttr);
        prepareLatestActivityProgresstoPersistInDB(activityProgressPercentage, validatedAttributes, activityJobAttr);
        dpsWriter.update(jobId, validatedAttributes);
        LOGGER.debug("Completed JobConfigurationService - readAndPersistJobAttributes : jobId - {} {}", jobId, validatedAttributes.get(ActivityConstants.JOB_PROPERTIES));
    }

    /**
     * @param activityProgressPercentage
     * @param validatedAttributes
     * @param activityJobAttr
     */
    protected void prepareLatestActivityProgresstoPersistInDB(final Double activityProgressPercentage, final Map<String, Object> validatedAttributes, final Map<String, Object> activityJobAttr) {
        if (activityProgressPercentage != null) {
            final double progressPercentageFromDB = (double) activityJobAttr.get(ShmConstants.PROGRESSPERCENTAGE);
            if (activityProgressPercentage > progressPercentageFromDB) {
                if (activityProgressPercentage >= 100) {
                    validatedAttributes.put(ShmConstants.PROGRESSPERCENTAGE, 100.0);
                } else {
                    validatedAttributes.put(ShmConstants.PROGRESSPERCENTAGE, activityProgressPercentage);
                }
            }
        }
    }

    /**
     * @param jobLogList
     * @param validatedAttributes
     * @param activityJobAttr
     */
    protected void prepareLatestJobLogsToPersistIntoDB(final List<Map<String, Object>> jobLogList, final Map<String, Object> validatedAttributes, final Map<String, Object> activityJobAttr) {
        if (jobLogList != null && !jobLogList.isEmpty() && !activityJobAttr.isEmpty()) {
            List<Map<String, Object>> activityJobLogList = new ArrayList<Map<String, Object>>();
            if (activityJobAttr.get(ActivityConstants.JOB_LOG) != null) {
                activityJobLogList = (List<Map<String, Object>>) activityJobAttr.get(ActivityConstants.JOB_LOG);
            }
            filterDuplicateLogs(jobLogList, activityJobLogList);
            if (!isMainJob(activityJobAttr)) {
                final String lastLogMessage = retrieveLastLogMessage(activityJobLogList);
                validatedAttributes.put(ShmConstants.LAST_LOG_MESSAGE, lastLogMessage);
            }
            validatedAttributes.put(ActivityConstants.JOB_LOG, activityJobLogList);
        }
    }

    /**
     * This method filter duplicate job log messages.
     *
     * @param jobLogList
     * @param activityJobLogList
     */
    private void filterDuplicateLogs(final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> activityJobLogList) {
        for (final Map<String, Object> jobLog : jobLogList) {
            boolean isLogMessageExists = false;
            final String logMessage = (String) jobLog.get(ActivityConstants.JOB_LOG_MESSAGE);

            for (final Map<String, Object> jobLogsFromDB : activityJobLogList) {
                if (jobLogsFromDB.containsValue(logMessage)) {
                    isLogMessageExists = true;
                    break;
                }
            }
            if (!isLogMessageExists) {
                activityJobLogList.add(jobLog);
            }
        }
    }

    /**
     * lastLogMessage attribute is present only in NE Job and Activity Job. So retrieve and persisting lastLogMessage is not applicable to Main Job. This method is used to decide whether the Job is
     * Main Job or not, based on JobTemplate Id as this attribute is present in only Main Job.
     *
     */
    private boolean isMainJob(final Map<String, Object> jobAttr) {
        return jobAttr.containsKey(ShmConstants.JOB_TEMPLATE_ID);
    }

    protected String retrieveLastLogMessage(final List<Map<String, Object>> activityJobLogList) {
        Collections.sort(activityJobLogList, new Comparator<Map<String, Object>>() {

            @Override
            public int compare(final Map<String, Object> map1, final Map<String, Object> map2) {
                final Date date1 = (Date) map1.get(ActivityConstants.JOB_LOG_ENTRY_TIME);
                final Date date2 = (Date) map2.get(ActivityConstants.JOB_LOG_ENTRY_TIME);
                return date1.compareTo(date2);
            }
        });
        final Date date = (Date) activityJobLogList.get(activityJobLogList.size() - 1).get(ActivityConstants.JOB_LOG_ENTRY_TIME);
        final String lastLogMessage = (String) activityJobLogList.get(activityJobLogList.size() - 1).get(ActivityConstants.JOB_LOG_MESSAGE);
        return lastLogMessage + ShmConstants.DELIMITER_PIPE + date.getTime();
    }

    /**
     * Compares the existing(in DB) job properties with properties from ongoing activity if key exist in both then update respective key value in existing properties with activity property value If
     * does not exist then add that property to existing set.
     *
     * TODO this method logic is very complex but this required to met the job properties modelling.If there is any change in the model then it should be updated accordingly
     *
     * @param jobPropertyList
     * @param storedJobPropertiesList
     * @return
     */
    @Override
    public List<Map<String, Object>> getUpdatedJobProperties(final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> storedJobPropertiesList) {
        LOGGER.debug("JobConfigurationService - getUpdatedJobProperties");
        boolean isNewJobProperty = true;
        final List<Map<String, Object>> deltaJobPropertiesList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> finalListOfProperties = new ArrayList<Map<String, Object>>();
        Entry<String, Object> keyEntry = null;
        Entry<String, Object> valueEntry = null;
        if (jobPropertyList != null && !jobPropertyList.isEmpty()) {

            for (final Map<String, Object> jobProperty : jobPropertyList) {
                final Iterator<Entry<String, Object>> jobPropertyEntrySet = jobProperty.entrySet().iterator();
                while (jobPropertyEntrySet.hasNext()) {
                    final Entry<String, Object> firstEntry = jobPropertyEntrySet.next();
                    final Entry<String, Object> secondEntry = jobPropertyEntrySet.next();
                    if (firstEntry.getKey().equalsIgnoreCase(ActivityConstants.JOB_PROP_KEY)) {
                        keyEntry = firstEntry;
                        valueEntry = secondEntry;
                    } else {
                        keyEntry = secondEntry;
                        valueEntry = firstEntry;
                    }

                    isNewJobProperty = true;
                    if (storedJobPropertiesList != null) {
                        Entry<String, Object> storedKeyEntry = null;
                        for (final Map<String, Object> storedJobProperty : storedJobPropertiesList) {
                            final Iterator<Entry<String, Object>> storedJobPropertyEntrySet = storedJobProperty.entrySet().iterator();
                            while (storedJobPropertyEntrySet.hasNext()) {
                                final Entry<String, Object> storedFirstEntry = storedJobPropertyEntrySet.next();
                                final Entry<String, Object> storedSecondEntry = storedJobPropertyEntrySet.next();
                                if (storedFirstEntry.getKey().equalsIgnoreCase(ActivityConstants.JOB_PROP_KEY)) {
                                    storedKeyEntry = storedFirstEntry;
                                } else {
                                    storedKeyEntry = storedSecondEntry;
                                }

                                if (keyEntry.getValue().toString().equalsIgnoreCase(storedKeyEntry.getValue().toString()) && keyEntry.getKey().equalsIgnoreCase(ActivityConstants.JOB_PROP_KEY)
                                        && storedKeyEntry.getKey().equalsIgnoreCase(ActivityConstants.JOB_PROP_KEY)) {
                                    isNewJobProperty = false;

                                    storedJobProperty.put(ActivityConstants.JOB_PROP_VALUE, valueEntry.getValue());
                                }
                            }
                        }
                    }

                    if (isNewJobProperty) {
                        final Map<String, Object> newJobProperty = new HashMap<String, Object>();
                        newJobProperty.put(ActivityConstants.JOB_PROP_KEY, keyEntry.getValue());
                        newJobProperty.put(ActivityConstants.JOB_PROP_VALUE, valueEntry.getValue());
                        deltaJobPropertiesList.add(newJobProperty);
                    }
                }
            }
        }
        if (storedJobPropertiesList != null) {
            finalListOfProperties.addAll(storedJobPropertiesList);
        }
        finalListOfProperties.addAll(deltaJobPropertiesList);
        return finalListOfProperties;

    }

    @Override
    public void addOrUpdateOrRemoveJobProperties(final long jobId, final Map<String, String> propertyTobeAdded, final List<Map<String, Object>> jobLogList) {
        final Map<String, Object> activityJobAttributes = retrieveJob(jobId);
        List<Map<String, Object>> validatedAttributes = new ArrayList<Map<String, Object>>();
        if (propertyTobeAdded != null && !propertyTobeAdded.isEmpty()) {
            validatedAttributes = getModifiedProperties(propertyTobeAdded, activityJobAttributes);
        }
        persistJobAttributes(jobId, validatedAttributes, jobLogList);
    }

    private List<Map<String, Object>> getModifiedProperties(final Map<String, String> propertyTobeAdded, final Map<String, Object> activityJobAttributes) {
        final List<Map<String, Object>> jobProperties = (List<Map<String, Object>>) activityJobAttributes.get(ActivityConstants.JOB_PROPERTIES);
        final List<Map<String, Object>> activityJobPropertyList = jobProperties != null ? jobProperties : new ArrayList<Map<String, Object>>();
        for (final Entry<String, String> propertyEntries : propertyTobeAdded.entrySet()) {
            final String value = propertyEntries.getValue();
            if (!activityJobPropertyList.isEmpty()) {
                boolean isUpdated = false;
                for (int index = activityJobPropertyList.size() - 1; index >= 0; index--) {
                    final Map<String, Object> jobProperty = activityJobPropertyList.get(index);
                    if (propertyEntries.getKey().equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                        if (value.isEmpty()) {
                            activityJobPropertyList.remove(jobProperty);
                        } else {
                            jobProperty.put(ActivityConstants.JOB_PROP_VALUE, value);
                        }
                        isUpdated = true;
                    }
                }
                if (!isUpdated) {
                    addNewProperty(activityJobPropertyList, propertyEntries);
                }
            } else {
                addNewProperty(activityJobPropertyList, propertyEntries);
            }
        }
        return activityJobPropertyList;
    }

    private void addNewProperty(final List<Map<String, Object>> activityJobPropertyList, final Entry<String, String> entries) {
        if (!entries.getValue().isEmpty()) {
            final Map<String, Object> newJobProperty = new HashMap<String, Object>();
            newJobProperty.put(ActivityConstants.JOB_PROP_KEY, entries.getKey());
            newJobProperty.put(ActivityConstants.JOB_PROP_VALUE, entries.getValue());
            activityJobPropertyList.add(newJobProperty);
        }
    }

    @Override
    public boolean readAndPersistJobAttributesForCancel(final long jobId, final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList) {
        readAndPersistJobAttributes(jobId, jobPropertyList, jobLogList, null);
        return true;
    }

    @Override
    public boolean isNEJobProceedsForCancel(final long neJobId) {
        final Map<String, Object> neJobAttributes = retrieveJob(neJobId);
        final String jobState = (String) neJobAttributes.get(ShmConstants.STATE);
        final boolean isJobCompleted = JobState.isJobCompleted(JobState.getJobState(jobState));

        final boolean isAxeNodeUpgrade = isAxeUpgrade(neJobAttributes);
        // If NE job not completed its execution , then only set ne Job state to cancelling
        if (!isJobCompleted) {
            if (!isAxeNodeUpgrade) {
                final Map<String, Object> poAttributes = new HashMap<String, Object>();
                poAttributes.put(ShmConstants.STATE, JobState.CANCELLING.getJobStateName());
                LOGGER.debug("updating NE job status to CANCELLING for the NE with ID: {}", neJobId);
                dpsWriter.update(neJobId, poAttributes);
            }
            return true;
        }
        return false;
    }

    public boolean isAxeUpgrade(final Map<String, Object> neJobAttributes) {
        final long mainJobId = (long) neJobAttributes.get(ShmConstants.MAIN_JOB_ID);
        final String nodeName = (String) neJobAttributes.get(ShmConstants.NE_NAME);
        final String jobType = getJobType(mainJobId);
        final List<NetworkElement> networkElements = fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(Arrays.asList(getParentNodeName(nodeName)), SHMCapabilities.UPGRADE_JOB_CAPABILITY);
        return !networkElements.isEmpty() && PlatformTypeEnum.AXE == networkElements.get(0).getPlatformType() && JobTypeEnum.getJobType(jobType) == JobTypeEnum.UPGRADE;
    }

    /*
     * Node name may contains component name /cluster name. Example : MSC07__BC01,MSC07-AXE_CLUSTER
     */
    private String getParentNodeName(final String nodeName) {
        String parentNodeName = null;
        if (nodeName.contains(ShmConstants.DELIMITER_DOUBLE_UNDERSCORE)) {
            parentNodeName = nodeName.substring(0, nodeName.indexOf(ShmConstants.DELIMITER_DOUBLE_UNDERSCORE));
        } else if (nodeName.contains(ShmConstants.CLUSTER_SUFFIX)) {
            parentNodeName = nodeName.substring(0, nodeName.indexOf(ShmConstants.CLUSTER_SUFFIX));
        } else {
            parentNodeName = nodeName;
        }
        return parentNodeName;
    }

    @Override
    public Map<String, Object> retrieveWaitingActivityDetails(final long neJobId) {
        LOGGER.debug("Entered Into retrieveActivityNameWhichWaitingForUserInput method with neJobId {}", neJobId);
        final Map<String, Object> activityAttributes = new HashMap<String, Object>();
        final Map<String, Object> restrictions = new HashMap<String, Object>();
        restrictions.put(ShmConstants.NE_JOB_ID, neJobId);
        restrictions.put(ShmConstants.STATE, JobState.WAIT_FOR_USER_INPUT.name());
        final List<PersistenceObject> activityJobPOs = dpsReader.findPOs(ShmConstants.NAMESPACE, ShmConstants.ACTIVITY_JOB, restrictions);
        LOGGER.debug("ActivityJob POs List : {}", activityJobPOs);
        if (activityJobPOs != null && !activityJobPOs.isEmpty()) {

            for (final PersistenceObject activityJob : activityJobPOs) {
                final String activityName = (String) activityJob.getAttribute(ShmConstants.NAME);
                final long poid = activityJob.getPoId();
                activityAttributes.put(ShmConstants.NAME, activityName);
                activityAttributes.put(ShmConstants.PO_ID, poid);
            }
        }
        LOGGER.debug("Exiting retrieveActivityNameWhichWaitingForUserInput() method with activityName : {}", activityAttributes);
        return activityAttributes;
    }

    @Override
    public Map<String, Object> getActivitiesCountAndPercentage(final long neJobId) {
        final Map<String, Object> restrictions = new HashMap<String, Object>();
        final Map<String, Object> activityCountAndPercentage = new HashMap<String, Object>();
        int totalActivities = 0;
        Double activitypercent = 0.0;
        restrictions.put(ShmConstants.NE_JOB_ID, neJobId);
        final List<PersistenceObject> activityJobPos = dpsReader.findPOs(ShmConstants.NAMESPACE, ShmConstants.ACTIVITY_JOB, restrictions);
        if (activityJobPos != null && !activityJobPos.isEmpty()) {
            for (final PersistenceObject activityJobPo : activityJobPos) {
                if (activityJobPo != null) {
                    totalActivities++;
                    final Double currentActivitypercent = activityJobPo.getAttribute(ShmConstants.PROGRESSPERCENTAGE);
                    activitypercent = activitypercent + currentActivitypercent;
                }
            }
        }
        activityCountAndPercentage.put(TOTAL_ACTIVITIES, totalActivities);
        activityCountAndPercentage.put(TOTAL_ACTIVITIES_PROGRESS_PERCENTAGE, activitypercent);
        LOGGER.debug("activityCount And percentage {}", activityCountAndPercentage);
        return activityCountAndPercentage;
    }

    @Override
    public List<Map<String, Object>> getActivityAttributesByNeJobId(final long neJobId, final Map<String, Object> restrictions) {
        final List<Map<String, Object>> activityJobAttributes = new ArrayList<>();
        final List<PersistenceObject> activityJobPos = dpsReader.findPOs(ShmConstants.NAMESPACE, ShmConstants.ACTIVITY_JOB, restrictions);
        if (activityJobPos != null && !activityJobPos.isEmpty()) {
            for (final PersistenceObject activityJobPo : activityJobPos) {
                if (activityJobPo != null) {
                    final Map<String, Object> activity = new HashMap<>();
                    activity.put(ShmConstants.ACTIVITY_JOB_ID, activityJobPo.getPoId());
                    activity.put(ShmConstants.PO_ATTRIBUTES, activityJobPo.getAllAttributes());
                    activityJobAttributes.add(activity);
                }
            }
        }
        LOGGER.debug("activityJobAttributes from getActivityAttributesByNeJobId are: {}", activityJobAttributes);
        return activityJobAttributes;
    }

    @Override
    public boolean readAndPersistRunningJobStepDuration(final long activityID, final String stepNameAndDurationToPersist, final String stepName) {
        boolean isStepDurationPersisted = false;
        final List<String> currentStepDurationsFromDPSAsList = retrieveExistingStepDurationsFromDPS(activityID);
        if (currentStepDurationsFromDPSAsList != null) {
            final Map<String, Object> stepDurations = updateLatestStepDurations(currentStepDurationsFromDPSAsList, stepNameAndDurationToPersist, stepName);
            dpsWriter.update(activityID, stepDurations);
            LOGGER.debug("Step Duration attributes for activityjobid {} saved to DPS as : {}", activityID, stepDurations);
            isStepDurationPersisted = true;
        }
        return isStepDurationPersisted;
    }

    /**
     * This method retrieves data for the specified job ID from DPS and returns the stepDuration attribute in a {@link List} format.
     *
     * @param jobId
     *            - The job ID.
     * @return The stepDuration attribute in a {@link List} format.
     */
    private List<String> retrieveExistingStepDurationsFromDPS(final long activityJobId) {
        final long startTime = System.currentTimeMillis();
        final PersistenceObject activityJobPO = findPOByPoId(activityJobId);
        final long endTime = System.currentTimeMillis();
        final String existingStepDurationsFromDPS = activityJobPO.getAttribute(ShmConstants.STEP_DURATIONS);
        if (existingStepDurationsFromDPS == null) {
            return new ArrayList<String>();
        }
        LOGGER.debug("Got stepDurations for jobId {} from DPS as : {} and time taken: {}", activityJobId, existingStepDurationsFromDPS, endTime - startTime);
        return convertStringToList(existingStepDurationsFromDPS);
    }

    private PersistenceObject findPOByPoId(final long poId) {
        return dpsReader.findPOByPoId(poId);
    }

    /**
     * This method removes any duplicate entries present for the supplied step name and updates the map with the new value of stepNameAndDurationToPersist.
     *
     * @param currentStepDurationsFromDPS
     *            - Current value fetched from DPS.
     * @param stepNameAndDurationToPersist
     *            - The new value to be updated/appended.
     * @param activityStepName
     *            - All duplicates with this step name will be removed.
     * @param metricAttributesMap
     *            - The map with the updated value(s).
     */
    private Map<String, Object> updateLatestStepDurations(final List<String> currentStepDurationsFromDPS, final String stepNameAndDurationToPersist, final String activityStepName) {
        String duplicateStepEntry = null;
        final Map<String, Object> metricAttributesMap = new HashMap<String, Object>();
        if (currentStepDurationsFromDPS != null && !currentStepDurationsFromDPS.isEmpty()) {
            final ListIterator<String> listIterrator = currentStepDurationsFromDPS.listIterator();
            while (listIterrator.hasNext()) {
                duplicateStepEntry = listIterrator.next();
                if (duplicateStepEntry != null && duplicateStepEntry.startsWith(activityStepName)) {
                    listIterrator.remove();
                    LOGGER.trace("Removed duplicate entry : {}", duplicateStepEntry);
                }
            }
        }
        currentStepDurationsFromDPS.add(stepNameAndDurationToPersist);
        final String stringToPersist = currentStepDurationsFromDPS.toString();
        metricAttributesMap.put(ShmConstants.STEP_DURATIONS, stringToPersist);
        LOGGER.trace("Step duration map after updation from DPS : {}", metricAttributesMap);
        return metricAttributesMap;
    }

    /**
     * This method converts a {@link String} to {@link List}, provided the {@link String} was previously obtained from an {@link ArrayList} using {@link ArrayList#toString()} method. If the supplied
     * String is null, it will return an empty {@link List}.
     * <p>
     * NOTE : This will not convert any String representation to list, only Strings from {@link ArrayList} without any nesting can be converted.
     * <p>
     *
     * @param strToConvert
     *            - The string to convert.
     * @return {@link List} representation of the provided strToConvert.
     */
    @Override
    public List<String> convertStringToList(final String strToConvert) {
        final List<String> processedList = new ArrayList<String>();
        if (strToConvert == null || strToConvert.trim().length() <= 0) {
            LOGGER.trace("Returning empty as this is the first entry in DPS for this attribute : {}", processedList);
            return processedList;
        }
        final String stringWithTrimmedBrackets = strToConvert.substring(1, strToConvert.length() - 1);
        if (!stringWithTrimmedBrackets.contains(ShmConstants.FDN_DELIMITER)) {
            processedList.add(stringWithTrimmedBrackets.trim());
            return processedList;
        }
        final String[] stringArrAfterSplit = stringWithTrimmedBrackets.split(ShmConstants.FDN_DELIMITER);
        for (final String stepDuartion : stringArrAfterSplit) {
            processedList.add(stepDuartion.trim());
        }
        LOGGER.trace("Returning list as : {}", processedList);
        return processedList;
    }

    @Override
    public List<Long> getNeJobIDs(final long mainJobPoId) {
        final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final Query<TypeRestrictionBuilder> neJobTypeQuery = queryBuilder.createTypeQuery(ShmJobConstants.NAMESPACE, ShmJobConstants.NE_JOB);
        final TypeRestrictionBuilder resrtictionBuilder = neJobTypeQuery.getRestrictionBuilder();
        final Restriction mainJobIdRestriction = resrtictionBuilder.equalTo(ShmConstants.MAINJOBID, mainJobPoId);
        neJobTypeQuery.setRestriction(mainJobIdRestriction);
        return dataPersistenceService.getLiveBucket().getQueryExecutor().executeProjection(neJobTypeQuery, ProjectionBuilder.field(ObjectField.PO_ID));
    }

    @Override
    public Map<HealthStatus, Integer> getNodesByHealthStatus(final long mainJobId) {

        dataPersistenceService.setWriteAccess(Boolean.FALSE);

        final EnumMap<com.ericsson.oss.services.shm.jobs.common.enums.HealthStatus, Integer> nodesCountByHealthStatus = new EnumMap<>(HealthStatus.class);
        for (final HealthStatus healthStatus : HealthStatus.values()) {
            final QueryExecutor queryExecutor = dataPersistenceService.getLiveBucket().getQueryExecutor();
            final Query<TypeRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.NE_JOB);

            final TypeRestrictionBuilder restrictionBuilder = query.getRestrictionBuilder();
            final Restriction mainJobIdRestriction = restrictionBuilder.equalTo(ShmConstants.MAIN_JOB_ID, mainJobId);
            final Restriction healthStatusRestriction = restrictionBuilder.equalTo(ShmConstants.NEJOB_HEALTH_STATUS, healthStatus.name());

            query.setRestriction(restrictionBuilder.allOf(mainJobIdRestriction, healthStatusRestriction));

            final Long healthStatusRestrictionNodes = queryExecutor.executeCount(query);

            nodesCountByHealthStatus.put(healthStatus, healthStatusRestrictionNodes.intValue());
        }
        return nodesCountByHealthStatus;
    }

    @Override
    public String getReportCategory(final Long mainJobId) {
        final PersistenceObject persistentObject = dpsReader.findPOByPoId(mainJobId);
        final Map<String, Object> mainJobData = persistentObject.getAllAttributes();
        if (!mainJobData.isEmpty()) {
            final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobData.get(ShmConstants.JOBCONFIGURATIONDETAILS);
            final List<Map<String, Object>> neTypeJobProperties = (List<Map<String, Object>>) jobConfigurationDetails.get(ShmConstants.NETYPEJOBPROPERTIES);
            for (final Map<String, Object> neTypemap : neTypeJobProperties) {
                final List<Map<String, Object>> jobProperties = (List<Map<String, Object>>) neTypemap.get(ShmConstants.JOBPROPERTIES);
                return getPropertyValue(jobProperties, ShmConstants.NODE_HEALTH_CHECK_TEMPLATE);
            }
            LOGGER.error("NODE_HEALTH_CHECK_TEMPLATE property not found for mainJobId {} ", mainJobId);
        }
        return "";
    }

    private static String getPropertyValue(final List<Map<String, Object>> jobProperties, final String propertyName) {
        if (jobProperties != null) {
            for (final Map<String, Object> jobProperty : jobProperties) {
                if (propertyName != null && propertyName.equals(jobProperty.get(ShmConstants.KEY))) {
                    LOGGER.debug("Requested Property entry found {}", jobProperty);
                    return (String) jobProperty.get(ShmConstants.VALUE);
                }
            }
        }
        return "";
    }

    @Override
    public List<Long> getJobPoIdsFromParentJobId(final long neJobPoId, final String typeOfJob, final String restrictionAttribute) {
        try {
            final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
            final Query<TypeRestrictionBuilder> typeQuery = queryBuilder.createTypeQuery(ShmJobConstants.NAMESPACE, typeOfJob);
            final TypeRestrictionBuilder resrtictionBuilder = typeQuery.getRestrictionBuilder();
            final Restriction jobIdRestriction = resrtictionBuilder.equalTo(restrictionAttribute, neJobPoId);
            typeQuery.setRestriction(jobIdRestriction);
            return dataPersistenceService.getLiveBucket().getQueryExecutor().executeProjection(typeQuery, ProjectionBuilder.field(ObjectField.PO_ID));
        } catch (final RuntimeException runtimeException) {
            final boolean databaseIsDown = dpsAvailabilityInfoProvider.isDatabaseDown();
            LOGGER.error("Exception while fetching JobIds based on parent jobId {}. DPS status:{} and Exception is: {}", neJobPoId, databaseIsDown, runtimeException);
            return Collections.emptyList();
        }
    }
}
