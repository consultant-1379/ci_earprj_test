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

package com.ericsson.oss.services.shm.es.impl.minilink.common;

import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.CM_FUNCTION;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.LIVE_BUCKET;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.OSS_NE_CM_DEF;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.SYNC_STATUS;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_CONFIG_FILE_NAME;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_CONFIG_LOAD_ACTIVE_FTP;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_CONFIG_LOAD_OBJECTS;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_CONFIG_STATUS;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_DCN_FTP;
import static com.ericsson.oss.services.shm.jobs.common.constants.SmrsServiceConstants.BACKUP_ACCOUNT;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_LICENSE_INSTALL_OBJECTS;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_LICENSE_INSTALL_OPER_STATUS;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_LICENSE_INSTALL_FILE_NAME;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.NETWORKELEMENT;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.OSSMODELIDENTITY;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeContainmentRestrictionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.core.retry.RetryContext;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.FdnUtils;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.minilink.backup.UnsecureFTPModelIdentity;
import com.ericsson.oss.services.shm.minilinkindoor.common.FtpUtil;
import com.ericsson.oss.services.shm.minilinkindoor.common.ManagedObjectUtil;

@Stateless
@Traceable
@Profiled
public class MiniLinkDps {

    @Inject
    private FtpUtil ftpUtil;

    @Inject
    private ManagedObjectUtil managedObjectUtil;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private RetryManager retryManager;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    @Inject
    private FdnUtils fdnUtils;

    @Inject
    private UnSecureFtpUtil unSecureFtpUtil;

    @Inject
    private DpsRetryPolicies dpsRetryPolicies;

    public final static String DPS_BUCKET = "Live";
    public static final String SUPPRESS_MEDIATION = "mediation_off";
    public static final String SUPPRESS_CONSTRAINTS = "constraints_off";

    public static final String ENT_PHYSICAL_NAME = "entPhysicalName";
    public static final String ENT_PHYSICAL_DESCR = "entPhysicalDescr";
    public static final String ENT_PHYSICAL_SERIAL_NUM = "entPhysicalSerialNum";
    public static final String ENT_PHYSICAL_MODEL_NAME = "entPhysicalModelName";
    public static final String ENT_PHYSICAL_ENTRY = "entPhysicalEntry";

    /**
     * Sets up the FTP attributes in the mirror model that are mediated to the node.
     *
     * @param backupActivityProperties
     */
    public void setupFtpForNode(final BackupActivityProperties backupActivityProperties) {
        final String nodeName = backupActivityProperties.getNodeName();
        final String ossModelIdentity = getOssModelIdentity(nodeName);
        if (UnsecureFTPModelIdentity.CN210.getOssModelIdentity().equalsIgnoreCase(ossModelIdentity)
                || UnsecureFTPModelIdentity.CN510R1.getOssModelIdentity().equalsIgnoreCase(ossModelIdentity)
                || UnsecureFTPModelIdentity.TN11B.getOssModelIdentity().equalsIgnoreCase(ossModelIdentity)) {
            unSecureFtpUtil.setupFtp(nodeName, BACKUP_ACCOUNT);
        } else {
            final Integer ftpTableEntryIndex = ftpUtil.setupFtp(nodeName, BACKUP_ACCOUNT);
            updateManagedObjectAttribute(nodeName, XF_DCN_FTP, XF_CONFIG_LOAD_ACTIVE_FTP, ftpTableEntryIndex);
        }
        updateManagedObjectAttribute(nodeName, XF_CONFIG_LOAD_OBJECTS, XF_CONFIG_FILE_NAME, backupActivityProperties.getBackupFileWithPath());
    }

    /**
     * Utility function for setting the attribute of an MO corresponding to the given node and type.
     *
     * @param nodeName
     * @param moTYpe
     * @param attributeName
     * @param attributeValue
     */
    public void updateManagedObjectAttribute(final String nodeName, final String moType, final String attributeName, final Object attributeValue) {
        try {
            retryManager.executeCommand(dpsRetryPolicies.getDpsMoActionRetryPolicy(), new RetriableCommand<Void>() {
                @Override
                public Void execute(final RetryContext retryContext) {
                    final ManagedObject mo = managedObjectUtil.getManagedObject(nodeName, moType);
                    mo.setAttribute(attributeName, attributeValue);
                    return null;
                }
            });
        } catch (RetriableCommandException retriableCommandException) {
            logger.error("Failed to set attribute {} on node {} for moType{}: {}", attributeName, nodeName, moType, retriableCommandException);
        } catch (Exception exception) {
            logger.warn("Failed to set attribute {} on node {} for moType{}: {}", attributeName, nodeName, moType, exception);
        }
    }

    private void subscribeToMoNotifications(final BackupActivityProperties backupActivityProperties, final String moFdn) {
        final long activityJobId = backupActivityProperties.getActivityJobId();
        final JobActivityInfo activityInfo = activityUtils.getActivityInfo(activityJobId, backupActivityProperties.getActivityServiceClass());
        activityUtils.subscribeToMoNotifications(moFdn, activityJobId, activityInfo);
    }

    /**
     * Subscribes to notifications on the xfConfigLoadObjects MO. The attributes that are watched for are defined in
     * mini-link-indoor-node-model-common.
     *
     * @param backupActivityProperties
     */
    public void subscribeToLoadObjectsNotifications(final BackupActivityProperties backupActivityProperties) {
        final String moFdn = managedObjectUtil.getManagedObject(backupActivityProperties.getNodeName(), XF_CONFIG_LOAD_OBJECTS).getFdn();
        subscribeToMoNotifications(backupActivityProperties, moFdn);
    }

