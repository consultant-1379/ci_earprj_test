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

package com.ericsson.oss.services.shm.es.impl.ecim.licensing;

import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.LICENSE_FILE_PATH;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.NODE_NAME;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfo;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfoProvider;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.ecim.common.FragmentVersionCheck;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.CommonLicensingActivityConstants;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.ecim.licensing.common.EcimLicensingInfo;
import com.ericsson.oss.services.shm.es.ecim.licensing.common.EcimLmUtils;
import com.ericsson.oss.services.shm.es.ecim.licensing.common.LicenseMoService;
import com.ericsson.oss.services.shm.es.ecim.licensing.common.LicenseMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.ecim.licensing.common.LicensePrecheckResponse;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.license.LicensingRetryService;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.ActionResult;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.EcimSwMConstants;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.job.service.JobParameterChangeListener;
import com.ericsson.oss.services.shm.job.timer.NEJobProgressPercentageCache;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.moaction.retry.ActionRetryPolicy;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.notifications.impl.license.LicenseUtil;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@RunWith(MockitoJUnitRunner.class)
public class InstallLicenseKeyFileServiceTest {

    @InjectMocks
    private InstallLicenseKeyFileService installLicenseKeyFileService;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private JobUpdateService jobUpdateService;

    @Mock
    private EcimLmUtils ecimLmUtils;

    @Mock
    private EcimLicensingInfo ecimLicensingInfo;

    @Mock
    private LicenseMoServiceRetryProxy licenseMoService;

    @Mock
    private SystemRecorder systemRecorder;

    @Mock
    private Notification notification;

    @Mock
    private NotificationSubject notificationSubject;

    @Mock
    private DpsDataChangedEvent dpsDataChangedEvent;

    @Mock
    private AsyncActionProgress reportProgress;

    @Mock
    private RetryManager retryManager;

    @Mock
    private ActionRetryPolicy moActionRetryPolicy;

    @Mock
    private RetryPolicy retryPolicyMock;

    @Mock
    private BrmMoServiceRetryProxy brmMoServiceRetryProxy;

    @Mock
    private FailsafeActivateDeactivateService failsafeActivateDeactivateService;

    @Mock
    private JobParameterChangeListener jobParameterChangeListener;

    @Mock
    private ManagedObject managedobject;

    @Mock
    private FdnNotificationSubject fdnNotificationSubjectMock;

    @Mock
    private JobActivityInfo jobActivityInfo;

    @Mock
    private Map<String, Object> actionResult;

    @Mock
    private Map<String, Object> activityJobAttributesMock;

    @Mock
    private Map<String, Object> neJobAttributesMock;

    @Mock
    private FragmentVersionCheck fragmentVersionCheckMock;

    @Mock
    private OssModelInfoProvider ossModelInfoProviderMock;

    @Mock
    private OssModelInfo ossModelInfoMock;

    @Mock
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelperMock;

    @Mock
    private ActivityTimeoutsService activityTimeoutsServiceMock;

    @Mock
    private LicensingRetryService licensingService;

    @Mock
    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Mock
    private List<NetworkElement> neElementListMock;

    @Mock
    private Map<String, Object> neAttributes;

    @Mock
    private LicenseMoService licenseMOService;

    @Mock
    private LicenseUtil licenseUtil;

    @Mock
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculator;

    @Mock
    private NEJobProgressPercentageCache jobProgressPercentageCache;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Mock
    NEJobStaticData neJobStaticData;

    @Mock
    private NetworkElementData networkElement;

    @Mock
    private JobConfigurationServiceRetryProxy jobConfigurationServiceRetryProxy;

    @Mock
    LicensePrecheckResponse licensePrecheckResponse;

    @Mock
    private LicenseMoServiceRetryProxy licenseMoServiceRetryProxy;

    @Mock
    private List<PersistenceObject> licensePOs;

    @Mock
    private PersistenceObject licensePO;

    @Mock
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Mock
    private JobStaticData jobStaticDataMock;

    @Mock
    private JobStaticDataProvider jobStaticDataProvider;

    @Mock
    JobLogUtil jobLogUtil;

    private static final long activityJobId = 123l;
    private static final short actionId = 1;
    private static final short FailedActionId = 0;
    private static final long mainJobId = 124l;
    private static final long JOBTEMPLATEID = 10l;
    private static final String businessKey = "businessKey";
    private static final String nodeName = "nodeName";
    private static final String keyFileMgmtMOFdn = "keyFileMgmtMoFdn";
    private static final String ACTIVITY_NAME = "install";
    private static final String BRM_FRAGMENT_VERSION = "3.4";
    private static final String OLDER_BRM_FRAGMENT_VERSION = "3.3";
    private static final long neJobid = 1234l;
    String inputVersion = "inputVersion";
    private static final String neType = "RadioNode";
    private static final String ossModelIdentity = "2042-630-876";
    private static final String filePath = "path";
    final String licenseFilePath = "Some License File Path";
    final String fingerPrint = "Some FingerPrint";
    final String ProductType = "LTE";

