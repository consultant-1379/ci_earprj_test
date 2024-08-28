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
package com.ericsson.oss.services.shm.es.impl.ecim.upgrade;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.CREATE_UPGRADEPKG_END_PROGRESS_PERCENTAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.NodeAttributesReaderException;
import com.ericsson.oss.services.shm.common.exception.UnsupportedAttributeException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.ecim.common.ActionResultType;
import com.ericsson.oss.services.shm.ecim.common.ActionStateType;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants;
import com.ericsson.oss.services.shm.es.common.ExecuteResponse;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.EcimSwMUtils;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.EcimUpgradeInfo;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.UpMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.ecim.common.SoftwarePackageNameNotFound;
import com.ericsson.oss.services.shm.es.upgrade.api.UpgradePrecheckResponse;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.ActionResult;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.EcimSwMConstants;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.SoftwarePackagePoNotFound;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.timer.NEJobProgressPercentageCache;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ DpsDataChangedEvent.class })
public class PrepareServiceHandlerTest {

    @InjectMocks
    private PrepareServiceHandler objectUnderTest;

    @Mock
    private EcimUpgradeInfo ecimUpgradeInfo;

    @Mock
    private JobEnvironment jobEnvironment;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private JobUpdateService jobUpdateService;

    @Mock
    private UpMoServiceRetryProxy upMoServiceRetryProxy;

    @Mock
    private Map<String, Object> smrsDetailsFromUpMo;

    @Mock
    private Map<String, Object> actualSmrsDetails;

    @Mock
    private JobActivityInfo jobActivityInfo;

    @Mock
    private List<NetworkElement> networkElementsList;

    @Mock
    private NetworkElement networkElement;

    @Mock
    private ActionResult actionResult;

    @Mock
    private ActivityTimeoutsService activityTimeoutsService;

    @Mock
    private SystemRecorder systemRecorder;

    @Mock
    private Notification notification;

    @Mock
    private NotificationSubject notificationSubject;

    @Mock
    private EcimSwMUtils ecimSwMUtils;

    @Mock
    private DpsDataChangedEvent dpsDataChangedEvent;

    @Mock
    private Map<String, AttributeChangeData> modifiedAttributes;

    @Mock
    private AsyncActionProgress progressReport;

    @Mock
    private Logger logger;

    @Mock
    private Map<String, Object> activityJobAttributes;

    @Mock
    private NEJobProgressPercentageCache jobProgressPercentageCache;

    @Mock
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculator;

    @Mock
    private JobLogUtil jobLogUtil;

    @Mock
    private NeJobStaticDataProvider jobStaticDataProvider;

    @Mock
    private JobConfigurationServiceRetryProxy configurationServiceRetryProxy;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    String nodeName = "NE Name";
    String upMoFdn = "UP MO Fdn";
    String swmMoFdn = "SwM Fdn";
    String neType = "RadioNode";
    String businessKey = "Some Business Key";
    private static final String PREPARE_MO_ACTION = "prepare";
    private static final String CREATE_ACTIVITY_NAME = "createUpgradePackage";
    private static final String PREPARE_ACTIVITY_NAME = "prepare";
    long activityJobId = 123;
    long mainJobId = 456;
    long neJobId = 789;
    int actionId = 1;
    final double progressPercentage = 10.0;

