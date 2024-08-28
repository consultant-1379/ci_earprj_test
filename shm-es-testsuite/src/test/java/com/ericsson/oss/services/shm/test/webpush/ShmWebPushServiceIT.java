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
package com.ericsson.oss.services.shm.test.webpush;

import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsObjectCreatedEvent;
import com.ericsson.oss.services.shm.webpush.api.WebPushConstants;
import com.ericsson.oss.services.shm.webpush.notifications.impl.JobNotificationUtil;
import com.ericsson.oss.services.shm.webpush.utils.WebPushServiceUtil;

@RunWith(Arquillian.class)
@ApplicationScoped
public class ShmWebPushServiceIT {

    private final static String WEB_PUSH_WAR = "shm-webpush-test";

    @ArquillianResource
    private Deployer deployer;

    @Inject
    IShmWebPushTestBase webPushDataBean;

    @Inject
    WebPushServiceUtil webPushServiceUtil;

    @Mock
    @Inject
    JobNotificationUtil jobNotificationUtil;

    private static final Logger LOGGER = LoggerFactory.getLogger(ShmWebPushServiceIT.class);

    @Deployment(name = WEB_PUSH_WAR, managed = false)
    public static Archive<?> createADeployableSHMEAR() {
        final WebArchive war = ShmWebPushTestDeployment.createTestDeploymentForES();

        war.addClass(IShmWebPushTestBase.class);
        war.addClass(ShmWebPushServiceIT.class);
        war.addClass(ShmWebPushTestBase.class);
        war.addClass(ShmWebPushTestDeployment.class);

        return war;
    }

    @Test
    @InSequence(1)
    @OperateOnDeployment(WEB_PUSH_WAR)
    public void deploySHMTestEAR() throws Exception {
        this.deployer.deploy(WEB_PUSH_WAR);
    }

    @Test
    @InSequence(2)
    public void initialize() throws Throwable {
        LOGGER.info("Creating Test Jobs");
        webPushDataBean.createJobDetails();
        LOGGER.info("All Test Jobs Created");
    }

    @Test
    @InSequence(3)
    public void testForNotifyAsCreate() {
        final long mainJobId = ShmWebPushTestBase.poIds.get(ShmWebPushTestBase.mainJobPoId);
        final DpsObjectCreatedEvent dpsObjectCreatedEvent = new DpsObjectCreatedEvent();
        dpsObjectCreatedEvent.setPoId(mainJobId);
        dpsObjectCreatedEvent.setType(WebPushConstants.JOB_KIND);

        verify(jobNotificationUtil).notifyAsCreate(dpsObjectCreatedEvent);
    }


    @Test
    @InSequence(5)
    public void testForMainJobNotifyAsUpdate() {
        final long mainJobId = ShmWebPushTestBase.poIds.get(ShmWebPushTestBase.mainJobPoId);
        final DpsAttributeChangedEvent dpsAttributeChangedEvent = new DpsAttributeChangedEvent();
        dpsAttributeChangedEvent.setPoId(mainJobId);
        dpsAttributeChangedEvent.setType(WebPushConstants.JOB_KIND);
        final Set<AttributeChangeData> attributesSet = new HashSet<AttributeChangeData>();
        final AttributeChangeData changeData = new AttributeChangeData();
        changeData.setName("starttime");
        changeData.setNewValue("Fri Jul 24 13:20:47 IST 2015");
        changeData.setOldValue(null);
        attributesSet.add(changeData);
        dpsAttributeChangedEvent.setChangedAttributes(attributesSet);

        verify(jobNotificationUtil).notifyAsUpdate(dpsAttributeChangedEvent);

    }

    @Test
    @InSequence(6)
    public void testForNeJobNotifyAsUpdate() {
        final long mainJobId = ShmWebPushTestBase.poIds.get(ShmWebPushTestBase.neJobPoId);
        final DpsAttributeChangedEvent dpsAttributeChangedEvent = new DpsAttributeChangedEvent();
        dpsAttributeChangedEvent.setPoId(mainJobId);
        dpsAttributeChangedEvent.setType(WebPushConstants.NE_JOB_KIND);
        final Set<AttributeChangeData> attributesSet = new HashSet<AttributeChangeData>();
        final AttributeChangeData changeData = new AttributeChangeData();
        changeData.setName("state");
        changeData.setNewValue("COMPLETED");
        changeData.setOldValue("RUNNING");
        attributesSet.add(changeData);
        dpsAttributeChangedEvent.setChangedAttributes(attributesSet);

        verify(jobNotificationUtil).notifyAsUpdate(dpsAttributeChangedEvent);

    }

    @Test
    @InSequence(7)
    public void testForActivityJobNotifyAsUpdate() {
        final long activityJobId = ShmWebPushTestBase.poIds.get(ShmWebPushTestBase.activityJobPoId);
        final DpsAttributeChangedEvent dpsAttributeChangedEvent = new DpsAttributeChangedEvent();
        dpsAttributeChangedEvent.setPoId(activityJobId);
        dpsAttributeChangedEvent.setType(WebPushConstants.ACTIVITY_JOB_KIND);
        final Set<AttributeChangeData> attributesSet = new HashSet<AttributeChangeData>();
        final AttributeChangeData changeData = new AttributeChangeData();
        changeData.setName("endTime");
        changeData.setNewValue("Fri Jul 29 15:20:47 IST 2015");
        changeData.setOldValue("RUNNING");
        attributesSet.add(changeData);
        dpsAttributeChangedEvent.setChangedAttributes(attributesSet);

        verify(jobNotificationUtil).notifyAsUpdate(dpsAttributeChangedEvent);

    }

    @Test
    @InSequence(8)
    public void testForJobLogsNotifyAsUpdate() {
        final long activityJobId = ShmWebPushTestBase.poIds.get(ShmWebPushTestBase.activityJobPoId);
        final DpsAttributeChangedEvent dpsAttributeChangedEvent = new DpsAttributeChangedEvent();
        dpsAttributeChangedEvent.setPoId(activityJobId);
        dpsAttributeChangedEvent.setType(WebPushConstants.ACTIVITY_JOB_KIND);
        final Set<AttributeChangeData> attributesSet = new HashSet<AttributeChangeData>();
        final AttributeChangeData changeData = new AttributeChangeData();
        changeData.setName("log");
        attributesSet.add(changeData);
        dpsAttributeChangedEvent.setChangedAttributes(attributesSet);

        verify(jobNotificationUtil).notifyAsUpdate(dpsAttributeChangedEvent);

    }

}
