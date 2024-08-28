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
package com.ericsson.oss.services.shm.jobservice.common;

public final class JobPropertyKeys {
	
	public static final String ME_FDN = "mefdn";
	public static final String CREATIONAL_INFO_KEY_PACKAGE_NAME_CPP = "CPP_PackageName";
	public static final String CREATIONAL_INFO_KEY_PACKAGE_NAME_ECIM = "ECIM_PackageName";
	
    private JobPropertyKeys()
    {
        // cannot instantiate even with reflection
        throw new IllegalStateException();
    }


}