    @Before
    public void setUp() throws UnsupportedFragmentException, MoNotFoundException, JobDataNotFoundException {
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(ecimLmUtils.getLicensingInfo(activityJobId, neJobStaticData, networkElement)).thenReturn(ecimLicensingInfo);
        when(ecimLicensingInfo.getBusinessKey()).thenReturn(businessKey);
        when(ecimLicensingInfo.getActionId()).thenReturn(actionId);
        when(ecimLicensingInfo.getLicenseKeyFilePath()).thenReturn(licenseFilePath);
        when(networkElementRetrivalBean.getNetworkElementData(nodeName)).thenReturn(networkElement);
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getActivityStartTime()).thenReturn(neJobid);
        when(networkElement.getNeType()).thenReturn(neType);
        when(networkElement.getOssModelIdentity()).thenReturn(ossModelIdentity);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        neAttributes = new HashMap<String, Object>();
        neAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neAttributes.put(ShmConstants.NE_NAME, nodeName);
        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(neAttributes);
        when(activityUtils.getPoAttributes(neJobid)).thenReturn(neAttributes);
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        final Map<String, Object> mainJobAttribtues = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, nodeName);
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProperty.put(ShmConstants.VALUE, null);
        jobPropertyList.add(jobProperty);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        neJobPropertyList.add(neJobProperty);
        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);
        mainJobAttribtues.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        final Map<String, Object> jobTemplateAttribtues = new HashMap<String, Object>();
        mainJobAttribtues.put(ShmConstants.JOB_TEMPLATE_ID, JOBTEMPLATEID);
        jobTemplateAttribtues.put(ShmConstants.JOB_CATEGORY, "UI");
        when(jobConfigurationServiceRetryProxy.getMainJobAttributes(Matchers.anyLong())).thenReturn(mainJobAttribtues);
        when(jobConfigurationServiceRetryProxy.getPOAttributes(Matchers.anyLong())).thenReturn(jobTemplateAttribtues);
        when(activityUtils.getPoAttributes(mainJobId)).thenReturn(mainJobAttribtues);
        when(licenseMOService.getFingerPrintFromNode(networkElement)).thenReturn("fingerprint");
    }

    @Test
    public void testPrecheck() throws UnsupportedFragmentException, MoNotFoundException, JobDataNotFoundException {
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(activityJobAttributesMock);
        when(activityJobAttributesMock.get(ShmConstants.NE_JOB_ID)).thenReturn(neJobid);
        when(activityJobAttributesMock.get(ShmConstants.ACTIVITY_START_DATE)).thenReturn(new Date());
        when(activityUtils.getPoAttributes(neJobid)).thenReturn(neJobAttributesMock);
        when(neJobAttributesMock.get(ShmConstants.NE_NAME)).thenReturn(nodeName);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, ActivityConstants.INSTALL_LICENSE_ACTIVITY)).thenReturn(true);
        final ActivityStepResult activityStepResult = installLicenseKeyFileService.precheck(activityJobId);
        Assert.assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, activityStepResult.getActivityResultEnum());
        verify(activityUtils, times(1)).persistStepDurations(eq(activityJobId), Matchers.anyLong(), eq(ActivityStepsEnum.PRECHECK));
    }

    @Test
    public void testPrecheckIfUnmatchedKeyFileOnRadioNode() throws UnsupportedFragmentException, MoNotFoundException, JobDataNotFoundException {
        when(activityJobAttributesMock.get(ShmConstants.NE_JOB_ID)).thenReturn(neJobid);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(licenseMOService.getProducTypeOfLKF(Matchers.any(NetworkElementData.class), Matchers.anyString())).thenReturn(ProductType);
        when(licenseUtil.isRadioNodeLKF(ProductType, "RadioNode")).thenReturn(false);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, ActivityConstants.INSTALL_LICENSE_ACTIVITY)).thenReturn(true);
        final ActivityStepResult activityStepResult = installLicenseKeyFileService.precheck(activityJobId);
        Assert.assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, activityStepResult.getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteForBrmFailsafeActivateDeActivate() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException {
        final List<Map<String, Object>> licensingPOs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        licensingPOs.add(map);
        when(licenseMoServiceRetryProxy.getNotifiableMoFdn(networkElement, ACTIVITY_NAME)).thenReturn(keyFileMgmtMOFdn);
        when(jobConfigurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributesMock);
        when(licenseMoService.isLicensingPoExists(filePath)).thenReturn(true);
        when(licenseMoService.isLicenseKeyFileExistsInSMRS(filePath)).thenReturn(true);
        when(jobParameterChangeListener.getPerformFailsafeBackup()).thenReturn(true);
        when(brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName)).thenReturn(BRM_FRAGMENT_VERSION);
        when(brmMoServiceRetryProxy.getBrmFailsafeBackupMo(nodeName)).thenReturn(managedobject);
        when(activityUtils.getActivityJobAttributeValue(anyMap(), anyString())).thenReturn("install");
        when(licenseMoService.isLicensingPoExists(filePath)).thenReturn(true);
        when(licenseMoService.isLicenseKeyFileExistsInSMRS(filePath)).thenReturn(true);
        when(licenseMoService.executeMoAction(licensePrecheckResponse, neJobStaticData)).thenReturn(actionId);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.ECIM.name(), JobTypeEnum.LICENSE.toString(), ACTIVITY_NAME)).thenReturn(2000);
        installLicenseKeyFileService.execute(activityJobId);
        verify(failsafeActivateDeactivateService, times(1)).triggerBrmFailsafeActivateDeActivate(anyLong(), any(NEJobStaticData.class), anyString(), any(JobActivityInfo.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteForBrmFailsafe_lowerFragmentVersion() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException {
        final List<Map<String, Object>> licensingPOs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        licensingPOs.add(map);

        when(jobParameterChangeListener.getPerformFailsafeBackup()).thenReturn(true);
        when(brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName)).thenReturn(OLDER_BRM_FRAGMENT_VERSION);
        when(brmMoServiceRetryProxy.getBrmFailsafeBackupMo(nodeName)).thenReturn(managedobject);
        when(activityUtils.getActivityJobAttributeValue(anyMap(), anyString())).thenReturn("");
        when(licenseMoService.getNotifiableMoFdn(networkElement, ACTIVITY_NAME)).thenReturn(keyFileMgmtMOFdn);
        when(licenseMoService.isLicensingPoExists(filePath)).thenReturn(true);
        when(licenseMoService.isLicenseKeyFileExistsInSMRS(filePath)).thenReturn(true);
        when(licenseMoServiceRetryProxy.getNotifiableMoFdn(networkElement, ACTIVITY_NAME)).thenReturn(keyFileMgmtMOFdn);
        when(jobConfigurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributesMock);
        when(licenseMoService.executeMoAction(licensePrecheckResponse, neJobStaticData)).thenReturn(actionId);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("SGSN-MME", PlatformTypeEnum.ECIM.name(), JobTypeEnum.LICENSE.toString(), ACTIVITY_NAME)).thenReturn(2000);
        installLicenseKeyFileService.execute(activityJobId);
        verify(systemRecorder, times(1)).recordCommand(SHMEvents.LICENSE_INSTALL_EXECUTE, CommandPhase.STARTED, nodeName, keyFileMgmtMOFdn,
                activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.LICENSE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteForBrmFailsafe_LicenseValidationKeyFilesFail() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException {
        final List<Map<String, Object>> licensingPOs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        licensingPOs.add(map);

        when(jobParameterChangeListener.getPerformFailsafeBackup()).thenReturn(true);
        when(brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName)).thenReturn(OLDER_BRM_FRAGMENT_VERSION);
        when(brmMoServiceRetryProxy.getBrmFailsafeBackupMo(nodeName)).thenReturn(managedobject);
        when(activityUtils.getActivityJobAttributeValue(anyMap(), anyString())).thenReturn("");
        when(licenseMoService.getNotifiableMoFdn(networkElement, ACTIVITY_NAME)).thenReturn(keyFileMgmtMOFdn);
        when(licenseMoService.isLicensingPoExists(filePath)).thenReturn(true);
        when(licenseMoService.isLicenseKeyFileExistsInSMRS(filePath)).thenReturn(true);
        when(licenseMoService.executeMoAction(licensePrecheckResponse, neJobStaticData)).thenReturn(actionId);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("SGSN-MME", PlatformTypeEnum.ECIM.name(), JobTypeEnum.LICENSE.toString(), ACTIVITY_NAME)).thenReturn(2000);
        installLicenseKeyFileService.execute(activityJobId);
        verify(activityUtils, times(5)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecutewithMediationServiceException() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException {
        final List<Map<String, Object>> licensingPOs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        licensingPOs.add(map);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(jobParameterChangeListener.getPerformFailsafeBackup()).thenReturn(true);
        when(brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName)).thenReturn(OLDER_BRM_FRAGMENT_VERSION);
        when(brmMoServiceRetryProxy.getBrmFailsafeBackupMo(nodeName)).thenReturn(managedobject);
        when(licenseMoServiceRetryProxy.getNotifiableMoFdn(networkElement, ACTIVITY_NAME)).thenReturn(keyFileMgmtMOFdn);
        when(jobConfigurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributesMock);
        when(licenseMoService.isLicensingPoExists(filePath)).thenReturn(true);
        when(licenseMoService.isLicenseKeyFileExistsInSMRS(filePath)).thenReturn(true);
        when(activityUtils.getActivityJobAttributeValue(anyMap(), anyString())).thenReturn("");
        when(licenseMoService.getNotifiableMoFdn(networkElement, ACTIVITY_NAME)).thenReturn(keyFileMgmtMOFdn);
        when(licenseMoService.isLicensingPoExists(filePath)).thenReturn(true);
        when(licenseMoService.isLicenseKeyFileExistsInSMRS(filePath)).thenReturn(false);
        final Throwable cause = new Throwable("MediationServiceException");
        final Exception exception = new RuntimeException("MediationServiceExceptionMessage", cause);
        when(licenseMoService.executeMoAction(licensePrecheckResponse, neJobStaticData)).thenThrow(exception);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("SGSN-MME", PlatformTypeEnum.ECIM.name(), JobTypeEnum.LICENSE.toString(), ACTIVITY_NAME)).thenReturn(2000);
        installLicenseKeyFileService.execute(activityJobId);
        verify(systemRecorder, times(1)).recordCommand(SHMEvents.LICENSE_INSTALL_EXECUTE, CommandPhase.FINISHED_WITH_ERROR, nodeName, keyFileMgmtMOFdn, null);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteForBrmFailsafeActivateDeActivateWithException() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException {
        when(jobParameterChangeListener.getPerformFailsafeBackup()).thenReturn(true);
        when(brmMoServiceRetryProxy.getBrmFailsafeBackupMo(nodeName)).thenReturn(managedobject);
        installLicenseKeyFileService.execute(activityJobId);
        verify(activityUtils, times(5)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());
    }

    @Test
    public void testExecuteBrmFailsafeActivateDeActivate_FlagFalse() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException {

        when(jobParameterChangeListener.getPerformFailsafeBackup()).thenReturn(false);
        when(brmMoServiceRetryProxy.getBrmFailsafeBackupMo(nodeName)).thenReturn(managedobject);
        when(licenseMoServiceRetryProxy.getNotifiableMoFdn(networkElement, ACTIVITY_NAME)).thenReturn(keyFileMgmtMOFdn);
        when(jobConfigurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributesMock);
        when(licenseMoService.isLicenseKeyFileExistsInSMRS(filePath)).thenReturn(true);
        when(licenseMoService.getNotifiableMoFdn(networkElement, ACTIVITY_NAME)).thenReturn(keyFileMgmtMOFdn);
        when(licenseMoService.isLicensingPoExists(filePath)).thenReturn(true);
        when(licenseMoService.isLicenseKeyFileExistsInSMRS(filePath)).thenReturn(true);
        installLicenseKeyFileService.execute(activityJobId);
        verify(systemRecorder, times(1)).recordCommand(SHMEvents.LICENSE_INSTALL_EXECUTE, CommandPhase.STARTED, nodeName, keyFileMgmtMOFdn,
                activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.LICENSE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecute_MoNotFoundException() throws UnsupportedFragmentException, ArgumentBuilderException, MoNotFoundException {
        final List<Map<String, Object>> licensingPOs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        licensingPOs.add(map);
        when(licenseMoService.getNotifiableMoFdn(networkElement, ACTIVITY_NAME)).thenThrow(new MoNotFoundException("Mo not found exception"));
        installLicenseKeyFileService.execute(activityJobId);
        verify(activityUtils, times(5)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecute_UnsupportedFragmentException() throws UnsupportedFragmentException, ArgumentBuilderException, MoNotFoundException {
        final List<Map<String, Object>> licensingPOs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        licensingPOs.add(map);
        when(licenseMoService.getNotifiableMoFdn(networkElement, ACTIVITY_NAME)).thenThrow(new UnsupportedFragmentException("Unsupported Fragment version"));
        installLicenseKeyFileService.execute(activityJobId);
        verify(activityUtils, times(5)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecute_ArgumentBuilderException() throws UnsupportedFragmentException, ArgumentBuilderException, MoNotFoundException {
        final List<Map<String, Object>> licensingPOs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        licensingPOs.add(map);
        when(licenseMoService.getNotifiableMoFdn(networkElement, ACTIVITY_NAME)).thenReturn(keyFileMgmtMOFdn);
        when(licenseMoService.isLicensingPoExists(filePath)).thenReturn(true);
        when(licenseMoService.isLicenseKeyFileExistsInSMRS(filePath)).thenReturn(true);
        when(licenseMoService.executeMoAction(licensePrecheckResponse, neJobStaticData)).thenThrow(new ArgumentBuilderException("failed to build action argument"));
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger("SGSN-MME", PlatformTypeEnum.ECIM.name(), JobTypeEnum.LICENSE.toString(), ACTIVITY_NAME)).thenReturn(2000);
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        installLicenseKeyFileService.execute(activityJobId);
        verify(activityUtils, times(5)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecute_licensePOsNotFound() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException {
        when(licenseMoService.getNotifiableMoFdn(networkElement, ACTIVITY_NAME)).thenReturn(keyFileMgmtMOFdn);
        when(licenseMoService.isLicensingPoExists(filePath)).thenReturn(false);
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        installLicenseKeyFileService.execute(activityJobId);
        verify(jobUpdateService, times(2)).readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList());
        verify(activityUtils, times(5)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecute_KeyFileNotFoundInSMRS() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException {
        final List<Map<String, Object>> licensingPOs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        licensingPOs.add(map);
        when(licenseMoService.getNotifiableMoFdn(networkElement, ACTIVITY_NAME)).thenReturn(keyFileMgmtMOFdn);
        when(licenseMoService.isLicensingPoExists(filePath)).thenReturn(true);
        when(licenseMoService.isLicenseKeyFileExistsInSMRS(filePath)).thenReturn(false);
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        installLicenseKeyFileService.execute(activityJobId);
        verify(jobUpdateService, times(2)).readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList());
        verify(activityUtils, times(5)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecute_ActionTriggerFailed() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException {
        final List<Map<String, Object>> licensingPOs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        licensingPOs.add(map);
        when(licenseMoService.getNotifiableMoFdn(networkElement, ACTIVITY_NAME)).thenReturn(keyFileMgmtMOFdn);
        when(licenseMoService.isLicensingPoExists(filePath)).thenReturn(true);
        when(licenseMoService.isLicenseKeyFileExistsInSMRS(filePath)).thenReturn(true);
        when(licenseMoService.executeMoAction(licensePrecheckResponse, neJobStaticData)).thenReturn(FailedActionId);
        when(activityUtils.sendActivateToWFS(ecimLicensingInfo.getBusinessKey(), new HashMap<String, Object>())).thenReturn(true);
        when(moActionRetryPolicy.getDpsMoActionRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManager.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(0);
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        installLicenseKeyFileService.execute(activityJobId);

        verify(jobUpdateService, times(2)).readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList());
        verify(activityUtils, times(5)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecute_ActionTriggerFailedAndActivationfailed() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException {
        final List<Map<String, Object>> licensingPOs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        licensingPOs.add(map);
        when(licenseMoService.getNotifiableMoFdn(networkElement, ACTIVITY_NAME)).thenReturn(keyFileMgmtMOFdn);
        when(licenseMoService.isLicensingPoExists(filePath)).thenReturn(true);
        when(licenseMoService.isLicenseKeyFileExistsInSMRS(filePath)).thenReturn(true);
        when(licenseMoService.executeMoAction(licensePrecheckResponse, neJobStaticData)).thenReturn(FailedActionId);
        when(activityUtils.sendActivateToWFS(ecimLicensingInfo.getBusinessKey(), new HashMap<String, Object>())).thenReturn(false);
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        installLicenseKeyFileService.execute(activityJobId);
        verify(jobUpdateService, times(2)).readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList());
        verify(activityUtils, times(5)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcessNotificationForActivateWithflagTrue() throws UnsupportedFragmentException, MoNotFoundException {
        when(notification.getNotificationSubject()).thenReturn(fdnNotificationSubjectMock);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(activityUtils.getActivityJobId(fdnNotificationSubjectMock)).thenReturn(activityJobId);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsDataChangedEvent);
        final Map<String, AttributeChangeData> modifiedAttributes = new HashMap<String, AttributeChangeData>();
        when(activityUtils.getModifiedAttributes(dpsDataChangedEvent)).thenReturn(modifiedAttributes);
        when(licenseMoService.getValidAsyncActionProgress(nodeName, modifiedAttributes, ACTIVITY_NAME)).thenReturn(reportProgress);
        when(activityUtils.getActivityJobAttributeValue(anyMap(), anyString())).thenReturn("ACTIVATE");
        when(brmMoServiceRetryProxy.getValidAsyncActionProgress(anyString(), anyString(), anyMap())).thenReturn(reportProgress);
        when(failsafeActivateDeactivateService.validateActionProgressReport(reportProgress)).thenReturn(true);
        installLicenseKeyFileService.processNotification(notification);
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    @Test
    public void testProcessNotificationForActivateDeactivateWithException() throws UnsupportedFragmentException, MoNotFoundException {
        when(notification.getNotificationSubject()).thenReturn(fdnNotificationSubjectMock);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(activityUtils.getActivityJobId(fdnNotificationSubjectMock)).thenReturn(activityJobId);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsDataChangedEvent);
        final Map<String, AttributeChangeData> modifiedAttributes = new HashMap<String, AttributeChangeData>();
        when(activityUtils.getModifiedAttributes(dpsDataChangedEvent)).thenReturn(modifiedAttributes);
        installLicenseKeyFileService.processNotification(notification);
        jobUpdateService.readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList());
        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList());
        activityUtils.prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());
        verify(activityUtils, times(1)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcessNotificationForActivateWithflagFalse() throws UnsupportedFragmentException, MoNotFoundException {
        when(notification.getNotificationSubject()).thenReturn(fdnNotificationSubjectMock);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(activityUtils.getActivityJobId(fdnNotificationSubjectMock)).thenReturn(activityJobId);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsDataChangedEvent);
        final Map<String, AttributeChangeData> modifiedAttributes = new HashMap<String, AttributeChangeData>();
        when(activityUtils.getModifiedAttributes(dpsDataChangedEvent)).thenReturn(modifiedAttributes);
        when(licenseMoService.getValidAsyncActionProgress(nodeName, modifiedAttributes, ACTIVITY_NAME)).thenReturn(reportProgress);
        when(activityUtils.getActivityJobAttributeValue(anyMap(), anyString())).thenReturn("ACTIVATE");
        when(brmMoServiceRetryProxy.getValidAsyncActionProgress(anyString(), anyString(), anyMap())).thenReturn(reportProgress);
        when(failsafeActivateDeactivateService.validateActionProgressReport(reportProgress)).thenReturn(false);
        installLicenseKeyFileService.processNotification(notification);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcessNotificationForInstall() throws UnsupportedFragmentException, MoNotFoundException {
        final List<Map<String, Object>> propertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when(notification.getNotificationSubject()).thenReturn(fdnNotificationSubjectMock);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(activityUtils.getActivityJobId(fdnNotificationSubjectMock)).thenReturn(activityJobId);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsDataChangedEvent);
        final Map<String, AttributeChangeData> modifiedAttributes = new HashMap<String, AttributeChangeData>();
        when(activityUtils.getModifiedAttributes(dpsDataChangedEvent)).thenReturn(modifiedAttributes);
        when(licenseMoServiceRetryProxy.getValidAsyncActionProgress(Matchers.anyString(), Matchers.anyMap(), Matchers.anyString())).thenReturn(reportProgress);
        when(activityUtils.getActivityJobAttributeValue(anyMap(), anyString())).thenReturn("install");
        when(licenseMoService.getNotifiableMoFdn(networkElement, ACTIVITY_NAME)).thenReturn(keyFileMgmtMOFdn);
        when(ecimLmUtils.getActionStatus(reportProgress, ACTIVITY_NAME)).thenReturn(getActionResultMap());
        when(reportProgress.getProgressPercentage()).thenReturn(56);
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(licenseMoServiceRetryProxy.getNotifiableMoFdn(networkElement, ACTIVITY_NAME)).thenReturn(keyFileMgmtMOFdn);
        when(jobConfigurationServiceRetryProxy.getNeJobAttributes(Matchers.anyLong())).thenReturn(activityJobAttributesMock);
        when(activityJobAttributesMock.get(ShmConstants.BUSINESS_KEY)).thenReturn(businessKey);
        when(jobConfigurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributesMock);
        when(licenseMoService.isLicensingPoExists(filePath)).thenReturn(true);
        when(licenseMoService.isLicenseKeyFileExistsInSMRS(filePath)).thenReturn(true);
        when(jobUpdateService.readAndUpdateRunningJobAttributes(Matchers.anyInt(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble())).thenReturn(true);
        installLicenseKeyFileService.processNotification(notification);
        verify(jobUpdateService, times(2)).readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList(), anyDouble());
        verify(activityUtils, times(3)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());
        verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, propertyList, jobLogList, reportProgress.getProgressPercentage());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandletimeoutWithActivateDeactivate() throws MoNotFoundException, UnsupportedFragmentException {
        when(activityUtils.getActivityJobAttributeValue(anyMap(), anyString())).thenReturn("activate");
        when(failsafeActivateDeactivateService.validateActionProgressReport(reportProgress)).thenReturn(false);
        when(failsafeActivateDeactivateService.handleTimeoutForActivateDeactivateActivity(any(EcimLicensingInfo.class), anyLong(), anyString(), any(AsyncActionProgress.class),
                any(JobActivityInfo.class))).thenReturn(JobResult.SUCCESS);
        final ActivityStepResultEnum stepResultEnum = ActivityStepResultEnum.REPEAT_EXECUTE;
        final ActivityStepResult repeatActivityStepResult = new ActivityStepResult();
        repeatActivityStepResult.setActivityResultEnum(stepResultEnum);
        when(activityUtils.getActivityStepResult(Matchers.any(ActivityStepResultEnum.class))).thenReturn(repeatActivityStepResult);
        final ActivityStepResult activityStepResult = installLicenseKeyFileService.handleTimeout(activityJobId);
        assertEquals(ActivityStepResultEnum.REPEAT_EXECUTE, activityStepResult.getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandletimeoutWithActivateDeactivateWithUnexpectedAction() throws MoNotFoundException, UnsupportedFragmentException {
        when(activityUtils.getActivityJobAttributeValue(anyMap(), anyString())).thenReturn("install");
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        installLicenseKeyFileService.handleTimeout(activityJobId);
        verify(jobUpdateService, times(2)).readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList());
        verify(activityUtils, times(1)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandletimeoutWithInstallFailed() throws MoNotFoundException, UnsupportedFragmentException {
        final String ACTION_STATUS = "actionStatus";
        when(activityUtils.getActivityJobAttributeValue(anyMap(), anyString())).thenReturn("install");
        when(failsafeActivateDeactivateService.handleTimeoutForActivateDeactivateActivity(ecimLicensingInfo, activityJobId, nodeName, reportProgress, jobActivityInfo)).thenReturn(JobResult.SUCCESS);
        when(licenseMoService.getNotifiableMoFdn(networkElement, ACTIVITY_NAME)).thenReturn(keyFileMgmtMOFdn);
        when(licenseMoService.getActionProgressOfKeyFileMgmtMO(networkElement)).thenReturn(reportProgress);
        when(ecimLmUtils.getActionStatus(reportProgress, ACTIVITY_NAME)).thenReturn(actionResult);
        when(actionResult.get(ACTION_STATUS)).thenReturn(false);
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        installLicenseKeyFileService.handleTimeout(activityJobId);
        verify(jobUpdateService, times(2)).readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList());
        verify(activityUtils, times(1)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandletimeoutWithInstallFailedWithoutActivate() throws MoNotFoundException, UnsupportedFragmentException {
        when(activityUtils.getActivityJobAttributeValue(anyMap(), anyString())).thenReturn("install");
        when(licenseMoService.getNotifiableMoFdn(networkElement, ACTIVITY_NAME)).thenReturn(keyFileMgmtMOFdn);
        final Map<String, Object> activityJobAttributes = new HashMap<>();
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        installLicenseKeyFileService.handleTimeout(activityJobId);
        verify(jobUpdateService, times(2)).readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList());
        verify(activityUtils, times(1)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandletimeoutWithActivateDeactivateWitEx() throws MoNotFoundException, UnsupportedFragmentException {
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        installLicenseKeyFileService.handleTimeout(activityJobId);
        final List<Map<String, Object>> propertyList = new ArrayList<Map<String, Object>>();
        verify(activityUtils, times(1)).prepareJobPropertyList(propertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
        verify(activityUtils, times(1)).prepareJobLogAtrributesList(anyList(), anyString(), (Date) anyObject(), anyString(), anyString());
        verify(jobUpdateService, times(2)).readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList());
    }

    @Test
    public void testCancel() {
        installLicenseKeyFileService.cancel(activityJobId);
    }

    @Test
    public void testProcessNotificationWithNonAVCEvent() {
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.CREATE);
        installLicenseKeyFileService.processNotification(notification);
        verify(activityUtils, times(0)).getModifiedAttributes(notification.getDpsDataChangedEvent());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteForBrmFailsafe_null() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException, JobDataNotFoundException {
        final List<Map<String, Object>> licensingPOs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        licensingPOs.add(map);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(jobParameterChangeListener.getPerformFailsafeBackup()).thenReturn(true);
        when(brmMoServiceRetryProxy.getBrmFailsafeBackupMo(nodeName)).thenReturn(null);
        when(activityUtils.getActivityJobAttributeValue(anyMap(), anyString())).thenReturn("");
        when(licenseMoService.getEmergencyUnlockActivationState(networkElement, neJobStaticData)).thenReturn(true);
        when(licenseMoService.getNotifiableMoFdn(networkElement, ACTIVITY_NAME)).thenReturn(keyFileMgmtMOFdn);
        when(licenseMoServiceRetryProxy.getLicenseKeyFileName(neJobStaticData, networkElement)).thenReturn(filePath);
        when(licenseMoServiceRetryProxy.getNotifiableMoFdn(networkElement, ACTIVITY_NAME)).thenReturn(keyFileMgmtMOFdn);
        when(jobConfigurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributesMock);
        when(licenseMoService.isLicensingPoExists(filePath)).thenReturn(true);
        when(licenseMoService.isLicenseKeyFileExistsInSMRS(filePath)).thenReturn(true);
        when(licenseMoService.executeMoAction(licensePrecheckResponse, neJobStaticData)).thenReturn(actionId);
        when(ecimLmUtils.getLicensingInfo(activityJobId, neJobStaticData, networkElement)).thenReturn(ecimLicensingInfo);
        when(ecimLicensingInfo.isActivateTriggered()).thenReturn(false);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.ECIM.name(), JobTypeEnum.LICENSE.toString(), ACTIVITY_NAME)).thenReturn(2000);
        installLicenseKeyFileService.execute(activityJobId);
        verify(systemRecorder, times(1)).recordCommand(SHMEvents.LICENSE_INSTALL_EXECUTE, CommandPhase.STARTED, nodeName, keyFileMgmtMOFdn,
                activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.LICENSE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteForBrmFailsafe_nullByRetrivingLicensePath() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException, JobDataNotFoundException {
        final List<Map<String, Object>> licensingPOs = new ArrayList<Map<String, Object>>();
        final String fingerprint = "fingerprint";
        final Map<String, Object> map = new HashMap<String, Object>();
        licensingPOs.add(map);
        when(ossModelInfoProviderMock.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoMock);
        when(ossModelInfoMock.getReferenceMIMVersion()).thenReturn(inputVersion);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(jobParameterChangeListener.getPerformFailsafeBackup()).thenReturn(true);
        when(brmMoServiceRetryProxy.getBrmFailsafeBackupMo(nodeName)).thenReturn(null);
        when(activityUtils.getActivityJobAttributeValue(anyMap(), anyString())).thenReturn("");
        when(licenseMoService.getEmergencyUnlockActivationState(networkElement, neJobStaticData)).thenReturn(true);
        when(licenseMoService.getNotifiableMoFdn(networkElement, ACTIVITY_NAME)).thenReturn(keyFileMgmtMOFdn);
        when(licenseMOService.getFingerPrintFromNode(networkElement)).thenReturn(fingerprint);
        when(licenseMoServiceRetryProxy.getLicenseKeyFileName(neJobStaticData, networkElement)).thenReturn(null);
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(NODE_NAME, nodeName);
        final List<Map<String, String>> jobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ShmConstants.KEY, LICENSE_FILE_PATH);
        jobProperty.put(ShmConstants.VALUE, licenseFilePath);
        jobPropertyList.add(jobProperty);
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        neJobPropertyList.add(neJobProperty);
        when(licenseMoServiceRetryProxy.getLicenseKeyFileNameFromFingerPrint(fingerprint)).thenReturn(licenseFilePath);
        when(licenseMoServiceRetryProxy.getNotifiableMoFdn(networkElement, ACTIVITY_NAME)).thenReturn(keyFileMgmtMOFdn);
        when(jobConfigurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributesMock);
        when(licenseMoService.isLicensingPoExists(filePath)).thenReturn(true);
        when(licenseMoService.isLicenseKeyFileExistsInSMRS(filePath)).thenReturn(true);
        when(licenseMoService.executeMoAction(licensePrecheckResponse, neJobStaticData)).thenReturn(actionId);
        when(ecimLmUtils.getLicensingInfo(activityJobId, neJobStaticData, networkElement)).thenReturn(ecimLicensingInfo);
        when(ecimLicensingInfo.isActivateTriggered()).thenReturn(false);
        when(activityTimeoutsServiceMock.getActivityTimeoutAsInteger(neType, PlatformTypeEnum.ECIM.name(), JobTypeEnum.LICENSE.toString(), ACTIVITY_NAME)).thenReturn(2000);
        installLicenseKeyFileService.execute(activityJobId);
        verify(systemRecorder, times(1)).recordCommand(SHMEvents.LICENSE_INSTALL_EXECUTE, CommandPhase.STARTED, nodeName, keyFileMgmtMOFdn,
                activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.LICENSE));
    }

    @Test
    public void testExecuteSuccessFromCLI() throws JobDataNotFoundException, MoNotFoundException, UnsupportedFragmentException {
        setUp();
        final long neJobId = 1L;
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(EcimCommonConstants.LicenseMoConstants.LAST_ACTION_TRIGGERED, "");
        final Map<String, Object> jobTemplateAttribtues = new HashMap<String, Object>();
        jobTemplateAttribtues.put(ShmConstants.JOB_CATEGORY, "CLI");
        when(jobConfigurationServiceRetryProxy.getPOAttributes(Matchers.anyLong())).thenReturn(jobTemplateAttribtues);
        when(jobConfigurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(ecimLicensingInfo.getLicenseKeyFilePath()).thenReturn(null);
        when(licenseMoServiceRetryProxy.getLicenseKeyFileNameFromFingerPrint(fingerPrint)).thenReturn(licenseFilePath);
        when(licenseMoServiceRetryProxy.getSequenceNumberFromNode(networkElement)).thenReturn("LTE11RNC");
        when(licenseMoServiceRetryProxy.getSequenceNumber(fingerPrint)).thenReturn("LTE11RNC");
        when(ecimLmUtils.getLicensingInfo(activityJobId, neJobStaticData, networkElement)).thenReturn(ecimLicensingInfo);
        when(ecimLicensingInfo.getLicenseKeyFilePath()).thenReturn(licenseFilePath);
        when(ecimLicensingInfo.getLicenseKeyFilePath()).thenReturn(licenseFilePath);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(activityUtils.getActivityJobAttributeValue(activityJobAttributes, EcimCommonConstants.LicenseMoConstants.LAST_ACTION_TRIGGERED)).thenReturn("");
        final List<Map<String, String>> jobPropertyList = new ArrayList<>();
        final Map<String, String> jobProperty = new HashMap<>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, CommonLicensingActivityConstants.LICENSE_FILE_PATH);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, licenseFilePath);
        jobPropertyList.add(jobProperty);
        final Map<String, Object> jobPropertiesMap = new HashMap<>();
        jobPropertiesMap.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        installLicenseKeyFileService.execute(activityJobId);

    }

    @Test
    public void testExecuteSuccessFromCLI_NoKeyFileInfoInMainJob() throws JobDataNotFoundException, MoNotFoundException, UnsupportedFragmentException {
        when(ecimLmUtils.getLicensingInfo(anyLong(), any(NEJobStaticData.class), any(NetworkElementData.class))).thenReturn(ecimLicensingInfo);
        when(ecimLicensingInfo.getLicenseKeyFilePath()).thenReturn(null);
        when(licenseMoServiceRetryProxy.getLicenseKeyFileNameFromFingerPrint(anyString())).thenReturn(licenseFilePath);
        installLicenseKeyFileService.execute(activityJobId);
        verify(activityUtils, times(2)).persistStepDurations(anyLong(), anyLong(), any(ActivityStepsEnum.class));
        verify(activityUtils, times(2)).recordEvent(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void testExecute_NoLicenseKeyFilesExists() throws JobDataNotFoundException, MoNotFoundException, UnsupportedFragmentException {
        when(ecimLmUtils.getLicensingInfo(anyLong(), any(NEJobStaticData.class), any(NetworkElementData.class))).thenReturn(ecimLicensingInfo);
        when(ecimLicensingInfo.getLicenseKeyFilePath()).thenReturn(null);
        when(licenseMoServiceRetryProxy.getLicenseKeyFileNameFromFingerPrint(fingerPrint)).thenReturn(null);
        installLicenseKeyFileService.execute(activityJobId);
        verify(jobLogUtil, times(1)).prepareJobLogAtrributesList(anyList(), anyString(), any(), anyString(), anyString());
        verify(activityUtils, times(1)).skipActivity(anyByte(), any(NEJobStaticData.class), anyList(), anyString(), anyString());
    }

    public ActionResult getActionResult_Success() {
        final ActionResult actionResult = new ActionResult();
        actionResult.setActionId(1);
        actionResult.setTriggerSuccess(true);
        return actionResult;
    }

    public ActionResult getActionResult_Fail() {
        final ActionResult actionResult = new ActionResult();
        actionResult.setActionId(0);
        actionResult.setTriggerSuccess(false);
        return actionResult;
    }

    private Map<String, Object> getActionResultMap() {
        final Map<String, Object> actionStatusMap = new HashMap<String, Object>();
        actionStatusMap.put(ActivityConstants.JOB_RESULT, JobResult.SUCCESS);
        actionStatusMap.put(EcimSwMConstants.ACTION_STATUS, true);
        actionStatusMap.put(ActivityConstants.JOB_LOG_MESSAGE, "log message");
        return actionStatusMap;
    }
}
