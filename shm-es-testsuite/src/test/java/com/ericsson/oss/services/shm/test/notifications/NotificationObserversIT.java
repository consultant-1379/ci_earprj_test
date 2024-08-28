///*------------------------------------------------------------------------------
// *******************************************************************************
// * COPYRIGHT Ericsson 2012
// *
// * The copyright to the computer program(s) herein is the property of
// * Ericsson Inc. The programs may be used and/or copied only with written
// * permission from Ericsson Inc. or in accordance with the terms and
// * conditions stipulated in the agreement/contract under which the
// * program(s) have been supplied.
// *******************************************************************************
// *----------------------------------------------------------------------------*/
//package com.ericsson.oss.services.shm.test.notifications;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//
//import java.util.*;
//import java.util.concurrent.TimeUnit;
//
//import javax.inject.Inject;
//
//import org.jboss.arquillian.container.test.api.Deployer;
//import org.jboss.arquillian.junit.InSequence;
//import org.jboss.arquillian.test.api.ArquillianResource;
//import org.jboss.shrinkwrap.api.Archive;
//
//import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
//import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
//
////@RunWith(Arquillian.class)
//public class NotificationObserversIT {
//
//    @ArquillianResource
//    private Deployer deployer;
//
//    @Inject
//    private IDpsTestBase databean;
//
//    //@Deployment(name = "shm-notif-test", managed = false, testable = true)
//    public static Archive<?> createADeployableSHMEAR() {
//        return NotificationTestDeployment.createTestDeployment();
//    }
//
//    //@Test
//    @InSequence(1)
//    public void deploySHMTestEAR() throws Exception {
//        this.deployer.deploy("shm-notif-test");
//    }
//
//    //@Test
//    @InSequence(2)
//    public void initialize() {
//        databean.initMOdata();
//    }
//
//    //@Test
//    @InSequence(3)
//    public void testUpgradeJobNotifications() throws Exception {
//        MockJobNotificationReciever.reset();
//        Map<String, Object> map = new HashMap<String, Object>();
//        map.put(DpsTestBase.UP_STATE, "INSTALL_EXECUTING");
//        map.put(DpsTestBase.UP_PROGRESS_COUNT, 50L);
//        map.put(DpsTestBase.UP_PROGRESS_TOTAL, 10L);
//        map.put(DpsTestBase.UP_PROGRESS_HEADER, "DOWNLOADING_FILES");
//        System.out.println("upfdn=" + DpsTestBase.UPMO_FDN);
//        databean.moUpdateROAttrs(DpsTestBase.UPMO_FDN, map);
//        boolean acq = MockJobNotificationReciever.getLock().tryAcquire(2, TimeUnit.MINUTES);
//        assertTrue(acq);
//        DpsAttributeChangedEvent e = MockJobNotificationReciever.getResult();
//        assertTrue(e != null);
//        Set<AttributeChangeData> attrs = e.getChangedAttributes();
//        System.out.println("retrived attributes are :: " + attrs.toString());
//        assertEquals(attrs.size(), map.size());
//        String n;
//        Object v;
//        for (AttributeChangeData a : attrs) {
//            n = a.getName();
//            v = a.getNewValue();
//            assertTrue(map.containsKey(n));
//            assertTrue(map.containsValue(v));
//        }
//    }
//
//    //@Test
//    @InSequence(4)
//    public void testUpgradeServiceRequestNotification() throws Exception {
//        MockServiceRequestNotificationReciever.reset();
//        Map<String, Object> map = new HashMap<String, Object>();
//        map.put(DpsTestBase.CURRENT_MAIN_ACTIVITY, "CREATING_CV");
//        map.put(DpsTestBase.CURRENT_DETAILED_ACTIVITY, "EXECUTION_FAILED");
//        databean.moUpdateROAttrs(DpsTestBase.CVMO_FDN, map);
//        boolean acq = MockServiceRequestNotificationReciever.getLock().tryAcquire(2, TimeUnit.MINUTES);
//        assertTrue(acq);
//        DpsAttributeChangedEvent e = MockServiceRequestNotificationReciever.getResult();
//        assertTrue(e != null);
//        Set<AttributeChangeData> attrs = e.getChangedAttributes();
//        System.out.println(attrs.toString());
//        assertEquals(attrs.size(), map.size());
//        String n;
//        Object v;
//        for (AttributeChangeData a : attrs) {
//            n = a.getName();
//            v = a.getNewValue();
//            assertTrue(map.containsKey(n));
//            assertTrue(map.containsValue(v));
//        }
//    }
//
//    //@Test
//    @InSequence(5)
//    public void delete() {
//        databean.delete();
//    }
//}
