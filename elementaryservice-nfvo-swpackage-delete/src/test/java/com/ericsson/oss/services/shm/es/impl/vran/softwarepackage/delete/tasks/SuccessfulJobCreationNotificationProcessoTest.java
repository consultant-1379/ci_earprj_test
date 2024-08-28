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

import java.util.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextForNfvo;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.persistence.DeleteSoftwarePackagePersistenceProvider;
import com.ericsson.oss.services.shm.es.vran.onboard.api.notifications.NfvoSoftwarePackageJobResponse;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityUtil;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

@RunWith(MockitoJUnitRunner.class)
public class SuccessfulJobCreationNotificationProcessoTest {

    @InjectMocks
    private SuccessfulJobCreationNotificationProcessor successfulJobCreationNotificationProcessor;

    @Mock
    private DeleteSoftwarePackagePersistenceProvider deleteSoftwarePackagePersistenceProvider;

    @Mock
    private VranJobActivityServiceHelper vranJobActivityService;

    @Mock
    private TasksBase tasksBase;

    @Mock
    private NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse;

    @Mock
    private DeletePackageContextForNfvo deletePackageContextForNfvo;

    @Mock
    private JobActivityInfo jobActivityInfo;

    @Mock
    private VranJobActivityUtil vranJobActivityUtil;

    private static final long ACTIVITY_JOB_ID = 12345;

    @Mock
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Before
    public void setup() {
        when(nfvoSoftwarePackageJobResponse.getJobId()).thenReturn("JOBID_1");
    }

    @Test
    public void testProcessCreateJobResponse() {
        when(vranJobActivityUtil.incrementTime(nfvoSoftwarePackageJobResponse.getNotificationTimeStamp(), 5)).thenReturn(Calendar.getInstance());
        Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, VranJobConstants.VNF_JOB_ID);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, ACTIVITY_JOB_ID);
        when(vranJobActivityService.buildJobProperty(VranJobConstants.VNF_JOB_ID, nfvoSoftwarePackageJobResponse.getJobId())).thenReturn(jobProperty);
        successfulJobCreationNotificationProcessor.processCreateJobResponse(nfvoSoftwarePackageJobResponse, deletePackageContextForNfvo, jobActivityInfo, ACTIVITY_JOB_ID);
        verify(tasksBase).unsubscribeNotifications(nfvoSoftwarePackageJobResponse, jobActivityInfo);
    }

}
