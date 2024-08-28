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
package com.ericsson.oss.services.shm.es.impl.ecim.deleteup;

public class DeleteUpgradePackageConstants {

    public static final String ACTIVE = "is active";
    public static final String INVALID = "data is invalid";
    public static final String NOT_EXISTED = "does not exist";
    public static final String PRODUCTDATA_SEPERATOR = "===";
    public static final String UP_DATA_INPUT_SEPERATOR = "\\*\\*\\|\\*\\*";

    public static final String REMOVE_UP_ACTION_NAME = "removeUpgradePackage";
    public static final String REMOVE_UP_ACTION_ARG = "upgradePackage";

    //Persisted constants
    public static final String CURRENT_UP_MO_DATA = "currentUpMoData";
    public static final String CURRENT_BKPNAME = "currentBkpName";
    public static final String PRODUCT_DATA_LIST_TO_BE_DELETED = "productDataList";
    public static final String REFFERED_BKPS_TO_BE_DELETED = "referredBackups";

    public static final String UP_INTERMEDIATE_FAILURE = "UP_INTERMEDIATE_FAILURE";
    public static final String BACKUP_INTERMEDIATE_FAILURE = "BACKUP_INTERMEDIATE_FAILURE";

    public static final String IS_ANY_ACTIVE_UP = "isAnyaciveUp";
    public static final String FAIL_UP_DELETION = "isAllaciveUp/ReferredUpsNotSelected";
}
