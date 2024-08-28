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
package com.ericsson.oss.services.shm.test.elementaryservices.upgrade;

import static org.junit.Assert.assertEquals;

import java.util.*;

import javax.cache.Cache;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.*;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.cache.annotation.NamedCache;
import com.ericsson.oss.services.shm.cpp.inventory.service.upgrade.UpgradeEventHandler;
import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.*;
import com.ericsson.oss.services.shm.jobs.common.constants.WorkFlowConstants;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.test.common.*;
import com.ericsson.oss.services.shm.test.elementaryservices.*;

/**
 * This test class facilitates the creation of configuration version of CPP based node by invoking the ConfigurationVersion MO action that initializes the create activity.
 *
 * @author xcharoh
 */
@RunWith(Arquillian.class)
@ApplicationScoped
public class VerifyServiceIT {

    @ArquillianResource
    private Deployer deployer;

    @Inject
    VerifyServiceTestFactory verifyServiceTestFactory;

    @Inject
    private IElementaryServicesTestBase elementaryDataBean;

    @Inject
    UpgradeTestGenerator upgradeTestGenerator;

    @Inject
    @NamedCache("CppInventorySynchServiceNotificationRegistryCache")
    private Cache<String, NotificationSubject> cache;

    private static final Logger logger = LoggerFactory.getLogger(VerifyServiceIT.class);

    @Deployment(name = "shm-es-test-1", managed = false, testable = true)
    public static EnterpriseArchive createDeployment() {
        final EnterpriseArchive ear = UpgradeTestDeployment.createTestDeploymentForUpgrade();
        final JavaArchive testJar = createVerifyServArchES();
        ear.addAsModule(testJar);
        return ear;
    }

    //TODO
    // Commented IdentityManagementServiceMock due to issues with Jenkins Release
    private static JavaArchive createVerifyServArchES() {
        final JavaArchive jarArc = ShrinkWrap.create(JavaArchive.class, "VerifyServIt.jar");
        jarArc.addAsResource("ServiceFrameworkConfiguration.properties", "ServiceFrameworkConfiguration.properties");
        jarArc.addAsResource(IntegrationTestDeploymentFactoryBase.BEANS_XML_FILE, "META-INF/beans.xml");
        jarArc.setManifest(IntegrationTestDeploymentFactoryBase.MANIFEST_MF_FILE);
        jarArc.addClasses(SmrsServiceMock.class, /* IdentityManagementServiceMock.class, */DataPersistenceServiceProxy.class, DataPersistenceServiceProxyBean.class,
                WorkflowInstanceServiceLocalImplMock.class, IElementaryServicesTestBase.class, ElementaryServicesTestBase.class, UpgradeTestGenerator.class, VerifyServiceIT.class,
                VerifyServiceTestFactory.class, UpgradeEventHandler.class);
        return jarArc;
    }

    @Test
    @InSequence(1)
    @OperateOnDeployment("shm-es-test-1")
    public void deploySHMTestEAR() throws Exception {
        this.deployer.deploy("shm-es-test-1");
    }

