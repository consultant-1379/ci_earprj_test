/*------------------------------------------------------------------------------
 *******************************************************************************
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
package com.ericsson.oss.services.shm.backupservice.ecim.remote;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.backupservice.remote.api.BackupManagementServiceException;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.notification.common.RemoteActivityNotificationHelper;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier;

/**
 * This class facilitates the common functionalities of create and uploading the backup
 * 
 * @author tcsnean
 * 
 */

public abstract class BackUpManagementAbstractService implements CommonRemoteBackUpManagementService {

    private static final long serialVersionUID = 4333068094884106391L;

    @Inject
    @NotificationTypeQualifier(type = NotificationType.SYNCHRONOUS_REQUEST)
    protected NotificationRegistry notificationRegistry;

    @Inject
    protected ActivityUtils activityUtils;

    @Inject
    protected RemoteActivityNotificationHelper remoteActivityNotificationHelper;

    @Inject
    protected SystemRecorder systemRecorder;

    @Inject
    BrmMoServiceRetryProxy brmMoServiceRetryProxy;

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    /**
     * This method get MoFdn of the corresponding node
     * 
     * @param backupType
     * @param nodeName
     * @param ecimBackupInfo
     * @return String
     * 
     */

    public String getNotifiableFdn(final String backupType, final String nodeName, final EcimBackupInfo ecimBackupInfo) {
        try {
            return brmMoServiceRetryProxy.getNotifiableMoFdn(backupType, nodeName, ecimBackupInfo);
        } catch (final MoNotFoundException e) {
            LOGGER.error("Failed to get MO Fdn on the node due to MoNotFoundException  ", e);
            return null;
        } catch (final UnsupportedFragmentException e1) {
            LOGGER.error("Failed to get MO Fdn on the node due to UnsupportedFragmentException", e1);
            return null;
        }
    }

    /**
     * uploads the backup from the specified Network Element to ENM SMRS file store.
     * 
     * @param nodeName
     *            - the network element name
     * @param ecimBackUpInfo
     * 
     * @return Integers
     * @throws BackupManagementServiceException
     */

    public int executeAction(final String nodeName, final EcimBackupInfo ecimBackUpInfo, final String moFdn, final String activity) throws BackupManagementServiceException {

        try {
            return brmMoServiceRetryProxy.executeMoAction(nodeName, ecimBackUpInfo, moFdn, activity);
        } catch (UnsupportedFragmentException | MoNotFoundException | ArgumentBuilderException e) {
            LOGGER.error("MO Action has Failed due to UnsupportedFragmentException | MoNotFoundException | ArgumentBuilderException", e);
            throw new BackupManagementServiceException("MO Action has Failed on fdn " + moFdn + " due to ", e);
        }

    }

}
