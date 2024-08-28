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
package com.ericsson.oss.services.shm.common.constants;

/**
 * Provides the common constants and variables.
 * 
 * @author xrajeke
 */
public class ShmCommonConstants {
    public static final String SHM_NOTIFICATION_CHANNEL_URI = "jms:/queue/shmNotificationQueue";
    public static final String MO_TYPE_LICENSING = "Licensing";
    public static final String SHM_BACKUP_NOTFICATION_FILTER = "type='ConfigurationVersion' OR type='BrmBackup' OR type='BrmBackupManager' OR type='BrmFailsafeBackup' OR type='xfConfigLoadObjects'";
    public static final String SHM_LICENSE_NOTFICATION_FILTER = "type = 'Licensing' OR type ='KeyFileManagement' OR type='xfLicenseInstallObjects'";
    public static final String SHM_UPGRADE_NOTFICATION_FILTER = "type='UpgradePackage' OR type='SwM' OR type='xfSwObjects'";
    public static final String SHM_INVENTORY_NOTIFICATION_FILTER = "(namespace = 'OSS_NE_CM_DEF' AND type = 'CmFunction') OR (type = 'SwManagement') OR (type = 'Equipment') OR (namespace = 'OSS_NE_SHM_DEF' AND type = 'SHMFunction') OR (namespace = 'OSS_NE_CM_DEF' AND type = 'CmNodeHeartbeatSupervision')";
    public static final String NAMESPACE_NETWORK_ELEMENT = "OSS_NE_DEF";
    public static final String MO_TYPE_NETWORK_ELEMENT = "NetworkElement";
    public static final String NETWORK_ELEMENT_ID = "networkElementId";
    public static final String PLATFORM = "platformType";
    public static final double EXPONENTIAL_BACK_OFF = 1.0;
    public static final String SHM_JOB_NOTIFICATION_CHANNEL_URI = "jms:/queue/shmJobNotificationQueue";
    public static final String DATABASE_SERVICE_NOT_AVAILABE = "Database not available";

    public static final String NAMESPACE_SHM = "shm";
    public static final String WORKFLOW_SERVICE_INTERNAL_ERROR = "Internal error occured in Workflowservice";

    public static final String JOB_TYPE = "Job";
    public static final String NE_JOB_TYPE = "NEJob";
    public static final String ACTIVITY_JOB_TYPE = "ActivityJob";
    public static final String SHM_JOB_NOTIFICATION_FILTER = "(namespace = '" + NAMESPACE_SHM + "' AND type = '" + JOB_TYPE + "') OR (namespace = '" + NAMESPACE_SHM + "' AND type = '" + NE_JOB_TYPE
            + "') OR (namespace = '" + NAMESPACE_SHM + "' AND type = '" + ACTIVITY_JOB_TYPE + "')";
    public static final String MO_TYPE_MECONTEXT = "MeContext";
    public static final String NETYPE = "neType";
    public static final String MO_TYPE_MANAGED_ELEMENT = "ManagedElement";

    public static final String NAMESPACE_OSS_TOP = "OSS_TOP";
    public static final String OSS_MODEL_IDENTITY = "ossModelIdentity";
    public static final String CPP = "CPP";
    public static final String NE_NAME = "neName";
    public static final String NAMESPACE_NOT_FOUND = "NameSpace_NotFound";
    public static final String NE_MODEL_NOT_SUPPORTED = "NetworkElement model not supported";

    public static final String CM_NODE_HEARTBEAT_SUPERVISION_MO_TYPE = "CmNodeHeartbeatSupervision";
    public static final String OSS_NE_CM_DEF_NAMESPACE = "OSS_NE_CM_DEF";
    public static final String CM_NODE_SUPERVISION_MO_HEARBEAT_INTERVAL = "heartbeatInterval";

    public static final String NE_PRODUCT_VERSION = "neProductVersion";
    public static final String NE_FDN = "neFdn";
    public static final String NODE_ROOT_FDN = "nodeRootFdn";
    public static final String UTC_OFFSET = "utcOffset";
    public static final String NODE_MODEL_IDENTITY = "nodeModelIdentity";
    public static final String NETWORK_ELEMENT_FILTER = "namespace = 'OSS_NE_DEF' AND type='NetworkElement'";
    public static final String SHM_LOAD_CONTROLLER_LOCALCOUNTER_NOTIFICATION_CHANNEL_URI = "jms:/topic/shmLoadControllerLocalCounterTopic";
    public static final String DPS_CONNECTION_EVENT_FILTER = "(database IS NOT NULL) AND (container='%s')";
    public static final String STRING_NULL = "null";
    public static final String SHM_STAGED_ACTIVITY_CHANNEL_URI = "jms:/queue/ShmStagedActivityQueue";

    public static final String NE_SEQURITY_MO_NAME_SPACE = "OSS_NE_SEC_DEF";
    public static final String NE_SEQURITY_MO = "NetworkElementSecurity";
    public static final String SECURE_USER_NAME = "secureUserName";
    public static final String LOCATION_NODE = "NODE";
    public static final String LOCATION_ENM = "ENM";
}
