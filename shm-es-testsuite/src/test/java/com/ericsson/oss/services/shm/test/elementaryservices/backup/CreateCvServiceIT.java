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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.cache.annotation.NamedCache;
import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.test.elementaryservices.*;
import com.ericsson.oss.services.shm.test.notifications.MockNotificationHandler;

/**
 * This test class facilitates the creation of configuration version of CPP
 * based node by invoking the ConfigurationVersion MO action that initializes
 * the create activity.
 * 
 * @author xcharoh
 */
@RunWith(Arquillian.class)
@ApplicationScoped
public class CreateCvServiceIT {

    @ArquillianResource
    private Deployer deployer;

    @Inject
    CreateCvServiceTestFactory createCvServiceTestFactory;

    @Inject
    private IElementaryServicesTestBase elementaryDataBean;

    @Inject
    BackupTestGenerator backupTestGenerator;

    @Inject
    @NamedCache("CppInventorySynchServiceNotificationRegistryCache")
    private Cache<String, NotificationSubject> cache;

    private static final Logger logger = LoggerFactory.getLogger(CreateCvServiceIT.class);

    @Deployment(name = "shm-es-test", managed = false)
    public static Archive<?> createADeployableSHMEAR() {
        final WebArchive war = ElementaryServicesTestDeployment.createTestDeploymentForES();
        war.addClass(CreateCvServiceTestFactory.class);
        war.addClass(MockNotificationHandler.class);
        war.addClass(BackupTestGenerator.class);
        war.addClass(CreateCvServiceIT.class);
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
    public void initializeJobData() throws Exception {
        backupTestGenerator.prepareBackupJobData("createcv");
    }

    @Test
    @InSequence(3)
    public void testCreateCvPreCheckWhenMOIsNull() throws Throwable {
        try {
            logger.debug("Test CreateCV PreCheck WhenMOIsNull Started.");
            elementaryDataBean.prepareBaseMOTestData();

            final Long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
            final Activity createCvService = createCvServiceTestFactory.getCreateCVService();
            final ActivityStepResult precheckResult = createCvService.precheck(activityJobPoId);
            assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, precheckResult.getActivityResultEnum());

            logger.debug("Test CreateCV PreCheck WhenMOIsNull Ended.");
        } catch (Exception e) {
            logger.error("Exception occured while executing this test is: {}", e);
        } finally {
            elementaryDataBean.deleteMOTestData();
        }
    }

    @Test
    @InSequence(4)
    public void initialize() throws Throwable {
        backupTestGenerator.prepareConfigurationVersionMOTestData();
    }

    @Test
    @InSequence(5)
    public void testCreateCv() throws Exception {
        logger.debug("Test CreateCV Started.");
        final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity createCvService = createCvServiceTestFactory.getCreateCVService();
        final ActivityStepResult precheckResult = createCvService.precheck(activityJobPoId);
        createCvService.execute(activityJobPoId);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, precheckResult.getActivityResultEnum());
        logger.debug("Test CreateCV Ended.");
    }

    @Test
    @InSequence(6)
    public void testCreateCvWhenTimedOutForSuccess() throws Exception {
        logger.debug("Test CreateCV TimeOut Started.");
        final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
        final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity createCvService = createCvServiceTestFactory.getCreateCVService();
        final ActivityStepResult precheckResult = createCvService.precheck(activityJobPoId);
        createCvService.execute(activityJobPoId);
        List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        storedConfigurationVersionList = backupTestGenerator.getStoredConfigurationVersionList();
        storedConfigurationVersionList.add(backupTestGenerator.getConfigurationVersionDetails(backupTestGenerator.CV_NAME));
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        elementaryDataBean.updateAttributes(backupTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);
        final ActivityStepResult timedOutResult = createCvService.handleTimeout(activityJobPoId);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, precheckResult.getActivityResultEnum());
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, timedOutResult.getActivityResultEnum());
        logger.debug("Test CreateCV TimeOut Ended.");
    }

    @Test
    @InSequence(7)
    public void testCreateCvWhenTimedOutForFailure() throws Exception {
        logger.debug("Test CreateCV TimeOut Started.");
        final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity createCvService = createCvServiceTestFactory.getCreateCVService();
        final ActivityStepResult precheckResult = createCvService.precheck(activityJobPoId);
        createCvService.execute(activityJobPoId);
        final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
        List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        storedConfigurationVersionList = backupTestGenerator.getStoredConfigurationVersionList();
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList);
        elementaryDataBean.updateAttributes(backupTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);
        final ActivityStepResult timedOutResult = createCvService.handleTimeout(activityJobPoId);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, precheckResult.getActivityResultEnum());
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, timedOutResult.getActivityResultEnum());
        logger.debug("Test CreateCV TimeOut Ended.");
    }

    @Test
    @InSequence(8)
    public void delete() {
        logger.debug("Cleaning up data started.");
        elementaryDataBean.deleteMOTestData();
        elementaryDataBean.cleanUpTestPOData();
        logger.debug("Cleaning up data ended.");
    }

}
