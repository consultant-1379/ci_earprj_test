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
package com.ericsson.oss.services.shm.tbac;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;

public class ActivityJobTBACHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityJobTBACHelper.class);

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationService;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    public String getJobExecutionUserFromNeJob(final long neJobId) {
        LOGGER.debug("Enterted into TBACActivityHelper:getJobExecutionUserFromNeJob for NE JobId{}", neJobId);

        final Map<Object, Object> restrictions = new HashMap<Object, Object>();
        restrictions.put(ObjectField.PO_ID, neJobId);
        final List<Map<String, Object>> neJobs = jobConfigurationService.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.NE_JOB, restrictions,
                Arrays.asList(ShmConstants.SHM_JOB_EXEC_USER));
        return (String) neJobs.get(0).get(ShmConstants.SHM_JOB_EXEC_USER);
    }

    public String getJobExecutionUserFromMainJob(final long mainJobId) {
        LOGGER.debug("Enterted into TBACActivityHelper:getJobExecutionUserFromMainJob for Main JobId{}", mainJobId);

        final Map<Object, Object> restrictions = new HashMap<Object, Object>();
        restrictions.put(ObjectField.PO_ID, mainJobId);
        final List<Map<String, Object>> mainJobAttributes = jobConfigurationService.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.JOB, restrictions,
                Arrays.asList(ShmConstants.SHM_JOB_EXEC_USER));
        return (String) mainJobAttributes.get(0).get(ShmConstants.SHM_JOB_EXEC_USER);
    }

    public String getActivityExecutionMode(final NEJobStaticData neJobStaticData, final JobStaticData jobStaticData, final String activityName) throws MoNotFoundException, JobDataNotFoundException {
        LOGGER.debug("Enterted into TBACActivityHelper:getActivityExecutionMode for activity {}", activityName);
        final String neType = networkElementRetrievalBean.getNeType(getNodeName(neJobStaticData));
        final String activityScheduleKey = neJobStaticData.getMainJobId() + "_" + neType + "_" + activityName.toLowerCase();
        return (String) jobStaticData.getActivitySchedules().get(activityScheduleKey);
    }

    /**
     * @param neJobStaticData
     * @return
     */
    private String getNodeName(final NEJobStaticData neJobStaticData) {
        String nodeName;
        if (neJobStaticData.getParentNodeName() != null) {
            nodeName = neJobStaticData.getParentNodeName();
        } else {
            nodeName = neJobStaticData.getNodeName();
        }
        return nodeName;
    }
}
