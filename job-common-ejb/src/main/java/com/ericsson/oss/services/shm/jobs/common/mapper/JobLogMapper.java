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

package com.ericsson.oss.services.shm.jobs.common.mapper;

import static com.ericsson.oss.services.shm.jobs.common.constants.JobConfigurationConstants.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobLogDetails;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobLogResponse;
import com.ericsson.oss.services.shm.jobs.common.modelentities.LogDetails;
import com.ericsson.oss.services.shm.jobs.common.modelentities.MainJobLogDetails;
import com.ericsson.oss.services.shm.jobs.common.modelentities.NeJobLogDetails;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;

public class JobLogMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobLogMapper.class);

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    /**
     * This method prepares a list of JobLogResponse containing NE name and activity log details in String format
     * 
     * @param NeJobLogDetails
     *            neJobDetails
     * @return List<JobLogResponse>
     * 
     */
    public List<JobLogResponse> getNEJobLogResponse(final NeJobLogDetails neJobLogDetails) {
        LOGGER.debug("getNEJobLogResponse method entry : {}", neJobLogDetails);
        final List<JobLogResponse> jobLogResponseList = new ArrayList<JobLogResponse>();
        JobLogResponse jobLogResponse = null;
        if (neJobLogDetails.getError() == null) {
            jobLogResponse = new JobLogResponse();
            final String neName = neJobLogDetails.getNeJobName();
            final String nodeType = neJobLogDetails.getNodeType();
            final List<JobLogDetails> jobLogDetails = neJobLogDetails.getJobLogDetails();
            if (jobLogDetails != null) {
                jobLogResponseList.addAll(fillJobLogs(jobLogDetails, neName, nodeType));
            }
        } else {
            jobLogResponse = new JobLogResponse();
            jobLogResponse.setError(neJobLogDetails.getError());
            jobLogResponse.setLogLevel(JobLogLevel.ERROR.toString());
            jobLogResponseList.add(jobLogResponse);
        }
        LOGGER.debug("getNEJobLogResponse method exit {}", jobLogResponse.getNeName());
        return jobLogResponseList;
    }

    public List<JobLogResponse> getMainJobLogResponse(final MainJobLogDetails mainJobLogDetails) {
        LOGGER.debug("getMainJobLogResponse method entry : {}", mainJobLogDetails);
        final List<JobLogResponse> jobLogResponseList = new ArrayList<JobLogResponse>();
        if (mainJobLogDetails != null) {
            final List<JobLogDetails> jobLogDetails = mainJobLogDetails.getJobLogDetails();
            final String nodeName = "";
            final String nodeType = "";
            jobLogResponseList.addAll(fillJobLogs(jobLogDetails, nodeName, nodeType));
        }
        LOGGER.debug("getMainJobLogResponse method exit");
        return jobLogResponseList;
    }

    private List<JobLogResponse> fillJobLogs(final List<JobLogDetails> jobLogDetails, final String neName, final String nodeType) {
        LOGGER.debug("jobLogDetails from fillJobLogs API: {} ", jobLogDetails);
        final List<JobLogResponse> jobLogResponseList = new ArrayList<JobLogResponse>();
        if (jobLogDetails != null) {
            for (final JobLogDetails jobLogDetail : jobLogDetails) {
                final String activityName = jobLogDetail.getActivityName();
                final List<LogDetails> logDetails = jobLogDetail.getActivityLogs();
                if (logDetails != null) {
                    for (final LogDetails logDetail : logDetails) {
                        final String entryTime = logDetail.getEntryTime();
                        final String message = logDetail.getMessage();
                        final String logLevel = logDetail.getLogLevel();
                        final JobLogResponse jobLogResponse = new JobLogResponse();
                        jobLogResponse.setEntryTime(entryTime);
                        jobLogResponse.setMessage(message);
                        jobLogResponse.setNeName(neName);
                        jobLogResponse.setActivityName(activityName);
                        jobLogResponse.setLogLevel(logLevel);
                        jobLogResponse.setNodeType(nodeType);
                        jobLogResponseList.add(jobLogResponse);
                    }
                }
            }
        }
        return jobLogResponseList;
    }

    /**
     * Method to filter and update the fields to <code>ActivityJobDetails</code> from given <code>activityJobAttributesMap</code>
     * 
     * @param activityJobAttributesMap
     * @return ActivityJobDetails
     */
    @SuppressWarnings("unchecked")
    public JobLogDetails mapJobAttributesToJobLogDetails(final Map<String, Object> jobAttributes) {
        final List<LogDetails> formattedLogDetails = new ArrayList<LogDetails>();
        if (jobAttributes != null && jobAttributes.get(LOG) != null) {
            final List<HashMap<String, Object>> logDetails = (List<HashMap<String, Object>>) jobAttributes.get(LOG);
            for (final HashMap<String, Object> logDetail : logDetails) {
                if (logDetail != null && !logDetail.isEmpty()) {
                    final Date date = (Date) logDetail.get(ENTRYTIME);
                    final String dateString = String.valueOf(date.getTime());
                    final LogDetails log = new LogDetails();
                    log.setEntryTime(dateString);
                    if (logDetail.get(MESSAGE) != null) {
                        log.setMessage(logDetail.get(MESSAGE).toString());
                    }
                    if (logDetail.get(JOB_lOG_lEVEL) != null) {
                        log.setLogLevel(logDetail.get(JOB_lOG_lEVEL).toString());
                    }
                    formattedLogDetails.add(log);
                }
            }
        }
        final JobLogDetails joblogDetails = new JobLogDetails();
        if (jobAttributes != null && jobAttributes.get(JOB_NAME) != null) {
            joblogDetails.setActivityName((String) jobAttributes.get(JOB_NAME));
            joblogDetails.setActivityLogs(formattedLogDetails);
        } else {
            joblogDetails.setActivityName("");
            joblogDetails.setActivityLogs(formattedLogDetails);
        }
        LOGGER.debug("Returning from mapper as:{} ", joblogDetails.getActivityName());
        return joblogDetails;
    }

    /**
     * Method to map NE joblogs to NEJobLogDetails POJO .
     * 
     * @param jobLogDetails
     * @param neName
     * @return NeJobLogDetails
     */
    public NeJobLogDetails mapNEJobLogDetailsFromJobLogDetails(final List<JobLogDetails> jobLogDetails, final String neName, String nodeType) {
        LOGGER.debug("mapNEJobLogDetailsFromJobLogs method entry {}", neName);
        final NeJobLogDetails neJobLogDetails = new NeJobLogDetails();
        neJobLogDetails.setNeJobName(neName);
        neJobLogDetails.setJobLogDetails(jobLogDetails);
        if (nodeType == null) {
            try {
                nodeType = networkElementRetrievalBean.getNeType(neName);
            } catch (MoNotFoundException e) {
                nodeType = "";
            }
        }
        neJobLogDetails.setNodeType(nodeType);
        LOGGER.debug("mapNEJobLogDetailsFromJobLogs method exit {}", neJobLogDetails.getNeJobName());
        return neJobLogDetails;
    }

    /**
     * Method to map Main job logs to MainJobLogDetails POJO.
     * 
     * @param jobLogDetails
     * @return MainJobLogDetails
     */
    public MainJobLogDetails mapMainJobLogDetailsFromJobLogDetails(final List<JobLogDetails> jobLogDetails) {
        LOGGER.debug("mapMainJobLogsFromJobLogs method entry");
        final MainJobLogDetails mainJobLogDetails = new MainJobLogDetails();
        mainJobLogDetails.setJobLogDetails(jobLogDetails);
        LOGGER.debug("mapMainJobLogsFromJobLogs method exit ");
        return mainJobLogDetails;
    }

}
