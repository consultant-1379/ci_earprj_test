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
package com.ericsson.oss.services.shm.es.impl.cpp.common;

public enum CVActionResultInformation {
    NOTFOUND(-1, "Additional Information not found"), CORE_CENTRAL_MP_POSITION_NOT_MATCHING(0, "The node core central MP position(s) is/are not the same as in the downloaded backup CV."), CV_BACKUP_NOT_FOUND(
            1, "The CV backup was not found on the FTP server."), CV_IS_DAMAGED(2, "The CV is damaged (not useable)."), CV_IS_INCOMPLETE(3, "Some CV files are missing. "), CV_IS_INVALID_FOR_BACKUP(4,
            "The CV type is invalid for creation of a remote backup."), CV_NAME_ALREADY_EXISTS(5,
            "The downloaded backup CV contains a CV with a name that is identical to a CV already existing on the node."), CV_IS_NOT_FOUND(6, "The CV does not exist."), FORMAT_OF_IP_ADDRESS_IS_FAULTY(
            7, "Erroneous IP Address Format."), FTP_ACCESS_OR_TRANSFER_FAILED(8, "An error was encountered during FTP file access or transfer."), FTP_SERVER_IS_NOT_ACCESSIBLE(
            9,
            "FTP server is not accessible.This reason may be one or several of the following:\n- Erroneous IP address, username or password \n- IP communication with the FTP server is down \n- The FTP server itself is malfunctioning."), HARDWARE_IS_INCOMPATIBLE(
            10, "Mismatch detected in the node board/slot configuration compared to the downloaded backup CV."), HARDWARE_SOFTWARE_IS_INCOMPATIBLE(11,
            "Boards in the node are not supported by the UCF(s)."), INSUFFICIENT_DISKSPACE_ON_NODE(12, "There are not enough with disk space on node"), MAX_NUMBER_OF_CVS_WILL_BE_EXCEEDED(13,
            "The maximum number of allowed CVs will be exceeded if the required action is executed."), NETWORK_CONFIGURATION_MISMATCH_FOR_IP_ATM(14,
            "Mismatch detected when comparing the node network configuration for IP/ATM with corresponding data in the downloaded backup CV."), UNKNOWN_NODE_IDENTITY(15,
            "The node identity/site in the downloaded backup CV doesn't match the existing node identity/site."), UNSPECIFIED(16,
            "A not specified error/ warning has been detected (see additional info for further details)."), UPGRADE_PACKAGE_IS_CORRUPTED(
            17,
            "Software integrity problems detected, that is, one or more of the load modules related to the downloaded backup CV have checksum errors. The found checksum errors are corrected by requesting forced install on any upgrade package that contains the faulty load modules.See further attribute corruptedUpgradePackages."), UPGRADE_PACKAGE_IS_MISSING(
            18, "Upgrade package(s) referenced in the downloaded backup CV are not present on the node. See further attribute missingUpgradePackages."), ILLEGAL_VALUE_RECEIVED_IN_INPUT_PARAMETER(19,
            "An input parameter has been assigned an illegal value."), CV_BACKUP_NAME(20,
            "This is the name with which a CV is saved in the FTP server after having executed the putToFtpServer action. Notice that the name will have the form *.zip"), INSUFFICIENT_DISKSPACE_ON_FTP(
            21, "There are not enough with disk space on the FTP server."), CV_IS_INVALID_FOR_RESTORE(22,
            "The CV type is invalid for restore (only CV of type downloaded is valid for restore/forcedRestore)."), CV_IS_NOT_CONNECTED_TO_AN_UPGRADE_PACKAGE(23,
            "The CV is not connected to an Upgrade Package."), RESTORE_MANUALLY_CANCELLED(24,
            "The execution of restore/forcedRestore has been cancelled (during the verify phase of restore, that is when action cancelRestore has been invoked)."), ACTION_RESTORE_IS_ALLOWED(25,
            "The verify restore action has determined that action restore is allowed"), ACTION_RESTORE_IS_NOT_ALLOWED(26,
            "The verify restore action has determined that action restore is not allowed."), ACTION_FORCED_RESTORE_IS_ALLOWED(27,
            "The verify restore action has determined that action forced restore is allowed"), ACTION_FORCED_RESTORE_IS_NOT_ALLOWED(28,
            "The verify restore action has determined that action forced restore is not allowed."), CORE_CENTRAL_MP_POSITION_PARTLY_MATCHING(29,
            "One node core central MP position is not the same as in the downloaded backup CV."), NETWORK_CONFIGURATION_MISMATCH_FOR_IP(30,
            "Mismatch detected when comparing the node network configuration for IP with corresponding data in the downloaded backup CV."), CERTIFICATE_OR_SIGNATURE_PROBLEM(31,
            "Certificate or signature problem detected for at least one load module."), UPGRADE_PACKAGE_UCF_IS_CORRUPTED(
            32,
            "Software integrity problems detected in the UCF for UP that is associated to the configuration version.If the UCF is associated to current upgrade package a forced install will correct the problem. Otherwise the UP has to be deleted and re-created/installed.");

    private String additionalResultInformation;
    private int resultId;

    /**
     * 
     */
    CVActionResultInformation(final int resultId, final String resultInformation) {
        this.resultId = resultId;
        this.additionalResultInformation = resultInformation;
    }

    public String getCVActionResultInformationDesc() {
        return additionalResultInformation;
    }

    public int getResultId() {
        return resultId;
    }

    public static CVActionResultInformation getCvActionResultInformation(final String resultInformation) {

        for (final CVActionResultInformation s : CVActionResultInformation.values()) {
            if (s.name().equalsIgnoreCase(resultInformation)) {
                return s;
            }
        }
        return null;
    }

}
