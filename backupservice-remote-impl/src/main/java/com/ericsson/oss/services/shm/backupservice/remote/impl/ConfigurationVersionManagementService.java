package com.ericsson.oss.services.shm.backupservice.remote.impl;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.security.accesscontrol.EAccessControl;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.ESecuritySubject;
import com.ericsson.oss.services.shm.backupservice.remote.api.CVOperationRemoteException;
import com.ericsson.oss.services.shm.backupservice.remote.api.ConfigurationVersionManagementServiceRemote;
import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.ResourceOperations;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.smrs.SmrsAccountInfo;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.common.smrs.SmrsServiceConstants;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CommonRemoteCvManagementService;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMO;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.UpgradePackageMO;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ConfigurationVersionManagementService implements ConfigurationVersionManagementServiceRemote {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationVersionManagementService.class);

    public static final String cvDefaultType = "OTHER";
    private static final String SLASH = "/";

    @Inject
    CVManagementServiceFactory cvManagementServiceFactory;

    @Inject
    SmrsFileStoreService smrsServiceUtil;

    @Inject
    ActivityUtils activityUtils;

    @Inject
    ResourceOperations resourceOperations;

    @Inject
    FdnServiceBean fdnServiceBean;

    @Inject
    EAccessControl accessControl;

    @Override
    public boolean createCV(final String nodeName, String cvName, final String identity, final String comment) throws CVOperationRemoteException {

        try {
            final CommonRemoteCvManagementService commonRemoteCvManagementService = cvManagementServiceFactory.getCvManagementService(nodeName, BackupActivityConstants.ACTION_CREATE_CV);
            if (nodeName == null && identity == null && comment == null) {
                LOGGER.error("Mandatory parameters(cvName/identity/comment) cannot be null");
                return false;
            }
            final ConfigurationVersionMO cvMo = commonRemoteCvManagementService.getCVMo(nodeName);

            if (cvName == null) {
                cvName = createCofigurationVersionName(commonRemoteCvManagementService, nodeName, cvMo);
            }
            if (cvMo == null) {
                LOGGER.error("CV MO does not exist for the supplied node name:{}", nodeName);
                throw new CVOperationRemoteException("CV MO does not exist for the supplied node name:" + nodeName);
            } else {
                LOGGER.info("CV MO is retrieved successfully for the supplied node name:{}", nodeName);

                final String operatorName = getOperatorName();

                //Added recordEvent
                activityUtils.recordEvent(SHMEvents.CREATE_BACKUP_SERVICE_REQUEST_ACTION, nodeName, cvMo.getFdn(), "SHM:" + nodeName + ":Proceeding creation of CV by service request with cvName: "
                        + cvName + " identity: " + identity + " comment: " + comment + " operatorName: " + operatorName);

                final Map<String, Object> actionParameters = new HashMap<String, Object>();
                actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME, cvName);
                actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_IDENTITY, identity);
                actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_TYPE, cvDefaultType);
                actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_OPERATOR_NAME, operatorName);
                actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_COMMENT, comment);
                if (commonRemoteCvManagementService.precheckOnMo(cvMo.getAllAttributes(), actionParameters)) {
                    try {
                        commonRemoteCvManagementService.executeAction(cvMo.getFdn(), nodeName, actionParameters);
                        return true;
                    } catch (final Exception e) {
                        throw new CVOperationRemoteException(e.getMessage());
                    }
                } else {
                    LOGGER.error("Create CV MO action has failed on Node:{}", nodeName);
                    return false;
                }
            }
        } catch (final Exception e) {
            throw new CVOperationRemoteException(e.getMessage());
        }

    }

    private String getOperatorName() {
        String operatorName = "";
        final ESecuritySubject securitySubject = accessControl.getAuthUserSubject();
        if (securitySubject != null) {
            operatorName = securitySubject.getSubjectId();
        } else {
            LOGGER.error("ESecuritySubject found to be null. Hence passing operator name as empty.");
        }
        return operatorName;
    }

    @Override
    public boolean uploadCV(final String nodeName, final String cvName) throws CVOperationRemoteException {

        try {
            final CommonRemoteCvManagementService commonCvManagementService = cvManagementServiceFactory.getCvManagementService(nodeName, BackupActivityConstants.ACTION_UPLOAD_CV);
            if (cvName == null && nodeName == null) {
                LOGGER.error("Mandatory parameters(cvName/nodeName) cannot be null");
                return false;
            }
            final List<NetworkElement> networkElementList = fdnServiceBean.getNetworkElementsByNeNames(Arrays.asList(nodeName));
            final String neType = networkElementList.get(0).getNeType();
            final ConfigurationVersionMO cvMo = commonCvManagementService.getCVMo(nodeName);
            if (cvMo == null) {
                LOGGER.error("CV MO does not exist for the supplied node name:{}", nodeName);
                throw new CVOperationRemoteException("CV MO does not exist for the supplied node name:" + nodeName);
            } else {
                activityUtils.recordEvent(SHMEvents.UPLOAD_BACKUP_SERVICE_REQUEST_ACTION, nodeName, cvMo.getFdn(), "SHM:" + nodeName + ":Proceeding with upload of CV by service request");
                LOGGER.info("CV MO is retrieved successfully for the supplied node name:{}", nodeName);
                SmrsAccountInfo smrsDetails = new SmrsAccountInfo();
               try {
                    smrsDetails = smrsServiceUtil.getSmrsDetails(SmrsServiceConstants.BACKUP_ACCOUNT, neType, nodeName);
                } catch (final Exception e) {
                    LOGGER.error("Unable to Upload CV {} for node {} due to: ", cvName,nodeName,e);
                    return false;
                }
                final String ftpServerIpAddress = smrsDetails.getServerIpAddress();
                final String ftpServerUserId = smrsDetails.getUser();
                final String ftpServerPassword = new String(smrsDetails.getPassword());
                String relativePath = smrsDetails.getRelativePathToSmrsRoot();
                if (!relativePath.endsWith(SLASH)) {
                    relativePath = relativePath + SLASH;
                }
                final String pathForNode = relativePath + nodeName;
                final String pathOnFtpServer = smrsDetails.getPathOnServer();
                resourceOperations.createDirectory(pathOnFtpServer, nodeName);
                final boolean isDirectoryExist = resourceOperations.isDirectoryExistsWithWritePermissions(pathOnFtpServer, nodeName);
                if (!isDirectoryExist) {
                    LOGGER.error("Directory :{}: doesn't exist so cannot Upload Configuration Version", pathForNode);
                    return false;
                }
                final Map<String, Object> actionParameters = new HashMap<String, Object>();
                actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_CV_NAME, cvName);
                actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_PATH_ON_FTP_SERVER, pathForNode);
                actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_CV_BACKUP_NAME_ON_FTP_SERVER, cvName);
                actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_IP_ADDRESS, ftpServerIpAddress);
                actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_USER_ID, ftpServerUserId);
                actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_PASSWORD, ftpServerPassword);
                if (commonCvManagementService.precheckOnMo(cvMo.getAllAttributes(), actionParameters)) {
                    final int actionStatusValue = commonCvManagementService.executeAction(cvMo.getFdn(), nodeName, actionParameters);
                    LOGGER.debug("actionStatusValue in uploadCV {}", actionStatusValue);
                    return actionStatusValue == 1;

                } else {
                    LOGGER.error("Failed to  upload CV {} from  Node:{}", cvName, nodeName);
                    return false;
                }
            }
        } catch (final Exception e) {
            throw new CVOperationRemoteException(e.getMessage());
        }
    }

    @Override
    public boolean setStartableCV(final String nodeName, final String cvName) throws CVOperationRemoteException {
        try {
            final CommonRemoteCvManagementService commonCvManagementService = cvManagementServiceFactory.getCvManagementService(nodeName, BackupActivityConstants.ACTION_SET_STARTABLE_CV);
            if (cvName == null && nodeName == null) {
                LOGGER.error("Mandatory parameters(cvName/nodeName) cannot be null");
                return false;
            }
            final ConfigurationVersionMO cvMo = commonCvManagementService.getCVMo(nodeName);
            if (cvMo == null) {
                LOGGER.error("CV MO does not exist for the supplied node name:{}", nodeName);
                throw new CVOperationRemoteException("CV MO does not exist for the supplied node name:" + nodeName);
            } else {
                //Added recordEvent
                activityUtils.recordEvent(SHMEvents.SETSTARTABLE_BACKUP_SERVICE_REQUEST_ACTION, nodeName, cvMo.getFdn(), "SHM:" + nodeName + ":Proceeding with setStartable of CV by service request");
                LOGGER.info("CV MO is retrieved successfully for the supplied node name:{}", nodeName);
                final Map<String, Object> actionParameters = new HashMap<String, Object>();
                actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME, cvName);
                commonCvManagementService.executeAction(cvMo.getFdn(), nodeName, actionParameters);
                return true;

            }
        } catch (final Exception e) {
            throw new CVOperationRemoteException(e.getMessage());
        }
    }

    @Override
    public boolean setCVFirstInRollBackList(final String neName, final String cvName) throws CVOperationRemoteException {
        try {
            final CommonRemoteCvManagementService commonCvManagementService = cvManagementServiceFactory.getCvManagementService(neName, BackupActivityConstants.ACTION_SET_FIRST_IN_ROLLBACK_CV);
            if (cvName == null && neName == null) {
                LOGGER.error("Mandatory parameters(cvName/nodeName) cannot be null");
                return false;
            }
            final ConfigurationVersionMO cvMo = commonCvManagementService.getCVMo(neName);
            if (cvMo == null) {
                LOGGER.error("CV MO does not exist for the supplied node name:{}", neName);
                throw new CVOperationRemoteException("CV MO does not exist for the supplied node name:" + neName);
            } else {
                //Added recordEvent
                activityUtils.recordEvent(SHMEvents.SETSTARTABLE_BACKUP_SERVICE_REQUEST_ACTION, neName, cvMo.getFdn(), "SHM:" + neName
                        + ":Proceeding with setCVFirstInRollBackList of CV by service request");
                LOGGER.info("CV MO is retrieved successfully for the supplied node name:{}", neName);
                final Map<String, Object> actionParameters = new HashMap<String, Object>();
                actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME, cvName);
                commonCvManagementService.executeAction(cvMo.getFdn(), neName, actionParameters);
                return true;

            }
        } catch (final Exception e) {
            throw new CVOperationRemoteException(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String createCofigurationVersionName(final CommonRemoteCvManagementService commonRemoteCvManagementService, final String nodeName, final ConfigurationVersionMO cvMo) {
        String prodNum_prodRev;
        final String currentUpgradePackage = (String) cvMo.getAllAttributes().get(ConfigurationVersionMoConstants.CURRENT_UPGRADE_PACKAGE);

        if (currentUpgradePackage == null || currentUpgradePackage.isEmpty()) {
            prodNum_prodRev = BackupActivityConstants.DEFAULT_PROD_ID;
        } else {
            final String searchValue = currentUpgradePackage.split(ActivityConstants.EQUAL)[3];
            final UpgradePackageMO upMo = commonRemoteCvManagementService.getUPMo(nodeName, searchValue);
            if (upMo == null) {
                prodNum_prodRev = BackupActivityConstants.DEFAULT_PROD_ID;
            } else {
                final Map<String, String> adminData = (Map<String, String>) cvMo.getAllAttributes().get(UpgradeActivityConstants.ADMINISTRATIVE_DATA);
                final String productNumber = adminData.get(UpgradeActivityConstants.PRODUCT_NUMBER);
                final String productRevision = adminData.get(UpgradeActivityConstants.PRODUCT_REVISION);
                prodNum_prodRev = productNumber + ActivityConstants.UNDERSCORE + productRevision;
                prodNum_prodRev = prodNum_prodRev.replace(ActivityConstants.SLASH, ActivityConstants.PERCENTAGE);
                if (prodNum_prodRev.length() > BackupActivityConstants.PROD_ID_LIMIT) {
                    prodNum_prodRev = prodNum_prodRev.substring(0, BackupActivityConstants.PROD_ID_LIMIT);
                }
            }

        }

        final Date dateTime = new Date();
        final SimpleDateFormat formatter = new SimpleDateFormat(BackupActivityConstants.DATE_FORMAT);
        final String dateValue = formatter.format(dateTime);

        final String cvName = BackupActivityConstants.CV_NAME_PREFIX + prodNum_prodRev + ActivityConstants.UNDERSCORE + dateValue;
        return cvName;
    }
}
