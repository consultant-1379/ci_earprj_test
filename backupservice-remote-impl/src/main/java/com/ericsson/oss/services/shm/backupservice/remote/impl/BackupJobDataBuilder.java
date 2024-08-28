/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.shm.backupservice.remote.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.backupservice.remote.api.BackupInfo;
import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.UserContextBean;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobCategory;
import com.ericsson.oss.services.shm.jobservice.common.JobInfo;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

/**
 * This class is used to build job data using the input from remote API to trigger SHM backup job
 * 
 * @author xsrirda
 */
@Stateless
public class BackupJobDataBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupJobDataBuilder.class);

    @Inject
    private FdnServiceBean fdnServiceBean;

    @Inject
    private UserContextBean userContextBean;

    public JobInfo prepareJobInfo(final String neName, final String activityName, final JobTypeEnum jobType, final List<Map<String, Object>> jobConfiguration,
            final Map<String, String> neTypeAndPlatformType) {
        LOGGER.debug("Inside prepareJobInfo for backup activity {}", activityName);
        final JobInfo jobInfo = new JobInfo();
        jobInfo.setJobType(jobType);
        jobInfo.setName(getJobName(jobType));
        jobInfo.setNeNames(getNeNames(neName));
        jobInfo.setMainSchedule(getMainJobScheduledInfo());
        jobInfo.setConfigurations(getJobConfiguration(neName, jobConfiguration, activityName, neTypeAndPlatformType));
        jobInfo.setActivitySchedules(getActivityScheduledInfo(neName, activityName, neTypeAndPlatformType));
        jobInfo.setJobCategory(JobCategory.REMOTE);
        return jobInfo;
    }

    private String getJobName(final JobTypeEnum jobType) {
        String jobName = "";
        if (JobTypeEnum.BACKUP.equals(jobType)) {
            jobName = String.format("%s_%s_%s", ShmConstants.BACKUP_JOB, userContextBean.getLoggedInUserName(), System.currentTimeMillis());
        }
        return jobName.trim();
    }

    private Map<String, Object> getMainJobScheduledInfo() {
        final Map<String, Object> mainSchedule = new HashMap<String, Object>();
        mainSchedule.put(ShmConstants.EXECUTION_MODE, ExecMode.IMMEDIATE.toString());
        final List<Map<String, Object>> scheduleAttributes = new ArrayList<Map<String, Object>>();
        mainSchedule.put(ShmConstants.SCHEDULINGPROPERTIES, scheduleAttributes);
        return mainSchedule;
    }

    public Map<String, String> getNeTypeAndPlatformType(final String neName) {
        final Map<String, String> neTypeAndPlatFormType = new HashMap<String, String>();
             try {
                 final List<NetworkElement> networkElementList = fdnServiceBean.getNetworkElementsByNeNames(Arrays.asList(neName), SHMCapabilities.BACKUP_JOB_CAPABILITY);
            if (!networkElementList.isEmpty()) {
                final NetworkElement networkElement = networkElementList.get(0);
                neTypeAndPlatFormType.put(ShmConstants.NETYPE, networkElement.getNeType());
                neTypeAndPlatFormType.put(ShmConstants.PLATFORM, networkElement.getPlatformType().toString());
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to fetch the network elements for the node:{}. Exception is: ", neName, ex);
        }

        return neTypeAndPlatFormType;
    }

    private Integer getActivityOrder(final String activityName) {
        Integer order = null;
        if (ShmConstants.CREATE_BACKUP.equalsIgnoreCase(activityName) || ShmConstants.CREATE_CV__ACTIVITY.equalsIgnoreCase(activityName)) {
            order = 1;
        } else if (ShmConstants.UPLOAD_BACKUP.equalsIgnoreCase(activityName) || ShmConstants.RESTORE_BACKUP.equalsIgnoreCase(activityName)) {
            order = 2;
        }
        return order;
    }

    private List<Map<String, Object>> getJobConfiguration(final String neName, final List<Map<String, Object>> configurationsList, final String activityName,
            final Map<String, String> neTypeAndPlatformType) {
        final List<Map<String, Object>> configurations = new ArrayList<Map<String, Object>>();
        final Map<String, Object> platformTypeMap = new HashMap<String, Object>();
        final Map<String, Object> neTypePropertiesMap = new HashMap<String, Object>();
        String neType = "";
        String platformType = "";
        if (!neTypeAndPlatformType.isEmpty()) {
            neType = neTypeAndPlatformType.get(ShmConstants.NETYPE);
            platformType = neTypeAndPlatformType.get(ShmConstants.PLATFORM);
        }
        neTypePropertiesMap.put(ShmConstants.NETYPE, neType);
        if (ShmConstants.RESTORE_BACKUP.equalsIgnoreCase(activityName)) {
            final List<Map<String, Object>> nePropertiesList = new ArrayList<Map<String, Object>>();
            final Map<String, Object> nePropertiesMap = new HashMap<String, Object>();
            nePropertiesMap.put(ShmConstants.NENAMES, neName);
            nePropertiesMap.put(ShmConstants.PROPERTIES, configurationsList);
            nePropertiesList.add(nePropertiesMap);
            neTypePropertiesMap.put(ShmConstants.PROPERTIES, Collections.emptyList());
            neTypePropertiesMap.put(ShmConstants.NE_PROPERTIES, nePropertiesList);
            //TO DO: in else if below conditions can be removed when all AP async activities are done
        } else if (ShmConstants.CREATE_BACKUP.equalsIgnoreCase(activityName) || ShmConstants.UPLOAD_BACKUP.equalsIgnoreCase(activityName)
                || ShmConstants.CREATE_CV__ACTIVITY.equalsIgnoreCase(activityName)) {
            platformTypeMap.put(ShmConstants.PLATFORM, platformType);
            platformTypeMap.put(ShmConstants.PROPERTIES, Collections.emptyList());
            configurations.add(platformTypeMap);
            neTypePropertiesMap.put(ShmConstants.PROPERTIES, configurationsList);
        } else {
            LOGGER.warn("Currently not supporting for the activity: {}", activityName);
        }
        configurations.add(neTypePropertiesMap);
        return configurations;
    }

    private List<Map<String, Object>> getActivityScheduledInfo(final String neName, final String activityName, final Map<String, String> neTypeAndPlatformType) {
        final List<Map<String, Object>> activitySchedules = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activitiesMap = new HashMap<String, Object>();
        final List<Map<String, Object>> activitiesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neTypeMap = new HashMap<String, Object>();
        final List<Map<String, Object>> propertiesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityScheduleMap = new HashMap<String, Object>();
        String neType = "";
        String platformType = "";
        if (!neTypeAndPlatformType.isEmpty()) {
            neType = neTypeAndPlatformType.get(ShmConstants.NETYPE);
            platformType = neTypeAndPlatformType.get(ShmConstants.PLATFORM);
            activitiesMap.put(ShmConstants.PLATFORMTYPE, platformType);
            neTypeMap.put(ShmConstants.NETYPE, neType);
            activityScheduleMap.put(ShmConstants.ACTIVITYNAME, activityName);
            activityScheduleMap.put(ShmConstants.EXECUTION_MODE, ExecMode.IMMEDIATE.toString());
            activityScheduleMap.put(ShmConstants.ORDER, getActivityOrder(activityName));
            propertiesList.add(activityScheduleMap);
            neTypeMap.put(ShmConstants.VALUE, propertiesList);
            activitiesList.add(neTypeMap);
            activitiesMap.put(ShmConstants.VALUE, activitiesList);
            activitySchedules.add(activitiesMap);
        } else {
            LOGGER.error("Failed to preapre JobConfiguration data for the nodename:{} and activity:{}", neName, activityName);
        }
        return activitySchedules;
    }

    private List<Map<String, Object>> getNeNames(final String neName) {
        final List<Map<String, Object>> neNames = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neNamesMap = new HashMap<String, Object>();
        neNamesMap.put(ShmConstants.NAME, neName);
        neNames.add(neNamesMap);
        return neNames;
    }

    /**
     * Prepares the configuration attributes for each activity of BackUp job .
     * 
     * @param BackupJobData
     *            - contains the details required for backup operation.
     * @param activityName
     *            - contains activityName for backup job .
     */
    public List<Map<String, Object>> prepareJobConfiguration(final BackupInfo backupInfo, final String activityName) {
        final List<Map<String, Object>> jobConfiguration = new ArrayList<Map<String, Object>>();
        final Map<String, Object> backupName = new HashMap<String, Object>();
        if (ShmConstants.RESTORE_BACKUP.equalsIgnoreCase(activityName)) {
            final Map<String, Object> type = new HashMap<String, Object>();
            final Map<String, Object> domain = new HashMap<String, Object>();
            final Map<String, Object> backupLocation = new HashMap<String, Object>();
            backupLocation.put(ShmConstants.KEY, EcimBackupConstants.BACKUP_FILE_LOCATION);
            backupLocation.put(ShmConstants.VALUE, EcimBackupConstants.LOCATION_NODE);
            domain.put(ShmConstants.KEY, EcimBackupConstants.BRM_BACKUP_DOMAIN);
            domain.put(ShmConstants.VALUE, backupInfo.getDomain());
            type.put(ShmConstants.KEY, EcimBackupConstants.BRM_BACKUP_TYPE);
            type.put(ShmConstants.VALUE, backupInfo.getType());
            jobConfiguration.add(backupLocation);
            jobConfiguration.add(domain);
            jobConfiguration.add(type);
        } else if (ShmConstants.CREATE_BACKUP.equalsIgnoreCase(activityName) || ShmConstants.UPLOAD_BACKUP.equalsIgnoreCase(activityName)) {
            final Map<String, Object> domainAndType = new HashMap<String, Object>();
            backupName.put(ShmConstants.KEY, EcimBackupConstants.BRM_BACKUP_NAME);
            backupName.put(ShmConstants.VALUE, backupInfo.getName());
            jobConfiguration.add(backupName);
            domainAndType.put(ShmConstants.KEY, EcimBackupConstants.BRM_BACKUP_MANAGER_ID);
            domainAndType.put(ShmConstants.VALUE, backupInfo.getDomain() + ActivityConstants.SLASH + backupInfo.getType());
            jobConfiguration.add(domainAndType);
        } else if (ShmConstants.CREATE_CV__ACTIVITY.equalsIgnoreCase(activityName)) {
            backupName.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
            backupName.put(ShmConstants.VALUE, backupInfo.getName());

            final Map<String, Object> comments = new HashMap<String, Object>();
            final Map<String, Object> identity = new HashMap<String, Object>();
            final Map<String, Object> type = new HashMap<String, Object>();
            final Map<String, Object> startableCvName = new HashMap<String, Object>();
            final Map<String, Object> rollbackCvName = new HashMap<String, Object>();
            final Map<String, Object> uploadCvName = new HashMap<String, Object>();

            comments.put(ShmConstants.KEY, BackupActivityConstants.CV_COMMENT);
            comments.put(ShmConstants.VALUE, backupInfo.getComment());

            identity.put(ShmConstants.KEY, BackupActivityConstants.CV_IDENTITY);
            identity.put(ShmConstants.VALUE, backupInfo.getIdentity());

            type.put(ShmConstants.KEY, BackupActivityConstants.CV_TYPE);
            type.put(ShmConstants.VALUE, ShmConstants.CV_DEFAULT_TYPE);

            startableCvName.put(ShmConstants.KEY, BackupActivityConstants.STARTABLE_CV_NAME);
            startableCvName.put(ShmConstants.VALUE, backupInfo.getName());

            rollbackCvName.put(ShmConstants.KEY, BackupActivityConstants.ROLLBACK_CV_NAME);
            rollbackCvName.put(ShmConstants.VALUE, backupInfo.getName());

            uploadCvName.put(ShmConstants.KEY, BackupActivityConstants.UPLOAD_CV_NAME);
            uploadCvName.put(ShmConstants.VALUE, backupInfo.getName());

            jobConfiguration.add(backupName);
            jobConfiguration.add(comments);
            jobConfiguration.add(identity);
            jobConfiguration.add(type);
            jobConfiguration.add(startableCvName);
            jobConfiguration.add(rollbackCvName);
            jobConfiguration.add(uploadCvName);
        } else {
            LOGGER.warn("Currently not supporting for the activity: {}", activityName);
        }
        return jobConfiguration;
    }
}
