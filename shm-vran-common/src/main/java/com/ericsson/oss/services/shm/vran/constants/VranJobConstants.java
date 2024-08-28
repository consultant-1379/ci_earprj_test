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
package com.ericsson.oss.services.shm.vran.constants;

/**
 * Provides constant values for VRAN related jobs.
 * 
 * @author xindkag
 */
public class VranJobConstants {

    public static final String VRAN = "vRAN";
    public static final String VNF_JOB_ID = "vnfJobId";
    public static final String VNF_ID = "vnf_id";
    public static final String VNF_PACKAGE_ID = "vnfPackageId";
    public static final String UPGRADE_JOB_VNF_PACKAGE_ID = "vnf_package_id";
    public static final String VNFD_ID = "vnfdId";
    public static final String VNF_DESCRIPTOR_ID = "vnfdescriptorid";
    public static final String UPGRADE_JOB_VNF_DESCRIPTOR_ID = "vnf_descriptor_id";
    public static final String VNF_PACKAGE_TYPE = "VnfPackage";
    public static final String FALLBACK_TIMER = "fallback_timeout";
    public static final String VIRTUAL_MANAGER = "virtualManager";
    public static final String NO_VNFM_FOUND = "VirtualNetworkFunctionManager not found for the selected network element";
    public static final String VIRTUAL_NETWORK_FUNCTION_DATA_RDN = "VirtualNetworkFunctionData=1";
    public static final String VIRTUAL_NETWORK_FUNCTION_MANAGER_FDN = "VirtualNetworkFunctionManager=";
    public static final String JOB_CANCEL = "Cancel";
    public static final String SUBSCRIPTION_KEY_DELIMETER = "@";
    public static final String FROM_VNF_ID = "from_vnf_id";
    public static final String TO_VNF_ID = "to_vnf_id";
    public static final String NO_OF_RETRIES = "noOfRetries";
    public static final String INTERVAL_IN_SECONDS = "intervalInSec";
    public static final String CLOSED_SQUARE_BRACKET = "]";
    public static final String LAST_UPGRADE_NE_JOB_ID = "lastUpgradeNeJobId";

    // Notification Type
    public static final int PROGRESS_LEVEL1 = 0;
    public static final int PROGRESS_LEVEL2 = 50;
    public static final int PROGRESS_LEVEL3 = 100;
    public static final String ACTION = "action";
    public static final String ACTIVITY_NAME = "ACTIVITY_NAME";
    public static final String ACTION_TRIGGERED = "actionTriggered";
    public static final String PROCESSING_STATUS = "processing";
    public static final String PROGRESS = "progress";
    public static final String FAILED_STATUS = "failed";
    public static final String FAIL = "failure";
    public static final String HANDLE_TIMEOUT = "HANDLE_TIMEOUT";
    public static final String ACTIVITY_JOB_ID = "activityJobId";

    public static final String UPGRADE = "UPGRADE";
    public static final String VRAN_UPGRADE_JOB_PROGRESS_NOTIFICATION_FILTER = "(platformType = '" + VranJobConstants.VRAN + "' AND jobType = '" + UPGRADE + "')";
    // onboard constants
    public static final String ONBOARD = "onboard";
    public static final String ON_BOARD_JOBID = "onBoardJobId";
    private static final String SOFTWARE_PACKAGE_ONBOARD = "SwPackageOnboard";
    public static final String VNF_PACKAGES_TO_ONBOARD = "VNF_PACKAGES_TO_ONBOARD";
    public static final String CREATE_SW_PACKAGE_ONBOARD_JOB = "SW_PACKAGE_CREATE_ON_BOARD_JOB";
    public static final String SW_PACKAGE_POLL_JOB = "SW_PACKAGE_POLL_JOB";

    public static final String NFVO = "NFVO";
    public static final String SMRS = "SMRS";
    public static final String SMRS_LOCATION = "ENM";
    public static final String SMRS_NFVO = "SMRS_NFVO";
    public static final String NFVO_MO = "NetworkFunctionVirtualizationOrchestrator";
    public static final String NFVO_JOB_ID = "nfvoJobId";
    public static final String NFVO_JOB_STATUS = "nfvoJobStatus";
    public static final String NFVO_JOB_STATUS_DESCRIPTION = "nfvoJobStatusDescription";

