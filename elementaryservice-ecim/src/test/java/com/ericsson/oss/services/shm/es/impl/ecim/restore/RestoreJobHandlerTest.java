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
package com.ericsson.oss.services.shm.es.impl.ecim.restore;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import javax.persistence.OptimisticLockException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@RunWith(MockitoJUnitRunner.class)
public class RestoreJobHandlerTest {

    @InjectMocks
    RestoreJobHandler objectUnderTest;

    @Mock
    private DataPersistenceService dataPersistenceService;

    @Mock
    private DataBucket liveBucket;

    @Mock
    private PersistenceObject activityJob;

    @Mock
    private JobConfigurationService jobConfigurationService;

    @Mock
    private ActivityUtils activityUtils;

    private List<Map<String, Object>> storedActivityJobProperties;
    private List<Map<String, Object>> activityJobPropertiesToBeUpdated;
    private List<Map<String, Object>> updatedActivityJobProperties;

    long activityJobId = 123;
    String nodeName = "Some Node Name";

    @Test
    public void testDetermineActivityCompletionAndUpdateCurrentPropertyAsIsBackupDownloadSuccessfulWhenOtherPropertyIsAlreadyPersisted() {
        final String propertyToBeUpdated = EcimBackupConstants.IS_BACKUP_DOWNLOAD_SUCCESSFUL;

        retrieveJob();
        fetchActivityJobProperties(EcimBackupConstants.IS_BRM_BACKUP_MO_CREATED);

        when(jobConfigurationService.getUpdatedJobProperties(activityJobPropertiesToBeUpdated, storedActivityJobProperties)).thenReturn(updatedActivityJobProperties);

        assertTrue(objectUnderTest.determineActivityCompletionAndUpdateCurrentProperty(activityJobId, nodeName, propertyToBeUpdated));

        activityJobPropertiesToBeUpdated = new ArrayList<Map<String, Object>>();
        updatedActivityJobProperties = new ArrayList<Map<String, Object>>();
        verify(activityUtils).prepareJobPropertyList(activityJobPropertiesToBeUpdated, propertyToBeUpdated, Boolean.toString(true));
        verify(activityJob).setAttribute(ActivityConstants.JOB_PROPERTIES, updatedActivityJobProperties);
    }

    @Test
    public void testDetermineActivityCompletionAndUpdateCurrentPropertyAsIsBackupDownloadSuccessfulWhenOtherPropertyIsNotPersisted() {
        final String propertyToBeUpdated = EcimBackupConstants.IS_BACKUP_DOWNLOAD_SUCCESSFUL;

        retrieveJob();
        fetchActivityJobProperties(null);

        when(jobConfigurationService.getUpdatedJobProperties(activityJobPropertiesToBeUpdated, storedActivityJobProperties)).thenReturn(updatedActivityJobProperties);

        assertFalse(objectUnderTest.determineActivityCompletionAndUpdateCurrentProperty(activityJobId, nodeName, propertyToBeUpdated));

        activityJobPropertiesToBeUpdated = new ArrayList<Map<String, Object>>();
        updatedActivityJobProperties = new ArrayList<Map<String, Object>>();
        verify(activityUtils).prepareJobPropertyList(activityJobPropertiesToBeUpdated, propertyToBeUpdated, Boolean.toString(true));
        verify(activityJob).setAttribute(ActivityConstants.JOB_PROPERTIES, updatedActivityJobProperties);
    }

    @Test(expected = OptimisticLockException.class)
    public void testDetermineActivityCompletionAndUpdateCurrentPropertyAsIsBackupDownloadSuccessfulThrowsOptimisticLockException() {
        final String propertyToBeUpdated = EcimBackupConstants.IS_BACKUP_DOWNLOAD_SUCCESSFUL;

        retrieveJob();
        fetchActivityJobProperties(EcimBackupConstants.IS_BRM_BACKUP_MO_CREATED);

        when(jobConfigurationService.getUpdatedJobProperties(activityJobPropertiesToBeUpdated, storedActivityJobProperties)).thenReturn(updatedActivityJobProperties);

        updatedActivityJobProperties = new ArrayList<Map<String, Object>>();
        doThrow(OptimisticLockException.class).when(activityJob).setAttribute(ActivityConstants.JOB_PROPERTIES, updatedActivityJobProperties);

        objectUnderTest.determineActivityCompletionAndUpdateCurrentProperty(activityJobId, nodeName, propertyToBeUpdated);

        activityJobPropertiesToBeUpdated = new ArrayList<Map<String, Object>>();
        updatedActivityJobProperties = new ArrayList<Map<String, Object>>();
        verify(activityUtils).prepareJobPropertyList(activityJobPropertiesToBeUpdated, propertyToBeUpdated, Boolean.toString(true));
    }

