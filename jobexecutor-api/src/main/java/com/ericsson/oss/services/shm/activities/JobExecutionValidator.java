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
package com.ericsson.oss.services.shm.activities;

import java.util.List;
import java.util.Map;

import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.networkelement.NetworkElementResponse;

/**
 * Interface to define the validations specific to platform type and job type on the Network Elements selected for a job.
 * 
 * @author xyerrsr
 * 
 */
public interface JobExecutionValidator {
    Map<NetworkElement, String> findUnSupportedNEs(JobTypeEnum jobType, List<NetworkElement> networkElementList);

    /**
     * This method find selected NEs for which components are selected by User during upgrade job creation and prepare Network element data for selected components
     * 
     * @param platformSpecificNEList
     * @param nesWithComponentInfo
     * @param neDetailsWithParentName
     * @param networkElementResponse
     * @return
     */
    Map<String, List<NetworkElement>> findNesWithComponents(final JobTypeEnum jobTypeEnum, List<NetworkElement> platformSpecificNEList, List<Map<String, Object>> nesWithComponentInfo,
            Map<String, String> neDetailsWithParentName, NetworkElementResponse networkElementResponse, final boolean flagForValidateNes);
}
