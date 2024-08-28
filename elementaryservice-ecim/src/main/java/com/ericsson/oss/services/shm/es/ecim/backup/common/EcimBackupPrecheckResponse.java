/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.ecim.backup.common;

import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.common.PrecheckResponse;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.model.NetworkElementData;

/**
 * This class holds the minimal data required to perform Pre Validation and to trigger an action for ECIM backup activities.
 * 
 * @author tcsgusw
 * 
 */
public class EcimBackupPrecheckResponse extends PrecheckResponse {

    private final EcimBackupInfo ecimBackupInfo;

    private final NetworkElementData networkElementData;

    public EcimBackupPrecheckResponse(final EcimBackupInfo ecimBackupInfo, final NetworkElementData networkElementData, final ActivityStepResultEnum activityStepResultEnum) {
        super(activityStepResultEnum);
        this.ecimBackupInfo = ecimBackupInfo;
        this.networkElementData = networkElementData;
    }

    public EcimBackupInfo getEcimBackupInfo() {
        return ecimBackupInfo;
    }

    public NetworkElementData getNetworkElementData() {
        return networkElementData;
    }
}
