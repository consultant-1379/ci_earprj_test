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

import javax.ejb.*;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.notifications.api.Notification;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class SetFirstRollBackListRemoteCVServiceImpl extends CVManagementAbstractService {

    private static final long serialVersionUID = -5931689542568197592L;

    private static final Logger LOGGER = LoggerFactory.getLogger(SetFirstRollBackListRemoteCVServiceImpl.class);

    @Inject
    ActivityUtils activityUtils;

    @Override
    public boolean precheckOnMo(final Map<String, Object> cvMoAttr, final Map<String, Object> actionParameters) {
        return isCvExistsWithValidState(cvMoAttr, (String) actionParameters.get(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME));
    }

    @Override
    public int executeAction(final String cvMoFdn, final String nodeName, final Map<String, Object> actionParameters) {
        try {
            activityUtils.recordEvent(SHMEvents.SETFIRSTINROLLBACK_SERVICE_REQUEST_ACTION, nodeName, cvMoFdn, "SHM:" + nodeName + ":Proceeding to set first in roll back list CV by service request");
            systemRecorder.recordCommand(SHMEvents.SETFIRSTINROLLBACK_SERVICE_REQUEST_ACTION, CommandPhase.STARTED, nodeName, cvMoFdn,
                    ":Proceeding to set first in roll back list CV by service request");
            final String actionType = BackupActivityConstants.ACTION_SET_FIRST_IN_ROLLBACK_CV;
            final int actionStatusValue = executeActionOnMo(actionType, cvMoFdn, actionParameters);
            LOGGER.debug("Supplied CV is set as first in roll back list successfully");
            return actionStatusValue;
        } catch (final Exception e) {
            LOGGER.error("Failed to trigger set first in roll back list CV Action on CvMo:{}", cvMoFdn);
            throw e;
        }
    }

    /**
     * setFirstRollBackList is a Synchronous Call,We do not process Notifications.
     * 
     */
    @Override
    public void processNotification(final Notification message) {
    }

}
