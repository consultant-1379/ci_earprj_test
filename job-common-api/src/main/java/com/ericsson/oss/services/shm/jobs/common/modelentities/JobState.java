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
package com.ericsson.oss.services.shm.jobs.common.modelentities;

import java.util.HashMap;
import java.util.Map;

public enum JobState {

    CREATED("CREATED"), SUBMITTED("SUBMITTED"), WAIT_FOR_USER_INPUT("WAIT_FOR_USER_INPUT"), SCHEDULED("SCHEDULED"), RUNNING("RUNNING"), CANCELLING("CANCELLING"), COMPLETED(
            "COMPLETED"), SYSTEM_CANCELLED("SYSTEM_CANCELLED"), SYSTEM_CANCELLING("SYSTEM_CANCELLING"), DELETING("DELETING"), WAIT_FOR_SCRIPT_INPUT("WAIT_FOR_SCRIPT_INPUT"), USER_INTERRUPTED("USER_INTERRUPTED"), INTERRUPTED("INTERRUPTED");

    private String jobStateName;

    private static Map<String, JobState> jobStates = new HashMap<String, JobState>();

    static {

        jobStates = new HashMap<String, JobState>();
        for (final JobState jobState : JobState.values()) {
            jobStates.put(jobState.getJobStateName(), jobState);

        }
    }

    /**
     * 
     */
    JobState(final String jobStateName) {
        this.jobStateName = jobStateName;
    }

    /**
     * @return the jobStateName
     */
    public String getJobStateName() {
        return jobStateName;
    }

    public static JobState getJobState(final String jobStateName) {
        return jobStates.get(jobStateName);
    }

    public static boolean isJobCancelled(final JobState neJobState) {

        boolean status = false;
        if (neJobState == SYSTEM_CANCELLED) {
            status = true;
        }
        return status;
    }

    public static boolean isJobCancelInProgress(final JobState jobState) {
        boolean status = false;
        if (jobState == CANCELLING || jobState == SYSTEM_CANCELLING) {
            status = true;
        }
        return status;
    }

    public static boolean isJobInactive(final JobState jobState) {
        boolean status = false;
        if (jobState == COMPLETED || jobState == SYSTEM_CANCELLED || jobState == DELETING) {
            status = true;
        }
        return status;
    }

    public static boolean isJobCreated(final JobState jobState) {
        boolean status = false;
        if (jobState == CREATED || jobState == SCHEDULED || jobState == WAIT_FOR_USER_INPUT || jobState == SUBMITTED) {
            status = true;
        }
        return status;
    }

    public static boolean isJobCompleted(final JobState jobState) {
        boolean status = false;
        if (jobState == COMPLETED) {
            status = true;
        }
        return status;
    }
}
