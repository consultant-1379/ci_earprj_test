package com.ericsson.oss.services.shm.es.impl.minilink.common.constants;

public enum XfNERestartCommands implements MibEntry {

    WARM_RESTART(1, "warmRestart"),
    COLD_RESTART(2, "coldRestart");

    private final int statusCode;
    private final String statusValue;

    XfNERestartCommands(final int statusCode, final String statusValue) {
        this.statusCode = statusCode;
        this.statusValue = statusValue;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusValue() {
        return statusValue;
    }

    public static XfNERestartCommands fromStatusValue(final String statusValue) {
        return MibEnumUtility.fromStatusValue(XfNERestartCommands.class, statusValue);
    }

}
