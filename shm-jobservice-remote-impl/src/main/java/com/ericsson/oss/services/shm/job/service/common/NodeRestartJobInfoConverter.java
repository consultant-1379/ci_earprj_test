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
import com.ericsson.oss.services.shm.job.remote.api.ShmNodeRestartJobData;
import com.ericsson.oss.services.shm.job.remote.api.ShmRemoteJobData;
import com.ericsson.oss.services.shm.job.remote.api.errorcodes.JobCreationResponseCode;
import com.ericsson.oss.services.shm.job.remote.impl.JobInfoConverterImpl;
import com.ericsson.oss.services.shm.jobs.common.annotations.JobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.jobservice.common.JobInfo;
import com.ericsson.oss.services.shm.shared.enums.RestartRank;
import com.ericsson.oss.services.shm.shared.enums.RestartReason;
import com.ericsson.oss.services.topologyCollectionsService.exception.service.TopologyCollectionsServiceException;

@JobTypeAnnotation(jobType = JobType.NODERESTART)
public class NodeRestartJobInfoConverter extends JobInfoConverterImpl {

    @Inject
    NodeRestartActivitySchedulesUtil activitySchedulesUtil;

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeRestartJobInfoConverter.class);

    public JobInfo prepareJobInfoData(final ShmRemoteJobData shmRemoteJobData) throws TopologyCollectionsServiceException, NoMeFDNSProvidedException {
        final JobInfo jobInfo = new JobInfo();
        final ShmNodeRestartJobData shmNodeRestartJobData = (ShmNodeRestartJobData) shmRemoteJobData;
        setNeNamesFromSearchScope(shmRemoteJobData);

        final Map<PlatformTypeEnum, List<String>> supportedPlatformTypeAndNodeTypes = platformAndNeTypeFinder.findSupportedPlatformAndNodeTypes(SHMCapabilities.NODE_RESTART_JOB_CAPABILITY,
                shmNodeRestartJobData);
        jobInfo.setJobType(JobTypeEnum.NODERESTART);
        jobInfo.setConfigurations(getConfigurations(supportedPlatformTypeAndNodeTypes, shmNodeRestartJobData));
        jobInfo.setActivitySchedules(getActivitySchedules(supportedPlatformTypeAndNodeTypes));
        setCommonAttributes(shmNodeRestartJobData, jobInfo);

        LOGGER.debug("JobInfo:" + jobInfo.getName() + " User name:" + jobInfo.getOwner() + "Job Info Configurations:" + jobInfo.getConfigurations() + " Activity schedules:"
                + jobInfo.getActivitySchedules() + "Main schedule:" + jobInfo.getMainSchedule() + "Collection Ids:" + jobInfo.getcollectionNames() + "Saved SearchIDs:" + jobInfo.getSavedSearchIds()
                + "NE Names:" + jobInfo.getNeNames());

        return jobInfo;
    }

    /**
     * Fetch and set activity params & NE params attributes for the corresponding job type with selected node types
     * 
     * @param supportedPlatformTypeAndNodeTypes
     * @param shmNodeRestartJobData
     * @return
     */
    private List<Map<String, Object>> getConfigurations(final Map<PlatformTypeEnum, List<String>> supportedPlatformTypeAndNodeTypes, final ShmNodeRestartJobData shmNodeRestartJobData) {
        final List<Map<String, Object>> configurations = new ArrayList<Map<String, Object>>();
        for (Entry<PlatformTypeEnum, List<String>> platformTypeAndNodeType : supportedPlatformTypeAndNodeTypes.entrySet()) {
            final Map<String, Object> platformType = new HashMap<String, Object>();
            platformType.put(ShmConstants.PLATFORM, platformTypeAndNodeType.getKey().name());
            for (String nodeType : platformTypeAndNodeType.getValue()) {
                final Map<String, Object> neTypeDetails = new HashMap<String, Object>();
                neTypeDetails.put(ShmConstants.NETYPE, nodeType);
                neTypeDetails.put(ShmConstants.PROPERTIES,
                        neTypePropertiesUtil.prepareNeTypeProperties(platformTypeAndNodeType.getKey(), nodeType, JobType.NODERESTART.getJobTypeName(), shmNodeRestartJobData));
                configurations.add(neTypeDetails);
            }
            configurations.add(platformType);
        }
        return configurations;
    }

    /**
     * Fetch and set activity schedule attributes for the corresponding job type with selected node types
     * 
     * @param supportedPlatformTypeAndNodeTypes
     * @return
     */
    private List<Map<String, Object>> getActivitySchedules(final Map<PlatformTypeEnum, List<String>> supportedPlatformTypeAndNodeTypes) {
        final List<Map<String, Object>> activitySchedules = new ArrayList<Map<String, Object>>();
        for (Entry<PlatformTypeEnum, List<String>> platformTypeAndNodeType : supportedPlatformTypeAndNodeTypes.entrySet()) {
            final Map<String, Object> platformTypeDetails = new HashMap<String, Object>();
            platformTypeDetails.put(ShmConstants.PLATFORMTYPE, platformTypeAndNodeType.getKey().name());
            final List<Map<String, Object>> neTypeDetails = new ArrayList<Map<String, Object>>();
            for (String nodeType : platformTypeAndNodeType.getValue()) {
                final Map<String, Object> neTypeDetail = new HashMap<String, Object>();
                neTypeDetail.put(ShmConstants.NETYPE, nodeType);
                neTypeDetail.put(ShmConstants.VALUE, activitySchedulesUtil.prepareActivitySchedules(platformTypeAndNodeType.getKey(), nodeType, JobType.NODERESTART.getJobTypeName()));
                neTypeDetails.add(neTypeDetail);
            }
            platformTypeDetails.put(ShmConstants.VALUE, neTypeDetails);
            activitySchedules.add(platformTypeDetails);
        }
        return activitySchedules;
    }

    public JobCreationResponseCode isValidData(final ShmRemoteJobData shmRemoteJobData) {
        LOGGER.info("Validate Node Restart Job Details");
        final ShmNodeRestartJobData shmNodeRestartJobData = (ShmNodeRestartJobData) shmRemoteJobData;

        if (!validateRestartReason(shmNodeRestartJobData)) {
            return JobCreationResponseCode.INVALID_RESATRT_REASON;
        }
        if (shmNodeRestartJobData.getRestartRank() == null) {
            shmNodeRestartJobData.setRestartRank(RestartRank.RESTART_WARM.name());
        } else if (!validateRestartRank(shmNodeRestartJobData)) {
            return JobCreationResponseCode.INVALID_RESATRT_RANK;
        }
        if (shmNodeRestartJobData.getRestartInfo() == null) {
            shmNodeRestartJobData.setRestartInfo(ShmConstants.DEFAULTVALUE_RESTART_INFO);
        }

        LOGGER.info(shmNodeRestartJobData.getRestartInfo() + " " + shmNodeRestartJobData.getRestartRank() + " " + shmNodeRestartJobData.getRestartReason());
        return null;
    }

    private static boolean validateRestartReason(final ShmNodeRestartJobData shmNodeRestartJobData) {
        for (final RestartReason reason : RestartReason.values()) {
            final String restartReason = shmNodeRestartJobData.getRestartReason();
            if (reason.name().equalsIgnoreCase(restartReason)) {
                shmNodeRestartJobData.setRestartReason(restartReason.toUpperCase());
                return true;
            }
        }
        return false;
    }

    private static boolean validateRestartRank(final ShmNodeRestartJobData shmNodeRestartJobData) {
        for (final RestartRank rank : RestartRank.values()) {
            final String restartRank = shmNodeRestartJobData.getRestartRank();
            if (rank.name().equalsIgnoreCase(restartRank)) {
                shmNodeRestartJobData.setRestartRank(restartRank.toUpperCase());
                return true;
            }
        }
        return false;
    }

}
