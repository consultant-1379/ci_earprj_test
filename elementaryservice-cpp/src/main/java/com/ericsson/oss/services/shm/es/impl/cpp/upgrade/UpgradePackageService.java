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
package com.ericsson.oss.services.shm.es.impl.cpp.upgrade;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.NodeModelNameSpaceProviderRetryProxy;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.common.exception.NodeAttributesReaderException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.job.utils.NodeAttributesReaderRetryProxy;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.smrs.SmrsAccountInfo;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.common.NodeReadAttributeFailedException;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.common.UpgradePackageMO;
import com.ericsson.oss.services.shm.filestore.swpackage.remote.api.RemoteSoftwarePackageService;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.SmrsServiceConstants;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

/**
 * This EJB class facilitates all Upgrade activities to communicate with DPS for UP MO and UP PO.
 * 
 * @author tcsrohc
 * 
 */
@Stateless
@Traceable
@Profiled
public class UpgradePackageService {

    private static final Logger logger = LoggerFactory.getLogger(UpgradePackageService.class);

    @Inject
    private DpsReader dpsReader;

    @Inject
    private DpsWriter dpsWriter;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private SmrsFileStoreService smrsServiceUtil;

    @EServiceRef
    private RemoteSoftwarePackageService remoteSoftwarePackageService;

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    @Inject
    private NodeModelNameSpaceProviderRetryProxy nodeModelNameSpaceProviderRetryProxy;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private NodeAttributesReaderRetryProxy nodeAttributesReaderRetryProxy;

    private static final String SMO_UPGRADE_CONTROL_FILE = "SMO_UPGRADE_CONTROL_FILE";
    private static final String UP_MO_FDN = "upMoFdn";
    private static final String UP_MO_ATTRIBUTES = "upMoAttributes";

    private static final String[] UP_MO_ADMINISTRATIVE_DATA = { UpgradePackageMoConstants.UP_MO_ADMINISTRATIVE_DATA };
    private static final String[] empty_array = {};

