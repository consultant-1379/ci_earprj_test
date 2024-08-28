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
package com.ericsson.oss.services.shm.es.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This NodeMediationServiceExceptionParser is used to return the exception message when MediationServiceException is caught
 * 
 */
public abstract class NodeMediationServiceExceptionParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeMediationServiceExceptionParser.class);
    private static final String exceptionString = "MediationServiceException";
    private static final String MOCI_EXCEPTION_STRING = "Error borrowing MociConnection";

    public static String getReason(final Exception exception) {
        int count = 1;
        String exceptionMessage = "";
        LOGGER.debug("Entered into method NodeMediationServiceExceptionParser : getMediationServiceException");
        if (exception != null) {
            Throwable exceptionCause = exception.getCause();
            while (exceptionCause != null && count <= 50) {
                final String cause = exceptionCause.toString();
                LOGGER.debug("Cause {} = {}", count++, cause);
                if (cause.contains(exceptionString)) {
                    exceptionMessage = exceptionCause.getMessage();
                    if (exceptionMessage.contains(MOCI_EXCEPTION_STRING)) {

                        return JobLogConstants.MOCI_CONNECTION_MESSAGE + exceptionMessage;
                    }
                    LOGGER.error("NodeMediationServiceExceptionParser : MediationServiceException, Exception Message : {} ", exceptionMessage);
                    break;
                }
                exceptionCause = exceptionCause.getCause();
            }
        }
        LOGGER.debug("Exited from method NodeMediationServiceExceptionParser : getMediationServiceException");
        return exceptionMessage;
    }

    public static boolean isNodeUnreachable(final Exception exception) {

        int count = 1;
        LOGGER.debug("Entered into method NodeMediationServiceExceptionParser : getMediationServiceException");
        if (exception != null) {
            Throwable exceptionCause = exception.getCause();
            while (exceptionCause != null && count <= 50) {
                final String cause = exceptionCause.toString();
                LOGGER.debug("Cause {} = {}", count++, cause);
                if (cause.contains(exceptionString)) {
                    LOGGER.error("NodeMediationServiceExceptionParser : MediationServiceException, Exception Message : {} ", exceptionCause.getMessage());
                    return true;
                }
                exceptionCause = exceptionCause.getCause();
            }
        }
        LOGGER.debug("Exited from method NodeMediationServiceExceptionParser : getMediationServiceException");
        return false;

    }
}
