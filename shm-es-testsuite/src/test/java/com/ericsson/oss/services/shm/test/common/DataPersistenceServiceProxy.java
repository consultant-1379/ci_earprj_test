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
package com.ericsson.oss.services.shm.test.common;

import javax.ejb.Local;

import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;

@Local
public interface DataPersistenceServiceProxy {
   /*
    * Proxy (wrapper) required for resolving EService References Please note for Arquillian tests the Implementation of this Interface HAS TO to be
    * done in each testsuite-integration-jee where it is required (cannot be done in this common jar because of deployment issue with EService)
    */
   DataPersistenceService getDataPersistenceService();
}
