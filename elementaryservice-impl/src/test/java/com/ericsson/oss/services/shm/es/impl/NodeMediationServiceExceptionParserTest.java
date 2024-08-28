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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NodeMediationServiceExceptionParserTest {

    //when there is no MediationServiceException String we need to get return string should not be empty null
    @Test
    public void testSuccess() {
        final Throwable cause1 = new Throwable("MediationServiceException");
        final Throwable cause2 = new Throwable("MediationS erviceException", cause1);
        final Throwable cause3 = new Throwable("Mediation exception", cause2);
        final Throwable cause4 = new Throwable("MediationSer viceException", cause3);
        final Exception exception = new Exception("example ", cause4);
        final String returnString = NodeMediationServiceExceptionParser.getReason(exception);
        final Boolean variable = returnString == null ? true : returnString.isEmpty();
        assertEquals(variable, false);
    }

    //when there is no MediationServiceException String we need to get return string as null 
    @Test
    public void testfailure() {
        final Throwable cause1 = new Throwable("MediationSe rviceException");
        final Throwable cause2 = new Throwable("MediationS erviceException", cause1);
        final Throwable cause3 = new Throwable("Mediation exception", cause2);
        final Throwable cause4 = new Throwable("MediationSer viceException", cause3);
        final Exception exception = new Exception("example ", cause4);
        final String returnString = NodeMediationServiceExceptionParser.getReason(exception);
        final Boolean variable = returnString == null ? true : returnString.isEmpty();
        assertEquals(variable, true);
    }

    //when the cause is null then we need to get null(String is empty)
    @Test
    public void testWailureWithNullCause() {
        final Throwable cause2 = new Throwable("MediationS erviceException", null);
        final Throwable cause3 = new Throwable("Mediation exception", cause2);
        final Throwable cause4 = new Throwable("MediationSer viceException", cause3);
        final Exception exception = new Exception("example ", cause4);
        final String returnString = NodeMediationServiceExceptionParser.getReason(exception);
        final Boolean variable = returnString == null ? true : returnString.isEmpty();
        assertEquals(variable, true);
    }

}
