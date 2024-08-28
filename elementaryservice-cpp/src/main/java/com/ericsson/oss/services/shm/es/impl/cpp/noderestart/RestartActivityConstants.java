/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.cpp.noderestart;

public class RestartActivityConstants {

    public static final String RESTART_NODE = "Node Restart";
    public static final String RESTART_RANK = "restartRank";
    public static final String RESTART_REASON = "restartReason";
    public static final String RESTART_INFO = "restartInfo";
    public static final String ACTION_NAME = "manualRestart";
    public static final String RESULT_INFORMATION = " Restart Rank: %s and Restart Reason: %s.";
    public static final String RESTART_NODE_SUCCESS = "Node \"%s\" Restarted successfully.";
    public static final String RESTART_NODE_REACHABLE = "Node \"%s\" is reachable.";
    public static final String RESTART_NODE_RETRY = "Node \"%s\" is not yet reachable.. retrying.";
    public static final String MO_NOT_EXIST = "Precheck for \"%s\" is failed. Managed Element not found.";
    public static final String RESTART_NODE_ERROR = "Node \"%s\" Restart failed. Reason: \"%s\".";
    public static final String RESTART_NODE_FAIL = "Node Restart has failed.";
    public static final String RESTART_NODE_NOT_REACHABLE = "Node \"%s\" is not reachable.";
    public static final String RESTART_NODE_CHECK = "Checking if node is reachable...";
    public static final String ERBS_NODE_MODEL = "ERBS_NODE_MODEL";
    public static final String MANAGED_ELEMENT = "ManagedElement";
    public static final int DISTANCE = 1;
    public static final String NAMESPACE_NOT_FOUND = "NameSpace_NotFound";
    public static final String[] attributeNames = { "productName", "productNumber", "productRevision" };

    public static final String RESTART_REASON_VALUE = "PLANNED_RECONFIGURATION";
    public static final String RESTART_RANK_KEY = "RESTART_RANK";
    public static final String RESTART_REASON_KEY = "RESTART_REASON";
    public static final String RESTART_INFO_MESSAGE = "Restart action triggered with CV \"%s\" .";
}
