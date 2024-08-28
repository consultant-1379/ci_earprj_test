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

public enum RestartReason {

    PLANNED_RECONFIGURATION("PLANNED_RECONFIGURATION"), UNPLANNED_NODE_EXTERNAL_PROBLEMS("UNPLANNED_NODE_EXTERNAL_PROBLEMS"), UNPLANNED_NODE_UPGRADE_PROBLEMS("UNPLANNED_NODE_UPGRADE_PROBLEMS"), UNPLANNED_O_AND_M_ISSUE(
            "UNPLANNED_O_AND_M_ISSUE"), UNPLANNED_CYCLIC_RECOVERY("UNPLANNED_CYCLIC_RECOVERY"), UNPLANNED_LOCKED_RESOURCES("UNPLANNED_LOCKED_RESOURCES"), UNPLANNED_COLD_WITH_HW_TEST(
            "UNPLANNED_COLD_WITH_HW_TEST"), UNPLANNED_CALL_PROCESSING_DEGRADATION("UNPLANNED_CALL_PROCESSING_DEGRADATION"), UNPLANNED_LOW_COVERAGE("UNPLANNED_LOW_COVERAGE"), UPGRADE_BOARD_RESTART(
            "UPGRADE_BOARD_RESTART"), OPERATOR_CLASSIFIED_PROBLEMS("OPERATOR_CLASSIFIED_PROBLEMS");

    private String restartReason;

    public String getRestartReason() {
        return restartReason;
    }

    private RestartReason(final String restartReason) {
        this.restartReason = restartReason;
    }

    public static RestartReason getRestartReason(final String restartReason) {

        for (final RestartReason s : RestartReason.values()) {
            if (s.name().equalsIgnoreCase(restartReason)) {
                return s;
            }
        }
        return null;
    }
}
