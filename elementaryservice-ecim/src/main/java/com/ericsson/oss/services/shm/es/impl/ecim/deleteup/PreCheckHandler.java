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
package com.ericsson.oss.services.shm.es.impl.ecim.deleteup;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.common.enums.NodeType;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BackupNotFoundException;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.ecim.common.ApMoHandlerRetryProxy;
import com.ericsson.oss.services.shm.fa.api.FaBuildingBlockResponseProvider;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.BrmBackup;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.BrmBackupCreationType;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobResult;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@Traceable
@RequestScoped
@SuppressWarnings("PMD.TooManyFields")
public class PreCheckHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreCheckHandler.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private DeleteUpgradePackageDataCollector deleteUpJobDataCollector;

    @Inject
    private BrmMoServiceRetryProxy brmMoServiceRetryProxy;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceProxy;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private FaBuildingBlockResponseProvider buildingBlockResponseProvider;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    @Inject
    ApMoHandlerRetryProxy apMoHandlerRetryProxy;

    @Inject
    private SystemRecorder systemRecorder;

    static private final String ERROR_MESSAGE = "Exception occurred while performing precheck on node {}, Reason : ";

    private String nodeName = "";
    private String platformType = null;
    private JobEnvironment jobEnvironment = null;
    private String neType = null;
    private NetworkElementData networkElement = null;

    private NEJobStaticData neJobStaticData = null;
    private JobStaticData jobStaticData = null;

    private final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
    private final List<Map<String, Object>> jobProperties = new ArrayList<Map<String, Object>>();
    private final ActivityStepResult activityStepResult = new ActivityStepResult();

    public ActivityStepResult performPreCheck(final long activityJobId) {
        LOGGER.debug("entered Delete Upgrade Package activity precheck with activityJobId : {}", activityJobId);
        String logMessage = null;
        try {
            initializeVariables(activityJobId);
            // TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ShmConstants.DELETEUPGRADEPKG_ACTIVITY);
            if (!isUserAuthorized) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
                return activityStepResult;
            }
            initiateActivity(activityJobId, nodeName);
            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.PRECHECK);

            final Map<String, String> userInputData = deleteUpJobDataCollector.getUserProvidedData(jobEnvironment, nodeName, neType, platformType);
            final String isNonActiveUpsToDelete = userInputData.get(JobPropertyConstants.DELETE_NON_ACTIVE_UPS);
            final boolean isNonActiveUpsFlagEnabled = Boolean.parseBoolean(isNonActiveUpsToDelete);
            final String deleteUpList = userInputData.get(JobPropertyConstants.DELETE_UP_LIST);
            final Set<String> inputProductDataSet = (deleteUpList != null) ? deleteUpJobDataCollector.getInputProductData(deleteUpList) : new HashSet<>();

            final String swIMNamespace = deleteUpJobDataCollector.getSWMNameSpace(nodeName, FragmentType.ECIM_SWIM_TYPE.getFragmentName());
            final String swMNamespace = deleteUpJobDataCollector.getSWMNameSpace(nodeName, FragmentType.ECIM_SWM_TYPE.getFragmentName());
            final Set<String> nodeUpData = deleteUpJobDataCollector.getUpData(nodeName, swMNamespace);
            LOGGER.info("ECIM Deleteup node upgrade package data {} activityJobId {}", nodeUpData, activityJobId);

            if (!inputProductDataSet.isEmpty()) {
                final Set<String> filteredValidUPMOData = deleteUpJobDataCollector.fetchValidUPDataOverInputData(nodeUpData, inputProductDataSet);
                buildJobLogForEachInvalidInputData(inputProductDataSet, nodeUpData, activityJobId, DeleteUpgradePackageConstants.NOT_EXISTED);
                if (!filteredValidUPMOData.isEmpty()) {
                    final Set<String> inactiveDeletableUPs = deleteUpJobDataCollector.filterActiveUps(nodeName, swIMNamespace, filteredValidUPMOData);

                    logNonDeletableUps(inactiveDeletableUPs, filteredValidUPMOData, activityJobId, DeleteUpgradePackageConstants.ACTIVE);
                    prepareDeletableUps(activityJobId, inactiveDeletableUPs, nodeUpData, swMNamespace, userInputData, isNonActiveUpsFlagEnabled);
                } else {
                    activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION);
                    logMessage = String.format(JobLogConstants.DELETEUP_PRECHECK_SKIP, ActivityConstants.DELETE_UP_DISPLAY_NAME);
                    prepareLogsandPersist(activityJobId, logMessage, JobLogLevel.ERROR.toString());
                }
            } else if (isNonActiveUpsFlagEnabled) {
                final Set<String> nodeUpMOData = deleteUpJobDataCollector.prepareNodeProductData(nodeUpData);
                final Set<String> inactiveDeletableUPs = deleteUpJobDataCollector.filterActiveUps(nodeName, swIMNamespace, nodeUpMOData);
                prepareLogsandPersist(activityJobId, String.format(JobLogConstants.INACTIVEUP_ACTIVITY_TRIGGERED, ActivityConstants.DELETE_UP_DISPLAY_NAME), JobLogLevel.INFO.toString());
                if (!inactiveDeletableUPs.isEmpty()) {
                    //only for RadioNode and if only one inactive UP is available, Skip Job.
                    if (NodeType.RADIONODE.getName().equals(neType) && inactiveDeletableUPs.size() == 1) {
                        LOGGER.info("Delete UP activity is skipped. Since only one inactive Upgrade Package is available. Inactive Packages: {}. Activity ID: {}", inactiveDeletableUPs, activityJobId);
                        prepareLogsandPersist(activityJobId, String.format(JobLogConstants.ONLY_ONE_INACTIVEUP_ACTIVITY_SKIP, ActivityConstants.DELETE_UP_DISPLAY_NAME), JobLogLevel.ERROR.toString());
                        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION);
                    } else {
                        prepareDeletableUps(activityJobId, inactiveDeletableUPs, nodeUpData, swMNamespace, userInputData, isNonActiveUpsFlagEnabled);
                    }
                } else {
                    prepareLogsandPersist(activityJobId, String.format(JobLogConstants.NO_INACTIVEUP_ACTIVITY_FAILED, ActivityConstants.DELETE_UP_DISPLAY_NAME), JobLogLevel.ERROR.toString());
                    activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION);
                }
            } else {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION);
                logMessage = String.format(JobLogConstants.DELETEUP_PRECHECK_SKIP, ActivityConstants.DELETE_UP_DISPLAY_NAME);
                prepareLogsandPersist(activityJobId, logMessage, JobLogLevel.ERROR.toString());
            }
        } catch (final Exception ex) {
            LOGGER.error(ERROR_MESSAGE, nodeName, ex);
            if (ex.getCause() instanceof MoNotFoundException || ex.getCause() instanceof UnsupportedFragmentException || ex.getCause() instanceof BackupNotFoundException) {
                LOGGER.error(ERROR_MESSAGE, nodeName, ex.getCause());
            } else {
                LOGGER.error(ERROR_MESSAGE, nodeName, ex);
            }
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
            prepareLogsandPersist(activityJobId, ex.getMessage(), JobLogLevel.ERROR.toString());
            return activityStepResult;
        }
        return activityStepResult;
    }

    private void prepareDeletableUps(final long activityJobId, final Set<String> inactiveDeletableUPs, final Set<String> nodeUpData, final String swMNamespace,
            final Map<String, String> userInputData, final boolean isNonActiveUpsFlagEnabled) throws BackupNotFoundException {
        boolean isActivitySuccess = false;
        if (!inactiveDeletableUPs.isEmpty()) {
            final Set<UpgradePackageBean> upFdnData = deleteUpJobDataCollector.getUpMoFdnData(inactiveDeletableUPs, nodeUpData);
            final List<BrmBackup> brmBackupList = brmMoServiceRetryProxy.getAllBrmBackups(nodeName);
            LOGGER.debug("ECIM delete up prepareDeletableUps upFdnData {}, brmBackuplist {} for activityJobId {}", upFdnData, brmBackupList, activityJobId);

            if (isNonActiveUpsFlagEnabled && neType.equals(NodeType.RADIONODE.getName())) {
                Map<String, Set<String>> upsWithSyscrBackups = deleteUpJobDataCollector.getReferredBackups(activityJobId, inactiveDeletableUPs, nodeUpData, brmBackupList, isNonActiveUpsFlagEnabled, true);
                // On identifying UPs with SYSCR backups >= 2, Going to check AP MO's rbsConfigLevel value for only RADIONODEs.
                if (upsWithSyscrBackups.size() >= 2) {
                    LOGGER.info("Inactive UPs with SYSCR backups size {}. UPs details are: {}", upsWithSyscrBackups.size(), upsWithSyscrBackups);
                    if(checkAndCorrectApMoRbsConfigLevel(activityJobId, upsWithSyscrBackups)) {
                        return;
                    }
                }
            }
            final Map<String, Set<String>> upMOReferredBKPMap = deleteUpJobDataCollector.getReferredBackups(activityJobId, inactiveDeletableUPs, nodeUpData, brmBackupList, isNonActiveUpsFlagEnabled, false);
            LOGGER.debug("ECIM delete up prepareDeletableUps upMOReferredBKPMap {}", upMOReferredBKPMap);

            deleteUpJobDataCollector.persistUPAndReferredBkpsData(upMOReferredBKPMap, activityJobId, swMNamespace);
            deleteUpJobDataCollector.persistUpMoFdns(upFdnData, upMOReferredBKPMap, activityJobId);
            if (isNonActiveUpsFlagEnabled) {
                deleteUpJobDataCollector.updateInactiveUps(neJobStaticData.getNeJobId(), upFdnData, upMOReferredBKPMap);
                updateActivityTimeout(activityJobId, neJobStaticData, upFdnData, brmBackupList);
            }
            if (!upMOReferredBKPMap.isEmpty()) {
                if (ActivityConstants.CHECK_FALSE.equalsIgnoreCase(userInputData.get(JobPropertyConstants.DELETE_REFERRED_BACKUPS))) {
                    if (isAnyReferredBackupPresentForUP(activityJobId, upMOReferredBKPMap)) {
                        isActivitySuccess = false;
                    } else {
                        prepareUpDataForDeletion(activityJobId, upMOReferredBKPMap);
                        isActivitySuccess = true;
                    }
                } else {
                    prepareUpDataForDeletion(activityJobId, upMOReferredBKPMap);
                    isActivitySuccess = true;
                }
            }

        } else {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
            prepareLogsandPersist(activityJobId, String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.DELETE_UP_DISPLAY_NAME), JobLogLevel.ERROR.toString());
        }
        updateActivityResult(isActivitySuccess, activityJobId);
    }

    private boolean checkAndCorrectApMoRbsConfigLevel(final long activityJobId, final Map<String, Set<String>> upsWithSyscrBackups) {
        try {
            Map<String, Object> apMO = apMoHandlerRetryProxy.getApMoDetails(nodeName);
            final String rbsConfigLevelValue = (String) apMO.get(EcimCommonConstants.AutoProvisioningMoConstants.RBS_CONFIG_LEVEL_KEY);
            final String apMoFdn = (String) apMO.get(PollingActivityConstants.MO_FDN);
            LOGGER.debug("Found AP MO state as: {} and mofdn: {}", rbsConfigLevelValue, apMoFdn);
            if(EcimCommonConstants.AutoProvisioningMoConstants.SITE_CONFIG_COMPLETE.equals(rbsConfigLevelValue)) {
                // Going to setting AP MO's rbsConfigLevel to READY_FOR_SERVICE, so that connected AI SYSCR Backup would be deleted by Node.
                Map<String, Object> rbsConfigLevelArugument = new HashMap<>();
                rbsConfigLevelArugument.put(EcimCommonConstants.AutoProvisioningMoConstants.RBS_CONFIG_LEVEL_KEY, EcimCommonConstants.AutoProvisioningMoConstants.READY_FOR_SERVICE);
                subscribeToBrmBackupMoNotifications(upsWithSyscrBackups, activityJobId);

                apMoHandlerRetryProxy.updateApMoAttributes(apMoFdn, rbsConfigLevelArugument);
                deleteUpJobDataCollector.persistUPsWithSyscrBkpsDataInActivityJobProperties(upsWithSyscrBackups, activityJobId);
                systemRecorder.recordCommand(SHMEvents.ECIM_DELETE_UPGRADEPACKAGE_AP_RBSCONFIGLEVEL_CHANGE, CommandPhase.ONGOING, nodeName, apMoFdn,
                        String.format(JobLogConstants.RESET_AP_MO_RBS_CONFIG_VALUE));
                prepareLogsandPersist(activityJobId, JobLogConstants.RESET_AP_MO_RBS_CONFIG_VALUE, JobLogLevel.INFO.toString());
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.REPEAT_EXECUTE);
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("Got Exception during AP MO details processing for Node {} with ActivityId {}. Details: ", e, nodeName, activityJobId);
        }
        return false;
    }

    private void subscribeToBrmBackupMoNotifications(Map<String, Set<String>> upsWithSyscrBackups, long activityJobId) {
        LOGGER.debug("Subscribing for BrmBackups of UPs {} for notificaitons with Activity ID: {}.", upsWithSyscrBackups, activityJobId);
        String brmBackupManagerMoFdn = "";
        JobActivityInfo jobActivityInfo = getActivityInfo(activityJobId);
        for (final Entry<String, Set<String>> upData : upsWithSyscrBackups.entrySet()) {
            Set<String> refferedBackups = upData.getValue();
            for (final String referredBkpUp : refferedBackups) {
                String[] backupData = referredBkpUp.split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);
                if (BrmBackupCreationType.SYSTEM_CREATED.toString().equals(backupData[2])) {
                    activityUtils.subscribeToMoNotifications(backupData[3], activityJobId, jobActivityInfo);    
                }
                if(!brmBackupManagerMoFdn.isEmpty()) {
                    brmBackupManagerMoFdn = backupData[1];
                }
            }
        }
        LOGGER.debug("Subscribing for Manager: {} with activity iD: {}", brmBackupManagerMoFdn, activityJobId);
        activityUtils.subscribeToMoNotifications(brmBackupManagerMoFdn, activityJobId, jobActivityInfo);
    }

    private JobActivityInfo getActivityInfo(final long activityJobId) {
        return activityUtils.getActivityInfo(activityJobId, DeleteUpgradePackageService.class);
    }

    private void updateActivityTimeout(final long activityJobId, final NEJobStaticData neJobStaticData, final Set<UpgradePackageBean> upFdnData, final List<BrmBackup> brmBackupList) {
        try {
            final int numberOfUpsToBeDeleted = upFdnData.size();
            final int numberOfReferredBackupsToBeDeleted = brmBackupList.size();
            networkElement = networkElementRetrivalBean.getNetworkElementData(neJobStaticData.getNodeName());
            final Integer deleteBkpTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(networkElement.getNeType(), neJobStaticData.getPlatformType(), JobTypeEnum.DELETEBACKUP.name(),
                    EcimBackupConstants.DELETE_BACKUP);
            final Integer deleteUpTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(networkElement.getNeType(), neJobStaticData.getPlatformType(),
                    JobTypeEnum.DELETE_UPGRADEPACKAGE.name(), ShmJobConstants.DELETE_UP_ACTIVITY);
            final int totalTimeout = (numberOfUpsToBeDeleted * deleteUpTimeout) + (numberOfReferredBackupsToBeDeleted * deleteBkpTimeout);
            final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
            buildingBlockResponseProvider.sendUpdatedActivityTimeout(activityJobId, neJobStaticData, activityJobAttributes, ShmJobConstants.DELETE_UP_ACTIVITY, totalTimeout);
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while updating activityTimeout to FA for Activity: {} with activityJobId: {} due to {}:", ShmJobConstants.DELETE_UP_ACTIVITY, activityJobId, ex);
        }
    }

    private boolean isAnyReferredBackupPresentForUP(final long activityJobId, final Map<String, Set<String>> upMOReferredBKPMap) {
        boolean isReferredBkp = false;
        for (final Map.Entry<String, Set<String>> entry : upMOReferredBKPMap.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                final String upProductData[] = entry.getKey().split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);
                final String log = String.format(JobLogConstants.DELETION_SKIP_IF_REFERREDBKPS, upProductData[0], upProductData[1], getBackNamesToLog(entry.getValue()));

                jobLogUtil.prepareJobLogAtrributesList(jobLogs, log, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                activityUtils.prepareJobPropertyList(jobProperties, DeleteUpgradePackageConstants.FAIL_UP_DELETION, "true");
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs, null);
                jobLogs.clear();

                isReferredBkp = true;
            }
        }
        return isReferredBkp;
    }

    private void prepareUpDataForDeletion(final long activityJobId, final Map<String, Set<String>> upMOReferredBKPMap) {
        buildSuccessUPJobLogs(activityJobId, upMOReferredBKPMap);
        for (final Map.Entry<String, Set<String>> entry : upMOReferredBKPMap.entrySet()) {
            prepareDataForDeletion(entry, upMOReferredBKPMap, activityJobId);
        }
    }

    private void updateActivityResult(final boolean isActivitySuccess, final long activityJobId) {
        if (isActivitySuccess) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        } else {
            final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
            final String failUPDeletion = activityUtils.getActivityJobAttributeValue(activityJobAttributes, DeleteUpgradePackageConstants.FAIL_UP_DELETION);
            if (failUPDeletion != null && Boolean.valueOf(failUPDeletion)) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
            } else {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION);
            }
        }

    }

    private List<String> getListOfUpMoFdns(final Map<String, Set<String>> upMOReferredBKPMap) {
        final List<String> upMoFdns = new ArrayList<String>();
        for (final Map.Entry<String, Set<String>> entry : upMOReferredBKPMap.entrySet()) {
            upMoFdns.add(entry.getKey());
        }
        return upMoFdns;
    }

    private void prepareDataForDeletion(final Map.Entry<String, Set<String>> entry, final Map<String, Set<String>> upMOReferredBKPMap, final long activityJobId) {
        String currentBkp = "";
        String log = "";

        final String currentUpMo = entry.getKey();
        final Set<String> listOfBkps = entry.getValue();

        final String currentUpMoDataArray[] = entry.getKey().split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);
        if (!listOfBkps.isEmpty()) {
            currentBkp = entry.getValue().iterator().next();
            log = String.format(JobLogConstants.UP_AND_BACKUPDATA_FOR_DELETION, currentUpMoDataArray[0], currentUpMoDataArray[1], getBackNamesToLog(listOfBkps));
        } else {
            log = String.format(JobLogConstants.UP_MO_FDN_NO_BACKUPS, currentUpMoDataArray[0], currentUpMoDataArray[1]);
        }

        jobLogUtil.prepareJobLogAtrributesList(jobLogs, log, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogs, null);
        jobLogs.clear();

        final List<String> listOfUpMoFdns = getListOfUpMoFdns(upMOReferredBKPMap);

        deleteUpJobDataCollector.persistUPDataToBeProcessed(currentBkp, currentUpMo, listOfBkps, listOfUpMoFdns, activityJobId);
    }

    private String getBackNamesToLog(final Set<String> listOfBkps) {
        final Set<String> backNames = new HashSet<String>();
        for (final String backpData : listOfBkps) {
            final String backupDataArray[] = backpData.split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);
            backNames.add(backupDataArray[0]);
        }
        return backNames.toString();
    }

    private void initiateActivity(final long activityJobId, final String nodeName) throws UnsupportedFragmentException {

        jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.ACTIVITY_INITIATED, ActivityConstants.DELETE_UP_DISPLAY_NAME), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.INFO.toString());

        final String treatAsInfo = activityUtils.isTreatAs(nodeName, FragmentType.ECIM_SWM_TYPE, SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY);
        if (treatAsInfo != null) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, treatAsInfo, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        }
        deleteUpJobDataCollector.checkFragmentAndUpdateLog(jobLogs, neType, networkElement.getOssModelIdentity());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogs, null);
        jobLogs.clear();
    }

    private void initializeVariables(final long activityJobId) {
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY);
            jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            platformType = neJobStaticData.getPlatformType();
            nodeName = neJobStaticData.getNodeName();
            networkElement = networkElementRetrivalBean.getNetworkElementData(nodeName);
            neType = networkElement.getNeType();
            jobEnvironment = activityUtils.getJobEnvironment(activityJobId);

        } catch (final MoNotFoundException moNotFoundException) {
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            LOGGER.error("Initialization of Variables failed for node {} due to: ", nodeName, moNotFoundException);
        } catch (final JobDataNotFoundException jobDataNotFoundException) {
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            LOGGER.error("Initialization of Variables failed for node {} due to: ", nodeName, jobDataNotFoundException);
        }
    }

    private void logNonDeletableUps(final Set<String> inactiveUPsTobeDeleted, final Set<String> filteredValidUPMODataOnNode, final long activityJobId, final String logMsg) {
        if (!inactiveUPsTobeDeleted.isEmpty()) {
            final Set<String> activeUpsTobeLogged = new HashSet<String>(filteredValidUPMODataOnNode);
            activeUpsTobeLogged.removeAll(inactiveUPsTobeDeleted);
            if (!activeUpsTobeLogged.isEmpty()) {
                activityUtils.prepareJobPropertyList(jobProperties, DeleteUpgradePackageConstants.IS_ANY_ACTIVE_UP, "true");
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, null, null);
                prepareandPersistlogsForActiveUps(activityJobId, activeUpsTobeLogged, logMsg);
            }
        } else {
            final Set<String> activeUpData = new HashSet<String>(filteredValidUPMODataOnNode);
            activityUtils.prepareJobPropertyList(jobProperties, DeleteUpgradePackageConstants.FAIL_UP_DELETION, "true");
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, null, null);

            prepareandPersistlogsForActiveUps(activityJobId, activeUpData, logMsg);
        }
    }

    private void prepareandPersistlogsForActiveUps(final long activityJobId, final Set<String> upDataList, final String logMsg) {
        for (final String data : upDataList) {
            final String upData[] = data.split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);
            final String logMessage = String.format(JobLogConstants.UP_MO_DATA, upData[0], upData[1], logMsg);
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogs, null);
        jobLogs.clear();
    }

    private void buildSuccessUPJobLogs(final long activityJobId, final Map<String, Set<String>> upMOReferredBKPMap) {
        if (!upMOReferredBKPMap.isEmpty()) {
            final List<String> listOfUpMoFdns = getListOfUpMoFdns(upMOReferredBKPMap);
            for (final String upMoFdn : listOfUpMoFdns) {
                final String[] productDetail = upMoFdn.split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);
                if (productDetail.length == 2) {
                    final String logMessage = String.format(JobLogConstants.FILTERED_UP_DATA_FOR_DELETION, productDetail[0], productDetail[1]);
                    jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                } else {
                    LOGGER.warn("Invalid UpgradePackage information:{} for activity: {}", upMoFdn, activityJobId);
                }
            }
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogs, null);
        }
        jobLogs.clear();
    }

    private void buildJobLogForEachInvalidInputData(final Set<String> inputProductDataSet, final Set<String> nodeUpData, final long activityJobId, final String logMsg) {
        final Set<String> nodeProductData = new HashSet<String>();
        final Set<String> inputData = new HashSet<String>(inputProductDataSet);
        if (!nodeUpData.isEmpty()) {
            for (final String nodeData : nodeUpData) {
                final String productData[] = nodeData.split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);
                nodeProductData.add(productData[0] + DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR + productData[1]);
            }
        }
        inputData.removeAll(nodeProductData);
        if (!inputData.isEmpty()) {
            for (final String data : inputData) {
                final String upData[] = data.split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);
                final String logMessage = String.format(JobLogConstants.UP_MO_DATA, upData[0], upData[1], logMsg);
                jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            }
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogs, null);
            jobLogs.clear();
        }
    }

    private void prepareLogsandPersist(final long activityJobId, final String logMessage, final String logLevel) {
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), logLevel);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogs, null);
        jobLogs.clear();
    }
}
