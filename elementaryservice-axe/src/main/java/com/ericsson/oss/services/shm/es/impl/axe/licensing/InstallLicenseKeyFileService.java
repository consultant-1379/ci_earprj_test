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

package com.ericsson.oss.services.shm.es.impl.axe.licensing;

import static com.ericsson.oss.services.shm.es.axe.common.AxeConstants.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetryContext;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.SecurityViolationException;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.FileResource;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.AsynchronousActivity;
import com.ericsson.oss.services.shm.es.api.CommonLicensingActivityConstants;
import com.ericsson.oss.services.shm.es.axe.common.AbstractAxeService;
import com.ericsson.oss.services.shm.es.axe.common.AxeConstants;
import com.ericsson.oss.services.shm.es.axe.common.SessionIdResponse;
import com.ericsson.oss.services.shm.es.axe.common.WinFIOLRequestStatus;
import com.ericsson.oss.services.shm.es.axe.common.WinFIOLServiceBusyException;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.license.LicenseKeyFileDeleteService;
import com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants;
import com.ericsson.oss.services.shm.es.impl.license.LicensingRetryService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;

/**
 *
 * This class facilitates the installation of license key files of AXE based node by invoking the WINFIOL that initializes the install activity
 *
 * @author xsanmee
 */
@EServiceQualifier("AXE.LICENSE.install")
@ActivityInfo(activityName = "install", jobType = JobTypeEnum.LICENSE, platform = PlatformTypeEnum.AXE)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class InstallLicenseKeyFileService extends AbstractAxeService implements Activity, AsynchronousActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstallLicenseKeyFileService.class);
    private static final ActivityInfo activityAnnotation = InstallLicenseKeyFileService.class.getAnnotation(ActivityInfo.class);
    /**
     * axe-node-liinventory/{neId}/licensekeyfile
     */
    private static final String WINFIOL_LICENSE_INSTALL_REQUEST_URI = "axe-node-liinventory/%s/licensekeyfile";
    /**
     * axe-node-liinventory/{neId}/licensekeyfile/{sessionId}
     */
    private static final String WINFIOL_LICENSE_POLLING_URI = "axe-node-liinventory/%s/licensekeyfile/%s";

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    private RetryManager retryManager;

    @Inject
    private AxeLicenseRetryPolicy axeLicenseRetryPolicy;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceRetryProxy;

    @Inject
    private LicensingRetryService licensingRetryService;

    @Inject
    private FileResource fileResource;

    @Inject
    private LicenseKeyFileDeleteService licenseKeyFileDeleteService;

    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        LOGGER.debug("Inside  AXE InstallLicense.execute with activityJobId: {}", activityJobId);
        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        long mainJobId = 0L;
        String nodeName = "";
        String businessKey = "";
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY);
            mainJobId = neJobStaticData.getMainJobId();
            nodeName = neJobStaticData.getNodeName();
            businessKey = neJobStaticData.getNeJobBusinessKey();
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(mainJobId);
            final String neType = networkElementRetrivalBean.getNeType(nodeName);
            final String inputLKFPath = getInputLicenseKeyFilePath(neJobStaticData, neType);

            evaluatePrechecks(activityJobId, neJobStaticData, jobStaticData, inputLKFPath);

            jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.ACTIVITY_INITIATED, INSTALL_LICENSE_DISPLAY_NAME), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            systemRecorder.recordCommand(SHMEvents.LICENSE_INSTALL_EXECUTE, CommandPhase.STARTED, nodeName, businessKey,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.LICENSE));

            final String shmJobExecUser = getShmJobExecUser(neJobStaticData.getMainJobId(), jobStaticData);
            final SessionIdResponse sessionIdResponse = triggerLicenseInstallRequest(nodeName, inputLKFPath, shmJobExecUser);
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.IS_ACTIVITY_TRIGGERED, ActivityConstants.CHECK_TRUE);

            if (sessionIdResponse != null && sessionIdResponse.getSessionId() != null) {
                final Integer activityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.AXE.name(), JobTypeEnum.LICENSE.name(), activityAnnotation.activityName());
                activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.EXECUTE);
                activityUtils.prepareJobPropertyList(jobProperties, SESSION_ID, sessionIdResponse.getSessionId());
                activityUtils.prepareJobPropertyList(jobProperties, HOST_NAME, sessionIdResponse.getHostname());
                activityUtils.prepareJobPropertyList(jobProperties, COOKIE_HEADER, sessionIdResponse.getCookie());
                jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.ASYNC_ACTION_TRIGGERED, INSTALL_LICENSE_DISPLAY_NAME, activityTimeout), new Date(),
                        JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());

                final LicenseInstallResponse licensePollResponse = triggerLicensePollRequest(shmJobExecUser, neJobStaticData, sessionIdResponse);
                LOGGER.info("WinFiolResponse for licensePollResponse {}", licensePollResponse);
                evaluateExecuteResponse(activityJobId, neJobStaticData, jobLogs, jobProperties, licensePollResponse, inputLKFPath);
            } else {
                final String sessionIdResponseError = sessionIdResponse != null ? sessionIdResponse.getError() : INVALID_RESPONSE;
                failExecute(activityJobId, jobLogs, nodeName, businessKey, mainJobId, sessionIdResponseError);
            }
        } catch (JobDataNotFoundException e) {
            failExecute(activityJobId, jobLogs, nodeName, businessKey, mainJobId, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE);
        } catch (Exception e) {
            failExecute(activityJobId, jobLogs, nodeName, businessKey, mainJobId, e.getMessage());
        }
    }

    private void evaluatePrechecks(final long activityJobId, final NEJobStaticData neJobStaticData, final JobStaticData jobStaticData, final String lkfPath)
            throws JobDataNotFoundException, MoNotFoundException, FileNotFoundException {
        if (!activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, activityAnnotation.activityName())) {
            throw new SecurityViolationException("Unauthorized request");
        }
        final Map<String, Object> lkfRestriction = Collections.singletonMap(LicensingActivityConstants.LICENSE_DATA_LICENSE_KEYFILE_PATH, (Object) lkfPath);
        final List<Map<String, Object>> lkfDBData = licensingRetryService.getAttributesListOfLicensePOs(lkfRestriction);
        if (lkfDBData == null || lkfDBData.isEmpty()) {
            LOGGER.warn("License Key file information is not available for [nodeName={}, lkf={}] in database, Installation can not be continued", neJobStaticData.getNodeName(), lkfPath);
            throw new FileNotFoundException("LicensekeyFile not available in Database");
        }
        if (!fileResource.exists(lkfPath)) {
            LOGGER.warn("License Key file is not available for [nodeName={}, lkf={}] in ENM(SMRS), Installation can not be continued", neJobStaticData.getNodeName(), lkfPath);
            throw new FileNotFoundException("LicensekeyFile not available in ENM/SMRS");
        }
    }

    private void evaluateExecuteResponse(final long activityJobId, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogs, final List<Map<String, Object>> jobProperties,
            final LicenseInstallResponse licensePollResponse, final String lkfPath) {
        if (licensePollResponse.getStatus() == WinFIOLRequestStatus.OK) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, INSTALL_LICENSE_DISPLAY_NAME), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.getJobResult());
            systemRecorder.recordCommand(SHMEvents.LICENSE_INSTALL_EXECUTE, CommandPhase.FINISHED_WITH_SUCCESS, neJobStaticData.getNodeName(), neJobStaticData.getNeJobBusinessKey(),
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.LICENSE));
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs, 100d);
            activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, INSTALL_LICENSE_DISPLAY_NAME, Collections.<String, Object> emptyMap());
            deleteHistoricLicensePOs(neJobStaticData, Collections.singletonMap(LicensingActivityConstants.LICENSE_DATA_LICENSE_KEYFILE_PATH, (Object) lkfPath));
        } else if (licensePollResponse.getStatus() == WinFIOLRequestStatus.FAILED) {
            failExecute(activityJobId, jobLogs, neJobStaticData.getNodeName(), neJobStaticData.getNeJobBusinessKey(), neJobStaticData.getMainJobId(), licensePollResponse.getLicense());
        } else {
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.SERVICE_BUSY_STATUS, INSTALL_LICENSE_DISPLAY_NAME), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            LOGGER.warn("Licence Installation on:{} is still running or its result not yet available. Wait till the timeout.", neJobStaticData.getNodeName());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs, null);
        }
    }

    private SessionIdResponse triggerLicenseInstallRequest(final String nodeName, final String inputLKFPath, final String shmJobExecUser) {
        // TO DO : Path retrieval from SMRS is avoided due to a technical debt:
        // https://jira-nam.lmera.ericsson.se/browse/TORF-324635
        final String modifiedLKFPath = inputLKFPath.replace(AxeConstants.HOME, AxeConstants.ERICSSON + File.separator + AxeConstants.TOR);
        final Map<String, Object> headers = Collections.singletonMap(ShmConstants.USER_ID_KEY, (Object) shmJobExecUser);
        final Map<String, Object> body = Collections.singletonMap(BODY_PARAM_FILE, (Object) modifiedLKFPath);
        LOGGER.debug("Invoking WinFIOL RestCall for Lincense install on [Node:{}, keyFile:{}] ", nodeName, modifiedLKFPath);

        final SessionIdResponse sessionIdResponse = executePostRequest(String.format(WINFIOL_LICENSE_INSTALL_REQUEST_URI, nodeName), REST_HOST_NAME, headers, body, SessionIdResponse.class);
        LOGGER.info("License installation action Response from WinFiol: {}", sessionIdResponse);
        return sessionIdResponse;
    }

    private void failExecute(final long activityJobId, final List<Map<String, Object>> jobLogs, final String nodeName, final String businessKey, final Long mainJobId, final String message) {
        LOGGER.error("License Installation execute step failed due to:{}", message);
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.ACTIVITY_EXECUTE_FAILED_WITH_REASON, INSTALL_LICENSE_DISPLAY_NAME, message), new Date(),
                JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        systemRecorder.recordCommand(SHMEvents.LICENSE_INSTALL_EXECUTE, CommandPhase.FINISHED_WITH_ERROR, nodeName, activityAnnotation.activityName(),
                activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.LICENSE));
        activityUtils.failActivity(activityJobId, jobLogs, businessKey, INSTALL_LICENSE_DISPLAY_NAME);
    }

    @Override
    @Asynchronous
    public void asyncHandleTimeout(final long activityJobId) {
        LOGGER.debug("Inside  AXE InstallLicense.handleTimeout with activityJobId: {}", activityJobId);
        NEJobStaticData neJobStaticData = null;
        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        JobResult jobResult = JobResult.FAILED;
        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        Double progressPercentage = 0d;
        long activityStartTime = 0L;
        try {
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.OPERATION_TIMED_OUT, INSTALL_LICENSE_DISPLAY_NAME), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY);
            activityStartTime = neJobStaticData.getActivityStartTime();
            final LicenseInstallResponse licensePollResponse = getLicenseInstallationStatus(activityJobId, neJobStaticData);
            LOGGER.info("WinFiolResponse for licensePollResponse {}", licensePollResponse);
            if (licensePollResponse != null && licensePollResponse.getStatus() != null) {
                jobResult = evaluateTimeoutResult(licensePollResponse, jobLogs);
                if (jobResult == JobResult.SUCCESS) {
                    progressPercentage = 100d;
                    final String neType = networkElementRetrivalBean.getNeType(neJobStaticData.getNodeName());
                    final String inputLKFPath = getInputLicenseKeyFilePath(neJobStaticData, neType);
                    final Map<String, Object> lkfRestriction = Collections.singletonMap(LicensingActivityConstants.LICENSE_DATA_LICENSE_KEYFILE_PATH, (Object) inputLKFPath);
                    persistLicenseInstalledTime(neJobStaticData, lkfRestriction);
                    deleteHistoricLicensePOs(neJobStaticData, lkfRestriction);
                }
            } else {
                jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.ACTIVITY_FAILED_IN_TIMEOUT, INSTALL_LICENSE_DISPLAY_NAME, INVALID_RESPONSE), new Date(),
                        JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            }
        } catch (Exception e) {
            LOGGER.error("Exception ocuured in asyncHandleTimeout().reason: {}", e.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.ACTIVITY_FAILED_IN_TIMEOUT, INSTALL_LICENSE_DISPLAY_NAME, e.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs, progressPercentage);
        activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
        activityUtils.sendPrecheckOrTimeoutMessageToWfs(activityJobId, neJobStaticData, activityAnnotation.activityName(), null, JobVariables.TIMEOUT_FOR_HANDLE_TIMEOUT_COMPLETED);
    }

    private LicenseInstallResponse getLicenseInstallationStatus(final long activityJobId, final NEJobStaticData neJobStaticData) throws JobDataNotFoundException {
        final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
        final String shmJobExecUser = getShmJobExecUser(neJobStaticData.getNeJobId(), jobStaticData);
        final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
        final String sessionId = activityUtils.getActivityJobAttributeValue(activityJobAttributes, SESSION_ID);
        final String host = getHostName(activityUtils.getActivityJobAttributeValue(activityJobAttributes, COOKIE_HEADER), activityUtils.getActivityJobAttributeValue(activityJobAttributes, HOST_NAME));
        final Map<String, Object> headers = getHeaders(shmJobExecUser, activityUtils.getActivityJobAttributeValue(activityJobAttributes, COOKIE_HEADER));
        return executeGetRequest(String.format(WINFIOL_LICENSE_POLLING_URI, neJobStaticData.getNodeName(), sessionId), host, headers, LicenseInstallResponse.class);
    }

    @SuppressWarnings("unchecked")
    private String getInputLicenseKeyFilePath(final NEJobStaticData neJobStaticData, final String neType) throws FileNotFoundException {
        final long mainJobId = neJobStaticData.getMainJobId();
        final Map<String, Object> mainJobProperties = jobConfigurationServiceRetryProxy.getMainJobAttributes(mainJobId);
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobProperties.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
        final Map<String, String> propertyValue = jobPropertyUtils.getPropertyValue(Arrays.asList(CommonLicensingActivityConstants.LICENSE_FILE_PATH), jobConfigurationDetails,
                neJobStaticData.getNodeName(), neType, neJobStaticData.getPlatformType());
        final String lkfPathProperty = propertyValue.get(CommonLicensingActivityConstants.LICENSE_FILE_PATH);
        if (lkfPathProperty == null || lkfPathProperty.isEmpty()) {
            throw new FileNotFoundException("LicensekeyFile not provided");
        }
        LOGGER.debug("Input License Key file Path is {}", lkfPathProperty);
        return lkfPathProperty;
    }

    private JobResult evaluateTimeoutResult(final LicenseInstallResponse licensePollResponse, final List<Map<String, Object>> jobLogList) {
        JobResult jobResult = JobResult.FAILED;
        if (licensePollResponse.getStatus() == WinFIOLRequestStatus.OK) {
            jobResult = JobResult.SUCCESS;
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, INSTALL_LICENSE_DISPLAY_NAME), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
        } else if (licensePollResponse.getStatus() == WinFIOLRequestStatus.BUSY) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.STILL_EXECUTING, INSTALL_LICENSE_DISPLAY_NAME, licensePollResponse.getLicense()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_FAILED, INSTALL_LICENSE_DISPLAY_NAME), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
        } else {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_EXECUTE_FAILED_WITH_REASON, INSTALL_LICENSE_DISPLAY_NAME, licensePollResponse.getLicense()),
                    new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        return jobResult;
    }

    private LicenseInstallResponse triggerLicensePollRequest(final String shmJobExecUser, final NEJobStaticData neJobStaticData, final SessionIdResponse sessionIdResponse) {
        final String licenseJobStatusRestUrl = String.format(WINFIOL_LICENSE_POLLING_URI, neJobStaticData.getNodeName(), sessionIdResponse.getSessionId());

        final RetryPolicy licenseRetryPolicy = axeLicenseRetryPolicy.getAxeLicenseRetryPolicy();
        final Map<String, Object> headers = getHeaders(shmJobExecUser, sessionIdResponse.getCookie());
        return retryManager.executeCommand(licenseRetryPolicy, new RetriableCommand<LicenseInstallResponse>() {
            @Override
            public LicenseInstallResponse execute(final RetryContext retryContext) throws WinFIOLServiceBusyException {
                if (retryContext.getCurrentAttempt() > 1) {
                    final String host = getHostName(sessionIdResponse.getCookie(), sessionIdResponse.getHostname());
                    final LicenseInstallResponse licensePollResponse = executeGetRequest(licenseJobStatusRestUrl, host, headers, LicenseInstallResponse.class);
                    if (retryContext.getCurrentAttempt() < licenseRetryPolicy.getAttempts() && licensePollResponse != null
                            && (licensePollResponse.getStatus() == WinFIOLRequestStatus.BUSY || licensePollResponse.getStatus() == WinFIOLRequestStatus.NOT_FOUND)) {
                        throw new WinFIOLServiceBusyException("Winfiol server is busy");
                    } else {
                        return licensePollResponse != null ? licensePollResponse : new LicenseInstallResponse();
                    }
                } else {
                    throw new WinFIOLServiceBusyException("Do not poll for 1st attempt.");
                }
            }
        });
    }

    private void persistLicenseInstalledTime(final NEJobStaticData neJobStaticData, final Map<String, Object> restrictionAttributes) {
        final boolean isUpdateSuccess = licensingRetryService.updateLicenseInstalledTime(restrictionAttributes);
        if (!isUpdateSuccess) {
            activityUtils.recordEvent(SHMEvents.LICENSE_INSTALL_DELETE, neJobStaticData.getNodeName(), null, "SHM:" + neJobStaticData.getNeJobId() + ":" + neJobStaticData.getNodeName() + ":"
                    + "Updating of InstalledOn attribute got failed because LicenseData PO does not exists in the DPS.");
        }
    }

    private void deleteHistoricLicensePOs(final NEJobStaticData neJobStaticData, final Map<String, Object> restrictionAttributes) {
        final List<Map<String, Object>> lkfDBData = licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes);
        if (lkfDBData.isEmpty()) {
            activityUtils.recordEvent(SHMEvents.LICENSE_INSTALL_DELETE, neJobStaticData.getNodeName(), null,
                    "SHM:" + neJobStaticData.getNeJobId() + ":" + neJobStaticData.getMainJobId() + ":" + "LicensekeyFile information not available in database.");
        }
        for (final Map<String, Object> lkfData : lkfDBData) {
            final String fingerPrint = (String) lkfData.get(LicensingActivityConstants.LICENSE_DATA_FINGERPRINT);
            final String sequenceNumber = (String) lkfData.get(LicensingActivityConstants.LICENSE_DATA_SEQUENCE_NUMBER);

            final String logMessage = licenseKeyFileDeleteService.deleteHistoricLicensePOs(fingerPrint, sequenceNumber);
            activityUtils.recordEvent(SHMEvents.LICENSE_INSTALL_DELETE, neJobStaticData.getNodeName(), null,
                    "SHM:" + neJobStaticData.getNeJobId() + ":" + neJobStaticData.getMainJobId() + ":" + logMessage);
        }
    }

    @Override
    public void timeoutForAsyncHandleTimeout(final long activityJobId) {
        activityUtils.failActivityForHandleTimeoutExpiry(activityJobId, INSTALL_LICENSE_DISPLAY_NAME);
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        LOGGER.info("Inside InstallLicenseKeyFile Service cancel() with activityJobId : {}", activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        String nodeName = null;
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            activityUtils.logCancelledByUser(jobLogList, neJobStaticData, INSTALL_LICENSE_DISPLAY_NAME);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.CANCEL_NOT_POSSIBLE, INSTALL_LICENSE_DISPLAY_NAME), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.WARN.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_CANCEL_TRIGGERED, ActivityConstants.CHECK_TRUE);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_SUCESS);
        } catch (final JobDataNotFoundException jdnfEx) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
            LOGGER.error("Unable to cancel InstallLicenseKeyFile job. Reason : {}", jdnfEx);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final Exception ex) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
            LOGGER.error("Exception occurred in InstallLicenseKeyFile service.cancel for node {}. Reason is : {}", nodeName, ex);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.FAILURE_REASON, ex.getMessage()), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
        return activityStepResult;
    }

    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        return new ActivityStepResult();
    }

    @Override
    public void asyncPrecheck(final long activityJobId) {
        // Not Needed - Backward compatible legacy interface
    }

    @Override
    public void precheckHandleTimeout(final long activityJobId) {
        // Not Needed - Backward compatible legacy interface
    }

    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        // Not Needed - Backward compatible legacy interface
        return null;
    }

    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        // Not Needed - Backward compatible legacy interface
        return null;
    }

}
