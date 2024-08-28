package com.ericsson.oss.services.shm.es.impl.minilink.common.constants;

final class MibEnumUtility {

    private MibEnumUtility() {}

    static <T extends Enum<T> & MibEntry> T fromStatusValue(final Class<T> clazz, final String statusValue) {
        for (T mibEntry : clazz.getEnumConstants()) {
            if (statusValue.equals(mibEntry.getStatusValue())) {
                return mibEntry;
            }
        }
        return null;
    }

}
