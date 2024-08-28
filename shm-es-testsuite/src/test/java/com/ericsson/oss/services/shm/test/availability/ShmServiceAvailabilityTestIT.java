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
package com.ericsson.oss.services.shm.test.availability;

import static org.junit.Assert.assertEquals;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

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

import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.test.elementaryservices.WorkflowInstanceServiceLocalImplMock;

/**
 * This test class facilitates the testing of SMRS and IdentityManagement service outage.
 * 
 * @author xmanush
 */

@RunWith(Arquillian.class)
@ApplicationScoped
public class ShmServiceAvailabilityTestIT extends ConfigParamUpdater {

    private static final String SHM_ES_TEST = "shm-es-test";
    private static final String VERSANT_STOP_DB = "stopdb -f dps_integration";
    private static final String VERSANT_UNSTARTABLE_MODE = "dbinfo -0 dps_integration";
    private static final String VERSANT_MULTIUSER_MODE = "dbinfo -m dps_integration";

    @ArquillianResource
    private Deployer deployer;

    @Inject
    ShmServicesProviderFactory smrsIdmServiceTestFactory;

    @Inject
    private ShmServiceAvailabilityTestBase availabilityDataBean;

    @Deployment(name = SHM_ES_TEST, managed = false)
    public static Archive<?> createADeployableSHMESWAR() {
        final WebArchive war = ShmServiceAvailabilityTestDeployment.createTestDeploymentForES();
        war.addClass(ShmServiceAvailabilityTestIT.class);
        war.addClass(ShmServicesProviderFactory.class);
        war.addClass(ConfigParamUpdater.class);
        war.addClass(WorkflowInstanceServiceLocalImplMock.class);
        return war;
    }

    @Deployment(name = "smrsidm-test", managed = false)
    public static Archive<?> createSMRSIDMDeployableWar() {
        final WebArchive war = ShmServiceAvailabilityTestDeployment.createModuleArchiveForSMRSIDM();
        return war;
    }

    @Deployment(managed = false, name = PIB, testable = false)
    public static Archive<?> createPibDeploymentear() throws Exception {
        return createPibDeployment();
    }

    @Test
    @InSequence(1)
    @OperateOnDeployment(SHM_ES_TEST)
    public void deployESTestEAR() throws Exception {
        this.deployer.deploy(SHM_ES_TEST);
    }

    @InSequence(2)
    @Test
    public void deployPib() throws Exception {
        this.deployer.deploy(PIB);
    }

    @Test
    @InSequence(3)
    @OperateOnDeployment(SHM_ES_TEST)
    public void initializeData() throws Throwable {
        availabilityDataBean.prepareLicenseJobData("install");
        availabilityDataBean.cretaeLicensingMOAndBaseMoTestData();
        availabilityDataBean.prepareLicensePOTestData();
        updateConfiguredParam("wfsRetryCount", "2");
        updateConfiguredParam("wfsWaitInterval_ms", "2");
    }

    @Test
    @InSequence(4)
    @OperateOnDeployment(SHM_ES_TEST)
    public void testInstallLicenseForSMRSAndIDMOutage() throws Exception {
        System.out.println("*** Test InstallLicense For SMRS And IDM Outage Started ***");
        final long activityJobPoId = ShmServiceAvailabilityTestBase.poIds.get(ShmServiceAvailabilityTestBase.activityJobPoId);
        Activity installLicenseKeyFileService = smrsIdmServiceTestFactory.getInstallLicenseKeyFileService();
        final Map<String, Object> modifiedAttributeMap = new HashMap<String, Object>();

        System.out.println("New thread for deployment of SMRS and IDM Service Started!!!");
        new Thread(new TriggerService(activityJobPoId, installLicenseKeyFileService)).start();
        System.out.println("Service triggered!!!");
        System.out.println("Sleep time started!!!");
        Thread.sleep(6000);
        System.out.println("Sleep time completed!!!");
        this.deployer.deploy("smrsidm-test");
        System.out.println("Deployed the SMRS and IDM Service in another thread!");

        modifiedAttributeMap.put(LicensingActivityConstants.LAST_LICENSING_PI_CHANGE, "141022_131638");
        availabilityDataBean.updateAttributes(availabilityDataBean.getLicensingMOFdn(), modifiedAttributeMap);
        final ActivityStepResult timedOutResult = installLicenseKeyFileService.handleTimeout(activityJobPoId);

        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, timedOutResult.getActivityResultEnum());
        Assert.assertNull((String) availabilityDataBean.getJobAttribute(activityJobPoId, ShmConstants.RESULT, false));
        System.out.println("*** Test InstallLicense TimeOut Ended ***");
    }

    @Test
    @InSequence(5)
    @OperateOnDeployment(SHM_ES_TEST)
    public void testDPSoutageForLicenseInstallJob() throws Exception {
        System.out.println("*** Test InstallLicense For DPS Outage Started ***");
        final long activityJobPoId = ShmServiceAvailabilityTestBase.poIds.get(ShmServiceAvailabilityTestBase.activityJobPoId);
        Activity installLicenseKeyFileService = smrsIdmServiceTestFactory.getInstallLicenseKeyFileService();

        System.out.println("=========Making versant down========");
        executeCmds(VERSANT_STOP_DB);
        executeCmds(VERSANT_UNSTARTABLE_MODE);
        System.out.println("=========Waiting for versant to go down========");
        Thread.sleep(20000);

        new Thread(new TriggerService(activityJobPoId, installLicenseKeyFileService)).start();
        Thread.sleep(50000);
        executeCmds(VERSANT_MULTIUSER_MODE);

        final ActivityStepResult response = installLicenseKeyFileService.handleTimeout(activityJobPoId);
        System.out.println("DPS retry result ..... " + response.getActivityResultEnum());
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, response.getActivityResultEnum());
    }

    @Test
    @InSequence(7)
    @OperateOnDeployment(SHM_ES_TEST)
    public void delete() {
        System.out.println("*** Cleaning up data start *** ");
        executeCmds(VERSANT_MULTIUSER_MODE);
        availabilityDataBean.deleteNodeMOTestData();
        availabilityDataBean.cleanUpTestJobsData();
        System.out.println("*** Cleaning up data end ***");
    }

    @InSequence(8)
    @Test
    public void undeployPib() throws Exception {
        this.deployer.undeploy(PIB);
    }

    class TriggerService implements Runnable {
        long activityJobPoId;
        Activity installLicenseKeyFileService;

        public TriggerService(long activityJobPoId, Activity installLicenseKeyFileService) {
            this.activityJobPoId = activityJobPoId;
            this.installLicenseKeyFileService = installLicenseKeyFileService;
        }

        @Override
        public void run() {
            try {
                final ActivityStepResult activityResultForPrecheck = installLicenseKeyFileService.precheck(activityJobPoId);
                assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, activityResultForPrecheck.getActivityResultEnum());
                installLicenseKeyFileService.execute(activityJobPoId);
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    private void executeCmds(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec("cmd /c " + cmd);
            BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while (true) {
                line = r.readLine();
                if (line == null) {
                    break;
                }
                System.out.println(line);
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}