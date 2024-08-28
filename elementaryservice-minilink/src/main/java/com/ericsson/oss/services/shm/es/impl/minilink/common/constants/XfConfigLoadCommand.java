package com.ericsson.oss.services.shm.es.impl.minilink.common.constants;

public enum XfConfigLoadCommand implements MibEntry {
    CONFIG_UPLOAD(1, "configUpload"),
    CONFIG_DOWNLOAD(2, "configDownload"),
    CONFIG_DOWNLOAD_AND_APPEND(3, "configDownloadAndAppend"),
    CREATE_SINGLE_FILE_CONFIG(4, "createSingleFileConfig"),
    LOAD_FROM_RMM(5, "loadFromRMM");

    private final int statusCode;
    private final String statusValue;

    XfConfigLoadCommand(final int statusCode, final String statusValue) {
        this.statusCode = statusCode;
        this.statusValue = statusValue;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusValue() {
        return statusValue;
    }

    public static XfConfigLoadCommand fromStatusValue(final String statusValue) {
        return MibEnumUtility.fromStatusValue(XfConfigLoadCommand.class, statusValue);
    }

}
