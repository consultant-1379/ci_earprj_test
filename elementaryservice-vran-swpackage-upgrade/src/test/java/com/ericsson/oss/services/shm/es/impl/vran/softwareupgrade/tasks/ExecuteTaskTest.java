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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.UpgradePackageContext;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.VranSoftwareUpgradeEventSender;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.VranUpgradeJobContextBuilder;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider.NeJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

@RunWith(MockitoJUnitRunner.class)
public class ExecuteTaskTest {

    @InjectMocks
    private ExecuteTask executeTask;

    @Mock
    private JobUpdateService jobUpdateService;

    @Mock
    private NeJobPropertiesPersistenceProvider jobPropertiesPersistenceProvider;

    @Mock
    private TaskBase taskBase;

    @Mock
    private VranJobActivityServiceHelper vranJobActivityService;

    @Mock
    private VranSoftwareUpgradeEventSender vranSoftwareUpgradeEventSender;

    @Mock
    private VranUpgradeJobContextBuilder vranUpgradeJobContextBuilder;


    @Mock
    private Map<String, Object> activityJobProperties;

    @Mock
    private Map<String, Object> eventAttributes;

    @Mock
    protected JobActivityInfo jobActivityInfo;

    @Mock
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Mock
    protected  ActivityUtils activityUtils;

    @Mock
    protected UpgradePackageContext vranJobInformation;

    @Mock
    protected JobEnvironment jobEnvironment;

    @Mock
    protected VranJobActivityServiceHelper vranJobActivityServiceHelper;

    static final long activityJobId = 345;

    @Before
    public void mockJobEnvironment() {

        final String nodeName = "VRAN";
        final String softwarePackageName = "TESTVRAN";
        final String JobLogMessge = "";
        final int mainJobId = 12121;

        jobActivityInfo = new JobActivityInfo(activityJobId, ActivityConstants.PREPARE, JobTypeEnum.UPGRADE, PlatformTypeEnum.vRAN);
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

        activityJobPropertiesList.add(activityJobPropertiesMap);
        when(vranJobInformation.getSoftwarePackageName()).thenReturn(nodeName);
        when(vranJobInformation.getJobEnvironment()).thenReturn(jobEnvironment);
        when(vranJobInformation.getJobEnvironment().getNodeName()).thenReturn(softwarePackageName);
        when(vranJobInformation.getJobEnvironment().getActivityJobAttributes()).thenReturn(activityJobProperties);
        when(vranJobInformation.getJobEnvironment().getActivityJobAttributes().get("neJobId")).thenReturn(1234L);
        when(vranJobInformation.getJobEnvironment().getNeJobAttributes()).thenReturn(neJobAttributes);
        when(vranJobInformation.getJobEnvironment().getNeJobAttributes().get(ActivityConstants.JOB_PROPERTIES)).thenReturn(neJobProperties);
        when(vranJobInformation.getVnfJobId()).thenReturn(345);
        when(vranJobInformation.getVnfId()).thenReturn("123");
        when(vranJobInformation.getVnfmFdn()).thenReturn("vnfmFdn");
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
        when(activityUtils.additionalInfoForEvent(activityJobId, nodeName, JobLogMessge)).thenReturn("");
        when(activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.UPGRADE)).thenReturn("");
    }

    @Test
    public void testExecuteTask() {
        when(vranUpgradeJobContextBuilder.build(activityJobId)).thenReturn(vranJobInformation);
        executeTask.executeTask(jobActivityInfo);
    }

    @Test
    public void testAddVnfNeJobProperties() {
        jobPropertiesPersistenceProvider.persistVnfInformation(activityJobId);
    }

    @Test
    public void testBuildUpgradeJobContext() {
        executeTask.buildUpgradeJobContext(activityJobId);
    }

    @Test
    public void testGetActivityToBeTriggered() {
        executeTask.getActivityToBeTriggered();
    }

    @Test
    public void testBuildEventAttributes() {
        when(taskBase.buildEventAttributes(vranJobInformation, ActivityConstants.PREPARE, activityJobId)).thenReturn(eventAttributes);
        executeTask.buildEventAttributes(vranJobInformation, ActivityConstants.PREPARE, activityJobId);
    }

    @Test
    public void testPersistJobDetails() {
        executeTask.persistJobDetails(ActivityConstants.PREPARE, activityJobId);
    }

    @Test
    public void testSubscribeToNotifications() {
        executeTask.subscribeToNotifications(vranJobInformation, jobActivityInfo);
    }

    @Test
    public void testRequestActivityInitiation() {
        eventAttributes = new HashMap<String, Object>();
        executeTask.requestActivityInitiation(activityJobId, ActivityConstants.PREPARE, vranJobInformation.getVnfmFdn(), eventAttributes);

    }

    @Test
    public void testHandleException() {
        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final Map<String, Object> processVariables = new HashMap<>();
        executeTask.handleException(jobLogs, processVariables, vranJobInformation, ActivityConstants.PREPARE, activityJobId, new Exception());

    }

}
