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

import java.util.*;

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
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.*;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionUtils;
import com.ericsson.oss.services.shm.filestore.swpackage.remote.api.RemoteSoftwarePackageService;
import com.ericsson.oss.services.shm.jobs.common.constants.WorkFlowConstants;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.test.common.TestConstants;
import com.ericsson.oss.services.shm.test.elementaryservices.*;

/**
 * This test class validates the Verify restore action on node.
 * 
 * @author xeeerrr
 */
@RunWith(Arquillian.class)
@ApplicationScoped
public class VerifyRestoreTestIT {

    @ArquillianResource
    private Deployer deployer;

    @Inject
    RestoreServiceTestFactory VerifyRestoreServiceTestFactory;

    @Inject
    private IElementaryServicesTestBase elementaryDataBean;

    @Inject
    RestoreTestGenerator VerifyRestoreServiceTestGenerator;

    @Inject
    ActivityUtils activityUtils;

    @Inject
    ConfigurationVersionUtils configurationVersionUtils;

    @Inject
    @NamedCache("CppInventorySynchServiceNotificationRegistryCache")
    private Cache<String, NotificationSubject> cache;

    //private String configurationVersionMOFdn;

    @Deployment(name = "shm-es-test", managed = false)
    public static Archive<?> createADeployableSHMESEAR() {
        final WebArchive war = ElementaryServicesTestDeployment.createTestDeploymentForES();
        war.addClass(VerifyRestoreTestIT.class);
        war.addClass(RestoreTestGenerator.class);
        war.addClass(RestoreServiceTestFactory.class);
        war.addClass(RemoteSoftwarePackageService.class);
        return war;

    }

    @Test
    @InSequence(1)
    @OperateOnDeployment("shm-es-test")
    public void deployESTestEAR() throws Exception {
        this.deployer.deploy("shm-es-test");
    }

    @Test
    @InSequence(2)
    public void initializeJobData() throws Exception {
        VerifyRestoreServiceTestGenerator.prepareRestoreJobData("verify");
    }

    @Test
    @InSequence(3)
    public void precheckResultWhenMoIsNull() throws Throwable {
        System.out.println("*** Test PreCheck for Verify Retore WhenMOIsNull Started ***");
        VerifyRestoreServiceTestGenerator.prepareBaseMOTestData();
        final Long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity verifyCVService = VerifyRestoreServiceTestFactory.getVerifyService();
        final ActivityStepResult result = verifyCVService.precheck(activityJobPoId);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, result.getActivityResultEnum());
        System.out.println("*** Test PreCheck WhenMOIsNull Ended ***");

