/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.loadcontrol.local.api;

/**
 * It is used to populate the load controller local counter value per member.
 * 
 * @author tcsvnag
 * 
 */
public interface PrepareLoadControllerLocalCounterService {

    void prepareMaxCountMap(int membersCount);

}
