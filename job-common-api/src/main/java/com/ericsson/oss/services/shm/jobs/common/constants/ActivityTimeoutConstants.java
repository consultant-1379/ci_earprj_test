package com.ericsson.oss.services.shm.jobs.common.constants;

public class ActivityTimeoutConstants {

    //Added for wfs timeouts for upgrade job
    public static final String INSTALL_ACTIVITY_TIMEOUT = "installTimeout";
    public static final String VERIFY_ACTIVITY_TIMEOUT = "verifyTimeout";
    public static final String UPGRADE_ACTIVITY_TIMEOUT = "upgradeTimeout";
    public static final String CONFIRM_ACTIVITY_TIMEOUT = "confirmTimeout";

    //Added for wfs activity timeouts for backup job
    public static final String CREATE_CV__ACTIVITY_TIMEOUT = "createCvTimeout";
    public static final String UPLOAD_CV__ACTIVITY_TIMEOUT = "exportCvTimeout";
    public static final String SET_STARTABLE__ACTIVITY_TIMEOUT = "setCvAsStartableTimeout";
    public static final String SET_FIRST_IN_THE_ROLLBACKLIST_ACTIVITY_TIMEOUT = "setCvFirstInRollbackList";

    public static final String DEFAULT_ACTIVITY_TIMEOUT = "defaultActivityTimeout";

    //Added for RESTORE activity
    public static final String PRERESTORE_ACTIVITY_TIMEOUT = "prerestore";
    public static final String RESTORE_DOWNLOAD_ACTIVITY_TIMEOUT = "download";
    public static final String RESTORE_VERIFY_ACTIVITY_TIMEOUT = "verify";
    public static final String RESTORE_INSTALL_ACTIVITY_TIMEOUT = "install";
    public static final String RESTORE_ACTIVITY_TIMEOUT = "restore";
    public static final String RESTORE_CONFIRM_ACTIVITY_TIMEOUT = "confirm";
    public static final String POSTRESTORE_ACTIVITY_TIMEOUT = "postrestore";

    //Added For Polling Attributes
    public static final String POLLING = "pollingWaitTime";
    public static final String DELIMETER_UNDERSCORE = "_";
    public static final String SLEEP_TIME = "sleepTime";

    public static final String NODE_SYNC_WAIT_TIME = "syncWaitTime";
    public static final String NODE_SYNC_TIMEOUT = "syncTimeOut";


}
