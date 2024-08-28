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
package com.ericsson.oss.services.shm.test.elementaryservices.license;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import javax.cache.Cache;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;

import com.ericsson.oss.itpf.sdk.cache.annotation.NamedCache;
import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.WorkFlowConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.test.common.TestConstants;
import com.ericsson.oss.services.shm.test.elementaryservices.*;

/**
 * This test class facilitates the installation of license key files of CPP based node by invoking the licensing MO action that initializes the install activity.
 * 
 * @author xmanush
 */
//@RunWith(Arquillian.class)
//@ApplicationScoped
public class LicenseOperationsIT {
    private static final String SHM_ES_TEST = "shm-es-test";
    @ArquillianResource
    private Deployer deployer;

    @Inject
    InstallLicenseKeyFileServiceTestFactory installLicenseKeyFileServiceTestFactory;

    @Inject
    private IElementaryServicesTestBase elementaryDataBean;

    @Inject
    LicenseTestGenerator licenseTestGenerator;

    @Inject
    @NamedCache("CppInventorySynchServiceNotificationRegistryCache")
    private Cache<String, NotificationSubject> cache;

    //@Deployment(name = SHM_ES_TEST, managed = false)
    public static Archive<?> createADeployableSHMESEAR() {
        final WebArchive war = ElementaryServicesTestDeployment.createTestDeploymentForES();
        war.addClass(LicenseOperationsIT.class);
        war.addClass(LicenseTestGenerator.class);
        war.addClass(InstallLicenseKeyFileServiceTestFactory.class);
        return war;
    }

    //@Test
    @InSequence(1)
    @OperateOnDeployment(SHM_ES_TEST)
    public void deployESTestEAR() throws Exception {
        this.deployer.deploy(SHM_ES_TEST);
    }

    //@Test
    @InSequence(2)
    public void initializeJobData() throws Exception {
        licenseTestGenerator.prepareLicenseJobData("install");
    }

