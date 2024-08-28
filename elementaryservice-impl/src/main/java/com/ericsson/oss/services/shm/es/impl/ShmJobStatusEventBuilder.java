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
package com.ericsson.oss.services.shm.es.impl;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.events.ShmJobStatusEvent;
import com.ericsson.oss.services.shm.shared.util.JobPropertyUtil;

public class ShmJobStatusEventBuilder {

    private static final String JOB_FAILED_FOR_NETWORK_ELEMENT = "Job Failed for network element: ";
    private static final String JOB_SUCCESSFUL_FOR_NETWORK_ELEMENT = "Job Successful for network element: ";
    private static final String JOB_SKIPPED_FOR_NETWORK_ELEMENT = "Job skipped for network element: ";

    private static final Logger LOGGER = LoggerFactory.getLogger(ShmJobStatusEventBuilder.class);

    public ShmJobStatusEvent buildJobStatusEvent(final Map<String, Object> jobAttributes, final String jobResult) {

        LOGGER.debug("Building JobStatusEvent with requried attributes {} and result {}", jobAttributes, jobResult);
        final ShmJobStatusEvent shmJobStatusEvent = new ShmJobStatusEvent();
        final String nodeName = getNodeNameFromJobAttributes(jobAttributes);
        shmJobStatusEvent.setNodeName(nodeName);
        shmJobStatusEvent.setMessageId(getJobNameFromJobAtrributes(jobAttributes));
        if (jobResult.equalsIgnoreCase(JobResult.SUCCESS.getJobResult())) {
            shmJobStatusEvent.setResult(com.ericsson.oss.services.shm.model.events.JobResult.SUCCESS);
            shmJobStatusEvent.setMessage(JOB_SUCCESSFUL_FOR_NETWORK_ELEMENT + nodeName);
        } else if (jobResult.equalsIgnoreCase(JobResult.SKIPPED.getJobResult())) {
            shmJobStatusEvent.setResult(com.ericsson.oss.services.shm.model.events.JobResult.SUCCESS);
            shmJobStatusEvent.setMessage(JOB_SKIPPED_FOR_NETWORK_ELEMENT + nodeName);
        } else if (jobResult.equalsIgnoreCase(JobResult.FAILED.getJobResult())) {
            shmJobStatusEvent.setResult(com.ericsson.oss.services.shm.model.events.JobResult.FAILED);
            shmJobStatusEvent.setMessage(JOB_FAILED_FOR_NETWORK_ELEMENT + nodeName);
        }
        LOGGER.debug("JobStatusEvent has been built as: {}", shmJobStatusEvent);
        return shmJobStatusEvent;
    }

    @SuppressWarnings("unchecked")
    private String getJobNameFromJobAtrributes(final Map<String, Object> jobAttributes) {
        final List<Map<String, String>> jobPropertyList = (List<Map<String, String>>) jobAttributes.get(ShmConstants.JOBPROPERTIES);
        final String jobName = JobPropertyUtil.getProperty(jobPropertyList, ShmConstants.JOBNAME);
        return (jobName != null && !jobName.isEmpty()) ? jobName : "";
    }

    @SuppressWarnings("unchecked")
    private String getNodeNameFromJobAttributes(final Map<String, Object> jobAttributes) {
        String nodeName = "";
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) jobAttributes.get(ShmConstants.JOBCONFIGURATIONDETAILS);
        final Map<String, Object> selectedNEs = (Map<String, Object>) jobConfigurationDetails.get(ShmConstants.SELECTED_NES);
        if (selectedNEs.containsKey(ShmConstants.NENAMES)) {
            final List<String> nodeNames = (List<String>) selectedNEs.get((ShmConstants.NENAMES));
            nodeName = nodeNames.get(0);
        }
        return nodeName;
    }
}