    /**
     * This private method retrieves Upgrade Package MOFDN and attributes for the corresponding activity.
     * 
     * @param activityJobId
     * @param productNumber
     * @param productRevision
     * @param attributeNames
     * @return
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getUpMoFdnAndMOAttributes(final long activityJobId, String productNumber, String productRevision, final String[] attributeNames) {
        if (productNumber == null || productRevision == null) {
            final Map<String, String> swPkgandUcfMap = getSwPkgNameandUcfName(activityJobId);
            final String swPkgName = swPkgandUcfMap.get(UpgradeActivityConstants.SWP_NAME);
            final String ucfName = swPkgandUcfMap.get(UpgradeActivityConstants.UCF);
            logger.debug("swPkgName {} and ucfName {} for activityJobId is {}", swPkgName, ucfName, activityJobId);
            // Getting UCF File
            final String ucfFile = getUcfFile(swPkgName, ucfName);
            logger.debug("UcfFile for activity {} is {}", activityJobId, ucfFile);

            Map<String, String> productNumberAndRevision = new HashMap<String, String>();
            try {
                productNumberAndRevision = getProductNumberAndRevision(ucfFile);
            } catch (final IllegalStateException illegalStateException) {
                logger.warn("UCF File doesn't exist for {}", ucfFile);
            }
            if (productNumberAndRevision.isEmpty()) {
                return null;
            }
            productNumber = productNumberAndRevision.get(UpgradePackageMoConstants.UP_MO_ADMIN_DATA_PRODUCT_NUMBER);
            productRevision = productNumberAndRevision.get(UpgradePackageMoConstants.UP_MO_ADMIN_DATA_PRODUCT_REVISION);
        }
        final Map<String, Object> neJobAttributesMap = activityUtils.getNeJobAttributes(activityJobId);
        final String neName = (String) neJobAttributesMap.get(ShmConstants.NE_NAME);
        logger.debug("Node Name for activity {} is {} ", activityJobId, neName);

        //Query All Upgrade Package MOs and then filter out based on product number and revision.
        logger.debug("productNumber from productId= {}", productNumber);

        final String namespace = nodeModelNameSpaceProviderRetryProxy.getNamespaceByNodeName(neName);
        logger.debug("Namespace for the neName {} is {}", neName, namespace);
        if (ShmCommonConstants.NAMESPACE_NOT_FOUND.equals(namespace)) {
            return null;
        }
        final Map<String, Object> restrictions = new HashMap<String, Object>();
        final List<ManagedObject> managedObjectList = dpsReader.getManagedObjects(namespace, UpgradeActivityConstants.UP_MO_TYPE, restrictions, neName);
        if (!managedObjectList.isEmpty()) {
            final String[] attributesToReadFromNode = appendUpMoAttributes(attributeNames, UP_MO_ADMINISTRATIVE_DATA);
            for (final ManagedObject managedObjectItem : managedObjectList) {
                final Map<String, Object> moAttributes = readAttributesWithRetires(managedObjectItem, attributesToReadFromNode);
                final Map<String, Object> administrativeData = (Map<String, Object>) moAttributes.get(UpgradePackageMoConstants.UP_MO_ADMINISTRATIVE_DATA);
                if (administrativeData != null && !administrativeData.isEmpty()) {
                    if (productNumber.equals(administrativeData.get(UpgradePackageMoConstants.UP_MO_ADMIN_DATA_PRODUCT_NUMBER))
                            && productRevision.equals(administrativeData.get(UpgradePackageMoConstants.UP_MO_ADMIN_DATA_PRODUCT_REVISION))) {
                        logger.debug("Matching UP MO found with productNumber= {}, and productRevision={}", productNumber, productRevision);
                        final Map<String, Object> upMoAndAttributes = new HashMap<>();
                        upMoAndAttributes.put(UP_MO_FDN, managedObjectItem.getFdn());
                        upMoAndAttributes.put(UP_MO_ATTRIBUTES, moAttributes);
                        return upMoAndAttributes;
                    }
                }
            }
        }
        return null;

    }

    private static String[] appendUpMoAttributes(final String[] attributeNames, final String[] attributeNamesToAppend) {
        final List<String> attributes = new ArrayList<>();
        attributes.addAll(Arrays.asList(attributeNames));
        for (final String attributeToAppend : Arrays.asList(attributeNamesToAppend)) {
            if (!attributes.contains(attributeToAppend)) {
                attributes.add(attributeToAppend);
            }
        }
        return attributes.toArray(new String[attributes.size()]);
    }

    private Map<String, Object> readAttributesWithRetires(final ManagedObject managedObject, final String[] attributeNames) {
        Map<String, Object> moAttributes = new HashMap<>();
        try {
            moAttributes = nodeAttributesReaderRetryProxy.readAttributesWithRetry(managedObject, attributeNames);
        } catch (final NodeAttributesReaderException nodeAttributesReaderException) {
            logger.error("Node read attributes with retries has failed for the MO {}. Reason : {}", managedObject.getFdn(), nodeAttributesReaderException);
            throw new NodeReadAttributeFailedException(nodeAttributesReaderException);
        }
        return moAttributes;
    }

    @SuppressWarnings("unchecked")
    public UpgradePackageMO getUPMOAttributes(final long activityJobId, final String productNumber, final String productRevision) {
        final String[] attributeNames = { UpgradeActivityConstants.ADMINISTRATIVE_DATA, UpgradeActivityConstants.DELETE_PREVENTING_CVS, UpgradeActivityConstants.DELETE_PREVENTING_UPS };
        final Map<String, Object> upMoFdnAndMOAttributes = getUpMoFdnAndMOAttributes(activityJobId, productNumber, productRevision, attributeNames);
        if (upMoFdnAndMOAttributes == null) {
            return null;
        }
        final String upMoFdn = (String) upMoFdnAndMOAttributes.get(UP_MO_FDN);
        final Map<String, Object> upMoAttributes = (Map<String, Object>) upMoFdnAndMOAttributes.get(UP_MO_ATTRIBUTES);
        return new UpgradePackageMO(upMoFdn, upMoAttributes);
    }

    /**
     * This method creates Upgrade Package MO with the required attributes fetched from SMRS.
     * 
     * @param activityJobId
     * @return upMoFdn
     */
    public String createUpgradeMO(final long activityJobId) {
        return createUpgradeMO(activityJobId, null, null);
    }

