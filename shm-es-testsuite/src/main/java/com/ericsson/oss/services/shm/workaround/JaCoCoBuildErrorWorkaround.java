/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2013
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.workaround;

/**
 * Class created only to serve as a workaround JaCoCo plugin execution that forces that there is a /target/class directory created.<br>
 * This will happen with any project that does not contain "product code" (i.e. code in src/main/java).<br>
 * There is a but open for JaCoCo do deal with this problem in here: https://github.com/jacoco/jacoco/issues/67 <br>
 * When this bug is fixed, this class can be deleted.<br>
 */
@SuppressWarnings("UnusedDeclaration")
public interface JaCoCoBuildErrorWorkaround {

}
