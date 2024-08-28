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
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.service;

import java.util.Map;

import org.junit.Before;
import org.mockito.Mock;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.vran.model.response.VranUpgradeJobResponse;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.UpgradePackageContext;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.VranSoftwareUpgradeEventSender;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.VranUpgradeJobContextBuilder;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.exception.NoVNFManagerFoundException;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider.VnfInformationProvider;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors.DeleteActivityNotificationProcessor;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors.JobCancelActivityNotificationProcessor;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks.HandleTimeoutTask;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks.PrecheckTask;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks.TaskBase;
import com.ericsson.oss.services.shm.es.vran.notifications.api.VranSoftwareUpgradeJobResponse;
import com.ericsson.oss.services.shm.vran.common.VNFMInformationProvider;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;

public class BaseService {

    @Mock
    protected VranUpgradeJobResponse vranUpgradeJobResponse;

    @Mock
    protected VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse;

    @Mock
    protected JobActivityInfo jobActivityInformation;

    @Mock
    protected VranUpgradeJobContextBuilder vranUpgradeJobContextBuilder;

    @Mock
    protected UpgradePackageContext vranUpgradeInformation;

    @Mock
    protected VranJobActivityServiceHelper vranJobActivityService;

    @Mock
    protected JobUpdateService jobUpdateService;

    @Mock
    protected ActivityUtils activityUtils;

    @Mock
    protected ActivityStepResult activityStepResult;

    @Mock
    protected SystemRecorder systemRecorder;

    @Mock
    protected JobEnvironment jobContext;

    @Mock
    protected VranSoftwareUpgradeEventSender vranSoftwareUpgradeEventSender;

    @Mock
    protected Map<String, Object> activityJobProperties;

    @Mock
    protected NoVNFManagerFoundException vnfmException;

    @Mock
    protected VnfInformationProvider vnfInfoProvider;

    @Mock
    protected VNFMInformationProvider virtualNetwrokFunctionManagerInformationProvider;

    @Mock
    protected PrecheckTask precheckTask;

    @Mock
    protected VranJobActivityServiceHelper vranJobActivityServiceHelper;

    @Mock
    protected TaskBase taskBase;

    @Mock
    protected UpgradePackageContext upgradePackageContext;

    @Mock
    private DeleteActivityNotificationProcessor deleteActivityNotificationProcessor;

    @Mock
    protected JobCancelActivityNotificationProcessor jobCancelActivityNotificationProcessor;

    @Mock
    protected HandleTimeoutTask handleTimeoutTask;

    static final long activityJobId = 345;
    static final String businessKey = "VNFM12345";

    @Before
    public void mockJobEnvironment() {
        vranSoftwareUpgradeJobResponse = new VranSoftwareUpgradeJobResponse();
        vranSoftwareUpgradeJobResponse.setActivityJobId(activityJobId);
    }

}
