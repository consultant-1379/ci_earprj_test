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
package com.ericsson.oss.services.shm.es.impl.cpp.common;

import static org.junit.Assert.assertEquals;

import java.util.*;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.*;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationVersionUtilsTest {

    @Mock
    @Inject
    JobUpdateService jobUpdateService;

    @Mock
    @Inject
    ConfigurationVersionService configurationVersionService;

    @Mock
    @Inject
    JobConfigurationService jobConfigurationService;

    @Mock
    @Inject
    SystemRecorder systemRecorder;

    @Mock
    @Inject
    CommonCvOperations commonCvOperations;

    @Mock
    @Inject
    DpsReader dpsReader;

    @Mock
    @Inject
    WorkflowInstanceNotifier workflowInstanceNotifier;

    @Mock
    @Inject
    ActivityUtils activityUtil;

    @InjectMocks
    ConfigurationVersionUtils objectUnderTest;

    @Mock
    Notification notification;

    long activityJobId = 123L;
    long neJobId = 123L;
    long mainJobId = 123L;
    long templateJobId = 123L;

    String neName = "Some Ne Name";
    String cvMoFdn = "Some Cv Mo Fdn";
    String configurationVersionName = "Some CV Name";
    String identity = "Some Identity";
    String type = "Standard";
    String operatorName = "Some Operator Name";
    String comment = "Some Comment";
    String jobName = "Some Job Name";
    String currentUpgradePackage = "ManagedElement=1,SwManagement=1,UpgradePackage=CXP102051_1_R4D21";

    int actionId = 5;

    Map<String, Object> templateJobAttr;
    Map<String, Object> mainJobAttr;

    @Test
    public void testGetConfigurationVersionName() {
        final Map<String, Object> activityJobAttr = new HashMap<String, Object>();
        activityJobAttr.put(ShmConstants.ACTIVITY_NE_JOB_ID, 123L);
        activityJobAttr.put(ShmConstants.MAIN_JOB_ID, 123L);
        activityJobAttr.put(ShmConstants.JOBTEMPLATEID, 123L);
        activityJobAttr.put(ShmConstants.NE_NAME, neName);
        activityJobAttr.put(ShmConstants.NAME, "name");
        activityJobAttr.put(ShmConstants.OWNER, "owner");
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, String>> mainJobConfPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> mainJobPropertyCvIdentity = new HashMap<String, String>();
        mainJobPropertyCvIdentity.put(ShmConstants.KEY, BackupActivityConstants.CV_IDENTITY);
        mainJobPropertyCvIdentity.put(ShmConstants.VALUE, identity);
        mainJobConfPropertyList.add(mainJobPropertyCvIdentity);
        final Map<String, String> mainJobPropertyCvType = new HashMap<String, String>();
        mainJobPropertyCvType.put(ShmConstants.KEY, BackupActivityConstants.CV_TYPE);
        mainJobPropertyCvType.put(ShmConstants.VALUE, type);
        mainJobConfPropertyList.add(mainJobPropertyCvType);
        jobConfigurationDetails.put(ActivityConstants.JOB_PROPERTIES, mainJobConfPropertyList);
        activityJobAttr.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        final List<Map<String, String>> mainJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> mainJobProperty = new HashMap<String, String>();
        mainJobProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        mainJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        mainJobPropertyList.add(mainJobProperty);
        activityJobAttr.put(ActivityConstants.JOB_PROPERTIES, mainJobPropertyList);
        assertEquals(configurationVersionName, objectUnderTest.getConfigurationVersionName(activityJobAttr, activityJobAttr));
    }

    @Test
    public void testGetConfigurationVersionNameFromNeProperties() {
        final Map<String, Object> activityJobAttr = new HashMap<String, Object>();
        activityJobAttr.put(ShmConstants.ACTIVITY_NE_JOB_ID, 123L);
        activityJobAttr.put(ShmConstants.MAIN_JOB_ID, 123L);
        activityJobAttr.put(ShmConstants.JOBTEMPLATEID, 123L);
        activityJobAttr.put(ShmConstants.NE_NAME, neName);
        activityJobAttr.put(ShmConstants.NAME, "name");
        activityJobAttr.put(ShmConstants.OWNER, "owner");
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, String>> mainJobConfPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> mainJobPropertyCvIdentity = new HashMap<String, String>();
        mainJobPropertyCvIdentity.put(ShmConstants.KEY, BackupActivityConstants.CV_IDENTITY);
        mainJobPropertyCvIdentity.put(ShmConstants.VALUE, identity);
        mainJobConfPropertyList.add(mainJobPropertyCvIdentity);
        final Map<String, String> mainJobPropertyCvType = new HashMap<String, String>();
        mainJobPropertyCvType.put(ShmConstants.KEY, BackupActivityConstants.CV_TYPE);
        mainJobPropertyCvType.put(ShmConstants.VALUE, type);
        mainJobConfPropertyList.add(mainJobPropertyCvType);
        jobConfigurationDetails.put(ActivityConstants.JOB_PROPERTIES, mainJobConfPropertyList);
        activityJobAttr.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        final List<Map<String, String>> mainJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> mainJobProperty = new HashMap<String, String>();
        mainJobProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        mainJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        mainJobPropertyList.add(mainJobProperty);
        activityJobAttr.put(ActivityConstants.JOB_PROPERTIES, mainJobPropertyList);
        assertEquals(configurationVersionName, objectUnderTest.getConfigurationVersionName(null, activityJobAttr));
    }

    @Test
    public void testGetConfigurationVersionNameFromNullNeProperties() {
        final Map<String, Object> activityJobAttr = new HashMap<String, Object>();
        activityJobAttr.put(ShmConstants.ACTIVITY_NE_JOB_ID, 123L);
        activityJobAttr.put(ShmConstants.MAIN_JOB_ID, 123L);
        activityJobAttr.put(ShmConstants.JOBTEMPLATEID, 123L);
        activityJobAttr.put(ShmConstants.NE_NAME, neName);
        activityJobAttr.put(ShmConstants.NAME, "name");
        activityJobAttr.put(ShmConstants.OWNER, "owner");
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, String>> mainJobConfPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> mainJobPropertyCvIdentity = new HashMap<String, String>();
        mainJobPropertyCvIdentity.put(ShmConstants.KEY, BackupActivityConstants.CV_IDENTITY);
        mainJobPropertyCvIdentity.put(ShmConstants.VALUE, identity);
        mainJobConfPropertyList.add(mainJobPropertyCvIdentity);
        final Map<String, String> mainJobPropertyCvType = new HashMap<String, String>();
        mainJobPropertyCvType.put(ShmConstants.KEY, BackupActivityConstants.CV_TYPE);
        mainJobPropertyCvType.put(ShmConstants.VALUE, type);
        mainJobConfPropertyList.add(mainJobPropertyCvType);
        jobConfigurationDetails.put(ActivityConstants.JOB_PROPERTIES, mainJobConfPropertyList);
        activityJobAttr.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        final List<Map<String, String>> mainJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> mainJobProperty = new HashMap<String, String>();
        mainJobProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        mainJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        mainJobPropertyList.add(mainJobProperty);
        activityJobAttr.put(ActivityConstants.JOB_PROPERTIES, mainJobPropertyList);
        assertEquals(null, objectUnderTest.getConfigurationVersionName(null, null));
    }

    @Test
    public void testGetConfigurationVersionNameToRestore() {
        final Map<String, Object> activityJobAttr = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        jobProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        jobProperty.put(ShmConstants.VALUE, configurationVersionName);
        jobPropertyList.add(jobProperty);

        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        neJobProperty.put(ShmConstants.NE_NAME, neName);
        neJobProperty.put(ShmConstants.JOBPROPERTIES, jobPropertyList);
        neJobPropertyList.add(neJobProperty);

        activityJobAttr.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);

        final Map<String, Object> mainJobProperties = new HashMap<String, Object>();
        mainJobProperties.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        final String key = BackupActivityConstants.CV_NAME;
        assertEquals(configurationVersionName, objectUnderTest.getNeJobPropertyValue(mainJobProperties, neName, key));
    }

    @Test
    public void testGetcvActivityShouldReturnCvActivity() {
        final Map<String, Object> cvMOAttr = new HashMap<String, Object>();
        cvMOAttr.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.VERIFYING_DOWNLOADED_BACKUP_CV_BEFORE_RESTORE.name());
        cvMOAttr.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.AWAITING_RESTORE_CONFIRMATION.name());
        final CvActivity cvActivity = new CvActivity(CVCurrentMainActivity.VERIFYING_DOWNLOADED_BACKUP_CV_BEFORE_RESTORE, CVCurrentDetailedActivity.AWAITING_RESTORE_CONFIRMATION);
        final CvActivity returnedCvActivity = objectUnderTest.getCvActivity(cvMOAttr);
        org.junit.Assert.assertTrue(returnedCvActivity.getMainActivityDesc().equalsIgnoreCase(cvActivity.getMainActivityDesc()));
        org.junit.Assert.assertTrue(returnedCvActivity.getDetailedActivityDesc().equalsIgnoreCase(cvActivity.getDetailedActivityDesc()));

    }

    @Test
    public void testgetActionId() {
        int actionId = -1;
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityProperty = new HashMap<String, Object>();
        activityProperty.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_ID);
        activityProperty.put(ActivityConstants.JOB_PROP_VALUE, "1111");
        activityJobPropertyList.add(activityProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        assertEquals(1111, objectUnderTest.getActionId(activityJobAttributes));

    }

    @Test
    public void testgetCorrputedUps() {

        final Map<String, Object> cvMOAttributes = new HashMap<String, Object>();
        List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityProperty = new HashMap<String, Object>();
        activityProperty.put(UpgradeActivityConstants.UP_PO_PROD_REVISION, "REVISION_1");
        activityProperty.put(UpgradeActivityConstants.UP_PO_PROD_NUMBER, "1111");
        activityProperty.put(UpgradeActivityConstants.UP_PO_PROD_NAME, "ECIM");
        activityProperty.put(UpgradeActivityConstants.UP_PO_PROD_DATE, "09072015");
        activityProperty.put(UpgradeActivityConstants.UP_PO_PROD_INFO, "BASIC");

        activityJobPropertyList.add(activityProperty);
        cvMOAttributes.put(ConfigurationVersionMoConstants.CORRUPTED_UPS, activityJobPropertyList);
        objectUnderTest.getCorrputedUps(cvMOAttributes);

    }

    @Test
    public void testgetMissingUps() {

        final Map<String, Object> cvMOAttributes = new HashMap<String, Object>();
        List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityProperty = new HashMap<String, Object>();
        activityProperty.put(UpgradeActivityConstants.UP_PO_PROD_REVISION, "REVISION_1");
        activityProperty.put(UpgradeActivityConstants.UP_PO_PROD_NUMBER, "1111");
        activityProperty.put(UpgradeActivityConstants.UP_PO_PROD_NAME, "ECIM");
        activityProperty.put(UpgradeActivityConstants.UP_PO_PROD_DATE, "09072015");
        activityProperty.put(UpgradeActivityConstants.UP_PO_PROD_INFO, "BASIC");

        activityJobPropertyList.add(activityProperty);
        cvMOAttributes.put(ConfigurationVersionMoConstants.MISSING_UPS, activityJobPropertyList);
        objectUnderTest.getMissingUps(cvMOAttributes);

    }
}
