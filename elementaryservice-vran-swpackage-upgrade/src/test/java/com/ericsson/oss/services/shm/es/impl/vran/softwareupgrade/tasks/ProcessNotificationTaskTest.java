/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.UpgradePackageContext;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider.JobLogsPersistenceProvider;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.task.common.ActivityStatusManager;
import com.ericsson.oss.services.shm.es.vran.notifications.api.VranSoftwareUpgradeJobResponse;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobEvents;
import com.ericsson.oss.services.shm.vran.constants.VranUprgradeConstants;

@RunWith(MockitoJUnitRunner.class)
@Ignore
public class ProcessNotificationTaskTest {

    @InjectMocks
    private ProcessNotificationTask processNotificationTask;

    @Mock
    private ProcessNotificationTask nextProcessor;
    @Mock
    private UpgradePackageContext upgradePackageContext;

    @Mock
    private JobActivityInfo jobActivityInfo;

    @Mock
    protected JobEnvironment jobEnvironment;

    @Mock
    protected Map<String, Object> activityJobProperties;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse;

    @Mock
    private JobLogsPersistenceProvider jobLogsPersistenceProvider;

    @Mock
    private TaskBase taskBase;

    @Mock
    private ActivityStatusManager activityStatusManager;

    static final long activityJobId = 345;

    private String activityName;

    @Before
    public void mockJobEnvironment() {

        final String nodeName = "VRAN";
        final String softwarePackageName = "TESTVRAN";
        final String JobLogMessge = "";
        final int mainJobId = 12121;

        final List<Map<String, Object>> activityJobPropertiesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityJobPropertiesMap = new HashMap<String, Object>();
        activityJobPropertiesMap.put(ActivityConstants.JOB_PROPERTIES, 1233445);
        activityJobPropertiesMap.put(ActivityConstants.JOB_PROP_KEY, "vnfJobId");
        activityJobPropertiesMap.put(ActivityConstants.JOB_PROP_VALUE, "12");
        activityJobPropertiesMap.put("neJobId", 1234L);

        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobPropertieslist = new ArrayList<Map<String, Object>>();
        final Map<String, Object> neJobProperties = new HashMap<String, Object>();
        neJobPropertieslist.add(neJobProperties);
        jobActivityInfo = new JobActivityInfo(activityJobId, ActivityConstants.PREPARE, JobTypeEnum.UPGRADE, PlatformTypeEnum.vRAN);
        activityJobPropertiesList.add(activityJobPropertiesMap);
        vranSoftwareUpgradeJobResponse.setActivityJobId(activityJobId);
        vranSoftwareUpgradeJobResponse.setNetworkElementName(nodeName);
        when(upgradePackageContext.getSoftwarePackageName()).thenReturn(nodeName);
        when(upgradePackageContext.getJobEnvironment()).thenReturn(jobEnvironment);
        when(upgradePackageContext.getJobEnvironment().getNodeName()).thenReturn(softwarePackageName);
        when(upgradePackageContext.getJobEnvironment().getActivityJobAttributes()).thenReturn(activityJobProperties);
        when(upgradePackageContext.getJobEnvironment().getActivityJobAttributes().get("neJobId")).thenReturn(1234L);
        when(upgradePackageContext.getJobEnvironment().getNeJobAttributes()).thenReturn(neJobAttributes);
        when(upgradePackageContext.getJobEnvironment().getNeJobAttributes().get(ActivityConstants.JOB_PROPERTIES)).thenReturn(neJobProperties);
        when(upgradePackageContext.getVnfJobId()).thenReturn(345);
        when(upgradePackageContext.getVnfId()).thenReturn("123");
        when(upgradePackageContext.getVnfmFdn()).thenReturn("vnfmFdn");
        when(activityUtils.additionalInfoForEvent(activityJobId, nodeName, JobLogMessge)).thenReturn("");
        when(activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.UPGRADE)).thenReturn("");

    }

    @Test
    public void testPerform() {
        fail("Not yet implemented");
    }

    @Test
    public void testProcessNotification() {
        fail("Not yet implemented");
    }

    @Test
    public void testProcessActivityActionNotification() {
        fail("Not yet implemented");
    }

    @Test
    public void testProcessActivityProgressNotification() {
        vranSoftwareUpgradeJobResponse.setState(ActivityConstants.PREPARE);
        processNotificationTask.processActivityProgressNotification(vranSoftwareUpgradeJobResponse, upgradePackageContext, jobActivityInfo, VranUprgradeConstants.PREPARE_OPERATION);
    }

    @Test
    public void testProceedWithNextSteps() {

        processNotificationTask.proceedWithNextSteps(vranSoftwareUpgradeJobResponse, upgradePackageContext, jobActivityInfo, upgradePackageContext.getNodeName(),
                VranUprgradeConstants.PREPARE_OPERATION);
    }

    @Test
    public void testRecordActivitySucess() {
        processNotificationTask.recordActivitySucess(vranSoftwareUpgradeJobResponse, upgradePackageContext, upgradePackageContext.getNodeName(), VranUprgradeConstants.PREPARE_OPERATION);
    }

    @Test
    public void testTrackActivityStatusOnVnfm() {
        processNotificationTask.trackActivityStatusOnVnfm(vranSoftwareUpgradeJobResponse, upgradePackageContext);
    }

    @Test
    public void testRecordNotification() {
        processNotificationTask.recordNotification(vranSoftwareUpgradeJobResponse, activityJobId, upgradePackageContext, VranJobEvents.PREPARE_PROCESS_NOTIFICATION);
    }

    @Test
    public void testGetNextProcessor() {
        processNotificationTask.getNextProcessor();
    }

    @Test
    public void testSetNextProcessor() {
        processNotificationTask.setNextProcessor(nextProcessor);
    }

    @Test
    public void testGetActivityName() {
        processNotificationTask.getActivityName();
    }

}
