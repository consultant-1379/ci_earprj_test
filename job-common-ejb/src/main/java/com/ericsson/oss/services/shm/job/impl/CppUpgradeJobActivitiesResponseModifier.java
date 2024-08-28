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
package com.ericsson.oss.services.shm.job.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.filestore.swpackage.api.ActivityDetails;
import com.ericsson.oss.services.shm.filestore.swpackage.api.SoftwarePackage;
import com.ericsson.oss.services.shm.filestore.swpackage.api.SoftwarePackageParameter;
import com.ericsson.oss.services.shm.job.activity.Activity;
import com.ericsson.oss.services.shm.job.activity.ActivityParams;
import com.ericsson.oss.services.shm.job.activity.NeActivityInformation;
import com.ericsson.oss.services.shm.job.activity.ParamType;
import com.ericsson.oss.services.shm.job.cpp.activity.CppUpgradeActivityConstants;
import com.ericsson.oss.services.shm.jobs.common.annotations.PlatformJobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.api.JobActivitiesResponse;
import com.ericsson.oss.services.shm.jobs.common.api.NeInfoQuery;
import com.ericsson.oss.services.shm.jobs.common.api.NeParams;
import com.ericsson.oss.services.shm.jobs.common.api.SHMJobActivitiesResponseModifier;
import com.ericsson.oss.services.shm.jobs.common.constants.JobConfigurationConstants;
import com.ericsson.oss.services.shm.swpackage.query.api.SoftwarePackageQueryService;

/**
 * To Retrieve the additional activities required from SMO_INFO.xml for CPP Upgrade
 * 
 * @author xsrabop
 * 
 */
@Stateless
@PlatformJobTypeAnnotation(platformType = PlatformTypeEnum.CPP, jobType = com.ericsson.oss.services.shm.job.activity.JobType.UPGRADE)
public class CppUpgradeJobActivitiesResponseModifier implements SHMJobActivitiesResponseModifier {

    private static final Logger logger = LoggerFactory.getLogger(CppUpgradeJobActivitiesResponseModifier.class);

    @EServiceRef
    private SoftwarePackageQueryService softwarePackageQueryService;

    public static final String INVALID_INPUT = "No Software package is selected for neType \"%s\".";

    public static final String SWP_DOESNOTEXISTSINDB = "Package Data for \"%s\" doesn't exist in the database.";

    public static final String UNSUPPORTEDNETYPE = "No activity information is present for the neType \"%s\".";

    public static final String SMO_UPGRADE_REBOOT = "false";

