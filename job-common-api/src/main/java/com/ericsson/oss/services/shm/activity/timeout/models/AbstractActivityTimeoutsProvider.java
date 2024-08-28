/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.activity.timeout.models;

public abstract class AbstractActivityTimeoutsProvider implements ActivityTimeoutsProvider {
    protected static final String DELIMETER_UNDERSCORE = "_";

    protected String prepareKey(final String neTypeorPlatformType, final String jobType, final String activityName) {
        final String neTypeorPlatformTypeData = neTypeorPlatformType + DELIMETER_UNDERSCORE + jobType.toUpperCase() + DELIMETER_UNDERSCORE + activityName.toLowerCase();
        return neTypeorPlatformTypeData;

    }

}
