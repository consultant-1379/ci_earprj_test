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
package com.ericsson.oss.services.shm.es.impl.cpp.licensing;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.HANDLE_TIMEOUT_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.MOACTION_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.MOACTION_START_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_START_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.UPDATE_LICENSE_MOACTION_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.FINGER_PRINT;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.LAST_LICENSING_PI_CHANGE;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.LICENSE_DATA_FINGERPRINT;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.LICENSE_DATA_LICENSE_KEYFILE_PATH;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.LICENSE_DATA_SEQUENCE_NUMBER;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.LICENSE_FILE_PATH;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.LICENSE_MO;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.NODE_NAME;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.PI_TIME_STAMP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.xml.sax.SAXException;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy.RetryPolicyBuilder;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.ProductTypeProviderImpl;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.retry.DpsRetryConfigurationParamProvider;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.DpsWriterRetryProxy;
import com.ericsson.oss.services.shm.common.smrs.SmrsAccountInfo;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.common.smrs.SmrsServiceConstants;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.api.CommonLicensingActivityConstants;
import com.ericsson.oss.services.shm.es.api.DefaultActionRetryPolicy;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.license.LicenseKeyFileDeleteService;
import com.ericsson.oss.services.shm.es.impl.license.LicensingRetryService;
import com.ericsson.oss.services.shm.es.impl.license.LicensingService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.moaction.retry.ActionRetryPolicy;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.notifications.impl.license.LicenseUtil;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