    /**
     * To Retrieve the Domain and Backup type from Node and updates it to the Response if they found.
     * 
     * @param jobActivitiesQuery
     * @param jobActivitiesResponse
     * @return
     */
    @Override
    public JobActivitiesResponse getUpdatedJobActivities(final NeInfoQuery neInfoQuery, final JobActivitiesResponse jobActivitiesResponse) {
        logger.info("Json Modification started in CppUpgradeJobActivitiesResponseModifier.");
        final Map<String, List<String>> softwarePackagesMap = new HashMap<String, List<String>>();
        final List<String> swpackageList = new ArrayList<String>();
        final List<NeParams> paramsList = neInfoQuery.getParams();
        final Map<String, String> unsupportedNeTypes = new HashMap<String, String>();
        String softwarePackage = "";
        NeActivityInformation neActivityInformation = null;
        for (final NeParams param : paramsList) {
            if (JobConfigurationConstants.SOFTWAREPKG_NAME.equals(param.getName())) {
                softwarePackage = param.getValue();
                if (softwarePackage != null && !("".equals(softwarePackage))) {
                    swpackageList.add(softwarePackage);
                    break;
                }
            }
        }

        //if SWP_NAME is not present in input request then returning response
        if (swpackageList.isEmpty()) {
            unsupportedNeTypes.put(neInfoQuery.getNeType(), String.format(INVALID_INPUT, neInfoQuery.getNeType()));
            jobActivitiesResponse.setUnsupportedNeTypes(unsupportedNeTypes);
            return jobActivitiesResponse;
        }
        softwarePackagesMap.put(neInfoQuery.getNeType(), swpackageList);
        // Gets the Software Package information form shm-softwarepackagemgmt repo
        final Map<String, SoftwarePackage> softwarePackagesDataMap = softwarePackageQueryService.getSoftwarePackagesBasedOnPackageName(softwarePackagesMap);

        //if SWP is not found in the database then returning response
        if (softwarePackagesDataMap.isEmpty()) {
            unsupportedNeTypes.put(neInfoQuery.getNeType(), String.format(SWP_DOESNOTEXISTSINDB, softwarePackage));
            jobActivitiesResponse.setUnsupportedNeTypes(unsupportedNeTypes);
            return jobActivitiesResponse;
        }
        logger.debug("SoftwarePackagesDataList size is : {}", softwarePackagesDataMap.size());
        final SoftwarePackage softwarePackageObj = softwarePackagesDataMap.get(softwarePackage);

        //step-1: Get NeActivityInformation for neType
        for (final NeActivityInformation activityInformation : jobActivitiesResponse.getNeActivityInformation()) {
            if (activityInformation.getNeType().equalsIgnoreCase(neInfoQuery.getNeType())) {
                neActivityInformation = activityInformation;
                break;
            }
        }

        //if neActivityInformation is not found for the selected neType
        if (neActivityInformation == null) {
            unsupportedNeTypes.put(neInfoQuery.getNeType(), String.format(UNSUPPORTEDNETYPE, softwarePackage));
            jobActivitiesResponse.setUnsupportedNeTypes(unsupportedNeTypes);
            return jobActivitiesResponse;
        }
        updateActivityParameters(neActivityInformation, softwarePackageObj);
        return jobActivitiesResponse;
    }

    private void updateActivityParameters(final NeActivityInformation neActivityInformation, final SoftwarePackage softwarePackage) {
        logger.debug("Entered into updateActivityParameters() method with softwarePackage : {}.", softwarePackage.getName());
        final List<ActivityDetails> activitiesList = softwarePackage.getActivities();
        //Preparing a smoMap with activities from SMO_INFO.xml
        final Map<String, Map<String, ParamType>> smoMap = new HashMap<String, Map<String, ParamType>>();
        logger.debug("Activities size from SMO is : {}", activitiesList.size());
        for (final ActivityDetails softwarePackageActivityDetails : activitiesList) {
            final Map<String, ParamType> smoParamMap = new HashMap<String, ParamType>();
            for (final SoftwarePackageParameter softwarePackageParameter : softwarePackageActivityDetails.getActivityParams()) {
                final ParamType cppUpgradeParameters = new ParamType();
                cppUpgradeParameters.setDefaultValue(softwarePackageParameter.getValue());
                cppUpgradeParameters.getItem().addAll(softwarePackageParameter.getItems());
                smoParamMap.put(softwarePackageParameter.getName(), cppUpgradeParameters);
            }
            smoMap.put(softwarePackageActivityDetails.getName(), smoParamMap);
        }

        for (int i = neActivityInformation.getActivity().size() - 1; i >= 0; i--) {
            final boolean result = isValidActivity(neActivityInformation.getActivity().get(i), smoMap);
            //If activity is not in SMOINFO.xml then removing the activity
            if (!result) {
                neActivityInformation.getActivity().remove(i);
                continue;
            }
        }

        logger.info("Exting updateActivityParameters() method with activities size : {}", neActivityInformation.getActivity().size());
    }

    private boolean isValidActivity(final Activity activity, final Map<String, Map<String, ParamType>> smoMap) {
        final String activityName = activity.getName();
        logger.debug("Entered into isValidActivity() method with activity : {}.", activityName);
        final boolean result = true;
        if (activityName.equalsIgnoreCase(CppUpgradeActivityConstants.INSTALL)) {
            final String keyForInstall = getKeyForInstall(activityName, smoMap);
            if (keyForInstall != null) {
                updateInstallActivityParams(activity, keyForInstall, smoMap);
            } else {
                return false;
            }
        } else if (activityName.equalsIgnoreCase(CppUpgradeActivityConstants.VERIFY)) {
            return isVerifyAValidActivity(activityName, smoMap);
        } else if (activityName.equalsIgnoreCase(CppUpgradeActivityConstants.UPGRADE)) {
            final String keyForUpgrade = getKeyForUpgrade(activityName, smoMap);
            if (keyForUpgrade != null) {
                updateUpgradeActivityParams(activity, keyForUpgrade, smoMap);

            } else {
                return false;
            }
        } else if (activityName.equalsIgnoreCase(CppUpgradeActivityConstants.CONFIRM)) {
            return isConfirmAValidActivity(activityName, smoMap);
        }
        return result;
    }

