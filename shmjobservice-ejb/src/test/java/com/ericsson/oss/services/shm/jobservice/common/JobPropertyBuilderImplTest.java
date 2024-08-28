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
package com.ericsson.oss.services.shm.jobservice.common;

import static org.junit.Assert.assertNotNull;

import java.util.*;

import org.junit.Test;
import org.mockito.InjectMocks;

public class JobPropertyBuilderImplTest {
    /**
     * Test method for {@link com.ericsson.oss.services.shm.jobservice.common.BackupJobConfigParams#buildJobPropertiesObject(java.util.List, com.ericsson.oss.services.shm.jobservice.common.JobInfo)} .
     */

    @InjectMocks
    JobPropertyBuilderImpl jobPropertyBuilderImpl = new JobPropertyBuilderImpl();

    @Test
    public void testaddPlatformJobProperties() {
        final JobInfo jobInfo = new JobInfo();
        final Map<String, Object> configurationsMap = new HashMap<String, Object>();
        final List<Map<String, Object>> propertiesMapList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> propertiesMap = new HashMap<String, Object>();
        propertiesMap.put("key", "keyname");
        propertiesMap.put("value", "valuename");
        propertiesMapList.add(propertiesMap);
        configurationsMap.put("platform", "CPP");
        configurationsMap.put("properties", propertiesMapList);
        jobPropertyBuilderImpl.populateJobConfiguration(jobInfo, configurationsMap);
        assertNotNull(jobInfo);
        assertNotNull(jobInfo.getPlatformJobProperties());
    }

    @Test
    public void testaddNeTypeJobProperties() {
        final JobInfo jobInfo = new JobInfo();
        final Map<String, Object> configurationsMap = new HashMap<String, Object>();
        final List<Map<String, Object>> propertiesMapList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> propertiesMap = new HashMap<String, Object>();
        propertiesMap.put("key", "SWP_NAME");
        propertiesMap.put("value", "CXP102051_9_R4D27");
        propertiesMap.put("key", "UCF");
        propertiesMap.put("value", "CXP102051%1_R4D25.xml");
        propertiesMapList.add(propertiesMap);
        configurationsMap.put("neType", "ERBS");
        configurationsMap.put("properties", propertiesMapList);
        jobPropertyBuilderImpl.populateJobConfiguration(jobInfo, configurationsMap);
        assertNotNull(jobInfo);
        assertNotNull(jobInfo.getNETypeJobProperties());
    }

    @Test
    public void testaddNeJobProperties() {
        final JobInfo jobInfo = new JobInfo();
        final List<Map<String, Object>> configurationsMapList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> configurationsMapListwithNe = new HashMap<String, Object>();
        configurationsMapListwithNe.put("neNames", "LTEERBS211");
        final Map<String, Object> configurationsMap = new HashMap<String, Object>();
        final List<Map<String, Object>> propertiesMapList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> propertiesMap = new HashMap<String, Object>();
        propertiesMap.put("key", "keyname");
        propertiesMap.put("value", "valuename");
        propertiesMapList.add(propertiesMap);
        configurationsMapListwithNe.put("properties", propertiesMapList);
        configurationsMapList.add(configurationsMapListwithNe);
        configurationsMap.put("neType", "ERBS");
        configurationsMap.put("neProperties", configurationsMapList);
        jobPropertyBuilderImpl.populateJobConfiguration(jobInfo, configurationsMap);
        assertNotNull(jobInfo);
        assertNotNull(jobInfo.getNeJobProperties());
    }
}
