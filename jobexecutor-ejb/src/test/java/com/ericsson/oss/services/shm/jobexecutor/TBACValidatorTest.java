/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.jobexecutor;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJBTransactionRolledbackException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.testng.Assert;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.SecurityViolationException;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.tbac.api.SHMTBACHandler;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobTemplate;
import com.ericsson.oss.services.shm.jobs.common.modelentities.Schedule;
import com.ericsson.oss.services.shm.networkelement.NetworkElementResponse;
import com.ericsson.oss.services.shm.tbac.TBACResponse;
import com.ericsson.oss.services.shm.tbac.models.TBACConfigurationProvider;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

@RunWith(value = MockitoJUnitRunner.class)
public class TBACValidatorTest {

    @InjectMocks
    private TBACValidator ObjectUnderTest;

    @Mock
    private JobTemplate jobTemplateMock;

    @Mock
    private TBACConfigurationProvider tbacConfigurationProvider;

    @Mock
    private JobConfiguration jobConfiguration;

    @Mock
    private Schedule mainSchedule;

    @Mock
    private SHMTBACHandler shmTbacHandler;

    @Mock
    JobUpdateService jobUpdateService;

    @Mock
    private NetworkElement networkElement, networkElement1;

    @Mock
    private WorkflowInstanceNotifier workflowInstanceNotifier;

    @Mock
    private SystemRecorder systemRecorder;

