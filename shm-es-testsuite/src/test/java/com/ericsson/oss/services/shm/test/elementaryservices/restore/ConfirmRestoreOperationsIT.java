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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ericsson.oss.itpf.sdk.cache.annotation.NamedCache;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CVActionMainResult;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CVCurrentDetailedActivity;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionUtils;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.test.common.TestConstants;
import com.ericsson.oss.services.shm.test.elementaryservices.*;

/**
 * This test class validates the confirm restore action on node.
 * 
 * @author xvishsr
 */
@RunWith(Arquillian.class)
@ApplicationScoped
public class ConfirmRestoreOperationsIT {
    private static final String SHM_ES_TEST = "shm-es-test";
    @ArquillianResource
    private Deployer deployer;

    @Inject
    RestoreServiceTestFactory confirmRestoreServiceTestFactory;

    @Inject
    private IElementaryServicesTestBase elementaryDataBean;

    @Inject
    RestoreTestGenerator confirmRestoreTestGenerator;
    @Inject
    ConfigurationVersionUtils configurationVersionUtils;
    @Inject
    ActivityUtils activityUtils;

    @Inject
    @NamedCache("CppInventorySynchServiceNotificationRegistryCache")
    private Cache<String, NotificationSubject> cache;

    @Deployment(name = SHM_ES_TEST, managed = false)
    public static Archive<?> createADeployableSHMESEAR() {
        final WebArchive war = ElementaryServicesTestDeployment.createTestDeploymentForES();
        war.addClass(ConfirmRestoreOperationsIT.class);
        war.addClass(RestoreTestGenerator.class);
        war.addClass(RestoreServiceTestFactory.class);
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
        confirmRestoreTestGenerator.prepareRestoreJobData("confirm");
    }

    @Test
    @InSequence(3)
    public void precheckResultWhenMoIsNull() throws Throwable {
        System.out.println("*** Test PreCheck for Confirm Retore WhenMOIsNull Started ***");
        confirmRestoreTestGenerator.prepareBaseMOTestData();
        final Long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity confirmRestoreService = confirmRestoreServiceTestFactory.getConfirmRestoreService();
        final ActivityStepResult result = confirmRestoreService.precheck(activityJobPoId);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, result.getActivityResultEnum());
        elementaryDataBean.deleteMOTestData();
        System.out.println("*** Test PreCheck WhenMOIsNull Ended ***");
    }

    @Test
    @InSequence(4)
    public void initialize() throws Throwable {
        confirmRestoreTestGenerator.prepareConfigurationVersionMOTestData();
        final Map<String, Object> changedAttributesMap = new HashMap<String, Object>();
        changedAttributesMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.AWAITING_RESTORE_CONFIRMATION.name());
        elementaryDataBean.updateAttributes(confirmRestoreTestGenerator.getConfigurationVersionMOFdn(), changedAttributesMap);
    }

    @Test
    @InSequence(5)
    public void testConfirmRestore() throws Exception {
        System.out.println("*** Test Execute testConfirmRestore Started ***");
        final Long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity confirmRestoreService = confirmRestoreServiceTestFactory.getConfirmRestoreService();
        final ActivityStepResult precheckResult = confirmRestoreService.precheck(activityJobPoId);
        confirmRestoreService.execute(activityJobPoId);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, precheckResult.getActivityResultEnum());
        // assertEquals(ActivityStepResultEnum.EXECUTION_SUCESS, executeResult.getActivityResultEnum());
        System.out.println("*** Test Execute testConfirmRestore Ended ***");
    }

    @Test
    @InSequence(6)
    public void testConfirmRestoreProcessNotification() throws Exception {
        WorkflowInstanceServiceLocalImplMock.reset();
        System.out.println("*** REGISTERING CACHE ***");
        String fdn = "MeContext=ERBS101,NetworkElement=ERBS101";
        final Long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        JobActivityInfo jobActivityInfo = new JobActivityInfo(ElementaryServicesTestBase.poIds.get(activityJobPoId), "restore", JobTypeEnum.RESTORE, PlatformTypeEnum.CPP);

        final FdnNotificationSubject subject = new FdnNotificationSubject(fdn, activityJobPoId, jobActivityInfo);
        cache.put(subject.getKey(), subject);
        System.out.println("*** REGISTERING CACHE DONE ***" + cache);

        final Map<String, Object> changedAttributesMap = new HashMap<String, Object>();
        changedAttributesMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.RESTORE_CONFIRMATION_RECEIVED.name());
        elementaryDataBean.updateAttributes(confirmRestoreTestGenerator.getConfigurationVersionMOFdn(), changedAttributesMap);
        final Map<String, Object> changedAttributesMap2 = new HashMap<String, Object>();
        changedAttributesMap2.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.IDLE.name());
        elementaryDataBean.updateAttributes(confirmRestoreTestGenerator.getConfigurationVersionMOFdn(), changedAttributesMap2);

        Thread.sleep(5000);
        Assert.assertNotNull(ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.mainJobPoId) + "@" + TestConstants.NETWORK_ELEMENT_NAME);
    }

    @Test
    @InSequence(7)
    public void testConfirmRestoreTimeout() throws Exception {
        System.out.println("*** Test handleTimeout Started ***");
        final Long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Map<String, Object> activityJobAttributes = activityUtils.getPoAttributes(activityJobPoId);
        final Map<String, Object> changedAttributesMap2 = new HashMap<String, Object>();
        changedAttributesMap2.put(ConfigurationVersionMoConstants.ACTION_ID, configurationVersionUtils.getActionId(activityJobAttributes));
        changedAttributesMap2.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, CVActionMainResult.EXECUTED.name());

        final Map<String, Object> changedAttributesMap = new HashMap<String, Object>();
        changedAttributesMap.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentDetailedActivity.IDLE.name());
        changedAttributesMap.put(ConfigurationVersionMoConstants.ACTION_RESULT, changedAttributesMap2);

        elementaryDataBean.updateAttributes(confirmRestoreTestGenerator.getConfigurationVersionMOFdn(), changedAttributesMap);

        final Activity confirmRestoreService = confirmRestoreServiceTestFactory.getConfirmRestoreService();
        final ActivityStepResult result = confirmRestoreService.handleTimeout(activityJobPoId);
        System.out.println(" Result is {} " + result);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, result.getActivityResultEnum());
        System.out.println("*** Test handleTimeout Ended ***");
    }

    @Test
    @InSequence(8)
    public void delete() {
        System.out.println("*** Cleaning up data start *** ");
        elementaryDataBean.deleteMOTestData();
        elementaryDataBean.cleanUpTestPOData();
        System.out.println("*** Cleaning up data end ***");
    }

}
