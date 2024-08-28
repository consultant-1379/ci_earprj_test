/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.ecim.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.query.ContainmentRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.exception.ServerInternalException;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.model.NetworkElementData;

@Stateless
public class BackupMOInformationProvider {

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    @Inject
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupMOInformationProvider.class);
    private static final String BRM_BACKUP = "BrmBackup";
    private static final String BACKUP_NAME = "backupName";
    private static final String SOFTWARE_VERSION = "swVersion";

    public List<Map<String, String>> getswVersionsListFromBrmBackupMOsList(final NetworkElementData networkElement, final EcimBackupInfo ecimBackupInfo) {
        List<Map<String, String>> swVersions = new ArrayList<>();
        try {
            final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
            final Query<ContainmentRestrictionBuilder> query = queryBuilder.createContainmentQuery(networkElement.getNodeRootFdn());
            final Restriction restriction = query.getRestrictionBuilder().equalTo(ObjectField.TYPE, BRM_BACKUP);
            query.setRestriction(restriction);
            final List<ManagedObject> backupMOsList = dataPersistenceService.getLiveBucket().getQueryExecutor().getResultList(query);
            LOGGER.debug("List of backupMOs from Data base : {} ", backupMOsList);
            if (backupMOsList != null) {
                for (final ManagedObject backupMO : backupMOsList) {
                    if (backupMO != null && ecimBackupInfo.getBackupName().equals(backupMO.getAttribute(BACKUP_NAME))) {
                        swVersions = backupMO.getAttribute(SOFTWARE_VERSION);
                        break;
                    }
                }
            }
        } catch (final RuntimeException ex) {
            LOGGER.error("Exception while retrieving backupMOs for networkElement: {}. Reason: ", networkElement.getNodeRootFdn(), ex);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
            throw new ServerInternalException("Exception while reading the Mo Data. Please try again.");
        } catch (final Exception e) {
            LOGGER.error("Exception while retrieving backupMOs for networkElement: {}. Reason: ", networkElement.getNodeRootFdn(), e);
        }
        return swVersions;
    }

}
