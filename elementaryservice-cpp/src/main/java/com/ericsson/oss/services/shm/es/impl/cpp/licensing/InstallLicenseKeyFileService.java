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
package com.ericsson.oss.services.shm.es.impl.cpp.licensing;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.HANDLE_TIMEOUT_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.MOACTION_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.MOACTION_START_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_START_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.UPDATE_LICENSE_MOACTION_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.ACTION_ARG_IP_ADDRESS;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.ACTION_ARG_PASSWORD;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.ACTION_ARG_SOURCE_FILE;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.ACTION_ARG_USER_ID;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.ACTION_INSTALL_LICENSE;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.DATE_FORMAT;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.FINGER_PRINT;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.LAST_LICENSING_PI_CHANGE;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.LICENSE_DATA_FINGERPRINT;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.LICENSE_DATA_SEQUENCE_NUMBER;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.LICENSE_FILE_PATH;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.LICENSE_MO;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.NODE_NAME;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.PI_TIME_STAMP;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.utils.ExceptionParser;
import com.ericsson.oss.services.shm.common.modelservice.ProductTypeProviderImpl;
import com.ericsson.oss.services.shm.common.retry.DpsWriterRetryProxy;
import com.ericsson.oss.services.shm.common.smrs.SmrsAccountInfo;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.common.smrs.SmrsServiceConstants;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.AsynchronousActivity;
import com.ericsson.oss.services.shm.es.api.CommonLicensingActivityConstants;
import com.ericsson.oss.services.shm.es.api.DefaultActionRetryPolicy;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.common.ExecuteResponse;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.license.LicenseKeyFileDeleteService;
import com.ericsson.oss.services.shm.es.impl.license.LicensingRetryService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobConfigurationException;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobCategory;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.moaction.retry.ActionRetryPolicy;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.notifications.impl.license.LicenseUtil;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

/**
 * This class facilitates the installation of license key files of CPP based node by invoking the licensing MO action that initializes the install activity
 * 
 * @author xmanush
 */
