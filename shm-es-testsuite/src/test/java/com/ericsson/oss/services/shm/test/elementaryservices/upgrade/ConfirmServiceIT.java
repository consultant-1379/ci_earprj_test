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

import java.util.HashMap;
import java.util.Map;

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
public class ConfirmServiceIT {

    @ArquillianResource
    private Deployer deployer;

    @Inject
    ConfirmServiceTestFactory confirmServiceTestFactory;

    @Inject
    private IElementaryServicesTestBase elementaryDataBean;

    @Inject
    UpgradeTestGenerator upgradeTestGenerator;

    @Inject
    @NamedCache("CppInventorySynchServiceNotificationRegistryCache")
    private Cache<String, NotificationSubject> cache;

    private static final Logger logger = LoggerFactory.getLogger(ConfirmServiceIT.class);

    @Deployment(name = "shm-es-test-1", managed = false, testable = true)
    public static EnterpriseArchive createDeployment() {
        final EnterpriseArchive ear = UpgradeTestDeployment.createTestDeploymentForUpgrade();
        final JavaArchive testJar = createConfirmServArchES();
        ear.addAsModule(testJar);
        return ear;
    }

    //TODO
    // Commented IdentityManagementServiceMock due to issues with Jenkins Release
    private static JavaArchive createConfirmServArchES() {
        final JavaArchive jarArc = ShrinkWrap.create(JavaArchive.class, "ConfirmServIt.jar");
        jarArc.addAsResource("ServiceFrameworkConfiguration.properties", "ServiceFrameworkConfiguration.properties");
        jarArc.addAsResource(IntegrationTestDeploymentFactoryBase.BEANS_XML_FILE, "META-INF/beans.xml");
        jarArc.setManifest(IntegrationTestDeploymentFactoryBase.MANIFEST_MF_FILE);
        jarArc.addClasses(SmrsServiceMock.class, /* IdentityManagementServiceMock.class, */DataPersistenceServiceProxy.class, DataPersistenceServiceProxyBean.class,
                WorkflowInstanceServiceLocalImplMock.class, IElementaryServicesTestBase.class, ElementaryServicesTestBase.class, UpgradeTestGenerator.class, ConfirmServiceIT.class,
                ConfirmServiceTestFactory.class, UpgradeEventHandler.class);
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
    public void testConfirmPreCheckWhenMOIsNull() throws Exception {
        try {
            logger.debug("Test Confirm PreCheck WhenMOIsNull Started.");

            initializeJobData();

            final Long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
            final Activity confirmService = confirmServiceTestFactory.getConfirmService();
            final ActivityStepResult precheckResult = confirmService.precheck(activityJobPoId);
            assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, precheckResult.getActivityResultEnum());

            logger.debug("Test Confirm PreCheck WhenMOIsNull Ended.");
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
        modifiedAttributeMap.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.AWAITING_CONFIRMATION.name());
        elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), modifiedAttributeMap);
    }

    @Test
    @InSequence(4)
    public void testConfirm() throws Exception {
        try {
            logger.debug("Test ConfirmService Started.");

            initializeJobData();

            final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
            final Activity confirmService = confirmServiceTestFactory.getConfirmService();
            final ActivityStepResult precheckResult = confirmService.precheck(activityJobPoId);
            confirmService.execute(activityJobPoId);

            System.out.println("Wait time for 1 second, since execution will take some time.");
            Thread.sleep(1000);

            //Below mentioned notifications are just for testing, it is not required that we will always get notifications in this order only.

            //First Notification
            final Map<String, Object> firstNotification = new HashMap<String, Object>();
            firstNotification.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.UPGRADE_EXECUTING.name());
            elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), firstNotification);

            //Second Notification
            final Map<String, Object> secondNotification = new HashMap<String, Object>();
            secondNotification.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.UPGRADE_COMPLETED.name());
            elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), secondNotification);

            assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, precheckResult.getActivityResultEnum());

            System.out.println("Wait time for 10 seconds since, notification needs to be processed before assertions.");
            Thread.sleep(10000);

            assertEquals(WorkFlowConstants.ACTIVATE_WFMESSAGE, WorkflowInstanceServiceLocalImplMock.MESSAGE);
            assertEquals(ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.mainJobPoId) + "@" + TestConstants.NETWORK_ELEMENT_NAME, WorkflowInstanceServiceLocalImplMock.BUSINESS_KEY);

            logger.debug("Test ConfirmService Ended");
        } catch (final Exception e) {
            logger.error("Exceptions occured while executing this test is: {}", e);
        } finally {
            deleteJobData();
        }
    }

    @Test
    @InSequence(5)
    public void testConfirmWhenMOIsBusy() throws Exception {
        try {
            logger.debug("Test ConfirmService When MO IS busy Started.");

            initializeJobData();

            final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
            final Activity confirmService = confirmServiceTestFactory.getConfirmService();
            final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
            modifiedAttributeMap.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.VERIFICATION_EXECUTING.name());
            modifiedAttributeMap.put(UpgradePackageMoConstants.UP_MO_PROG_HEADER, UpgradeProgressInformation.VERIFICATION_INITIATED.name());
            elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), modifiedAttributeMap);
            final ActivityStepResult precheckResult = confirmService.precheck(activityJobPoId);
            assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, precheckResult.getActivityResultEnum());

            //Modifying back to original state
            modifiedAttributeMap.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.AWAITING_CONFIRMATION.name());
            modifiedAttributeMap.put(UpgradePackageMoConstants.UP_MO_PROG_HEADER, UpgradeProgressInformation.IDLE.name());
            elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), modifiedAttributeMap);

            logger.debug("Test ConfirmService When MO IS busy Ended.");
        } catch (final Exception e) {
            logger.error("Exceptions occured while executing this test is: {}", e);
        } finally {
            deleteJobData();
        }
    }

    @Test
    @InSequence(6)
    public void testConfirmWhenTimedOutForSuccess() throws Exception {
        try {
            logger.debug("Test ConfirmService TimeOut Started For Success Scenario.");

            initializeJobData();

            final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
            final Activity confirmService = confirmServiceTestFactory.getConfirmService();
            final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
            modifiedAttributeMap.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.UPGRADE_COMPLETED.name());
            elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), modifiedAttributeMap);
            final ActivityStepResult timedOutResult = confirmService.handleTimeout(activityJobPoId);
            assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, timedOutResult.getActivityResultEnum());

            //Modifying back to original state
            modifiedAttributeMap.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.AWAITING_CONFIRMATION.name());
            elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), modifiedAttributeMap);

            logger.debug("Test ConfirmService TimeOut Ended For Success Scenario.");
        } catch (final Exception e) {
            logger.error("Exceptions occured while executing this test is: {}", e);
        } finally {
            deleteJobData();
        }
    }

    @Test
    @InSequence(7)
    public void testConfirmWhenTimedOutForFailure() throws Exception {
        try {
            logger.debug("Test ConfirmService TimeOut Started For Failure Scenario.");

            initializeJobData();

            final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
            final Activity confirmService = confirmServiceTestFactory.getConfirmService();
            final ActivityStepResult timedOutResult = confirmService.handleTimeout(activityJobPoId);

            assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, timedOutResult.getActivityResultEnum());

            logger.debug("Test confirmService TimeOut Ended For Failure Scenario.");
        } catch (final Exception e) {
            logger.error("Exceptions occured while executing this test is: {}", e);
        } finally {
            deleteJobData();
        }
    }

    @Test
    @InSequence(8)
    public void testConfirmWhenTimedOutForStillExecuting() throws Exception {
        try {
            logger.debug("Test ConfirmService TimeOut Started For Still Executing Scenario.");

            initializeJobData();

            final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
            final Activity confirmService = confirmServiceTestFactory.getConfirmService();
            final Map<String, Object> modifiedAttributes = new HashMap<String, Object>();
            modifiedAttributes.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.UPGRADE_EXECUTING.name());
            elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), modifiedAttributes);
            final ActivityStepResult timedOutResult = confirmService.handleTimeout(activityJobPoId);

            assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, timedOutResult.getActivityResultEnum());

            //Modifying back to original state
            modifiedAttributes.put(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.AWAITING_CONFIRMATION.name());
            modifiedAttributes.put(UpgradePackageMoConstants.UP_MO_PROG_HEADER, UpgradeProgressInformation.IDLE.name());
            elementaryDataBean.updateAttributes(upgradeTestGenerator.getUpgradePackageMOFdn(), modifiedAttributes);

            logger.debug("Test ConfirmService TimeOut Ended For Still Executing Scenario.");
        } catch (final Exception e) {
            logger.error("Exceptions occured while executing this test is: {}", e);
        } finally {
            deleteJobData();
        }
    }

    @Test
    @InSequence(9)
    public void delete() {
        logger.info("Cleaning up data started");
        elementaryDataBean.deleteMOTestData();
        logger.info("Cleaning up data ended");
    }

    private void initializeJobData() throws Exception {
        upgradeTestGenerator.prepareUpgradeJobData("confirm");
        upgradeTestGenerator.prepareUpgradePacakgePO();
    }

    private void deleteJobData() {
        elementaryDataBean.cleanUpTestPOData();
        elementaryDataBean.cleanUpgradePackageData();
    }

}
