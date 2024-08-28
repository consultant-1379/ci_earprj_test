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

package com.ericsson.oss.services.shm.es.impl.minilink.common.constants;

public enum XfConfigStatus implements MibEntry {

    CONFIG_UP_LOADING(1, "configUpLoading"),
    CONFIG_UP_LOAD_OK(2, "configUpLoadOK"),
    CONFIG_UP_LOAD_FAILED(3, "configUpLoadFailed"),
    CONFIG_DOWN_LOADING(4, "configDownLoading"),
    CONFIG_DOWN_LOAD_OK(5, "configDownLoadOK"),
    CONFIG_DOWNLOAD_FAILED(6, "configDownloadFailed"),
    ERROR_INTERNAL_ERROR(50, "errorInternalError"),
    ERROR_FILE_STORAGE(51, "errorFileStorage"),
    FTP_PING_FAILED(52, "ftpPingFailed"),
    FTP_NO_ACCESS(53, "ftpNoAccess"),
    FTP_CONNECTION_DETAILS_MISSING(54, "ftpConnectionDetailsMissing"),
    FTP_CONNECTION_DETAILS_INVALID(55, "ftpConnectionDetailsInvalid"),
    FTP_CONNECTION_TIMEOUT(56, "ftpConnectionTimeout"),
    FTP_NO_SUCH_REMOTE_FILE(57, "ftpNoSuchRemoteFile"),
    FTP_NO_SUCH_REMOTE_DIR(58, "ftpNoSuchRemoteDir"),
    FTP_SERVICE_NOT_AVAILABLE(421, "ftpServiceNotAvailable"),
    FTP_UNABLE_TO_OPEN_DATA_CONNECTION(425, "ftpUnableToOpenDataConnection"),
    FTP_CONNECTION_CLOSED(426, "ftpConnectionClosed"),
    FTP_FILE_BUSY(450, "ftpFileBusy"),
    FTP_LOCAL_ERROR(451, "ftpLocalError"),
    FTP_INSUFFICIENT_STORAGE_SPACE(452, "ftpInsufficientStorageSpace"),
    FTP_SYNTAX_ERROR(501, "ftpSyntaxError"),
    FTP_COMMAND_NOT_IMPLENTED(502, "ftpCommandNotImplented"),
    FTP_BAD_SEQUENCE_COMMANDS(503, "ftpBadSequenceCommands"),
    FTP_PARAMETER_NOT_IMPLEMENTED(504, "ftpParameterNotImplemented"),
    FTP_NOT_LOGGED_IN(530, "ftpNotLoggedIn"),
    FTP_NEED_ACCOUNT(532, "ftpNeedAccount"),
    FTP_FILE_UNAVAILABLE(550, "ftpFileUnavailable"),
    FTP_REQUESTED_ACTION_ABORTED(551, "ftpRequestedActionAborted"),
    FTP_EXCEEDED_STORAGE_ALLOCATION(552, "ftpExceededStorageAllocation"),
    FTP_FILE_NAME_NOT_ALLOWED(553, "ftpFileNameNotAllowed");

    private final int statusCode;
    private final String statusValue;

    XfConfigStatus(final int statusCode, final String statusValue) {
        this.statusCode = statusCode;
        this.statusValue = statusValue;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusValue() {
        return statusValue;
    }

    public static XfConfigStatus fromStatusValue(final String statusValue) {
        return MibEnumUtility.fromStatusValue(XfConfigStatus.class, statusValue);
    }

}
