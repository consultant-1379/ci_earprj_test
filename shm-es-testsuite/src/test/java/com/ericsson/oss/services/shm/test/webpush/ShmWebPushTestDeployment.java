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

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import com.ericsson.oss.services.shm.cpp.inventory.service.upgrade.UpgradeEventHandler;
import com.ericsson.oss.services.shm.test.common.*;
import com.ericsson.oss.services.shm.test.elementaryservices.WorkflowInstanceServiceLocalImplMock;

/*
 * @author xcharoh
 */
public class ShmWebPushTestDeployment {
    private static final String SERVICE_FRAMEWORK_DIST = "com.ericsson.oss.itpf.sdk:service-framework-dist";
    private static final String SMRS_SERVICE_API = "com.ericsson.nms.security:smrs-service-api";
    private static final String IDENTITYMGMTSERVICE_API = "com.ericsson.nms.security:identitymgmtservices-api";
    private static final String JODA_TIME = "joda-time:joda-time";
    private static final String WFS_API = "com.ericsson.oss.services.wfs:wfs-api";
    private static final String WFS_JEE_LOCAL_API = "com.ericsson.oss.services.wfs:wfs-jee-local-api";
    private static final String SHM_MODELS = "com.ericsson.oss.services.shm.models:shm-models-jar";
    private static final String ELEMETARY_SERVICE_API = "com.ericsson.oss.services.shm:elementaryservice-api";
    private static final String SHMJOBSERVICE_API = "com.ericsson.oss.services.shm:shmjobservice-api";
    private static final String JOBCOMMON_API = "com.ericsson.oss.services.shm:job-common-api";
    private static final String CPPINVENTORYSYNCSERVICE_API = "com.ericsson.oss.services.shm:cppinventorysynchservice-api";
    private static final String SHM_COMMON_API = "com.ericsson.oss.services.shm:shm-common-api";
    private static final String ELEMETARY_SERVICE_REMOTE_API = "com.ericsson.oss.services.shm:elementaryservice-remote-api";
    private static final String PERSISTENCESTORE_API = "com.ericsson.oss.services.shm:persistencestore-api";
    private static final String PERSISTENCESTORE_IMPL = "com.ericsson.oss.services.shm:persistencestore-impl";
    private static final String SHM_COMMON_EXCEPTIONS = "com.ericsson.oss.services.shm:shm-common-exceptions";
    private static final String JOB_COMMON_EJB = "com.ericsson.oss.services.shm:job-common-ejb";
    private static final String SHM_COMMON_EJB = "com.ericsson.oss.services.shm:shm-common-ejb";
    private static final String JOB_EXECUTOR_API = "com.ericsson.oss.services.shm:jobexecutor-api";
    private static final String JOB_EXECUTOR_IMPL = "com.ericsson.oss.services.shm:jobexecutor-ejb";
    private static final String SHM_SUPERVISION_CONTROLLER_EJB = "com.ericsson.oss.services.shm:shm-supervision-controller-ejb";
    private static final String ELEMENTARYSERVICE_IMPL = "com.ericsson.oss.services.shm:elementaryservice-impl";
    private static final String ELEMENTARYSERVICE_CPP = "com.ericsson.oss.services.shm:elementaryservice-cpp";
    private static final String SHM_WEBPUSHSERVICE_API = "com.ericsson.oss.services.shm:shmwebpushservice-api";
    private static final String SHM_WEBPUSHSERVICE_IMPL = "com.ericsson.oss.services.shm:shmwebpushservice-impl";
    private static final String RESTSDK_WEBPUSH_API = "com.ericsson.oss.uisdk:restsdk-webpush-api";
    private static final String WEB_PUSH_MODEL_XML = "com.ericsson.oss.presentation.server:web-push-model-xml";
    //private static final String WEB_PUSH = "com.ericsson.oss.presentation.server:web-push";
    private static final String SHM_COMMON_JOB_UTILS = "com.ericsson.oss.services.shm:shm-common-job-utils";

    //TODO
    // Commented IdentityManagementServiceMock due to issues with Jenkins Release
    public static WebArchive createTestDeploymentForES() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "shm-webpush-test.war");
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(SERVICE_FRAMEWORK_DIST));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(SHM_MODELS));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(ELEMETARY_SERVICE_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(ELEMETARY_SERVICE_REMOTE_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(SHMJOBSERVICE_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(JOBCOMMON_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(SHM_COMMON_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(WFS_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(WFS_JEE_LOCAL_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(CPPINVENTORYSYNCSERVICE_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(PERSISTENCESTORE_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(PERSISTENCESTORE_IMPL));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(SMRS_SERVICE_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(IDENTITYMGMTSERVICE_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(JODA_TIME));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(SHM_COMMON_EXCEPTIONS));
        //webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(FILE_STORE_COMMON_EXCEPTIONS));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(JOB_COMMON_EJB));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(SHM_COMMON_EJB));
        //webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(LICENSE_SERVICE_EJB));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(JOB_EXECUTOR_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(JOB_EXECUTOR_IMPL));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(SHM_SUPERVISION_CONTROLLER_EJB));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(ELEMENTARYSERVICE_CPP));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(ELEMENTARYSERVICE_IMPL));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(SHM_WEBPUSHSERVICE_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(SHM_WEBPUSHSERVICE_IMPL));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(RESTSDK_WEBPUSH_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(WEB_PUSH_MODEL_XML));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(SHM_COMMON_JOB_UTILS));
        webArchive.setManifest(IntegrationTestDeploymentFactoryBase.MANIFEST_MF_FILE);
        webArchive.addAsWebInfResource(IntegrationTestDeploymentFactoryBase.BEANS_XML_FILE);
        webArchive.addAsResource("ServiceFrameworkConfiguration.properties", "ServiceFrameworkConfiguration.properties");
        webArchive.addClasses(UpgradeEventHandler.class, DataPersistenceServiceProxy.class, DataPersistenceServiceProxyBean.class, SmrsServiceMock.class, /* IdentityManagementServiceMock.class, */
                WorkflowInstanceServiceLocalImplMock.class);
        return webArchive;
    }

    private static File[] resolveFilesWithTransitiveDependencies(final String artifactCoordinates) {
        return Maven.resolver().loadPomFromFile("pom.xml").resolve(artifactCoordinates).withTransitivity().asFile();
    }

}