    /**
     * This method creates Upgrade Package MO with the required attributes fetched from SMRS.
     * 
     * @param activityJobId
     * @param productId
     * @return upMoFdn
     */
    public String createUpgradeMO(final long activityJobId, String productNumber, String productRevision) {

        // Getting nodename
        final String nodeName = activityUtils.getNodeName(activityJobId);
        String neType = null;
        // Getting parentMO
        final Map<String, Object> restrictions = new HashMap<String, Object>();
        final String namespace = nodeModelNameSpaceProviderRetryProxy.getNamespaceByNodeName(nodeName);
        logger.debug("Namespace for the neName {} is {}", nodeName, namespace);
        if (ShmCommonConstants.NAMESPACE_NOT_FOUND.equals(namespace)) {
            logger.error("Namespace not found for node::{}", nodeName);
            return null;
        }
        final List<ManagedObject> managedObjectList = dpsReader.getManagedObjects(namespace, ShmConstants.UP_PARENT_MO_TYPE, restrictions, nodeName);
        final ManagedObject upgradePackageParentMo = managedObjectList.get(0);
        final String parentFdn = upgradePackageParentMo.getFdn();
        logger.debug("ParentFdn for activity {} : {} ", activityJobId, parentFdn);
        final String upModelVersion = upgradePackageParentMo.getVersion();
        logger.debug("UpModelVersion for activity {} : {} ", activityJobId, upModelVersion);

        String upFilePathOnFtpServer = "";
        if (productNumber == null || productRevision == null) {
            final Map<String, String> swPkgandUcfMap = getSwPkgNameandUcfName(activityJobId);
            final String swPkgName = swPkgandUcfMap.get(UpgradeActivityConstants.SWP_NAME);
            final String ucfName = swPkgandUcfMap.get(UpgradeActivityConstants.UCF);
            logger.debug("swPkgName {} and ucfName {} in createUpgradeMO() for activityJobId is {}", swPkgName, ucfName, activityJobId);

            // Getting upFilePathOnFtpServer
            upFilePathOnFtpServer = getUcfFile(swPkgName, ucfName);
            logger.debug("UpFilePathOnFtpServer for activity {} is {}", activityJobId, upFilePathOnFtpServer);

            // Getting ProductId
            final Map<String, String> productNumberAndRevision = getProductNumberAndRevision(upFilePathOnFtpServer);

            productNumber = productNumberAndRevision.get(UpgradePackageMoConstants.UP_MO_ADMIN_DATA_PRODUCT_NUMBER);
            productRevision = productNumberAndRevision.get(UpgradePackageMoConstants.UP_MO_ADMIN_DATA_PRODUCT_REVISION);
        } else {
            //Retrieve the UCF file path from SMRS where it matches with productID
            upFilePathOnFtpServer = readUCFItemsFromDB(productNumber, productRevision);
        }
        // Getting UserLabel
        final String userLabel = UpgradeActivityConstants.USERLABEL_PREFIX + System.currentTimeMillis();

        try {
            final List<NetworkElement> networkElementsList = fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(Arrays.asList(nodeName));
            if (!networkElementsList.isEmpty()) {
                neType = networkElementsList.get(0).getNeType();
            }
        } catch (final RetriableCommandException | IllegalArgumentException e) {
            logger.error("Exception while fetching neType of node :  {}", nodeName);
        }
        // Getting SMRS Details
        SmrsAccountInfo smrsDetails = new SmrsAccountInfo();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        try {
            smrsDetails = smrsServiceUtil.getSmrsDetails(SmrsServiceConstants.SOFTWARE_ACCOUNT, neType, nodeName);
        } catch (final Exception e) {
            logger.error("Unable to perform verify activity for node {} due to", nodeName, e);
            activityUtils.prepareJobLogAtrributesList(jobLogList, "Unable to perform verify activity due to : " + e.getMessage(), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList);
            return null;
        }
        final String ftpServerIpAddress = smrsDetails.getServerIpAddress();
        final String user = smrsDetails.getUser();
        final String password = new String(smrsDetails.getPassword());

        logger.debug("The FTPserverIpAddress is: {}, user name is:{}", ftpServerIpAddress, user);
        //UCF File obtained from SoftwarePackageService is absolute.
        //Below Code written is to get the relative path.
        String relativePath = "";
        if (upFilePathOnFtpServer.contains(smrsDetails.getSmrsRootDirectory())) {
            relativePath = upFilePathOnFtpServer.replace(smrsDetails.getSmrsRootDirectory(), "");
        } else {
            logger.error("There is no SMRS root path starting with /home/smrs and the obtained path is:{}", upFilePathOnFtpServer);
        }

        final Map<String, Object> upgradePkgMOAttributes = new HashMap<String, Object>();
        upgradePkgMOAttributes.put(UpgradePackageMoConstants.UP_MO_USER_LABEL, userLabel);
        upgradePkgMOAttributes.put(UpgradePackageMoConstants.UP_MO_UP_FILEPATH_ON_FTP_SERVER, relativePath);
        upgradePkgMOAttributes.put(UpgradePackageMoConstants.UP_MO_FTP_SERVER_IP_ADDRESS, ftpServerIpAddress);
        upgradePkgMOAttributes.put(UpgradePackageMoConstants.UP_MO_USER, user);
        upgradePkgMOAttributes.put(UpgradePackageMoConstants.UP_MO_PASSWORD, password);
        upgradePkgMOAttributes.put(UpgradePackageMoConstants.UP_MO_UPGRADE_PACKAGE_ID, productNumber + "_" + productRevision);
        final ManagedObject upMo = dpsWriter.createManagedObject(ShmConstants.UP_MO_TYPE, parentFdn, namespace, upModelVersion, upgradePkgMOAttributes);
        final String upMoFdn = upMo.getFdn();
        return upMoFdn;
    }

