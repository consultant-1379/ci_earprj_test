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
package com.ericsson.oss.services.shm.es.ecim.licensing.common;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeContainmentRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.services.shm.common.FileResource;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.license.InstallLicenseService;
import com.ericsson.oss.services.shm.ecim.common.ActionResultType;
import com.ericsson.oss.services.shm.ecim.common.ActionStateType;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.es.api.CommonLicensingActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimCommonConstants;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@RunWith(MockitoJUnitRunner.class)
public class EcimLmUtilsTest {

    @InjectMocks
    private EcimLmUtils ecimLmUtils;

    @Mock
    private FileResource fileResource;

    @Mock
    private EcimLicensingInfo ecimLicensingInfo;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private JobEnvironment jobEnvironment;

    @Mock
    private DataPersistenceService dataPersistenceService;

    @Mock
    private QueryBuilder queryBuilder;

    @Mock
    private Query<TypeRestrictionBuilder> typeRestrictionQuery;

    @Mock
    private TypeContainmentRestrictionBuilder typeContainmentRestrictionBuilder;

    @Mock
    private DataBucket dataBucket;

    @Mock
    private QueryExecutor queryExecutor;

    @Mock
    private Iterator<Object> iterator;

    @Mock
    private AsyncActionProgress asyncActionProgress;

    @Mock
    private JobConfigurationServiceRetryProxy jobConfigurationServiceRetryProxy;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    private NetworkElementData networkElement;

    @Mock
    private JobPropertyUtils jobPropertyUtils;
    
    @Mock
    private InstallLicenseService installLicenseService;

    private static final String filePath = "direcoty/filePath/file";
    private static final String ACTION_STATUS = "actionStatus";
    private static final String ACTIVITY_NAME = "install";
    private static final String nodeName = "nodeName";
    private static final String neType = "RadioNode";
    private static final String fingerPrint = "LTE02ERBS00001_fp";
    final long activityJobId = 0L;
    final long neJobId = 1L;
    final long mainJobId = 1L;

    @Test
    public void testGetActionStatus() {
        when(asyncActionProgress.getState()).thenReturn(ActionStateType.FINISHED);
        when(asyncActionProgress.getResult()).thenReturn(ActionResultType.SUCCESS);
        final Map<String, Object> result = ecimLmUtils.getActionStatus(asyncActionProgress, ACTIVITY_NAME);

        Assert.assertNotNull(result);
        Assert.assertEquals(true, result.get(ACTION_STATUS));
    }

    @Test
    public void testGetActionStatus_whenActionInRunninginHandleTimeout() {
        when(asyncActionProgress.getState()).thenReturn(ActionStateType.RUNNING);
        when(asyncActionProgress.getResult()).thenReturn(ActionResultType.SUCCESS);
        final Map<String, Object> result = ecimLmUtils.getActionStatus(asyncActionProgress, ACTIVITY_NAME);
        Assert.assertEquals(false, result.get(ACTION_STATUS));
    }

    @Test
    public void testGetActionStatus_ActionInCancelledState() {
        when(asyncActionProgress.getState()).thenReturn(ActionStateType.FINISHED);
        final Map<String, Object> result = ecimLmUtils.getActionStatus(asyncActionProgress, ACTIVITY_NAME);
        Assert.assertEquals(true, result.get(ACTION_STATUS));
    }

    @Test
    public void testGetActionStatus_ActionResultTypeFailure() {
        when(asyncActionProgress.getState()).thenReturn(ActionStateType.FINISHED);
        when(asyncActionProgress.getResult()).thenReturn(ActionResultType.FAILURE);
        final Map<String, Object> result = ecimLmUtils.getActionStatus(asyncActionProgress, ACTIVITY_NAME);
        Assert.assertNotNull(result);
        Assert.assertEquals(true, result.get(ACTION_STATUS));
    }

    @Test
    public void testGetActionStatus_ActionResultTypeNotAvailable() {
        when(asyncActionProgress.getState()).thenReturn(ActionStateType.FINISHED);
        when(asyncActionProgress.getResult()).thenReturn(ActionResultType.NOT_AVAILABLE);
        final Map<String, Object> result = ecimLmUtils.getActionStatus(asyncActionProgress, ACTIVITY_NAME);
        Assert.assertNotNull(result);
        Assert.assertEquals(true, result.get(ACTION_STATUS));
    }

