/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.api;

import java.util.Map;

import javax.ejb.Remote;

import com.ericsson.oss.itpf.sdk.core.annotation.EService;

/**
 * Consolidation service consolidates and evaluate activity data i.e rules count and health status and the same data will be collected and persisted in NE job before NE job completed.
 * 
 * @author xkalkil
 *
 */
@EService
@Remote
public interface JobConsolidationService {

    Map<String, Object> consolidateNeJobData(final long neJobId);

}
