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
package com.ericsson.oss.services.shm.test.elementaryservices.restore;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import javax.cache.Cache;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.*;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ericsson.oss.itpf.sdk.cache.annotation.NamedCache;
import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.*;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.WorkFlowConstants;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.test.common.TestConstants;
import com.ericsson.oss.services.shm.test.elementaryservices.*;

/**
 * This test class validates the restore action on node.
 * 
 * @author xmanush
 */
@RunWith(Arquillian.class)
@ApplicationScoped
public class RestoreOperationsIT {
    private static final String SHM_ES_TEST = "shm-es-test";
    @ArquillianResource
    private Deployer deployer;

    @Inject
    RestoreServiceTestFactory restoreServiceTestFactory;

    @Inject
    private IElementaryServicesTestBase elementaryDataBean;

    @Inject
    RestoreTestGenerator restoreTestGenerator;

    @Inject
    @NamedCache("CppInventorySynchServiceNotificationRegistryCache")
    private Cache<String, NotificationSubject> cache;

    @Deployment(name = SHM_ES_TEST, managed = false)
    public static Archive<?> createADeployableSHMESWAR() {
        final WebArchive war = ElementaryServicesTestDeployment.createTestDeploymentForES();
        war.addClass(RestoreOperationsIT.class);
        war.addClass(RestoreTestGenerator.class);
        war.addClass(RestoreServiceTestFactory.class);
        return war;
    }

    @Test
    @InSequence(1)
    @OperateOnDeployment(SHM_ES_TEST)
    public void deployESTestEAR() throws Exception {
        this.deployer.deploy(SHM_ES_TEST);
    }

    @Test
    @InSequence(2)
    public void initializeJobData() throws Exception {
        restoreTestGenerator.prepareRestoreJobData("restore");
    }

