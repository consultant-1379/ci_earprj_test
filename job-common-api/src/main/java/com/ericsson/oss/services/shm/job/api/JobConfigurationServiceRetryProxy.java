/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.api;

import java.util.List;
import java.util.Map;

/**
 * This interface provides the services to get the job configuration data.
 * 
 * @author tcsgusw
 * 
 */
public interface JobConfigurationServiceRetryProxy {

    /**
     * Fetch activityJob attributes by activityJobId.
     * 
     * @param activityJobId
     * @return
     */
    Map<String, Object> getActivityJobAttributes(final long activityJobId);

    /**
     * Fetch NEJob attributes by neJobId.
     * 
     * @param neJobId
     * @return
     */

    Map<String, Object> getNeJobAttributes(final long neJobId);

    /**
     * Fetch main Job attributes by mainJobId.
     * 
     * @param mainJobId
     * @return
     */

    Map<String, Object> getMainJobAttributes(final long mainJobId);

    /**
     * Fetch PO attributes by poId.
     * 
     * @param poId
     * @return
     */

    Map<String, Object> getPOAttributes(final long poId);

    /**
     * Fetch projected attributes by taking given inputs.
     * 
     * @param namespace
     * @param type
     * @param restrictions
     * @param reqdAttributes
     * @return
     */

    List<Map<String, Object>> getProjectedAttributes(final String namespace, final String type, final Map<Object, Object> restrictions, final List<String> reqdAttributes);

    List<Long> getJobPoIdsFromParentJobId(final long neJobPoId, final String typeOfJob, final String restrictionAttribute);

    /**
     * Returns activity Job attributes by neJobId.
     * 
     * @param neJobId
     * @param restrictions
     * @return
     */
    List<Map<String, Object>> getActivityJobAttributesByNeJobId(final long neJobId, final Map<String, Object> restrictions);
}
