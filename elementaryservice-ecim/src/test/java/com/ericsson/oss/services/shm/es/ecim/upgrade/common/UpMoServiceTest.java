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
 *---------------------------------------------------------------------------- */
package com.ericsson.oss.services.shm.es.ecim.upgrade.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.StringMatchCondition;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeContainmentRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoActionAbortRetryException;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.NodeAttributesReaderException;
import com.ericsson.oss.services.shm.common.exception.UnsupportedAttributeException;
import com.ericsson.oss.services.shm.common.exception.UpgradePackageMoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfo;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfoProvider;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.smrs.SmrsAccountInfo;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.common.smrs.SmrsServiceConstants;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.ecim.common.SoftwarePackageNameNotFound;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.ActionResult;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.ActivityAllowed;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.SoftwarePackagePoNotFound;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.SwMHandler;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.SwMVersionHandlersProviderFactory;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@RunWith(MockitoJUnitRunner.class)
public class UpMoServiceTest {

    @Mock
    private OssModelInfoProvider ossModelInfoProvider;

    @Mock
    private OssModelInfo ossModelInfo;

    @Mock
    private JobPropertyUtils jobPropertyUtils;

    @Mock
    private SwMVersionHandlersProviderFactory swMprovidersFactory;

    @Mock
    private DataPersistenceService dataPersistenceService;

    @Mock
    private DataBucket dataBucket;

    @Mock
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    @Mock
    private JobUpdateService jobUpdateService;

    @Mock
    private ManagedObject managedObject;

    @Mock
    private PersistenceObject persistenceObject;

    @Mock
    private QueryBuilder queryBuilder;

    @Mock
    private Query<TypeRestrictionBuilder> query;

    @Mock
    private Query<TypeContainmentRestrictionBuilder> containmentQuery;

    @Mock
    private QueryExecutor queryExecutor;

    @Mock
    private Iterator<Object> iterator;

    @Mock
    private Restriction restriction;

    @Mock
    private TypeRestrictionBuilder typeRestrictionBuilder;

    @Mock
    private TypeContainmentRestrictionBuilder typeContainmentRestrictionBuilder;

    @Mock
    private SwMHandler swMHandler;

    @Mock
    private JobEnvironment jobEnvironment;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    private ActivityAllowed activityAllowed;

    @Mock
    private ActionResult actionResult;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Mock
    private List<NetworkElement> neElementListMock;

    @Mock
    private NetworkElementData networkElementData;

    @InjectMocks
    private UpMoService upMoService;

    private AsyncActionProgress asyncActionProgress;

    @Mock
    private RetryManager retryManager;

    @Mock
    private DpsRetryPolicies dpsPolicies;

    @Mock
    private Map<String, String> smrsDetailsMap;

    @Mock
    private SmrsFileStoreService smrsServiceUtil;

    @Mock
    private SmrsAccountInfo smrsAccountInfo;

    @Mock
    private JobConfigurationServiceRetryProxy configurationServiceRetryProxy;

    long activityJobId = 123L;
    long neJobId = 123L;
    long mainJobId = 123L;
    final String nodeName = "NodeName";
    final String activityName = "prepare";

    private final String INPUT_VERSION = "2.3";

    @Mock
    private EcimUpgradeInfo upgradeEnvironment;

