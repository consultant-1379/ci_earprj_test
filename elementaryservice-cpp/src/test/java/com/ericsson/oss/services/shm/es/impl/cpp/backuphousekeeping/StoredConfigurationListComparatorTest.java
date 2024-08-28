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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.es.backuphousekeeping.BackupSortException;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;

@RunWith(MockitoJUnitRunner.class)
public class StoredConfigurationListComparatorTest {

    @Test
    public void sortStoredConfigurationListTest() {
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion1 = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion2 = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test|NODE");
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2004");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test1|NODE1");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2012");
        storedConfigurationVersion2.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test2|NODE2");
        storedConfigurationVersion2.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2007");
        storedConfigurationVersionList.add(storedConfigurationVersion);
        storedConfigurationVersionList.add(storedConfigurationVersion1);
        storedConfigurationVersionList.add(storedConfigurationVersion2);
        Collections.sort(storedConfigurationVersionList, new StoredConfigurationListComparator());
        assertEquals(storedConfigurationVersionList.get(0), storedConfigurationVersion);
        assertEquals(storedConfigurationVersionList.get(1), storedConfigurationVersion2);
        assertEquals(storedConfigurationVersionList.get(2), storedConfigurationVersion1);
    }

    @Test
    public void sortStoredConfigurationListTestWithoutDate() {
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion1 = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion2 = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion3 = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test|NODE");
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2004");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test1|NODE1");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2012");
        storedConfigurationVersion2.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test2|NODE2");
        storedConfigurationVersion2.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "Thu Jun 21 17:32:05 2007");
        storedConfigurationVersion3.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test3|NODE3");
        storedConfigurationVersion3.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "");
        storedConfigurationVersionList.add(storedConfigurationVersion);
        storedConfigurationVersionList.add(storedConfigurationVersion1);
        storedConfigurationVersionList.add(storedConfigurationVersion2);
        storedConfigurationVersionList.add(storedConfigurationVersion3);
        Collections.sort(storedConfigurationVersionList, new StoredConfigurationListComparator());
        assertEquals(storedConfigurationVersionList.get(0), storedConfigurationVersion3);
        assertEquals(storedConfigurationVersionList.get(1), storedConfigurationVersion);
        assertEquals(storedConfigurationVersionList.get(2), storedConfigurationVersion2);
        assertEquals(storedConfigurationVersionList.get(3), storedConfigurationVersion1);
    }

    @Test
    public void sortStoredConfigurationListParseExceptionTest() {
        boolean SortException = false;
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion1 = new HashMap<String, String>();
        final Map<String, String> storedConfigurationVersion2 = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test|NODE");
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "21/01/2007 17:32:05");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test1|NODE1");
        storedConfigurationVersion1.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "21/01/2004 17:32:05");
        storedConfigurationVersion2.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "cv_test2|NODE2");
        storedConfigurationVersion2.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, "21/01/2009 17:32:05");
        storedConfigurationVersionList.add(storedConfigurationVersion);
        storedConfigurationVersionList.add(storedConfigurationVersion1);
        storedConfigurationVersionList.add(storedConfigurationVersion2);
        try {
            Collections.sort(storedConfigurationVersionList, new StoredConfigurationListComparator());
        } catch (final BackupSortException cvSortException) {
            SortException = true;
        }
        assertTrue(SortException);
    }
}