    private void updateUpgradeActivityParams(final Activity activity, final String keyForUpgrade, final Map<String, Map<String, ParamType>> smoMap) {
        for (final ActivityParams activityParams : activity.getActivityParams()) {
            final List<ParamType> paramList = activityParams.getParam();
            final ListIterator<ParamType> paramIterator = paramList.listIterator();
            logger.debug("keyForUpgrade : {},smoMap {} ", keyForUpgrade, smoMap);
            while (paramIterator.hasNext()) {
                final ParamType xmlParamType = paramIterator.next();
                Map<String, ParamType> smoUpgradeMap = null;
                if (smoMap.get(keyForUpgrade) != null) {
                    smoUpgradeMap = smoMap.get(keyForUpgrade);
                    if (UpgradeActivityConstants.REBOOTNODEUPGRADE.equalsIgnoreCase(xmlParamType.getName())) {
                        final ParamType smoUpgradeParam = getUpgradeParam(smoUpgradeMap);
                        if (smoUpgradeParam != null) {
                            logger.debug("getUpgradeParam-upgradeParam: {}.", smoUpgradeParam.getName());
                            xmlParamType.setDefaultValue(smoUpgradeParam.getDefaultValue());
                        } else {
                            xmlParamType.setDefaultValue(SMO_UPGRADE_REBOOT);
                        }
                    }
                }
            }
        }
    }

    private String getKeyForInstall(final String installActivityName, final Map<String, Map<String, ParamType>> smoMap) {
        final Set<String> smoMapKeys = smoMap.keySet();
        for (final String eachKey : smoMapKeys) {
            if (eachKey.equalsIgnoreCase(installActivityName) || eachKey.equalsIgnoreCase(CppUpgradeActivityConstants.INSTALLATION)
                    || eachKey.equalsIgnoreCase(CppUpgradeActivityConstants.SMO_INSTALL)) {
                return eachKey;
            }
        }
        return null;
    }

    private boolean isVerifyAValidActivity(final String verifyActivityName, final Map<String, Map<String, ParamType>> smoMap) {
        final Set<String> smoMapKeys = smoMap.keySet();
        for (final String eachKey : smoMapKeys) {
            if (eachKey.equalsIgnoreCase(verifyActivityName) || eachKey.equalsIgnoreCase(CppUpgradeActivityConstants.VERIFY_UPGRADE)
                    || eachKey.equalsIgnoreCase(CppUpgradeActivityConstants.VERIFYUPGRADE)) {
                return true;
            }
        }
        return false;
    }

    private String getKeyForUpgrade(final String upgradeActivityName, final Map<String, Map<String, ParamType>> smoMap) {
        final Set<String> smoMapKeys = smoMap.keySet();
        for (final String eachKey : smoMapKeys) {
            if (eachKey.equalsIgnoreCase(upgradeActivityName) || eachKey.equalsIgnoreCase(CppUpgradeActivityConstants.UPGRADE) || eachKey.equalsIgnoreCase(CppUpgradeActivityConstants.UPDATE)) {
                return eachKey;
            }
        }
        return null;
    }

    private boolean isConfirmAValidActivity(final String confirmActivityName, final Map<String, Map<String, ParamType>> smoMap) {
        final Set<String> smoMapKeys = smoMap.keySet();
        for (final String eachKey : smoMapKeys) {
            if (eachKey.equalsIgnoreCase(confirmActivityName) || eachKey.equalsIgnoreCase(CppUpgradeActivityConstants.CONFIRM_UPGRADE)
                    || eachKey.equalsIgnoreCase(CppUpgradeActivityConstants.CONFIRMUPGRADE)) {
                return true;
            }
        }
        return false;
    }