    @Test
    public void testDetermineActivityCompletionAndUpdateCurrentPropertyAsIsBrmBackupMoCreatedWhenOtherPropertyIsAlreadyPersisted() {
        final String propertyToBeUpdated = EcimBackupConstants.IS_BRM_BACKUP_MO_CREATED;

        retrieveJob();
        fetchActivityJobProperties(EcimBackupConstants.IS_BACKUP_DOWNLOAD_SUCCESSFUL);

        when(jobConfigurationService.getUpdatedJobProperties(activityJobPropertiesToBeUpdated, storedActivityJobProperties)).thenReturn(updatedActivityJobProperties);

        assertTrue(objectUnderTest.determineActivityCompletionAndUpdateCurrentProperty(activityJobId, nodeName, propertyToBeUpdated));

        activityJobPropertiesToBeUpdated = new ArrayList<Map<String, Object>>();
        updatedActivityJobProperties = new ArrayList<Map<String, Object>>();
        verify(activityUtils).prepareJobPropertyList(activityJobPropertiesToBeUpdated, propertyToBeUpdated, Boolean.toString(true));
        verify(activityJob).setAttribute(ActivityConstants.JOB_PROPERTIES, updatedActivityJobProperties);
    }

    @Test
    public void testDetermineActivityCompletionAndUpdateCurrentPropertyAsIsBrmBackupMoCreatedWhenOtherPropertyIsNotPersisted() {
        final String propertyToBeUpdated = EcimBackupConstants.IS_BRM_BACKUP_MO_CREATED;

        retrieveJob();
        fetchActivityJobProperties(null);

        when(jobConfigurationService.getUpdatedJobProperties(activityJobPropertiesToBeUpdated, storedActivityJobProperties)).thenReturn(updatedActivityJobProperties);

        assertFalse(objectUnderTest.determineActivityCompletionAndUpdateCurrentProperty(activityJobId, nodeName, propertyToBeUpdated));

        activityJobPropertiesToBeUpdated = new ArrayList<Map<String, Object>>();
        updatedActivityJobProperties = new ArrayList<Map<String, Object>>();
        verify(activityUtils).prepareJobPropertyList(activityJobPropertiesToBeUpdated, propertyToBeUpdated, Boolean.toString(true));
        verify(activityJob).setAttribute(ActivityConstants.JOB_PROPERTIES, updatedActivityJobProperties);
    }

    @Test(expected = OptimisticLockException.class)
    public void testDetermineActivityCompletionAndUpdateCurrentPropertyAsIsBrmBackupMoCreatedThrowsOptimisticLockException() {
        final String propertyToBeUpdated = EcimBackupConstants.IS_BRM_BACKUP_MO_CREATED;

        retrieveJob();
        fetchActivityJobProperties(EcimBackupConstants.IS_BACKUP_DOWNLOAD_SUCCESSFUL);

        when(jobConfigurationService.getUpdatedJobProperties(activityJobPropertiesToBeUpdated, storedActivityJobProperties)).thenReturn(updatedActivityJobProperties);

        updatedActivityJobProperties = new ArrayList<Map<String, Object>>();
        doThrow(OptimisticLockException.class).when(activityJob).setAttribute(ActivityConstants.JOB_PROPERTIES, updatedActivityJobProperties);

        objectUnderTest.determineActivityCompletionAndUpdateCurrentProperty(activityJobId, nodeName, propertyToBeUpdated);

        activityJobPropertiesToBeUpdated = new ArrayList<Map<String, Object>>();
        verify(activityUtils).prepareJobPropertyList(activityJobPropertiesToBeUpdated, propertyToBeUpdated, Boolean.toString(true));
    }

    private void retrieveJob() {
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findPoById(activityJobId)).thenReturn(activityJob);
    }

    private void fetchActivityJobProperties(final String key) {
        storedActivityJobProperties = new ArrayList<Map<String, Object>>();
        if (key != null) {
            final Map<String, Object> brmBackupMoCreatedMap = new HashMap<String, Object>();
            brmBackupMoCreatedMap.put(ShmConstants.KEY, key);
            brmBackupMoCreatedMap.put(ShmConstants.VALUE, "true");
            storedActivityJobProperties.add(brmBackupMoCreatedMap);
        }
        when(activityJob.getAttribute(ActivityConstants.JOB_PROPERTIES)).thenReturn(storedActivityJobProperties);
    }
}
