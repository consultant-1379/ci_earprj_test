/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2015
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.jobs.common.enums;

/**
 * Holds the Node Health Status which is being stored into <code>NEJob.healthStatus</code>
 *
 */
public enum HealthStatus {
    HEALTHY, NOT_HEALTHY, WARNING, NOT_AVAILABLE, UNDETERMINED
}
