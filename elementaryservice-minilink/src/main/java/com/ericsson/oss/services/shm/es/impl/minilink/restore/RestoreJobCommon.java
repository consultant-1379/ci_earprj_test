package com.ericsson.oss.services.shm.es.impl.minilink.restore;

import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.EXCEPTION_OCCURED_FAILURE_REASON;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.NODE_IS_NOT_SYNCED;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.RESTORE_JOB;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.CONFIG_DOWN_LOAD_OK_NOT_SET;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.SyncStatus.SYNCHRONIZED;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfConfigStatus.CONFIG_DOWN_LOAD_OK;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.minilink.backup.UnsecureFTPModelIdentity;
import com.ericsson.oss.services.shm.es.impl.minilink.common.BackupActivityProperties;
import com.ericsson.oss.services.shm.es.impl.minilink.common.BackupRestoreJobsCommon;
import com.ericsson.oss.services.shm.es.impl.minilink.common.MiniLinkDps;
import com.ericsson.oss.services.shm.es.impl.minilink.common.MiniLinkJobUtil;
import com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants;
import com.ericsson.oss.services.shm.es.impl.minilink.common.constants.SyncStatus;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@Stateless
@Traceable
@Profiled
public class RestoreJobCommon {

    @Inject
    private MiniLinkDps miniLinkDps;

    @Inject
    private MiniLinkJobUtil miniLinkJobUtil;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private BackupRestoreJobsCommon backupRestoreJobsCommon;

    private static final Logger LOGGER = LoggerFactory.getLogger(RestoreJobCommon.class);
    private static final String THIS_ACTIVITY = "verify";

    public static final String ENTITY_PHYSICAL = "entityPhysical";
    public static final String ENT_PHYSICAL_CONTAINED_IN = "entPhysicalContainedIn";
    public static final String ENT_PHYSICAL_INDEX = "entPhysicalIndex";
    public static final String ENT_PHYSICAL_ENTRY = "entPhysicalEntry";
    public static final String ENT_PHYSICAL_CLASS = "entPhysicalClass";
    public static final String ENT_PHYSICAL_PARENT_REL_POS = "entPhysicalParentRelPos";
    public static final String ENT_PHYSICAL_NAME = "entPhysicalName";
    public static final String ENT_PHYSICAL_DESCR = "entPhysicalDescr";
    public static final String ENT_PHYSICAL_SERIAL_NUM = "entPhysicalSerialNum";
    public static final String ENT_PHYSICAL_MODEL_NAME = "entPhysicalModelName";
    public static final String ENT_PHYSICAL_HARDWARE_REV = "entPhysicalHardwareRev";
    public static final String ENT_PHYSICAL_MFG_DATE = "entPhysicalMfgDate";
    public static final String ENT_PHYSICAL_ASSET_ID = "entPhysicalAssetID";
    public static final String ENT_PHYSICAL_MFG_NAME = "entPhysicalMfgName";
    public static final String FAR_END_NAME_PREFIX = "F";
    public static final String RAU_DESCR_PREFIX = "RAU";
    public static final String CONTAINER = "container";
    public static final String MODULE = "module";
    public static final String VERIFY_FAILED_CONTINUE = "Verify failed but continuing with Restore...";

    private static final String XF_CONFIG_VALIDATION_BOARD_PRODUCT_NUM = "xfConfigValidationBoardProductNum";
    private static final String XF_CONFIG_VALIDATION_BOARD_POSITION = "xfConfigValidationBoardPosition";
    private static final String XF_CONFIG__VALIDATION_BOARD_ENTRY = "xfConfigValidationBoardEntry";
    private static final String XF_CONFIG_VALIDATION_ENTRY = "xfConfigValidationEntry";
    private static final String XF_CONFIG_VALIDATION_DCN_IP = "xfConfigValidationDcnIP";

    private static final String CONNECTIVITY_INFO_FDN = ",MINILINKIndoorConnectivityInformation=1";
    private static final String MANAGED_ELEMENT = "ManagedElement=";
    private static final String NETWORK_ELEMENT = "NetworkElement";
    private static final String EQUAL_SEPARATOR = "=";
    private static final String COMMA_SEPARATOR = ",";
    private static final String IP_ADDRESS = "ipAddress";
    private static final String OSS_PREFIX = "ossPrefix";
    private static final String PRODUCT_NUMBER = "Product Number";

