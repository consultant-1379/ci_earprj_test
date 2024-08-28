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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@RunWith(MockitoJUnitRunner.class)
public class ConfirmActivityExecuteProcessorTest extends ExecuteProcessorTestBase {

    @InjectMocks
    private ConfirmActivityExecuteProcessor confirmActivityExecuteProcessor;

    @Before
    public void mockJobEnvironment() {
        jobActivityInfo = new JobActivityInfo(activityJobId, ActivityConstants.CONFIRM, JobTypeEnum.UPGRADE, PlatformTypeEnum.vRAN);
    }

    @Test
    public void testGetActivityToBeTriggered() {
        confirmActivityExecuteProcessor.getActivityToBeTriggered();
    }

    @Test
    public void testExecuteTask() {
        when(vranUpgradeJobContextBuilder.build(activityJobId)).thenReturn(upgradePackageContext);
        confirmActivityExecuteProcessor.executeTask(jobActivityInfo);
    }

    @Test
    public void testAddVnfNeJobProperties() {
        neJobPropertiesPersistenceProvider.persistVnfInformation(activityJobId);
    }

    @Test
    public void testBuildUpgradeJobContext() {
        confirmActivityExecuteProcessor.buildUpgradeJobContext(activityJobId);
    }

    @Test
    public void testBuildEventAttributes() {
        when(taskBase.buildEventAttributes(upgradePackageContext, ActivityConstants.CONFIRM, activityJobId)).thenReturn(eventAttributes);
        confirmActivityExecuteProcessor.buildEventAttributes(upgradePackageContext, ActivityConstants.CONFIRM, activityJobId);
    }

    @Test
    public void testSubscribeToNotifications() {
        confirmActivityExecuteProcessor.subscribeToNotifications(upgradePackageContext, jobActivityInfo);
    }

    @Test
    public void testRequestActivityInitiation() {
        eventAttributes = new HashMap<String, Object>();
        confirmActivityExecuteProcessor.requestActivityInitiation(activityJobId, ActivityConstants.CONFIRM, upgradePackageContext.getVnfmFdn(), eventAttributes);
    }

    @Test
    public void testHandleException() {
        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final Map<String, Object> processVariables = new HashMap<>();
        confirmActivityExecuteProcessor.handleException(jobLogs, processVariables, upgradePackageContext, ActivityConstants.CONFIRM, activityJobId, new Exception());
    }

}
