/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.housekeeping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.job.activity.JobType;

/**
 * 
 * This class is used to check if housekeeping is required for each job type and then trigger housekeeping if required.
 * 
 * @author xsrakon
 * 
 */

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class JobsHouseKeepingService {

    @Inject
    private HouseKeepingConfigParamChangeListener houseKeepingConfigParamChangeListener;

    @Inject
    private JobsHouseKeepingDelegator jobsHouseKeepingDelegator;

    @Inject
    private JobsHouseKeepingClusterCounterManager jobsHouseKeepingClusterCounterManager;

    @Inject
    private JobsHouseKeepingHelperUtil jobsHouseKeepingHelperUtil;

    private static final Logger LOGGER = LoggerFactory.getLogger(JobsHouseKeepingService.class);

    /**
     * 
     * This method checks the below conditions
     * 
     * 1) Any housekeeping process is running or not. If not checks point No 2.
     * 
     * 2) Age of a job(i.e job creation date crosses the specified number of days) and Count of jobs exceeds specified limit in the model.
     * 
     * 3) If yes, delegates the request for deletion.
     * 
     */
    public void triggerHouseKeepingOfJobs() {

        final List<Future<JobsHouseKeepingResponse>> responsesBasedOnAge = new ArrayList<Future<JobsHouseKeepingResponse>>();
        final List<Future<JobsHouseKeepingResponse>> responsesBasedOnCount = new ArrayList<Future<JobsHouseKeepingResponse>>();

        final long houseKeepingCounter = jobsHouseKeepingClusterCounterManager.getClusterCounter();
        if (houseKeepingCounter == 0) {
            //Here we are setting counter value to 1(i.e Housekeeping is Running).
            jobsHouseKeepingClusterCounterManager.setClusterCounter(true);
            LOGGER.info("HouseKeepingCounter Value after update {} ", jobsHouseKeepingClusterCounterManager.getClusterCounter());
            final List<JobsHouseKeepingResponse> cumulativeHouseKeepingResponse = new ArrayList<JobsHouseKeepingResponse>();
            final JobType[] jobTypes = JobType.values();
            try {
                populateCumulativeResponseBasedOnJobState(cumulativeHouseKeepingResponse);
                populateCumulativeResponseBasedOnAge(responsesBasedOnAge, cumulativeHouseKeepingResponse, jobTypes);
                populateCumulativeResponseBasedOnCount(responsesBasedOnCount, cumulativeHouseKeepingResponse, jobTypes);
                prepareFinalHouseKeepingResponse(cumulativeHouseKeepingResponse);
                LOGGER.info("HouseKeepingOfJobs Process Completed.");
            } catch (final Exception e) {
                LOGGER.error("Exception occured in HouseKeepingService-Reason [{}] ", e);
            } finally {
                // Here we are setting counter value to 0(i.e Housekeeping Process is Completed/NotRunning).
                jobsHouseKeepingClusterCounterManager.setClusterCounter(false);
                LOGGER.info("Updating HouseKeepingCounter Value to {} again", jobsHouseKeepingClusterCounterManager.getClusterCounter());
            }
        } else {
            LOGGER.error("HouseKeeping Process is already triggered and is ongoing, So Please try after some time.");
        }

    }

    private void populateCumulativeResponseBasedOnJobState(final List<JobsHouseKeepingResponse> cumulativeHouseKeepingResponse) {

        final Future<JobsHouseKeepingResponse> responseBasedOnJobStatus = triggerHouseKeepingBasedOnJobstate();

        try {
            if (responseBasedOnJobStatus != null) {
                cumulativeHouseKeepingResponse.add(responseBasedOnJobStatus.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Unable to retrieve the response of housekeeping Based On job state [DELETING]. Reason : {}", e);
        } catch (Exception e) {
            LOGGER.error("Exception occured while retrieving the response of housekeeping Based On job state [DELETING]. Reason : {}", e);
        }
    }

    private Future<JobsHouseKeepingResponse> triggerHouseKeepingBasedOnJobstate() {
        Future<JobsHouseKeepingResponse> houseKeepingResponse = null;

        final List<Long> mainJobsInDeletingState = jobsHouseKeepingHelperUtil.fetchJobsInDeletingStatus();
        houseKeepingResponse = jobsHouseKeepingDelegator.houseKeepingOfJobs(mainJobsInDeletingState);

        return houseKeepingResponse;
    }

    /**
     * @param responsesBasedOnCount
     * @param liveBucket
     * @param cumulativeHouseKeepingResponse
     * @param jobTypes
     */
    private void populateCumulativeResponseBasedOnCount(final List<Future<JobsHouseKeepingResponse>> houseKeepingResponseBasedOnCountList,
            final List<JobsHouseKeepingResponse> cumulativeHouseKeepingResponse, final JobType[] jobTypes) {
        Future<JobsHouseKeepingResponse> responseBasedOnCount;
        for (final JobType jobType : jobTypes) {
            if (!jobType.toString().equals(JobType.SYSTEM.toString())) {
                final int jobCount = houseKeepingConfigParamChangeListener.getJobCountForHouseKeeping(jobType);
                LOGGER.info("Started fetching of PO Ids BasedOnCount for JobType {} with Max count {}", jobType, jobCount);
                responseBasedOnCount = triggerHouseKeepingBasedOnCount(jobType.toString(), jobCount);
                if (responseBasedOnCount != null) {
                    houseKeepingResponseBasedOnCountList.add(responseBasedOnCount);
                }
            }
        }
        try {
            for (final Future<JobsHouseKeepingResponse> response : houseKeepingResponseBasedOnCountList) {
                if (response != null) {
                    cumulativeHouseKeepingResponse.add(response.get());
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Unable to retrieve the response of housekeeping Based On Count", e);
        }
    }

    /**
     * @param responsesBasedOnAge
     * @param liveBucket
     * @param cumulativeHouseKeepingResponse
     * @param jobTypes
     */
    private void populateCumulativeResponseBasedOnAge(final List<Future<JobsHouseKeepingResponse>> houseKeepingResponseBasedOnAgeList,
            final List<JobsHouseKeepingResponse> cumulativeHouseKeepingResponse, final JobType[] jobTypes) {
        Future<JobsHouseKeepingResponse> responseBasedOnAge;
        for (final JobType jobType : jobTypes) {
            if (!jobType.toString().equals(JobType.SYSTEM.toString())) {
                LOGGER.info("Started fetching of PO Ids BasedOnAge for JobType {} with Age {}", jobType.toString(), houseKeepingConfigParamChangeListener.getMinJobAgeForHouseKeeping(jobType));
                responseBasedOnAge = triggerHouseKeepingBasedOnAge(jobType.toString(), houseKeepingConfigParamChangeListener.getMinJobAgeForHouseKeeping(jobType));
                if (responseBasedOnAge != null) {
                    houseKeepingResponseBasedOnAgeList.add(responseBasedOnAge);
                }
            }
        }
        try {
            for (final Future<JobsHouseKeepingResponse> response : houseKeepingResponseBasedOnAgeList) {
                if (response != null) {
                    cumulativeHouseKeepingResponse.add(response.get());
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Unable to retrieve the response of housekeeping Based On Age", e);
        }
    }

    /**
     * @param liveBucket
     * @param string
     * @param backupJobCountForHouseKeeping
     */
    public Future<JobsHouseKeepingResponse> triggerHouseKeepingBasedOnCount(final String jobType, final int specificJobCountForHouseKeeping) {
        Future<JobsHouseKeepingResponse> houseKeepingResponse = null;
        final List<Object[]> poIdAndEndTimeEntries = jobsHouseKeepingHelperUtil.fetchJobTypeSpecificPoIdsByCount(jobType);
        if (!poIdAndEndTimeEntries.isEmpty()) {
            final List<Long> jobPoIdsList = fetchPoIdsIfHouseKeepingRequired(poIdAndEndTimeEntries, specificJobCountForHouseKeeping, jobType);
            if (jobPoIdsList != null && !jobPoIdsList.isEmpty()) {
                LOGGER.info("Based On Count HouseKeeping is required for {} poIds of  {} jobs", jobPoIdsList.size(), jobType);
                houseKeepingResponse = jobsHouseKeepingDelegator.houseKeepingOfJobs(jobPoIdsList, jobType);
            }
        } else {
            LOGGER.info("Based On Count - No HouseKeeping is required for {} jobs ", jobType);
        }
        return houseKeepingResponse;
    }

    /**
     * @param liveBucket
     * @param string
     * @param backupJobCountForHouseKeeping
     */
    public Future<JobsHouseKeepingResponse> triggerHouseKeepingBasedOnAge(final String jobType, final int maxJobAge) {
        Future<JobsHouseKeepingResponse> houseKeepingResponse = null;
        final List<Long> jobPoIds = jobsHouseKeepingHelperUtil.fetchJobTypeSpecificPoIdsByAge(jobType, maxJobAge);
        if (jobPoIds != null && !jobPoIds.isEmpty()) {
            LOGGER.info("Based On Age HouseKeeping is required for {} poIds of  {} jobs", jobPoIds.size(), jobType);
            houseKeepingResponse = jobsHouseKeepingDelegator.houseKeepingOfJobs(jobPoIds, jobType);
        } else {
            LOGGER.info("Based On Age - No HouseKeeping is required for {} jobs ", jobType);
        }
        return houseKeepingResponse;
    }

    private List<Long> fetchPoIdsIfHouseKeepingRequired(final List<Object[]> poIdAndEndTimeEntries, final int jobCountToKeep, final String jobType) {
        final List<Long> posToBeDeleted = new ArrayList<Long>();
        int noOfScheduledJobs = 0;
        int noOfJobsAddedToDelete = 0;
        final Map<Long, Date> poIdsAndEndTimeMap = new HashMap<Long, Date>();
        if (poIdAndEndTimeEntries.size() > jobCountToKeep) {
            //Save Incomplete jobs and consider only completed jobs for deletion
            for (final Object[] poObject : poIdAndEndTimeEntries) {
                final Date endTime = (Date) poObject[1];
                if (endTime == null) {
                    noOfScheduledJobs++;
                    continue;
                }
                poIdsAndEndTimeMap.put((long) poObject[0], endTime);
            }
            //Will give number of complete jobs to be deleted after considering running jobs in noOfScheduledJobs variable
            final int noOfCompletedJobsToBeDeleted = poIdAndEndTimeEntries.size() - jobCountToKeep;
            //All Jobs are in Scheduled/Wait For User Input Mode in database
            if (noOfCompletedJobsToBeDeleted <= 0 || noOfScheduledJobs == poIdAndEndTimeEntries.size()) {
                LOGGER.debug("HouseKeeping is not triggered for {} type as all Jobs are Scheduled", jobType);
                return null;
            }

            final List<Entry<Long, Date>> endTimeEntryList = new ArrayList<Entry<Long, Date>>(poIdsAndEndTimeMap.entrySet());

            //Sort the List in Ascending order with latest job Po Id as last entry
            Collections.sort(endTimeEntryList, new Comparator<Map.Entry<Long, Date>>() {
                @Override
                public int compare(final Map.Entry<Long, Date> endTime1, final Map.Entry<Long, Date> endTime2) {
                    return (endTime1.getValue()).compareTo(endTime2.getValue());
                }
            });

            //Oldest entry Po id is added in same order
            for (final Entry<Long, Date> endTimeSortedEntry : endTimeEntryList) {
                posToBeDeleted.add(endTimeSortedEntry.getKey());
                noOfJobsAddedToDelete++;
                if (noOfJobsAddedToDelete == noOfCompletedJobsToBeDeleted) {
                    break;
                }
            }
        } else {
            LOGGER.info("Based on Count No HouseKeeping required for {} jobs", jobType);
        }
        LOGGER.info("{} poIds requires HouseKeeping based on Count for {}", posToBeDeleted.size(), jobType);
        return posToBeDeleted;
    }

    private void prepareFinalHouseKeepingResponse(final List<JobsHouseKeepingResponse> listOfAllJobsHouseKeepingResponse) {
        final JobsHouseKeepingResponse houseKeepingResponse = new JobsHouseKeepingResponse();

        int totalSuccessCount = 0;
        int totalFailureCount = 0;
        int totalJobsNotFoundCount = 0;

        for (final JobsHouseKeepingResponse response : listOfAllJobsHouseKeepingResponse) {
            if (response != null) {
                if (response.getSuccessfullyDeletedJobsCount() != 0) {
                    totalSuccessCount += response.getSuccessfullyDeletedJobsCount();
                }
                if (response.getFailedToDeleteJobsCount() != 0) {
                    totalFailureCount += response.getFailedToDeleteJobsCount();
                }
                if (response.getJobsNotFoundCount() != 0) {
                    totalJobsNotFoundCount += response.getJobsNotFoundCount();
                }
            }
        }
        houseKeepingResponse.setSuccessfullyDeletedJobsCount(totalSuccessCount);
        houseKeepingResponse.setFailedToDeleteJobsCount(totalFailureCount);
        houseKeepingResponse.setJobsNotFoundCount(totalJobsNotFoundCount);

        LOGGER.info("Total No. of Jobs Deleted through housekeeping {}", totalSuccessCount);
        LOGGER.info("Total No. of Jobs Failed to Delete through housekeeping {}", totalFailureCount);
        LOGGER.info("Total No. of Jobs Not Found through housekeeping {}", totalJobsNotFoundCount);
    }
}
