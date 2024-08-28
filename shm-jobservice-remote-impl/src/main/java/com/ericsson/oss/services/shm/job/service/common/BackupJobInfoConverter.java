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
package com.ericsson.oss.services.shm.job.service.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.job.exceptions.NoMeFDNSProvidedException;
import com.ericsson.oss.services.shm.job.remote.api.BackupActivityEnum;
import com.ericsson.oss.services.shm.job.remote.api.ShmBackupJobData;
import com.ericsson.oss.services.shm.job.remote.api.ShmRemoteJobData;
import com.ericsson.oss.services.shm.job.remote.api.errorcodes.JobCreationResponseCode;
import com.ericsson.oss.services.shm.job.remote.impl.JobInfoConverterImpl;
import com.ericsson.oss.services.shm.jobs.common.annotations.JobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.jobservice.common.JobInfo;
import com.ericsson.oss.services.topologyCollectionsService.exception.service.TopologyCollectionsServiceException;

@JobTypeAnnotation(jobType = JobType.BACKUP)
public class BackupJobInfoConverter extends JobInfoConverterImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupJobInfoConverter.class);

    @Inject
    BackupActivitySchedulesUtil backupActivitySchedulesUtil;

    private static final int MAX_CV_NAME_OR_COMMENT_LENGTH = 40;

    public JobInfo prepareJobInfoData(final ShmRemoteJobData shmRemoteJobData) throws TopologyCollectionsServiceException, NoMeFDNSProvidedException {
        final JobInfo jobInfo = new JobInfo();
        final ShmBackupJobData shmBackupJobData = (ShmBackupJobData) shmRemoteJobData;
        setNeNamesFromSearchScope(shmRemoteJobData);

        final Map<PlatformTypeEnum, List<String>> supportedPlatformTypeAndNodeTypes = platformAndNeTypeFinder
                .findSupportedPlatformAndNodeTypes(SHMCapabilities.BACKUP_JOB_CAPABILITY, shmRemoteJobData);
        jobInfo.setJobType(JobTypeEnum.BACKUP);
        jobInfo.setConfigurations(getConfigurations(supportedPlatformTypeAndNodeTypes, shmBackupJobData));
        jobInfo.setActivitySchedules(getActivitySchedules(supportedPlatformTypeAndNodeTypes, shmBackupJobData.getActivities()));
        setCommonAttributes(shmBackupJobData, jobInfo);

        LOGGER.debug("JobInfo:" + jobInfo.getName() + " User name:" + jobInfo.getOwner() + "Job Info Configurations:" + jobInfo.getConfigurations() + " Activity schedules:"
                + jobInfo.getActivitySchedules() + "Main schedule:" + jobInfo.getMainSchedule() + "Collection Ids:" + jobInfo.getcollectionNames() + "Saved SearchIDs:" + jobInfo.getSavedSearchIds()
                + "NE Names:" + jobInfo.getNeNames());

        return jobInfo;
    }

    /**
     * Fetch and set activity params & NE params attributes for the corresponding jobtype with selected node types
     * 
     * @param supportedPlatformTypeAndNodeTypes
     * @param shmBackupJobData
     * @return
     */
    private List<Map<String, Object>> getConfigurations(final Map<PlatformTypeEnum, List<String>> supportedPlatformTypeAndNodeTypes, final ShmBackupJobData shmBackupJobData) {

        final List<Map<String, Object>> configurations = new ArrayList<Map<String, Object>>();
        for (final Entry<PlatformTypeEnum, List<String>> platformTypeAndNodeType : supportedPlatformTypeAndNodeTypes.entrySet()) {
            final Map<String, Object> platformType = new HashMap<String, Object>();
            platformType.put(ShmConstants.PLATFORM, platformTypeAndNodeType.getKey().name());
            for (final String nodeType : platformTypeAndNodeType.getValue()) {
                final Map<String, Object> neTypeDetails = new HashMap<String, Object>();
                neTypeDetails.put(ShmConstants.NETYPE, nodeType);
                neTypeDetails.put(ShmConstants.PROPERTIES, neTypePropertiesUtil.prepareNeTypeProperties(platformTypeAndNodeType.getKey(), nodeType, JobType.BACKUP.getJobTypeName(), shmBackupJobData));
                configurations.add(neTypeDetails);
            }
            configurations.add(platformType);
        }
        return configurations;
    }

    /**
     * Fetch and set activity schedule attributes for the corresponding jobtype with selected node types
     * 
     * @param supportedPlatformTypeAndNodeTypes
     * @param backupActivities
     * @return
     */

    private List<Map<String, Object>> getActivitySchedules(final Map<PlatformTypeEnum, List<String>> supportedPlatformTypeAndNodeTypes, final List<BackupActivityEnum> backupActivities) {
        final List<Map<String, Object>> activitySchedules = new ArrayList<Map<String, Object>>();
        for (final Entry<PlatformTypeEnum, List<String>> platformTypeAndNodeType : supportedPlatformTypeAndNodeTypes.entrySet()) {
            final Map<String, Object> platformTypeDetails = new HashMap<String, Object>();
            platformTypeDetails.put(ShmConstants.PLATFORMTYPE, platformTypeAndNodeType.getKey().name());
            final List<Map<String, Object>> neTypeDetails = new ArrayList<Map<String, Object>>();
            for (final String nodeType : platformTypeAndNodeType.getValue()) {
                final Map<String, Object> neTypeDetail = new HashMap<String, Object>();
                neTypeDetail.put(ShmConstants.NETYPE, nodeType);
                neTypeDetail.put(ShmConstants.VALUE,
                        backupActivitySchedulesUtil.prepareActivitySchedules(platformTypeAndNodeType.getKey(), nodeType, JobType.BACKUP.getJobTypeName(), backupActivities));
                neTypeDetails.add(neTypeDetail);
            }
            platformTypeDetails.put(ShmConstants.VALUE, neTypeDetails);
            activitySchedules.add(platformTypeDetails);
        }
        return activitySchedules;
    }

    @Override
    public JobCreationResponseCode isValidData(final ShmRemoteJobData shmJobData) {
        final ShmBackupJobData shmBackupJobData = (ShmBackupJobData) shmJobData;
        if (shmBackupJobData.getBackupComment() != null && isMaxLengthExceeded(shmBackupJobData.getBackupComment())) {
            return JobCreationResponseCode.BACKUPCOMMENT_NAME_NOT_ALLOWED;
        }
        return null;
    }

    private static boolean isMaxLengthExceeded(final String backupParameter) {
        return backupParameter.length() > MAX_CV_NAME_OR_COMMENT_LENGTH;
    }
}
