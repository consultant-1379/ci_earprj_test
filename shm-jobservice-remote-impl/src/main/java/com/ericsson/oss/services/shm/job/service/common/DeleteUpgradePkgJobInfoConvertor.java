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
import com.ericsson.oss.services.shm.job.remote.api.ShmDeleteUpgradePkgJobData;
import com.ericsson.oss.services.shm.job.remote.api.ShmRemoteJobData;
import com.ericsson.oss.services.shm.job.remote.api.errorcodes.JobCreationResponseCode;
import com.ericsson.oss.services.shm.job.remote.impl.JobInfoConverterImpl;
import com.ericsson.oss.services.shm.jobs.common.annotations.JobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.jobservice.common.JobInfo;
import com.ericsson.oss.services.topologyCollectionsService.exception.service.TopologyCollectionsServiceException;

@JobTypeAnnotation(jobType = JobType.DELETE_UPGRADEPACKAGE)
public class DeleteUpgradePkgJobInfoConvertor extends JobInfoConverterImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteUpgradePkgJobInfoConvertor.class);

    @Inject
    private DeleteUpgardePkgActivitySchedulesUtil deleteUpgardePkgActivitySchedulesUtil;

    /**
     * To Prepare the JobInfo data by Converting the shmRemoteJobData coming from the External Interfaces.
     * 
     * @param shmRemoteJobData
     * @return jobInfo
     */
    @Override
    public JobInfo prepareJobInfoData(final ShmRemoteJobData shmRemoteJobData) throws TopologyCollectionsServiceException, NoMeFDNSProvidedException {
        LOGGER.debug("DeleteUpgradePkgJobInfoConvertor conversion starts...");
        final JobInfo jobInfo = new JobInfo();
        final ShmDeleteUpgradePkgJobData shmDeleteUpgradePkgJobData = (ShmDeleteUpgradePkgJobData) shmRemoteJobData;
        final Map<PlatformTypeEnum, List<String>> supportedPlatformTypeAndNodeTypes = platformAndNeTypeFinder.findSupportedPlatformAndNodeTypes(SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY,
                shmDeleteUpgradePkgJobData);
        jobInfo.setJobType(JobTypeEnum.DELETE_UPGRADEPACKAGE);
        jobInfo.setConfigurations(getConfigurations(supportedPlatformTypeAndNodeTypes, shmDeleteUpgradePkgJobData));
        jobInfo.setActivitySchedules(getActivitySchedules(supportedPlatformTypeAndNodeTypes));
        setCommonAttributes(shmDeleteUpgradePkgJobData, jobInfo);

        LOGGER.debug("JobInfo:" + jobInfo.getName() + " User name:" + jobInfo.getOwner() + "Job Info Configurations:" + jobInfo.getConfigurations() + " Activity schedules:"
                + jobInfo.getActivitySchedules() + "Main schedule:" + jobInfo.getMainSchedule() + "Collection Ids:" + jobInfo.getcollectionNames() + "Saved SearchIDs:" + jobInfo.getSavedSearchIds()
                + "NE Names:" + jobInfo.getNeNames() + "jobInfo.getConfigurations()" + jobInfo.getConfigurations() + "jobInfo.getActivitySchedules()" + jobInfo.getActivitySchedules());

        return jobInfo;
    }

    /**
     * To Validate ShmRemoteJobData coming from the External Interfaces
     * 
     * @param shmJobData
     */
    @Override
    public JobCreationResponseCode isValidData(final ShmRemoteJobData shmJobData) {

        final ShmDeleteUpgradePkgJobData shmDeleteUpgradePkgJobData = (ShmDeleteUpgradePkgJobData) shmJobData;
        if (shmDeleteUpgradePkgJobData.getProductNumber() == null || shmDeleteUpgradePkgJobData.getProductNumber() == "") {
            return JobCreationResponseCode.PRODUCTNUMBER_NOT_NULL_EMPTY;
        }
        if (shmDeleteUpgradePkgJobData.getProductRevision() == null || shmDeleteUpgradePkgJobData.getProductRevision() == "") {
            return JobCreationResponseCode.PRODUCTREVISION_NOT_NULL_EMPTY;
        }
        return null;

    }

    /**
     * Fetch and set activity params & NE params attributes for the corresponding jobtype with selected node types
     * 
     * @param supportedPlatformTypeAndNodeTypes
     * @param shmDeleteUpgradePkgJobData
     * @return
     */
    private List<Map<String, Object>> getConfigurations(final Map<PlatformTypeEnum, List<String>> supportedPlatformTypeAndNodeTypes, final ShmDeleteUpgradePkgJobData shmDeleteUpgradePkgJobData) {
        final List<Map<String, Object>> configurations = new ArrayList<Map<String, Object>>();
        for (final Entry<PlatformTypeEnum, List<String>> platformTypeAndNodeType : supportedPlatformTypeAndNodeTypes.entrySet()) {
            final Map<String, Object> platformType = new HashMap<String, Object>();
            platformType.put(ShmConstants.PLATFORM, platformTypeAndNodeType.getKey().name());
            for (final String nodeType : platformTypeAndNodeType.getValue()) {
                final Map<String, Object> neTypeDetails = new HashMap<String, Object>();
                neTypeDetails.put(ShmConstants.NETYPE, nodeType);
                neTypeDetails.put(ShmConstants.PROPERTIES,
                        neTypePropertiesUtil.prepareNeTypeProperties(platformTypeAndNodeType.getKey(), nodeType, JobType.DELETE_UPGRADEPACKAGE.getJobTypeName(), shmDeleteUpgradePkgJobData));
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
     * @param deleteUpgradePkgActivities
     * @return
     */

    private List<Map<String, Object>> getActivitySchedules(final Map<PlatformTypeEnum, List<String>> supportedPlatformTypeAndNodeTypes) {
        final List<Map<String, Object>> activitySchedules = new ArrayList<Map<String, Object>>();
        for (final Entry<PlatformTypeEnum, List<String>> platformTypeAndNodeType : supportedPlatformTypeAndNodeTypes.entrySet()) {
            final Map<String, Object> platformTypeDetails = new HashMap<String, Object>();
            platformTypeDetails.put(ShmConstants.PLATFORMTYPE, platformTypeAndNodeType.getKey().name());
            final List<Map<String, Object>> neTypeDetails = new ArrayList<Map<String, Object>>();
            for (final String nodeType : platformTypeAndNodeType.getValue()) {
                final Map<String, Object> neTypeDetail = new HashMap<String, Object>();
                neTypeDetail.put(ShmConstants.NETYPE, nodeType);
                neTypeDetail.put(ShmConstants.VALUE,
                        deleteUpgardePkgActivitySchedulesUtil.prepareActivitySchedules(platformTypeAndNodeType.getKey(), nodeType, JobType.DELETE_UPGRADEPACKAGE.getJobTypeName()));
                neTypeDetails.add(neTypeDetail);
            }
            platformTypeDetails.put(ShmConstants.VALUE, neTypeDetails);
            activitySchedules.add(platformTypeDetails);
        }
        return activitySchedules;
    }

}