    @Test
    public void testGetLicensingInfo() throws UnsupportedFragmentException, MoNotFoundException {
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, EcimCommonConstants.ReportProgress.REPORT_PROGRESS_ACTION_ID);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "2");
        jobPropertyList.add(jobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, "123@NodeName");
        when(jobConfigurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(jobConfigurationServiceRetryProxy.getNeJobAttributes(activityJobId)).thenReturn(neJobAttributes);
        when(jobConfigurationServiceRetryProxy.getMainJobAttributes(mainJobId)).thenReturn(neJobAttributes);
        ecimLicensingInfo = ecimLmUtils.getLicensingInfo(activityJobId, neJobStaticData, networkElement);
        assertTrue(ecimLicensingInfo != null);
    }

    @Test
    public void testGetLicensingInfoActivateTriggered() throws MoNotFoundException, UnsupportedFragmentException {
        final String filePathWitSmrsPath = "/home/smrs";
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, String> propertyValue = new HashMap<String, String>();
        propertyValue.put(CommonLicensingActivityConstants.LICENSE_FILE_PATH, filePathWitSmrsPath);
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, "isActivateTriggered");
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "2");
        jobPropertyList.add(jobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, "123@NodeName");
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getPlatformType()).thenReturn("ECIM");
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(networkElementRetrivalBean.getNetworkElementData(nodeName)).thenReturn(networkElement);
        when(networkElement.getNeType()).thenReturn(neType);
        when(networkElementRetrivalBean.getNetworkElementData(nodeName)).thenReturn(networkElement);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(networkElement.getNeType()).thenReturn(neType);
        when(jobConfigurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(jobConfigurationServiceRetryProxy.getNeJobAttributes(activityJobId)).thenReturn(neJobAttributes);
        when(jobConfigurationServiceRetryProxy.getMainJobAttributes(mainJobId)).thenReturn(neJobAttributes);
        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(propertyValue);
        ecimLicensingInfo = ecimLmUtils.getLicensingInfo(activityJobId, neJobStaticData, networkElement);
        assertTrue(ecimLicensingInfo != null);
    }

    @Test
    public void testGetLicensingInfoInstallStatus() throws UnsupportedFragmentException, MoNotFoundException {
        final long activityJobId = 0L;
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, "installStatus");
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "2");
        jobPropertyList.add(jobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, "123@NodeName");
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        when(jobEnvironment.getNeJobAttributes()).thenReturn(neJobAttributes);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        ecimLicensingInfo = ecimLmUtils.getLicensingInfo(activityJobId, neJobStaticData, networkElement);
        assertTrue(ecimLicensingInfo != null);
    }

    @Test
    public void testIIsLicensingPOExists() throws MoNotFoundException, UnsupportedFragmentException {
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(CommonLicensingActivityConstants.LICENSE_DATA_PO_NAMESPACE, CommonLicensingActivityConstants.LICENSE_DATA_PO)).thenReturn(typeRestrictionQuery);
        when(typeRestrictionQuery.getRestrictionBuilder()).thenReturn(typeContainmentRestrictionBuilder);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(queryExecutor.execute(typeRestrictionQuery)).thenReturn(iterator);
        Assert.assertNotNull(ecimLmUtils.isLicensingPOExists(filePath));
    }

    @Test
    public void testIsLicenseKeyFileExistsInSMRS() throws MoNotFoundException, UnsupportedFragmentException {

        when(fileResource.exists(filePath)).thenReturn(true);
        ecimLmUtils.isLicenseKeyFileExistsInSMRS(filePath);
    }
    
    @Test
    public void testGetLicenseKeyFilePath() throws MoNotFoundException, UnsupportedFragmentException {
        final String filePathWitSmrsPath = "/home/smrs";
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperty = new HashMap<String, Object>();
        neJobProperty.put(ActivityConstants.JOB_PROP_KEY, CommonLicensingActivityConstants.LICENSE_FILE_PATH);
        neJobProperty.put(ActivityConstants.JOB_PROP_VALUE, filePathWitSmrsPath);
        neJobPropertyList.add(neJobProperty);
        neJobAttributes.put(ActivityConstants.JOB_PROPERTIES, neJobPropertyList);
        neJobAttributes.put(ShmConstants.BUSINESS_KEY, "123@"+nodeName);
        final Map<String, String> propertyValue = new HashMap<String, String>();
        propertyValue.put(CommonLicensingActivityConstants.LICENSE_FILE_PATH, null);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getPlatformType()).thenReturn("ECIM");
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(networkElementRetrivalBean.getNetworkElementData(nodeName)).thenReturn(networkElement);
        when(networkElement.getNeType()).thenReturn(neType);
        when(jobConfigurationServiceRetryProxy.getNeJobAttributes(neJobId)).thenReturn(neJobAttributes);
        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(propertyValue);
        Assert.assertNotNull(ecimLmUtils.getLicenseKeyFilePath(neJobStaticData, networkElement,neJobAttributes));
    }
    
    @Test
    public void testGetLicenseKeyFilePathFromFingerPrint() {
        when(installLicenseService.generateLicenseKeyFilePath(fingerPrint)).thenReturn("LTE01dg2ERBS00001_fp_161005_170054.xml");
        Assert.assertNotNull(ecimLmUtils.getLicenseKeyFilePathFromFingerPrint(fingerPrint));
    }
    
    @Test
    public void testGetSequenceNumber() {
        when(installLicenseService.getSequencenumber(fingerPrint)).thenReturn("1000");
        Assert.assertNotNull(ecimLmUtils.getSequenceNumber(fingerPrint));
    }
}
