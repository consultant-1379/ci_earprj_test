package com.ericsson.oss.services.shm.es.impl.minilink.common.constants;

public enum SyncStatus implements MibEntry {
    SYNCHRONIZED(1, "SYNCHRONIZED"),
    UNSYNCHRONIZED(2, "UNSYNCHRONIZED"),
    TOPOLOGY(3, "TOPOLOGY"),
    ATTRIBUTE(4, "ATTRIBUTE"),
    PENDING(5, "PENDING"),
    DELTA(6, "DELTA");

    private final int statusCode;
    private final String statusValue;

    SyncStatus(final int statusCode, final String statusValue) {
        this.statusCode = statusCode;
        this.statusValue = statusValue;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusValue() {
        return statusValue;
    }

    public static SyncStatus fromStatusValue(final String statusValue) {
        return MibEnumUtility.fromStatusValue(SyncStatus.class, statusValue);
    }

}
