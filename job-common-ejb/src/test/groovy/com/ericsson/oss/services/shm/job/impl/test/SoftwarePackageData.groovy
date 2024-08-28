/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.impl.test

import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.filestore.swpackage.constants.SoftwarePackageConstants

class SoftwarePackageData extends CdiSpecification {
    private static final String SOFTWARE_PACKAGE_NAME = "R1H20_CXP102200_1"
    private static final String SOFTWARE_PACKAGE_FILEPATH = "/src/main/resources/R1H20_CXP102200_1.zip"

    public Map<String, Object> buildSoftwarePackagePO(){
        final Map<String, Object> axeSoftwarePackageMap = new HashMap<>();
        axeSoftwarePackageMap.put(SoftwarePackageConstants.SOFTWAREPACKAGE_PACKAGENAME, SOFTWARE_PACKAGE_NAME);
        axeSoftwarePackageMap.put(SoftwarePackageConstants.SOFTWAREPACKAGE_PLATFORM, "AXE");
        axeSoftwarePackageMap.put(SoftwarePackageConstants.SOFTWAREPACKAGE_DESCRIPTION, "Package:" + SOFTWARE_PACKAGE_NAME);
        axeSoftwarePackageMap.put(SoftwarePackageConstants.SOFTWAREPACKAGE_IMPORTEDDATE, new Date(System.currentTimeMillis()));
        axeSoftwarePackageMap.put(SoftwarePackageConstants.SOFTWAREPACKAGE_IMPORTEDBY, "userName");
        axeSoftwarePackageMap.put(SoftwarePackageConstants.SOFTWAREPACKAGE_FILEPATH, SOFTWARE_PACKAGE_FILEPATH + SOFTWARE_PACKAGE_NAME);
        final List<Object> softwarePackageProdDetailsList = new ArrayList<>();
        final Map<String, Object> softwarePackageProdDetails = createSoftwarePackageProductDetailsMap();
        softwarePackageProdDetailsList.add(softwarePackageProdDetails);
        axeSoftwarePackageMap.put(SoftwarePackageConstants.SOFTWAREPACKAGE_PRODUCTDETAILS, softwarePackageProdDetailsList);
        final List<Object> jobParamsList = new ArrayList<>();
        final Map<String, Object> jobParamsMap = createAxeSoftwarePackageActivityParams();
        jobParamsList.add(jobParamsMap);
        axeSoftwarePackageMap.put(SoftwarePackageConstants.SOFTWAREPACKAGE_JOBPARAMS, jobParamsList);
        final List<Object> activities = new ArrayList<>();
        for (int index_local = 1; index_local < 2; index_local++) {
            final Map<String, Object> activity = createSoftwarePackageActivity();
            activities.add(activity);
        }
        axeSoftwarePackageMap.put(SoftwarePackageConstants.SOFTWAREPACKAGE_ACTIVITES, activities);
        axeSoftwarePackageMap.put(SoftwarePackageConstants.CPPSOFTWAREPACKAGE_NODEFUNCTION, "MSC");
        axeSoftwarePackageMap.put(SoftwarePackageConstants.NODE_PLATFORM, PlatformTypeEnum.AXE.getName())
        return axeSoftwarePackageMap
    }
    def Map<String, Object> createAxeSoftwarePackageActivityParams() {
        final Map<String, Object> jobParamsMap = new HashMap<>();
        jobParamsMap.put(SoftwarePackageConstants.SOFTWAREPACKAGE_PARAMNAME, "_PACKAGE_NAME");
        jobParamsMap.put(SoftwarePackageConstants.SOFTWAREPACKAGE_PARAM_TYPE, "STRING");
        jobParamsMap.put(SoftwarePackageConstants.SOFTWAREPACKAGE_PARAM_DESCRIPTION, "" );
        jobParamsMap.put(SoftwarePackageConstants.SOFTWAREPACKAGE_PARAM_PROMPT, "Package Directory name");
        jobParamsMap.put(SoftwarePackageConstants.SOFTWAREPACKAGE_PARAM_VALUE, "MSS18A_CM02_BC_UPGRADE_PA3");
        jobParamsMap.put(SoftwarePackageConstants.SOFTWAREPACKAGE_PARAM_MIN, 5 );
        jobParamsMap.put(SoftwarePackageConstants.SOFTWAREPACKAGE_PARAM_MAX, 50 );
        final List<String> itemsList = new ArrayList<>();
        itemsList.add("");
        jobParamsMap.put(SoftwarePackageConstants.SOFTWAREPACKAGE_PARAM_ITEMS, itemsList);
        return jobParamsMap;
    }

