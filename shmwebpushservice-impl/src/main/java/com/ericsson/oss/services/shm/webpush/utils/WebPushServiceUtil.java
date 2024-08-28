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
package com.ericsson.oss.services.shm.webpush.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobCategory;
import com.ericsson.oss.services.shm.webpush.api.JobWebPushEvent;
import com.ericsson.oss.services.shm.webpush.api.WebPushConstants;

/**
 * This utility class is used for preparing of Json object of Job, NEJob and ActivityJob which is required to send to the client.
 * 
 * @author xcharoh
 */
public class WebPushServiceUtil {

    private static final Logger logger = LoggerFactory.getLogger(WebPushServiceUtil.class);

    @Inject
    private JobConfigurationService jobConfigurationService;
    
    @Inject
    private Event<JobWebPushEvent> eventSender;

    @Inject
    private JobUpdateService jobUpdateService;

    /**
     * This method is used for building object for Job PO which will be sent to the client.
     */
    public void prepareAndPushMainJob(final long mainJobId) {

        //Build Object - Required in second phase implementation 
        final String mainJobIdAsString = Long.toString(mainJobId);

        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        mainJobAttributes.put(WebPushConstants.JOB_ID, mainJobIdAsString);

        final String jobCategory = jobConfigurationService.getJobCategory(mainJobId);
        final Map<String, Object> jobAttributes = jobUpdateService.retrieveJobWithRetry(mainJobId);
        final String jobState = (String) jobAttributes.get(ShmConstants.STATE);
        logger.info("Building Object for Main Job with ID {} Category {} Job State {}", mainJobId, jobCategory, jobState);
        if (!ShmConstants.DELETING.equals(jobState) && !jobCategory.isEmpty()) {

            if (JobCategory.getNhcJobCategories().contains(jobCategory)) {
                fireWebPushEvent(mainJobIdAsString, mainJobAttributes, WebPushConstants.NHC_MAIN_JOBS_APPLICATION);
                fireWebPushEvent(mainJobIdAsString, mainJobAttributes, WebPushConstants.NHC_JOB_DETAILS_APPLICATION);
            } else if (JobCategory.getShmJobCategories().contains(jobCategory)) {
                fireWebPushEvent(mainJobIdAsString, mainJobAttributes, WebPushConstants.MAIN_JOBS_APPLICATION);
                fireWebPushEvent(mainJobIdAsString, mainJobAttributes, WebPushConstants.JOB_DETAILS_APPLICATION);
            }
        }

    }

    /**
     * This method is used for building object for NEJob PO which will be sent to the client.
     */
    public void prepareAndPushNeJob(final long neJobId) {
        //Build Object - Required in second phase implementation 
        logger.info("Building Object  for NE Job with ID {}", neJobId);
        final String neJobIdAsString = Long.toString(neJobId);

        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        neJobAttributes.put(WebPushConstants.JOB_ID, neJobIdAsString);

        final long mainJobId = getMainJobId(neJobId);

        final String jobCategory = jobConfigurationService.getJobCategory(mainJobId);
        if (JobCategory.getNhcJobCategories().contains(jobCategory)) {
            fireWebPushEvent(neJobIdAsString, neJobAttributes, WebPushConstants.NHC_JOB_DETAILS_APPLICATION);
        } else if (JobCategory.getShmJobCategories().contains(jobCategory)) {
            fireWebPushEvent(neJobIdAsString, neJobAttributes, WebPushConstants.JOB_DETAILS_APPLICATION);
        }
    }

    /**
     * This method is used for building object for ActivityJob PO which will be sent to the client.
     */
    public void prepareAndPushActivityJob(final long activityJobId, final Set<AttributeChangeData> attributeChangeDataSet) {
        //Build Object - Required in second phase implementation 
        logger.debug("Building Object for Activity Job with ID {}", activityJobId);
        final String activityJobIdAsString = Long.toString(activityJobId);
        boolean isJobLogNotification = false;

        for (final AttributeChangeData attributeChangeData : attributeChangeDataSet) {
            logger.debug("attributeChangeData.getName() {} ", attributeChangeData.getName());
            if (ShmConstants.LOG.equalsIgnoreCase(attributeChangeData.getName())) {
                isJobLogNotification = true;
                break;
            }
        }
        final Map<String, Object> activityJobAttributes = jobUpdateService.retrieveJobWithRetry(activityJobId);
        final long neJobId = (long) activityJobAttributes.get(ShmConstants.NE_JOB_ID);
        final long mainJobId = getMainJobId(neJobId);
        final String jobCategory = jobConfigurationService.getJobCategory(mainJobId);

        logger.debug("Acivity job log notification received - {} ", isJobLogNotification);
        if (isJobLogNotification) {
            pushToJobLogsApp(neJobId, activityJobAttributes, activityJobId, jobCategory);
        } else {
            final Map<String, Object> activityJobAttribute = new HashMap<String, Object>();
            activityJobAttribute.put(WebPushConstants.JOB_ID, activityJobIdAsString);
            logger.debug("Pushing to JobDetails Page for Activity Job with ID {}", activityJobId);
            if (JobCategory.getNhcJobCategories().contains(jobCategory)) {
                fireWebPushEvent(activityJobIdAsString, activityJobAttribute, WebPushConstants.NHC_JOB_DETAILS_APPLICATION);
            } else if (JobCategory.getShmJobCategories().contains(jobCategory)) {
                logger.debug("Is Activity job update having id {} pushed to shmJobDetails page", activityJobId);
                fireWebPushEvent(activityJobIdAsString, activityJobAttribute, WebPushConstants.JOB_DETAILS_APPLICATION);

            }
        }
    }

