package com.ericsson.oss.services.shm.es.impl.minilink.common.constants;

public enum XfLicenseInstallAdminStatus implements MibEntry {
    DOWNLOAD_INSTALL(1, "downloadAndInstall"),
    ACCEPT_INVALID_LICENSE(2, "acceptInvalidLicense"),
    REJECT_INVALID_LICENSE(3, "rejectInvalidLicense");

    private final int statusCode;
    private final String statusValue;

    XfLicenseInstallAdminStatus(final int statusCode, final String statusValue) {
        this.statusCode = statusCode;
        this.statusValue = statusValue;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusValue() {
        return statusValue;
    }

    public static XfLicenseInstallAdminStatus fromStatusValue(final String statusValue) {
        return MibEnumUtility.fromStatusValue(XfLicenseInstallAdminStatus.class, statusValue);
    }

}