package com.ericsson.oss.services.shm.jobs.common.annotations;

import javax.enterprise.util.AnnotationLiteral;

import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;

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
 * This class is used to select a Job Type specific implementation
 * 
 * @author xsrabop
 */
public class JobTypeQualifier extends AnnotationLiteral<JobTypeAnnotation> implements JobTypeAnnotation {

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
        final JobTypeQualifier other = (JobTypeQualifier) obj;
        if (jobType != other.jobType) {
            return false;
        }
        return true;
    }

    private static final long serialVersionUID = 4137986347126623600L;

    private final JobType jobType;

    public JobTypeQualifier(final JobType jobType) {
        this.jobType = jobType;
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
