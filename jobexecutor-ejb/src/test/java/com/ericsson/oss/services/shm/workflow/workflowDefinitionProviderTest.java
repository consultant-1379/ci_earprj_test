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
package com.ericsson.oss.services.shm.workflow;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.activities.WorkflowDefinitionsProvider;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

@RunWith(MockitoJUnitRunner.class)
public class workflowDefinitionProviderTest {

    @InjectMocks
    private WorkflowDefinitionsProvider objectUnderTest;

    @Test
    public void test() {

        final String response = objectUnderTest.getWorkflowDefinition(PlatformTypeEnum.CPP, JobTypeEnum.NODE_HEALTH_CHECK);
        Assert.assertEquals("CPP_NODE_HEALTH_CHECK_wf_v1.1.0", response);
    }

}
