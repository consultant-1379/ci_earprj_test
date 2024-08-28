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
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.exception.NoNetworkElementAssociatedException;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.UpgradePackageContext;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.VranSoftwareUpgradeEventSender;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.VranUpgradeJobContextBuilder;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.exception.NoVNFManagerFoundException;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.exception.NoVnfIdFoundException;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider.NeJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobLogMessageTemplate;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

public class ExecuteTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteTask.class);

    @Inject
    private VranJobActivityServiceHelper vranJobActivityService;

    @Inject
    private NeJobPropertiesPersistenceProvider neJobPropertiesPersistenceProvider;

    @Inject
    private TaskBase taskBase;

    @Inject
    private VranJobActivityServiceHelper vranJobActivityServiceHelper;

    @Inject
    private VranSoftwareUpgradeEventSender vranSoftwareUpgradeEventSender;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private VranUpgradeJobContextBuilder vranUpgradeJobContextBuilder;

    @Inject
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    protected UpgradePackageContext upgradePackageContext;

    public void executeTask(final JobActivityInfo jobActivityInfo) {

        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final Map<String, Object> processVariables = new HashMap<>();
        String activityName = jobActivityInfo.getActivityName();
        final long activityJobId = jobActivityInfo.getActivityJobId();
        try {
            addVnfNeJobProperties(activityJobId);
            upgradePackageContext = buildUpgradeJobContext(activityJobId);
            activityName = getActivityToBeTriggered();
            persistJobDetails(activityName, activityJobId);
            subscribeToNotifications(upgradePackageContext, jobActivityInfo);
            final Map<String, Object> eventAttributes = buildEventAttributes(upgradePackageContext, activityName, activityJobId);
            requestActivityInitiation(activityJobId, activityName, upgradePackageContext.getVnfmFdn(), eventAttributes);

            LOGGER.debug("ActivityJob ID - [{}] : Execute of {} activity is triggered successfully.", activityJobId, activityName);
        } catch (final NoVNFManagerFoundException vnfmException) {
            handleException(jobLogs, processVariables, upgradePackageContext, activityName, activityJobId, vnfmException);
        } catch (final NoVnfIdFoundException noVnfIdFoundException) {
            handleException(jobLogs, processVariables, upgradePackageContext, activityName, activityJobId, noVnfIdFoundException);
        } catch (final NoNetworkElementAssociatedException noNetworkElementAssociatedException) {
            handleException(jobLogs, processVariables, upgradePackageContext, activityName, activityJobId, noNetworkElementAssociatedException);
        } catch (final Exception e) {
            handleException(jobLogs, processVariables, upgradePackageContext, activityName, activityJobId, e);
        }
        jobAttributesPersistenceProvider.persistJobLogs(activityJobId, jobLogs);
    }

    public void persistJobDetails(final String activityName, final long activityJobId) {
        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final List<Map<String, Object>> activityJobProperties = new ArrayList<>();
        activityUtils.prepareJobPropertyList(activityJobProperties, VranJobConstants.ACTION_TRIGGERED, activityName);
        activityUtils.prepareJobPropertyList(activityJobProperties, ActivityConstants.IS_ACTIVITY_TRIGGERED, ActivityConstants.CHECK_TRUE);

        jobLogs.add(vranJobActivityServiceHelper.buildJobLog(String.format(VranJobLogMessageTemplate.ACTION_ABOUT_TO_TRIGGER, activityName, upgradePackageContext.getVnfId()), new Date(),
                JobLogLevel.INFO.toString()));

        jobAttributesPersistenceProvider.persistJobPropertiesAndLogs(activityJobId, activityJobProperties, jobLogs);
    }

    public void addVnfNeJobProperties(final long activityJobId) {
        neJobPropertiesPersistenceProvider.persistVnfInformation(activityJobId);
    }

    public UpgradePackageContext buildUpgradeJobContext(final long activityJobId) {
        return vranUpgradeJobContextBuilder.build(activityJobId);
    }

    public String getActivityToBeTriggered() {

        return null;
    }

    public Map<String, Object> buildEventAttributes(final UpgradePackageContext vranUpgradeInformation, final String activityName, final long activityJobId) {
        return taskBase.buildEventAttributes(vranUpgradeInformation, activityName, activityJobId);

    }

    public void subscribeToNotifications(final UpgradePackageContext upgradePackageContext, final JobActivityInfo jobActivityInfo) {
        final String subscriptionKey = taskBase.buildSubscriptionKey(upgradePackageContext, jobActivityInfo.getActivityJobId());
        activityUtils.subscribeToMoNotifications(subscriptionKey, jobActivityInfo.getActivityJobId(), jobActivityInfo);
    }

    public void requestActivityInitiation(final long activityJobId, final String activityName, final String vnfManagerFdn, final Map<String, Object> eventAttributes) {
        vranSoftwareUpgradeEventSender.sendSoftwareUpgradeActionRequest(activityJobId, activityName, vnfManagerFdn, eventAttributes);
    }

    public void handleException(final List<Map<String, Object>> jobLogs, final Map<String, Object> processVariables, final UpgradePackageContext upgradePackageContext, final String activityName,
            final long activityJobId, final Exception e) {
        LOGGER.error("ActivityJob ID - [{}] : Failed to execute {} activity. Reason : {}", activityJobId, activityName, e.getMessage(), e);
        final String logMessage = String.format(VranJobLogMessageTemplate.ACTIVITY_FAILED_WITH_REASON, activityName, e.getMessage());
        jobLogs.add(vranJobActivityServiceHelper.buildJobLog(logMessage, new Date(), JobLogLevel.ERROR.toString()));
        vranJobActivityService.failActivity(activityJobId, jobLogs, upgradePackageContext.getJobEnvironment(), activityName, processVariables);
    }

}
