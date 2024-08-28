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
package com.ericsson.oss.services.shm.es.impl.minilink.common;

import static com.ericsson.oss.itpf.datalayer.dps.BucketProperties.SUPPRESS_CONSTRAINTS;
import static com.ericsson.oss.itpf.datalayer.dps.BucketProperties.SUPPRESS_MEDIATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.ericsson.cds.cdi.support.configuration.InjectionProperties;
import com.ericsson.cds.cdi.support.rule.CdiInjectorRule;
import com.ericsson.cds.cdi.support.rule.ImplementationInstance;
import com.ericsson.cds.cdi.support.rule.MockedImplementation;
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest;
import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeContainmentRestrictionBuilder;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.FdnUtils;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.ServerInternalException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants;
import com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.xfSwGlobalState;
import com.ericsson.oss.services.shm.es.impl.minilink.upgrade.MiniLinkActivityUtil;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.minilinkindoor.common.ManagedObjectUtil;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;

public class UpgradeUtilTest {

    private static final long ACTIVITY_JOB_ID = 123456L;
    private static final String SW_OBJECT_FDN = "ManagedElement=MLTN-1301,xfSwObjects=1";
    private static final String LIVE_BUCKET = "Live";

    private final JobActivityInfo jobActivityInfo = new JobActivityInfo(ACTIVITY_JOB_ID, "DOWNLOAD_ACTIVITY", JobTypeEnum.UPGRADE, PlatformTypeEnum.MINI_LINK_INDOOR);

    private InjectionProperties injectionProperties = new InjectionProperties();

    public UpgradeUtilTest() {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.es");
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.shared");
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.common");
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.jobs");
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job");
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.nejob");
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.networkelement");
    }

    @Rule
    public CdiInjectorRule cdiInjectorRule = new CdiInjectorRule(this, injectionProperties);

    @ObjectUnderTest
    private MiniLinkActivityUtil miniLinkActivityUtil;

    @MockedImplementation
    private DpsReader dpsReader;

    @MockedImplementation
    private DpsWriter dpsWriter;

    @MockedImplementation
    private JobPropertyUtils jobPropertyUtils;

    @MockedImplementation
    private ManagedObjectUtil managedObjectUtil;

    @MockedImplementation
    private JobEnvironment jobEnvironment;

    @MockedImplementation
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    @MockedImplementation
    private JobUpdateService jobUpdateService;

    @MockedImplementation
    private ManagedObject entryMO;

    @MockedImplementation
    private FdnUtils fdnUtils;

    @MockedImplementation
    private DataPersistenceService dataPersistenceService;

    @MockedImplementation
    private QueryBuilder queryBuilder;

    @MockedImplementation
    private DataBucket liveBucket;

    @MockedImplementation
    private Iterator queryIterator;

    @MockedImplementation
    private Query<TypeContainmentRestrictionBuilder> query;

    @MockedImplementation
    private QueryExecutor queryExecutor;

    @MockedImplementation
    private ManagedObject managedObject;

    class TestActivityUtils extends ActivityUtils {
        public int sendNotifications = 0;

        public List<String> unsubscribeMos;

        @Override
        public Map<String, Object> getJobConfigurationDetails(final long activityJobId) {
            return Collections.emptyMap();
        }

        @Override
        public JobEnvironment getJobEnvironment(final long activityJobId) {
            return jobEnvironment;
        }

        @Override
        public void sendNotificationToWFS(final NEJobStaticData neJobStaticData, final long activityJobId, final String activity, final Map<String, Object> processVariables) {
            sendNotifications++;
        }

        @Override
        public boolean unSubscribeToMoNotifications(final String moFdn, final long activityJobId, final JobActivityInfo jobActivityInfo) {
            unsubscribeMos.add(moFdn);
            return true;
        }

        public void clearUp() {
            unsubscribeMos = new ArrayList<>();
            sendNotifications = 0;
        }

        public boolean isUnsubscribedMo(final String moFdn) {
            return unsubscribeMos.contains(moFdn);
        }

    }

