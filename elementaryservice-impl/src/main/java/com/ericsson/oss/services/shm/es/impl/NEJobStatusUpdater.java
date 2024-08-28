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
package com.ericsson.oss.services.shm.es.impl;

import java.util.*;

import javax.ejb.*;
import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.*;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.JobConsolidationService;
import com.ericsson.oss.services.shm.inventory.remote.axe.api.AxeInvSupervisionRemoteHandler;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.*;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.shared.util.JobPropertyUtil;

/**
 * 
 * Callers of this class needs to take care of Tx handling.
 * 
 * @author UPDATED by xrajeke
 * 
 */
@Stateless
public class NEJobStatusUpdater {

    private final static Logger LOGGER = LoggerFactory.getLogger(NEJobStatusUpdater.class);
    private static final EnumMap<JobType, String> axeInventorySyncMap = new EnumMap<>(JobType.class);
    private static final EnumMap<JobType, String> capabilityMap = new EnumMap<>(JobType.class);
    static {
        axeInventorySyncMap.put(JobType.UPGRADE, ShmConstants.SOFTWARE);
        axeInventorySyncMap.put(JobType.BACKUP, JobType.BACKUP.name());
        axeInventorySyncMap.put(JobType.LICENSE, JobType.LICENSE.name());
        axeInventorySyncMap.put(JobType.DELETEBACKUP, JobType.BACKUP.name());
        capabilityMap.put(JobType.UPGRADE, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
        capabilityMap.put(JobType.BACKUP, SHMCapabilities.BACKUP_JOB_CAPABILITY);
        capabilityMap.put(JobType.LICENSE, SHMCapabilities.LICENSE_JOB_CAPABILITY);
        capabilityMap.put(JobType.DELETEBACKUP, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY);
    }

    @Inject
    DpsWriter dpsWriter;

    @Inject
    private JobConfigurationService jobConfigurationService;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    @EServiceRef
    private AxeInvSupervisionRemoteHandler axeInvSupervisionRemoteHandler;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private FaBuildingBlockResponseProcessor faBuildingBlockResponseProcessor;

    /**
     * Updates the NE job with required attributes to fulfill the completion criteria (like ENDTIME,STATE,RESULT)
     * 
     * @param neJobId
     * @return {@code True} - if all the associated NEJobs are completed (including the current one), otherwise {@code False}.
     */
    public boolean updateNEJobEndAttributes(final long neJobId) {
        LOGGER.debug("Inside NEJobStatusUpdater.updateNEJobEndAttributes() with jobId : {}", neJobId);
        boolean isAllNeJobsDone = false;
        final Map<String, Object> neJobAttr = jobConfigurationService.retrieveJob(neJobId);
        if (neJobAttr != null && neJobAttr.get(ShmConstants.ENDTIME) == null) {
            String jobResult = JobResult.SUCCESS.toString();
            if (isNeJobSkipped(neJobAttr)) {
                jobResult = JobResult.SKIPPED.getJobResult();
            } else {
                jobResult = jobConfigurationService.retrieveActivityJobResult(neJobId);
            }
            LOGGER.debug("updateNEJobEndAttributes jobResult: {}", jobResult);
            final Map<String, Object> neJobEndAttributes = new HashMap<String, Object>();
            neJobEndAttributes.put(ShmConstants.RESULT, jobResult);
            neJobEndAttributes.put(ShmConstants.STATE, JobState.COMPLETED.getJobStateName());
            neJobEndAttributes.put(ShmConstants.ENDTIME, new Date());

            final long mainJobId = (long) neJobAttr.get(ShmConstants.MAIN_JOB_ID);
            final String nodeName = (String) neJobAttr.get(ShmConstants.NE_NAME);
            JobType jobType = null;
            try {
                jobType = jobStaticDataProvider.getJobStaticData(mainJobId).getJobType();
            } catch (final JobDataNotFoundException | RuntimeException exception) {
                LOGGER.error("Unable to get JobType Or Inventory Synch failed for {} node ,Exception {}", nodeName, exception);
            }

            if (jobType != null && JobType.NODE_HEALTH_CHECK.name() == jobType.name()) {
                consolidateNHCNeProperties(neJobId, neJobAttr, neJobEndAttributes);
            }
            dpsWriter.update(neJobId, neJobEndAttributes);
            LOGGER.debug("Updated NEJobEnd of : {} with the attributes: {}", neJobId, neJobEndAttributes);

            final Map<String, Object> mainJobAttributes = jobConfigurationService.retrieveJob(mainJobId);
            sendResponseToFa(neJobId, neJobAttr, neJobEndAttributes, mainJobId, nodeName, jobType, mainJobAttributes);
            synchAXEInventoryIfAXENode(mainJobId, nodeName, jobResult, jobType);
            recordNeJobEndDetails(neJobAttr, new Date());
            isAllNeJobsDone = checkAllNEJobsDone(mainJobId, mainJobAttributes);
        } else {
            LOGGER.debug("NeJob {} has already been executed.", neJobId);
        }
        return isAllNeJobsDone;
    }

    /**
     * @param neJobId
     * @param neJobAttr
     * @param neJobEndAttributes
     * @param mainJobId
     * @param nodeName
     * @param jobType
     * @param mainJobAttributes
     */
    @SuppressWarnings("unchecked")
    private void sendResponseToFa(final long neJobId, final Map<String, Object> neJobAttr, final Map<String, Object> neJobEndAttributes, final long mainJobId, final String nodeName, JobType jobType,
            final Map<String, Object> mainJobAttributes) {
        final String jobCategory = JobPropertyUtil.getProperty((List<Map<String, String>>) mainJobAttributes.get(ShmConstants.JOBPROPERTIES), ShmConstants.JOB_CATEGORY);
        if (JobType.NODE_HEALTH_CHECK == jobType && JobCategory.NHC_FA == JobCategory.getJobCategory(jobCategory)) {
            neJobEndAttributes.put(ShmConstants.NE_NAME, nodeName);
            if (neJobAttr.get(ShmJobConstants.LAST_LOG_MESSAGE) != null) {
                final String lastLogMessage = (String) neJobAttr.get(ShmJobConstants.LAST_LOG_MESSAGE);
                neJobEndAttributes.put(ShmConstants.LAST_LOG_MESSAGE, lastLogMessage);
            }
            faBuildingBlockResponseProcessor.sendNhcFaResponse(neJobId, mainJobId, jobCategory, neJobEndAttributes, mainJobAttributes);
        }
    }

    @SuppressWarnings("unchecked")
    private void consolidateNHCNeProperties(final long neJobId, final Map<String, Object> neJobAttr, final Map<String, Object> neJobEndAttributes) {
        try {
            final ServiceFinderBean sfb = new ServiceFinderBean();
            final JobConsolidationService nhcConsolidationService = (JobConsolidationService) sfb.find(JobConsolidationService.class, JobType.NODE_HEALTH_CHECK.name());
            final Map<String, Object> nhcNeJobUpdate = nhcConsolidationService.consolidateNeJobData(neJobId);
            LOGGER.debug("consolidateNHCNeProperties nhcNeJobUpdate {}", nhcNeJobUpdate);

            List<Map<String, Object>> existingJobProperties = (List<Map<String, Object>>) neJobAttr.get(ShmConstants.JOBPROPERTIES);
            final List<Map<String, Object>> jobProperties = (List<Map<String, Object>>) nhcNeJobUpdate.get(ShmConstants.JOBPROPERTIES);
            LOGGER.debug("existingJobProperties {} in ne done nhcNeJobUpdate jobProperties {}", existingJobProperties, jobProperties);
            if(existingJobProperties==null)
            {
                existingJobProperties=new ArrayList<>();
            }
            existingJobProperties.addAll(jobProperties);
            neJobEndAttributes.put(ShmConstants.JOBPROPERTIES, existingJobProperties);
            neJobEndAttributes.put(ShmConstants.NEJOB_HEALTH_STATUS, (String) nhcNeJobUpdate.get(ShmConstants.NEJOB_HEALTH_STATUS));
        } catch (final Exception exception) {
            LOGGER.error("Exception occured while consolidating NHC NE job properties for NE job id {}. Exception is ", neJobId, exception);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void synchAXEInventoryIfAXENode(final long mainJobId, final String nodeName, final String jobResult, final JobType jobType) {
        try {
            if (capabilityMap.get(jobType) != null) {
                final List<NetworkElement> networkElements = fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(Arrays.asList(getParentNodeName(nodeName)), capabilityMap.get(jobType));
                if (!networkElements.isEmpty() && PlatformTypeEnum.AXE == networkElements.get(0).getPlatformType()) {
                    LOGGER.info("invoking inventory sync for AXE NetworkElements:{}", networkElements);
                    if (jobType.equals(JobType.UPGRADE) && (JobResult.SUCCESS.toString()).equals(jobResult)) {
                        axeInvSupervisionRemoteHandler.refreshNetworkInventoryForAxeNodes(networkElements.get(0).getNetworkElementFdn(), axeInventorySyncMap.get(JobType.UPGRADE));
                        axeInvSupervisionRemoteHandler.refreshNetworkInventoryForAxeNodes(networkElements.get(0).getNetworkElementFdn(), axeInventorySyncMap.get(JobType.BACKUP));
                    } else if (axeInventorySyncMap.containsKey(jobType)) {
                        axeInvSupervisionRemoteHandler.refreshNetworkInventoryForAxeNodes(networkElements.get(0).getNetworkElementFdn(), axeInventorySyncMap.get(jobType));
                    }
                }
            }
        } catch (final RuntimeException exception) {
            LOGGER.error("Unable to get JobType Or Inventory Synch failed for {} node ,Exception {}", nodeName, exception);
        }

    }

    private String getParentNodeName(final String nodeName) {
        String parentNodeName = null;
        if (nodeName.contains(ShmConstants.DELIMITER_DOUBLE_UNDERSCORE)) {
            parentNodeName = nodeName.substring(0, nodeName.indexOf(ShmConstants.DELIMITER_DOUBLE_UNDERSCORE));
        } else if (nodeName.contains(ShmConstants.CLUSTER_SUFFIX)) {
            parentNodeName = nodeName.substring(0, nodeName.indexOf(ShmConstants.CLUSTER_SUFFIX));
        } else {
            parentNodeName = nodeName;
        }
        return parentNodeName;
    }

    private boolean isNeJobSkipped(final Map<String, Object> neJobAttr) {
        final List<Map<String, String>> jobPropertyList = (List<Map<String, String>>) neJobAttr.get(ShmConstants.JOBPROPERTIES);
        if (jobPropertyList != null && jobPropertyList.size() > 0) {
            for (final Map<String, String> jobProperty : jobPropertyList) {
                if (jobProperty.get(ShmConstants.VALUE) != null && jobProperty.get(ShmConstants.VALUE).equals(JobResult.SKIPPED.toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param mainJobId
     */
    @SuppressWarnings("unchecked")
    private boolean checkAllNEJobsDone(final long mainJobId, final Map<String, Object> mainJobAttributes) {
        LOGGER.debug("Checking if NE Jobs are completed for Main Job {} ", mainJobId);
        int neCompleted = 0, neSubmitted = 0;
        LOGGER.debug("mainJobAttributes: {}", mainJobAttributes);
        if (MapUtils.isNotEmpty(mainJobAttributes)) {
            final List<Map<String, String>> mainJobPropertyList = (List<Map<String, String>>) mainJobAttributes.get(ShmConstants.JOBPROPERTIES);
            LOGGER.debug("Main Job Property List {} ", mainJobPropertyList);
            if (CollectionUtils.isNotEmpty(mainJobPropertyList)) {
                for (final Map<String, String> jobProperty : mainJobPropertyList) {
                    neSubmitted = getNeSubmitted(neSubmitted, jobProperty);
                    neCompleted = getNeCompleted(neCompleted, jobProperty);
                }
            } else {
                LOGGER.error("Job property List does not exist for {}", mainJobId);
            }
        } else {
            LOGGER.error("Main Job Attributes does not exist for {}", mainJobId);
        }
        return isAllNeJobsDone(neCompleted, neSubmitted);

    }

    /**
     * @param neCompleted
     * @param neSubmitted
     * @return
     */
    private boolean isAllNeJobsDone(int neCompleted, int neSubmitted) {
        boolean isAllNeJobsDone = false;
        if (neSubmitted != 0 && neSubmitted == neCompleted) {
            LOGGER.debug("All submitted and valid NetworkElements[{}] are completed[{}] their NE Job execution.", neSubmitted, neCompleted);
            isAllNeJobsDone = true;
        }
        LOGGER.debug("Number of NE jobs completed={} ", neCompleted);
        return isAllNeJobsDone;
    }

    /**
     * @param neSubmitted
     * @param jobProperty
     * @return
     */
    private int getNeSubmitted(int neSubmitted, final Map<String, String> jobProperty) {
        if (ShmConstants.SUBMITTED_NES.equals(jobProperty.get(ShmConstants.KEY))) {
            final String value = jobProperty.get(ShmConstants.VALUE);
            neSubmitted = Integer.parseInt(value);
        }
        return neSubmitted;
    }

    /**
     * @param neCompleted
     * @param jobProperty
     * @return
     */
    private int getNeCompleted(int neCompleted, final Map<String, String> jobProperty) {
        if (ShmConstants.NE_COMPLETED.equals(jobProperty.get(ShmConstants.KEY))) {
            final String value = jobProperty.get(ShmConstants.VALUE);
            //Adding +1 , since the current job also got completed, and assuming CompletedCount is not updated by MainJobProgressUpdater.updateNeJobsCompletedCount().
            neCompleted = Integer.parseInt(value) + 1;
        }
        return neCompleted;
    }

    private void recordNeJobEndDetails(final Map<String, Object> neJobAttributes, final Date endTime) {
        try {
            final Date startTime = (Date) neJobAttributes.get(ShmConstants.STARTTIME);
            final String neName = (String) neJobAttributes.get(ShmConstants.NE_NAME);
            final String timeSpend = TimeSpendOnJob.getDifference(endTime, startTime);
            final String userMessage = "Job has been completed in " + timeSpend + " with StartTime " + startTime + " and EndTime " + endTime;
            systemRecorder.recordEvent("NeJobComplete", EventLevel.COARSE, "  ", neName, userMessage);
        } catch (final Exception e) {
            LOGGER.error("NE job status updation has failed due to :", e);
        }
    }
}
