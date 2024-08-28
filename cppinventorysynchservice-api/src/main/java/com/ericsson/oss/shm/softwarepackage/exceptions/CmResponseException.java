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
package com.ericsson.oss.shm.softwarepackage.exceptions;

public class CmResponseException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String ERROR_MESSAGE = "Operation failed; statusMessage: %s;SuggestedSolution: %s";

	/**
	 * Creates the exception outlining the problem
	 * 
	 * @param statusCode
	 * @param statusMessage
	 * @param suggestedSolution
	 */
	public CmResponseException(final String statusMessage, final String suggestedSolution) {
		super(buildMessage(statusMessage, suggestedSolution));
	}

	private static String buildMessage(	final String statusMessage, final String suggestedSolution) {
		return String.format(ERROR_MESSAGE, statusMessage,
				suggestedSolution);
	}

}