    public String readUCFItemsFromDB(final String productNumber, final String productRevision) {
        String upFilePathOnFtpServer = "";
        final PersistenceObject upgradePackagePO = getUpPoFromProductNumberAndRevision(productNumber, productRevision);
        if (upgradePackagePO != null) {
            final String packageName = upgradePackagePO.getAttribute(UpgradeActivityConstants.UP_PO_PACKAGE_NAME);
            if (upgradePackagePO.getAttribute(UpgradeActivityConstants.UP_PO_JOBPARAMS) != null) {
                final List<Map<String, Object>> jobParameters = upgradePackagePO.getAttribute(UpgradeActivityConstants.UP_PO_JOBPARAMS);
                logger.trace("JobParameters under UP PO for productNumber {} and productRevision {} is {}", productNumber, productRevision, jobParameters);
                if (jobParameters != null && !jobParameters.isEmpty()) {
                    for (final Map<String, Object> eachJobParam : jobParameters) {
                        if (SMO_UPGRADE_CONTROL_FILE.equalsIgnoreCase((String) eachJobParam.get(UpgradeActivityConstants.UP_PO_PARAM_NAME))) {
                            final String upgradeControlFileName = (String) eachJobParam.get(UpgradeActivityConstants.UP_PO_VALUE);
                            upFilePathOnFtpServer = getUcfFile(packageName, upgradeControlFileName);
                        }
                    }
                }
            }
        } else {
            final String exceptionMessage = "Respective Upgrade Package does not exist for productNumber %s and productRevision %s";
            throw new IllegalArgumentException(String.format(exceptionMessage, productNumber, productRevision));
        }

        return upFilePathOnFtpServer;
    }

    /**
     * This private method retrieves Upgrade Package PO for the corresponding software package.
     * 
     * @param swPkgName
     * @return
     */
    private PersistenceObject getUpPoFromProductNumberAndRevision(final String productNumber, final String productRevision) {

        final Map<String, Object> restrictions = new HashMap<String, Object>();
        final List<PersistenceObject> upPoList = dpsReader.findPOs(UpgradeActivityConstants.UPPKG_NAMESPACE, UpgradeActivityConstants.UPPKG_TYPE_CPP, restrictions);

        if (upPoList.isEmpty()) {
            logger.info("Unable to find PO with namespace {} and type {}", UpgradeActivityConstants.UPPKG_NAMESPACE, UpgradeActivityConstants.UPPKG_TYPE_CPP);

        } else {
            return requiredUpPoBasedOnProductNumberAndRevision(productNumber, productRevision, upPoList);

        }
        return null;

    }

