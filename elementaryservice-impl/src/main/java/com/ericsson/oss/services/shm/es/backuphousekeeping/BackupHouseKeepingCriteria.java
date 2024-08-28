/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.backuphousekeeping;

import java.util.Map;

public class BackupHouseKeepingCriteria {

    private final Map<String, String> housekeepingCriteriaMap;

    public BackupHouseKeepingCriteria(final Map<String, String> housekeepingCriteriaMap) {
        this.housekeepingCriteriaMap = housekeepingCriteriaMap;
    }

    /**
     * @return the clearAllBackups
     */
    public boolean isCvPurgeRequested() {
        final String clearAllBackups = housekeepingCriteriaMap.get(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS);
        if (clearAllBackups != null && !clearAllBackups.isEmpty() && clearAllBackups.equalsIgnoreCase("TRUE")) {
            return true;
        }
        return false;
    }

    /**
     * @return the clearEligibleBackups
     */
    public boolean isCvCleanRequested() {
        final String clearEligibleBackups = housekeepingCriteriaMap.get(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS);
        if (clearEligibleBackups != null && !clearEligibleBackups.isEmpty() && clearEligibleBackups.equalsIgnoreCase("TRUE")) {
            return true;
        }
        return false;
    }

    public boolean isDeleteEligibleBackupsFromRollBackList() {
        String isDeleteEligibleBackupsFromRollBackList = housekeepingCriteriaMap.get(NodeBackupHousekeepingConstants.DELETE_ELIGIBLE_BACKUPS_FROM_ROLLBACK_LIST);
        return (isDeleteEligibleBackupsFromRollBackList != null && !isDeleteEligibleBackupsFromRollBackList.isEmpty() && isDeleteEligibleBackupsFromRollBackList.equalsIgnoreCase("TRUE"));
    }

    /**
     * @return the maxbackupsToKeepOnNode
     */
    public int getMaxbackupsToKeepOnNode() {
        final String maxBackupsToKeepOnNode = housekeepingCriteriaMap.get(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE);
        if (maxBackupsToKeepOnNode != null && !maxBackupsToKeepOnNode.isEmpty()) {
            return Integer.parseInt(maxBackupsToKeepOnNode);
        }
        return -1;
    }

    /**
     * @return the backupsToKeepInRollBackList
     */
    public int getBackupsToKeepInRollBackList() {
        final String backupsToKeepInRollBackList = housekeepingCriteriaMap.get(NodeBackupHousekeepingConstants.BACKUPS_TO_KEEP_IN_ROLLBACK_LIST);
        if (backupsToKeepInRollBackList != null && !backupsToKeepInRollBackList.isEmpty()) {
            return Integer.parseInt(backupsToKeepInRollBackList);
        }
        return -1;
    }

}
