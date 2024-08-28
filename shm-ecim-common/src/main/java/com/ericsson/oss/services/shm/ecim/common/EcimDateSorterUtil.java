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
package com.ericsson.oss.services.shm.ecim.common;

import java.text.*;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility Class for Date Formating Operations for CPP date formats.
 * 
 * @author xmanush
 * 
 */
public abstract class EcimDateSorterUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(EcimDateSorterUtil.class);
    public static final String UTC_DATE_FORMAT = "Z";
    public static final String ECIM_DATE_FORMAT_1 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String ECIM_DATE_FORMAT_2 = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    public static final String ECIM_DATE_FORMAT_3 = "yyyy-MM-dd'T'HH:mm:ss";

    /**
     * This getAsDate expects String dateString to be in is08601 date format i.e ("2015-09-15T12:32:30.945+05:30" or "2015-09-15T12:32:30.945Z"). This method converts String to Ecim Specific
     * DateObject.
     * 
     * @param dateString
     * @return formatedDate
     */
    public static Date getFormatedDateForEcim(final String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        } else {
            final DateFormat formatter;
            if (dateString.contains(UTC_DATE_FORMAT)) {
                formatter = new SimpleDateFormat(ECIM_DATE_FORMAT_1);
            } else if (dateString.contains(".")) {
                formatter = new SimpleDateFormat(ECIM_DATE_FORMAT_2);
            } else {
                formatter = new SimpleDateFormat(ECIM_DATE_FORMAT_3);
            }
            try {
                final Date formatedDate = formatter.parse(dateString.substring(0, dateString.length()));
                return formatedDate;
            } catch (final ParseException e) {
                LOGGER.error("Exception while Parsing the String to Date in ECIM date sorter due to:{}", e);
                return null;
            }
        }
    }
}
