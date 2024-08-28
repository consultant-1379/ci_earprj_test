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
package com.ericsson.oss.services.shm.backupservice.remote.impl;

import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.backupservice.ecim.remote.CommonRemoteBackUpManagementService;
import com.ericsson.oss.services.shm.backupservice.remote.api.BackupInfo;
import com.ericsson.oss.services.shm.backupservice.remote.api.BackupManagementServiceException;
import com.ericsson.oss.services.shm.backupservice.remote.api.BackupManagementServiceRemote;
import com.ericsson.oss.services.shm.backupservice.remote.api.DeleteBackupOptions;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.job.exceptions.NoJobConfigurationException;
import com.ericsson.oss.services.shm.job.exceptions.NoMeFDNSProvidedException;
import com.ericsson.oss.services.shm.job.service.SHMJobService;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobservice.common.JobHandlerErrorCodes;
import com.ericsson.oss.services.shm.jobservice.common.JobInfo;

/**
 * Remote Interface implementation for performing backup management operations on a network element.
 */

@Stateless
@Traceable
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class BackupManagementService implements BackupManagementServiceRemote {

    @Inject
    private BackupManagementServiceFactory backupManagementServiceFactory;

    @Inject
    private BackupJobDataBuilder backupJobDataBuilder;

    @EServiceRef
    private SHMJobService shmJobService;

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupManagementService.class);

    @Override
    public void createBackup(final String neName, final BackupInfo backupInfo) throws BackupManagementServiceException {
        if (neName == null || backupInfo == null) {
            LOGGER.error("Mandatory parameters(neName/backupInfo) cannot be null for createBackup");
            throw new BackupManagementServiceException("Invalid inputs: neName/backupInfo cannot be null for createBackup");
        }
        try {
            final CommonRemoteBackUpManagementService createBackupService = backupManagementServiceFactory.getBackUpManagementService(EcimBackupConstants.CREATE_BACKUP);
            final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(backupInfo.getDomain(), backupInfo.getName(), backupInfo.getType());
            if (createBackupService.precheck(neName, ecimBackupInfo)) {
                createBackupService.executeMoAction(neName, ecimBackupInfo);
            } else {
                LOGGER.error("Precheck action for CreateBackUp has failed on Node:{}", neName);
                throw new BackupManagementServiceException("precheck action failed for CreateBackup. ");
            }
        } catch (final Exception e) {
            LOGGER.error("Create backup has Failed due to", e);
            throw new BackupManagementServiceException("Exception occured while Creating Backup: Failure reason: " + e.getMessage() + "backupName : " + backupInfo.getName() + "domain : "
                    + backupInfo.getDomain() + " type : " + backupInfo.getType() + " node : " + neName);
        }

    }

    /**
     * uploads the backup from the specified Network Element to ENM SMRS file store.
     * 
     * @param neName
     *            - the network element name
     * @param backupInfo
     *            - contains the details required for upload backup operation.In case of CPP - the backupInfo must only have the name set.In case of ECIM - the backupInfo must have the name, domain
     *            and type set.
     * @throws BackupManagementServiceException
     *             - in case where the operation has failed.
     */
    @Override
    public void uploadBackup(final String neName, final BackupInfo backupInfo) throws BackupManagementServiceException {

        if (neName == null || backupInfo == null) {
            LOGGER.error("Mandatory parameters(neName/backupInfo) cannot be null for uploadBackup");
            throw new BackupManagementServiceException("Invalid inputs: neName/backupInfo cannot be null for uploadBackup.");
        }
        try {
            final CommonRemoteBackUpManagementService uploadBackupService = backupManagementServiceFactory.getBackUpManagementService(EcimBackupConstants.UPLOAD_BACKUP);
            final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(backupInfo.getDomain(), backupInfo.getName(), backupInfo.getType());

            if (uploadBackupService.precheck(neName, ecimBackupInfo)) {
                uploadBackupService.executeMoAction(neName, ecimBackupInfo);
            } else {
                LOGGER.error("Precheck action for Upload Backup has failed on Node:{}", neName);
                throw new BackupManagementServiceException("precheck has failed for Upload Backup. ");
            }
        } catch (final Exception e) {
            LOGGER.error("Upload backup has Failed due to", e);
            throw new BackupManagementServiceException("Exception occured while Uploading Backup: Failure reason :" + e.getMessage() + "backupName : " + backupInfo.getName() + "domain : "
                    + backupInfo.getDomain() + " type : " + backupInfo.getType() + " node : " + neName);
        }
    }

    @Override
    public void setStartableBackup(final String neName, final BackupInfo backupInfo) throws BackupManagementServiceException {

    }

    @Override
    public void setBackupFirstInRollBackList(final String neName, final BackupInfo backupInfo) throws BackupManagementServiceException {

    }

    /**
     * deletes the backup of specified Network Element from ENM SMRS file store.
     * 
     * @param neName
     *            - the network element name
     * @param backupInfo
     *            - contains the details required for delete backup operation.In case of CPP - the backupInfo must have the name set.In case of ECIM - the backupInfo must have the name, domain and
     *            type set.
     * @param deleteBackupOptions
     *            - Holds options for delete backup operation
     * @throws BackupManagementServiceException
     *             - in case where the operation has failed.
     */

    @Override
    public void deleteBackup(final String neName, final BackupInfo backupInfo, final DeleteBackupOptions deleteBackupOptions) throws BackupManagementServiceException {

        if (neName == null || backupInfo == null) {
            LOGGER.error("Mandatory parameters(neName/backupInfo) cannot be null for deleteBackup");
            throw new BackupManagementServiceException("Invalid inputs: neName/backupInfo cannot be null for deleteBackup.");
        }
        try {
            final CommonRemoteBackUpManagementService deleteBackupService = backupManagementServiceFactory.getBackUpManagementService(EcimBackupConstants.DELETE_BACKUP);
            final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(backupInfo.getDomain(), backupInfo.getName(), backupInfo.getType());

            if (deleteBackupService.precheck(neName, ecimBackupInfo)) {
                deleteBackupService.executeMoAction(neName, ecimBackupInfo);
            } else {
                LOGGER.error("Precheck action for Delete Backup has failed on Node:{}", neName);
                throw new BackupManagementServiceException("precheck has failed for Delete Backup. ");
            }
        } catch (final Exception e) {
            LOGGER.error("Delete backup has Failed due to", e);
            throw new BackupManagementServiceException("Exception occured while Deleting Backup: Failure reason :" + e.getMessage() + "backupName : " + backupInfo.getName() + "domain : "
                    + backupInfo.getDomain() + " type : " + backupInfo.getType() + " node : " + neName);
        }
    }

    /**
     * Restores the - previously created backup on NE and confirms the restore operation. Not supported for CPP - invocation will result in BackupOperationException.
     * 
     * @param neName
     *            - the network element name
     * @param backupInfo
     *            - contains the details required for restore backup operation. In case of ECIM - the backupInfo must have the name , domain and type set.
     * @throws BackupManagementServiceException
     */
    @Override
    public void restoreBackup(final String neName, final BackupInfo backupInfo) throws BackupManagementServiceException {

        if (neName == null || backupInfo == null) {
            LOGGER.error("Mandatory parameters(neName/backupInfo) cannot be null for restoreBackup");
            throw new BackupManagementServiceException("Invalid inputs: neName/backupInfo cannot be null for restoreBackup.");
        }
        try {
            final CommonRemoteBackUpManagementService restoreService = backupManagementServiceFactory.getBackUpManagementService(EcimBackupConstants.RESTORE_BACKUP);
            final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(backupInfo.getDomain(), backupInfo.getName(), backupInfo.getType());

            if (restoreService.precheck(neName, ecimBackupInfo)) {
                if (restoreService.executeMoAction(neName, ecimBackupInfo) == 0) {
                    LOGGER.debug("Restore backup action triggered successfuly on the node : {}.", neName);
                } else {
                    LOGGER.error("Restore backup action invocation failed on the node : {} .", neName);
                    throw new BackupManagementServiceException("Restore backup action invocation failed on the node.");
                }
            } else {
                LOGGER.error("Precheck Action has Failed on the node {} due to Backup is present in improper state.", neName);
                throw new BackupManagementServiceException("Precheck Action has Failed on the node due to Backup is present in improper state.");
            }
        } catch (final Exception e) {
            LOGGER.error("Restore backup has Failed due to", e);
            throw new BackupManagementServiceException("Exception occured while Restoring Backup. BackupName : " + backupInfo.getName() + " domain : " + backupInfo.getDomain() + " type : "
                    + backupInfo.getType() + " node : " + neName + "Exception is:" + e);
        }
    }

    /**
     * Creates a backup on the specified Network Element.
     * <p>
     * If create backup request accepted, a unique message ID which is job name will be returned.If create backup request rejected, {@link BackupManagementServiceException} will be thrown that details
     * the reason for rejection.
     * 
     * @param neName
     *            - the network element name
     * @param backupInfo
     *            - contains the details required for create backup operation.In case of CPP - the backupInfo must have the name set , identity and comment are optional. In case of ECIM - the
     *            backupInfo must have the domain and type set , name is optional.
     * @throws BackupManagementServiceException
     *             - in case where the operation has failed.
     * @return JobName in response
     */

    @Override
    public String create(final String neName, final BackupInfo backupInfo) throws BackupManagementServiceException {
        LOGGER.info("Create Backup initiated for network element: {}", neName);
        JobInfo jobInfo = new JobInfo();
        final Map<String, String> neTypeAndPlatformType = backupJobDataBuilder.getNeTypeAndPlatformType(neName);
        final String platformType = neTypeAndPlatformType.get(ShmConstants.PLATFORM);
        try {
            if (PlatformTypeEnum.CPP.toString().equalsIgnoreCase(platformType)) {
                final List<Map<String, Object>> jobConfiguration = backupJobDataBuilder.prepareJobConfiguration(backupInfo, ShmConstants.CREATE_CV__ACTIVITY);
                jobInfo = backupJobDataBuilder.prepareJobInfo(neName, ShmConstants.CREATE_CV__ACTIVITY, JobTypeEnum.BACKUP, jobConfiguration, neTypeAndPlatformType);
            } else if (PlatformTypeEnum.ECIM.toString().equalsIgnoreCase(platformType)) {
                final List<Map<String, Object>> jobConfiguration = backupJobDataBuilder.prepareJobConfiguration(backupInfo, ShmConstants.CREATE_BACKUP);
                jobInfo = backupJobDataBuilder.prepareJobInfo(neName, ShmConstants.CREATE_BACKUP, JobTypeEnum.BACKUP, jobConfiguration, neTypeAndPlatformType);
            }
            if (jobInfo.getName() != null) {
                LOGGER.debug("{} create backup job {} has triggered on the node {} ", platformType, jobInfo.getName(), neName);
            }
            final Map<String, Object> response = shmJobService.createShmJob(jobInfo);
            if (response.get(ShmConstants.ERROR_CODE).toString().equals(JobHandlerErrorCodes.SUCCESS.getResponseDescription())) {
                final String result = JobHandlerErrorCodes.SUCCESS.getResponseDescription();
                LOGGER.debug("Backup created successfully: {}", result);
                return response.get(ShmConstants.JOBNAME).toString();
            } else {
                final String result = response.get(ShmConstants.ERROR_CODE).toString();
                LOGGER.error("Failed to create backup: {} on the node: {}. Failure reason: {}", backupInfo.getName(), neName, result);
                return result;
            }
        } catch (NoMeFDNSProvidedException | NoJobConfigurationException e) {
            LOGGER.error("Failed to create backup with backup name:{} on the node: {}. Exception is: ", backupInfo.getName(), neName, e);
            return e.getMessage();
        } catch (final Exception e) {
            LOGGER.error("Exception occurred while creating backup with backup name:{} on the node: {}. Exception is: ", backupInfo.getName(), neName, e);
            return e.getMessage();
        }
    }

    /**
     * uploads the backup from the specified Network Element to ENM SMRS file store. Asynchronous activity If upload backup request accepted, a unique message ID, which is job name will be returned.If
     * upload backup request rejected, {@link BackupManagementServiceException} will be thrown that details the reason for rejection.
     * 
     * @param neName
     *            - the network element name
     * @param backupInfo
     *            - contains the details required for upload backup operation.In case of CPP - the backupInfo must only have the name set.In case of ECIM - the backupInfo must have the name, domain
     *            and type set.
     * @throws BackupManagementServiceException
     *             - in case where the operation has failed.
     * @return JobName in response
     */
    @Override
    public String export(final String neName, final BackupInfo backupInfo) throws BackupManagementServiceException {
        LOGGER.info("Upload Backup initiated for network element: {}", neName);
        JobInfo jobInfo = new JobInfo();
        final Map<String, String> neTypeAndPlatformType = backupJobDataBuilder.getNeTypeAndPlatformType(neName);
        try {
            final List<Map<String, Object>> jobConfiguration = backupJobDataBuilder.prepareJobConfiguration(backupInfo, ShmConstants.UPLOAD_BACKUP);
            jobInfo = backupJobDataBuilder.prepareJobInfo(neName, ShmConstants.UPLOAD_BACKUP, JobTypeEnum.BACKUP, jobConfiguration, neTypeAndPlatformType);
            if (jobInfo.getName() != null) {
                LOGGER.debug("Start of create upload backup Job for the job: {} ", jobInfo.getName());
            }
            final Map<String, Object> response = shmJobService.createShmJob(jobInfo);
            if (response.get(ShmConstants.ERROR_CODE).toString().equals(JobHandlerErrorCodes.SUCCESS.getResponseDescription())) {
                final String result = JobHandlerErrorCodes.SUCCESS.getResponseDescription();
                LOGGER.debug("Backup: {} successfully uploaded on the node: {}. Result is {}", backupInfo.getName(), neName, result);
                return response.get(ShmConstants.JOBNAME).toString();
            } else {
                final String result = response.get(ShmConstants.ERROR_CODE).toString();
                LOGGER.error("Failed to upload backup: {} on the node: {}. Failure reason: {}", backupInfo.getName(), neName, result);
                return result;
            }
        } catch (NoMeFDNSProvidedException | NoJobConfigurationException e) {
            LOGGER.error("Failed to upload backup with backup name:{} on the node: {}. Exception is:{}", backupInfo.getName(), neName, e);
            return e.getMessage();
        } catch (final Exception e) {
            LOGGER.error("Exception occurred while uploading backup with backup name:{} on the node: {}. Exception is:{}", backupInfo.getName(), neName, e);
            return e.getMessage();
        }
    }
}