    def Map<String, Object> createSoftwarePackageProductDetailsMap() {
        final Map<String, Object> softwarePackageProdDetails = new HashMap<>();
        softwarePackageProdDetails.put(SoftwarePackageConstants.SOFTWAREPACKAGE_PRODUCTNAME, "CSUC");
        softwarePackageProdDetails.put(SoftwarePackageConstants.SOFTWAREPACKAGE_PRODUCTNUMBER, "CXC1721554_");
        softwarePackageProdDetails.put(SoftwarePackageConstants.SOFTWAREPACKAGE_PRODUCT_DESCRIPTION, "CSUC_CXC1721554_");
        softwarePackageProdDetails.put(SoftwarePackageConstants.SOFTWAREPACKAGE_PRODUCT_RELEASEDATE, new Date(System.currentTimeMillis()));
        softwarePackageProdDetails.put(SoftwarePackageConstants.SOFTWAREPACKAGE_PRODUCTREVISION, "R61N01");
        softwarePackageProdDetails.put(SoftwarePackageConstants.SOFTWAREPACKAGE_PRODUCT_TYPE, "MSC");
        return softwarePackageProdDetails;
    }

    def Map<String, Object> createAxeSoftwarePackageJobParams() {
        final Map<String, Object> jobParamsMap = new HashMap<>();
        jobParamsMap.put(SoftwarePackageConstants.SOFTWAREPACKAGE_PARAMNAME, "_INTERACTION_LEVEL");
        jobParamsMap.put(SoftwarePackageConstants.SOFTWAREPACKAGE_PARAM_TYPE, "STRING");
        jobParamsMap.put(SoftwarePackageConstants.SOFTWAREPACKAGE_PARAM_DESCRIPTION, "Top Down Menu" );
        jobParamsMap.put(SoftwarePackageConstants.SOFTWAREPACKAGE_PARAM_PROMPT, "User Interaction level");
        jobParamsMap.put(SoftwarePackageConstants.SOFTWAREPACKAGE_PARAM_VALUE, "Display all message types");
        jobParamsMap.put(SoftwarePackageConstants.SOFTWAREPACKAGE_PARAM_MIN, 5 );
        jobParamsMap.put(SoftwarePackageConstants.SOFTWAREPACKAGE_PARAM_MAX, 10 );
        final List<String> itemsList = new ArrayList<>();
        itemsList.add("0: Display all message types");
        itemsList.add("1: Display Decisions/Warnings/Errors");
        itemsList.add("2: Display Warnings/Errors");
        itemsList.add("3: No dialog, display Fallback Menu");
        jobParamsMap.put(SoftwarePackageConstants.SOFTWAREPACKAGE_PARAM_ITEMS, itemsList);
        return jobParamsMap;
    }

    def Map<String, Object> createSoftwarePackageActivity() {
        final Map<String, Object> activityMap = new HashMap<>();
        activityMap.put(SoftwarePackageConstants.CPPSOFTWAREPACKAGE_ACTIVITY_NAME, "Health Check");
        activityMap.put(SoftwarePackageConstants.CPPSOFTWAREPACKAGE_ACTIVITY_STARTUP, "IMMEDIATE");
        activityMap.put(SoftwarePackageConstants.CPPSOFTWAREPACKAGE_ACTIVITY_SCRIPTFILENAME, "File:" );
        activityMap.put(SoftwarePackageConstants.CPPSOFTWAREPACKAGE_ACTIVITY_DESCRIPTION, "Activity under progress");
        activityMap.put(SoftwarePackageConstants.CPPSOFTWAREPACKAGE_ACTIVITY_SELECTED, true);
        final List<Map<String, Object>> paramsList = new ArrayList<>();
        for (int index_local = 1; index_local < 2; index_local++) {
            final Map<String, Object> param = createAxeSoftwarePackageJobParams();
            paramsList.add(param);
        }
        activityMap.put(SoftwarePackageConstants.CPPSOFTWAREPACKAGE_ACTIVITY_PARAMS, paramsList);
        return activityMap;
    }
}
