/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.remote.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.util.StringUtils;
import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.job.exceptions.NoMeFDNSProvidedException;
import com.ericsson.oss.services.shm.job.remote.api.ShmRemoteJobData;
import com.ericsson.oss.services.shm.job.service.SHMJobService;
import com.ericsson.oss.services.shm.job.service.api.JobInfoConverter;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobCapabilityProvider;
import com.ericsson.oss.services.shm.jobservice.common.JobInfo;
import com.ericsson.oss.services.shm.jobservice.common.NeJobProperty;
import com.ericsson.oss.services.shm.jobservice.common.NeTypeJobProperty;
import com.ericsson.oss.services.shm.jobservice.common.PlatformProperty;
import com.ericsson.oss.services.shm.shared.constants.PeriodicSchedulerConstants;
import com.ericsson.oss.services.shm.topologyservice.TopologyEvaluationService;
import com.ericsson.oss.services.topologyCollectionsService.exception.service.TopologyCollectionsServiceException;

/**
 * Common Converter implementation to convert external POJO values to domain specific JobInfo to create job
 * 
 * @author tcsmaup
 * 
 */
@Stateless
public abstract class JobInfoConverterImpl implements JobInfoConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobInfoConverterImpl.class);

    private static final String ME_FDNS_NOT_FOUND = "No meFdns specified";

    @Inject
    FdnServiceBean fdnServiceBean;

    @Inject
    NetworkElementFinderHelper networkElementFinderHelper;

    @Inject
    SHMJobService shmJobService;

    @Inject
    TopologyEvaluationService topologyEvaluationService;

    @Inject
    private JobCapabilityProvider jobCapabilityProvider;

    @Inject
    protected SupportedPlatformAndNeTypeFinder platformAndNeTypeFinder;

    @Inject
    protected NeTypePropertiesUtil neTypePropertiesUtil;

    /**
     * To set common attributes of {@link JobInfo} for all job types when request is received from External Remote service.
     * 
     * @param shmRemoteJobData
     * @param jobInfo
     * @throws TopologyCollectionsServiceException
     * @throws NoMeFDNSProvidedException
     */
    @SuppressWarnings("deprecation")
    public void setCommonAttributes(final ShmRemoteJobData shmRemoteJobData, final JobInfo jobInfo) throws NoMeFDNSProvidedException, TopologyCollectionsServiceException {
        LOGGER.debug("Job data conversion starts...");

        final List<Map<String, String>> jobProperties = new ArrayList<Map<String, String>>();
        final List<PlatformProperty> platformJobProperties = new ArrayList<PlatformProperty>();
        final List<NeTypeJobProperty> neTypeJobProperties = new ArrayList<NeTypeJobProperty>();
        final List<NeJobProperty> neJobProperties = new ArrayList<NeJobProperty>();
        final String loggedInUser = shmRemoteJobData.getLoggedInUser();
        jobInfo.setDescription(shmRemoteJobData.getJobDescription());
        setCollectionId(shmRemoteJobData, jobInfo, loggedInUser);
        setSavedSearchId(shmRemoteJobData, jobInfo, loggedInUser);

        jobInfo.setMainSchedule(getMainSchedule(shmRemoteJobData));
        jobInfo.setName(shmRemoteJobData.getJobName());
        jobInfo.setOwner(loggedInUser);
        jobInfo.setPackageNames(null);

        final String capability = jobCapabilityProvider.getCapability(jobInfo.getJobType());
        extractFdns(shmRemoteJobData, capability);

        if (shmRemoteJobData.getFdns() != null && !shmRemoteJobData.getFdns().isEmpty()) {
            final List<String> fdns = new ArrayList<String>(shmRemoteJobData.getFdns());
            jobInfo.setFdns(fdns);
        }

        checkNEsParams(shmRemoteJobData);

        if ((shmRemoteJobData.getCollection() == null || shmRemoteJobData.getCollection().isEmpty())
                && (shmRemoteJobData.getSavedSearchId() == null || shmRemoteJobData.getSavedSearchId().isEmpty())) {
            jobInfo.setNeNames(getNenames(shmRemoteJobData.getNeNames()));
        }

        jobInfo.setJobCategory(shmRemoteJobData.getJobCategory());
        jobInfo.setCreationTime(new Date());
        jobInfo.setJobProperties(jobProperties);
        jobInfo.setPlatformJobProperties(platformJobProperties);
        jobInfo.setNETypeJobProperties(neTypeJobProperties);
        jobInfo.setNeJobProperties(neJobProperties);

    }

    /**
     * 
     * @param shmRemoteJobData
     */
    public void setNeNamesFromSearchScope(final ShmRemoteJobData shmRemoteJobData) {
        if (shmRemoteJobData.getNetworkElementSearchScopes() != null && !shmRemoteJobData.getNetworkElementSearchScopes().isEmpty()) {
            networkElementFinderHelper.findAndAddNeNamesFromSearchScopes(shmRemoteJobData);
        }
    }

    /**
     * Check if any one of fdns/Nes/savedsearch/collection data has been received with ShmRemoteJobData, otherwise throws exception.
     * 
     * @param shmRemoteJobData
     * @throws NoMeFDNSProvidedException
     */
    private static void checkNEsParams(final ShmRemoteJobData shmRemoteJobData) throws NoMeFDNSProvidedException {
        if ((shmRemoteJobData.getFdns() == null || shmRemoteJobData.getFdns().isEmpty()) && (shmRemoteJobData.getNeNames() == null || shmRemoteJobData.getNeNames().isEmpty())
                && (StringUtils.isEmpty(shmRemoteJobData.getSavedSearchId())) && (StringUtils.isEmpty(shmRemoteJobData.getCollection()))) {
            LOGGER.error(ME_FDNS_NOT_FOUND);
            throw new NoMeFDNSProvidedException(ME_FDNS_NOT_FOUND);
        }
    }

    /**
     * Function to extract names from fdns
     * 
     * @param shmJobData
     * @throws TopologyCollectionsServiceException
     */
    private void extractFdns(final ShmRemoteJobData shmJobData, final String capability) throws TopologyCollectionsServiceException {
        try {
            if (shmJobData.getFdns() != null && !shmJobData.getFdns().isEmpty()) {
                Set<String> neNames = null;
                if (shmJobData.getNeNames() != null && !shmJobData.getNeNames().isEmpty()) {
                    neNames = shmJobData.getNeNames();
                } else {
                    neNames = new HashSet<String>();
                }
                neNames.addAll(getNeNamesFromFdns(shmJobData.getFdns(), capability));
                shmJobData.setNeNames(neNames);
            }

        } catch (TopologyCollectionsServiceException e) {
            if (shmJobData.getNeNames() != null && !shmJobData.getNeNames().isEmpty()) {
                //swallow exception to proceed with job creation along with available NEs 
            } else {
                throw e;
            }
        }
    }

    /**
     * Sets saved search as per Search ID and logged in user.
     * 
     * @param shmRemoteJobData
     * @param jobInfo
     * @param loggedInUser
     * @throws TopologyCollectionsServiceException
     */
    private void setSavedSearchId(final ShmRemoteJobData shmRemoteJobData, final JobInfo jobInfo, final String loggedInUser) throws TopologyCollectionsServiceException {
        final List<String> savedSearchIds = new ArrayList<String>();
        if (!StringUtils.isEmpty(shmRemoteJobData.getSavedSearchId())) {
            final String savedSearchId = topologyEvaluationService.getSavedSearchPoId(shmRemoteJobData.getSavedSearchId(), loggedInUser);
            if (savedSearchId != null) {
                savedSearchIds.add(savedSearchId);
            }
        }
        jobInfo.setSavedSearchIds(savedSearchIds);
    }

    /**
     * Sets collection name as per collection ID and logged in user.
     * 
     * @param shmRemoteJobData
     * @param jobInfo
     * @param loggedInUser
     * @throws TopologyCollectionsServiceException
     */
    private void setCollectionId(final ShmRemoteJobData shmRemoteJobData, final JobInfo jobInfo, final String loggedInUser) throws TopologyCollectionsServiceException {
        final List<String> collectionNames = new ArrayList<String>();
        if (!StringUtils.isEmpty(shmRemoteJobData.getCollection())) {
            final String collectionId = topologyEvaluationService.getCollectionPoId(shmRemoteJobData.getCollection(), loggedInUser);
            if (collectionId != null) {
                collectionNames.add(collectionId);
            }
        }
        jobInfo.setcollectionNames(collectionNames);
    }

    /**
     * Gets NE names from fdns.
     * 
     * @param fdns
     * @throws TopologyCollectionsServiceException
     */
    @SuppressWarnings("deprecation")
    private List<String> getNeNamesFromFdns(final Set<String> fdns, final String capability) throws TopologyCollectionsServiceException {
        final List<String> neNames = new ArrayList<>();
        final List<String> fdnsList = new ArrayList<String>(fdns);
        final List<NetworkElement> networkElements = fdnServiceBean.getNetworkElements(fdnsList, capability);
        if (networkElements != null && !networkElements.isEmpty()) {
            for (NetworkElement networkElement : networkElements) {
                neNames.add(networkElement.getName());
            }
        } else {
            throw new TopologyCollectionsServiceException("Provided Fdns does not exist");
        }
        return neNames;
    }

    private static Map<String, Object> getMainSchedule(final ShmRemoteJobData shmRemoteJobData) {
        LOGGER.debug("Preparing Scheduling attributes map from SHMJobData...");
        final Map<String, Object> mainSchedule = new HashMap<String, Object>();
        mainSchedule.put(ShmConstants.EXECUTION_MODE, shmRemoteJobData.getExecMode());
        if (shmRemoteJobData.getScheduleData() != null) {
            final List<Map<String, Object>> scheduleAttributes = new ArrayList<Map<String, Object>>();
            if (shmRemoteJobData.getScheduleData().getStartTime() != null) {
                scheduleAttributes.add(createPropertyMap(PeriodicSchedulerConstants.START_DATE, shmRemoteJobData.getScheduleData().getStartTime()));
            }
            if (shmRemoteJobData.getScheduleData().getEndTime() != null) {
                scheduleAttributes.add(createPropertyMap(PeriodicSchedulerConstants.END_DATE, shmRemoteJobData.getScheduleData().getEndTime()));
            }
            if (shmRemoteJobData.getScheduleData().getCronExpression() != null) {
                scheduleAttributes.add(createPropertyMap(PeriodicSchedulerConstants.CRON_EXP, shmRemoteJobData.getScheduleData().getCronExpression()));
            }
            mainSchedule.put(ShmConstants.SCHEDULINGPROPERTIES, scheduleAttributes);
        }
        LOGGER.debug("Values of Scheduling attributes map from SHMJobData :{}", mainSchedule);
        return mainSchedule;
    }

    private static Map<String, Object> createPropertyMap(final String key, final String value) {
        final HashMap<String, Object> property = new HashMap<String, Object>();
        property.put(ShmConstants.NAME, key);
        property.put(ShmConstants.VALUE, value);
        return property;
    }

    private static List<Map<String, Object>> getNenames(final Set<String> neNames) {
        final List<Map<String, Object>> neNamesList = new ArrayList<Map<String, Object>>();
        for (String neName : neNames) {
            final HashMap<String, Object> neNameMap = new HashMap<String, Object>();
            neNameMap.put(ShmConstants.NAME, neName);
            neNamesList.add(neNameMap);
        }
        return neNamesList;
    }

}
