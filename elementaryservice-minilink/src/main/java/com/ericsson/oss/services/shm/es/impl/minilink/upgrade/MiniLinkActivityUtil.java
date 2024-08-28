package com.ericsson.oss.services.shm.es.impl.minilink.upgrade;

import static com.ericsson.oss.itpf.datalayer.dps.BucketProperties.SUPPRESS_CONSTRAINTS;
import static com.ericsson.oss.itpf.datalayer.dps.BucketProperties.SUPPRESS_MEDIATION;
import static com.ericsson.oss.services.shm.shared.constants.ActivityConstants.JOB_LOG_MESSAGE;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.MODEL_IDENTITY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeContainmentRestrictionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.FdnUtils;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.exception.NodeAttributesReaderException;
import com.ericsson.oss.services.shm.common.exception.ServerInternalException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.job.utils.NodeAttributesReaderRetryProxy;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.smrs.SmrsServiceConstants;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.minilink.common.MiniLinkDps;
import com.ericsson.oss.services.shm.es.impl.minilink.common.UnSecureFtpUtil;
import com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.minilinkindoor.common.FtpUtil;
import com.ericsson.oss.services.shm.minilinkindoor.common.ManagedObjectUtil;
import com.ericsson.oss.services.shm.nejob.cache.*;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

/**
 * This class facilitates the common methods or implementations required for all activities of Upgrade Use Case.
 */

@Stateless
@Traceable
@Profiled
public class MiniLinkActivityUtil {

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    @Inject
    private DpsReader dpsReader;

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private ManagedObjectUtil managedObjectUtil;

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    @Inject
    private FtpUtil ftpUtil;

    @Inject
    private FdnUtils fdnUtils;

    @Inject
    private MiniLinkDps miniLinkDps;

    @Inject
    private UnSecureFtpUtil unSecureFtpUtil;

    @Inject
    private NodeAttributesReaderRetryProxy nodeAttributesReaderRetryProxy;

    private static final Logger LOGGER = LoggerFactory.getLogger(MiniLinkActivityUtil.class);

    private static final String RAU_PATTERN = "RAU";
    private static final String PATTERN_PNUM_REVISION = "(CX[A-Z][0-9]+_[0-9]+).*(MINI-LINK.*)";
    private static final String PATTERN_RAU_PNUM_REVISION = "(CX[A-Z][0-9]+)_RAU_(.*)";
    private static final int INDEX_PRODUCTNUM = 1;
    private static final int INDEX_REVISION = 2;
    private static final String LIVE_BUCKET = "Live";
    private static final String XF_DCN_FTP_CONFIG_ENTRY = "xfDcnFTPConfigEntry";
    private static final String XF_DCN_FTP_CONFIG_ALIAS = "xfDcnFTPConfigAlias";
    private static final String XF_DCN_FTP_CONFIG_ADDRESS = "xfDcnFTPConfigAddress";
    private static final String SOFTWARE_ALIAS = "ENM_Upgrade_SMRS";

    /**
     * Returns a job property
     * 
     * @param activityJobId
     * @param propertyKey
     * @return String
     */
    @SuppressWarnings("unchecked")
    public String fetchJobProperty(final long activityJobId, final String propertyKey) {
        final Map<String, Object> jobPoAttributes = jobUpdateService.retrieveJobWithRetry(activityJobId);
        final List<Map<String, Object>> activityJobPropertyList = (List<Map<String, Object>>) jobPoAttributes.get(ActivityConstants.JOB_PROPERTIES);

        if (activityJobPropertyList == null || activityJobPropertyList.isEmpty()) {
            throw new ServerInternalException("Couldn't get job properties of activity: " + activityJobId);
        }

        for (final Map<String, Object> jobproperty : activityJobPropertyList) {
            if (jobproperty.get(ShmConstants.KEY).equals(propertyKey)) {
                return (String) jobproperty.get(ShmConstants.VALUE);
            }
        }
        throw new ServerInternalException("Couldn't get " + propertyKey + " from job properties of activity: " + activityJobId);
    }