        elementaryDataBean.deleteMOTestData();
        Thread.sleep(10000);
    }

    @Test
    @InSequence(4)
    public void initialize() throws Throwable {
        VerifyRestoreServiceTestGenerator.prepareConfigurationVersionMOTestData();
        final Map<String, Object> changedAttributesMap = new HashMap<String, Object>();

        changedAttributesMap.put("currentDetailedActivity", CVCurrentDetailedActivity.IDLE.name());
        changedAttributesMap.put("currentMainActivity", CVCurrentMainActivity.IDLE.name());

        elementaryDataBean.updateAttributes(VerifyRestoreServiceTestGenerator.getConfigurationVersionMOFdn(), changedAttributesMap);
    }

    @Test
    @InSequence(5)
    public void testPrecheckVerifyCv() throws Exception {
        System.out.println("*** Test Execute testVerifyRestore Started ***");
        final Long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity verifyRestoreService = VerifyRestoreServiceTestFactory.getVerifyService();
        final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
        final Map<String, Object> actionResult = new HashMap<String, Object>();

        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, VerifyRestoreServiceTestGenerator.CV_NAME);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, CVInvokedAction.VERIFY_RESTORE.name());
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, null);
        actionResult.put(ConfigurationVersionMoConstants.CV_PATH_TO_DETAILED_INFORMATION, "");
        actionResult.put(ConfigurationVersionMoConstants.ACTION_ID, -1);
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResult);
        elementaryDataBean.updateAttributes(VerifyRestoreServiceTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);

        final ActivityStepResult precheckResult = verifyRestoreService.precheck(activityJobPoId);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, precheckResult.getActivityResultEnum());
        System.out.println("*** Test Execute testVerifyRestore Ended ***");
    }

    @Test
    @InSequence(6)
    public void testVerifyRestoreProcessNotification() throws Exception {
        System.out.println("*** Test VerifyRestore Started ***");
        WorkflowInstanceServiceLocalImplMock.reset();
        final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity verifyRestoreService = VerifyRestoreServiceTestFactory.getVerifyService();

        final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
        final Map<String, Object> actionResult = new HashMap<String, Object>();

        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, VerifyRestoreServiceTestGenerator.CV_NAME);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, CVInvokedAction.VERIFY_RESTORE.name());
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, null);
        actionResult.put(ConfigurationVersionMoConstants.CV_PATH_TO_DETAILED_INFORMATION, "");
        actionResult.put(ConfigurationVersionMoConstants.ACTION_ID, -1);
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResult);
        elementaryDataBean.updateAttributes(VerifyRestoreServiceTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);

        final ActivityStepResult precheckResult = verifyRestoreService.precheck(activityJobPoId);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, precheckResult.getActivityResultEnum());
        System.out.println("++++++++++++ intialTrigger ++++++++++++++++ Test Execute Started");
        verifyRestoreService.execute(activityJobPoId);
        //As execute is an asynchronous action, wait until its action is invoked, then will proceed for next step
        Thread.sleep(50000);

        final Map<String, Object> fourthNotification = new HashMap<String, Object>();
        fourthNotification.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.IDLE.name());

        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, VerifyRestoreServiceTestGenerator.CV_NAME);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, CVInvokedAction.VERIFY_RESTORE.name());
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, CVActionMainResult.EXECUTED.name());
        actionResult.put(ConfigurationVersionMoConstants.CV_PATH_TO_DETAILED_INFORMATION, "");
        actionResult.put(ConfigurationVersionMoConstants.ACTION_ID, -1);
        fourthNotification.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResult);
        elementaryDataBean.updateAttributes(VerifyRestoreServiceTestGenerator.getConfigurationVersionMOFdn(), fourthNotification);

        Thread.sleep(10000);

        assertEquals(WorkFlowConstants.ACTIVATE_WFMESSAGE, WorkflowInstanceServiceLocalImplMock.MESSAGE);
        assertEquals((ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.mainJobPoId) + "@" + TestConstants.NETWORK_ELEMENT_NAME), WorkflowInstanceServiceLocalImplMock.BUSINESS_KEY);

        System.out.println("*** Test VerifyRestore Ended ***");
    }

    @Test
    @InSequence(7)
    public void testVerifyRestoreTimeoutSuccess() throws Exception {
        System.out.println("*** Test handleTimeout Started ***");
        final Long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity verifyRestoreService = VerifyRestoreServiceTestFactory.getVerifyService();

        final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
        final Map<String, Object> actionResult = new HashMap<String, Object>();

        final List<Map<String, Object>> additionalActionResult = new ArrayList<Map<String, Object>>();

        verifyRestoreService.execute(activityJobPoId);
        //As execute is an asynchronous action, wait until its action is invoked, then will proceed for next step
        Thread.sleep(50000);

        final Map<String, Object> activityJobAttributes = activityUtils.getPoAttributes(activityJobPoId);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, VerifyRestoreServiceTestGenerator.CV_NAME);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, CVInvokedAction.VERIFY_RESTORE.name());
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, CVActionMainResult.EXECUTED.name());
        actionResult.put(ConfigurationVersionMoConstants.CV_PATH_TO_DETAILED_INFORMATION, "");
        actionResult.put(ConfigurationVersionMoConstants.ACTION_ID, configurationVersionUtils.getActionId(activityJobAttributes));
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResult);

        final Map<String, Object> additionalActionResultData = new HashMap<String, Object>();
        additionalActionResultData.put(ConfigurationVersionMoConstants.CV_ADDITIONAL_INFORMATION, "");
        additionalActionResultData.put(ConfigurationVersionMoConstants.CV_INFORMATION, "ACTION_RESTORE_IS_ALLOWED");

        additionalActionResult.add(additionalActionResultData);
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.ADDITIONAL_ACTION_RESULT_DATA, additionalActionResult);

        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.IDLE.name());
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.IDLE.name());
        elementaryDataBean.updateAttributes(VerifyRestoreServiceTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);

        final ActivityStepResult result = verifyRestoreService.handleTimeout(activityJobPoId);
        System.out.println(" Result is {} " + result);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, result.getActivityResultEnum());
        //Modifying back to original state for further test cases
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.IDLE.name());
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.IDLE.name());
        elementaryDataBean.updateAttributes(VerifyRestoreServiceTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);
        System.out.println("*** Test VerifyRestoreService TimeOut Ended ***");

    }

    @Test
    @InSequence(8)
    public void testRestoreServiceWhenTimedOutForFailure() throws Exception {
        System.out.println("*** Test VerifyRestore TimeOut Started ***");
        final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity verifyRestoreService = VerifyRestoreServiceTestFactory.getVerifyService();
        final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
        final Map<String, Object> actionResult = new HashMap<String, Object>();
        final List<Map<String, Object>> additionalActionResult = new ArrayList<Map<String, Object>>();
        verifyRestoreService.execute(activityJobPoId);
        //As execute is an asynchronous action, wait until its action is invoked, then will proceed for next step
        Thread.sleep(5000);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, VerifyRestoreServiceTestGenerator.CV_NAME);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, CVInvokedAction.VERIFY_RESTORE.name());
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, CVActionMainResult.EXECUTION_FAILED.name());
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResult);

        final Map<String, Object> additionalActionResultData = new HashMap<String, Object>();
        additionalActionResultData.put(ConfigurationVersionMoConstants.CV_ADDITIONAL_INFORMATION, "");
        additionalActionResultData.put(ConfigurationVersionMoConstants.CV_INFORMATION, null);

        additionalActionResult.add(additionalActionResultData);
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.ADDITIONAL_ACTION_RESULT_DATA, additionalActionResult);

        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.EXECUTION_FAILED.name());
        elementaryDataBean.updateAttributes(VerifyRestoreServiceTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);
        final ActivityStepResult timedOutResult = verifyRestoreService.handleTimeout(activityJobPoId);

        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, timedOutResult.getActivityResultEnum());

        //Modifying back to original state for further test cases
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.IDLE.name());
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.IDLE.name());
        elementaryDataBean.updateAttributes(VerifyRestoreServiceTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);
        System.out.println("*** Test VerifyRestore TimeOut Ended ***");
    }

    @Test
    @InSequence(9)
    public void testRestoreServiceWhenTimedOutForStillExecuting() throws Exception {
        System.out.println("*** Test VerifyRestore TimeOut Started For Still Executing Scenario***");
        final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity verifyRestoreService = VerifyRestoreServiceTestFactory.getVerifyService();

        final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
        final Map<String, Object> actionResult = new HashMap<String, Object>();
        final List<Map<String, Object>> additionalActionResult = new ArrayList<Map<String, Object>>();
        verifyRestoreService.execute(activityJobPoId);
        //As execute is an asynchronous action, wait until its action is invoked, then will proceed for next step
        Thread.sleep(5000);

        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, VerifyRestoreServiceTestGenerator.CV_NAME);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, CVInvokedAction.VERIFY_RESTORE.name());
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, null);
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResult);

        final Map<String, Object> additionalActionResultData = new HashMap<String, Object>();
        additionalActionResultData.put(ConfigurationVersionMoConstants.CV_ADDITIONAL_INFORMATION, "");
        additionalActionResultData.put(ConfigurationVersionMoConstants.CV_INFORMATION, null);

        additionalActionResult.add(additionalActionResultData);
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.ADDITIONAL_ACTION_RESULT_DATA, additionalActionResult);

        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.VERIFYING_DOWNLOADED_BACKUP_CV_BEFORE_RESTORE.name());
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.VERIFYING_HARDWARE_COMPATIBILITY.name());
        elementaryDataBean.updateAttributes(VerifyRestoreServiceTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);
        final ActivityStepResult timedOutResult = verifyRestoreService.handleTimeout(activityJobPoId);

        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, timedOutResult.getActivityResultEnum());

        //Modifying back to original state for further test cases
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.IDLE.name());
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.IDLE.name());
        elementaryDataBean.updateAttributes(VerifyRestoreServiceTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);

        System.out.println("*** Test VerifyRestore TimeOut Ended For Still Executing Scenario ***");
    }

    @Test
    @InSequence(10)
    public void delete() {
        System.out.println("*** Cleaning up data start *** ");
        elementaryDataBean.deleteMOTestData();
        elementaryDataBean.cleanUpTestPOData();
        System.out.println("*** Cleaning up data end ***");
    }
}
