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
package com.ericsson.oss.services.shm.jobs.common.api;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

/**
 * This class is used to check if shm job is scheduled periodic or not.
 * 
 * @author xamtsih
 * 
 */

public class CheckPeriodicity {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckPeriodicity.class);

    public boolean isJobPeriodic(final List<Map<String, Object>> schedulePropertiesList) {
        boolean isJobPeriodic = false;
        for (final Map<String, Object> scheduleProperty : schedulePropertiesList) {
            if ((JobPropertyConstants.REPEAT_TYPE.equalsIgnoreCase((String) scheduleProperty.get(ShmConstants.NAME)))
                    || (JobPropertyConstants.CRON_EXP.equalsIgnoreCase((String) scheduleProperty.get(ShmConstants.NAME)))) {
                isJobPeriodic = true;
            }
        }
        LOGGER.debug("value for isJobPeriodic {}", isJobPeriodic);
        return isJobPeriodic;
    }
}
