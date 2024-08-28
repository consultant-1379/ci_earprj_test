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
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks.ExecuteTask;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobLogMessageTemplate;
import com.ericsson.oss.services.shm.vran.constants.VranUprgradeConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

public class PrepareActivityExecuteProcessor extends ExecuteTask {

    @Inject
    private VranJobActivityServiceHelper vranJobActivityServiceHelper;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Override
    public String getActivityToBeTriggered() {
        String activityName;
        final int vnfJobId = upgradePackageContext.getVnfJobId();
        if (isDeleteActivityNotTriggered(vnfJobId)) {
            activityName = VranUprgradeConstants.DELETE_ACTIVITY;
        } else {
            activityName = ActivityConstants.PREPARE;
        }
        return activityName;
    }

    private boolean isDeleteActivityNotTriggered(final int vnfJobId) {
        return vnfJobId < 0;
    }

    @Override
    public void persistJobDetails(final String activityName, final long activityJobId) {
        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final List<Map<String, Object>> activityJobProperties = new ArrayList<>();
        activityUtils.prepareJobPropertyList(activityJobProperties, VranJobConstants.ACTION_TRIGGERED, activityName);
        activityUtils.prepareJobPropertyList(activityJobProperties, ActivityConstants.IS_ACTIVITY_TRIGGERED, ActivityConstants.CHECK_TRUE);

        if (VranUprgradeConstants.DELETE_ACTIVITY.equals(activityName)) {
            jobLogs.add(vranJobActivityServiceHelper.buildJobLog(String.format("Checking if any previous upgrade exists for VNFID : \"%s\" ", upgradePackageContext.getVnfId()), new Date(),
                    JobLogLevel.INFO.toString()));
        } else {
            jobLogs.add(vranJobActivityServiceHelper.buildJobLog(String.format(VranJobLogMessageTemplate.ACTION_ABOUT_TO_TRIGGER, activityName, upgradePackageContext.getVnfId()), new Date(),
                    JobLogLevel.INFO.toString()));
        }

        jobAttributesPersistenceProvider.persistJobPropertiesAndLogs(activityJobId, activityJobProperties, jobLogs);
    }

}
