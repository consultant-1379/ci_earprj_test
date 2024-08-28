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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.stream.JsonGenerationException;

import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMO;
import com.ericsson.oss.services.shm.es.impl.cpp.common.UpgradePackageMO;
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.DeleteUpgradePackageActionInfo;
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.DeleteUpgradePackageDataCollector;
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.DeleteUpgradePackageUtil;
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.ProductDataBean;
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.UpgradePackageDeletePreventingInfoProvider;
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.persistjobdata.PersistJobData;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@RequestScoped
@SuppressWarnings("PMD.TooManyFields")
public class PreCheckHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreCheckHandler.class);

    @Inject
    private DeleteUpgradePackageUtil deleteUpgradePackageUtil;

    @Inject
    private UpgradePackageDeletePreventingInfoProvider upgradePackageDeletePreventingInfoProvider;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    protected ActivityUtils activityUtils;

    @Inject
    protected JobUpdateService jobUpdateService;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Inject
    private PersistJobData persistJobData;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    DeleteUpgradePackageDataCollector deleteUpgradePackageDataCollector;

    String nodeName = "";
    String platformType = null;
    JobEnvironment jobEnvironment = null;
    String neType = null;
    NetworkElementData networkElement = null;

    long neJobId = 0;
    Map<String, HashMap<String, Object>> preventingUpsAndCvs = new HashMap<String, HashMap<String, Object>>();
    NEJobStaticData neJobStaticData = null;
    DeleteUpgradePackageActionInfo deleteUpgradePackageCommand = null;
    List<ProductDataBean> listProductData = null;
    UpgradePackageMO activeUpgradePackage = null;
    private boolean preCheckSuccessFlag = false;
    private boolean preCheckSkippedFlag = false;

    final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
    final List<Map<String, Object>> jobProperties = new ArrayList<Map<String, Object>>();
    ActivityStepResult activityStepResult = new ActivityStepResult();

    public ActivityStepResult performPreCheck(final long activityJobId) {
        LOGGER.debug("Entered Delete Upgrade Package activity precheck with activityJobId : {}", activityJobId);
        try {
            initializeVariables(activityJobId);
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ShmConstants.DELETEUPGRADEPKG_ACTIVITY);
            if (!isUserAuthorized) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
                return activityStepResult;
            }
            if (activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION
                    || activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION) {
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogs, null);
                return activityStepResult;
            }
            initiateActivity(activityJobId, jobLogs, neJobId);
            for (ProductDataBean productDataBean : listProductData) {
                try {
                    if (deleteUpgradePackageUtil.isActiveUPSelectedForDeletion(activeUpgradePackage, productDataBean)) {
                        final String logMessage = String.format(JobLogConstants.UP_MO_DATA, productDataBean.getProductNumber(), productDataBean.getProductRevision(), "is active");
                        jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                        LOGGER.debug("Active UpgradePackage:{} can not be deleted.", activeUpgradePackage);
                        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
                    } else {
                        preventingUpsAndCvs = upgradePackageDeletePreventingInfoProvider.findPreventiveUpsAndCvs(activityJobId, deleteUpgradePackageCommand, productDataBean, preventingUpsAndCvs);
                        activityStepResult.setActivityResultEnum(upgradePackageDeletePreventingInfoProvider.getActivityStepResult());
                    }
                } catch (Exception exception) {
                    buildJobLogsForNodeException(exception);
                    activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
                }
                if (activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION)) {
                    preCheckSuccessFlag = true;
                } else if (activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION)) {
                    preCheckSkippedFlag = true;
                }
            }
            buildPropertiesToBePersisted();

        } catch (final Exception exception) {
            buildJobLogsForNodeException(exception);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        }
        persistPropertiesLogsAndProgress(activityJobId);
        return activityStepResult;
    }

    private void initiateActivity(final long activityJobId, final List<Map<String, Object>> jobLogList, final long neJobId) {
        LOGGER.debug("Performing  initiate Activity for Delete Upgrade Package for activityJobId:{}", activityJobId);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_INITIATED, ActivityConstants.DELETE_UP_DISPLAY_NAME), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.INFO.toString());

        final String treatAsInfo = activityUtils.isTreatAs(nodeName);
        if (treatAsInfo != null) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, treatAsInfo, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        }
        persistJobData.persistPropertiesLogsAndProgress(activityJobId, neJobId, jobProperties, jobLogs, ProgressPercentageConstants.PRECHECK_START_PROGRESS_PERCENTAGE);
    }

    private void initializeVariables(final long activityJobId) {
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY);
            platformType = neJobStaticData.getPlatformType();
            neJobId = neJobStaticData.getNeJobId();
            nodeName = neJobStaticData.getNodeName();
            networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
            neType = networkElement.getNeType();
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
            final ConfigurationVersionMO configurationVersionMO = deleteUpgradePackageUtil.getCvMO(nodeName);
            if (configurationVersionMO == null) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.FAILED_TO_READ_ACTIVE_SOFTWARE, nodeName), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.INFO.toString());
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
                return;
            }
            deleteUpgradePackageCommand = getActionArguments(activityJobId, configurationVersionMO);
            if (activeUpgradePackage == null) {
                activeUpgradePackage = deleteUpgradePackageUtil.getActiveUpgradePackageMO(configurationVersionMO);
            }
            jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
            listProductData = deleteUpgradePackageCommand.getProductData();
        } catch (final MoNotFoundException moNotFoundException) {
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            LOGGER.error("Initialization of Variables failed for node {} due to: ", nodeName, moNotFoundException);
        } catch (JobDataNotFoundException jobDataNotFoundException) {
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            LOGGER.error("Initialization of Variables failed for node {} due to: ", nodeName, jobDataNotFoundException);
        }
    }

    @SuppressWarnings("unchecked")
    public DeleteUpgradePackageActionInfo getActionArguments(final long activityJobId, final ConfigurationVersionMO cvMo) {
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) activityUtils.getMainJobAttributes(activityJobId).get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
        LOGGER.debug("entering  delete upgrade activity precheck with jobConfigurationDetails : {} for activityJobId {} ", jobConfigurationDetails, activityJobId);
        final List<String> keyList = new ArrayList<>();
        keyList.add(UpgradeActivityConstants.DELETE_UP_LIST);
        keyList.add(UpgradeActivityConstants.IS_PREVENT_UP_DELETABALE);
        keyList.add(UpgradeActivityConstants.IS_PREVENT_CV_DELETABALE_FROM_ROLLBACKLIST);
        keyList.add(JobPropertyConstants.DELETE_NON_ACTIVE_UPS);
        final Map<String, String> actionArguments = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neJobStaticData.getNodeName(), neType, neJobStaticData.getPlatformType());
        String deleteUPInputList = actionArguments.get(UpgradeActivityConstants.DELETE_UP_LIST);
        final String isNonActiveUpsToDelete = actionArguments.get(JobPropertyConstants.DELETE_NON_ACTIVE_UPS);
        if (deleteUPInputList != null) {
            activityUtils.prepareJobPropertyList(jobProperties, UpgradeActivityConstants.DELETABLE_UP_LIST, deleteUPInputList);
            LOGGER.debug("CPP Delete upgrade package getActionArguments deleteUPInputList : {} isNonActiveUpsToDelete : {}", deleteUPInputList, isNonActiveUpsToDelete);
            return deleteUpgradePackageUtil.getDeleteUpgradePackageActionInfo(deleteUPInputList, actionArguments);
        } else if (Boolean.parseBoolean(isNonActiveUpsToDelete)) {
            final List<UpgradePackageMO> upgradePackageMOs = deleteUpgradePackageUtil.getUpMos(nodeName);
            activeUpgradePackage = deleteUpgradePackageUtil.getActiveUpgradePackageMO(cvMo);
            deleteUPInputList = deleteUpgradePackageDataCollector.getDeletableUpgradePackageIds(upgradePackageMOs, activeUpgradePackage, cvMo);
            LOGGER.debug("CPP Delete upgrade package getInActiveUPListFromNode deleteUPInputList : {} isNonActiveUpsToDelete : {}", deleteUPInputList, isNonActiveUpsToDelete);
            if (!deleteUPInputList.isEmpty()) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.INACTIVEUP_ACTIVITY_TRIGGERED, ActivityConstants.DELETE_UP_DISPLAY_NAME), new Date(),
                        JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                activityUtils.prepareJobPropertyList(jobProperties, UpgradeActivityConstants.DELETABLE_UP_LIST, deleteUPInputList);
                return deleteUpgradePackageUtil.getDeleteUpgradePackageActionInfo(deleteUPInputList, actionArguments);
            } else {
                return preparePrecheckSkip(JobLogConstants.NO_INACTIVEUP_ACTIVITY_FAILED);
            }
        } else {
            return preparePrecheckSkip(JobLogConstants.DELETEUP_PRECHECK_SKIP);
        }

    }

    private DeleteUpgradePackageActionInfo preparePrecheckSkip(final String jobLogMessage) {
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(jobLogMessage, ActivityConstants.DELETE_UP_DISPLAY_NAME), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        final DeleteUpgradePackageActionInfo deleteUpgradePackageActionInfo = new DeleteUpgradePackageActionInfo();
        deleteUpgradePackageActionInfo.setProductData(Collections.<ProductDataBean> emptyList());
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION);
        preCheckSkippedFlag = true;
        return deleteUpgradePackageActionInfo;
    }

    private void buildJobLogsForNodeException(final Exception exception) {
        String logMessage = "";
        final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(exception);
        if (exceptionMessage != null && !exceptionMessage.isEmpty()) {
            logMessage = String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.DELETE_UP_DISPLAY_NAME, exceptionMessage);
        } else {
            logMessage = String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.DELETE_UP_DISPLAY_NAME, exception.getMessage());
        }

        jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        LOGGER.error("Exception occurred in precheck of Delete Upgrade Package for node: {} due to: ", nodeName, exception);
    }

    private void buildPropertiesToBePersisted() throws IOException, JsonGenerationException, JsonMappingException {
        final ObjectMapper mapper = new ObjectMapper();
        activityUtils.prepareJobPropertyObjectList(jobProperties, UpgradeActivityConstants.PREVENTING_UP_CV_INFO, mapper.writeValueAsString(preventingUpsAndCvs));
        activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.IS_PRECHECK_DONE, ActivityConstants.CHECK_TRUE);
    }

    private void persistPropertiesLogsAndProgress(final long activityJobId) {
        LOGGER.debug("Activity Step Result = {}", activityStepResult.getActivityResultEnum());

        buildJobLogsForProcessingItems();

        if (preCheckSuccessFlag) {
            final String logMessage = "Precheck Success.";
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        } else if (preCheckSkippedFlag) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION);
        }
        persistJobData.persistPropertiesLogsAndProgress(activityJobId, neJobId, jobProperties, jobLogs, ProgressPercentageConstants.PRECHECK_END_PROGRESS_PERCENTAGE);
    }

    @SuppressWarnings("unchecked")
    private void buildJobLogsForProcessingItems() {
        for (Entry<String, HashMap<String, Object>> processingUP : preventingUpsAndCvs.entrySet()) {
            final String[] productData = processingUP.getKey().split(UpgradeActivityConstants.UPGRADEPACKAGES_PERSISTENCE_DELIMTER);
            final String deletePreventingCVs = processingUP.getValue().get(UpgradeActivityConstants.DELETE_PREVENTING_CVS).toString();
            final List<Map<String, String>> deletePreventingUPsTobeLogged = new ArrayList<Map<String, String>>();
            final List<Map<String, String>> deletePreventingUPs = (List<Map<String, String>>) processingUP.getValue().get(UpgradeActivityConstants.DELETE_PREVENTING_UPS);
            for (Map<String, String> deletePreventingUP : deletePreventingUPs) {
                final Map<String, String> deletePreventingUPTobeLogged = new HashMap<String, String>();
                deletePreventingUPTobeLogged.put(UpgradeActivityConstants.PRODUCT_NUMBER, deletePreventingUP.get(UpgradeActivityConstants.PRODUCT_NUMBER));
                deletePreventingUPTobeLogged.put(UpgradeActivityConstants.PRODUCT_REVISION, deletePreventingUP.get(UpgradeActivityConstants.PRODUCT_REVISION));
                deletePreventingUPsTobeLogged.add(deletePreventingUPTobeLogged);
            }
            if (!deletePreventingCVs.isEmpty() || !deletePreventingUPsTobeLogged.isEmpty()) {
                final String logMessage = String.format(JobLogConstants.PROCCESSING_ITEMS_LIST, productData[0], productData[1], deletePreventingCVs, deletePreventingUPsTobeLogged.toString());
                jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            }
        }
    }

}
