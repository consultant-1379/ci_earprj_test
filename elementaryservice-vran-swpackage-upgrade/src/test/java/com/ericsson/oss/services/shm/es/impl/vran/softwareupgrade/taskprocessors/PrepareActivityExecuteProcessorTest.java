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
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider.VnfInformationProvider;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@RunWith(MockitoJUnitRunner.class)
public class PrepareActivityExecuteProcessorTest extends ExecuteProcessorTestBase {

    @InjectMocks
    private PrepareActivityExecuteProcessor prepareActivityExecuteProcessor;

    @Mock
    private VnfInformationProvider vnfInformationProvider;

    @Before
    public void mockJobEnvironment() {
        jobActivityInfo = new JobActivityInfo(activityJobId, ActivityConstants.PREPARE, JobTypeEnum.UPGRADE, PlatformTypeEnum.vRAN);
    }

    @Test
    public void testGetActivityToBeTriggered_EmptyVnfJobId() {
        when(upgradePackageContext.getVnfJobId()).thenReturn(-1);
        prepareActivityExecuteProcessor.getActivityToBeTriggered();
    }

    @Test
    public void testGetActivityToBeTriggered_Create() {
        when(upgradePackageContext.getVnfJobId()).thenReturn(-1);
        prepareActivityExecuteProcessor.getActivityToBeTriggered();
    }

    @Test
    public void testGetActivityToBeTriggered_Delete() {
        when(upgradePackageContext.getVnfJobId()).thenReturn(-1);
        prepareActivityExecuteProcessor.getActivityToBeTriggered();
    }

    @Test
    public void testGetActivityToBeTriggered_Prepare() {
        when(upgradePackageContext.getVnfJobId()).thenReturn(1);
        prepareActivityExecuteProcessor.getActivityToBeTriggered();
    }

    @Test
    public void testAddVnfNeJobProperties() {
        neJobPropertiesPersistenceProvider.persistVnfInformation(activityJobId);
    }

    @Test
    public void testBuildUpgradeJobContext() {
        prepareActivityExecuteProcessor.buildUpgradeJobContext(activityJobId);
    }

    @Test
    public void testExecuteTask() {
        when(vranUpgradeJobContextBuilder.build(activityJobId)).thenReturn(upgradePackageContext);
        prepareActivityExecuteProcessor.executeTask(jobActivityInfo);
    }

    @Test
    public void testBuildEventAttributes() {
        when(taskBase.buildEventAttributes(upgradePackageContext, ActivityConstants.VERIFY, activityJobId)).thenReturn(eventAttributes);
        prepareActivityExecuteProcessor.buildEventAttributes(upgradePackageContext, ActivityConstants.VERIFY, activityJobId);
    }

    @Test
    public void testSubscribeToNotifications() {
        prepareActivityExecuteProcessor.subscribeToNotifications(upgradePackageContext, jobActivityInfo);
    }

    @Test
    public void testPersistJobDetails() {
        prepareActivityExecuteProcessor.persistJobDetails(ActivityConstants.PREPARE, activityJobId);

    }

    @Test
    public void testRequestActivityInitiation() {
        eventAttributes = new HashMap<String, Object>();
        prepareActivityExecuteProcessor.requestActivityInitiation(activityJobId, ActivityConstants.PREPARE, upgradePackageContext.getVnfmFdn(), eventAttributes);
    }

    @Test
    public void testHandleException() {
        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final Map<String, Object> processVariables = new HashMap<>();
        prepareActivityExecuteProcessor.handleException(jobLogs, processVariables, upgradePackageContext, ActivityConstants.PREPARE, activityJobId, new Exception());

    }
}
