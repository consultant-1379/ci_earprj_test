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

public enum LatestNeWorkFlowDefinitions {

    CPP_UPGRADE_WORKFLOW_DEF_ID("CPP_UPGRADE_wf_v2.0.0"),
    CPP_BACKUP_WORKFLOW_DEF_ID("CPP_BACKUP_wf_v2.0.0"),
    CPP_LICENSE_WORKFLOW_DEF_ID("CPP_LICENSE_wf_v2.0.0"),
    CPP_DELETE_CV_WORKFLOW_DEF_ID("CPP_DELETE_CV_wf_v2.0.0"),
    CPP_RESTORE_WORKFLOW_DEF_ID("CPP_RESTORE_wf_v2.0.0"),
    CPP_NODERESTART_WORKFLOW_DEF_ID("CPP_NODE_RESTART_wf_v2.0.0"),
    CPP_DELETE_UPGRADEPACKAGE_WORKFLOW_DEF_ID("CPP_DELETE_UPGRADEPACKAGE_wf_v1.0.0"),
    CPP_NODE_HEALTH_CHECK_WORKFLOW_DEF_ID("CPP_NODE_HEALTH_CHECK_wf_v1.1.0"),
    ECIM_BACKUP_WORKFLOW_DEF_ID("ECIM_BACKUP_wf_v2.0.0"),
    ECIM_UPGRADE_WORKFLOW_DEF_ID("ECIM_UPGRADE_wf_v2.0.0"),
    ECIM_DELETE_BACKUP_WORKFLOW_DEF_ID("ECIM_DELETE_BACKUP_wf_v2.0.0"),
    ECIM_LICENSE_WORKFLOW_DEF_ID("ECIM_LICENSE_wf_v2.0.0"),
    ECIM_RESTORE_WORKFLOW_DEF_ID("ECIM_RESTORE_wf_v3.0.0"),
    CPP_BACKUP_HOUSEKEEPING_WORKFLOW_DEF_ID("CPP_CV_HOUSEKEEPING_wf_v1.0.0"),
    ECIM_BACKUP_HOUSEKEEPING_WORKFLOW_DEF_ID("ECIM_BACKUP_HOUSEKEEPING_wf_v1.0.0"),
    ECIM_DELETE_UPGRADEPACKAGE_WORKFLOW_DEF_ID("ECIM_DELETE_UPGRADEPACKAGE_wf_v1.0.0"),
    ECIM_NODE_HEALTH_CHECK_WORKFLOW_DEF_ID("ECIM_NODE_HEALTH_CHECK_wf_v1.1.0"),
    ECIM_LICENSE_REFRESH_WORKFLOW_DEF_ID("ECIM_LICENSE_REFRESH_wf_v1.0.0"),
    MINI_LINK_INDOOR_UPGRADE_DEF_ID("MINI_LINK_INDOOR_UPGRADE_wf_v1.0.0"),
    MINI_LINK_INDOOR_BACKUP_DEF_ID("MINI_LINK_INDOOR_BACKUP_wf_v1.0.0"),
    MINI_LINK_INDOOR_RESTORE_DEF_ID("MINI_LINK_INDOOR_RESTORE_wf_v1.0.0"),
    MINI_LINK_INDOOR_DELETEBACKUP_DEF_ID("MINI_LINK_INDOOR_DELETEBACKUP_wf_v1.0.0"),
    MINI_LINK_INDOOR_LICENSE_DEF_ID("MINI_LINK_INDOOR_LICENSE_wf_v1.0.0"),
    MINI_LINK_OUTDOOR_LICENSE_DEF_ID("MINI_LINK_OUTDOOR_LICENSE_wf_v1.0.0"),
    VRAN_UPGRADE_WORKFLOW_DEF_ID("VRAN_UPGRADE_wf_v1.0.0"),
    VRAN_ONBOARD_WORKFLOW_DEF_ID("VRAN_ONBOARD_wf_v1.0.0"),
    STN_UPGRADE_WORKFLOW_DEF_ID("STN_UPGRADE_wf_v1.0.0"),
    VRAN_DELETE_SOFTWAREPACKAGE_WORKFLOW_DEF_ID("VRAN_DELETE_SOFTWAREPACKAGE_wf_v1.0.0"),
    MINI_LINK_OUTDOOR_BACKUP_WORKFLOW_DEF_ID("MINI_LINK_OUTDOOR_BACKUP_wf_v1.0.0"),
    MINI_LINK_OUTDOOR_UPGRADE_DEF_ID("MINI_LINK_OUTDOOR_UPGRADE_wf_v1.0.0"),
    MINI_LINK_OUTDOOR_RESTORE_WORKFLOW_DEF_ID("MINI_LINK_OUTDOOR_RESTORE_wf_v1.0.0"),
    MINI_LINK_OUTDOOR_DELETEBACKUP_WORKFLOW_DEF_ID("MINI_LINK_OUTDOOR_DELETEBACKUP_wf_v1.0.0"),
    AXE_UPGRADE_DEF_ID("AXE_UPGRADE_wf_v1.0.0"),
    AXE_BACKUP_DEF_ID("AXE_BACKUP_wf_v1.0.0"),
    AXE_LICENSE_DEF_ID("AXE_LICENSE_wf_v1.0.0"),
    AXE_DELETE_DEF_ID("AXE_DELETE_BACKUP_wf_v1.0.0");

    String neWorkFlowDefinition;

    private LatestNeWorkFlowDefinitions(final String neWorkFlowDefinition) {
        this.neWorkFlowDefinition = neWorkFlowDefinition;
    }

    public static List<String> getNeWorkflowList() {
        final List<String> neWorkflows = new ArrayList<String>();
        final LatestNeWorkFlowDefinitions[] neWorkFlowDefinitions = LatestNeWorkFlowDefinitions.values();
        for (final LatestNeWorkFlowDefinitions eachWorkflowDefintion : neWorkFlowDefinitions) {
            neWorkflows.add(eachWorkflowDefintion.getWorkFlowDefinition());
        }
        return neWorkflows;
    }

    public String getWorkFlowDefinition() {
        return this.neWorkFlowDefinition;
    }
}
