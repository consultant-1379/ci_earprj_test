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
package com.ericsson.oss.services.shm.job.exceptions;

/**
 * Exception thrown if the Job configuration is null
 * @author tcserra
 *
 */
public class NoJobConfigurationException extends Exception{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NoJobConfigurationException(final String errorMessage) {
        super(errorMessage);
    }

}
