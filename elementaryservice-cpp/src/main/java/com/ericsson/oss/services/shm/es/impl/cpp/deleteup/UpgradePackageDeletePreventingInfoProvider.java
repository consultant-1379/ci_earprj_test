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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.UpgradePackageMO;
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.persistjobdata.PersistJobData;
import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.UpgradePackageService;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

@RequestScoped
public class UpgradePackageDeletePreventingInfoProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradePackageDeletePreventingInfoProvider.class);

    @Inject
    private UpgradePackageService upgradePackageService;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private PersistJobData persistJobData;

    final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
    final List<Map<String, Object>> jobProperties = new ArrayList<Map<String, Object>>();
    final ActivityStepResult activityStepResult = new ActivityStepResult();

    public Map<String, HashMap<String, Object>> findPreventiveUpsAndCvs(final long activityJobId, final DeleteUpgradePackageActionInfo deleteUpgradePackageActionInfo,
            final ProductDataBean productDataBean, final Map<String, HashMap<String, Object>> preventingUpsAndCvs) {
        final UpgradePackageMO suppliedUpMo = upgradePackageService.getUPMOAttributes(activityJobId, productDataBean.getProductNumber(), productDataBean.getProductRevision());
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        LOGGER.debug("suppliedUpMo {} for activityJobId {}", suppliedUpMo, activityJobId);
        if (null == suppliedUpMo) {
            logErrorMessageForNotAvailablePackage(activityStepResult, productDataBean.getProductNumber(), productDataBean.getProductRevision());
        } else {
            final List<Map<String, String>> deletePreventingUPS = (List<Map<String, String>>) suppliedUpMo.getAllAttributes().get(UpgradeActivityConstants.DELETE_PREVENTING_UPS);
            if (!isDeletePreventingCVExists(deleteUpgradePackageActionInfo.isPreventCvDeletable(), suppliedUpMo)) {
                persistJobLogs(activityJobId);
                return preventingUpsAndCvs;
            }

            if (deleteUpgradePackageActionInfo.isPreventUpDeletable()) {
                findDeltaUPs(activityJobId, suppliedUpMo, deleteUpgradePackageActionInfo.isPreventCvDeletable(), preventingUpsAndCvs);
            } else if (deletePreventingUPS != null && !deletePreventingUPS.isEmpty()) {
                logErrorMessageForPreventiveUps(activityStepResult, productDataBean.getProductNumber(), productDataBean.getProductRevision());
                persistJobLogs(activityJobId);
                return preventingUpsAndCvs;
            }
            updateUpsAndCvsInfoForDeletion(suppliedUpMo, deletePreventingUPS, preventingUpsAndCvs);
        }
        persistJobLogs(activityJobId);
        LOGGER.debug("preventingUpsAndCvs {} activityJobId {}", preventingUpsAndCvs, activityJobId);
        return preventingUpsAndCvs;
    }

    private void persistJobLogs(final long activityJobId) {
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs);
        jobLogs.clear();
    }

    private void findDeltaUPs(final long activityJobId, final UpgradePackageMO upMo, final boolean isDeleteCVSlected, final Map<String, HashMap<String, Object>> preventingUpsCvs) {
        if (null == upMo || !isDeletePreventingCVExists(isDeleteCVSlected, upMo)) {
            return;
        }
        final List<Map<String, String>> listDeletePreventingUPS = (List<Map<String, String>>) upMo.getAllAttributes().get(UpgradeActivityConstants.DELETE_PREVENTING_UPS);
        updateUpsAndCvsInfoForDeletion(upMo, listDeletePreventingUPS, preventingUpsCvs);
        if (listDeletePreventingUPS != null && !listDeletePreventingUPS.isEmpty()) {
            for (int index = listDeletePreventingUPS.size() - 1; index >= 0; index--) {
                final Map<String, String> deltaUPs = listDeletePreventingUPS.get(index);
                final String productNumber = deltaUPs.get(UpgradeActivityConstants.PRODUCT_NUMBER);
                final String productRevision = deltaUPs.get(UpgradeActivityConstants.PRODUCT_REVISION);
                final UpgradePackageMO deltaUpMo = upgradePackageService.getUPMOAttributes(activityJobId, productNumber, productRevision);
                if (deltaUpMo == null) {
                    final String logMessage = String.format(JobLogConstants.UP_WITH_PNUM_PREV, productNumber, productRevision)
                            .concat(" is currently not available on the node. So it will be skipped from deletion.");
                    LOGGER.warn(logMessage + " but have listed on its base MO. So will ingnore this item during deletion. activityJobId:{} ", activityJobId);
                    jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                    listDeletePreventingUPS.remove(deltaUPs);
                    updateUpsAndCvsInfoForDeletion(upMo, listDeletePreventingUPS, preventingUpsCvs);
                } else {
                    findDeltaUPs(activityJobId, deltaUpMo, isDeleteCVSlected, preventingUpsCvs);
                }
            }
        }
    }

    public ActivityStepResultEnum getActivityStepResult() {
        return activityStepResult.getActivityResultEnum();
    }

    /**
     * This method is for validating preventiveCv exists when user is not opted for preventingcvs deletable option.
     */
    private boolean isDeletePreventingCVExists(final boolean isPreventCvDeletable, final UpgradePackageMO upMo) {
        if (!isPreventCvDeletable) {
            final Map<String, String> adminData = (Map<String, String>) upMo.getAllAttributes().get(UpgradeActivityConstants.ADMINISTRATIVE_DATA);
            final String productNumber = adminData.get(UpgradeActivityConstants.PRODUCT_NUMBER);
            final String productRevision = adminData.get(UpgradeActivityConstants.PRODUCT_REVISION);
            final List<String> deletePreventingCVS = (List<String>) upMo.getAllAttributes().get(UpgradeActivityConstants.DELETE_PREVENTING_CVS);
            if (deletePreventingCVS != null && !deletePreventingCVS.isEmpty()) {
                logErrorMessageForPreventiveCVs(activityStepResult, productNumber, productRevision);
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    private void updateUpsAndCvsInfoForDeletion(final UpgradePackageMO upMo, final List<Map<String, String>> listDeletePreventingUPS, final Map<String, HashMap<String, Object>> preventingUpsAndCvs) {
        final Map<String, String> adminData = (Map<String, String>) upMo.getAllAttributes().get(UpgradeActivityConstants.ADMINISTRATIVE_DATA);
        final List<String> listDeletePreventingCVS = (List<String>) upMo.getAllAttributes().get(UpgradeActivityConstants.DELETE_PREVENTING_CVS);
        if (null == adminData) {
            LOGGER.warn("not able to update data into preventingUpsAndCvs for upMo {} ", upMo);
            return;
        }
        final String productNumber = adminData.get(UpgradeActivityConstants.PRODUCT_NUMBER);
        final String productRevision = adminData.get(UpgradeActivityConstants.PRODUCT_REVISION);
        final HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(UpgradeActivityConstants.DELETE_PREVENTING_CVS, listDeletePreventingCVS);
        attributes.put(UpgradeActivityConstants.DELETE_PREVENTING_UPS, listDeletePreventingUPS);
        attributes.put(UpgradeActivityConstants.PRODUCT_NUMBER, productNumber);
        attributes.put(UpgradeActivityConstants.PRODUCT_REVISION, productRevision);
        attributes.put(UpgradeActivityConstants.UP_FDN, upMo.getFdn());
        preventingUpsAndCvs.put(productNumber + UpgradeActivityConstants.UPGRADEPACKAGES_REQUEST_DELIMTER + productRevision, attributes);
    }

    public String findFDN(final Map<String, HashMap<String, Object>> preventingUpsAndCvs, final String productNumber, final String productRevision) {
        LOGGER.debug("Entering into findFDN preventingUpsAndCvs {} ,productNumber {} productRevision {} ", preventingUpsAndCvs, productNumber, productRevision);
        final HashMap<String, Object> attributes = preventingUpsAndCvs.get(productNumber + UpgradeActivityConstants.UPGRADEPACKAGES_REQUEST_DELIMTER + productRevision);
        if (attributes == null) {
            return null;
        } else {
            if (JobResult.FAILED.name().equals(attributes.get(UpgradeActivityConstants.DELETE_PREVENTING_UP_STATUS))) {
                return null;
            }
            return (String) attributes.get(UpgradeActivityConstants.UP_FDN);
        }
    }

    public boolean findCvFailStatus(final Map<String, HashMap<String, Object>> preventingUpsAndCvs, final String productNumber, final String productRevision) {
        LOGGER.debug("Entering into findFailStatus preventingUpsAndCvs {} ,productNumber {} productRevision {} ", preventingUpsAndCvs, productNumber, productRevision);
        final HashMap<String, Object> attributes = preventingUpsAndCvs.get(productNumber + UpgradeActivityConstants.UPGRADEPACKAGES_REQUEST_DELIMTER + productRevision);
        if (attributes == null) {
            return false;
        } else {
            if (JobResult.FAILED.name().equals(attributes.get(UpgradeActivityConstants.DELETE_PREVENTING_UP_STATUS))) {
                return true;
            }
            return false;
        }
    }

    private void logErrorMessageForNotAvailablePackage(final ActivityStepResult activityStepResult, final String productNumber, final String productRevision) {
        final String logMessage = String.format(JobLogConstants.UP_MO_DATA, productNumber, productRevision, "does not exist");
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION);
    }

    private void logErrorMessageForPreventiveCVs(final ActivityStepResult activityStepResult, final String productNumber, final String productRevision) {
        final String logMessage = String.format(JobLogConstants.UP_MO_DATA, productNumber, productRevision, "has delete preventing CVs");
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
    }

    private void logErrorMessageForPreventiveUps(final ActivityStepResult activityStepResult, final String productNumber, final String productRevision) {
        final String logMessage = String.format(JobLogConstants.UP_MO_DATA, productNumber, productRevision, "has delete preventing UPs");
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
    }

    public Map<String, HashMap<String, Object>> findPreventiveUpsAndCvsMap(final Map<String, Object> activityJobAttributes) {
        final ObjectMapper mapper = new ObjectMapper();
        final String deleteUPrequestProperty = activityUtils.getActivityJobAttributeValue(activityJobAttributes, UpgradeActivityConstants.PREVENTING_UP_CV_INFO);
        try {
            final HashMap<String, HashMap<String, Object>> preventingUpsCvs = mapper.readValue(deleteUPrequestProperty, new TypeReference<HashMap<String, HashMap<String, Object>>>() {
            });
            return preventingUpsCvs;
        } catch (Exception exception) {
            LOGGER.error("Unable to transform string input to Map object, while converting the delete up request, due to ::", exception);
        }
        return new HashMap<String, HashMap<String, Object>>();
    }

    public String readAndUpdateCvs(final long activityJobId, final long neJobId, final Map<String, HashMap<String, Object>> preventingUpsAndCvs, final String productNumber,
            final String productRevision) {
        HashMap<String, Object> upAttributes = preventingUpsAndCvs.get(productNumber + UpgradeActivityConstants.UPGRADEPACKAGES_REQUEST_DELIMTER + productRevision);
        if (upAttributes == null) {
            return null;
        }
        final List<String> listCvs = (List<String>) upAttributes.get(UpgradeActivityConstants.DELETE_PREVENTING_CVS);
        if (listCvs == null || listCvs.isEmpty()) {
            final ArrayList<HashMap<String, String>> listDeletePreventingUPS = (ArrayList<HashMap<String, String>>) upAttributes.get(UpgradeActivityConstants.DELETE_PREVENTING_UPS);
            if (listDeletePreventingUPS == null) {
                return null;
            } else {
                for (int index = 0; index < listDeletePreventingUPS.size(); index++) {
                    final String deltaCvName = readAndUpdateCvs(activityJobId, neJobId, preventingUpsAndCvs, listDeletePreventingUPS.get(index).get(UpgradeActivityConstants.PRODUCT_NUMBER),
                            listDeletePreventingUPS.get(index).get(UpgradeActivityConstants.PRODUCT_REVISION));
                    if (deltaCvName == null) {
                        continue;
                    } else {
                        return deltaCvName;
                    }
                }
            }
        } else {
            final String cvName = listCvs.get(0);
            listCvs.remove(cvName);
            upAttributes = preventingUpsAndCvs.get(productNumber + UpgradeActivityConstants.UPGRADEPACKAGES_REQUEST_DELIMTER + productRevision);
            upAttributes.put(UpgradeActivityConstants.DELETE_PREVENTING_CVS, listCvs);
            preventingUpsAndCvs.put(productNumber + UpgradeActivityConstants.UPGRADEPACKAGES_REQUEST_DELIMTER + productRevision, upAttributes);
            buildPropertiesToBePersisted(activityJobId, neJobId, preventingUpsAndCvs);
            return cvName;
        }
        return null;
    }

    public Map<String, HashMap<String, Object>> updateStatus(final long activityJobId, final long neJobId, final Map<String, HashMap<String, Object>> preventingUpsAndCvs, final String productNumber,
            final String productRevision) {
        final HashMap<String, Object> upAttributes = preventingUpsAndCvs.get(productNumber + UpgradeActivityConstants.UPGRADEPACKAGES_REQUEST_DELIMTER + productRevision);
        if (upAttributes == null) {
            LOGGER.warn("upAttributes are null for productNumber {} and productRevision {} ", productNumber, productRevision);
            return preventingUpsAndCvs;
        }
        upAttributes.put(UpgradeActivityConstants.DELETE_PREVENTING_UP_STATUS, JobResult.FAILED.name());
        preventingUpsAndCvs.put(productNumber + UpgradeActivityConstants.UPGRADEPACKAGES_REQUEST_DELIMTER + productRevision, upAttributes);
        buildPropertiesToBePersisted(activityJobId, neJobId, preventingUpsAndCvs);
        return preventingUpsAndCvs;
    }

    private void buildPropertiesToBePersisted(final long activityJobId, final long neJobId, final Map<String, HashMap<String, Object>> preventingUpsAndCvs) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            activityUtils.prepareJobPropertyObjectList(jobProperties, UpgradeActivityConstants.PREVENTING_UP_CV_INFO, mapper.writeValueAsString(preventingUpsAndCvs));
            persistJobData.persistPropertiesLogsAndProgress(activityJobId, neJobId, jobProperties, jobLogs, 0.0);
        } catch (Exception exception) {
            LOGGER.error("Unable to persist preventingUpsAndCvs {} ", exception);
        }
    }

    public Map<String, Object> getLowestPossibleDeltaUP(final Map<String, HashMap<String, Object>> preventingUpsAndCvs, final String productNumber, final String productRevision) {
        final Map<String, Object> deltaUps = getLowestDeltaUPRecursively(preventingUpsAndCvs, productNumber, productRevision);
        if (deltaUps.isEmpty() || (productNumber.equals(deltaUps.get(UpgradeActivityConstants.PRODUCT_NUMBER)) && productRevision.equals(deltaUps.get(UpgradeActivityConstants.PRODUCT_REVISION)))) {
            LOGGER.info("Currently DeltaUPs does not exists on UpgradePackage [productNumber={}, productRevision={}]", productNumber, productRevision);
            return Collections.emptyMap();
        } else {
            return deltaUps;
        }
    }

    private Map<String, Object> getLowestDeltaUPRecursively(final Map<String, HashMap<String, Object>> preventingUpsAndCvs, final String productNumber, final String productRevision) {
        Map<String, Object> upAttributes = preventingUpsAndCvs.get(productNumber + UpgradeActivityConstants.UPGRADEPACKAGES_REQUEST_DELIMTER + productRevision);
        if (upAttributes == null) {
            return Collections.emptyMap();
        }
        final List<Map<String, String>> deltaUPs = (List<Map<String, String>>) upAttributes.get(UpgradeActivityConstants.DELETE_PREVENTING_UPS);
        if (deltaUPs == null || deltaUPs.isEmpty()) {
            return upAttributes;
        } else {
            for (int index = 0; index < deltaUPs.size(); index++) {
                final String productNumber2 = deltaUPs.get(index).get(UpgradeActivityConstants.PRODUCT_NUMBER);
                final String productRevision2 = deltaUPs.get(index).get(UpgradeActivityConstants.PRODUCT_REVISION);
                upAttributes = getLowestDeltaUPRecursively(preventingUpsAndCvs, productNumber2, productRevision2);
            }
        }
        LOGGER.debug("The DeltaUP in recursive method is: {}", upAttributes);
        return upAttributes;
    }

    public String getAndRemoveReferredCVOccurences(final long activityJobId, final long neJobId, final Map<String, HashMap<String, Object>> preventingUpsAndCvs, final String productNumber,
            final String productRevision) {
        final Map<String, Object> upAttributes = preventingUpsAndCvs.get(productNumber + UpgradeActivityConstants.UPGRADEPACKAGES_REQUEST_DELIMTER + productRevision);
        final List<String> dpCVs = (List<String>) upAttributes.get(UpgradeActivityConstants.DELETE_PREVENTING_CVS);
        if (dpCVs == null || dpCVs.isEmpty()) {
            return null;
        } else {
            final String referredCV = dpCVs.remove(0);
            buildPropertiesToBePersisted(activityJobId, neJobId, preventingUpsAndCvs); //preventingUpsAndCvs must have updated value (CV at zero should be removed)
            return referredCV;
        }
    }

    public void removeDeltaUPOccurences(final long activityJobId, final long neJobId, final Map<String, HashMap<String, Object>> preventingUpsAndCvs, final String productNumber,
            final String productRevision) {
        for (Entry<String, HashMap<String, Object>> map : preventingUpsAndCvs.entrySet()) {
            final List<Map<String, String>> deltaUPsPerMainUP = (List<Map<String, String>>) map.getValue().get(UpgradeActivityConstants.DELETE_PREVENTING_UPS);

            for (int index = deltaUPsPerMainUP.size() - 1; index >= 0; index--) {
                final Map<String, String> eachDeltaUP = deltaUPsPerMainUP.get(index);
                if (eachDeltaUP.containsValue(productNumber) && eachDeltaUP.containsValue(productRevision)) {
                    deltaUPsPerMainUP.remove(index);
                }
            }
        }
        final String deltaUPKeyTobeRemoved = productNumber + UpgradeActivityConstants.UPGRADEPACKAGES_REQUEST_DELIMTER + productRevision;
        preventingUpsAndCvs.remove(deltaUPKeyTobeRemoved);
        buildPropertiesToBePersisted(activityJobId, neJobId, preventingUpsAndCvs);
    }

}
