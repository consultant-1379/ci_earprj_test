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
package com.ericsson.oss.services.shm.es.impl.cpp.backuphousekeeping;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.backuphousekeeping.BackupSortException;
import com.ericsson.oss.services.shm.es.backuphousekeeping.NodeBackupHousekeepingConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;

public class StoredConfigurationListComparator implements Comparator<Map<String, String>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoredConfigurationListComparator.class);

    private static final ThreadLocal<SimpleDateFormat> dateFormat = new ThreadLocal<SimpleDateFormat>() {

        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat(NodeBackupHousekeepingConstants.SIMPLEDATE_FORMAT);
        }
    };

    @Override
    public final int compare(final Map map1, final Map map2) {
        Date date1 = null, date2 = null;
        try {
            if (map1.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE) != null
                    && !map1.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE).toString().isEmpty()) {
                date1 = dateFormat.get().parse((String) map1.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE));
            } else {
                date1 = new Date(0);
            }
            if (map2.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE) != null
                    && !map2.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE).toString().isEmpty()) {
                date2 = dateFormat.get().parse((String) map2.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE));
            } else {
                date2 = new Date(0);
            }
        } catch (final ParseException parseException) {
            LOGGER.debug("Failed to parse the date while sorting StoredConfigurationVersionList");
            throw new BackupSortException(parseException.getMessage());
        }
        if (date1.compareTo(date2) > 0) {
            return +1;
        } else if (date1.compareTo(date2) == 0) {
            return 0;
        } else {
            return -1;
        }
    }

    public void cleanUp() {
        dateFormat.remove();
    }

}
