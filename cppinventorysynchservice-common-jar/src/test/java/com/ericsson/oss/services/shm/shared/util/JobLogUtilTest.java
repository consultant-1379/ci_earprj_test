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
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

/**
 * Test class for JobLogUtil.
 * 
 * @author xnitpar
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class JobLogUtilTest {

    @InjectMocks
    JobLogUtil objectUnderTest;

    @Test
    public void testPrepareJobLogAtrributesList() {
        final List<Map<String, Object>> jobList = new ArrayList<Map<String, Object>>();
        objectUnderTest.prepareJobLogAtrributesList(jobList, "activity log message", new Date(), "log type", "INFO");
        assertEquals(1, jobList.size());
        assertEquals("activity log message", jobList.get(0).get(ActivityConstants.JOB_LOG_MESSAGE));
        assertEquals("log type", jobList.get(0).get(ActivityConstants.JOB_LOG_TYPE));
    }

}
