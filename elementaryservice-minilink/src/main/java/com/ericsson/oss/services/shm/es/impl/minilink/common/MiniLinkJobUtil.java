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

package com.ericsson.oss.services.shm.es.impl.minilink.common;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.EAccessControl;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActiveSoftwareProvider;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.minilink.backup.BackupService;
import com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MiniLinkJobUtil {

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private ActiveSoftwareProvider activeSoftwareProvider;

    @Inject
    private EAccessControl accessControl;

    public static final List<NotificationEventTypeEnum> GOOD_EVENT_TYPES = Arrays.asList(NotificationEventTypeEnum.AVC, NotificationEventTypeEnum.CREATE);

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupService.class);

    private static final String PRODUCT_DATA_SPLIT_CHARACTER = "\\|\\|";

    /**
     * Returns the backup name from the job properties
     * 
     * @param activityJobId
     */
    public String getBackupName(final long activityJobId, final JobEnvironment jobEnvironment) {
        String backupName = null;
        try {
            final Map<String, String> backupDetails = jobPropertyUtils.getPropertyValue(
                    Arrays.asList(MiniLinkConstants.BACKUP_NAME, JobPropertyConstants.AUTO_GENERATE_BACKUP),
                    activityUtils.getJobConfigurationDetails(activityJobId));
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            final Map<String, Object> neJobAttributes = jobEnvironment.getNeJobAttributes();
            backupName = activityUtils.getActivityJobAttributeValue(neJobAttributes, MiniLinkConstants.BACKUP_NAME);
            if (backupName == null || backupName.isEmpty()) {
                backupName = backupDetails.get(MiniLinkConstants.BACKUP_NAME);
            }
            final String autoGenerateBackup = backupDetails.get(JobPropertyConstants.AUTO_GENERATE_BACKUP);
            if ((backupName == null || backupName.isEmpty())
                    && (autoGenerateBackup == null || autoGenerateBackup.isEmpty() || ActivityConstants.CHECK_FALSE.equals(autoGenerateBackup))) {
                return null;
            }
            if (ActivityConstants.CHECK_TRUE.equals(autoGenerateBackup)) {
                backupName = activityUtils.getActivityJobAttributeValue(neJobAttributes, MiniLinkConstants.BACKUP_NAME);
                if (backupName == null || backupName.isEmpty()) {
                    final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
                    accessControl.setAuthUserSubject(jobStaticData.getOwner());
                    final Map<String, String> activeSoftware = activeSoftwareProvider.getActiveSoftwareDetails(Arrays.asList(neJobStaticData
                            .getNodeName()));
                    LOGGER.debug("Fetched active software {} for node name {}", activeSoftware, neJobStaticData.getNodeName());
                    if (activeSoftware == null || activeSoftware.isEmpty()) {
                        return null;
                    }
                    final String[] productDataDetails = activeSoftware.get(neJobStaticData.getNodeName()).split(PRODUCT_DATA_SPLIT_CHARACTER);
                    final String executingUpProductNumber = productDataDetails[0];
                    final String executingUpProductRevison = productDataDetails[1];
                    backupName = generateBackupName(executingUpProductNumber, executingUpProductRevison);
                    updateBackupNameInNeJob(neJobStaticData.getNeJobId(), backupName);
                }
            }
        } catch (JobDataNotFoundException e) {
            LOGGER.error(String.format(MiniLinkConstants.LOG_EXCEPTION, MiniLinkConstants.BACKUP_JOB, activityJobId), e);
        }
        LOGGER.debug("Backup name in getBackupName() method is : {}", backupName);
        return backupName;
    }

    private String generateBackupName(final String productNumber, final String productRevision) {
        final Date dateTime = new Date();
        final SimpleDateFormat formatter = new SimpleDateFormat(JobPropertyConstants.AUTO_GENERATE_DATE_FORMAT);
        final String timestamp = formatter.format(dateTime);
        String prodNum_prodRev = productNumber.concat(ActivityConstants.UNDERSCORE).concat(productRevision);
        prodNum_prodRev = prodNum_prodRev.replace(ActivityConstants.SLASH, ActivityConstants.UNDERSCORE);
        return prodNum_prodRev.concat(ActivityConstants.UNDERSCORE).concat(timestamp);
    }

    /**
     * This method updates Backup Name in the NE Job properties for given NE Job Id.
     * 
     * @param neJobId
     * @param backupName
     */
    private void updateBackupNameInNeJob(final long neJobId, final String backupName) {
        final List<Map<String, Object>> neJobPropertiesList = new ArrayList<>();
        activityUtils.prepareJobPropertyList(neJobPropertiesList, MiniLinkConstants.BACKUP_NAME, backupName);
        LOGGER.debug("Updating NE Job property for : {} with attributes {}", neJobId, neJobPropertiesList);
        jobUpdateService.readAndUpdateRunningJobAttributes(neJobId, neJobPropertiesList, null, null);
    }

    /**
     * Factory method for creating a BackupJobProperties object from a notification message.
     * 
     * @param message
     * @return
     */
    public BackupActivityProperties getBackupActivityProperties(final Notification message, final String activityName, final Class<?> activityServiceClass) {
        final long activityJobId = activityUtils.getActivityJobId(message.getNotificationSubject());
        return getBackupActivityProperties(activityJobId, activityName, activityServiceClass);
    }

    /**
     * actory method for creating a BackupJobProperties object from a given activity job ID.
     * 
     * @param activityJobId
     * @return
     */
    public BackupActivityProperties getBackupActivityProperties(final long activityJobId, final String activityName, final Class<?> activityServiceClass) {
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final String backupName = getBackupName(activityJobId, jobEnvironment);
        final BackupActivityProperties backupActivityProperties = new BackupActivityProperties(activityJobId, jobEnvironment, backupName, activityName, activityServiceClass);
        return backupActivityProperties;
    }

    /**
     * Utility method for writing to the job log.
     * 
     * @param backupActivityProperties
     * @param messages
     */
    public void writeToJobLog(final BackupActivityProperties backupActivityProperties, final String... messages) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        for (final String message : messages) {
            jobLogList.add(activityUtils.createNewLogEntry(message, JobLogLevel.INFO.getLogLevel()));
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(backupActivityProperties.getActivityJobId(), Collections.<Map<String, Object>> emptyList(), jobLogList);
    }

    /**
     * Writes to the job log and return the appropriate ActivityStepResult in case of a successful activityPrecheck.
     * 
     * @param backupActivityProperties
     * @return
     */
    public ActivityStepResult precheckSuccess(final BackupActivityProperties backupActivityProperties) {
        writeToJobLog(backupActivityProperties, String.format(JobLogConstants.PRE_CHECK_SUCCESS, backupActivityProperties.getActivityName()));
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        return activityStepResult;
    }

    /**
     * Writes to the job log and return the appropriate ActivityStepResult in case of a activityPrecheck failure.
     * 
     * @param backupActivityProperties
     * @return
     */

    public ActivityStepResult precheckFailure(final BackupActivityProperties backupActivityProperties, final String reason) {
        writeToJobLog(backupActivityProperties, String.format(JobLogConstants.PRE_CHECK_FAILURE, backupActivityProperties.getActivityName(), reason));
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        return activityStepResult;
    }

    /**
     * Utility method for updateing the job property.
     * 
     * @param backupActivityProperties
     * @param key
     * @param value
     */
    public void updateJobProperty(final BackupActivityProperties backupActivityProperties, final String key, final Object value) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        activityUtils.addJobProperty(key, value, jobPropertyList);
        jobUpdateService.readAndUpdateRunningJobAttributes(backupActivityProperties.getActivityJobId(), jobPropertyList, Collections.<Map<String, Object>> emptyList());
    }

    /**
     * Utility method for finishing a job. It sends the notification to the workflow service and unsubsribes from the xfConfigLoadObjects MO notifications.
     * 
     * @param backupActivityProperties
     */
    public void sendNotificationToWFS(final BackupActivityProperties backupActivityProperties) {
        activityUtils.sendNotificationToWFS(backupActivityProperties.getJobEnvironment(), backupActivityProperties.getActivityJobId(), backupActivityProperties.getActivityName(),
                Collections.<String, Object> emptyMap());
    }

    public void updateBackupInNeJob(final long neJobId, final String backupName) {
        updateBackupNameInNeJob(neJobId, backupName);
    }

}
