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
package com.ericsson.oss.services.shm.es.impl.license;

public class LicensingActivityConstants {

    private LicensingActivityConstants() {

    }

    //CPP-MOM Licensing MO Constants
    public static final String LICENSE_MO = "Licensing";
    public static final String LICENSE_ID = "LicensingId";
    public static final String FINGER_PRINT = "fingerprint";
    public static final String LAST_LICENSING_PI_CHANGE = "lastLicensingPiChange";

    //Model Information Constants
    public static final String LICENSE_DATA_PO_NAMESPACE = "shm";
    public static final String LICENSE_DATA_PO = "LicenseData";
    public static final String LICENSE_DATA_LICENSE_KEYFILE_PATH = "licenseKeyFilePath";
    public static final String LICENSE_DATA_FINGERPRINT = "fingerPrint";
    public static final String LICENSE_DATA_SEQUENCE_NUMBER = "sequenceNumber";
    public static final String INSTALLED_ON = "installedOn";

    // License Properties
    public static final String LICENSE_FILE_PATH = "LICENSE_FILEPATH";

    //Action Activity Constants
    public static final String ACTION_INSTALL_LICENSE = "updateLicenseKeyFile";

    //DPS Model Information
    public static final String NODE_NAME = "neName";

    //Action Arguments
    public static final String ACTION_ARG_IP_ADDRESS = "ipAddress";
    public static final String ACTION_ARG_USER_ID = "userId";
    @SuppressWarnings("squid:S2068")
    public static final String ACTION_ARG_PASSWORD = "password";
    public static final String ACTION_ARG_SOURCE_FILE = "sFile";

    //Time stamp
    public static final String PI_TIME_STAMP = "LAST_LICENSING_PI_CHANGE";
    public static final String DATE_FORMAT = "yy-MM-dd HH:mm:ss";

    //Delete Constants
    public static final String LDF_ENDING_SEQUENCE = "_info.xml";
    
    public static final String CPP_NODE_MODEL="CPP_NODE_MODEL";
    public static final String LICENSE_INVENTORY_TYPE = "LicenseInventory";
    public static final String LICENSE_INVENTORY_FINGERPRINT = "fingerPrint";
    public static final String LICENSE_INVENTORY_INSTALLATION = "installation";
    public static final String LICENSE_INVENTORY_SEQUENCE_NUMBER = "seqNumber";

}