    private void pushToJobLogsApp(final long neJobId, final Map<String, Object> activityJobAttributes, final long activityJobId, final String jobCategory) {
        final String neJobIdAsString = Long.toString(neJobId);
        final String activityName = (String) activityJobAttributes.get(ShmConstants.ACTIVITY_NAME);
        logger.debug("JobLog : activityName {} ", activityName);
        final Map<String, Object> neJobAttributes = jobUpdateService.retrieveJobWithRetry(neJobId);
        final String neName = (String) neJobAttributes.get(ShmConstants.NE_NAME);
        logger.debug("Ne Name for neJobId {} is {} ", neName, neJobIdAsString);

        final Map<String, Object> neJobAttributesMap = new HashMap<String, Object>();
        neJobAttributesMap.put(WebPushConstants.JOB_ID, neJobIdAsString);
        neJobAttributesMap.put(ShmConstants.NE_NAME, neName);
        if (!jobCategory.isEmpty()) {
            if (JobCategory.getNhcJobCategories().contains(jobCategory)) {
                fireWebPushEvent(neJobIdAsString, neJobAttributesMap, WebPushConstants.NHC_JOB_LOGS_APPLICATION);
            } else if (JobCategory.getShmJobCategories().contains(jobCategory)) {
                logger.debug("Pushing to ShmJobLogs Page for neJobId {} and activityJobId {}", neJobId, activityJobId);
                fireWebPushEvent(neJobIdAsString, neJobAttributesMap, WebPushConstants.JOB_LOGS_APPLICATION);
            }
        }
    }

    public void prepareAndPushCreateJobEvent(final long jobId) {
        final String createJobIdAsString = Long.toString(jobId);
        final Map<String, Object> craeteJobAttributes = new HashMap<String, Object>();
        craeteJobAttributes.put(WebPushConstants.JOB_ID, createJobIdAsString);
        craeteJobAttributes.put(WebPushConstants.JOB_EVENT, WebPushConstants.CREATE_JOB);

        final String jobCategory = jobConfigurationService.getJobCategory(jobId);
        if (JobCategory.getNhcJobCategories().contains(jobCategory)) {
            fireWebPushEvent(null, craeteJobAttributes, WebPushConstants.NHC_JOBS_APPLICATION);
        } else if (JobCategory.getShmJobCategories().contains(jobCategory)) {
            fireWebPushEvent(null, craeteJobAttributes, WebPushConstants.SHM_JOBS_APPLICATION);
        }
    }

    public void prepareAndPushCreateNeJobEvent(final long neJobId) {
        final String createNeJobIdAsString = Long.toString(neJobId);
        final Map<String, Object> craeteJobAttributes = new HashMap<String, Object>();
        craeteJobAttributes.put(ShmConstants.NE_JOB_ID, createNeJobIdAsString);
        craeteJobAttributes.put(WebPushConstants.JOB_EVENT, WebPushConstants.CREATE_JOB);

        final long mainJobId = getMainJobId(neJobId);

        final String jobCategory = jobConfigurationService.getJobCategory(mainJobId);
        if (JobCategory.getNhcJobCategories().contains(jobCategory)) {
            fireWebPushEvent(null, craeteJobAttributes, WebPushConstants.NHC_JOBS_APPLICATION);
        } else if (JobCategory.getShmJobCategories().contains(jobCategory)) {
            fireWebPushEvent(null, craeteJobAttributes, WebPushConstants.SHM_JOBS_APPLICATION);
        }
    }

    private long getMainJobId(final long neJobId) {
        final Map<String, Object> neJobAttributes = jobUpdateService.retrieveJobWithRetry(neJobId);
        return (long) neJobAttributes.get(ShmConstants.MAIN_JOB_ID);

    }

    private void fireWebPushEvent(final String subscriptionId, final Map<String, Object> jobAttributes, final String applicationType) {
        logger.debug("Pushing to {} Page with job attributes {}", applicationType, jobAttributes);
        final JobWebPushEvent jobWebPushEvent = buildJobWebPushEvent(subscriptionId, jobAttributes, applicationType);
        eventSender.fire(jobWebPushEvent);
    }

    /**
     * This method builds the job web push event based on application type.
     * 
     * @param subscriptionId
     * @param jobAttributes
     * @param applicationType
     * @return JobWebPushEvent
     */
    private JobWebPushEvent buildJobWebPushEvent(final String subscriptionId, final Map<String, Object> jobAttributes, final String applicationType) {
        final JobWebPushEvent jobWebPushEvent = new JobWebPushEvent();
        jobWebPushEvent.setAttributeMap(jobAttributes);
        jobWebPushEvent.setJobId(subscriptionId);
        jobWebPushEvent.setApplicationType(applicationType);
        return jobWebPushEvent;
    }
}
