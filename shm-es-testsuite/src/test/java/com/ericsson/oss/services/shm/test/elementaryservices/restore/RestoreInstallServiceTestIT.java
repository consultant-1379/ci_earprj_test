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
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;

import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.WorkFlowConstants;
import com.ericsson.oss.services.shm.test.elementaryservices.ElementaryServicesTestDeployment;
import com.ericsson.oss.services.shm.test.elementaryservices.WorkflowInstanceServiceLocalImplMock;
import com.ericsson.oss.services.shm.test.notifications.DpsTestBase;
import com.ericsson.oss.services.shm.test.notifications.IDpsTestBase;

//@RunWith(Arquillian.class)
//@ApplicationScoped
public class RestoreInstallServiceTestIT {

    private static final String SHM_RESTORE = "shm-es-restoreinstall-test";

    @ArquillianResource
    private Deployer deployer;

    @Inject
    private IDpsTestBase databean;

    @Inject
    RestoreServiceTestFactory factory;

    @Inject
    TestDataGenerator helper;

    Activity restoreInstaller;

    //@Deployment(name = SHM_RESTORE, managed = false)
    public static Archive<?> createADeployableSHMEAR() {
        return createTestDeployment_RestoreCV();
    }

    //@Test
    @InSequence(1)
    @OperateOnDeployment(SHM_RESTORE)
    public void deploySHMTestEAR() throws Exception {
        this.deployer.deploy(SHM_RESTORE);
    }

    //@Test
    @InSequence(2)
    public void initialize() throws IOException {
        databean.initMOdata();
        helper.createAllTestJobs();
        helper.createSoftwarePackageTestData();
    }

    /**
     * We have two packages in CV Mo, Now Starts with a package1, and says the execute step needs to be repeated
     * 
     * @throws Exception
     */
    //@Test
    @InSequence(3)
    public void testRestoreInstall_intialTrigger() throws Exception {
        System.out.println("++++++++++++++++++++++++++++ Test PreCheck Started");
        WorkflowInstanceServiceLocalImplMock.reset();
        databean.createUpgradePackageMO("1");
        Long activityJobPoId = TestDataGenerator.poIds.get(TestDataGenerator.activityJobPoId);
        restoreInstaller = factory.getInstallService();
        ActivityStepResult preCheckResult = restoreInstaller.precheck(activityJobPoId);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, preCheckResult.getActivityResultEnum());
        System.out.println("++++++++++++++++++++++++++++ Test PreCheck Ended");

        System.out.println("++++++++++++ intialTrigger ++++++++++++++++ Test Execute Started");
        restoreInstaller.execute(activityJobPoId);
        //As execute is an asynchronous action, wait untill its the action is invoked, then go for next step
        Thread.sleep(5000);

        //As this test case doesnt behaves like a simulated node, the state will be by default NOT_INSTALLED, and can not be updated when we do action on UP MO
        int upMOId = 1;
        assertEquals("NOT_INSTALLED", databean.checkUPMoState(upMOId, "state"));
        System.out.println("++++++++++++++ intialTrigger ++++++++++++++ Test Execute Ended");

        Map<String, Object> map = new HashMap<String, Object>();
        map.put(DpsTestBase.UP_STATE, "INSTALL_EXECUTING");
        map.put(DpsTestBase.UP_PROGRESS_COUNT, 5);
        map.put(DpsTestBase.UP_PROGRESS_TOTAL, 10);
        map.put(DpsTestBase.UP_PROGRESS_HEADER, "IDLE");
        databean.moUpdateROAttrs(DpsTestBase.UPMO_FDN_2 + upMOId, map);
        System.out.println("waiting for 5 seconds since, notification needs to be processed before assertions.");
        Thread.sleep(5000);
        assertNull(WorkflowInstanceServiceLocalImplMock.MESSAGE);
        assertNull(WorkflowInstanceServiceLocalImplMock.BUSINESS_KEY);
        assertNull(WorkflowInstanceServiceLocalImplMock.PROCESS_VARIABLES);

