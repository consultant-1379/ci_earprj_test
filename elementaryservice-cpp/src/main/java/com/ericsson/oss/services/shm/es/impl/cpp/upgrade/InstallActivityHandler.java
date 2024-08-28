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

package com.ericsson.oss.services.shm.es.impl.cpp.upgrade;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.HANDLE_TIMEOUT_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.MOACTION_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.MOACTION_START_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.impl.cpp.restore.RestoreServiceConstants.SW_PKG_NAME_APPENDER;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.job.utils.UpgradeJobConfigurationListener;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.retry.DpsWriterRetryProxy;
import com.ericsson.oss.services.shm.common.smrs.SmrsAccountInfo;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.common.smrs.SmrsServiceConstants;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.common.NodeReadAttributeFailedException;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

/**
 * This class facilitates the installation of one upgrade package of CPP based node by invoking the UpgradePackage MO action(depending on the action type) that initializes the install activity. This
 * is a common activity handler class for RestoreInstallService.java and InstallService.java.
 * 
 * @author xrajeke
 * 
 */

@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class InstallActivityHandler extends AbstractUpgradeActivity {

    private static final String EMPTY_STRING = "";
    private static final String ACTIVITYNAME_INSTALL = "install";
    private static final Logger LOGGER = LoggerFactory.getLogger(InstallActivityHandler.class);

    @Inject
    private SmrsFileStoreService smrsServiceUtil;

    @Inject
    private DpsWriterRetryProxy dpsWriter;

    @Inject
    private UpgradeJobConfigurationListener upgradeJobConfigurationListener;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    @Inject
    private JobLogUtil jobLogUtil;

    private static final String[] UPMO_STATE = { UpgradePackageMoConstants.UP_MO_STATE };

    private static final String[] UPMO_ATTRIBUTES = { UpgradePackageMoConstants.UP_MO_USER, UpgradePackageMoConstants.UP_MO_PASSWORD, UpgradePackageMoConstants.UP_MO_FTP_SERVER_IP_ADDRESS,
            UpgradePackageMoConstants.UP_MO_UP_FILEPATH_ON_FTP_SERVER };

    private static final String INSTALL_CANCEL_EXECUTED = "instalCancellExecuted";
    private static final String INSTALL_CANCEL_FAILED = "installCancelFailed";

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    /**
     * This method validates the upgrade package to decide if install activity can be started or not and sends back the activity result to Work Flow Service.
     * 
     * @param activityJobId
     * @param jobActivityInfo
     * @param productId
     * @return ActivityStepResult
     * 
     */
    public ActivityStepResult precheck(final long activityJobId, final String productNumber, final String productRevision, final String jobType, final NEJobStaticData neJobStaticData) {
        final long neJobId = neJobStaticData.getNeJobId();
        final Map<String, Object> neJobAttributes = jobConfigurationServiceProxy.getNeJobAttributes(neJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final String nodeName = neJobStaticData.getNodeName();
        LOGGER.info("Performing Install Activity precheck for job Type : {}", jobType);
        final String treatAsInfo = activityUtils.isTreatAs(nodeName);
        if (treatAsInfo != null) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, treatAsInfo, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        }
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        final Map<String, Object> upgradePackageMoData = upgradePackageService.getUpMoData(activityJobId, UPMO_STATE, productNumber, productRevision);
        if (upgradePackageMoData.size() == 0) {
            activityStepResultEnum = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
            final String logMessage = "Proceeding Install Activity as MO doesn't exist.";
            LOGGER.debug("Proceeding to create MO as it doesn't exist for jobId={}.", activityJobId);
            activityUtils.recordEvent(SHMEvents.INSTALL_PRECHECK, nodeName, EMPTY_STRING, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
        } else {
            activityStepResultEnum = validateUPMoState(activityJobId, nodeName, upgradePackageMoData, productNumber, productRevision, neJobId, neJobAttributes, jobType);
        }
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(activityStepResultEnum);
        return activityStepResult;
    }

    /**
     * This method is the activity execute step called when pre-check stage is passed. This registers for notifications, initiates and performs the MO action and sends back activity result to Work
     * Flow Service
     * 
     * This must be called from Upgrade Install Service.
     * 
     * @param productNumber
     * @param productRevision
     * @param packageTypeTobeLogged
     * @param jobActivityInfo
     * @return ActivityStepResult
     * 
     */
    public void execute(final String productNumber, final String productRevision, final String packageTypeTobeLogged, final JobActivityInfo jobActivityInfo) {
        execute(productNumber, productRevision, getActionType(jobActivityInfo.getActivityJobId()), packageTypeTobeLogged, jobActivityInfo);
    }

    /**
     * This method is the activity execute step called when pre-check stage is passed. This registers for notifications, initiates and performs the MO action and sends back activity result to Work
     * Flow Service *
     * 
     * @param productNumber
     * @param productRevision
     * @param actionType
     * @param packageTypeTobeLogged
     * @param jobActivityInfo
     * @return ActivityStepResult
     */
    public void execute(final String productNumber, final String productRevision, final String actionType, final String packageTypeTobeLogged, final JobActivityInfo jobActivityInfo) {
        String neType = null;
        final long activityJobId = jobActivityInfo.getActivityJobId();
        final JobTypeEnum jobType = jobActivityInfo.getJobType();
        NEJobStaticData neJobStaticData = null;
        String nodeName = "";
        String upMoFdn = null;
        String pkgNameTobeAppended = "";
        String neJobBusinessKey = null;
        long neJobId = 0L;
        long mainJobId = 0L;
        Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            LOGGER.debug("Job Id : {} is performing MO action : {} on UpgradePackage  MO with productNumber {} and productRevision {}", activityJobId, actionType, productNumber, productRevision);
            final String productId = activityUtils.getProductId(productNumber, productRevision);
            pkgNameTobeAppended = productId != null ? String.format(SW_PKG_NAME_APPENDER, packageTypeTobeLogged, productId) : EMPTY_STRING;
            // Obtaining nodename
            neJobId = neJobStaticData.getNeJobId();
            neJobBusinessKey = neJobStaticData.getNeJobBusinessKey();
            mainJobId = neJobStaticData.getMainJobId();
            neJobAttributes = jobConfigurationServiceProxy.getNeJobAttributes(neJobId);
            nodeName = neJobStaticData.getNodeName();
            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, MOACTION_START_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
            upMoFdn = getUpMoFdn(activityJobId, productNumber, productRevision, neJobId, neJobAttributes);
            LOGGER.debug("Retrivied UpMoFdn {} for activity {}", upMoFdn, activityJobId);
            if (actionType == null) {
                failActivityForNoActionType(activityJobId, jobLogList, pkgNameTobeAppended, neJobBusinessKey);
                return;
            }
            final NetworkElementData networkElement = networkElementRetrivalBean.getNetworkElementData(nodeName);
            neType = networkElement.getNeType();
        } catch (final MoNotFoundException | JobDataNotFoundException e) {
            LOGGER.error("Exception while fetching networkElementData for the node :  {}", nodeName);
            final String jobLogEntry = "Unable to fetch Network Element data. Failing the Install activity";
            final String logMessage = String.format("Failed  to fetch network element data for node %s", nodeName);
            logExceptionAndNotifyToWFS(activityJobId, jobLogList, nodeName, neJobBusinessKey, upMoFdn, logMessage, jobLogEntry);
            return;
        } catch (final NodeReadAttributeFailedException ex) {
            final String exceptionMessage = ex.getMessage();
            final String logMessage = String.format(JobLogConstants.NODE_READ_FAIL, nodeName);
            LOGGER.error("Unable to retrieve data for node {} activityJobId: {}. Exception is: ", nodeName, activityJobId, ex);
            final String jobLogEntry = logMessage + " " + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
            logExceptionAndNotifyToWFS(activityJobId, jobLogList, nodeName, neJobBusinessKey, upMoFdn, logMessage, jobLogEntry);
            return;
        } catch (final Exception ex) {
            LOGGER.error("An exception occured while processing execute for Upgrade activity for activityJobId: {}. Exception is:", activityJobId, ex);
            final String jobLogEntry = String.format(JobLogConstants.UNABLE_TO_PROCEED_ACTION, ActivityConstants.INSTALL, ex.getMessage());
            logExceptionAndNotifyToWFS(activityJobId, jobLogList, nodeName, neJobBusinessKey, upMoFdn, jobLogEntry, jobLogEntry);
            return;
        }
        if (upMoFdn == null || EMPTY_STRING.equals(upMoFdn)) {
            LOGGER.debug("creating UpgradePackage MO in Install if not present for nodeName {}", nodeName);
            try {
                upMoFdn = createUpgradeMO(activityJobId, productNumber, productRevision);
                LOGGER.debug("UP MO created with fdn {} for activityJobId: {}", activityJobId, upMoFdn);
            } catch (final Exception exception) {
                LOGGER.error("Unable to start Install Activity for :{} as creation of MO failed {} ", activityJobId, exception.getMessage());
                final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(exception);
                final String logEntry = appendMediationFailureReasonInJobLog(String.format(JobLogConstants.MO_CREATION_FAILED_INSTALL, ActivityConstants.INSTALL, pkgNameTobeAppended),
                        exceptionMessage);

                final String logMessage = "Unable to start Install Activity as creation of MO failed because " + exceptionMessage;
                logExceptionAndNotifyToWFS(activityJobId, jobLogList, nodeName, neJobBusinessKey, upMoFdn, logMessage, logEntry);
                return;
            }
            if (upMoFdn != null) {
                jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.UP_MO_CREATED, upMoFdn), JobLogLevel.INFO.toString()));
                persistNeJobProperty(activityJobId, upMoFdn, neJobId, neJobAttributes);
            }
        } else {
            updateFtpServerDetails(upMoFdn, neType, activityJobId, productNumber, productRevision, nodeName);
        }
        // COM_ERICSSON_OSS_ITPF_COMMAND_LOGGER-Started
        systemRecorder.recordCommand(SHMEvents.INSTALL_SERVICE, CommandPhase.STARTED, nodeName, upMoFdn, activityUtils.additionalInfoForCommand(activityJobId, mainJobId, jobType));
        triggerInstallActionOnNode(actionType, neType, jobActivityInfo, jobLogList, pkgNameTobeAppended, neJobStaticData, upMoFdn);
    }

    private void triggerInstallActionOnNode(final String actionType, final String neType, final JobActivityInfo jobActivityInfo, final List<Map<String, Object>> jobLogList,
            final String pkgNameTobeAppended, final NEJobStaticData neJobStaticData, final String upMoFdn) {
        final long activityJobId = jobActivityInfo.getActivityJobId();
        final JobTypeEnum jobType = jobActivityInfo.getJobType();
        final String nodeName = neJobStaticData.getNodeName();
        final long mainJobId = neJobStaticData.getMainJobId();
        final FdnNotificationSubject fdnNotificationSubject = new FdnNotificationSubject(upMoFdn, activityJobId, jobActivityInfo);
        notificationRegistry.register(fdnNotificationSubject);
        // COM_ERICSSON_OSS_ITPF_COMMAND_LOGGER-Started
        systemRecorder.recordCommand(SHMEvents.INSTALL_SERVICE, CommandPhase.STARTED, nodeName, upMoFdn, activityUtils.additionalInfoForCommand(activityJobId, mainJobId, jobType));
        String logMessage;
        int actionId = -1;
        try {
            LOGGER.debug("Performing action on FDN {} == {}", upMoFdn, actionType);
            actionId = dpsWriter.performAction(upMoFdn, actionType, new HashMap<String, Object>(), dpsRetryPolicies.getDpsMoActionRetryPolicy());
            LOGGER.debug("ActionId for activity {} : {}", activityJobId, actionId);
            final Integer activityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.CPP.name(), JobTypeEnum.UPGRADE.name(), ACTIVITYNAME_INSTALL);
            final String formattedLog = String.format(String.format(JobLogConstants.ASYNC_ACTION_TRIGGERED, ActivityConstants.INSTALL, activityTimeout));
            final String joblogMessage = formattedLog.substring(0, formattedLog.length() - 1).concat(pkgNameTobeAppended);
            jobLogList.add(activityUtils.createNewLogEntry(joblogMessage, JobLogLevel.INFO.toString()));
            //Below properties will be used for each package, so clear them before going for another package install.
            final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
            activityUtils.addJobProperty(ActivityConstants.ACTION_ID, Integer.toString(actionId), jobPropertyList);
            if (actionId != -1) {
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, "true");
            }
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            logMessage = "MO Action Initiated with action:" + actionType + " on UP MO having FDN: " + upMoFdn;
            LOGGER.debug("{} for activity {}", logMessage, activityJobId);
            // COM_ERICSSON_OSS_ITPF_COMMAND_LOGGER-Success
            systemRecorder.recordCommand(SHMEvents.INSTALL_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, upMoFdn, activityUtils.additionalInfoForCommand(activityJobId, mainJobId, jobType));
            activityUtils.recordEvent(SHMEvents.INSTALL_EXECUTE, nodeName, upMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
        } catch (final Exception e) {
            if (actionId == -1) {
                logMessage = "Unable to start MO Action with action: " + actionType + " on UP MO having FDN: " + upMoFdn;
                notificationRegistry.removeSubject(fdnNotificationSubject);
                final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
                String logEntry = null;
                if (!exceptionMessage.isEmpty()) {
                    logEntry = String.format(JobLogConstants.INSTALL_TRIGGER_FAILED_WITH_REASON, ActivityConstants.INSTALL, pkgNameTobeAppended, exceptionMessage);
                } else {
                    logEntry = String.format(JobLogConstants.INSTALL_TRIGGER_FAILED, ActivityConstants.INSTALL, pkgNameTobeAppended);
                }
                logExceptionAndNotifyToWFS(activityJobId, jobLogList, nodeName, neJobStaticData.getNeJobBusinessKey(), upMoFdn, logMessage, logEntry);
                systemRecorder.recordCommand(SHMEvents.INSTALL_SERVICE, CommandPhase.FINISHED_WITH_ERROR, nodeName, upMoFdn, activityUtils.additionalInfoForCommand(activityJobId, mainJobId, jobType));
            } else {
                logMessage = "Unexpected error occured while executing" + actionType + " on UP MO having FDN: " + upMoFdn;
            }
            LOGGER.error(logMessage, e);
            return;
        }
    }

    /**
     * Logs the exception into recording events and updates the job result as failed into job properties
     * 
     * @param activityJobId
     * @param jobLogList
     * @param pkgNameTobeAppended
     * @param businessKey
     * @param logMessage
     */
    private void logExceptionAndNotifyToWFS(final long activityJobId, final List<Map<String, Object>> jobLogList, final String nodeName, final String businessKey, final String upMoFdn,
            final String logMessage, final String jobLogEntry) {
        jobLogList.add(activityUtils.createNewLogEntry(jobLogEntry, JobLogLevel.ERROR.toString()));
        activityUtils.recordEvent(SHMEvents.INSTALL_EXECUTE, nodeName, upMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        //Persist Result as Failed in case of unable to trigger action.
        final Map<String, Object> result = new HashMap<String, Object>();
        result.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTIVITY_RESULT);
        result.put(ActivityConstants.JOB_PROP_VALUE, JobResult.FAILED.toString());
        jobPropertyList.add(result);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        sendActivateToWFS(activityJobId, businessKey, "execute");
        return;
    }

    /**
     * Updates the upgrade package MO with the ftp server details by querying the details from the SMRS service.
     * 
     * @param upMoFdn
     * @param neType
     * @param activityJobId
     */
    private boolean updateFtpServerDetails(final String upMoFdn, final String neType, final long activityJobId, final String productNumber, final String productRevision, final String nodeName) {
        boolean isFTPDataUpdated = false;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        try {
            // Obtaining details from SMRS file store
            LOGGER.debug("Updating SMRS details for UP MO Fdn = {},neType = {},activityJobId = {}", upMoFdn, neType, activityJobId);
            final SmrsAccountInfo smrsAccountInfo = smrsServiceUtil.getSmrsDetails(SmrsServiceConstants.SOFTWARE_ACCOUNT, neType, nodeName);
            final String ftpServerIpAddress = smrsAccountInfo.getServerIpAddress();
            final String ftpServerUserId = smrsAccountInfo.getUser();
            final String ftpServerPassword = new String(smrsAccountInfo.getPassword());

            String upFilePathOnFtpServer = "";
            if (productNumber == null || productRevision == null) {
                final Map<String, String> swPkgandUcfMap = upgradePackageService.getSwPkgNameandUcfName(activityJobId);
                final String swPkgName = swPkgandUcfMap.get(UpgradeActivityConstants.SWP_NAME);
                final String ucfName = swPkgandUcfMap.get(UpgradeActivityConstants.UCF);
                LOGGER.debug("swPkgName {} and ucfName {} in createUpgradeMO() for activityJobId is {}", swPkgName, ucfName, activityJobId);

                // Getting upFilePathOnFtpServer
                upFilePathOnFtpServer = upgradePackageService.getUcfFile(swPkgName, ucfName);
                LOGGER.debug("UpFilePathOnFtpServer for activity {} is {}", activityJobId, upFilePathOnFtpServer);

            } else {
                //Retrieve the UCF file path from SMRS where it matches with productID
                upFilePathOnFtpServer = upgradePackageService.readUCFItemsFromDB(productNumber, productRevision);
            }

            //UCF File obtained from SoftwarePackageService is absolute.
            //Below Code written is to get the relative path.
            String relativePath = EMPTY_STRING;
            if (upFilePathOnFtpServer.contains(smrsAccountInfo.getSmrsRootDirectory())) {
                relativePath = upFilePathOnFtpServer.replace(smrsAccountInfo.getSmrsRootDirectory(), EMPTY_STRING);
            } else {
                LOGGER.error("There is no SMRS root path starting with /home/smrs and the obtained path is:{}", upFilePathOnFtpServer);
            }

            final Map<String, Object> actionArguments = new HashMap<String, Object>();
            actionArguments.put(UpgradeActivityConstants.ACTION_ARG_USER_ID, ftpServerUserId);
            actionArguments.put(UpgradeActivityConstants.ACTION_ARG_PASSWORD, ftpServerPassword);
            actionArguments.put(UpgradeActivityConstants.ACTION_ARG_IP_ADDRESS, ftpServerIpAddress);
            actionArguments.put(UpgradeActivityConstants.ACTION_ARG_UPFILE_PATH_ON_FTPSERVER, relativePath);

            if (isFTPdataChangedOnNode(upMoFdn, actionArguments)) {
                try {
                    LOGGER.debug("Arguments for updateFTPServerData ftpuser={},ftpipAddress={},upFilePathOnFtpServer={}", ftpServerUserId, ftpServerIpAddress, relativePath);
                    final int actionId = dpsWriter.performAction(upMoFdn, UpgradeActivityConstants.ACTION_UPDATE_FTP_SERVER_DATA, actionArguments, dpsRetryPolicies.getDpsMoActionRetryPolicy());
                    LOGGER.debug("ActionId for activity {} : {}", activityJobId, actionId);
                    jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.FTPSERVER_DATA_UPDATED, upMoFdn, actionId), JobLogLevel.INFO.toString()));
                } catch (final Exception e) {
                    LOGGER.warn("Execution of action [{}] failed due to {}", UpgradeActivityConstants.ACTION_UPDATE_FTP_SERVER_DATA, e.getMessage());
                    final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
                    if (!exceptionMessage.isEmpty()) {
                        jobLogList.add(activityUtils.createNewLogEntry(
                                String.format(JobLogConstants.FTPSERVER_DATA_NOT_UPDATED, upMoFdn) + ". " + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage),
                                JobLogLevel.INFO.toString()));
                    } else {
                        jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.FTPSERVER_DATA_NOT_UPDATED, upMoFdn), JobLogLevel.INFO.toString()));
                    }
                }
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList);
            } else {
                LOGGER.info("FTP server details on node did not changed, so execution of action[{}] skipped.", UpgradeActivityConstants.ACTION_UPDATE_FTP_SERVER_DATA);
            }
            isFTPDataUpdated = true;
        } catch (final Exception exception) {
            LOGGER.error("Unable to perform install activity for node {}due to ", nodeName, exception);
            activityUtils.prepareJobLogAtrributesList(jobLogList, "Unable to perform install activity due to : " + exception.getMessage(), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList);
            isFTPDataUpdated = false;

        }

        return isFTPDataUpdated;
    }

    /**
     * compares the node FTP data with SMRS FTP data
     * 
     * @param upMoFdn
     * @param actionArguments
     * @return
     */
    private boolean isFTPdataChangedOnNode(final String upMoFdn, final Map<String, Object> newFTPdata) {

        final Map<String, Object> upMoAttributes = upgradePackageService.getUpMoAttributesByFdn(upMoFdn, UPMO_ATTRIBUTES);
        final String newUser = (String) newFTPdata.get(UpgradeActivityConstants.ACTION_ARG_USER_ID);
        final String newPwd = (String) newFTPdata.get(UpgradeActivityConstants.ACTION_ARG_PASSWORD);
        final String newIP = (String) newFTPdata.get(UpgradeActivityConstants.ACTION_ARG_IP_ADDRESS);
        final String newFilePath = (String) newFTPdata.get(UpgradeActivityConstants.ACTION_ARG_UPFILE_PATH_ON_FTPSERVER);

        final String oldUser = (String) upMoAttributes.get(UpgradePackageMoConstants.UP_MO_USER);
        final String oldPwd = (String) upMoAttributes.get(UpgradePackageMoConstants.UP_MO_PASSWORD);
        final String oldIP = (String) upMoAttributes.get(UpgradePackageMoConstants.UP_MO_FTP_SERVER_IP_ADDRESS);
        final String oldFilePath = (String) upMoAttributes.get(UpgradePackageMoConstants.UP_MO_UP_FILEPATH_ON_FTP_SERVER);

        if (!oldUser.equals(newUser) || !oldPwd.equals(newPwd) || !oldIP.equals(newIP) || !oldFilePath.equals(newFilePath)) {
            LOGGER.debug("FTP details were modified, needs to trigger an action on the UP MO fdn to update the new details");
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param activityJobId
     * @param jobLogList
     * @param pkgNameTobeAppended
     * @param activityResult
     * @param businessKey
     */
    private void failActivityForNoActionType(final long activityJobId, final List<Map<String, Object>> jobLogList, final String pkgNameTobeAppended, final String businessKey) {
        final String formattedLog = String.format(String.format(JobLogConstants.INSUFFICIENT_INPUTS, ActivityConstants.INSTALL));
        final String logMessage = formattedLog.substring(0, formattedLog.length() - 1).concat(pkgNameTobeAppended);
        jobLogList.add(activityUtils.createNewLogEntry(logMessage, JobLogLevel.ERROR.toString()));
        //Persist Result as Failed in case of unable to trigger action.
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> result = new HashMap<String, Object>();
        result.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTIVITY_RESULT);
        result.put(ActivityConstants.JOB_PROP_VALUE, JobResult.FAILED.toString());
        jobPropertyList.add(result);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList);
        sendActivateToWFS(activityJobId, businessKey, "execute");
        return;
    }

    /**
     * This method processes the notifications by fetching the notification subject and validates the notification. It de-register from the notification as it founds activity is completed and notifies
     * to WorkFlowService or else it will wait for another notification.
     * 
     * @param notification
     * @param jobActivityInfo
     * @return boolean.
     * 
     */
    public Map<String, Object> processNotification(final Notification notification, final JobActivityInfo jobActivityInfo) {
        LOGGER.debug("Entering InstallService.processNotification() with notificationSubject:{}", notification.getNotificationSubject());
        final Map<String, Object> jobState = new HashMap<String, Object>();
        boolean isActivityCompleted = false;
        JobResult jobResult = JobResult.FAILED;
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(jobActivityInfo.getActivityJobId(), SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
            final long activityJobId = jobActivityInfo.getActivityJobId();
            final Map<String, AttributeChangeData> modifiedAttr = activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent());
            LOGGER.debug("Inside Install Service processNotification with modified attributes : {} for activityJobId : {}", modifiedAttr , activityJobId);
            final UpgradeProgressInformation progressHeader = getCurrentProgressHeader(modifiedAttr);
            final UpgradePackageState currentUpState = getCurrentUpState(modifiedAttr);
            final List<Map<String, Object>> actionResultData = getActionResult(modifiedAttr);

            reportUPProgress(jobLogList, progressHeader, currentUpState);

            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList);
            final Map<String, Object> cancelExecutedOrFailedMap = evaluateCancelMainActionResult(neJobStaticData, actionResultData, activityJobId);

            if (!cancelExecutedOrFailedMap.isEmpty()) {
                if ((boolean) cancelExecutedOrFailedMap.get(INSTALL_CANCEL_EXECUTED) || isInstallCancelled(currentUpState, activityJobId)) {
                    reportInstallCancelStatusToWFS(jobLogList, jobPropertyList, activityJobId, neJobStaticData);
                } else if ((boolean) cancelExecutedOrFailedMap.get(INSTALL_CANCEL_FAILED)) {
                    activityUtils.addJobLog(String.format(JobLogConstants.CANCELLATION_FAILED, ActivityConstants.INSTALL), JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
                    jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList);
                }
            } else {
                if (isInstallSuccess(currentUpState)) {
                    jobResult = JobResult.SUCCESS;
                    isActivityCompleted = true;
                } else if (actionResultData != null && !actionResultData.isEmpty()) {
                    final Map<String, Object> mainActionResult = getMainActionResult(neJobStaticData, actionResultData, false, activityJobId);
                    if (!mainActionResult.isEmpty()) {
                        jobResult = evalutateMainActionResult(mainActionResult);
                        isActivityCompleted = true;
                    }
                }

                if (isActivityCompleted && jobActivityInfo.getJobType() == JobTypeEnum.UPGRADE) {
                    activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, null, MOACTION_END_PROGRESS_PERCENTAGE);
                    activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
                    notifyWfs(jobResult, neJobStaticData, jobActivityInfo);
                    LOGGER.debug("Exiting InstallService.processNotification() with NodeName={},result={}", neJobStaticData.getNodeName(), jobResult);
                }
            }
        } catch (final Exception e) {
            LOGGER.error("An exception occured while processing notification for install activity. Exception is :", e);
            isActivityCompleted = false;
        }
        jobState.put(ActivityConstants.ACTIVITY_RESULT, jobResult);
        jobState.put(ActivityConstants.ACTIVITY_STATUS, isActivityCompleted);
        return jobState;
    }

    /**
     * @param jobResult
     * @param jobLogList
     * @param jobPropertyList
     * @param activityJobId
     * @param jobEnvironment
     */
    private void reportInstallCancelStatusToWFS(final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> jobPropertyList, final long activityJobId,
            final NEJobStaticData neJobStaticData) {
        activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, JobResult.CANCELLED.getJobResult(), jobPropertyList);
        activityUtils.addJobLog(String.format(JobLogConstants.ACTIVITY_JOB_CANCELLED, ActivityConstants.INSTALL), JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList);
        final String businessKey = neJobStaticData.getNeJobBusinessKey();
        sendCancelMOActionDoneToWFS(activityJobId, businessKey, ActivityConstants.INSTALL);
    }

    private JobResult evalutateMainActionResult(final Map<String, Object> mainActionResult) {

        JobResult jobResult = JobResult.FAILED;
        final ActionResultInformation actionResultInfo = (ActionResultInformation) mainActionResult.get(UpgradePackageMoConstants.UP_ACTION_RESULT_INFO);

        if (actionResultInfo == ActionResultInformation.EXECUTED || actionResultInfo == ActionResultInformation.EXECUTED_WITH_WARNINGS) {
            jobResult = JobResult.SUCCESS;
        }

        return jobResult;
    }

    private Map<String, Object> evaluateCancelMainActionResult(final NEJobStaticData neJobStaticData, final List<Map<String, Object>> actionResultData, final long activityJobId) {
        final Map<String, Object> cancelExecutedOrFailedMap = new HashMap<String, Object>();
        if (actionResultData != null && !actionResultData.isEmpty()) {
            final Map<String, Object> cancelMainActionResult = getMainActionResult(neJobStaticData, actionResultData, true, activityJobId);
            LOGGER.debug("cancelMainActionResult {}", cancelMainActionResult);
            if (!cancelMainActionResult.isEmpty()) {
                final ActionResultInformation actionResultInfo = (ActionResultInformation) cancelMainActionResult.get(UpgradePackageMoConstants.UP_ACTION_RESULT_INFO);
                if (actionResultInfo == ActionResultInformation.EXECUTED || actionResultInfo == ActionResultInformation.EXECUTED_WITH_WARNINGS) {
                    cancelExecutedOrFailedMap.put(INSTALL_CANCEL_EXECUTED, true);
                } else if (actionResultInfo == ActionResultInformation.UNSPECIFIED || actionResultInfo == ActionResultInformation.EXECUTION_FAILED
                        || actionResultInfo == ActionResultInformation.ACTION_NOT_ALLOWED) {
                    cancelExecutedOrFailedMap.put(INSTALL_CANCEL_FAILED, true);
                }

            }
        }
        return cancelExecutedOrFailedMap;
    }

    private void notifyWfs(final JobResult jobResult, final NEJobStaticData neJobStaticData, final JobActivityInfo jobActivityInfo) {
        final long activityJobId = jobActivityInfo.getActivityJobId();

        LOGGER.debug("Action is completed for activity {} with activityJobId:{} node name {} . Starting wait timer", ActivityConstants.INSTALL, activityJobId, neJobStaticData.getNodeName());

        final String upMoFdn = getUpMoFdn(activityJobId, neJobStaticData.getNeJobId(), jobConfigurationServiceProxy.getNeJobAttributes(neJobStaticData.getNeJobId()));
        final FdnNotificationSubject fdnNotificationSubject = new FdnNotificationSubject(upMoFdn, activityJobId, jobActivityInfo);
        final boolean isNotificationRemoved = notificationRegistry.removeSubject(fdnNotificationSubject);
        activityUtils.recordEvent(SHMEvents.INSTALL_PROCESS_NOTIFICATION, neJobStaticData.getNodeName(), upMoFdn, "SHM:" + activityJobId + ":" + ActivityConstants.INSTALL);
        if (isNotificationRemoved) {
            LOGGER.debug("Exiting InstallService.processNotification() with NodeName={},result={}", neJobStaticData.getNodeName(), jobResult);
            reportJobStateToWFS(neJobStaticData, jobResult, ActivityConstants.INSTALL, activityJobId);
        } else {
            LOGGER.info("Notification Subject is already removed for the given node by other thread. Ignoring the current thread");
        }

    }

    /**
     * This method handles timeout scenario for Install Activity and checks the state on node to see if it is failed or success.
     * 
     * @param activityJobId
     * @param jobActivityInfo
     * @param jobActivityInfo
     * @return ActivityStepResult
     * 
     */
    public ActivityStepResult handleTimeout(final JobActivityInfo jobActivityInfo) {
        final long activityJobId = jobActivityInfo.getActivityJobId();
        LOGGER.debug("In handle timeout with activity id {}", activityJobId);
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            activityStepResultEnum = processTimeout(jobActivityInfo, jobLogList, jobPropertyList, neJobStaticData);
        } catch (final Exception e) {
            final String errorMsg = "An exception occured while handlingTimeout for activityJobId :" + activityJobId + ". Exception is: ";
            LOGGER.error(errorMsg, e);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            String jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.INSTALL);
            if (!exceptionMessage.isEmpty()) {
                jobLogMessage += String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
            } else {
                jobLogMessage += String.format(JobLogConstants.FAILURE_REASON, e);
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult(), jobPropertyList);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        }
        return activityUtils.getActivityStepResult(activityStepResultEnum);
    }

    private ActivityStepResultEnum processTimeout(final JobActivityInfo jobActivityInfo, final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> jobPropertyList,
            final NEJobStaticData neJobStaticData) {
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        final long neJobId = neJobStaticData.getNeJobId();
        final long activityJobId = jobActivityInfo.getActivityJobId();
        final String upMoFdn = getUpMoFdn(activityJobId, neJobId, jobConfigurationServiceProxy.getNeJobAttributes(neJobId));
        activityUtils.unSubscribeToMoNotifications(upMoFdn, activityJobId, jobActivityInfo);
        final String logMessage = String.format(JobLogConstants.TIMEOUT, ActivityConstants.INSTALL);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        jobLogList.clear();
        activityUtils.recordEvent(SHMEvents.INSTALL_TIME_OUT, neJobStaticData.getNodeName(), upMoFdn, "SHM:" + activityJobId + ":" + neJobStaticData.getNodeName() + ":" + logMessage);
        if (StringUtils.isEmpty(upMoFdn)) {
            activityUtils.addJobLog(String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.INSTALL, JobLogConstants.FAILURE_REASON, JobLogConstants.UP_MO_FDN_IS_EMPTY),
                    JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
            activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult(), jobPropertyList);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            return activityStepResultEnum;
        }
        final JobResult jobResult = evaluateJobResult(neJobStaticData, upMoFdn, jobActivityInfo.getActivityJobId());
        if (jobResult != null && jobResult == JobResult.SUCCESS) {
            activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS;
            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, HANDLE_TIMEOUT_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
        } else if (jobResult != null && jobResult == JobResult.FAILED) {
            activityUtils.addJobLog(String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.INSTALL), JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
            activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult(), jobPropertyList);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        }
        return activityStepResultEnum;
    }

    /**
     * Performs the cancelInstall Action on the Node for given activity Job ID.
     * 
     * @param jobActivityInfo
     * @return ActivityStepResult
     */
    public ActivityStepResult cancel(final JobActivityInfo jobActivityInfo) {
        final long activityJobId = jobActivityInfo.getActivityJobId();
        final JobTypeEnum jobType = jobActivityInfo.getJobType();
        LOGGER.debug("Inside InstallService.cancel() with activityJobId {}", activityJobId);
        NEJobStaticData neJobStaticData = null;
        String actionType = null;
        String upMoFdn = null;
        String nodeName = null;
        String logMessage = null;
        String neType = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            final long neJobId = neJobStaticData.getNeJobId();
            final long mainJobId = neJobStaticData.getMainJobId();
            final Map<String, Object> neJobAttributes = jobConfigurationServiceProxy.getNeJobAttributes(neJobId);
            nodeName = neJobStaticData.getNodeName();
            upMoFdn = getUpMoFdn(activityJobId, neJobId, neJobAttributes);
            actionType = UpgradeActivityConstants.ACTION_CANCEL_INSTALL;
            final Map<String, Object> actionArguments = new HashMap<String, Object>();
            activityUtils.logCancelledByUser(jobLogList, jobConfigurationServiceProxy.getMainJobAttributes(mainJobId), jobConfigurationServiceProxy.getNeJobAttributes(neJobId),
                    ActivityConstants.INSTALL);
            final FdnNotificationSubject fdnNotificationSubject = new FdnNotificationSubject(upMoFdn, activityJobId, jobActivityInfo);
            notificationRegistry.register(fdnNotificationSubject);
            final int actionId = dpsWriter.performAction(upMoFdn, actionType, actionArguments, dpsRetryPolicies.getDpsMoActionRetryPolicy());
            LOGGER.debug("cancel actionId : {}", actionId);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_SUCESS);
            logMessage = String.format(JobLogConstants.ACTION_TRIGGERING, actionType) + upMoFdn;
            final NetworkElementData networkElement = networkElementRetrivalBean.getNetworkElementData(nodeName);
            neType = networkElement.getNeType();
            final Integer installTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.CPP.name(), jobType.name(), actionType);
            jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.ASYNC_ACTION_TRIGGERED, actionType, installTimeout), JobLogLevel.INFO.toString()));
            activityUtils.addJobProperty(ActivityConstants.CANCEL_ACTION_ID, Integer.toString(actionId), jobPropertyList);
            activityUtils.addJobProperty(ActivityConstants.IS_CANCEL_TRIGGERED, "true", jobPropertyList);
        } catch (final Exception e) {
            LOGGER.error("Unable to start MO action due to:", e);
            logMessage = "Unable to start MO Action with action: " + actionType + " on UP MO having FDN: " + upMoFdn + " because " + e.getMessage();
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            if (!exceptionMessage.isEmpty()) {
                jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, actionType, exceptionMessage), JobLogLevel.ERROR.toString()));
            } else {
                jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.ACTION_TRIGGERED, actionType), JobLogLevel.ERROR.toString()));
            }
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);

        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList);
        activityUtils.recordEvent(SHMEvents.INSTALL_CANCEL, nodeName, upMoFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
        LOGGER.debug(logMessage);
        return activityStepResult;
    }

    /**
     * 
     * This method is responsible for deciding the action type for install activity.
     * 
     * @param activityJobId
     * @return String
     * 
     */
    @SuppressWarnings("unchecked")
    private String getActionType(final long activityJobId) {
        String actionType = null;

        final Map<String, Object> mainAttributesMap = activityUtils.getMainJobAttributes(activityJobId);
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainAttributesMap.get(ShmConstants.JOBCONFIGURATIONDETAILS);
        final String neName = activityUtils.getNodeName(activityJobId);
        final List<String> neFdns = new ArrayList<String>();
        neFdns.add(neName);
        final List<NetworkElement> networkElementList = fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(neFdns);
        final String neType = networkElementList.get(0).getNeType();
        final String platform = networkElementList.get(0).getPlatformType().name();
        LOGGER.debug("NeType {}, platform {} in getActionType method ", neType, platform);
        final List<String> keyList = new ArrayList<String>();
        keyList.add(UpgradeActivityConstants.SELECTIVEINSTALL);
        keyList.add(UpgradeActivityConstants.FORCEINSTALL);
        final Map<String, String> keyValueMap = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neName, neType, platform);
        final String selectiveInstallValue = keyValueMap.get(UpgradeActivityConstants.SELECTIVEINSTALL);
        final String forcedInstallValue = keyValueMap.get(UpgradeActivityConstants.FORCEINSTALL);
        LOGGER.debug("Is selectiveInstall and forcedInstall for activity {} , {} ,{} ", activityJobId, selectiveInstallValue, forcedInstallValue);

        if (UpgradeActivityConstants.TYPE_SELECTIVE.equalsIgnoreCase(selectiveInstallValue)) {
            if (UpgradeActivityConstants.TRANSFER_TYPE_FULL.equalsIgnoreCase(forcedInstallValue)) {
                actionType = UpgradeActivityConstants.ACTION_SELECTIVE_FORCED_INSTALL;
            } else if (UpgradeActivityConstants.TRANSFER_TYPE_DELTA.equalsIgnoreCase(forcedInstallValue)) {
                actionType = UpgradeActivityConstants.ACTION_SELECTIVE_INSTALL;
            }
        } else {
            if (UpgradeActivityConstants.TRANSFER_TYPE_FULL.equalsIgnoreCase(forcedInstallValue)) {
                actionType = UpgradeActivityConstants.ACTION_FORCED_INSTALL;
            } else if (UpgradeActivityConstants.TRANSFER_TYPE_DELTA.equalsIgnoreCase(forcedInstallValue)) {
                actionType = UpgradeActivityConstants.ACTION_INSTALL;
            }
        }

        LOGGER.debug("Action type for activity {} : {}", activityJobId, actionType);
        return actionType;
    }

    /**
     * This method determines that whether UP MO is valid for Installation or not.
     * 
     * @param activityJobId
     * @param nodeName
     * @param upgradePackageMoData
     * @param productID
     * @return status
     */
    private ActivityStepResultEnum validateUPMoState(final long activityJobId, final String nodeName, final Map<String, Object> upgradePackageMoData, final String productNumber,
            final String productRevision, final long neJobId, final Map<String, Object> neJobAttributes, final String jobType) {
        final UpgradePackageState upState = getUPState(upgradePackageMoData);
        final String upMoFdn = getUpMoFdn(activityJobId, productNumber, productRevision, neJobId, neJobAttributes);
        ActivityStepResultEnum activityStepResultEnum;
        LOGGER.debug("In Precheck UpState for activity {} : {}", activityJobId, upState);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        if (upState == null) {

            activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
            final String logMessage = "Precheck for Install Activity has failed because Upgrade Package state is null";
            LOGGER.debug("{} for activity {}", logMessage, activityJobId);
            activityUtils.recordEvent(SHMEvents.INSTALL_PRECHECK, nodeName, upMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList);
            return activityStepResultEnum;
        }
        final String stateMessage = upState.getStateMessage();
        LOGGER.debug("In Precheck UpStateMessage for activity {} : {}", activityJobId, stateMessage);
        if (upState == UpgradePackageState.INSTALL_EXECUTING || upState == UpgradePackageState.AWAITING_CONFIRMATION || upState == UpgradePackageState.ONLY_DELETEABLE
                || upState == UpgradePackageState.UPGRADE_EXECUTING) {
            activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
            final String logMessage = "Upgrade Package is not ready for Installation because " + stateMessage;
            LOGGER.debug("{} for activity {}", logMessage, activityJobId);
            activityUtils.recordEvent(SHMEvents.INSTALL_PRECHECK, nodeName, upMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
        } else if (jobType.equals(JobTypeEnum.UPGRADE.name()) && upgradeJobConfigurationListener.isSkipInstallActivityEnabled() && isUpAlreadyInstalledOnNode(upState)) {
            activityStepResultEnum = ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION;
            final String logMessage = String.format(JobLogConstants.PRE_CHECK_ACTIVITY_SKIP, ActivityConstants.INSTALL);
            LOGGER.debug("{} for activity {}", logMessage, activityJobId);
            activityUtils.recordEvent(SHMEvents.INSTALL_PRECHECK, nodeName, upMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
        } else {
            activityStepResultEnum = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
            final String logMessage = "Proceeding Install Activity as " + stateMessage;
            LOGGER.debug("{} for activity {}", logMessage, activityJobId);
            activityUtils.recordEvent(SHMEvents.INSTALL_PRECHECK, nodeName, upMoFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
        }

        return activityStepResultEnum;
    }

    private boolean isUpAlreadyInstalledOnNode(final UpgradePackageState upState) {
        if (upState == UpgradePackageState.INSTALL_COMPLETED || upState == UpgradePackageState.UPGRADE_COMPLETED) {
            return true;
        }
        return false;
    }

    private JobResult evaluateJobResult(final NEJobStaticData neJobStaticData, final String upMoFdn, final long activityJobId) {
        JobResult jobResult = JobResult.FAILED;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final String[] attributeNames = { UpgradePackageMoConstants.UP_ACTION_RESULT, UpgradePackageMoConstants.UP_MO_STATE };
        final Map<String, Object> upgradePackageMoData = getUpMoAttributesByFdn(upMoFdn, attributeNames);
        final UpgradePackageState currentUpState = getUPState(upgradePackageMoData);

        final List<Map<String, Object>> actionResultDataList = (List<Map<String, Object>>) upgradePackageMoData.get(UpgradePackageMoConstants.UP_ACTION_RESULT);
        LOGGER.trace("Action Result data {} fetched for upMOFdn {}", actionResultDataList, upMoFdn);
        final Map<String, Object> cancelExecutedOrFailedMap = evaluateCancelMainActionResult(neJobStaticData, actionResultDataList, activityJobId);

        if (!cancelExecutedOrFailedMap.isEmpty()) {
            if ((boolean) cancelExecutedOrFailedMap.get(INSTALL_CANCEL_EXECUTED) || isInstallCancelled(currentUpState, activityJobId)) {
                reportInstallCancelStatusToWFS(jobLogList, jobPropertyList, activityJobId, neJobStaticData);

            } else if ((boolean) cancelExecutedOrFailedMap.get(INSTALL_CANCEL_FAILED)) {
                activityUtils.addJobLog(String.format(JobLogConstants.CANCELLATION_FAILED, ActivityConstants.INSTALL), JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList);

            }
        } else {
            if (isInstallSuccess(currentUpState)) {
                jobResult = JobResult.SUCCESS;
                activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult(), jobPropertyList);
                activityUtils.addJobLog(String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, ActivityConstants.INSTALL), JobLogType.SYSTEM.toString(), jobLogList,
                        JobLogLevel.INFO.toString());
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList);
            } else {
                final Map<String, Object> mainActionResult = getMainActionResult(neJobStaticData, actionResultDataList, false, activityJobId);
                if (mainActionResult.isEmpty()) {
                    jobResult = JobResult.FAILED;
                    final String jobLogMessage = JobLogConstants.MAIN_ACTION_RESULT_NOT_FOUND;
                    activityUtils.addJobLog(jobLogMessage, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
                    jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList);
                    LOGGER.warn("Unable to find main action result for activityJobId {}.", activityJobId);
                }
            }
        }
        return jobResult;
    }

    private static boolean isInstallSuccess(final UpgradePackageState currentUpState) {
        return currentUpState == UpgradePackageState.INSTALL_COMPLETED || currentUpState == UpgradePackageState.UPGRADE_COMPLETED;
    }

    private boolean isInstallCancelled(final UpgradePackageState currentUpState, final long activityJobId) {
        return activityUtils.cancelTriggered(activityJobId) && currentUpState == UpgradePackageState.INSTALL_NOT_COMPLETED;
    }

    public void precheckHandleTimeout(final long activityJobId) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Integer precheckTimeout = activityTimeoutsService.getPrecheckTimeoutAsInteger();
        jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.PRECHECK_TIMEOUT, ActivityConstants.INSTALL, precheckTimeout), JobLogLevel.ERROR.toString()));
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList);
    }

    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean isCancelRetriesExhausted) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_ACTIVITY_ONGOING);
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            final long neJobId = neJobStaticData.getNeJobId();
            final String upMoFdn = getUpMoFdn(activityJobId, neJobId, jobConfigurationServiceProxy.getNeJobAttributes(neJobId));
            final JobResult jobResult = evaluateJobResult(neJobStaticData, upMoFdn, activityJobId);
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            setTimeoutResultForCancel(activityStepResult, jobResult, isCancelRetriesExhausted);
            if (isCancelRetriesExhausted) {
                final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
                activityUtils.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.STILL_EXECUTING, ActivityConstants.INSTALL), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.ERROR.toString());
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList);
            }
        } catch (final Exception e) {
            final String errorMsg = "An exception occured while handlingTimeout for activityJobId :" + activityJobId + ". Exception is: ";
            LOGGER.error(errorMsg, e);
        }
        return activityStepResult;
    }

    private static void setTimeoutResultForCancel(final ActivityStepResult activityStepResult, final JobResult jobResult, final boolean finalizeResult) {
        if (finalizeResult && jobResult == null) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        } else if (jobResult == JobResult.SUCCESS || jobResult == JobResult.SKIPPED) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        } else if (jobResult == JobResult.FAILED) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        } else {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_ACTIVITY_ONGOING);
        }

    }
}
