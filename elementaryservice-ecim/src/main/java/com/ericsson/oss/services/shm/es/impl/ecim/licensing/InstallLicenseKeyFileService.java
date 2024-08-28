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

package com.ericsson.oss.services.shm.es.impl.ecim.licensing;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.nms.security.smrs.api.NodeType;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfo;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfoProvider;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.ecim.common.FragmentVersionCheck;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.AsynchronousActivity;
import com.ericsson.oss.services.shm.es.api.CommonLicensingActivityConstants;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.ecim.licensing.common.EcimLicensingInfo;
import com.ericsson.oss.services.shm.es.ecim.licensing.common.EcimLmUtils;
import com.ericsson.oss.services.shm.es.ecim.licensing.common.LicenseMoService;
import com.ericsson.oss.services.shm.es.ecim.licensing.common.LicenseMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.ecim.licensing.common.LicensePrecheckResponse;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.job.service.JobParameterChangeListener;
import com.ericsson.oss.services.shm.job.timer.NEJobProgressPercentageCache;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobCategory;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.notifications.impl.license.LicenseUtil;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@EServiceQualifier("ECIM.LICENSE.install")
@ActivityInfo(activityName = "install", jobType = JobTypeEnum.LICENSE, platform = PlatformTypeEnum.ECIM)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@SuppressWarnings("PMD.TooManyFields")
public class InstallLicenseKeyFileService implements Activity, ActivityCallback, AsynchronousActivity {

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private EcimLmUtils ecimLmUtils;

    @Inject
    private LicenseMoServiceRetryProxy licenseMoServiceRetryProxy;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private BrmMoServiceRetryProxy brmMoServiceRetryProxy;

    @Inject
    private FailsafeActivateDeactivateService failsafeActivateDeactivateService;

    @Inject
    private JobParameterChangeListener jobParameterChangeListener;

    @Inject
    private FragmentVersionCheck fragmentVersionCheck;

    @Inject
    private OssModelInfoProvider ossModelInfoProvider;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    @Inject
    private LicenseMoService licenseMoService;

    @Inject
    private LicenseUtil licenseUtil;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculator;

    @Inject
    private NEJobProgressPercentageCache progressPercentageCache;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceRetryProxy;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    JobLogUtil jobLogUtil;

    private static final Logger LOGGER = LoggerFactory.getLogger(InstallLicenseKeyFileService.class);

    private static final String ACTIVITY_NAME = "install";
    private static final String FAILURE = "Exception occurred in \"%s\" for the activity \"%s\" on node \"%s\".";
    private static final String VALIDATION_FAILURE = "License Validation Failed for \"%s\" ";
    private static final String ACTION_STATUS = "actionStatus";
    public static final String MO_NOT_EXIST = "MO Not Found";
    private static final String INSTALL_STATUS = "installStatus";
    private static final String NODE_NAME = "neName";

