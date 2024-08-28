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
public class SetStartableCVRemoteServiceImpl extends CVManagementAbstractService {

    private static final long serialVersionUID = -5136289536529236831L;

    private static final Logger LOGGER = LoggerFactory.getLogger(SetStartableCVRemoteServiceImpl.class);

    @Inject
    ActivityUtils activityUtils;

    @Override
    public boolean precheckOnMo(final Map<String, Object> cvMoAttr, final Map<String, Object> actionParameters) {
        return isCvExistsWithValidState(cvMoAttr, (String) actionParameters.get(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME));
    }

    @Override
    public int executeAction(final String cvMoFdn, final String nodeName, final Map<String, Object> actionParameters) {
        try {
            final String actionType = BackupActivityConstants.ACTION_SET_STARTABLE_CV;
            systemRecorder.recordCommand(SHMEvents.SETSTARTABLE_BACKUP_SERVICE_REQUEST_ACTION, CommandPhase.STARTED, nodeName, cvMoFdn, ":Proceeding to set startable CV by service request");
            activityUtils.recordEvent(SHMEvents.SETSTARTABLE_BACKUP_SERVICE_REQUEST_ACTION, nodeName, cvMoFdn, "SHM:" + nodeName + ":Proceeding to set startable CV by service request");
            final int actionStatusValue = executeActionOnMo(actionType, cvMoFdn, actionParameters);
            LOGGER.debug("Supplied CV is set as startable CV successfully");
            return actionStatusValue;

        } catch (final Exception e) {
            LOGGER.error("Failed to trigger set startable CV Action on CvMo:{}", cvMoFdn);
            throw e;
        }
    }

    /**
     * SetFirstRollBackList is a Synchronous Call, We do not process Notifications.
     * 
     */
    @Override
    public void processNotification(final Notification message) {
    }

}