    //@Test
    @InSequence(3)
    public void testInstallLicensePreCheckWhenMOIsNull() throws Exception {
        System.out.println("*** Test PreCheck WhenMOIsNull Started ***");
        final Long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity installLicenseKeyFileService = installLicenseKeyFileServiceTestFactory.getInstallLicenseKeyFileService();
        final ActivityStepResult result = installLicenseKeyFileService.precheck(activityJobPoId);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, result.getActivityResultEnum());
        System.out.println("*** Test PreCheck WhenMOIsNull Ended ***");
    }

    //@Test
    @InSequence(4)
    public void testInstallLicenseExecuteWhenMOIsNull() throws Exception {
        System.out.println("*** Test Execute WhenMOIsNull Started ***");
        final Long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity installLicenseKeyFileService = installLicenseKeyFileServiceTestFactory.getInstallLicenseKeyFileService();
        installLicenseKeyFileService.execute(activityJobPoId);
        System.out.println("*** Test Execute WhenMOIsNull Ended ***");
    }

    //@Test
    @InSequence(5)
    public void initializeLicensingMOData() throws Throwable {
        licenseTestGenerator.prepareLicensingMOTestData();
    }

    //@Test
    @InSequence(6)
    public void testInstallLicensePreCheckWhenPOIsEmpty() throws Exception {
        System.out.println("*** Test PreCheck WhenPOIsEmpty Started ***");
        final Long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity installLicenseKeyFileService = installLicenseKeyFileServiceTestFactory.getInstallLicenseKeyFileService();
        final ActivityStepResult result = installLicenseKeyFileService.precheck(activityJobPoId);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, result.getActivityResultEnum());
        System.out.println("*** Test PreCheck WhenPOIsEmpty Ended ***");
    }

    //@Test
    @InSequence(7)
    public void initializeLicensingPOData() throws Throwable {
        licenseTestGenerator.prepareLicensePOTestData();
    }

    //@Test
    @InSequence(8)
    public void testInstallLicensePreCheckWithExecute() throws Exception {
        System.out.println("*** Test PreCheck Started ***");
        Long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        Activity installLicenseKeyFileService = installLicenseKeyFileServiceTestFactory.getInstallLicenseKeyFileService();
        final ActivityStepResult activityResultForPrecheck = installLicenseKeyFileService.precheck(activityJobPoId);
        installLicenseKeyFileService.execute(activityJobPoId);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, activityResultForPrecheck.getActivityResultEnum());
        //assertEquals(ActivityStepResultEnum.EXECUTION_SUCESS, activityResultForExecute.getActivityResultEnum());
        System.out.println("*** Test PreCheck Ended ***");
    }

    //@Test
    @InSequence(9)
    public void testInstallLicenseWhenTimedoutForSuccess() throws Exception {
        System.out.println("*** Test InstallLicense TimeOut Started ***");
        final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
        final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        Activity installLicenseKeyFileService = installLicenseKeyFileServiceTestFactory.getInstallLicenseKeyFileService();
        final ActivityStepResult activityResultForPrecheck = installLicenseKeyFileService.precheck(activityJobPoId);
        installLicenseKeyFileService.execute(activityJobPoId);

        modifiedAttributeMap.put(LicensingActivityConstants.LAST_LICENSING_PI_CHANGE, "141022_131638");
        elementaryDataBean.updateAttributes(licenseTestGenerator.getLicensingMOFdn(), modifiedAttributeMap);
        final ActivityStepResult timedOutResult = installLicenseKeyFileService.handleTimeout(activityJobPoId);

        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, activityResultForPrecheck.getActivityResultEnum());
        //assertEquals(ActivityStepResultEnum.EXECUTION_SUCESS, activityResultForExecute.getActivityResultEnum());
        assertEquals(JobResult.SUCCESS.toString(), elementaryDataBean.getJobResultForActivity(activityJobPoId));
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, timedOutResult.getActivityResultEnum());
        System.out.println("*** Test InstallLicense TimeOut Ended ***");
    }

    //@Test
    @InSequence(10)
    public void testInstallLicenseWhenTimedOutForFailure() throws Exception {
        System.out.println("*** Test InstallLicense TimeOut Started ***");
        final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();
        final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        Activity installLicenseKeyFileService = installLicenseKeyFileServiceTestFactory.getInstallLicenseKeyFileService();
        final ActivityStepResult activityResultForPrecheck = installLicenseKeyFileService.precheck(activityJobPoId);
        installLicenseKeyFileService.execute(activityJobPoId);

        modifiedAttributeMap.put(LicensingActivityConstants.LAST_LICENSING_PI_CHANGE, "131022_131637");
        elementaryDataBean.updateAttributes(licenseTestGenerator.getLicensingMOFdn(), modifiedAttributeMap);
        final ActivityStepResult timedOutResult = installLicenseKeyFileService.handleTimeout(activityJobPoId);

        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, activityResultForPrecheck.getActivityResultEnum());
        // assertEquals(ActivityStepResultEnum.EXECUTION_SUCESS, activityResultForExecute.getActivityResultEnum());
        assertEquals(JobResult.SUCCESS.toString(), elementaryDataBean.getJobResultForActivity(activityJobPoId));
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, timedOutResult.getActivityResultEnum());
        System.out.println("*** Test InstallLicense TimeOut Ended ***");
    }

    //@Test
    @InSequence(11)
    public void testInstallLicenseProcessNotification() throws Exception {
        WorkflowInstanceServiceLocalImplMock.reset();

        System.out.println("Entered into testProcessNotification");

        final long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity installLicenseKeyFileService = installLicenseKeyFileServiceTestFactory.getInstallLicenseKeyFileService();
        final ActivityStepResult precheckResult = installLicenseKeyFileService.precheck(activityJobPoId);
        System.out.println("*** REGISTERING CACHE ***");
        installLicenseKeyFileService.execute(activityJobPoId);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, precheckResult.getActivityResultEnum());
        System.out.println("*** REGISTERING CACHE DONE ***" + cache);

        Map<String, Object> changedAttributesMap = new HashMap<String, Object>();
        changedAttributesMap.put("lastLicensingPiChange", "161022_131637");
        changedAttributesMap.put("userLabel", "test label1");
        System.out.println("The Licensing MO fdn is:{}" + licenseTestGenerator.getLicensingMOFdn());
        elementaryDataBean.updateAttributes(licenseTestGenerator.getLicensingMOFdn(), changedAttributesMap);
        System.out.println("Wait time for 10 seconds since, notification needs to be processed before assertions.");
        Thread.sleep(10000);
        assertEquals(WorkFlowConstants.ACTIVATE_WFMESSAGE, WorkflowInstanceServiceLocalImplMock.MESSAGE);
        assertEquals((ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.mainJobPoId) + "@" + TestConstants.NETWORK_ELEMENT_NAME), WorkflowInstanceServiceLocalImplMock.BUSINESS_KEY);
        Assert.assertTrue(WorkflowInstanceServiceLocalImplMock.PROCESS_VARIABLES.containsKey("activityResult"));
    }

    //@Test
    @InSequence(12)
    public void testInstallLicenseHandleTimeout() throws Exception {
        System.out.println("*** Test handleTimeout Started ***");
        final Long activityJobPoId = ElementaryServicesTestBase.poIds.get(ElementaryServicesTestBase.activityJobPoId);
        final Activity installLicenseKeyFileService = installLicenseKeyFileServiceTestFactory.getInstallLicenseKeyFileService();
        final ActivityStepResult result = installLicenseKeyFileService.handleTimeout(activityJobPoId);
        System.out.println(" Result is {} " + result);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, result.getActivityResultEnum());
        System.out.println("*** Test handleTimeout Ended ***");
    }

    //@Test
    @InSequence(13)
    public void delete() {
        System.out.println("*** Cleaning up data start *** ");
        elementaryDataBean.deleteMOTestData();
        elementaryDataBean.cleanUpTestPOData();
        System.out.println("*** Cleaning up data end ***");
    }

}
