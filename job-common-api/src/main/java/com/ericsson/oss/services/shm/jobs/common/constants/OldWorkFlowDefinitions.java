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

public enum OldWorkFlowDefinitions {

    CPP_UPGRADE_WORKFLOW_DEF_ID("CPP_UPGRADE_wf_v1.0.0"), CPP_BACKUP_WORKFLOW_DEF_ID("CPP_BACKUP_wf_v1.0.0"), CPP_LICENSE_WORKFLOW_DEF_ID("CPP_LICENSE_wf_v1.0.0"), CPP_DELETE_CV_WORKFLOW_DEF_ID(
            "CPP_DELETE_CV_wf_v1.0.0"), CPP_RESTORE_WORKFLOW_DEF_ID("CPP_RESTORE_wf_v1.0.0"), ECIM_BACKUP_WORKFLOW_DEF_ID("ECIM_BACKUP_wf_v1.0.0"), ECIM_UPGRADE_WORKFLOW_DEF_ID(
            "ECIM_UPGRADE_wf_v1.0.0"), ECIM_DELETE_BACKUP_WORKFLOW_DEF_ID("ECIM_DELETE_BACKUP_wf_v1.0.0"), ECIM_LICENSE_WORKFLOW_DEF_ID("ECIM_LICENSE_wf_v1.0.0"), ECIM_RESTORE_WORKFLOW_DEF_ID(
            "ECIM_RESTORE_wf_v2.0.0"), CPP_NODERESTART_WORKFLOW_DEF_ID("CPP_NODE_RESTART_wf_v1.0.0");

    String oldWorkFlowDefinition;

    private OldWorkFlowDefinitions(final String oldWorkFlowDefinition) {
        this.oldWorkFlowDefinition = oldWorkFlowDefinition;
    }

    public String getOldWorkFlowDefinition() {
        return this.oldWorkFlowDefinition;
    }

    public static List<String> getOldWorkflowList() {
        final List<String> oldWorkflows = new ArrayList<String>();
        final OldWorkFlowDefinitions[] oldWorkFlowDefinitions = OldWorkFlowDefinitions.values();
        for (final OldWorkFlowDefinitions eachWorkflowDefintion : oldWorkFlowDefinitions) {
            oldWorkflows.add(eachWorkflowDefintion.getOldWorkFlowDefinition());
        }
        return oldWorkflows;
    }

}
