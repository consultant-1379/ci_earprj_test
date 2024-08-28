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
package com.ericsson.oss.services.shm.jobs.common.annotations;

import java.lang.annotation.*;

import javax.inject.Qualifier;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.job.activity.JobType;

/**
 * 
 * @author xrajeke
 * 
 */
@Qualifier
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = { ElementType.METHOD, ElementType.FIELD, ElementType.TYPE, ElementType.PARAMETER })
public @interface PlatformJobTypeAnnotation {
    public PlatformTypeEnum platformType();

    public JobType jobType();
}