    @ImplementationInstance
    private final ActivityUtils activityUtils = new TestActivityUtils();

    @Before
    public void setup() {
        ((TestActivityUtils) activityUtils).clearUp();
        when(jobUpdateService.readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList())).thenReturn(true);
        when(jobEnvironment.getNodeName()).thenReturn("testname");
    }

    @Test
    public void testFinishActivity() {
        final List<Map<String, Object>> jobLogList = Collections.emptyList();

        miniLinkActivityUtil.finishInstallActivity(jobActivityInfo, "grandpa=1,dad=1,son=1", JobResult.SUCCESS, jobLogList, "testActivity");
        miniLinkActivityUtil.sendNotification(jobActivityInfo, "testActivity");

        assertEquals(1, ((TestActivityUtils) activityUtils).sendNotifications);
        assertTrue(((TestActivityUtils) activityUtils).isUnsubscribedMo("grandpa=1,dad=1,son=1"));
        assertTrue(((TestActivityUtils) activityUtils).isUnsubscribedMo("grandpa=1,dad=1"));
    }

    @Test
    public void testSetActiveRelease() {
        miniLinkActivityUtil.setJobProperty("ActiveRelease", "2", ACTIVITY_JOB_ID);

        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        activityUtils.addJobProperty("ActiveRelease", "2", jobPropertyList);
        verify(jobUpdateService, times(1)).readAndUpdateRunningJobAttributes(ACTIVITY_JOB_ID, jobPropertyList, null, 0.0);
    }

    @Test
    public void testFetchActiveRelease() {
        final Map<String, Object> jobAttr = new HashMap<String, Object>();
        final List<Map<String, String>> jobPropertyList = new ArrayList<>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put("key", "ActiveRelease");
        jobProperty.put("value", "2");
        jobPropertyList.add(jobProperty);
        jobAttr.put("jobProperties", jobPropertyList);
        when(jobUpdateService.retrieveJobWithRetry(ACTIVITY_JOB_ID)).thenReturn(jobAttr);

        final String activeRelease = miniLinkActivityUtil.fetchJobProperty(ACTIVITY_JOB_ID, "ActiveRelease");
        assertTrue("2".equals(activeRelease));
    }

    @Test
    public void testGetParentFdn() {
        final String parentFdn = miniLinkActivityUtil.getParentFdn("tag1=1,tag2=2,tag3=3");
        assertTrue("tag1=1,tag2=2".equals(parentFdn));
    }

    @Test
    public void testGetParentFdnShort() {
        final String parentFdn = miniLinkActivityUtil.getParentFdn("tag1=1");
        assertTrue("tag1=1".equals(parentFdn));
    }

    @Test
    public void testGetXfSwReleaseEntryMO() {
        when(managedObject.getAttribute("xfSwActiveRelease")).thenReturn(2);
        when(managedObject.getFdn()).thenReturn("fdnbase");
        miniLinkActivityUtil.getXfSwReleaseEntryMO(managedObject);
        verify(dpsReader, times(1)).findMoByFdn("fdnbase,xfSwReleaseTable=1,xfSwReleaseEntry=1");
    }

    @Test(expected = ServerInternalException.class)
    public void testGetManagedObject() {
        when(managedObjectUtil.getManagedObject("testname", "xfSwObjects")).thenThrow(new ServerInternalException("testerror"));
        miniLinkActivityUtil.getManagedObject(ACTIVITY_JOB_ID, "xfSwObjects");
    }

    @Test
    public void getSwPkgName() {
        NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("MINI-LINK-Indoor");
        networkElement.setPlatformType(PlatformTypeEnum.MINI_LINK_INDOOR);
        final List<NetworkElement> networkElements = Collections.singletonList(networkElement);
        final List<String> nodeNames = Collections.singletonList("testname");
        final Map<String, String> str = Collections.singletonMap("SWP_NAME", "CXP9010021_1_MINI-LINK_TN_5.4FP_LH_1.6FP_R32B126");
        final List<String> keyList = Collections.singletonList("SWP_NAME");
        when(fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(nodeNames)).thenReturn(networkElements);
        when(jobPropertyUtils.getPropertyValue(keyList, Collections.emptyMap(), "testname", "MINI-LINK-Indoor", "MINI_LINK_INDOOR")).thenReturn(str);
        assertEquals("Check sw package name.", "CXP9010021_1_MINI-LINK_TN_5.4FP_LH_1.6FP_R32B126", miniLinkActivityUtil.getSwPkgName(ACTIVITY_JOB_ID));
    }

    @Test
    public void isRAUPackage() {
        assertTrue("Check SBL.", !miniLinkActivityUtil.isRAUPackage("CXP9010021_1_MINI-LINK_TN_5.4FP_LH_1.6FP_R32B126"));
        assertTrue("Check RAU.", miniLinkActivityUtil.isRAUPackage("CXP9012878_RAU_R4F02"));
    }

    @Test
    public void testUpdateXfSwLmUpgradeTableEntry() {
        final Map<String, Object> xfSwLmUpgradeEntryArguments = new HashMap<>();
        final MiniLinkActivityUtil upgradeUtilSpy = Mockito.spy(miniLinkActivityUtil);
        xfSwLmUpgradeEntryArguments.put("xfSwLmUpgradeRevision", "R4F02");
        xfSwLmUpgradeEntryArguments.put("xfSwLmUpgradeAdminStatus", "upgradeStarted");

        when(dpsReader.findMoByFdn(SW_OBJECT_FDN + ",xfSwLmUpgradeTable=1")).thenReturn(managedObject);
        when(managedObject.getChildren()).thenReturn(Collections.singletonList(entryMO));
        when(entryMO.getAttribute("xfSwLmUpgradeProductNumber")).thenReturn("CXP 901 2878 ");
        when(entryMO.getPoId()).thenReturn(123L);
        when(entryMO.getFdn()).thenReturn(SW_OBJECT_FDN + ",xfSwLmUpgradeTable=1,xfSwLmUpgradeEntry=1");

        assertTrue("Check updateXfSwLmUpgradeTableEntry", upgradeUtilSpy.updateXfSwLmUpgradeTableEntry(ACTIVITY_JOB_ID, SW_OBJECT_FDN, "CXP9012878", xfSwLmUpgradeEntryArguments));
        verify(upgradeUtilSpy).updateManagedObject(entryMO, xfSwLmUpgradeEntryArguments);
    }

    @Test
    public void testIsXfSwBoardTableUpdated() {
        NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("MINI-LINK-Indoor");
        networkElement.setPlatformType(PlatformTypeEnum.MINI_LINK_INDOOR);
        final List<NetworkElement> networkElements = Collections.singletonList(networkElement);
        final List<String> nodeNames = Collections.singletonList("testname");
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final Map<String, String> str = Collections.singletonMap("SWP_NAME", "CXP9012878_RAU_R4F02");
        final List<String> keyList = Collections.singletonList("SWP_NAME");
        when(fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(nodeNames)).thenReturn(networkElements);
        when(jobPropertyUtils.getPropertyValue(keyList, Collections.emptyMap(), "testname", "MINI-LINK-Indoor", "MINI_LINK_INDOOR")).thenReturn(str);
        when(managedObject.getFdn()).thenReturn(SW_OBJECT_FDN);
        when(dpsReader.findMoByFdn(SW_OBJECT_FDN + ",xfSwBoardTable=1")).thenReturn(managedObject);
        when(managedObject.getChildren()).thenReturn(Collections.singletonList(entryMO));
        when(entryMO.getAttribute("xfSwBoardProductNumber")).thenReturn("CXP 901 2878 ");
        when(entryMO.getAttribute("xfSwBoardRevision")).thenReturn("R4F02");
        when(entryMO.getPoId()).thenReturn(123L);
        when(entryMO.getAttribute("xfSwBoardStatus")).thenReturn("active");
        when(managedObjectUtil.getNamespace("testname", "xfSwObjects")).thenReturn("testnamespace");
        when(managedObjectUtil.getManagedObject("testname", "xfSwObjects")).thenReturn(managedObject);

        assertTrue("Check isXfSwBoardTableUpdated", miniLinkActivityUtil.isXfSwBoardTableUpdated(ACTIVITY_JOB_ID, jobLogList));
        assertTrue("jobLogList check", "Revision of RAU has been updated.".equals(jobLogList.get(0).get("message")));

        when(entryMO.getAttribute("xfSwBoardRevision")).thenReturn("R5A04");
        jobLogList.clear();
        assertTrue("Check isXfSwBoardTableUpdated", !miniLinkActivityUtil.isXfSwBoardTableUpdated(ACTIVITY_JOB_ID, jobLogList));
        assertTrue("jobLogList check", "Revision has not changed: R5A04".equals(jobLogList.get(0).get("message")));

        when(entryMO.getAttribute("xfSwBoardProductNumber")).thenReturn("CXP 111 222");
        jobLogList.clear();
        assertTrue("Check isXfSwBoardTableUpdated", !miniLinkActivityUtil.isXfSwBoardTableUpdated(ACTIVITY_JOB_ID, jobLogList));
        assertTrue("jobLogList check", "RAU is missing from xfSwBoardTable.".equals(jobLogList.get(0).get("message")));
    }

    @Test
    public void testGetProductNumberAndRevision() {
        final String[] resultODU = { "CXC1985673", "R405" };
        assertTrue(resultODU[0].equals(miniLinkActivityUtil.getProductNumberAndRevisionRAU("CXC1985673_RAU_R405")[0]));
        assertTrue(resultODU[1].equals(miniLinkActivityUtil.getProductNumberAndRevisionRAU("CXC1985673_RAU_R405")[1]));
        final String[] resultSBL = { "CXP9010021_3", "MINI-LINK_TN_6.0_LH_2.0_R34A121" };
        assertTrue(resultSBL[0].equals(miniLinkActivityUtil.getProductNumberAndRevisionSBL("CXP9010021_3_MINI-LINK_TN_6.0_LH_2.0_R34A121")[0]));
        assertTrue(resultSBL[1].equals(miniLinkActivityUtil.getProductNumberAndRevisionSBL("CXP9010021_3_MINI-LINK_TN_6.0_LH_2.0_R34A121")[1]));
    }

    @Test(expected = ServerInternalException.class)
    public void testGetProductNumberAndRevisionException() {
        miniLinkActivityUtil.getProductNumberAndRevisionRAU("XC1985673_RAU_R405");
    }

    @Test
    public void testSetXfSwGlobalStateWithoutMediation() {
        when(dataPersistenceService.getDataBucket(LIVE_BUCKET, SUPPRESS_MEDIATION, SUPPRESS_CONSTRAINTS)).thenReturn(liveBucket);
        when(jobEnvironment.getNodeName()).thenReturn("NodeName");
        when(managedObjectUtil.getManagedObject("NodeName", MiniLinkConstants.XF_SW_OBJECTS)).thenReturn(managedObject);
        when(managedObject.getNamespace()).thenReturn("NameSpace");
        when(fdnUtils.getMeContextFdnFromNodeName("NodeName")).thenReturn("baseMOFdn");
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery("NameSpace", MiniLinkConstants.XF_SW_OBJECTS, "baseMOFdn")).thenReturn(query);
        when(liveBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(queryExecutor.execute(query)).thenReturn(queryIterator);
        when(queryIterator.next()).thenReturn(managedObject);
        miniLinkActivityUtil.setXfSwGlobalStateWithoutMediation(1l, xfSwGlobalState.sblStarted);
        verify(managedObject).setAttribute(MiniLinkConstants.XF_SW_GLOBAL_STATE, Collections.<String> singletonList(xfSwGlobalState.sblStarted.name()));
    }
}
