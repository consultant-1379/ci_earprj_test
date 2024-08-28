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
package com.ericsson.oss.services.shm.activity.timeout.models;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.jobs.common.constants.LatestNeWorkFlowDefinitions;

@RunWith(MockitoJUnitRunner.class)
public class LatestNeWorkflowDefinitionsTest {

    private LatestNeWorkFlowDefinitions objectUnderTest;
    private final int NeFlowsSize = 36;

    @Test
    public void test() {

        final List<String> NeFlows = objectUnderTest.getNeWorkflowList();

        Assert.assertTrue(NeFlows.contains("CPP_NODE_HEALTH_CHECK_wf_v1.1.0"));
        Assert.assertEquals(NeFlowsSize, NeFlows.size());

    }

}
