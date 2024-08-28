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
package com.ericsson.oss.services.shm.es.impl.cpp.deleteup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CommonCvOperations;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMO;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.UpgradePackageMO;
import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.UpgradePackageService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

public class DeleteUpgradePackageUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteUpgradePackageUtil.class);

    @Inject
    private CommonCvOperations commonCvOperations;

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    private UpgradePackageService upgradePackageService;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceRetryProxy;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    protected JobUpdateService jobUpdateService;

    public ConfigurationVersionMO getCvMO(final String nodeName) {
        return commonCvOperations.getCVMo(nodeName);
    }

    public UpgradePackageMO getActiveUpgradePackageMO(final ConfigurationVersionMO cvMo) {
        final String currentUpgradePackage = (String) cvMo.getAllAttributes().get(ConfigurationVersionMoConstants.CURRENT_UPGRADE_PACKAGE);
        final UpgradePackageMO upMo = upgradePackageService.getUpMoByFdn(currentUpgradePackage);
        return upMo;
    }

    public List<UpgradePackageMO> getUpMos(final String nodeName) {
        return upgradePackageService.getUpMos(nodeName);
    }

    @SuppressWarnings("unchecked")
    public boolean isActiveUPSelectedForDeletion(final UpgradePackageMO activeUPMo, final ProductDataBean productDataBean) {
        if (activeUPMo == null) {
            return false;
        }
        final Map<String, String> adminData = (Map<String, String>) activeUPMo.getAllAttributes().get(UpgradeActivityConstants.ADMINISTRATIVE_DATA);
        final String nodeActiveproductNumber = adminData.get(UpgradeActivityConstants.PRODUCT_NUMBER);
        final String nodeActiveproductRevision = adminData.get(UpgradeActivityConstants.PRODUCT_REVISION);
        return nodeActiveproductNumber.equalsIgnoreCase(productDataBean.getProductNumber()) && nodeActiveproductRevision.equalsIgnoreCase(productDataBean.getProductRevision());
    }

    @SuppressWarnings("unchecked")
    public DeleteUpgradePackageActionInfo getActionArguments(final long activityJobId, final NEJobStaticData neJobStaticData, final String neType) {
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) activityUtils.getMainJobAttributes(activityJobId).get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
        LOGGER.debug("entering  delete upgrade activity precheck with jobConfigurationDetails : {} for activityJobId {} ", jobConfigurationDetails, activityJobId);
        final List<String> keyList = new ArrayList<>();
        keyList.add(UpgradeActivityConstants.IS_PREVENT_UP_DELETABALE);
        keyList.add(UpgradeActivityConstants.IS_PREVENT_CV_DELETABALE_FROM_ROLLBACKLIST);
        final Map<String, String> actionArguments = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neJobStaticData.getNodeName(), neType, neJobStaticData.getPlatformType());
        final String deletableInActiveUPList = activityUtils.getActivityJobAttributeValue(activityUtils.getJobEnvironment(activityJobId).getActivityJobAttributes(),
                UpgradeActivityConstants.DELETABLE_UP_LIST);
        LOGGER.debug("CPP delete UP User provided information for Deleting an UpgradePackage is: {}", deletableInActiveUPList);
        return getDeleteUpgradePackageActionInfo(deletableInActiveUPList, actionArguments);
    }

    public DeleteUpgradePackageActionInfo getDeleteUpgradePackageActionInfo(final String deleteUPInputList, final Map<String, String> actionArguments) {
        final List<String> inputDeleteUPDataList = prepareDeleteUPDataList(deleteUPInputList);
        final List<ProductDataBean> listProductData = new ArrayList<>();
        final DeleteUpgradePackageActionInfo deleteUpgradePackageActionInfo = new DeleteUpgradePackageActionInfo();
        if (!inputDeleteUPDataList.isEmpty()) {
            for (final String productData : inputDeleteUPDataList) {
                if (productData.contains(UpgradeActivityConstants.UPGRADEPACKAGES_REQUEST_DELIMTER)) {
                    final String[] deleteUPData = productData.split(UpgradeActivityConstants.UPGRADEPACKAGES_PERSISTENCE_DELIMTER);
                    final ProductDataBean productDataBean = new ProductDataBean();
                    productDataBean.setProductNumber(deleteUPData[0]);
                    productDataBean.setProductRevision(deleteUPData[1]);
                    listProductData.add(productDataBean);
                }
            }
        }
        deleteUpgradePackageActionInfo.setProductData(listProductData);
        deleteUpgradePackageActionInfo.setPreventCvDeletable(Boolean.parseBoolean(actionArguments.get(UpgradeActivityConstants.IS_PREVENT_CV_DELETABALE_FROM_ROLLBACKLIST)));
        deleteUpgradePackageActionInfo.setPreventUpDeletable(Boolean.parseBoolean(actionArguments.get(UpgradeActivityConstants.IS_PREVENT_UP_DELETABALE)));
        return deleteUpgradePackageActionInfo;
    }

    private List<String> prepareDeleteUPDataList(final String deleteUPInputList) {
        final List<String> deleteUPDataList = new ArrayList<>();
        if (deleteUPInputList.contains(ActivityConstants.COMMA)) {
            final String[] deleteUPDetails = deleteUPInputList.split(ActivityConstants.COMMA);
            Collections.addAll(deleteUPDataList, deleteUPDetails);
        } else {
            deleteUPDataList.add(deleteUPInputList);
        }
        return deleteUPDataList;
    }

    public String findFDN(final Map<String, HashMap<String, Object>> preventingUpsCvs, final String productNumber, final String productRevision) {
        final HashMap<String, Object> attributes = preventingUpsCvs.get(productNumber + UpgradeActivityConstants.UPGRADEPACKAGES_REQUEST_DELIMTER + productRevision);
        if (attributes == null) {
            return null;
        } else {
            return (String) attributes.get(UpgradeActivityConstants.UP_FDN);
        }
    }

    public Map<String, Object> evaluateRepeatRequiredAndActivityResult(final long activityJobId, final JobResult currentActivityResult, final NEJobStaticData neJobStaticData, final String neType) {
        LOGGER.debug("Evaluate whether repeat is Required and activity result for currentActivityResult {}", currentActivityResult);
        boolean repeatExecute = true;
        JobResult activityJobResult = null;
        final boolean allUPsProcessed = isAllUPsProcessed(activityJobId, neJobStaticData, neType);

        if (allUPsProcessed) {
            final boolean intermediateFailureHappened = isAnyIntermediateFailureHappened(activityJobId);
            if (intermediateFailureHappened || currentActivityResult == JobResult.FAILED) {
                activityJobResult = JobResult.FAILED;
            } else if (currentActivityResult == null) {
                activityJobResult = null;
            } else {
                activityJobResult = JobResult.SUCCESS;
            }
            repeatExecute = false;
        } else if (activityUtils.cancelTriggered(activityJobId)) {
            activityJobResult = JobResult.CANCELLED;
            repeatExecute = false;
        }

        final Map<String, Object> repeatRequiredAndActivityResult = new HashMap<String, Object>();
        repeatRequiredAndActivityResult.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, repeatExecute);
        repeatRequiredAndActivityResult.put(ActivityConstants.ACTIVITY_RESULT, activityJobResult);
        LOGGER.debug("Is Repeat Required or ActivityResult evaluated : {}", repeatRequiredAndActivityResult);
        return repeatRequiredAndActivityResult;
    }

    private boolean isAllUPsProcessed(final long activityJobId, final NEJobStaticData neJobStaticData, final String neType) {
        final Map<String, Object> activityJobAttributes = jobConfigurationServiceRetryProxy.getActivityJobAttributes(activityJobId);
        final String processedUpsString = activityUtils.getActivityJobAttributeValue(activityJobAttributes, UpgradeActivityConstants.PROCESSED_UPS);
        final DeleteUpgradePackageActionInfo requestedUPData = getActionArguments(activityJobId, neJobStaticData, neType);
        LOGGER.info("isAllUPsProcessed processedUpsString {} ", processedUpsString);
        final int processedUpsCount = (processedUpsString == null || processedUpsString.isEmpty()) ? 0 : Integer.parseInt(processedUpsString);

        LOGGER.debug("Evaluating All UPs have processes or not ! [processedUpsInt={}, totalRequestedUPs={}]", processedUpsCount, requestedUPData.getProductData().size());
        if (processedUpsCount >= requestedUPData.getProductData().size()) {
            return true;
        }
        return false;
    }

    private boolean isAnyIntermediateFailureHappened(final long activityJobId) {
        final Map<String, Object> activityJobAttributes = jobConfigurationServiceRetryProxy.getActivityJobAttributes(activityJobId);
        final String intermediateFailure = activityUtils.getActivityJobAttributeValue(activityJobAttributes, UpgradeActivityConstants.INTERMEDIATE_FAILURE);
        if (intermediateFailure != null && !intermediateFailure.isEmpty()) {
            return true;
        }
        return false;
    }

    public static String getRdnId(final String upMoFdn) {
        if (upMoFdn == null) {
            return "";
        } else {
            final String[] ids = upMoFdn.split(ActivityConstants.EQUAL);
            return ids[ids.length - 1];
        }
    }
}
