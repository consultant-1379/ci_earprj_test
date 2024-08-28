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
package com.ericsson.oss.services.shm.backupservice.cpp.remote;

import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CommonCvOperations;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CommonRemoteCvManagementService;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMO;
import com.ericsson.oss.services.shm.es.impl.cpp.common.UpgradePackageMO;

public abstract class CVManagementAbstractService implements CommonRemoteCvManagementService {

    private static final long serialVersionUID = -250623780660116844L;

    @Inject
    protected CommonCvOperations commonCvOperations;

    @Inject
    protected SystemRecorder systemRecorder;

    @Inject
    protected DpsReader dpsReader;

    public int executeActionOnMo(final String actionType, final String cvMoFdn, final Map<String, Object> actionParameters) {

        return commonCvOperations.executeActionOnMo(actionType, cvMoFdn, actionParameters);
    }

    @Override
    public ConfigurationVersionMO getCVMo(final String nodeName) {

        return commonCvOperations.getCVMo(nodeName);
    }

    @Override
    public UpgradePackageMO getUPMo(final String nodeName, final String searchValue) {

        return commonCvOperations.getUPMo(nodeName, searchValue);
    }

    public boolean isCvExistsWithValidState(final Map<String, Object> cvMoAttr, final String cvName) {
        return commonCvOperations.precheckForSetStartCVSetFistInRolback(cvMoAttr, cvName);
    }

}
