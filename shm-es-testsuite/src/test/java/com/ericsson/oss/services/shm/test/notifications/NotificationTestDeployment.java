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
//import org.jboss.shrinkwrap.api.Archive;
//import org.jboss.shrinkwrap.api.ShrinkWrap;
//import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
//import org.jboss.shrinkwrap.api.spec.JavaArchive;
//
//import com.ericsson.oss.services.shm.backupservice.impl.BackupNotificationTopicObserver;
//import com.ericsson.oss.services.shm.backupservice.impl.RemoteRequestNotificationReciever;
//import com.ericsson.oss.services.shm.cpp.inventory.notifications.InventoryNotificationLoadCounter;
//import com.ericsson.oss.services.shm.cpp.inventory.service.*;
//import com.ericsson.oss.services.shm.cpp.inventory.service.registration.CppInventorySynchEventListenerRegistration;
//import com.ericsson.oss.services.shm.es.api.AVCNotification;
//import com.ericsson.oss.services.shm.es.api.ActivityCallback;
//import com.ericsson.oss.services.shm.es.impl.cpp.backup.CppBackupNotificationQueueObserver;
//import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.CppUpgradeNotificationObserver;
//import com.ericsson.oss.services.shm.model.NotificationSubject;
//import com.ericsson.oss.services.shm.model.NotificationType;
//import com.ericsson.oss.services.shm.notifications.api.*;
//import com.ericsson.oss.services.shm.notifications.impl.*;
//import com.ericsson.oss.services.shm.notifications.impl.license.LicenseNotificationQueueListener;
//import com.ericsson.oss.services.shm.test.common.IntegrationTestDependencies;
//import com.ericsson.oss.services.shm.test.common.IntegrationTestDeploymentFactoryBase;
//
//public class NotificationTestDeployment {
//
//    private static final String SHM_MODELS_JAR = "com.ericsson.oss.services.shm.models:shm-models-jar";
//    private static String JODA_TIME = "joda-time:joda-time";
//    private static final String SHM_COMMON_EJB = "com.ericsson.oss.services.shm:shm-common-ejb";
//
//    /**
//     * To test the model extension for recieving notification using the src observer classes - cpp backup and upgrade.
//     */
//    public static Archive<?> createTestDeployment() {
//        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "shm-notif-test.ear");
//        ear.addAsModule(createModuleArchive());
//        ear.addAsLibraries(IntegrationTestDeploymentFactoryBase.resolveFilesWithTransitiveDependencies(IntegrationTestDependencies.SDK_DIST));
//        ear.addAsLibraries(IntegrationTestDeploymentFactoryBase.resolveFilesWithTransitiveDependencies(SHM_MODELS_JAR));
//        ear.setManifest(IntegrationTestDeploymentFactoryBase.MANIFEST_MF_FILE);
//        ear.addAsApplicationResource(IntegrationTestDeploymentFactoryBase.BEANS_XML_FILE);
//        return ear;
//    }
//
//    private static Archive<?> createModuleArchive() {
//        final JavaArchive archive = ShrinkWrap
//                .create(JavaArchive.class, "shm-notif-test-bean-lib.jar")
//                .addAsResource("META-INF/beans.xml", "META-INF/beans.xml")
//                .addAsResource("META-INF/MANIFEST.MF", "META-INF/MANIFEST.MF")
//                .addAsResource("ServiceFrameworkConfiguration.properties", "ServiceFrameworkConfiguration.properties")
//                .addClasses(NotificationObserversIT.class, IDpsTestBase.class, DpsTestBase.class, CppInventorySynchEventListenerRegistration.class, NotificationReciever.class,
//                        MockJobNotificationReciever.class, MockServiceRequestNotificationReciever.class, CppUpgradeNotificationObserver.class, BackupNotificationTopicObserver.class,
//                        NotificationTypeQualifier.class, NotificationType.class, AbstractNotificationListener.class, NotificationListener.class, ShmNotificationQueueListener.class,
//                        LicenseNotificationQueueListener.class,  InventoryNotificationLoadCounter.class
//                        );
//        return archive;
//    }
//
//    /**
//     * To test notification for cpp backup using local cache registry and mock notification handler.
//     */
//    public static Archive<?> createTestDeployment_2() {
//        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "shm-notif-test-2.ear");
//        ear.addAsModule(createModuleArchive_2());
//        ear.addAsLibraries(IntegrationTestDeploymentFactoryBase.resolveFilesWithTransitiveDependencies(IntegrationTestDependencies.SDK_DIST));
//        ear.addAsLibraries(IntegrationTestDeploymentFactoryBase.resolveFilesWithTransitiveDependencies(SHM_MODELS_JAR));
//        ear.setManifest(IntegrationTestDeploymentFactoryBase.MANIFEST_MF_FILE);
//        ear.addAsApplicationResource(IntegrationTestDeploymentFactoryBase.BEANS_XML_FILE);
//        return ear;
//    }
//
//    private static Archive<?> createModuleArchive_2() {
//        final JavaArchive archive = ShrinkWrap
//                .create(JavaArchive.class, "shm-notif-test-bean-lib.jar")
//                .addAsResource("META-INF/beans.xml", "META-INF/beans.xml")
//                .addAsResource("META-INF/MANIFEST.MF", "META-INF/MANIFEST.MF")
//                .addAsResource("ServiceFrameworkConfiguration.properties", "ServiceFrameworkConfiguration.properties")
//                .addClasses(ActivityCallback.class, NotificationCallback.class, NotificationCallbackResult.class, AbstractNotificationCallBack.class, IDpsTestBase.class, DpsTestBase.class,
//                        AVCNotificationsSubscriptionIT.class, IDpsTestBase.class, DpsTestBase.class, CppInventorySynchEventListenerRegistration.class, NotificationReciever.class,
//                        CppBackupNotificationQueueObserver.class, RemoteRequestNotificationReciever.class, NotificationRegistry.class, AbstractNotificationRegistry.class,
//                        NotificationRegistryLocal.class, NotificationHandler.class, MockNotificationHandler.class, Notification.class, AVCNotification.class, NotificationSubject.class,
//                        FdnNotificationSubject.class, NotificationType.class, NotificationTypeQualifier.class);
//        return archive;
//    }
//
//}
