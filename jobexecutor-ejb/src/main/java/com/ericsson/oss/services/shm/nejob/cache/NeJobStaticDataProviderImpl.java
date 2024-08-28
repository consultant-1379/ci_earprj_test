/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.shm.nejob.cache;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;

/**
 * Provides a method to get NE Job Static Data from elementary services.
 * 
 * @author tcsgusw
 * 
 */
public class NeJobStaticDataProviderImpl implements NeJobStaticDataProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(NeJobStaticDataProviderImpl.class);

    @Inject
    private NeJobStaticDataCache jobStaticDataCache;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationService;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    @Inject
    private PlatformTypeProviderImpl platformTypeProvider;

    @Override
    public NEJobStaticData getNeJobStaticData(final long activityJobId, final String platformCapbility) throws JobDataNotFoundException {

        NEJobStaticData neJobStaticData = jobStaticDataCache.get(activityJobId);

        if (neJobStaticData == null) {
            //Fetch from DPS, if data not exists in cache.
            neJobStaticData = getNEJobInfoFromDPS(activityJobId, platformCapbility, 0);
            if (neJobStaticData != null) {
                jobStaticDataCache.put(activityJobId, neJobStaticData);
            } else {
                LOGGER.error("Failed to get NE Job static data either from cache or from DPS for activityId: {} ", activityJobId);
                throw new JobDataNotFoundException("Database service is not accessible");
            }
        } else {
            //There are few scenarios , where NeJoBStaticData objects exists in the cache but activityStart is null.
            //This can occur when NeJobStaticData added into cache in one VM and activity started in another VM, but now trying get that object from first VM.
            neJobStaticData = validateAndUpdateStartTimeInCache(activityJobId, neJobStaticData);
        }
        return neJobStaticData;
    }

    private NEJobStaticData validateAndUpdateStartTimeInCache(final long activityJobId, NEJobStaticData neJobStaticData) {
        if (neJobStaticData.getActivityStartTime() == 0) {
            long activityStartTime = 0l;
            try {
                activityStartTime = getActivityStartTimeFromDps(activityJobId);
            } catch (final Exception ex) {
                LOGGER.error("ActivityJob PO not exists in DPS for activityJobId: {}", activityJobId);
            }
            neJobStaticData = new NEJobStaticData(neJobStaticData.getNeJobId(), neJobStaticData.getMainJobId(), neJobStaticData.getNodeName(), neJobStaticData.getNeJobBusinessKey(),
                    neJobStaticData.getPlatformType(), activityStartTime, neJobStaticData.getParentNodeName());
            jobStaticDataCache.put(activityJobId, neJobStaticData);
        }
        return neJobStaticData;
    }

    @Override
    public void updateNeJobStaticDataCache(final long activityJobId, final String platformCapbility, final long activityStartTime) throws JobDataNotFoundException {

        NEJobStaticData neJobStaticData = jobStaticDataCache.get(activityJobId);

        if (neJobStaticData == null) {
            //Fetch from DPS, if data not exists in cache.
            neJobStaticData = getNEJobInfoFromDPS(activityJobId, platformCapbility, activityStartTime);
            //throwing exception If failed to get data from DPS also
            if (neJobStaticData == null) {
                LOGGER.error("Failed to get NE Job static data either from cache or from DPS for activityId:{} ", activityJobId);
                throw new JobDataNotFoundException("Database service is not accessible");
            }
        } else {
            // Updating NeJobStaticData in cache because existing entry doesn't contains activityStartTime.
            neJobStaticData = new NEJobStaticData(neJobStaticData.getNeJobId(), neJobStaticData.getMainJobId(), neJobStaticData.getNodeName(), neJobStaticData.getNeJobBusinessKey(),
                    neJobStaticData.getPlatformType(), activityStartTime, neJobStaticData.getParentNodeName());

        }
        jobStaticDataCache.put(activityJobId, neJobStaticData);
    }

    private long getActivityStartTimeFromDps(final long activityJobId) throws MoNotFoundException {
        long activityStartTime = 0l;
        try {
            final Map<String, Object> activityJobAttributes = jobConfigurationService.getActivityJobAttributes(activityJobId);
            if (validateJobPO(activityJobAttributes)) {
                throw new MoNotFoundException("ActivityJob PO not exists in DPS");
            }
            activityStartTime = activityJobAttributes.get(ShmConstants.ACTIVITY_START_DATE) != null ? ((Date) activityJobAttributes.get(ShmConstants.ACTIVITY_START_DATE)).getTime() : 0L;
        } catch (final MoNotFoundException ex) {
            LOGGER.error("MoNotFoundException occurred while reading activityStartTime for activity:{}, Exception is:  ", activityJobId, ex);
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while reading activityStartTime for activity:{}, Exception is:  ", activityJobId, ex);
        }
        return activityStartTime;
    }

    @SuppressWarnings("unchecked")
    private NEJobStaticData getNEJobInfoFromDPS(final long activityJobId, final String platformCapbility, long activityStartTime) {
        LOGGER.debug("Entered into getNEJobInfoFromDPS method to get data from DPS: {}", activityJobId);
        NEJobStaticData neJobStaticData = null;
        try {
            final Map<String, Object> activityJobAttributes = jobConfigurationService.getActivityJobAttributes(activityJobId);
            if (validateJobPO(activityJobAttributes)) {
                return neJobStaticData;
            }
            final long neJobId = (long) activityJobAttributes.get(ShmConstants.NE_JOB_ID);
            if (activityStartTime == 0) {
                activityStartTime = activityJobAttributes.get(ShmConstants.ACTIVITY_START_DATE) != null ? ((Date) activityJobAttributes.get(ShmConstants.ACTIVITY_START_DATE)).getTime() : 0L;
            }
            if (neJobId != 0) {
                neJobStaticData = buildNeJobStaticData(neJobId, activityJobId, platformCapbility, activityStartTime);
            }
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while getting Job static data for the activityJobId: {}. Exception is:", activityJobId, ex);
        }
        return neJobStaticData;
    }

    private NEJobStaticData buildNeJobStaticData(final long neJobId, final long activityJobId, final String platformCapbility, final long activityStartTime) {
        NEJobStaticData neJobStaticData = null;
        String nodeName = null;
        try {
            final Map<String, Object> neJobAttributes = jobConfigurationService.getNeJobAttributes(neJobId);
            if (!validateJobPO(neJobAttributes)) {
                String neType = null;
                nodeName = (String) neJobAttributes.get(ShmConstants.NE_NAME);
                @SuppressWarnings("unchecked")
                final List<Map<String, Object>> jobParameters = (List<Map<String, Object>>) neJobAttributes.get(ShmConstants.JOBPROPERTIES);
                final String parentNeName = getParentNeName(jobParameters);
                final String nodeNameToGetNeType = parentNeName != null ? parentNeName : nodeName;
                neType = networkElementRetrivalBean.getNeType(nodeNameToGetNeType);
                final PlatformTypeEnum platformTypeEnum = platformTypeProvider.getPlatformTypeBasedOnCapability(neType, platformCapbility);
                final long mainJobId = (Long) neJobAttributes.get(ShmConstants.MAIN_JOB_ID);
                final String neBusinessKey = String.valueOf(neJobAttributes.get(ShmConstants.BUSINESS_KEY));
                if (validateNEJobAttrs(neJobId, mainJobId, nodeName, neBusinessKey, platformTypeEnum)) {
                    neJobStaticData = new NEJobStaticData(neJobId, mainJobId, nodeName, neBusinessKey, platformTypeEnum.toString(), activityStartTime, parentNeName);
                } else {
                    LOGGER.info("Failed to get Job static data for the activityJobId: {}. neJobId:{},  mainJobId:{} , nodeName:{},neBusinessKey:{} platformTypeEnum: {} and activityStartTime: {} ",
                            activityJobId, neJobId, mainJobId, nodeName, neBusinessKey, platformTypeEnum, activityStartTime);
                }
            } else {
                LOGGER.info("Failed to get NEJob PO for NE JobId{}", neJobId);
            }
        } catch (final MoNotFoundException ex) {
            LOGGER.error("Failed to get neType from networkElement cache for the node: {} with activityJobId: {}, Exception is :{}", nodeName, activityJobId, ex);
        }
        return neJobStaticData;
    }

    /**
     * @param parentNodeName
     * @param jobParameters
     * @return
     */
    private String getParentNeName(final List<Map<String, Object>> jobParameters) {
        String parentNodeName = null;
        if (jobParameters != null && !jobParameters.isEmpty()) {
            for (final Map<String, Object> jobParameter : jobParameters) {
                if (jobParameter != null && !jobParameter.isEmpty()) {
                    LOGGER.info("IS_COMPONENT_JOB exists in properties {}", jobParameter.containsValue(ShmConstants.IS_COMPONENT_JOB));
                    if (jobParameter.containsValue(ShmConstants.PARENT_NAME)) {
                        parentNodeName = String.valueOf(jobParameter.get(ShmConstants.VALUE));
                    }
                }
            }
        }
        return parentNodeName;
    }

    private boolean validateJobPO(final Map<String, Object> poIds) {
        return poIds == null || poIds.isEmpty();
    }

    private boolean validateNEJobAttrs(final long neJobId, final long mainJobId, final String nodeName, final String neJobBusinessKey, final PlatformTypeEnum platformTypeEnum) {
        return !(neJobId == 0 || mainJobId == 0 || nodeName == null || neJobBusinessKey == null || platformTypeEnum == null);
    }

    @Override
    public void clear(final long activityJobId) {
        jobStaticDataCache.clear(activityJobId);
    }

    @Override
    public void clearAll() {
        jobStaticDataCache.clearAll();
    }

    @Override
    public void put(final long activityJobId, final NEJobStaticData neJobStaticData) {
        jobStaticDataCache.put(activityJobId, neJobStaticData);
    }

    @Override
    public long getActivityStartTime(final long activityJobId) throws MoNotFoundException {
        NEJobStaticData neJobStaticData = jobStaticDataCache.get(activityJobId);
        if (neJobStaticData != null) {
            neJobStaticData = validateAndUpdateStartTimeInCache(activityJobId, neJobStaticData);
            return neJobStaticData.getActivityStartTime();
        } else {
            //Fetch from DPS, if data not exists in cache.
            return getActivityStartTimeFromDps(activityJobId);
        }
    }
}
