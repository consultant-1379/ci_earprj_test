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
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.UpgradePackageContext;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskutils.NotificationTaskUtils;
import com.ericsson.oss.services.shm.es.vran.notifications.api.VranSoftwareUpgradeJobResponse;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityUtil;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

@RunWith(MockitoJUnitRunner.class)
public class ProcessNotificationFailureHandlerTest {

    @InjectMocks
    private ProcessNotificationFailureHandler processNotificationFailureHandler;

    @Mock
    private VranJobActivityUtil vranJobActivityUtil;

    @Mock
    private VranJobActivityServiceHelper vranJobActivityServiceHelper;

    @Mock
    private TaskBase taskBase;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;
    @Mock
    private VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse;

    @Mock
    private UpgradePackageContext upgradePackageContext;

    @Mock
    private JobActivityInfo jobActivityInfo;

    @Mock
    private Map<String, Object> activityJobProperties;

    @Mock
    private JobEnvironment jobEnvironment;

    @Mock
    private NotificationTaskUtils notificationTaskUtils;

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

        activityJobPropertiesList.add(activityJobPropertiesMap);
        vranSoftwareUpgradeJobResponse.setActivityJobId(activityJobId);
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
    public void testHandle() {

        activityName = ActivityConstants.PREPARE;
        vranSoftwareUpgradeJobResponse.setActivityName(activityName);
        when(taskBase.buildSubscriptionKey(upgradePackageContext, vranSoftwareUpgradeJobResponse.getActivityJobId())).thenReturn("1234");
        jobActivityInfo = new JobActivityInfo(activityJobId, activityName, JobTypeEnum.UPGRADE, PlatformTypeEnum.vRAN);
        processNotificationFailureHandler.handle(vranSoftwareUpgradeJobResponse, activityName, upgradePackageContext, jobActivityInfo);

    }

}
