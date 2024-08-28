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
package com.ericsson.oss.services.shm.system.restore.common;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;
import com.ericsson.oss.services.wfs.api.query.Query;
import com.ericsson.oss.services.wfs.api.query.QueryBuilder;
import com.ericsson.oss.services.wfs.api.query.WorkflowObject;

@RunWith(MockitoJUnitRunner.class)
public class WorkFlowQueryServiceImplTest {

    @InjectMocks
    WorkFlowQueryServiceImpl workFlowQueryServiceImplMock;

    @Mock
    WorkflowInstanceNotifier workflowQueryServiceLocal;

    @Mock
    WorkflowObject workflowObjectMock;

    @Mock
    QueryBuilder wfsQueryBuilderMock;

    @Mock
    Query wfsQueryMock;

    @Test
    public void testGetSuspendedBatchWorkflows() {

        when(workflowQueryServiceLocal.executeWorkflowQuery(org.mockito.Matchers.any(Query.class))).thenReturn(Arrays.asList(workflowObjectMock));
        final List<WorkflowObject> batchWorkFlowList = workFlowQueryServiceImplMock.getSuspendedBatchWorkflows();
        assertNotNull(batchWorkFlowList);
        assertTrue(batchWorkFlowList.size() == 3);
    }

    @Test
    public void testGetSuspendedNEWorkflows() {

        when(workflowQueryServiceLocal.executeWorkflowQuery(org.mockito.Matchers.any(Query.class))).thenReturn(Arrays.asList(workflowObjectMock));
        workFlowQueryServiceImplMock.getSuspendedNEWorkflows();
    }

}