    /**
     * @param productNumber
     * @param productRevision
     * @param upPoList
     * @return
     */
    private PersistenceObject requiredUpPoBasedOnProductNumberAndRevision(final String productNumber, final String productRevision, final List<PersistenceObject> upPoList) {
        for (final PersistenceObject eachUpPo : upPoList) {
            final String packageName = eachUpPo.getAttribute(UpgradeActivityConstants.UP_PO_PACKAGE_NAME);
            if (eachUpPo.getAttribute(UpgradeActivityConstants.UP_PO_JOBPARAMS) != null) {
                final List<Map<String, Object>> jobParameters = eachUpPo.getAttribute(UpgradeActivityConstants.UP_PO_JOBPARAMS);
                logger.trace("JobParameters under each UP PO are {}", jobParameters);
                if (jobParameters != null && !jobParameters.isEmpty()) {
                    for (final Map<String, Object> eachJobParam : jobParameters) {
                        if (SMO_UPGRADE_CONTROL_FILE.equalsIgnoreCase((String) eachJobParam.get(UpgradeActivityConstants.UP_PO_PARAM_NAME))) {
                            final String upgradeControlFileName = (String) eachJobParam.get(UpgradeActivityConstants.UP_PO_VALUE);
                            logger.debug("upgradeControlFileName for each UP PO is {}", upgradeControlFileName);
                            Map<String, String> productNumberAndRevisionFromUcf = new HashMap<String, String>();
                            try {
                                final String ucfFileWithPath = getUcfFile(packageName, upgradeControlFileName);
                                productNumberAndRevisionFromUcf = getProductNumberAndRevision(ucfFileWithPath);
                            } catch (final IllegalArgumentException illegalArgumentException) {
                                logger.warn("UCF File doesn't exist for the given package {} with UCF File Name {}", packageName, upgradeControlFileName);
                            }
                            logger.debug("ProductNumber and ProductRevision form UCF File is {}", productNumberAndRevisionFromUcf);
                            if (productNumber.equalsIgnoreCase(productNumberAndRevisionFromUcf.get(UpgradePackageMoConstants.UP_MO_ADMIN_DATA_PRODUCT_NUMBER))
                                    && productRevision.equalsIgnoreCase(productNumberAndRevisionFromUcf.get(UpgradePackageMoConstants.UP_MO_ADMIN_DATA_PRODUCT_REVISION))) {
                                return eachUpPo;
                            }
                        }

                    }
                }
            }
        }
        return null;
    }

    /**
     * This private method retrieves UCF File for the corresponding software package and UCF Name.
     * 
     * @param activityJobId
     * @return ucfName
     */
    public String getUcfFile(final String swPkgName, final String ucfName) {
        logger.debug("For getting UCF File, SwPkgName = {}, and UCF Name = {}", swPkgName, ucfName);
        String ucfFileWithPath = null;
        final List<String> availableUcfFiles = remoteSoftwarePackageService.getSoftwarePackageDetails(swPkgName);
        logger.debug("Availabe Ucf Files for Software Pacakge {} are {}", swPkgName, availableUcfFiles);
        if (availableUcfFiles.size() == 0) {
            logger.error("No UCF File exists for the Software Package {}", swPkgName);
            return ucfFileWithPath;
        }
        for (final String eachUcfFile : availableUcfFiles) {
            if (eachUcfFile.contains(ucfName)) {
                final File ucfFile = new File(eachUcfFile);
                if (ucfFile.getName().equals(ucfName)) {
                    ucfFileWithPath = eachUcfFile;
                }
            }
        }

        logger.debug("ucfFileWithPath  for software package {} is {} ", swPkgName, ucfFileWithPath);
        return ucfFileWithPath;
    }

    /**
     * This method builds product Id based on product number and product revision fetched from UCF File.
     * 
     * @param ucfFile
     * @return
     */
    public Map<String, String> getProductNumberAndRevision(final String ucfFile) {
        final UpgradeControlFileParser ucfParser = new UpgradeControlFileParser();
        try {
            ucfParser.parse(ucfFile);
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
            throw new IllegalArgumentException("Parsing UCF has failed " + ucfFile + " , reason=" + e.getMessage());
        }

        String productNumber = ucfParser.getProductNumber();
        logger.debug("Product Number retrieved from UcfFile {} is {} ", ucfFile, productNumber);
        String productRevision = ucfParser.getProductRevision();
        logger.debug("Product Revision retrieved from UcfFile {} is {} ", ucfFile, productRevision);

        productNumber = productNumber.trim();
        logger.trace("Product Number after trimming is {} ", productNumber);
        productRevision = productRevision.trim();
        logger.trace("Product Revision after trimming is {} ", productRevision);

        final Map<String, String> productNumberAndRevision = new HashMap<String, String>();
        productNumberAndRevision.put(UpgradePackageMoConstants.UP_MO_ADMIN_DATA_PRODUCT_NUMBER, productNumber);
        productNumberAndRevision.put(UpgradePackageMoConstants.UP_MO_ADMIN_DATA_PRODUCT_REVISION, productRevision);
        return productNumberAndRevision;
    }

