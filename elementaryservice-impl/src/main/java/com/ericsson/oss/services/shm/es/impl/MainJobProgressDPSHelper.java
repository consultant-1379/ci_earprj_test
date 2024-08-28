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
package com.ericsson.oss.services.shm.es.impl;

import java.util.*;

import javax.ejb.*;
import javax.inject.Inject;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

/**
 * This class is used retrieve and update main job progress in dps. This only contains dps calls.  
 * @author xkarrve
 * 
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class MainJobProgressDPSHelper {

    @Inject
    private DpsReader dpsReader;

    public void updateProgressAndProperties(final long mainJobId, final List<Map<String, String>> mainJobPropertyList, final double currentProgress,
                                        final double previousProgress, final boolean isPropertyUpdateNeeded) {
        final PersistenceObject mainJobPO = dpsReader.findPOByPoId(mainJobId);
        //For AXE upgrade job sometimes the main job progress might decrease from current progress as activityJob progress will decrease from current.
        if (currentProgress != previousProgress) {
            mainJobPO.setAttribute(ShmConstants.PROGRESSPERCENTAGE, currentProgress);
        }
        if (isPropertyUpdateNeeded) {
            mainJobPO.setAttribute(ShmConstants.JOBPROPERTIES, mainJobPropertyList);
        }
    }

    /**
     * To get Ne Job Progress Percentage.
     * @param mainJobId
     * @return neJobsProgress
     */
    public List<Map<String, Object>> getNeJobsProgress(final long mainJobId) {
        final Map<Object, Object> restrictionAttributes = new HashMap<Object, Object>();
        restrictionAttributes.put(ShmConstants.MAINJOBID, mainJobId);
        final List<Map<String, Object>> neJobsProgress = dpsReader.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.NE_JOB,
                restrictionAttributes, Arrays.asList(ShmConstants.PROGRESSPERCENTAGE));
        return neJobsProgress;
    }

    /**
     * To get main job attributes
     * 
     * @param mainJobId
     * @return map which contains no of network elements, progress percentage, job properties,job templateid only.
     */
    public Map<String, Object> getMainJobAttributes(final long mainJobId,final String... requiredAttributeNames) {
        final PersistenceObject mainJobPO = dpsReader.findPOByPoId(mainJobId);
        return mainJobPO.getAttributes(Arrays.asList(requiredAttributeNames));
    }

    public void updateMainJobAttributes(final long mainJobId, final Map<String, Object> mainJobAttributes) {
        final PersistenceObject persistenceObject = dpsReader.findPOByPoId(mainJobId);
        persistenceObject.setAttributes(mainJobAttributes);
    }

}
