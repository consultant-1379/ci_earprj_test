/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.jobexecutorlocal;

import java.util.List;
import java.util.Map;

import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.networkelement.NetworkElementResponse;

/**
 * 
 * @author xeswpot
 * 
 */
public interface TargetResolver {

    NetworkElementResponse getNetworkElementResponse(final long mainJobId, final List<String> neNames, final long templateJobId, final Map<String, Object> attributeMap, final JobTypeEnum jobType,
            final boolean isMainJobCreated);

}
