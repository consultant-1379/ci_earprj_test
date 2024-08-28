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
package com.ericsson.oss.services.shm.es.impl;

import static com.ericsson.oss.services.shm.jobs.common.constants.JobConfigurationConstants.OWNER;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import javax.ejb.Asynchronous;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.resources.FileResourceImpl;
import com.ericsson.oss.itpf.sdk.resources.Resources;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.DateTimeUtils;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.FdnUtils;
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.common.smrs.retry.SmrsRetryPolicies;
import com.ericsson.oss.services.shm.es.api.ActivityStepDurationsReportGenerator;
import com.ericsson.oss.services.shm.es.api.JobReportConstants;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.service.SHMJobService;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.SmrsServiceConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.topologyservice.TopologyEvaluationService;

/**
 * This class is used for each main Job log report generation at the SMRS location ( default is "/home/smrs/smrsroot/jobreporting" ).
 * 
 * @author xarirud, tcschat
 */
@Stateless
@Traceable
public class ActivityStepDurationsReportGeneratorImpl implements ActivityStepDurationsReportGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityStepDurationsReportGeneratorImpl.class.getName());

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    @Inject
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    @Inject
    private FdnServiceBean fdnServiceBean;

    @Inject
    private SmrsFileStoreService smrsServiceUtil;

    @Inject
    private SmrsRetryPolicies smrsRetryPolicies;

    @Inject
    private PlatformTypeProviderImpl platformTypeProvider;

    @Inject
    private TopologyEvaluationService topologyEvaluationService;

    @Inject
    private SHMJobService shmJobService;

    @Inject
    private JobConfigurationService jobConfigurationService;

    @Inject
    private MainJobStepDurationsCalculator mainJobStepDurationsCalculator;

    @Override
    public void triggerJobReportGenerationThroughPibScript(final String jobName) {
        try {
            final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
            final Query<TypeRestrictionBuilder> jobQuery = setRestrictionToQueryMainJob(getTemplateJobId(jobName), queryBuilder);
            final QueryExecutor queryExecutor = dataPersistenceService.getLiveBucket().getQueryExecutor();
            final Iterator<PersistenceObject> mainJobIterator = queryExecutor.execute(jobQuery);
            long mainJobId = 0L;
            while (mainJobIterator.hasNext()) {
                final PersistenceObject persistenceObject = mainJobIterator.next();
                mainJobId = persistenceObject.getPoId();
            }
            writeJobReportText(mainJobId);
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while initiating job report generation Through Pib param for the job: {}. Exception is: ", jobName, ex);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Asynchronous
    @Override
    public void generateJobReportAndUpdateMainJob(final long mainJobId) {
        final Map<String, Object> mainJobStepDurationMetrics = new HashMap<String, Object>();
        final boolean isMainJobActivityAndStepDurationsCalculated = mainJobStepDurationsCalculator.calculateMetrics(mainJobId, mainJobStepDurationMetrics);
        if (isMainJobActivityAndStepDurationsCalculated) {
            updateMainJobStepDurationDetails(mainJobId, mainJobStepDurationMetrics);
        } else {
            LOGGER.error("Failed to calculate activity and step durations for Main Job having ID : {}", mainJobId);
        }
        try {
            writeJobReportText(mainJobId);
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while generating job report for the mainJob Id: {}. Exception is: ", mainJobId, ex);
        }
    }

    private void writeJobReportText(final long mainJobPoId) {
        String jobName = null;
        StringBuilder jobReportCsv = null;
        LOGGER.debug("In ActivityStepDurationsReportGeneratorImpl.writeJobReportText(), main job id and entering time : {}", mainJobPoId, new Date());
        try {
            final Map<String, String> neNamePlatformTypeMap = new HashMap<String, String>();
            final Map<String, StringBuilder> jobNameAndCsv = getMainJobDetailsForJobReport(mainJobPoId, neNamePlatformTypeMap);
            jobReportCsv = jobNameAndCsv.get(JobReportConstants.JOB_REPORT_TXT);
            jobName = jobNameAndCsv.get(JobReportConstants.JOB_NAME).toString();
            final String zipFileName = jobName + ShmConstants.ZIPFILE;
            final String reportFileName = jobName + JobReportConstants.FILE_EXTENSION_TXT;
            if (!jobReportCsv.equals(JobReportConstants.JOB_NOT_COMPLETED_MESSAGE)) {
                final StringBuilder content = createJobReportLogText(jobConfigurationService.getNeJobIDs(mainJobPoId), jobReportCsv, neNamePlatformTypeMap);
                writeToFileSystemUsingOutputStream(content.toString(), reportFileName, zipFileName);
            }
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while getting Main Job Details for creating job report : {} ", ex);
        }
        LOGGER.debug("In ActivityStepDurationsReportGeneratorImpl.writeJobReportText(), main job id and exiting time : {}", mainJobPoId, new Date());
    }

    private Query<TypeRestrictionBuilder> setRestrictionToQueryTemplateJob(final String jobName, final QueryBuilder queryBuilder) {
        final Query<TypeRestrictionBuilder> typeQuery = queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE);
        final Restriction restriction = typeQuery.getRestrictionBuilder().equalTo(ShmConstants.NAME, jobName);
        typeQuery.setRestriction(restriction);
        return typeQuery;
    }

    private Query<TypeRestrictionBuilder> setRestrictionToQueryMainJob(final Long templateJobId, final QueryBuilder queryBuilder) {
        final Query<TypeRestrictionBuilder> typeQuery = queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOB);
        final Restriction restriction = typeQuery.getRestrictionBuilder().equalTo(ShmConstants.JOBTEMPLATEID, templateJobId);
        typeQuery.setRestriction(restriction);
        return typeQuery;
    }

    private Map<String, List<String>> getActivityJobsForNeJob(final List<Long> neJobIdList, final Map<String, String> neNamePlatformTypeMap) {
        final Map<String, List<String>> activityDetails = new HashMap<String, List<String>>();
        try {
            final List<List<Long>> batchedNeJobIdsList = ListUtils.partition(neJobIdList, 200);
            final Map<Long, String> neJobdetails = new HashMap<Long, String>();
            final List<PersistenceObject> activityJobs = new ArrayList<PersistenceObject>();

            for (final List<Long> batchedNeJobIds : batchedNeJobIdsList) {
                neJobdetails.putAll(shmJobService.getNeJobDetails(batchedNeJobIds));
                activityJobs.addAll(getActivityJobPoAttributes(batchedNeJobIds));
            }
            if (activityJobs != null && (neJobdetails != null && !neJobdetails.isEmpty())) {
                for (final PersistenceObject activityJob : activityJobs) {
                    final Map<String, Object> poAttributes = activityJob.getAllAttributes();
                    final String nodeName = neJobdetails.get(activityJob.getAttribute(ShmConstants.NE_JOB_ID));
                    final String platform = neNamePlatformTypeMap.get(nodeName);
                    if (poAttributes.get(ShmConstants.ACTIVITY_NE_STATUS).equals(ShmConstants.COMPLETED)) {
                        final String key = platform + JobReportConstants.AT_THE_RATE + poAttributes.get(ShmConstants.ACTIVITY_NAME);
                        final String value = nodeName + JobReportConstants.AT_THE_RATE + poAttributes.get(ShmConstants.ACTIVITY_RESULT) + JobReportConstants.AT_THE_RATE
                                + DateTimeUtils.format((Date) poAttributes.get(ShmConstants.ACTIVITY_START_DATE)) + JobReportConstants.AT_THE_RATE
                                + DateTimeUtils.format((Date) poAttributes.get(ShmConstants.ACTIVITY_END_DATE)) + JobReportConstants.AT_THE_RATE
                                + getStepDurations((String) poAttributes.get(ShmConstants.STEP_DURATIONS));
                        if (!activityDetails.containsKey(key)) {
                            activityDetails.put(key, new ArrayList<String>());
                        }
                        activityDetails.get(key).add(value);
                    }
                }
            }
        } catch (final RuntimeException ex) {
            LOGGER.error("Exception while retrieving Activity job POs. Reason:{} ", ex);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
        }
        return activityDetails;
    }

    private String trimLong(final double duration) {
        return new DecimalFormat("#0.00").format(duration);
    }

    private List<PersistenceObject> getActivityJobPoAttributes(final List<Long> eachBatchOfNeJobIds) {
        List<PersistenceObject> activityJobs = new ArrayList<PersistenceObject>();
        try {
            final QueryExecutor queryExecutor = dataPersistenceService.getLiveBucket().getQueryExecutor();
            final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
            final Query<TypeRestrictionBuilder> typeQuery = queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.ACTIVITYJOB_TYPE);
            final Restriction restriction = typeQuery.getRestrictionBuilder().in(ShmConstants.NE_JOB_ID, eachBatchOfNeJobIds.toArray());
            typeQuery.setRestriction(restriction);
            activityJobs = queryExecutor.getResultList(typeQuery);
        } catch (final EJBException ejbException) {
            LOGGER.error("Exception occurred while querying Activity Jobs. Reason: ", ejbException);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ejbException);
            throw ejbException;
        }
        return activityJobs;
    }

    private String getStepDurations(final String stepDurationWithBraces) {
        final Map<String, String> stepDurationsMap = new HashMap<String, String>();
        LOGGER.debug("Step duration received: {}", stepDurationWithBraces);
        if (stepDurationWithBraces != null) {
            final String stepDuration = stepDurationWithBraces.substring(1, (stepDurationWithBraces.length() - 1));
            final String[] stepDurations = stepDuration.split(ActivityConstants.COMMA);
            for (final String step : stepDurations) {
                final String[] eachStep = step.split(ActivityConstants.EQUAL);
                stepDurationsMap.put(eachStep[0].trim(), eachStep[1].trim());
            }

            final double precheckDuration = (stepDurationsMap.get(ActivityStepsEnum.PRECHECK.getStep())) != null ? Double.parseDouble(stepDurationsMap.get(ActivityStepsEnum.PRECHECK.getStep())) : 0;
            final double executeDuration = (stepDurationsMap.get(ActivityStepsEnum.EXECUTE.getStep())) != null ? (Double.parseDouble(stepDurationsMap.get(ActivityStepsEnum.EXECUTE.getStep())) - precheckDuration)
                    : 0;
            final double processNotificationDuration = (stepDurationsMap.get(ActivityStepsEnum.PROCESS_NOTIFICATION.getStep())) != null ? (Double.parseDouble(stepDurationsMap
                    .get(ActivityStepsEnum.PROCESS_NOTIFICATION.getStep())) - executeDuration) : 0;
            final double handleTimeoutDuration = (stepDurationsMap.get(ActivityStepsEnum.HANDLE_TIMEOUT.getStep())) != null ? (Double.parseDouble(stepDurationsMap.get(ActivityStepsEnum.HANDLE_TIMEOUT
                    .getStep())) - executeDuration) : 0;

            final StringBuilder stepDurationValues = new StringBuilder();
            stepDurationValues.append(precheckDuration != 0 ? trimLong(precheckDuration) + JobReportConstants.AT_THE_RATE : JobReportConstants.AT_THE_RATE);
            stepDurationValues.append(executeDuration != 0 ? trimLong(executeDuration) + JobReportConstants.AT_THE_RATE : JobReportConstants.AT_THE_RATE);
            if (handleTimeoutDuration != 0 && processNotificationDuration != 0) {
                stepDurationValues.append(JobReportConstants.AT_THE_RATE + trimLong(handleTimeoutDuration));
            } else if (processNotificationDuration != 0 && handleTimeoutDuration == 0) {
                stepDurationValues.append(trimLong(processNotificationDuration));
            } else if (processNotificationDuration == 0 && handleTimeoutDuration != 0) {
                stepDurationValues.append(JobReportConstants.AT_THE_RATE + trimLong(handleTimeoutDuration));
            } else {
                stepDurationValues.append(JobReportConstants.AT_THE_RATE);
            }
            return stepDurationValues.toString();
        }
        return JobReportConstants.AT_THE_RATE + JobReportConstants.AT_THE_RATE;
    }

    private Map<String, StringBuilder> getMainJobDetailsForJobReport(final Long mainJobId, final Map<String, String> neNamePlatformTypeMap) {
        final StringBuilder jobReportCsv = new StringBuilder();
        final DataBucket liveBucket = getLiveBucket();
        final Map<String, StringBuilder> jobNameAndCsv = new HashMap<String, StringBuilder>();
        LOGGER.debug("Main job id to fetch step durations: {} ", mainJobId);

        final PersistenceObject mainJobPO = findPOByPoId(liveBucket, mainJobId);
        final String jobState = mainJobPO.getAttribute(ShmConstants.NE_STATUS);
        if (JobState.COMPLETED.toString().equals(jobState)) {
            final Date startTime = mainJobPO.getAttribute(ShmConstants.STARTTIME);
            final Date endTime = mainJobPO.getAttribute(ShmConstants.ENDTIME);
            final Long mainJobDuration = (endTime.getTime() - startTime.getTime()) / 1000;
            final PersistenceObject jobTemplatePo = findPOByPoId(liveBucket, (Long) mainJobPO.getAttribute(ShmConstants.JOBTEMPLATEID));
            final String jobName = jobTemplatePo.getAttribute(ShmConstants.NAME);
            final String owner = (String) jobTemplatePo.getAttribute(OWNER);
            final Map<String, Object> jobConfiguration = (Map<String, Object>) mainJobPO.getAttribute(ShmConstants.JOBCONFIGURATIONDETAILS);
            final List<String> neNames = getNodeNamesByMainJobId(jobConfiguration, owner);
            final List<NetworkElement> networkElementList = fdnServiceBean.getNetworkElementsByNeNames(neNames);
            for (final NetworkElement networkElement : networkElementList) {
                if (networkElement.getPlatformType() != null) {
                    neNamePlatformTypeMap.put(networkElement.getName(), networkElement.getPlatformType().toString());
                } else {
                    LOGGER.debug("Setting platform type {} using neType. ", platformTypeProvider.getPlatformType(networkElement.getNeType()).toString());
                    neNamePlatformTypeMap.put(networkElement.getName(), platformTypeProvider.getPlatformType(networkElement.getNeType()).toString());
                }
            }
            jobReportCsv.append(JobReportConstants.HEADER_NOTE_DURATIONS_IN_SECONDS);
            jobReportCsv.append(JobReportConstants.NEW_LINE + JobReportConstants.HEADER_NOTE_NEGETIVE_DURATION);
            jobReportCsv.append(JobReportConstants.NEW_LINE + JobReportConstants.JOB_ID + mainJobId);
            jobReportCsv.append(JobReportConstants.NEW_LINE + JobReportConstants.JOB_NAME + jobName);
            jobReportCsv.append(JobReportConstants.NEW_LINE + JobReportConstants.JOB_START_TIME + DateTimeUtils.format(startTime));
            jobReportCsv.append(JobReportConstants.NEW_LINE + JobReportConstants.JOB_END_TIME + DateTimeUtils.format(endTime));
            jobReportCsv.append(JobReportConstants.NEW_LINE + JobReportConstants.JOB_DURATION + mainJobDuration);
            jobReportCsv.append(JobReportConstants.CARRIAGE_RETURN + JobReportConstants.NEW_LINE);
            jobNameAndCsv.put(JobReportConstants.JOB_NAME, new StringBuilder(jobName));
            jobNameAndCsv.put(JobReportConstants.JOB_REPORT_TXT, jobReportCsv);
            return jobNameAndCsv;
        } else {
            LOGGER.debug("Job is not in Completed state. So, job report cannot be generated");
            jobNameAndCsv.put(JobReportConstants.JOB_REPORT_TXT, new StringBuilder(JobReportConstants.JOB_NOT_COMPLETED_MESSAGE));
            return jobNameAndCsv;
        }
    }

    private PersistenceObject findPOByPoId(final DataBucket liveBucket, final long poId) {
        return liveBucket.findPoById(poId);
    }

    private static String getHeader() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("%s" + JobReportConstants.AT_THE_RATE, JobReportConstants.NODE_NAME));
        stringBuilder.append(String.format("%s" + JobReportConstants.AT_THE_RATE, JobReportConstants.ACTIVITY_RESULT));
        stringBuilder.append(String.format("%s" + JobReportConstants.AT_THE_RATE, JobReportConstants.ACTIVITY_START_TIME));
        stringBuilder.append(String.format("%s" + JobReportConstants.AT_THE_RATE, JobReportConstants.ACTIVITY_END_TIME));
        stringBuilder.append(String.format("%s" + JobReportConstants.AT_THE_RATE, JobReportConstants.PRECHECK_DURATION));
        stringBuilder.append(String.format("%s" + JobReportConstants.AT_THE_RATE, JobReportConstants.EXECUTE_DURATION));
        stringBuilder.append(String.format("%s" + JobReportConstants.AT_THE_RATE, JobReportConstants.PROCESS_NOTIFICATION_DURATION));
        stringBuilder.append(JobReportConstants.HANDLE_NOTIFICATION_DURATION);
        return stringBuilder.toString();
    }

    private StringBuilder createJobReportLogText(final List<Long> neJobIdList, final StringBuilder jobReportCsv, final Map<String, String> neNamePlatformTypeMap) {
        final String JOB_REPORT_HEADER = getHeader();
        final Map<String, List<String>> activityDetails = getActivityJobsForNeJob(neJobIdList, neNamePlatformTypeMap);
        final Map<String, StringBuilder> jobReport = new HashMap<String, StringBuilder>();

        for (final Map.Entry<String, List<String>> platformActivities : activityDetails.entrySet()) {
            final String[] platformAndActivity = platformActivities.getKey().split(JobReportConstants.AT_THE_RATE);
            final String platform = platformAndActivity[0];
            final String activityName = platformAndActivity[1];

            if (!jobReport.containsKey(platform)) {
                jobReport.put(platform, new StringBuilder().append(JobReportConstants.NEW_LINE + JobReportConstants.PLATFORM_TYPE + platform));

            }
            jobReport.get(platform).append(JobReportConstants.NEW_LINE + JobReportConstants.ACTIVITY_NAME + activityName);
            jobReport.get(platform).append(JobReportConstants.NEW_LINE + JOB_REPORT_HEADER);
            for (final String val : platformActivities.getValue()) {
                jobReport.get(platform).append(JobReportConstants.NEW_LINE + val);
            }
            jobReport.get(platform).append(JobReportConstants.NEW_LINE);
        }

        for (final Map.Entry<String, StringBuilder> jobReportString : jobReport.entrySet()) {
            jobReportCsv.append(jobReportString.getValue());
        }
        return jobReportCsv;
    }

    private DataBucket getLiveBucket() {
        return dataPersistenceService.getLiveBucket();
    }

    private void writeToFileSystemUsingOutputStream(final String content, final String reportFileName, final String zipFileName) {
        final String smrsHomeDirectory = smrsServiceUtil.getSmrsPath(null, SmrsServiceConstants.JOBREPORTING_ACCOUNT, smrsRetryPolicies.getSmrsImportRetryPolicy());
        FileResourceImpl fileResource = new FileResourceImpl(smrsHomeDirectory);
        final String zipFileAbsolutePathURI = smrsHomeDirectory + File.separator + zipFileName;
        LOGGER.debug("zipFileAbsolutePathURI: {} ", zipFileAbsolutePathURI);
        OutputStream outStream = null;
        ZipOutputStream zipOutputStream = null;
        ZipEntry zipEntry = null;
        try {
            if (!fileResource.isDirectoryExists()) {
                LOGGER.info("Directory doesn't exist, creating it ... {}", smrsHomeDirectory);
                fileResource.createDirectory();
            }
            fileResource = new FileResourceImpl(zipFileAbsolutePathURI);
            LOGGER.trace("Creating a file ... {}", zipFileAbsolutePathURI);
            fileResource.createFile();
            if (fileResource.exists() && fileResource.supportsWriteOperations()) {
                outStream = fileResource.getOutputStream();
                zipOutputStream = new ZipOutputStream(new BufferedOutputStream(outStream));
                zipEntry = new ZipEntry(reportFileName);
                zipOutputStream.putNextEntry(zipEntry);
                final byte[] data = content.getBytes();
                zipOutputStream.write(data, 0, data.length);
                zipOutputStream.closeEntry();
                LOGGER.debug("{} data saved to location {} successfully.", reportFileName, zipFileAbsolutePathURI);
            }
        } catch (final ZipException zipEx) {
            LOGGER.error("ZipException occurred while writing job report data to file : {}", zipEx);
        } catch (final IOException ioe) {
            LOGGER.error("IOException occurred while writing job report data to file : {}", ioe);
        } catch (final Exception ex) {
            LOGGER.error("Exception while forming zip for job report : {} ", ex);
        } finally {
            try {
                if (zipOutputStream != null) {
                    zipOutputStream.close();
                }
                if (outStream != null) {
                    outStream.close();
                }
                Resources.safeClose(outStream);
            } catch (final IOException ioEx) {
                LOGGER.error("IOException occurred while closing streams for job report : {}", ioEx);
            } catch (final Exception ex) {
                LOGGER.error("Exception occurred while closing streams for job report : {} ", ex);
            }
        }
    }

    private Long getTemplateJobId(final String jobName) {
        final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final Query<TypeRestrictionBuilder> jobTypeQuery = setRestrictionToQueryTemplateJob(jobName, queryBuilder);
        final QueryExecutor queryExecutor = dataPersistenceService.getLiveBucket().getQueryExecutor();
        final Iterator<PersistenceObject> templateJobIterator = queryExecutor.execute(jobTypeQuery);
        Long templateJobId = 0L;
        while (templateJobIterator.hasNext()) {
            final PersistenceObject persistenceObject = templateJobIterator.next();
            templateJobId = persistenceObject.getPoId();
        }
        return templateJobId;
    }

    private List<String> getNodeNamesByMainJobId(final Map<String, Object> jobConfiguration, final String owner) {
        final Map<String, Object> neInfo = (Map<String, Object>) jobConfiguration.get(ShmConstants.SELECTED_NES);
        final List<String> savedSearchIds = (List<String>) neInfo.get(ShmConstants.SAVED_SEARCH_IDS);
        final List<String> collectionNames = (List<String>) neInfo.get(ShmConstants.COLLECTION_NAMES);
        final List<String> neNames = (List<String>) neInfo.get(ShmConstants.NENAMES);
        final List<String> allNeNames = new ArrayList<String>();
        if (collectionNames != null && !collectionNames.isEmpty()) {
            for (final String collectionName : collectionNames) {
                final Set<String> nodeNames = topologyEvaluationService.getCollectionInfo(owner, collectionName);
                if (nodeNames != null && !nodeNames.isEmpty()) {
                    LOGGER.debug("collectionName: {} and number of nodeNames in a savedSearch are : {} ", collectionName, nodeNames.size());
                    for (final String nodeFdn : nodeNames) {
                        allNeNames.add(FdnUtils.getNodeName(nodeFdn));
                    }
                }
            }
        }
        if (savedSearchIds != null && !savedSearchIds.isEmpty()) {
            for (final String savedSearchId : savedSearchIds) {
                final Set<String> nodeNames = topologyEvaluationService.getSavedSearchInfo(owner, savedSearchId);
                if (nodeNames != null && !nodeNames.isEmpty()) {
                    LOGGER.debug("savedSearchId: {} and number of nodeNames in a savedSearch are : {} ", savedSearchId, nodeNames.size());
                    for (final String nodeFdn : nodeNames) {
                        allNeNames.add(FdnUtils.getNodeName(nodeFdn));
                    }
                }
            }
        }
        if (neNames != null && !neNames.isEmpty()) {
            allNeNames.addAll(neNames);
        }
        return allNeNames;
    }

    private void updateMainJobStepDurationDetails(final long mainJobId, final Map<String, Object> mainJobAttributes) {
        final PersistenceObject persistenceObject = dataPersistenceService.getLiveBucket().findPoById(mainJobId);

        persistenceObject.setAttributes(mainJobAttributes);
        LOGGER.debug("Updated main job step duration metrics :{} for main job id: {}", mainJobId, mainJobAttributes);
    }

}
