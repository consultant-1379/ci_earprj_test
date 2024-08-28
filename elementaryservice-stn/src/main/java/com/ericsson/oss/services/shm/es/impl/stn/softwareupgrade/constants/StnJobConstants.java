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
package com.ericsson.oss.services.shm.es.impl.stn.softwareupgrade.constants;
/**
 * Provides constant values for STN related jobs.
 * 
 * @author xsamven/xgowbom/xvenupe
 */
public class StnJobConstants {
    
    private StnJobConstants() {
    }
    
    public static final String FDN_NOT_FOUND = "NodeFdn not found for the selected network element";
    public static final String ACTION_TRIGGERED = "actionTriggered";
    public static final String SUBSCRIPTION_KEY_DELIMETER = "@";
    public static final String INSTALL_ACTIVITY = "Install";
    public static final String UPGRADE_ACTIVITY = "Upgrade";
    public static final String APPROVE_SW_ACTIVITY = "ApproveSw";
    public static final String ADJUST_SW_ACTIVITY = "SwAdjust";
    public static final String CREATE_ACTIVITY = "Create";
    public static final String ACTION_ABOUT_TO_TRIGGER = "\"%s\" action is going to be triggered for Node \"%s\"";
    public static final String STN_PACKAGE_TYPE = "CppSoftwarePackage";
    public static final String STN_PACKAGE_FILE_PATH = "filePath";
    public static final String STN_PACKAGE_NAME = "packageName";
    public static final String SOFTWAREPACKAGE_NFVODETAILS = "swpProductDetails";
    public static final String PRODUCT_NUMBER = "productNumber";
    public static final String PRODUCT_REVISION = "productRevision";
    public static final String SOFTWAREPACKAGE_ACTIVITES = "activities";
    public static final String SOFTWAREPACKAGE_ACTIVITESPARAMS = "activityParams";
    public static final String SMO_BUNDLE_FILENAME = "SMO_BUNDLE_FILENAME";
    public static final String VALUE = "value";
    public static final String SOFTWAREPACKAGE_FILENAME = "filename";
    public static final String SLASH = System.getProperty("file.separator","/");
    public static final String UNDERSCORE = "_";
    public static final String ACTIVITY_IN_TIMEOUT = "Notifications not received for the \"%s\" activity";
    public static final String JOB_CANCEL = "Cancel";
    public static final String ACTIVITY_JOB_ID = "ActivityJobId" ;
    public static final String NOTIFICATION_TIME_STAMP = "NotificationTimeStamp";
    public static final String ACTIVITY_NAME = "ActivityName";
    public static final String ERROR_MESSAGE = "Failed to fetch package details of \"%s\" due to \"%s\"";
}
