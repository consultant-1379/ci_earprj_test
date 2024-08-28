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

import java.util.Date;

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

import com.ericsson.oss.services.shm.jobexecutorremote.JobExecutorRemote;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;

/**
 * Arquillian Test file to test the JobExecution remote service,
 * 
 * @author xrajeke
 * 
 */
@RunWith(Arquillian.class)
@ApplicationScoped
public class ShmJobExecutionServiceAvailabilityTestIT extends ConfigParamUpdater {

    private static final String SHM_ES_TEST = "shm-es-test";
    private String wfsId = "123456789";

    @ArquillianResource
    private Deployer deployer;

    @Inject
    ShmServicesProviderFactory smrsIdmServiceTestFactory;

    @Inject
    private ShmServiceAvailabilityTestBase availabilityDataBean;

    @Deployment(name = SHM_ES_TEST, managed = false)
    public static Archive<?> createADeployableSHMESWAR() {
        final WebArchive war = ShmServiceAvailabilityTestDeployment.createTestDeploymentForES();
        war.addClass(ShmJobExecutionServiceAvailabilityTestIT.class);
        war.addClass(ShmServicesProviderFactory.class);
        war.addClass(ConfigParamUpdater.class);
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
    @InSequence(6)
    @OperateOnDeployment(SHM_ES_TEST)
    public void testPrepareMainJobWhenItsAlreadyExists() throws Throwable {
        System.out.println("========testPrepareMainJobWhenItsAlreadyExists started==========");
        JobExecutorRemote serviceTobeTested = smrsIdmServiceTestFactory.getJobExecutionService();
        Long jobTemplateId = ShmServiceAvailabilityTestBase.poIds.get(ShmServiceAvailabilityTestBase.jobTemplatePoId);
        long createdJobID = serviceTobeTested.prepareMainJob(wfsId, jobTemplateId, new Date());
        Assert.assertEquals(-1, createdJobID);
        System.out.println("========testPrepareMainJobWhenItsAlreadyExists ended==========");

    }

    @Test
    @InSequence(7)
    @OperateOnDeployment(SHM_ES_TEST)
    public void testExecuteMainJob_When1NeJobsexists1Notexists() throws Throwable {
        System.out.println("========testExecuteMainJobWhenItsAlreadyExists started==========");
        Long mainJobId = ShmServiceAvailabilityTestBase.poIds.get(ShmServiceAvailabilityTestBase.mainJobPoId);
        JobExecutorRemote serviceTobeTested = smrsIdmServiceTestFactory.getJobExecutionService();

        serviceTobeTested.execute(wfsId, mainJobId);
        Thread.sleep(60000);
        //Check the state of the JOB , AND IT SHUD BE SUCCESS IN THIS CASE
        //since 1 job is already running, 1 job is not yet submitted to wfs, so it will not be in failed state
        Assert.assertEquals(JobState.RUNNING.name(), availabilityDataBean.getJobAttribute(mainJobId, ShmConstants.STATE, true));
        Assert.assertNull((String) availabilityDataBean.getJobAttribute(mainJobId, ShmConstants.RESULT, false));
        Assert.assertNull(availabilityDataBean.getJobAttribute(mainJobId, ShmConstants.ENDTIME, false));
        System.out.println("========testExecuteMainJobWhenItsAlreadyExists ended==========");
    }

    @Test
    @InSequence(8)
    @OperateOnDeployment(SHM_ES_TEST)
    public void delete() {
        System.out.println("*** Cleaning up data start *** ");
        availabilityDataBean.deleteNodeMOTestData();
        availabilityDataBean.cleanUpTestJobsData();
        System.out.println("*** Cleaning up data end ***");
    }

    @InSequence(9)
    @Test
    public void undeployPib() throws Exception {
        this.deployer.undeploy(PIB);
    }

}