    /**
     * This method holds pre-checks to decide if the current activity can be started or not and sends back the activity result to Work Flow Service.
     * 
     * 
     */
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        LOGGER.debug("inside ecim license job install activity precheck with activityJobId {}", activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        activityUtils.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_INITIATED, ActivityConstants.INSTALL_LICENSE), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.INFO.toString());
        String logMessage = null;
        ActivityStepResultEnum preCheckStatus = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        long activityStartTime = 0;
        NEJobStaticData neJobStaticData = null;
        JobStaticData jobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY);
            jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            // TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ACTIVITY_NAME);
            if (!isUserAuthorized) {
                activityStepResult.setActivityResultEnum(preCheckStatus);
                return activityStepResult;
            }
        } catch (final JobDataNotFoundException ex) {
            preCheckStatus = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
            activityStepResult.setActivityResultEnum(preCheckStatus);
            activityUtils.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.INSTALL_LICENSE, ex.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
            LOGGER.error(String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.INSTALL_LICENSE, ex.getMessage()));
            return activityStepResult;
        } catch (final MoNotFoundException ex) {
            preCheckStatus = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
            LOGGER.error("MoNotFoundException occurred for activityJobId {} with Reason : {}", activityJobId, ex);
            logMessage = ex.getMessage();
            activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        }
        final String nodeName = neJobStaticData.getNodeName();
        final String treatAsInfo = activityUtils.isTreatAs(nodeName, FragmentType.ECIM_LM_TYPE, SHMCapabilities.LICENSE_JOB_CAPABILITY);
        if (treatAsInfo != null) {
            activityUtils.prepareJobLogAtrributesList(jobLogList, treatAsInfo, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        }
        preCheckStatus = getPrecheckStatus(nodeName, activityJobId);

        if (preCheckStatus == ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION || preCheckStatus == ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION) {
            LOGGER.debug("Skipping persisting step duration as activity is to be skipped.");
        } else {
            activityStartTime = neJobStaticData.getActivityStartTime();
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PRECHECK);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        activityStepResult.setActivityResultEnum(preCheckStatus);
        return activityStepResult;
    }

    private ActivityStepResultEnum getPrecheckStatus(final String nodeName, final long activityJobId) {
        ActivityStepResultEnum preCheckStatus = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        String logMessage = null;
        final String licenseKeyFileName = "";
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        try {
            final NetworkElementData networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
            final String neType = networkElement.getNeType();
            final OssModelInfo ossModelInfo = ossModelInfoProvider.getOssModelInfo(neType, networkElement.getOssModelIdentity(), FragmentType.ECIM_LM_TYPE.getFragmentName());
            if (ossModelInfo != null) {
                preCheckStatus = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
                logMessage = fragmentVersionCheck.checkFragmentVersion(FragmentType.ECIM_LM_TYPE, ossModelInfo.getReferenceMIMVersion());
                if (logMessage != null) {
                    activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                }

                if (NodeType.RADIONODE.getName().equalsIgnoreCase(networkElement.getNeType())) {
                    final boolean isValidateSuccess = validateLKF(jobLogList, licenseKeyFileName, nodeName, networkElement);
                    if (!isValidateSuccess) {
                        preCheckStatus = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
                    }
                }
            } else {
                logMessage = String.format(JobLogConstants.UNSUPPORTED_NODE_MODEL, networkElement.getNeType(), networkElement.getOssModelIdentity());
                activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            }
        } catch (final MoNotFoundException ex) {
            preCheckStatus = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
            LOGGER.error("MoNotFoundException occurred for activityJobId {} with Reason : {}", activityJobId, ex);
            logMessage = ex.getMessage();
            activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        } catch (final UnsupportedFragmentException ex) {
            preCheckStatus = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
            LOGGER.error("UnsupportedFragmentException occurred for activityJobId {} with Reason : {}", activityJobId, ex);
            logMessage = ex.getMessage();
            activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        } catch (final Exception ex) {
            preCheckStatus = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
            LOGGER.error("Exception occurred for activityJobId {} with Reason : {}", activityJobId, ex);
            logMessage = ex.getMessage();
            activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        }
        return preCheckStatus;

    }

    private boolean validateLKF(final List<Map<String, Object>> jobLogList, final String licenseKeyFileName, final String nodeName, final NetworkElementData networkElement)
            throws MoNotFoundException {
        String logMessage;
        boolean preCheckStatus = true;
        final String productType = licenseMoService.getProducTypeOfLKF(networkElement, licenseKeyFileName);
        if (productType != null) {
            final boolean isRadioNodeLKF = licenseUtil.isRadioNodeLKF(productType, networkElement.getNeType());
            if (isRadioNodeLKF) {
                logMessage = String.format(JobLogConstants.PRE_CHECK_SUCCESS, ActivityConstants.INSTALL);
                activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            } else {
                preCheckStatus = false;
                logMessage = String.format(JobLogConstants.KEYFILE_MISMATCH_WITH_NETYPE, ActivityConstants.INSTALL_LICENSE, productType, nodeName);
                activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                LOGGER.error("KeyFile name {} is not matching for {} with neType : {}", licenseKeyFileName, nodeName, networkElement.getNeType());
            }
        }
        return preCheckStatus;
    }

    private void initiateActivity(final String nodeName, final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> jobPropertyList, final Map<String, Object> activityJobAttributes)
            throws MoNotFoundException {
        final boolean isPrecheckDone = activityUtils.isPrecheckDone((List<Map<String, String>>) activityJobAttributes.get(ShmConstants.JOBPROPERTIES));
        if (!isPrecheckDone) {
            activityUtils.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_INITIATED, ActivityConstants.INSTALL_LICENSE), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_PRECHECK_DONE, ActivityConstants.CHECK_TRUE);
            final String treatAsInfo = activityUtils.isTreatAs(nodeName, FragmentType.ECIM_LM_TYPE, SHMCapabilities.LICENSE_JOB_CAPABILITY);
            if (treatAsInfo != null) {
                activityUtils.prepareJobLogAtrributesList(jobLogList, treatAsInfo, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            }
        } else {
            LOGGER.debug("License install pre-Validation already done for the nodeName: {}", nodeName);
        }

    }

    /**
     * This method is the activity execute step called when pre-check stage is passed .It registers for notifications and initiates the MO action.
     * 
     * @param activityJobId
     */

    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        LOGGER.debug("Inside ecim license job install activity execute method with the activityJobId {}", activityJobId);
        String logMessage = null;
        String nodeName = "";
        boolean performfailsafeBackup = false;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        EcimLicensingInfo ecimLicensingInfo = null;
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            final NetworkElementData networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
            final String fingerprint = licenseMoService.getFingerPrintFromNode(networkElement);

            ecimLicensingInfo = ecimLmUtils.getLicensingInfo(activityJobId, neJobStaticData, networkElement);
            if (ecimLicensingInfo.getLicenseKeyFilePath() == null || ecimLicensingInfo.getLicenseKeyFilePath().isEmpty()) {
                final String licenseKeyFilePath = licenseMoServiceRetryProxy.getLicenseKeyFileNameFromFingerPrint(fingerprint);
                if(licenseKeyFilePath != null && !licenseKeyFilePath.isEmpty()) {
                    final Map<String, Object> mainJobAttributes = jobConfigurationServiceRetryProxy.getMainJobAttributes(neJobStaticData.getMainJobId());
                    final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
                    final List<Map<String, Object>> neJobPropertyList = (List<Map<String, Object>>) jobConfigurationDetails.get(ActivityConstants.NE_JOB_PROPERTIES);
                    for (final Map<String, Object> neJobProperty : neJobPropertyList) {
                        if (neJobProperty.get(NODE_NAME).equals(nodeName)) {
                            final List<Map<String, String>> jobpropertyList = (List<Map<String, String>>) neJobProperty.get(ActivityConstants.JOB_PROPERTIES);
                            final Map<String, String> jobProperty = new HashMap<>();
                            jobProperty.put(ActivityConstants.JOB_PROP_KEY, CommonLicensingActivityConstants.LICENSE_FILE_PATH);
                            jobProperty.put(ActivityConstants.JOB_PROP_VALUE, licenseKeyFilePath);
                            jobpropertyList.add(jobProperty);
                            final Map<String, Object> jobPropertiesMap = new HashMap<>();
                            jobPropertiesMap.put(ActivityConstants.JOB_PROPERTIES, jobpropertyList);
                            jobUpdateService.updateJobAttributes(neJobStaticData.getNeJobId(), jobPropertiesMap);
                        }
                    }
                } else {
                    logMessage = String.format(JobLogConstants.ACTIVITY_SKIPPED + "Because \"%s\".", ActivityConstants.INSTALL, "Licence Key File not found");
                    jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                    activityUtils.skipActivity(activityJobId, neJobStaticData, jobLogList, neJobStaticData.getNeJobBusinessKey(), ACTIVITY_NAME);
                    return;
                }
            }
            performfailsafeBackup = jobParameterChangeListener.getPerformFailsafeBackup();
            LOGGER.debug("performfailsafeBackup flag {}", performfailsafeBackup);
            final Map<String, Object> activityJobAttributes = jobConfigurationServiceRetryProxy.getActivityJobAttributes(activityJobId);

            initiateActivity(nodeName, jobLogList, jobPropertyList, activityJobAttributes);
            final LicensePrecheckResponse licensePrecheckResponse = getPrecheckResponse(activityJobId, neJobStaticData, jobLogList, networkElement);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            if (licensePrecheckResponse.getActivityStepResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {
                final long activityStartTime = neJobStaticData.getActivityStartTime();
                activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PRECHECK);
                executeMoAction(activityJobId, performfailsafeBackup, neJobStaticData, licensePrecheckResponse);
            } else if (licensePrecheckResponse.getActivityStepResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION) {
                activityUtils.skipActivity(activityJobId, neJobStaticData, jobLogList, neJobStaticData.getNeJobBusinessKey(), ACTIVITY_NAME);
            } else {
                LOGGER.error("Failing {} activity as Preccheck Failed for {}", ACTIVITY_NAME, nodeName);
                activityUtils.failActivity(activityJobId, jobLogList, neJobStaticData.getNeJobBusinessKey(), ACTIVITY_NAME);
            }
        }

        catch (final MoNotFoundException ex) {
            LOGGER.error("MoNotFoundException occurred for activityJobId {} with Reason : {}", activityJobId, ex);
            logMessage = ex.getMessage();
            handleException(activityJobId, logMessage, jobLogList, ecimLicensingInfo, neJobStaticData);
        } catch (final UnsupportedFragmentException ex) {
            LOGGER.error("UnsupportedFragmentException occurred ,Reason : {}", ex);
            logMessage = String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName);
            handleException(activityJobId, logMessage, jobLogList, ecimLicensingInfo, neJobStaticData);
        } catch (final ArgumentBuilderException ex) {
            LOGGER.error("ArgumentBuilderException occurred activityJobId {} with Reason : {}", activityJobId, ex);
            logMessage = ex.getMessage();
            handleException(activityJobId, logMessage, jobLogList, ecimLicensingInfo, neJobStaticData);
        } catch (final JobDataNotFoundException ex) {
            LOGGER.error("JobDataNotFoundException in {} License ", ACTIVITY_NAME, ex.getMessage());
            logMessage = ex.getMessage();
            handleException(activityJobId, logMessage, jobLogList, ecimLicensingInfo, neJobStaticData);
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred activityJobId {} with Reason : {}", ex);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ACTIVITY_NAME);
            if (!exceptionMessage.isEmpty()) {
                logMessage += String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
            }
            handleException(activityJobId, logMessage, jobLogList, ecimLicensingInfo, neJobStaticData);
        }
        LOGGER.debug("exit from execute of ecim Install {}", activityJobId);
    }

    private LicensePrecheckResponse getPrecheckResponse(final long activityJobId, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList,
            final NetworkElementData networkElement) {
        String logMessage = "";
        boolean isValidationSuccess = false;
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        String licenseKeyFileName = "";
        String keyFileMgmtMOFdn = "";
        try {
            final OssModelInfo ossModelInfo = ossModelInfoProvider.getOssModelInfo(networkElement.getNeType(), networkElement.getOssModelIdentity(), FragmentType.ECIM_LM_TYPE.getFragmentName());
            final String fingerprint = licenseMoService.getFingerPrintFromNode(networkElement);
            final String sequenceNumberFromNode = licenseMoServiceRetryProxy.getSequenceNumberFromNode(networkElement);
            final String sequenceNumber = licenseMoServiceRetryProxy.getSequenceNumber(fingerprint);
            final Map<String, Object> mainJobAttributes = jobConfigurationServiceRetryProxy.getMainJobAttributes(neJobStaticData.getMainJobId());
            final Map<String, Object> jobTemplateAttributes = jobConfigurationServiceRetryProxy.getPOAttributes((long) mainJobAttributes.get(ShmConstants.JOB_TEMPLATE_ID));
            final String jobCategoryAsString = (String) jobTemplateAttributes.get(ShmConstants.JOB_CATEGORY);
            final JobCategory jobCategory = JobCategory.getJobCategory(jobCategoryAsString);
            LOGGER.info("JobTemplateAttributes {} jobCategoryAsString {} and jobCategory {} of node with fingerprint {}", jobTemplateAttributes, jobCategoryAsString, jobCategory, fingerprint);
            if (ossModelInfo != null) {
                activityStepResultEnum = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
                logMessage = fragmentVersionCheck.checkFragmentVersion(FragmentType.ECIM_LM_TYPE, ossModelInfo.getReferenceMIMVersion());
                if (logMessage != null) {
                    activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                }
                licenseKeyFileName = licenseMoServiceRetryProxy.getLicenseKeyFileName(neJobStaticData, networkElement);
                keyFileMgmtMOFdn = licenseMoServiceRetryProxy.getNotifiableMoFdn(networkElement, ACTIVITY_NAME);
                if (NodeType.RADIONODE.getName().equalsIgnoreCase(networkElement.getNeType())) {
                    isValidationSuccess = validateLKF(jobLogList, licenseKeyFileName, neJobStaticData.getNodeName(), networkElement);
                    final String lastActionTriggered = getLastActionTriggered(activityJobId);
                    if (isValidationSuccess) {
                        if ("".equals(lastActionTriggered)) {
                            activityStepResultEnum = validateSequenceNumber(activityStepResultEnum, sequenceNumberFromNode, sequenceNumber, jobLogList, jobCategory);
                        }
                    } else {
                        activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
                    }
                } else {
                    logMessage = String.format(JobLogConstants.UNSUPPORTED_NODE_MODEL, networkElement.getNeType(), networkElement.getOssModelIdentity());
                    activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                    LOGGER.error("Precheck Failed for {} due to : {}", neJobStaticData.getNodeName(), logMessage);
                }
            }
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred for activityJobId {} with Reason:{}", activityJobId, ex);
            logMessage = ex.getMessage();
            activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        }
        final EcimLicensingInfo ecimLicensingInfo = new EcimLicensingInfo(keyFileMgmtMOFdn, licenseKeyFileName);
        return new LicensePrecheckResponse(ecimLicensingInfo, networkElement, activityStepResultEnum);
    }

    private ActivityStepResultEnum validateSequenceNumber(ActivityStepResultEnum activityStepResultEnum, final String sequenceNumberFromNode, final String sequenceNumber,
            final List<Map<String, Object>> jobLogList, final JobCategory jobCategory) {
        if ((jobCategory == JobCategory.CLI || jobCategory == JobCategory.FA) && (sequenceNumberFromNode != null && sequenceNumber != null)) {
            final int compare = sequenceNumber.compareTo(sequenceNumberFromNode);
            LOGGER.debug("Value of comparison between sequence number in ENM {} and sequence Number from node {} is {}", sequenceNumber, sequenceNumberFromNode, compare);
            if (compare <= 0) {
                activityStepResultEnum = ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION;
                jobLogUtil.prepareJobLogAtrributesList(jobLogList,
                        String.format(JobLogConstants.ACTIVITY_SKIP, ActivityConstants.INSTALL_LICENSE,
                                "sequence number in license file is less than or equal to the sequence number present on node."),
                        new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            }
        }
        return activityStepResultEnum;

    }

    private void executeMoAction(final long activityJobId, final boolean performfailsafeBackup, final NEJobStaticData neJobStaticData, final LicensePrecheckResponse licensePrecheckResponse)
            throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException {
        ManagedObject brmFailsafeBackupMo = null;
        final NetworkElementData networkElement = licensePrecheckResponse.getNetworkElement();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> propertyList = new ArrayList<Map<String, Object>>();
        final String lastActionTriggered = getLastActionTriggered(activityJobId);
        if (performfailsafeBackup) {
            final boolean emergencyUnlock = licenseMoServiceRetryProxy.getEmergencyUnlockActivationState(networkElement, neJobStaticData);
            LOGGER.debug("emergencyUnlock flag {}", emergencyUnlock);
            if ("".equals(lastActionTriggered) && emergencyUnlock) {
                brmFailsafeBackupMo = brmMoServiceRetryProxy.getBrmFailsafeBackupMo(neJobStaticData.getNodeName());
                if (brmFailsafeBackupMo != null) {
                    failsafeActivateDeactivateService.triggerBrmFailsafeActivateDeActivate(activityJobId, neJobStaticData, EcimCommonConstants.LicenseMoConstants.FAILSAFE_ACTIVATE_BACKUP,
                            getActivityInfo(activityJobId));
                } else {
                    LOGGER.debug("brmFailsafeBackupMo is notAvailable for node {} ", neJobStaticData.getNodeName());
                    triggerLicenseMoAction(neJobStaticData, activityJobId, licensePrecheckResponse, jobLogList, propertyList);
                }
            } else if (EcimCommonConstants.LicenseMoConstants.FAILSAFE_ACTIVATE_BACKUP.equalsIgnoreCase(lastActionTriggered)) {
                triggerLicenseMoAction(neJobStaticData, activityJobId, licensePrecheckResponse, jobLogList, propertyList);
            } else if (ACTIVITY_NAME.equalsIgnoreCase(lastActionTriggered)) {
                failsafeActivateDeactivateService.triggerBrmFailsafeActivateDeActivate(activityJobId, neJobStaticData, EcimCommonConstants.LicenseMoConstants.FAILSAFE_DEACTIVATE_BACKUP,
                        getActivityInfo(activityJobId));
            } else {
                triggerLicenseMoAction(neJobStaticData, activityJobId, licensePrecheckResponse, jobLogList, propertyList);
            }
        } else {
            LOGGER.info("performfailsafeBackup flag is {} for node{}", performfailsafeBackup, neJobStaticData.getNodeName());
            final long activityStartTime = neJobStaticData.getActivityStartTime();
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.EXECUTE);
            triggerLicenseMoAction(neJobStaticData, activityJobId, licensePrecheckResponse, jobLogList, propertyList);
        }
    }

    /**
     * 
     * This method is called when notification not received within the specified amount of timeout value. This method checks the action status i.e ongoing or success or failure. Accordingly update the
     * job logs and job result
     * 
     * @param activityJobId
     * @throws MoNotFoundException
     * 
     */
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        final ActivityStepResultEnum activityStepResultEnum = processTimeout(activityJobId);
        return activityUtils.getActivityStepResult(activityStepResultEnum);
    }

    public ActivityStepResultEnum processTimeout(final long activityJobId) {
        LOGGER.debug("Inside ecim license job install activity handleTimeout method with activityJob Id: {}", activityJobId);

        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        String logMessage = null;
        String keyFileMgmtMOFdn = null;
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> propertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobResultPropertyList = new ArrayList<Map<String, Object>>();
        final String lastActionTriggered = getLastActionTriggered(activityJobId);
        LOGGER.debug("lastActionTriggered in handleTimeout {}", lastActionTriggered);
        JobResult jobResult = JobResult.FAILED;
        EcimLicensingInfo ecimLicensingInfo = null;
        String errorMessage = "";
        String nodeName = "";
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            final NetworkElementData networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
            ecimLicensingInfo = ecimLmUtils.getLicensingInfo(activityJobId, neJobStaticData, networkElement);

            keyFileMgmtMOFdn = licenseMoServiceRetryProxy.getNotifiableMoFdn(networkElement, ACTIVITY_NAME);
            switch (lastActionTriggered) {
            case EcimCommonConstants.LicenseMoConstants.FAILSAFE_ACTIVATE_BACKUP:
            case EcimCommonConstants.LicenseMoConstants.FAILSAFE_DEACTIVATE_BACKUP:
                final AsyncActionProgress asyncActionProgress = brmMoServiceRetryProxy.getActionProgressOfBrmfailsafeMO(nodeName);
                final boolean progressReportflag = failsafeActivateDeactivateService.validateActionProgressReport(asyncActionProgress);
                LOGGER.warn("progressReportflag in handleTimeout {}", progressReportflag);
                if (!progressReportflag) {
                    jobResult = failsafeActivateDeactivateService.handleTimeoutForActivateDeactivateActivity(ecimLicensingInfo, activityJobId, nodeName, asyncActionProgress,
                            getActivityInfo(activityJobId));
                }
                break;
            case ACTIVITY_NAME:
                jobResult = handleTimeoutForInstallActivity(networkElement, activityJobId, neJobStaticData, ecimLicensingInfo);
                break;
            default:
                LOGGER.error("{} ActivityJobId {}, actionTriggeredProperty: {}", JobLogConstants.NO_ACTION_TRIGGERED, activityJobId, lastActionTriggered);
                logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ACTIVITY_NAME) + " because " + JobLogConstants.NO_ACTION_TRIGGERED;
                activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                activityUtils.prepareJobPropertyList(propertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, propertyList, jobLogList);
            }
        } catch (final UnsupportedFragmentException ex) {
            LOGGER.error("UnsupportedFragmentException occurred activityJobId {} with Reason : {}", activityJobId, ex);
            errorMessage = ex.getMessage();
            logMessage = String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName);
            jobResult = handleExceptionForTimeout(activityJobId, logMessage, errorMessage, jobLogList, propertyList);
        } catch (final MoNotFoundException ex) {
            LOGGER.error("MoNotFoundException occurred activityJobId {} with Reason : {}", activityJobId, ex);
            errorMessage = ex.getMessage();
            logMessage = String.format(JobLogConstants.BRMFAILSAFEBACKUP_NOT_FOUND, nodeName);
            jobResult = handleExceptionForTimeout(activityJobId, logMessage, errorMessage, jobLogList, propertyList);
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred for activityJobId {} with Reason : {}", activityJobId, ex);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            if (!exceptionMessage.isEmpty()) {
                errorMessage = exceptionMessage;
            } else {
                errorMessage = ex.getMessage();
            }
            logMessage = String.format(FAILURE, "handleTimeout", lastActionTriggered, nodeName);
            jobResult = handleExceptionForTimeout(activityJobId, logMessage, errorMessage, jobLogList, propertyList);
        }
        LOGGER.warn("handleTimeout-JobResult {},for Action {} ,isActivateTriggered() {}", jobResult.getJobResult(), lastActionTriggered, ecimLicensingInfo.isActivateTriggered());
        activityStepResultEnum = processTimeoutStatus(lastActionTriggered, jobResult, ecimLicensingInfo, activityJobId, keyFileMgmtMOFdn, jobResultPropertyList);

        if (EcimCommonConstants.LicenseMoConstants.FAILSAFE_DEACTIVATE_BACKUP.equalsIgnoreCase(lastActionTriggered)
                && (activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS
                        || activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.TIMEOUT_RESULT_FAIL)) {
            if (activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS) {
                final Map<String, Object> restrictionAttributes = ecimLmUtils.getRestrictionAttributes(ecimLicensingInfo);
                ecimLmUtils.persistLicenseInstalledTime(neJobStaticData, restrictionAttributes);
                ecimLmUtils.deleteLicenseKeyFile(neJobStaticData, restrictionAttributes);
            }
            final long activityStartTime = neJobStaticData.getActivityStartTime();
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobResultPropertyList, null);
        LOGGER.debug("Handletimeout completed in install license with activityJobId {} , nodeName {} and the result is {}", activityJobId, nodeName, activityStepResultEnum);
        return activityStepResultEnum;
    }

    private ActivityStepResultEnum processTimeoutStatus(final String lastActionTriggered, final JobResult jobResult, final EcimLicensingInfo ecimLicensingInfo, final long activityJobId,
            final String keyFileMgmtMOFdn, final List<Map<String, Object>> jobResultPropertyList) {
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        if (EcimCommonConstants.LicenseMoConstants.FAILSAFE_ACTIVATE_BACKUP.equalsIgnoreCase(lastActionTriggered) && jobResult == JobResult.SUCCESS) {
            activityStepResultEnum = ActivityStepResultEnum.REPEAT_EXECUTE;
        } else if (ACTIVITY_NAME.equalsIgnoreCase(lastActionTriggered) && jobResult == JobResult.SUCCESS && ecimLicensingInfo.isActivateTriggered()) {
            activityUtils.unSubscribeToMoNotifications(keyFileMgmtMOFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
            activityStepResultEnum = ActivityStepResultEnum.REPEAT_EXECUTE;
        } else if (ACTIVITY_NAME.equalsIgnoreCase(lastActionTriggered) && jobResult == JobResult.SUCCESS && !ecimLicensingInfo.isActivateTriggered()) {
            activityUtils.unSubscribeToMoNotifications(keyFileMgmtMOFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
            activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS;
        } else if (ACTIVITY_NAME.equalsIgnoreCase(lastActionTriggered) && jobResult == JobResult.FAILED && ecimLicensingInfo.isActivateTriggered()) {
            activityStepResultEnum = ActivityStepResultEnum.REPEAT_EXECUTE;
        } else if (EcimCommonConstants.LicenseMoConstants.FAILSAFE_DEACTIVATE_BACKUP.equalsIgnoreCase(lastActionTriggered) && jobResult == JobResult.SUCCESS
                && JobResult.SUCCESS.toString().equalsIgnoreCase(ecimLicensingInfo.getInstallStatus())) {
            activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult(), jobResultPropertyList);
            activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS;
        } else if (EcimCommonConstants.LicenseMoConstants.FAILSAFE_DEACTIVATE_BACKUP.equalsIgnoreCase(lastActionTriggered) && jobResult == JobResult.FAILED) {
            activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult(), jobResultPropertyList);
            activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        } else {
            activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString(), jobResultPropertyList);
            activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        }
        return activityStepResultEnum;
    }

    /**
     * @param ecimLicensingInfo
     * @return
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     */
    private JobResult handleTimeoutForInstallActivity(final NetworkElementData networkElement, final long activityJobId, final NEJobStaticData neJobStaticData,
            final EcimLicensingInfo ecimLicensingInfo) throws UnsupportedFragmentException, MoNotFoundException {
        final String nodeName = neJobStaticData.getNodeName();
        LOGGER.debug("Inside handleTimeoutForInstallActivity-activityJobId {}, nodeName {}", activityJobId, nodeName);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> propertyList = new ArrayList<Map<String, Object>>();
        JobResult jobResult = JobResult.FAILED;
        final String keyFileMgmtMOFdn = licenseMoServiceRetryProxy.getNotifiableMoFdn(networkElement, ACTIVITY_NAME);
        activityUtils.unSubscribeToMoNotifications(keyFileMgmtMOFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
        final AsyncActionProgress asyncActionProgress = licenseMoServiceRetryProxy.getActionProgressOfKeyFileMgmtMO(networkElement);
        activityUtils.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.TIMEOUT, ACTIVITY_NAME), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        if (asyncActionProgress != null) {
            if (asyncActionProgress.getActionId() != ecimLicensingInfo.getActionId()) {
                LOGGER.info("Handle timeout triggered but not related to activity {} with action Id : {} ", ACTIVITY_NAME, asyncActionProgress.getActionId());
            }
            jobResult = processReportProgress(asyncActionProgress, keyFileMgmtMOFdn, jobLogList, propertyList, neJobStaticData, activityJobId, ecimLicensingInfo);
        } else {
            LOGGER.error("Invalid actionProgress is : {}", asyncActionProgress);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_FAILED, ACTIVITY_NAME), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult(), propertyList);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, propertyList, jobLogList);
        return jobResult;
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        LOGGER.debug("Inside InstallLicenseKeyFileService cancel() with activityJobId:{}", activityJobId);

        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        activityUtils.logCancelledByUser(jobLogList, jobEnvironment, ActivityConstants.INSTALL_LICENSE);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList);

        return null;
    }

    /**
     * This method processes the notifications by fetching the notification subject and validates the notification. It de-register from the notification as it founds activity is completed and notifies
     * to WorkFlowService or else it will wait for another notification.
     * 
     * @param notification
     */
    @Override
    public void processNotification(final Notification notification) {
        LOGGER.debug("Entered ECIM - Install License - processNotification with event type : {}", notification.getNotificationEventType());
        if (!NotificationEventTypeEnum.AVC.equals(notification.getNotificationEventType())) {
            LOGGER.debug("ECIM - license install - Discarding non-AVC notification.");
            return;
        }
        LOGGER.debug("Inside ecim license job install activity processNotification()");
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        String errorMessage = null;
        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        final long activityJobId = activityUtils.getActivityJobId(notificationSubject);
        LOGGER.debug("Inside ecim license job install activity processNotification for activityJobId {}", activityJobId);
        String lastInvokedAction = "";
        String nodeName = "";
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            final NetworkElementData networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
            final EcimLicensingInfo ecimLicensingInfo = ecimLmUtils.getLicensingInfo(activityJobId, neJobStaticData, networkElement);
            lastInvokedAction = getLastActionTriggered(activityJobId);
            final Map<String, AttributeChangeData> modifiedAttributes = activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent());
            final String brmFailsafeBackupMoFdn = ((FdnNotificationSubject) notification.getNotificationSubject()).getFdn();
            LOGGER.info("processNotification with activityJobId :{} , nodeName : {},lastActionTriggered {},brmFailsafeBackupMoFdn{} and Modified Attributes are : {}", activityJobId, nodeName,
                    lastInvokedAction, brmFailsafeBackupMoFdn, modifiedAttributes);

            if (lastInvokedAction != null && !"".equals(lastInvokedAction)) {
                switch (lastInvokedAction) {
                case EcimCommonConstants.LicenseMoConstants.FAILSAFE_ACTIVATE_BACKUP:
                case EcimCommonConstants.LicenseMoConstants.FAILSAFE_DEACTIVATE_BACKUP:
                    processNotificationsForActivateDeactivate(ecimLicensingInfo, modifiedAttributes, neJobStaticData, brmFailsafeBackupMoFdn, activityJobId, networkElement);
                    break;
                case ACTIVITY_NAME:
                    processNotificationsForLicenseInstall(modifiedAttributes, ecimLicensingInfo, neJobStaticData, notificationSubject, activityJobId, networkElement);
                    break;
                default:
                    LOGGER.warn("None of the required actions are triggered. ActivityJobId {}, actionTriggeredProperty: {}", activityJobId, lastInvokedAction);
                }
            } else {
                LOGGER.warn("lastInvokedAction {} is Invalid for activityJobId: {}", activityJobId, lastInvokedAction);
            }
            activityUtils.recordEvent(SHMEvents.LICENSE_INSTALL_EXECUTE, nodeName, brmFailsafeBackupMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, lastInvokedAction));
        } catch (final UnsupportedFragmentException ex) {
            errorMessage = String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName);
            LOGGER.error("UnsupportedFragmentException occurred for activityJobId {} in InstallLicenseKeyFileService.processNotification(), Reason: {} ", activityJobId, ex);
            updateJobLogAndProperty(activityJobId, errorMessage, jobLogList, null);
        } catch (final MoNotFoundException ex) {
            errorMessage = ex.getMessage();
            LOGGER.error("MoNotFoundException occurred for activityJobId {} in InstallLicenseKeyFileService.processNotification(), Reason: {} ", activityJobId, ex);
            updateJobLogAndProperty(activityJobId, errorMessage, jobLogList, null);
        } catch (final Exception ex) {
            errorMessage = String.format(FAILURE, "processNotification", ACTIVITY_NAME, nodeName);
            LOGGER.error("Exception occurred for activityJobId {} in InstallLicenseKeyFileService.processNotification(), Reason: {} ", activityJobId, ex);
            updateJobLogAndProperty(activityJobId, errorMessage, jobLogList, null);
        }
        LOGGER.info("Exiting from ProcessNotification of license install with activity jobId {},nodeName {} and lastInvokedAction {}", activityJobId, nodeName, lastInvokedAction);
    }

    private boolean validateKeyFileDetails(final NEJobStaticData neJobStaticData, final LicensePrecheckResponse licensePrecheckResponse, final List<Map<String, Object>> jobLogList)
            throws UnsupportedFragmentException, MoNotFoundException {
        String logMessage = null;
        final String nodeName = neJobStaticData.getNodeName();
        final boolean isLicensePOsExists = licenseMoServiceRetryProxy.isLicensingPoExists(licensePrecheckResponse.getEcimLicenseInfo().getLicenseKeyFilePath());
        if (!isLicensePOsExists) {
            logMessage = String.format(JobLogConstants.ACTIVITY_FAILED + "Because \"%s\".", ActivityConstants.INSTALL, "License POs not found");
            activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            LOGGER.error("License POs not found for the node with name : {}", nodeName);
            return false;
        }
        final boolean keyFileExistsInSMRS = licenseMoServiceRetryProxy.isLicenseKeyFileExistsInSMRS(licensePrecheckResponse.getEcimLicenseInfo().getLicenseKeyFilePath());
        if (!keyFileExistsInSMRS) {
            logMessage = String.format(JobLogConstants.ACTIVITY_FAILED + "Because \"%s\".", ActivityConstants.INSTALL, "Key File not found in SMRS");
            activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            LOGGER.error("Key file not exists in SMRS for the node : {}", nodeName);
            return false;
        }
        return true;
    }

    private boolean performActionOnNode(final LicensePrecheckResponse licensePrecheckResponse, final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> propertyList,
            final NEJobStaticData neJobStaticData, final long activityJobId) throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
        String logMessage = null;
        String neType = null;
        boolean isActionTriggered = false;
        CommandPhase commandPhase = CommandPhase.FINISHED_WITH_ERROR;
        final String nodeName = neJobStaticData.getNodeName();
        final List<Map<String, Object>> installPropertyList = new ArrayList<Map<String, Object>>();
        short actionId = 0;
        neType = licensePrecheckResponse.getNetworkElement().getNeType();
        final String keyFileMgmtMOFdn = licensePrecheckResponse.getEcimLicenseInfo().getLicenseMoFdn();
        LOGGER.debug("keyFileMgmtMOFdn {} in performActionOnNode for node {} ", keyFileMgmtMOFdn, neJobStaticData.getNodeName());
        activityUtils.subscribeToMoNotifications(keyFileMgmtMOFdn, activityJobId, getActivityInfo(activityJobId));
        activityUtils.addJobProperty(EcimCommonConstants.LicenseMoConstants.LAST_ACTION_TRIGGERED, ACTIVITY_NAME, installPropertyList);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, installPropertyList, null);
        try {
            actionId = licenseMoServiceRetryProxy.executeMoAction(licensePrecheckResponse, neJobStaticData);
            LOGGER.debug("LicenseMoAction actionId is : {} ", actionId);
        } catch (final Exception exception) {
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(exception);
            if (!exceptionMessage.isEmpty()) {
                logMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ACTIVITY_NAME) + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
                activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            }
        }
        if (actionId > 0) {
            LOGGER.debug("{} activity is triggered ", ACTIVITY_NAME);
            final Integer activityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.ECIM.name(), JobTypeEnum.LICENSE.name(), ACTIVITY_NAME);
            logMessage = String.format(JobLogConstants.ASYNC_ACTION_TRIGGERED, ACTIVITY_NAME, activityTimeout);
            activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            final String licensActionId = String.valueOf(actionId);
            activityUtils.addJobProperty(EcimCommonConstants.ReportProgress.REPORT_PROGRESS_ACTION_ID, licensActionId, propertyList);
            commandPhase = CommandPhase.FINISHED_WITH_SUCCESS;
            isActionTriggered = true;
        } else {
            logMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ACTIVITY_NAME);
            activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult(), propertyList);
            commandPhase = CommandPhase.FINISHED_WITH_ERROR;
            LOGGER.error("Failed to trigger installKeyFile action on node : {} ", nodeName);

        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, propertyList, jobLogList);
        systemRecorder.recordCommand(SHMEvents.LICENSE_INSTALL_EXECUTE, commandPhase, nodeName, keyFileMgmtMOFdn,
                activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.LICENSE));
        activityUtils.recordEvent(SHMEvents.LICENSE_INSTALL_EXECUTE, nodeName, keyFileMgmtMOFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
        return isActionTriggered;
    }

    /**
     * @param ecimLicensingInfo
     * @param modifiedAttributes
     * @param nodeName
     */
    public void processNotificationsForActivateDeactivate(final EcimLicensingInfo ecimLicensingInfo, final Map<String, AttributeChangeData> modifiedAttributes, final NEJobStaticData neJobStaticData,
            final String brmFailsafeBackupMoFdn, final long activityJobId, final NetworkElementData networkElement) {
        String jobLogMessage = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        String nodeName = "";
        try {
            nodeName = neJobStaticData.getNodeName();
            final AsyncActionProgress asyncActionProgress = brmMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, EcimCommonConstants.LicenseMoConstants.FAILSAFE_ACTIVATE_BACKUP,
                    modifiedAttributes);
            final boolean progressReportflag = failsafeActivateDeactivateService.validateActionProgressReport(asyncActionProgress);
            LOGGER.debug("progressReportflag : {}", progressReportflag);
            if (progressReportflag) {
                LOGGER.warn("Discarding invalid notification,for the modifiedAttributes {}", modifiedAttributes);
                return;
            }
            failsafeActivateDeactivateService.handleProgressReportState(nodeName, ecimLicensingInfo, activityJobId, neJobStaticData, asyncActionProgress, brmFailsafeBackupMoFdn,
                    getActivityInfo(activityJobId));
        } catch (final MoNotFoundException e) {
            LOGGER.error("BrmBackupManagerMo not found for corresponding notification" + e);
            jobLogMessage = String.format(JobLogConstants.BRMFAILSAFEBACKUP_NOT_FOUND, nodeName);
            updateJobLogAndProperty(activityJobId, jobLogMessage, jobLogList, null);
        } catch (final UnsupportedFragmentException e) {
            LOGGER.error("Unsupported fragment for the corresponding notification recieved" + e);
            jobLogMessage = String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED);
            updateJobLogAndProperty(activityJobId, jobLogMessage, jobLogList, null);
        } catch (final Exception e) {
            LOGGER.error("Exception occured during processing of notifications for activate action with node name=" + nodeName + e);
            jobLogMessage = String.format(JobLogConstants.FAILURE_DUE_TO_EXCEPTION);
            updateJobLogAndProperty(activityJobId, jobLogMessage, jobLogList, null);
        }

    }

    private void processNotificationsForLicenseInstall(final Map<String, AttributeChangeData> modifiedAttributes, final EcimLicensingInfo ecimLicensingInfo, final NEJobStaticData neJobStaticData,
            final NotificationSubject notificationSubject, final long activityJobId, final NetworkElementData networkElement) throws UnsupportedFragmentException, MoNotFoundException {
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        final List<Map<String, Object>> propertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final String nodeName = neJobStaticData.getNodeName();
        final Map<String, Object> neJobAttributes = jobConfigurationServiceRetryProxy.getNeJobAttributes(neJobStaticData.getNeJobId());
        final String businessKey = (String) neJobAttributes.get(ShmConstants.BUSINESS_KEY);
        final AsyncActionProgress reportProgress = licenseMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, modifiedAttributes, ACTIVITY_NAME);
        if (reportProgress == null) {
            LOGGER.warn("Discarding invalid notification received nodeName {} and expected action is installKeyFile", nodeName);
            return;
        }
        final String keyFileMgmtMOFdn = licenseMoServiceRetryProxy.getNotifiableMoFdn(networkElement, ACTIVITY_NAME);
        final Map<String, Object> actionResult = ecimLmUtils.getActionStatus(reportProgress, ACTIVITY_NAME);
        final boolean isActionCompleted = (boolean) actionResult.get(ACTION_STATUS);
        LOGGER.debug("isActionCompleted  : {} ", isActionCompleted);
        activityUtils.prepareJobLogAtrributesList(jobLogList, reportProgress.toString(), notificationSubject.getTimeStamp(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, propertyList, jobLogList, reportProgress.getProgressPercentage());
        progressPercentageCache.bufferNEJobs(neJobStaticData.getNeJobId());
        if (isActionCompleted) {
            final JobResult jobResult = (JobResult) actionResult.get(ActivityConstants.JOB_RESULT);
            LOGGER.debug("JobResult from notification : {}", jobResult.getJobResult());
            activityUtils.unSubscribeToMoNotifications(keyFileMgmtMOFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
            updateJobResultProperties(keyFileMgmtMOFdn, actionResult, neJobStaticData, activityJobId);
            LOGGER.debug("isActivateTriggered : {}", ecimLicensingInfo.isActivateTriggered());
            if (ecimLicensingInfo.isActivateTriggered()) {
                LOGGER.debug("License ACTIVITY_REPEAT_EXECUTE: {} ", jobResult.getJobResult());
                activityUtils.addJobProperty(INSTALL_STATUS, jobResult.getJobResult().toString(), propertyList);
                processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, true);
            } else if ((jobResult == JobResult.SUCCESS && !ecimLicensingInfo.isActivateTriggered()) || (jobResult == JobResult.FAILED && !ecimLicensingInfo.isActivateTriggered())) {
                LOGGER.debug("License install jobResult : {} ", jobResult.getJobResult());
                if (jobResult == JobResult.SUCCESS) {
                    final Map<String, Object> restrictionAttributes = ecimLmUtils.getRestrictionAttributes(ecimLicensingInfo);
                    ecimLmUtils.deleteLicenseKeyFile(neJobStaticData, restrictionAttributes);
                }
                final long activityStartTime = neJobStaticData.getActivityStartTime();
                activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PROCESS_NOTIFICATION);
                activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult().toString(), propertyList);
            }
            final boolean isJobResultPersisted = jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, propertyList, null, null);
            if (isJobResultPersisted) {
                final boolean wfsActivated = activityUtils.sendActivateToWFS(businessKey, processVariables);

                if (!wfsActivated) {
                    LOGGER.debug("WFS not notified in install processNotification: {} ", wfsActivated);
                    activityUtils.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.WORKFLOW_SERVICE_INVOCATION_FAILED, reportProgress.getActionName()), new Date(),
                            JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                    jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, propertyList, jobLogList);
                }
            }

        }
        LOGGER.debug("Exit form handleNotificationForLicenseInstall  : {} ", isActionCompleted);
    }

    private void updateJobResultProperties(final String keyFileMgmtMOFdn, final Map<String, Object> actionResult, final NEJobStaticData neJobStaticData, final long activityJobId) {
        final List<Map<String, Object>> propertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final String nodeName = neJobStaticData.getNodeName();
        final String logmessage = (String) actionResult.get(ActivityConstants.JOB_LOG_MESSAGE);

        activityUtils.prepareJobLogAtrributesList(jobLogList, logmessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, propertyList, jobLogList, null);
        activityUtils.recordEvent(SHMEvents.LICENSE_INSTALL_PROCESS_NOTIFICATION, nodeName, keyFileMgmtMOFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logmessage));
    }

    private JobResult processReportProgress(final AsyncActionProgress reportProgress, final String keyFileMgmtMOFdn, final List<Map<String, Object>> jobLogList,
            final List<Map<String, Object>> propertyList, final NEJobStaticData neJobStaticData, final long activityJobId, final EcimLicensingInfo ecimLicensingInfo) {
        JobResult jobResult = JobResult.FAILED;
        JobLogLevel jobLogLevel = JobLogLevel.INFO;
        String jobLogMessage = "";
        final String nodeName = neJobStaticData.getNodeName();
        final Map<String, Object> actionResult = ecimLmUtils.getActionStatus(reportProgress, ACTIVITY_NAME);
        final boolean isActionCompleted = (boolean) actionResult.get(ACTION_STATUS);
        if (isActionCompleted && JobResult.SUCCESS.getJobResult().equals(((JobResult) actionResult.get(ActivityConstants.JOB_RESULT)).getJobResult())) {
            LOGGER.debug("In {} action handle timeout , action found to be successful for the activityJobId {} and nodeName {}. ", reportProgress.getActionName(), activityJobId, nodeName);
            jobLogLevel = (JobLogLevel) actionResult.get(ActivityConstants.JOB_LOG_LEVEL);
            jobLogMessage = (String) actionResult.get(ActivityConstants.JOB_LOG_MESSAGE);
            jobResult = (JobResult) actionResult.get(ActivityConstants.JOB_RESULT);
        } else {
            LOGGER.error("In {} action handle timeout , action found to be not yet completed, so Failing the activity now for the activityJobId {} and nodeName {}.", reportProgress.getActionName(),
                    activityJobId, nodeName);
            jobLogLevel = JobLogLevel.ERROR;
            jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED_IN_TIMEOUT, ACTIVITY_NAME, reportProgress.getResultInfo());
        }
        activityUtils.addJobProperty(INSTALL_STATUS, jobResult.getJobResult(), propertyList);
        activityUtils.recordEvent(SHMEvents.LICENSE_INSTALL_TIME_OUT, nodeName, keyFileMgmtMOFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, jobLogMessage));
        activityUtils.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), jobLogLevel.toString());
        LOGGER.debug("In Install handletimeout isActivateTriggered: {}", ecimLicensingInfo.isActivateTriggered());
        if (!ecimLicensingInfo.isActivateTriggered()) {
            activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult().toString(), propertyList);
        }
        return jobResult;
    }

    private void triggerLicenseMoAction(final NEJobStaticData neJobStaticData, final long activityJobId, final LicensePrecheckResponse licensePrecheckResponse,
            final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> propertyList) throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException {

        activityUtils.recordEvent(SHMEvents.LICENSE_INSTALL_EXECUTE, neJobStaticData.getNodeName(), licensePrecheckResponse.getEcimLicenseInfo().getLicenseMoFdn(),
                "SHM:" + activityJobId + ":" + neJobStaticData.getNodeName());
        systemRecorder.recordCommand(SHMEvents.LICENSE_INSTALL_EXECUTE, CommandPhase.STARTED, neJobStaticData.getNodeName(), licensePrecheckResponse.getEcimLicenseInfo().getLicenseMoFdn(),
                activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.LICENSE));
        final boolean isValidationSuccess = validateKeyFileDetails(neJobStaticData, licensePrecheckResponse, jobLogList);
        LOGGER.debug("is Validation Success {} for activityJobId {} ", isValidationSuccess, activityJobId);
        if (!isValidationSuccess) {
            handleException(activityJobId, String.format(VALIDATION_FAILURE, neJobStaticData.getNodeName()), jobLogList, licensePrecheckResponse.getEcimLicenseInfo(), neJobStaticData);
        }
        final boolean isActionTriggered = performActionOnNode(licensePrecheckResponse, jobLogList, propertyList, neJobStaticData, activityJobId);
        LOGGER.debug("isActionTriggered {} for activityJobId  {} ", isActionTriggered, activityJobId);
        if (!isActionTriggered) {
            handleException(activityJobId, String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ACTIVITY_NAME), jobLogList, licensePrecheckResponse.getEcimLicenseInfo(), neJobStaticData);
        }
    }

    /**
     * @param activityJobId
     * @param logMessage
     * @param jobLogList
     * @param ecimLicensingInfo
     */
    private void handleException(final long activityJobId, final String logMessage, final List<Map<String, Object>> jobLogList, final EcimLicensingInfo ecimLicensingInfo,
            final NEJobStaticData neJobStaticData) {
        if (neJobStaticData != null) {
            activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            LOGGER.debug("isActivateTriggered {} in handleException for activityJobId {} ", ecimLicensingInfo.isActivateTriggered(), activityJobId);
            if (ecimLicensingInfo.isActivateTriggered()) {
                failsafeActivateDeactivateService.triggerBrmFailsafeActivateDeActivate(activityJobId, neJobStaticData, EcimCommonConstants.LicenseMoConstants.FAILSAFE_DEACTIVATE_BACKUP,
                        getActivityInfo(activityJobId));
            }
            activityUtils.failActivity(activityJobId, jobLogList, neJobStaticData.getNeJobBusinessKey(), ACTIVITY_NAME);
        } else {
            LOGGER.error("Could not find NEJobStaticData in ECIM License Install HandleException for activityJobId {}", activityJobId);
        }
    }

    /**
     * @param activityJobId
     * @return
     */
    private JobActivityInfo getActivityInfo(final long activityJobId) {
        return activityUtils.getActivityInfo(activityJobId, InstallLicenseKeyFileService.class);
    }

    private JobResult handleExceptionForTimeout(final long activityJobId, final String jobLogMessage, final String errorMessage, final List<Map<String, Object>> jobLogList,
            final List<Map<String, Object>> propertyList) {
        LOGGER.error(errorMessage);
        activityUtils.prepareJobPropertyList(propertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
        updateJobLogAndProperty(activityJobId, jobLogMessage, jobLogList, propertyList);
        return JobResult.FAILED;

    }

    private String getLastActionTriggered(final long activityJobId) {
        final Map<String, Object> activityJobAttributes = jobConfigurationServiceRetryProxy.getActivityJobAttributes(activityJobId);
        LOGGER.debug("activityJobAttributes in getLastActionTriggered {}", activityJobAttributes);
        final String ActionTriggered = activityUtils.getActivityJobAttributeValue(activityJobAttributes, EcimCommonConstants.LicenseMoConstants.LAST_ACTION_TRIGGERED);
        LOGGER.debug("lastActionTriggered in  getLastActionTriggered {}", ActionTriggered);
        return ActionTriggered;
    }

    private void updateJobLogAndProperty(final long activityJobId, final String logMessage, final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> propertyList) {
        activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, propertyList, jobLogList);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancelTimeout(long)
     */
    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        // TODO Auto-generated method stub
        return new ActivityStepResult();
    }

    @Override
    @Asynchronous
    public void asyncHandleTimeout(final long activityJobId) {
        LOGGER.debug("Inside ecim License Install asyncHandleTimeout with activityJobID: {}", activityJobId);
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY);
            final ActivityStepResultEnum activityStepResultEnum = processTimeout(activityJobId);
            LOGGER.info("Sending back ActivityStepResult to WorkFlow from ecim InstallLicenseKeyFileService.asyncHandleTimeout with result:{} for node {} with activityJobId {} and neJobId {}",
                    activityStepResultEnum, neJobStaticData.getNodeName(), activityJobId, neJobStaticData.getNeJobId());
            activityUtils.buildProcessVariablesForTimeoutAndNotifyWfs(activityJobId, neJobStaticData, ACTIVITY_NAME, activityStepResultEnum);
        } catch (final Exception e) {
            LOGGER.error("An exception occurred while processing ecim License Install asyncHandleTimeout  with activityJobId: {}. Failure reason: ", activityJobId, e);
        }
    }

    @Override
    public void timeoutForAsyncHandleTimeout(final long activityJobId) {
        LOGGER.debug("Entered into InstallLicenseKeyFileService.timeoutForAsyncHandleTimeout for the activityJobId: {}", activityJobId);
        activityUtils.failActivityForHandleTimeoutExpiry(activityJobId, ACTIVITY_NAME);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity#asyncPrecheck(long)
     */
    @Override
    public void asyncPrecheck(final long activityJobId) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity# precheckHandleTimeout(long)
     */
    @Override
    public void precheckHandleTimeout(final long activityJobId) {
        // TODO Auto-generated method stub

    }

}
