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
package com.ericsson.oss.services.shm.test.jobs;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.shm.jobs.common.api.JobReportData;
import com.ericsson.oss.services.shm.jobs.common.modelentities.NEInfo;
import com.ericsson.oss.services.shm.jobs.common.restentities.RestJobConfiguration;
import com.ericsson.oss.services.shm.test.common.DataPersistenceServiceProxy;
import com.ericsson.oss.services.shm.test.common.DataPersistenceServiceProxyBean;
import com.ericsson.oss.services.shm.test.common.IntegrationTestDependencies;
import com.ericsson.oss.services.shm.test.common.IntegrationTestDeploymentFactoryBase;
import com.ericsson.oss.services.shm.test.elementaryservices.WorkflowInstanceServiceLocalImplMock;

@RunWith(Arquillian.class)
public class JobsServiceTestIT {
    private static final String SERVICE_FRAMEWORK_DIST = "com.ericsson.oss.itpf.sdk:service-framework-dist";
    private static final String CPPINVENTORYSYNCHSERVICE_EAR = "com.ericsson.oss.services.shm:cppinventorysynchservice-ear";
    public static final String ACTIVITY_INFORMATION_JAR = "com.ericsson.oss.services.shm:activity-information";

    private static final String CPP_JOBS_SERVICE_TEST = "jobsTest";
    private static String JOB_CONFIG_URL = "/oss/shm/rest/job/jobconfiguration/";
    private static String NE_LEVEL_JOBS_URL = "/oss/shm/rest/job/jobdetails?";
    private static String DELETE_JOBS_URL = "/oss/shm/rest/job/delete";
    private static String JOB_LOGS_URL = "/oss/shm/rest/job/joblog";
    private static String CREATE_JOB_URL = "/oss/shm/rest/job";
    private static String ADD_JOB_COMMENT_URL = "/oss/shm/rest/job/comment";
    public static final String ORG_JBOSS_RESTEASY = "org.jboss.resteasy:resteasy-jaxrs";
    private static final String WFS_API = "com.ericsson.oss.services.wfs:wfs-api";
    private static final String WFS_JEE_LOCAL_API = "com.ericsson.oss.services.wfs:wfs-jee-local-api";
    public static final File MANIFEST_MF_FILE = new File("src/test/resources/META-INF/MANIFEST.MF");
    public static final File BEANS_XML_FILE = new File("src/test/resources/META-INF/beans.xml");
    private final long nonExistingJobTemplateId = 12345L;
    private final long nonExistingNeJobId = 12345L;
    private long poid;
    private String response1;
    private String response2;

    @Inject
    JobsCreatorTestBase testJobs;

    @Inject
    DataPersistenceServiceProxy dps;

    @ArquillianResource
    private Deployer deployer;

    private static final Logger LOGGER = LoggerFactory.getLogger(JobsServiceTestIT.class);

    @Deployment(managed = false, name = "CppSynchService", testable = false)
    public static EnterpriseArchive createCppSynchServiceDeployment() {
        return ShrinkWrap.createFromZipFile(EnterpriseArchive.class, resolveAsFilesForEar(CPPINVENTORYSYNCHSERVICE_EAR)[0]);
    }

    @Deployment(managed = false, name = "SHMService", testable = false)
    public static EnterpriseArchive createSHMServiceDeployment() {
        return (EnterpriseArchive) createSHMServiceEar();
    }

    private static File[] resolveAsFilesForEar(final String artifactCoordinates) {
        return Maven.resolver().loadPomFromFile("pom.xml").resolve(artifactCoordinates + ":ear:?").withoutTransitivity().asFile();
    }