    private void unsubscribeFromMoNotifications(final BackupActivityProperties backupActivityProperties, final String moFdn) {
        final long activityJobId = backupActivityProperties.getActivityJobId();
        final JobActivityInfo activityInfo = activityUtils.getActivityInfo(activityJobId, backupActivityProperties.getActivityServiceClass());
        activityUtils.unSubscribeToMoNotifications(moFdn, activityJobId, activityInfo);
    }

    /**
     * Unsubscribes from the xfConfigLoadObjects MO attribute change notifications. Necessary after the job is finished.
     *
     * @param backupActivityProperties
     */
    public void unsubscribeFromLoadObjectsNotifications(final BackupActivityProperties backupActivityProperties) {
        final String moFdn = managedObjectUtil.getManagedObject(backupActivityProperties.getNodeName(), XF_CONFIG_LOAD_OBJECTS).getFdn();
        unsubscribeFromMoNotifications(backupActivityProperties, moFdn);
    }

    /**
     * Reads the xfConfigStatus attribute's value from DPS.
     *
     * @param backupActivityProperties
     * @return
     */
    public String getConfigStatus(final BackupActivityProperties backupActivityProperties) {
        final ManagedObject mo = managedObjectUtil.getManagedObject(backupActivityProperties.getNodeName(), XF_CONFIG_LOAD_OBJECTS);
        return mo.getAttribute(XF_CONFIG_STATUS);
    }

    public String getsyncStatus(final BackupActivityProperties backupActivityProperties) {
        final ManagedObject mo = getCmFunctionMo(backupActivityProperties);
        return mo.getAttribute(SYNC_STATUS);
    }

    /**
     * Sets the given attribute of the given MO type without mediation.
     *
     * @param nodeName
     * @param moType
     * @param attrName
     * @param attrValue
     */
    public void setMoAttributeWithoutMediation(final String nodeName, final String moType, final String attrName, final String attrValue) {
        final DataBucket liveBucket = dataPersistenceService.getDataBucket(LIVE_BUCKET, SUPPRESS_MEDIATION, SUPPRESS_CONSTRAINTS);
        final String namespace = managedObjectUtil.getNamespace(nodeName, moType);
        final String meContextFdn = fdnUtils.getMeContextFdnFromNodeName(nodeName);
        final Query<TypeContainmentRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(namespace, moType,
                meContextFdn);
        final ManagedObject xfConfigLoadObjectsMo = (ManagedObject) liveBucket.getQueryExecutor().execute(query).next();
        xfConfigLoadObjectsMo.setAttribute(attrName, attrValue);
    }

    /**
     * Sets the xfConfigStatus attribute without mediating it. This attribute is modeled as read-only and cannot be set otherwise.
     *
     * @param nodeName
     * @param value
     */
    public void setXfConfigStatusWithoutMediation(final String nodeName, final String value) {
        setMoAttributeWithoutMediation(nodeName, XF_CONFIG_LOAD_OBJECTS, XF_CONFIG_STATUS, value);
    }

    /**
     * Sets the xfLicenseInstallOperStatus attribute without mediating it. This attribute is modeled as read-only and cannot be set otherwise.
     *
     * @param nodeName
     * @param value
     */
    public void setXfLicenseInstallOperStatusWithoutMediation(final String nodeName, final String value) {
        setMoAttributeWithoutMediation(nodeName, XF_LICENSE_INSTALL_OBJECTS, XF_LICENSE_INSTALL_OPER_STATUS, value);
    }

    /**
     * Sets the xfLicenseInstallFileName attribute without mediating it. This attribute is modeled as read-only and cannot be set otherwise.
     *
     * @param nodeName
     * @param value
     */
    public void setXfLicenseInstallFileNameWithoutMediation(final String nodeName, final String value) {
        setMoAttributeWithoutMediation(nodeName, XF_LICENSE_INSTALL_OBJECTS, XF_LICENSE_INSTALL_FILE_NAME, value);
    }

    private ManagedObject getCmFunctionMo(final BackupActivityProperties backupActivityProperties) {
        return managedObjectUtil.getManagedObjectUnderNetworkElement(backupActivityProperties.getNodeName(), CM_FUNCTION, OSS_NE_CM_DEF);
    }

    public List<ManagedObject> getMoByType(final String nodeName, final String moType, final String neFdn) {
        logger.debug("meFdn: {} :", neFdn);
        final List<ManagedObject> hwMos = dataPersistenceService
                .getLiveBucket()
                .getQueryExecutor()
                .getResultList(
                        dataPersistenceService.getQueryBuilder().createTypeQuery(managedObjectUtil.getNamespace(nodeName, moType), moType, neFdn));
        logger.debug("{} type retrieved : and its size : {} :", moType, hwMos.size());
        return hwMos;
    }

    public Object getMoAttribute(final String moFdn, final String moAttributeName) {
        checkParameters(moFdn, moAttributeName);

        final DataBucket liveBucket = dataPersistenceService.getDataBucket(DPS_BUCKET, SUPPRESS_MEDIATION, SUPPRESS_CONSTRAINTS);
        final ManagedObject managedObject = liveBucket.findMoByFdn(moFdn);
        final Object moAttributeValue = managedObject.getAttribute(moAttributeName);
        return moAttributeValue;
    }

    private void checkParameters(final String moFdn, final String moAttributeName) {
        if (moFdn == null) {
            throw new IllegalArgumentException("The FDN passed cannot be null.");
        }
        if (moAttributeName == null) {
            throw new IllegalArgumentException("The moAttributeName passed cannot be null.");
        }
    }

    public String getOssModelIdentity(final String nodeName) {
        final String networkElementFdn = NETWORKELEMENT + nodeName;
        final ManagedObject nodeMo = dataPersistenceService.getLiveBucket().findMoByFdn(networkElementFdn);
        return nodeMo.getAttribute(OSSMODELIDENTITY);
    }
}
