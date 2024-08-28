/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2014
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.test.enmupgrade;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.*;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.*;
import com.ericsson.oss.itpf.sdk.eventbus.Channel;
import com.ericsson.oss.itpf.sdk.eventbus.EventPropertiesBuilder;
import com.ericsson.oss.itpf.sdk.eventbus.annotation.Endpoint;
import com.ericsson.oss.services.shm.notifications.api.*;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.test.common.*;

//@RunWith(Arquillian.class)
public class CppSynchServiceUpgradeTestIT {
    private static final String CPPINVENTORYSYNCHSERVICE_EAR = "com.ericsson.oss.services.shm:cppinventorysynchservice-ear";
    public static final String SDK_DIST = "com.ericsson.oss.itpf.sdk:service-framework-dist";
    public static final String SHM_MODELS_JAR = "com.ericsson.oss.services.shm.models:shm-models-jar";
    public static final String COM_ERICSSON_OSS_ITPF_COMMON__PLATFORMINTEGRARIONBRIDGE_EAR = "com.ericsson.oss.itpf.common:PlatformIntegrationBridge-ear";

    private static final int MAX_WAIT_TIME = 70000;
    private static final String RESPONSE = "CppInventorySynchEventListener Instance is ready for Upgrade";
    private static final CountDownLatch latch = new CountDownLatch(1);
    private static final String CPP_SYNCHSERVICE_UPGRADE_TEST_WAR = "CPP_SYNCHSERVICE_UPGRADE_TEST_WAR";
    private static final String CPP_SYNCHSERVICE_EAR = "CPP_SYNCHSERVICE_EAR";

    private static final String PIB = "PIB";
    private static final Logger LOGGER = LoggerFactory.getLogger(CppSynchServiceUpgradeTestIT.class);

    @ArquillianResource
    private Deployer deployer;

    @Inject
    MockCacheRegister mockCacheRegister;

    @Inject
    @Endpoint("jms:/queue/shmNotificationQueue")
    private Channel channel;

    private String returnVal = "";