    private ParamType updateSelectType(final Map<String, ParamType> smoParams) {
        ParamType smoParam = null;
        if (smoParams.containsKey(CppUpgradeActivityConstants.SMO_INSTALL_SELECT_TYPE)) {
            smoParam = smoParams.get(CppUpgradeActivityConstants.SMO_INSTALL_SELECT_TYPE);
        } else if (smoParams.containsKey(UpgradeActivityConstants.SELECTIVEINSTALL)) {
            smoParam = smoParams.get(UpgradeActivityConstants.SELECTIVEINSTALL);
        }
        return smoParam;
    }

    private ParamType updateTransferType(final Map<String, ParamType> smoParams) {
        ParamType smoParam = null;
        if (smoParams.containsKey(CppUpgradeActivityConstants.SMO_INSTALL_TRANSFER_TYPE)) {
            smoParam = smoParams.get(CppUpgradeActivityConstants.SMO_INSTALL_TRANSFER_TYPE);
        } else if (smoParams.containsKey(UpgradeActivityConstants.FORCEINSTALL)) {
            smoParam = smoParams.get(UpgradeActivityConstants.FORCEINSTALL);
        }
        return smoParam;

    }

    private void updateInstallActivityParams(final Activity activity, final String keyForInstall, final Map<String, Map<String, ParamType>> smoMap) {
        for (final ActivityParams activityParams : activity.getActivityParams()) {
            final List<ParamType> paramList = activityParams.getParam();
            final ListIterator<ParamType> paramIterator = paramList.listIterator();
            logger.debug("updateInstallActivityParams-keyForInstall: {},smoMap {}", keyForInstall, smoMap);
            while (paramIterator.hasNext()) {
                final ParamType xmlParam = paramIterator.next();
                Map<String, ParamType> smoInstallMap = new HashMap();
                if (smoMap.get(keyForInstall) != null) {
                    smoInstallMap = smoMap.get(keyForInstall);
                }

                if (UpgradeActivityConstants.FORCEINSTALL.equalsIgnoreCase(xmlParam.getName())) {
                    final ParamType forceInstallParam = updateTransferType(smoInstallMap);
                    if (forceInstallParam != null) {
                        logger.debug("forceInstallParam: {}.", forceInstallParam.getName());
                        xmlParam.setDefaultValue(forceInstallParam.getDefaultValue());
                        xmlParam.getItem().clear();
                        xmlParam.getItem().addAll(forceInstallParam.getItem());
                    } else {
                        paramIterator.remove();
                    }

                }

                if (UpgradeActivityConstants.SELECTIVEINSTALL.equalsIgnoreCase(xmlParam.getName())) {
                    final ParamType selectiveInstallParam = updateSelectType(smoInstallMap);
                    if (selectiveInstallParam != null) {
                        logger.debug("selectiveInstallParam: {}.", selectiveInstallParam.getName());
                        xmlParam.setDefaultValue(selectiveInstallParam.getDefaultValue());
                        xmlParam.getItem().clear();
                        xmlParam.getItem().addAll(selectiveInstallParam.getItem());
                    } else {
                        paramIterator.remove();
                    }
                }
            }
        }

    }

    //This method is used to update the Activity Parameters of Upgrade Activity
    private ParamType getUpgradeParam(final Map<String, ParamType> smoParams) {
        logger.debug("Entered into getUpgradeParam-smoParams {}", smoParams);
        ParamType smoParam = null;
        if (smoParams.containsKey(CppUpgradeActivityConstants.SMO_UPGRADE_REBOOT)) {
            smoParam = smoParams.get(CppUpgradeActivityConstants.SMO_UPGRADE_REBOOT);
        } else if (smoParams.containsKey(UpgradeActivityConstants.REBOOTNODEUPGRADE)) {
            smoParam = smoParams.get(UpgradeActivityConstants.REBOOTNODEUPGRADE);
        }

        return smoParam;
    }
}
