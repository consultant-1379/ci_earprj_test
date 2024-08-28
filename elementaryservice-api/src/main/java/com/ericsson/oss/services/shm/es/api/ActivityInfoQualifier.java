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
package com.ericsson.oss.services.shm.es.api;

import javax.enterprise.util.AnnotationLiteral;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

public class ActivityInfoQualifier extends AnnotationLiteral<ActivityInfo> implements ActivityInfo {

    private final String activityName;

    private final JobTypeEnum jobType;

    private final PlatformTypeEnum platform;

    private static final long serialVersionUID = 8026609803843482049L;

    public ActivityInfoQualifier(final PlatformTypeEnum platform, final JobTypeEnum jobType, final String activityName) {
        this.platform = platform;
        this.jobType = jobType;
        this.activityName = activityName;
    }

    @Override
    public String activityName() {

        return activityName;
    }

    @Override
    public JobTypeEnum jobType() {

        return jobType;
    }

    @Override
    public PlatformTypeEnum platform() {

        return platform;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + platform.hashCode();
        result = 31 * result + jobType.hashCode();
        result = 31 * result + activityName.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object o) {

        if (o == null) {
            return false;
        }
        if (!(o instanceof ActivityInfoQualifier)) {
            return false;
        }
        final ActivityInfoQualifier other = (ActivityInfoQualifier) o;

        if (!other.activityName.equals(activityName)) {
            return false;
        }

        if (jobType != other.jobType) {
            return false;
        }

        if (platform != other.platform) {
            return false;
        }
        return true;

    }

}