/**
 * This class tests the installation of license key files of CPP based node.
 * 
 * @author xmanush
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(RetryPolicy.class)
public class InstallLicenseKeyFileServiceTest {

    @InjectMocks
    InstallLicenseKeyFileService installLicenseKeyFileServiceMock;

    @Mock
    @Inject
    DpsReader dpsReaderMock;

    @Mock
    @Inject
    SystemRecorder systemRecorderMock;

    @Mock
    @Inject
    JobConfigurationService jobConfigurationService;

    @Mock
    NotificationRegistry notificationRegistryMock;

    @Mock
    @Inject
    SmrsFileStoreService smrsServiceUtil;

    @Mock
    Notification notificationmock;

    @Mock
    @Inject
    DpsAttributeChangedEvent avcEvent;

    @Mock
    WorkflowInstanceNotifier workflowInstanceNotifierMock;

    @Mock
    SimpleDateFormat formatterMock;

    @Mock
    @Inject
    DpsWriterRetryProxy dpsWriterMock;

    @Mock
    @Inject
    LicenseKeyFileDeleteService licenseKeyFileDeleteServiceMock;

    @Mock
    List<PersistenceObject> persistenceObjectList;

    @Mock
    PersistenceObject poAttributes;

    @Mock
    List<ManagedObject> managedObjectsList;

    @Mock
    ManagedObject licPO;

    @Mock
    PersistenceObject mainJobPO;

    @Mock
    PersistenceObject persistenceObject;

    @Mock
    PersistenceObject activityPO;

    @Mock
    PersistenceObject neJobPO;

    @Mock
    LicensingRetryService licensingRetryService;

    @Mock
    @Inject
    ActivityUtils activityUtils;

    @Mock
    @Inject
    JobUpdateService jobUpdateService;

    @Mock
    private DpsRetryPolicies dpsRetryPolicies;

    @Mock
    @Inject
    FdnNotificationSubject fdnNotificationSubject;

    @Mock
    @Inject
    JobPropertyUtils jobPropertyUtils;

    @Mock
    private List<NetworkElement> neElementListMock;

    @Mock
    private NetworkElement neElementMock;

    @Mock
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    @Mock
    private LicensingService licensingService;

    @Mock
    NetworkElement networkElement;

    @Mock
    ActivityTimeoutsService activityTimeoutsService;

    @Mock
    private LicenseUtil licenseUtil;

    @Mock
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculator;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    private NetworkElementData networkElementInfoMock;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Mock
    private JobConfigurationServiceRetryProxy jobConfigurationServiceRetryProxyMock;

    @Mock
    private JobLogUtil jobLogUtil;

    @Mock
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Mock
    private JobStaticData jobStaticDataMock;

    @Mock
    private JobStaticDataProvider jobStaticDataProvider;

    final long activityJobId = 281474976740953L;
    final long neJobId = 281475014606261L;
    final long mainJobId = 281475014606250L;
    final long JOBTEMPLATEID = 0L;
    final String neName = "ERBS00006";
    final String neType = "ERBS";
    final String licenseMOFdn = "MeContext=ERBS00006,ManagedElement=1,SystemFunctions=1, Licensing=1";
    final String licenseFilePath = "Some License File Path";
    final String fingerPrint = "Some FingerPrint";
    final String jobExecutionUser = "TEST_USER";

    String lastLicensePiChange = "lastLicensePiChange";

    final String businessKey = "Some Business Key";

    Map<String, Object> neAttributes;

    @Mock
    private RetryPolicyBuilder retryPolicyBuilderMock;

    @Mock
    private DpsRetryConfigurationParamProvider dpsConfigMock;

    @Mock
    private RetryPolicy retryPolicyMock;

    @Mock
    private RetryManager retryManagerMock;

    @Mock
    private JobActivityInfo jobActivityInfoMock;

    @Mock
    @DefaultActionRetryPolicy
    private ActionRetryPolicy moActionRetryPolicy;

    @Mock
    private ProductTypeProviderImpl productTypeProviderImpl;

    @Mock
    private Date dateMock;

    @SuppressWarnings("unchecked")
    private final Class<? extends Exception>[] exceptionsArray = new Class[] {};

    @Before
    public void setup() {
        when(licensingRetryService.getLicenseMoFdn(activityJobId)).thenReturn(licenseMOFdn);
        when(jobConfigurationServiceRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neAttributes);
        when(jobConfigurationServiceRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neAttributes);
    }

    @SuppressWarnings("unchecked")
    private void requiredTestData() throws MoNotFoundException, JobDataNotFoundException {
        //    when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(networkElementRetrievalBean.getNetworkElementData(neName)).thenReturn(networkElementInfoMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(getActivityJobPo());
        when(licensingRetryService.getLicenseMoFdn(activityJobId)).thenReturn(licenseMOFdn);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        final Map<String, Object> licenseMOAttributes = new HashMap<String, Object>();
        licenseMOAttributes.put(FINGER_PRINT, "Some FingerPrint");
        final Map<String, Object> licenseMoWithFDN = new HashMap<String, Object>();
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);
        final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("fingerPrint", "Some FingerPrint");
        map.put("sequenceNumber", "1000");
        map.put("productType", "LTE");
        list.add(map);
        when(licensingRetryService.getAttributesListOfLicensePOs(anyMap())).thenReturn(list);
        neAttributes = new HashMap<String, Object>();
        neAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neAttributes.put(ShmConstants.NE_NAME, neName);
        neAttributes.put(ShmConstants.NETYPE, neType);
        activityUtils.getPoAttributes(neJobId);
        when(jobConfigurationServiceRetryProxyMock.getNeJobAttributes(activityJobId)).thenReturn(neAttributes);
    }

    @Test
    public void testPreCheckShouldPass() throws IOException, SAXException, MoNotFoundException, JobDataNotFoundException {
        requiredTestData();
        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProperty.put(ShmConstants.VALUE, licenseFilePath);
        jobPropertyList.add(jobProperty);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);

        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttribtues);
        when(productTypeProviderImpl.getProductType("")).thenReturn("Baseband");
        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);
        final List<Map<String, Object>> poAttributesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(LICENSE_DATA_FINGERPRINT, fingerPrint);
        poAttributes.put(CommonLicensingActivityConstants.PRODUCTTYPE, "LTE");
        poAttributesList.add(poAttributes);
        when(licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes)).thenReturn(poAttributesList);
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, installLicenseKeyFileServiceMock.precheck(activityJobId).getActivityResultEnum());
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPreCheckShouldFailWithRadioNodeLKF() throws IOException, SAXException, MoNotFoundException, JobDataNotFoundException {
        requiredTestData();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("fingerPrint", "Some FingerPrint");
        map.put("productType", "Baseband");
        list.add(map);
        when(licensingRetryService.getAttributesListOfLicensePOs(anyMap())).thenReturn(list);
        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProperty.put(ShmConstants.VALUE, licenseFilePath);
        jobPropertyList.add(jobProperty);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttribtues);
        when(productTypeProviderImpl.getProductType("")).thenReturn("Baseband");
        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);
        final List<Map<String, Object>> poAttributesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(LICENSE_DATA_FINGERPRINT, fingerPrint);
        poAttributes.put(CommonLicensingActivityConstants.PRODUCTTYPE, "Baseband");
        poAttributesList.add(poAttributes);
        when(licenseUtil.isRadioNodeLKF("Baseband", "")).thenReturn(true);
        when(licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes)).thenReturn(poAttributesList);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, installLicenseKeyFileServiceMock.precheck(activityJobId).getActivityResultEnum());
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPreCheckIfProductTypeOfLKFIsNull() throws IOException, SAXException, MoNotFoundException, JobDataNotFoundException {
        requiredTestData();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("fingerPrint", "Some FingerPrint");
        map.put("productType", null);
        list.add(map);
        when(licensingRetryService.getAttributesListOfLicensePOs(anyMap())).thenReturn(list);
        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProperty.put(ShmConstants.VALUE, licenseFilePath);
        jobPropertyList.add(jobProperty);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(neJobStaticData.getMainJobId())).thenReturn(mainJobAttribtues);
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(mainJobAttribtues);
        when(productTypeProviderImpl.getProductType("")).thenReturn("Baseband");
        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);
        final List<Map<String, Object>> poAttributesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(LICENSE_DATA_FINGERPRINT, fingerPrint);
        poAttributes.put(CommonLicensingActivityConstants.PRODUCTTYPE, null);
        poAttributesList.add(poAttributes);
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes)).thenReturn(poAttributesList);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, installLicenseKeyFileServiceMock.precheck(activityJobId).getActivityResultEnum());
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
    }

    @Test
    public void testAsyncPreCheckShouldPass() throws IOException, SAXException, MoNotFoundException, JobDataNotFoundException {
        requiredTestData();
        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProperty.put(ShmConstants.VALUE, licenseFilePath);
        jobPropertyList.add(jobProperty);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttribtues);
        getSMRSDetails();

        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);
        final List<Map<String, Object>> poAttributesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(LICENSE_DATA_FINGERPRINT, fingerPrint);
        poAttributes.put(CommonLicensingActivityConstants.PRODUCTTYPE, "LTE");
        poAttributesList.add(poAttributes);
        when(licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes)).thenReturn(poAttributesList);
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        installLicenseKeyFileServiceMock.asyncPrecheck(activityJobId);
        activityUtils.buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.INSTALL_LICENSE, ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        verify(activityUtils, times(1)).buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.INSTALL_LICENSE,
                ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
    }

    @Test
    public void testPreCheckShouldFailAsNoMatchingFingerPrintFound() throws IOException, SAXException, MoNotFoundException, JobDataNotFoundException {
        requiredTestData();

        final Map<String, Object> licenseMOAttributes = new HashMap<String, Object>();
        final Map<String, Object> licenseMO = new HashMap<String, Object>();
        licenseMOAttributes.put(FINGER_PRINT, "Some FingerPrint2");
        licenseMO.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMO);

        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProperty.put(ShmConstants.VALUE, licenseFilePath);
        jobPropertyList.add(jobProperty);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttribtues);
        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);
        final List<Map<String, Object>> poAttributesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(LICENSE_DATA_FINGERPRINT, fingerPrint);
        when(licenseUtil.isRadioNodeLKF("LTE", "RadioNode")).thenReturn(false);
        poAttributesList.add(poAttributes);
        when(licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes)).thenReturn(poAttributesList);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, installLicenseKeyFileServiceMock.precheck(activityJobId).getActivityResultEnum());
    }

    @Test
    public void testAsyncPreCheckShouldFailAsNoMatchingFingerPrintFound() throws IOException, SAXException, MoNotFoundException, JobDataNotFoundException {
        requiredTestData();
        final Map<String, Object> licenseMOAttributes = new HashMap<String, Object>();
        final Map<String, Object> licenseMO = new HashMap<String, Object>();
        licenseMOAttributes.put(FINGER_PRINT, "Some FingerPrint2");
        licenseMO.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMO);
        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProperty.put(ShmConstants.VALUE, licenseFilePath);
        jobPropertyList.add(jobProperty);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttribtues);
        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);
        final List<Map<String, Object>> poAttributesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(LICENSE_DATA_FINGERPRINT, fingerPrint);
        poAttributesList.add(poAttributes);
        when(licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes)).thenReturn(poAttributesList);
        installLicenseKeyFileServiceMock.asyncPrecheck(activityJobId);
        verify(activityUtils, times(1)).buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.INSTALL_LICENSE,
                ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
    }

    @Test
    public void testPreCheckShouldFailAsMOIsNull() throws IOException, SAXException, MoNotFoundException, JobDataNotFoundException {
        requiredTestData();
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("ECIM");
        networkElement.setPlatformType(PlatformTypeEnum.CPP);
        when(licensingRetryService.getLicenseMoFdn(activityJobId)).thenReturn(null);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, installLicenseKeyFileServiceMock.precheck(activityJobId).getActivityResultEnum());
    }

    @Test
    public void testPrecheck_WhenRetrivingLicenseMOThrowsException_Fail() throws IOException, SAXException, MoNotFoundException, JobDataNotFoundException {
        requiredTestData();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final String logMessage = ActivityConstants.INSTALL_LICENSE + " Precheck failed." + String.format(JobLogConstants.FAILURE_REASON, "");
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("ERBS");
        networkElement.setPlatformType(PlatformTypeEnum.CPP);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, "install")).thenReturn(true);
        when(licensingRetryService.getLicenseMoFdn(activityJobId)).thenThrow(Exception.class);
        when(activityUtils.isTreatAs(neName)).thenReturn("");
        when(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId())).thenReturn(jobExecutionUser);
        installLicenseKeyFileServiceMock.execute(activityJobId);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
        verify(activityUtils, times(1)).recordEvent(jobExecutionUser, SHMEvents.LICENSE_INSTALL_PRECHECK, neName, licenseMOFdn,
                activityUtils.additionalInfoForEvent(activityJobId, neName, logMessage));
    }

    @Test
    public void testAsyncPreCheckShouldFailAsMOIsNull() throws IOException, SAXException, JobDataNotFoundException {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("ECIM");
        networkElement.setPlatformType(PlatformTypeEnum.CPP);
        when(licensingRetryService.getLicenseMoFdn(activityJobId)).thenReturn(null);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        installLicenseKeyFileServiceMock.asyncPrecheck(activityJobId);
        verify(activityUtils, times(1)).buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.INSTALL_LICENSE,
                ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        Mockito.verify(activityAndNEJobProgressCalculator, times(0)).updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_START_PROGRESS_PERCENTAGE);
    }

    @Test
    public void testPreCheckShouldFailAsNoDataInPO() throws IOException, SAXException, MoNotFoundException, JobDataNotFoundException {
        requiredTestData();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> licenseMOAttributes = new HashMap<String, Object>();
        licenseMOAttributes.put(FINGER_PRINT, "Some FingerPrint2");
        final Map<String, Object> licenseMoWithFDN = new HashMap<String, Object>();
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);

        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProperty.put(ShmConstants.VALUE, licenseFilePath);
        jobPropertyList.add(jobProperty);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttribtues);
        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);
        final List<Map<String, Object>> poAttributesList = new ArrayList<Map<String, Object>>();
        when(licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes)).thenReturn(poAttributesList);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, installLicenseKeyFileServiceMock.precheck(activityJobId).getActivityResultEnum());
    }

    @Test
    public void testAsyncPreCheckShouldFailAsNoDataInPO() throws IOException, SAXException, JobDataNotFoundException, MoNotFoundException {
        requiredTestData();
        final Map<String, Object> licenseMOAttributes = new HashMap<String, Object>();
        licenseMOAttributes.put(FINGER_PRINT, "Some FingerPrint2");
        final Map<String, Object> licenseMoWithFDN = new HashMap<String, Object>();
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);

        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProperty.put(ShmConstants.VALUE, licenseFilePath);
        jobPropertyList.add(jobProperty);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttribtues);
        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);
        final List<Map<String, Object>> poAttributesList = new ArrayList<Map<String, Object>>();
        when(licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes)).thenReturn(poAttributesList);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        installLicenseKeyFileServiceMock.asyncPrecheck(activityJobId);
        verify(activityUtils, times(1)).buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.INSTALL_LICENSE,
                ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInstallShouldPassWhenrequestFromUI() throws JobDataNotFoundException, MoNotFoundException {
        mockJobActivityInfo();
        requiredTestData();
        final List<Map<String, Object>> jobPropertyList1 = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(getActivityJobPo());
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ShmConstants.NE_NAME, neName);
        neJobAttributes.put(ShmConstants.NETYPE, neType);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, businessKey);
        when(jobConfigurationServiceRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        final Map<String, Object> licenseMOAttributes = new HashMap<String, Object>();
        licenseMOAttributes.put(LAST_LICENSING_PI_CHANGE, lastLicensePiChange);
        final Map<String, Object> licenseMoWithFDN = new HashMap<String, Object>();
        licenseMoWithFDN.put(ShmConstants.FDN, licenseMOFdn);
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        getSMRSDetails();
        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProperty.put(ShmConstants.VALUE, "/home/smrs/path");
        jobPropertyList.add(jobProperty);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        licenseMOAttributes.put(FINGER_PRINT, "Some FingerPrint");
        licenseMOAttributes.put(LAST_LICENSING_PI_CHANGE, "141021_131638");
        licenseMOAttributes.put(LICENSE_DATA_SEQUENCE_NUMBER, "1000");
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        when(jobUpdateService.retrieveJobWithRetry(activityJobId)).thenReturn(activityJobAttributes);
        neAttributes = new HashMap<String, Object>();
        neAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neAttributes.put(ShmConstants.NE_NAME, neName);
        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(neAttributes);
        when(networkElementRetrievalBean.getNetworkElementData(neName)).thenReturn(networkElementInfoMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(networkElementRetrievalBean.getNeType(Matchers.anyString())).thenReturn("ERBS");
        when(networkElementInfoMock.getNeType()).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.CPP.toString());
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProp = new HashMap<String, String>();
        jobProp.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProp.put(ShmConstants.VALUE, licenseFilePath);
        jobPropList.add(jobProp);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropList);
        neJobPropertyList.add(neJobProperty);
        mainJobAttribtues.put(ShmConstants.JOB_TEMPLATE_ID, JOBTEMPLATEID);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        final Map<String, Object> jobTemplateAttribtues = new HashMap<String, Object>();
        jobTemplateAttribtues.put(ShmConstants.JOB_CATEGORY, "UI");
        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(Matchers.anyLong())).thenReturn(mainJobAttribtues);
        when(jobConfigurationServiceRetryProxyMock.getPOAttributes(Matchers.anyLong())).thenReturn(jobTemplateAttribtues);

        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);
        final List<Map<String, Object>> poAttributesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(LICENSE_DATA_FINGERPRINT, fingerPrint);
        poAttributes.put(LICENSE_DATA_SEQUENCE_NUMBER, "1000");
        poAttributesList.add(poAttributes);
        when(licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes)).thenReturn(poAttributesList);
        when(licensingRetryService.updateLicenseInstalledTime(restrictionAttributes)).thenReturn(true);
        when(licenseKeyFileDeleteServiceMock.deleteHistoricLicensePOs(fingerPrint, "sequenceNumber")).thenReturn("ss");
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, "install")).thenReturn(true);
        when(licensingRetryService.getNodeSequenceNumber(Matchers.anyString())).thenReturn("900");
        installLicenseKeyFileServiceMock.execute(activityJobId);
        verify(activityUtils, times(1)).sendNotificationToWFS(neJobStaticData, activityJobId, "execute", null);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, jobPropertyList1, PRECHECK_END_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, jobPropertyList1, null, MOACTION_START_PROGRESS_PERCENTAGE);
        //activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList1, null, UPDATE_LICENSE_MOACTION_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, jobPropertyList1, jobPropertyList1, UPDATE_LICENSE_MOACTION_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, jobPropertyList1, null, MOACTION_END_PROGRESS_PERCENTAGE);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInstallShouldSkipWhenrequestFromCLI() throws JobDataNotFoundException, MoNotFoundException {
        mockJobActivityInfo();
        requiredTestData();
        final List<Map<String, Object>> jobPropertyList1 = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(getActivityJobPo());
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ShmConstants.NE_NAME, neName);
        neJobAttributes.put(ShmConstants.NETYPE, neType);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, businessKey);
        when(jobConfigurationServiceRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        final Map<String, Object> licenseMOAttributes = new HashMap<String, Object>();
        licenseMOAttributes.put(LAST_LICENSING_PI_CHANGE, lastLicensePiChange);
        final Map<String, Object> licenseMoWithFDN = new HashMap<String, Object>();
        licenseMoWithFDN.put(ShmConstants.FDN, licenseMOFdn);
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        getSMRSDetails();
        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProperty.put(ShmConstants.VALUE, "/home/smrs/path");
        jobPropertyList.add(jobProperty);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        licenseMOAttributes.put(FINGER_PRINT, "Some FingerPrint");
        licenseMOAttributes.put(LAST_LICENSING_PI_CHANGE, "141021_131638");
        licenseMOAttributes.put(LICENSE_DATA_SEQUENCE_NUMBER, "1000");
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        when(jobUpdateService.retrieveJobWithRetry(activityJobId)).thenReturn(activityJobAttributes);
        neAttributes = new HashMap<String, Object>();
        neAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neAttributes.put(ShmConstants.NE_NAME, neName);
        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(neAttributes);
        when(networkElementRetrievalBean.getNetworkElementData(neName)).thenReturn(networkElementInfoMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(networkElementRetrievalBean.getNeType(Matchers.anyString())).thenReturn("ERBS");
        when(networkElementInfoMock.getNeType()).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.CPP.toString());
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProp = new HashMap<String, String>();
        jobProp.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProp.put(ShmConstants.VALUE, licenseFilePath);
        jobPropList.add(jobProp);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropList);
        neJobPropertyList.add(neJobProperty);
        mainJobAttribtues.put(ShmConstants.JOB_TEMPLATE_ID, 10l);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        final Map<String, Object> jobTemplateAttribtues = new HashMap<String, Object>();
        jobTemplateAttribtues.put(ShmConstants.JOB_CATEGORY, "CLI");
        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(Matchers.anyLong())).thenReturn(mainJobAttribtues);
        when(jobConfigurationServiceRetryProxyMock.getPOAttributes(Matchers.anyLong())).thenReturn(jobTemplateAttribtues);

        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);
        final List<Map<String, Object>> poAttributesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(LICENSE_DATA_FINGERPRINT, fingerPrint);
        poAttributes.put(LICENSE_DATA_SEQUENCE_NUMBER, "900");
        poAttributesList.add(poAttributes);
        when(licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes)).thenReturn(poAttributesList);
        when(licensingRetryService.updateLicenseInstalledTime(restrictionAttributes)).thenReturn(true);
        when(licenseKeyFileDeleteServiceMock.deleteHistoricLicensePOs(fingerPrint, "sequenceNumber")).thenReturn("ss");
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, "install")).thenReturn(true);
        when(licensingRetryService.getNodeSequenceNumber(Matchers.anyString())).thenReturn("1000");
        installLicenseKeyFileServiceMock.execute(activityJobId);
        verify(activityUtils, times(1)).addJobProperty(ActivityConstants.ACTIVITY_RESULT, "SKIPPED", jobPropertyList1);
        verify(activityUtils, times(1)).sendNotificationToWFS(neJobStaticData, activityJobId, "execute", null);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_START_PROGRESS_PERCENTAGE);
    }

    @Test
    public void testInstallShouldSkipWhenrequestFromCLISuccessFullExecute() throws JobDataNotFoundException, MoNotFoundException {
        mockJobActivityInfo();
        requiredTestData();
        final List<Map<String, Object>> jobPropertyList1 = setDataForJobThroughCli(true);
        when(licensingRetryService.getLicenseKeyFilePathFromNeJob(neJobStaticData.getNeJobId())).thenReturn("/home/smrs/path");
        installLicenseKeyFileServiceMock.execute(activityJobId);
        verify(activityUtils, times(1)).addJobProperty(ActivityConstants.ACTIVITY_RESULT, "SUCCESS", jobPropertyList1);
        verify(activityUtils, times(1)).sendNotificationToWFS(neJobStaticData, activityJobId, "execute", null);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_START_PROGRESS_PERCENTAGE);
    }

    @Test
    public void testInstallShouldSkipWhenrequestFromCLIOnLicensePathNotFoundInJobConfig() throws JobDataNotFoundException, MoNotFoundException {
        mockJobActivityInfo();
        requiredTestData();
        final List<Map<String, Object>> jobPropertyList1 = setDataForJobThroughCli(false);
        installLicenseKeyFileServiceMock.execute(activityJobId);
        verify(activityUtils, times(1)).addJobProperty(ActivityConstants.ACTIVITY_RESULT, "FAILED", jobPropertyList1);
        verify(activityUtils, times(1)).sendNotificationToWFS(neJobStaticData, activityJobId, "execute", null);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_START_PROGRESS_PERCENTAGE);
    }

    private List<Map<String, Object>> setDataForJobThroughCli(final boolean setLicensePath) throws JobDataNotFoundException, MoNotFoundException {
        final List<Map<String, Object>> jobPropertyList1 = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(getActivityJobPo());
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ShmConstants.NE_NAME, neName);
        neJobAttributes.put(ShmConstants.NETYPE, neType);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, businessKey);
        when(jobConfigurationServiceRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        final Map<String, Object> licenseMOAttributes = new HashMap<String, Object>();
        licenseMOAttributes.put(LAST_LICENSING_PI_CHANGE, lastLicensePiChange);
        final Map<String, Object> licenseMoWithFDN = new HashMap<String, Object>();
        licenseMoWithFDN.put(ShmConstants.FDN, licenseMOFdn);
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        getSMRSDetails();
        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobPropertyList.add(jobProperty);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        licenseMOAttributes.put(FINGER_PRINT, "Some FingerPrint");
        licenseMOAttributes.put(LAST_LICENSING_PI_CHANGE, "141021_131638");
        licenseMOAttributes.put(LICENSE_DATA_SEQUENCE_NUMBER, "10000");
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        when(jobUpdateService.retrieveJobWithRetry(activityJobId)).thenReturn(activityJobAttributes);
        neAttributes = new HashMap<String, Object>();
        neAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neAttributes.put(ShmConstants.NE_NAME, neName);
        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(neAttributes);
        when(networkElementRetrievalBean.getNetworkElementData(neName)).thenReturn(networkElementInfoMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(networkElementRetrievalBean.getNeType(Matchers.anyString())).thenReturn("ERBS");
        when(networkElementInfoMock.getNeType()).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.CPP.toString());
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProp = new HashMap<String, String>();
        jobPropList.add(jobProp);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropList);
        neJobPropertyList.add(neJobProperty);
        mainJobAttribtues.put(ShmConstants.JOB_TEMPLATE_ID, 10l);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        final Map<String, Object> jobTemplateAttribtues = new HashMap<String, Object>();
        jobTemplateAttribtues.put(ShmConstants.JOB_CATEGORY, "CLI");
        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(Matchers.anyLong())).thenReturn(mainJobAttribtues);

        final Map<String, Object> neJobProperties = new HashMap<String, Object>();
        neJobProperties.put(ShmJobConstants.JOBPROPERTIES, getNeJobPO(neJobId, mainJobId, setLicensePath));
        when(jobConfigurationServiceRetryProxyMock.getNeJobAttributes(Matchers.anyLong())).thenReturn(neJobProperties);

        when(jobConfigurationServiceRetryProxyMock.getPOAttributes(Matchers.anyLong())).thenReturn(jobTemplateAttribtues);

        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);
        final List<Map<String, Object>> poAttributesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(LICENSE_DATA_FINGERPRINT, fingerPrint);
        poAttributes.put(LICENSE_DATA_SEQUENCE_NUMBER, "90000");
        poAttributesList.add(poAttributes);
        when(licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes)).thenReturn(poAttributesList);
        when(licensingRetryService.updateLicenseInstalledTime(restrictionAttributes)).thenReturn(true);
        when(licenseKeyFileDeleteServiceMock.deleteHistoricLicensePOs(fingerPrint, "sequenceNumber")).thenReturn("ss");
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, "install")).thenReturn(true);
        when(licensingRetryService.getNodeSequenceNumber(Matchers.anyString())).thenReturn("1");
        return jobPropertyList1;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteShouldFailWithException() throws MoNotFoundException, JobDataNotFoundException {
        requiredTestData();
        final List<Map<String, Object>> jobPropertyList1 = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        mockJobActivityInfo();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ShmConstants.NE_NAME, neName);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, businessKey);
        when(jobConfigurationServiceRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(getActivityJobPo());
        final Map<String, Object> licenseMOAttributes = new HashMap<String, Object>();
        licenseMOAttributes.put(LAST_LICENSING_PI_CHANGE, lastLicensePiChange);
        final Map<String, Object> licenseMoWithFDN = new HashMap<String, Object>();
        licenseMoWithFDN.put(ShmConstants.FDN, licenseMOFdn);
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);
        when(neJobStaticData.getNodeName()).thenReturn(neName);

        when(networkElementRetrievalBean.getNetworkElementData(neName)).thenReturn(networkElementInfoMock);
        when(networkElement.getNeType()).thenReturn(neType);
        getSMRSDetailsFailed();
        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProperty.put(ShmConstants.VALUE, licenseFilePath);
        jobPropertyList.add(jobProperty);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttribtues);
        Mockito.when(activityUtils.getActivityInfo(activityJobId, InstallLicenseKeyFileService.class)).thenReturn(jobActivityInfoMock);
        final Exception ex = new RuntimeException();
        when(dpsWriterMock.performAction(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenThrow(ex);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        installLicenseKeyFileServiceMock.execute(activityJobId);
        when(neJobStaticDataProvider.getNeJobStaticData(eq(activityJobId), anyString())).thenReturn(neJobStaticData);
        verify(activityUtils, times(1)).sendNotificationToWFS(neJobStaticData, activityJobId, "execute", null);
        activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList1, null, MOACTION_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, jobPropertyList1, null, MOACTION_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressCalculator, times(0)).updateActivityJobProgressPercentage(activityJobId, jobPropertyList1, null, UPDATE_LICENSE_MOACTION_PROGRESS_PERCENTAGE);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteShouldFailWithMediationServiceException() throws MoNotFoundException, JobDataNotFoundException {
        requiredTestData();
        final List<Map<String, Object>> jobPropertyList1 = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        mockJobActivityInfo();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ShmConstants.NE_NAME, neName);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, businessKey);
        when(jobConfigurationServiceRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(getActivityJobPo());
        final Map<String, Object> licenseMOAttributes = new HashMap<String, Object>();
        licenseMOAttributes.put(LAST_LICENSING_PI_CHANGE, lastLicensePiChange);
        final Map<String, Object> licenseMoWithFDN = new HashMap<String, Object>();
        licenseMoWithFDN.put(ShmConstants.FDN, licenseMOFdn);
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        licenseMoWithFDN.put(FINGER_PRINT, "Some FingerPrint");
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        final List<NetworkElement> networkElementList = new ArrayList<NetworkElement>();
        networkElementList.add(networkElement);
        when(networkElementRetrievalBean.getNetworkElementData(neName)).thenReturn(networkElementInfoMock);
        when(networkElementRetrievalBean.getNeType(neName)).thenReturn("ERBS");
        getSMRSDetailsFailed();
        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProperty.put(ShmConstants.VALUE, licenseFilePath);
        jobPropertyList.add(jobProperty);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttribtues);
        Mockito.when(activityUtils.getActivityInfo(activityJobId, InstallLicenseKeyFileService.class)).thenReturn(jobActivityInfoMock);
        final Exception ex = new RuntimeException();
        when(dpsWriterMock.performAction(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenThrow(ex);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        installLicenseKeyFileServiceMock.execute(activityJobId);
        verify(activityUtils, times(1)).sendNotificationToWFS(neJobStaticData, activityJobId, "execute", null);
        activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList1, null, MOACTION_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, jobPropertyList1, null, MOACTION_START_PROGRESS_PERCENTAGE);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWithException() throws JobDataNotFoundException, MoNotFoundException {
        mockJobActivityInfo();
        requiredTestData();
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityJobAttributes);
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ShmConstants.NE_NAME, neName);
        neJobAttributes.put(ShmConstants.NETYPE, neType);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, businessKey);
        when(activityUtils.getPoAttributes(neJobId)).thenReturn(neJobAttributes);
        final Map<String, Object> licenseMOAttributes = new HashMap<String, Object>();
        licenseMOAttributes.put(LAST_LICENSING_PI_CHANGE, lastLicensePiChange);
        final Map<String, Object> licenseMoWithFDN = new HashMap<String, Object>();
        licenseMoWithFDN.put(ShmConstants.FDN, licenseMOFdn);
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);
        when(activityUtils.getNodeName(activityJobId)).thenReturn(neName);
        final List<NetworkElement> networkElementList = new ArrayList<NetworkElement>();
        final NetworkElement networkElement1 = new NetworkElement();
        networkElement1.setNeType("CPP");
        networkElement1.setPlatformType(PlatformTypeEnum.CPP);
        networkElementList.add(networkElement1);
        when(fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(Arrays.asList(neName))).thenReturn(networkElementList);
        when(networkElement.getNeType()).thenReturn(neType);
        getSMRSDetails();

        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProperty.put(ShmConstants.VALUE, licenseFilePath);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ShmConstants.JOB_TEMPLATE_ID, JOBTEMPLATEID);
        when(activityUtils.getMainJobAttributes(activityJobId)).thenReturn(mainJobAttribtues);
        licenseMOAttributes.put(FINGER_PRINT, "Some FingerPrint");
        licenseMOAttributes.put(LAST_LICENSING_PI_CHANGE, "141021_131638");
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        when(jobUpdateService.retrieveJobWithRetry(activityJobId)).thenReturn(activityJobAttributes);
        neAttributes = new HashMap<String, Object>();
        neAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neAttributes.put(ShmConstants.NE_NAME, neName);
        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(neAttributes);
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProp = new HashMap<String, String>();
        jobProp.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProp.put(ShmConstants.VALUE, licenseFilePath);
        jobPropList.add(jobProp);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        final Map<String, Object> jobTemplateAttribtues = new HashMap<String, Object>();
        jobTemplateAttribtues.put(ShmConstants.JOB_CATEGORY, "UI");
        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(Matchers.anyLong())).thenReturn(mainJobAttribtues);
        when(jobConfigurationServiceRetryProxyMock.getPOAttributes(Matchers.anyLong())).thenReturn(jobTemplateAttribtues);
        when(activityUtils.getPoAttributes(mainJobId)).thenReturn(mainJobAttribtues);
        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);
        final List<Map<String, Object>> poAttributesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(LICENSE_DATA_FINGERPRINT, fingerPrint);
        poAttributes.put(LICENSE_DATA_SEQUENCE_NUMBER, "sequenceNumber");
        poAttributesList.add(poAttributes);
        when(licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes)).thenReturn(poAttributesList);
        when(licensingRetryService.updateLicenseInstalledTime(restrictionAttributes)).thenReturn(true);
        when(licenseKeyFileDeleteServiceMock.deleteHistoricLicensePOs(fingerPrint, "sequenceNumber")).thenReturn("ss");
        final Exception ex = new RuntimeException();
        when(dpsWriterMock.performAction(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenThrow(ex);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, "install")).thenReturn(true);
        installLicenseKeyFileServiceMock.execute(activityJobId);
        verify(activityUtils, times(1)).sendNotificationToWFS(neJobStaticData, activityJobId, "execute", null);
        Mockito.verify(activityAndNEJobProgressCalculator, times(4)).updateActivityJobProgressPercentage(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble());
    }

    @Test
    public void testExecuteShouldFailAsMOIsNull() throws MoNotFoundException, JobDataNotFoundException {
        requiredTestData();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        mockJobActivityInfo();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ShmConstants.NE_NAME, neName);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, businessKey);
        when(jobConfigurationServiceRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);

        getNeJobPO(neJobId, mainJobId, false);
        getActivityPO(activityJobId, neJobId);
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(getActivityJobPo());

        when(networkElementRetrievalBean.getNetworkElementData(neName)).thenReturn(networkElementInfoMock);
        when(networkElementRetrievalBean.getNeType(neName)).thenReturn(neType);
        final Map<String, Object> restrictions = new HashMap<String, Object>();
        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        attributesMap.put(FINGER_PRINT, "CPPREFRXI");
        when(licPO.getAllAttributes()).thenReturn(attributesMap);
        when(licPO.getFdn()).thenReturn("MeContext=ERBS1111,ManagedElement=1,SystemFunctions=1, Licensing=1");

        final Map<String, Object> licenseMOAttributes = new HashMap<String, Object>();
        licenseMOAttributes.put(LAST_LICENSING_PI_CHANGE, lastLicensePiChange);
        final Map<String, Object> licenseMoWithFDN = new HashMap<String, Object>();
        licenseMoWithFDN.put(ShmConstants.FDN, licenseMOFdn);
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, null);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);

        managedObjectsList.add(licPO);
        when(dpsReaderMock.getManagedObjects("ERBS_NODE_MODEL", LICENSE_MO, restrictions, "ERBS1111")).thenReturn(managedObjectsList);

        installLicenseKeyFileServiceMock.execute(activityJobId);
        verify(activityUtils, times(1)).sendNotificationToWFS(neJobStaticData, activityJobId, "execute", null);
        fdnNotificationSubject = new FdnNotificationSubject(licenseMOFdn, activityJobId, jobActivityInfoMock);
        verify(notificationRegistryMock, times(0)).register(fdnNotificationSubject);
        activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, null, MOACTION_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, jobPropertyList, null, MOACTION_START_PROGRESS_PERCENTAGE);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWithNoSMRSRoot() throws JobDataNotFoundException, MoNotFoundException {

        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityJobAttributes);
        mockJobActivityInfo();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ShmConstants.NE_NAME, neName);
        neJobAttributes.put(ShmConstants.NETYPE, neType);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, businessKey);
        when(activityUtils.getPoAttributes(neJobId)).thenReturn(neJobAttributes);
        final Map<String, Object> licenseMOAttributes = new HashMap<String, Object>();
        licenseMOAttributes.put(LAST_LICENSING_PI_CHANGE, lastLicensePiChange);
        final Map<String, Object> licenseMoWithFDN = new HashMap<String, Object>();
        licenseMoWithFDN.put(ShmConstants.FDN, licenseMOFdn);
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);
        when(activityUtils.getNodeName(activityJobId)).thenReturn(neName);
        getSMRSDetails();

        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList1 = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProperty.put(ShmConstants.VALUE, licenseFilePath);
        jobPropertyList.add(jobProperty);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        licenseMOAttributes.put(FINGER_PRINT, "Some FingerPrint");
        licenseMOAttributes.put(LAST_LICENSING_PI_CHANGE, "141021_131638");
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        when(jobUpdateService.retrieveJobWithRetry(activityJobId)).thenReturn(activityJobAttributes);
        neAttributes = new HashMap<String, Object>();
        neAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neAttributes.put(ShmConstants.NE_NAME, neName);

        when(neJobStaticDataProvider.getNeJobStaticData(Matchers.anyLong(), Matchers.anyString())).thenReturn(neJobStaticData);

        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(neAttributes);
        when(networkElementRetrievalBean.getNetworkElementData(neName)).thenReturn(networkElementInfoMock);
        when(networkElementInfoMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProp = new HashMap<String, String>();
        jobProp.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProp.put(ShmConstants.VALUE, licenseFilePath);
        jobPropList.add(jobProp);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ShmConstants.JOB_TEMPLATE_ID, JOBTEMPLATEID);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        final Map<String, Object> jobTemplateAttribtues = new HashMap<String, Object>();
        jobTemplateAttribtues.put(ShmConstants.JOB_CATEGORY, "UI");
        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(Matchers.anyLong())).thenReturn(mainJobAttribtues);
        when(jobConfigurationServiceRetryProxyMock.getPOAttributes(Matchers.anyLong())).thenReturn(jobTemplateAttribtues);
        when(activityUtils.getPoAttributes(mainJobId)).thenReturn(mainJobAttribtues);
        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);
        final List<Map<String, Object>> poAttributesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(LICENSE_DATA_FINGERPRINT, fingerPrint);
        poAttributes.put(LICENSE_DATA_SEQUENCE_NUMBER, "sequenceNumber");

        poAttributes.put(CommonLicensingActivityConstants.PRODUCTTYPE, "ERBS");

        poAttributesList.add(poAttributes);

        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);
        when(licensingRetryService.getAttributesListOfLicensePOs(Matchers.anyMap())).thenReturn(poAttributesList);

        when(licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes)).thenReturn(poAttributesList);
        when(licensingRetryService.updateLicenseInstalledTime(restrictionAttributes)).thenReturn(true);
        when(licenseKeyFileDeleteServiceMock.deleteHistoricLicensePOs(fingerPrint, "sequenceNumber")).thenReturn("ss");
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticDataMock);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, "install")).thenReturn(true);
        installLicenseKeyFileServiceMock.execute(activityJobId);
        verify(activityUtils, times(1)).sendNotificationToWFS(neJobStaticData, activityJobId, "execute", null);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, jobPropertyList1, PRECHECK_END_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, jobPropertyList1, null, MOACTION_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, jobPropertyList1, null, MOACTION_END_PROGRESS_PERCENTAGE);
    }

    @Test
    public void testExecuteWithNoLicensingMo() throws JobDataNotFoundException, MoNotFoundException {
        /* final List<Map<String, Object>> jobPropertyList = */new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);

        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityJobAttributes);
        final List<NetworkElement> networkElementList = new ArrayList<NetworkElement>();
        networkElementList.add(networkElement);
        when(fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(Arrays.asList(neName))).thenReturn(networkElementList);
        when(networkElement.getNeType()).thenReturn(neType);
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ShmConstants.NE_NAME, neName);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, businessKey);
        when(activityUtils.getPoAttributes(neJobId)).thenReturn(neJobAttributes);
        when(licensingRetryService.getLicenseMoFdn(activityJobId)).thenReturn(null);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, "install")).thenReturn(true);
        installLicenseKeyFileServiceMock.execute(activityJobId);
        verify(activityUtils, times(1)).sendNotificationToWFS(neJobStaticData, activityJobId, "execute", null);
        activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, null, MOACTION_START_PROGRESS_PERCENTAGE);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, MOACTION_START_PROGRESS_PERCENTAGE);
        verify(activityUtils, times(1)).sendNotificationToWFS(neJobStaticData, activityJobId, "execute", null);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_START_PROGRESS_PERCENTAGE);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteLKFShouldFailAsPOListisEmpty() {
        when(activityUtils.getNodeName(activityJobId)).thenReturn(neName);
        when(notificationmock.getDpsDataChangedEvent()).thenReturn(avcEvent);
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(getActivityJobPo());

        final Map<String, AttributeChangeData> modifiedAttributes = new HashMap<String, AttributeChangeData>();
        when(activityUtils.getModifiedAttributes(notificationmock.getDpsDataChangedEvent())).thenReturn(modifiedAttributes);

        final Map<String, Object> lastLicensingPiChangeMap = new HashMap<String, Object>();
        lastLicensingPiChangeMap.put("previousNotifiableAttributeValue", "141021_131637");
        lastLicensingPiChangeMap.put("notifiableAttributeValue", "141021_131638");
        when(activityUtils.getNotifiableAttribute(modifiedAttributes, LAST_LICENSING_PI_CHANGE)).thenReturn(lastLicensingPiChangeMap);

        final List<Map<String, String>> jobPropertiesList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, PI_TIME_STAMP);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "141021_131637");
        jobPropertiesList.add(jobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertiesList);
        when(activityUtils.getActivityJobAttributeValue(activityJobAttributes, PI_TIME_STAMP)).thenReturn("141021_131637");

        neAttributes = new HashMap<String, Object>();
        neAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neAttributes.put(ShmConstants.NE_NAME, neName);

        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(neAttributes);

        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProp = new HashMap<String, String>();
        jobProp.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProp.put(ShmConstants.VALUE, licenseFilePath);
        jobPropertyList.add(jobProp);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        when(activityUtils.getPoAttributes(mainJobId)).thenReturn(mainJobAttribtues);
        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);
        final List<Map<String, Object>> poAttributesList = new ArrayList<Map<String, Object>>();
        when(licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes)).thenReturn(poAttributesList);

        retryMock();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(jobPropertiesList, "businessKey");

        //installLicenseKeyFileServiceMock.processNotification(notificationmock);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleTimeoutShouldPass() throws MoNotFoundException, JobDataNotFoundException {
        requiredTestData();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList1 = new ArrayList<Map<String, Object>>();
        final String logMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, ActivityConstants.INSTALL_LICENSE);
        when(networkElementRetrievalBean.getNetworkElementData(neName)).thenReturn(networkElementInfoMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(networkElementRetrievalBean.getNeType(neName)).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.CPP.toString());
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(getActivityJobPo());
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(licensingRetryService.getLicenseMoFdn(activityJobId)).thenReturn(licenseMOFdn);
        final Map<String, Object> licenseMOAttributes = new HashMap<String, Object>();
        licenseMOAttributes.put(FINGER_PRINT, "Some FingerPrint");
        licenseMOAttributes.put(LAST_LICENSING_PI_CHANGE, "141021_131638");
        final Map<String, Object> licenseMoWithFDN = new HashMap<String, Object>();
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, PI_TIME_STAMP);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "141021_131637");
        jobPropertyList.add(jobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        when(activityUtils.getActivityJobAttributeValue(activityJobAttributes, PI_TIME_STAMP)).thenReturn("141021_131637");
        when(jobUpdateService.retrieveJobWithRetry(activityJobId)).thenReturn(activityJobAttributes);

        neAttributes = new HashMap<String, Object>();
        neAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neAttributes.put(ShmConstants.NE_NAME, neName);
        when(jobConfigurationServiceRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neAttributes);

        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProp = new HashMap<String, String>();
        jobProp.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProp.put(ShmConstants.VALUE, licenseFilePath);
        jobPropList.add(jobProp);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttribtues);
        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);
        final List<Map<String, Object>> poAttributesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(LICENSE_DATA_FINGERPRINT, fingerPrint);
        poAttributes.put(LICENSE_DATA_SEQUENCE_NUMBER, "sequenceNumber");
        poAttributesList.add(poAttributes);
        when(licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes)).thenReturn(poAttributesList);
        when(licensingRetryService.updateLicenseInstalledTime(restrictionAttributes)).thenReturn(true);
        when(licenseKeyFileDeleteServiceMock.deleteHistoricLicensePOs(fingerPrint, "sequenceNumber")).thenReturn("ss");
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId())).thenReturn(jobExecutionUser);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, installLicenseKeyFileServiceMock.handleTimeout(activityJobId).getActivityResultEnum());
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, jobPropertyList1, jobLogList, HANDLE_TIMEOUT_PROGRESS_PERCENTAGE);
        verify(activityUtils, times(1)).recordEvent(SHMEvents.LICENSE_INSTALL_TIME_OUT, neName, licenseMOFdn, activityUtils.additionalInfoForEvent(activityJobId, neName, logMessage));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAsyncHandleTimeoutShouldPass() throws MoNotFoundException, JobDataNotFoundException {
        requiredTestData();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList1 = new ArrayList<Map<String, Object>>();
        when(networkElementRetrievalBean.getNetworkElementData(neName)).thenReturn(networkElementInfoMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(networkElementRetrievalBean.getNeType(neName)).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.CPP.toString());
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(getActivityJobPo());

        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(licensingRetryService.getLicenseMoFdn(activityJobId)).thenReturn(licenseMOFdn);

        final Map<String, Object> licenseMOAttributes = new HashMap<String, Object>();
        licenseMOAttributes.put(FINGER_PRINT, "Some FingerPrint");
        licenseMOAttributes.put(LAST_LICENSING_PI_CHANGE, "141021_131638");
        final Map<String, Object> licenseMoWithFDN = new HashMap<String, Object>();
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, PI_TIME_STAMP);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "141021_131637");
        jobPropertyList.add(jobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        when(activityUtils.getActivityJobAttributeValue(activityJobAttributes, PI_TIME_STAMP)).thenReturn("141021_131637");
        when(jobUpdateService.retrieveJobWithRetry(activityJobId)).thenReturn(activityJobAttributes);

        neAttributes = new HashMap<String, Object>();
        neAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neAttributes.put(ShmConstants.NE_NAME, neName);
        when(jobConfigurationServiceRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neAttributes);

        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProp = new HashMap<String, String>();
        jobProp.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProp.put(ShmConstants.VALUE, licenseFilePath);
        jobPropList.add(jobProp);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttribtues);
        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);
        final List<Map<String, Object>> poAttributesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(LICENSE_DATA_FINGERPRINT, fingerPrint);
        poAttributes.put(LICENSE_DATA_SEQUENCE_NUMBER, "sequenceNumber");
        poAttributesList.add(poAttributes);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes)).thenReturn(poAttributesList);
        when(licensingRetryService.updateLicenseInstalledTime(restrictionAttributes)).thenReturn(true);
        when(licenseKeyFileDeleteServiceMock.deleteHistoricLicensePOs(fingerPrint, "sequenceNumber")).thenReturn("ss");
        installLicenseKeyFileServiceMock.asyncHandleTimeout(activityJobId);
        verify(activityUtils, times(1)).buildProcessVariablesForTimeoutAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.INSTALL_LICENSE, ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, jobPropertyList1, jobLogList, HANDLE_TIMEOUT_PROGRESS_PERCENTAGE);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleTimeoutShouldFail() throws MoNotFoundException, JobDataNotFoundException {
        requiredTestData();
        when(networkElementRetrievalBean.getNetworkElementData(neName)).thenReturn(networkElementInfoMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(networkElementRetrievalBean.getNeType(neName)).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.CPP.toString());
        final Map<String, Object> licenseMOAttributes = new HashMap<String, Object>();
        licenseMOAttributes.put(FINGER_PRINT, "Some FingerPrint");
        licenseMOAttributes.put(LAST_LICENSING_PI_CHANGE, "141021_131636");
        final Map<String, Object> licenseMoWithFDN = new HashMap<String, Object>();
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(licensingRetryService.getLicenseMoFdn(activityJobId)).thenReturn(licenseMOFdn);

        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, PI_TIME_STAMP);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "141021_131637");
        jobPropertyList.add(jobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        when(activityUtils.getActivityJobAttributeValue(activityJobAttributes, PI_TIME_STAMP)).thenReturn("141021_131637");

        neAttributes = new HashMap<String, Object>();
        neAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neAttributes.put(ShmConstants.NE_NAME, neName);
        when(jobConfigurationServiceRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neAttributes);

        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProp = new HashMap<String, String>();
        jobProp.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProp.put(ShmConstants.VALUE, licenseFilePath);
        jobPropList.add(jobProp);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        when(activityUtils.getActivityJobAttributeValue(activityJobAttributes, PI_TIME_STAMP)).thenReturn("141021_131637");
        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttribtues);
        when(jobConfigurationServiceRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neAttributes);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);
        final List<Map<String, Object>> poAttributesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(LICENSE_DATA_FINGERPRINT, fingerPrint);
        poAttributes.put(LICENSE_DATA_SEQUENCE_NUMBER, "sequenceNumber");
        poAttributesList.add(poAttributes);
        when(licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes)).thenReturn(poAttributesList);
        when(licensingRetryService.updateLicenseInstalledTime(restrictionAttributes)).thenReturn(true);
        when(licenseKeyFileDeleteServiceMock.deleteHistoricLicensePOs(fingerPrint, "sequenceNumber")).thenReturn("ss");
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, installLicenseKeyFileServiceMock.handleTimeout(activityJobId).getActivityResultEnum());
    }

    @Test
    public void testHandleTimeout_WhenRetrivingLicenseMOThrowsException_Fail() throws MoNotFoundException, JobDataNotFoundException {
        requiredTestData();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList1 = new ArrayList<Map<String, Object>>();
        final String logMessage = ActivityConstants.INSTALL_LICENSE + " handleTimeout failed." + String.format(JobLogConstants.FAILURE_REASON, "");

        when(networkElementRetrievalBean.getNetworkElementData(neName)).thenReturn(networkElementInfoMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(networkElementRetrievalBean.getNeType(neName)).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.CPP.toString());
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(getActivityJobPo());
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(licensingRetryService.getLicenseMoFdn(activityJobId)).thenReturn(licenseMOFdn);

        final Map<String, Object> licenseMOAttributes = new HashMap<String, Object>();
        licenseMOAttributes.put(FINGER_PRINT, "Some FingerPrint");
        licenseMOAttributes.put(LAST_LICENSING_PI_CHANGE, "141021_131638");
        final Map<String, Object> licenseMoWithFDN = new HashMap<String, Object>();
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenThrow(Exception.class);
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, PI_TIME_STAMP);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "141021_131637");
        jobPropertyList.add(jobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        when(activityUtils.getActivityJobAttributeValue(activityJobAttributes, PI_TIME_STAMP)).thenReturn("141021_131637");
        when(jobUpdateService.retrieveJobWithRetry(activityJobId)).thenReturn(activityJobAttributes);

        neAttributes = new HashMap<String, Object>();
        neAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neAttributes.put(ShmConstants.NE_NAME, neName);
        when(jobConfigurationServiceRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neAttributes);

        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProp = new HashMap<String, String>();
        jobProp.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProp.put(ShmConstants.VALUE, licenseFilePath);
        jobPropList.add(jobProp);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttribtues);
        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);
        final List<Map<String, Object>> poAttributesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(LICENSE_DATA_FINGERPRINT, fingerPrint);
        poAttributes.put(LICENSE_DATA_SEQUENCE_NUMBER, "sequenceNumber");
        poAttributesList.add(poAttributes);
        when(licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes)).thenReturn(poAttributesList);
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId())).thenReturn(jobExecutionUser);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, installLicenseKeyFileServiceMock.handleTimeout(activityJobId).getActivityResultEnum());
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        verify(activityAndNEJobProgressCalculator, times(0)).updateActivityJobProgressPercentage(activityJobId, jobPropertyList1, jobLogList, HANDLE_TIMEOUT_PROGRESS_PERCENTAGE);
        verify(activityUtils, times(1)).recordEvent(SHMEvents.LICENSE_INSTALL_TIME_OUT, neName, licenseMOFdn, activityUtils.additionalInfoForEvent(activityJobId, neName, logMessage));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAsyncHandleTimeoutShouldFail() throws MoNotFoundException, JobDataNotFoundException {
        requiredTestData();
        when(networkElementRetrievalBean.getNetworkElementData(neName)).thenReturn(networkElementInfoMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(networkElementRetrievalBean.getNeType(neName)).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.CPP.toString());

        final Map<String, Object> licenseMOAttributes = new HashMap<String, Object>();
        licenseMOAttributes.put(FINGER_PRINT, "Some FingerPrint");
        licenseMOAttributes.put(LAST_LICENSING_PI_CHANGE, "141021_131636");
        final Map<String, Object> licenseMoWithFDN = new HashMap<String, Object>();
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(licensingRetryService.getLicenseMoFdn(activityJobId)).thenReturn(licenseMOFdn);

        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, PI_TIME_STAMP);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "141021_131637");
        jobPropertyList.add(jobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        when(activityUtils.getActivityJobAttributeValue(activityJobAttributes, PI_TIME_STAMP)).thenReturn("141021_131637");
        neAttributes = new HashMap<String, Object>();
        neAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neAttributes.put(ShmConstants.NE_NAME, neName);
        when(jobConfigurationServiceRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neAttributes);

        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProp = new HashMap<String, String>();
        jobProp.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProp.put(ShmConstants.VALUE, licenseFilePath);
        jobPropList.add(jobProp);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttribtues);
        when(jobConfigurationServiceRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neAttributes);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);
        final List<Map<String, Object>> poAttributesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(LICENSE_DATA_FINGERPRINT, fingerPrint);
        poAttributes.put(LICENSE_DATA_SEQUENCE_NUMBER, "sequenceNumber");
        poAttributesList.add(poAttributes);
        when(licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes)).thenReturn(poAttributesList);
        when(licensingRetryService.updateLicenseInstalledTime(restrictionAttributes)).thenReturn(true);
        when(licenseKeyFileDeleteServiceMock.deleteHistoricLicensePOs(fingerPrint, "sequenceNumber")).thenReturn("ss");
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        installLicenseKeyFileServiceMock.asyncHandleTimeout(activityJobId);

        verify(activityUtils, times(1)).buildProcessVariablesForTimeoutAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.INSTALL_LICENSE, ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
    }

    @Test
    public void testCancel() {
        installLicenseKeyFileServiceMock.cancel(activityJobId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCancelTimeout() throws MoNotFoundException, JobDataNotFoundException {
        requiredTestData();
        when(networkElementRetrievalBean.getNetworkElementData(neName)).thenReturn(networkElementInfoMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(networkElementRetrievalBean.getNeType(neName)).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.CPP.toString());
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(getActivityJobPo());

        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(licensingRetryService.getLicenseMoFdn(activityJobId)).thenReturn(licenseMOFdn);

        final Map<String, Object> licenseMOAttributes = new HashMap<String, Object>();
        licenseMOAttributes.put(FINGER_PRINT, "Some FingerPrint");
        licenseMOAttributes.put(LAST_LICENSING_PI_CHANGE, "141021_131638");
        final Map<String, Object> licenseMoWithFDN = new HashMap<String, Object>();
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, PI_TIME_STAMP);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "141021_131637");
        jobPropertyList.add(jobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        when(activityUtils.getActivityJobAttributeValue(activityJobAttributes, PI_TIME_STAMP)).thenReturn("141021_131637");
        when(jobUpdateService.retrieveJobWithRetry(activityJobId)).thenReturn(activityJobAttributes);

        neAttributes = new HashMap<String, Object>();
        neAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neAttributes.put(ShmConstants.NE_NAME, neName);
        when(jobConfigurationServiceRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neAttributes);

        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProp = new HashMap<String, String>();
        jobProp.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProp.put(ShmConstants.VALUE, licenseFilePath);
        jobPropList.add(jobProp);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttribtues);
        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);
        final List<Map<String, Object>> poAttributesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(LICENSE_DATA_FINGERPRINT, fingerPrint);
        poAttributes.put(LICENSE_DATA_SEQUENCE_NUMBER, "sequenceNumber");
        poAttributesList.add(poAttributes);
        when(licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes)).thenReturn(poAttributesList);
        when(licensingRetryService.updateLicenseInstalledTime(restrictionAttributes)).thenReturn(true);
        when(licenseKeyFileDeleteServiceMock.deleteHistoricLicensePOs(fingerPrint, "sequenceNumber")).thenReturn("ss");
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, installLicenseKeyFileServiceMock.cancelTimeout(activityJobId, true).getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCancelTimeoutShouldFail() throws MoNotFoundException, JobDataNotFoundException {
        when(networkElementRetrievalBean.getNetworkElementData(neName)).thenReturn(networkElementInfoMock);

        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);

        when(networkElementRetrievalBean.getNeType(neName)).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.CPP.toString());
        final Map<String, Object> licenseMOAttributes = new HashMap<String, Object>();
        licenseMOAttributes.put(FINGER_PRINT, "Some FingerPrint");
        licenseMOAttributes.put(LAST_LICENSING_PI_CHANGE, "141021_131636");
        final Map<String, Object> licenseMoWithFDN = new HashMap<String, Object>();
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);

        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(licensingRetryService.getLicenseMoFdn(activityJobId)).thenReturn(licenseMOFdn);

        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, PI_TIME_STAMP);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "141021_131637");
        jobPropertyList.add(jobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        when(activityUtils.getActivityJobAttributeValue(activityJobAttributes, PI_TIME_STAMP)).thenReturn("141021_131637");
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(getActivityJobPo(), activityJobAttributes);

        neAttributes = new HashMap<String, Object>();
        neAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neAttributes.put(ShmConstants.NE_NAME, neName);
        when(jobConfigurationServiceRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neAttributes);

        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProp = new HashMap<String, String>();
        jobProp.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProp.put(ShmConstants.VALUE, licenseFilePath);
        jobPropList.add(jobProp);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        when(activityUtils.getPoAttributes(mainJobId)).thenReturn(mainJobAttribtues);
        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);
        final List<Map<String, Object>> poAttributesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(LICENSE_DATA_FINGERPRINT, fingerPrint);
        poAttributes.put(LICENSE_DATA_SEQUENCE_NUMBER, "sequenceNumber");
        poAttributesList.add(poAttributes);
        when(licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes)).thenReturn(poAttributesList);
        when(licensingRetryService.updateLicenseInstalledTime(restrictionAttributes)).thenReturn(true);
        when(licenseKeyFileDeleteServiceMock.deleteHistoricLicensePOs(fingerPrint, "sequenceNumber")).thenReturn("ss");
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, installLicenseKeyFileServiceMock.cancelTimeout(activityJobId, true).getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleTimeoutWithEmptyPropertyList() throws MoNotFoundException, JobDataNotFoundException {
        requiredTestData();
        when(networkElementRetrievalBean.getNetworkElementData(neName)).thenReturn(networkElementInfoMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(networkElementRetrievalBean.getNeType(neName)).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.CPP.toString());
        final Map<String, Object> licenseMOAttributes = new HashMap<String, Object>();
        licenseMOAttributes.put(FINGER_PRINT, "Some FingerPrint");
        licenseMOAttributes.put(LAST_LICENSING_PI_CHANGE, "141021_131636");
        final Map<String, Object> licenseMoWithFDN = new HashMap<String, Object>();
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(licensingRetryService.getLicenseMoFdn(activityJobId)).thenReturn(licenseMOFdn);

        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, PI_TIME_STAMP);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "141021_131637");
        jobPropertyList.add(jobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        when(activityUtils.getActivityJobAttributeValue(activityJobAttributes, PI_TIME_STAMP)).thenReturn("141021_131637");
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(getActivityJobPo());

        neAttributes = new HashMap<String, Object>();
        neAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neAttributes.put(ShmConstants.NE_NAME, neName);
        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(neAttributes);

        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProp = new HashMap<String, String>();
        jobProp.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProp.put(ShmConstants.VALUE, licenseFilePath);
        jobPropList.add(jobProp);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttribtues);
        when(jobConfigurationServiceRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neAttributes);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);
        final List<Map<String, Object>> poAttributesList = new ArrayList<Map<String, Object>>();
        when(licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes)).thenReturn(poAttributesList);
        when(licensingRetryService.updateLicenseInstalledTime(restrictionAttributes)).thenReturn(true);
        when(licenseKeyFileDeleteServiceMock.deleteHistoricLicensePOs(fingerPrint, "sequenceNumber")).thenReturn("ss");
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, installLicenseKeyFileServiceMock.handleTimeout(activityJobId).getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAsyncHandleTimeoutWithEmptyPropertyList() throws MoNotFoundException, JobDataNotFoundException {
        requiredTestData();
        when(networkElementRetrievalBean.getNetworkElementData(neName)).thenReturn(networkElementInfoMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(networkElementRetrievalBean.getNeType(neName)).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.CPP.toString());

        final Map<String, Object> licenseMOAttributes = new HashMap<String, Object>();
        licenseMOAttributes.put(FINGER_PRINT, "Some FingerPrint");
        licenseMOAttributes.put(LAST_LICENSING_PI_CHANGE, "141021_131636");
        final Map<String, Object> licenseMoWithFDN = new HashMap<String, Object>();
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(licensingRetryService.getLicenseMoFdn(activityJobId)).thenReturn(licenseMOFdn);

        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, PI_TIME_STAMP);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "141021_131637");
        jobPropertyList.add(jobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        when(activityUtils.getActivityJobAttributeValue(activityJobAttributes, PI_TIME_STAMP)).thenReturn("141021_131637");
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(getActivityJobPo());

        neAttributes = new HashMap<String, Object>();
        neAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neAttributes.put(ShmConstants.NE_NAME, neName);
        when(jobConfigurationServiceRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neAttributes);

        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProp = new HashMap<String, String>();
        jobProp.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProp.put(ShmConstants.VALUE, licenseFilePath);
        jobPropList.add(jobProp);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttribtues);
        when(jobConfigurationServiceRetryProxyMock.getNeJobAttributes(neJobId)).thenReturn(neAttributes);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(getActivityJobPo());
        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);
        final List<Map<String, Object>> poAttributesList = new ArrayList<Map<String, Object>>();
        when(licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes)).thenReturn(poAttributesList);
        when(licensingRetryService.updateLicenseInstalledTime(restrictionAttributes)).thenReturn(true);
        when(licenseKeyFileDeleteServiceMock.deleteHistoricLicensePOs(fingerPrint, "sequenceNumber")).thenReturn("ss");
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobConfigurationServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        installLicenseKeyFileServiceMock.asyncHandleTimeout(activityJobId);
        verify(activityUtils, times(1)).buildProcessVariablesForTimeoutAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.INSTALL_LICENSE, ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
    }

    @Test
    public void testPrecheckHandleTimeout() {
        installLicenseKeyFileServiceMock.precheckHandleTimeout(activityJobId);
        verify(activityUtils).failActivityForPrecheckTimeoutExpiry(activityJobId, ActivityConstants.INSTALL_LICENSE);
    }

    @Test
    public void testTimeoutForHandleTimeout() {
        installLicenseKeyFileServiceMock.timeoutForAsyncHandleTimeout(activityJobId);
        verify(activityUtils).failActivityForHandleTimeoutExpiry(activityJobId, ActivityConstants.INSTALL_LICENSE);
    }

    /**
     *
     */
    private void getSMRSDetails() {
        final String ftpServerIpAddress = "10.32.227.173";
        final String user = "root";
        final char[] password = { 'p', 'a', 's', 's', 'w', '0', 'r', 'd' };
        final SmrsAccountInfo smrsDetails = new SmrsAccountInfo();
        smrsDetails.setServerIpAddress(ftpServerIpAddress);
        smrsDetails.setUser(user);
        smrsDetails.setPassword(password);
        smrsDetails.setSmrsRootDirectory("some");
        when(smrsServiceUtil.getSmrsDetails(SmrsServiceConstants.LICENCE_ACCOUNT, neType, neName)).thenReturn(smrsDetails);
    }

    private void getSMRSDetailsFailed() {
        final RuntimeException ex = new RuntimeException();
        when(smrsServiceUtil.getSmrsDetails(SmrsServiceConstants.LICENCE_ACCOUNT, neType, neName)).thenThrow(ex);
    }

    /**
     * @param activityJobId
     * @param neJobId
     */
    private void getActivityPO(final long activityJobId, final long neJobId) {
        final Map<String, Object> activityPOAttributesMap = new HashMap<String, Object>();
        activityPOAttributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityPO.getAllAttributes()).thenReturn(activityPOAttributesMap);
        when(dpsReaderMock.findPOByPoId(activityJobId)).thenReturn(activityPO);
    }

    /**
     * @param neJobId
     * @param mainJobId
     * @return
     */
    private List<Map<String, Object>> getNeJobPO(final long neJobId, final long mainJobId, final boolean setLicensePath) {
        neAttributes = new HashMap<String, Object>();
        neAttributes.put(ShmConstants.NE_NAME, "ERBS00006");
        neAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        final Map<String, Object> nePoAttrMap = new HashMap<String, Object>();
        final List<Map<String, Object>> nePropertyList = new ArrayList<Map<String, Object>>();
        nePropertyList.add(neAttributes);
        if (setLicensePath) {
            final Map<String, Object> lkfJobProperty = new HashMap<String, Object>();
            lkfJobProperty.put(ShmConstants.KEY, LICENSE_FILE_PATH);
            lkfJobProperty.put(ShmConstants.VALUE, "/home/smrs/path");
            nePropertyList.add(lkfJobProperty);
        }
        nePoAttrMap.put(ShmConstants.JOBPROPERTIES, nePropertyList);
        when(neJobPO.getAllAttributes()).thenReturn(neAttributes);
        when(dpsReaderMock.findPOByPoId(neJobId)).thenReturn(neJobPO);
        return nePropertyList;
    }

    private Map<String, Object> getActivityJobPo() {

        final Map<String, Object> activityJobAttr = new HashMap<String, Object>();
        activityJobAttr.put(ShmConstants.ACTIVITY_NE_JOB_ID, neJobId);
        activityJobAttr.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        activityJobAttr.put(ShmConstants.JOBTEMPLATEID, 123L);
        activityJobAttr.put(ShmConstants.NE_NAME, neName);
        activityJobAttr.put(ShmConstants.NAME, "name");
        activityJobAttr.put(ShmConstants.OWNER, "owner");
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, String>> mainJobConfPropertyList = new ArrayList<Map<String, String>>();

        final Map<String, String> mainJobPropertyCvIdentity = new HashMap<String, String>();
        mainJobPropertyCvIdentity.put(ShmConstants.KEY, BackupActivityConstants.CV_IDENTITY);
        mainJobPropertyCvIdentity.put(ShmConstants.VALUE, "cvidentity");
        mainJobConfPropertyList.add(mainJobPropertyCvIdentity);

        final Map<String, String> mainJobPropertyCvType = new HashMap<String, String>();
        mainJobPropertyCvType.put(ShmConstants.KEY, BackupActivityConstants.CV_TYPE);
        mainJobPropertyCvType.put(ShmConstants.VALUE, "CVTYPE");
        mainJobConfPropertyList.add(mainJobPropertyCvType);

        jobConfigurationDetails.put(ActivityConstants.JOB_PROPERTIES, mainJobConfPropertyList);
        activityJobAttr.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        final List<Map<String, String>> mainJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> mainJobProperty = new HashMap<String, String>();
        mainJobProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        mainJobProperty.put(ShmConstants.VALUE, "CV");
        mainJobPropertyList.add(mainJobProperty);
        activityJobAttr.put(ActivityConstants.JOB_PROPERTIES, mainJobPropertyList);

        return activityJobAttr;
    }

    private void retryMock() {
        PowerMockito.mockStatic(RetryPolicy.class);
        when(RetryPolicy.builder()).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.attempts(anyInt())).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.waitInterval(anyInt(), eq(TimeUnit.SECONDS))).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.exponentialBackoff(anyDouble())).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.retryOn(exceptionsArray)).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.build()).thenReturn(retryPolicyMock);
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
    }

    private void mockJobActivityInfo() {
        Mockito.when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.LICENSE);
        Mockito.when(jobActivityInfoMock.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
    }

    @Test
    public void testGetLicenseMoFdnShouldPass() {

        final Map<String, Object> licenseMOAttributes = new HashMap<String, Object>();
        licenseMOAttributes.put(FINGER_PRINT, "Some FingerPrint");
        final Map<String, Object> licenseMoWithFDN = new HashMap<String, Object>();
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        licenseMoWithFDN.put(ShmConstants.FDN, licenseMOFdn);

        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, PI_TIME_STAMP);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "141021_131637");
        jobPropertyList.add(jobProperty);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();

        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);
        installLicenseKeyFileServiceMock.getLicenseMoFdn(activityJobId, jobPropertyList, neJobStaticData, jobLogList);
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, jobPropertyList, null, MOACTION_START_PROGRESS_PERCENTAGE);

    }

    @Test
    public void testGetLicenseMoFdnReturnNull() {

        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, PI_TIME_STAMP);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "141021_131637");
        jobPropertyList.add(jobProperty);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();

        assertEquals(null, installLicenseKeyFileServiceMock.getLicenseMoFdn(activityJobId, jobPropertyList, neJobStaticData, jobLogList));
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, jobPropertyList, null, MOACTION_START_PROGRESS_PERCENTAGE);

    }

    @Test
    public void testPrecheckForPoattributeslistIsNull() throws JobDataNotFoundException, MoNotFoundException {

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();

        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(networkElementRetrievalBean.getNetworkElementData(neName)).thenReturn(networkElementInfoMock);
        when(licensingRetryService.getLicenseMoFdn(activityJobId)).thenReturn(licenseMOFdn);
        when(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId())).thenReturn("administrator");

        installLicenseKeyFileServiceMock.precheck(activityJobId);

        Mockito.verify(jobUpdateService).updateRunningJobAttributes(activityJobId, null, jobLogList);
        Mockito.verify(activityUtils).recordEvent("administrator", SHMEvents.LICENSE_INSTALL_PRECHECK, neName, licenseMOFdn,
                "SHM:" + activityJobId + ":" + neName + ":" + JobLogConstants.LICENSE_KEY_FILE_NOT_FOUND);

    }

    @Test
    public void testPrecheckForPoattributeslistIsNotNull() throws JobDataNotFoundException, MoNotFoundException {

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        String logMessage = String.format(JobLogConstants.KEYFILE_MISMATCH_WITH_NETYPE, ActivityConstants.INSTALL_LICENSE, "LTE", neName);
        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProp = new HashMap<String, String>();
        jobProp.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProp.put(ShmConstants.VALUE, licenseFilePath);
        jobPropList.add(jobProp);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);

        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(networkElementRetrievalBean.getNetworkElementData(neName)).thenReturn(networkElementInfoMock);
        when(licensingRetryService.getLicenseMoFdn(activityJobId)).thenReturn(licenseMOFdn);
        when(licenseUtil.isRadioNodeLKF("LTE", neType)).thenReturn(true);

        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);

        final List<Map<String, Object>> poAttributesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(LICENSE_DATA_FINGERPRINT, fingerPrint);
        poAttributes.put(CommonLicensingActivityConstants.PRODUCTTYPE, "LTE");
        poAttributesList.add(poAttributes);
        when(networkElementInfoMock.getNeType()).thenReturn(neType);
        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttribtues);
        when(licensingRetryService.getRestrictedAttributesOfNode(neJobStaticData, neType, "", mainJobAttribtues)).thenReturn(restrictionAttributes);
        when(licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes)).thenReturn(poAttributesList);
        when(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId())).thenReturn("administrator");

        installLicenseKeyFileServiceMock.precheck(activityJobId);

        Mockito.verify(jobUpdateService, times(2)).updateRunningJobAttributes(activityJobId, null, jobLogList);
        Mockito.verify(activityUtils).recordEvent("administrator", SHMEvents.LICENSE_INSTALL_PRECHECK, neName, licenseMOFdn, "SHM:" + activityJobId + ":" + neName + ":" + logMessage);

    }

    @Test
    public void testPrecheckValidationForLKF() throws JobDataNotFoundException, MoNotFoundException {

        final String logMessage = String.format(JobLogConstants.KEYFILE_MISMATCH_WITH_NETYPE, ActivityConstants.INSTALL_LICENSE, "LTE", neName);

        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, "install")).thenReturn(true);
        when(networkElementRetrievalBean.getNetworkElementData(neName)).thenReturn(networkElementInfoMock);
        when(licensingRetryService.getLicenseMoFdn(activityJobId)).thenReturn(licenseMOFdn);
        when(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId())).thenReturn("administrator");

        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);

        final Map<String, Object> jobTemplateAttribtues = new HashMap<String, Object>();
        jobTemplateAttribtues.put(ShmConstants.JOB_CATEGORY, "UI");

        final List<Map<String, Object>> poAttributesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(LICENSE_DATA_FINGERPRINT, fingerPrint);
        poAttributes.put(CommonLicensingActivityConstants.PRODUCTTYPE, "LTE");
        poAttributesList.add(poAttributes);

        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProp = new HashMap<String, String>();
        jobProp.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProp.put(ShmConstants.VALUE, licenseFilePath);
        jobPropList.add(jobProp);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        mainJobAttribtues.put(ShmConstants.JOB_TEMPLATE_ID, JOBTEMPLATEID);

        when(networkElementInfoMock.getNeType()).thenReturn(neType);
        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttribtues);
        when(licensingRetryService.getRestrictedAttributesOfNode(neJobStaticData, neType, "", mainJobAttribtues)).thenReturn(restrictionAttributes);
        when(licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes)).thenReturn(poAttributesList);
        when(jobConfigurationServiceRetryProxyMock.getPOAttributes(Matchers.anyLong())).thenReturn(jobTemplateAttribtues);
        when(licenseUtil.isRadioNodeLKF("LTE", neType)).thenReturn(true);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticDataMock);
        installLicenseKeyFileServiceMock.execute(activityJobId);

        Mockito.verify(activityUtils).recordEvent("administrator", SHMEvents.LICENSE_INSTALL_PRECHECK, neName, licenseMOFdn, "SHM:" + activityJobId + ":" + neName + ":" + logMessage);

    }

    @Test
    public void testGetFingerprintAndProductType_ForPoattributeslistNull() throws JobDataNotFoundException, MoNotFoundException {

        final String logMessage = "License Key file not found.";

        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId())).thenReturn("administrator");
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, "install")).thenReturn(true);
        when(networkElementRetrievalBean.getNetworkElementData(neName)).thenReturn(networkElementInfoMock);
        when(licensingRetryService.getLicenseMoFdn(activityJobId)).thenReturn(licenseMOFdn);

        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);

        final Map<String, Object> jobTemplateAttribtues = new HashMap<String, Object>();
        jobTemplateAttribtues.put(ShmConstants.JOB_CATEGORY, "UI");
        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProp = new HashMap<String, String>();
        jobProp.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProp.put(ShmConstants.VALUE, licenseFilePath);
        jobPropList.add(jobProp);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        mainJobAttribtues.put(ShmConstants.JOB_TEMPLATE_ID, JOBTEMPLATEID);

        when(networkElementInfoMock.getNeType()).thenReturn(neType);
        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttribtues);
        when(licensingRetryService.getRestrictedAttributesOfNode(neJobStaticData, neType, "", mainJobAttribtues)).thenReturn(restrictionAttributes);
        when(licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes)).thenReturn(null);
        when(licensingRetryService.getLicenseMoFdn(activityJobId)).thenReturn(licenseMOFdn);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticDataMock);

        installLicenseKeyFileServiceMock.execute(activityJobId);

        Mockito.verify(activityUtils).recordEvent("administrator", SHMEvents.LICENSE_INSTALL_PRECHECK, neName, "", "SHM:" + neJobStaticData.getNeJobId() + ":" + neName + ":" + logMessage);

    }

    @Test
    public void testMethodThrowsException() throws JobDataNotFoundException {

        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenThrow(Exception.class);
        final ActivityStepResult activityStepResult = installLicenseKeyFileServiceMock.precheck(activityJobId);
        installLicenseKeyFileServiceMock.asyncPrecheck(activityJobId);
        assertNotNull(activityStepResult.getActivityResultEnum());

    }

    @Test
    public void testMethodThrowsJobDataNotFoundException() throws JobDataNotFoundException {

        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenThrow(JobDataNotFoundException.class);
        installLicenseKeyFileServiceMock.cancel(activityJobId);
        final ActivityStepResult activityStepResult = installLicenseKeyFileServiceMock.handleTimeout(activityJobId);
        installLicenseKeyFileServiceMock.asyncHandleTimeout(activityJobId);
        assertNotNull(activityStepResult.getActivityResultEnum());
    }

    @Test
    public void testPrecheckValidation() throws JobDataNotFoundException, MoNotFoundException {

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticDataMock);
        when(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId())).thenReturn("administrator");
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, "install")).thenReturn(true);
        when(networkElementRetrievalBean.getNetworkElementData(neName)).thenReturn(networkElementInfoMock);
        when(licensingRetryService.getLicenseMoFdn(activityJobId)).thenReturn(null);
        installLicenseKeyFileServiceMock.execute(activityJobId);
        Mockito.verify(jobUpdateService).readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);

    }

    @Test
    public void testHandletimeoutThrowsMoNotFoundException() throws JobDataNotFoundException {

        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenThrow(MoNotFoundException.class);
        final ActivityStepResult activityStepResult = installLicenseKeyFileServiceMock.handleTimeout(activityJobId);
        assertNotNull(activityStepResult.getActivityResultEnum());

    }

    @Test
    public void testDeleteLicenseKeyFile() throws JobDataNotFoundException {

        final Map<String, Object> licenseMOAttributes = new HashMap<String, Object>();
        licenseMOAttributes.put(FINGER_PRINT, "Some FingerPrint");
        final Map<String, Object> licenseMoWithFDN = new HashMap<String, Object>();
        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMOAttributes);
        licenseMoWithFDN.put(ShmConstants.FDN, licenseMOFdn);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(licensingRetryService.getLicenseMoAttributes(activityJobId)).thenReturn(licenseMoWithFDN);
        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, neName);
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProperty.put(ShmConstants.VALUE, licenseFilePath);
        jobPropertyList.add(jobProperty);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);
        when(licensingRetryService.getRestrictedAttributesOfNode(neJobStaticData, neType, "", mainJobAttribtues)).thenReturn(restrictionAttributes);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(jobConfigurationServiceRetryProxyMock.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttribtues);
        final List<Map<String, Object>> poAttributesList = new ArrayList<Map<String, Object>>();
        when(licensingRetryService.getAttributesListOfLicensePOs(anyMap())).thenReturn(poAttributesList);
        final ActivityStepResult activityStepResult = installLicenseKeyFileServiceMock.handleTimeout(activityJobId);
        assertNotNull(activityStepResult.getActivityResultEnum());

    }

}
