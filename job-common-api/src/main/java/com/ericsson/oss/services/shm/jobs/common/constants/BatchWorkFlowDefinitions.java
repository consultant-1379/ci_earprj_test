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
package com.ericsson.oss.services.shm.jobs.common.constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains all the Batch workflow Definitions (All Old and New versions) . BATCH_WORKFLOW_ID_LATEST will always represent the latest Batch workflow. Old Batch workflows will be represented as
 * BATCH_WORKFLOW_ID_100, BATCH_WORKFLOW_ID_110 etc.
 * 
 * @author xnagvar
 */
public enum BatchWorkFlowDefinitions {

    BATCH_WORKFLOW_ID_100("BatchWorkFlow_v1.0.0"), BATCH_WORKFLOW_ID_110("BatchWorkFlow_v1.1.0"), BATCH_WORKFLOW_ID_LATEST("BatchWorkFlow_v1.1.1");

    String batchWorkFlowDefinition;

    private BatchWorkFlowDefinitions(final String batchWorkFlowDefinition) {
        this.batchWorkFlowDefinition = batchWorkFlowDefinition;
    }

    public String getBatchWorkFlowDefinition() {
        return this.batchWorkFlowDefinition;
    }

    public static List<String> getBatchWorkflowList() {
        final List<String> batchWorkflows = new ArrayList<String>();
        final BatchWorkFlowDefinitions[] neWorkFlowDefinitions = BatchWorkFlowDefinitions.values();
        for (final BatchWorkFlowDefinitions eachWorkflowDefintion : neWorkFlowDefinitions) {
            batchWorkflows.add(eachWorkflowDefintion.getBatchWorkFlowDefinition());
        }
        return batchWorkflows;
    }

}