    /**
     * This method fetch UP MO attributes from UP MO.
     * 
     * @param activityJobId
     * @param attributeNames
     * @return
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUpMoData(final long activityJobId, final String[] attributeNames, final String productNumber, final String productRevision) {
        Map<String, Object> upMoAttributesData = new HashMap<>();
        final Map<String, Object> upMoFdnAndAttributes = getUpMoFdnAndMOAttributes(activityJobId, productNumber, productRevision, attributeNames);
        if (upMoFdnAndAttributes != null) {
            upMoAttributesData = (Map<String, Object>) upMoFdnAndAttributes.get(UP_MO_ATTRIBUTES);
        }
        return upMoAttributesData;
    }

    /**
     * This method fetch UP MO Fdn from UP MO.
     * 
     * @param activityJobId
     * @param productId
     * @param attributeNames
     * @return
     */
    public String getUpMoFdn(final long activityJobId, final String productNumber, final String productRevision) {
        final Map<String, Object> upMoFdnAndMOAttributes = getUpMoFdnAndMOAttributes(activityJobId, productNumber, productRevision, empty_array);
        if (upMoFdnAndMOAttributes != null) {
            return (String) upMoFdnAndMOAttributes.get(UP_MO_FDN);
        }
        return null;
    }

    /**
     * This method fetch UP PO attributes from UP PO.
     * 
     * @param activityJobId
     * @param attributeNames
     * @return
     */
    public Map<String, Object> getUpPoData(final long activityJobId) {

        Map<String, Object> upPoAttributes = new HashMap<String, Object>();

        final Map<String, String> swPkgandUcfMap = getSwPkgNameandUcfName(activityJobId);
        final String swPkgName = swPkgandUcfMap.get(UpgradeActivityConstants.SWP_NAME);
        logger.debug("swPkgName for activity {} is {}", activityJobId, swPkgName);
        final PersistenceObject upPo = getUpPoFromSwPkgName(swPkgName);
        if (upPo != null) {
            upPoAttributes = upPo.getAllAttributes();
        }

        return upPoAttributes;

    }

    /**
     * Retrieves the Upgrade Package PO details filtered by its productId
     * 
     * @param productId
     * @return - attributes
     */
    public Map<String, Object> getUpPoData(final String productNumber, final String productRevision) {
        Map<String, Object> upPoAttributes = new HashMap<String, Object>();
        final PersistenceObject upPo = getUpPoFromProductNumberAndRevision(productNumber, productRevision);
        if (upPo != null) {
            upPoAttributes = upPo.getAllAttributes();
        }

        return upPoAttributes;

    }

    public Map<String, Object> getUpMoAttributesByFdn(final String upMoFdn, final String[] attributeNames) {
        Map<String, Object> upMoAttributes = new HashMap<String, Object>();
        final ManagedObject upgradePackageMO = dpsReader.findMoByFdn(upMoFdn);
        if (upgradePackageMO != null) {
            upMoAttributes = readAttributesWithRetires(upgradePackageMO, attributeNames);
        }
        return upMoAttributes;
    }

    private PersistenceObject getUpPoFromSwPkgName(final String swPkgName) {

        final Map<String, Object> restrictions = new HashMap<String, Object>();
        restrictions.put(UpgradeActivityConstants.UP_PO_PACKAGE_NAME, swPkgName);
        final List<PersistenceObject> upPoList = dpsReader.findPOs(UpgradeActivityConstants.UPPKG_NAMESPACE, UpgradeActivityConstants.UPPKG_TYPE_CPP, restrictions);

        if (upPoList.isEmpty()) {
            logger.info("Unable to find PO with namespace {} , type {} and package name {}", UpgradeActivityConstants.UPPKG_NAMESPACE, UpgradeActivityConstants.UPPKG_TYPE_CPP,
                    UpgradeActivityConstants.UP_PO_PACKAGE_NAME);
            return null;
        } else {
            final PersistenceObject upPo = upPoList.get(0);
            return upPo;
        }

    }

