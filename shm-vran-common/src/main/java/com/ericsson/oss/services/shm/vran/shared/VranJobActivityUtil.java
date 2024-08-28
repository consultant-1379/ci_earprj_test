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
package com.ericsson.oss.services.shm.vran.shared;

import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for VRAN jobs
 * 
 * @author xindkag
 */
public class VranJobActivityUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(VranJobActivityUtil.class);

    /**
     * Method to split pipe separated software packages string into individual software packages
     * 
     * @param neFdns
     * @return propertyValue
     */
    public String[] splitSoftwarePackages(final String softwarePackages) {
        LOGGER.trace("Splitting software packages: {}", softwarePackages);
        String packages[] = {};
        if (softwarePackages != null && !softwarePackages.isEmpty()) {
            packages = softwarePackages.split("\\|");
        }
        return packages;
    }

    /**
     * Method to increment time by milliseconds, this is to avoid swap of job logs when multiple job logs are persisted at a time.
     * 
     * @param initialTime
     * @param milliSeconds
     * @return
     */
    public Calendar incrementTime(final Date initialTime, final int milliSeconds) {
        final Calendar calendar = Calendar.getInstance();
        if (initialTime != null) {
            calendar.setTime(initialTime);
        }
        calendar.add(Calendar.MILLISECOND, milliSeconds);
        return calendar;
    }
}
