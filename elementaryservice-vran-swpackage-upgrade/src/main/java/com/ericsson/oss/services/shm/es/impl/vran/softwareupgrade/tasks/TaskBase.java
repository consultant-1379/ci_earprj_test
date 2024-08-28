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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.UpgradePackageContext;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.VranSoftwareUpgradeEventSender;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.VranUpgradeJobContextBuilder;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.exception.NoVNFManagerFoundException;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider.JobLogsPersistenceProvider;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider.VnfInformationProvider;
import com.ericsson.oss.services.shm.es.vran.configuration.ConfigurationParameterValueProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;

public class TaskBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskBase.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private ConfigurationParameterValueProvider configurationParameterValueProvider;

    @Inject
    private VranSoftwareUpgradeEventSender vranSoftwareUpgradeEventSender;

    @Inject
    private VranUpgradeJobContextBuilder vranUpgradeJobContextBuilder;

    @Inject
    private JobLogsPersistenceProvider jogLogsPersistenceProvider;

    @Inject
    private VranJobActivityServiceHelper vranJobActivityServiceHelper;

    @Inject
    private VnfInformationProvider vnfInformationProvider;

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    public void performSoftwareUpgrade(final long activityJobId, final UpgradePackageContext upgradePackageContext, final String activityName, final JobActivityInfo jobActivityInfo,
            final String deleteActivityInitiatedFrom) {
        Map<String, Object> eventAttributes = null;
        String subscriptionKey;
        LOGGER.debug("ActivityJob ID - [{}] : Processing send software upgrade request for activity[{}]. VranJobInformation: {}", activityJobId, activityName, upgradePackageContext);
        final String vnfmFdn = upgradePackageContext.getVnfmFdn();
        if (vnfmFdn != null) {
            jogLogsPersistenceProvider.persistActivityInitiationJobDetails(activityJobId, upgradePackageContext, activityName, deleteActivityInitiatedFrom, vnfmFdn);
            eventAttributes = buildEventAttributes(upgradePackageContext, activityName, activityJobId);
            subscriptionKey = buildSubscriptionKey(upgradePackageContext, activityJobId);
            activityUtils.subscribeToMoNotifications(subscriptionKey, activityJobId, jobActivityInfo);
            vranSoftwareUpgradeEventSender.sendSoftwareUpgradeActionRequest(activityJobId, activityName, vnfmFdn, eventAttributes);
        } else {
            // unSubscribeToMoNotifications?
            activityUtils.unSubscribeToMoNotifications(upgradePackageContext.getVnfId() + upgradePackageContext.getVnfJobId(), activityJobId, jobActivityInfo);
            throw new NoVNFManagerFoundException(VranJobConstants.NO_VNFM_FOUND);
        }
    }

    public Map<String, Object> buildEventAttributes(final UpgradePackageContext upgradePackageContext, final String activityName, final long activityJobId) {

        final Map<String, Object> eventAttributes = new HashMap<>();
        eventAttributes.put(VranJobConstants.VNF_ID, upgradePackageContext.getVnfId());
        eventAttributes.put(VranJobConstants.UPGRADE_JOB_VNF_PACKAGE_ID, upgradePackageContext.getSoftwarePackageName());
        eventAttributes.put(VranJobConstants.UPGRADE_JOB_VNF_DESCRIPTOR_ID, upgradePackageContext.getVnfDescription());
        eventAttributes.put(ShmConstants.NE_NAME, upgradePackageContext.getNodeName());
        eventAttributes.put(ActivityConstants.ACTIVITY_JOB_ID, activityJobId);

        if (!ActivityConstants.CREATE.equals(activityName)) {
            final int noOfRetries = configurationParameterValueProvider.getRetriesForActivityWhenFailure();
            final int intervalInSec = configurationParameterValueProvider.getRetryIntervalInSec();
            eventAttributes.put(VranJobConstants.NO_OF_RETRIES, noOfRetries);
            eventAttributes.put(VranJobConstants.INTERVAL_IN_SECONDS, intervalInSec);
            eventAttributes.put(VranJobConstants.VNF_JOB_ID, upgradePackageContext.getVnfJobId());
            final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
            final List<NetworkElement> networkElements = vnfInformationProvider.fetchNetworkElements(jobEnvironment);
            final Map<String, Object> jobConfigurationDetails = vranJobActivityServiceHelper.getMainJobAttributes(jobEnvironment);
            if(jobConfigurationDetails !=null && networkElements!=null && !networkElements.isEmpty()) {
                final String fallbackTimeout = getFallbackTimeout(jobConfigurationDetails, networkElements.get(0));
                eventAttributes.put(ActivityConstants.FALLBACK_TIMEOUT, fallbackTimeout);
            }
        }
        return eventAttributes;
    }

    private String getFallbackTimeout(final Map<String, Object> jobConfigurationDetails, final NetworkElement networkElement) {
        final List<String> jobPropertyKeys = new ArrayList<>();
        jobPropertyKeys.add(ActivityConstants.FALLBACK_TIMEOUT);
        final Map<String, String> propertyMap = jobPropertyUtils.getPropertyValue(jobPropertyKeys, jobConfigurationDetails, networkElement.getName(), networkElement.getNeType(),
                networkElement.getPlatformType().name());
        return propertyMap.get(ActivityConstants.FALLBACK_TIMEOUT);
    }

    public String buildSubscriptionKey(final UpgradePackageContext upgradePackageContext, final long activityJobId) {
        return upgradePackageContext.getVnfId() + VranJobConstants.SUBSCRIPTION_KEY_DELIMETER + upgradePackageContext.getNodeName() + VranJobConstants.SUBSCRIPTION_KEY_DELIMETER + activityJobId;

    }

    public void unSubscribeNotification(final JobActivityInfo jobActivityInformation, final UpgradePackageContext upgradePackageContext, final long activityJobId) {
        final String subscriptionKey = buildSubscriptionKey(upgradePackageContext, activityJobId);
        activityUtils.unSubscribeToMoNotifications(subscriptionKey, activityJobId, jobActivityInformation);
    }

    public UpgradePackageContext buildUpgradeJobContext(final long activityJobId) {
        return vranUpgradeJobContextBuilder.build(activityJobId);
    }

}
