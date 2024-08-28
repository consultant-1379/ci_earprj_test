/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl;

import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

/**
 * Calculates the Main Job progress
 * 
 * @author xrajeke
 * 
 */
@Stateless
public class MainJobProgressCalculator {

    final static private Logger LOGGER = LoggerFactory.getLogger(MainJobProgressCalculator.class);

    /**
     * Retrieves All the individual NE job's progress percentage and calculates the aggregate progress to be updated for Main job.
     * 
     * @param mainJobId
     * @param numberOfNes
     * @param neJobsProgress
     * @return aggregateProgressPercentage
     */
    public double calculateMainJobProgressPercentage(final long mainJobId, final int numberOfNes, final List<Map<String, Object>> neJobsProgress) {
        LOGGER.trace("Total {} valid Network Element(s) present under the Main job:{}", numberOfNes, mainJobId);
        final double cumulativeProgress = getCumulativeProgress(neJobsProgress);
        double mainJobProgress = cumulativeProgress / numberOfNes;
        mainJobProgress = Math.round(mainJobProgress * 100);
        mainJobProgress = mainJobProgress / 100;
        return mainJobProgress;
    }

    /**
     * @param neJobsProgress
     * @return
     */
    private double getCumulativeProgress(final List<Map<String, Object>> neJobsProgress) {
        double cumulativeProgress = 0;
        for (final Map<String, Object> neJobProgressMap : neJobsProgress) {
            //Every NEJob contains PROGRESSPERCENTAGE as 0.0 by default, so definitely this property will be found.
                final double neJobProgressPercentage = (double) neJobProgressMap.get(ShmConstants.PROGRESSPERCENTAGE);
                cumulativeProgress = cumulativeProgress + neJobProgressPercentage;
        }
        return cumulativeProgress;
    }

}