    @Test
    public void testValidateTBACWhenTbacForAllNodesAsSingleTarget() {
        final long mainJobId = 12345;
        final Map<String, Object> mainJobAttribute = new HashMap<>();
        final String[] networkElementsArray = new String[2];
        final List<String> allNodes = new ArrayList<>();
        allNodes.add("node1");
        allNodes.add("node2");
        allNodes.toArray(networkElementsArray);

        final List<NetworkElement> supportedNodes = new ArrayList<>();
        final Map<NetworkElement, String> unSupportedNodes = new HashMap<>();
        supportedNodes.add(networkElement);
        unSupportedNodes.put(networkElement1, "Unsupported Node");
        final NetworkElementResponse networkElementResponse = new NetworkElementResponse();
        networkElementResponse.setSupportedNes(supportedNodes);
        networkElementResponse.setUnsupportedNes(unSupportedNodes);

        when(tbacConfigurationProvider.isTBACAtJobLevel()).thenReturn(true);
        when(networkElement.getName()).thenReturn("node1");
        when(networkElement1.getName()).thenReturn("node2");
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfiguration);
        when(jobConfiguration.getMainSchedule()).thenReturn(mainSchedule);
        when(mainSchedule.getExecMode()).thenReturn(ExecMode.IMMEDIATE);
        when(shmTbacHandler.isAuthorized(networkElementsArray)).thenReturn(false);
        final TBACResponse tbacResponse = ObjectUnderTest.validateTBAC(networkElementResponse, jobTemplateMock, mainJobId, mainJobAttribute);
        Assert.assertTrue(tbacResponse.isTBACValidationSuccess());
        Assert.assertTrue(tbacResponse.isTbacValidationToBeDoneForAllNodesAsSingleTarget());

    }

    @Test
    public void testValidateTBACWhenTbacForAllNodesAsSingleTargetForScheduleJob() {
        final long mainJobId = 12345;
        final Map<String, Object> mainJobAttribute = new HashMap<>();
        final String[] networkElementsArray = new String[2];
        final List<String> allNodes = new ArrayList<>();
        allNodes.add("node1");
        allNodes.add("node2");
        allNodes.toArray(networkElementsArray);

        final List<NetworkElement> supportedNodes = new ArrayList<>();
        final Map<NetworkElement, String> unSupportedNodes = new HashMap<>();
        supportedNodes.add(networkElement);
        unSupportedNodes.put(networkElement1, "Unsupported Node");
        final NetworkElementResponse networkElementResponse = new NetworkElementResponse();
        networkElementResponse.setSupportedNes(supportedNodes);
        networkElementResponse.setUnsupportedNes(unSupportedNodes);
        when(tbacConfigurationProvider.isTBACAtJobLevel()).thenReturn(true);
        when(networkElement.getName()).thenReturn("node1");
        when(networkElement1.getName()).thenReturn("node2");
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfiguration);
        when(jobConfiguration.getMainSchedule()).thenReturn(mainSchedule);
        when(mainSchedule.getExecMode()).thenReturn(ExecMode.SCHEDULED);
        when(jobTemplateMock.getOwner()).thenReturn("owner");
        when(shmTbacHandler.isAuthorized("owner", networkElementsArray)).thenReturn(false);
        final TBACResponse tbacResponse = ObjectUnderTest.validateTBAC(networkElementResponse, jobTemplateMock, mainJobId, mainJobAttribute);
        Assert.assertTrue(tbacResponse.isTBACValidationSuccess());
        Assert.assertTrue(tbacResponse.isTbacValidationToBeDoneForAllNodesAsSingleTarget());

    }

    @Test
    public void testValidateTBACForAuthorizedNodeForIndividualNodeAsTarget() {
        final Map<String, Object> mainJobAttribute = new HashMap<>();
        final long mainJobId = 12345;
        final String[] networkElementsArray = new String[2];
        final List<String> allNodes = new ArrayList<>();
        allNodes.add("node1");
        allNodes.add("node2");
        allNodes.toArray(networkElementsArray);

        final List<NetworkElement> supportedNodes = new ArrayList<>();
        final Map<NetworkElement, String> unSupportedNodes = new HashMap<>();
        supportedNodes.add(networkElement);
        unSupportedNodes.put(networkElement1, "Unsupported Node");
        final NetworkElementResponse networkElementResponse = new NetworkElementResponse();
        networkElementResponse.setSupportedNes(supportedNodes);
        networkElementResponse.setUnsupportedNes(unSupportedNodes);
        when(tbacConfigurationProvider.isTBACAtJobLevel()).thenReturn(false);
        when(networkElement.getName()).thenReturn("node1");
        when(networkElement1.getName()).thenReturn("node2");
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfiguration);
        when(jobConfiguration.getMainSchedule()).thenReturn(mainSchedule);
        when(mainSchedule.getExecMode()).thenReturn(ExecMode.IMMEDIATE);
        when(shmTbacHandler.isAuthorized(Matchers.anyString())).thenReturn(true);
        final TBACResponse tbacResponse = ObjectUnderTest.validateTBAC(networkElementResponse, jobTemplateMock, mainJobId, mainJobAttribute);
        Assert.assertTrue(tbacResponse.isTBACValidationSuccess());
        Assert.assertFalse(tbacResponse.isTbacValidationToBeDoneForAllNodesAsSingleTarget());
        Assert.assertTrue(tbacResponse.getUnAuthorizedNes().isEmpty());

    }

    @Test
    public void testValidateTBACForUnAuthorizedNodeForIndividualNodeAsTarget() {
        final long mainJobId = 12345;
        final Map<String, Object> mainJobAttribute = new HashMap<>();
        final String[] networkElementsArray = new String[2];
        final List<String> allNodes = new ArrayList<>();
        allNodes.add("node1");
        allNodes.add("node2");
        allNodes.toArray(networkElementsArray);

        final List<NetworkElement> supportedNodes = new ArrayList<>();
        final Map<NetworkElement, String> unSupportedNodes = new HashMap<>();
        supportedNodes.add(networkElement);
        unSupportedNodes.put(networkElement1, "Unsupported Node");
        final NetworkElementResponse networkElementResponse = new NetworkElementResponse();
        networkElementResponse.setSupportedNes(supportedNodes);
        networkElementResponse.setUnsupportedNes(unSupportedNodes);
        when(tbacConfigurationProvider.isTBACAtJobLevel()).thenReturn(false);
        when(networkElement.getName()).thenReturn("node1");
        when(networkElement1.getName()).thenReturn("node2");
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfiguration);
        when(jobConfiguration.getMainSchedule()).thenReturn(mainSchedule);
        when(mainSchedule.getExecMode()).thenReturn(ExecMode.IMMEDIATE);
        when(shmTbacHandler.isAuthorized(Matchers.anyString())).thenReturn(false);
        final TBACResponse tbacResponse = ObjectUnderTest.validateTBAC(networkElementResponse, jobTemplateMock, mainJobId, mainJobAttribute);
        Assert.assertTrue(tbacResponse.isTBACValidationSuccess());
        Assert.assertFalse(tbacResponse.isTbacValidationToBeDoneForAllNodesAsSingleTarget());
        Assert.assertFalse(tbacResponse.getUnAuthorizedNes().isEmpty());

    }

    @Test
    public void testValidateTBACForAuthorizedIndividualNodeAsTargetForScheduleJob() {
        final long mainJobId = 12345;
        final Map<String, Object> mainJobAttribute = new HashMap<>();
        final String[] networkElementsArray = new String[2];
        final List<String> allNodes = new ArrayList<>();
        allNodes.add("node1");
        allNodes.add("node2");
        allNodes.toArray(networkElementsArray);

        final List<NetworkElement> supportedNodes = new ArrayList<>();
        final Map<NetworkElement, String> unSupportedNodes = new HashMap<>();
        supportedNodes.add(networkElement);
        unSupportedNodes.put(networkElement1, "Unsupported Node");
        final NetworkElementResponse networkElementResponse = new NetworkElementResponse();
        networkElementResponse.setSupportedNes(supportedNodes);
        networkElementResponse.setUnsupportedNes(unSupportedNodes);
        when(tbacConfigurationProvider.isTBACAtJobLevel()).thenReturn(false);
        when(networkElement.getName()).thenReturn("node1");
        when(networkElement1.getName()).thenReturn("node2");
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfiguration);
        when(jobConfiguration.getMainSchedule()).thenReturn(mainSchedule);
        when(mainSchedule.getExecMode()).thenReturn(ExecMode.SCHEDULED);
        when(jobTemplateMock.getOwner()).thenReturn("owner");
        when(shmTbacHandler.isAuthorized(Matchers.anyString(), Matchers.anyString())).thenReturn(true);
        final TBACResponse tbacResponse = ObjectUnderTest.validateTBAC(networkElementResponse, jobTemplateMock, mainJobId, mainJobAttribute);
        Assert.assertTrue(tbacResponse.isTBACValidationSuccess());
        Assert.assertFalse(tbacResponse.isTbacValidationToBeDoneForAllNodesAsSingleTarget());
        Assert.assertTrue(tbacResponse.getUnAuthorizedNes().isEmpty());

    }

    @Test
    public void testValidateTBACForUnAuthorizedIndividualNodeAsTargetForScheduleJob() {
        final long mainJobId = 12345;
        final Map<String, Object> mainJobAttribute = new HashMap<>();
        final String[] networkElementsArray = new String[2];
        final List<String> allNodes = new ArrayList<>();
        allNodes.add("node1");
        allNodes.add("node2");
        allNodes.toArray(networkElementsArray);

        final List<NetworkElement> supportedNodes = new ArrayList<>();
        final Map<NetworkElement, String> unSupportedNodes = new HashMap<>();
        supportedNodes.add(networkElement);
        unSupportedNodes.put(networkElement1, "Unsupported Node");
        final NetworkElementResponse networkElementResponse = new NetworkElementResponse();
        networkElementResponse.setSupportedNes(supportedNodes);
        networkElementResponse.setUnsupportedNes(unSupportedNodes);
        when(tbacConfigurationProvider.isTBACAtJobLevel()).thenReturn(false);
        when(networkElement.getName()).thenReturn("node1");
        when(networkElement1.getName()).thenReturn("node2");
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfiguration);
        when(jobConfiguration.getMainSchedule()).thenReturn(mainSchedule);
        when(mainSchedule.getExecMode()).thenReturn(ExecMode.SCHEDULED);
        when(jobTemplateMock.getOwner()).thenReturn("owner");
        when(shmTbacHandler.isAuthorized(Matchers.anyString(), Matchers.anyString())).thenReturn(false);
        final TBACResponse tbacResponse = ObjectUnderTest.validateTBAC(networkElementResponse, jobTemplateMock, mainJobId, mainJobAttribute);
        Assert.assertTrue(tbacResponse.isTBACValidationSuccess());
        Assert.assertFalse(tbacResponse.isTbacValidationToBeDoneForAllNodesAsSingleTarget());
        Assert.assertFalse(tbacResponse.getUnAuthorizedNes().isEmpty());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testValidateTBACWhenThrowSecurityVoilationExp() {
        final long mainJobId = 12345;
        final Map<String, Object> mainJobAttribute = new HashMap<>();
        final String[] networkElementsArray = new String[2];
        final List<String> allNodes = new ArrayList<>();
        allNodes.add("node1");
        allNodes.add("node2");
        allNodes.toArray(networkElementsArray);

        final List<NetworkElement> supportedNodes = new ArrayList<>();
        final Map<NetworkElement, String> unSupportedNodes = new HashMap<>();
        supportedNodes.add(networkElement);
        unSupportedNodes.put(networkElement1, "Unsupported Node");
        final NetworkElementResponse networkElementResponse = new NetworkElementResponse();
        networkElementResponse.setSupportedNes(supportedNodes);
        networkElementResponse.setUnsupportedNes(unSupportedNodes);
        when(tbacConfigurationProvider.isTBACAtJobLevel()).thenReturn(false);
        when(networkElement.getName()).thenReturn("node1");
        when(networkElement1.getName()).thenReturn("node2");
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfiguration);
        when(jobConfiguration.getMainSchedule()).thenReturn(mainSchedule);
        when(mainSchedule.getExecMode()).thenReturn(ExecMode.IMMEDIATE);
        when(jobTemplateMock.getOwner()).thenReturn("owner");
        final SecurityViolationException securityViolationException = new SecurityViolationException("not found in DB");
        when(shmTbacHandler.isAuthorized(Matchers.anyString(), Matchers.anyString())).thenThrow(securityViolationException);
        final TBACResponse tbacResponse = ObjectUnderTest.validateTBAC(networkElementResponse, jobTemplateMock, mainJobId, mainJobAttribute);
        Assert.assertFalse(tbacResponse.isTBACValidationSuccess());
        Assert.assertFalse(tbacResponse.isTbacValidationToBeDoneForAllNodesAsSingleTarget());
        Assert.assertNotNull(tbacResponse.getUnAuthorizedNes());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testValidateTBACWhenThrowIllegalArgExp() {
        final long mainJobId = 12345;
        final Map<String, Object> mainJobAttribute = new HashMap<>();
        final String[] networkElementsArray = new String[2];
        final List<String> allNodes = new ArrayList<>();
        allNodes.add("node1");
        allNodes.add("node2");
        allNodes.toArray(networkElementsArray);

        final List<NetworkElement> supportedNodes = new ArrayList<>();
        final Map<NetworkElement, String> unSupportedNodes = new HashMap<>();
        supportedNodes.add(networkElement);
        unSupportedNodes.put(networkElement1, "Unsupported Node");
        final NetworkElementResponse networkElementResponse = new NetworkElementResponse();
        networkElementResponse.setSupportedNes(supportedNodes);
        networkElementResponse.setUnsupportedNes(unSupportedNodes);
        when(tbacConfigurationProvider.isTBACAtJobLevel()).thenReturn(false);
        when(networkElement.getName()).thenReturn("node1");
        when(networkElement1.getName()).thenReturn("node2");
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfiguration);
        when(jobConfiguration.getMainSchedule()).thenReturn(mainSchedule);
        when(mainSchedule.getExecMode()).thenReturn(ExecMode.IMMEDIATE);
        when(jobTemplateMock.getOwner()).thenReturn("owner");
        final IllegalArgumentException illegalArgumentException = new IllegalArgumentException("not found in DB");
        when(shmTbacHandler.isAuthorized(Matchers.anyString(), Matchers.anyString())).thenThrow(illegalArgumentException);
        final TBACResponse tbacResponse = ObjectUnderTest.validateTBAC(networkElementResponse, jobTemplateMock, mainJobId, mainJobAttribute);
        Assert.assertFalse(tbacResponse.isTBACValidationSuccess());
        Assert.assertFalse(tbacResponse.isTbacValidationToBeDoneForAllNodesAsSingleTarget());
        Assert.assertNotNull(tbacResponse.getUnAuthorizedNes());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testValidateTBACWhenThrowException() {
        final long mainJobId = 12345;
        final Map<String, Object> mainJobAttribute = new HashMap<>();
        final String[] networkElementsArray = new String[2];
        final List<String> allNodes = new ArrayList<>();
        allNodes.add("node1");
        allNodes.toArray(networkElementsArray);

        final List<NetworkElement> supportedNodes = new ArrayList<>();
        final Map<NetworkElement, String> unSupportedNodes = new HashMap<>();
        supportedNodes.add(networkElement);
        unSupportedNodes.put(networkElement1, "Unsupported Node");
        final NetworkElementResponse networkElementResponse = new NetworkElementResponse();
        networkElementResponse.setSupportedNes(supportedNodes);
        networkElementResponse.setUnsupportedNes(unSupportedNodes);
        when(tbacConfigurationProvider.isTBACAtJobLevel()).thenReturn(false);
        when(networkElement.getName()).thenReturn("node1");
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfiguration);
        when(jobConfiguration.getMainSchedule()).thenReturn(mainSchedule);
        when(mainSchedule.getExecMode()).thenReturn(ExecMode.IMMEDIATE);
        when(jobTemplateMock.getOwner()).thenReturn("owner");
        final EJBTransactionRolledbackException e = new EJBTransactionRolledbackException("User ghulam not found in DB");
        when(shmTbacHandler.isAuthorized(Matchers.anyString(), Matchers.anyString())).thenThrow(e);
        final TBACResponse tbacResponse = ObjectUnderTest.validateTBAC(networkElementResponse, jobTemplateMock, mainJobId, mainJobAttribute);
        Assert.assertFalse(tbacResponse.isTBACValidationSuccess());
        Assert.assertFalse(tbacResponse.isTbacValidationToBeDoneForAllNodesAsSingleTarget());
        Assert.assertNotNull((tbacResponse.getUnAuthorizedNes()));

    }

}
