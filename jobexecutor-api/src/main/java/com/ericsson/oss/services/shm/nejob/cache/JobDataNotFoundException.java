/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.nejob.cache;

/**
 * It will be thrown when NE job static data not found in neJob cache and failed to get from DPS.
 * 
 * @author tcsgusw
 * 
 */
public class JobDataNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    public JobDataNotFoundException() {

    }

    public JobDataNotFoundException(final String message) {
        super(message);
    }

}
