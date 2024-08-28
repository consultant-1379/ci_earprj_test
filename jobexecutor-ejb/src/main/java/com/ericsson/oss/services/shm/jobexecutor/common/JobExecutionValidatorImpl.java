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
package com.ericsson.oss.services.shm.jobexecutor.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ericsson.oss.services.shm.activities.JobExecutionValidator;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.networkelement.NetworkElementResponse;

/**
 * Validating the DELETE_UPGRADEPACKAGE job is Skipping for the Unsupported Platforms
 * 
 * @author xneranu
 * 
 */
public class JobExecutionValidatorImpl implements JobExecutionValidator {

    @Override
    public Map<NetworkElement, String> findUnSupportedNEs(final JobTypeEnum jobType, final List<NetworkElement> networkElementList) {
        final Map<NetworkElement, String> msgnetworkElementMap = new HashMap<NetworkElement, String>();
        if (JobTypeEnum.DELETE_UPGRADEPACKAGE.toString().equals(jobType.name())) {
            for (final NetworkElement networkElement : networkElementList) {
                msgnetworkElementMap.put(networkElement, "Skipped " + jobType.name() + " Job for " + networkElement.getNeType() + " nodes , as not supported.");
            }
        }
        return msgnetworkElementMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.activities.JobExecutionValidator#findNesWithComponents(java.util.List, java.util.List, java.util.Map,
     * com.ericsson.oss.services.shm.networkelement.NetworkElementResponse)
     */
    @Override
    public Map<String, List<NetworkElement>> findNesWithComponents(final JobTypeEnum jobTypeEnum, final List<NetworkElement> platformSpecificNEList,
            final List<Map<String, Object>> nesWithComponentInfo, final Map<String, String> neDetailsWithParentName, final NetworkElementResponse networkElementResponse,
            final boolean flagForValidateNes) {
        return null;
    }

}