@SuppressWarnings("PMD.TooManyFields")
@EServiceQualifier("CPP.LICENSE.install")
@ActivityInfo(activityName = "install", jobType = JobTypeEnum.LICENSE, platform = PlatformTypeEnum.CPP)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class InstallLicenseKeyFileService implements Activity, AsynchronousActivity {

    private static final Logger logger = LoggerFactory.getLogger(InstallLicenseKeyFileService.class);

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private SmrsFileStoreService smrsServiceUtil;

    @Inject
    private DpsWriterRetryProxy dpsWriterRetryProxy;

    @Inject
    private LicenseKeyFileDeleteService licenseKeyFileDeleteService;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private LicensingRetryService licensingRetryService;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    @DefaultActionRetryPolicy
    private ActionRetryPolicy moActionRetryPolicy;

    @Inject
    ProductTypeProviderImpl productTypeProviderImpl;

    @Inject
    private LicenseUtil licenseUtil;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculator;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceRetryProxy;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    /**
     * This method holds the validation for the license installation by a pre-action validation to decide if the current activity can be started or not and sends back the activity result to Work Flow
     * Service.
     * 
     */
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        logger.debug("Inside Install License Key File Service Precheck with activityJobID:{}", activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY);
            activityStepResultEnum = getPrecheckResult(activityJobId, neJobStaticData);
        } catch (final Exception e) {
            final String errorMsg = "An exception occured while processing precheck for cpp license install activity with activityJobId :{}" + activityJobId + ". Exception is: ";
            logger.error(errorMsg, e);
        }
        activityStepResult.setActivityResultEnum(activityStepResultEnum);
        logger.debug("Sending back Activity Step Result to WorkFlow in License Precheck with status:{}", activityStepResultEnum);
        return activityStepResult;
    }

    @SuppressWarnings("unchecked")
    private ActivityStepResultEnum getPrecheckResult(final long activityJobId, final NEJobStaticData neJobStaticData) throws MoNotFoundException {
        final long neJobId = neJobStaticData.getNeJobId();
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        String fingerPrint = "";
        String logMessage = null;
        String lkfProductType = "";
        final String nodeName = neJobStaticData.getNodeName();
        String neType = "";
        String jobLogMessage = "";

        try {
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            final String jobExecutedUser = activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId());
            // Obtaining the licensing MO Fdn
            final String licensingMoFdn = licensingRetryService.getLicenseMoFdn(activityJobId);

            if (licensingMoFdn == null) {
                jobLogMessage = String.format(JobLogConstants.MO_NOT_EXIST, ActivityConstants.INSTALL_LICENSE, LICENSE_MO);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                jobUpdateService.updateRunningJobAttributes(activityJobId, null, jobLogList);
                logger.debug("As licensingMoFdn is not available, Sending back ActivityStepResult from CPP install License Precheck to WorkFlow: activityStepResult:Status = {} of node {}",
                        activityStepResultEnum, nodeName);
                return activityStepResultEnum;
            }
            final String treatAsInfo = activityUtils.isTreatAs(nodeName);
            if (treatAsInfo != null) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, treatAsInfo, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            }

            final String fingerPrintValue = getFingerprintFromNode(activityJobId);

            // To obtain the restriction attributes while querying the DPS
            final NetworkElementData networkElementData = networkElementRetrievalBean.getNetworkElementData(nodeName);
            neType = networkElementData.getNeType();
            final Map<String, Object> mainJobAttributes = jobConfigurationServiceRetryProxy.getMainJobAttributes(neJobStaticData.getMainJobId());
            final Map<String, Object> restrictionAttributes = licensingRetryService.getRestrictedAttributesOfNode(neJobStaticData, neType, fingerPrintValue, mainJobAttributes);

            // Obtaining the license Data PO
            final List<Map<String, Object>> poAttributesList = licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes);
            if (poAttributesList == null || poAttributesList.isEmpty()) {
                final Map<String, Object> resultsMap = new HashMap<String, Object>();
                resultsMap.put(ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
                updateJogLogAttributesListAndRunningJobAttributes(nodeName, activityJobId, jobLogList, licensingMoFdn, JobLogConstants.LICENSE_KEY_FILE_NOT_FOUND, jobExecutedUser);
                logger.debug("As license po attributes are not available, sending back Activity Step Result to WorkFlow in Precheck of cpp install License  with status : {}", activityStepResultEnum);
                return activityStepResultEnum;
            }

            for (final Map<String, Object> poAttributes : poAttributesList) {
                // Obtaining license data finger print
                fingerPrint = (String) poAttributes.get(LICENSE_DATA_FINGERPRINT);
                lkfProductType = (String) poAttributes.get(CommonLicensingActivityConstants.PRODUCTTYPE);
            }

            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PROCESSING_PRECHECK, ActivityConstants.INSTALL_LICENSE), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            jobUpdateService.updateRunningJobAttributes(activityJobId, null, jobLogList);
            jobLogList.clear();

            //Validation check to precheck to fail if any RadioNode LKF trying to install on CPP nodes
            if (lkfProductType != null && licenseUtil.isRadioNodeLKF(lkfProductType, neType)) {
                logger.debug("ProductType value of LKF for node : {} ", lkfProductType, nodeName);
                logMessage = String.format(JobLogConstants.KEYFILE_MISMATCH_WITH_NETYPE, ActivityConstants.INSTALL_LICENSE, lkfProductType, nodeName);
                updateJogLogAttributesListAndRunningJobAttributes(nodeName, activityJobId, jobLogList, licensingMoFdn, logMessage, jobExecutedUser);
                logger.error("Sending back Activity Step Result to WorkFlow in License Precheck with status :{} as key file is not matching for the node :{} and activityJobId {}",
                        activityStepResultEnum, nodeName, activityJobId);
                return activityStepResultEnum;
            }
            // To set the pre-check status
            activityStepResultEnum = evaluatePrecheckStatus(activityJobId, fingerPrint, fingerPrintValue, nodeName, jobLogList, jobExecutedUser);
            if (activityStepResultEnum == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {
                activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
                activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(neJobId);
                activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.PRECHECK);
            } else {
                jobUpdateService.updateRunningJobAttributes(activityJobId, null, jobLogList);
            }
        } catch (final Exception e) {
            logger.error("Exception occured in License Precheck for node {} and activityJobId {} and reason {}", nodeName, activityJobId, e);
        }
        return activityStepResultEnum;
    }

    private void updateJogLogAttributesListAndRunningJobAttributes(final String nodeName, final long activityJobId, final List<Map<String, Object>> jobLogList, final String licensingMoFdn,
            final String logMessage, final String jobExecutionUser) {
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        jobUpdateService.updateRunningJobAttributes(activityJobId, null, jobLogList);
        // Recording the event
        activityUtils.recordEvent(jobExecutionUser, SHMEvents.LICENSE_INSTALL_PRECHECK, nodeName, licensingMoFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
    }

    private void initiateActivity(final long activityJobId, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList) {
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_INITIATED, ActivityConstants.INSTALL_LICENSE), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.INFO.toString());
        final String treatAsInfo = activityUtils.isTreatAs(neJobStaticData.getNodeName());
        if (treatAsInfo != null) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, treatAsInfo, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        }
        activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_START_PROGRESS_PERCENTAGE);
        activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
    }

    private LicenseInfo precheckValidation(final long activityJobId, final String nodeName, final List<Map<String, Object>> jobLogList, final NEJobStaticData neJobStaticData, final String neType)
            throws MoNotFoundException {
        boolean isPrecheckSuccess = false;
        boolean isPrecheckSkipped = false;
        String fingerPrint = "";
        String sequenceNumber = "";
        String lkfProductType = "";
        String logMessage = "";
        String licensingMoFdn = null;
        Map<String, Object> licenseDataAttributesMap = new HashMap<String, Object>();
        final long neJobId = neJobStaticData.getNeJobId();
        String jobExecutedUser = "";
        try {
            jobExecutedUser = activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId());
            licensingMoFdn = licensingRetryService.getLicenseMoFdn(activityJobId);
            if (licensingMoFdn != null) {

                final String fingerprintFromNode = getFingerprintFromNode(activityJobId);

                logger.debug("FingerPrint {} is available on Node {} in cpp license installation precheck validation", fingerprintFromNode, nodeName);
                // To obtain the restriction attributes while querying the DPS
                licenseDataAttributesMap = getFingerprintAndProductType(nodeName, neJobStaticData, jobLogList, activityJobId);
                if (!licenseDataAttributesMap.isEmpty()) {
                    fingerPrint = (String) licenseDataAttributesMap.get(LICENSE_DATA_FINGERPRINT);
                    sequenceNumber = (String) licenseDataAttributesMap.get(LICENSE_DATA_SEQUENCE_NUMBER);
                    lkfProductType = (String) licenseDataAttributesMap.get(CommonLicensingActivityConstants.PRODUCTTYPE);
                    logger.debug("Proceeding for Compare fingerprints on the Node [{}] and imported LKF's[{}]", fingerprintFromNode, fingerPrint);

                    final Map<String, Object> mainJobAttributes = jobConfigurationServiceRetryProxy.getMainJobAttributes(neJobStaticData.getMainJobId());
                    final Map<String, Object> jobTemplateAttributes = jobConfigurationServiceRetryProxy.getPOAttributes((long) mainJobAttributes.get(ShmConstants.JOB_TEMPLATE_ID));
                    final String jobCategoryAsString = (String) jobTemplateAttributes.get(ShmConstants.JOB_CATEGORY);
                    final JobCategory jobCategory = JobCategory.getJobCategory(jobCategoryAsString);
                    isPrecheckSkipped = validateSequenceNumber(fingerPrint, sequenceNumber, jobLogList, jobCategory);
                    if (isPrecheckSkipped) {
                        return new LicenseInfo(licensingMoFdn, isPrecheckSuccess, isPrecheckSkipped);
                    }

                    //Validation check to precheck to fail if any RadioNode LKF trying to install on CPP nodes
                    if (lkfProductType != null && licenseUtil.isRadioNodeLKF(lkfProductType, neType)) {
                        logger.info("ProductType {} of LKF for node {} and nodeType {} ", lkfProductType, nodeName, neType);
                        logMessage = String.format(JobLogConstants.KEYFILE_MISMATCH_WITH_NETYPE, ActivityConstants.INSTALL_LICENSE, lkfProductType, nodeName);
                        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                        activityUtils.recordEvent(jobExecutedUser, SHMEvents.LICENSE_INSTALL_PRECHECK, nodeName, licensingMoFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
                        logger.debug(" {} as key file is not matching for the node :{}", logMessage, nodeName);
                    } else {
                        isPrecheckSuccess = compareFingerprint(activityJobId, fingerPrint, fingerprintFromNode, nodeName, jobLogList, jobExecutedUser);
                    }
                } else {
                    logger.error("LicenseAttributes Notfound from DB {} for node {} and activityJobId {} ", licenseDataAttributesMap, nodeName, activityJobId);
                    isPrecheckSkipped = true;
                }
            } else {
                logMessage = String.format(JobLogConstants.MO_NOT_EXIST, ActivityConstants.INSTALL_LICENSE, LICENSE_MO);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                activityUtils.recordEvent(jobExecutedUser, SHMEvents.LICENSE_INSTALL_PRECHECK, nodeName, licensingMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
                logger.error("Precheck for Install License Key File is failed. Reason: Licensing MO not found for {}", nodeName);
            }
        } catch (final Exception exception) {
            logger.error("Exception occured in License Precheck for node {} and activityJobId {} and reason:", nodeName, activityJobId, exception);
            final String exceptionMessage = ExceptionParser.getMediationServiceExceptionReason(exception);
            logMessage = ActivityConstants.INSTALL_LICENSE + " Precheck failed." + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
            activityUtils.recordEvent(jobExecutedUser, SHMEvents.LICENSE_INSTALL_PRECHECK, nodeName, licensingMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
        activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(neJobId);
        activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.PRECHECK);
        return new LicenseInfo(licensingMoFdn, isPrecheckSuccess, isPrecheckSkipped);
    }

    private boolean validateSequenceNumber(final String fingerPrint, final String sequenceNumber, final List<Map<String, Object>> jobLogList, final JobCategory jobCategory) {
        boolean isPrecheckSkipped = false;
        if (jobCategory == JobCategory.CLI || jobCategory == JobCategory.FA) {
            final String nodeSequenceNumber = licensingRetryService.getNodeSequenceNumber(fingerPrint);
            logger.info("Comparison between sequence number for License PO {} and sequence Number from node {}", sequenceNumber, nodeSequenceNumber);
            if ((!nodeSequenceNumber.isEmpty() && !sequenceNumber.isEmpty()) && (Integer.parseInt(nodeSequenceNumber) >= (Integer.parseInt(sequenceNumber)))) {
                isPrecheckSkipped = true;
                jobLogUtil.prepareJobLogAtrributesList(jobLogList,
                        String.format(JobLogConstants.ACTIVITY_SKIP, ActivityConstants.INSTALL_LICENSE,
                                "sequence number in license file is less than or equal to the sequence number present on node."),
                        new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            }
        }
        return isPrecheckSkipped;
    }

    private boolean compareFingerprint(final long activityJobId, final String fingerPrint, final String fingerPrintValue, final String nodeName, final List<Map<String, Object>> jobLogList,
            final String jobExecutionUser) {
        boolean isValidationSuccess = false;
        final String jobLogMessage, logLevel;
        final int comparedResult = fingerPrintValue.compareTo(fingerPrint);
        if (comparedResult == 0) {
            logger.debug(JobLogConstants.FINGER_PRINT_MATCHED, "with compared result:", comparedResult);
            jobLogMessage = String.format(JobLogConstants.PRE_CHECK_SUCCESS, ActivityConstants.INSTALL_LICENSE, JobLogConstants.FINGER_PRINT_MATCHED);
            logLevel = JobLogLevel.INFO.toString();
            isValidationSuccess = true;
        } else {
            jobLogMessage = String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.INSTALL_LICENSE, JobLogConstants.FINGER_PRINT_MISMATCH);
            logLevel = JobLogLevel.ERROR.toString();
        }
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), logLevel);
        activityUtils.recordEvent(jobExecutionUser, SHMEvents.LICENSE_INSTALL_PRECHECK, nodeName, "", "SHM:" + activityJobId + ":" + nodeName + ":" + jobLogMessage);
        return isValidationSuccess;
    }

    private Map<String, Object> getFingerprintAndProductType(final String nodeName, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList, final long activityJobId)
            throws MoNotFoundException {
        String fingerPrint = "";
        String logMessage = null;
        String lkfProductType = "";
        String sequenceNumber = "";
        final Map<String, Object> licneseAttributes = new HashMap<String, Object>();
        final NetworkElementData networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
        final String fingerprintOnNode = getFingerprintFromNode(activityJobId);
        final Map<String, Object> mainJobAttributes = jobConfigurationServiceRetryProxy.getMainJobAttributes(neJobStaticData.getMainJobId());
        final Map<String, Object> restrictionAttributes = licensingRetryService.getRestrictedAttributesOfNode(neJobStaticData, networkElement.getNeType(), fingerprintOnNode, mainJobAttributes);

        // Obtaining the license Data PO
        final List<Map<String, Object>> poAttributesList = licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes);
        if (poAttributesList == null || poAttributesList.isEmpty()) {
            logMessage = "License Key file not found.";
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.recordEvent(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), SHMEvents.LICENSE_INSTALL_PRECHECK, nodeName, "",
                    "SHM:" + neJobStaticData.getNeJobId() + ":" + nodeName + ":" + logMessage);
            logger.error("{} for : {}", logMessage, nodeName);
            return licneseAttributes;
        }
        for (final Map<String, Object> poAttributes : poAttributesList) {
            // Obtaining license data finger print
            fingerPrint = (String) poAttributes.get(LICENSE_DATA_FINGERPRINT);
            sequenceNumber = (String) poAttributes.get(LICENSE_DATA_SEQUENCE_NUMBER);
            lkfProductType = (String) poAttributes.get(CommonLicensingActivityConstants.PRODUCTTYPE);
        }
        licneseAttributes.put(LICENSE_DATA_FINGERPRINT, fingerPrint);
        licneseAttributes.put(LICENSE_DATA_SEQUENCE_NUMBER, sequenceNumber);
        licneseAttributes.put(CommonLicensingActivityConstants.PRODUCTTYPE, lkfProductType);
        return licneseAttributes;
    }

    /**
     * This method is the activity execute step called when pre-check stage is passed. This registers for notifications, initiates and performs the MO action and sends back activity result to Work
     * Flow Service
     * 
     */
    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#execute(long)
     */
    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        logger.debug("Inside Install License Key File Service Execute phase with activityJobID:{}", activityJobId);
        String nodeName = "";
        NEJobStaticData neJobStaticData = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        JobStaticData jobStaticData = null;
        String logMessage = "";
        long activityStartTime = 0;
        String jobExecutionUser = "";
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            activityStartTime = neJobStaticData.getActivityStartTime();
            jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            //TBAC Validation\
            jobExecutionUser = activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId());
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ActivityConstants.INSTALL_LICENSE_ACTIVITY);
            if (!isUserAuthorized) {
                sendJobResultToWFS(activityJobId, jobLogList, neJobStaticData, JobResult.FAILED, jobPropertyList);
                return;
            }

            initiateActivity(activityJobId, neJobStaticData, jobLogList);//initiateActivity
            final NetworkElementData networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
            final LicenseInfo precheckResponse = precheckValidation(activityJobId, nodeName, jobLogList, neJobStaticData, networkElement.getNeType());//precheckValidation

            if (precheckResponse.isPrecheckSuccess()) {
                final ExecuteResponse executeResponse = performActionOnNode(activityJobId, neJobStaticData, precheckResponse, networkElement.getNeType()); //performActionOnNode
                doPostValidation(activityJobId, executeResponse, neJobStaticData);//post validation
            } else if (precheckResponse.isPrecheckSkipped()) {
                logger.debug("Skipping activity as trying to install lkf with less or same sequence number on Node {} or no lkf in ENM", nodeName);
                activityUtils.recordEvent(jobExecutionUser, SHMEvents.LICENSE_INSTALL_EXECUTE, nodeName, "", "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
                sendJobResultToWFS(activityJobId, jobLogList, neJobStaticData, JobResult.SKIPPED, jobPropertyList);
            } else {
                logger.debug("Failing activity as precheck failed for activityJobId {} and Node {}", activityJobId, nodeName);
                activityUtils.recordEvent(jobExecutionUser, SHMEvents.LICENSE_INSTALL_EXECUTE, nodeName, "", "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
                sendJobResultToWFS(activityJobId, jobLogList, neJobStaticData, JobResult.FAILED, jobPropertyList);
            }

        } catch (final Exception exception) {
            logger.error("Exception occured while triggering Install activity for {} and Node {} and Reason :", activityJobId, nodeName, exception);
            final String exceptionMessage = ExceptionParser.getMediationServiceExceptionReason(exception);
            logMessage = ActivityConstants.INSTALL_LICENSE + " Execute failed." + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
            activityUtils.recordEvent(jobExecutionUser, SHMEvents.LICENSE_INSTALL_EXECUTE, nodeName, "", "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            sendJobResultToWFS(activityJobId, jobLogList, neJobStaticData, JobResult.FAILED, jobPropertyList);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.EXECUTE);
    }

    /**
     * This method sets the Activity Result. Once the MO action is performed on the node, based on the status the result is set.
     * 
     */
    private void doPostValidation(final long activityJobId, final ExecuteResponse executeResponse, final NEJobStaticData neJobStaticData) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        String logMessage = "";
        JobResult jobResult = JobResult.FAILED;
        final String jobExecutedUser = activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId());
        if (executeResponse.isActionTriggered()) {
            systemRecorder.recordCommand(jobExecutedUser, SHMEvents.LICENSE_INSTALL_EXECUTE, CommandPhase.FINISHED_WITH_SUCCESS, neJobStaticData.getNodeName(), executeResponse.getFdn(),
                    Long.toString(neJobStaticData.getMainJobId()));
            logMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, ActivityConstants.INSTALL_LICENSE);
            jobResult = JobResult.SUCCESS;
            //Delete Old LKF's
            deleteLicenseKeyFile(activityJobId, neJobStaticData, executeResponse.getFdn());
        } else {
            systemRecorder.recordCommand(jobExecutedUser, SHMEvents.LICENSE_INSTALL_SERVICE, CommandPhase.FINISHED_WITH_ERROR, neJobStaticData.getNodeName(), executeResponse.getFdn(),
                    Long.toString(neJobStaticData.getMainJobId()));
            logMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.INSTALL_LICENSE);
        }
        logger.debug("The status of installation is:{} ", logMessage);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, null, MOACTION_END_PROGRESS_PERCENTAGE);
        activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
        activityUtils.recordEvent(jobExecutedUser, SHMEvents.LICENSE_INSTALL_EXECUTE, logMessage, executeResponse.getFdn(),
                "SHM:" + activityJobId + ":" + neJobStaticData.getNodeName() + ":" + CommandPhase.FINISHED_WITH_SUCCESS.toString());
        sendJobResultToWFS(activityJobId, jobLogList, neJobStaticData, jobResult, jobPropertyList);
    }

    private ExecuteResponse performActionOnNode(final long activityJobId, final NEJobStaticData neJobStaticData, final LicenseInfo licensePrecheckResponse, final String neType) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        updateProgresPecentageandLogs(activityJobId, neJobStaticData, jobLogList, jobPropertyList);
        final String licensingMoFdn = licensePrecheckResponse.getFdn();
        return executeActionAndgetResponse(activityJobId, neJobStaticData, licensePrecheckResponse, neType, jobLogList, jobPropertyList, licensingMoFdn);
    }

    private ExecuteResponse executeActionAndgetResponse(final long activityJobId, final NEJobStaticData neJobStaticData, final LicenseInfo licensePrecheckResponse, final String neType,
            final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> jobPropertyList, final String licensingMoFdn) {
        Map<String, Object> actionArguments = new HashMap<>();
        boolean performActionStatus = false;
        String logMessage = "";
        int actionId = -1;
        try {
            final String jobExecutedUser = activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId());
            actionArguments = prepareActionArguments(neJobStaticData, neType);
            actionId = dpsWriterRetryProxy.performAction(licensePrecheckResponse.getFdn(), ACTION_INSTALL_LICENSE, actionArguments, moActionRetryPolicy.getDpsMoActionRetryPolicy());

            logMessage = String.format(JobLogConstants.ACTION_TRIGGERED, ActivityConstants.INSTALL_LICENSE);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            systemRecorder.recordCommand(jobExecutedUser, SHMEvents.LICENSE_INSTALL_EXECUTE, CommandPhase.STARTED, neJobStaticData.getNodeName(), licensePrecheckResponse.getFdn(),
                    Long.toString(neJobStaticData.getMainJobId()));
            activityUtils.recordEvent(jobExecutedUser, SHMEvents.LICENSE_INSTALL_EXECUTE, neJobStaticData.getNodeName(), licensePrecheckResponse.getFdn(),
                    activityUtils.additionalInfoForEvent(activityJobId, neJobStaticData.getNodeName(), logMessage));
            activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, UPDATE_LICENSE_MOACTION_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
            performActionStatus = true;
            final long activityStartTime = neJobStaticData.getActivityStartTime();
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.EXECUTE);
            logger.debug("The actionId obtained after performing the action is:{}", actionId);
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, "true");
        } catch (final JobConfigurationException jobConfigurationException) {
            logger.error("Got Job configuration failure during execute action: ", jobConfigurationException);
            logMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.INSTALL_LICENSE) + ". "
                    + String.format(JobLogConstants.FAILURE_REASON, jobConfigurationException.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final Exception exception) {
            logger.error("Caught Exception during execute action: ", exception);
            final Throwable failureCause = exception.getCause();
            final String failureMessage = failureCause != null ? failureCause.getMessage() : exception.getMessage();
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(exception);
            if (!exceptionMessage.isEmpty()) {
                logMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.INSTALL_LICENSE) + ". " + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
            } else {
                logMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.INSTALL_LICENSE) + " because " + failureMessage;
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        final ExecuteResponse executeResponse = new ExecuteResponse(performActionStatus, licensingMoFdn, actionId);
        return executeResponse;
    }

    private void updateProgresPecentageandLogs(final long activityJobId, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList,
            final List<Map<String, Object>> jobPropertyList) {
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.EXECUTING, ActivityConstants.INSTALL_LICENSE), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.INFO.toString());
        activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, null, MOACTION_START_PROGRESS_PERCENTAGE);
        activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.LICENSE_INSTALLATION_SUCCESS, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
    }

    public String getLicenseMoFdn(final long activityJobId, final List<Map<String, Object>> jobPropertyList, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList) {
        String jobLogMessage = null;
        final Map<String, Object> licensingMoMap = licensingRetryService.getLicenseMoAttributes(activityJobId);
        final Map<String, Object> licenseMOAttributes = (Map<String, Object>) licensingMoMap.get(ShmConstants.MO_ATTRIBUTES);
        final String licensingMoFdn = (String) licensingMoMap.get(ShmConstants.FDN);
        activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, null, MOACTION_START_PROGRESS_PERCENTAGE);
        activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
        if (licensingMoFdn == null || licenseMOAttributes == null) {
            jobLogMessage = String.format(JobLogConstants.MO_NOT_EXIST, ActivityConstants.INSTALL_LICENSE, LICENSE_MO);
            logger.debug("License MO is null in Execute,Sending back ActivityStepResult to WorkFlow:{} ", JobResult.FAILED.getJobResult());
            //Persist Result as Failed in case of unable to trigger action.
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            sendJobResultToWFS(activityJobId, jobLogList, neJobStaticData, JobResult.FAILED, jobPropertyList);
            return null;
        }
        return licensingMoFdn;
    }

    public boolean getPerformActionStatus(final String licensingMoFdn, final Map<String, Object> actionArguments, final long activityJobId, final NEJobStaticData neJobStaticData,
            final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList) {
        // Obtaining the action ID after performing the action
        String logMessage = null;
        try {
            final int actionId = dpsWriterRetryProxy.performAction(licensingMoFdn, ACTION_INSTALL_LICENSE, actionArguments, moActionRetryPolicy.getDpsMoActionRetryPolicy());
            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.EXECUTE);
            activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, null, UPDATE_LICENSE_MOACTION_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
            logger.debug("The actionId obtained after performing the action is:{}", actionId);
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, "true");
            return true;
        } catch (final Exception exception) {
            final Throwable failureCause = exception.getCause();
            final String failureMessage = failureCause != null ? failureCause.getMessage() : exception.getMessage();
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(exception);
            if (!exceptionMessage.isEmpty()) {
                logMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.INSTALL_LICENSE) + ". " + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
            } else {
                logMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, ActivityConstants.INSTALL_LICENSE) + " because " + failureMessage;
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            sendJobResultToWFS(activityJobId, jobLogList, neJobStaticData, JobResult.FAILED, jobPropertyList);
            return false;
        }
    }

    private void sendJobResultToWFS(final long activityJobId, final List<Map<String, Object>> jobLogList, final NEJobStaticData neJobStaticData, final JobResult jobResult,
            final List<Map<String, Object>> jobPropertyList) {
        activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, jobResult.toString(), jobPropertyList);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, ShmConstants.WFS_ACTIVATE_EXECUTE, null);
    }

    /**
     * This method is called when instance receives time out while waiting for notification.This checks if activity was invoked already, then timeout is due to no notifications from node,then checks
     * the state on node to see if ongoing, failed or success else restarts the activity and logs it to job logs and also prepares the activity result.
     */
    /*
     * } (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#handleTimeout(long)
     */
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        logger.debug("Inside Install Licensekey file Service handle timeout phase with activityJobId:{}", activityJobId);

        NEJobStaticData neJobStaticData = null;
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY);
            activityStepResultEnum = handleTimeout(activityJobId, neJobStaticData);
        } catch (final JobDataNotFoundException e) {
            final String errorMsg = "An exception occured while processing handleTimeout for license install activity with activityJobId :" + activityJobId + ". Exception is: ";
            logger.error(errorMsg, e);
        }

        activityStepResult.setActivityResultEnum(activityStepResultEnum);
        logger.debug("Sending back ActivityStepResult to WorkFlow in License Handle Time Out with result:{} ", activityStepResultEnum);
        return activityStepResult;
    }

    @SuppressWarnings("unchecked")
    private ActivityStepResultEnum handleTimeout(final long activityJobId, final NEJobStaticData neJobStaticData) {
        ActivityStepResultEnum activityStepResultEnum = null;
        boolean activityStatus = false;
        String logMessage = null;
        String licensingMOFdn = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobAttributes = jobConfigurationServiceRetryProxy.getNeJobAttributes(neJobStaticData.getNeJobId());
        final String nodeName = (String) neJobAttributes.get(ShmConstants.NE_NAME);
        String jobExecutedUser = "";

        try {
            final Map<String, Object> licensingMoMap = licensingRetryService.getLicenseMoAttributes(activityJobId);
            final Map<String, Object> licenseMOAttributes = (Map<String, Object>) licensingMoMap.get(ShmConstants.MO_ATTRIBUTES);
            jobExecutedUser = activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId());

            if (licenseMOAttributes != null && !licenseMOAttributes.isEmpty()) {
                licensingMOFdn = (String) licensingMoMap.get(ShmConstants.FDN);
            } else {
                throw new MoNotFoundException("Licensing MO not found for node " + nodeName);
            }

            final String jobLogMessage = String.format(JobLogConstants.TIMEOUT, ActivityConstants.INSTALL_LICENSE);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString());
            // LastLicensingPiChange new value
            final String latestLastLicensingPiChange = (String) licenseMOAttributes.get(LAST_LICENSING_PI_CHANGE);
            // To Obtain the PI Time Stamp
            final String previousLastLicensingPiChange = activityUtils.getActivityJobAttributeValue(jobConfigurationServiceRetryProxy.getActivityJobAttributes(activityJobId), PI_TIME_STAMP);

            activityStatus = parseNotification(activityJobId, neJobStaticData, jobLogList, previousLastLicensingPiChange, latestLastLicensingPiChange, licensingMOFdn);
            if (!activityStatus) {
                logMessage = "Failing License key file Installation as it is taking more than expected time.";
                // Recording the event
                activityUtils.recordEvent(jobExecutedUser, SHMEvents.LICENSE_INSTALL_TIME_OUT, nodeName, licensingMOFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
            }

            if (activityStatus) {
                activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS;
                logMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, ActivityConstants.INSTALL_LICENSE);
                activityUtils.recordEvent(jobExecutedUser, SHMEvents.LICENSE_INSTALL_TIME_OUT, nodeName, licensingMOFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.getJobResult());
                activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, HANDLE_TIMEOUT_PROGRESS_PERCENTAGE);
                activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
                updateInstalledDateAndDeleteOldLicenseKeyFile(activityJobId, neJobStaticData, licensingMOFdn);

            } else {
                activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
                logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.INSTALL_LICENSE);
                activityUtils.recordEvent(jobExecutedUser, SHMEvents.LICENSE_INSTALL_TIME_OUT, nodeName, licensingMOFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
                jobUpdateService.updateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList);
                updateInstalledDateAndDeleteOldLicenseKeyFile(activityJobId, neJobStaticData, licensingMOFdn);

            }

        } catch (final MoNotFoundException moNotFoundException) {
            logger.error("Handle time out failed for node {} due to : {}", neJobStaticData.getNodeName(), moNotFoundException);
            logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.INSTALL_LICENSE) + String.format(JobLogConstants.FAILURE_REASON, moNotFoundException.getMessage());
            activityUtils.recordEvent(jobExecutedUser, SHMEvents.LICENSE_INSTALL_TIME_OUT, nodeName, licensingMOFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
        } catch (final Exception exception) {
            final String exceptionMessage = ExceptionParser.getMediationServiceExceptionReason(exception);
            logMessage = ActivityConstants.INSTALL_LICENSE + " handleTimeout failed." + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
            activityUtils.recordEvent(jobExecutedUser, SHMEvents.LICENSE_INSTALL_TIME_OUT, nodeName, licensingMOFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            logger.error("Handle time out failed for node {} due to :", neJobStaticData.getNodeName(), exception);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.HANDLE_TIMEOUT);
        logger.debug("Sending back ActivityStepResult to WorkFlow in License Handle Time Out with  Status:{}", activityStatus);
        return activityStepResultEnum;
    }

    /**
     * This method sets the PreCheckStatus based on the result obtained while comparing the fingerprints.
     * 
     */
    private ActivityStepResultEnum evaluatePrecheckStatus(final long activityJobId, final String fingerPrint, final String fingerPrintValue, final String nodeName,
            final List<Map<String, Object>> jobLogList, final String jobExecutionUser) {
        ActivityStepResultEnum activityStepResultEnum;
        String logMessage;
        final String jobLogMessage, logLevel;
        int comparedResult;
        // Comparing the fingerprints
        comparedResult = fingerPrintValue.compareTo(fingerPrint);
        if (comparedResult == 0) {
            logMessage = "Proceeding License Install Activity as FingerPrints match.";
            logger.debug(logMessage, "with compared result:", comparedResult);
            // setting the pre-check status
            activityStepResultEnum = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
            jobLogMessage = String.format(JobLogConstants.PRE_CHECK_SUCCESS, ActivityConstants.INSTALL_LICENSE);
            logLevel = JobLogLevel.INFO.toString();
        } else {
            logMessage = "Fingerprint of License Key file doesn't match with Node Fingerprint";
            // setting the pre-check status
            activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
            jobLogMessage = String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.INSTALL_LICENSE, "fingerprint of license key file doesn't match with node's fingerprint");
            logLevel = JobLogLevel.ERROR.toString();
        }
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), logLevel);
        // Recording the event
        activityUtils.recordEvent(jobExecutionUser, SHMEvents.LICENSE_INSTALL_PRECHECK, nodeName, "", "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
        return activityStepResultEnum;
    }

    /**
     * This method parses the notification, stores the job result based on the comparison result obtained while comparing the old and new time stamps of LastLicensingPiChange and returns the install
     * license activity status.
     * 
     */
    private boolean parseNotification(final long activityJobId, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList, final String previousLastLicensingPiChange,
            final String latestLastLicensingPiChange, final String licensingMOFdn) {
        boolean installLicenseActivityStatus = false;
        String logMessage = null;
        String logLevel = null;
        int comparisonResult = 0;
        String jobLogMessage = null;
        final List<Map<String, Object>> jobPropertiesList = new ArrayList<Map<String, Object>>();
        String jobResult = null;
        final String nodeName = neJobStaticData.getNodeName();
        //Logging the new LastLicensingPiChange Value
        logMessage = "The new LastLicensingPiChange Value obtained is: " + latestLastLicensingPiChange;
        // Recording the event
        final String jobExecutedUser = activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId());
        activityUtils.recordEvent(jobExecutedUser, SHMEvents.LICENSE_INSTALL_TIME_OUT, nodeName, licensingMOFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);

        // If previousLastLicensingPiChange is empty (or) null, then the result is set true and the activity status is set SUCCESS, as the job should not get failed
        if ("".equals(previousLastLicensingPiChange) || previousLastLicensingPiChange == null) {
            installLicenseActivityStatus = true;
            logMessage = "Install License Key File Activity is COMPLETED(but previous LastLicensingPiChange value is )" + previousLastLicensingPiChange;
            // Recording the event
            jobLogMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, ActivityConstants.INSTALL_LICENSE);
            jobResult = JobResult.SUCCESS.getJobResult();
            logLevel = JobLogLevel.INFO.toString();
            activityUtils.recordEvent(jobExecutedUser, SHMEvents.LICENSE_INSTALL_PROCESS_NOTIFICATION, nodeName, licensingMOFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
        } else {
            // Compares the old and new values of LastLicensingPiChange obtained from AVCNotification
            comparisonResult = compareTimeStamps(latestLastLicensingPiChange, previousLastLicensingPiChange);
            if (comparisonResult > 0) {
                // Stores Job Result as SUCCESS as both the values are equal
                installLicenseActivityStatus = true;
                jobResult = JobResult.SUCCESS.getJobResult();
                logMessage = "Install License Key File Activity is COMPLETED";
                logLevel = JobLogLevel.INFO.toString();
                jobLogMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, ActivityConstants.INSTALL_LICENSE);
                // Recording the event
                activityUtils.recordEvent(jobExecutedUser, SHMEvents.LICENSE_INSTALL_PROCESS_NOTIFICATION, nodeName, licensingMOFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
            } else {
                // Stores Job Result as FAILED as both the values are not-equal
                installLicenseActivityStatus = false;
                jobResult = JobResult.FAILED.getJobResult();
                logMessage = "Install License Key File Activity is FAILED";
                logLevel = JobLogLevel.ERROR.toString();
                jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.INSTALL_LICENSE);
                // Recording the event
                activityUtils.recordEvent(jobExecutedUser, SHMEvents.LICENSE_INSTALL_PROCESS_NOTIFICATION, nodeName, licensingMOFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
            }
        }
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), logLevel);
        logger.debug("The results of modifiedAttributesMap:{}", jobResult);
        activityUtils.prepareJobPropertyList(jobPropertiesList, ActivityConstants.ACTIVITY_RESULT, jobResult);

        // Persisting into job Logs
        jobUpdateService.updateRunningJobAttributes(activityJobId, jobPropertiesList, jobLogList);
        logger.debug("The results are persisted in Activity Job Property!");

        // Recording the event
        activityUtils.recordEvent(jobExecutedUser, SHMEvents.LICENSE_INSTALL_PROCESS_NOTIFICATION, nodeName, licensingMOFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
        return installLicenseActivityStatus;
    }

    /**
     * Executes and the "cancel" message is sent to a work flow instance
     * 
     */
    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancel(long)
     */
    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        logger.debug("Inside InstallLicenseKeyFileService cancel() with activityJobId:{}", activityJobId);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY);
        } catch (final JobDataNotFoundException e) {
            final String errorMsg = "An exception occured while processing cancel for license install activity with activityJobId :" + activityJobId + ". Exception is: ";
            logger.error(errorMsg, e);
        }
        activityUtils.logCancelledByUser(jobLogList, neJobStaticData, ActivityConstants.INSTALL_LICENSE);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.CANCEL_NOT_POSSIBLE, ActivityConstants.INSTALL_LICENSE), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.WARN.toString());
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_CANCEL_TRIGGERED, "true");

        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);

        // Return type will be changed to void after making cancel() asynchronous.
        return new ActivityStepResult();
    }

    /**
     * This method returns the action arguments that are used during execute phase that are used while performing the MO action
     * 
     * @throws JobConfigurationException
     * 
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> prepareActionArguments(final NEJobStaticData neJobStaticData, final String neType) throws JobConfigurationException {
        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        // Obtaining details from SMRS file store
        final SmrsAccountInfo smrsDetails = smrsServiceUtil.getSmrsDetails(SmrsServiceConstants.LICENCE_ACCOUNT, neType, neJobStaticData.getNodeName());
        final String ftpServerIpAddress = smrsDetails.getServerIpAddress();
        final String ftpServerUserId = smrsDetails.getUser();
        final String ftpServerPassword = new String(smrsDetails.getPassword());
        actionArguments.put(ACTION_ARG_USER_ID, ftpServerUserId);
        actionArguments.put(ACTION_ARG_PASSWORD, ftpServerPassword);
        actionArguments.put(ACTION_ARG_IP_ADDRESS, ftpServerIpAddress);
        // To obtain Source file(License file path)
        final Map<String, Object> mainJobAttributes = jobConfigurationServiceRetryProxy.getMainJobAttributes(neJobStaticData.getMainJobId());
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
        final List<Map<String, Object>> neJobPropertyList = (List<Map<String, Object>>) jobConfigurationDetails.get(ActivityConstants.NE_JOB_PROPERTIES);

        String licenseKeyFilePath = null;
        List<Map<String, String>> jobPropertyList;
        // fetching the source file from jobProperties
        for (final Map<String, Object> neJobProperty : neJobPropertyList) {
            if (neJobProperty.get(NODE_NAME).equals(neJobStaticData.getNodeName())) {
                jobPropertyList = (List<Map<String, String>>) neJobProperty.get(ActivityConstants.JOB_PROPERTIES);
                for (final Map<String, String> jobProperty : jobPropertyList) {
                    if (LICENSE_FILE_PATH.equals(jobProperty.get(ShmConstants.KEY))) {
                        licenseKeyFilePath = jobProperty.get(ShmConstants.VALUE);
                    }
                }
            }
        }
        logger.debug("LicenseKeyfilePath from MainJob {}", licenseKeyFilePath);
        // fetch licenseKeyFilePath  from NE Job properties if not found in Main Job
        if (licenseKeyFilePath == null) {
            licenseKeyFilePath = licensingRetryService.getLicenseKeyFilePathFromNeJob(neJobStaticData.getNeJobId());
        }
        if (licenseKeyFilePath == null) {
            throw new JobConfigurationException("License key file path not found in Job configuration.");
        }
        if (licenseKeyFilePath.contains(smrsDetails.getSmrsRootDirectory())) {
            licenseKeyFilePath = licenseKeyFilePath.replace(smrsDetails.getSmrsRootDirectory(), "");
            logger.info("The licensekeyfilepath is:{}", licenseKeyFilePath);
        } else {
            logger.debug("There is no SMRS root path starting with /home/smrs and the obtained path is:{}", licenseKeyFilePath);
        }
        actionArguments.put(ACTION_ARG_SOURCE_FILE, licenseKeyFilePath);
        return actionArguments;
    }

    /**
     * This method holds the comparison logic for the latest and the previous lastLicensingPiChange attribute that is present in License where the value is of the format : 140916-043923, which is
     * interpreted as yymmdd-hhmmss. Formats the object as String in <b>yyyy-MM-dd HH:mm:ss</b> format and parses to <code>java.util.Date</code> format.
     * 
     * @return compared result of the timeStamps
     */
    private int compareTimeStamps(final String latestLastLicensingPiChange, final String previousLastLicensingPiChange) {
        Date previousDate = null;
        Date latestDate = null;
        int compare = 0;

        final StringBuffer oldTimeStamp = new StringBuffer(previousLastLicensingPiChange).replace(6, 7, " ").insert(2, '-').insert(5, '-').insert(11, ':').insert(14, ':');
        final StringBuffer newTimeStamp = new StringBuffer(latestLastLicensingPiChange).replace(6, 7, " ").insert(2, '-').insert(5, '-').insert(11, ':').insert(14, ':');

        final SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);

        try {
            previousDate = formatter.parse(oldTimeStamp.toString());
            latestDate = formatter.parse(newTimeStamp.toString());
            compare = latestDate.compareTo(previousDate);
        } catch (final ParseException e) {
            logger.error("Unable to parse. Reason : {}", e);
        }
        return compare;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancelTimeout(long)
     */
    @SuppressWarnings("unchecked")
    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY);
            final Map<String, Object> licensingMoMap = licensingRetryService.getLicenseMoAttributes(activityJobId);
            final String licensingMOFdn = (String) licensingMoMap.get(ShmConstants.FDN);
            final Map<String, Object> licenseMOAttributes = (Map<String, Object>) licensingMoMap.get(ShmConstants.MO_ATTRIBUTES);
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            // LastLicensingPiChange new value
            final String latestLastLicensingPiChange = (String) licenseMOAttributes.get(LAST_LICENSING_PI_CHANGE);
            // To Obtain the PI Time Stamp
            final String previousLastLicensingPiChange = activityUtils.getActivityJobAttributeValue(jobConfigurationServiceRetryProxy.getActivityJobAttributes(activityJobId), PI_TIME_STAMP);
            final boolean status = parseNotification(activityJobId, neJobStaticData, jobLogList, previousLastLicensingPiChange, latestLastLicensingPiChange, licensingMOFdn);
            if (status) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
            } else {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
            }
        } catch (final JobDataNotFoundException e) {
            logger.error("Exception occurred in Install License Key File Service cancelTimeout is {} with activityJobID : {}", e, activityJobId);
        }
        return activityStepResult;
    }

    /**
     * This method updates the installedOn value in the LicenseData PO when the license key file is installed
     */
    private void persistLicenseInstalledTime(final long activityJobId, final NEJobStaticData neJobStaticData, final String licensingMOFdn) {
        logger.debug("Entering into persistLicenseInstalledTime with nodeName : {} and activityJobId : {}", neJobStaticData.getNodeName(), activityJobId);
        final String nodeName = neJobStaticData.getNodeName();
        String neType = "";

        try {
            neType = networkElementRetrievalBean.getNeType(nodeName);
        } catch (final Exception e) {
            logger.error("Exception occurred in cpp persistLicenseInstalledTime on node {} and activityJobId {} is {}", nodeName, activityJobId, nodeName);
        }
        // To obtain the restriction attributes

        final String fingerprintOnNode = getFingerprintFromNode(activityJobId);
        final Map<String, Object> mainJobAttributes = jobConfigurationServiceRetryProxy.getMainJobAttributes(neJobStaticData.getMainJobId());

        final Map<String, Object> restrictionAttributes = licensingRetryService.getRestrictedAttributesOfNode(neJobStaticData, neType, fingerprintOnNode, mainJobAttributes);
        final boolean isUpdateSuccess = licensingRetryService.updateLicenseInstalledTime(restrictionAttributes);
        logger.debug("Installed date updated in LicenseData PO : {} ", isUpdateSuccess);
        if (!isUpdateSuccess) {
            final String logMessage = "Updating of InstalledOn attribute got failed because LicenseData PO does not exists in the DPS.";
            logger.debug(logMessage);
            // Recording the event
            activityUtils.recordEvent(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), SHMEvents.LICENSE_INSTALL_PROCESS_NOTIFICATION, nodeName, licensingMOFdn,
                    "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
        }

    }

    /**
     * This method deletes the License Key Files.If there are License Key files that are already installed with the same FingerPrint and lesser Sequence Number than the License Key file that to be
     * installed, then the historic license key files are deleted from the DPS and also from SMRS location. logs in job logs.
     */
    private void deleteLicenseKeyFile(final long activityJobId, final NEJobStaticData neJobStaticData, final String licensingMOFdn) {
        final String nodeName = neJobStaticData.getNodeName();
        logger.debug("Entering into deleteLicenseKeyFile with nodeName : {} and activityJobId : {}", nodeName, activityJobId);
        String neType = "";
        try {
            neType = networkElementRetrievalBean.getNeType(nodeName);
        } catch (final MoNotFoundException moNotFoundException) {
            logger.error("Mo Not found for the node name: {} and activityJobId: {}", nodeName, activityJobId);
        }
        // To obtain the restriction attributes

        final String fingerprintOnNode = getFingerprintFromNode(activityJobId);
        final Map<String, Object> mainJobAttributes = jobConfigurationServiceRetryProxy.getMainJobAttributes(neJobStaticData.getMainJobId());
        final Map<String, Object> restrictionAttributes = licensingRetryService.getRestrictedAttributesOfNode(neJobStaticData, neType, fingerprintOnNode, mainJobAttributes);
        // get the licenseData PO
        final List<Map<String, Object>> poAttributesList = licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes);
        final String jobExecutedUser = activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId());
        if (poAttributesList.isEmpty()) {
            final String logMessage = "There is no data present in the database.";
            logger.debug("The reason is:{}", logMessage);
            // Recording the event
            activityUtils.recordEvent(jobExecutedUser, SHMEvents.LICENSE_INSTALL_PROCESS_NOTIFICATION, nodeName, licensingMOFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
        }
        for (final Map<String, Object> poAttributes : poAttributesList) {
            // obtaining license data key file details for the deletion of PO
            final String fingerPrint = (String) poAttributes.get(LICENSE_DATA_FINGERPRINT);
            final String sequenceNumber = (String) poAttributes.get(LICENSE_DATA_SEQUENCE_NUMBER);
            final String logMessage = licenseKeyFileDeleteService.deleteHistoricLicensePOs(fingerPrint, sequenceNumber);
            logger.debug("The delete result is : {}", logMessage);
            // Recording the event
            activityUtils.recordEvent(jobExecutedUser, SHMEvents.LICENSE_INSTALL_DELETE, nodeName, licensingMOFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
        }
    }

    /**
     * Updates the installed Date in LicenseData PO and delete license key files which are having less sequence number than currently installed license.
     */
    private void updateInstalledDateAndDeleteOldLicenseKeyFile(final long activityJobId, final NEJobStaticData neJobStaticData, final String licensingMoFdn) {

        persistLicenseInstalledTime(activityJobId, neJobStaticData, licensingMoFdn);

        deleteLicenseKeyFile(activityJobId, neJobStaticData, licensingMoFdn);
    }

    @Override
    @Asynchronous
    public void asyncPrecheck(final long activityJobId) {
        logger.debug("Inside Install License Key File Service AsyncPrecheck with activityJobID : {}", activityJobId);
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        long neJobId = 0;
        String nodeName = "";
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY);
            neJobId = neJobStaticData.getNeJobId();
            nodeName = neJobStaticData.getNodeName();
            activityUtils.buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.INSTALL_LICENSE, activityStepResultEnum);
            activityStepResultEnum = getPrecheckResult(activityJobId, neJobStaticData);
        } catch (final Exception e) {
            final String errorMsg = "An exception occured while processing async precheck for install activity with activityJobId :" + activityJobId + ". Exception is: ";
            logger.error(errorMsg, e);
        }
        logger.info("Sending back Activity Step Result to WorkFlow in License Precheck with status : {} for node {} with activityJobId {} and neJobId {}", activityStepResultEnum, nodeName,
                activityJobId, neJobId);
        activityUtils.failActivity(activityJobId, null, null, ActivityConstants.INSTALL_LICENSE);
    }

    @Override
    public void precheckHandleTimeout(final long activityJobId) {
        logger.debug("Inside Install License Key File Service AsyncPrecheckTimeout with activityJobID : {}", activityJobId);
        activityUtils.failActivityForPrecheckTimeoutExpiry(activityJobId, ActivityConstants.INSTALL_LICENSE);
    }

    @Override
    @Asynchronous
    public void asyncHandleTimeout(final long activityJobId) {
        logger.debug("Inside Install Licensekey file Service Async handle timeout phase with activityJobId:{}", activityJobId);
        long neJobId = 0;
        String neName = "";
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY);
            neJobId = neJobStaticData.getNeJobId();
            neName = neJobStaticData.getNodeName();
            activityStepResultEnum = handleTimeout(activityJobId, neJobStaticData);
        } catch (final JobDataNotFoundException e) {
            final String errorMsg = "An exception occured while processing asyncHandleTimeout for license install activity with activityJobId :" + activityJobId + ". Exception is: ";
            logger.error(errorMsg, e);
        }
        logger.info("Sending back ActivityStepResult to WorkFlow in License Handle Time Out with result:{} for node {} with activityJobId {} and neJobId {}", activityStepResultEnum, neName,
                activityJobId, neJobId);
        activityUtils.buildProcessVariablesForTimeoutAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.INSTALL_LICENSE, activityStepResultEnum);
    }

    @Override
    public void timeoutForAsyncHandleTimeout(final long activityJobId) {
        logger.debug("Inside Install License Key File Service timeoutForAsyncHandleTimeout with activityJobID : {}", activityJobId);
        activityUtils.failActivityForHandleTimeoutExpiry(activityJobId, ActivityConstants.INSTALL_LICENSE);
    }

    private String getFingerprintFromNode(final long activityJobId) {
        String fingerPrintValue = "";
        final Map<String, Object> licensingMoMap = licensingRetryService.getLicenseMoAttributes(activityJobId);
        if (licensingMoMap != null && !licensingMoMap.isEmpty() && licensingMoMap.get(ShmConstants.MO_ATTRIBUTES) != null) {
            final Map<String, Object> licenseMOAttributes = (Map<String, Object>) licensingMoMap.get(ShmConstants.MO_ATTRIBUTES);
            fingerPrintValue = (String) licenseMOAttributes.get(FINGER_PRINT);
        }
        return fingerPrintValue;
    }
}
