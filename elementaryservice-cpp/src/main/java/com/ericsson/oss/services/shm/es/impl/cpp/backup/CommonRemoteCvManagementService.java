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
package com.ericsson.oss.services.shm.es.impl.cpp.backup;

import java.util.Map;

import com.ericsson.oss.services.shm.es.api.RemoteActivityCallBack;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMO;
import com.ericsson.oss.services.shm.es.impl.cpp.common.UpgradePackageMO;

public interface CommonRemoteCvManagementService extends RemoteActivityCallBack {

    boolean precheckOnMo(Map<String, Object> cvMoAttr, Map<String, Object> actionParameters);

    int executeAction(String cvMoFdn, String nodeName, Map<String, Object> actionParameters);

    ConfigurationVersionMO getCVMo(String nodeName);

    UpgradePackageMO getUPMo(String nodeName, String searchValue);

}
