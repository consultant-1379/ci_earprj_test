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
package com.ericsson.oss.services.shm.axe.synchronous;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.shm.activity.axe.cache.AxeSynchronousActivityData;
import com.ericsson.oss.services.shm.activity.axe.cache.AxeUpgradeSynchronousActivityProvider;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.jobexecutor.JobExecutorServiceHelper;
import com.ericsson.oss.services.shm.jobexecutor.SynchronousActivityProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobMapper;
import com.ericsson.oss.services.shm.jobs.common.modelentities.Activity;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.modelentities.NeTypeActivityJobProperties;

/**
 * Class provides the implementation of cache operations
 * 
 * @author ztamsra
 *
 */
@Stateless
public class AxeSynchronousActivityDataProviderImpl implements AxeUpgradeSynchronousActivityProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AxeSynchronousActivityDataProviderImpl.class);

    @Inject
    private AXEUpgradeSynchronousActivityDataCache axeAcivityStaticDataCache;

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    @Inject
    private JobExecutorServiceHelper executorServiceHelper;

    @Inject
    private SynchronousActivityProvider synchronousActivityProvider;

    @Inject
    private JobMapper jobMapper;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private ActivityUtils activityUtils;

    @Override
    public Map<String, List<AxeSynchronousActivityData>> getSynchronousAxeActivities(final long mainJobId, final String neType) {
        Map<String, List<AxeSynchronousActivityData>> response = new HashMap<>();
        final String cacheKey = generateKey(mainJobId, neType);
        if (axeAcivityStaticDataCache.get(cacheKey) == null) {
            LOGGER.debug("Cache is Empty,Git it from dps");
            response = getSynchronousAxeActivitiesFromDPS(mainJobId, neType);
            axeAcivityStaticDataCache.putAll(response);
            return response;
        }
        response.put(cacheKey, axeAcivityStaticDataCache.get(cacheKey));
        return response;
    }

    @Override
    public boolean getSyncCompletedStatusFromCache(final long mainJobId, final String neType) {
        return axeAcivityStaticDataCache.getSyncCompletionStatus(generateKey(mainJobId, neType) + "_SyncCompleted");
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<AxeSynchronousActivityData>> getSynchronousAxeActivitiesFromDPS(final long mainJobId, final String neType) {
        final Map<String, List<AxeSynchronousActivityData>> response = new HashMap<>();
        LOGGER.debug("Fetch sync activites for mainJob :{}", mainJobId);
        final PersistenceObject mainJobPO = dataPersistenceService.getLiveBucket().findPoById(mainJobId);
        final JobConfiguration jobConfiguration = jobMapper.getJobConfigurationDetails((Map<String, Object>) mainJobPO.getAttribute(ShmConstants.JOBCONFIGURATIONDETAILS));
        final List<NeTypeActivityJobProperties> neTypeActivityJobProperties = jobConfiguration.getNeTypeActivityJobProperties();
        for (final NeTypeActivityJobProperties neTypeActivityJobProperty : neTypeActivityJobProperties) {
            if (neTypeActivityJobProperty.getNeType().equals(neType)) {
                final List<AxeSynchronousActivityData> axeSynchronousActivitiesList = prepareActivitiesList(jobConfiguration, neTypeActivityJobProperty);
                if (!axeSynchronousActivitiesList.isEmpty()) {
                    response.put(generateKey(mainJobId, neTypeActivityJobProperty.getNeType()), axeSynchronousActivitiesList);
                }
                break;
            }
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    private List<AxeSynchronousActivityData> prepareActivitiesList(final JobConfiguration jobConfiguration, final NeTypeActivityJobProperties neTypeActivityJobProperty) {
        final List<AxeSynchronousActivityData> axeActivityStaticDataList = new ArrayList<>();
        final Map<String, Object> activitesGroupedByNeType = executorServiceHelper.groupActivitiesByNeType(jobConfiguration.getActivities());
        if (activitesGroupedByNeType.containsKey(neTypeActivityJobProperty.getNeType())) {
            for (final Activity activity : (List<Activity>) activitesGroupedByNeType.get(neTypeActivityJobProperty.getNeType())) {
                final boolean isSyncActivity = synchronousActivityProvider.isActivitySynchronous(jobConfiguration, neTypeActivityJobProperty.getNeType(), activity.getName());
                if (isSyncActivity) {
                    final AxeSynchronousActivityData axeActivityStaticData = new AxeSynchronousActivityData(activity.getName(), activity.getOrder());
                    axeActivityStaticDataList.add(axeActivityStaticData);
                }
            }
        }
        return axeActivityStaticDataList;
    }

    @Override
    public void put(final long mainJobId, final String neType, final List<AxeSynchronousActivityData> activityStaticDataList) {
        final String key = generateKey(mainJobId, neType);
        if (!axeAcivityStaticDataCache.getCacheKeys().contains(key)) {
            axeAcivityStaticDataCache.put(generateKey(mainJobId, neType), activityStaticDataList);
        }
    }

    @Override
    public void clear(final long mainJobId, final String neType) {
        if (axeAcivityStaticDataCache.get(generateKey(mainJobId, neType)) != null && !axeAcivityStaticDataCache.get(generateKey(mainJobId, neType)).isEmpty()) {
            axeAcivityStaticDataCache.clear(generateKey(mainJobId, neType));
        }
    }

    @Override
    public List<AxeSynchronousActivityData> get(final long mainJobId, final String neType) {
        if (axeAcivityStaticDataCache.get(generateKey(mainJobId, neType)) != null && !axeAcivityStaticDataCache.get(generateKey(mainJobId, neType)).isEmpty()) {
            return axeAcivityStaticDataCache.get(generateKey(mainJobId, neType));
        }
        return Collections.emptyList();
    }

    @Override
    public void clearAll() {
        LOGGER.info("Clear the synchronous cache");
        axeAcivityStaticDataCache.clearAll();
    }

    private static String generateKey(final Object mainJobId, final String neType) {
        return mainJobId + "_" + neType;
    }

    @Override
    public void updateAxeSynchronousActivites(final String key, final List<AxeSynchronousActivityData> updatedAxeStaticList) {
        if (axeAcivityStaticDataCache.getCacheKeys().contains(key) && !updatedAxeStaticList.isEmpty()) {
            axeAcivityStaticDataCache.put(key, updatedAxeStaticList);
        } else if (axeAcivityStaticDataCache.getCacheKeys().contains(key) && updatedAxeStaticList.isEmpty()) {
            axeAcivityStaticDataCache.clear(key);
        }
    }

    private void updateSyncCompletionStatusInDps(final long mainJobId, final String key) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        activityUtils.prepareJobPropertyList(jobPropertyList, key, "true");
        jobUpdateService.readAndUpdateRunningJobAttributes(mainJobId, jobPropertyList, null, null);
    }

    @Override
    public void updateSyncCompleted(final long mainJobId, final String neType, final int orderId) {
        updateSyncCompletionStatusInDps(mainJobId, neType + "_" + orderId);
    }

    @Override
    public void updateSyncCompleted(final long mainJobId, final String neType) {
        final String cacheKey = generateKey(mainJobId, neType);
        axeAcivityStaticDataCache.updateSyncCompletionStatus(cacheKey + "_SyncCompleted", true);
        updateSyncCompletionStatusInDps(mainJobId, neType + "_SyncCompleted");
    }

    @Override
    public void updateSyncCompletedStatusInCache(final String cacheKey) {
        axeAcivityStaticDataCache.updateSyncCompletionStatus(cacheKey + "_SyncCompleted", true);
    }

}
