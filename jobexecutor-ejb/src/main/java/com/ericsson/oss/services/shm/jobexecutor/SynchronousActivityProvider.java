/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.jobexecutor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.filestore.swpackage.constants.SoftwarePackageConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.Activity;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.modelentities.NeTypeActivityJobProperties;

public class SynchronousActivityProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SynchronousActivityProvider.class);

    /**
     * Only AXE node related activities will be checked for synchronous attribute and update in map
     * 
     * @param jobConfiguration
     * @param JobType
     */

    public Map<String, Map<Object, Object>> prepareNextSynchronousActivityMap(final JobConfiguration jobConfiguration, final Map<String, Object> neTypeGroupedActivities, final JobTypeEnum jobType) {

        LOGGER.info("prepareNextSynchronousActivityMap neTypeGroupedActivities: {}", neTypeGroupedActivities);
        if (neTypeGroupedActivities != null && !neTypeGroupedActivities.isEmpty() && jobType.equals(JobTypeEnum.UPGRADE)) {
            return prepareNeTypeSynchronousActivityMap(jobConfiguration, neTypeGroupedActivities);
        }
        return null;
    }

    /**
     * @param jobConfiguration
     * @param neTypeGroupedActivities
     * @param neTypeSynchronousActivityMap
     */
    private Map<String, Map<Object, Object>> prepareNeTypeSynchronousActivityMap(final JobConfiguration jobConfiguration, final Map<String, Object> neTypeGroupedActivities) {
        final Map<String, Map<Object, Object>> neTypeSynchronousActivityMap = new HashMap<>();
        for (final Entry<String, Object> neTypeGroupedActivity : neTypeGroupedActivities.entrySet()) {
            final String neType = neTypeGroupedActivity.getKey();
            final List<Activity> activities = (List<Activity>) neTypeGroupedActivity.getValue();
            LOGGER.info("neType :{},activities size:{}", neType, activities.size());
            final Map<Object, Object> activityOrderToNextActivitySyncStatus = getActivityOrderToNextActivitySyncStatus(jobConfiguration, neType, activities);
            LOGGER.info("prepareNextSynchronousActivityMap activityOrderToNextActivitySyncStatus: {}", activityOrderToNextActivitySyncStatus);
            neTypeSynchronousActivityMap.put(neType, activityOrderToNextActivitySyncStatus);
        }
        LOGGER.info("neTypeSynchronousActivityMap : {}", neTypeSynchronousActivityMap);
        return neTypeSynchronousActivityMap;
    }

    /**
     * @param jobConfiguration
     * @param activityOrderAndSyncStatus
     * @param neType
     * @param activities
     */
    private Map<Object, Object> getActivityOrderToNextActivitySyncStatus(final JobConfiguration jobConfiguration, final String neType, final List<Activity> activities) {
        final Map<Object, Object> activityOrderToNextActivitySyncStatus = new HashMap<>();
        for (final Activity activity : activities) {
            if (activity.getPlatform().toString().equals(PlatformTypeEnum.AXE.getName())) {
                final String activityName = activity.getName();
                final boolean isActivitySynchronous = isActivitySynchronous(jobConfiguration, neType, activityName);
                LOGGER.info("Actvitiy Name is {} synchronous?: {}", activityName, isActivitySynchronous);
                if (isActivitySynchronous) {
                    final int orderId = activity.getOrder();
                    if (orderId != 1) {
                        activityOrderToNextActivitySyncStatus.put(orderId - 1, isActivitySynchronous);
                    }

                }
            }

        }
        return activityOrderToNextActivitySyncStatus;
    }

    public boolean isActivitySynchronous(final JobConfiguration jobConfiguration, final String neType, final String activityName) {
        boolean isActivitySynchronous = false;
        final List<NeTypeActivityJobProperties> neTypeActivityJobProperties = jobConfiguration.getNeTypeActivityJobProperties();
        if (neTypeActivityJobProperties != null && !neTypeActivityJobProperties.isEmpty()) {
            LOGGER.info("isActivitySynchronous neTypeActivityJobProperty size {}", neTypeActivityJobProperties.size());
            isActivitySynchronous = verifyActivitySynchStateInNeTypeActivityProperties(neType, activityName, neTypeActivityJobProperties);
        }
        return isActivitySynchronous;
    }

    /**
     * @param neType
     * @param activityName
     * @param isActivitySynchronous
     * @param neTypeActivityJobProperties
     * @return
     */
    private boolean verifyActivitySynchStateInNeTypeActivityProperties(final String neType, final String activityName, final List<NeTypeActivityJobProperties> neTypeActivityJobProperties) {
        final boolean isActivitySynchronous = false;
        for (final NeTypeActivityJobProperties neTypeActivityJobProperty : neTypeActivityJobProperties) {
            final String nodeType = neTypeActivityJobProperty.getNeType();
            if (nodeType.equals(neType)) {
                final List<Map<String, Object>> activityJobProperties = neTypeActivityJobProperty.getActivityJobProperties();
                if (activityJobProperties != null && !activityJobProperties.isEmpty()) {
                    return verifyActivitySynchStateInActivityJobProperties(activityName, activityJobProperties);
                }
            }
        }
        return isActivitySynchronous;
    }

    /**
     * @param activityName
     * @param activityJobProperties
     */
    @SuppressWarnings("unchecked")
    private boolean verifyActivitySynchStateInActivityJobProperties(final String activityName, final List<Map<String, Object>> activityJobProperties) {
        final boolean isActivitySynchronous = false;
        for (final Map<String, Object> activityJobProperty : activityJobProperties) {
            if (activityJobProperty.get(ShmConstants.ACTIVITYNAME).toString().equals(activityName)) {
                LOGGER.info("activityName {}", activityJobProperty.get(ShmConstants.ACTIVITYNAME));
                final List<Map<String, Object>> jobProperties = (List<Map<String, Object>>) activityJobProperty.get(ShmConstants.JOBPROPERTIES);
                LOGGER.info("jobProperties size {}", jobProperties.size());
                return verifyActivitySynchStateInJobProperties(jobProperties);
            }
        }
        return isActivitySynchronous;
    }

    /**
     * @param isActivitySynchronous
     * @param jobProperties
     * @return
     */
    private boolean verifyActivitySynchStateInJobProperties(final List<Map<String, Object>> jobProperties) {
        boolean isActivitySynchronous = false;
        if (jobProperties != null && !jobProperties.isEmpty()) {
            for (final Map<String, Object> jobProperty : jobProperties) {
                LOGGER.info("isActivitySynchronous  sync data contains {}", jobProperty);
                if (jobProperty.containsValue(SoftwarePackageConstants.AXESOFTWAREPACKAGE_ACTIVITY_SYNCHRONUS)) {
                    LOGGER.info("isActivitySynchronous Synchornus{}", jobProperty.get(ShmConstants.VALUE));
                    isActivitySynchronous = Boolean.parseBoolean(jobProperty.get(ShmConstants.VALUE).toString());
                }
            }
        }
        return isActivitySynchronous;
    }

}
