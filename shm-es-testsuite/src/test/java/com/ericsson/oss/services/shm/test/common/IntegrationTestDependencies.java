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

import org.jboss.shrinkwrap.resolver.api.maven.Maven;

public class IntegrationTestDependencies {

    public static final String SHM_SERVICE_EAR = "com.ericsson.oss.services.shm:cppinventorysynchservice-ear";
    public static final String SDK_DIST = "com.ericsson.oss.itpf.sdk:service-framework-dist";
    public static final String SHM_MODELS_JAR = "com.ericsson.oss.services.shm.models:shm-models-jar";
    public static final String COM_ERICSSON_OSS_ITPF_COMMON__PLATFORMINTEGRARIONBRIDGE_EAR = "com.ericsson.oss.itpf.common:PlatformIntegrationBridge-ear";

    /**
     * // * Maven resolver that will try to resolve dependencies using pom.xml of the project where this class is located. // * // * @return MavenDependencyResolver //
     */
    //    public static MavenDependencyResolver getMavenResolver() {
    //        return DependencyResolvers.use(MavenDependencyResolver.class).loadMetadataFromPom("pom.xml");
    //
    //    }

    private static File[] resolveFilesWithTransitiveDependencies(final String artifactCoordinates) {
        return Maven.resolver().loadPomFromFile("pom.xml").resolve(artifactCoordinates).withTransitivity().asFile();
    }

    /**
     * Resolve artifacts without dependencies
     * 
     * @param artifactCoordinates
     * @return
     */
    public static File resolveArtifactWithoutDependencies(final String artifactCoordinates) {
        //   final File[] artifacts = getMavenResolver().artifact(artifactCoordinates).exclusion("*").resolveAsFiles();
        final File[] artifacts = resolveFilesWithTransitiveDependencies(artifactCoordinates);
        if (artifacts == null) {
            throw new IllegalStateException("Artifact with coordinates " + artifactCoordinates + " was not resolved");
        }
        if (artifacts.length != 1) {
            throw new IllegalStateException("Resolved more then one artifact with coordinates " + artifactCoordinates);
        }
        return artifacts[0];
    }

}
