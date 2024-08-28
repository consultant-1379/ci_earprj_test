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
package com.ericsson.oss.services.shm.activities;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;

/**
 * Interface to define the job execution validation class specific to platform type.
 * 
 * @author tcsyesa
 * 
 */
public interface JobExecutionValidatorFactory {

    JobExecutionValidator getJobExecutionValidator(final PlatformTypeEnum platformType);

}