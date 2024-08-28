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
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.UpgradePackageContext;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskutils.NotificationTaskUtils;
import com.ericsson.oss.services.shm.es.vran.notifications.api.VranSoftwareUpgradeJobResponse;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.common.VranActivityUtil;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobLogMessageTemplate;
import com.ericsson.oss.services.shm.vran.constants.VranUprgradeConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityUtil;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

public class JobLogsPersistenceProvider {

    @Inject
    private VranJobActivityServiceHelper vranJobActivityServiceHelper;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Inject
    private NotificationTaskUtils notificationTaskUtils;

    @Inject
    private VranJobActivityUtil vranJobActivityUtil;

    @Inject
    private VranActivityUtil vranActivityUtil;

    public void persistActivityJobLogs(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse, final String operationName) {
        persistActivityJobLogs(vranSoftwareUpgradeJobResponse, operationName, null);
    }

    public void persistActivityJobLogs(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse, final String operationName, final List<Map<String, Object>> jobProperties) {
        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final String jobLogMessage = notificationTaskUtils.prepareJobLogMessage(vranSoftwareUpgradeJobResponse, operationName);
        jobLogs.add(vranJobActivityServiceHelper.buildJobLog(jobLogMessage, vranSoftwareUpgradeJobResponse.getNotificationReceivedTime(), JobLogLevel.INFO.toString()));
        jobAttributesPersistenceProvider.persistJobPropertiesAndLogs(vranSoftwareUpgradeJobResponse.getActivityJobId(), jobProperties, jobLogs);
    }

    public void persistActivityInitiationJobDetails(final long activityJobId, final UpgradePackageContext vranUpgradeInfo, final String activityName, final String deleteInitiatedFrom,
            final String vnfmFdn) {
        final List<Map<String, Object>> jobLogs = new ArrayList<>();

        final List<Map<String, Object>> activityJobProperties = new ArrayList<>();
        activityUtils.prepareJobPropertyList(activityJobProperties, VranJobConstants.ACTION_TRIGGERED, activityName);
        activityUtils.prepareJobPropertyList(activityJobProperties, ActivityConstants.IS_ACTIVITY_TRIGGERED, ActivityConstants.CHECK_TRUE);

        final String vnfmName = vranActivityUtil.getNeNameFromFdn(vnfmFdn);

        if (activityName.equalsIgnoreCase(VranUprgradeConstants.DELETE_ACTIVITY)) {
            buildDeleteActivityJobLogs(vranUpgradeInfo, jobLogs);
        } else {
            buildActivityInitiationJobLogs(vranUpgradeInfo, activityName, deleteInitiatedFrom, jobLogs, vnfmName);
        }
        jobAttributesPersistenceProvider.persistJobPropertiesAndLogs(activityJobId, activityJobProperties, jobLogs);

    }

    private void buildActivityInitiationJobLogs(final UpgradePackageContext upgradePackageContext, final String activityName, final String deleteInitiatedFrom, final List<Map<String, Object>> jobLogs,
            final String vnfmName) {
        if (isDeleteInitiatedFromCreate(deleteInitiatedFrom)) {
            jobLogs.add(vranJobActivityServiceHelper.buildJobLog(String.format("Found no upgrade jobs for VNFID: \"%s\" on VNFM: \"%s\" ", upgradePackageContext.getVnfId(), vnfmName),
                    vranJobActivityUtil.incrementTime(null, 1).getTime(), JobLogLevel.INFO.toString()));
        }
        jobLogs.add(vranJobActivityServiceHelper.buildJobLog(String.format(VranJobLogMessageTemplate.ACTION_ABOUT_TO_TRIGGER, activityName, upgradePackageContext.getVnfId()), new Date(),
                JobLogLevel.INFO.toString()));
    }

    private boolean isDeleteInitiatedFromCreate(final String deleteInitiatedFrom) {
        return deleteInitiatedFrom.equalsIgnoreCase(ActivityConstants.CREATE);
    }

    private void buildDeleteActivityJobLogs(final UpgradePackageContext upgradePackageContext, final List<Map<String, Object>> jobLogs) {
        jobLogs.add(vranJobActivityServiceHelper.buildJobLog(String.format("Checking if any previous upgrade exists for VNFID : \"%s\" ", upgradePackageContext.getVnfId()),
                vranJobActivityUtil.incrementTime(null, 1).getTime(), JobLogLevel.INFO.toString()));

    }

}
