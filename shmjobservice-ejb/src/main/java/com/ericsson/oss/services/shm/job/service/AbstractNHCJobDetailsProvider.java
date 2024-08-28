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
package com.ericsson.oss.services.shm.job.service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

import javax.inject.Inject;

import com.ericsson.oss.services.shm.job.utils.ActivityParamMapper;




/**
 * This class is to get the job configuration details of Node health check Job when ECIM & CPP specific node(s) selected, which has to be displayed as Node Activities under Configuration Details panel of
 * Job summary on nhc main page 
 * @author zindsri
 *
 */

public abstract class AbstractNHCJobDetailsProvider implements JobTypeDetailsProvider {

    @Inject
    protected ActivityParamMapper activityParamMapper;

    private static final String NODE_HEALTH_CHECK = "nodehealthcheck";
    private static final String ENM_HEALTH_CHECK = "enmhealthcheck";

    protected static final Map<String, List<String>> acitvityParameters = new HashMap<>();

    static {
        acitvityParameters.put(NODE_HEALTH_CHECK, Arrays.asList("NODE_HEALTH_CHECK_TEMPLATE"));
        acitvityParameters.put(ENM_HEALTH_CHECK, Arrays.asList("ENM_HEALTH_CHECK_RULES"));
    }


}