    @Before
    public void setup() {
        when(ossModelInfoProvider.getOssModelInfo(Matchers.anyString(), Matchers.anyString(), Matchers.eq(FragmentType.ECIM_SWM_TYPE.getFragmentName()))).thenReturn(ossModelInfo);
        when(ossModelInfo.getReferenceMIMVersion()).thenReturn(INPUT_VERSION);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void isActivityAllowedTest() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackagePoNotFound, SoftwarePackageNameNotFound, ArgumentBuilderException,
            MoActionAbortRetryException, NodeAttributesReaderException, UpgradePackageMoNotFoundException {

        createUpgradeEnvironment();
        mockJobEnvironment();

        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> jobConfiguration = new HashMap<String, Object>();

        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();

        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, EcimCommonConstants.ACTION_TRIGGERED);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "prepare");
        jobPropertyList.add(jobProperty);

        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);

        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.SWP_NAME);
        neJobProperty.put(ActivityConstants.JOB_PROP_VALUE, "SoftwarePackage");

        final Map<String, Object> neJobProperty1 = new HashMap<String, Object>();
        neJobProperty1.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_FDN);
        neJobProperty1.put(ActivityConstants.JOB_PROP_VALUE, "upMoFdn");

        neJobPropertyList.add(neJobProperty);
        neJobPropertyList.add(neJobProperty1);

        neJobAttributes.put(ShmConstants.NE_NAME, nodeName);
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ActivityConstants.JOB_PROPERTIES, neJobPropertyList);

        jobConfiguration.put(ActivityConstants.NETYPEJOBPROPERTIES, neJobPropertyList);

        mainJobAttributes.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfiguration);
        final Map<String, String> keyValueMap = new HashMap<String, String>();
        keyValueMap.put(UpgradeActivityConstants.SWP_NAME, "UpgPkg9");

        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementData);

        when(networkElementData.getNeType()).thenReturn("SGSN-MME");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.ECIM.getName());
        when(jobPropertyUtils.getPropertyValue(Matchers.anyListOf(String.class), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(keyValueMap);
        when(swMprovidersFactory.getSoftwareManagementHandler(INPUT_VERSION)).thenReturn(swMHandler);
        when(swMHandler.isActivityAllowed(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), (NetworkElementData) Matchers.anyObject()))
                .thenReturn(activityAllowed);

        when(upgradeEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(configurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(configurationServiceRetryProxy.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttributes);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(configurationServiceRetryProxy.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);

        Assert.assertNotNull(upMoService.isActivityAllowed(activityName, upgradeEnvironment));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void executeMoActionTest() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound {

        createUpgradeEnvironment();
        mockJobEnvironment();

        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> jobConfiguration = new HashMap<String, Object>();

        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();

        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, EcimCommonConstants.ACTION_TRIGGERED);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "prepare");
        jobPropertyList.add(jobProperty);

        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);

        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.SWP_NAME);
        neJobProperty.put(ActivityConstants.JOB_PROP_VALUE, "SoftwarePackage");

        final Map<String, Object> neJobProperty1 = new HashMap<String, Object>();
        neJobProperty1.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_FDN);
        neJobProperty1.put(ActivityConstants.JOB_PROP_VALUE, "upMoFdn");

        neJobPropertyList.add(neJobProperty);
        neJobPropertyList.add(neJobProperty1);

        neJobAttributes.put(ShmConstants.NE_NAME, nodeName);
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ActivityConstants.JOB_PROPERTIES, neJobPropertyList);

        jobConfiguration.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);

        mainJobAttributes.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfiguration);

        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(UpgradeActivityConstants.UP_PO_FILE_PATH, "filePath");

        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataBucket.findMoByFdn(Matchers.anyString())).thenReturn(null);
        when(managedObject.getFdn()).thenReturn("fdn");
        when(managedObject.getAllAttributes()).thenReturn(attributes);
        when(persistenceObject.getAllAttributes()).thenReturn(attributes);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(queryExecutor.execute(query)).thenReturn(iterator);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(query);
        when(queryBuilder.createTypeQuery(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(containmentQuery);
        when(query.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(containmentQuery.getRestrictionBuilder()).thenReturn(typeContainmentRestrictionBuilder);
        when(typeRestrictionBuilder.equalTo(Matchers.anyString(), Matchers.anyString())).thenReturn(restriction);
        when(typeRestrictionBuilder.allOf(restriction)).thenReturn(restriction);
        when(typeRestrictionBuilder.allOf(restriction, restriction)).thenReturn(restriction);
        when(typeContainmentRestrictionBuilder.matchesString(Matchers.anyString(), Matchers.anyString(), Matchers.any(StringMatchCondition.class))).thenReturn(restriction);
        when(typeContainmentRestrictionBuilder.equalTo(Matchers.anyString(), Matchers.anyString())).thenReturn(restriction);
        when(typeContainmentRestrictionBuilder.allOf(restriction)).thenReturn(restriction);
        when(typeContainmentRestrictionBuilder.allOf(restriction, restriction)).thenReturn(restriction);
        when(queryExecutor.execute(containmentQuery)).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(true, false, true, false);
        when(iterator.next()).thenReturn(managedObject);

        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementData);
        when(networkElementData.getNeType()).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.ECIM.getName());

        when(swMprovidersFactory.getSoftwareManagementHandler(INPUT_VERSION)).thenReturn(swMHandler);

        when(upgradeEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(upgradeEnvironment.getUpgradeType()).thenReturn("SOFT");
        when(configurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(configurationServiceRetryProxy.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttributes);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(configurationServiceRetryProxy.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);

        when(swMHandler.executeMoAction(Matchers.anyString(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), (NetworkElementData) Matchers.anyObject())).thenReturn(actionResult);
        final Map<String, String> keyValueMap = new HashMap<String, String>();
        keyValueMap.put(UpgradeActivityConstants.SWP_NAME, "UpgPkg9");
        when(jobPropertyUtils.getPropertyValue(Matchers.anyListOf(String.class), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(keyValueMap);
        Assert.assertNotNull(upMoService.executeMoAction(upgradeEnvironment, activityName));

    }

    @Test
    public void getValidAsyncActionProgressTest() throws UnsupportedFragmentException, MoNotFoundException {

        final Map<String, AttributeChangeData> attributes = new HashMap<String, AttributeChangeData>();

        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementData);
        when(networkElementData.getNeType()).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.ECIM.getName());

        when(swMprovidersFactory.getSoftwareManagementHandler(INPUT_VERSION)).thenReturn(swMHandler);
        when(swMHandler.getValidAsyncActionProgress(attributes)).thenReturn(asyncActionProgress);

        upMoService.getValidAsyncActionProgress("nodeName", attributes);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void getAsyncActionProgressTest()
            throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException, NodeAttributesReaderException {

        createUpgradeEnvironment();
        mockJobEnvironment();

        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> jobConfiguration = new HashMap<String, Object>();

        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();

        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, EcimCommonConstants.ACTION_TRIGGERED);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "prepare");
        jobPropertyList.add(jobProperty);

        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);

        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.SWP_NAME);
        neJobProperty.put(ActivityConstants.JOB_PROP_VALUE, "SoftwarePackage");

        final Map<String, Object> neJobProperty1 = new HashMap<String, Object>();
        neJobProperty1.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_FDN);
        neJobProperty1.put(ActivityConstants.JOB_PROP_VALUE, "upMoFdn");

        neJobPropertyList.add(neJobProperty);
        neJobPropertyList.add(neJobProperty1);

        neJobAttributes.put(ShmConstants.NE_NAME, nodeName);
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ActivityConstants.JOB_PROPERTIES, neJobPropertyList);

        jobConfiguration.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);

        mainJobAttributes.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfiguration);

        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(UpgradeActivityConstants.UP_PO_FILE_PATH, "filePath");

        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementData);
        when(networkElementData.getNeType()).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.ECIM.getName());

        when(swMprovidersFactory.getSoftwareManagementHandler(INPUT_VERSION)).thenReturn(swMHandler);

        when(upgradeEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(configurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(configurationServiceRetryProxy.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttributes);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(configurationServiceRetryProxy.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);

        when(swMHandler.getAsyncActionProgress(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), (NetworkElementData) Matchers.anyObject()))
                .thenReturn(asyncActionProgress);
        final Map<String, String> keyValueMap = new HashMap<String, String>();
        keyValueMap.put(UpgradeActivityConstants.SWP_NAME, "UpgPkg9");
        when(jobPropertyUtils.getPropertyValue(Matchers.anyListOf(String.class), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(keyValueMap);
        upMoService.getAsyncActionProgress(upgradeEnvironment);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void getNotifiableMoFdnTest() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackagePoNotFound, SoftwarePackageNameNotFound, ArgumentBuilderException {

        createUpgradeEnvironment();
        mockJobEnvironment();

        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> jobConfiguration = new HashMap<String, Object>();

        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();

        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, EcimCommonConstants.ACTION_TRIGGERED);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "prepare");
        jobPropertyList.add(jobProperty);

        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);

        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_FDN);
        neJobProperty.put(ActivityConstants.JOB_PROP_VALUE, "fdn");

        neJobPropertyList.add(neJobProperty);

        neJobAttributes.put(ShmConstants.NE_NAME, nodeName);
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ActivityConstants.JOB_PROPERTIES, neJobPropertyList);

        mainJobAttributes.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfiguration);

        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementData);
        when(networkElementData.getNeType()).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.ECIM.getName());

        when(swMprovidersFactory.getSoftwareManagementHandler(INPUT_VERSION)).thenReturn(swMHandler);
        when(swMHandler.getNotifiableMoFdn(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), (NetworkElementData) Matchers.anyObject())).thenReturn("fdn");

        when(upgradeEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(configurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(configurationServiceRetryProxy.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttributes);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(configurationServiceRetryProxy.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        final Map<String, String> keyValueMap = new HashMap<String, String>();
        keyValueMap.put(UpgradeActivityConstants.SWP_NAME, "UpgPkg9");
        when(jobPropertyUtils.getPropertyValue(Matchers.anyListOf(String.class), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(keyValueMap);
        Assert.assertNotNull(upMoService.getNotifiableMoFdn(activityName, upgradeEnvironment));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void updateMOAttributesTest() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackagePoNotFound, SoftwarePackageNameNotFound, ArgumentBuilderException {

        createUpgradeEnvironment();
        mockJobEnvironment();

        final Map<String, Object> changedAttributes = new HashMap<String, Object>();
        changedAttributes.put(EcimCommonConstants.UpgradePackageMoConstants.UP_MO_IGNORE_BREAK_POINTS, "true");

        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> jobConfiguration = new HashMap<String, Object>();

        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();

        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, EcimCommonConstants.ACTION_TRIGGERED);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "prepare");
        jobPropertyList.add(jobProperty);

        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);

        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_FDN);
        neJobProperty.put(ActivityConstants.JOB_PROP_VALUE, "fdn");

        neJobPropertyList.add(neJobProperty);

        neJobAttributes.put(ShmConstants.NE_NAME, nodeName);
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ActivityConstants.JOB_PROPERTIES, neJobPropertyList);

        mainJobAttributes.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfiguration);

        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementData);
        when(networkElementData.getNeType()).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.ECIM.getName());

        when(swMprovidersFactory.getSoftwareManagementHandler(INPUT_VERSION)).thenReturn(swMHandler);

        when(upgradeEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(configurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(configurationServiceRetryProxy.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttributes);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(configurationServiceRetryProxy.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);

        Mockito.doNothing().when(swMHandler).updateMOAttributes(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), (NetworkElementData) Matchers.anyObject());
        final Map<String, String> keyValueMap = new HashMap<String, String>();
        keyValueMap.put(UpgradeActivityConstants.SWP_NAME, "UpgPkg9");
        when(jobPropertyUtils.getPropertyValue(Matchers.anyListOf(String.class), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(keyValueMap);
        upMoService.updateMOAttributes(upgradeEnvironment, changedAttributes);

    }

    private void createUpgradeEnvironment() {
        when(upgradeEnvironment.getActionTriggered()).thenReturn(activityName);
        when(upgradeEnvironment.isIgnoreBreakPoints()).thenReturn(true);
        when(upgradeEnvironment.getNeJobStaticData()).thenReturn(neJobStaticData);
        when(upgradeEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(upgradeEnvironment.getJobEnvironment()).thenReturn(jobEnvironment);
    }

    private void mockJobEnvironment() {

        Mockito.when(upgradeEnvironment.getActivityJobId()).thenReturn(activityJobId);
        Mockito.when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        Mockito.when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIsUpgradePackageMoExists() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackagePoNotFound {
        createUpgradeEnvironment();
        mockJobEnvironment();

        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> jobConfiguration = new HashMap<String, Object>();

        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();

        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, EcimCommonConstants.ACTION_TRIGGERED);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "prepare");
        jobPropertyList.add(jobProperty);

        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);

        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.SWP_NAME);
        neJobProperty.put(ActivityConstants.JOB_PROP_VALUE, "SoftwarePackage");

        final Map<String, Object> neJobProperty1 = new HashMap<String, Object>();
        neJobProperty1.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_FDN);
        neJobProperty1.put(ActivityConstants.JOB_PROP_VALUE, "upMoFdn");

        neJobPropertyList.add(neJobProperty);
        neJobPropertyList.add(neJobProperty1);

        neJobAttributes.put(ShmConstants.NE_NAME, nodeName);
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ActivityConstants.JOB_PROPERTIES, neJobPropertyList);

        jobConfiguration.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);

        mainJobAttributes.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfiguration);

        when(networkElementRetrievalBean.getNetworkElementData(nodeName)).thenReturn(networkElementData);
        when(networkElementData.getNeType()).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn("ECIM");

        when(swMprovidersFactory.getSoftwareManagementHandler(INPUT_VERSION)).thenReturn(swMHandler);

        when(upgradeEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(configurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(configurationServiceRetryProxy.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttributes);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(configurationServiceRetryProxy.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);

        when(swMHandler.isUpgradePackageMoExists(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), (NetworkElementData) Matchers.anyObject())).thenReturn(true);
        final Map<String, String> keyValueMap = new HashMap<String, String>();
        keyValueMap.put(UpgradeActivityConstants.SWP_NAME, "UpgPkg9");
        when(jobPropertyUtils.getPropertyValue(Matchers.anyListOf(String.class), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(keyValueMap);
        assertTrue(upMoService.isUpgradePackageMoExists(upgradeEnvironment));
    }

    @Test
    public void testIsUpgradePackageMoExistsThrowsSoftwarePackageNameNotFound() throws UnsupportedFragmentException, MoNotFoundException {
        createUpgradeEnvironment();
        mockJobEnvironment();

        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> jobConfiguration = new HashMap<String, Object>();

        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();

        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, EcimCommonConstants.ACTION_TRIGGERED);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "prepare");
        jobPropertyList.add(jobProperty);

        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);

        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.SWP_NAME);
        neJobProperty.put(ActivityConstants.JOB_PROP_VALUE, null);

        final Map<String, Object> neJobProperty1 = new HashMap<String, Object>();
        neJobProperty1.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_FDN);
        neJobProperty1.put(ActivityConstants.JOB_PROP_VALUE, "upMoFdn");

        neJobPropertyList.add(neJobProperty);
        neJobPropertyList.add(neJobProperty1);

        neJobAttributes.put(ShmConstants.NE_NAME, nodeName);
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ActivityConstants.JOB_PROPERTIES, neJobPropertyList);

        jobConfiguration.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);

        mainJobAttributes.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfiguration);

        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementData);
        when(networkElementData.getNeType()).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.ECIM.getName());

        when(swMprovidersFactory.getSoftwareManagementHandler(INPUT_VERSION)).thenReturn(swMHandler);

        when(upgradeEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(configurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(configurationServiceRetryProxy.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttributes);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(configurationServiceRetryProxy.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);

        when(swMHandler.isUpgradePackageMoExists(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), (NetworkElementData) Matchers.anyObject())).thenReturn(false);
        assertFalse(upMoService.isUpgradePackageMoExists(upgradeEnvironment));
    }

    @Test
    public void testIsUpgradePackageMoExistsThrowsUnsupportedFragmentException() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackagePoNotFound {
        createUpgradeEnvironment();
        mockJobEnvironment();

        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> jobConfiguration = new HashMap<String, Object>();

        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();

        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, EcimCommonConstants.ACTION_TRIGGERED);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "prepare");
        jobPropertyList.add(jobProperty);

        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);

        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.SWP_NAME);
        neJobProperty.put(ActivityConstants.JOB_PROP_VALUE, "SoftwarePackage");

        final Map<String, Object> neJobProperty1 = new HashMap<String, Object>();
        neJobProperty1.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_FDN);
        neJobProperty1.put(ActivityConstants.JOB_PROP_VALUE, "upMoFdn");

        neJobPropertyList.add(neJobProperty);
        neJobPropertyList.add(neJobProperty1);

        neJobAttributes.put(ShmConstants.NE_NAME, nodeName);
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ActivityConstants.JOB_PROPERTIES, neJobPropertyList);

        jobConfiguration.put(ActivityConstants.NE_JOB_PROPERTIES, neJobPropertyList);

        mainJobAttributes.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfiguration);

        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementData);
        when(networkElementData.getNeType()).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.ECIM.getName());

        doThrow(UnsupportedFragmentException.class).when(swMprovidersFactory).getSoftwareManagementHandler(INPUT_VERSION);

        when(upgradeEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(configurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(configurationServiceRetryProxy.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttributes);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(configurationServiceRetryProxy.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);

        when(swMHandler.isUpgradePackageMoExists(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), (NetworkElementData) Matchers.anyObject())).thenReturn(true);
        assertFalse(upMoService.isUpgradePackageMoExists(upgradeEnvironment));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetSmrsDetails() throws SoftwarePackageNameNotFound, UnsupportedFragmentException, MoNotFoundException, SoftwarePackagePoNotFound, ArgumentBuilderException,
            MoActionAbortRetryException, NodeAttributesReaderException, UnsupportedAttributeException, UpgradePackageMoNotFoundException {

        createUpgradeEnvironment();
        mockJobEnvironment();

        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> jobConfiguration = new HashMap<String, Object>();

        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();

        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, EcimCommonConstants.ACTION_TRIGGERED);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "prepare");
        jobPropertyList.add(jobProperty);

        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);

        final List<Map<String, Object>> neSpecificPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ShmJobConstants.KEY, UpgradeActivityConstants.SWP_NAME);
        neJobProperty.put(ShmJobConstants.VALUE, "SoftwarePackage");

        final Map<String, Object> neJobProperty1 = new HashMap<String, Object>();
        neJobProperty1.put(ShmJobConstants.KEY, UpgradeActivityConstants.UP_FDN);
        neJobProperty1.put(ShmJobConstants.VALUE, "upMoFdn");

        neSpecificPropertyList.add(neJobProperty);
        neSpecificPropertyList.add(neJobProperty1);

        final Map<String, Object> neJobProperty2 = new HashMap<String, Object>();
        neJobProperty2.put(ShmJobConstants.NE_NAME, nodeName);
        neJobProperty2.put(ShmJobConstants.JOBPROPERTIES, neSpecificPropertyList);

        neJobPropertyList.add(neJobProperty2);

        neJobAttributes.put(ShmConstants.NE_NAME, nodeName);
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ActivityConstants.JOB_PROPERTIES, neJobPropertyList);

        jobConfiguration.put(ShmConstants.NEJOB_PROPERTIES, neJobPropertyList);

        mainJobAttributes.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfiguration);

        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementData);
        when(networkElementData.getNeType()).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.ECIM.getName());

        when(swMprovidersFactory.getSoftwareManagementHandler(INPUT_VERSION)).thenReturn(swMHandler);
        when(swMHandler.isActivityAllowed(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), (NetworkElementData) Matchers.anyObject()))
                .thenReturn(activityAllowed);

        when(upgradeEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(configurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(configurationServiceRetryProxy.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttributes);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(configurationServiceRetryProxy.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        final Map<String, String> keyValueMap = new HashMap<String, String>();
        keyValueMap.put(UpgradeActivityConstants.SWP_NAME, "UpgPkg9");
        when(jobPropertyUtils.getPropertyValue(Matchers.anyListOf(String.class), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(keyValueMap);
        final Map<String, Object> smrsDetails = new HashMap<String, Object>();
        when(swMHandler.getUpgradePackageFileServerDetails("SoftwarePackage", "upMoFdn", nodeName, networkElementData)).thenReturn(smrsDetails);
        assertEquals(smrsDetails, upMoService.getUpgradePackageUri(upgradeEnvironment));

    }

    @Test
    public void testGetUriFromUpgradePackageFdn() throws UnsupportedFragmentException, MoNotFoundException {

        final String upgradePackageMOFdn = "Some Fdn";
        final String uri = "Some URI";
        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementData);
        when(networkElementData.getNeType()).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.ECIM.getName());
        when(swMprovidersFactory.getSoftwareManagementHandler(INPUT_VERSION)).thenReturn(swMHandler);
        when(swMHandler.getUriFromUpgradePackageFdn(upgradePackageMOFdn)).thenReturn(uri);
        assertEquals(uri, upMoService.getUriFromUpgradePackageFdn(nodeName, upgradePackageMOFdn));

    }

    @Test
    public void testGetFilePath() throws MoNotFoundException, ArgumentBuilderException {
        final String filePath = "/home/smrs/smrsroot/backup/ecim/xyz";
        createUpgradeEnvironment();
        when(upgradeEnvironment.getUpgradePackageFilePath()).thenReturn(filePath);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);

        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementData);
        when(networkElementRetrievalBean.getNeType(Matchers.anyString())).thenReturn("ERBS");
        when(networkElementData.getNeType()).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.ECIM.getName());
        when(smrsServiceUtil.getSmrsDetails(SmrsServiceConstants.SOFTWARE_ACCOUNT, "ERBS", nodeName)).thenReturn(smrsAccountInfo);

        when(smrsAccountInfo.getSmrsRootDirectory()).thenReturn("/home/smrs");
        assertEquals("/smrsroot/backup/ecim/xyz", upMoService.getFilePath(upgradeEnvironment));
    }

    @Test
    public void testGetExecuteCancelAction() throws MoNotFoundException, SoftwarePackagePoNotFound, ArgumentBuilderException, UnsupportedFragmentException, SoftwarePackageNameNotFound {
        createUpgradeEnvironment();
        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementData);
        when(networkElementData.getNeType()).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.ECIM.getName());
        when(swMprovidersFactory.getSoftwareManagementHandler(INPUT_VERSION)).thenReturn(swMHandler);
        when(swMHandler.executeCancelAction(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), (NetworkElementData) Matchers.anyObject()))
                .thenReturn(actionResult);
        Assert.assertNotNull(upMoService.executeCancelAction(upgradeEnvironment, activityName));
    }

    @Test
    public void testGetActivationSteps()
            throws UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, MoNotFoundException, ArgumentBuilderException, NodeAttributesReaderException {
        createUpgradeEnvironment();
        mockJobEnvironment();
        final Map<String, Object> changedAttributes = new HashMap<String, Object>();
        changedAttributes.put(EcimCommonConstants.UpgradePackageMoConstants.UP_MO_IGNORE_BREAK_POINTS, "true");
        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementData);
        when(networkElementData.getNeType()).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.ECIM.getName());
        when(swMprovidersFactory.getSoftwareManagementHandler(INPUT_VERSION)).thenReturn(swMHandler);
        when(swMHandler.getActivationSteps(Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), (NetworkElementData) Matchers.anyObject())).thenReturn(10);
        final int getActivationSteps = upMoService.getActivationSteps(upgradeEnvironment, changedAttributes);
        assertTrue(getActivationSteps != 0);
    }

    @Test
    public void testGetSwPkgName() throws SoftwarePackageNameNotFound, MoNotFoundException {

        createUpgradeEnvironment();
        mockJobEnvironment();

        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> jobConfiguration = new HashMap<String, Object>();

        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();

        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, EcimCommonConstants.ACTION_TRIGGERED);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "prepare");
        jobPropertyList.add(jobProperty);

        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);

        final List<Map<String, Object>> neSpecificPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ShmJobConstants.KEY, UpgradeActivityConstants.SWP_NAME);
        neJobProperty.put(ShmJobConstants.VALUE, "SoftwarePackage");

        final Map<String, Object> neJobProperty1 = new HashMap<String, Object>();
        neJobProperty1.put(ShmJobConstants.KEY, UpgradeActivityConstants.UP_FDN);
        neJobProperty1.put(ShmJobConstants.VALUE, "upMoFdn");

        neSpecificPropertyList.add(neJobProperty);
        neSpecificPropertyList.add(neJobProperty1);

        final Map<String, Object> neJobProperty2 = new HashMap<String, Object>();
        neJobProperty2.put(ShmJobConstants.NE_NAME, nodeName);
        neJobProperty2.put(ShmJobConstants.JOBPROPERTIES, neSpecificPropertyList);

        neJobPropertyList.add(neJobProperty2);

        neJobAttributes.put(ShmConstants.NE_NAME, nodeName);
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ActivityConstants.JOB_PROPERTIES, neJobPropertyList);

        jobConfiguration.put(ShmConstants.NEJOB_PROPERTIES, neJobPropertyList);
        mainJobAttributes.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfiguration);

        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementData);
        when(networkElementData.getNeType()).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.ECIM.getName());

        final Map<String, String> softwarePackageNameMap = new HashMap<String, String>();
        softwarePackageNameMap.put(UpgradeActivityConstants.SWP_NAME, "TestPackage29");
        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(softwarePackageNameMap);
        final String swPackage = upMoService.getSwPkgName(upgradeEnvironment);
        assertTrue(swPackage.length() != 0);
    }

    @Test(expected = SoftwarePackageNameNotFound.class)
    public void testGetSwPkgNameThrownWithException() throws SoftwarePackageNameNotFound, MoNotFoundException {

        createUpgradeEnvironment();
        mockJobEnvironment();

        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> jobConfiguration = new HashMap<String, Object>();

        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();

        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, EcimCommonConstants.ACTION_TRIGGERED);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "prepare");
        jobPropertyList.add(jobProperty);

        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);

        final List<Map<String, Object>> neSpecificPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ShmJobConstants.KEY, UpgradeActivityConstants.SWP_NAME);
        neJobProperty.put(ShmJobConstants.VALUE, "SoftwarePackage");

        final Map<String, Object> neJobProperty1 = new HashMap<String, Object>();
        neJobProperty1.put(ShmJobConstants.KEY, UpgradeActivityConstants.UP_FDN);
        neJobProperty1.put(ShmJobConstants.VALUE, "upMoFdn");

        neSpecificPropertyList.add(neJobProperty);
        neSpecificPropertyList.add(neJobProperty1);

        final Map<String, Object> neJobProperty2 = new HashMap<String, Object>();
        neJobProperty2.put(ShmJobConstants.NE_NAME, nodeName);
        neJobProperty2.put(ShmJobConstants.JOBPROPERTIES, neSpecificPropertyList);

        neJobPropertyList.add(neJobProperty2);

        neJobAttributes.put(ShmConstants.NE_NAME, nodeName);
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttributes.put(ActivityConstants.JOB_PROPERTIES, neJobPropertyList);

        jobConfiguration.put(ShmConstants.NEJOB_PROPERTIES, neJobPropertyList);
        mainJobAttributes.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfiguration);

        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementData);
        when(networkElementData.getNeType()).thenReturn("ERBS");
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.ECIM.getName());

        final Map<String, String> softwarePackageNameMap = new HashMap<String, String>();
        softwarePackageNameMap.put(UpgradeActivityConstants.SWP_NAME, null);
        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(softwarePackageNameMap);
        upMoService.getSwPkgName(upgradeEnvironment);
    }

}
