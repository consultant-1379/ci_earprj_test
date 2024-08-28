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
package com.ericsson.oss.services.shm.test.elementaryservices.upgrade;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import com.ericsson.oss.services.shm.test.common.IntegrationTestDeploymentFactoryBase;
import com.ericsson.oss.services.shm.test.common.TestConstants;

public class UpgradeTestDeployment {

    private static final String FILESTORE_SWPKG_API = "com.ericsson.oss.services.shm:filestore-softwarepackage-api";
    private static final String FILESTORE_SWPKG_IMPL = "filestore-softwarepackage-impl";
    private static final String FILESTORE_SWPKG_REM_API = "com.ericsson.oss.services.shm:filestore-softwarepackage-remote-api";
    private static final String SHM_GROUP_ID = "com.ericsson.oss.services.shm";

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
    private static final String PERSISTENCESTORE_IMPL = "persistencestore-impl";
    private static final String SHM_COMMON_EXCEPTIONS = "com.ericsson.oss.services.shm:shm-common-exceptions";
    private static final String FILE_STORE_COMMON_EXCEPTIONS = "com.ericsson.oss.services.shm:filestore-common-exceptions";
    private static final String JOB_COMMON_EJB = "job-common-ejb";
    private static final String SHM_COMMON_EJB = "shm-common-ejb";
    private static final String LICENSE_SERVICE_EJB = "licenseservice-ejb";
    private static final String JOB_EXECUTOR_API = "com.ericsson.oss.services.shm:jobexecutor-api";
    private static final String JOB_EXECUTOR_IMPL = "jobexecutor-ejb";
    private static final String SHM_SUPERVISION_CONTROLLER_EJB = "shm-supervision-controller-ejb";

    private static final String LICENSE_SERVICE_API = "com.ericsson.oss.services.shm:licenseservice-api";
    private static final String SHM_SUPERVISION_CONTROLLER_API = "com.ericsson.oss.services.shm:shm-supervision-controller-api";
    private static final String FILESTORE_SOFTWAREPACKAGE_REMOTE_SHM_API = "com.ericsson.oss.services.shm:filestore-softwarepackage-remote-shm-api";
    private static final String LICENSESERVICE_REMOTEAPI = "com.ericsson.oss.services.shm:licenseservice-remoteapi";
    private static final String LICENSESERVICE_REMOTE_SHM_API = "com.ericsson.oss.services.shm:licenseservice-remote-shm-api";
    private static final String LICENSESERVICE_XML_MODELS = "com.ericsson.oss.services.shm:licenseservicexmlmodels";
    private static final String SOFTWAREPACKAGE_REMOTE_API = "com.ericsson.oss.services.shm:softwarepackage-remote-api";
    private static final String SOFTWAREPACKAGE_DESCRIPTOR_MODEL = "com.ericsson.oss.services.shm:softwarepackagedescriptormodels";

    private static final String SHM_JCA_FILE_ADAPTER_RAR = "shm-jca-file-adapter-rar";
    private static final String SHM_COMMON_JOB_UTILS = "com.ericsson.oss.services.shm:shm-common-job-utils";

