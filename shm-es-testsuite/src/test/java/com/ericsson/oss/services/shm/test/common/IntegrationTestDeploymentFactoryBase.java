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
package com.ericsson.oss.services.shm.test.common;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IntegrationTestDeploymentFactoryBase {

    private static final Logger log = LoggerFactory.getLogger(IntegrationTestDeploymentFactoryBase.class);

    public static final File MANIFEST_MF_FILE = new File("src/test/resources/META-INF/MANIFEST.MF");

    public static final File BEANS_XML_FILE = new File("src/test/resources/META-INF/beans.xml");

    /**
     * Create deployment from given maven coordinates
     * 
     * @param mavenCoordinates
     *            Maven coordinates in form of groupId:artifactId:type
     * @return Deployment archive represented by this maven artifact
     */
    public static EnterpriseArchive createEARDeploymentFromMavenCoordinates(final String mavenCoordinates) {
        log.debug("******Creating deployment {} for test******", mavenCoordinates);
        final File archiveFile = IntegrationTestDependencies.resolveArtifactWithoutDependencies(mavenCoordinates);
        if (archiveFile == null) {
            throw new IllegalStateException("Unable to resolve artifact " + mavenCoordinates);
        }
        final EnterpriseArchive ear = ShrinkWrap.createFromZipFile(EnterpriseArchive.class, archiveFile);

        log.debug("******Created from maven artifact with coordinates {} ******", mavenCoordinates);
        return ear;
    }

    public static File[] resolveFilesWithTransitiveDependencies(final String artifactCoordinates) {
        return Maven.resolver().loadPomFromFile("pom.xml").resolve(artifactCoordinates).withTransitivity().asFile();
    }

    public static File[] resolveAsFiles(final String groupId, final String artifactId, final String pomFilePath) {
        return Maven.resolver().loadPomFromFile(pomFilePath).resolve(groupId + ":" + artifactId).withTransitivity().asFile();
    }

    //
    //    public static MavenDependencyResolver getMavenResolver() {
    //        return DependencyResolvers.use(MavenDependencyResolver.class).loadMetadataFromPom("pom.xml");
    //    }

}
