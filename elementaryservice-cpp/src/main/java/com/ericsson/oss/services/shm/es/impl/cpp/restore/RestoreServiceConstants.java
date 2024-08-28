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
package com.ericsson.oss.services.shm.es.impl.cpp.restore;

public class RestoreServiceConstants {
    public static final String CONFIGURATION_VERSION_MO = "ConfigurationVersion";
    public static final String CVMO_MISSING_UPGRADE_PACKAGES = "missingUpgradePackages";
    public static final String CVMO_CORRUPTED_UPGRADE_PACKAGES = "corruptedUpgradePackages";

    public static final String CVMO_ADMINDATA_PRODUCT_REVISION = "productRevision";
    public static final String CVMO_ADMINDATA_PRODUCT_NUMBER = "productNumber";

    public static final String CORRUPTED_PKGS = "corrupted";
    public static final String MISSING_PKGS = "missing";

    public static final String SW_PKG_NAME_APPENDER = " for %s software package \"%s\". ";
    public static final String MISSING_PKG_INSTALLED = "Missing software package \"%s\" installed successfully.";
    public static final String CORRUPTED_PKG_REPLACED = "Corrupted software package \"%s\" replaced successfully.";
    public static final String INSTALL_CAN_NOT_BE_CONTINUED = "Install Activity failed for %s software package \"%s\", and remaining packages can not be continued to install.";
}
