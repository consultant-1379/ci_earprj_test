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
package com.ericsson.oss.services.shm.test.cvremoteoperations;

import static org.junit.Assert.assertTrue;

import java.util.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.*;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ericsson.oss.services.shm.backupservice.remote.impl.CVManagementServiceFactory;
import com.ericsson.oss.services.shm.filestore.swpackage.remote.api.RemoteSoftwarePackageService;
import com.ericsson.oss.services.shm.test.elementaryservices.ElementaryServicesTestDeployment;
import com.ericsson.oss.services.shm.test.notifications.DpsTestBase;
import com.ericsson.oss.services.shm.test.notifications.IDpsTestBase;

//@RunWith(Arquillian.class)
//@ApplicationScoped
public class CvOperationsRemoteIT {

    @ArquillianResource
    private Deployer deployer;

    @Inject
    private IDpsTestBase databean;

    @Inject
    CvRemoteServiceTestFactory cvRemoteServiceTestFactory;

    @Inject
    CVManagementServiceFactory cvManagementServiceFactory;

    private static final String MECONTEXT_NAME = "ERBS101";

    private static final String CV_NAME = "newCV";
    private static final String CV_IDENTITY = "shm";

    //@Deployment(name = "shm-remotecv-test", managed = false)
    public static Archive<?> createADeployableSHMEAR() {
        final WebArchive war = ElementaryServicesTestDeployment.createTestDeploymentForES();
        war.addClass(CvOperationsRemoteIT.class);
        war.addClass(IDpsTestBase.class);
        war.addClass(DpsTestBase.class);
        war.addClass(CvRemoteServiceTestFactory.class);
        return war;
    }

    //@Test
    @InSequence(1)
    @OperateOnDeployment("shm-remotecv-test")
    public void deploySHMTestEAR() throws Exception {
        this.deployer.deploy("shm-remotecv-test");
    }

    //@Test
    @InSequence(2)
    public void initialize() {
        databean.initMOdata();
    }

    //@Test
    @InSequence(3)
    public void testCreateCV() throws Exception {
        assertTrue(cvRemoteServiceTestFactory.getConfigurationVersionManagementServiceRemote().createCV(MECONTEXT_NAME, CV_NAME, CV_IDENTITY, ""));
        final Map<String, Object> attributes = new HashMap<String, Object>();
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        storedConfigurationVersion.put(DpsTestBase.STORED_CONFIGRUATION_VERSION_DATE, new Date().toString());
        storedConfigurationVersion.put(DpsTestBase.STORED_CONFIGRUATION_VERSION_IDENTITY, CV_IDENTITY);
        storedConfigurationVersion.put(DpsTestBase.STORED_CONFIGRUATION_VERSION_NAME, CV_NAME);
        storedConfigurationVersion.put(DpsTestBase.STORED_CONFIGRUATION_VERSION_OPERATOR_COMMENT, "");
        storedConfigurationVersion.put(DpsTestBase.STORED_CONFIGRUATION_VERSION_OPERATOR_NAME, "");
        storedConfigurationVersion.put(DpsTestBase.STORED_CONFIGRUATION_VERSION_STATUS, "OK");
        storedConfigurationVersion.put(DpsTestBase.STORED_CONFIGRUATION_VERSION_TYPE, "OTHER");
        storedConfigurationVersion.put(DpsTestBase.STORED_CONFIGRUATION_VERSION_UPGRADE_PACKAGE_ID, "Some Dummy ID");
        storedConfigurationVersionList.add(storedConfigurationVersion);
        attributes.put(DpsTestBase.STORED_CONFIGURATION_VERSION, storedConfigurationVersionList);
        databean.moUpdateROAttrs(DpsTestBase.CVMO_FDN, attributes);
    }

    //@Test
    @InSequence(4)
    public void testSetStartableCV() throws Exception {
        assertTrue(cvRemoteServiceTestFactory.getConfigurationVersionManagementServiceRemote().setStartableCV(MECONTEXT_NAME, CV_NAME));
    }

    //@Test
    @InSequence(5)
    public void testSetFirstInRollBackLIst() throws Exception {
        assertTrue(cvRemoteServiceTestFactory.getConfigurationVersionManagementServiceRemote().setCVFirstInRollBackList(MECONTEXT_NAME, CV_NAME));
    }

    //@Test
    @InSequence(7)
    public void delete() {
        databean.delete();
    }

}
