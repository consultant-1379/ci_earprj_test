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
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskutils.NotificationTaskUtils;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.common.VranActivityUtil;
import com.ericsson.oss.services.shm.vran.constants.VranUprgradeConstants;

@RunWith(MockitoJUnitRunner.class)
public class JobLogsPersistenceProviderTest extends ProviderTestBase {

    @InjectMocks
    private JobLogsPersistenceProvider jobLogsPersistenceProvider;

    @Mock
    private NotificationTaskUtils notificationTaskUtils;

    private static String vnfManagerFdn = "fdn";

    @Mock
    private VranActivityUtil vranActivityUtil;

    @Before
    public void mockJobEnvironment() {
        super.mockJobEnvironment();
    }

    @Test
    public void testPersistActivityJobLogsVranSoftwareUpgradeJobResponseString() {
        jobLogsPersistenceProvider.persistActivityJobLogs(vranSoftwareUpgradeJobResponse, VranUprgradeConstants.PREPARE_OPERATION);

    }

    @Test
    public void testPersistActivityJobLogsVranSoftwareUpgradeJobResponseStringListOfMapOfStringObject() {
        final List<Map<String, Object>> jobProperties = new ArrayList<Map<String, Object>>();
        jobLogsPersistenceProvider.persistActivityJobLogs(vranSoftwareUpgradeJobResponse, VranUprgradeConstants.PREPARE_OPERATION, jobProperties);
    }

    @Test
    public void testPersistActivityInitiationJobDetailsFrom_Confrim() {
        jobLogsPersistenceProvider.persistActivityInitiationJobDetails(activityJobId, upgradePackageContext, ActivityConstants.PREPARE, ActivityConstants.CONFIRM, vnfManagerFdn);
    }

    @Test
    public void testPersistActivityInitiationJobDetailsFrom_Create() {
        when(vranJobActivityUtil.incrementTime(null, 1)).thenReturn(Calendar.getInstance());
        jobLogsPersistenceProvider.persistActivityInitiationJobDetails(activityJobId, upgradePackageContext, ActivityConstants.PREPARE, ActivityConstants.CREATE, vnfManagerFdn);
    }

    @Test
    public void testPersistActivityInitiationJobDetails_Delete() {

        when(vranJobActivityUtil.incrementTime(null, 1)).thenReturn(Calendar.getInstance());
        jobLogsPersistenceProvider.persistActivityInitiationJobDetails(activityJobId, upgradePackageContext, VranUprgradeConstants.DELETE_ACTIVITY, ActivityConstants.CREATE, vnfManagerFdn);
    }
}