    public static final String FULL_FOLDER_PATH = "fullFolderPath";
    public static final String SW_PACKAGE_LOCATION = "location";
    public static final String SOFTWAREPACKAGE_NFVODETAILS = "nfvoDetails";
    public static final String SMRS_FILEPATH_NOT_FOUND = "SMRS filepath not found for the selected software package : ";
    public static final String TOTAL_PKG_COUNT = "totalPckgCount";
    public static final String TOTAL_NUMBER_OF_PACKAGES_IN_ENM = "totalNumberOfPackages";
    public static final String CURRENT_DELETED_PACKAGE_INDEX_IN_ENM = "currentPackageIndex";
    public static final String ONBOARD_JOB_PKG_STATUS = "onboardJobPkgStatus";
    public static final String FAILED_PKG_COUNT = "failedPkgCount";
    public static final String FAILED_PKG_COUNT_IN_ENM = "failedpackageCountInENM";
    public static final String VRAN_ONBOARD_JOB_PROGRESS_NOTIFICATION_FILTER = "(platformType = '" + VRAN + "' AND jobType = '" + SOFTWARE_PACKAGE_ONBOARD + "')";

    //Newly added constants for onboard
    public static final String TOTAL_NUMBER_OF_PACKAGES_TO_BE_ONBOARDED = "totalNumberOfPackagesToBeOnboarded";
    public static final String CURRENT_PACKAGE_INDEX_TO_BE_ONBOARDED = "currentPackageIndexToBeOnboarded";
    public static final String ONBOARD_FAILURE_PACKAGES_COUNT = "onboardFailurePackagesCount";
    public static final String ONBOARD_SUCCESS_PACKAGES_COUNT = "onboardSuccessPackagesCount";
    // Delete Software package Constants
    public static final String DEL_SW_ACTIVITY = "delete_softwarepackage";
    public static final String BASIC_PACKAGE = "basic";
    public static final String DELETE_VNF_PACKAGES_FROM_NFVO = "DELETE_VNF_PACKAGES_FROM_NFVO";
    public static final String DELETE_VNF_PACKAGES_FROM_ENM = "DELETE_VNF_PACKAGES_FROM_ENM";
    public static final String VRAN_SOFTWAREPACKAGE_NAME = "VRAN.SWP_NAME";
    public static final String CREATE_SW_PACKAGE_DELETE_JOB = "SW_PACKAGE_CREATE_DELETE_JOB";

    public static final String TOTAL_NUMBER_OF_PACKAGES_IN_NFVO = "totalDelPkgCount";
    public static final String CURRENT_DELETED_PACKAGE_INDEX_IN_NFVO = "currentDelPkgIndex";
    public static final String NUMBER_OF_FAILURE_PACKAGES_IN_NFVO = "failedDelPkgCount";
    public static final String DELETE_FROM_TARGET = "deleteFromTarget";
    public static final String DELETE_SW_PKG_IN_PROGRESS = "Deleting package";
    public static final String UPDATE_SW_PKG_IN_PROGRESS = "Updating Package";
    public static final String REMOVE_PKG_IN_PROGRESS = "Removing package";
    public static final String DELETEFROMENM = "delete_softwarepackage_enm";

    public static final String FAILED_PACKAGES_FROM_NFVO = "failedPackagesFromNfvo";
    public static final String FAILED_PACKAGES_FROM_ENM = "failedPackagesFromEnm";
    public static final String NUMBER_OF_SUCCESS_PACKAGES_IN_NFVO = "successPackagesFromNfvo";
    public static final String NUMBER_OF_SUCCESS_PACKAGES_IN_ENM = "successPackagesFromSmrs";
    public static final String NUMBER_OF_FAILURE_PACKAGES_IN_ENM = "failedSMRSpackageCount";
    public static final String SWPACKAGE_IN_USE_BY_JOBS = "SoftwarePackage can not be deleted as it is being used by following job";

    public static final String SOFTWAREPACKAGE_NFVOIDENTIFIER = "nfvoId";
}
