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
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.mockito.Mock;

import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.UpgradePackageContext;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.VranSoftwareUpgradeEventSender;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.VranUpgradeJobContextBuilder;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider.NeJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks.TaskBase;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

public class ExecuteProcessorTestBase {

    @Mock
    protected NeJobPropertiesPersistenceProvider neJobPropertiesPersistenceProvider;

    @Mock
    protected JobActivityInfo jobActivityInfo;

    @Mock
    protected UpgradePackageContext upgradePackageContext;

    @Mock
    protected JobEnvironment jobEnvironment;

    @Mock
    protected Map<String, Object> eventAttributes;

    @Mock
    protected ActivityUtils activityUtils;

    @Mock
    protected Map<String, Object> activityJobProperties;

    @Mock
    protected TaskBase taskBase;

    @Mock
    protected VranJobActivityServiceHelper vranJobActivityServiceHelper;

    @Mock
    protected VranSoftwareUpgradeEventSender vranSoftwareUpgradeEventSender;

    @Mock
    protected VranUpgradeJobContextBuilder vranUpgradeJobContextBuilder;

    @Mock
    protected VranJobActivityServiceHelper vranJobActivityService;

    @Mock
    protected JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    static final long activityJobId = 345;

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

}
