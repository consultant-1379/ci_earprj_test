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
package com.ericsson.oss.services.shm.jobexecutor;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.ericsson.oss.services.shm.activities.JobExecutionValidator;
import com.ericsson.oss.services.shm.activities.JobExecutionValidatorFactory;
import com.ericsson.oss.services.shm.common.annotations.PlatformAnnotation;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;

/**
 * Implementation to fetch the job execution validation class specific to input platform type. There is a memory leakage issue in using InstanceProvider. So, we changed the implementation to Factory
 * Design as a quick fix. However, using Factory Design leads to poor code maintainability, so we need to analyze and find out a better solution which improves code maintainability and doesn't leak
 * any memory. JIRA : https://jira-nam.lmera.ericsson.se/browse/TORF-180324
 * 
 * @author tcsyesa
 * 
 */
@ApplicationScoped
public class JobExecutionValidatorFactoryImpl implements JobExecutionValidatorFactory {

    @Inject
    @PlatformAnnotation(name = PlatformTypeEnum.CPP)
    JobExecutionValidator cppJobExecutionValidator;

    @Inject
    @PlatformAnnotation(name = PlatformTypeEnum.ECIM)
    JobExecutionValidator ecimJobExecutionValidator;

    @Inject
    @PlatformAnnotation(name = PlatformTypeEnum.AXE)
    JobExecutionValidator axeJobExecutionValidator;

    @Inject
    JobExecutionValidator commonExecutionValidator;

    /**
     * Method to fetch the job execution validation class specific to input platform type.
     * 
     * @return specific implementation of JobExecutionValidator or null in case of non-existence of validator.
     */
    @Override
    public JobExecutionValidator getJobExecutionValidator(final PlatformTypeEnum platformType) {
        JobExecutionValidator jobExecutionValidatorInstance = null;
        switch (platformType) {

        case CPP:
            jobExecutionValidatorInstance = cppJobExecutionValidator;
            break;

        case ECIM:
            jobExecutionValidatorInstance = ecimJobExecutionValidator;
            break;

        case AXE:
            jobExecutionValidatorInstance = axeJobExecutionValidator;
            break;

        //Common Validator for all unsupported Platforms and platforms which do not have any specific validator.
        default:
            jobExecutionValidatorInstance = commonExecutionValidator;
            break;
        }
        return jobExecutionValidatorInstance;
    }
}
