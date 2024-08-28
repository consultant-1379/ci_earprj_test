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
package com.ericsson.oss.services.shm.test.elementaryservices.backup;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.cache.annotation.NamedCache;
import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.*;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.test.elementaryservices.*;
import com.ericsson.oss.services.shm.test.notifications.MockNotificationHandler;

/**
 * This test class facilitates the upload of configuration version of CPP based
 * node by invoking the ConfigurationVersion MO action that initializes the
 * upload activity.
 * 
 * @author xcharoh
 */

@RunWith(Arquillian.class)
@ApplicationScoped
public class UploadCvServiceIT {

    @ArquillianResource
    private Deployer deployer;

    @Inject
    UploadCvServiceTestFactory uploadCvServiceTestFactory;

    @Inject
    private IElementaryServicesTestBase elementaryDataBean;

    @Inject
    BackupTestGenerator backupTestGenerator;

    @Inject
    @NamedCache("CppInventorySynchServiceNotificationRegistryCache")
    private Cache<String, NotificationSubject> cache;

    private static final Logger logger = LoggerFactory.getLogger(UploadCvServiceIT.class);

    @Deployment(name = "shm-es-test", managed = false)
    public static Archive<?> createADeployableSHMEAR() {
        final WebArchive war = ElementaryServicesTestDeployment.createTestDeploymentForES();
        war.addClass(BackupTestGenerator.class);
        war.addClass(MockNotificationHandler.class);
        war.addClass(UploadCvServiceTestFactory.class);
        war.addClass(UploadCvServiceIT.class);
        return war;
    }

    @Test
    @InSequence(1)
    @OperateOnDeployment("shm-es-test")
    public void deploySHMTestEAR() throws Exception {
        this.deployer.deploy("shm-es-test");
    }

