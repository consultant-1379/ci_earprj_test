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
package com.ericsson.oss.shm.backup.constants;

public class CppBackupConstants {
    // ConfigurationVersionAttributes
    public static final String NAME = "name";
    public static final String IDENTITY = "identity";
    public static final String TYPE = "type";
    public static final String UPGRADEPACKAGE_ID = "upgradePackageId";
    public static final String OPERATOR_NAME = "operatorName";
    public static final String OPERATOR_COMMENT = "operatorComment";
    public static final String DATE = "date";
    public static final String STATUS = "status";
    public static final String ROLLBACK_POSITION = "rollbackPosition";
    // AdminProductData Attributes
    public static final String PRODUCT_INFO = "productInfo";
    public static final String PRODUCTION_DATE = "productionDate";
    public static final String PRODUCT_NAME = "productName";
    public static final String PRODUCT_NUMBER = "productNumber";
    public static final String PRODUCT_REVISION = "productRevision";
    // ConfigurationVersion NodeStatusInfo
    public static final String CONFIGURATIONVERSION = "ConfigurationVersion";
    public static final String CURRENT_UPGRADEPACKAGE = "currentUpgradePackage";
    public static final String STORED_CONFIGURATIONVERSIONS = "storedConfigurationVersions";
    public static final String ROLLBACK_LIST = "rollbackList";
    public static final String UPGRADEPACKAGE = "UpgradePackage";
    public static final String ADMINISTRATIVE_DATA = "administrativeData";
    public static final String TYPE_CV = "CV";
    public static final String TYPE_UP = "UP";

    // ConfigurationVersion Status Information Attributes
    public static final String CURRENT_DETAILED_ACTIVITY = "currentDetailedActivity";
    public static final String CURRENT_LOADED_CV = "currentLoadedConfigurationVersion";
    public static final String EXECUTING_CV = "executingCv";
    public static final String STARTABLE_CV = "startableConfigurationVersion";
    public static final String LASTCREATED_CV = "lastCreatedCv";
    public static final String USER_LABEL = "userLabel";
    public static final String AUTOCREATED_CV_TURNEDON = "autoCreatedCVIsTurnedOn";

    public static final String BACKUP_NAME_PROPERTY = "BACKUP_NAME";
    public static final String URI = "uri";

    public static final String BACKUP_ACCOUNT_TYPE = "backup";

}
