/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.cpp.deleteup.handlers;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.EXECUTE_REPEAT_PROGRESS_PERCENTAGE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.DeleteUpHelper;
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.DeleteUpgradePackageActionInfo;
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.DeleteUpgradePackageUtil;
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.UpgradePackageDeletePreventingInfoProvider;
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.persistjobdata.PersistJobData;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

@RequestScoped
@Traceable
@SuppressWarnings("PMD.TooManyFields")
public class ExecuteHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteHandler.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    @Inject
    private DeleteUpHelper deleteUpHelper;

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Inject
    private PersistJobData persistJobData;

    @Inject
    private DeleteCvHandler deleteCvHandler;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private UpgradePackageDeletePreventingInfoProvider upgradePackageDeletePreventingInfoProvider;

    @Inject
    private PreCheckHandler preCheckHandler;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    @Inject
    private DeleteUpgradePackageUtil deleteUpgradePackageUtil;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceProxy;

    private String nodeName = null;
    private long neJobId = 0;
    private String neType = null;
    private String platformType = null;
    private String recordingEvent = null;
    private JobEnvironment jobEnvironment = null;
    private boolean isRemoveFromRollbackListSelected = false;

    private int deleteUpResult = 0;
    private String[] userProvidedUPs = null;
    private NEJobStaticData neJobStaticData = null;
    private NetworkElementData networkElement = null;
    private String upMoFdn = null;

    final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
    final List<Map<String, Object>> jobProperties = new ArrayList<Map<String, Object>>();
    private JobResult jobResult = null;
    final Map<String, Object> processVariables = new HashMap<String, Object>();

    public void execute(final long activityJobId, final JobActivityInfo jobActivityInfo) {
        LOGGER.debug("Entered in ExecuteHandler execute() Activity with activityJobId {}", activityJobId);
        try {
            initializeVariables(activityJobId);
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.IS_ACTIVITY_TRIGGERED, "true");
            final boolean isPrecheckAlreadyDone = isPrecheckDone();
            if (!isPrecheckAlreadyDone) {
                final ActivityStepResult activityStepResult = preCheckHandler.performPreCheck(activityJobId);
                if (activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION) {
                    final String businessKey = neJobStaticData.getNeJobBusinessKey();
                    activityUtils.skipActivity(activityJobId, neJobStaticData, jobLogs, businessKey, ShmConstants.DELETEUPGRADEPKG_ACTIVITY);
                    return;
                } else if (activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION) {
                    final String businessKey = neJobStaticData.getNeJobBusinessKey();
                    activityUtils.failActivity(activityJobId, jobLogs, businessKey, ShmConstants.DELETEUPGRADEPKG_ACTIVITY);
                    return;
                }
            }
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.IS_PRECHECK_DONE, ActivityConstants.CHECK_TRUE);
            final int processedUPs = getCountOfProcessedUPs(jobEnvironment.getActivityJobAttributes());

            final DeleteUpgradePackageActionInfo deleteUPActionInfo = getUpgradePackageToBeDeleted(activityJobId, processedUPs);
            final String suppliedProductNumber = deleteUPActionInfo.getProductNumber();
            final String suppliedProductRevision = deleteUPActionInfo.getProductRevision();

            final Map<String, Object> activityJobAttributes = activityUtils.getPoAttributes(activityJobId);
            final Map<String, HashMap<String, Object>> preventingUpsAndCvs = upgradePackageDeletePreventingInfoProvider.findPreventiveUpsAndCvsMap(activityJobAttributes);

            final Map<String, Object> deltaUps = upgradePackageDeletePreventingInfoProvider.getLowestPossibleDeltaUP(preventingUpsAndCvs, suppliedProductNumber, suppliedProductRevision);
            LOGGER.debug("The lowest possible Deltaup found is: {}", deltaUps);
            String logMessage = "";
            if (!deltaUps.isEmpty()) {
                final String deltaUPProductNumber = (String) deltaUps.get(UpgradeActivityConstants.PRODUCT_NUMBER);
                final String deltaUPProductRevision = (String) deltaUps.get(UpgradeActivityConstants.PRODUCT_REVISION);
                final String cvName = upgradePackageDeletePreventingInfoProvider.getAndRemoveReferredCVOccurences(activityJobId, neJobId, preventingUpsAndCvs, deltaUPProductNumber,
                        deltaUPProductRevision);

                if (cvName != null) {
                    logMessage = String.format(JobLogConstants.DELETING_CV_FOR_DELTAUP, cvName, deltaUPProductNumber, deltaUPProductRevision);
                    jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                    deleteCv(activityJobId, deltaUPProductNumber, deltaUPProductRevision, preventingUpsAndCvs, cvName);
                } else {
                    upgradePackageDeletePreventingInfoProvider.removeDeltaUPOccurences(activityJobId, neJobId, preventingUpsAndCvs, deltaUPProductNumber, deltaUPProductRevision);
                    upMoFdn = (String) deltaUps.get(UpgradeActivityConstants.UP_FDN);
                    logMessage = String.format(JobLogConstants.DELETING_DELTAUP_NO_BACKUPS, deltaUPProductNumber, deltaUPProductRevision);
                    jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                    deleteUP(activityJobId, jobActivityInfo, deltaUPProductNumber, deltaUPProductRevision, preventingUpsAndCvs);
                }

            } else {
                final String cvName = upgradePackageDeletePreventingInfoProvider.readAndUpdateCvs(activityJobId, neJobId, preventingUpsAndCvs, suppliedProductNumber, suppliedProductRevision);
                if (cvName != null) {
                    logMessage = String.format(JobLogConstants.CPP_UP_AND_BACKUPDATA_FOR_DELETION, suppliedProductNumber, suppliedProductRevision, cvName);
                    jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                    deleteCv(activityJobId, suppliedProductNumber, suppliedProductRevision, preventingUpsAndCvs, cvName);
                } else {
                    upMoFdn = upgradePackageDeletePreventingInfoProvider.findFDN(preventingUpsAndCvs, suppliedProductNumber, suppliedProductRevision);
                    LOGGER.debug("For the activityJobId {} upMoFdn {} suppliedProductNumber {} and suppliedProductRevision {}.", activityJobId, upMoFdn, suppliedProductNumber,
                            suppliedProductRevision);
                    activityUtils.prepareJobPropertyList(jobProperties, UpgradeActivityConstants.PROCESSED_UPS, Integer.toString(processedUPs + 1));
                    if (upMoFdn != null) {
                        logMessage = String.format(JobLogConstants.FILTERED_UP_DATA_FOR_DELETION, suppliedProductNumber, suppliedProductRevision);
                        jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                    }

                    deleteUP(activityJobId, jobActivityInfo, suppliedProductNumber, suppliedProductRevision, preventingUpsAndCvs);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Unable to delete the UpgradePackage on Node:{} due to: ", nodeName, ex);
            jobResult = JobResult.FAILED;
            buildJobLogsAndPropertiesIfExceptionOccurs(activityJobId, upMoFdn, ex);
            persistJobData.persistPropertiesLogsAndProgress(activityJobId, neJobId, jobProperties, jobLogs, 0.0);
            configureRepeatExecute(activityJobId);
        }
    }

    private DeleteUpgradePackageActionInfo getUpgradePackageToBeDeleted(final long activityJobId, final int intProcessedUPs) {
        final String upInputData = getUPDataFromInputData(activityJobId);
        final List<String> requestedUPsTobeDeleted = convertUPDataToCommaSeperatedList(upInputData);
        userProvidedUPs = requestedUPsTobeDeleted.toArray(new String[0]);
        final int totalUPsCount = userProvidedUPs.length;
        final String totalUPs = Integer.toString(totalUPsCount);
        final String processingUPData = getUPToBeProcessed(requestedUPsTobeDeleted, intProcessedUPs);
        activityUtils.prepareJobPropertyList(jobProperties, UpgradeActivityConstants.CURRENT_USER_PROVIDED_UP, processingUPData);
        activityUtils.prepareJobPropertyList(jobProperties, UpgradeActivityConstants.TOTAL_UPS, totalUPs);
        LOGGER.debug("Retrieving Upgrade package for activityJobId:{}, listOfUPs:{}, processedUPs:{}, processingUPData:{}", activityJobId, userProvidedUPs, intProcessedUPs, processingUPData);
        persistJobData.persistPropertiesLogsAndProgress(activityJobId, neJobId, jobProperties, jobLogs, 0.0);
        return convertProcessedUPDataToDeleteUPInfo(processingUPData);
    }

    private void deleteUP(final long activityJobId, final JobActivityInfo jobActivityInfo, final String productNumber, final String productRevision,
            final Map<String, HashMap<String, Object>> preventingUpsAndCvs) {
        if (upMoFdn != null) {
            activityUtils.prepareJobPropertyList(jobProperties, UpgradeActivityConstants.PROCESSING_MO_TYPE, ShmConstants.UP_MO_TYPE);
            activityUtils.prepareJobPropertyList(jobProperties, ShmConstants.FDN, upMoFdn);
            activityUtils.prepareJobPropertyList(jobProperties, UpgradeActivityConstants.CURRENT_PROCESSING_UP,
                    productNumber + UpgradeActivityConstants.UPGRADEPACKAGES_REQUEST_DELIMTER + productRevision);

            buildJobLogsBeforeActionStarts(activityJobId, jobLogs, nodeName, upMoFdn, SHMEvents.CPP_DELETEUPGRADEPACKAGE_EXECUTE, jobActivityInfo, productNumber, productRevision);
            persistJobData.persistPropertiesLogsAndProgress(activityJobId, neJobId, jobProperties, jobLogs, 0.0);
            try {
                deleteUpResult = deleteUpHelper.deleteUP(upMoFdn);
            } catch (Exception exception) {
                LOGGER.error("exception when try to delete the up having fdn: {}, activityJobId: {}:", upMoFdn, activityJobId, exception);

                //When try to delete non existing Upgrade package from node, node will always throw exception containing message which always contains String "There is no such MO:"
                //This is mainly happens when there is database and node are not in sync.
                if (exception.toString().contains("There is no such MO:")) {
                    LOGGER.error("Either There is no UpMo to Delete Or UpMo was Already Deleted . upMoFdn {}", upMoFdn);
                    deleteUpResult = 0;
                }

            }
            LOGGER.info("deleteUpResult {} in executehandler", deleteUpResult);

            if (deleteUpResult >= 0) {
                jobResult = JobResult.SUCCESS;
                final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
                final String existingStepDurations = ((String) activityJobAttributes.get(ShmConstants.STEP_DURATIONS));
                if (existingStepDurations != null && !existingStepDurations.contains(ActivityStepsEnum.EXECUTE.getStep())) {
                    activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.EXECUTE);
                }
                final String logMessage = String.format(JobLogConstants.UP_DELETED_SUCCESSFULLY, productNumber, productRevision);
                jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                final int totalUPs = userProvidedUPs.length;
                final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
                final Double currentProgressPercentage = activityAndNEJobProgressPercentageCalculator.calculateActivityProgressPercentage(jobEnvironment, totalUPs, EXECUTE_REPEAT_PROGRESS_PERCENTAGE);
                LOGGER.debug("currentProgressPercentage on activityJobId: {} is:{}", activityJobId, currentProgressPercentage);
                persistJobData.persistPropertiesLogsAndProgress(activityJobId, neJobId, jobProperties, jobLogs, currentProgressPercentage);
            } else if (deleteUpResult == -1) {
                LOGGER.warn("Transaction has timedout, since it is taking long time at Node. Waiting for MO Deletion Event Notification");
                jobResult = null;
                activityUtils.subscribeToMoNotifications(upMoFdn, activityJobId, jobActivityInfo);
            } else {
                jobResult = JobResult.FAILED;
                buildJobLogsForFailedDeletionOfUP(productNumber, productRevision);
            }
            persistJobData.persistPropertiesLogsAndProgress(activityJobId, neJobId, jobProperties, jobLogs, 0.0);
        } else {
            LOGGER.error("UpgradePackage FDN for [productNumber:{}, productRevision:{}] not found on node:{}", productNumber, productRevision, nodeName);
            if (upgradePackageDeletePreventingInfoProvider.findCvFailStatus(preventingUpsAndCvs, productNumber, productRevision)) {
                buildJobLogsForFailedDeletionOfUP(productNumber, productRevision);
                jobResult = JobResult.FAILED;
            }
            persistJobData.persistPropertiesLogsAndProgress(activityJobId, neJobId, jobProperties, jobLogs, 0.0);
        }
        configureRepeatExecute(activityJobId);
    }

    private void configureRepeatExecute(final long activityJobId) {
        if (jobResult == JobResult.FAILED) {
            activityUtils.prepareJobPropertyList(jobProperties, UpgradeActivityConstants.INTERMEDIATE_FAILURE, jobResult.toString());
        }
        final Map<String, Object> repeatRequiredAndActivityResult = deleteUpgradePackageUtil.evaluateRepeatRequiredAndActivityResult(activityJobId, jobResult, neJobStaticData, neType);
        final boolean repeatRequired = (boolean) repeatRequiredAndActivityResult.get(JobVariables.ACTIVITY_REPEAT_EXECUTE);
        final JobResult evaluatedActivityResult = (JobResult) repeatRequiredAndActivityResult.get(ActivityConstants.ACTIVITY_RESULT);
        if (repeatRequired) {
            processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, repeatRequired);
            persistJobData.persistPropertiesLogsAndProgress(activityJobId, neJobId, jobProperties, jobLogs, 0.0);
            activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, "execute", processVariables);
        } else {
            if (evaluatedActivityResult != null) {
                activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, evaluatedActivityResult.toString());
                if (evaluatedActivityResult == JobResult.SUCCESS) {
                    buildJobLogsAndPropertiesForSuccessfulActivityCompletion(activityJobId);
                }
                persistJobData.persistPropertiesLogsAndProgress(activityJobId, neJobId, jobProperties, jobLogs, 0.0);
                activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, "execute", processVariables);
            } else {
                LOGGER.warn("Delete UP action not yet completed on the node. So ActivityJob result not evaluated and not correlating to wfs. Waiting for dps DELETE event");
            }

        }

    }

    private void deleteCv(final long activityJobId, final String suppliedProductNumber, final String suppliedProductRevision, final Map<String, HashMap<String, Object>> preventingUpsAndCvs,
            final String cvName) {
        boolean repeatRequired = true;
        activityUtils.prepareJobPropertyList(jobProperties, UpgradeActivityConstants.PROCESSING_MO_TYPE, ShmConstants.CV_MO_TYPE);
        activityUtils.prepareJobPropertyList(jobProperties, ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME, cvName);
        persistJobData.persistPropertiesLogsAndProgress(activityJobId, neJobId, jobProperties, jobLogs, 0.0);
        systemRecorder.recordCommand(SHMEvents.CPP_DELETEUPGRADEPACKAGE_EXECUTE, CommandPhase.STARTED, nodeName, cvName, Long.toString(activityJobId));
        final boolean isBackUpRemoved = deleteCvHandler.deleteCv(activityJobId, neJobId, nodeName, cvName, isRemoveFromRollbackListSelected);
        if (!isBackUpRemoved) {
            upgradePackageDeletePreventingInfoProvider.updateStatus(activityJobId, activityJobId, preventingUpsAndCvs, suppliedProductNumber, suppliedProductRevision);
        }

        else if (activityUtils.cancelTriggered(activityJobId)) {
            jobResult = JobResult.CANCELLED;
            repeatRequired = false;
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, jobResult.toString());
        }

        processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, repeatRequired);
        persistJobData.persistPropertiesLogsAndProgress(activityJobId, neJobId, jobProperties, jobLogs, 0.0);
        activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, "execute", processVariables);
    }

    private void buildJobLogsForFailedDeletionOfUP(final String suppliedProductNumber, final String suppliedProductRevision) {
        final String logMessage = String.format(JobLogConstants.UP_DELETION_FAILED, suppliedProductNumber, suppliedProductRevision);
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
    }

    private void buildJobLogsAndPropertiesForSuccessfulActivityCompletion(final long activityJobId) {
        final String logMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, ActivityConstants.DELETE_UP_DISPLAY_NAME);
        activityUtils.recordEvent(recordingEvent, nodeName, "", activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
    }

    private void initializeVariables(final long activityJobId) {
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
            neType = networkElement.getNeType();
            platformType = neJobStaticData.getPlatformType();
            neJobId = neJobStaticData.getNeJobId();
            jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
            recordingEvent = SHMEvents.CPP_DELETEUPGRADEPACKAGE_EXECUTE;
        } catch (final MoNotFoundException moNotFoundException) {
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            LOGGER.error("Initialization of Variables failed for node {} due to: ", nodeName, moNotFoundException);
        } catch (JobDataNotFoundException jobDataNotFoundException) {
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            LOGGER.error("Initialization of Variables failed for node {} due to: ", nodeName, jobDataNotFoundException);
        }
    }

    private String getUPDataFromInputData(final long activityJobId) {
        final List<String> jobPropertyMapKeys = new ArrayList<String>();
        jobPropertyMapKeys.add(UpgradeActivityConstants.IS_PREVENT_UP_DELETABALE);
        jobPropertyMapKeys.add(UpgradeActivityConstants.IS_PREVENT_CV_DELETABALE_FROM_ROLLBACKLIST);
        final Map<String, Object> jobConfigurationData = (Map<String, Object>) activityUtils.getMainJobAttributes(activityJobId).get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
        final Map<String, String> jobPropertyMapValues = jobPropertyUtils.getPropertyValue(jobPropertyMapKeys, jobConfigurationData, nodeName, neType, platformType);
        isRemoveFromRollbackListSelected = Boolean.parseBoolean(jobPropertyMapValues.get(UpgradeActivityConstants.IS_PREVENT_CV_DELETABALE_FROM_ROLLBACKLIST));

        final String deletableInActiveUPList = activityUtils.getActivityJobAttributeValue(activityUtils.getPoAttributes(activityJobId), UpgradeActivityConstants.DELETABLE_UP_LIST);
        LOGGER.debug("User provided information for Deleting CPP UpgradePackage is: {}", deletableInActiveUPList);
        return deletableInActiveUPList;
    }

    private boolean isPrecheckDone() {
        final String isPrecheckDone = activityUtils.getActivityJobAttributeValue(jobEnvironment.getActivityJobAttributes(), ActivityConstants.IS_PRECHECK_DONE);
        if (isPrecheckDone != null) {
            return Boolean.parseBoolean(isPrecheckDone);
        }
        return false;
    }

    private void buildJobLogsBeforeActionStarts(final long activityJobId, final List<Map<String, Object>> jobLogs, final String nodeName, final String currentUpFdn, final String event,
            final JobActivityInfo jobActivityInfo, final String productNumber, final String productRevision) {
        final Integer activityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.CPP.name(), JobTypeEnum.DELETE_UPGRADEPACKAGE.name(),
                ShmConstants.DELETEUPGRADEPKG_ACTIVITY);
        final String upId = DeleteUpgradePackageUtil.getRdnId(upMoFdn);
        final String logMessage = String.format(JobLogConstants.DELETEUP_ASYNC_ACTION_TRIGGERED, ActivityConstants.DELETE_UP_DISPLAY_NAME, activityTimeout, productNumber, productRevision)
                + " UpgradePackage Id:" + upId;
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        systemRecorder.recordCommand(event, CommandPhase.STARTED, nodeName, currentUpFdn, String.format(JobLogConstants.ACTION_TRIGGERED, jobActivityInfo));
        activityUtils.recordEvent(event, nodeName, currentUpFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
    }

    private void buildJobLogsAndPropertiesIfExceptionOccurs(final long activityJobId, final String currentUpFdn, final Exception ex) {
        final String errorMessage = activityUtils.prepareErrorMessage(ex);
        final String logMessage = String.format(JobLogConstants.UP_ACTION_TRIGGER_FAILED, ActivityConstants.DELETE_UP_DISPLAY_NAME, currentUpFdn)
                + String.format(JobLogConstants.FAILURE_REASON, errorMessage);
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        systemRecorder.recordCommand(recordingEvent, CommandPhase.FINISHED_WITH_ERROR, nodeName, currentUpFdn, logMessage);
        activityUtils.recordEvent(recordingEvent, nodeName, currentUpFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, errorMessage));
    }

    private List<String> convertUPDataToCommaSeperatedList(final String upInputData) {
        final List<String> upDataList = new ArrayList<String>();
        if (upInputData.contains(ActivityConstants.COMMA)) {
            final String[] upData = upInputData.split(ActivityConstants.COMMA);
            Collections.addAll(upDataList, upData);
        } else {
            upDataList.add(upInputData);
        }
        return upDataList;
    }

    private String getUPToBeProcessed(final List<String> deleteUPDataList, final int processedUPs) {
        final String[] listOfUPs = deleteUPDataList.toArray(new String[0]);
        final String UpToBeProcessed = listOfUPs[processedUPs];
        return UpToBeProcessed;
    }

    private int getCountOfProcessedUPs(final Map<String, Object> activityJobproperties) {
        int processedUPs = 0;
        final String processedUpsString = activityUtils.getActivityJobAttributeValue(activityJobproperties, UpgradeActivityConstants.PROCESSED_UPS);
        if (processedUpsString != null && !processedUpsString.isEmpty()) {
            processedUPs = Integer.parseInt(processedUpsString);
        }
        return processedUPs;
    }

    private DeleteUpgradePackageActionInfo convertProcessedUPDataToDeleteUPInfo(final String processingUPData) {
        final DeleteUpgradePackageActionInfo deleteUpgradePackageActionInfo = new DeleteUpgradePackageActionInfo();
        final String[] deleteUPData = processingUPData.split(UpgradeActivityConstants.UPGRADEPACKAGES_PERSISTENCE_DELIMTER);
        deleteUpgradePackageActionInfo.setProductNumber(deleteUPData[0]);
        deleteUpgradePackageActionInfo.setProductRevision(deleteUPData[1]);
        return deleteUpgradePackageActionInfo;
    }
}
