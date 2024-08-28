/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.shm.instantaneouslicensing;

/**
 * InstantaneousLicensing MO related constants
 * 
 * @author Team Royals
 *
 */
public class InstantaneousLicensingMOConstants {

    public enum Filters {
        INSTANTANEOUS_LICENSING("namespace = 'RmeLicenseSupport' AND type = 'InstantaneousLicensing'");

        private final String filter;

        private Filters(final String filter) {
            this.filter = filter;
        }

        public String getFilter() {
            return filter;
        }
    }

    public enum Attributes {
        ADMINISTRATIVESTATE("administrativeState"), OPERATIONALSTATE("operationalState"), AVAILABILITYSTATUS("availabilityStatus"), PROGRESSREPORT("progressReport");
        private final String attributeName;

        private Attributes(final String attributeName) {
            this.attributeName = attributeName;
        }

        public String getAttributeName() {
            return attributeName;
        }
    }

    public enum AdministrativeState {
        LOCKED("LOCKED"), UNLOCKED("UNLOCKED");
        private final String value;

        private AdministrativeState(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum OperationalState {
        DISABLED("DISABLED"), ENABLED("ENABLED");
        private final String value;

        private OperationalState(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum AvailabilityStatus {
        OFF_LINE("OFF_LINE");
        private final String value;

        private AvailabilityStatus(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