    @Test
    public void testHandleExceptionForPrecheck() {
        final String jobLogMessage = "Some Log Message";
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final String errorMessage = "Some Error Message";

        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        final UpgradePrecheckResponse upgradePrecheckResponse = objectUnderTest.handleExceptionForPrecheck(jobLogMessage, ecimUpgradeInfo, jobLogList, errorMessage);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, upgradePrecheckResponse.getActivityStepResultEnum());
    }

    @Test
    public void testMarkActivityWithTriggeredAction() {
        final String actionName = "Some action";
        objectUnderTest.markActivityWithTriggeredAction(activityJobId, actionName);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        verify(activityUtils).prepareJobPropertyList(jobPropertyList, EcimCommonConstants.ACTION_TRIGGERED, actionName);
        verify(jobUpdateService).readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, null, null);
    }

    @Test
    public void testUpdateSmrsDetailsIfRequired()
            throws SoftwarePackageNameNotFound, UnsupportedFragmentException, MoNotFoundException, SoftwarePackagePoNotFound, ArgumentBuilderException, UnsupportedAttributeException {
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(upMoServiceRetryProxy.getUpgradePackageUri(ecimUpgradeInfo)).thenReturn(smrsDetailsFromUpMo);
        when(upMoServiceRetryProxy.buildUpgradePackageUri(ecimUpgradeInfo)).thenReturn(actualSmrsDetails);
        when(smrsDetailsFromUpMo.get(UpgradeActivityConstants.ACTION_ARG_PASSWORD)).thenReturn("passwordFromMO");
        when(actualSmrsDetails.get(UpgradeActivityConstants.ACTION_ARG_PASSWORD)).thenReturn("actualPassword");
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        objectUnderTest.updateSmrsDetailsIfRequired(ecimUpgradeInfo, jobLogList);

        final Map<String, Object> changedAttributes = new HashMap<String, Object>();
        changedAttributes.put(UpgradeActivityConstants.ACTION_ARG_PASSWORD, "actualPassword");
        verify(upMoServiceRetryProxy).updateMOAttributes(ecimUpgradeInfo, changedAttributes);
    }

    @Test
    public void testTriggerPrepareActionWhenPrepareActionTrigerredSuccessfully()
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(ecimUpgradeInfo.getJobEnvironment()).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.PREPARE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn(neType);
        when(actionResult.isTriggerSuccess()).thenReturn(true);
        when(upMoServiceRetryProxy.executeMoAction(ecimUpgradeInfo, EcimSwMConstants.PREPARE_UPGRADE_PACKAGE)).thenReturn(actionResult);
        when(activityTimeoutsService.getActivityTimeoutAsInteger("SGSN-MME", PlatformTypeEnum.ECIM.name(), JobTypeEnum.UPGRADE.toString(), PREPARE_MO_ACTION)).thenReturn(2000);
        activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(configurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final ExecuteResponse executeResponse = objectUnderTest.triggerPrepareAction(upMoFdn, ecimUpgradeInfo, jobActivityInfo, jobLogList);
        assertEquals(true, executeResponse.isActionTriggered());
    }

    @Test
    public void testTriggerPrepareActionWhenPrepareActionTrigerredFailed()
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.PREPARE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn(neType);
        when(actionResult.isTriggerSuccess()).thenReturn(false);
        when(upMoServiceRetryProxy.executeMoAction(ecimUpgradeInfo, EcimSwMConstants.PREPARE_UPGRADE_PACKAGE)).thenReturn(actionResult);
        when(activityTimeoutsService.getActivityTimeoutAsInteger("SGSN-MME", PlatformTypeEnum.ECIM.name(), JobTypeEnum.UPGRADE.toString(), PREPARE_MO_ACTION)).thenReturn(2000);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        objectUnderTest.triggerPrepareAction(upMoFdn, ecimUpgradeInfo, jobActivityInfo, jobLogList);
        verify(jobLogUtil, times(2)).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), (Date) Matchers.any(), Matchers.anyString(), Matchers.anyString());
    }

    @Test
    public void testTriggerPrepareActionWhenPrepareActionTrigerredThrowsUnsupportedFragmentException()
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.PREPARE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn(neType);
        doThrow(UnsupportedFragmentException.class).when(upMoServiceRetryProxy).executeMoAction(ecimUpgradeInfo, EcimSwMConstants.PREPARE_UPGRADE_PACKAGE);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();

        final ExecuteResponse executeResponse = objectUnderTest.triggerPrepareAction(upMoFdn, ecimUpgradeInfo, jobActivityInfo, jobLogList);
        assertEquals(0, executeResponse.getActionId());
        assertEquals(false, executeResponse.isActionTriggered());
    }

    @Test
    public void testTriggerPrepareActionWhenPrepareActionTrigerredThrowsMediationServiceException()
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.PREPARE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn(neType);
        final Throwable cause = new Throwable("MediationServiceException");
        final Exception exception = new RuntimeException("MediationServiceExceptionMessage", cause);
        doThrow(exception).when(upMoServiceRetryProxy).executeMoAction(ecimUpgradeInfo, EcimSwMConstants.PREPARE_UPGRADE_PACKAGE);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final ExecuteResponse executeResponse = objectUnderTest.triggerPrepareAction(upMoFdn, ecimUpgradeInfo, jobActivityInfo, jobLogList);
        assertEquals(false, executeResponse.isActionTriggered());
        assertEquals(0, executeResponse.getActionId());
    }

    @Test
    public void testTriggerPrepareActionWhenPrepareActionTrigerredThrowsException()
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.PREPARE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn(neType);
        doThrow(Exception.class).when(upMoServiceRetryProxy).executeMoAction(ecimUpgradeInfo, EcimSwMConstants.PREPARE_UPGRADE_PACKAGE);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final ExecuteResponse executeResponse = objectUnderTest.triggerPrepareAction(upMoFdn, ecimUpgradeInfo, jobActivityInfo, jobLogList);
        verify(jobLogUtil, times(1)).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), (Date) Matchers.any(), Matchers.anyString(), Matchers.anyString());
        assertEquals(false, executeResponse.isActionTriggered());
        assertEquals(0, executeResponse.getActionId());
    }

    @Test
    public void testTriggerCreateActionWhenPrepareActionTrigerredSuccessfully()
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.CREATE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(swmMoFdn);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn(neType);
        when(actionResult.getActionId()).thenReturn(actionId);
        when(actionResult.isTriggerSuccess()).thenReturn(true);
        when(upMoServiceRetryProxy.executeMoAction(ecimUpgradeInfo, EcimSwMConstants.CREATE_UPGRADE_PACKAGE)).thenReturn(actionResult);
        when(activityTimeoutsService.getActivityTimeoutAsInteger("SGSN-MME", PlatformTypeEnum.ECIM.name(), JobTypeEnum.UPGRADE.toString(), PREPARE_MO_ACTION)).thenReturn(2000);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final ExecuteResponse executeResponse = objectUnderTest.triggerCreateAction(ecimUpgradeInfo, jobActivityInfo, jobLogList);

        assertEquals(true, executeResponse.isActionTriggered());
        assertEquals(1, executeResponse.getActionId());
    }

    @Test(expected = MoNotFoundException.class)
    public void testTriggerCreateActionWhenGetSwmMoThrowsMoNotFoundException()
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {

        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        doThrow(MoNotFoundException.class).when(upMoServiceRetryProxy).getNotifiableMoFdn(EcimSwMConstants.CREATE_UPGRADE_PACKAGE, ecimUpgradeInfo);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        objectUnderTest.triggerCreateAction(ecimUpgradeInfo, jobActivityInfo, jobLogList);
    }

    @Test(expected = SoftwarePackageNameNotFound.class)
    public void testTriggerCreateActionGetUpMoThrowsSoftwarePackageNameNotFound()
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {

        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        doThrow(SoftwarePackageNameNotFound.class).when(upMoServiceRetryProxy).getNotifiableMoFdn(EcimSwMConstants.CREATE_UPGRADE_PACKAGE, ecimUpgradeInfo);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        objectUnderTest.triggerCreateAction(ecimUpgradeInfo, jobActivityInfo, jobLogList);
    }

    @Test(expected = UnsupportedFragmentException.class)
    public void testTriggerCreateActionGetUpMoThrowsUnsupportedFragmentException()
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {

        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        doThrow(UnsupportedFragmentException.class).when(upMoServiceRetryProxy).getNotifiableMoFdn(EcimSwMConstants.CREATE_UPGRADE_PACKAGE, ecimUpgradeInfo);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        objectUnderTest.triggerCreateAction(ecimUpgradeInfo, jobActivityInfo, jobLogList);
    }

    @Test(expected = SoftwarePackagePoNotFound.class)
    public void testTriggerCreateActionGetUpMoThrowsSoftwarePackagePoNotFound()
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {

        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        doThrow(SoftwarePackagePoNotFound.class).when(upMoServiceRetryProxy).getNotifiableMoFdn(EcimSwMConstants.CREATE_UPGRADE_PACKAGE, ecimUpgradeInfo);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        objectUnderTest.triggerCreateAction(ecimUpgradeInfo, jobActivityInfo, jobLogList);
    }

    @Test
    public void testTriggerCreateActionWhenCreateActionTrigerredFailed()
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.CREATE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(swmMoFdn);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn(neType);
        when(actionResult.getActionId()).thenReturn(0);
        when(upMoServiceRetryProxy.executeMoAction(ecimUpgradeInfo, EcimSwMConstants.CREATE_UPGRADE_PACKAGE)).thenReturn(actionResult);
        when(activityTimeoutsService.getActivityTimeoutAsInteger("SGSN-MME", PlatformTypeEnum.ECIM.name(), JobTypeEnum.UPGRADE.toString(), PREPARE_MO_ACTION)).thenReturn(2000);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final ExecuteResponse executeResponse = objectUnderTest.triggerCreateAction(ecimUpgradeInfo, jobActivityInfo, jobLogList);

        assertEquals(false, executeResponse.isActionTriggered());
        assertEquals(0, executeResponse.getActionId());
    }

    @Test
    public void testTriggerCreateActionWhenCreateActionTrigerredThrowsUnsupportedFragmentException()
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.CREATE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(swmMoFdn);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn(neType);
        doThrow(UnsupportedFragmentException.class).when(upMoServiceRetryProxy).executeMoAction(ecimUpgradeInfo, EcimSwMConstants.CREATE_UPGRADE_PACKAGE);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        objectUnderTest.triggerCreateAction(ecimUpgradeInfo, jobActivityInfo, jobLogList);

    }

    @Test
    public void testTriggerCreateActionWhenCreateActionTrigerredThrowsMediationServiceException()
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.CREATE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(swmMoFdn);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn(neType);
        final Throwable cause = new Throwable("MediationServiceException");
        final Exception exception = new RuntimeException("MediationServiceExceptionMessage", cause);
        doThrow(exception).when(upMoServiceRetryProxy).executeMoAction(ecimUpgradeInfo, EcimSwMConstants.CREATE_UPGRADE_PACKAGE);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final ExecuteResponse executeResponse = objectUnderTest.triggerCreateAction(ecimUpgradeInfo, jobActivityInfo, jobLogList);
        assertEquals(false, executeResponse.isActionTriggered());
        assertEquals(0, executeResponse.getActionId());
    }

    @Test
    public void testTriggerCreateActionWhenCreateActionTrigerredThrowsException()
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.CREATE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(swmMoFdn);
        when(networkElementRetrievalBean.getNeType(nodeName)).thenReturn(neType);
        doThrow(Exception.class).when(upMoServiceRetryProxy).executeMoAction(ecimUpgradeInfo, EcimSwMConstants.CREATE_UPGRADE_PACKAGE);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final ExecuteResponse executeResponse = objectUnderTest.triggerCreateAction(ecimUpgradeInfo, jobActivityInfo, jobLogList);
        assertEquals(false, executeResponse.isActionTriggered());
    }

    @Test
    public void testFailActivity() {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        Mockito.doNothing().when(activityUtils).sendNotificationToWFS(neJobStaticData, activityJobId, EcimSwMConstants.PREPARE_UPGRADE_PACKAGE, new HashMap<String, Object>());
        objectUnderTest.failActivity(activityJobId, neJobStaticData, jobLogList, businessKey);
    }

    @Test
    public void testProcessAVCNotificationsForCreateActivityThrowingUnsupportedFragmentException() throws UnsupportedFragmentException, JobDataNotFoundException, MoNotFoundException {

        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(ecimSwMUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(CREATE_ACTIVITY_NAME);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsDataChangedEvent);
        when(activityUtils.getModifiedAttributes(dpsDataChangedEvent)).thenReturn(modifiedAttributes);
        final Date notificationTime = new Date();
        when(activityUtils.getNotificationTimeStamp(notificationSubject)).thenReturn(notificationTime);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(swmMoFdn);

        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        doThrow(UnsupportedFragmentException.class).when(upMoServiceRetryProxy).getValidAsyncActionProgress(nodeName, modifiedAttributes);

        objectUnderTest.processAVCNotifications(notification, jobActivityInfo);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq(String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName)), Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()),
                eq(JobLogLevel.ERROR.toString()));

    }

    @Test
    public void testProcessAVCNotificationsForCreateActivityInRunningState() throws UnsupportedFragmentException, JobDataNotFoundException, MoNotFoundException {
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(ecimSwMUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(CREATE_ACTIVITY_NAME);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsDataChangedEvent);
        when(activityUtils.getModifiedAttributes(dpsDataChangedEvent)).thenReturn(modifiedAttributes);
        final Date notificationTime = new Date();
        when(activityUtils.getNotificationTimeStamp(notificationSubject)).thenReturn(notificationTime);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(swmMoFdn);

        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(upMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, modifiedAttributes)).thenReturn(progressReport);
        when(upMoServiceRetryProxy.isValidAsyncActionProgress(ecimUpgradeInfo, progressReport, EcimSwMConstants.CREATE_UPGRADE_PACKAGE)).thenReturn(false);

        when(progressReport.getState()).thenReturn(ActionStateType.RUNNING);

        objectUnderTest.processAVCNotifications(notification, jobActivityInfo);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), Matchers.anyString(), eq(notificationTime), eq(JobLogType.NE.toString()), eq(JobLogLevel.INFO.toString()));

        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final int progressPercentage = 10;
        when(progressReport.getProgressPercentage()).thenReturn(progressPercentage);
        verify(jobUpdateService).readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);

    }

    @Test
    public void testProcessAVCNotificationsForCreateActivityInFinishedState() throws UnsupportedFragmentException, JobDataNotFoundException, MoNotFoundException {
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(ecimSwMUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(CREATE_ACTIVITY_NAME);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsDataChangedEvent);
        when(activityUtils.getModifiedAttributes(dpsDataChangedEvent)).thenReturn(modifiedAttributes);
        final Date notificationTime = new Date();
        when(activityUtils.getNotificationTimeStamp(notificationSubject)).thenReturn(notificationTime);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(swmMoFdn);

        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(upMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, modifiedAttributes)).thenReturn(progressReport);
        when(upMoServiceRetryProxy.isValidAsyncActionProgress(ecimUpgradeInfo, progressReport, EcimSwMConstants.CREATE_UPGRADE_PACKAGE)).thenReturn(false);

        when(progressReport.getState()).thenReturn(ActionStateType.FINISHED);
        when(progressReport.getResult()).thenReturn(ActionResultType.SUCCESS);

        objectUnderTest.processAVCNotifications(notification, jobActivityInfo);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), Matchers.anyString(), Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.INFO.toString()));

        verify(jobUpdateService).readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcessAVCNotificationsForCreateActivityInFinishedStateButFailed() throws UnsupportedFragmentException, JobDataNotFoundException, MoNotFoundException {
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(ecimSwMUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(CREATE_ACTIVITY_NAME);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsDataChangedEvent);
        when(activityUtils.getModifiedAttributes(dpsDataChangedEvent)).thenReturn(modifiedAttributes);
        final Date notificationTime = new Date();
        when(activityUtils.getNotificationTimeStamp(notificationSubject)).thenReturn(notificationTime);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(swmMoFdn);

        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(upMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, modifiedAttributes)).thenReturn(progressReport);
        when(upMoServiceRetryProxy.isValidAsyncActionProgress(ecimUpgradeInfo, progressReport, EcimSwMConstants.CREATE_UPGRADE_PACKAGE)).thenReturn(false);

        when(progressReport.getState()).thenReturn(ActionStateType.FINISHED);
        when(progressReport.getResult()).thenReturn(ActionResultType.FAILURE);
        when(jobUpdateService.readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList(), anyDouble())).thenReturn(true);

        final String resultInfo = "Result Info";
        when(progressReport.getResultInfo()).thenReturn(resultInfo);
        when(progressReport.getProgressPercentage()).thenReturn((int) progressPercentage);
        when(activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(anyLong(), anyList(), anyList(), anyDouble())).thenReturn(true);

        objectUnderTest.processAVCNotifications(notification, jobActivityInfo);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), Matchers.anyString(), Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.INFO.toString()));

        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);

        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final JobResult jobResult = JobResult.FAILED;
        verify(activityUtils).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());

        verify(activityUtils).recordEvent(SHMEvents.CREATE_UPGRADE_PACKAGE_PROCESS_NOTIFICATION, nodeName, swmMoFdn,
                "SHM:" + activityJobId + ":" + nodeName + ":" + EcimSwMConstants.CREATE_UPGRADE_PACKAGE + jobResult.getJobResult());

        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), Matchers.anyString(), Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.ERROR.toString()));

        verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble());

        verify(activityUtils).unSubscribeToMoNotifications(swmMoFdn, activityJobId, jobActivityInfo);
        verify(activityUtils).sendNotificationToWFS(neJobStaticData, activityJobId, CREATE_ACTIVITY_NAME, null);

    }

    @Test
    public void testProcessCreateNotificationsAsSuccess()
            throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException, JobDataNotFoundException {
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(ecimSwMUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsDataChangedEvent);
        when(dpsDataChangedEvent.getFdn()).thenReturn(upMoFdn);

        final String uri = "/smrsroot/backup/radionode/package1";
        when(upMoServiceRetryProxy.getUriFromUpgradePackageFdn(nodeName, upMoFdn)).thenReturn(uri);

        final String filePath = "/radionode/package1";
        when(upMoServiceRetryProxy.getFilePath(ecimUpgradeInfo)).thenReturn(filePath);

        when(upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.CREATE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(swmMoFdn);

        objectUnderTest.processCreateNotifications(notification, jobActivityInfo);

        verify(activityUtils).unSubscribeToMoNotifications(swmMoFdn, activityJobId, jobActivityInfo);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq(String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, EcimSwMConstants.CREATE_UPGRADE_PACKAGE)),
                Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.INFO.toString()));

        verify(jobUpdateService).readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);

        final Map<String, Object> processVariables = new HashMap<String, Object>();
        processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, true);
        verify(activityUtils).sendNotificationToWFS(neJobStaticData, activityJobId, CREATE_ACTIVITY_NAME, processVariables);

    }

    @Test
    public void testpProcessCreateNotificationsAsSuccessButUnableToUnsubscribeFromNotifications()
            throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException, JobDataNotFoundException {
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(ecimSwMUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsDataChangedEvent);
        when(dpsDataChangedEvent.getFdn()).thenReturn(upMoFdn);

        final String uri = "/smrsroot/backup/radionode/package1";
        when(upMoServiceRetryProxy.getUriFromUpgradePackageFdn(nodeName, upMoFdn)).thenReturn(uri);

        final String filePath = "/radionode/package1";
        when(upMoServiceRetryProxy.getFilePath(ecimUpgradeInfo)).thenReturn(filePath);

        doThrow(UnsupportedFragmentException.class).when(upMoServiceRetryProxy).getNotifiableMoFdn(EcimSwMConstants.CREATE_UPGRADE_PACKAGE, ecimUpgradeInfo);

        objectUnderTest.processCreateNotifications(notification, jobActivityInfo);

        verify(activityUtils, times(0)).unSubscribeToMoNotifications(swmMoFdn, activityJobId, jobActivityInfo);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), eq(String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, EcimSwMConstants.CREATE_UPGRADE_PACKAGE)),
                Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.INFO.toString()));

        verify(jobUpdateService).readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);

        final Map<String, Object> processVariables = new HashMap<String, Object>();
        processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, true);
        verify(activityUtils).sendNotificationToWFS(neJobStaticData, activityJobId, CREATE_ACTIVITY_NAME, processVariables);

    }

    @Test
    public void testProcessAVCNotificationsForCancelActivityInRunningState() throws UnsupportedFragmentException, JobDataNotFoundException, MoNotFoundException {
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(ecimSwMUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(jobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(EcimSwMConstants.CANCEL_UPGRADE_PACKAGE);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsDataChangedEvent);
        when(activityUtils.getModifiedAttributes(dpsDataChangedEvent)).thenReturn(modifiedAttributes);
        final Date notificationTime = new Date();
        when(activityUtils.getNotificationTimeStamp(notificationSubject)).thenReturn(notificationTime);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(swmMoFdn);

        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(upMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, modifiedAttributes)).thenReturn(progressReport);
        when(upMoServiceRetryProxy.isValidAsyncActionProgress(ecimUpgradeInfo, progressReport, EcimSwMConstants.CREATE_UPGRADE_PACKAGE)).thenReturn(false);
        when(progressReport.getState()).thenReturn(ActionStateType.RUNNING);
        when(progressReport.getProgressPercentage()).thenReturn((int) progressPercentage);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        objectUnderTest.processAVCNotifications(notification, jobActivityInfo);
        verify(jobProgressPercentageCache).bufferNEJobs(neJobId);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), Matchers.anyString(), Matchers.any(Date.class), eq(JobLogType.NE.toString()), eq(JobLogLevel.INFO.toString()));

        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, progressPercentage);

    }

    @Test
    public void testProcessAVCNotificationsForCancelActivityInFinishedState() throws UnsupportedFragmentException, JobDataNotFoundException, MoNotFoundException {
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(ecimSwMUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(EcimSwMConstants.CANCEL_UPGRADE_PACKAGE);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsDataChangedEvent);
        when(activityUtils.getModifiedAttributes(dpsDataChangedEvent)).thenReturn(modifiedAttributes);
        final Date notificationTime = new Date();
        when(activityUtils.getNotificationTimeStamp(notificationSubject)).thenReturn(notificationTime);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(swmMoFdn);

        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(upMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, modifiedAttributes)).thenReturn(progressReport);
        when(upMoServiceRetryProxy.isValidAsyncActionProgress(ecimUpgradeInfo, progressReport, EcimSwMConstants.CANCEL_UPGRADE_PACKAGE)).thenReturn(false);

        when(progressReport.getState()).thenReturn(ActionStateType.FINISHED);
        when(progressReport.getResult()).thenReturn(ActionResultType.SUCCESS);

        when(neJobStaticData.getNeJobBusinessKey()).thenReturn(businessKey);
        objectUnderTest.processAVCNotifications(notification, jobActivityInfo);

        verify(activityUtils).sendNotificationToWFS(neJobStaticData, activityJobId, EcimSwMConstants.CANCEL_UPGRADE_PACKAGE, new HashMap<String, Object>());
    }

    @Test
    public void testProcessAVCNotificationsForCreateActivityNotHavingProgressReport() throws UnsupportedFragmentException, JobDataNotFoundException, MoNotFoundException {
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(ecimSwMUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(CREATE_ACTIVITY_NAME);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsDataChangedEvent);
        when(activityUtils.getModifiedAttributes(dpsDataChangedEvent)).thenReturn(modifiedAttributes);
        final Date notificationTime = new Date();
        when(activityUtils.getNotificationTimeStamp(notificationSubject)).thenReturn(notificationTime);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(swmMoFdn);

        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(upMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, modifiedAttributes)).thenReturn(progressReport);
        when(upMoServiceRetryProxy.isValidAsyncActionProgress(ecimUpgradeInfo, progressReport, EcimSwMConstants.CREATE_UPGRADE_PACKAGE)).thenReturn(true);

        objectUnderTest.processAVCNotifications(notification, jobActivityInfo);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        verify(jobLogUtil, times(0)).prepareJobLogAtrributesList(eq(jobLogList), Matchers.anyString(), eq(notificationTime), eq(JobLogType.NE.toString()), eq(JobLogLevel.INFO.toString()));

        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final int progressPercentage = 10;
        when(progressReport.getProgressPercentage()).thenReturn(progressPercentage);
        verify(activityUtils, times(0)).prepareJobPropertyList(eq(jobPropertyList), eq(EcimCommonConstants.ReportProgress.REPORT_PROGRESS_PROGRESS_PERCENTAGE), Matchers.anyString());

        verify(jobUpdateService, times(0)).readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);

    }

    @Test
    public void testProcessAVCNotificationsForPrepareActivityInRunningState() throws UnsupportedFragmentException, JobDataNotFoundException, MoNotFoundException {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(ecimSwMUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(PREPARE_MO_ACTION);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsDataChangedEvent);
        when(activityUtils.getModifiedAttributes(dpsDataChangedEvent)).thenReturn(modifiedAttributes);
        final Date notificationTime = new Date();
        when(activityUtils.getNotificationTimeStamp(notificationSubject)).thenReturn(notificationTime);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(upMoFdn);
        when(jobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(upMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, modifiedAttributes)).thenReturn(progressReport);
        when(upMoServiceRetryProxy.isValidAsyncActionProgress(ecimUpgradeInfo, progressReport, EcimSwMConstants.PREPARE_UPGRADE_PACKAGE)).thenReturn(false);
        when(progressReport.getProgressPercentage()).thenReturn((int) progressPercentage);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(progressReport.getState()).thenReturn(ActionStateType.RUNNING);

        objectUnderTest.processAVCNotifications(notification, jobActivityInfo);
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), Matchers.anyString(), eq(notificationTime), eq(JobLogType.NE.toString()), eq(JobLogLevel.INFO.toString()));
        verify(jobProgressPercentageCache).bufferNEJobs(neJobId);
        verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, CREATE_UPGRADEPKG_END_PROGRESS_PERCENTAGE + progressPercentage / 2);

    }

    @Test
    public void testProcessAVCNotificationsForPrepareActivityInFinishedStateAndCompletedSuccesfully() throws UnsupportedFragmentException, JobDataNotFoundException, MoNotFoundException {
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(ecimSwMUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(PREPARE_ACTIVITY_NAME);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsDataChangedEvent);
        when(activityUtils.getModifiedAttributes(dpsDataChangedEvent)).thenReturn(modifiedAttributes);
        final Date notificationTime = new Date();
        when(activityUtils.getNotificationTimeStamp(notificationSubject)).thenReturn(notificationTime);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(upMoFdn);

        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(upMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, modifiedAttributes)).thenReturn(progressReport);
        when(upMoServiceRetryProxy.isValidAsyncActionProgress(ecimUpgradeInfo, progressReport, EcimSwMConstants.PREPARE_UPGRADE_PACKAGE)).thenReturn(false);

        when(progressReport.getState()).thenReturn(ActionStateType.FINISHED);
        when(progressReport.getResult()).thenReturn(ActionResultType.SUCCESS);
        activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(configurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);

        objectUnderTest.processAVCNotifications(notification, jobActivityInfo);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        verify(jobLogUtil, times(2)).prepareJobLogAtrributesList(eq(jobLogList), Matchers.anyString(), Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.INFO.toString()));

        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        verify(activityUtils).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.getJobResult());

        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);

    }

    @Test
    public void testProcessAVCNotificationsForPrepareActivityInFinishedStateAndFailed() throws UnsupportedFragmentException, JobDataNotFoundException, MoNotFoundException {
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(ecimSwMUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(PREPARE_ACTIVITY_NAME);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsDataChangedEvent);
        when(activityUtils.getModifiedAttributes(dpsDataChangedEvent)).thenReturn(modifiedAttributes);
        final Date notificationTime = new Date();
        when(activityUtils.getNotificationTimeStamp(notificationSubject)).thenReturn(notificationTime);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(upMoFdn);

        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(upMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, modifiedAttributes)).thenReturn(progressReport);
        when(upMoServiceRetryProxy.isValidAsyncActionProgress(ecimUpgradeInfo, progressReport, EcimSwMConstants.PREPARE_UPGRADE_PACKAGE)).thenReturn(false);

        when(progressReport.getState()).thenReturn(ActionStateType.FINISHED);
        when(progressReport.getResult()).thenReturn(ActionResultType.FAILURE);
        activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(configurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);

        objectUnderTest.processAVCNotifications(notification, jobActivityInfo);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        verify(jobLogUtil, times(1)).prepareJobLogAtrributesList(eq(jobLogList), Matchers.anyString(), Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.INFO.toString()));

        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        verify(activityUtils).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());

        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);

    }

    @Test
    public void testProcessAVCNotificationsForPrepareActivityNotHavingProgressReport() throws UnsupportedFragmentException, JobDataNotFoundException, MoNotFoundException {
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(notificationSubject)).thenReturn(activityJobId);
        when(ecimSwMUtils.getEcimUpgradeInformation(activityJobId)).thenReturn(ecimUpgradeInfo);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(ecimUpgradeInfo.getActionTriggered()).thenReturn(PREPARE_ACTIVITY_NAME);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsDataChangedEvent);
        when(activityUtils.getModifiedAttributes(dpsDataChangedEvent)).thenReturn(modifiedAttributes);
        final Date notificationTime = new Date();
        when(activityUtils.getNotificationTimeStamp(notificationSubject)).thenReturn(notificationTime);
        when(activityUtils.getMoFdnFromNotificationSubject(notificationSubject)).thenReturn(upMoFdn);

        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(upMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, modifiedAttributes)).thenReturn(progressReport);
        when(upMoServiceRetryProxy.isValidAsyncActionProgress(ecimUpgradeInfo, progressReport, EcimSwMConstants.PREPARE_UPGRADE_PACKAGE)).thenReturn(true);

        objectUnderTest.processAVCNotifications(notification, jobActivityInfo);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        verify(jobLogUtil, times(0)).prepareJobLogAtrributesList(eq(jobLogList), Matchers.anyString(), eq(notificationTime), eq(JobLogType.NE.toString()), eq(JobLogLevel.INFO.toString()));

        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final int progressPercentage = 10;
        when(progressReport.getProgressPercentage()).thenReturn(progressPercentage);
        verify(activityUtils, times(0)).prepareJobPropertyList(eq(jobPropertyList), eq(EcimCommonConstants.ReportProgress.REPORT_PROGRESS_PROGRESS_PERCENTAGE), Matchers.anyString());

        verify(jobUpdateService, times(0)).readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);

    }

    @Test
    public void testValidateActionProgress() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound {
        final String activityName = PREPARE_ACTIVITY_NAME;

        when(upMoServiceRetryProxy.isValidAsyncActionProgress(ecimUpgradeInfo, progressReport, activityName)).thenReturn(false);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        assertTrue(objectUnderTest.validateActionProgress(progressReport, nodeName, ecimUpgradeInfo, jobLogList, activityName));
    }

    @Test
    public void testValidateActionProgressForInvalidNotification() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound {
        final String activityName = PREPARE_ACTIVITY_NAME;

        when(upMoServiceRetryProxy.isValidAsyncActionProgress(ecimUpgradeInfo, progressReport, activityName)).thenReturn(true);
        when(progressReport.getActionName()).thenReturn(PREPARE_MO_ACTION);
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        assertFalse(objectUnderTest.validateActionProgress(progressReport, nodeName, ecimUpgradeInfo, jobLogList, activityName));
    }

    @Test
    public void testValidteActionProgressForNullAsProgressReport() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound {
        final String activityName = PREPARE_ACTIVITY_NAME;

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        assertFalse(objectUnderTest.validateActionProgress(null, nodeName, ecimUpgradeInfo, jobLogList, activityName));
        verify(jobLogUtil).prepareJobLogAtrributesList(eq(jobLogList), Matchers.anyString(), Matchers.any(Date.class), eq(JobLogType.SYSTEM.toString()), eq(JobLogLevel.ERROR.toString()));
    }

    @Test
    public void testHandleTimeoutForCreateActivity() throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getJobEnvironment()).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.CREATE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(swmMoFdn);
        when(configurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        final List<Map<String, String>> activityJobProperties = new ArrayList<Map<String, String>>();
        final Map<String, String> actionIdProperty = new HashMap<String, String>();
        actionIdProperty.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_ID);
        actionIdProperty.put(ActivityConstants.JOB_PROP_VALUE, Integer.toString(actionId));
        activityJobProperties.add(actionIdProperty);
        when(activityJobAttributes.get(ShmConstants.JOBPROPERTIES)).thenReturn(activityJobProperties);
        when(progressReport.getActionId()).thenReturn(actionId);
        when(progressReport.getResult()).thenReturn(ActionResultType.SUCCESS);

        assertEquals(JobResult.SUCCESS, objectUnderTest.handleTimeoutForCreateActivity(swmMoFdn, ecimUpgradeInfo, progressReport, jobActivityInfo));
    }

    @Test
    public void testHandleTimeoutForCreateActivityForResultAsFailure()
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(ecimUpgradeInfo.getJobEnvironment()).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.CREATE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(swmMoFdn);
        final List<Map<String, String>> activityJobProperties = new ArrayList<Map<String, String>>();
        final Map<String, String> actionIdProperty = new HashMap<String, String>();
        actionIdProperty.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_ID);
        actionIdProperty.put(ActivityConstants.JOB_PROP_VALUE, Integer.toString(actionId));
        activityJobProperties.add(actionIdProperty);
        when(activityJobAttributes.get(ShmConstants.JOBPROPERTIES)).thenReturn(activityJobProperties);
        when(progressReport.getActionId()).thenReturn(actionId);
        when(progressReport.getResult()).thenReturn(ActionResultType.FAILURE);

        assertEquals(JobResult.FAILED, objectUnderTest.handleTimeoutForCreateActivity(swmMoFdn, ecimUpgradeInfo, progressReport, jobActivityInfo));
    }

    @Test
    public void testHandleTimeoutForCreateActivityForResultAsNotAvailable()
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(ecimUpgradeInfo.getJobEnvironment()).thenReturn(jobEnvironment);

        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.CREATE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(swmMoFdn);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        final List<Map<String, String>> activityJobProperties = new ArrayList<Map<String, String>>();
        final Map<String, String> actionIdProperty = new HashMap<String, String>();
        actionIdProperty.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_ID);
        actionIdProperty.put(ActivityConstants.JOB_PROP_VALUE, Integer.toString(actionId));
        activityJobProperties.add(actionIdProperty);
        when(activityJobAttributes.get(ShmConstants.JOBPROPERTIES)).thenReturn(activityJobProperties);
        when(progressReport.getActionId()).thenReturn(actionId);
        when(progressReport.getResult()).thenReturn(ActionResultType.NOT_AVAILABLE);

        assertEquals(JobResult.FAILED, objectUnderTest.handleTimeoutForCreateActivity(swmMoFdn, ecimUpgradeInfo, progressReport, jobActivityInfo));
    }

    @Test
    public void testHandleTimeoutForCreateActivityWhenActionIdDoesntMatch()
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(ecimUpgradeInfo.getJobEnvironment()).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.CREATE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(swmMoFdn);
        when(configurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        final List<Map<String, String>> activityJobProperties = new ArrayList<Map<String, String>>();
        final Map<String, String> actionIdProperty = new HashMap<String, String>();
        actionIdProperty.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_ID);
        actionIdProperty.put(ActivityConstants.JOB_PROP_VALUE, Integer.toString(actionId));
        activityJobProperties.add(actionIdProperty);
        when(activityJobAttributes.get(ShmConstants.JOBPROPERTIES)).thenReturn(activityJobProperties);
        when(progressReport.getActionId()).thenReturn(0);

        when(upMoServiceRetryProxy.isUpgradePackageMoExists(ecimUpgradeInfo)).thenReturn(true);

        assertEquals(JobResult.SUCCESS, objectUnderTest.handleTimeoutForCreateActivity(swmMoFdn, ecimUpgradeInfo, progressReport, jobActivityInfo));
    }

    @Test
    public void testHandleTimeoutForCreateActivityWhenActionIdDoesntMatchAndUpMoDoesntExist()
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getJobEnvironment()).thenReturn(jobEnvironment);
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.CREATE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(swmMoFdn);
        final List<Map<String, String>> activityJobProperties = new ArrayList<Map<String, String>>();
        final Map<String, String> actionIdProperty = new HashMap<String, String>();
        actionIdProperty.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_ID);
        actionIdProperty.put(ActivityConstants.JOB_PROP_VALUE, Integer.toString(actionId));
        activityJobProperties.add(actionIdProperty);
        when(activityJobAttributes.get(ShmConstants.JOBPROPERTIES)).thenReturn(activityJobProperties);
        when(progressReport.getActionId()).thenReturn(0);

        when(upMoServiceRetryProxy.isUpgradePackageMoExists(ecimUpgradeInfo)).thenReturn(false);

        assertEquals(JobResult.FAILED, objectUnderTest.handleTimeoutForCreateActivity(swmMoFdn, ecimUpgradeInfo, progressReport, jobActivityInfo));
    }

    @Test
    public void testHandleTimeoutForPrepareActivity()
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException, NodeAttributesReaderException {
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.PREPARE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(upMoServiceRetryProxy.getAsyncActionProgress(ecimUpgradeInfo)).thenReturn(progressReport);

        when(progressReport.getResult()).thenReturn(ActionResultType.SUCCESS);

        assertEquals(JobResult.SUCCESS, objectUnderTest.handleTimeoutForPrepareActivity(upMoFdn, ecimUpgradeInfo, jobActivityInfo));
    }

    @Test
    public void testHandleTimeoutForPrepareActivityForResultAsFailed()
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException, NodeAttributesReaderException {
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.PREPARE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(upMoServiceRetryProxy.getAsyncActionProgress(ecimUpgradeInfo)).thenReturn(progressReport);

        when(progressReport.getResult()).thenReturn(ActionResultType.FAILURE);

        assertEquals(JobResult.FAILED, objectUnderTest.handleTimeoutForPrepareActivity(upMoFdn, ecimUpgradeInfo, jobActivityInfo));
    }

    @Test
    public void testHandleTimeoutForPrepareActivityForResultAsNotAvailable()
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException, NodeAttributesReaderException {
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.PREPARE_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(upMoServiceRetryProxy.getAsyncActionProgress(ecimUpgradeInfo)).thenReturn(progressReport);

        when(progressReport.getResult()).thenReturn(ActionResultType.NOT_AVAILABLE);

        assertEquals(JobResult.FAILED, objectUnderTest.handleTimeoutForPrepareActivity(upMoFdn, ecimUpgradeInfo, jobActivityInfo));
    }

    @Test
    public void testHandleTimeoutForCancel()
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException, NodeAttributesReaderException {
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.CANCEL_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(upMoServiceRetryProxy.getAsyncActionProgress(ecimUpgradeInfo)).thenReturn(progressReport);
        when(progressReport.getResult()).thenReturn(ActionResultType.SUCCESS);
        assertEquals(JobResult.SUCCESS, objectUnderTest.handleTimeoutForCancel(upMoFdn, ecimUpgradeInfo, jobActivityInfo));
    }

    @Test
    public void testHandleTimeoutForCancelForResultAsFailed()
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException, NodeAttributesReaderException {
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.CANCEL_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(upMoServiceRetryProxy.getAsyncActionProgress(ecimUpgradeInfo)).thenReturn(progressReport);
        when(progressReport.getResult()).thenReturn(ActionResultType.FAILURE);
        assertEquals(JobResult.FAILED, objectUnderTest.handleTimeoutForCancel(upMoFdn, ecimUpgradeInfo, jobActivityInfo));
    }

    @Test
    public void testHandleTimeoutForCancelForResultAsNotAvailable()
            throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException, NodeAttributesReaderException {
        when(ecimUpgradeInfo.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(ecimUpgradeInfo.getActivityJobId()).thenReturn(activityJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(upMoServiceRetryProxy.getNotifiableMoFdn(EcimSwMConstants.CANCEL_UPGRADE_PACKAGE, ecimUpgradeInfo)).thenReturn(upMoFdn);
        when(upMoServiceRetryProxy.getAsyncActionProgress(ecimUpgradeInfo)).thenReturn(progressReport);
        when(progressReport.getResult()).thenReturn(ActionResultType.NOT_AVAILABLE);
        assertEquals(JobResult.FAILED, objectUnderTest.handleTimeoutForCancel(upMoFdn, ecimUpgradeInfo, jobActivityInfo));
    }

    @Test
    public void testhandleExceptionForTimeout() {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        assertEquals(JobResult.FAILED, objectUnderTest.handleExceptionForTimeout("jobLogMessage", "errorMessage", jobLogList));
    }

    @Test
    public void testprecheckHandleTimeout() {
        final long activityJobId = 1;
        final int precheckTimeout = 5;
        when(activityTimeoutsService.getPrecheckTimeoutAsInteger()).thenReturn(precheckTimeout);
        final Map<String, Object> logEntry = new HashMap<String, Object>();
        logEntry.put(ActivityConstants.JOB_LOG_MESSAGE, String.format(JobLogConstants.PRECHECK_TIMEOUT, ActivityConstants.PREPARE, precheckTimeout));
        logEntry.put(ActivityConstants.JOB_LOG_ENTRY_TIME, new Date());
        logEntry.put(ActivityConstants.JOB_LOG_TYPE, JobLogType.SYSTEM.toString());
        logEntry.put(ActivityConstants.JOB_LOG_LEVEL, JobLogLevel.ERROR.toString());
        when(activityUtils.createNewLogEntry(String.format(JobLogConstants.PRECHECK_TIMEOUT, ActivityConstants.PREPARE, precheckTimeout), JobLogLevel.ERROR.toString())).thenReturn(logEntry);

        objectUnderTest.precheckHandleTimeout(activityJobId);
        verify(activityUtils).prepareJobPropertyList(new ArrayList<Map<String, Object>>(), ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        jobLogList.add(logEntry);
        verify(jobUpdateService).readAndUpdateRunningJobAttributes(activityJobId, new ArrayList<Map<String, Object>>(), jobLogList, null);
    }
}
