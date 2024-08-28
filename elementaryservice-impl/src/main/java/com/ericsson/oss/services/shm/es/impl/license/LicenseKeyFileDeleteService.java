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

import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.FileResource;

/**
 * This class deletes the license key files with same finger print and lesser sequence number that are already installed.
 * 
 * @author xmanush
 */
@Stateless
public class LicenseKeyFileDeleteService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    DpsWriter dpsWriter;

    @Resource
    private EJBContext ejbContext;

    @Inject
    private DpsReader dpsReader;

    @Inject
    private FileResource fileResource;

    @Inject
    private SystemRecorder systemRecorder;

    private static final String DELETE_LICENSEKEY_FILES = "SHM.DELETE_LICENSEKEYFILES";

    /**
     * This method deletes the historic license key files from the DPS and also from SMRS location.
     * 
     */
    /**
     * @param fingerPrint
     * @param sequenceNumber
     * @return logMessage
     */
    public String deleteHistoricLicensePOs(final String fingerPrint, final String sequenceNumber) {
        String ldfPathRecord = null;
        String logMessage = null;

        // Sequence Number and FingerPrint Restrictions
        final Map<String, Map<String, List<String>>> attributeRestrictionMap = restrictionFormation(fingerPrint, sequenceNumber);

        try {
            // To get the matching POs
            final List<LicenseData> licenseDatalist = prepareLicenseData(attributeRestrictionMap);
            for (final LicenseData licenseData : licenseDatalist) {
                if (licenseData != null) {
                    // Removing the extension ".xml" and appending "_info.xml"
                    ldfPathRecord = FilenameUtils.removeExtension(licenseData.getLicenseKeyFilePath()) + LDF_ENDING_SEQUENCE;

                    // Deleting the LicenseData PO from DPS
                    final int deleteLicenseData = dpsWriter.deletePoByPoId(licenseData.getPoId());

                    if (deleteLicenseData > 0) {
                        // Deleting the LicenseData from File Store
                        deleteFromFileStore(licenseData);
                        final String ldfFileName = FilenameUtils.getName(licenseData.getLicenseKeyFilePath());
                        logMessage = "Successfully Deleted the previously installed License Key File " + ldfFileName + " instances from DPS and SMRS";
                        logger.info(logMessage);
                    } else {
                        logger.debug("Unable to delete DB entry for {}", licenseData.getFingerPrint());
                        logMessage = "Unable to delete DB entry for: " + licenseData.getFingerPrint();
                    }
                } else {
                    logger.info("No Matching entry found in Database for fingerprint {}", fingerPrint);
                    logMessage = "No Matching entry found in Database for deletion!";
                }
            }
        } catch (final LicenseDeleteException e) {
            logMessage = "LicenseFile deletion failed due to : " + e;
            logger.error(logMessage);
            ejbContext.setRollbackOnly();
        }
        systemRecorder.recordEvent(DELETE_LICENSEKEY_FILES, EventLevel.DETAILED, "  ", ldfPathRecord, " ");

        return logMessage;
    }

    /**
     * This method forms the required parameter for forming the query and adds restrictions.
     * 
     */
    /**
     * @param fingerPrint
     * @param sequenceNumber
     * @return attributeRestrictionMap
     */
    private Map<String, Map<String, List<String>>> restrictionFormation(final String fingerPrint, final String sequenceNumber) {
        final Map<String, Map<String, List<String>>> attributeRestrictionMap = new HashMap<>();
        // Sequence Number Restrictions
        final Map<String, List<String>> sequenceNumberValueMap = new HashMap<>();
        final List<String> sequenceNumberValueList = new ArrayList<>();
        sequenceNumberValueList.add(sequenceNumber);
        sequenceNumberValueMap.put("LESS_THAN", sequenceNumberValueList);
        // Adding to the the restriction Map
        attributeRestrictionMap.put(LICENSE_DATA_SEQUENCE_NUMBER, sequenceNumberValueMap);
        // Finger Print Restriction
        final Map<String, List<String>> fingerPrintValueMap = new HashMap<>();
        final List<String> fingerPrintValueList = new ArrayList<>();
        fingerPrintValueList.add(fingerPrint);
        fingerPrintValueMap.put("EQUALS", fingerPrintValueList);
        // Adding to the the restriction Map
        attributeRestrictionMap.put(LICENSE_DATA_FINGERPRINT, fingerPrintValueMap);
        return attributeRestrictionMap;
    }

    /**
     * This method deletes the license key file paths from the file strore i.e., SMRS.
     * 
     */
    /**
     * @param licenseData
     * @throws LicenseDeleteException
     */
    private void deleteFromFileStore(final LicenseData licenseData) throws LicenseDeleteException {
        final String ldfPath = FilenameUtils.removeExtension(licenseData.getLicenseKeyFilePath()) + LDF_ENDING_SEQUENCE;
        // Deleting the LicenseData fileExtentionPath from SMRS Location
        final boolean ldfExtPathstatus = deleteFile(licenseData.getLicenseKeyFilePath());
        // Deleting the LicenseData filePath from SMRS Location
        final boolean ldfPathstatus = deleteFile(ldfPath);
        logger.debug("The license data fileExtentionPath deletion status is: {}  and the license data filePath deletion status is: {}", ldfExtPathstatus, ldfPathstatus);
    }

    /**
     * This method gets the required parameter from restrictions that are formed and prepares the required License Data.
     * 
     */
    /**
     * @param attributeRestrictionMap
     */
    private List<LicenseData> prepareLicenseData(final Map<String, Map<String, List<String>>> attributeRestrictionMap) {
        final List<LicenseData> licenseDataList = new ArrayList<>();
        final List<PersistenceObject> matchingPOList = dpsReader.findPOS(LICENSE_DATA_PO_NAMESPACE, LICENSE_DATA_PO, attributeRestrictionMap);
        if (matchingPOList.isEmpty()) {
            logger.debug("No Matching data found in DB for fingerprint {}", attributeRestrictionMap.get(LICENSE_DATA_FINGERPRINT));
            return licenseDataList;
        }
        for (final PersistenceObject matchingPO : matchingPOList) {
            final LicenseData licenseData = new LicenseData();
            final Map<String, Object> poAttributes = matchingPO.getAllAttributes();
            final long poId = matchingPO.getPoId();
            final String licenseKeyfilePath = (String) poAttributes.get(LICENSE_DATA_LICENSE_KEYFILE_PATH);
            final String sequenceNumber = (String) poAttributes.get(LICENSE_DATA_SEQUENCE_NUMBER);
            logger.debug("The Sequence number is :{}", sequenceNumber);
            final String fingerPrint = (String) poAttributes.get(LICENSE_DATA_FINGERPRINT);
            logger.debug("The fingerPrint is:{}", fingerPrint);
            licenseData.setPoId(poId);
            licenseData.setLicenseKeyFilePath(licenseKeyfilePath);
            licenseDataList.add(licenseData);
        }
        return licenseDataList;
    }

    /**
     * This method deletes the file using the File based operation Resource provided by sdk api
     * 
     */
    /**
     * @param filePath
     * @throws LicenseDeleteException
     */
    private boolean deleteFile(final String filePath) throws LicenseDeleteException {
        logger.debug("Removing file with file path as: {}", filePath);
        boolean status = false;
        // Initializing the file
        if (fileResource.exists(filePath)) {
            if (fileResource.delete(filePath)) {
                // The file deleted
                status = true;
                logger.debug("The Deletion is successfully done from file store:{}", status);
            } else {
                // The file not deleted
                status = false;
                logger.debug("Unable to delete entry from file store:{}", status);
                throw new LicenseDeleteException("Unable to delete ");
            }
        } else {
            status = false;
            logger.debug("The Deletion is:{}", status);
            logger.debug("{} is not exists", filePath);
        }
        return status;
    }

}
