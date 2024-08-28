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
//import java.util.*;
//
//import javax.inject.Inject;
//
//import org.jboss.arquillian.container.test.api.Deployer;
//import org.jboss.arquillian.container.test.api.OperateOnDeployment;
//import org.jboss.arquillian.junit.InSequence;
//import org.jboss.arquillian.test.api.ArquillianResource;
//import org.jboss.shrinkwrap.api.Archive;
//
//import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
//import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
//import com.ericsson.oss.services.shm.model.NotificationType;
//import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
//import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier;
//import com.ericsson.oss.services.shm.notifications.impl.AbstractNotificationCallBack;
//import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
//
////@RunWith(Arquillian.class)
//public class AVCNotificationsSubscriptionIT {
//
//    @ArquillianResource
//    private Deployer deployer;
//
//    @Inject
//    private IDpsTestBase databean;
//
//    @Inject
//    @NotificationTypeQualifier(type = NotificationType.SYNCHRONOUS_REQUEST)
//    private NotificationRegistry registry;
//
//    //@Deployment(name = "shm-notif-test-2", managed = false, testable = true)
//    public static Archive<?> createADeployableSHMEAR() {
//        return NotificationTestDeployment.createTestDeployment_2();
//    }
//
//    //@Test
//    @InSequence(1)
//    @OperateOnDeployment("shm-notif-test-2")
//    public void deploySHMTestEAR() throws Exception {
//        this.deployer.deploy("shm-notif-test-2");
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
//    public void testNotificationprocessing() throws Exception {
//        /*
//         * MockNotificationHandler.reset(); MockNotificationCallBack mCallback = new MockNotificationCallBack(); FdnNotificationSubject subject = new
//         * FdnNotificationSubject(DpsTestBase.CVMO_FDN, mCallback); registry.register(subject); List<String> states = new ArrayList<String>();
//         * states.add("CREATING_CV"); states.add("IDLE"); new MOAction(databean, states).start(); NotificationCallbackResult result =
//         * mCallback.waitForProcessNotifications(); registry.removeSubject(subject); assertTrue(result.isCompleted()); assertTrue(result.isSuccess());
//         */
//    }
//
//    private static class MockNotificationCallBack extends AbstractNotificationCallBack {
//
//        /**
//         * 
//         */
//        private static final long serialVersionUID = 3514811626724747009L;
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see
//         * com.ericsson.oss.services.shm.notifications.impl.AbstractNotificationCallBack#processPayLoad(com.ericsson.oss.services.shm.notifications
//         * .impl.FdnNotificationSubject, com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent)
//         */
//        @Override
//        public void processPayLoad(final FdnNotificationSubject subject, final DpsAttributeChangedEvent event) {
//            System.out.println("procssing payload");
//            Set<AttributeChangeData> attrs = event.getChangedAttributes();
//            for (AttributeChangeData a : attrs) {
//                if (a.getName().equals(DpsTestBase.CURRENT_MAIN_ACTIVITY)) {
//                    Object v = a.getNewValue();
//                    Object pv = a.getOldValue();
//                    System.out.println("old value=" + pv + ", value=" + v);
//                    if (v.equals("IDLE") && pv.equals("CREATING_CV")) {
//                        result.setCompleted(true);
//                        result.setSuccess(true);
//                        break;
//                    }
//                }
//            }
//
//        }
//
//    }
//
//    static class MOAction extends Thread {
//
//        private final IDpsTestBase databean;
//        private final List<String> states;
//
//        /**
//         * 
//         */
//        public MOAction(final IDpsTestBase dpsTestBase, final List<String> states) {
//            this.databean = dpsTestBase;
//            this.states = states;
//        }
//
//        @Override
//        public void run() {
//
//            for (String state : states) {
//                Map<String, Object> map = new HashMap<String, Object>();
//                map.put(DpsTestBase.CURRENT_MAIN_ACTIVITY, state);
//                databean.moUpdateROAttrs(DpsTestBase.CVMO_FDN, map);
//                try {
//                    Thread.sleep(10 * 1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
//
//    //@Test
//    @InSequence(Integer.MAX_VALUE - 1)
//    public void cleanup() {
//        databean.delete();
//    }
//}
