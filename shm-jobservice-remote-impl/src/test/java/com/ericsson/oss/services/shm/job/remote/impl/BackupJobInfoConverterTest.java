/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.remote.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.UserContextBean;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.job.exceptions.NoMeFDNSProvidedException;
import com.ericsson.oss.services.shm.job.remote.api.BackupActivityEnum;
import com.ericsson.oss.services.shm.job.remote.api.ShmBackupJobData;
import com.ericsson.oss.services.shm.job.remote.api.errorcodes.JobCreationResponseCode;
import com.ericsson.oss.services.shm.job.service.SHMJobService;
import com.ericsson.oss.services.shm.job.service.common.BackupActivitySchedulesUtil;
import com.ericsson.oss.services.shm.job.service.common.BackupJobInfoConverter;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobCapabilityProvider;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.jobservice.common.JobInfo;
import com.ericsson.oss.services.shm.topologyservice.TopologyEvaluationService;
import com.ericsson.oss.services.topologyCollectionsService.exception.service.TopologyCollectionsServiceException;

@RunWith(MockitoJUnitRunner.class)
public class BackupJobInfoConverterTest {

    @InjectMocks
    BackupJobInfoConverter backupJobInfoConverter;

    @Mock
    FdnServiceBean fdnServiceBean;

    @Mock
    UserContextBean userContextBean;

    @Mock
    SupportedPlatformAndNeTypeFinder platformAndNeTypeFinder;

    @Mock
    BackupActivitySchedulesUtil backupActivitySchedulesUtil;

    @Mock
    NeTypePropertiesUtil neTypePropertiesUtil;

    @Mock
    TopologyEvaluationService topologyEvaluationService;

    @Mock
    SHMJobService shmJobService;

    @Mock
    private JobCapabilityProvider jobCapabilityProviderMock;

    Map<PlatformTypeEnum, List<String>> supportedPlatformTypeAndNodeTypes;

    private static final String JOB_NAME = "CliJobName001";

    private static final String BACKUP_NAME = "BackupName";

    private static final String TEST_BACKUP_NAME = "LoooooonggggggggBAaaaaaaaackkkkupppppppppNaaaaaameToCheckLegth";

    @Before
    public void setup() {
        supportedPlatformTypeAndNodeTypes = new EnumMap<PlatformTypeEnum, List<String>>(PlatformTypeEnum.class);
        List<String> neTypes = new ArrayList<String>();
        neTypes.add("ERBS");
        supportedPlatformTypeAndNodeTypes.put(PlatformTypeEnum.CPP, neTypes);
    }

    @Test
    public void testConvert() throws TopologyCollectionsServiceException, NoMeFDNSProvidedException {
        Set<String> neNamesSet = new HashSet<String>();
        List<String> neNames = new ArrayList<String>(neNamesSet);
        NetworkElement networkElement = new NetworkElement();
        ShmBackupJobData shmBackupJobData = new ShmBackupJobData();
        shmBackupJobData.setJobType(JobType.BACKUP);
        networkElement.setPlatformType(PlatformTypeEnum.CPP);
        neNamesSet.add("LTE01ERBS01");
        shmBackupJobData.setNeNames(neNamesSet);
        shmBackupJobData.setBackupName("newBackupName");
        List<NetworkElement> networkElements = new ArrayList<NetworkElement>();
        networkElements.add(networkElement);
        List<BackupActivityEnum> backupActivityEnums = new ArrayList<BackupActivityEnum>();
        backupActivityEnums.add(BackupActivityEnum.CREATE_CV);
        shmBackupJobData.setActivities(backupActivityEnums);
        when(fdnServiceBean.getNetworkElementsByNeNames(neNames, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(networkElements);
        final JobInfo jobInfo = backupJobInfoConverter.prepareJobInfoData(shmBackupJobData);
        when(platformAndNeTypeFinder.findSupportedPlatformAndNodeTypes(SHMCapabilities.BACKUP_JOB_CAPABILITY, shmBackupJobData)).thenReturn(supportedPlatformTypeAndNodeTypes);
        backupJobInfoConverter.prepareJobInfoData(shmBackupJobData);
        when(topologyEvaluationService.getCollectionPoId("Collection", "Admin")).thenReturn("poid");
        when(topologyEvaluationService.getSavedSearchPoId("SavedSearchId", "Admin")).thenReturn("poid");
        assertNotNull(jobInfo);
        assertTrue(jobInfo.getJobType().equals(JobTypeEnum.BACKUP));
    }

    @Test
    public void testConvertWithCollectionAndFdns() throws TopologyCollectionsServiceException, NoMeFDNSProvidedException {
        Set<String> neNamesSet = new HashSet<String>();
        List<String> neNamesList = new ArrayList<String>(neNamesSet);
        Set<String> fdns = new HashSet<String>();
        NetworkElement networkElement = new NetworkElement();
        ShmBackupJobData shmBackupJobData = new ShmBackupJobData();
        shmBackupJobData.setJobType(JobType.BACKUP);
        networkElement.setPlatformType(PlatformTypeEnum.CPP);
        neNamesSet.add("LTE01ERBS01");
        shmBackupJobData.setNeNames(neNamesSet);
        shmBackupJobData.setBackupName("newBackupName");
        shmBackupJobData.setLoggedInUser("administrator");
        List<NetworkElement> networkElements = new ArrayList<NetworkElement>();
        networkElements.add(networkElement);
        List<BackupActivityEnum> backupActivityEnums = new ArrayList<BackupActivityEnum>();
        backupActivityEnums.add(BackupActivityEnum.CREATE_CV);
        shmBackupJobData.setActivities(backupActivityEnums);
        shmBackupJobData.setCollection("Collection");
        fdns.add("NetworkElement=LTE01ERBS01");
        shmBackupJobData.setFdns(fdns);
        when(fdnServiceBean.getNetworkElementsByNeNames(neNamesList, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(networkElements);
        final JobInfo jobInfo = backupJobInfoConverter.prepareJobInfoData(shmBackupJobData);
        when(platformAndNeTypeFinder.findSupportedPlatformAndNodeTypes(SHMCapabilities.BACKUP_JOB_CAPABILITY, shmBackupJobData)).thenReturn(supportedPlatformTypeAndNodeTypes);
        when(topologyEvaluationService.getSavedSearchPoId("SavedSearchId", "Admin")).thenReturn("poid");
        backupJobInfoConverter.prepareJobInfoData(shmBackupJobData);
        assertNotNull(jobInfo);
        assertTrue(jobInfo.getJobType().equals(JobTypeEnum.BACKUP));
        assertTrue("administrator".equals(jobInfo.getOwner()));
    }

    @Test
    public void testIsBackupNameAllowed() {
        ShmBackupJobData shmBackupJobData = new ShmBackupJobData();
        shmBackupJobData.setJobName(JOB_NAME);
        shmBackupJobData.setBackupName(TEST_BACKUP_NAME);
        backupJobInfoConverter.isValidData(shmBackupJobData);
        final JobCreationResponseCode jobCreationResponseCode = backupJobInfoConverter.isValidData(shmBackupJobData);
        assertNull(jobCreationResponseCode);
    }

}