    public ManagedObject getManagedObject(final long activityJobId, final String type) {
        final String nodeName = activityUtils.getJobEnvironment(activityJobId).getNodeName();
        return managedObjectUtil.getManagedObject(nodeName, type);
    }

    /**
     * Updates a ManagedObject
     * 
     * @param activityJobId
     * @param type
     * @param attributes
     */
    public void updateManagedObject(final long activityJobId, final String type, final Map<String, Object> attributes) {
        updateManagedObject(getManagedObject(activityJobId, type), attributes);
    }

    public void updateManagedObject(final ManagedObject managedObject, final Map<String, Object> attributes) {
        LOGGER.trace("Updating MO {} with attributes {}", managedObject, attributes);
        if (attributes != null && managedObject != null && !attributes.isEmpty()) {
            managedObject.setAttributes(attributes);
        }
    }

    /**
     * Returns the passive release entry MO
     * 
     * @param xfSwObjectsMO
     */
    public ManagedObject getXfSwReleaseEntryMO(final ManagedObject xfSwObjectsMO) {
        final int activeRelease = xfSwObjectsMO.getAttribute(MiniLinkConstants.XF_SW_ACTIVE_RELEASE);
        final String xfSwObjectsFdn = xfSwObjectsMO.getFdn();
        final String fdn = xfSwObjectsFdn + "," + MiniLinkConstants.XF_SW_RELEASE_TABLE + "=1," + MiniLinkConstants.XF_SW_RELEASE_ENTRY + "=" + (3 - activeRelease); // Change release 1->2, 2->1
        return dpsReader.findMoByFdn(fdn);
    }

    /**
     * Returns active bank number
     * 
     * @param activityJobId
     */
    public int getActiveRelease(final long activityJobId) {
        final ManagedObject xfSwObjectsMO = getManagedObject(activityJobId, MiniLinkConstants.XF_SW_OBJECTS);
        return xfSwObjectsMO.getAttribute(MiniLinkConstants.XF_SW_ACTIVE_RELEASE);
    }

    /**
     * Returns the oper status of passive bank
     * 
     * @param activityJobId
     * @return String xfSwReleaseOperStatus of passive bank
     */
    public String getXfSwReleaseOperStatus(final long activityJobId) {
        final ManagedObject swObjectsMo = getManagedObject(activityJobId, MiniLinkConstants.XF_SW_OBJECTS);
        final ManagedObject xfSwReleaseEntryMo = getXfSwReleaseEntryMO(swObjectsMo);
        return xfSwReleaseEntryMo.getAttribute(MiniLinkConstants.XF_SW_RELEASE_OPER_STATUS);
    }