    private static String getVersion(String key) {
        Properties properties = new Properties();
        try {
            properties.load(CppSynchServiceUpgradeTestIT.class.getResourceAsStream("/version.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties.getProperty(key);

    }

    //@Deployment(managed = false, name = CPP_SYNCHSERVICE_EAR, testable = false)
    public static EnterpriseArchive createCppSynchServiceDeployment() {
        return ShrinkWrap.createFromZipFile(EnterpriseArchive.class, resolveAsFilesForEar(CPPINVENTORYSYNCHSERVICE_EAR, getVersion("project.version"))[0]);
    }

    //@Deployment(managed = false, name = PIB, testable = false)
    public static EnterpriseArchive createPibDeployment() {
        return ShrinkWrap.createFromZipFile(EnterpriseArchive.class, resolveAsFilesForEar(COM_ERICSSON_OSS_ITPF_COMMON__PLATFORMINTEGRARIONBRIDGE_EAR, getVersion("pib.version"))[0]);
    }

    private static File[] resolveAsFiles(final String artifactCoordinates) {
        return Maven.resolver().loadPomFromFile("pom.xml").resolve(artifactCoordinates).withTransitivity().asFile();
    }

    private static File[] resolveAsFilesForEar(final String artifactCoordinates, final String version) {
        return Maven.resolver().loadPomFromFile("pom.xml").resolve(artifactCoordinates + ":ear:" + version).withoutTransitivity().asFile();
    }

    //@Deployment(managed = false, name = CPP_SYNCHSERVICE_UPGRADE_TEST_WAR, testable = true)
    public static WebArchive createTestWar() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "test_EventClientUpgrade.war");
        war.addPackage(CppSynchServiceUpgradeTestIT.class.getPackage());
        war.addClass(DataPersistenceServiceProxy.class);
        war.addClass(DataPersistenceServiceProxyBean.class);
        war.addClass(FdnNotificationSubject.class);
        war.addClass(NotificationCallback.class);
        war.addClass(Notification.class);
        war.addClass(NotificationCallbackResult.class);

        war.setManifest(IntegrationTestDeploymentFactoryBase.MANIFEST_MF_FILE);
        war.addAsWebInfResource(IntegrationTestDeploymentFactoryBase.BEANS_XML_FILE);
        war.addAsResource("ServiceFrameworkConfiguration.properties", "ServiceFrameworkConfiguration.properties");
        war.addAsLibraries(resolveAsFiles(SDK_DIST));
        war.addAsLibraries(resolveAsFiles(SHM_MODELS_JAR));
        return war;
    }

    @InSequence(1)
    //@Test
    public void deployTestWar() throws Exception {
        this.deployer.deploy(CPP_SYNCHSERVICE_UPGRADE_TEST_WAR);
    }

    @InSequence(2)
    //@Test
    public void deployCppSynchService() {
        this.deployer.deploy(CPP_SYNCHSERVICE_EAR);
    }

    @InSequence(3)
    //@Test
    public void deployPib() throws Exception {
        this.deployer.deploy(PIB);
    }

    @OperateOnDeployment(CPP_SYNCHSERVICE_UPGRADE_TEST_WAR)
    //@Test
    @InSequence(5)
    public void test_sendUpgradeEvent_toCppSynchService() throws Exception {
        LOGGER.info("trying to send events on channel=={}", channel);
        mockCacheRegister.registerToCacheAndCreateJobs();

        new Thread(new GenerateEvents("DPS")).start();
        Thread.sleep(5000);
        new Thread(new GenerateEvents("UPGRADE")).start();

        LOGGER.info("Waiting for {} milliseconds to send and recieve the upgrade accepted event", MAX_WAIT_TIME + 50000);
        // It is required to wait here , since upgrade event sending and for getting the response may take more than a minute.
        Thread.sleep(MAX_WAIT_TIME + 50000);

        mockCacheRegister.deletePos();
        Assert.assertTrue(returnVal + " {should contain} " + RESPONSE, returnVal.contains(RESPONSE));
    }

    private void sendDpsAttributeChangedEvents(String namespace, String type, int neName, Long poId) {
        final DpsAttributeChangedEvent attrChangedreatedEvent = new DpsAttributeChangedEvent();
        attrChangedreatedEvent.setFdn("MeContext=1,NetworkElement=" + neName);
        attrChangedreatedEvent.setNamespace(namespace);
        attrChangedreatedEvent.setType(type);
        attrChangedreatedEvent.setVersion("1.0.0");
        attrChangedreatedEvent.setBucketName("live");
        attrChangedreatedEvent.setPoId(poId);
        Set<AttributeChangeData> attributesSet = new HashSet<AttributeChangeData>();
        AttributeChangeData changeData = new AttributeChangeData();
        changeData.setName("active");
        changeData.setNewValue("true");
        changeData.setOldValue("false");
        attributesSet.add(changeData);
        attrChangedreatedEvent.setChangedAttributes(attributesSet);
        LOGGER.info("Sending event == {}", attrChangedreatedEvent);
        this.channel.send(attrChangedreatedEvent, generateBaseEventPropertiesFilter(attrChangedreatedEvent));
    }

    private EventPropertiesBuilder generateBaseEventPropertiesFilter(final DpsDataChangedEvent event) {
        return new EventPropertiesBuilder().addEventProperty("namespace", event.getNamespace()).addEventProperty("type", event.getType()).addEventProperty("version", event.getVersion())
                .addEventProperty("bucket", event.getBucketName());
    }

    private String sendUpgradeEventAndGetResponse() throws MalformedURLException, IOException, ProtocolException, InterruptedException {
        String returnVal = "";
        final URL url = new URL(generateRestURL());
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        LOGGER.info("...Http upgrade request sent and Response is ....{}", conn.getResponseCode());
        if (conn.getResponseCode() != 200) {
            returnVal = "Failed : HTTP error code : " + conn.getResponseCode();
        }
        final BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        String output;
        while ((output = br.readLine()) != null) {
            returnVal += output;
        }
        LOGGER.info("Upgrade id : {}", returnVal);
        conn.disconnect();

        latch.await(MAX_WAIT_TIME, TimeUnit.MILLISECONDS);

        final URL urlresp = new URL(generateRestURLForResponse(returnVal));
        final HttpURLConnection connResp = (HttpURLConnection) urlresp.openConnection();
        connResp.setRequestMethod("GET");
        LOGGER.info("....Retreiving upgrade event Response from service and response code is ....{}", connResp.getResponseCode());
        if (connResp.getResponseCode() != 200) {
            returnVal = "Failed : HTTP error code : " + connResp.getResponseCode();
        }
        final BufferedReader br1 = new BufferedReader(new InputStreamReader((connResp.getInputStream())));
        while ((output = br1.readLine()) != null) {
            returnVal += output;
        }
        LOGGER.info("Upgrade Response : {}", returnVal);
        connResp.disconnect();
        return returnVal;
    }

    @InSequence(6)
    //@Test
    public void undeployCppSynchService() {
        this.deployer.undeploy(CPP_SYNCHSERVICE_EAR);
    }

    @InSequence(7)
    //@Test
    public void undeployPib() throws Exception {
        this.deployer.undeploy(PIB);
    }

    @InSequence(8)
    //@Test
    public void undeployTestWar() throws Exception {
        this.deployer.undeploy(CPP_SYNCHSERVICE_UPGRADE_TEST_WAR);
    }

    private String generateRestURL() {
        final String instanceId = System.getProperty("com.ericsson.oss.sdk.node.identifier");
        final String portOffset = System.getProperty("jboss.socket.binding.port-offset");
        final Integer port = new Integer(portOffset) + 8080;
        final StringBuilder triggerUrl = new StringBuilder();
        triggerUrl.append("http://localhost:");
        triggerUrl.append(port.toString());
        triggerUrl.append("/pib/upgradeService/startUpgrade?app_server_identifier=");
        triggerUrl.append(instanceId);
        triggerUrl.append("&service_identifier=cppinventorysynchservice&upgrade_operation_type=service&upgrade_phase=SERVICE_INSTANCE_UPGRADE_PREPARE");
        LOGGER.info("URL constructed: " + triggerUrl.toString());
        return triggerUrl.toString();
    }

    private String generateRestURLForResponse(final String upgradeId) {
        final String portOffset = System.getProperty("jboss.socket.binding.port-offset");
        final Integer port = new Integer(portOffset) + 8080;
        final StringBuilder triggerUrl = new StringBuilder();
        triggerUrl.append("http://localhost:");
        triggerUrl.append(port.toString());
        triggerUrl.append("/pib/upgradeService/getUpgradeResponse?id=" + upgradeId);
        return triggerUrl.toString();
    }

    class GenerateEvents implements Runnable {

        String type;

        public GenerateEvents(String type) {
            this.type = type;
        }

        @Override
        public void run() {
            try {
                switch (type) {
                case "DPS":
                    LOGGER.info(".......sending DPS events ");
                    sendDpsAttributeChangedEvents("ERBS_NODE_MODEL", "ConfigurationVersion", 1, MockCacheRegister.poIds.get(MockCacheRegister.activityJobPoId));
                    Thread.sleep(2000);
                    sendDpsAttributeChangedEvents("ERBS_NODE_MODEL", "Licensing", 1, MockCacheRegister.poIds.get(MockCacheRegister.activityJobPoId));
                    Thread.sleep(2000);
                    sendDpsObjectCreatedEvent("OSS_NE_SHM_DEF", "SHMFunction", 5, MockCacheRegister.poIds.get(MockCacheRegister.activityJobPoId));
                    Thread.sleep(2000);
                    sendDpsAttributeChangedEvents("ERBS_NODE_MODEL", "Equipment", 2, MockCacheRegister.poIds.get(MockCacheRegister.activityJobPoId));
                    Thread.sleep(20000);
                    sendDpsAttributeChangedEvents("OSS_NE_CM_DEF", "CmFunction", 2, MockCacheRegister.poIds.get(MockCacheRegister.activityJobPoId));
                    Thread.sleep(20000);
                    sendDpsAttributeChangedEvents("ERBS_NODE_MODEL", "Licensing", 3, MockCacheRegister.poIds.get(MockCacheRegister.activityJobPoId));
                    Thread.sleep(5000);
                    sendDpsAttributeChangedEvents("ERBS_NODE_MODEL", "Licensing", 4, MockCacheRegister.poIds.get(MockCacheRegister.activityJobPoId));
                    Thread.sleep(5000);
                    sendDpsAttributeChangedEvents("OSS_NE_CM_DEF", "CmFunction", 2, MockCacheRegister.poIds.get(MockCacheRegister.activityJobPoId));
                    break;
                case "UPGRADE":
                    LOGGER.info(".......sending UPGRADE events ");
                    returnVal = sendUpgradeEventAndGetResponse();
                    break;
                }
            } catch (Exception e) {
                LOGGER.error(" Event generation failed due to ::{}", e);
            }

        }
    }

    private void sendDpsObjectCreatedEvent(String namespace, String type, int neName, Long poId) {
        final DpsObjectCreatedEvent objectCreatedEvent = new DpsObjectCreatedEvent();
        objectCreatedEvent.setFdn("MeContext=1,NetworkElement=" + neName);
        objectCreatedEvent.setNamespace(namespace);
        objectCreatedEvent.setType(type);
        objectCreatedEvent.setVersion("1.0.0");
        objectCreatedEvent.setPoId(poId);
        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("active", true);
        objectCreatedEvent.setAttributeValues(attributes);
        LOGGER.info("Sending  DpsObjectCreatedEvent event == {}", objectCreatedEvent);
        this.channel.send(objectCreatedEvent, generateBaseEventPropertiesFilter(objectCreatedEvent));
    }
}
