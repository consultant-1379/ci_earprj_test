/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.activity.timeout.models.NodeHealthCheckJobActivityTimeouts;
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.fa.api.FaBuildingBlockResponseProvider;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.HealthStatus;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobCategory;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.util.JobPropertyUtil;

/**
 * This class provided methods to process activity/NE job attributes and send response to FA.
 * 
 * @author znlxvnk
 * 
 */
public class FaBuildingBlockResponseProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(FaBuildingBlockResponseProcessor.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceRetryProxy;

    @Inject
    private FaBuildingBlockResponseProvider buildingBlockResponseProvider;

    /**
     * This method will validate and process the given activity job PO attributes, prepare the FA response attributes and send the response to FA.
     * 
     * @param activityJobId
     * @param jobType
     * @param jobResult
     * @param activityJobPoAttributes
     * @return
     */
    @SuppressWarnings("unchecked")
    public void sendFaResponse(final long activityJobId, final String jobType, final String jobResult, final Map<String, Object> activityJobPoAttributes) {
        try {
            LOGGER.info("Entered into FaBuildingBlockResponseProcessor.sendFaResponse: {}", activityJobPoAttributes);
            final String capability = activityUtils.getCapabilityByJobType(jobType);
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, capability);
            final long mainJobId = neJobStaticData.getMainJobId();
            final Map<String, Object> mainJobAttributes = activityUtils.getPoAttributes(mainJobId);
            final long neJobId = neJobStaticData.getNeJobId();
            LOGGER.debug("Inside updateJobProgress of FaBuildingBlockResponseProcessor with mainjobAttributes : {} ", mainJobAttributes);
            final List<Map<String, String>> jobPropertyList = (List<Map<String, String>>) mainJobAttributes.get(ShmConstants.JOBPROPERTIES);
            final String jobCategory = JobPropertyUtil.getProperty(jobPropertyList, ShmConstants.JOB_CATEGORY);
            String lastLogMessage = (String) activityJobPoAttributes.get(ShmConstants.LAST_LOG_MESSAGE);
            final String activityName = (String) activityJobPoAttributes.get(ShmJobConstants.ACTIVITY_NAME);
            if (JobCategory.FA == JobCategory.getJobCategory(jobCategory)) {
                LOGGER.info("In sendFaResponse JobCategory is: {}", jobCategory);
                lastLogMessage = getUpdatedLastLogMessage(lastLogMessage, neJobId, mainJobAttributes, activityName, jobResult);
                sendResponseToFa(activityJobId, jobResult, activityJobPoAttributes, neJobStaticData.getNodeName(), mainJobId, lastLogMessage, jobCategory);
            }
        } catch (final Exception ex) {
            LOGGER.error("Error occurred while sending response to FA with activityJobId {} ,so failed to send message to building block with Exception: ", activityJobId, ex);
        }
    }

    /**
     * This method will validate and process the given NE/main job PO attributes, consolidate the node health check response status from NE job PO attributes, prepare the FA response attributes and
     * send the response to FA.
     * 
     * @param neJobId
     * @param mainJobId
     * @param jobType
     * @param jobCategory
     * @param neJobPoAttributes
     * @param mainJobAttributes
     * @return
     */
    @SuppressWarnings("unchecked")
    public void sendNhcFaResponse(final long neJobId, final long mainJobId, final String jobCategory, final Map<String, Object> neJobPoAttributes, final Map<String, Object> mainJobAttributes) {
        LOGGER.info("Entered into FaBuildingBlockResponseProcessor.sendNhcFaResponse with neJobPoAttributes: {}", neJobPoAttributes);
        final long activityjobId = -1L;
        try {
            final String healthCheckStatus = (String) neJobPoAttributes.get(ShmConstants.NEJOB_HEALTH_STATUS);
            String result = (String) neJobPoAttributes.get(ShmJobConstants.RESULT);
            final Map<String, Object> activityAttributes = (Map<String, Object>) getActivityDetais(neJobId).get(ShmConstants.PO_ATTRIBUTES);
            if (MapUtils.isNotEmpty(activityAttributes)) {
                LOGGER.info("NHC Activity Details: {}", activityAttributes);
                final String activityName = (String) activityAttributes.get(ShmJobConstants.ACTIVITY_NAME);
                final String lastLogMessage = getUpdatedLastLogMessage((String) activityAttributes.get(ShmConstants.LAST_LOG_MESSAGE), (String) neJobPoAttributes.get(ShmConstants.LAST_LOG_MESSAGE),
                        mainJobAttributes, activityName, result);
                result = getUpdatedResultByHealthStatus(healthCheckStatus, result);
                //sending activityJobId as -1 as NHC job there are 2 activities but FA treat them as single activity. So, sending of one activityJob instead of 2 as respone expecting single activityJob. activityJob id is not mandatory anyway.
                sendResponseToFa(activityjobId, result, activityAttributes, (String) neJobPoAttributes.get(ShmConstants.NE_NAME), mainJobId, lastLogMessage, jobCategory);
            } else {
                LOGGER.warn("Failed to get activityJob information using NEJob Id:{}", neJobId);
            }

        } catch (final Exception ex) {
            LOGGER.error("Error occurred while sending response to FA with neJobId {} ,so failed to send message to building block with Exception: ", neJobId, ex);
        }
    }

    private Map<String, Object> getActivityDetais(final long neJobId) {

        final Map<String, Object> restrictions = new HashMap<>();
        restrictions.put(ShmJobConstants.NE_JOB_ID, neJobId);
        try {
            final List<Map<String, Object>> activityJobPos = jobConfigurationServiceRetryProxy.getActivityJobAttributesByNeJobId(neJobId, restrictions);
            if (CollectionUtils.isNotEmpty(activityJobPos)) {
                if (activityJobPos.size() == 1) {
                    return activityJobPos.get(0);
                } else {
                    for (final Map<String, Object> activityPo : activityJobPos) {
                        final Map<String, Object> poAttributes = (Map<String, Object>) activityPo.get(ShmConstants.PO_ATTRIBUTES);
                        if (NodeHealthCheckJobActivityTimeouts.NODE_HEALTH_CHECK.equals(poAttributes.get(ShmConstants.NAME))) {
                            return activityPo;
                        }
                    }
                }
            }
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while retrieving activityJob po detials using neJobId: {}. Exception is: ", neJobId, ex);
        }
        return new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private void sendResponseToFa(final long activityJobId, final String jobResult, final Map<String, Object> activityJobPoAttributes, final String neName, final long mainJobId, String lastLogMessage,
            final String jobCategory) {
        final Map<String, Object> responseToFa = new HashMap<>();
        final List<Map<String, Object>> activityJobProperties = (List<Map<String, Object>>) activityJobPoAttributes.get(ShmJobConstants.JOBPROPERTIES);
        final String activityName = (String) activityJobPoAttributes.get(ShmJobConstants.ACTIVITY_NAME);
        LOGGER.info("activityJobProperties: {} from faResponse sender", activityJobProperties);
        for (final Map<String, Object> property : activityJobProperties) {
            if (((String) property.get(ShmJobConstants.KEY)).equalsIgnoreCase(ShmJobConstants.FaCommonConstants.FA_REQUEST_ID)) {
                responseToFa.put(ShmJobConstants.FaCommonConstants.FA_REQUEST_ID, property.get(ShmJobConstants.VALUE));
            } else if (((String) property.get(ShmJobConstants.KEY)).equalsIgnoreCase(ShmJobConstants.FaCommonConstants.FLOW_EXECUTION_NAME)) {
                responseToFa.put(ShmJobConstants.FaCommonConstants.FLOW_EXECUTION_NAME, property.get(ShmJobConstants.VALUE));
            } else if (((String) property.get(ShmJobConstants.KEY)).equalsIgnoreCase(ShmJobConstants.USERNAME)) {
                responseToFa.put(ShmJobConstants.USERNAME, property.get(ShmJobConstants.VALUE));
            } else if (((String) property.get(ShmJobConstants.KEY)).equalsIgnoreCase(ShmJobConstants.FaCommonConstants.FBB_TYPE)) {
                responseToFa.put(ShmJobConstants.FaCommonConstants.FBB_TYPE, property.get(ShmJobConstants.VALUE));
            }
        }
        responseToFa.put(ShmConstants.LAST_LOG_MESSAGE, lastLogMessage);
        responseToFa.put(ShmConstants.RESULT, jobResult);
        responseToFa.put(ShmConstants.STATE, JobState.COMPLETED.getJobStateName());
        responseToFa.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        responseToFa.put(ShmConstants.NE_JOB_ID, activityJobPoAttributes.get(ShmJobConstants.NE_JOB_ID));
        responseToFa.put(ShmJobConstants.NE_NAME, neName);
        responseToFa.put(ShmConstants.ACTIVITY_NAME, activityName);
        responseToFa.put(ActivityConstants.ACTIVITY_JOB_ID, activityJobId);
        LOGGER.info("Sending {} response:{} after activity completion.", jobCategory, responseToFa);
        buildingBlockResponseProvider.send(responseToFa, JobCategory.getJobCategory(jobCategory));
    }

    private String getUpdatedResultByHealthStatus(final String healthCheckStatus, final String result) {
        if (null == healthCheckStatus || !(HealthStatus.HEALTHY.equals(HealthStatus.valueOf(healthCheckStatus)) || HealthStatus.WARNING.equals(HealthStatus.valueOf(healthCheckStatus)))) {
            LOGGER.error("Node Health Check reported the Health Status as {}", healthCheckStatus);
            return JobResult.FAILED.getJobResult();
        } else {
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    private String getUpdatedLastLogMessage(String lastLogMessage, final String neJobLastLogMessage, final Map<String, Object> mainJobAttributes, final String activityName, final String result) {
        if (StringUtils.isNotBlank(lastLogMessage)) {
            return format(lastLogMessage, activityName);
        }
        LOGGER.info("Retrieved LastLogMessage:{} from NEJob PO as it is not available in ActivityJob", neJobLastLogMessage);
        if (StringUtils.isNotBlank(neJobLastLogMessage)) {
            return format(neJobLastLogMessage, activityName);
        }
        if (MapUtils.isNotEmpty(mainJobAttributes)) {
            final List<Map<String, String>> log = (List<Map<String, String>>) mainJobAttributes.get(ShmJobConstants.LOG);
            lastLogMessage = CollectionUtils.isNotEmpty(log) ? getMessage(log, ShmJobConstants.MESSAGE) : ActivityConstants.EMPTY;
            LOGGER.info("Retrieved LastLogMessage:{} from Job PO as it is not available in ActivityJob/NEJob", log);
            if (StringUtils.isNotBlank(lastLogMessage)) {
                return format(lastLogMessage, activityName);
            }
        }
        return getDefaultLastLogMsg(activityName, result);
    }

    private String getUpdatedLastLogMessage(String lastLogMessage, final long neJobId, final Map<String, Object> mainJobAttributes, final String activityName, final String result) {
        if (StringUtils.isNotBlank(lastLogMessage)) {
            return format(lastLogMessage, activityName);
        }
        final Map<String, Object> neJobAttributes = activityUtils.getPoAttributes(neJobId);
        return getUpdatedLastLogMessage(lastLogMessage, (String) neJobAttributes.get(ShmConstants.LAST_LOG_MESSAGE), mainJobAttributes, activityName, result);

    }

    private String getMessage(final List<Map<String, String>> logList, final String key) {
        String message = ActivityConstants.EMPTY;
        for (final Map<String, String> log : logList) {
            if (log.containsKey(key)) {
                message = log.get(key);
                break;
            }
        }
        return message;
    }

    private String getDefaultLastLogMsg(final String activityName, final String result) {
        String lastLogMessage = "";
        if (ShmJobConstants.SUCCESS.equalsIgnoreCase(result)) {
            lastLogMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, activityName);
        } else if (ShmJobConstants.SKIPPED.equalsIgnoreCase(result)) {
            lastLogMessage = String.format(JobLogConstants.ACTIVITY_SKIPPED, activityName);
        } else {
            lastLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, activityName);
        }
        return format(lastLogMessage, activityName);
    }

    private String format(String lastLogMessage, final String activityName) {
        LOGGER.debug("LastLogMessage:{} for activity:{}", lastLogMessage, activityName);
        final String DELIMITER_PIPE = "||";
        lastLogMessage = lastLogMessage.contains(DELIMITER_PIPE) ? lastLogMessage.substring(0, lastLogMessage.lastIndexOf(DELIMITER_PIPE)) : lastLogMessage;
        lastLogMessage = StringEscapeUtils.escapeJava(lastLogMessage);
        return lastLogMessage;
    }
}