    @Test
    @InSequence(3)
    public void testPrecheckResultWhenMoIsNull() throws Throwable {
        System.out.println("*** Test PreCheck for Restore WhenMOIsNull Started ***");
        restoreTestGenerator.prepareBaseMOTestData();
        final Long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity restoreService = restoreServiceTestFactory.getRestoreService();
        final ActivityStepResult result = restoreService.precheck(activityJobPoId);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, result.getActivityResultEnum());
        elementaryDataBean.deleteMOTestData();
        System.out.println("*** Test PreCheck WhenMOIsNull Ended ***");
    }

    @Test
    @InSequence(4)
    public void initializeCvMoData() throws Throwable {
        restoreTestGenerator.prepareConfigurationVersionMOTestData();
        final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.IDLE.name());
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.IDLE.name());
        elementaryDataBean.updateAttributes(restoreTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);
    }

    @Test
    @InSequence(5)
    public void testRestorePreCheckAndExecute() throws Exception {
        System.out.println("*** Test PreCheck Started ***");
        final Long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity restoreService = restoreServiceTestFactory.getRestoreService();
        final ActivityStepResult result = restoreService.precheck(activityJobPoId);
        System.out.println(" Result for pre-check is {} " + result);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, result.getActivityResultEnum());
        System.out.println("++++++++++++ intialTrigger ++++++++++++++++ Test Execute Started");
        restoreService.execute(activityJobPoId);
        //As execute is an asynchronous action, wait until its action is invoked, then will proceed for next step
        Thread.sleep(5000);
        System.out.println("*** Test PreCheck Ended ***");
    }

    @Test
    @InSequence(6)
    public void testRestoreServiceWhileProcessingNotification() throws Exception {
        System.out.println("*** Test RestoreService Started ***");
        WorkflowInstanceServiceLocalImplMock.reset();
        final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity restoreService = restoreServiceTestFactory.getRestoreService();
        final ActivityStepResult precheckResult = restoreService.precheck(activityJobPoId);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, precheckResult.getActivityResultEnum());
        System.out.println("++++++++++++ intialTrigger ++++++++++++++++ Test Execute Started");
        restoreService.execute(activityJobPoId);
        //As execute is an asynchronous action, wait until its action is invoked, then will proceed for next step
        Thread.sleep(5000);

        final Map<String, Object> firstNotification = new HashMap<String, Object>();
        firstNotification.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.VERIFYING_DOWNLOADED_BACKUP_CV_BEFORE_RESTORE.name());
        firstNotification.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.VERIFYING_UPGRADE_PACKAGE_PRESENT.name());
        elementaryDataBean.updateAttributes(restoreTestGenerator.getConfigurationVersionMOFdn(), firstNotification);

        final Map<String, Object> secondNotification = new HashMap<String, Object>();
        secondNotification.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.VERIFYING_CORE_CENTRAL_MP.name());
        elementaryDataBean.updateAttributes(restoreTestGenerator.getConfigurationVersionMOFdn(), secondNotification);

        final Map<String, Object> thirdNotification = new HashMap<String, Object>();
        thirdNotification.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.VERIFYING_NETWORK_CONFIGURATION_FOR_IP_ATM.name());
        elementaryDataBean.updateAttributes(restoreTestGenerator.getConfigurationVersionMOFdn(), thirdNotification);

        final Map<String, Object> fourthNotification = new HashMap<String, Object>();
        fourthNotification.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.RESTORING_DOWNLOADED_BACKUP_CV.name());
        fourthNotification.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.AWAITING_RESTORE_CONFIRMATION.name());
        final Map<String, Object> actionResult = new HashMap<String, Object>();
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, restoreTestGenerator.CV_NAME);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, CVInvokedAction.RESTORE.name());
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, CVActionMainResult.EXECUTED.name());
        fourthNotification.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResult);
        elementaryDataBean.updateAttributes(restoreTestGenerator.getConfigurationVersionMOFdn(), fourthNotification);

        Thread.sleep(10000);
        // assertEquals(JobResult.SUCCESS.toString(), elementaryDataBean.getJobResultForActivity(activityJobPoId));
        assertEquals(WorkFlowConstants.ACTIVATE_WFMESSAGE, WorkflowInstanceServiceLocalImplMock.MESSAGE);
        assertEquals((ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.mainJobPoId) + "@" + TestConstants.NETWORK_ELEMENT_NAME), WorkflowInstanceServiceLocalImplMock.BUSINESS_KEY);

        System.out.println("*** Test RestoreService Ended ***");
    }

    @Test
    @InSequence(7)
    public void testrestoreServiceWhenMOIsBusy() throws Exception {
        System.out.println("*** Test RestoreService When MO IS busy Started ***");
        final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity restoreService = restoreServiceTestFactory.getRestoreService();
        final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.VERIFYING_DOWNLOADED_BACKUP_CV_BEFORE_RESTORE.name());
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.AWAITING_RESTORE_CONFIRMATION.name());
        elementaryDataBean.updateAttributes(restoreTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);
        final ActivityStepResult precheckResult = restoreService.precheck(activityJobPoId);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, precheckResult.getActivityResultEnum());

        //Modifying back to original state for further test cases
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.IDLE.name());
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.IDLE.name());
        elementaryDataBean.updateAttributes(restoreTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);

        System.out.println("*** Test RestoreService When MO IS busy Ended ***");
    }

    @Test
    @InSequence(8)
    public void testRestoreServiceHandleTimeout() throws Exception {
        System.out.println("*** Test handleTimeout Started ***");
        final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.RESTORING_DOWNLOADED_BACKUP_CV.name());
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.AWAITING_RESTORE_CONFIRMATION.name());
        elementaryDataBean.updateAttributes(restoreTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);
        final Long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity restoreService = restoreServiceTestFactory.getRestoreService();
        final ActivityStepResult result = restoreService.handleTimeout(activityJobPoId);
        System.out.println(" Result is {} " + result);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, result.getActivityResultEnum());
        System.out.println("*** Test handleTimeout Ended ***");
    }

    @Test
    @InSequence(9)
    public void testRestoreServiceWhenTimedOutForSuccess() throws Exception {
        System.out.println("*** Test RestoreService TimeOut Started For Success Scenario ***");
        final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity restoreService = restoreServiceTestFactory.getRestoreService();
        final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.IDLE.name());
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.IDLE.name());
        elementaryDataBean.updateAttributes(restoreTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);
        final Map<String, Object> actionResult = new HashMap<String, Object>();
        final ActivityStepResult precheckResult = restoreService.precheck(activityJobPoId);
        restoreService.execute(activityJobPoId);
        //As execute is an asynchronous action, wait until its action is invoked, then will proceed for next step
        Thread.sleep(5000);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, restoreTestGenerator.CV_NAME);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, CVInvokedAction.RESTORE.name());
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, CVActionMainResult.EXECUTED.name());
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResult);
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.RESTORING_DOWNLOADED_BACKUP_CV.name());
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.AWAITING_RESTORE_CONFIRMATION.name());
        elementaryDataBean.updateAttributes(restoreTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);
        final ActivityStepResult timedOutResult = restoreService.handleTimeout(activityJobPoId);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, precheckResult.getActivityResultEnum());
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, timedOutResult.getActivityResultEnum());
        System.out.println("*** Test RestoreService TimeOut Ended For Success Scenario ***");
    }

    @Test
    @InSequence(10)
    public void testRestoreServiceWhenTimedOutForFailure() throws Exception {
        System.out.println("*** Test RestoreService TimeOut Started ***");
        final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity restoreService = restoreServiceTestFactory.getRestoreService();
        final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.IDLE.name());
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.IDLE.name());
        elementaryDataBean.updateAttributes(restoreTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);
        final Map<String, Object> actionResult = new HashMap<String, Object>();
        final ActivityStepResult precheckResult = restoreService.precheck(activityJobPoId);
        restoreService.execute(activityJobPoId);
        //As execute is an asynchronous action, wait until its action is invoked, then will proceed for next step
        Thread.sleep(5000);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, restoreTestGenerator.CV_NAME);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, CVInvokedAction.RESTORE.name());
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, CVActionMainResult.EXECUTION_FAILED.name());
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResult);
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.EXECUTION_FAILED.name());
        elementaryDataBean.updateAttributes(restoreTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);
        final ActivityStepResult timedOutResult = restoreService.handleTimeout(activityJobPoId);

        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, precheckResult.getActivityResultEnum());
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, timedOutResult.getActivityResultEnum());

        //Modifying back to original state for further test cases
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.IDLE.name());
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.IDLE.name());
        elementaryDataBean.updateAttributes(restoreTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);
        System.out.println("*** Test RestoreService TimeOut Ended ***");
    }

    @Test
    @InSequence(11)
    public void testRestoreServiceWhenTimedOutForStillExecuting() throws Exception {
        System.out.println("*** Test RestoreService TimeOut Started For Still Executing Scenario***");
        final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity restoreService = restoreServiceTestFactory.getRestoreService();
        final Map<String, Object> modifiedAttributes = new HashMap<String, Object>();
        final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
        modifiedAttributes.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.VERIFYING_DOWNLOADED_BACKUP_CV_BEFORE_RESTORE.name());
        modifiedAttributes.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.VERIFYING_UPGRADE_PACKAGE_PRESENT.name());
        elementaryDataBean.updateAttributes(restoreTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributes);
        final ActivityStepResult timedOutResult = restoreService.handleTimeout(activityJobPoId);

        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, timedOutResult.getActivityResultEnum());

        //Modifying back to original state for further test cases
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.IDLE.name());
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.IDLE.name());
        elementaryDataBean.updateAttributes(restoreTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);

        System.out.println("*** Test RestoreService TimeOut Ended For Still Executing Scenario ***");
    }

    @Test
    @InSequence(12)
    public void delete() {
        System.out.println("*** Cleaning up data start *** ");
        elementaryDataBean.deleteMOTestData();
        elementaryDataBean.cleanUpTestPOData();
        System.out.println("*** Cleaning up data end ***");
    }

}
