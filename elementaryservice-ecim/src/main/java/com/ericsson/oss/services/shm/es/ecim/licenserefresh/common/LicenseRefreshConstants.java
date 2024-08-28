/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.ecim.licenserefresh.common;

public class LicenseRefreshConstants {

    private LicenseRefreshConstants() {

    }

    public static final String ECIM = "ECIM";
    public static final String LICENSE_REFRESH_JOB_TYPE = "LICENSE_REFRESH";
    public static final String LICENSE_REFRESH_JOB_PROGRESS_NOTIFICATION_FILTER = "(platformType = '" + ECIM + "' AND jobType = '" + LICENSE_REFRESH_JOB_TYPE + "')";
    public static final String SUBSCRIPTION_KEY_DELIMETER = "@";
    public static final String CORRELATION_ID = "correlationId";
    public static final String PROGRESS_REPORT = "progressReport";
    public static final String INSTANTANEOUS_LICENSING_MO_FDN = "instantaneousLicensingMOFdn";
    // RefreshActivity constants
    public static final String NODE_REFRESH_REQUESTED = "NODE_REFRESH_REQUESTED";
    // RequestActivity processNotification constants
    public static final String ACTIVITY_NAME_REQUEST = "REQUEST";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String LKF_IMPORT_INITIATED = "LKF_IMPORT_INITIATED";
    public static final String LKF_IMPORT_COMPLETED = "LKF_IMPORT_COMPLETED";
    public static final String JOB_IDS = "jobIds";
    public static final String CAPACITIES = "capacities";
    public static final String NODE_TYPE = "nodeType";
    public static final String SW_RELEASE = "swRelease";
    public static final String SWLT_ID = "swltId";
    public static final String EUFT = "euft";
    public static final String ACTION_ID = "actionId";
    public static final String LICENSE_REFRESH_REQUEST_TYPE = "licenseRefreshRequestType";
    public static final String FINGERPRINT = "fingerprint";
    public static final String NE_JOB_ID = "neJobId";
    public static final String SHM_NODE_LICENSE_REFRESH_REQUEST_DATA = "ShmNodeLicenseRefreshRequestData";
    public static final String MO_ACTION_REFRESH_KEY_FILE = "refreshKeyFile";
    public static final String NETWORK_ELEMENT_NAME = "networkElementName";
    public static final String LICENSE_REFRESH_SOURCE = "licenseRefreshSource";
    public static final String LICENSE_REFRESH_REQUEST_INFO = "licenseRefreshRequestInfo";
    public static final String INSTANTANEOUS_LICENSING_NAMESPACE = "RmeLicenseSupport";
    public static final String INSTANTANEOUS_LICENSING_MO = "InstantaneousLicensing";
    public static final String NODE_NAME = "networkElementName";
    public static final String LKF_REQUEST_DATA = "LkfRequestData";
    public static final String NE_NAME = "neName";
    public static final String EXPANSION = "expansion";
    public static final String REFRESH = "refresh";
    public static final String CAPACITY_REQUEST = "CAPACITY_REQUEST";
    public static final String LKF_REFRESH = "LKF_REFRESH";
    public static final String UPGRADE_LICENSE_KEYS = "UpgradeLicensekeys";
    public static final String LICENSE_REFRESH_TYPE = "LicenseRefreshType";
    public static final String AUTO_REFRESH_LICENSE_KEYS = "AutoRefreshLicenseKeys";
    public static final String REFRESH_LICENSE_KEYS = "RefreshLicenseKeys";
    public static final String LKF_REFRESH_REJECTED = "LKF_REFRESH_REJECTED";
    public static final String RESULT_CODE = "resultCode";
    public static final String REQUEST_TYPE = "requestType";
    public static final String REQUEST_INFO = "requestInfo";
    public static final String REQUEST_ID = "requestId";
    public static final String LKF_REFRESH_COMPLETED = "LKF_REFRESH_COMPLETED";
    public static final String LKF_REFRESH_REQUEST_SERVICE_HANDLE_TIMEOUT = "LKF Refresh Job failed due to Request Service Handle Timeout";

    //RefreshActivity jobLogs
    public static final String PRECHECK_INITIATED = "Precheck for \"%s\" activity is initiated.";
    public static final String MO_ACTION_SKIPPED = "License Refresh job is not initiated by the Operator.";
    public static final String BEFORE_MO_ACTION_INVOCATION = MO_ACTION_REFRESH_KEY_FILE + " action is going to be triggered for InstantaneousLicensing MO on the node \"%s\".";
    public static final String MO_ACTION_TRIGGERED = MO_ACTION_REFRESH_KEY_FILE + " action is triggered for InstantaneousLicensing MO on the node \"%s\" with actionId \"%s\".";
    public static final String MO_ACTION_FAILURE_REASON = MO_ACTION_REFRESH_KEY_FILE + " action is failed for InstantaneousLicensing MO on the node \"%s\".";
    public static final String HANDLE_TIMEOUT = "Notifications not received for the \"%s\" activity on the Node \"%s\".";
    public static final String IL_REQUEST_PROCESS_NOTIFICATION = "\"%s\" activity is completed as InstantaneousLicensing request is received from node.";
    // RequestActivity jobLogs
    public static final String REQUEST_ACTIVITY_INPROGRESS = "License Key File is received from ELIS with fingerprint \"%s\" and License Key File import is initiated.";
    public static final String REQUEST_ACTIVITY_COMPLETED = "License Key File is imported successfully.";
    public static final String REQUEST_ACTIVITY_FAILURE = "\"%s\" activity is failed. Failure Reason : %s.";
    public static final String ELIS_NOTIFICATION = "Request is sent to ELIS for License Key File with the parameters : %s.";

}
