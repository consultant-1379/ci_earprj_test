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
package com.ericsson.oss.services.shm.job.service.vran;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.job.exceptions.NoMeFDNSProvidedException;
import com.ericsson.oss.services.shm.job.remote.api.ShmDeleteSwPkgJobData;
import com.ericsson.oss.services.shm.job.remote.api.ShmRemoteJobData;
import com.ericsson.oss.services.shm.job.remote.api.errorcodes.JobCreationResponseCode;
import com.ericsson.oss.services.shm.job.remote.impl.JobInfoConverterImpl;
import com.ericsson.oss.services.shm.jobs.common.annotations.JobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobCategory;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.jobs.vran.constants.VranConstants;
import com.ericsson.oss.services.shm.jobservice.common.JobInfo;
import com.ericsson.oss.services.topologyCollectionsService.exception.service.TopologyCollectionsServiceException;

@JobTypeAnnotation(jobType = JobType.DELETE_SOFTWAREPACKAGE)
public class DeleteSoftwarePackageJobInfoConverter extends JobInfoConverterImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteSoftwarePackageJobInfoConverter.class);

    /**
     * Converts the received input data of DeleteSoftwarePackageJob to {@link JobInfo}.
     *
     * @param shmRemoteJobData
     *            Input data of a job
     * @return JobInfo
     */
    public JobInfo prepareJobInfoData(final ShmRemoteJobData shmRemoteJobData) throws TopologyCollectionsServiceException, NoMeFDNSProvidedException {

        LOGGER.debug("Converting input job data in DeleteSoftwarePackageJobConverter: {}", shmRemoteJobData);
        final JobInfo jobInfo = new JobInfo();
        final ShmDeleteSwPkgJobData shmDeleteSwPkgJobData = (ShmDeleteSwPkgJobData) shmRemoteJobData;
        final String activityName = shmDeleteSwPkgJobData.getActivity();
        LOGGER.debug("ShmDeleteSwPkgJobData object contains {}", shmDeleteSwPkgJobData);
        jobInfo.setJobType(JobTypeEnum.DELETE_SOFTWAREPACKAGE);
        setCommonAttributes(shmDeleteSwPkgJobData, jobInfo);
        jobInfo.setJobCategory(JobCategory.UI);
        jobInfo.setName(shmDeleteSwPkgJobData.getJobName());
        jobInfo.setConfigurations(shmDeleteSwPkgJobData.getConfigurations());
        jobInfo.setActivitySchedules(getActivitySchedules(activityName));

        LOGGER.debug("Converted DeleteSoftwarePackage JobInfo: {}", jobInfo);
        return jobInfo;
    }

    /**
     * Fetch and set activity schedule attributes for the corresponding jobtype with selected node types
     * 
     * @param supportedPlatformTypeAndNodeTypes
     * @param backupActivities
     * @return
     */

    private static List<Map<String, Object>> getActivitySchedules(final String activityName) {
        final List<Map<String, Object>> activitySchedules = new ArrayList<>();
        final Map<String, Object> platformTypeDetails = new HashMap<>();
        platformTypeDetails.put(ShmConstants.PLATFORMTYPE, PlatformTypeEnum.vRAN.name());
        final List<Map<String, Object>> neTypeDetails = new ArrayList<>();
        final Map<String, Object> neTypeDetail = new HashMap<>();
        neTypeDetail.put(ShmConstants.NETYPE, VranConstants.NFVO);
        neTypeDetail.put(ShmConstants.VALUE, getActivitySchedule(activityName));
        neTypeDetails.add(neTypeDetail);
        LOGGER.debug("Activity Details list: {}", neTypeDetails);
        platformTypeDetails.put(ShmConstants.VALUE, neTypeDetails);
        activitySchedules.add(platformTypeDetails);
        LOGGER.debug("ActivitySchedules: {}", activitySchedules);
        return activitySchedules;
    }

    private static List<Map<String, Object>> getActivitySchedule(final String activityName) {
        LOGGER.debug("ActivityName is {}", activityName);
        final Map<String, Object> activityMap = new HashMap<>();
        final List<Map<String, Object>> activitySchedule = new ArrayList<>();
        activityMap.put(ShmConstants.ACTIVITYNAME, activityName);
        activityMap.put(ShmConstants.EXECUTION_MODE, ExecMode.IMMEDIATE.name());
        activityMap.put(ShmConstants.ORDER, VranConstants.ORDER_VALUE);
        activitySchedule.add(activityMap);
        LOGGER.debug("ActivitySchedule values are {} ", activitySchedule);
        return activitySchedule;
    }

    public JobCreationResponseCode isValidData(final ShmRemoteJobData shmJobData) {
        return null;
    }

}