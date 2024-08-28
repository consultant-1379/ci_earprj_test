/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.ecim.licenserefresh;

import static com.ericsson.oss.services.shm.es.ecim.licenserefresh.common.LicenseRefreshConstants.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.CommonLicensingActivityConstants;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.ecim.licenserefresh.common.LicenseRefreshConstants;
import com.ericsson.oss.services.shm.es.ecim.licensing.common.LicenseMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.license.refresh.api.LkfImportResponse;
import com.ericsson.oss.services.shm.instantaneouslicensing.LkfRefreshRequestResultMTRSender;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

public class RequestServiceProcessNotificationTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestServiceProcessNotificationTask.class);

    private static final ActivityInfo activityAnnotation = RequestService.class.getAnnotation(ActivityInfo.class);

    @Inject
    protected ActivityUtils activityUtils;

    @Inject
    protected JobLogUtil jobLogUtil;

    @Inject
    protected SystemRecorder systemRecorder;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private LicenseMoServiceRetryProxy licenseMoServiceRetryProxy;

    @Inject
    private LicenseRefreshServiceProvider licenseRefreshServiceProvider;

    @Inject
    private LkfRefreshRequestResultMTRSender lkfRefreshRequestResultMTRSender;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceRetryProxy;

    @Inject
    private LkfRefreshRequestResultBuilder lkfRefreshRequestResultBuilder;

    public void processNotification(final LkfImportResponse lkfImportResponse, final JobActivityInfo jobActivityInformation) {
        final long activityJobId = lkfImportResponse.getActivityJobId();
        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_REFRESH_JOB_CAPABILITY);
            LOGGER.info("LicenseRefreshJob:Request lkfImportResponse received as {}", lkfImportResponse);
            if (lkfImportResponse.getStatus().equalsIgnoreCase(STATUS_SUCCESS)) {
                processSuccessStatus(lkfImportResponse, jobActivityInformation, jobLogs, jobProperties, neJobStaticData);
            } else {
                sendLkfRequestResultForFailure(lkfImportResponse, neJobStaticData);
                processFailedStatus(lkfImportResponse, neJobStaticData, jobActivityInformation, jobLogs);
            }
        } catch (final JobDataNotFoundException jobDataNotFoundException) {
            LOGGER.error("LicenseRefreshJob:Request Elis process notification is failed due to : {} ", jobDataNotFoundException.getMessage());
            processException(lkfImportResponse, activityJobId, jobLogs, neJobStaticData, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE);
        } catch (final Exception exception) {
            LOGGER.error("LicenseRefreshJob:Request Elis process notification is failed due to : {} ", exception.getMessage());
            processException(lkfImportResponse, activityJobId, jobLogs, neJobStaticData, exception.getMessage());
        }

    }

    private void processException(final LkfImportResponse lkfImportResponse, final long activityJobId, final List<Map<String, Object>> jobLogs, NEJobStaticData neJobStaticData,
            final String errMessage) {
        sendLkfRequestResultForFailure(lkfImportResponse, neJobStaticData);
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, errMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        activityUtils.failActivity(activityJobId, jobLogs, (neJobStaticData != null && neJobStaticData.getNeJobBusinessKey() != null) ? neJobStaticData.getNeJobBusinessKey() : null,
                activityAnnotation.activityName());
        licenseRefreshServiceProvider.deleteShmNodeLicenseRefreshRequestDataPO((neJobStaticData != null && neJobStaticData.getNodeName() != null) ? neJobStaticData.getNodeName() : "");
    }

    private void processSuccessStatus(final LkfImportResponse lkfImportResponse, final JobActivityInfo jobActivityInformation, final List<Map<String, Object>> jobLogs,
            final List<Map<String, Object>> jobProperties, NEJobStaticData neJobStaticData) throws UnsupportedFragmentException, MoNotFoundException {
        if (lkfImportResponse.getState().equalsIgnoreCase(LKF_IMPORT_INITIATED)) {
            processImportInitiated(lkfImportResponse, jobLogs, neJobStaticData);
        } else if (lkfImportResponse.getState().equalsIgnoreCase(LKF_IMPORT_COMPLETED)) {
            sendLkfRequestResultForSuccess(neJobStaticData);
            processImportCompletion(lkfImportResponse, jobLogs, jobProperties, jobActivityInformation, neJobStaticData);
        }
    }

    private void processFailedStatus(final LkfImportResponse lkfImportResponse, final NEJobStaticData neJobStaticData, final JobActivityInfo jobActivityInformation,
            final List<Map<String, Object>> jobLogs) {
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(REQUEST_ACTIVITY_FAILURE, jobActivityInformation.getActivityName(), lkfImportResponse.getAdditionalInfo()), new Date(),
                JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        activityUtils.failActivity(lkfImportResponse.getActivityJobId(), jobLogs, neJobStaticData.getNeJobBusinessKey(), activityAnnotation.activityName());
        systemRecorder.recordCommand(SHMEvents.LICENSEREFRESH_REQUEST_NOTIFICATION, CommandPhase.FINISHED_WITH_ERROR, neJobStaticData.getNodeName(), neJobStaticData.getNeJobBusinessKey(),
                activityUtils.additionalInfoForCommand(lkfImportResponse.getActivityJobId(), neJobStaticData.getMainJobId(), JobTypeEnum.LICENSE_REFRESH));
        activityUtils.unSubscribeToMoNotifications(buildSubscriptionKey(lkfImportResponse.getFingerprint(), lkfImportResponse.getNeJobId()), lkfImportResponse.getActivityJobId(),
                jobActivityInformation);
        licenseRefreshServiceProvider.deleteShmNodeLicenseRefreshRequestDataPO(neJobStaticData.getNodeName());
    }

    private void processImportInitiated(final LkfImportResponse lkfImportResponse, final List<Map<String, Object>> jobLogs, final NEJobStaticData neJobStaticData) {
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(REQUEST_ACTIVITY_INPROGRESS, lkfImportResponse.getFingerprint()), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.INFO.toString());
        systemRecorder.recordCommand(SHMEvents.LICENSEREFRESH_REQUEST_NOTIFICATION, CommandPhase.ONGOING, neJobStaticData.getNodeName(), neJobStaticData.getNeJobBusinessKey(),
                activityUtils.additionalInfoForCommand(lkfImportResponse.getActivityJobId(), neJobStaticData.getMainJobId(), JobTypeEnum.LICENSE_REFRESH));
        jobUpdateService.readAndUpdateRunningJobAttributes(lkfImportResponse.getActivityJobId(), null, jobLogs, 50d);
        LOGGER.debug("LicenseRefreshJob:Request activity {} - shmElisLicenseRefreshProcessNotification IMPORT_INITIATED completed", lkfImportResponse.getActivityJobId());
    }

    private void processImportCompletion(final LkfImportResponse lkfImportResponse, final List<Map<String, Object>> jobLogs, final List<Map<String, Object>> jobProperties,
            final JobActivityInfo jobActivityInformation, final NEJobStaticData neJobStaticData) throws UnsupportedFragmentException, MoNotFoundException {

        jobLogUtil.prepareJobLogAtrributesList(jobLogs, REQUEST_ACTIVITY_COMPLETED, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());

        activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.getJobResult());
        systemRecorder.recordCommand(SHMEvents.LICENSEREFRESH_REQUEST_NOTIFICATION, CommandPhase.FINISHED_WITH_SUCCESS, neJobStaticData.getNodeName(), neJobStaticData.getNeJobBusinessKey(),
                activityUtils.additionalInfoForCommand(lkfImportResponse.getActivityJobId(), neJobStaticData.getMainJobId(), JobTypeEnum.LICENSE_REFRESH));
        updateNeJobProperties(lkfImportResponse, neJobStaticData);
        activityUtils.prepareJobPropertyList(jobProperties, FINGERPRINT, lkfImportResponse.getFingerprint());
        jobUpdateService.readAndUpdateRunningJobAttributes(lkfImportResponse.getActivityJobId(), jobProperties, jobLogs, 100d);
        activityUtils.unSubscribeToMoNotifications(buildSubscriptionKey(lkfImportResponse.getFingerprint(), lkfImportResponse.getNeJobId()), lkfImportResponse.getActivityJobId(),
                jobActivityInformation);
        final Map<String, Object> processVariables = new HashMap<>();
        processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, false);
        activityUtils.sendNotificationToWFS(neJobStaticData, lkfImportResponse.getActivityJobId(), activityAnnotation.activityName(), processVariables);
        licenseRefreshServiceProvider.deleteShmNodeLicenseRefreshRequestDataPO(neJobStaticData.getNodeName());
        LOGGER.debug("LicenseRefreshJob:Request activity {} shmElisLicenseRefreshProcessNotification completed", lkfImportResponse.getActivityJobId());

    }

    private void updateNeJobProperties(final LkfImportResponse lkfImportResponse, final NEJobStaticData neJobStaticData) throws UnsupportedFragmentException, MoNotFoundException {
        final String licenseKeyFilePath = licenseMoServiceRetryProxy.getLicenseKeyFileNameFromFingerPrint(lkfImportResponse.getFingerprint());
        final Map<String, Object> neJobAttributes = jobConfigurationServiceRetryProxy.getNeJobAttributes(neJobStaticData.getNeJobId());
        List<Map<String, String>> neJobProperties = (List<Map<String, String>>) neJobAttributes.get(ActivityConstants.JOB_PROPERTIES);
        if (neJobProperties == null) {
            neJobProperties = new ArrayList<>();
        }
        final Map<String, String> licenseFilePathJobProperty = new HashMap<>();
        licenseFilePathJobProperty.put(ActivityConstants.JOB_PROP_KEY, CommonLicensingActivityConstants.LICENSE_FILE_PATH);
        licenseFilePathJobProperty.put(ActivityConstants.JOB_PROP_VALUE, licenseKeyFilePath);
        neJobProperties.add(licenseFilePathJobProperty);
        neJobAttributes.put(ActivityConstants.JOB_PROPERTIES, neJobProperties);
        neJobAttributes.put(NE_NAME, neJobStaticData.getNodeName());
        LOGGER.debug("LicenseRefreshJob:Request activity neJobAttributes after licenseFilePathJobProperty update are : [{}]", neJobAttributes);
        jobUpdateService.updateJobAttributes(neJobStaticData.getNeJobId(), neJobAttributes);

    }

    private String buildSubscriptionKey(final String fingerPrint, final String neJobId) {
        return fingerPrint + SUBSCRIPTION_KEY_DELIMETER + neJobId;

    }

    private void sendLkfRequestResultForSuccess(NEJobStaticData neJobStaticData) {
        if (neJobStaticData != null && neJobStaticData.getNeJobId() > 0 && neJobStaticData.getNodeName() != null) {
            LOGGER.debug("LicenseRefreshJob:Request activity sendLkfRequestResultForSuccess for the neJobId {} and nodeName {}", neJobStaticData.getNeJobId(), neJobStaticData.getNodeName());
            final Map<String, Object> lkfRefreshRequestResultProperties = lkfRefreshRequestResultBuilder.buildLkfRequestResultForSuccess(neJobStaticData);
            if (isLkfRefreshRequestResultRequired(lkfRefreshRequestResultProperties)) {
                lkfRefreshRequestResultMTRSender.sendMTR(lkfRefreshRequestResultProperties);
            }
        } else {
            LOGGER.error("LicenseRefreshJob:LkfRequestResult sending to node is not triggerred due to values not available in neJobStaticData");
        }

    }

    private void sendLkfRequestResultForFailure(final LkfImportResponse lkfImportResponse, NEJobStaticData neJobStaticData) {
        if (neJobStaticData != null && neJobStaticData.getNeJobId() > 0 && neJobStaticData.getNodeName() != null) {
            LOGGER.debug("LicenseRefreshJob:Request activity sendLkfRequestResultForSuccess for the neJobId {} and nodeName {}", neJobStaticData.getNeJobId(), neJobStaticData.getNodeName());
            Map<String, Object> lkfRefreshRequestResultProperties = lkfRefreshRequestResultBuilder.buildLkfRequestResultForFailure(neJobStaticData, lkfImportResponse.getAdditionalInfo());
            if (isLkfRefreshRequestResultRequired(lkfRefreshRequestResultProperties)) {
                lkfRefreshRequestResultMTRSender.sendMTR(lkfRefreshRequestResultProperties);
            }
        } else {
            LOGGER.error("LicenseRefreshJob:LkfRequestResult sending to node is not triggerred due to values not available in neJobStaticData");
        }

    }

    private boolean isLkfRefreshRequestResultRequired(final Map<String, Object> lkfRefreshRequestResultProperties) {
        boolean isLkfRefreshRequestResultRequired = false;
        if (lkfRefreshRequestResultProperties != null && lkfRefreshRequestResultProperties.size() > 0 && lkfRefreshRequestResultProperties.get(LicenseRefreshConstants.REQUEST_TYPE) != null
                && ((String) lkfRefreshRequestResultProperties.get(LicenseRefreshConstants.REQUEST_TYPE)).equals(LicenseRefreshConstants.LKF_REFRESH)) {
            isLkfRefreshRequestResultRequired = true;
        }
        LOGGER.debug("LicenseRefreshJob:Request activity isLkfRefreshRequestResultRequired :  {}  ", isLkfRefreshRequestResultRequired);
        return isLkfRefreshRequestResultRequired;
    }

}
