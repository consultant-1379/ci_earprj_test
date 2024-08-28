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
 * This test class validates the Download restore action on node.
 * 
 * @author xbonsan
 */
@RunWith(Arquillian.class)
@ApplicationScoped
public class DownloadCvTestIT {

    @ArquillianResource
    private Deployer deployer;

    @Inject
    RestoreServiceTestFactory DownloadCvServiceTestFactory;

    @Inject
    private IElementaryServicesTestBase elementaryDataBean;

    @Inject
    RestoreTestGenerator DownloadCvTestGenerator;

    @Inject
    ActivityUtils activityUtils;

    @Inject
    ConfigurationVersionUtils configurationVersionUtils;

    @Inject
    @NamedCache("CppInventorySynchServiceNotificationRegistryCache")
    private Cache<String, NotificationSubject> cache;

    @Deployment(name = "shm-es-test-download", managed = false)
    public static Archive<?> createADeployableSHMESEAR() {
        final WebArchive war = ElementaryServicesTestDeployment.createTestDeploymentForES();
        war.addClass(DownloadCvTestIT.class);
        war.addClass(RestoreTestGenerator.class);
        war.addClass(RestoreServiceTestFactory.class);
        war.addClass(RemoteSoftwarePackageService.class);
        return war;

    }

    @Test
    @InSequence(1)
    @OperateOnDeployment("shm-es-test-download")
    public void deployESTestEAR() throws Exception {
        this.deployer.deploy("shm-es-test-download");
    }

    @Test
    @InSequence(2)
    public void initializeJobData() throws Exception {
        DownloadCvTestGenerator.prepareRestoreJobData("download");
    }

    @Test
    @InSequence(3)
    public void precheckResultWhenMoIsNull() throws Throwable {
        System.out.println("*** Test PreCheck for Download Retore WhenMOIsNull Started ***");
        DownloadCvTestGenerator.prepareBaseMOTestData();
        final Long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity downloadCVService = DownloadCvServiceTestFactory.getDownloadCVService();
        final ActivityStepResult result = downloadCVService.precheck(activityJobPoId);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, result.getActivityResultEnum());
        System.out.println("*** Test PreCheck WhenMOIsNull Ended ***");

