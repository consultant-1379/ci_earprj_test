/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.backup;

import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.BACKUP_JOB;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.BACKUP_NAME;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.DELIMETER;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.EXCEPTION_OCCURED_FAILURE_REASON;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.LOG_EXCEPTION;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.NETWORKELEMENT;

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

import com.ericsson.oss.itpf.sdk.core.util.StringUtils;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.EAccessControl;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActiveSoftwareProvider;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@Stateless
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MiniLinkOutdoorJobUtil {

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

    private static final Logger LOGGER = LoggerFactory.getLogger(MiniLinkOutdoorJobUtil.class);

    private static final String PRODUCT_DATA_SPLIT_CHARACTER = "\\|\\|";
    private static final Double PERCENT_ZERO = 0.0;
    private static final Double PERCENT_HUNDRED = 100.0;

    /**
     * Returns the backup name from the job properties
     * 
     * @param activityJobId
     */
    public String getBackupName(final long activityJobId, final JobEnvironment jobEnvironment) {
        String backupName = null;
        try {
            final Map<String, String> backupDetails = jobPropertyUtils.getPropertyValue(
                    Arrays.asList(BACKUP_NAME, JobPropertyConstants.AUTO_GENERATE_BACKUP), activityUtils.getJobConfigurationDetails(activityJobId));
            backupName = backupDetails.get(BACKUP_NAME);
            final String autoGenerateBackup = backupDetails.get(JobPropertyConstants.AUTO_GENERATE_BACKUP);
            if ((StringUtils.isEmpty(backupName))
                    && (StringUtils.isEmpty(autoGenerateBackup) || ActivityConstants.CHECK_FALSE.equals(autoGenerateBackup))) {
                return null;
            }
            if (ActivityConstants.CHECK_TRUE.equals(autoGenerateBackup)) {
                final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId,
                        SHMCapabilities.BACKUP_JOB_CAPABILITY);
                final Map<String, Object> neJobAttributes = jobEnvironment.getNeJobAttributes();
                backupName = activityUtils.getActivityJobAttributeValue(neJobAttributes, BACKUP_NAME);
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
            LOGGER.error(String.format(LOG_EXCEPTION, BACKUP_JOB, activityJobId), e);
        }
        LOGGER.debug("Backup name in getBackupName() method is : {}", backupName);
        return backupName;
    }

    private String generateBackupName(final String productNumber, final String productRevision) {
        final Date dateTime = new Date();
        final SimpleDateFormat formatter = new SimpleDateFormat(JobPropertyConstants.AUTO_GENERATE_DATE_FORMAT);
        final String timestamp = formatter.format(dateTime);
        String prodNumProdRev = productNumber.concat(ActivityConstants.UNDERSCORE).concat(productRevision);
        prodNumProdRev = prodNumProdRev.replace(ActivityConstants.SLASH, ActivityConstants.UNDERSCORE);
        return prodNumProdRev.concat(ActivityConstants.UNDERSCORE).concat(timestamp);
    }

    /**
     * This method updates Backup Name in the NE Job properties for given NE Job Id.
     * 
     * @param neJobId
     * @param backupName
     */
    private void updateBackupNameInNeJob(final long neJobId, final String backupName) {
        final List<Map<String, Object>> neJobPropertiesList = new ArrayList<>();
        activityUtils.prepareJobPropertyList(neJobPropertiesList, BACKUP_NAME, backupName);
        LOGGER.debug("Updating NE Job property for : {} with attributes {}", neJobId, neJobPropertiesList);
        jobUpdateService.readAndUpdateRunningJobAttributes(neJobId, neJobPropertiesList, null, null);
    }

    /**
     * actory method for creating a BackupJobProperties object from a given activity job ID.
     * 
     * @param activityJobId
     * @return
     */
    public BackupActivityProperties getBackupActivityProperties(final long activityJobId, final String activityName,
                                                                final Class<?> activityServiceClass) {
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final String backupName = getBackupName(activityJobId, jobEnvironment);
        return new BackupActivityProperties(activityJobId, jobEnvironment, backupName, activityName, activityServiceClass);
    }

    /**
     * Writes to the job log and return the appropriate ActivityStepResult in case of a successful activityPrecheck.
     * 
     * @param backupActivityProperties
     * @return
     */
    public ActivityStepResult precheckSuccess(final Double progressPercent, final BackupActivityProperties backupActivityProperties) {
        writeToJobLog(progressPercent, backupActivityProperties.getActivityJobId(),
                String.format(JobLogConstants.PRE_CHECK_SUCCESS, backupActivityProperties.getActivityName()));
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

    public ActivityStepResult precheckFailure(final Double progressPercent, final BackupActivityProperties backupActivityProperties,
                                              final String reason) {
        writeToJobLog(progressPercent, backupActivityProperties.getActivityJobId(),
                String.format(JobLogConstants.PRE_CHECK_FAILURE, backupActivityProperties.getActivityName(), reason));
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        return activityStepResult;
    }

    public ActivityStepResult timeoutFail(final Double progressPercent, final BackupActivityProperties activityProperties) {
        updateJobProperty(progressPercent, activityProperties.getActivityJobId(), ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
        writeToJobLog(progressPercent, activityProperties.getActivityJobId(), String.format(JobLogConstants.TIMEOUT, activityProperties.getActivityName()));
        activityUtils.unSubscribeToMoNotifications(getSubscriptionKey(activityProperties.getNodeName(), activityProperties.getActivityName()),
                activityProperties.getActivityJobId(), activityUtils.getActivityInfo(activityProperties.getActivityJobId(), this.getClass()));
        return activityUtils.getActivityStepResult(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
    }

    public ActivityStepResult timeoutSuccess(final Double progressPercent, final BackupActivityProperties activityProperties) {
        updateJobProperty(progressPercent, activityProperties.getActivityJobId(), ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.getJobResult());
        writeToJobLog(progressPercent, activityProperties.getActivityJobId(), String.format(JobLogConstants.TIMEOUT, activityProperties.getActivityName()));
        activityUtils.unSubscribeToMoNotifications(getSubscriptionKey(activityProperties.getNodeName(), activityProperties.getActivityName()),
                activityProperties.getActivityJobId(), activityUtils.getActivityInfo(activityProperties.getActivityJobId(), this.getClass()));
        return activityUtils.getActivityStepResult(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
    }

    public void failWithException(final NEJobStaticData neJobStaticData, final BackupActivityProperties activityProperties, final String jobType,
                                  final Exception e) {
        final String activityName = activityProperties.getActivityName();
        final String nodeName = activityProperties.getNodeName();
        final String exceptionMessage = String.format(LOG_EXCEPTION, jobType, activityName, nodeName);
        LOGGER.error(exceptionMessage, e);
        activityUtils.unSubscribeToMoNotifications(getSubscriptionKey(activityProperties.getNodeName(), activityProperties.getActivityName()),
                activityProperties.getActivityJobId(), activityUtils.getActivityInfo(activityProperties.getActivityJobId(), this.getClass()));
        failBackupRestoreActivity(neJobStaticData, activityProperties.getActivityJobId(), activityProperties.getActivityName(),
                EXCEPTION_OCCURED_FAILURE_REASON, e.getMessage());
    }

    /**
     * Writes to the job log and updates the job's property in case of a successful job finish.
     * 
     * @param backupActivityProperties
     */
    public void succeedBackupRestoreActivity(final NEJobStaticData neJobStaticData, final long activityId, final String activityName) {
        writeToJobLog(PERCENT_HUNDRED, activityId, String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, activityName));
        updateJobProperty(PERCENT_HUNDRED, activityId, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.getJobResult());
        activityUtils.sendNotificationToWFS(neJobStaticData, activityId, activityName, Collections.<String, Object> emptyMap());
    }

    /**
     * Writes to the job log and updates the job's property in case of a job failure.
     * 
     * @param backupActivityProperties
     */
    public void failBackupRestoreActivity(final NEJobStaticData neJobStaticData, final long activityId, final String activityName,
                                          final String reason, final String subject) {
        writeToJobLog(PERCENT_ZERO, activityId, String.format(JobLogConstants.ACTIVITY_FAILED, activityName),
                String.format(JobLogConstants.ADDITIONAL_FAILURE_RESULT, activityName, reason, subject));
        updateJobProperty(PERCENT_ZERO, activityId, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
        activityUtils.sendNotificationToWFS(neJobStaticData, activityId, activityName, Collections.<String, Object> emptyMap());
    }

    public String getSubscriptionKey(final String nodeName, final String activityName) {
        return DELIMETER + NETWORKELEMENT + nodeName + DELIMETER + activityName + DELIMETER;
    }

    public void updateJobProperty(final Double progressPercent, final long activityId, final String key, final Object value) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        activityUtils.addJobProperty(key, value, jobPropertyList);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityId, jobPropertyList, Collections.<Map<String, Object>> emptyList(),
                progressPercent);
    }

    /**
     * Utility method for writing to the job log.
     * 
     * @param backupActivityProperties
     * @param messages
     */
    public void writeToJobLog(final Double progressPercent, final long activityId, final String... messages) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        for (final String message : messages) {
            jobLogList.add(activityUtils.createNewLogEntry(message, JobLogLevel.INFO.getLogLevel()));
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityId, Collections.<Map<String, Object>> emptyList(), jobLogList, progressPercent);
    }

    /**
     * Finishes activity
     * 
     * @param jobEnvironment
     * @param jobActivityInfo
     * @param unsubscribeEventFdn
     * @param jobResult
     * @param jobLogList
     * @param activityName
     */
    public void finishActivity(final JobActivityInfo jobActivityInfo, final String unsubscribeEventFdn,
                               final JobResult jobResult, final List<Map<String, Object>> jobLogList, final String activityName) {
        LOGGER.debug("Finishing {} activity.", activityName);

        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final long activityJobId = jobActivityInfo.getActivityJobId();
        activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult(), jobPropertyList);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, 99.0);
        activityUtils.unSubscribeToMoNotifications(unsubscribeEventFdn, activityJobId, jobActivityInfo);
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY);
        } catch (JobDataNotFoundException e) {
            LOGGER.error("NE job static data not found in neJob cache and failed to get from DPS. {}", e);
        }
        activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, activityName, null);
    }
}