    @Test
    @InSequence(2)
    public void testVerifyPreCheckWhenMOIsNull() throws Exception {
        try {
            logger.debug("Test Verify PreCheck WhenMOIsNull Started.");

            initializeJobData();

            final Long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
            final Activity verifyService = verifyServiceTestFactory.getVerifyService();
            final ActivityStepResult precheckResult = verifyService.precheck(activityJobPoId);
            assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, precheckResult.getActivityResultEnum());

            logger.debug("Test Verify PreCheck WhenMOIsNull Ended.");
        } catch (final Exception e) {
            logger.error("Exceptions occured while executing this test is: {}", e);
        } finally {
            deleteJobData();
        }
    }

    @Test
    @InSequence(3)
    public void initialize() throws Throwable {
        upgradeTestGenerator.prepareUpgradePackageMOTestData();
        final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
        modifiedAttributeMap.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.INSTALL_COMPLETED.name());
        elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), modifiedAttributeMap);
    }

    @Test
    @InSequence(4)
    public void testVerify() throws Exception {
        try {
            logger.debug("Test VerifyService Started.");

            initializeJobData();

            final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
            final Activity verifyService = verifyServiceTestFactory.getVerifyService();
            final ActivityStepResult precheckResult = verifyService.precheck(activityJobPoId);
            verifyService.execute(activityJobPoId);

            System.out.println("Wait time for 1 second, since execution will take some time.");
            Thread.sleep(1000);

            //First Notification
            final Map<String, Object> firstNotification = new HashMap<String, Object>();
            firstNotification.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.VERIFICATION_EXECUTING.name());
            firstNotification.put(UpgradePackageMoConstants.UP_MO_PROG_HEADER, UpgradeProgressInformation.VERIFICATION_INITIATED.name());
            elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), firstNotification);

            //Second Notification
            final Map<String, Object> secondNotification = new HashMap<String, Object>();
            secondNotification.put(UpgradePackageMoConstants.UP_MO_PROG_COUNT, 2L);
            elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), secondNotification);

            //Third Notification
            final Map<String, Object> thirdNotification = new HashMap<String, Object>();
            thirdNotification.put(UpgradePackageMoConstants.UP_MO_PROG_HEADER, UpgradeProgressInformation.VERIFY_UPGR_FROM_VER.name());
            elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), thirdNotification);

            //Fourth Notification
            final Map<String, Object> fourthNotification = new HashMap<String, Object>();
            fourthNotification.put(UpgradePackageMoConstants.UP_MO_PROG_HEADER, UpgradeProgressInformation.VERIFY_CREATE_REQ_CVS.name());
            elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), fourthNotification);

            //Fifth Notification
            final Map<String, Object> fifthNotification = new HashMap<String, Object>();
            fifthNotification.put(UpgradePackageMoConstants.UP_MO_PROG_COUNT, 3L);
            elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), fifthNotification);

            //Sixth Notification
            final Map<String, Object> sixthNotification = new HashMap<String, Object>();
            sixthNotification.put(UpgradePackageMoConstants.UP_MO_PROG_HEADER, UpgradeProgressInformation.VERIFY_PIUS_SUPPORTED.name());
            elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), sixthNotification);

            //Seventh Notification
            final Map<String, Object> seventhNotification = new HashMap<String, Object>();
            seventhNotification.put(UpgradePackageMoConstants.UP_MO_PROG_HEADER, UpgradeProgressInformation.VERIFY_CHECKSUM_FOR_LM.name());
            elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), seventhNotification);

            //Eighth Notification
            final Map<String, Object> eighthNotification = new HashMap<String, Object>();
            eighthNotification.put(UpgradePackageMoConstants.UP_MO_PROG_HEADER, UpgradeProgressInformation.VERIFY_PIUS_NOT_FAULTY.name());
            elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), eighthNotification);

            //Ninth Notification
            final Map<String, Object> ninethNotification = new HashMap<String, Object>();
            ninethNotification.put(UpgradePackageMoConstants.UP_MO_PROG_COUNT, 4L);
            elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), ninethNotification);

            //Tenth Notification
            final Map<String, Object> tenthNotification = new HashMap<String, Object>();
            tenthNotification.put(UpgradePackageMoConstants.UP_MO_PROG_HEADER, UpgradeProgressInformation.VERIFICATION_FINISHED.name());
            elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), tenthNotification);

            //Eleventh Notification
            final Map<String, Object> eleventhNotification = new HashMap<String, Object>();
            eleventhNotification.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.INSTALL_COMPLETED.name());
            eleventhNotification.put(UpgradePackageMoConstants.UP_MO_PROG_HEADER, UpgradeProgressInformation.IDLE.name());
            final Map<String, Object> actionResult = new HashMap<String, Object>();
            actionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ACTION_ID, 123L);
            actionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ADDITIONAL_INFO, "Some Additional Info");
            actionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_INFO, ActionResultInformation.EXECUTED.name());
            actionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_TIME, "Some Time in String");
            actionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_TYPE_OF_INVOKED_ACTION, "VERIFY_UPGRADE");
            List<Map<String, Object>> actionResultList = new ArrayList<Map<String, Object>>();
            actionResultList = upgradeTestGenerator.getActionResultList();
            actionResultList.add(actionResult);
            eleventhNotification.put(UpgradePackageMoConstants.UP_ACTION_RESULT, actionResultList);
            elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), eleventhNotification);

            assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, precheckResult.getActivityResultEnum());

            System.out.println("Wait time for 10 seconds since, notification needs to be processed before assertions.");
            Thread.sleep(10000);

            assertEquals(WorkFlowConstants.ACTIVATE_WFMESSAGE, WorkflowInstanceServiceLocalImplMock.MESSAGE);
            assertEquals(ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.mainJobPoId) + "@" + TestConstants.NETWORK_ELEMENT_NAME, WorkflowInstanceServiceLocalImplMock.BUSINESS_KEY);

            logger.debug("Test VerifyService Ended.");
        } catch (final Exception e) {
            logger.error("Exceptions occured while executing this test is: {}", e);
        } finally {
            deleteJobData();
        }
    }

    @Test
    @InSequence(5)
    public void testVerifyWhenMOIsBusy() throws Exception {
        try {
            logger.debug("Test VerifyService When MO IS busy Started.");

            initializeJobData();

            final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
            final Activity verifyService = verifyServiceTestFactory.getVerifyService();
            final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
            modifiedAttributeMap.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.VERIFICATION_EXECUTING.name());
            modifiedAttributeMap.put(UpgradePackageMoConstants.UP_MO_PROG_HEADER, UpgradeProgressInformation.VERIFICATION_INITIATED.name());
            elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), modifiedAttributeMap);
            final ActivityStepResult precheckResult = verifyService.precheck(activityJobPoId);
            assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, precheckResult.getActivityResultEnum());

            //Modifying back to original state
            modifiedAttributeMap.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.INSTALL_COMPLETED.name());
            modifiedAttributeMap.put(UpgradePackageMoConstants.UP_MO_PROG_HEADER, UpgradeProgressInformation.IDLE.name());
            elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), modifiedAttributeMap);

            logger.debug("Test VerifyService When MO IS busy Ended.");
        } catch (final Exception e) {
            logger.error("Exceptions occured while executing this test is: {}", e);
        } finally {
            deleteJobData();
        }
    }

    @Test
    @InSequence(6)
    public void testVerifyWhenTimedOutForSuccess() throws Exception {
        try {
            logger.debug("Test VerifyService TimeOut Started For Success Scenario.");

            initializeJobData();

            final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
            final Activity verifyService = verifyServiceTestFactory.getVerifyService();
            final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
            final Map<String, Object> actionResult = new HashMap<String, Object>();
            actionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ACTION_ID, -1L);
            actionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ADDITIONAL_INFO, "Some Additional Info");
            actionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_INFO, ActionResultInformation.EXECUTED.name());
            actionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_TIME, "Some Time in String");
            actionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_TYPE_OF_INVOKED_ACTION, "VERIFY_UPGRADE");
            List<Map<String, Object>> actionResultList = new ArrayList<Map<String, Object>>();
            actionResultList = upgradeTestGenerator.getActionResultList();
            actionResultList.add(actionResult);
            modifiedAttributeMap.put(UpgradePackageMoConstants.UP_ACTION_RESULT, actionResultList);
            elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), modifiedAttributeMap);
            final ActivityStepResult timedOutResult = verifyService.handleTimeout(activityJobPoId);
            assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, timedOutResult.getActivityResultEnum());

            logger.debug("Test VerifyService TimeOut Ended For Success Scenario.");
        } catch (final Exception e) {
            logger.error("Exceptions occured while executing this test is: {}", e);
        } finally {
            deleteJobData();
        }
    }

    @Test
    @InSequence(7)
    public void testVerifyWhenTimedOutForFailure() throws Exception {
        try {
            logger.debug("Test VerifyService TimeOut Started For Failure Scenario.");

            initializeJobData();

            final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
            final Activity verifyService = verifyServiceTestFactory.getVerifyService();
            final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
            final Map<String, Object> actionResult = new HashMap<String, Object>();
            actionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ACTION_ID, -1L);
            actionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ADDITIONAL_INFO, "Some Additional Info");
            actionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_INFO, ActionResultInformation.EXECUTION_FAILED.name());
            actionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_TIME, "Some Time in String");
            actionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_TYPE_OF_INVOKED_ACTION, "VERIFY_UPGRADE");
            List<Map<String, Object>> actionResultList = new ArrayList<Map<String, Object>>();
            actionResultList = upgradeTestGenerator.getActionResultList();
            actionResultList.add(actionResult);
            modifiedAttributeMap.put(UpgradePackageMoConstants.UP_ACTION_RESULT, actionResultList);
            elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), modifiedAttributeMap);
            final ActivityStepResult timedOutResult = verifyService.handleTimeout(activityJobPoId);

            assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, timedOutResult.getActivityResultEnum());

            logger.debug("Test VerifyService TimeOut Ended For Failure Scenario.");
        } catch (final Exception e) {
            logger.error("Exceptions occured while executing this test is: {}", e);
        } finally {
            deleteJobData();
        }
    }

    @Test
    @InSequence(8)
    public void testVerifyWhenTimedOutForStillExecuting() throws Exception {
        try {
            logger.debug("Test VerifyService TimeOut Started For Still Executing Scenario.");

            initializeJobData();

            final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
            final Activity verifyService = verifyServiceTestFactory.getVerifyService();
            final Map<String, Object> modifiedAttributes = new HashMap<String, Object>();
            modifiedAttributes.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.VERIFICATION_EXECUTING.name());
            modifiedAttributes.put(UpgradePackageMoConstants.UP_MO_PROG_HEADER, UpgradeProgressInformation.VERIFICATION_INITIATED.name());
            elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), modifiedAttributes);
            final ActivityStepResult timedOutResult = verifyService.handleTimeout(activityJobPoId);

            assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, timedOutResult.getActivityResultEnum());

            //Modifying back to original state
            modifiedAttributes.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.INSTALL_COMPLETED.name());
            modifiedAttributes.put(UpgradePackageMoConstants.UP_MO_PROG_HEADER, UpgradeProgressInformation.IDLE.name());
            elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), modifiedAttributes);

            logger.debug("Test VerifyService TimeOut Ended For Still Executing Scenario.");
        } catch (final Exception e) {
            logger.error("Exceptions occured while executing this test is: {}", e);
        } finally {
            deleteJobData();
        }
    }

    @Test
    @InSequence(9)
    public void delete() {
        logger.debug("Cleaning up data started.");
        elementaryDataBean.deleteMOTestData();
        elementaryDataBean.cleanUpgradePackageData();
        logger.debug("Cleaning up data ended.");
    }

    private void initializeJobData() throws Exception {
        upgradeTestGenerator.prepareUpgradeJobData("verify");
        upgradeTestGenerator.prepareUpgradePacakgePO();
    }

    private void deleteJobData() {
        elementaryDataBean.cleanUpTestPOData();
    }

}
