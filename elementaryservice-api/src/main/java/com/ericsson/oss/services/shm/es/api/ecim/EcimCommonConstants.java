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
package com.ericsson.oss.services.shm.es.api.ecim;

/**
 * This class contains the common ECIM constant values.
 * 
 */
public class EcimCommonConstants {

    public static final String ACTION_TRIGGERED = "actionTriggered";
    public static final String INV_INTERNAL_ERROR = "Exception while reading the inventory. Please try again.";
    public static final String CVS_AND_UPS_TOBE_DELETED = "cvsAndUpsTobeDeleted";
    public static final String UPS_WITH_SYSCR_BACKUPS = "upsWithSystemCreatedBkps";
    public static final String SWM_NAMESPACE = "swmNameSpace";
    public static final String SWMO_ACTIVE = "active";

    /**
     * This class contains the ReportProgress constant values.
     * 
     */
    public static class ReportProgress {
        public static final String ASYNC_ACTION_PROGRESS = "progressReport";
        public static final String REPORT_PROGRESS_ACTION_ID = "actionId";
        public static final String REPORT_PROGRESS_ACTION_NAME = "actionName";
        public static final String REPORT_PROGRESS_PROGRESS_INFO = "progressInfo";
        public static final String REPORT_PROGRESS_PROGRESS_PERCENTAGE = "progressPercentage";
        public static final String REPORT_PROGRESS_STEP_PROGRESS_PERCENTAGE = "stepProgressPercentage";
        public static final String REPORT_PROGRESS_STEP = "step";
        public static final String REPORT_PROGRESS_RESULT = "result";
        public static final String REPORT_PROGRESS_RESULT_INFO = "resultInfo";
        public static final String REPORT_PROGRESS_STATE = "state";
        public static final String REPORT_PROGRESS_TIME_ACTION_COMPLETED = "timeActionCompleted";
        public static final String REPORT_PROGRESS_TIME_ACTION_STARTED = "timeActionStarted";
        public static final String REPORT_PROGRESS_TIME_OF_LAST_STATUS_UPDATE = "timeOfLastStatusUpdate";
        public static final String REPORT_PROGRESS_ADDITIONAL_INFO = "additionalInfo";
        public static final String MO_ACTIVITY_END_PROGRESS = "moActivityEndProgress";
    }

    /**
     * This class contains all the UpgradePackage constant values of ECIM Software Upgrade Use Case.
     * 
     */
    public static class UpgradePackageMoConstants {
        public static final String UP_MO_STATE = "state";
        public static final String UP_MO_REPORT_PROGRESS = "reportProgress";
        public static final String UP_MO_UPGRADE_PACKAGE_ID = "upgradePackageId";
        public static final String UP_MO_ACTIVATION_STEP = "activationStep";
        public static final String UP_MO_URI = "uri";
        public static final String UP_MO_TYPE_OF_INVOKED_ACTION = "typeOfInvokedAction";
        public static final String UP_MO_IGNORE_BREAK_POINTS = "ignoreBreakPoints";
        public static final String UP_MO_ADMINISTRATIVE_DATA = "administrativeData";
    }

    public static class SwIMConstants {
        public static final String SWIM_ACTIVE = "active";
    }

    /**
     * This class contains all the SwMConstant values of ECIM Software Upgrade Use Case.
     * 
     */
    public static class SwMConstants {
        public static final String SWM_REPORT_PROGRESS = "reportProgress";
        public static final String SWM_FALLBACKTIMER = "fallbackTimer";
    }

    public static class ProductData {
        public static final String DESCRIPTION = "description";
        public static final String PRODUCTION_DATE = "productionDate";
        public static final String PRODUCT_NAME = "productName";
        public static final String PRODUCT_NUMBER = "productNumber";
        public static final String PRODUCT_REVISION = "productRevision";
        public static final String TYPE = "type";
    }

    public static class LicenseMoConstants {
        public static final String LM_STATE = "lmState";
        public static final String KEYFILEMANAGEMENT_REPORT_PROGRESS = "reportProgress";
        public static final String LAST_ACTION_TRIGGERED = "lastActionTriggered";
        public static final String FAILSAFE_ACTIVATE_TRIGGERED = "isActivateTriggered";
        public static final String FAILSAFE_DEACTIVATE_TRIGGERED = "isDeActivateTriggered";
        public static final String FAILSAFE_ACTIVATE = "ACTIVATE";
        public static final String FAILSAFE_DEACTIVATE = "DEACTIVATE";
        public static final String FAILSAFE_CREATE = "CREATE";
        public static final String FAILSAFE_DELETE = "DELETE";
        public static final String FAILSAFE = "FAILSAFE";
        public static final String FAILSAFEDEACTIVATE = "FAILSAFE_DEACTIVATE";
        public static final String FAILSAFE_ACTIVATE_BACKUP = "activate";
        public static final String FAILSAFE_DEACTIVATE_BACKUP = "deactivate";
    }

    public static class AutoProvisioningMoConstants {
        public static final String SITE_CONFIG_COMPLETE = "SITE_CONFIG_COMPLETE";
        public static final String READY_FOR_SERVICE = "READY_FOR_SERVICE";
        public static final String RBS_CONFIG_LEVEL_KEY = "rbsConfigLevel";
        public static final String AP_MO = "AutoProvisioning";
        public static final String AP_MO_NAMESPACE = "RmeAI";
    }
}