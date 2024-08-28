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
package com.ericsson.oss.services.shm.job.cache;

import static com.ericsson.oss.services.shm.jobs.common.constants.JobConfigurationConstants.JOB_CONFIGURATIONDETAILS;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobMapper;
import com.ericsson.oss.services.shm.jobs.common.modelentities.Activity;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;

/**
 * Provides a method to get NE Job Static Data from elementary services.
 * 
 * @author tcssbop
 * 
 */
public class JobStaticDataProviderImpl implements JobStaticDataProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobStaticDataProviderImpl.class);

    @Inject
    private JobStaticDataCache jobStaticDataCache;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationService;

    @Inject
    private JobMapper jobMapper;

    @Override
    public JobStaticData getJobStaticData(final long mainJobId) throws JobDataNotFoundException {

        JobStaticData jobStaticData = jobStaticDataCache.get(mainJobId);

        if (jobStaticData == null) {
            //Fetch from DPS, if data not exists in cache. 
            jobStaticData = getJobInfoFromDPS(mainJobId);
            if (jobStaticData != null) {
                jobStaticDataCache.put(mainJobId, jobStaticData);
            } else {
                LOGGER.error("Failed to get Job static data either from cache or from DPS for mainJobId: {} ", mainJobId);
                throw new JobDataNotFoundException("Database service is not accessible");
            }
        }
        return jobStaticData;
    }

    private JobStaticData getJobInfoFromDPS(final long mainJobId) {

        LOGGER.info("Entered into getJobInfoFromDPS method to get data from DPS: {}", mainJobId);
        try {
            final Map<String, Object> mainJobAttributes = jobConfigurationService.getMainJobAttributes(mainJobId);

            if (validateJobPO(mainJobAttributes)) {
                return null;
            }
            final long templateJobId = (long) mainJobAttributes.get(ShmConstants.JOB_TEMPLATE_ID);
            if (templateJobId != 0) {
                return prepareJobAttributes(mainJobId, mainJobAttributes, templateJobId);
            }

        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while getting Job static data for the mainJobId: {}. Exception is:", mainJobId, ex);
        }
        return null;
    }

    /**
     * @param mainJobId
     * @param jobStaticData
     * @param mainJobAttributes
     * @param templateJobId
     * @return
     */
    @SuppressWarnings("unchecked")
    private JobStaticData prepareJobAttributes(final long mainJobId, final Map<String, Object> mainJobAttributes, final long templateJobId) {
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(JOB_CONFIGURATIONDETAILS);
        final JobConfiguration jobConfiguration = jobMapper.getJobConfigurationDetails(jobConfigurationDetails);

        final Map<Object, Object> restrictions = getRestrictions(templateJobId);
        final List<Map<String, Object>> jobDetails = jobConfigurationService.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE, restrictions,
                Arrays.asList(ShmConstants.OWNER, ShmConstants.JOB_TYPE));

        if (!validateJobDetails(jobDetails)) {
            return prepareJobStaticData(mainJobId, templateJobId, jobConfiguration, jobDetails);
        } else {
            LOGGER.info("Failed to get Job PO for NE JobId{}", mainJobId);
        }
        return null;
    }

    /**
     * @param mainJobId
     * @param jobStaticData
     * @param templateJobId
     * @param jobConfiguration
     * @param jobDetails
     * @return
     */
    private JobStaticData prepareJobStaticData(final long mainJobId, final long templateJobId, final JobConfiguration jobConfiguration, final List<Map<String, Object>> jobDetails) {
        final String executionMode = jobConfiguration.getMainSchedule().getExecMode().getMode();
        final String owner = (String) jobDetails.get(0).get(ShmConstants.OWNER);
        final String jobType = (String) jobDetails.get(0).get(ShmConstants.JOB_TYPE);
        final Map<String, Object> activitySchedules = prepareActivitySchedules(mainJobId, jobConfiguration);
        String jobExecutionUser = getShmJobExecUser(mainJobId, executionMode, owner);
        if (validateJobAttrs(templateJobId, owner, executionMode, jobType)) {
            final JobStaticData jobStaticData = new JobStaticData(owner, activitySchedules, executionMode, JobType.getJobType(jobType), jobExecutionUser);
            LOGGER.debug("Job Static Data owner : {}, schedules : {}", jobStaticData.getOwner(), jobStaticData.getActivitySchedules());
            return jobStaticData;
        } else {
            LOGGER.error("Failed to get Job static data for the mainJobId: {}. templateJobId:{},  owner:{} ", mainJobId, templateJobId, owner);
        }
        return null;
    }

    /**
     * @param mainJobId
     * @param jobConfiguration
     * @return
     */
    private static Map<String, Object> prepareActivitySchedules(final long mainJobId, final JobConfiguration jobConfiguration) {
        final List<Activity> activitiesList = jobConfiguration.getActivities();
        final Map<String, Object> activitySchedules = new HashMap<String, Object>();
        for (Activity activity : activitiesList) {
            final String key = mainJobId + "_" + activity.getNeType() + "_" + activity.getName().toLowerCase();
            if (!activitySchedules.containsKey(key)) {
                activitySchedules.put(key, activity.getSchedule().getExecMode().getMode());
            }
        }
        return activitySchedules;
    }

    public String getShmJobExecUser(final long mainJobId, final String execMode, final String owner) {
        String shmJobExecUser = null;
        if (ExecMode.MANUAL.getMode().equals(execMode)) {
            final Map<Object, Object> restrictions = new HashMap<>();
            restrictions.put(ObjectField.PO_ID, mainJobId);
            final List<Map<String, Object>> mainJobDetails = jobConfigurationService.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.JOB, restrictions,
                    Arrays.asList(ShmConstants.SHM_JOB_EXEC_USER));
            shmJobExecUser = (String) mainJobDetails.get(0).get(ShmConstants.SHM_JOB_EXEC_USER);
            if (shmJobExecUser == null || shmJobExecUser.isEmpty()) {
                shmJobExecUser = owner;
            }
        } else {
            shmJobExecUser = owner;
        }
        LOGGER.trace("Job Executed User {}", shmJobExecUser);
        return shmJobExecUser;
    }

    private static Map<Object, Object> getRestrictions(final long poId) {
        final Map<Object, Object> restrictions = new HashMap<Object, Object>();
        restrictions.put(ObjectField.PO_ID, poId);
        return restrictions;
    }

    private static boolean validateJobPO(final Map<String, Object> mainJobAttributes) {
        return mainJobAttributes == null || mainJobAttributes.isEmpty();
    }

    private static boolean validateJobDetails(final List<Map<String, Object>> poIds) {
        return poIds == null || poIds.isEmpty();
    }

    private static boolean validateJobAttrs(final long templateJobId, final String owner, final String executionMode, final String jobType) {
        return (templateJobId != 0 && owner != null && executionMode != null && jobType != null);
    }

    @Override
    public void clear(final long mainJobId) {
        jobStaticDataCache.clear(mainJobId);
    }

    @Override
    public void clearAll() {
        jobStaticDataCache.clearAll();
    }

    @Override
    public void put(final long mainJobId, final JobStaticData jobStaticData) {
        jobStaticDataCache.put(mainJobId, jobStaticData);
    }
}