    /**
     * This method will return the software package name and ucfName for the activity.
     * 
     * @param activityJobId
     * @return
     */
    public Map<String, String> getSwPkgNameandUcfName(final long activityJobId) {
        String neType = null;
        String platform = null;
        final Map<String, Object> jobConfigurationDetails = activityUtils.getJobConfigurationDetails(activityJobId);
        final String neName = activityUtils.getNodeName(activityJobId);
        final List<String> neFdns = new ArrayList<String>();
        neFdns.add(neName);
        try {
            final List<NetworkElement> networkElementsList = fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(neFdns);
            if (!networkElementsList.isEmpty()) {
                neType = networkElementsList.get(0).getNeType();
                platform = networkElementsList.get(0).getPlatformType().name();
            }
        } catch (final RetriableCommandException | IllegalArgumentException e) {
            logger.error("Exception while fetching neType of node :  {}", neFdns);
        }
        logger.debug("Nemane {},neType {}, platform {} Fetched for activityJobId {} are ", neName, neType, platform, activityJobId);
        final List<String> keyList = new ArrayList<String>();
        keyList.add(UpgradeActivityConstants.SWP_NAME);
        keyList.add(UpgradeActivityConstants.UCF);
        final Map<String, String> keyValueMap = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neName, neType, platform);
        logger.debug("Fetched SwPkgNameandUcfNameMap {} for activityJobId : {}", keyValueMap, activityJobId);
        return keyValueMap;
    }

    /**
     * Retrieves the administrativeData from the UpgradePackage MO by Notified FDN
     * 
     * @param message
     * @return Map - of administrativeData
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUpAdminData(final Notification message) {
        final NotificationSubject notificationSubject = message.getNotificationSubject();
        if (notificationSubject instanceof FdnNotificationSubject) {
            final String fdn = ((FdnNotificationSubject) notificationSubject).getFdn();
            final Map<String, Object> upMoAttributes = getUpMoAttributes(fdn);
            final Map<String, Object> adminData = (Map<String, Object>) upMoAttributes.get(UpgradePackageMoConstants.UP_MO_ADMINISTRATIVE_DATA);
            return adminData;
        }
        return new HashMap<String, Object>();

    }

    /**
     * @param fdn
     * @return
     */
    private Map<String, Object> getUpMoAttributes(final String upMoFdn) {
        Map<String, Object> upMoAttributes = new HashMap<String, Object>();
        final ManagedObject upgradePackageMO = dpsReader.findMoByFdn(upMoFdn);
        if (upgradePackageMO != null) {
            upMoAttributes = upgradePackageMO.getAllAttributes();
        }
        return upMoAttributes;
    }

    public UpgradePackageMO getUpMoByFdn(final String upMoFdn) {
        final ManagedObject upgradePackageMO = dpsReader.findMoByFdn(upMoFdn);
        final String[] ADMINISTRATIVE_DATA = { UpgradeActivityConstants.ADMINISTRATIVE_DATA };
        return new UpgradePackageMO(upgradePackageMO.getFdn(), readAttributesWithRetires(upgradePackageMO, ADMINISTRATIVE_DATA));
    }

    public List<UpgradePackageMO> getUpMos(final String nodeName) {
        final Map<String, Object> restrictions = new HashMap<>();
        final String namespace = nodeModelNameSpaceProviderRetryProxy.getNamespaceByNodeName(nodeName);
        final List<ManagedObject> upMos = dpsReader.getManagedObjects(namespace, UpgradeActivityConstants.UP_MO_TYPE, restrictions, nodeName);
        List<UpgradePackageMO> upgradePkgs = new ArrayList<>();
        for (ManagedObject upMo : upMos) {
            UpgradePackageMO packageMO = new UpgradePackageMO(upMo.getFdn(), upMo.getAllAttributes());
            upgradePkgs.add(packageMO);
        }
        return upgradePkgs;
    }

}