    @Deployment(managed = true, name = CPP_JOBS_SERVICE_TEST, testable = true)
    public static WebArchive createTestWar_license() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "Cpp_Jobs_Service_test.war");
        war.addPackage(JobsServiceTestIT.class.getPackage());
        war.addAsManifestResource(IntegrationTestDeploymentFactoryBase.MANIFEST_MF_FILE);
        war.addClass(DataPersistenceServiceProxy.class);
        war.addClass(DataPersistenceServiceProxyBean.class);
        war.addClass(NEInfo.class);
        war.addPackage(JobReportData.class.getPackage());
        war.addPackage(RestJobConfiguration.class.getPackage());
        war.addAsLibraries(IntegrationTestDependencies.resolveArtifactWithoutDependencies(SERVICE_FRAMEWORK_DIST));
        war.addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve(ORG_JBOSS_RESTEASY).withTransitivity().asFile());
        war.addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve(ACTIVITY_INFORMATION_JAR).withTransitivity().asFile());
        war.addAsWebInfResource(new File("src/test/resources/META-INF/beans.xml"), "beans.xml");
        return war;
    }

    @InSequence(1)
    @Test
    public void cleanupTestDataAtBegining() throws Throwable {

        int deletedPOs = testJobs.cleanUpTestData();
        LOGGER.info("Deleted PO's : {} ", deletedPOs);
    }

    @InSequence(2)
    @Test
    public void deploySHMService() throws Exception {
        this.deployer.deploy("SHMService");
    }

    @InSequence(3)
    @Test
    public void deployCppsynchservice() throws Exception {
        this.deployer.deploy("CppSynchService");
    }

    @OperateOnDeployment(CPP_JOBS_SERVICE_TEST)
    @Test
    @InSequence(4)
    public void testActivities() throws Exception {
        final String url = System.getProperty("host") + System.getProperty("port") + "/oss/shm/rest/jobs/activityData/";
        String request = "[{\"platform\":\"ECIM\",\"jobType\":\"backup\",\"neTypes\":[{\"neType\":\"MME\", \"neFdns\":[\"NetworkElement=node3\",\"NetworkElement=node4\"]}]}, {\"platform\":\"CPP\",\"jobType\":\"backup\",\"neTypes\":[{\"neType\":\"\",\"neFdns\":[\"NetworkElement=node1\",\"NetworkElement=node2\"]}]}]";
        final ClientResponse<String> clientResponse = sendPOSTRequest(url, String.class, request);
        System.out.println(" Job Activity Data call(jobs/activityData/) response::  " + clientResponse.getEntity());
    }

    @OperateOnDeployment(CPP_JOBS_SERVICE_TEST)
    @Test
    @InSequence(4)
    public void retrieveJobsConfigurationData_success() throws Exception {
        final String url = System.getProperty("host") + System.getProperty("port") + JOB_CONFIG_URL;
        LOGGER.info("test_retrieveJobsConfigurationData_success @ {}", url);
        final PersistenceObject jobTemplate = testJobs.createJobConfiguration();
        testJobs.createTempJob("CREATED", jobTemplate.getPoId());
        Assert.assertNotNull(jobTemplate);
        final ClientResponse<RestJobConfiguration> clientResponse = sendGETRequest(url + jobTemplate.getPoId(), RestJobConfiguration.class);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), clientResponse.getResponseStatus().getStatusCode());
        final RestJobConfiguration response = clientResponse.getEntity();
        LOGGER.info("...Recieved Response is...{}", response);
        Assert.assertNotNull(response);
        Assert.assertEquals("jobName", response.getJobName());
        Assert.assertEquals("UPGRADE", response.getJobType());
        Assert.assertEquals("Immediate", response.getMode());
        Assert.assertEquals(1, response.getJobParams().size());
    }

    @OperateOnDeployment(CPP_JOBS_SERVICE_TEST)
    @Test
    @InSequence(5)
    public void retrieveJobsConfigurationData_notSuccess() throws Exception {
        final String url = System.getProperty("host") + System.getProperty("port") + JOB_CONFIG_URL;
        final ClientResponse<String> clientResponse = sendGETRequest(url + nonExistingJobTemplateId, String.class);
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), clientResponse.getResponseStatus().getStatusCode());
        final String response = clientResponse.getEntity();
        LOGGER.info("...Recieved Response is...{}", response);
        Assert.assertEquals("No Data Found for selected Job", response);
    }

    @OperateOnDeployment(CPP_JOBS_SERVICE_TEST)
    @Test
    @InSequence(6)
    public void retrieveNeJobs_notSuccess() throws Exception {
        final String url = System.getProperty("host") + System.getProperty("port") + NE_LEVEL_JOBS_URL;
        final String append = "jobId=123789456&offset=1&limit=10&sortBy=neNodeName&orderBy=desc";
        LOGGER.info("test_retrieveNeJobs_notSuccess @ {}", url);
        final ClientResponse<String> clientResponse = sendGETRequest(url + append, String.class);
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), clientResponse.getResponseStatus().getStatusCode());
        final String response = clientResponse.getEntity();
        LOGGER.info("...Recieved Response is...{}", response);
        Assert.assertNull(response);

    }

    @OperateOnDeployment(CPP_JOBS_SERVICE_TEST)
    @Test
    @InSequence(7)
    public void retrieveNeJobs_success() throws Exception {
        testJobs.createAllJobs("CREATED");
        Assert.assertEquals(5, JobsCreatorTestBase.poIds.size());
        final String url = System.getProperty("host") + System.getProperty("port") + NE_LEVEL_JOBS_URL;
        final String append = "jobId=" + testJobs.getMainJobId() + "&offset=1&limit=10&sortBy=neNodeName&orderBy=desc";
        LOGGER.info("test_retrieveNeJobs_notSuccess @ {}", url);
        final ClientResponse<JobReportData> clientResponse = sendGETRequest(url + append, JobReportData.class);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), clientResponse.getResponseStatus().getStatusCode());
        final JobReportData response = clientResponse.getEntity();
        LOGGER.info("...Recieved Response is...{}", response);
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getJobDetails());
        Assert.assertNotNull(response.getNeDetails());
        Assert.assertEquals("jobName", response.getJobDetails().getJobName());
        Assert.assertEquals("UPGRADE", response.getJobDetails().getJobType());
        Assert.assertEquals(1, response.getNeDetails().getTotalCount());
        Assert.assertNotNull(response.getNeDetails().getResult());
        Assert.assertEquals("Ne1", response.getNeDetails().getResult().get(0).getNeNodeName());
        Assert.assertEquals("SUCCESS", response.getNeDetails().getResult().get(0).getNeResult());
        Assert.assertNotNull(response.getNeDetails().getResult().get(0).getNeActivity());
    }

    @InSequence(8)
    @OperateOnDeployment(CPP_JOBS_SERVICE_TEST)
    @Test
    public void delete_Jobs_success() throws Exception {
        final String url = System.getProperty("host") + System.getProperty("port") + DELETE_JOBS_URL;
        testJobs.createAllJobs("COMPLETED");
        final Long mainJobId = JobsCreatorTestBase.poIds.get("mainJobPoId");
        LOGGER.info("----mainJobId------{}", mainJobId);
        final String deleteRequest = "[\"" + mainJobId + "\"]";
        final ClientResponse<String> clientResponse = sendPOSTRequest(url, String.class, deleteRequest);
        final String response = clientResponse.getEntity();
        LOGGER.info("----clientResponse------{}", response);
        Assert.assertNotNull(response);
        Assert.assertEquals(true, response.contains("1 job(s) successfully deleted."));
        Assert.assertEquals(true, response.contains("success"));

        //Checking whether the PO is deleted from the DPS
        Assert.assertNull(testJobs.checkPO());
    }

    @InSequence(9)
    @OperateOnDeployment(CPP_JOBS_SERVICE_TEST)
    @Test
    public void delete_Jobs_Fail_No_Job() throws Exception {
        final String url = System.getProperty("host") + System.getProperty("port") + DELETE_JOBS_URL;
        final String deleteRequest = "[\"" + nonExistingJobTemplateId + "\"]";
        final ClientResponse<String> clientResponse = sendPOSTRequest(url, String.class, deleteRequest);
        final String response = clientResponse.getEntity();
        LOGGER.info("----clientResponse------{}", response);
        Assert.assertNotNull(response);
        Assert.assertEquals(true, response.contains("1 job(s) not found."));
        Assert.assertEquals(true, response.contains("success"));

    }

    @InSequence(10)
    @OperateOnDeployment(CPP_JOBS_SERVICE_TEST)
    @Test
    public void delete_Jobs_Fail_Running_Job() throws Exception {
        final String url = System.getProperty("host") + System.getProperty("port") + DELETE_JOBS_URL;
        testJobs.createAllJobs("RUNNING");
        final Long mainJobId = JobsCreatorTestBase.poIds.get("mainJobPoId");
        LOGGER.info("----mainJobId------{}", mainJobId);
        final String deleteRequest = "[\"" + mainJobId + "\"]";
        final ClientResponse<String> clientResponse = sendPOSTRequest(url, String.class, deleteRequest);
        final String response = clientResponse.getEntity();
        LOGGER.info("----clientResponse------{}", response);
        Assert.assertNotNull(response);
        Assert.assertEquals(true, response.contains("1 job(s) is/are still active."));
        Assert.assertEquals(true, response.contains("error"));

        //Checking whether the PO is present in the DPS
        Assert.assertNotNull(testJobs.checkPO());

    }

    @InSequence(11)
    @OperateOnDeployment(CPP_JOBS_SERVICE_TEST)
    @Test
    public void delete_Jobs_Fail_Cancelling_Job() throws Exception {
        final String url = System.getProperty("host") + System.getProperty("port") + DELETE_JOBS_URL;
        testJobs.createAllJobs("CANCELLING");
        final Long mainJobId = JobsCreatorTestBase.poIds.get("mainJobPoId");
        LOGGER.info("----mainJobId------{}", mainJobId);
        final String deleteRequest = "[\"" + mainJobId + "\"]";
        final ClientResponse<String> clientResponse = sendPOSTRequest(url, String.class, deleteRequest);
        final String response = clientResponse.getEntity();
        LOGGER.info("----clientResponse------{}", response);
        Assert.assertNotNull(response);
        Assert.assertEquals(true, response.contains("1 job(s) is/are still active."));
        Assert.assertEquals(true, response.contains("error"));

        //Checking whether the PO is present in the DPS
        Assert.assertNotNull(testJobs.checkPO());

    }

    @InSequence(12)
    @OperateOnDeployment(CPP_JOBS_SERVICE_TEST)
    @Test
    public void delete_Jobs_Fail_Multiple_deletions() throws Exception {
        final String url = System.getProperty("host") + System.getProperty("port") + DELETE_JOBS_URL;
        final List<Map<String, Long>> listOfPoIds = testJobs.getMultipleJobList();

        final StringBuilder deleteRequest = new StringBuilder("[\"");
        final String last = "\"]";
        final String middle = "\",\"";

        final int countOfMainJobPoIds = listOfPoIds.size();
        int mainJobIdsAdded = 0;

        for (final Map<String, Long> map : listOfPoIds) {
            final String mainJobId = String.valueOf(map.get("mainJobPoId"));
            deleteRequest.append(mainJobId);
            mainJobIdsAdded++;

            if (mainJobIdsAdded == countOfMainJobPoIds) {
                deleteRequest.append(last);
            } else {
                deleteRequest.append(middle);
            }
        }

        LOGGER.info("request url :{}", deleteRequest.toString());
        final ClientResponse<String> clientResponse = sendPOSTRequest(url, String.class, deleteRequest.toString());
        final String response = clientResponse.getEntity();
        LOGGER.info("...Response is...{}", response);
        Assert.assertNotNull(response);
        Assert.assertEquals(true, response.contains("3 job(s) successfully deleted.1 job(s) is/are still active."));
        Assert.assertEquals(true, response.contains("error"));

        //Checking whether the PO is present in the DPS
        final int deletedpos = testJobs.cleanUpTestData(listOfPoIds.get(1).get("mainJobPoId"));
        Assert.assertEquals(1, deletedpos);

    }

    @OperateOnDeployment(CPP_JOBS_SERVICE_TEST)
    @Test
    @InSequence(13)
    public void add_job_comments() throws Exception {
        final String url = System.getProperty("host") + System.getProperty("port") + ADD_JOB_COMMENT_URL;
        testJobs.createAllJobs("CREATED");
        final Long mainJobId = JobsCreatorTestBase.poIds.get("mainJobPoId");
        LOGGER.info("Add job comment mainjobid : {}", mainJobId);
        final String commentInfo = "{\"comment\":\"JobTest\",\"jobId\":\"" + mainJobId + "\"}";
        LOGGER.info("Add job comment request : {}", commentInfo);
        final ClientResponse<String> clientResponse = sendPOSTRequest(url, String.class, commentInfo);
        final String response = clientResponse.getEntity();
        LOGGER.info("Add job comment response : {} : ", response);
        Assert.assertNotNull(response);
        Assert.assertEquals(true, response.contains("JobTest"));
    }

    @SuppressWarnings("static-access")
    @OperateOnDeployment(CPP_JOBS_SERVICE_TEST)
    @Test
    @InSequence(14)
    public void jobLogs_success() throws Exception {
        final String url = System.getProperty("host") + System.getProperty("port") + JOB_LOGS_URL;
        testJobs.createAllJobs("CREATED");
        final Long neJobId = testJobs.poIds.get("neJobPoId");
        Assert.assertEquals(5, testJobs.poIds.size());
        LOGGER.info("test_job_log_success @ {}", url);
        final String jobLogRequest = "{\"neJobIds\":\"" + neJobId + "\", \"orderBy\":\"asc\", \"sortBy\":\"neName\", \"offset\":\"1\", \"limit\":\"5\"}";
        System.out.println("REQUEST::::::::::: " + jobLogRequest);
        final ClientResponse<String> clientResponse = sendPOSTRequest(url, String.class, jobLogRequest);
        final String response = clientResponse.getEntity();
        LOGGER.info("...Recieved Response is...{}", response);
        Assert.assertNotNull(response);
        Assert.assertEquals(true, response.contains("install"));
        Assert.assertEquals(true, response.contains("1"));
    }

    @OperateOnDeployment(CPP_JOBS_SERVICE_TEST)
    @Test
    @InSequence(15)
    public void jobLogs_fail() throws Exception {
        final String url = System.getProperty("host") + System.getProperty("port") + JOB_LOGS_URL;
        LOGGER.info("test_job_log_not_found @ {}", url);
        final String jobLogRequest = "{\"neJobIds\":\"" + nonExistingNeJobId + "\", \"orderBy\":\"asc\", \"sortBy\":\"neName\", \"offset\":\"1\", \"limit\":\"5\"}";
        final ClientResponse<String> clientResponse = sendPOSTRequest(url, String.class, jobLogRequest);
        final String response = clientResponse.getEntity();
        LOGGER.info("...Recieved Response is...{}", response);
        Assert.assertEquals(true, response.contains("NE Name not available with POId 12345"));
    }

    @SuppressWarnings("static-access")
    @OperateOnDeployment(CPP_JOBS_SERVICE_TEST)
    @Test
    @InSequence(16)
    public void nonExistingjobLog() throws Exception {
        final String url = System.getProperty("host") + System.getProperty("port") + JOB_LOGS_URL;
        testJobs.createAllJobsWithouActivity("CREATED");
        final Long neJobId = testJobs.poIdsWithoutActivity.get("neJobPoId");
        Assert.assertEquals(3, testJobs.poIdsWithoutActivity.size());
        LOGGER.info("test_job_log_success @ {}", url);
        final String jobLogRequest = "{\"neJobIds\":\"" + neJobId + "\", \"orderBy\":\"asc\", \"sortBy\":\"neName\", \"offset\":\"1\", \"limit\":\"5\"}";
        final ClientResponse<String> clientResponse = sendPOSTRequest(url, String.class, jobLogRequest);
        final String response = clientResponse.getEntity();
        LOGGER.info("Recieved Response is {}", response);
        Assert.assertEquals(true, response.contains("PO not available in database with NE Job Id"));
    }

    @OperateOnDeployment(CPP_JOBS_SERVICE_TEST)
    @Test
    @InSequence(17)
    public void create_backup_job_success() throws Exception {
        final String url = System.getProperty("host") + System.getProperty("port") + CREATE_JOB_URL;
        // Job name should be unique
        final String jobInfo = "{\"jobType\":\"BACKUP\",\"name\":\"shm_backupjob\",\"description\":\"\",\"neNames\":[{\"name\":\"LTE01ERBS0001\"}],\"mainSchedule\":{\"scheduleAttributes\":[],\"execMode\":\"IMMEDIATE\"},\"configurations\":[{\"platform\":\"CPP\",\"properties\":[]},{\"neType\":\"ERBS\",\"properties\":[{\"key\":\"CV_NAME\",\"value\":\"erbs_name\"},{\"key\":\"CV_TYPE\",\"value\":\"TEST\"},{\"key\":\"CV_IDENTITY\",\"value\":\"idnetity_name\"}]}],\"activitySchedules\":[{\"platformType\":\"CPP\",\"value\":[{\"neType\":\"ERBS\",\"value\":[{\"activityName\":\"createcv\",\"execMode\":\"IMMEDIATE\",\"order\":1},{\"activityName\":\"setcvfirstinrollbacklist\",\"execMode\":\"IMMEDIATE\",\"order\":3},{\"activityName\":\"exportcv\",\"execMode\":\"IMMEDIATE\",\"order\":4},{\"activityName\":\"setcvasstartable\",\"execMode\":\"IMMEDIATE\",\"order\":2}]}]}]}";
        LOGGER.info("JSON for backup job persistence{}", jobInfo);
        final ClientResponse<String> clientResponse = sendPOSTRequest(url, String.class, jobInfo);
        final String response = clientResponse.getEntity();
        LOGGER.info("Response JSON for backup job persistence : ", response);
        Assert.assertNotNull(response);
        Assert.assertEquals(true, response.contains("success"));
    }

    @OperateOnDeployment(CPP_JOBS_SERVICE_TEST)
    @Test
    @InSequence(18)
    public void create_deletebackup_job_success() throws Exception {
        final String url = System.getProperty("host") + System.getProperty("port") + CREATE_JOB_URL;
        // Job name should be unique
        final String jobInfo = "{\"name\":\"deleteCV\",\"description\":\"deleteCV\",\"jobType\":\"DELETEBACKUP\",\"owner\":\"shmtest\",\"neNames\":[{\"name\":\"LTE02ERBS00001\"}],\"configurations\":[{\"neType\":\"ERBS\",\"properties\":[{\"key\":\"ROLL_BACK\",\"value\":\"TRUE\"}],\"neProperties\":[{\"neNames\":\"LTE02ERBS00001\",\"properties\":[{\"key\":\"CV_NAME\",\"value\":\"asda\"}]}]}],\"mainSchedule\":{\"scheduleAttributes\":[],\"execMode\":\"IMMEDIATE\"},\"activitySchedules\":[{\"platformType\":\"CPP\",\"value\":[{\"neType\":\"ERBS\",\"value\":[{\"activityName\":\"deletecv\",\"execMode\":\"IMMEDIATE\"}]}]}]}";
        LOGGER.info("JSON for deletebackup job persistence{}", jobInfo);
        final ClientResponse<String> clientResponse = sendPOSTRequest(url, String.class, jobInfo);
        final String response = clientResponse.getEntity();
        LOGGER.info("Response JSON for deletebackup job persistence : ", response);
        Assert.assertNotNull(response);
        Assert.assertEquals(true, response.contains("success"));
    }

    @OperateOnDeployment(CPP_JOBS_SERVICE_TEST)
    @Test
    @InSequence(19)
    public void create_restorebackup_job_success() throws Exception {
        final String url = System.getProperty("host") + System.getProperty("port") + CREATE_JOB_URL;
        // Job name should be unique
        final String jobInfo = "{\"name\":\"Restore_J2\",\"description\":\"\",\"jobType\":\"RESTORE\",\"configurations\":[{\"neType\":\"ERBS\",\"properties\":[{\"key\":\"FORCED_RESTORE\",\"value\":\"true\"},{\"key\":\"AUTO_CONFIGURATION\",\"value\":\"ON\"}],\"neProperties\":[{\"neNames\":\"LTE02ERBS00001\",\"properties\":[{\"key\":\"CV_NAME\",\"value\":\"Backup_sd2\"}]}]}],\"neNames\":[{\"name\":\"LTE02ERBS00001\"}],\"mainSchedule\":{\"scheduleAttributes\":[],\"execMode\":\"IMMEDIATE\"},\"activitySchedules\":[{\"platformType\":\"CPP\",\"value\":[{\"neType\":\"ERBS\",\"value\":[{\"activityName\":\"restore\",\"execMode\":\"IMMEDIATE\"},{\"activityName\":\"download\",\"execMode\":\"IMMEDIATE\"},{\"activityName\":\"verify\",\"execMode\":\"IMMEDIATE\"},{\"activityName\":\"install\",\"execMode\":\"IMMEDIATE\"},{\"activityName\":\"confirm\",\"execMode\":\"IMMEDIATE\"}]}]}]}";
        LOGGER.info("JSON for restorebackup job persistence{}", jobInfo);
        final ClientResponse<String> clientResponse = sendPOSTRequest(url, String.class, jobInfo);
        final String response = clientResponse.getEntity();
        LOGGER.info("Response JSON for restorebackup job persistence : ", response);
        Assert.assertNotNull(response);
        Assert.assertEquals(true, response.contains("success"));

    }

    @OperateOnDeployment(CPP_JOBS_SERVICE_TEST)
    @Test
    @InSequence(20)
    public void create_upgrade_job_success() throws Exception {
        final String url = System.getProperty("host") + System.getProperty("port") + CREATE_JOB_URL;
        // Job name should be unique
        final String jobInfo = "{\"name\":\"Upgrade Job Test\",\"description\":\"\",\"jobType\":\"UPGRADE\",\"configurations\":[{\"neType\":\"ERBS\",\"properties\":[{\"key\":\"SWP_NAME\",\"value\":\"CXP102051_1_R4D73\"},{\"key\":\"UCF\",\"value\":\"CXP1020511_R4D73.xml\"},{\"key\":\"SELECTIVEINSTALL\",\"value\":\"not selective\"},{\"key\":\"FORCEINSTALL\",\"value\":\"delta\"},{\"key\":\"REBOOTNODEUPGRADE\",\"value\":\"true\"}]}],\"packageNames\":{\"CPP\":\"CXP102051_1_R4D73\"},\"neNames\":[{\"name\":\"LTE02ERBS00001\"}],\"mainSchedule\":{\"scheduleAttributes\":[],\"execMode\":\"IMMEDIATE\"},\"activitySchedules\":[{\"platformType\":\"CPP\",\"value\":[{\"neType\":\"ERBS\",\"value\":[{\"activityName\":\"install\",\"execMode\":\"IMMEDIATE\"},{\"activityName\":\"verify\",\"execMode\":\"IMMEDIATE\"},{\"activityName\":\"upgrade\",\"execMode\":\"IMMEDIATE\"},{\"activityName\":\"confirm\",\"execMode\":\"IMMEDIATE\"}]}]}]}";
        LOGGER.info("JSON for upgrade job persistence{}", jobInfo);
        final ClientResponse<String> clientResponse = sendPOSTRequest(url, String.class, jobInfo);
        final String response = clientResponse.getEntity();
        LOGGER.info("Response JSON for upgrade job persistence : ", response);
        Assert.assertNotNull(response);
        Assert.assertEquals(true, response.contains("success"));

    }

    @OperateOnDeployment(CPP_JOBS_SERVICE_TEST)
    @Test
    @InSequence(21)
    public void create_lincense_job_success() throws Exception {
        final String url = System.getProperty("host") + System.getProperty("port") + CREATE_JOB_URL;
        // Job name should be unique
        final String jobInfo = "{\"name\":\"License Job Test\",\"description\":\"\",\"jobType\":\"LICENSE\",\"owner\":\"shmtest\",\"configurations\":[{\"neType\":\"ERBS\",\"neProperties\":[{\"properties\":[{\"key\":\"LICENSE_FILEPATH\",\"value\":\"/home/smrs/lran/licence/erbs/LTE02ERBS00001_fp_150617_152109.xml\"}],\"neNames\":\"LTE02ERBS00001\"}]}],\"neNames\":[{\"name\":\"LTE02ERBS00001\"}],\"mainSchedule\":{\"scheduleAttributes\":[],\"execMode\":\"IMMEDIATE\"},\"activitySchedules\":[{\"platformType\":\"CPP\",\"value\":[{\"neType\":\"ERBS\",\"value\":[{\"activityName\":\"install\",\"execMode\":\"IMMEDIATE\"}]}]}]}";
        LOGGER.info("JSON for job persistence{}", jobInfo);
        final ClientResponse<String> clientResponse = sendPOSTRequest(url, String.class, jobInfo);
        final String response = clientResponse.getEntity();
        LOGGER.info("Response JSON for job persistence : ", response);
        Assert.assertNotNull(response);
        Assert.assertEquals(true, response.contains("success"));
    }

    @OperateOnDeployment(CPP_JOBS_SERVICE_TEST)
    @Test
    @InSequence(22)
    public void create_2nd_job_failure() throws Exception {
        final String url = System.getProperty("host") + System.getProperty("port") + CREATE_JOB_URL;
        // Job Name should be Unique
        final String jobInfo = "{\"jobType\":\"BACKUP\",\"name\":\"shm_backupjob\",\"description\":\"\",\"neNames\":[{\"name\":\"LTE01ERBS0001\"}],\"mainSchedule\":{\"scheduleAttributes\":[],\"execMode\":\"IMMEDIATE\"},\"configurations\":[{\"platform\":\"CPP\",\"properties\":[]},{\"neType\":\"ERBS\",\"properties\":[{\"key\":\"CV_NAME\",\"value\":\"erbs_name\"},{\"key\":\"CV_TYPE\",\"value\":\"TEST\"},{\"key\":\"CV_IDENTITY\",\"value\":\"idnetity_name\"}]}],\"activitySchedules\":[{\"platformType\":\"CPP\",\"value\":[{\"neType\":\"ERBS\",\"value\":[{\"activityName\":\"createcv\",\"execMode\":\"IMMEDIATE\",\"order\":1},{\"activityName\":\"setcvfirstinrollbacklist\",\"execMode\":\"IMMEDIATE\",\"order\":3},{\"activityName\":\"exportcv\",\"execMode\":\"IMMEDIATE\",\"order\":4},{\"activityName\":\"setcvasstartable\",\"execMode\":\"IMMEDIATE\",\"order\":2}]}]}]}";
        LOGGER.info("JSON for job persistence{}", jobInfo);
        final ClientResponse<String> clientResponse = sendPOSTRequest(url, String.class, jobInfo);
        final String response = clientResponse.getEntity();
        LOGGER.info("Response JSON for job persistence : {}", response);
        Assert.assertNotNull(response);
        Assert.assertEquals(true, response.contains("\"shm_backupjob\" failed"));
        final int deletedPOs = testJobs.cleanUpTestData(poid);
        LOGGER.info("Deleted PO's : {} ", deletedPOs);

    }

    @OperateOnDeployment(CPP_JOBS_SERVICE_TEST)
    @Test
    @InSequence(23)
    public void testTwoJobsSimiltaneously() throws Exception {
        Runnable myRunnable = new Runnable() {
            public void run() {
                System.out.println("Runnable running");
                final String url = System.getProperty("host") + System.getProperty("port") + CREATE_JOB_URL;
                // Job Name should be Unique
                final String jobInfo = "{\"jobType\":\"BACKUP\",\"name\":\"BackupJobPersistTest110\",\"description\":\"BackupTestDescription\",\"neNames\":[{\"name\":\"LTE01ERBS0001\"}],\"mainSchedule\":{\"scheduleAttributes\":[],\"execMode\":\"IMMEDIATE\"},\"configurations\":[{\"platform\":\"CPP\",\"properties\":[{\"key\":\"CV_NAME\",\"value\":\"BackupFile\"},{\"key\":\"CV_TYPE\",\"value\":\"Standard\"},{\"key\":\"CV_IDENTITY\",\"value\":\"\"}]}],\"activitySchedules\":[{\"PlatformType\":\"CPP\",\"value\":[{\"activityName\":\"setcvasstartable\",\"execMode\":\"IMMEDIATE\"},{\"activityName\":\"setcvfirstinrollbacklist\",\"execMode\":\"IMMEDIATE\"},{\"activityName\":\"exportcv\",\"execMode\":\"IMMEDIATE\"},{\"activityName\":\"createcv\",\"execMode\":\"IMMEDIATE\"}]}]}";
                LOGGER.info("JSON for job persistence{}", jobInfo);
                ClientResponse<String> clientResponse = null;
                try {
                    clientResponse = sendPOSTRequest(url, String.class, jobInfo);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                Thread t = Thread.currentThread();
                if ("thread1".equals(t.getName())) {
                    response1 = clientResponse.getEntity();
                } else {
                    response2 = clientResponse.getEntity();
                }
            }
        };
        Thread thread1 = new Thread(myRunnable);
        thread1.setName("thread1");
        Thread thread2 = new Thread(myRunnable);
        thread2.setName("thread2");
        thread1.start();
        thread2.start();
        Thread.sleep(30000);
        Assert.assertNotNull(response1);
        Assert.assertNotNull(response2);
        LOGGER.info("Response for thread 1 : {} ", response1);
        LOGGER.info("Response for thread 2 : {} ", response2);
        if (response1.contains("\"BackupJobPersistTest110\" failed")) {
            Assert.assertEquals(true, response2.contains("success"));
        } else if (response1.contains("success")) {
            Assert.assertEquals(true, (response2.contains("\"BackupJobPersistTest110\" failed")));
        }

    }

    @InSequence(24)
    @Test
    public void undeployCppSynchService() {
        this.deployer.undeploy("CppSynchService");
    }

    @InSequence(25)
    @Test
    public void undeploySHMService() {
        this.deployer.undeploy("SHMService");
    }

    @InSequence(26)
    @Test
    public void cleanupTestDataAtEND() throws Throwable {

        int deletedPOs = testJobs.cleanUpTestData();
        LOGGER.info("Deleted PO's : {} ", deletedPOs);
    }

    private <T> ClientResponse<T> sendGETRequest(final String url, final Class<T> responseObject) throws Exception {
        // Define the API URI where API will be accessed
        final ClientRequest request = new ClientRequest(url);
        // Set the accept header to tell the accepted response format
        request.accept("application/json");
        // RESTEasy client automatically converts the response to desired
        // objects.
        final ClientResponse<T> response = request.get(responseObject);
        return response;
    }

    private <T> ClientResponse<T> sendPOSTRequest(final String url, final Class<T> responseObject, final String input) throws Exception {
        // Define the API URI where API will be accessed
        final ClientRequest request = new ClientRequest(url);
        // Set the accept header to tell the accepted response format
        request.accept("application/json");
        request.header("X-Tor-UserID", "shmtest");
        request.body("application/json", input);
        // RESTEasy client automatically converts the response to desired
        // objects.
        final ClientResponse<T> response = request.post(responseObject);
        return response;
    }

    private static File[] resolveFilesWithTransitiveDependencies(final String artifactCoordinates) {
        return Maven.resolver().loadPomFromFile("pom.xml").resolve(artifactCoordinates).withTransitivity().asFile();
    }

    public static Archive<?> createSHMServiceEar() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "shm-es-1.ear");
        ear.addAsModule(createWFSMockModule());
        ear.addAsLibraries(resolveFilesWithTransitiveDependencies(WFS_API));
        ear.addAsLibraries(resolveFilesWithTransitiveDependencies(WFS_JEE_LOCAL_API));
        ear.setManifest(MANIFEST_MF_FILE);
        ear.addAsApplicationResource(BEANS_XML_FILE);
        ear.addAsLibraries(IntegrationTestDependencies.resolveArtifactWithoutDependencies(SERVICE_FRAMEWORK_DIST));
        return ear;
    }

    private static Archive<?> createWFSMockModule() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "wfs-mock-lib.jar").addAsResource("META-INF/beans.xml", "META-INF/beans.xml")
                .addAsResource("ServiceFrameworkConfiguration.properties", "ServiceFrameworkConfiguration.properties").addAsManifestResource(MANIFEST_MF_FILE)
                .addClasses(WorkflowInstanceServiceLocalImplMock.class);
        return archive;
    }
}
