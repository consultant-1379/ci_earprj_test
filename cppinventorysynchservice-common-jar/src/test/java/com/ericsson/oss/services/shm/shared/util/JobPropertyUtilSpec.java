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
package com.ericsson.oss.services.shm.shared.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Test class for JobPropertyUtil.
 * 
 * @author xaniama
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class JobPropertyUtilSpec {

    @InjectMocks
    JobPropertyUtil objectUnderTest;

    @Test
    public void testUpdateProperty() {
        List<Map<String, String>> jobProperties = new ArrayList<>();
        Map<String, String> passwordProp = new HashMap<>();
        passwordProp.put("key", "Password");
        passwordProp.put("value", "12345");
        jobProperties.add(passwordProp);
        objectUnderTest.updateJobProperty(jobProperties, "Password", "EeMxO35BZ1fE+0GfrfK2qA16OB98hKNK9j7DPzj2b84=");
        for (Map<String, String> eachJobProp : jobProperties) {
            for (Map.Entry map : eachJobProp.entrySet()) {
                if (map.getKey().equals("Password")) {
                    assertEquals("EeMxO35BZ1fE+0GfrfK2qA16OB98hKNK9j7DPzj2b84=", map.getValue());
                }
            }
        }
    }

}