    /**
     * Returns if RAU upgrade failed
     * 
     * @param activityJobId
     * @return boolean
     */
    public boolean isRAUUpgradeFailure(final long activityJobId) {
        final ManagedObject swObjectsMo = getManagedObject(activityJobId, MiniLinkConstants.XF_SW_OBJECTS);
        final ManagedObject entryMO = getXfSwLmUpgradeEntryMO(swObjectsMo.getFdn(), fetchJobProperty(activityJobId, ShmConstants.KEY_LMUPGRADE_ENTRY_INDEX));
        final String failure = entryMO.getAttribute(MiniLinkConstants.XF_SW_LMUPGRADE_FAILURE);
        return !failure.equals(MiniLinkConstants.xfSwLmUpgradeFailure.noFailure.name());
    }
    /**
     * Finishes Install License Activity
     *
     * @param jobActivityInfo
     * @param unsubscribeEventFdn
     * @param jobResult
     * @param jobLogList
     * @param activityName
     */
    public void finishInstallActivity(final JobActivityInfo jobActivityInfo, final String unsubscribeEventFdn, final JobResult jobResult,
                                      final List<Map<String, Object>> jobLogList, final String activityName) {
        LOGGER.debug("Finishing {} activity.", activityName);
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(jobActivityInfo.getActivityJobId());
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final long activityJobId = jobActivityInfo.getActivityJobId();

        activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult(), jobPropertyList);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, 99.0);
        unsubscribeFromMoNotifications(unsubscribeEventFdn, activityJobId, jobActivityInfo);
        if (jobResult != JobResult.SUCCESS) {
            logJobLogs(jobLogList, jobEnvironment.getNodeName());
        }
    }
    /**
     * Sends Notification to workflows
     *
     * @param jobActivityInfo
     * @param activityName
     */
    public void sendNotification(final JobActivityInfo jobActivityInfo, final String activityName) {
        final long activityJobId = jobActivityInfo.getActivityJobId();
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY);
        } catch (JobDataNotFoundException e) {
            LOGGER.error("NE job static data not found in neJob cache and failed to get from DPS. {}", e);
        }
        activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, activityName, null);
    }

    public void unsubscribeFromMoNotifications(final String moFdn, final long activityJobId, final JobActivityInfo jobActivityInfo) {
        if (moFdn != null) {
            activityUtils.unSubscribeToMoNotifications(moFdn, activityJobId, jobActivityInfo);
            activityUtils.unSubscribeToMoNotifications(getParentFdn(moFdn), activityJobId, jobActivityInfo);
        }
    }

    public void subscribeToMoNotifications(final String moFdn, final long activityJobId, final JobActivityInfo jobActivityInfo) {
        activityUtils.subscribeToMoNotifications(moFdn, activityJobId, jobActivityInfo);
        activityUtils.subscribeToMoNotifications(getParentFdn(moFdn), activityJobId, jobActivityInfo);
    }

    /**
     * Formats and invokes activityUtils.createNewLogEntry.
     * 
     * @param jobLogLevel
     * @param logMessage
     * @param placeHolders
     */
    public Map<String, Object> createNewLogEntry(final JobLogLevel jobLogLevel, final String logMessage, final Object... placeHolders) {
        return activityUtils.createNewLogEntry(String.format(logMessage, placeHolders), jobLogLevel.toString());
    }

    /**
     * Sets a job property
     * 
     * @param key
     * @param value
     * @param activityJobId
     */
    public void setJobProperty(final String key, final String value, final long activityJobId) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        activityUtils.addJobProperty(key, value, jobPropertyList);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, null, 0.0);
    }

    /**
     * Returns the software package name from the job properties
     * 
     * @param activityJobId
     */
    public String getSwPkgName(final long activityJobId) {
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final String nodeName = jobEnvironment.getNodeName();
        final List<NetworkElement> networkElementList = fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(Collections.singletonList(nodeName));
        final String neType = networkElementList.get(0).getNeType();
        final String platform = networkElementList.get(0).getPlatformType().name();
        final Map<String, String> swpPkg = jobPropertyUtils.getPropertyValue(Collections.singletonList(UpgradeActivityConstants.SWP_NAME),
                activityUtils.getJobConfigurationDetails(activityJobId), nodeName, neType, platform);
        return swpPkg.get(UpgradeActivityConstants.SWP_NAME);
    }

    /**
     * Returns from the activityJobId, that the package is RAU or not (SBL)
     * 
     * @param activityJobId
     * @return boolean
     */
    public boolean isRAUPackage(final long activityJobId) {
        final String packageName = getSwPkgName(activityJobId);
        return isRAUPackage(packageName);
    }

    /**
     * Returns from the packageName, that the package is RAU or not (SBL)
     * 
     * @param packageName
     * @return boolean
     */
    public boolean isRAUPackage(final String packageName) {
        return packageName.contains(RAU_PATTERN);
    }

    /**
     * Example: ManagedElement=MLCN-REAL-93,xfSwObjects=1,xfSwLmUpgradeTable=1,xfSwLmUpgradeEntry=1
     * 
     * @param xfSwObjectsFdn
     * @param entryNumber
     * @return ManagedObject
     */
    public ManagedObject getXfSwLmUpgradeEntryMO(final String xfSwObjectsFdn, final String entryNumber) {
        final String fdn = xfSwObjectsFdn + "," + MiniLinkConstants.XF_SW_LMUPGRADE_TABLE + "=1," + MiniLinkConstants.XF_SW_LMUPGRADE_ENTRY + "=" + entryNumber;
        return dpsReader.findMoByFdn(fdn);
    }

    /**
     * Updates the productNumber entry in the xfSwLmUpgradeTable. Returns true, if it was successful. Saves the index of the entry into the job properties.
     * 
     * @param activityJobId
     * @param xfSwObjectsFdn
     * @param productNumber
     * @param arguments
     * @return boolean
     */
    public boolean updateXfSwLmUpgradeTableEntry(final long activityJobId, final String xfSwObjectsFdn, final String productNumber, final Map<String, Object> arguments) {
        boolean foundRAU = false;

        final String fdn = xfSwObjectsFdn + "," + MiniLinkConstants.XF_SW_LMUPGRADE_TABLE + "=1";
        final ManagedObject lmUpgradeTable = dpsReader.findMoByFdn(fdn);
        final Collection<ManagedObject> entries = lmUpgradeTable.getChildren();
        for (final ManagedObject entryMo : entries) {
            final String numberOfEntry = entryMo.getFdn().substring(entryMo.getFdn().lastIndexOf('=') + 1);
            final String prodNum = entryMo.getAttribute(MiniLinkConstants.XF_SW_LMUPGRADE_PRODUCT_NUMBER);
            foundRAU = prodNum.replace(" ", "").equals(productNumber);
            if (foundRAU) {
                setJobProperty(ShmConstants.KEY_LMUPGRADE_ENTRY_INDEX, numberOfEntry, activityJobId);
                updateManagedObject(entryMo, arguments);
                return true;
            }
        }
        LOGGER.warn("xfSwLmUpgradeTable update fail. Product is missing: %s", productNumber);
        return false;
    }

    public String[] getProductNumberAndRevisionSBL(final String swpPkgName) {
        return getProductNumberAndRevision(PATTERN_PNUM_REVISION, swpPkgName);
    }

    public String[] getProductNumberAndRevisionRAU(final String swpPkgName) {
        return getProductNumberAndRevision(PATTERN_RAU_PNUM_REVISION, swpPkgName);
    }

    String[] getProductNumberAndRevision(final String patternInput, final String swpPkgName) {
        final Pattern pattern = Pattern.compile(patternInput);
        final Matcher matcher = pattern.matcher(swpPkgName);
        if (matcher.find()) {
            return new String[] { matcher.group(INDEX_PRODUCTNUM), matcher.group(INDEX_REVISION) };
        }
        throw new ServerInternalException("Fetching product number and revision failed.");
    }

    /**
     * Returns the jobLogList filled with error messages
     * 
     * @param jobLogList
     * @param isRAU
     * @param activityJobID
     * @param globalState
     */
    public void getErrorJobLog(final List<Map<String, Object>> jobLogList, final boolean isRAU, final long activityJobID, final List<String> globalState) {
        if (isRAU) {
            final ManagedObject swObjectsMo = getManagedObject(activityJobID, MiniLinkConstants.XF_SW_OBJECTS);
            final ManagedObject entryMO = getXfSwLmUpgradeEntryMO(swObjectsMo.getFdn(), fetchJobProperty(activityJobID, ShmConstants.KEY_LMUPGRADE_ENTRY_INDEX));

            final String operStatus = entryMO.getAttribute(MiniLinkConstants.XF_SW_LMUPGRADE_OPER_STATUS);
            final String failure = entryMO.getAttribute(MiniLinkConstants.XF_SW_LMUPGRADE_FAILURE);

            activityUtils.addJobLog(String.format("Upgrade failure. %s: %s; %s: %s; %s: %s", MiniLinkConstants.XF_SW_GLOBAL_STATE, globalState, MiniLinkConstants.XF_SW_LMUPGRADE_OPER_STATUS,
                    operStatus, MiniLinkConstants.XF_SW_LMUPGRADE_FAILURE, failure), JobLogType.SYSTEM.getLogType(), jobLogList, JobLogLevel.WARN.getLogLevel());
        } else {
            final String operStatus = getXfSwReleaseOperStatus(activityJobID);
            activityUtils.addJobLog(String.format("Upgrade failure. %s: %s %s: %s", MiniLinkConstants.XF_SW_GLOBAL_STATE, globalState, MiniLinkConstants.XF_SW_RELEASE_OPER_STATUS, operStatus),
                    JobLogType.SYSTEM.getLogType(), jobLogList, JobLogLevel.WARN.getLogLevel());
        }
    }

    /**
     * Returns true if all xfSwBoardEntry with a product number has the right revision in xfSwBoardTable.
     * 
     * @param activityJobId
     * @return boolean
     */
    public boolean isXfSwBoardTableUpdated(final Long activityJobId, final List<Map<String, Object>> jobLogList) {
        final ManagedObject xfSwObjectsMo = getManagedObject(activityJobId, MiniLinkConstants.XF_SW_OBJECTS);
        final String xfSwObjectsFdn = xfSwObjectsMo.getFdn();
        final String packageName = getSwPkgName(activityJobId);
        final String[] productNumberAndRevision = getProductNumberAndRevisionRAU(packageName);
        final String fdn = xfSwObjectsFdn + "," + MiniLinkConstants.XF_SW_BOARD_TABLE + "=1";
        final ManagedObject boardTable = dpsReader.findMoByFdn(fdn);
        final Collection<ManagedObject> entries = boardTable.getChildren();
        boolean found = false;
        for (final ManagedObject entryMo : entries) {
            final String prodNum = entryMo.getAttribute(MiniLinkConstants.XF_SW_BOARD_PRODUCT_NUMBER).toString().replace(" ", "");
            final String status = entryMo.getAttribute(MiniLinkConstants.XF_SW_BOARD_STATUS).toString().toLowerCase();
            if (productNumberAndRevision[0].equals(prodNum) && MiniLinkConstants.xfSwBoardStatus.active.toString().equals(status)) {
                found = true;
                final String prodRevision = entryMo.getAttribute(MiniLinkConstants.XF_SW_BOARD_REVISION);
                if (!productNumberAndRevision[1].equals(prodRevision)) {
                    activityUtils.addJobLog(String.format("Revision has not changed: %s", prodRevision), JobLogType.SYSTEM.getLogType(), jobLogList, JobLogLevel.WARN.getLogLevel());
                    LOGGER.warn("xfSwBoardTable update fail. Product number: {} revision: {} != {}", prodNum, prodRevision, productNumberAndRevision[1]);
                    return false;
                }
            }
        }
        if (found) {
            activityUtils.addJobLog("Revision of RAU has been updated.", JobLogType.SYSTEM.getLogType(), jobLogList, JobLogLevel.INFO.getLogLevel());
            return true;
        } else {
            activityUtils.addJobLog("RAU is missing from xfSwBoardTable.", JobLogType.SYSTEM.getLogType(), jobLogList, JobLogLevel.WARN.getLogLevel());
            LOGGER.warn("xfSwBoardTable update fail. Product is missing: {}", productNumberAndRevision[0]);
            return false;
        }
    }

    public void updateXfSwReleaseEntry(final long activityJobId, final Map<String, Object> parameters) {
        final ManagedObject swObjectsMo = getManagedObject(activityJobId, MiniLinkConstants.XF_SW_OBJECTS);
        final ManagedObject xfSwReleaseEntryMo = getXfSwReleaseEntryMO(swObjectsMo);
        updateManagedObject(xfSwReleaseEntryMo, parameters);
    }

    public void updateXfSwLmUpgradeTable(final long activityJobId, final String xfSwObjectsFdn, final Map<String, Object> parameters) {
        final String[] productNumberAndRevision = getProductNumberAndRevisionRAU(getSwPkgName(activityJobId));
        updateXfSwLmUpgradeTableEntry(activityJobId, xfSwObjectsFdn, productNumberAndRevision[0], parameters);
    }

    public String getXfSwObjectsFdn(final Long activityJobId) {
        return getManagedObject(activityJobId, MiniLinkConstants.XF_SW_OBJECTS).getFdn();
    }

    @SuppressWarnings("unchecked")
    public List<String> getGlobalState(final long activityJobId) {
        final ManagedObject swObjectsMo = getManagedObject(activityJobId, MiniLinkConstants.XF_SW_OBJECTS);
        return Collections.unmodifiableList((List<String>) swObjectsMo.getAttribute(MiniLinkConstants.XF_SW_GLOBAL_STATE));
    }

    public String getCommitType(final long activityJobId) {
        return getManagedObject(activityJobId, MiniLinkConstants.XF_SW_OBJECTS).getAttribute(MiniLinkConstants.XF_SW_COMMIT_TYPE);
    }

    public boolean updateXfSwLmUpgradeTable(final long activityJobId, final String xfSwObjectsFdn, final String packageName) {
        final String[] productNumberAndRevision = getProductNumberAndRevisionRAU(packageName);
        final boolean revUpdate = updateXfSwLmUpgradeTableEntry(activityJobId, xfSwObjectsFdn, productNumberAndRevision[0],
                Collections.<String, Object> singletonMap(MiniLinkConstants.XF_SW_LMUPGRADE_REVISION, productNumberAndRevision[1]));
        return revUpdate && updateXfSwLmUpgradeTableEntry(activityJobId, xfSwObjectsFdn, productNumberAndRevision[0],
                Collections.<String, Object> singletonMap(MiniLinkConstants.XF_SW_LMUPGRADE_ADMIN_STATUS, MiniLinkConstants.xfSwLmUpgradeAdminStatus.upgradeStarted.toString()));
    }

    public void abortUpdateInXfSwLmUpgradeEntry(final long activityJobId) {
        final ManagedObject swObjectsMo = getManagedObject(activityJobId, MiniLinkConstants.XF_SW_OBJECTS);
        final ManagedObject xfSwLmUpgradeEntryMO = getXfSwLmUpgradeEntryMO(swObjectsMo.getFdn(), fetchJobProperty(activityJobId, ShmConstants.KEY_LMUPGRADE_ENTRY_INDEX));
        updateManagedObject(xfSwLmUpgradeEntryMO,
                Collections.<String, Object> singletonMap(MiniLinkConstants.XF_SW_LMUPGRADE_ADMIN_STATUS, MiniLinkConstants.xfSwLmUpgradeAdminStatus.upgradeAborted.toString()));
    }

    public void setSmrsFtpOnNode(final long activityJobId, final String nodeName) {
        if (MODEL_IDENTITY.equals(miniLinkDps.getOssModelIdentity(nodeName))) {
            unSecureFtpUtil.setupFtp(nodeName, SmrsServiceConstants.SOFTWARE_ACCOUNT);
        } else {
            final Integer tableEntry = ftpUtil.setupFtp(nodeName, SmrsServiceConstants.SOFTWARE_ACCOUNT);
            updateManagedObject(activityJobId, MiniLinkConstants.XF_DCN_FTP,
                    Collections.<String, Object> singletonMap(MiniLinkConstants.XF_SW_UPGRADE_ACTIVE_FTP, tableEntry));
        }
    }

    public void setXfSwGlobalStateWithoutMediation(final long activityJobId, final MiniLinkConstants.xfSwGlobalState globalState) {
        final DataBucket liveBucket = dataPersistenceService.getDataBucket(LIVE_BUCKET, SUPPRESS_MEDIATION, SUPPRESS_CONSTRAINTS);
        final String baseMoFdn = fdnUtils.getMeContextFdnFromNodeName(activityUtils.getJobEnvironment(activityJobId).getNodeName());
        final String nameSpace = getManagedObject(activityJobId, MiniLinkConstants.XF_SW_OBJECTS).getNamespace();
        final Query<TypeContainmentRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(nameSpace, MiniLinkConstants.XF_SW_OBJECTS, baseMoFdn);
        final ManagedObject xfSwObjectsMo = (ManagedObject) liveBucket.getQueryExecutor().execute(query).next();
        xfSwObjectsMo.setAttribute(MiniLinkConstants.XF_SW_GLOBAL_STATE, Collections.<String> singletonList(globalState.name()));
    }

    public String getParentFdn(final String fdn) {
        final int lastIndex = fdn.lastIndexOf(',');
        if (lastIndex < 0) {
            return fdn;
        }
        return fdn.substring(0, lastIndex);
    }

    /**
     * Utility method for logging the job logs into info.
     * 
     * @param jobLogList
     * @param nodeName
     */
    public void logJobLogs(final List<Map<String, Object>> jobLogList, final String nodeName) {
        for (final Map<String, Object> jobLog : jobLogList) {
            LOGGER.info("{} joblog: {}", nodeName, jobLog.get(JOB_LOG_MESSAGE));
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean isSmrsDetailFoundOnNode(final long activityJobId) throws NodeAttributesReaderException {
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final String nodeName = jobEnvironment.getNodeName();
        if (MODEL_IDENTITY.equals(miniLinkDps.getOssModelIdentity(nodeName))) {
            return true;
        } else {
            final ManagedObject ftpEntryInDps = findFtpEntryFromDps(nodeName);
        LOGGER.info("MiniLinkActivityUtil: Found upgrade FtpEntry {}", ftpEntryInDps);
        final String[] ftpAttributes = { XF_DCN_FTP_CONFIG_ADDRESS, XF_DCN_FTP_CONFIG_ALIAS };
            Map<String, Object> ftpAttributesOnNode;
            if (ftpEntryInDps != null) {
                for (int i = 0; i < 4; i++) {
                    /*
                     * FTP attributes will be fetched directly from node
                     */
                ftpAttributesOnNode = nodeAttributesReaderRetryProxy.readAttributesWithRetry(ftpEntryInDps, ftpAttributes);
                LOGGER.info("MiniLinkActivityUtil: ftpAttributes on Node {} on retrieval-{}  are: {}", nodeName, i, ftpAttributesOnNode);
                    if (ftpEntryInDps.getAttribute(XF_DCN_FTP_CONFIG_ADDRESS).equals(ftpAttributesOnNode.get(XF_DCN_FTP_CONFIG_ADDRESS))
                            && ftpEntryInDps.getAttribute(XF_DCN_FTP_CONFIG_ALIAS).equals(ftpAttributesOnNode.get(XF_DCN_FTP_CONFIG_ALIAS))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private ManagedObject findFtpEntryFromDps(final String nodeName) {
        final List<ManagedObject> ftpEntryMos = managedObjectUtil.getManagedObjects(nodeName, XF_DCN_FTP_CONFIG_ENTRY);
        if (ftpEntryMos != null) {
            for (final ManagedObject ftpEntry : ftpEntryMos) {
                final String alias = ftpEntry.getAttribute(XF_DCN_FTP_CONFIG_ALIAS);
                if (SOFTWARE_ALIAS.equals(alias)) {
                    return ftpEntry;
                }
            }
        }
        return null;
    }
}
