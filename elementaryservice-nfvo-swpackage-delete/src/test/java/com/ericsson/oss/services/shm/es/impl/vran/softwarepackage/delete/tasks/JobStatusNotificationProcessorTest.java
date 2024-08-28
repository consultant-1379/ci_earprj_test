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
package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.tasks;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.retry.DpsRetryConfigurationParamProvider;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextForNfvo;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.persistence.DeleteJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.persistence.DeleteSoftwarePackagePersistenceProvider;
import com.ericsson.oss.services.shm.es.vran.onboard.api.notifications.NfvoSoftwarePackageJobResponse;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityUtil;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;
import com.ericsson.oss.services.shm.vran.shared.persistence.VnfSoftwarePackagePersistenceProvider;

@RunWith(MockitoJUnitRunner.class)
public class JobStatusNotificationProcessorTest {

    @InjectMocks
    private JobStatusNotificationProcessor jobStatusNotificationProcessor;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private DeleteSoftwarePackagePersistenceProvider deleteSoftwarePackagePersistenceProvider;

    @Mock
    private DeleteJobPropertiesPersistenceProvider deleteJobPropertiesPersistenceProvider;

    @Mock
    private VnfSoftwarePackagePersistenceProvider vnfSoftwarePackagePersistenceProvider;

    @Mock
    private VranJobActivityServiceHelper vranJobActivityService;

    @Mock
    private TasksBase tasksBase;

    @Mock
    private SystemRecorder systemRecorder;

    @Mock
    private NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse;

    @Mock
    private JobActivityInfo jobActivityInfo;

    @Mock
    private DeletePackageContextForNfvo deletePackageContextForNfvo;

    @Mock
    private JobEnvironment jobContext;

    private static final long ACTIVITY_JOB_ID = 12345;

    private static final long PO_ID = 12345;

    private static final String JOB_ID = "JOB_1";

    @Mock
    private DpsRetryConfigurationParamProvider dpsConfigurationParamProvider;

    @Mock
    private PersistenceObject persistenceObject;

    @Mock
    private VranJobActivityUtil vranJobActivityUtil;

    @Mock
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Before
    public void setUp() throws Exception {
        when(nfvoSoftwarePackageJobResponse.getActivityJobId()).thenReturn(ACTIVITY_JOB_ID);
        when(nfvoSoftwarePackageJobResponse.getJobId()).thenReturn(JOB_ID);
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobContext);
        when(jobContext.getNodeName()).thenReturn("TestNode");
        when(deletePackageContextForNfvo.getContext()).thenReturn(jobContext);
        when(deletePackageContextForNfvo.getCurrentPackage()).thenReturn("TestVPPPackage");

        when(persistenceObject.getPoId()).thenReturn(PO_ID);
        when(vranJobActivityUtil.incrementTime(nfvoSoftwarePackageJobResponse.getNotificationTimeStamp(), 10)).thenReturn(Calendar.getInstance());
    }

    @Test
    public void processJobInprogressResponse() {
        when(nfvoSoftwarePackageJobResponse.getNotificationTimeStamp()).thenReturn(new Date());
        when(dpsConfigurationParamProvider.getdpsRetryCount()).thenReturn(2);
        jobStatusNotificationProcessor.processJobInprogressResponse(nfvoSoftwarePackageJobResponse, deletePackageContextForNfvo, jobActivityInfo);
        verify(tasksBase).unsubscribeNotifications(nfvoSoftwarePackageJobResponse, jobActivityInfo);
        verify(tasksBase).requestDeleteSoftwarePackageJobStatus(ACTIVITY_JOB_ID, JOB_ID, deletePackageContextForNfvo, jobActivityInfo);
    }

    @Test
    public void processJobSuccessResponseFromNfvoWithNullPersistanceObject() {
        when(vnfSoftwarePackagePersistenceProvider.getVnfSoftwarePackageEntity("TestVPPPackage")).thenReturn(null);
        jobStatusNotificationProcessor.processJobSuccessResponse(nfvoSoftwarePackageJobResponse, deletePackageContextForNfvo, jobActivityInfo);
        verify(tasksBase).unsubscribeNotifications(nfvoSoftwarePackageJobResponse, jobActivityInfo);
    }

    @Test
    public void processJobSuccessResponseFromNfvoWithNotNullPersistanceObject() {
        when(vnfSoftwarePackagePersistenceProvider.getVnfSoftwarePackageEntity("TestVPPPackage")).thenReturn(persistenceObject);
        when(persistenceObject.getAttribute(VranJobConstants.SW_PACKAGE_LOCATION)).thenReturn(VranJobConstants.NFVO);
        jobStatusNotificationProcessor.processJobSuccessResponse(nfvoSoftwarePackageJobResponse, deletePackageContextForNfvo, jobActivityInfo);
        verify(tasksBase).unsubscribeNotifications(nfvoSoftwarePackageJobResponse, jobActivityInfo);
    }

    @Test
    public void processJobSuccessResponseFromEnmNfvoWithNullPersistanceObject() {
        when(vnfSoftwarePackagePersistenceProvider.getVnfSoftwarePackageEntity("TestVPPPackage")).thenReturn(null);
        jobStatusNotificationProcessor.processJobSuccessResponse(nfvoSoftwarePackageJobResponse, deletePackageContextForNfvo, jobActivityInfo);
        verify(tasksBase).unsubscribeNotifications(nfvoSoftwarePackageJobResponse, jobActivityInfo);
    }

    @Test
    public void processJobSuccessResponseFromEnmNfvoWithNotNullPersistanceObject() {
        when(vnfSoftwarePackagePersistenceProvider.getVnfSoftwarePackageEntity("TestVPPPackage")).thenReturn(persistenceObject);
        when(persistenceObject.getAttribute(VranJobConstants.SW_PACKAGE_LOCATION)).thenReturn(VranJobConstants.SMRS_NFVO);
        when(tasksBase.isPackageAvailableBothInEnmAndNfvo(VranJobConstants.SMRS_NFVO)).thenReturn(true);
        jobStatusNotificationProcessor.processJobSuccessResponse(nfvoSoftwarePackageJobResponse, deletePackageContextForNfvo, jobActivityInfo);
        verify(tasksBase).unsubscribeNotifications(nfvoSoftwarePackageJobResponse, jobActivityInfo);
    }

}
