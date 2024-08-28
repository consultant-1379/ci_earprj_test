package com.ericsson.oss.services.shm.jobs.common.annotations;

import javax.enterprise.util.AnnotationLiteral;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.job.activity.JobType;

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

/**
 * This class is used to select a platform specific implementation Type
 * 
 */
public class PlatformJobTypeQualifier extends AnnotationLiteral<PlatformJobTypeAnnotation> implements PlatformJobTypeAnnotation {

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((jobType == null) ? 0 : jobType.hashCode());
        result = prime * result + ((platformType == null) ? 0 : platformType.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PlatformJobTypeQualifier other = (PlatformJobTypeQualifier) obj;
        if (jobType != other.jobType) {
            return false;
        }
        if (platformType != other.platformType) {
            return false;
        }
        return true;
    }

    private static final long serialVersionUID = 4137986347126623600L;

    private final PlatformTypeEnum platformType;
    private final JobType jobType;

    public PlatformJobTypeQualifier(final PlatformTypeEnum platformType, final JobType jobType) {
        this.platformType = platformType;
        this.jobType = jobType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.shm.jobs.common.annotations.PlatformJobTypeAnnotation#platformType()
     */
    @Override
    public PlatformTypeEnum platformType() {
        return platformType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.shm.jobs.common.annotations.PlatformJobTypeAnnotation#jobType()
     */
    @Override
    public JobType jobType() {
        return jobType;
    }
}