    public ActivityStepResult activityPrecheck(final BackupActivityProperties activityProperties) {
        try {
            final String syncStatus = miniLinkDps.getsyncStatus(activityProperties);
            if (SYNCHRONIZED.equals(SyncStatus.fromStatusValue(syncStatus))) {
                return miniLinkJobUtil.precheckSuccess(activityProperties);
            } else {
                return miniLinkJobUtil.precheckFailure(activityProperties, NODE_IS_NOT_SYNCED);
            }
        } catch (final Exception e) {
            LOGGER.error(String.format(MiniLinkConstants.LOG_EXCEPTION, RESTORE_JOB, activityProperties.getActivityName(), activityProperties.getNodeName()), e);
            return miniLinkJobUtil.precheckFailure(activityProperties, e.getMessage());
        }
    }

    public ActivityStepResult verifyPrecheck(final long activityJobId) {
        final BackupActivityProperties activityProperties = geActivityProperties(activityJobId);
        miniLinkJobUtil.writeToJobLog(activityProperties, String.format(JobLogConstants.ACTIVITY_INITIATED, THIS_ACTIVITY));
        miniLinkJobUtil.writeToJobLog(activityProperties, String.format(JobLogConstants.PROCESSING_PRECHECK, THIS_ACTIVITY));
        return miniLinkJobUtil.precheckSuccess(activityProperties);
    }
    public ActivityStepResult restorePrecheck(
        final BackupActivityProperties activityProperties) {
        try {
            final String configStatus = miniLinkDps.getConfigStatus(activityProperties);
            if (configStatus.equals(CONFIG_DOWN_LOAD_OK.getStatusValue())) {
                return miniLinkJobUtil.precheckSuccess(activityProperties);
            } else {
                return miniLinkJobUtil.precheckFailure(activityProperties,CONFIG_DOWN_LOAD_OK_NOT_SET);
            }
        } catch (Exception e) {
            LOGGER.error(String.format(MiniLinkConstants.LOG_EXCEPTION, RESTORE_JOB, activityProperties.getActivityName(), activityProperties.getNodeName()), e);
            return miniLinkJobUtil.precheckFailure(activityProperties, e.getMessage());
        }
        }
    public void verifyExecute(final long activityJobId) {
        final BackupActivityProperties activityProperties = geActivityProperties(activityJobId);
        final String nodeName = activityProperties.getNodeName();
        LOGGER.info("verifyExecute for Activity Name: {} Activity Job ID :: {}", activityProperties.getActivityName(), activityJobId);
        final boolean verifyflag = ifVerifyActivity(activityJobId);
        final String ossModelIdentityDps = miniLinkDps.getOssModelIdentity(nodeName);
        LOGGER.debug("Verifyflag in RestoreJobCommon :: {}", verifyflag);
        if (!(UnsecureFTPModelIdentity.CN210.getOssModelIdentity().equalsIgnoreCase(ossModelIdentityDps)
                || UnsecureFTPModelIdentity.CN510R1.getOssModelIdentity().equalsIgnoreCase(ossModelIdentityDps)
                || UnsecureFTPModelIdentity.TN11B.getOssModelIdentity().equalsIgnoreCase(ossModelIdentityDps))) {
            try {
                final List<ManagedObject> listOfEntPhysicalTableEntries = getEntPhysicalTableMO(nodeName);
                final List<ManagedObject> listOfXFConfigValidationBoardTableEntries = getXFConfigValidationBoardTableMO(nodeName);
                final List<ManagedObject> listOfXFConfigValidationTableEntries = getXFConfigValidationTableMO(nodeName);
                if (listOfXFConfigValidationBoardTableEntries != null && listOfXFConfigValidationTableEntries != null) {
                    final String ipAddress = (String) miniLinkDps.getMoAttribute(getNeFdn(nodeName) + CONNECTIVITY_INFO_FDN, IP_ADDRESS);
                    LOGGER.debug("ipAddress of node from connectivity info: {}", ipAddress);
                    if (!verifyParamters(listOfEntPhysicalTableEntries, listOfXFConfigValidationBoardTableEntries, activityProperties)
                            && !verifyIPAddress(ipAddress, listOfXFConfigValidationTableEntries)) {
                        if (verifyflag) {
                            LOGGER.debug("Throwing exception: mismatches found for activity:  {}", activityProperties.getActivityName());
                            //throw new Exception("Mismatches found on the node for the activity");
                            backupRestoreJobsCommon.failBackupRestoreActivity(activityProperties, EXCEPTION_OCCURED_FAILURE_REASON, "Mismatches found on the node for the activity");
                        } else {
                            LOGGER.debug("Verify failed with mismatches but will proceed with restore");
                            miniLinkJobUtil.writeToJobLog(activityProperties, VERIFY_FAILED_CONTINUE);
                        }
                    }
                    miniLinkJobUtil.writeToJobLog(activityProperties, String.format(JobLogConstants.EXECUTION_STARTED, THIS_ACTIVITY, activityProperties.getBackupFileName()));
                    miniLinkJobUtil.writeToJobLog(activityProperties, String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, activityProperties.getActivityName()));
                    if (verifyflag) {
                        miniLinkJobUtil.updateJobProperty(activityProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.getJobResult());
                        miniLinkJobUtil.sendNotificationToWFS(activityProperties);
                    }
                } else {
                    if (verifyflag) {
                        LOGGER.debug("Throwing exception: no data found for activity:  {}", activityProperties.getActivityName());
                        //throw new Exception("No data found on the node for activity");
                        backupRestoreJobsCommon.failBackupRestoreActivity(activityProperties, EXCEPTION_OCCURED_FAILURE_REASON, "No data found on the node for activity");
                    } else {
                        LOGGER.debug("Verify failed as no data found but will proceed with restore");
                        miniLinkJobUtil.writeToJobLog(activityProperties, VERIFY_FAILED_CONTINUE);
                    }

                }
            } catch (final Exception e) {
                backupRestoreJobsCommon.failWithException(activityProperties, RESTORE_JOB, e);
            }
        } else {
            miniLinkJobUtil.writeToJobLog(activityProperties, String.format(JobLogConstants.EXECUTION_STARTED, THIS_ACTIVITY, activityProperties.getBackupFileName()));
            miniLinkJobUtil.writeToJobLog(activityProperties, String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, activityProperties.getActivityName()));
            if (verifyflag) {
                miniLinkJobUtil.updateJobProperty(activityProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.getJobResult());
                miniLinkJobUtil.sendNotificationToWFS(activityProperties);
            }
        }
    }

    public BackupActivityProperties geActivityProperties(final long activityJobId) {
        return miniLinkJobUtil.getBackupActivityProperties(activityJobId, THIS_ACTIVITY, VerifyService.class);
    }

    /**
     * @param nodeName
     * @return
     */
    private List<ManagedObject> getXFConfigValidationTableMO(final String nodeName) {
        final List<ManagedObject> config = miniLinkDps.getMoByType(nodeName, XF_CONFIG_VALIDATION_ENTRY, getRootFdn(nodeName));
        if (config != null && config.size() > 1) {
            LOGGER.debug(" getXFConfigValidationTableMO size {} ", config.size());
            return config;
        }
        return null;
    }

    /**
     * @param nodeName
     * @return
     */
    private List<ManagedObject> getXFConfigValidationBoardTableMO(final String nodeName) {
        final List<ManagedObject> config = miniLinkDps.getMoByType(nodeName, XF_CONFIG__VALIDATION_BOARD_ENTRY, getRootFdn(nodeName));
        if (config != null && config.size() > 0) {
            LOGGER.debug("getXFConfigValidationBoardTableMO size:: {}", config.size());
            return config;
        }
        return null;
    }

    /**
     * @param ipAddress
     * @param listOfXFConfigValidationTableEntries
     * @return
     */
    private Boolean verifyIPAddress(final Object ipAddress, final List<ManagedObject> listOfXFConfigValidationTableEntries) {
        LOGGER.debug("inside verifyIP address: total entries in configvalidation:: {} ", listOfXFConfigValidationTableEntries.size());
        for (final ManagedObject config : listOfXFConfigValidationTableEntries) {
            final String validationDcnIp = config.getAttribute(XF_CONFIG_VALIDATION_DCN_IP);
            if (validationDcnIp != null && ipAddress.equals(validationDcnIp)) {
                LOGGER.debug("inside verifyIP address: ip matched:: {} ", validationDcnIp);
                return true;
            }
        }
        return false;
    }

    /**
     * @param listOfEntPhysicalTableEntries
     * @param listOfXFConfigValidationBoardTableEntries
     */
    private Boolean verifyParamters(final List<ManagedObject> listOfEntPhysicalTableEntries, final List<ManagedObject> listOfXFConfigValidationBoardTableEntries,
            final BackupActivityProperties activityProperties) {
        final Map<String, String> posAndProductDetails = filterEntPhysical(listOfEntPhysicalTableEntries);
        LOGGER.debug("inside verifyparameters: filteredentphysicalentries: {} ", posAndProductDetails.size());
        for (final ManagedObject configValBoardObj : listOfXFConfigValidationBoardTableEntries) {
            final String xfConfigValidationBoardProductNum = configValBoardObj.getAttribute(XF_CONFIG_VALIDATION_BOARD_PRODUCT_NUM);
            LOGGER.debug("inside verify: Checking productNum: {} ", xfConfigValidationBoardProductNum);
            if (posAndProductDetails != null && configValBoardObj.getAttribute(XF_CONFIG_VALIDATION_BOARD_POSITION) != null && xfConfigValidationBoardProductNum != null
                    && (posAndProductDetails.get(configValBoardObj.getAttribute(XF_CONFIG_VALIDATION_BOARD_POSITION)) != null)
                    && (!posAndProductDetails.get(configValBoardObj.getAttribute(XF_CONFIG_VALIDATION_BOARD_POSITION).toString()).equals(xfConfigValidationBoardProductNum))) {
                LOGGER.debug("inside verify of productNum: {} is not matching", xfConfigValidationBoardProductNum);
                miniLinkJobUtil.writeToJobLog(activityProperties, String.format(JobLogConstants.EXECUTION_STARTED, THIS_ACTIVITY, activityProperties.getBackupFileName()));
                miniLinkJobUtil.writeToJobLog(activityProperties, String.format(JobLogConstants.ACTION_FAILED, activityProperties.getActivityName()));
                miniLinkJobUtil.writeToJobLog(activityProperties, String.format(JobLogConstants.MISMATCHED_PARAMETERS, PRODUCT_NUMBER, xfConfigValidationBoardProductNum));
                miniLinkJobUtil.updateJobProperty(activityProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
                miniLinkJobUtil.sendNotificationToWFS(activityProperties);
                return false;
            }
        }
        return true;
    }

    /**
     * @param listOfEntPhysicalTableEntries
     * @return
     */
    private Map<String, String> filterEntPhysical(final List<ManagedObject> listOfEntPhysicalTableEntries) {
        LOGGER.debug("inside filterEntPhysical() {} , size :", listOfEntPhysicalTableEntries.size());
        String[] pos = null;
        String entPhysicalModelName = null;
        final Map<String, String> posAndProductNumPairs = new HashMap<String, String>();
        for (final ManagedObject obj : listOfEntPhysicalTableEntries) {
            pos = obj.getAttribute(ENT_PHYSICAL_NAME).toString().split("/");
            entPhysicalModelName = obj.getAttribute(ENT_PHYSICAL_MODEL_NAME);

            if (pos.length > 1) {
                posAndProductNumPairs.put(pos[1], entPhysicalModelName);
            }
        }
        LOGGER.debug("exiting filterEntPhysical() {} , size :", posAndProductNumPairs.size());
        return posAndProductNumPairs;
    }

    private List<ManagedObject> getEntPhysicalTableMO(final String nodeName) {
        List<ManagedObject> filteredEntPhysicalMO = null;

        final List<ManagedObject> entPhysicalMO = miniLinkDps.getMoByType(nodeName, ENT_PHYSICAL_ENTRY, getRootFdn(nodeName));
        LOGGER.debug("entPhysicalMO size before filter: {} ", entPhysicalMO.size());
        if (entPhysicalMO != null && entPhysicalMO.size() > 0) {

            filteredEntPhysicalMO = prefilterHwItemMos(entPhysicalMO);
            LOGGER.debug("entPhysicalMO after filter:: {}", filteredEntPhysicalMO.size());

        }
        return filteredEntPhysicalMO;
    }

    protected List<ManagedObject> prefilterHwItemMos(final Collection<ManagedObject> listOfXFConfigValidationRows) {
        final List<ManagedObject> res = new ArrayList<>();
        LOGGER.debug("Enter: entphysicaltable  total rows: {}", listOfXFConfigValidationRows.size());
        for (final ManagedObject hwMo : listOfXFConfigValidationRows) {
            if (hwMo.getAttribute(ENT_PHYSICAL_CLASS).equals(MODULE) && !isNullOrEmptyString((String) hwMo.getAttribute(ENT_PHYSICAL_NAME))
                    && !isNullOrEmptyString((String) hwMo.getAttribute(ENT_PHYSICAL_MODEL_NAME)) && !isFarEnd(hwMo)) {
                res.add(hwMo);
            }
        }
        LOGGER.debug("entphysicaltable filtered successfully. filtered rows: {}", res.size());
        return res;
    }

    private boolean isFarEnd(final ManagedObject hwMo) {
        final String entPhysicalName = hwMo.getAttribute(ENT_PHYSICAL_NAME);
        return !isNullOrEmptyString(entPhysicalName) && entPhysicalName.startsWith(FAR_END_NAME_PREFIX);
    }

    protected boolean isEmptyRau(final ManagedObject hwMo) {
        boolean emptyRau = false;
        final String entPhysicalDescr = hwMo.getAttribute(ENT_PHYSICAL_DESCR);
        final String entPhysicalSerialNum = hwMo.getAttribute(ENT_PHYSICAL_SERIAL_NUM);
        final String entPhysicalModelName = hwMo.getAttribute(ENT_PHYSICAL_MODEL_NAME);
        if (!isNullOrEmptyString(entPhysicalDescr) && entPhysicalDescr.startsWith(RAU_DESCR_PREFIX) && isNullOrEmptyString(entPhysicalModelName) && isNullOrEmptyString(entPhysicalSerialNum)) {
            emptyRau = true;
        }
        return emptyRau;
    }

    private boolean isNullOrEmptyString(final String inputString) {
        return inputString == null || inputString.isEmpty();
    }

    private String getRootFdn(final String nodeName) {
        final String ossPrefix = (String) miniLinkDps.getMoAttribute(getNeFdn(nodeName), OSS_PREFIX);
        if (ossPrefix == null || ossPrefix.isEmpty()) {
            return MANAGED_ELEMENT + nodeName;
        }
        return ossPrefix + COMMA_SEPARATOR + MANAGED_ELEMENT + nodeName;
    }

    private String getNeFdn(final String nodeName) {
        return NETWORK_ELEMENT + EQUAL_SEPARATOR + nodeName;
    }

    @SuppressWarnings({ "unchecked" })
    public boolean ifVerifyActivity(final long activityJobId) {
        final BackupActivityProperties activityProperties = geActivityProperties(activityJobId);
        try {
            final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
            final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) jobEnvironment.getMainJobAttributes().get(ShmConstants.JOBCONFIGURATIONDETAILS);
            for (Map.Entry<String, Object> entry : jobConfigurationDetails.entrySet()) {
                LOGGER.info("jobConfigurationDetails " + entry.getKey() + ":" + entry.getValue().toString() + ": ");
                if ("activities".equals(entry.getKey()) && (entry.getValue().toString().contains("name=verify"))) {
                    LOGGER.info("verifyflag is true ");
                    return true;
                }
            }
            LOGGER.debug("verifyflag is false");
        } catch (Exception e) {
            backupRestoreJobsCommon.failWithException(activityProperties, RESTORE_JOB, e);
        }
        return false;
    }
}