        Map<String, Object> map2 = new HashMap<String, Object>();
        map2.put(DpsTestBase.UP_STATE, "INSTALL_COMPLETED");
        map2.put(DpsTestBase.UP_PROGRESS_COUNT, 5);
        map2.put(DpsTestBase.UP_PROGRESS_TOTAL, 10);
        map2.put(DpsTestBase.UP_PROGRESS_HEADER, "IDLE");
        databean.moUpdateROAttrs(DpsTestBase.UPMO_FDN_2 + upMOId, map2);
        System.out.println("waiting for 5 seconds since, notification needs to be processed before assertions.");
        Thread.sleep(5000);
        assertEquals(WorkFlowConstants.ACTIVATE_WFMESSAGE, WorkflowInstanceServiceLocalImplMock.MESSAGE);
        assertEquals("222", WorkflowInstanceServiceLocalImplMock.BUSINESS_KEY);
        Assert.assertTrue(WorkflowInstanceServiceLocalImplMock.PROCESS_VARIABLES.containsKey(JobVariables.ACTIVITY_REPEAT_EXECUTE));
    }

    /**
     * Starts with Package2 and no repetition required.
     * 
     * @throws Exception
     */
    //@Test
    @InSequence(4)
    public void testRestoreInstall_firstRepetition() throws Exception {
        WorkflowInstanceServiceLocalImplMock.reset();
        Long activityJobPoId = TestDataGenerator.poIds.get(TestDataGenerator.activityJobPoId);
        System.out.println("+++++++++++ firstRepetition +++++++++++++++++ Test Execute Started");
        restoreInstaller = factory.getInstallService();
        restoreInstaller.execute(activityJobPoId);
        //As execute is an asynchronous action, wait untill its the action is invoked, then go for next step
        Thread.sleep(5000);

        //As this test case doesnt behaves like a simulated node, the state will be by default NOT_INSTALLED, and can not be updated when we do action on UP MO
        int upMOId = 3;
        assertEquals("NOT_INSTALLED", databean.checkUPMoState(upMOId, "state"));
        System.out.println("++++++++++++++ firstRepetition ++++++++++++++ Test Execute Ended");

        System.out.println("+++++++++++++ firstRepetition +++++++++++++++ Test handleTimeout Started");
        //Update the prop and then go into time out, then timeout should pass.
        Map<String, Object> map2 = new HashMap<String, Object>();
        map2.put(DpsTestBase.UP_STATE, "INSTALL_COMPLETED");
        map2.put(DpsTestBase.UP_PROGRESS_COUNT, 5);
        map2.put(DpsTestBase.UP_PROGRESS_TOTAL, 10);
        map2.put(DpsTestBase.UP_PROGRESS_HEADER, "IDLE");
        databean.moUpdateROAttrs(DpsTestBase.UPMO_FDN_2 + upMOId, map2);
        ActivityStepResult timeoutResult = restoreInstaller.handleTimeout(activityJobPoId);
        assertEquals(ActivityStepResultEnum.REPEAT_EXECUTE, timeoutResult.getActivityResultEnum());
        System.out.println("+++++++++++++ firstRepetition +++++++++++++++ Test handleTimeout Ended");
    }

    /**
     * All the packages already processed and no more packages left to install in this case
     * 
     * @throws Exception
     */
    //@Test
    @InSequence(5)
    public void testRestoreInstall_secondRepetation() throws Exception {
        Long activityJobPoId = TestDataGenerator.poIds.get(TestDataGenerator.activityJobPoId);
        System.out.println("+++++++++secondRepetation+++++++++++ Test Execute Started");
        restoreInstaller = factory.getInstallService();
        restoreInstaller.execute(activityJobPoId);
        //As execute is an asynchronous action, wait untill its the action is invoked, then go for next step
        Thread.sleep(5000);

        //As this test case doesnt behaves like a simulated node, the state will be by default NOT_INSTALLED, and can not be updated when we do action on UP MO
        int upMOId = 2;
        assertEquals("NOT_INSTALLED", databean.checkUPMoState(upMOId, "state"));
        System.out.println("++++++++++secondRepetation+++++++++++++ Test Execute Ended");

        System.out.println("++++++++++secondRepetation+++++++++++++++ Test handleTimeout Started");

        ActivityStepResult timeoutResult = restoreInstaller.handleTimeout(activityJobPoId);

        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, timeoutResult.getActivityResultEnum());
        System.out.println("++++++++++++secondRepetation+++++++++++++ Test handleTimeout Ended");
    }

    /**
     * All the packages already processed and no more packages left to install in this case
     * 
     * @throws Exception
     */
    //@Test
    @InSequence(6)
    public void testRestoreInstall_noPackagesLeftToInstall() throws Exception {
        int upMOId = 2;
        Long activityJobPoId = TestDataGenerator.poIds.get(TestDataGenerator.activityJobPoId);
        System.out.println("+++++++++noPackagesFound+++++++++++ Test Execute Started");
        restoreInstaller = factory.getInstallService();
        restoreInstaller.execute(activityJobPoId);
        //As execute is an asynchronous action, wait untill its the action is invoked, then go for next step
        Thread.sleep(5000);
        System.out.println("++++++++++noPackagesFound+++++++++++++ Test Execute Ended");

        System.out.println("++++++++++noPackagesFound+++++++++++++++ Test handleTimeout Started");
        Map<String, Object> map2 = new HashMap<String, Object>();
        map2.put(DpsTestBase.UP_STATE, "INSTALL_COMPLETED");
        map2.put(DpsTestBase.UP_PROGRESS_COUNT, 5);
        map2.put(DpsTestBase.UP_PROGRESS_TOTAL, 10);
        map2.put(DpsTestBase.UP_PROGRESS_HEADER, "IDLE");
        databean.moUpdateROAttrs(DpsTestBase.UPMO_FDN_2 + upMOId, map2);
        ActivityStepResult timeoutResult = restoreInstaller.handleTimeout(activityJobPoId);

        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, timeoutResult.getActivityResultEnum());
        System.out.println("++++++++++++noPackagesFound+++++++++++++ Test handleTimeout Ended");
    }

    //@Test
    @InSequence(7)
    public void delete() {
        System.out.println("======= Cleaning up data start=======");
        databean.delete();
        helper.cleanUpTestJobsData();
        helper.deleteSoftwarePackageTestData();
        System.out.println("======= Cleaning up data end=======");
    }

    public static Archive<?> createTestDeployment_RestoreCV() {
        final WebArchive war = ElementaryServicesTestDeployment.createTestDeploymentForES();
        war.addClass(RestoreInstallServiceTestIT.class);
        war.addClass(RestoreServiceTestFactory.class);
        war.addClass(TestDataGenerator.class);
        war.addClass(IDpsTestBase.class);
        war.addClass(DpsTestBase.class);
        return war;
    }
}
