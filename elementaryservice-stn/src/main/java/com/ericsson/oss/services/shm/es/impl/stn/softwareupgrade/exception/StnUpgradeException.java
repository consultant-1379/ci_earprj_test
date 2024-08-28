/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.stn.softwareupgrade.exception;
/**
 * Exception to be thrown when no nodeFdn MO is found for the selected network element.
 * 
 * @author xsamven/xgowbom
 * 
 */
public class StnUpgradeException extends RuntimeException {

    private static final long serialVersionUID = 6480350172470773285L;

    public StnUpgradeException(final String errorMessage) {
        super(errorMessage);
    }

    public StnUpgradeException(final String errorMessage, final Exception cause) {
        super(errorMessage, cause);
    }
}