        elementaryDataBean.deleteMOTestData();
        Thread.sleep(10000);
    }

    @Test
    @InSequence(4)
    public void testdownloadServiceWhenMOIsBusy() throws Throwable {
        System.out.println("*** Test DownloadService When MO IS busy Started ***");
        DownloadCvTestGenerator.prepareConfigurationVersionMOTestData();
        final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity downloadService = DownloadCvServiceTestFactory.getDownloadCVService();
        final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.IMPORTING_BACKUP_CV.name());
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.IMPORT_OF_BACKUP_CV_REQUESTED.name());
        elementaryDataBean.updateAttributes(DownloadCvTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);
        final ActivityStepResult precheckResult = downloadService.precheck(activityJobPoId);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, precheckResult.getActivityResultEnum());

        //Modifying back to original state for further test cases
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.IDLE.name());
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.IDLE.name());
        elementaryDataBean.updateAttributes(DownloadCvTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);

        System.out.println("*** Test DownloadService When MO IS busy Ended ***");

        elementaryDataBean.deleteMOTestData();
        Thread.sleep(10000);
    }

    @Test
    @InSequence(5)
    public void initialize() throws Throwable {

        DownloadCvTestGenerator.prepareConfigurationVersionMOTestData();
        final Map<String, Object> changedAttributesMap = new HashMap<String, Object>();

        final List<Map<String, String>> storedCVsList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedCV = new HashMap<String, String>();
        storedCV.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "CV_Name");
        storedCV.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_TYPE, "DOWNLOADED");
        storedCVsList.add(storedCV);

        changedAttributesMap.put("currentDetailedActivity", CVCurrentDetailedActivity.IDLE.name());
        changedAttributesMap.put("currentMainActivity", CVCurrentMainActivity.IDLE.name());
        changedAttributesMap.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedCVsList);

        elementaryDataBean.updateAttributes(DownloadCvTestGenerator.getConfigurationVersionMOFdn(), changedAttributesMap);
    }

    @Test
    @InSequence(6)
    public void testPrecheckDownloadCv() throws Exception {
        System.out.println("*** Test Execute testDownloadCv Started ***");
        final Long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity downloadCvService = DownloadCvServiceTestFactory.getDownloadCVService();
        final ActivityStepResult precheckResult = downloadCvService.precheck(activityJobPoId);
        downloadCvService.execute(activityJobPoId);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION, precheckResult.getActivityResultEnum());
        System.out.println("*** Test Execute testDownloadCv Ended ***");
    }

    @Test
    @InSequence(7)
    public void testDownloadRestoreProcessNotification() throws Exception {
        System.out.println("*** Test DownloadService Started ***");
        WorkflowInstanceServiceLocalImplMock.reset();
        final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity downloadCvService = DownloadCvServiceTestFactory.getDownloadCVService();
        final ActivityStepResult precheckResult = downloadCvService.precheck(activityJobPoId);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION, precheckResult.getActivityResultEnum());
        System.out.println("++++++++++++ intialTrigger ++++++++++++++++ Test Execute Started");
        downloadCvService.execute(activityJobPoId);
        //As execute is an asynchronous action, wait until its action is invoked, then will proceed for next step
        Thread.sleep(50000);

        final Map<String, Object> activityJobAttributes = activityUtils.getPoAttributes(activityJobPoId);

        final Map<String, Object> actionResult = new HashMap<String, Object>();
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, DownloadCvTestGenerator.CV_NAME);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, CVInvokedAction.GET_FROM_FTP_SERVER.name());
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, CVActionMainResult.EXECUTED.name());
        actionResult.put(ConfigurationVersionMoConstants.ACTION_ID, configurationVersionUtils.getActionId(activityJobAttributes));

        final Map<String, Object> firstNotification = new HashMap<String, Object>();
        firstNotification.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.IMPORTING_BACKUP_CV.name());
        firstNotification.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.IMPORT_OF_BACKUP_CV_REQUESTED.name());
        elementaryDataBean.updateAttributes(DownloadCvTestGenerator.getConfigurationVersionMOFdn(), firstNotification);

        final Map<String, Object> secondNotification = new HashMap<String, Object>();
        secondNotification.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.RETREIVING_BACKUP_FROM_REMOTE_SERVER.name());
        elementaryDataBean.updateAttributes(DownloadCvTestGenerator.getConfigurationVersionMOFdn(), secondNotification);

        final Map<String, Object> thirdNotification = new HashMap<String, Object>();
        thirdNotification.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.EXECUTION_FAILED.name());
        elementaryDataBean.updateAttributes(DownloadCvTestGenerator.getConfigurationVersionMOFdn(), thirdNotification);

        // fourthNotification.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.IDLE.name());
        final Map<String, Object> fourthNotification = new HashMap<String, Object>();
        fourthNotification.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResult);
        elementaryDataBean.updateAttributes(DownloadCvTestGenerator.getConfigurationVersionMOFdn(), fourthNotification);

        Thread.sleep(30000);

        assertEquals(WorkFlowConstants.ACTIVATE_WFMESSAGE, WorkflowInstanceServiceLocalImplMock.MESSAGE);
        assertEquals((ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.mainJobPoId) + "@" + TestConstants.NETWORK_ELEMENT_NAME), WorkflowInstanceServiceLocalImplMock.BUSINESS_KEY);

        System.out.println("*** Test DownloadService Ended ***");
    }

    @Test
    @InSequence(8)
    public void testDownloadCvTimeoutSuccess() throws Exception {
        System.out.println("*** Test handleTimeout Started ***");
        final Long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity downloadCvService = DownloadCvServiceTestFactory.getDownloadCVService();

        final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
        final Map<String, Object> actionResult = new HashMap<String, Object>();
        downloadCvService.execute(activityJobPoId);
        //As execute is an asynchronous action, wait until its action is invoked, then will proceed for next step
        Thread.sleep(50000);

        final Map<String, Object> activityJobAttributes = activityUtils.getPoAttributes(activityJobPoId);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, DownloadCvTestGenerator.CV_NAME);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, CVInvokedAction.GET_FROM_FTP_SERVER.name());
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, CVActionMainResult.EXECUTED.name());
        actionResult.put(ConfigurationVersionMoConstants.ACTION_ID, configurationVersionUtils.getActionId(activityJobAttributes));
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResult);

        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.IMPORTING_BACKUP_CV.name());
        elementaryDataBean.updateAttributes(DownloadCvTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);

        final ActivityStepResult result = downloadCvService.handleTimeout(activityJobPoId);
        System.out.println(" Result is {} " + result);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, result.getActivityResultEnum());
        //Modifying back to original state for further test cases
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.IDLE.name());
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.IDLE.name());
        elementaryDataBean.updateAttributes(DownloadCvTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);
        System.out.println("*** Test downloadCvService TimeOut Ended ***");
    }

    @Test
    @InSequence(9)
    public void testRestoreServiceWhenTimedOutForFailure() throws Exception {
        System.out.println("*** Test downloadCvService TimeOut Started ***");
        final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity downloadCvService = DownloadCvServiceTestFactory.getDownloadCVService();
        final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
        final Map<String, Object> actionResult = new HashMap<String, Object>();
        downloadCvService.execute(activityJobPoId);
        //As execute is an asynchronous action, wait until its action is invoked, then will proceed for next step
        Thread.sleep(5000);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, DownloadCvTestGenerator.CV_NAME);
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, CVInvokedAction.GET_FROM_FTP_SERVER.name());
        actionResult.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, CVActionMainResult.EXECUTION_FAILED.name());

        modifiedAttributeMap.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResult);

        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.EXECUTION_FAILED.name());
        elementaryDataBean.updateAttributes(DownloadCvTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);
        final ActivityStepResult timedOutResult = downloadCvService.handleTimeout(activityJobPoId);

        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, timedOutResult.getActivityResultEnum());

        //Modifying back to original state for further test cases
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.IDLE.name());
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.IDLE.name());
        elementaryDataBean.updateAttributes(DownloadCvTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);
        System.out.println("*** Test downloadCvService TimeOut Ended ***");
    }

    @Test
    @InSequence(10)
    public void testRestoreServiceWhenTimedOutForStillExecuting() throws Exception {
        System.out.println("*** Test downloadCvService TimeOut Started For Still Executing Scenario***");
        final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity downloadCvService = DownloadCvServiceTestFactory.getDownloadCVService();
        final Map<String, Object> modifiedAttributes = new HashMap<String, Object>();
        final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
        modifiedAttributes.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.IMPORTING_BACKUP_CV.name());
        modifiedAttributes.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.IMPORT_OF_BACKUP_CV_REQUESTED.name());
        elementaryDataBean.updateAttributes(DownloadCvTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributes);
        final ActivityStepResult timedOutResult = downloadCvService.handleTimeout(activityJobPoId);

        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, timedOutResult.getActivityResultEnum());

        //Modifying back to original state for further test cases
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.IDLE.name());
        modifiedAttributeMap.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.IDLE.name());
        elementaryDataBean.updateAttributes(DownloadCvTestGenerator.getConfigurationVersionMOFdn(), modifiedAttributeMap);

        System.out.println("*** Test downloadCvService TimeOut Ended For Still Executing Scenario ***");
    }

    @Test
    @InSequence(11)
    public void delete() {
        System.out.println("*** Cleaning up data start *** ");
        elementaryDataBean.deleteMOTestData();
        elementaryDataBean.cleanUpTestPOData();
        System.out.println("*** Cleaning up data end ***");
    }

}