    public static EnterpriseArchive createTestDeploymentForUpgrade() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "upg-test-depl.ear");
        ear.addAsLibraries(resolveFilesWithTransitiveDependencies(SERVICE_FRAMEWORK_DIST));

        /*
         * ear.addAsLibraries(resolveFilesWithTransitiveDependencies(LICENSE_SERVICE_API)); ear.addAsLibraries(resolveFilesWithTransitiveDependencies(LICENSESERVICE_REMOTEAPI));
         * ear.addAsLibraries(resolveFilesWithTransitiveDependencies(LICENSESERVICE_REMOTE_SHM_API)); ear.addAsLibraries(resolveFilesWithTransitiveDependencies(LICENSESERVICE_XML_MODELS));
         * ear.addAsLibraries(resolveFilesWithTransitiveDependencies(SHM_SUPERVISION_CONTROLLER_API));
         * ear.addAsLibraries(resolveFilesWithTransitiveDependencies(FILESTORE_SOFTWAREPACKAGE_REMOTE_SHM_API)); ear.addAsLibraries(resolveFilesWithTransitiveDependencies(SOFTWAREPACKAGE_REMOTE_API));
         * ear.addAsLibraries(resolveFilesWithTransitiveDependencies(SOFTWAREPACKAGE_DESCRIPTOR_MODEL));
         */

        ear.addAsLibraries(resolveFilesWithTransitiveDependencies(SHM_MODELS));
        ear.addAsLibraries(resolveFilesWithTransitiveDependencies(ELEMETARY_SERVICE_API));
        ear.addAsLibraries(resolveFilesWithTransitiveDependencies(ELEMETARY_SERVICE_REMOTE_API));
        ear.addAsLibraries(resolveFilesWithTransitiveDependencies(SHMJOBSERVICE_API));
        ear.addAsLibraries(resolveFilesWithTransitiveDependencies(JOBCOMMON_API));
        ear.addAsLibraries(resolveFilesWithTransitiveDependencies(SHM_COMMON_API));
        ear.addAsLibraries(resolveFilesWithTransitiveDependencies(WFS_API));
        ear.addAsLibraries(resolveFilesWithTransitiveDependencies(WFS_JEE_LOCAL_API));
        ear.addAsLibraries(resolveFilesWithTransitiveDependencies(CPPINVENTORYSYNCSERVICE_API));
        ear.addAsLibraries(resolveFilesWithTransitiveDependencies(PERSISTENCESTORE_API));
        ear.addAsModule(resolveAsFiles(SHM_GROUP_ID, PERSISTENCESTORE_IMPL, "pom.xml")[0]);
        ear.addAsLibraries(resolveFilesWithTransitiveDependencies(SMRS_SERVICE_API));
        ear.addAsLibraries(resolveFilesWithTransitiveDependencies(IDENTITYMGMTSERVICE_API));
        ear.addAsLibraries(resolveFilesWithTransitiveDependencies(JODA_TIME));
        ear.addAsLibraries(resolveFilesWithTransitiveDependencies(SHM_COMMON_EXCEPTIONS));
        //ear.addAsLibraries(resolveFilesWithTransitiveDependencies(FILE_STORE_COMMON_EXCEPTIONS));
        ear.addAsLibraries(resolveFilesWithTransitiveDependencies(JOB_EXECUTOR_API));
        ear.addAsModule(resolveAsFiles(SHM_GROUP_ID, JOB_COMMON_EJB, "pom.xml")[0]);
        ear.addAsModule(resolveAsFiles(SHM_GROUP_ID, SHM_COMMON_EJB, "pom.xml")[0]);
        //ear.addAsModule(resolveAsFiles(SHM_GROUP_ID, LICENSE_SERVICE_EJB, "pom.xml")[0]);
        ear.addAsModule(resolveAsFiles(SHM_GROUP_ID, JOB_EXECUTOR_IMPL, "pom.xml")[0]);
        ear.addAsModule(resolveAsFiles(SHM_GROUP_ID, SHM_SUPERVISION_CONTROLLER_EJB, "pom.xml")[0]);

        //ear.addAsLibraries(resolveFilesWithTransitiveDependencies(FILESTORE_SWPKG_API));
        //ear.addAsLibraries(resolveFilesWithTransitiveDependencies(FILESTORE_SWPKG_REM_API));
        //ear.addAsModule(resolveAsFiles(SHM_GROUP_ID, FILESTORE_SWPKG_IMPL, "pom.xml")[0]);

        ear.addAsModule(resolveAsFiles(SHM_GROUP_ID, TestConstants.ELEMENTARY_SERVICE_CPP_ARTIFACT_ID, "pom.xml")[0]);
        ear.addAsModule(resolveAsFiles(SHM_GROUP_ID, TestConstants.ELEMENTARY_SERVICE_IMPL_ARTIFACT_ID, "pom.xml")[0]);
        ear.addAsLibraries(resolveAsFiles(SHM_GROUP_ID, TestConstants.BACKUP_SERVICE_REMOTE_API_ARTIFACT_ID, "pom.xml")[0]);
        ear.addAsModule(resolveAsFiles(SHM_GROUP_ID, TestConstants.BACKUP_SERVICE_REMOTE_IMPL_ARTIFACT_ID, "pom.xml")[0]);
        ear.addAsLibraries(resolveFilesWithTransitiveDependencies(SHM_COMMON_JOB_UTILS));
        ear.setManifest(IntegrationTestDeploymentFactoryBase.MANIFEST_MF_FILE);
        //ear.addAsModule(resolveAsFilesForRar(SHM_GROUP_ID, SHM_JCA_FILE_ADAPTER_RAR)[0]);

        return ear;
    }

    private static File[] resolveFilesWithTransitiveDependencies(final String artifactCoordinates) {
        return Maven.resolver().loadPomFromFile("pom.xml").resolve(artifactCoordinates).withTransitivity().asFile();
    }

    private static File[] resolveAsFiles(final String groupId, final String artifactId, final String pomFilePath) {
        return Maven.resolver().loadPomFromFile(pomFilePath).resolve(groupId + ":" + artifactId).withTransitivity().asFile();
    }

    private static File[] resolveAsFilesForRar(final String groupId, final String artifactId) {
        return Maven.resolver().loadPomFromFile("pom.xml").resolve(groupId + ":" + artifactId + ":rar:?").withoutTransitivity().asFile();
    }
}
