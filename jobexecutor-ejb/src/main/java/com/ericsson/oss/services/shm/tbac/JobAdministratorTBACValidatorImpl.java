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
package com.ericsson.oss.services.shm.tbac;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.tbac.api.SHMTBACHandler;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.jobexecutor.JobExecutorServiceHelper;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobMapper;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobTemplate;
import com.ericsson.oss.services.shm.jobs.common.modelentities.NEInfo;

/**
 * @author tcssbop
 * 
 */
public class JobAdministratorTBACValidatorImpl implements JobAdministratorTBACValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobAdministratorTBACValidatorImpl.class);

    @Inject
    private JobMapper jobMapper;

    @Inject
    private JobExecutorServiceHelper jobExecutorServiceHelper;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private SHMTBACHandler shmTbacHandler;

    @Override
    public boolean validateTBACForMainJob(final long mainJobId, final long mainJobTemplateId, final String cancelledBy) {
        LOGGER.debug("Validating TBAC for Main Job : {} for user {}", mainJobId, cancelledBy);
        final Map<String, Object> jobTemplateAttributes = getTemplateJobDetails(mainJobTemplateId);
        final JobConfiguration jobConfigurationsDetails = getJobConfigurationDetails(jobTemplateAttributes, mainJobTemplateId);
        final NEInfo neInfo = jobConfigurationsDetails.getSelectedNEs();
        final List<String> nodeNames = getNodeNames(neInfo, mainJobId, (String) jobTemplateAttributes.get(ShmConstants.OWNER));
        return shmTbacHandler.isAuthorized(cancelledBy, nodeNames.toArray(new String[0]));
    }

    private JobConfiguration getJobConfigurationDetails(final Map<String, Object> jobTemplateAttributes, final long mainJobTemplateId) {
        final JobTemplate jobTemplate = jobMapper.getJobTemplateDetails(jobTemplateAttributes, mainJobTemplateId);
        return jobTemplate.getJobConfigurationDetails();
    }

    private List<String> getNodeNames(final NEInfo neInfo, final long mainJobId, final String jobOwner) {
        final List<Map<String, Object>> topologyJobLogList = new ArrayList<>();
        boolean isDataRetrievedForAllCollectionIds = false;
        boolean isDataRetrievedForAllSavedSearches = false;
        List<String> nodeNames = new ArrayList<>();
        if (neInfo != null) {
            nodeNames = neInfo.getNeNames();
            final List<String> collectionNames = neInfo.getCollectionNames();
            final List<String> savedSearchIds = neInfo.getSavedSearchIds();
            if (!nodeNames.isEmpty() || !collectionNames.isEmpty() || !savedSearchIds.isEmpty()) {
                //Evaluate collections if exists
                isDataRetrievedForAllCollectionIds = jobExecutorServiceHelper.populateNeNamesFromCollections(mainJobId, nodeNames, topologyJobLogList, collectionNames, jobOwner);
                //Evaluate savedSearches if exists
                isDataRetrievedForAllSavedSearches = jobExecutorServiceHelper.populateNeNamesFromSavedSearches(mainJobId, nodeNames, topologyJobLogList, savedSearchIds, jobOwner);

                if (!isDataRetrievedForAllCollectionIds || !isDataRetrievedForAllSavedSearches) {
                    LOGGER.error("Unable to fetch Network Elements for Collection/SavedSearch");
                }
            }
        }
        return nodeNames;

    }

    @Override
    public boolean validateTBACForNEJob(final String nodeName, final String cancelledBy) {
        LOGGER.debug("Validating TBAC for NE Job for node : {} for user {}", nodeName, cancelledBy);
        return shmTbacHandler.isAuthorized(cancelledBy, nodeName);
    }

    @Override
    public boolean validateTBACAtJobLevel(final List<String> nodeNames, final String cancelledBy) {
        LOGGER.debug("Validating TBAC for NE Job for {} nodes for user {}", nodeNames.size(), cancelledBy);
        return shmTbacHandler.isAuthorized(cancelledBy, nodeNames.toArray(new String[0]));
    }

    /**
     * Retrieves the Job Template from the DB and retries if DB is down for the given ID
     * 
     * @param jobTemplateId
     * @return
     */
    public Map<String, Object> getTemplateJobDetails(final long jobId) {
        return jobUpdateService.retrieveJobWithRetry(jobId);
    }

}
