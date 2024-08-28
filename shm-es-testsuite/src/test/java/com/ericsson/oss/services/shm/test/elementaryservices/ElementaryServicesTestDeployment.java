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
package com.ericsson.oss.services.shm.test.elementaryservices;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import com.ericsson.oss.itpf.security.identitymgmtservices.IdentityManagementService;
import com.ericsson.oss.services.shm.test.common.*;

/*
 * @author xmanush
 */
public class ElementaryServicesTestDeployment {

    private static final String SERVICE_FRAMEWORK_DIST = "com.ericsson.oss.itpf.sdk:service-framework-dist";
    private static final String SMRS_SERVICE_API = "com.ericsson.nms.security:smrs-service-api";
    private static final String IDENTITYMGMTSERVICE_API = "com.ericsson.nms.security:identitymgmtservices-api";
    private static final String JODA_TIME = "joda-time:joda-time";
    private static final String WFS_API = "com.ericsson.oss.services.wfs:wfs-api";
    private static final String WFS_JEE_LOCAL_API = "com.ericsson.oss.services.wfs:wfs-jee-local-api";
    private static final String ELEMETARY_SERVICE_API = "com.ericsson.oss.services.shm:elementaryservice-api";
    private static final String SHMJOBSERVICE_API = "com.ericsson.oss.services.shm:shmjobservice-api";
    private static final String JOBCOMMON_API = "com.ericsson.oss.services.shm:job-common-api";
    private static final String CPPINVENTORYSYNCSERVICE_API = "com.ericsson.oss.services.shm:cppinventorysynchservice-api";
    private static final String SHM_COMMON_API = "com.ericsson.oss.services.shm:shm-common-api";
    private static final String SHM_COMMON__SMRS_API = "com.ericsson.oss.services.shm:shm-common-smrs-api";
    private static final String SHM_COMMON__SMRS_IMPL = "com.ericsson.oss.services.shm:shm-common-smrs-impl";
    private static final String SHM_CPP_SYNC_EJB = "com.ericsson.oss.services.shm:cppinventorysynchservice-ejb";
    private static final String SHM_BACKUP_ELE_ECIM = "com.ericsson.oss.services.shm:elementaryservice-ecim";
    private static final String SHM_COMMON = "com.ericsson.oss.services.shm:shm-common-utils";
    private static final String ELEMETARY_SERVICE_REMOTE_API = "com.ericsson.oss.services.shm:elementaryservice-remote-api";
    private static final String PERSISTENCESTORE_API = "com.ericsson.oss.services.shm:shm-common-persistence-api";
    private static final String PERSISTENCESTORE_IMPL = "com.ericsson.oss.services.shm:shm-common-persistence-impl";
    private static final String SHM_COMMON_EXCEPTIONS = "com.ericsson.oss.services.shm:shm-common-exception";

    private static final String JOB_COMMON_EJB = "com.ericsson.oss.services.shm:job-common-ejb";

    private static final String JOB_EXECUTOR_API = "com.ericsson.oss.services.shm:jobexecutor-api";
    private static final String JOB_EXECUTOR_IMPL = "com.ericsson.oss.services.shm:jobexecutor-ejb";

    private static final String SHM_MODELS_MODEL = "com.ericsson.oss.services.shm.models:shm-models-model";
    private static final String SHM_VERSANT_LISTEN = "com.ericsson.oss.services.shm:shm-dps-notification-listener-ejb";
    private static final String SHM_COMMON_JOB_UTILS = "com.ericsson.oss.services.shm:shm-common-job-utils";

    public static WebArchive createTestDeploymentForES() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "shm-es-test.war");
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(SERVICE_FRAMEWORK_DIST));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(SHM_MODELS_MODEL));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(ELEMETARY_SERVICE_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(ELEMETARY_SERVICE_REMOTE_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(SHMJOBSERVICE_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(JOBCOMMON_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(SHM_COMMON_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(SHM_COMMON));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(SHM_COMMON__SMRS_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(SHM_COMMON__SMRS_IMPL));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(SHM_CPP_SYNC_EJB));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(WFS_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(WFS_JEE_LOCAL_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(CPPINVENTORYSYNCSERVICE_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(PERSISTENCESTORE_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(PERSISTENCESTORE_IMPL));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(SMRS_SERVICE_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(IDENTITYMGMTSERVICE_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(JODA_TIME));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(SHM_COMMON_EXCEPTIONS));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(JOB_COMMON_EJB));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(JOB_EXECUTOR_API));
        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(JOB_EXECUTOR_IMPL));

        webArchive.addAsLibraries(resolveFilesWithTransitiveDependencies(SHM_COMMON_JOB_UTILS));
        webArchive.addAsLibraries(resolveAsFiles(TestConstants.SHM_GROUP_ID, TestConstants.ELEMENTARY_SERVICE_CPP_ARTIFACT_ID, "pom.xml")[0]);
        webArchive.addAsLibraries(resolveAsFiles(TestConstants.SHM_GROUP_ID, TestConstants.ELEMENTARY_SERVICE_IMPL_ARTIFACT_ID, "pom.xml")[0]);
        webArchive.addAsLibraries(IntegrationTestDeploymentFactoryBase.resolveAsFiles(TestConstants.SHM_GROUP_ID, TestConstants.BACKUP_SERVICE_REMOTE_API_ARTIFACT_ID, "pom.xml")[0]);
        webArchive.setManifest(IntegrationTestDeploymentFactoryBase.MANIFEST_MF_FILE);
        webArchive.addAsWebInfResource(IntegrationTestDeploymentFactoryBase.BEANS_XML_FILE);
        webArchive.addAsResource("ServiceFrameworkConfiguration.properties", "ServiceFrameworkConfiguration.properties");
        webArchive.addClasses(SmrsServiceMock.class, IdentityManagementService.class, DataPersistenceServiceProxy.class, DataPersistenceServiceProxyBean.class,
                WorkflowInstanceServiceLocalImplMock.class, IElementaryServicesTestBase.class, ElementaryServicesTestBase.class);
        return webArchive;
    }

    private static File[] resolveFilesWithTransitiveDependencies(final String artifactCoordinates) {
        return Maven.resolver().loadPomFromFile("pom.xml").resolve(artifactCoordinates).withTransitivity().asFile();
    }

    private static File[] resolveAsFiles(final String groupId, final String artifactId, final String pomFilePath) {
        return Maven.resolver().loadPomFromFile(pomFilePath).resolve(groupId + ":" + artifactId).withTransitivity().asFile();
    }

    public static void main(final String[] args) {
        createTestDeploymentForES();
    }
}
