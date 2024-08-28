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
package com.ericsson.oss.services.shm.es.impl.cpp.backup;

public enum ConfigurationVersionType {

    STANDARD("STANDARD"), TEST("TEST"), OTHER("OTHER"), DOWNLOADED("DOWNLOADED");

    private String cvType;

    private ConfigurationVersionType(final String cvType) {
        this.cvType = cvType;
    }

    /**
     * @return the cvType
     */
    public String getCvType() {
        return cvType;
    }

    public static ConfigurationVersionType getCvType(final String cvType) {

        for (final ConfigurationVersionType s : ConfigurationVersionType.values()) {
            if (s.name().equalsIgnoreCase(cvType)) {
                return s;
            }
        }
        return null;
    }

}
