/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.shared.enums;

public enum RestartRank {

    RESTART_WARM("RESTART_WARM"), RESTART_REFRESH("RESTART_REFRESH"), RESTART_COLD("RESTART_COLD"), RESTART_COLDWTEST("RESTART_COLDWTEST");

    private String restartRank;

    public String getRestartRank() {
        return restartRank;
    }

    private RestartRank(final String restartRank) {
        this.restartRank = restartRank;
    }

    public static RestartRank getRestartRank(final String restartRank) {

        for (final RestartRank s : RestartRank.values()) {
            if (s.name().equalsIgnoreCase(restartRank)) {
                return s;
            }
        }
        return null;
    }

}
