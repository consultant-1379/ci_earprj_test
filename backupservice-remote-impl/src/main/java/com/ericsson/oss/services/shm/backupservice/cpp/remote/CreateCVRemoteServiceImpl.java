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

import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.notifications.api.Notification;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CreateCVRemoteServiceImpl extends CVManagementAbstractService {

    private static final long serialVersionUID = 591364796213886312L;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    ActivityUtils activityUtils;

    @Override
    public boolean precheckOnMo(final Map<String, Object> cvMoAttr, final Map<String, Object> actionParameters) {

        return true;
    }

    @Override
    public int executeAction(final String cvMoFdn, final String nodeName, final Map<String, Object> actionParameters) {

        try {
            final String actionType = BackupActivityConstants.ACTION_CREATE_CV;
            activityUtils.recordEvent(SHMEvents.CREATE_BACKUP_SERVICE_REQUEST_ACTION, nodeName, cvMoFdn, "SHM:" + nodeName + ":Proceeding creation of CV by service request");
            final int actionStatusValue = executeActionOnMo(actionType, cvMoFdn, actionParameters);
            logger.info("CV created successfuly");
            return actionStatusValue;
        } catch (final Exception e) {
            logger.info("CV creation failed: Failed to trigger create CV Action on CvMo:{}", cvMoFdn);
            throw e;
        }
    }

    /**
     * CreateCv is a Synchronous Call,So we do not get notifications.
     */
    @Override
    public void processNotification(final Notification message) {
    }

}
