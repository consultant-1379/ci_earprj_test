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
package com.ericsson.oss.services.shm.activities;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.constants.LatestNeWorkFlowDefinitions;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

@ApplicationScoped
public class WorkflowDefinitionsProvider {

    private static final Map<String, String> WFS_DEFINITION_IDS = new HashMap<String, String>();
    private static final String DELIMETER_UNDERSCORE = "_";

    static {
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE, LatestNeWorkFlowDefinitions.CPP_UPGRADE_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP, LatestNeWorkFlowDefinitions.CPP_BACKUP_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.LICENSE, LatestNeWorkFlowDefinitions.CPP_LICENSE_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE, LatestNeWorkFlowDefinitions.CPP_RESTORE_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.DELETEBACKUP, LatestNeWorkFlowDefinitions.CPP_DELETE_CV_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.NODE_HEALTH_CHECK, LatestNeWorkFlowDefinitions.CPP_NODE_HEALTH_CHECK_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP_HOUSEKEEPING,
                LatestNeWorkFlowDefinitions.CPP_BACKUP_HOUSEKEEPING_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.DELETE_UPGRADEPACKAGE,
                LatestNeWorkFlowDefinitions.CPP_DELETE_UPGRADEPACKAGE_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + JobTypeEnum.NODERESTART, LatestNeWorkFlowDefinitions.CPP_NODERESTART_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP, LatestNeWorkFlowDefinitions.ECIM_BACKUP_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE, LatestNeWorkFlowDefinitions.ECIM_UPGRADE_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.DELETEBACKUP, LatestNeWorkFlowDefinitions.ECIM_DELETE_BACKUP_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.LICENSE, LatestNeWorkFlowDefinitions.ECIM_LICENSE_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE, LatestNeWorkFlowDefinitions.ECIM_RESTORE_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP_HOUSEKEEPING,
                LatestNeWorkFlowDefinitions.ECIM_BACKUP_HOUSEKEEPING_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.DELETE_UPGRADEPACKAGE,
                LatestNeWorkFlowDefinitions.ECIM_DELETE_UPGRADEPACKAGE_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.NODE_HEALTH_CHECK,
                LatestNeWorkFlowDefinitions.ECIM_NODE_HEALTH_CHECK_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.ECIM + DELIMETER_UNDERSCORE + JobTypeEnum.LICENSE_REFRESH, LatestNeWorkFlowDefinitions.ECIM_LICENSE_REFRESH_WORKFLOW_DEF_ID.getWorkFlowDefinition());

        WFS_DEFINITION_IDS.put(PlatformTypeEnum.MINI_LINK_INDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE, LatestNeWorkFlowDefinitions.MINI_LINK_INDOOR_UPGRADE_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.MINI_LINK_INDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP, LatestNeWorkFlowDefinitions.MINI_LINK_INDOOR_BACKUP_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.MINI_LINK_INDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE, LatestNeWorkFlowDefinitions.MINI_LINK_INDOOR_RESTORE_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.MINI_LINK_INDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.DELETEBACKUP,
                LatestNeWorkFlowDefinitions.MINI_LINK_INDOOR_DELETEBACKUP_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.MINI_LINK_INDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.LICENSE, LatestNeWorkFlowDefinitions.MINI_LINK_INDOOR_LICENSE_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.vRAN + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE, LatestNeWorkFlowDefinitions.VRAN_UPGRADE_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.vRAN + DELIMETER_UNDERSCORE + JobTypeEnum.ONBOARD, LatestNeWorkFlowDefinitions.VRAN_ONBOARD_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.vRAN + DELIMETER_UNDERSCORE + JobTypeEnum.DELETE_SOFTWAREPACKAGE,
                LatestNeWorkFlowDefinitions.VRAN_DELETE_SOFTWAREPACKAGE_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.STN + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE, LatestNeWorkFlowDefinitions.STN_UPGRADE_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.MINI_LINK_OUTDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP, LatestNeWorkFlowDefinitions.MINI_LINK_OUTDOOR_BACKUP_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.MINI_LINK_OUTDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.LICENSE, LatestNeWorkFlowDefinitions.MINI_LINK_OUTDOOR_LICENSE_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.MINI_LINK_OUTDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.RESTORE, LatestNeWorkFlowDefinitions.MINI_LINK_OUTDOOR_RESTORE_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.MINI_LINK_OUTDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE, LatestNeWorkFlowDefinitions.MINI_LINK_OUTDOOR_UPGRADE_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.MINI_LINK_OUTDOOR + DELIMETER_UNDERSCORE + JobTypeEnum.DELETEBACKUP, LatestNeWorkFlowDefinitions.MINI_LINK_OUTDOOR_DELETEBACKUP_WORKFLOW_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.AXE + DELIMETER_UNDERSCORE + JobTypeEnum.UPGRADE, LatestNeWorkFlowDefinitions.AXE_UPGRADE_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.AXE + DELIMETER_UNDERSCORE + JobTypeEnum.BACKUP, LatestNeWorkFlowDefinitions.AXE_BACKUP_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.AXE + DELIMETER_UNDERSCORE + JobTypeEnum.LICENSE, LatestNeWorkFlowDefinitions.AXE_LICENSE_DEF_ID.getWorkFlowDefinition());
        WFS_DEFINITION_IDS.put(PlatformTypeEnum.AXE + DELIMETER_UNDERSCORE + JobTypeEnum.DELETEBACKUP, LatestNeWorkFlowDefinitions.AXE_DELETE_DEF_ID.getWorkFlowDefinition());
    }

    public String getWorkflowDefinition(final PlatformTypeEnum platform, final JobTypeEnum jobType) {
        final String key = platform + DELIMETER_UNDERSCORE + jobType;
        return WFS_DEFINITION_IDS.containsKey(key) ? WFS_DEFINITION_IDS.get(key) : null;
    }

}
