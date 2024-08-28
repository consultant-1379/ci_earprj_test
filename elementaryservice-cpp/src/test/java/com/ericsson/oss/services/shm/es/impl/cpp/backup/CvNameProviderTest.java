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
package com.ericsson.oss.services.shm.es.impl.cpp.backup;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.security.accesscontrol.EAccessControl;
import com.ericsson.oss.services.shm.common.exception.BackupDataNotFoundException;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActiveSoftwareProvider;
import com.ericsson.oss.services.shm.es.impl.AutoGenerateNameValidator;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.CheckPeriodicity;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@RunWith(MockitoJUnitRunner.class)
public class CvNameProviderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CvNameProviderTest.class);

    @Mock
    private JobConfigurationServiceRetryProxy jobConfigServiceRetryProxyMock;

    @InjectMocks
    private CvNameProvider objectUnderTest;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrievalBeanMock;

    @Mock
    private JobPropertyUtils jobPropertyUtilsMock;

    @Mock
    private JobStaticDataProvider jobStaticDataProviderMock;

    @Mock
    private JobStaticData jobStaticDataMock;

    @Mock
    private EAccessControl eAccessControlMock;

    @Mock
    private ActiveSoftwareProvider activeSoftwareProviderMock;

    @Mock
    private JobUpdateService jobUpdateServiceMock;

    @Mock
    private AutoGenerateNameValidator autoGenerateNameValidator;

    @Mock
    private CheckPeriodicity periodicityChecker;

    long neJobId = 123L;
    long mainJobId = 123L;

    String neName = "Some Ne Name";
    String definedCvName = "OP_$productnumber_$productrevision_$timestamp";
    String neType = "ERBS";
    String platform = "CPP";
    String operatorName = "Some Operator Name";
    String comment = "Some Comment";
    String jobName = "Some Job Name";
    String generatebackupName = "true";
    String neJobBusinesskey = "neJobBusinesskey";

    Map<String, Object> mainJobAttr;
    Map<String, Object> neJobAttributes;

    private Map<String, Object> setMainJobAttributes() {
        mainJobAttr = new HashMap<>();
        final Map<String, Object> jobConfigurationDetails = getJobConfigurationDetails();
        mainJobAttr.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        final List<Map<String, String>> jobProperties = new ArrayList<>();
        final Map<String, String> mainJobPropertyCvName = new HashMap<>();
        mainJobPropertyCvName.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        mainJobPropertyCvName.put(ShmConstants.VALUE, definedCvName);
        jobProperties.add(mainJobPropertyCvName);
        final Map<String, String> mainJobPropertyAutogenerate = new HashMap<>();
        mainJobPropertyAutogenerate.put(ShmConstants.KEY, JobPropertyConstants.AUTO_GENERATE_BACKUP);
        mainJobPropertyAutogenerate.put(ShmConstants.VALUE, generatebackupName);
        jobProperties.add(mainJobPropertyAutogenerate);
        mainJobAttr.put(ActivityConstants.JOB_PROPERTIES, jobProperties);
        return mainJobAttr;
    }

    private Map<String, Object> getJobConfigurationDetails() {
        Map<String, Object> jobConfigurationDetails = new HashMap<>();
        final List<Map<String, String>> mainJobPropertyList = new ArrayList<>();
        jobConfigurationDetails.put(ActivityConstants.JOB_PROPERTIES, mainJobPropertyList);
        final Map<String, Object> mainSchedule = new HashMap<>();
        final List<Map<String, Object>> schedulePropertiesList = new ArrayList<>();
        mainSchedule.put(ShmConstants.SCHEDULINGPROPERTIES, schedulePropertiesList);
        jobConfigurationDetails.put(ShmConstants.MAIN_SCHEDULE, mainSchedule);

        return jobConfigurationDetails;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetConfigurationVersionNameShouldGenerateCVNameWhenActiveSWIsValid() {

        NEJobStaticData neJobStaticdata = new NEJobStaticData(neJobId, mainJobId, neName, neJobBusinesskey, platform, new Date().getTime(), null);
        neJobAttributes = new HashMap<>();
        final List<Map<String, String>> neJobProperties = new ArrayList<>();
        final Map<String, String> cvNameProperty = new HashMap<>();
        cvNameProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        cvNameProperty.put(ShmConstants.VALUE, "");
        neJobProperties.add(cvNameProperty);
        neJobAttributes.put(ActivityConstants.JOB_PROPERTIES, neJobProperties);
        when(jobConfigServiceRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        mainJobAttr = setMainJobAttributes();
        final List<Map<String, String>> jobProperties = new ArrayList<>();
        final Map<String, String> mainJobPropertyCvName = new HashMap<>();
        mainJobPropertyCvName.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        mainJobPropertyCvName.put(ShmConstants.VALUE, definedCvName);
        jobProperties.add(mainJobPropertyCvName);

        mainJobAttr.put(ActivityConstants.JOB_PROPERTIES, jobProperties);
        when(jobConfigServiceRetryProxyMock.getMainJobAttributes(Matchers.eq(mainJobId))).thenReturn(mainJobAttr);
        Map<String, String> propertyKeyValueMap = new HashMap<>();
        propertyKeyValueMap.put(BackupActivityConstants.CV_NAME, definedCvName);
        propertyKeyValueMap.put(JobPropertyConstants.AUTO_GENERATE_BACKUP, generatebackupName);

        when(jobPropertyUtilsMock.getPropertyValue(Arrays.asList(BackupActivityConstants.CV_NAME, JobPropertyConstants.AUTO_GENERATE_BACKUP), getJobConfigurationDetails(), neName, neType, platform))
                .thenReturn(propertyKeyValueMap);
        when(jobStaticDataMock.getOwner()).thenReturn(operatorName);
        final Map<String, String> activeSoftware = new HashMap<>();
        activeSoftware.put(neName, "CXP00||RX123");
        when(activeSoftwareProviderMock.getActiveSoftwareDetails(Arrays.asList(neName))).thenReturn(activeSoftware);
        List<Map<String, Object>> neJobPropertyList = (List<Map<String, Object>>) neJobAttributes.get(ActivityConstants.JOB_PROPERTIES);
        when(jobUpdateServiceMock.updateRunningJobAttributes(neJobId, neJobPropertyList, null)).thenReturn(Boolean.TRUE);
        when(autoGenerateNameValidator.getValidatedAutoGenerateBackupName(Matchers.anyString())).thenReturn("OP_CXP00_RX123");
        String configurationVersionname = "cvname";
        final List<Map<String, Object>> scheduledProperties = new ArrayList<Map<String, Object>>();
        when(periodicityChecker.isJobPeriodic(scheduledProperties)).thenReturn(true);
        try {
            when(networkElementRetrievalBeanMock.getNeType(Matchers.anyString())).thenReturn(neType);
            when(jobStaticDataProviderMock.getJobStaticData(Matchers.eq(mainJobId))).thenReturn(jobStaticDataMock);
            configurationVersionname = objectUnderTest.getConfigurationVersionName(neJobStaticdata, BackupActivityConstants.CV_NAME);
        } catch (JobDataNotFoundException | MoNotFoundException e) {
            LOGGER.error("Exception Occured {} ", e);
        }
        verify(activeSoftwareProviderMock, times(1)).getActiveSoftwareDetails(Arrays.asList(neName));
        verify(jobUpdateServiceMock, times(1)).updateRunningJobAttributes(neJobId, neJobPropertyList, null);
        assertTrue(configurationVersionname.contains("OP_CXP00_RX123"));
    }

    @Test(expected = BackupDataNotFoundException.class)
    public void testGetConfigurationVersionNameShouldThrowBackupDataNotFoundExpnWhenActiveSWIsNull() {

        NEJobStaticData neJobStaticdata = new NEJobStaticData(neJobId, mainJobId, neName, neJobBusinesskey, platform, new Date().getTime(), null);
        neJobAttributes = new HashMap<>();
        final List<Map<String, String>> neJobProperties = new ArrayList<>();
        final Map<String, String> cvNameProperty = new HashMap<>();
        cvNameProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        cvNameProperty.put(ShmConstants.VALUE, "");
        neJobProperties.add(cvNameProperty);
        neJobAttributes.put(ActivityConstants.JOB_PROPERTIES, neJobProperties);
        when(jobConfigServiceRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        mainJobAttr = setMainJobAttributes();
        final List<Map<String, String>> jobProperties = new ArrayList<>();
        final Map<String, String> mainJobPropertyCvName = new HashMap<>();
        mainJobPropertyCvName.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        mainJobPropertyCvName.put(ShmConstants.VALUE, definedCvName);
        jobProperties.add(mainJobPropertyCvName);

        mainJobAttr.put(ActivityConstants.JOB_PROPERTIES, jobProperties);
        when(jobConfigServiceRetryProxyMock.getMainJobAttributes(Matchers.eq(mainJobId))).thenReturn(mainJobAttr);
        Map<String, String> propertyKeyValueMap = new HashMap<>();
        propertyKeyValueMap.put(BackupActivityConstants.CV_NAME, definedCvName);
        propertyKeyValueMap.put(JobPropertyConstants.AUTO_GENERATE_BACKUP, generatebackupName);

        when(jobPropertyUtilsMock.getPropertyValue(Arrays.asList(BackupActivityConstants.CV_NAME, JobPropertyConstants.AUTO_GENERATE_BACKUP), getJobConfigurationDetails(), neName, neType, platform))
                .thenReturn(propertyKeyValueMap);
        when(jobStaticDataMock.getOwner()).thenReturn(operatorName);
        final Map<String, String> activeSoftware = new HashMap<>();
        activeSoftware.put(neName, null);
        when(activeSoftwareProviderMock.getActiveSoftwareDetails(Arrays.asList(neName))).thenReturn(activeSoftware);
        final List<Map<String, Object>> scheduledProperties = new ArrayList<Map<String, Object>>();
        when(periodicityChecker.isJobPeriodic(scheduledProperties)).thenReturn(true);
        try {
            when(networkElementRetrievalBeanMock.getNeType(Matchers.anyString())).thenReturn(neType);
            when(jobStaticDataProviderMock.getJobStaticData(Matchers.eq(mainJobId))).thenReturn(jobStaticDataMock);
            objectUnderTest.getConfigurationVersionName(neJobStaticdata, BackupActivityConstants.CV_NAME);
        } catch (JobDataNotFoundException | MoNotFoundException e) {
            LOGGER.error("Exception Occured is {} ", e);
        }
        verify(activeSoftwareProviderMock, times(1)).getActiveSoftwareDetails(Arrays.asList(neName));
    }

    @Test(expected = BackupDataNotFoundException.class)
    public void testGetConfigurationVersionNameShouldThrowBackupDataNotFoundExpnWhenActiveSWHasProductNumberOnly() {

        NEJobStaticData neJobStaticdata = new NEJobStaticData(neJobId, mainJobId, neName, neJobBusinesskey, platform, new Date().getTime(), null);
        neJobAttributes = new HashMap<>();
        final List<Map<String, String>> neJobProperties = new ArrayList<>();
        final Map<String, String> cvNameProperty = new HashMap<>();
        cvNameProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        cvNameProperty.put(ShmConstants.VALUE, "");
        neJobProperties.add(cvNameProperty);
        neJobAttributes.put(ActivityConstants.JOB_PROPERTIES, neJobProperties);
        when(jobConfigServiceRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        mainJobAttr = setMainJobAttributes();
        final List<Map<String, String>> jobProperties = new ArrayList<>();
        final Map<String, String> mainJobPropertyCvName = new HashMap<>();
        mainJobPropertyCvName.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        mainJobPropertyCvName.put(ShmConstants.VALUE, definedCvName);
        jobProperties.add(mainJobPropertyCvName);

        mainJobAttr.put(ActivityConstants.JOB_PROPERTIES, jobProperties);
        when(jobConfigServiceRetryProxyMock.getMainJobAttributes(Matchers.eq(mainJobId))).thenReturn(mainJobAttr);
        Map<String, String> propertyKeyValueMap = new HashMap<>();
        propertyKeyValueMap.put(BackupActivityConstants.CV_NAME, definedCvName);
        propertyKeyValueMap.put(JobPropertyConstants.AUTO_GENERATE_BACKUP, generatebackupName);

        when(jobPropertyUtilsMock.getPropertyValue(Arrays.asList(BackupActivityConstants.CV_NAME, JobPropertyConstants.AUTO_GENERATE_BACKUP), getJobConfigurationDetails(), neName, neType, platform))
                .thenReturn(propertyKeyValueMap);
        when(jobStaticDataMock.getOwner()).thenReturn(operatorName);
        final Map<String, String> activeSoftware = new HashMap<>();
        activeSoftware.put(neName, "CXP00"); //Active Software is given only a single token and so invalid as it cannot be split into productNumber and productRevison
        when(activeSoftwareProviderMock.getActiveSoftwareDetails(Arrays.asList(neName))).thenReturn(activeSoftware);
        final List<Map<String, Object>> scheduledProperties = new ArrayList<Map<String, Object>>();
        when(periodicityChecker.isJobPeriodic(scheduledProperties)).thenReturn(true);
        try {
            when(networkElementRetrievalBeanMock.getNeType(Matchers.anyString())).thenReturn(neType);
            when(jobStaticDataProviderMock.getJobStaticData(Matchers.eq(mainJobId))).thenReturn(jobStaticDataMock);
            objectUnderTest.getConfigurationVersionName(neJobStaticdata, BackupActivityConstants.CV_NAME);
        } catch (JobDataNotFoundException | MoNotFoundException e) {
            LOGGER.error("Exception Occured : {} ", e);
        }
        verify(activeSoftwareProviderMock, times(1)).getActiveSoftwareDetails(Arrays.asList(neName));
    }
}