    @Test
    @InSequence(2)
    public void testUploadCvPreCheckWhenMOIsNull() throws Throwable {
        try {
            logger.debug("Test UploadCv PreCheck WhenMOIsNull Started.");

            initializeJobData();
            elementaryDataBean.prepareBaseMOTestData();

            final Long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
            final Activity uploadCv = uploadCvServiceTestFactory.getUploadCvService();
            final ActivityStepResult precheckResult = uploadCv.precheck(activityJobPoId);
            assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, precheckResult.getActivityResultEnum());

            logger.debug("Test UploadCv PreCheck WhenMOIsNull Ended.");
        } catch (Exception e) {
            logger.error("Exception occured while executing this test is: {}", e);
        } finally {
            deleteJobData();
            elementaryDataBean.deleteMOTestData();
        }
    }

    @Test
    @InSequence(3)
    public void initialize() throws Throwable {
        backupTestGenerator.prepareConfigurationVersionMOTestData();
        final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.IDLE.name());
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.IDLE.name());
        elementaryDataBean.updateAttributes(backupTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);
    }

    @Test
    @InSequence(4)
    public void testUploadCv() throws Exception {
        try {
            logger.debug("Test UploadCvService Started.");

            initializeJobData();
            WorkflowInstanceServiceLocalImplMock.reset();
            final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
            final Activity uploadCv = uploadCvServiceTestFactory.getUploadCvService();
            final ActivityStepResult precheckResult = uploadCv.precheck(activityJobPoId);

            uploadCv.execute(activityJobPoId);

            System.out.println("Wait time for 1 second, since execution will take some time.");
            Thread.sleep(1000);

            //First Notification
            final Map<String, Object> firstNotification = new HashMap<String, Object>();
            firstNotification.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.EXPORTING_BACKUP_CV.name());
            firstNotification.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.CREATING_BACKUP.name());
            elementaryDataBean.updateAttributes(backupTestGenerator.getConfigurationVersionMOFdn(), firstNotification);

            //Second Notification
            final Map<String, Object> secondNotification = new HashMap<String, Object>();
            secondNotification.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.TRANSFERING_BACKUP_TO_REMOTE_SERVER.name());
            elementaryDataBean.updateAttributes(backupTestGenerator.getConfigurationVersionMOFdn(), secondNotification);

            //Third Notification
            final Map<String, Object> thirdNotification = new HashMap<String, Object>();
            thirdNotification.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.EXPORT_OF_BACKUP_CV_REQUESTED.name());
            elementaryDataBean.updateAttributes(backupTestGenerator.getConfigurationVersionMOFdn(), thirdNotification);

            //Fourth Notification
            final Map<String, Object> fourthNotification = new HashMap<String, Object>();
            fourthNotification.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.IDLE.name());
            fourthNotification.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.IDLE.name());
            final Map<String, Object> actionResult = new HashMap<String, Object>();
            actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, backupTestGenerator.CV_NAME);
            actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, CVInvokedAction.PUT_TO_FTP_SERVER.name());
            actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, CVActionMainResult.EXECUTED.name());
            fourthNotification.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResult);
            elementaryDataBean.updateAttributes(backupTestGenerator.getConfigurationVersionMOFdn(), fourthNotification);

            System.out.println("Wait time for 10 seconds since, notification needs to be processed before assertions.");
            Thread.sleep(10000);
            assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, precheckResult.getActivityResultEnum());

            logger.debug("Test UploadCvService Ended.");
        } catch (Exception e) {
            logger.error("Exception occured while executing this test is: {}", e);
        } finally {
            deleteJobData();
        }
    }

    @Test
    @InSequence(5)
    public void testUploadCvWhenTimedOutForSuccess() throws Exception {
        try {
            logger.debug("Test UploadCvService TimeOut Started For Success Scenario.");

            initializeJobData();

            final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
            final Activity uploadCv = uploadCvServiceTestFactory.getUploadCvService();

            final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
            final Map<String, Object> actionResult = new HashMap<String, Object>();

            actionResult.put(ConfigurationVersionMoConstants.ACTION_ID, -1);

            actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, backupTestGenerator.CV_NAME);
            actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, CVInvokedAction.PUT_TO_FTP_SERVER.name());
            actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, CVActionMainResult.EXECUTED.name());
            modifiedAttributeMap.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResult);

            elementaryDataBean.updateAttributes(backupTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);
            final ActivityStepResult timedOutResult = uploadCv.handleTimeout(activityJobPoId);
            assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, timedOutResult.getActivityResultEnum());

            logger.debug("Test UploadCvService TimeOut Ended For Success Scenario.");
        } catch (Exception e) {
            logger.error("Exception occured while executing this test is: {}", e);
        } finally {
            deleteJobData();
        }
    }

    @Test
    @InSequence(6)
    public void testUploadCvWhenTimedOutForFailure() throws Exception {
        try {
            logger.debug("Test UploadCvService TimeOut Started For Failure Scenario.");

            initializeJobData();

            final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
            final Activity uploadCv = uploadCvServiceTestFactory.getUploadCvService();
            final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
            final Map<String, Object> actionResult = new HashMap<String, Object>();
            actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, backupTestGenerator.CV_NAME);
            actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION,
                    CVInvokedAction.PUT_TO_FTP_SERVER.name());
            actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, CVActionMainResult.EXECUTION_FAILED.name());
            modifiedAttributeMap.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResult);
            modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY,
                    CVCurrentDetailedActivity.EXECUTION_FAILED.name());
            elementaryDataBean.updateAttributes(backupTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);
            final ActivityStepResult timedOutResult = uploadCv.handleTimeout(activityJobPoId);

            assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, timedOutResult.getActivityResultEnum());

            //Modifying back to original state modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.IDLE.name());
            elementaryDataBean.updateAttributes(backupTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);

            logger.debug("Test UploadCvService TimeOut Ended For Failure Scenario.");
        } catch (Exception e) {
            logger.error("Exception occured while executing this test is: {}", e);
        } finally {
            deleteJobData();
        }
    }

    @Test
    @InSequence(7)
    public void testUploadCvWhenTimedOutForStillExecuting() throws Exception {
        try {
            logger.debug("Test UploadCvService TimeOut Started For Still Executing Scenario.");

            initializeJobData();

            final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
            final Activity uploadCv = uploadCvServiceTestFactory.getUploadCvService();
            final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
            final Map<String, Object> modifiedAttributes = new HashMap<String, Object>();
            modifiedAttributes.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.EXPORTING_BACKUP_CV.name());
            elementaryDataBean.updateAttributes(backupTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributes);
            final ActivityStepResult timedOutResult = uploadCv.handleTimeout(activityJobPoId);

            assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, timedOutResult.getActivityResultEnum());

            //Modifying back to original state modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.IDLE.name());
            elementaryDataBean.updateAttributes(backupTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);

            logger.debug("Test UploadCvService TimeOut Ended For Still Executing Scenario.");
        } catch (Exception e) {
            logger.error("Exception occured while executing this test is: {}", e);
        } finally {
            deleteJobData();
        }
    }

    @Test
    @InSequence(8)
    public void delete() {
        logger.debug("UploadCvServiceIT Cleaning up data started.");
        elementaryDataBean.deleteMOTestData();
        elementaryDataBean.cleanUpTestPOData();
        logger.debug("UploadCvServiceIT Cleaning up data ended.");
    }

    private void initializeJobData() throws Exception {
        backupTestGenerator.prepareBackupJobData("exportcv");
    }

    private void deleteJobData() {
        elementaryDataBean.cleanUpTestPOData();
    }

}
